package net.cyberneticforge.quickstocks.core.services;

import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.core.model.OrderRequest;
import net.cyberneticforge.quickstocks.core.model.OrderType;
import net.cyberneticforge.quickstocks.core.services.TradingService.TradeResult;
import net.cyberneticforge.quickstocks.infrastructure.config.TradingConfig;
import net.cyberneticforge.quickstocks.infrastructure.db.Db;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Enhanced trading service with economy features: fees, limits, circuit breakers,
 * order types, and slippage.
 */
public class EnhancedTradingService {

    private static final Logger logger = Logger.getLogger(EnhancedTradingService.class.getName());

    private final Db database;
    private final FeeService feeService;
    private final SlippageService slippageService;
    private final RateLimitService rateLimitService;
    private final CircuitBreakerService circuitBreakerService;
    private final TradingConfig tradingConfig;

    public EnhancedTradingService(Db database, TradingConfig tradingConfig) {
        this.database = database;
        this.tradingConfig = tradingConfig;

        // Initialize component services
        this.feeService = new FeeService(tradingConfig.getFee());
        this.slippageService = new SlippageService(tradingConfig.getSlippage());
        this.rateLimitService = new RateLimitService(database, tradingConfig.getLimits());
        this.circuitBreakerService = new CircuitBreakerService(database, tradingConfig.getCircuitBreakers());
    }

    /**
     * Executes a trading order with all economy features applied.
     */
    public TradeResult executeOrder(OrderRequest orderRequest) {
        try {
            // Validate the order request
            orderRequest.validate();

            // Check if order type is allowed
            if (!isOrderTypeAllowed(orderRequest.type())) {
                return new TradeResult(false, "Order type " + orderRequest.type() + " is not allowed");
            }

            // Check if trading is halted
            if (circuitBreakerService.isTradingHalted(orderRequest.instrumentId())) {
                CircuitBreakerService.HaltInfo haltInfo = circuitBreakerService.getHaltInfo(orderRequest.instrumentId());
                return new TradeResult(false, "Trading is halted due to circuit breaker (Level " +
                        haltInfo.level() + ")");
            }

            // Get current market price
            Double marketPrice = database.queryValue(
                    "SELECT last_price FROM instrument_state WHERE instrument_id = ?",
                    orderRequest.instrumentId()
            );

            if (marketPrice == null) {
                return new TradeResult(false, "Instrument not found or price unavailable");
            }

            // Determine execution price based on order type
            double executionPrice = determineExecutionPrice(orderRequest, marketPrice);
            if (executionPrice <= 0) {
                return new TradeResult(false, "Order conditions not met for execution");
            }

            // Apply slippage to execution price
            double finalExecutionPrice = slippageService.calculateExecutionPrice(
                    executionPrice, orderRequest.qty(), orderRequest.side()
            );

            // Calculate notional value and fees
            double notionalValue = orderRequest.qty() * finalExecutionPrice;
            double fee = feeService.calculateFee(notionalValue);

            // Check rate limits
            RateLimitService.ValidationResult rateLimitResult = rateLimitService.validateTrade(
                    orderRequest.playerUuid(), orderRequest.qty(), notionalValue
            );
            if (!rateLimitResult.allowed()) {
                return new TradeResult(false, rateLimitResult.message());
            }

            // Execute based on order side
            TradeResult result;
            if ("BUY".equals(orderRequest.side())) {
                result = executeBuyOrder(orderRequest, finalExecutionPrice, fee);
            } else {
                result = executeSellOrder(orderRequest, finalExecutionPrice, fee);
            }

            // If successful, record for rate limiting and check circuit breakers
            if (result.success()) {
                rateLimitService.recordTrade(orderRequest.playerUuid(), notionalValue);
                circuitBreakerService.checkAndTriggerCircuitBreaker(orderRequest.instrumentId(), finalExecutionPrice);
            }

            return result;

        } catch (Exception e) {
            logger.severe("Failed to execute order: " + e.getMessage());
            return new TradeResult(false, "Order execution failed: " + e.getMessage());
        }
    }

    /**
     * Executes a buy order with fees and validation.
     */
    private TradeResult executeBuyOrder(OrderRequest orderRequest, double executionPrice, double fee) throws SQLException {
        double notionalValue = orderRequest.qty() * executionPrice;
        double totalCost = notionalValue + fee;

        // Check wallet balance
        if (!QuickStocksPlugin.getWalletService().hasBalance(orderRequest.playerUuid(), totalCost)) {
            return new TradeResult(false, String.format(
                    "Insufficient funds. Required: $%.2f (including $%.2f fee)", totalCost, fee
            ));
        }

        try {
            // Remove total cost from wallet
            if (!QuickStocksPlugin.getWalletService().removeBalance(orderRequest.playerUuid(), totalCost)) {
                return new TradeResult(false, "Failed to debit wallet");
            }

            // Add shares to holdings
            QuickStocksPlugin.getHoldingsService().addHolding(orderRequest.playerUuid(), orderRequest.instrumentId(),
                    orderRequest.qty(), executionPrice);

            // Record the order
            recordOrder(orderRequest, executionPrice, fee);

            String message = String.format(
                    "%s BUY %.2f shares at $%.2f per share (Total: $%.2f, Fee: $%.2f)",
                    orderRequest.type(), orderRequest.qty(), executionPrice, notionalValue, fee
            );

            logger.info("Executed buy order for " + orderRequest.playerUuid() + ": " + message);
            return new TradeResult(true, message);

        } catch (SQLException e) {
            // Rollback wallet debit
            try {
                QuickStocksPlugin.getWalletService().addBalance(orderRequest.playerUuid(), totalCost);
            } catch (SQLException rollbackError) {
                logger.severe("Failed to rollback wallet debit: " + rollbackError.getMessage());
            }
            throw e;
        }
    }

    /**
     * Executes a sell order with fees and validation.
     */
    private TradeResult executeSellOrder(OrderRequest orderRequest, double executionPrice, double fee) throws SQLException {
        // Check if player has sufficient shares
        HoldingsService.Holding holding = QuickStocksPlugin.getHoldingsService().getHolding(orderRequest.playerUuid(), orderRequest.instrumentId());
        if (holding == null || holding.qty() < orderRequest.qty()) {
            return new TradeResult(false, "Insufficient shares. Available: " +
                    (holding != null ? String.format("%.2f", holding.qty()) : "0"));
        }

        double notionalValue = orderRequest.qty() * executionPrice;
        double netProceeds = notionalValue - fee;

        try {
            // Remove shares from holdings
            if (!QuickStocksPlugin.getHoldingsService().removeHolding(orderRequest.playerUuid(), orderRequest.instrumentId(), orderRequest.qty())) {
                return new TradeResult(false, "Failed to remove shares from portfolio");
            }

            // Add net proceeds to wallet
            QuickStocksPlugin.getWalletService().addBalance(orderRequest.playerUuid(), netProceeds);

            // Record the order
            recordOrder(orderRequest, executionPrice, fee);

            String message = String.format(
                    "%s SELL %.2f shares at $%.2f per share (Gross: $%.2f, Net: $%.2f, Fee: $%.2f)",
                    orderRequest.type(), orderRequest.qty(), executionPrice, notionalValue, netProceeds, fee
            );

            logger.info("Executed sell order for " + orderRequest.playerUuid() + ": " + message);
            return new TradeResult(true, message);

        } catch (SQLException e) {
            // Rollback share removal
            try {
                QuickStocksPlugin.getHoldingsService().addHolding(orderRequest.playerUuid(), orderRequest.instrumentId(),
                        orderRequest.qty(), executionPrice);
            } catch (SQLException rollbackError) {
                logger.severe("Failed to rollback share removal: " + rollbackError.getMessage());
            }
            throw e;
        }
    }

    /**
     * Determines the execution price based on order type.
     */
    private double determineExecutionPrice(OrderRequest orderRequest, double marketPrice) {
        switch (orderRequest.type()) {
            case MARKET:
                return marketPrice;
            case LIMIT:
                // For limit orders, we execute at the limit price if favorable, otherwise don't execute
                if ("BUY".equals(orderRequest.side())) {
                    return marketPrice <= orderRequest.limitPrice() ? marketPrice : -1;
                } else {
                    return marketPrice >= orderRequest.limitPrice() ? marketPrice : -1;
                }
            case STOP:
                // For stop orders, check if stop price is triggered, then execute at market
                if ("BUY".equals(orderRequest.side())) {
                    return marketPrice >= orderRequest.stopPrice() ? marketPrice : -1;
                } else {
                    return marketPrice <= orderRequest.stopPrice() ? marketPrice : -1;
                }
            default:
                return -1;
        }
    }

    /**
     * Records an order in the database.
     */
    private void recordOrder(OrderRequest orderRequest, double executionPrice, double fee) throws SQLException {
        String orderId = UUID.randomUUID().toString();
        database.execute(
                """
                         INSERT INTO orders (id, player_uuid, instrument_id, side, qty, price, ts, order_type,\s
                                            limit_price, stop_price, fee_paid, execution_price)\s
                         VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        \s""",
                orderId, orderRequest.playerUuid(), orderRequest.instrumentId(),
                orderRequest.side(), orderRequest.qty(), executionPrice, System.currentTimeMillis(),
                orderRequest.type().name(), orderRequest.limitPrice(), orderRequest.stopPrice(),
                fee, executionPrice
        );
    }

    /**
     * Checks if an order type is allowed based on configuration.
     */
    private boolean isOrderTypeAllowed(OrderType orderType) {
        return switch (orderType) {
            case MARKET -> tradingConfig.getOrders().isAllowMarket();
            case LIMIT -> tradingConfig.getOrders().isAllowLimit();
            case STOP -> tradingConfig.getOrders().isAllowStop();
        };
    }

    // Legacy methods for backward compatibility
    public TradeResult executeBuyOrder(String playerUuid, String instrumentId, double qty) throws SQLException {
        OrderRequest orderRequest = OrderRequest.marketOrder(playerUuid, instrumentId, "BUY", qty);
        return executeOrder(orderRequest);
    }

    public TradeResult executeSellOrder(String playerUuid, String instrumentId, double qty) throws SQLException {
        OrderRequest orderRequest = OrderRequest.marketOrder(playerUuid, instrumentId, "SELL", qty);
        return executeOrder(orderRequest);
    }

    /**
     * Gets trading history for a player.
     */
    public List<Order> getOrderHistory(String playerUuid, int limit) throws SQLException {
        List<Map<String, Object>> results = database.query(
                """
                        SELECT o.id, o.instrument_id, o.side, o.qty, o.price, o.ts, i.symbol, i.display_name,
                               o.order_type, o.limit_price, o.stop_price, o.fee_paid, o.execution_price
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
                    ((Number) row.get("ts")).longValue(),
                    (String) row.get("order_type"),
                    row.get("limit_price") != null ? ((Number) row.get("limit_price")).doubleValue() : null,
                    row.get("stop_price") != null ? ((Number) row.get("stop_price")).doubleValue() : null,
                    row.get("fee_paid") != null ? ((Number) row.get("fee_paid")).doubleValue() : 0.0,
                    row.get("execution_price") != null ? ((Number) row.get("execution_price")).doubleValue() : ((Number) row.get("price")).doubleValue()
            ));
        }

        return orders;
    }

    /**
     * Enhanced order class with new fields.
     *
     * @param id Getters
     */

    public record Order(String id, String instrumentId, String symbol, String displayName, String side, double qty,
                        double price, long timestamp, String orderType, Double limitPrice, Double stopPrice,
                        double feePaid, double executionPrice) {

        public double getTotalValue() {
            return qty * executionPrice;
        }

        public double getNetValue() {
            return getTotalValue() - feePaid;
        }
    }
}