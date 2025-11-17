package net.cyberneticforge.quickstocks.api.events;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Unified event fired when a player buys any tradeable asset (instruments, shares, crypto).
 * This event is cancellable - cancel to prevent the purchase.
 */
@Getter
@SuppressWarnings("unused")
public class ShareBuyEvent extends Event implements Cancellable {
    
    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled = false;
    
    private final Player buyer;
    private final TransactionType transactionType;
    private final String assetId;        // instrumentId, companyId, or cryptoId
    private final String assetSymbol;    // symbol or name for display
    private final double quantity;
    private final double pricePerUnit;
    private final double totalCost;
    
    /**
     * Creates a ShareBuyEvent for any type of asset purchase.
     * 
     * @param buyer The player making the purchase
     * @param transactionType The type of asset being purchased
     * @param assetId The unique identifier of the asset
     * @param assetSymbol The symbol or display name of the asset
     * @param quantity The quantity being purchased
     * @param pricePerUnit The price per unit
     * @param totalCost The total cost of the purchase
     */
    public ShareBuyEvent(Player buyer, TransactionType transactionType, String assetId, 
                         String assetSymbol, double quantity, double pricePerUnit, double totalCost) {
        this.buyer = buyer;
        this.transactionType = transactionType;
        this.assetId = assetId;
        this.assetSymbol = assetSymbol;
        this.quantity = quantity;
        this.pricePerUnit = pricePerUnit;
        this.totalCost = totalCost;
    }
    
    /**
     * Legacy constructor for backward compatibility with company shares.
     * @deprecated Use {@link #ShareBuyEvent(Player, TransactionType, String, String, double, double, double)} instead
     */
    @Deprecated
    public ShareBuyEvent(Player buyer, String companyId, String companyName, 
                         int quantity, double pricePerShare, double totalCost) {
        this(buyer, TransactionType.SHARE, companyId, companyName, quantity, pricePerShare, totalCost);
    }
    
    // Legacy getters for backward compatibility
    /**
     * @deprecated Use {@link #getAssetId()} instead
     */
    @Deprecated
    public String getCompanyId() {
        return assetId;
    }
    
    /**
     * @deprecated Use {@link #getAssetSymbol()} instead
     */
    @Deprecated
    public String getCompanyName() {
        return assetSymbol;
    }
    
    /**
     * @deprecated Use {@link #getPricePerUnit()} instead
     */
    @Deprecated
    public double getPricePerShare() {
        return pricePerUnit;
    }
    
    @Override
    public boolean isCancelled() {
        return cancelled;
    }
    
    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
    
    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }
    
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
