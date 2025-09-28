package com.example.quickstocks.core.services;

import com.example.quickstocks.infrastructure.db.Db;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Handles trading operations including buy/sell orders and execution.
 */
public class TradingService {
    
    private static final Logger logger = Logger.getLogger(TradingService.class.getName());
    
    private final Db database;
    private final WalletService walletService;
    private final HoldingsService holdingsService;
    
    public TradingService(Db database, WalletService walletService, HoldingsService holdingsService) {
        this.database = database;
        this.walletService = walletService;
        this.holdingsService = holdingsService;
    }
    
    /**
     * Executes a market buy order at current price.
     */
    public TradeResult executeBuyOrder(String playerUuid, String instrumentId, double qty) throws SQLException {
        // Get current price
        Double currentPrice = database.queryValue(
            "SELECT last_price FROM instrument_state WHERE instrument_id = ?",
            instrumentId
        );
        
        if (currentPrice == null) {
            return new TradeResult(false, "Instrument not found or price unavailable");
        }
        
        double totalCost = qty * currentPrice;
        
        // Check if player has sufficient balance
        if (!walletService.hasBalance(playerUuid, totalCost)) {
            return new TradeResult(false, "Insufficient funds. Required: $" + String.format("%.2f", totalCost));
        }
        
        // Execute the trade in a transaction-like manner
        try {
            // Remove money from wallet
            if (!walletService.removeBalance(playerUuid, totalCost)) {
                return new TradeResult(false, "Failed to debit wallet");
            }
            
            // Add shares to holdings
            holdingsService.addHolding(playerUuid, instrumentId, qty, currentPrice);
            
            // Record the order
            String orderId = UUID.randomUUID().toString();
            database.execute(
                "INSERT INTO orders (id, player_uuid, instrument_id, side, qty, price, ts) VALUES (?, ?, ?, ?, ?, ?, ?)",
                orderId, playerUuid, instrumentId, "BUY", qty, currentPrice, System.currentTimeMillis()
            );
            
            String message = String.format("BUY %.2f shares at $%.2f per share (Total: $%.2f)", 
                qty, currentPrice, totalCost);
            
            logger.info("Executed buy order for " + playerUuid + ": " + message);
            return new TradeResult(true, message);
            
        } catch (SQLException e) {
            logger.warning("Failed to execute buy order: " + e.getMessage());
            // Try to rollback wallet debit (basic attempt)
            try {
                walletService.addBalance(playerUuid, totalCost);
            } catch (SQLException rollbackError) {
                logger.severe("Failed to rollback wallet debit: " + rollbackError.getMessage());
            }
            return new TradeResult(false, "Trade execution failed: " + e.getMessage());
        }
    }
    
    /**
     * Executes a market sell order at current price.
     */
    public TradeResult executeSellOrder(String playerUuid, String instrumentId, double qty) throws SQLException {
        // Get current price
        Double currentPrice = database.queryValue(
            "SELECT last_price FROM instrument_state WHERE instrument_id = ?",
            instrumentId
        );
        
        if (currentPrice == null) {
            return new TradeResult(false, "Instrument not found or price unavailable");
        }
        
        // Check if player has sufficient shares
        HoldingsService.Holding holding = holdingsService.getHolding(playerUuid, instrumentId);
        if (holding == null || holding.getQty() < qty) {
            return new TradeResult(false, "Insufficient shares. Available: " + 
                (holding != null ? String.format("%.2f", holding.getQty()) : "0"));
        }
        
        double totalValue = qty * currentPrice;
        
        // Execute the trade
        try {
            // Remove shares from holdings
            if (!holdingsService.removeHolding(playerUuid, instrumentId, qty)) {
                return new TradeResult(false, "Failed to remove shares from portfolio");
            }
            
            // Add money to wallet
            walletService.addBalance(playerUuid, totalValue);
            
            // Record the order
            String orderId = UUID.randomUUID().toString();
            database.execute(
                "INSERT INTO orders (id, player_uuid, instrument_id, side, qty, price, ts) VALUES (?, ?, ?, ?, ?, ?, ?)",
                orderId, playerUuid, instrumentId, "SELL", qty, currentPrice, System.currentTimeMillis()
            );
            
            String message = String.format("SELL %.2f shares at $%.2f per share (Total: $%.2f)", 
                qty, currentPrice, totalValue);
            
            logger.info("Executed sell order for " + playerUuid + ": " + message);
            return new TradeResult(true, message);
            
        } catch (SQLException e) {
            logger.warning("Failed to execute sell order: " + e.getMessage());
            // Try to rollback share removal (basic attempt)
            try {
                holdingsService.addHolding(playerUuid, instrumentId, qty, currentPrice);
            } catch (SQLException rollbackError) {
                logger.severe("Failed to rollback share removal: " + rollbackError.getMessage());
            }
            return new TradeResult(false, "Trade execution failed: " + e.getMessage());
        }
    }
    
    /**
     * Gets trading history for a player.
     */
    public List<Order> getOrderHistory(String playerUuid, int limit) throws SQLException {
        List<Map<String, Object>> results = database.query(
            """
            SELECT o.id, o.instrument_id, o.side, o.qty, o.price, o.ts, i.symbol, i.display_name
            FROM orders o
            JOIN instruments i ON o.instrument_id = i.id
            WHERE o.player_uuid = ?
            ORDER BY o.ts DESC
            LIMIT ?
            """,
            playerUuid, limit
        );
        
        List<Order> orders = new ArrayList<>();
        for (Map<String, Object> row : results) {
            orders.add(new Order(
                (String) row.get("id"),
                (String) row.get("instrument_id"),
                (String) row.get("symbol"),
                (String) row.get("display_name"),
                (String) row.get("side"),
                ((Number) row.get("qty")).doubleValue(),
                ((Number) row.get("price")).doubleValue(),
                ((Number) row.get("ts")).longValue()
            ));
        }
        
        return orders;
    }
    
    /**
     * Result of a trading operation.
     */
    public static class TradeResult {
        private final boolean success;
        private final String message;
        
        public TradeResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }
    
    /**
     * Represents a trading order.
     */
    public static class Order {
        private final String id;
        private final String instrumentId;
        private final String symbol;
        private final String displayName;
        private final String side;
        private final double qty;
        private final double price;
        private final long timestamp;
        
        public Order(String id, String instrumentId, String symbol, String displayName,
                    String side, double qty, double price, long timestamp) {
            this.id = id;
            this.instrumentId = instrumentId;
            this.symbol = symbol;
            this.displayName = displayName;
            this.side = side;
            this.qty = qty;
            this.price = price;
            this.timestamp = timestamp;
        }
        
        public String getId() { return id; }
        public String getInstrumentId() { return instrumentId; }
        public String getSymbol() { return symbol; }
        public String getDisplayName() { return displayName; }
        public String getSide() { return side; }
        public double getQty() { return qty; }
        public double getPrice() { return price; }
        public long getTimestamp() { return timestamp; }
        public double getTotalValue() { return qty * price; }
    }
}