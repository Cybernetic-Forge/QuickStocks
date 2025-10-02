package net.cyberneticforge.quickstocks.core.services;

import net.cyberneticforge.quickstocks.infrastructure.config.TradingConfig;
import net.cyberneticforge.quickstocks.core.model.OrderRequest;
import net.cyberneticforge.quickstocks.core.model.OrderType;
import net.cyberneticforge.quickstocks.infrastructure.db.Db;
import net.cyberneticforge.quickstocks.core.services.TradingService.TradeResult;

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
    private final WalletService walletService;
    private final HoldingsService holdingsService;
    private final FeeService feeService;
    private final SlippageService slippageService;
    private final RateLimitService rateLimitService;
    private final CircuitBreakerService circuitBreakerService;
    private final TradingConfig tradingConfig;
    
    public EnhancedTradingService(Db database, WalletService walletService, HoldingsService holdingsService,
                                 TradingConfig tradingConfig) {
        this.database = database;
        this.walletService = walletService;
        this.holdingsService = holdingsService;
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
    public TradeResult executeOrder(OrderRequest orderRequest) throws SQLException {
        try {
            // Validate the order request
            orderRequest.validate();
            
            // Check if order type is allowed
            if (!isOrderTypeAllowed(orderRequest.getType())) {
                return new TradeResult(false, "Order type " + orderRequest.getType() + " is not allowed");
            }
            
            // Check if trading is halted
            if (circuitBreakerService.isTradingHalted(orderRequest.getInstrumentId())) {
                CircuitBreakerService.HaltInfo haltInfo = circuitBreakerService.getHaltInfo(orderRequest.getInstrumentId());
                return new TradeResult(false, "Trading is halted due to circuit breaker (Level " + 
                    haltInfo.getLevel() + ")");
            }
            
            // Get current market price
            Double marketPrice = database.queryValue(
                "SELECT last_price FROM instrument_state WHERE instrument_id = ?",
                orderRequest.getInstrumentId()
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
                executionPrice, orderRequest.getQty(), orderRequest.getSide()
            );
            
            // Calculate notional value and fees
            double notionalValue = orderRequest.getQty() * finalExecutionPrice;
            double fee = feeService.calculateFee(notionalValue);
            
            // Check rate limits
            RateLimitService.ValidationResult rateLimitResult = rateLimitService.validateTrade(
                orderRequest.getPlayerUuid(), orderRequest.getQty(), notionalValue
            );
            if (!rateLimitResult.isAllowed()) {
                return new TradeResult(false, rateLimitResult.getMessage());
            }
            
            // Execute based on order side
            TradeResult result;
            if ("BUY".equals(orderRequest.getSide())) {
                result = executeBuyOrder(orderRequest, finalExecutionPrice, fee);
            } else {
                result = executeSellOrder(orderRequest, finalExecutionPrice, fee);
            }
            
            // If successful, record for rate limiting and check circuit breakers
            if (result.isSuccess()) {
                rateLimitService.recordTrade(orderRequest.getPlayerUuid(), notionalValue);
                circuitBreakerService.checkAndTriggerCircuitBreaker(orderRequest.getInstrumentId(), finalExecutionPrice);
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
        double notionalValue = orderRequest.getQty() * executionPrice;
        double totalCost = notionalValue + fee;
        
        // Check wallet balance
        if (!walletService.hasBalance(orderRequest.getPlayerUuid(), totalCost)) {
            return new TradeResult(false, String.format(
                "Insufficient funds. Required: $%.2f (including $%.2f fee)", totalCost, fee
            ));
        }
        
        try {
            // Remove total cost from wallet
            if (!walletService.removeBalance(orderRequest.getPlayerUuid(), totalCost)) {
                return new TradeResult(false, "Failed to debit wallet");
            }
            
            // Add shares to holdings
            holdingsService.addHolding(orderRequest.getPlayerUuid(), orderRequest.getInstrumentId(), 
                                     orderRequest.getQty(), executionPrice);
            
            // Record the order
            recordOrder(orderRequest, executionPrice, fee);
            
            String message = String.format(
                "%s BUY %.2f shares at $%.2f per share (Total: $%.2f, Fee: $%.2f)", 
                orderRequest.getType(), orderRequest.getQty(), executionPrice, notionalValue, fee
            );
            
            logger.info("Executed buy order for " + orderRequest.getPlayerUuid() + ": " + message);
            return new TradeResult(true, message);
            
        } catch (SQLException e) {
            // Rollback wallet debit
            try {
                walletService.addBalance(orderRequest.getPlayerUuid(), totalCost);
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
        HoldingsService.Holding holding = holdingsService.getHolding(orderRequest.getPlayerUuid(), orderRequest.getInstrumentId());
        if (holding == null || holding.getQty() < orderRequest.getQty()) {
            return new TradeResult(false, "Insufficient shares. Available: " + 
                (holding != null ? String.format("%.2f", holding.getQty()) : "0"));
        }
        
        double notionalValue = orderRequest.getQty() * executionPrice;
        double netProceeds = notionalValue - fee;
        
        try {
            // Remove shares from holdings
            if (!holdingsService.removeHolding(orderRequest.getPlayerUuid(), orderRequest.getInstrumentId(), orderRequest.getQty())) {
                return new TradeResult(false, "Failed to remove shares from portfolio");
            }
            
            // Add net proceeds to wallet
            walletService.addBalance(orderRequest.getPlayerUuid(), netProceeds);
            
            // Record the order
            recordOrder(orderRequest, executionPrice, fee);
            
            String message = String.format(
                "%s SELL %.2f shares at $%.2f per share (Gross: $%.2f, Net: $%.2f, Fee: $%.2f)", 
                orderRequest.getType(), orderRequest.getQty(), executionPrice, notionalValue, netProceeds, fee
            );
            
            logger.info("Executed sell order for " + orderRequest.getPlayerUuid() + ": " + message);
            return new TradeResult(true, message);
            
        } catch (SQLException e) {
            // Rollback share removal
            try {
                holdingsService.addHolding(orderRequest.getPlayerUuid(), orderRequest.getInstrumentId(), 
                                         orderRequest.getQty(), executionPrice);
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
        switch (orderRequest.getType()) {
            case MARKET:
                return marketPrice;
            case LIMIT:
                // For limit orders, we execute at the limit price if favorable, otherwise don't execute
                if ("BUY".equals(orderRequest.getSide())) {
                    return marketPrice <= orderRequest.getLimitPrice() ? marketPrice : -1;
                } else {
                    return marketPrice >= orderRequest.getLimitPrice() ? marketPrice : -1;
                }
            case STOP:
                // For stop orders, check if stop price is triggered, then execute at market
                if ("BUY".equals(orderRequest.getSide())) {
                    return marketPrice >= orderRequest.getStopPrice() ? marketPrice : -1;
                } else {
                    return marketPrice <= orderRequest.getStopPrice() ? marketPrice : -1;
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
            INSERT INTO orders (id, player_uuid, instrument_id, side, qty, price, ts, order_type, 
                               limit_price, stop_price, fee_paid, execution_price) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            orderId, orderRequest.getPlayerUuid(), orderRequest.getInstrumentId(), 
            orderRequest.getSide(), orderRequest.getQty(), executionPrice, System.currentTimeMillis(),
            orderRequest.getType().name(), orderRequest.getLimitPrice(), orderRequest.getStopPrice(),
            fee, executionPrice
        );
    }
    
    /**
     * Checks if an order type is allowed based on configuration.
     */
    private boolean isOrderTypeAllowed(OrderType orderType) {
        switch (orderType) {
            case MARKET:
                return tradingConfig.getOrders().isAllowMarket();
            case LIMIT:
                return tradingConfig.getOrders().isAllowLimit();
            case STOP:
                return tradingConfig.getOrders().isAllowStop();
            default:
                return false;
        }
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
        private final String orderType;
        private final Double limitPrice;
        private final Double stopPrice;
        private final double feePaid;
        private final double executionPrice;
        
        public Order(String id, String instrumentId, String symbol, String displayName,
                    String side, double qty, double price, long timestamp, String orderType,
                    Double limitPrice, Double stopPrice, double feePaid, double executionPrice) {
            this.id = id;
            this.instrumentId = instrumentId;
            this.symbol = symbol;
            this.displayName = displayName;
            this.side = side;
            this.qty = qty;
            this.price = price;
            this.timestamp = timestamp;
            this.orderType = orderType;
            this.limitPrice = limitPrice;
            this.stopPrice = stopPrice;
            this.feePaid = feePaid;
            this.executionPrice = executionPrice;
        }
        
        // Getters
        public String getId() { return id; }
        public String getInstrumentId() { return instrumentId; }
        public String getSymbol() { return symbol; }
        public String getDisplayName() { return displayName; }
        public String getSide() { return side; }
        public double getQty() { return qty; }
        public double getPrice() { return price; }
        public long getTimestamp() { return timestamp; }
        public String getOrderType() { return orderType; }
        public Double getLimitPrice() { return limitPrice; }
        public Double getStopPrice() { return stopPrice; }
        public double getFeePaid() { return feePaid; }
        public double getExecutionPrice() { return executionPrice; }
        public double getTotalValue() { return qty * executionPrice; }
        public double getNetValue() { return getTotalValue() - feePaid; }
    }
}