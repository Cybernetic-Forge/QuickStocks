package net.cyberneticforge.quickstocks.core.model;

import net.cyberneticforge.quickstocks.core.enums.OrderType;

/**
 * Represents a trading order request with all necessary parameters.
 *
 * @param playerUuid Getters
 * @param side       BUY | SELL
 * @param limitPrice nullable, required for LIMIT orders
 * @param stopPrice  nullable, required for STOP orders
 */
@SuppressWarnings("unused")
public record OrderRequest(String playerUuid, String instrumentId, String side, OrderType type, double qty,
                           Double limitPrice, Double stopPrice) {

    // Static factory methods for common order types
    public static OrderRequest marketOrder(String playerUuid, String instrumentId, String side, double qty) {
        return new OrderRequest(playerUuid, instrumentId, side, OrderType.MARKET, qty, null, null);
    }

    public static OrderRequest limitOrder(String playerUuid, String instrumentId, String side, double qty, double limitPrice) {
        return new OrderRequest(playerUuid, instrumentId, side, OrderType.LIMIT, qty, limitPrice, null);
    }

    public static OrderRequest stopOrder(String playerUuid, String instrumentId, String side, double qty, double stopPrice) {
        return new OrderRequest(playerUuid, instrumentId, side, OrderType.STOP, qty, null, stopPrice);
    }

    /**
     * Validates the order request based on order type requirements.
     */
    public void validate() throws IllegalArgumentException {
        if (playerUuid == null || playerUuid.trim().isEmpty()) {
            throw new IllegalArgumentException("Player UUID is required");
        }
        if (instrumentId == null || instrumentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Instrument ID is required");
        }
        if (side == null || (!side.equals("BUY") && !side.equals("SELL"))) {
            throw new IllegalArgumentException("Side must be BUY or SELL");
        }
        if (qty <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        switch (type) {
            case LIMIT:
                if (limitPrice == null || limitPrice <= 0) {
                    throw new IllegalArgumentException("Limit price is required and must be positive for LIMIT orders");
                }
                break;
            case STOP:
                if (stopPrice == null || stopPrice <= 0) {
                    throw new IllegalArgumentException("Stop price is required and must be positive for STOP orders");
                }
                break;
            case MARKET:
                // No additional validation required for market orders
                break;
        }
    }
}