package net.cyberneticforge.quickstocks.core.model;

/**
 * Represents a cryptocurrency instrument with its current market state.
 */
public record Crypto(Instrument instrument, InstrumentState state) {

    // Convenience methods
    public String getId() {
        return instrument.id();
    }

    public String getSymbol() {
        return instrument.symbol();
    }

    public String getDisplayName() {
        return instrument.displayName();
    }

    public String getCreatedBy() {
        return instrument.createdBy();
    }

    public long getCreatedAt() {
        return instrument.createdAt();
    }

    public double getCurrentPrice() {
        return state.lastPrice();
    }

    public double getChange24h() {
        return state.change24h();
    }

    public double getVolatility24h() {
        return state.volatility24h();
    }

    public double getMarketCap() {
        return state.marketCap();
    }
}
