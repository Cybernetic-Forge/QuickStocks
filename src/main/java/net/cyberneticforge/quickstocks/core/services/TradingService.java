package net.cyberneticforge.quickstocks.core.services;

import lombok.Setter;
import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.core.model.OrderRequest;
import net.cyberneticforge.quickstocks.infrastructure.config.TradingCfg;
import net.cyberneticforge.quickstocks.infrastructure.db.Db;
import net.cyberneticforge.quickstocks.infrastructure.logging.PluginLogger;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles trading operations including buy/sell orders and execution.
 * Now includes enhanced economy features when TradingConfig is provided.
 * <p>
 * NOTE: This service is for traditional instruments (items, crypto, etc.).
 * For market stocks (company shares), use CompanyMarketService instead.
 */
@SuppressWarnings("ALL")
public class TradingService {

    private static final PluginLogger logger = QuickStocksPlugin.getPluginLogger();

    private final Db database = QuickStocksPlugin.getDatabaseManager().getDb();
    private final EnhancedTradingService enhancedTradingService;
    /**
     * -- SETTER --
     *  Sets the stock market service for recording trading activity.
     *  This is called after both services are initialized.
     */
    @Setter
    private StockMarketService stockMarketService; // For recording trading activity


    // Constructor with enhanced features
    public TradingService(TradingCfg tradingConfig) {
        this.enhancedTradingService = new EnhancedTradingService(database, tradingConfig);
    }

    // Legacy constructor for backward compatibility
    public TradingService() {
        this.enhancedTradingService = null; // No enhanced features
    }

    /**
     * Executes a market buy order at current price.
     * Uses enhanced trading features if available.
     */
    public TradeResult executeBuyOrder(String playerUuid, String instrumentId, double qty) throws SQLException {
        // Use enhanced service if available
        if (enhancedTradingService != null) {
            return enhancedTradingService.executeBuyOrder(playerUuid, instrumentId, qty);
        }

        // Legacy implementation
        return executeBuyOrderLegacy(playerUuid, instrumentId, qty);
    }

    /**
     * Legacy buy order implementation for backward compatibility.
     */
    private TradeResult executeBuyOrderLegacy(String playerUuid, String instrumentId, double qty) throws SQLException {
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
        if (!QuickStocksPlugin.getWalletService().hasBalance(playerUuid, totalCost)) {
            return new TradeResult(false, "Insufficient funds. Required: $" + String.format("%.2f", totalCost));
        }

        // Execute the trade in a transaction-like manner
        try {
            // Remove money from wallet
            if (!QuickStocksPlugin.getWalletService().removeBalance(playerUuid, totalCost)) {
                return new TradeResult(false, "Failed to debit wallet");
            }

            // Add shares to holdings
            QuickStocksPlugin.getHoldingsService().addHolding(playerUuid, instrumentId, qty, currentPrice);

            // Record the order with enhanced fields for compatibility
            String orderId = UUID.randomUUID().toString();
            database.execute(
                    "INSERT INTO orders (id, player_uuid, instrument_id, side, qty, price, ts, order_type, execution_price) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    orderId, playerUuid, instrumentId, "BUY", qty, currentPrice, System.currentTimeMillis(), "MARKET", currentPrice
            );

            String message = String.format("BUY %.2f shares at $%.2f per share (Total: $%.2f)",
                    qty, currentPrice, totalCost);

            // Record trading activity for threshold calculations
            recordTradingActivity(instrumentId, (int) qty);

            logger.info("Executed buy order for " + playerUuid + ": " + message);
            return new TradeResult(true, message);

        } catch (SQLException e) {
            logger.warning("Failed to execute buy order: " + e.getMessage());
            // Try to rollback wallet debit (basic attempt)
            try {
                QuickStocksPlugin.getWalletService().addBalance(playerUuid, totalCost);
            } catch (SQLException rollbackError) {
                logger.severe("Failed to rollback wallet debit: " + rollbackError.getMessage());
            }
            return new TradeResult(false, "Trade execution failed: " + e.getMessage());
        }
    }

    /**
     * Executes a market sell order at current price.
     * Uses enhanced trading features if available.
     */
    public TradeResult executeSellOrder(String playerUuid, String instrumentId, double qty) throws SQLException {
        // Use enhanced service if available
        if (enhancedTradingService != null) {
            return enhancedTradingService.executeSellOrder(playerUuid, instrumentId, qty);
        }

        // Legacy implementation
        return executeSellOrderLegacy(playerUuid, instrumentId, qty);
    }

    /**
     * Legacy sell order implementation for backward compatibility.
     */
    private TradeResult executeSellOrderLegacy(String playerUuid, String instrumentId, double qty) throws SQLException {
        // Get current price
        Double currentPrice = database.queryValue(
                "SELECT last_price FROM instrument_state WHERE instrument_id = ?",
                instrumentId
        );

        if (currentPrice == null) {
            return new TradeResult(false, "Instrument not found or price unavailable");
        }

        // Check if player has sufficient shares
        HoldingsService.Holding holding = QuickStocksPlugin.getHoldingsService().getHolding(playerUuid, instrumentId);
        if (holding == null || holding.qty() < qty) {
            return new TradeResult(false, "Insufficient shares. Available: " +
                    (holding != null ? String.format("%.2f", holding.qty()) : "0"));
        }

        double totalValue = qty * currentPrice;

        // Execute the trade
        try {
            // Remove shares from holdings
            if (!QuickStocksPlugin.getHoldingsService().removeHolding(playerUuid, instrumentId, qty)) {
                return new TradeResult(false, "Failed to remove shares from portfolio");
            }

            // Add money to wallet
            QuickStocksPlugin.getWalletService().addBalance(playerUuid, totalValue);

            // Record the order with enhanced fields for compatibility
            String orderId = UUID.randomUUID().toString();
            database.execute(
                    "INSERT INTO orders (id, player_uuid, instrument_id, side, qty, price, ts, order_type, execution_price) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    orderId, playerUuid, instrumentId, "SELL", qty, currentPrice, System.currentTimeMillis(), "MARKET", currentPrice
            );

            String message = String.format("SELL %.2f shares at $%.2f per share (Total: $%.2f)",
                    qty, currentPrice, totalValue);

            // Record trading activity for threshold calculations
            recordTradingActivity(instrumentId, (int) qty);

            logger.info("Executed sell order for " + playerUuid + ": " + message);
            return new TradeResult(true, message);

        } catch (SQLException e) {
            logger.warning("Failed to execute sell order: " + e.getMessage());
            // Try to rollback share removal (basic attempt)
            try {
                QuickStocksPlugin.getHoldingsService().addHolding(playerUuid, instrumentId, qty, currentPrice);
            } catch (SQLException rollbackError) {
                logger.severe("Failed to rollback share removal: " + rollbackError.getMessage());
            }
            return new TradeResult(false, "Trade execution failed: " + e.getMessage());
        }
    }

    /**
     * Executes an enhanced order with full economy features.
     * Only available if TradingConfig was provided during construction.
     */
    public TradeResult executeOrder(OrderRequest orderRequest) {
        if (enhancedTradingService == null) {
            throw new UnsupportedOperationException("Enhanced trading features not available. Provide TradingConfig during construction.");
        }
        return enhancedTradingService.executeOrder(orderRequest);
    }

    /**
     * Gets trading history for a player.
     * Uses enhanced service if available for better order details.
     */
    public List<Order> getOrderHistory(String playerUuid, int limit) throws SQLException {
        // Use enhanced service if available for richer order data
        if (enhancedTradingService != null) {
            List<EnhancedTradingService.Order> enhancedOrders = enhancedTradingService.getOrderHistory(playerUuid, limit);
            return getOrders(enhancedOrders);
        }

        // Legacy implementation with enhanced field support
        List<Map<String, Object>> results = database.query(
                """
                        SELECT o.id, o.instrument_id, o.side, o.qty, o.price, o.ts, i.symbol, i.display_name,
                               COALESCE(o.execution_price, o.price) as execution_price
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
            // Use execution_price if available, otherwise fall back to price
            double effectivePrice = row.get("execution_price") != null ?
                    ((Number) row.get("execution_price")).doubleValue() :
                    ((Number) row.get("price")).doubleValue();

            orders.add(new Order(
                    (String) row.get("id"),
                    (String) row.get("instrument_id"),
                    (String) row.get("symbol"),
                    (String) row.get("display_name"),
                    (String) row.get("side"),
                    ((Number) row.get("qty")).doubleValue(),
                    effectivePrice,
                    ((Number) row.get("ts")).longValue()
            ));
        }

        return orders;
    }

    private static @NotNull List<Order> getOrders(List<EnhancedTradingService.Order> enhancedOrders) {
        List<Order> orders = new ArrayList<>();

        // Convert enhanced orders to legacy format
        for (EnhancedTradingService.Order enhancedOrder : enhancedOrders) {
            orders.add(new Order(
                    enhancedOrder.id(),
                    enhancedOrder.instrumentId(),
                    enhancedOrder.symbol(),
                    enhancedOrder.displayName(),
                    enhancedOrder.side(),
                    enhancedOrder.qty(),
                    enhancedOrder.executionPrice(), // Use execution price instead of original price
                    enhancedOrder.timestamp()
            ));
        }
        return orders;
    }

    /**
     * Records trading activity for a stock symbol to be used in threshold calculations.
     */
    private void recordTradingActivity(String instrumentId, int volume) {
        if (stockMarketService != null && stockMarketService.getThresholdController() != null) {
            // Convert instrument ID to symbol if needed
            try {
                String symbol = database.queryValue(
                        "SELECT symbol FROM instrument_state WHERE instrument_id = ?",
                        instrumentId
                );
                if (symbol != null) {
                    stockMarketService.getThresholdController().recordTradingActivity(symbol, volume);
                }
            } catch (SQLException e) {
                logger.debug("Could not record trading activity for " + instrumentId + ": " + e.getMessage());
            }
        }
    }

    /**
     * Result of a trading operation.
     */
    public record TradeResult(boolean success, String message) {
    }

    /**
     * Represents a trading order.
     */
    public record Order(String id, String instrumentId, String symbol, String displayName, String side, double qty,
                        double price, long timestamp) {
        public double getTotalValue() {
            return qty * price;
        }
    }
}