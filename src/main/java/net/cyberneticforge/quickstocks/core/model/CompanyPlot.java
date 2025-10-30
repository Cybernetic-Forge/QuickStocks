package net.cyberneticforge.quickstocks.core.model;

import lombok.Getter;

/**
 * Represents a plot/chunk owned by a company.
 */
@Getter
public class CompanyPlot {
    private final String id;
    private final String companyId;
    private final String worldName;
    private final int chunkX;
    private final int chunkZ;
    private final double buyPrice;
    private final long purchasedAt;
    private final double rentAmount;
    private final String rentInterval;
    private final Long lastRentPayment;
    
    public CompanyPlot(String id, String companyId, String worldName, int chunkX, int chunkZ,
                       double buyPrice, long purchasedAt, double rentAmount, String rentInterval,
                       Long lastRentPayment) {
        this.id = id;
        this.companyId = companyId;
        this.worldName = worldName;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.buyPrice = buyPrice;
        this.purchasedAt = purchasedAt;
        this.rentAmount = rentAmount;
        this.rentInterval = rentInterval;
        this.lastRentPayment = lastRentPayment;
    }
    
    /**
     * Checks if rent is enabled for this plot.
     */
    public boolean hasRent() {
        return rentAmount >= 0;
    }
    
    /**
     * Gets the rent interval in milliseconds.
     */
    public long getRentIntervalMillis() {
        return switch (rentInterval.toLowerCase()) {
            case "hourly", "1h" -> 60 * 60 * 1000L;
            case "daily", "24h" -> 24 * 60 * 60 * 1000L;
            case "weekly", "1w" -> 7 * 24 * 60 * 60 * 1000L;
            case "monthly", "1m" -> 30L * 24 * 60 * 60 * 1000L;
            default -> 30L * 24 * 60 * 60 * 1000L; // default to monthly
        };
    }
    
    /**
     * Checks if rent is due based on the last payment time.
     */
    public boolean isRentDue() {
        if (!hasRent() || lastRentPayment == null) {
            return false;
        }
        
        long now = System.currentTimeMillis();
        long nextPaymentDue = lastRentPayment + getRentIntervalMillis();
        return now >= nextPaymentDue;
    }
}
