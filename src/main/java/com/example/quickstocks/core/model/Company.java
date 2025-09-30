package com.example.quickstocks.core.model;

/**
 * Represents a company/corporation entity.
 */
public class Company {
    private final String id;
    private final String name;
    private final String type;
    private final String ownerUuid;
    private final double balance;
    private final long createdAt;
    
    public Company(String id, String name, String type, String ownerUuid, double balance, long createdAt) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.ownerUuid = ownerUuid;
        this.balance = balance;
        this.createdAt = createdAt;
    }
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public String getType() {
        return type;
    }
    
    public String getOwnerUuid() {
        return ownerUuid;
    }
    
    public double getBalance() {
        return balance;
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
}
