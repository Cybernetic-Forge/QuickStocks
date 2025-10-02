package net.cyberneticforge.quickstocks.core.model;

/**
 * Represents a cryptocurrency instrument with its current market state.
 */
public class Crypto {
    private final Instrument instrument;
    private final InstrumentState state;

    public Crypto(Instrument instrument, InstrumentState state) {
        this.instrument = instrument;
        this.state = state;
    }

    public Instrument getInstrument() {
        return instrument;
    }

    public InstrumentState getState() {
        return state;
    }

    // Convenience methods
    public String getId() {
        return instrument.getId();
    }

    public String getSymbol() {
        return instrument.getSymbol();
    }

    public String getDisplayName() {
        return instrument.getDisplayName();
    }

    public String getCreatedBy() {
        return instrument.getCreatedBy();
    }

    public long getCreatedAt() {
        return instrument.getCreatedAt();
    }

    public double getCurrentPrice() {
        return state.getLastPrice();
    }

    public double getChange24h() {
        return state.getChange24h();
    }

    public double getVolatility24h() {
        return state.getVolatility24h();
    }

    public double getMarketCap() {
        return state.getMarketCap();
    }
}
