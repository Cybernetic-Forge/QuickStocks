package net.cyberneticforge.quickstocks.core.model;

/**
 * Represents a historical price entry for an instrument.
 */
public class PriceHistory {
    private final String id;
    private final String instrumentId;
    private final long timestamp;
    private final double price;
    private final double volume;
    private final String reason;

    public PriceHistory(String id, String instrumentId, long timestamp, 
                       double price, double volume, String reason) {
        this.id = id;
        this.instrumentId = instrumentId;
        this.timestamp = timestamp;
        this.price = price;
        this.volume = volume;
        this.reason = reason;
    }

    public String getId() {
        return id;
    }

    public String getInstrumentId() {
        return instrumentId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public double getPrice() {
        return price;
    }

    public double getVolume() {
        return volume;
    }

    public String getReason() {
        return reason;
    }
}
