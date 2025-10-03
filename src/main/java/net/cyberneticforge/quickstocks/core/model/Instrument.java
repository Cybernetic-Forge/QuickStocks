package net.cyberneticforge.quickstocks.core.model;

/**
 * Represents an instrument in the market system.
 * Can be a stock, cryptocurrency, Minecraft item, or other tradeable asset.
 */
public class Instrument {
    private final String id;
    private final String type;
    private final String symbol;
    private final String displayName;
    private final String mcMaterial;
    private final int decimals;
    private final String createdBy;
    private final long createdAt;

    public Instrument(String id, String type, String symbol, String displayName, 
                     String mcMaterial, int decimals, String createdBy, long createdAt) {
        this.id = id;
        this.type = type;
        this.symbol = symbol;
        this.displayName = displayName;
        this.mcMaterial = mcMaterial;
        this.decimals = decimals;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getMcMaterial() {
        return mcMaterial;
    }

    public int getDecimals() {
        return decimals;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
