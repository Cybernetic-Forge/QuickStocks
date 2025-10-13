package net.cyberneticforge.quickstocks.core.services;

import lombok.Getter;
import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.core.enums.MarketFactor;
import net.cyberneticforge.quickstocks.core.model.MarketInfluence;
import net.cyberneticforge.quickstocks.core.model.Stock;
import net.cyberneticforge.quickstocks.infrastructure.db.Db;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.cyberneticforge.quickstocks.infrastructure.logging.PluginLogger;
import java.util.stream.Collectors;

/**
 * Manages the simulation engine that runs market updates every 5 seconds
 * and persists the results to the database.
 */
@SuppressWarnings("unused")
public class SimulationEngine {
    
    private static final PluginLogger logger = QuickStocksPlugin.getPluginLogger();
    private static final int TICK_INTERVAL_SECONDS = 5;

    private final Db database = QuickStocksPlugin.getDatabaseManager().getDb();
    /**
     * -- GETTER --
     *  Get the analytics service for direct access to advanced analytics.
     */
    @Getter
    private final AnalyticsService analyticsService = QuickStocksPlugin.getAnalyticsService();
    private final ScheduledExecutorService scheduler;
    private volatile boolean running = false;
    
    public SimulationEngine() {
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
        
        // Schedule periodic reset of trading activity (every 60 seconds)
        scheduler.scheduleAtFixedRate(this::resetTradingActivity, 60, 60, TimeUnit.SECONDS);
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
            logger.debug("Executing simulation tick");
            
            // Update market prices (this applies all market factors)
            QuickStocksPlugin.getStockMarketService().updateAllStockPrices();
            
            // Get the affected factors that contributed to this tick
            List<MarketFactor> contributingFactors = getContributingFactors();
            
            // Persist all updated stocks to database in a single transaction
            persistMarketState(contributingFactors);
            
            logger.debug("Simulation tick completed successfully");
            
        } catch (Exception ex) {
            logger.severe("Error during simulation tick: " + ex.getMessage());
            QuickStocksPlugin.getInstance().getLogger().severe("StackTrace: " + Arrays.toString(ex.getStackTrace()));

            // Continue running even if one tick fails
        }
    }
    
    /**
     * Determines which market factors contributed significantly to price changes in this tick.
     */
    private List<MarketFactor> getContributingFactors() {
        return QuickStocksPlugin.getStockMarketService().getMarketInfluences().stream()
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
            for (Stock stock : QuickStocksPlugin.getStockMarketService().getAllStocks()) {
                try {
                    String instrumentId = QuickStocksPlugin.getInstrumentPersistenceService().ensureInstrument(stock).id();
                    stockToInstrumentMap.put(stock, instrumentId);
                } catch (Exception e) {
                    logger.severe("Failed to ensure instrument for " + stock.getSymbol() + ": " + e.getMessage());
                    throw new RuntimeException("Failed to ensure instrument for " + stock.getSymbol(), e);
                }
            }
            
            // Now execute the main transaction for state and history updates
            database.executeTransaction(db -> {
                long currentTime = System.currentTimeMillis();
                
                for (Stock stock : QuickStocksPlugin.getStockMarketService().getAllStocks()) {
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
                            change1h = getChangePercent(instrumentId, 60);
                            change24h = getChangePercent(instrumentId, 1440);
                            volatility24h = getVolatility(instrumentId, 1440);
                        } catch (Exception e) {
                            logger.warning("Failed to calculate rolling window metrics for " + stock.getSymbol() + ": " + e.getMessage());
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
            
            logger.debug("Persisted market state for " + QuickStocksPlugin.getStockMarketService().getAllStocks().size() + " instruments");
            
        } catch (Exception ex) {
            logger.severe("Failed to persist market state: " + ex.getMessage());
            QuickStocksPlugin.getInstance().getLogger().severe("StackTrace: " + Arrays.toString(ex.getStackTrace()));
        }
    }
    
    /**
     * UPSERT operation for instrument_state table.
     */
    private void upsertInstrumentState(Db.TransactionDb db, String instrumentId, Stock stock, double change1h, 
                                     double change24h, double volatility24h, long timestamp) throws Exception {
        
        // Use INSERT OR REPLACE for SQLite, or ON DUPLICATE KEY UPDATE for MySQL
        db.execute("""
            INSERT OR REPLACE INTO instrument_state\s
            (instrument_id, last_price, last_volume, change_1h, change_24h, volatility_24h, market_cap, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
           \s""",
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
            INSERT INTO instrument_price_history\s
            (id, instrument_id, ts, price, volume, reason)
            VALUES (?, ?, ?, ?, ?, ?)
           \s""",
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
     * Delegates to AnalyticsService.
     * @param instrumentId The instrument identifier
     * @param windowMinutes Time window in minutes
     * @return Change percentage (-1.0 to +1.0)
     */
    public double getChangePercent(String instrumentId, int windowMinutes) {
        return analyticsService.getChangePct(instrumentId, windowMinutes);
    }
    
    /**
     * Calculates volatility over a given time window.
     * Delegates to AnalyticsService.
     * @param instrumentId The instrument identifier
     * @param windowMinutes Time window in minutes
     * @return Volatility as standard deviation of price changes
     */
    public double getVolatility(String instrumentId, int windowMinutes) {
        return analyticsService.getVolatilityEWMA(instrumentId, windowMinutes);
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
        return QuickStocksPlugin.getInstrumentPersistenceService().getAllInstruments().entrySet().stream()
            .filter(entry -> entry.getValue().id().equals(instrumentId))
            .map(Map.Entry::getKey)
            .findFirst()
            .orElse(null);
    }

    /**
     * Records portfolio values for all players (for Sharpe ratio calculations).
     * This should be called periodically, e.g., once per hour or day.
     * Note: This is a placeholder - real implementation would query actual player portfolios.
     */
    public void recordPortfolioSnapshots() {
        try {
            logger.debug("Recording portfolio snapshots for Sharpe ratio calculations");
            
            // TODO: In real implementation, query actual player portfolios from holdings/wallet data
            // For now, this is a placeholder that shows the integration pattern
            
            // Example of how this would work:
            // 1. Query all active players
            // 2. Calculate their current portfolio value (cash + holdings)
            // 3. Record the snapshot using analyticsService.recordPortfolioValue()
            
            logger.debug("Portfolio snapshot recording placeholder - integrate with actual portfolio service");
            
        } catch (Exception e) {
            logger.warning("Failed to record portfolio snapshots: " + e.getMessage());
        }
    }
    
    /**
     * Resets trading activity counters for threshold calculations.
     * Called periodically to track recent trading activity.
     */
    private void resetTradingActivity() {
        try {
            if (QuickStocksPlugin.getStockMarketService().getThresholdController() != null) {
                QuickStocksPlugin.getStockMarketService().getThresholdController().resetTradingActivity();
                logger.debug("Reset trading activity counters");
            }
        } catch (Exception e) {
            logger.warning("Error resetting trading activity: " + e.getMessage());
        }
    }
}