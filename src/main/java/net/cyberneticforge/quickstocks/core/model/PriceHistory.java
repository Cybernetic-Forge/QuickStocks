package net.cyberneticforge.quickstocks.core.model;

import lombok.Getter;

/**
 * Represents a historical price entry for an instrument.
 */
public record PriceHistory(String id, String instrumentId, long timestamp, double price, double volume, String reason) {

}
