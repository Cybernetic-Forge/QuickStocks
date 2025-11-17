package net.cyberneticforge.quickstocks.api.events;

import lombok.Getter;
import net.cyberneticforge.quickstocks.core.enums.TransactionType;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Unified event fired when a player sells any tradeable asset (instruments, shares, crypto).
 * This event is cancellable - cancel to prevent the sale.
 */
@Getter
@SuppressWarnings("unused")
public class ShareSellEvent extends Event implements Cancellable {
    
    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled = false;
    
    private final Player seller;
    private final TransactionType transactionType;
    private final String assetId;        // instrumentId, companyId, or cryptoId
    private final String assetSymbol;    // symbol or name for display
    private final double quantity;
    private final double pricePerUnit;
    private final double totalRevenue;
    
    /**
     * Creates a ShareSellEvent for any type of asset sale.
     * 
     * @param seller The player making the sale
     * @param transactionType The type of asset being sold
     * @param assetId The unique identifier of the asset
     * @param assetSymbol The symbol or display name of the asset
     * @param quantity The quantity being sold
     * @param pricePerUnit The price per unit
     * @param totalRevenue The total revenue from the sale
     */
    public ShareSellEvent(Player seller, TransactionType transactionType, String assetId, 
                          String assetSymbol, double quantity, double pricePerUnit, double totalRevenue) {
        this.seller = seller;
        this.transactionType = transactionType;
        this.assetId = assetId;
        this.assetSymbol = assetSymbol;
        this.quantity = quantity;
        this.pricePerUnit = pricePerUnit;
        this.totalRevenue = totalRevenue;
    }
    
    /**
     * Legacy constructor for backward compatibility with company shares.
     * @deprecated Use {@link #ShareSellEvent(Player, TransactionType, String, String, double, double, double)} instead
     */
    @Deprecated
    public ShareSellEvent(Player seller, String companyId, String companyName, 
                          int quantity, double pricePerShare, double totalRevenue) {
        this(seller, TransactionType.SHARE, companyId, companyName, quantity, pricePerShare, totalRevenue);
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
