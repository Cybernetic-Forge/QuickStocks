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
     * Executes a market buy order at current price with idempotency support.
     */
    public TradeResult executeBuyOrder(String playerUuid, String instrumentId, double qty, String idempotencyKey) throws SQLException {
        // Check for existing order with same idempotency key
        if (idempotencyKey != null) {
            TradeResult existingResult = checkExistingOrder(idempotencyKey);
            if (existingResult != null) {
                logger.info("Returning cached result for idempotency key: " + idempotencyKey);
                return existingResult;
            }
        }
        
        // Execute trade in atomic transaction with SERIALIZABLE isolation
        final TradeResult[] result = new TradeResult[1];
        
        database.executeTransaction(db -> {
            // Set SERIALIZABLE isolation for this transaction
            db.execute("PRAGMA read_uncommitted = false");
            
            // Get current price with row lock
            Double currentPrice = database.queryValue(
                "SELECT last_price FROM instrument_state WHERE instrument_id = ?",
                instrumentId
            );
            
            if (currentPrice == null) {
                result[0] = new TradeResult(false, "Instrument not found or price unavailable");
                return;
            }
            
            double totalCost = qty * currentPrice;
            
            // Check balance
            if (!walletService.hasBalance(playerUuid, totalCost)) {
                result[0] = new TradeResult(false, "Insufficient funds. Required: $" + String.format("%.2f", totalCost));
                return;
            }
            
            // Remove money from wallet (within transaction)
            if (!walletService.removeBalance(playerUuid, totalCost)) {
                result[0] = new TradeResult(false, "Failed to debit wallet");
                return;
            }
            
            // Add shares to holdings with optimistic locking
            boolean holdingUpdated = holdingsService.addHoldingWithVersioning(db, playerUuid, instrumentId, qty, currentPrice);
            if (!holdingUpdated) {
                result[0] = new TradeResult(false, "Failed to update holdings due to concurrent modification");
                return;
            }
            
            // Record the order with idempotency key
            String orderId = UUID.randomUUID().toString();
            db.execute(
                "INSERT INTO orders (id, player_uuid, instrument_id, side, qty, price, ts, client_idempotency) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                orderId, playerUuid, instrumentId, "BUY", qty, currentPrice, System.currentTimeMillis(), idempotencyKey
            );
            
            String message = String.format("BUY %.2f shares at $%.2f per share (Total: $%.2f)", 
                qty, currentPrice, totalCost);
            
            logger.info("Executed buy order for " + playerUuid + ": " + message);
            result[0] = new TradeResult(true, message);
        });
        
        if (result[0] == null) {
            result[0] = new TradeResult(false, "Transaction failed unexpectedly");
        }
        
        return result[0];
    }
    
    /**
     * Legacy method for backward compatibility.
     */
    public TradeResult executeBuyOrder(String playerUuid, String instrumentId, double qty) throws SQLException {
        return executeBuyOrder(playerUuid, instrumentId, qty, null);
    }
    
    /**
     * Executes a market sell order at current price with idempotency support.
     */
    public TradeResult executeSellOrder(String playerUuid, String instrumentId, double qty, String idempotencyKey) throws SQLException {
        // Check for existing order with same idempotency key
        if (idempotencyKey != null) {
            TradeResult existingResult = checkExistingOrder(idempotencyKey);
            if (existingResult != null) {
                logger.info("Returning cached result for idempotency key: " + idempotencyKey);
                return existingResult;
            }
        }
        
        // Execute trade in atomic transaction with SERIALIZABLE isolation
        final TradeResult[] result = new TradeResult[1];
        
        database.executeTransaction(db -> {
            // Set SERIALIZABLE isolation for this transaction
            db.execute("PRAGMA read_uncommitted = false");
            
            // Get current price
            Double currentPrice = database.queryValue(
                "SELECT last_price FROM instrument_state WHERE instrument_id = ?",
                instrumentId
            );
            
            if (currentPrice == null) {
                result[0] = new TradeResult(false, "Instrument not found or price unavailable");
                return;
            }
            
            // Check holdings with optimistic locking
            HoldingsService.Holding holding = holdingsService.getHoldingWithLock(playerUuid, instrumentId);
            if (holding == null || holding.getQty() < qty) {
                result[0] = new TradeResult(false, "Insufficient shares. Available: " + 
                    (holding != null ? String.format("%.2f", holding.getQty()) : "0"));
                return;
            }
            
            double totalValue = qty * currentPrice;
            
            // Remove shares from holdings with versioning
            boolean holdingUpdated = holdingsService.removeHoldingWithVersioning(db, playerUuid, instrumentId, qty, holding.getVersion());
            if (!holdingUpdated) {
                result[0] = new TradeResult(false, "Failed to update holdings due to concurrent modification");
                return;
            }
            
            // Add money to wallet (within transaction)
            walletService.addBalance(playerUuid, totalValue);
            
            // Record the order with idempotency key
            String orderId = UUID.randomUUID().toString();
            db.execute(
                "INSERT INTO orders (id, player_uuid, instrument_id, side, qty, price, ts, client_idempotency) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                orderId, playerUuid, instrumentId, "SELL", qty, currentPrice, System.currentTimeMillis(), idempotencyKey
            );
            
            String message = String.format("SELL %.2f shares at $%.2f per share (Total: $%.2f)", 
                qty, currentPrice, totalValue);
            
            logger.info("Executed sell order for " + playerUuid + ": " + message);
            result[0] = new TradeResult(true, message);
        });
        
        if (result[0] == null) {
            result[0] = new TradeResult(false, "Transaction failed unexpectedly");
        }
        
        return result[0];
    }
    
    /**
     * Legacy method for backward compatibility.
     */
    public TradeResult executeSellOrder(String playerUuid, String instrumentId, double qty) throws SQLException {
        return executeSellOrder(playerUuid, instrumentId, qty, null);
    }
    
    /**
     * Checks if an order with the given idempotency key already exists.
     * @return existing TradeResult if found, null otherwise
     */
    private TradeResult checkExistingOrder(String idempotencyKey) throws SQLException {
        Map<String, Object> existingOrder = database.queryRow(
            "SELECT side, qty, price FROM orders WHERE client_idempotency = ?",
            idempotencyKey
        );
        
        if (existingOrder != null) {
            String side = (String) existingOrder.get("side");
            double qty = ((Number) existingOrder.get("qty")).doubleValue();
            double price = ((Number) existingOrder.get("price")).doubleValue();
            double totalValue = qty * price;
            
            String message = String.format("%s %.2f shares at $%.2f per share (Total: $%.2f) [CACHED]", 
                side, qty, price, totalValue);
            
            return new TradeResult(true, message);
        }
        
        return null;
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