package net.cyberneticforge.quickstocks.api.managers;

import net.cyberneticforge.quickstocks.core.model.Instrument;
import net.cyberneticforge.quickstocks.core.model.InstrumentState;
import net.cyberneticforge.quickstocks.core.model.PriceHistory;
import net.cyberneticforge.quickstocks.core.model.Stock;
import net.cyberneticforge.quickstocks.core.services.InstrumentPersistenceService;
import net.cyberneticforge.quickstocks.core.services.StockMarketService;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * API Manager for market and instrument operations.
 * Provides a high-level interface for external plugins to interact with the market system.
 */
@SuppressWarnings("unused")
public class MarketManager {
    
    private final StockMarketService stockMarketService;
    private final InstrumentPersistenceService instrumentService;

    public MarketManager(StockMarketService stockMarketService, InstrumentPersistenceService instrumentService) {
        this.stockMarketService = stockMarketService;
        this.instrumentService = instrumentService;
    }

    /**
     * Gets an instrument by ID.
     * 
     * @param instrumentId The instrument ID
     * @return Optional containing the instrument data if found
     * @throws SQLException if database error occurs
     */
    public Optional<Instrument> getInstrument(String instrumentId) throws SQLException {
        return instrumentService.getInstrumentById(instrumentId);
    }
    
    /**
     * Gets an instrument by symbol.
     * 
     * @param symbol The instrument symbol
     * @return Optional containing the instrument data if found
     * @throws SQLException if database error occurs
     */
    public Optional<Instrument> getInstrumentBySymbol(String symbol) throws SQLException {
        return instrumentService.getInstrumentBySymbol(symbol);
    }
    
    /**
     * Gets all instruments of a specific type.
     * 
     * @param type The instrument type (ITEM, CRYPTO, EQUITY, etc.)
     * @return List of instruments
     * @throws SQLException if database error occurs
     */
    public List<Instrument> getInstrumentsByType(String type) throws SQLException {
        return instrumentService.getInstrumentsByType(type);
    }
    
    /**
     * Gets all instruments.
     * 
     * @return List of all instruments
     */
    public List<Instrument> getAllInstruments() {
        return instrumentService.getAllInstruments().values().stream().toList();
    }
    
    /**
     * Gets the current price of an instrument.
     * 
     * @param instrumentId The instrument ID
     * @return Current price or 0.0 if not found
     * @throws SQLException if database error occurs
     */
    public double getCurrentPrice(String instrumentId) throws SQLException {
        Optional<InstrumentState> state = instrumentService.getInstrumentState(instrumentId);
        return state.map(InstrumentState::lastPrice).orElse(0.0);
    }
    
    /**
     * Gets the current state of an instrument (price, volume, changes, etc.).
     * 
     * @param instrumentId The instrument ID
     * @return Optional containing the instrument state if found
     * @throws SQLException if database error occurs
     */
    public Optional<InstrumentState> getInstrumentState(String instrumentId) throws SQLException {
        return instrumentService.getInstrumentState(instrumentId);
    }
    
    /**
     * Gets price history for an instrument.
     * 
     * @param instrumentId The instrument ID
     * @param limit Maximum number of history entries to return
     * @return List of price history entries
     * @throws SQLException if database error occurs
     */
    public List<PriceHistory> getPriceHistory(String instrumentId, int limit) throws SQLException {
        return instrumentService.getPriceHistory(instrumentId, limit);
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
