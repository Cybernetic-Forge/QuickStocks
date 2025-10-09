package net.cyberneticforge.quickstocks.core.model;

/**
 * Represents a historical price entry for an instrument.
 */
@SuppressWarnings("unused")
public record PriceHistory(String id, String instrumentId, long timestamp, double price, double volume, String reason) {
}
