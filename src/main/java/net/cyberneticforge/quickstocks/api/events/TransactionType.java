package net.cyberneticforge.quickstocks.api.events;

/**
 * Enum representing the type of transaction in a buy/sell event.
 * Used to distinguish between different types of tradeable assets.
 */
public enum TransactionType {
    /**
     * Trading a standard instrument (item, crypto, etc.)
     */
    INSTRUMENT,
    
    /**
     * Trading company shares
     */
    SHARE,
    
    /**
     * Trading cryptocurrency (custom or standard)
     */
    CRYPTO
}
