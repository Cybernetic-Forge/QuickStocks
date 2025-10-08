package net.cyberneticforge.quickstocks.core.model;

import lombok.Getter;

/**
 * Represents an instrument in the market system.
 * Can be a stock, cryptocurrency, Minecraft item, or other tradeable asset.
 */
public record Instrument(String id, String type, String symbol, String displayName, String mcMaterial, int decimals,
                         String createdBy, long createdAt) {

}
