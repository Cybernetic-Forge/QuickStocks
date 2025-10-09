package net.cyberneticforge.quickstocks.core.model;

/**
 * Represents the current market state of an instrument.
 */
@SuppressWarnings("unused")
public record InstrumentState(String instrumentId, double lastPrice, double lastVolume, double change1h,
                              double change24h, double volatility24h, double marketCap, long updatedAt) {

}
