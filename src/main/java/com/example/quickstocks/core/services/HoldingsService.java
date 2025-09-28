package com.example.quickstocks.core.services;

import com.example.quickstocks.infrastructure.db.Db;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Service for managing player portfolio holdings.
 */
public class HoldingsService {
    
    private final Db database;
    
    public HoldingsService(Db database) {
        this.database = database;
    }
    
    /**
     * Gets all holdings for a player.
     */
    public List<Map<String, Object>> getHoldings(String playerUuid) throws SQLException {
        return database.query("""
            SELECT h.instrument_id, h.qty, h.avg_cost, i.symbol, i.display_name, i.type,
                   s.last_price, s.change_24h
            FROM user_holdings h
            JOIN instruments i ON h.instrument_id = i.id
            LEFT JOIN instrument_state s ON h.instrument_id = s.instrument_id
            WHERE h.player_uuid = ? AND h.qty > 0
            ORDER BY (h.qty * COALESCE(s.last_price, 0)) DESC
            """, playerUuid);
    }
    
    /**
     * Adds holdings for buy orders.
     */
    public void addHolding(String playerUuid, String instrumentId, double qty, double price) throws SQLException {
        database.execute("""
            INSERT INTO user_holdings (player_uuid, instrument_id, qty, avg_cost)
            VALUES (?, ?, ?, ?)
            ON CONFLICT(player_uuid, instrument_id) DO UPDATE SET
                qty = qty + ?,
                avg_cost = (qty * avg_cost + ? * ?) / (qty + ?)
            """, playerUuid, instrumentId, qty, price, qty, qty, price, qty);
        
        // Clean up zero positions
        database.execute("""
            DELETE FROM user_holdings 
            WHERE player_uuid = ? AND instrument_id = ? AND qty <= 0
            """, playerUuid, instrumentId);
    }
    
    /**
     * Removes holdings for sell orders.
     */
    public boolean removeHolding(String playerUuid, String instrumentId, double qty) throws SQLException {
        Double currentQty = database.queryValue("""
            SELECT qty FROM user_holdings 
            WHERE player_uuid = ? AND instrument_id = ?
            """, playerUuid, instrumentId);
        
        if (currentQty == null || currentQty < qty) {
            return false; // Insufficient holdings
        }
        
        database.execute("""
            UPDATE user_holdings 
            SET qty = qty - ? 
            WHERE player_uuid = ? AND instrument_id = ?
            """, qty, playerUuid, instrumentId);
        
        // Clean up zero positions
        database.execute("""
            DELETE FROM user_holdings 
            WHERE player_uuid = ? AND instrument_id = ? AND qty <= 0
            """, playerUuid, instrumentId);
        
        return true;
    }
    
    /**
     * Gets total portfolio value for a player.
     */
    public double getPortfolioValue(String playerUuid) throws SQLException {
        Double value = database.queryValue("""
            SELECT COALESCE(SUM(h.qty * COALESCE(s.last_price, 0)), 0)
            FROM user_holdings h
            LEFT JOIN instrument_state s ON h.instrument_id = s.instrument_id
            WHERE h.player_uuid = ? AND h.qty > 0
            """, playerUuid);
        
        return value != null ? value : 0.0;
    }
    
    /**
     * Gets quantity of a specific holding.
     */
    public double getHoldingQuantity(String playerUuid, String instrumentId) throws SQLException {
        Double qty = database.queryValue("""
            SELECT qty FROM user_holdings 
            WHERE player_uuid = ? AND instrument_id = ?
            """, playerUuid, instrumentId);
        
        return qty != null ? qty : 0.0;
    }
}