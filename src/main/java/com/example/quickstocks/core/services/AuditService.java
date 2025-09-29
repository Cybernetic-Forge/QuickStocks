package com.example.quickstocks.core.services;

import com.example.quickstocks.infrastructure.db.Db;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Handles integrity auditing and repair operations for the trading system.
 * Validates portfolio consistency and can repair detected discrepancies.
 */
public class AuditService {
    
    private static final Logger logger = Logger.getLogger(AuditService.class.getName());
    
    private final Db database;
    private final HoldingsService holdingsService;
    
    public AuditService(Db database, HoldingsService holdingsService) {
        this.database = database;
        this.holdingsService = holdingsService;
    }
    
    /**
     * Performs a comprehensive audit of all player holdings against order history.
     * @param repair if true, automatically repairs detected inconsistencies
     * @return audit result with findings and actions taken
     */
    public AuditResult auditAllHoldings(boolean repair) throws SQLException {
        logger.info("Starting comprehensive holdings audit" + (repair ? " with auto-repair" : ""));
        
        AuditResult result = new AuditResult();
        
        // Get all players who have either holdings or orders
        Set<String> allPlayers = new HashSet<>();
        
        // Add players with current holdings
        List<Map<String, Object>> holdingsPlayers = database.query(
            "SELECT DISTINCT player_uuid FROM user_holdings WHERE qty > 0"
        );
        for (Map<String, Object> row : holdingsPlayers) {
            allPlayers.add((String) row.get("player_uuid"));
        }
        
        // Add players with order history
        List<Map<String, Object>> orderPlayers = database.query(
            "SELECT DISTINCT player_uuid FROM orders"
        );
        for (Map<String, Object> row : orderPlayers) {
            allPlayers.add((String) row.get("player_uuid"));
        }
        
        result.totalPlayersChecked = allPlayers.size();
        
        for (String playerUuid : allPlayers) {
            AuditPlayerResult playerResult = auditPlayerHoldings(playerUuid, repair);
            result.playerResults.put(playerUuid, playerResult);
            
            if (!playerResult.discrepancies.isEmpty()) {
                result.playersWithIssues++;
                result.totalDiscrepancies += playerResult.discrepancies.size();
                
                if (repair) {
                    result.totalRepairs += playerResult.repairsApplied;
                }
            }
        }
        
        // Log audit completion
        logAuditEvent("INTEGRITY_CHECK", null, null, result.toJson());
        
        logger.info(String.format("Audit completed: %d players checked, %d with issues, %d discrepancies found%s",
            result.totalPlayersChecked, result.playersWithIssues, result.totalDiscrepancies,
            repair ? ", " + result.totalRepairs + " repairs applied" : ""));
        
        return result;
    }
    
    /**
     * Audits holdings for a specific player against their order history.
     */
    public AuditPlayerResult auditPlayerHoldings(String playerUuid, boolean repair) throws SQLException {
        AuditPlayerResult result = new AuditPlayerResult(playerUuid);
        
        // Calculate expected holdings from order history
        Map<String, Double> expectedHoldings = calculateExpectedHoldings(playerUuid);
        
        // Get current holdings from database
        Map<String, Double> currentHoldings = getCurrentHoldings(playerUuid);
        
        // Find discrepancies
        Set<String> allInstruments = new HashSet<>(expectedHoldings.keySet());
        allInstruments.addAll(currentHoldings.keySet());
        
        for (String instrumentId : allInstruments) {
            double expected = expectedHoldings.getOrDefault(instrumentId, 0.0);
            double current = currentHoldings.getOrDefault(instrumentId, 0.0);
            
            if (Math.abs(expected - current) > 0.001) { // Use small epsilon for floating point comparison
                Discrepancy discrepancy = new Discrepancy(instrumentId, expected, current);
                result.discrepancies.add(discrepancy);
                
                if (repair) {
                    try {
                        repairHolding(playerUuid, instrumentId, expected);
                        result.repairsApplied++;
                        logger.info(String.format("Repaired holding for %s: %s from %.4f to %.4f",
                            playerUuid, instrumentId, current, expected));
                    } catch (SQLException e) {
                        logger.warning(String.format("Failed to repair holding for %s: %s - %s",
                            playerUuid, instrumentId, e.getMessage()));
                    }
                }
            }
        }
        
        return result;
    }
    
    /**
     * Calculates expected holdings based on order history.
     */
    private Map<String, Double> calculateExpectedHoldings(String playerUuid) throws SQLException {
        Map<String, Double> holdings = new HashMap<>();
        
        List<Map<String, Object>> orders = database.query(
            """
            SELECT instrument_id, side, qty
            FROM orders
            WHERE player_uuid = ?
            ORDER BY ts ASC
            """,
            playerUuid
        );
        
        for (Map<String, Object> order : orders) {
            String instrumentId = (String) order.get("instrument_id");
            String side = (String) order.get("side");
            double qty = ((Number) order.get("qty")).doubleValue();
            
            double currentQty = holdings.getOrDefault(instrumentId, 0.0);
            
            if ("BUY".equals(side)) {
                holdings.put(instrumentId, currentQty + qty);
            } else if ("SELL".equals(side)) {
                holdings.put(instrumentId, currentQty - qty);
            }
        }
        
        // Remove instruments with zero or negative holdings
        holdings.entrySet().removeIf(entry -> entry.getValue() <= 0.001);
        
        return holdings;
    }
    
    /**
     * Gets current holdings from the database.
     */
    private Map<String, Double> getCurrentHoldings(String playerUuid) throws SQLException {
        Map<String, Double> holdings = new HashMap<>();
        
        List<Map<String, Object>> currentHoldings = database.query(
            "SELECT instrument_id, qty FROM user_holdings WHERE player_uuid = ? AND qty > 0",
            playerUuid
        );
        
        for (Map<String, Object> holding : currentHoldings) {
            String instrumentId = (String) holding.get("instrument_id");
            double qty = ((Number) holding.get("qty")).doubleValue();
            holdings.put(instrumentId, qty);
        }
        
        return holdings;
    }
    
    /**
     * Repairs a specific holding by setting it to the expected value.
     */
    private void repairHolding(String playerUuid, String instrumentId, double expectedQty) throws SQLException {
        if (expectedQty <= 0.001) {
            // Remove the holding
            database.execute(
                "DELETE FROM user_holdings WHERE player_uuid = ? AND instrument_id = ?",
                playerUuid, instrumentId
            );
        } else {
            // Update or insert the holding with correct quantity
            database.execute(
                """
                INSERT OR REPLACE INTO user_holdings (player_uuid, instrument_id, qty, avg_cost, version)
                VALUES (?, ?, ?, 0.0, 1)
                """,
                playerUuid, instrumentId, expectedQty
            );
        }
        
        // Log the repair
        String details = String.format("{\"action\":\"repair\",\"instrument_id\":\"%s\",\"expected_qty\":%.4f}",
            instrumentId, expectedQty);
        logAuditEvent("REPAIR", playerUuid, instrumentId, details);
    }
    
    /**
     * Logs an audit event to the audit log.
     */
    private void logAuditEvent(String auditType, String playerUuid, String instrumentId, String details) throws SQLException {
        String auditId = UUID.randomUUID().toString();
        database.execute(
            "INSERT INTO audit_log (id, audit_type, player_uuid, instrument_id, details, timestamp) VALUES (?, ?, ?, ?, ?, ?)",
            auditId, auditType, playerUuid, instrumentId, details, System.currentTimeMillis()
        );
    }
    
    /**
     * Result of an audit operation.
     */
    public static class AuditResult {
        public int totalPlayersChecked = 0;
        public int playersWithIssues = 0;
        public int totalDiscrepancies = 0;
        public int totalRepairs = 0;
        public Map<String, AuditPlayerResult> playerResults = new HashMap<>();
        
        public String toJson() {
            return String.format(
                "{\"totalPlayersChecked\":%d,\"playersWithIssues\":%d,\"totalDiscrepancies\":%d,\"totalRepairs\":%d}",
                totalPlayersChecked, playersWithIssues, totalDiscrepancies, totalRepairs
            );
        }
    }
    
    /**
     * Result of auditing a specific player.
     */
    public static class AuditPlayerResult {
        public final String playerUuid;
        public List<Discrepancy> discrepancies = new ArrayList<>();
        public int repairsApplied = 0;
        
        public AuditPlayerResult(String playerUuid) {
            this.playerUuid = playerUuid;
        }
    }
    
    /**
     * Represents a discrepancy between expected and actual holdings.
     */
    public static class Discrepancy {
        public final String instrumentId;
        public final double expectedQty;
        public final double actualQty;
        public final double difference;
        
        public Discrepancy(String instrumentId, double expectedQty, double actualQty) {
            this.instrumentId = instrumentId;
            this.expectedQty = expectedQty;
            this.actualQty = actualQty;
            this.difference = actualQty - expectedQty;
        }
        
        @Override
        public String toString() {
            return String.format("%s: expected %.4f, actual %.4f, diff %.4f",
                instrumentId, expectedQty, actualQty, difference);
        }
    }
}