package net.cyberneticforge.quickstocks.core.model;

import lombok.Getter;

/**
 * Represents a company/corporation entity.
 */
@Getter
public class Company {
    private final String id;
    private final String name;
    private final String type;
    private final String ownerUuid;
    private final double balance;
    private final long createdAt;
    private final String symbol;
    private final boolean onMarket;
    private final double marketPercentage;
    private final boolean allowBuyout;
    
    public Company(String id, String name, String type, String ownerUuid, double balance, long createdAt) {
        this(id, name, type, ownerUuid, balance, createdAt, null, false, 70.0, false);
    }
    
    public Company(String id, String name, String type, String ownerUuid, double balance, long createdAt,
                   String symbol, boolean onMarket, double marketPercentage, boolean allowBuyout) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.ownerUuid = ownerUuid;
        this.balance = balance;
        this.createdAt = createdAt;
        this.symbol = symbol;
        this.onMarket = onMarket;
        this.marketPercentage = marketPercentage;
        this.allowBuyout = allowBuyout;
    }

}
