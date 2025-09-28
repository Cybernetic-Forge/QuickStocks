package com.example.quickstocks.core.services;

import com.example.quickstocks.infrastructure.db.Db;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for executing trades and managing order history.
 */
public class TradingService {
    
    private final Db database;
    private final WalletService walletService;
    private final HoldingsService holdingsService;
    
    public TradingService(Db database, WalletService walletService, HoldingsService holdingsService) {
        this.database = database;
        this.walletService = walletService;
        this.holdingsService = holdingsService;
    }
    
    /**
     * Executes a buy order.
     */
    public boolean executeBuyOrder(String playerUuid, String instrumentId, double qty) throws SQLException {
        // Get current price
        Double price = database.queryValue("""
            SELECT last_price FROM instrument_state WHERE instrument_id = ?
            """, instrumentId);
        
        if (price == null || price <= 0) {
            return false; // No price available
        }
        
        double totalCost = qty * price;
        
        // Check balance
        if (!walletService.hasBalance(playerUuid, totalCost)) {
            return false; // Insufficient funds
        }
        
        // Execute transaction
        database.withTransaction(() -> {
            // Debit wallet
            walletService.removeBalance(playerUuid, totalCost);
            
            // Add holdings
            holdingsService.addHolding(playerUuid, instrumentId, qty, price);
            
            // Record order
            recordOrder(playerUuid, instrumentId, "BUY", qty, price);
            
            return null;
        });
        
        return true;
    }
    
    /**
     * Executes a sell order.
     */
    public boolean executeSellOrder(String playerUuid, String instrumentId, double qty) throws SQLException {
        // Get current price
        Double price = database.queryValue("""
            SELECT last_price FROM instrument_state WHERE instrument_id = ?
            """, instrumentId);
        
        if (price == null || price <= 0) {
            return false; // No price available
        }
        
        // Check holdings
        if (holdingsService.getHoldingQuantity(playerUuid, instrumentId) < qty) {
            return false; // Insufficient holdings
        }
        
        double totalValue = qty * price;
        
        // Execute transaction
        database.withTransaction(() -> {
            // Remove holdings
            holdingsService.removeHolding(playerUuid, instrumentId, qty);
            
            // Credit wallet
            walletService.addBalance(playerUuid, totalValue);
            
            // Record order
            recordOrder(playerUuid, instrumentId, "SELL", qty, price);
            
            return null;
        });
        
        return true;
    }
    
    /**
     * Records an order in history.
     */
    private void recordOrder(String playerUuid, String instrumentId, String side, double qty, double price) throws SQLException {
        String orderId = UUID.randomUUID().toString();
        long timestamp = System.currentTimeMillis();
        
        database.execute("""
            INSERT INTO orders (id, player_uuid, instrument_id, side, qty, price, ts)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """, orderId, playerUuid, instrumentId, side, qty, price, timestamp);
    }
    
    /**
     * Gets order history for a player.
     */
    public List<Map<String, Object>> getOrderHistory(String playerUuid, int limit) throws SQLException {
        return database.query("""
            SELECT o.id, o.side, o.qty, o.price, o.ts, i.symbol, i.display_name
            FROM orders o
            JOIN instruments i ON o.instrument_id = i.id
            WHERE o.player_uuid = ?
            ORDER BY o.ts DESC
            LIMIT ?
            """, playerUuid, limit);
    }
    
    /**
     * Gets current price for an instrument.
     */
    public Double getCurrentPrice(String instrumentId) throws SQLException {
        return database.queryValue("""
            SELECT last_price FROM instrument_state WHERE instrument_id = ?
            """, instrumentId);
    }
    
    /**
     * Validates if an instrument exists and is tradeable.
     */
    public boolean isInstrumentTradeable(String instrumentId) throws SQLException {
        Integer count = database.queryValue("""
            SELECT COUNT(*) FROM instruments WHERE id = ?
            """, instrumentId);
        
        return count != null && count > 0;
    }
}