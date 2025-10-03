package net.cyberneticforge.quickstocks.listeners.shops;

import com.Acrobot.ChestShop.Events.Protection.ProtectionCheckEvent;
import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.infrastructure.config.CompanyConfig;
import net.cyberneticforge.quickstocks.infrastructure.hooks.ChestShopHook;
import net.cyberneticforge.quickstocks.infrastructure.hooks.HookType;
import org.bukkit.block.Sign;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.logging.Logger;

/**
 * Listener for ChestShop protection events to allow company employees to manage shops.
 * Employees with chestshop permission can access and modify company-owned chest shops.
 */
public class ChestShopProtectionListener implements Listener {
    
    private static final Logger logger = Logger.getLogger(ChestShopProtectionListener.class.getName());
    
    private final QuickStocksPlugin plugin;
    private final ChestShopHook chestShopHook;
    private final CompanyConfig companyConfig;
    
    public ChestShopProtectionListener(QuickStocksPlugin plugin, ChestShopHook chestShopHook, CompanyConfig companyConfig) {
        this.plugin = plugin;
        this.chestShopHook = chestShopHook;
        this.companyConfig = companyConfig;
    }
    
    /**
     * Handles protection checks for company-owned shops.
     * Allows employees with chestshop permission to manage the shop.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onProtectionCheck(ProtectionCheckEvent event) {
        // Only process if ChestShop is hooked and enabled
        if (!plugin.getHookManager().isHooked(HookType.ChestShop)) return;
        if (!companyConfig.isChestShopEnabled()) return;
        if (!(event.getBlock().getState() instanceof Sign sign)) return;

        String ownerName = sign.getLine(0);

        if (ownerName.trim().isEmpty()) return;
        
        // Check if this is a company shop and if the player has permission
        if (chestShopHook.canManageShop(ownerName.trim(), event.getPlayer())) {
            event.setResult(Event.Result.ALLOW);
            logger.fine("Granted shop access to " + event.getPlayer().getName() + " for company '" + ownerName + "'");
        }
    }
}
