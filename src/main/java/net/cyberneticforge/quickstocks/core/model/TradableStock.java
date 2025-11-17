package net.cyberneticforge.quickstocks.core.model;

/**
 * Unified model representing any tradable asset in the QuickStocks system.
 * This combines the Instrument metadata with its current market state.
 * 
 * All tradable assets (items, crypto, company shares) are represented as Instruments in the database.
 * This class provides a convenient wrapper with price history and market data.
 */
public class TradableStock {
    private final Instrument instrument;
    private final InstrumentState state;
    
    public TradableStock(Instrument instrument, InstrumentState state) {
        this.instrument = instrument;
        this.state = state;
    }
    
    // Instrument properties
    public String getId() { return instrument.id(); }
    public String getType() { return instrument.type(); }
    public String getSymbol() { return instrument.symbol(); }
    public String getDisplayName() { return instrument.displayName(); }
    public String getMcMaterial() { return instrument.mcMaterial(); }
    public int getDecimals() { return instrument.decimals(); }
    public String getCreatedBy() { return instrument.createdBy(); }
    public long getCreatedAt() { return instrument.createdAt(); }
    
    // State properties
    public double getCurrentPrice() { return state != null ? state.lastPrice() : 0.0; }
    public double getLastVolume() { return state != null ? state.lastVolume() : 0.0; }
    public double getChange1h() { return state != null ? state.change1h() : 0.0; }
    public double getChange24h() { return state != null ? state.change24h() : 0.0; }
    public double getVolatility24h() { return state != null ? state.volatility24h() : 0.0; }
    public double getMarketCap() { return state != null ? state.marketCap() : 0.0; }
    public long getUpdatedAt() { return state != null ? state.updatedAt() : 0; }
    
    // Convenience methods
    public boolean isItem() { return "ITEM".equals(instrument.type()); }
    public boolean isCrypto() { return "CRYPTO".equals(instrument.type()) || "CUSTOM_CRYPTO".equals(instrument.type()); }
    public boolean isEquity() { return "EQUITY".equals(instrument.type()); }
    
    public Instrument getInstrument() { return instrument; }
    public InstrumentState getState() { return state; }
    
    @Override
    public String toString() {
        return String.format("TradableStock{%s (%s): $%.2f}", 
            instrument.symbol(), instrument.type(), getCurrentPrice());
    }
}
