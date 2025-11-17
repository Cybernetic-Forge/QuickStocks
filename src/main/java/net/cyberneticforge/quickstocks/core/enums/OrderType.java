package net.cyberneticforge.quickstocks.core.enums;

/**
 * Enumeration of supported order types for trading operations.
 */
public enum OrderType {
    /**
     * Market order - executes immediately at current market price.
     */
    MARKET,
    
    /**
     * Limit order - executes only at the specified limit price or better.
     */
    LIMIT,
    
    /**
     * Stop order - triggers at stop price and executes as market order.
     */
    STOP
}