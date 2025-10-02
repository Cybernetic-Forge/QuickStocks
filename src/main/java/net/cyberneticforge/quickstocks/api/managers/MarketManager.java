package net.cyberneticforge.quickstocks.api.managers;

import net.cyberneticforge.quickstocks.core.services.HoldingsService;
import net.cyberneticforge.quickstocks.core.services.StockMarketService;
import net.cyberneticforge.quickstocks.core.services.InstrumentPersistenceService;
import net.cyberneticforge.quickstocks.core.model.Stock;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * API Manager for market and instrument operations.
 * Provides a high-level interface for external plugins to interact with the market system.
 */
public class MarketManager {
    
    private final StockMarketService stockMarketService;
    private final InstrumentPersistenceService instrumentService;
    private final HoldingsService holdingsService;

    public MarketManager(StockMarketService stockMarketService, InstrumentPersistenceService instrumentService, HoldingsService holdingsService) {
        this.stockMarketService = stockMarketService;
        this.instrumentService = instrumentService;
        this.holdingsService = holdingsService;
    }

    /**
     * Gets an instrument by ID.
     * 
     * @param instrumentId The instrument ID
     * @return Optional containing the instrument data if found
     * @throws SQLException if database error occurs
     */
    public Optional<InstrumentPersistenceService.Instrument> getInstrument(String instrumentId) throws SQLException {
        return instrumentService.getAllInstruments().values().stream().filter(i -> i.getInstrumentId().equals(instrumentId)).findFirst();
    }
    
    /**
     * Gets an instrument by symbol.
     * 
     * @param symbol The instrument symbol
     * @return Optional containing the instrument data if found
     * @throws SQLException if database error occurs
     */
    public Optional<InstrumentPersistenceService.Instrument> getInstrumentBySymbol(String symbol) throws SQLException {
        return Optional.of(instrumentService.getInstrument(symbol));
    }
    
    /**
     * Gets all instruments of a specific type.
     * 
     * @param type The instrument type (ITEM, CRYPTO, EQUITY, etc.)
     * @return List of instruments
     * @throws SQLException if database error occurs
     */
    public List<InstrumentPersistenceService.Instrument> getInstrumentsByType(String type) throws SQLException {
        return instrumentService.getAllInstruments().values().stream().filter(instrument -> instrument.getType().equals(type)).collect(Collectors.toList());
    }
    
    /**
     * Gets all instruments.
     * 
     * @return List of all instruments
     * @throws SQLException if database error occurs
     */
    public List<InstrumentPersistenceService.Instrument> getAllInstruments() {
        return instrumentService.getAllInstruments().values().stream().toList();
    }
    
    /**
     * Gets the current price of an instrument.
     * 
     * @param instrumentId The instrument ID
     * @return Current price or 0.0 if not found
     * @throws SQLException if database error occurs
     */
    public double getCurrentPrice(String playerUuid, String instrumentId) throws SQLException {
        return holdingsService.getHolding(playerUuid, instrumentId).getCurrentPrice();
    }
    
    /**
     * Checks if the market is currently open.
     * 
     * @return true if market is open, false otherwise
     */
    public boolean isMarketOpen() {
        return stockMarketService.isMarketOpen();
    }
    
    /**
     * Opens the market for trading.
     */
    public void openMarket() {
        stockMarketService.setMarketOpen(true);
    }
    
    /**
     * Closes the market for trading.
     */
    public void closeMarket() {
        stockMarketService.setMarketOpen(false);
    }
    
    /**
     * Gets a stock from the market service by symbol.
     * 
     * @param symbol The stock symbol
     * @return Optional containing the stock if found
     */
    public Optional<Stock> getStock(String symbol) {
        return stockMarketService.getStock(symbol);
    }
    
    /**
     * Gets all stocks from the market service.
     * 
     * @return List of all stocks
     */
    public List<Stock> getAllStocks() {
        return stockMarketService.getAllStocks().stream().toList();
    }
    
    /**
     * Gets top performing stocks.
     * 
     * @param limit Maximum number of stocks to return
     * @return List of top performing stocks
     */
    public List<Stock> getTopPerformers(int limit) {
        return stockMarketService.getTopPerformers(limit);
    }
    
    /**
     * Gets worst performing stocks.
     * 
     * @param limit Maximum number of stocks to return
     * @return List of worst performing stocks
     */
    public List<Stock> getWorstPerformers(int limit) {
        return stockMarketService.getWorstPerformers(limit);
    }
}
