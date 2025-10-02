package net.cyberneticforge.quickstocks.core.model;

/**
 * Represents the current market state of an instrument.
 */
public class InstrumentState {
    private final String instrumentId;
    private final double lastPrice;
    private final double lastVolume;
    private final double change1h;
    private final double change24h;
    private final double volatility24h;
    private final double marketCap;
    private final long updatedAt;

    public InstrumentState(String instrumentId, double lastPrice, double lastVolume,
                          double change1h, double change24h, double volatility24h,
                          double marketCap, long updatedAt) {
        this.instrumentId = instrumentId;
        this.lastPrice = lastPrice;
        this.lastVolume = lastVolume;
        this.change1h = change1h;
        this.change24h = change24h;
        this.volatility24h = volatility24h;
        this.marketCap = marketCap;
        this.updatedAt = updatedAt;
    }

    public String getInstrumentId() {
        return instrumentId;
    }

    public double getLastPrice() {
        return lastPrice;
    }

    public double getLastVolume() {
        return lastVolume;
    }

    public double getChange1h() {
        return change1h;
    }

    public double getChange24h() {
        return change24h;
    }

    public double getVolatility24h() {
        return volatility24h;
    }

    public double getMarketCap() {
        return marketCap;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }
}
