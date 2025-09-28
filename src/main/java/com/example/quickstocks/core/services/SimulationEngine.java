package com.example.quickstocks.core.services;

import com.example.quickstocks.core.enums.MarketFactor;
import com.example.quickstocks.core.models.MarketInfluence;
import com.example.quickstocks.core.models.Stock;
import com.example.quickstocks.infrastructure.db.Db;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Manages the simulation engine that runs market updates every 5 seconds
 * and persists the results to the database.
 */
public class SimulationEngine {
    
    private static final Logger logger = Logger.getLogger(SimulationEngine.class.getName());
    private static final int TICK_INTERVAL_SECONDS = 5;
    
    private final StockMarketService marketService;
    private final Db database;
    private final InstrumentPersistenceService instrumentService;
    private final AnalyticsService analyticsService;
    private final ScheduledExecutorService scheduler;
    private volatile boolean running = false;
    
    public SimulationEngine(StockMarketService marketService, Db database) {
        this.marketService = Objects.requireNonNull(marketService, "StockMarketService cannot be null");
        this.database = Objects.requireNonNull(database, "Database cannot be null");
        this.instrumentService = new InstrumentPersistenceService(database);
        // Initialize AnalyticsService with default configuration
        this.analyticsService = new AnalyticsService(database, 0.94, 1440, 1440, 1440);
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "SimulationEngine-Ticker");
            t.setDaemon(true);
            return t;
        });
    }
    
    /**
     * Constructor with custom analytics configuration.
     */
    public SimulationEngine(StockMarketService marketService, Db database, AnalyticsService analyticsService) {
        this.marketService = Objects.requireNonNull(marketService, "StockMarketService cannot be null");
        this.database = Objects.requireNonNull(database, "Database cannot be null");
        this.instrumentService = new InstrumentPersistenceService(database);
        this.analyticsService = Objects.requireNonNull(analyticsService, "AnalyticsService cannot be null");
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "SimulationEngine-Ticker");
            t.setDaemon(true);
            return t;
        });
    }
    
    /**
     * Starts the simulation engine with periodic ticks.
     */
    public void start() {
        if (running) {
            logger.warning("Simulation engine is already running");
            return;
        }
        
        running = true;
        logger.info("Starting simulation engine with " + TICK_INTERVAL_SECONDS + "s tick interval");
        
        // Load existing state from database
        loadMarketStateFromDatabase();
        
        // Schedule periodic ticks
        scheduler.scheduleAtFixedRate(this::tick, 0, TICK_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }
    
    /**
     * Stops the simulation engine.
     */
    public void stop() {
        if (!running) {
            return;
        }
        
        running = false;
        logger.info("Stopping simulation engine");
        scheduler.shutdown();
        
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Executes a single simulation tick.
     */
    private void tick() {
        try {
            logger.fine("Executing simulation tick");
            
            // Update market prices (this applies all market factors)
            marketService.updateAllStockPrices();
            
            // Get the affected factors that contributed to this tick
            List<MarketFactor> contributingFactors = getContributingFactors();
            
            // Persist all updated stocks to database in a single transaction
            persistMarketState(contributingFactors);
            
            logger.fine("Simulation tick completed successfully");
            
        } catch (Exception e) {
            logger.severe("Error during simulation tick: " + e.getMessage());
            e.printStackTrace();
            // Continue running even if one tick fails
        }
    }
    
    /**
     * Determines which market factors contributed significantly to price changes in this tick.
     */
    private List<MarketFactor> getContributingFactors() {
        return marketService.getMarketInfluences().stream()
            .filter(influence -> Math.abs(influence.calculateImpact()) > 0.01) // 1% threshold
            .map(MarketInfluence::getFactor)
            .collect(Collectors.toList());
    }
    
    /**
     * Persists the current market state to the database.
     */
    private void persistMarketState(List<MarketFactor> contributingFactors) {
        String factorReason = contributingFactors.stream()
            .map(MarketFactor::name)
            .collect(Collectors.joining(","));
        
        if (factorReason.isEmpty()) {
            factorReason = "RANDOM_FLUCTUATION";
        }
        
        final String finalFactorReason = factorReason; // Make effectively final for lambda
        
        try {
            // First, ensure all stocks exist as instruments (do this outside the main transaction)
            Map<Stock, String> stockToInstrumentMap = new HashMap<>();
            for (Stock stock : marketService.getAllStocks()) {
                try {
                    String instrumentId = instrumentService.ensureInstrument(stock);
                    stockToInstrumentMap.put(stock, instrumentId);
                } catch (Exception e) {
                    logger.severe("Failed to ensure instrument for " + stock.getSymbol() + ": " + e.getMessage());
                    throw new RuntimeException("Failed to ensure instrument for " + stock.getSymbol(), e);
                }
            }
            
            // Now execute the main transaction for state and history updates
            database.executeTransaction(db -> {
                long currentTime = System.currentTimeMillis();
                
                for (Stock stock : marketService.getAllStocks()) {
                    try {
                        String instrumentId = stockToInstrumentMap.get(stock);
                        if (instrumentId == null) {
                            throw new RuntimeException("No instrument ID found for " + stock.getSymbol());
                        }
                        
                        // Calculate derived fields (with fallback for errors)
                        double change1h = 0.0;
                        double change24h = 0.0;
                        double volatility24h = 0.0;
                        
                        try {
                            change1h = analyticsService.getChangePct(instrumentId, 60);
                            change24h = analyticsService.getChangePct(instrumentId, 1440);
                            volatility24h = analyticsService.getVolatilityEWMA(instrumentId, 1440);
                        } catch (Exception e) {
                            logger.warning("Failed to calculate rolling window metrics for " + stock.getSymbol() + ": " + e.getMessage());
                            // Continue with zero values
                        }
                        
                        // UPSERT instrument_state
                        upsertInstrumentState(db, instrumentId, stock, change1h, change24h, volatility24h, currentTime);
                        
                        // INSERT instrument_price_history
                        insertPriceHistory(db, instrumentId, stock, finalFactorReason, currentTime);
                        
                    } catch (Exception e) {
                        logger.severe("Failed to persist stock " + stock.getSymbol() + ": " + e.getMessage());
                        throw new RuntimeException("Failed to persist stock " + stock.getSymbol(), e);
                    }
                }
            });
            
            logger.fine("Persisted market state for " + marketService.getAllStocks().size() + " instruments");
            
        } catch (Exception e) {
            logger.severe("Failed to persist market state: " + e.getMessage());
            e.printStackTrace();
            // Don't re-throw - allow the simulation to continue
        }
    }
    
    /**
     * UPSERT operation for instrument_state table.
     */
    private void upsertInstrumentState(Db.TransactionDb db, String instrumentId, Stock stock, double change1h, 
                                     double change24h, double volatility24h, long timestamp) throws Exception {
        
        // Use INSERT OR REPLACE for SQLite, or ON DUPLICATE KEY UPDATE for MySQL
        db.execute("""
            INSERT OR REPLACE INTO instrument_state 
            (instrument_id, last_price, last_volume, change_1h, change_24h, volatility_24h, market_cap, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """, 
            instrumentId,
            stock.getCurrentPrice(),
            stock.getDailyVolume(),
            change1h,
            change24h,
            volatility24h,
            stock.getMarketCap(),
            timestamp
        );
    }
    
    /**
     * INSERT operation for instrument_price_history table.
     */
    private void insertPriceHistory(Db.TransactionDb db, String instrumentId, Stock stock, String reason, long timestamp) throws Exception {
        db.execute("""
            INSERT INTO instrument_price_history 
            (id, instrument_id, ts, price, volume, reason)
            VALUES (?, ?, ?, ?, ?, ?)
            """,
            UUID.randomUUID().toString(),
            instrumentId,
            timestamp,
            stock.getCurrentPrice(),
            stock.getDailyVolume(),
            reason
        );
    }
    
    /**
     * Loads the current market state from the database on startup.
     */
    private void loadMarketStateFromDatabase() {
        try {
            logger.info("Loading market state from database");
            
            var instruments = database.query("SELECT * FROM instruments");
            var states = database.query("SELECT * FROM instrument_state");
            
            logger.info("Found " + instruments.size() + " instruments and " + states.size() + " states in database");
            
            // For now, we'll use the existing in-memory stocks
            // In a full implementation, we'd reconstruct Stock objects from the database
            
        } catch (Exception e) {
            logger.warning("Failed to load market state from database: " + e.getMessage());
            // Continue with empty state - this is acceptable for the simulation
        }
    }
    
    /**
     * Calculates price change percentage over a given time window.
     * @param instrumentId The instrument identifier
     * @param windowMinutes Time window in minutes
     * @return Change percentage (-1.0 to +1.0)
     */
    public double getChangePercent(String instrumentId, int windowMinutes) {
        try {
            long windowStart = System.currentTimeMillis() - (windowMinutes * 60 * 1000L);
            
            var results = database.query("""
                SELECT price, ts FROM instrument_price_history 
                WHERE instrument_id = ? AND ts >= ?
                ORDER BY ts ASC
                LIMIT 1
                """, instrumentId, windowStart);
            
            if (results.isEmpty()) {
                return 0.0;
            }
            
            double oldPrice = ((Number) results.get(0).get("price")).doubleValue();
            
            // Get current price by looking up the stock by symbol
            String symbol = getSymbolForInstrumentId(instrumentId);
            if (symbol == null) {
                return 0.0;
            }
            
            Optional<Stock> stock = marketService.getStock(symbol);
            if (stock.isEmpty()) {
                return 0.0;
            }
            
            double currentPrice = stock.get().getCurrentPrice();
            
            return oldPrice > 0 ? (currentPrice - oldPrice) / oldPrice : 0.0;
            
        } catch (Exception e) {
            logger.warning("Failed to calculate change percent for " + instrumentId + ": " + e.getMessage());
            return 0.0;
        }
    }
    
    /**
     * Calculates volatility over a given time window.
     * @param instrumentId The instrument identifier
     * @param windowMinutes Time window in minutes
     * @return Volatility as standard deviation of price changes
     */
    public double getVolatility(String instrumentId, int windowMinutes) {
        try {
            long windowStart = System.currentTimeMillis() - (windowMinutes * 60 * 1000L);
            
            var results = database.query("""
                SELECT price FROM instrument_price_history 
                WHERE instrument_id = ? AND ts >= ?
                ORDER BY ts ASC
                """, instrumentId, windowStart);
            
            if (results.size() < 2) {
                return 0.0;
            }
            
            // Calculate price changes
            List<Double> changes = new ArrayList<>();
            for (int i = 1; i < results.size(); i++) {
                double prevPrice = ((Number) results.get(i-1).get("price")).doubleValue();
                double currPrice = ((Number) results.get(i).get("price")).doubleValue();
                
                if (prevPrice > 0) {
                    changes.add((currPrice - prevPrice) / prevPrice);
                }
            }
            
            if (changes.isEmpty()) {
                return 0.0;
            }
            
            // Calculate standard deviation
            double mean = changes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            double variance = changes.stream()
                .mapToDouble(change -> Math.pow(change - mean, 2))
                .average().orElse(0.0);
            
            return Math.sqrt(variance);
            
        } catch (Exception e) {
            logger.warning("Failed to calculate volatility for " + instrumentId + ": " + e.getMessage());
            return 0.0;
        }
    }
    
    public boolean isRunning() {
        return running;
    }
    
    /**
     * Manually execute a single tick (for testing).
     */
    public void manualTick() {
        tick();
    }
    
    /**
     * Helper method to get symbol from instrument ID.
     */
    private String getSymbolForInstrumentId(String instrumentId) {
        return instrumentService.getAllMappings().entrySet().stream()
            .filter(entry -> entry.getValue().equals(instrumentId))
            .map(Map.Entry::getKey)
            .findFirst()
            .orElse(null);
    }
    
    // Analytics methods delegation
    
    /**
     * Gets price change percentage over the default time window.
     */
    public double getChangePct(String instrumentId) {
        return analyticsService.getChangePct(instrumentId, analyticsService.getDefaultChangeWindow());
    }
    
    /**
     * Gets price change percentage over a given time window.
     */
    public double getChangePct(String instrumentId, int windowMinutes) {
        return analyticsService.getChangePct(instrumentId, windowMinutes);
    }
    
    /**
     * Gets EWMA volatility using default parameters.
     */
    public double getVolatilityEWMA(String instrumentId) {
        return analyticsService.getVolatilityEWMA(instrumentId, analyticsService.getDefaultVolatilityWindow());
    }
    
    /**
     * Gets EWMA volatility over a given time window.
     */
    public double getVolatilityEWMA(String instrumentId, int windowMinutes) {
        return analyticsService.getVolatilityEWMA(instrumentId, windowMinutes);
    }
    
    /**
     * Gets EWMA volatility with custom lambda.
     */
    public double getVolatilityEWMA(String instrumentId, int windowMinutes, double lambda) {
        return analyticsService.getVolatilityEWMA(instrumentId, windowMinutes, lambda);
    }
    
    /**
     * Gets correlation between two instruments.
     */
    public double getCorrelation(String instrumentA, String instrumentB, int windowMinutes) {
        return analyticsService.getCorrelation(instrumentA, instrumentB, windowMinutes);
    }
    
    /**
     * Gets Sharpe ratio for a player's portfolio.
     */
    public double getSharpe(String playerUuid, int windowDays, double riskFree) {
        return analyticsService.getSharpe(playerUuid, windowDays, riskFree);
    }
    
    /**
     * Gets Sharpe ratio with default risk-free rate (0).
     */
    public double getSharpe(String playerUuid, int windowDays) {
        return getSharpe(playerUuid, windowDays, 0.0);
    }
    
    /**
     * Get the analytics service for direct access.
     */
    public AnalyticsService getAnalyticsService() {
        return analyticsService;
    }
    
    /**
     * Records portfolio values for all players (for Sharpe ratio calculations).
     * This should be called periodically, e.g., once per hour or day.
     * Note: This is a placeholder - real implementation would query actual player portfolios.
     */
    public void recordPortfolioSnapshots() {
        try {
            logger.fine("Recording portfolio snapshots for Sharpe ratio calculations");
            
            // TODO: In real implementation, query actual player portfolios from holdings/wallet data
            // For now, this is a placeholder that shows the integration pattern
            
            // Example of how this would work:
            // 1. Query all active players
            // 2. Calculate their current portfolio value (cash + holdings)
            // 3. Record the snapshot using analyticsService.recordPortfolioValue()
            
            logger.fine("Portfolio snapshot recording placeholder - integrate with actual portfolio service");
            
        } catch (Exception e) {
            logger.warning("Failed to record portfolio snapshots: " + e.getMessage());
        }
    }
}