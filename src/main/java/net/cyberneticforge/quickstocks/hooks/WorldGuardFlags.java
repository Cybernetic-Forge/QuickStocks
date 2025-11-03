package net.cyberneticforge.quickstocks.hooks;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.infrastructure.logging.PluginLogger;

/**
 * Defines and registers custom WorldGuard flags for QuickStocks.
 */
public class WorldGuardFlags {
    
    private static final PluginLogger logger = QuickStocksPlugin.getPluginLogger();
    
    /**
     * Flag to control whether players can buy plots in a WorldGuard region.
     * When set to ALLOW, players can purchase plots within the region.
     * When set to DENY, plot purchases are blocked.
     * Default state is ALLOW (null means allow).
     */
    public static StateFlag QUICKSTOCKS_PLOTS;
    
    /**
     * Flag to control stock trading in a WorldGuard region.
     * When set to ALLOW, players can trade stocks within the region.
     * When set to DENY, stock trading is blocked.
     * Default state is ALLOW (null means allow).
     */
    public static StateFlag QUICKSTOCKS_TRADING;
    
    /**
     * Flag to control company ChestShop placement in a WorldGuard region.
     * When set to ALLOW, companies can create ChestShops within the region.
     * When set to DENY, ChestShop creation is blocked.
     * Default state is ALLOW (null means allow).
     */
    public static StateFlag QUICKSTOCKS_CHESTSHOPS;
    
    /**
     * Registers all QuickStocks custom flags with WorldGuard.
     * This must be called before WorldGuard finishes loading.
     */
    public static void registerFlags() {
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
        
        try {
            // Register quickstocks-plots flag
            StateFlag plotsFlag = new StateFlag("quickstocks-plots", true);
            registry.register(plotsFlag);
            QUICKSTOCKS_PLOTS = plotsFlag;
            logger.info("Registered WorldGuard flag: quickstocks-plots");
            
        } catch (FlagConflictException e) {
            // Flag already exists, try to get it
            Flag<?> existing = registry.get("quickstocks-plots");
            if (existing instanceof StateFlag) {
                QUICKSTOCKS_PLOTS = (StateFlag) existing;
                logger.debug("WorldGuard flag 'quickstocks-plots' already registered");
            } else {
                logger.warning("Could not register flag 'quickstocks-plots': conflict with existing flag", e);
            }
        }
        
        try {
            // Register quickstocks-trading flag
            StateFlag tradingFlag = new StateFlag("quickstocks-trading", true);
            registry.register(tradingFlag);
            QUICKSTOCKS_TRADING = tradingFlag;
            logger.info("Registered WorldGuard flag: quickstocks-trading");
            
        } catch (FlagConflictException e) {
            // Flag already exists, try to get it
            Flag<?> existing = registry.get("quickstocks-trading");
            if (existing instanceof StateFlag) {
                QUICKSTOCKS_TRADING = (StateFlag) existing;
                logger.debug("WorldGuard flag 'quickstocks-trading' already registered");
            } else {
                logger.warning("Could not register flag 'quickstocks-trading': conflict with existing flag", e);
            }
        }
        
        try {
            // Register quickstocks-chestshops flag
            StateFlag chestshopsFlag = new StateFlag("quickstocks-chestshops", true);
            registry.register(chestshopsFlag);
            QUICKSTOCKS_CHESTSHOPS = chestshopsFlag;
            logger.info("Registered WorldGuard flag: quickstocks-chestshops");
            
        } catch (FlagConflictException e) {
            // Flag already exists, try to get it
            Flag<?> existing = registry.get("quickstocks-chestshops");
            if (existing instanceof StateFlag) {
                QUICKSTOCKS_CHESTSHOPS = (StateFlag) existing;
                logger.debug("WorldGuard flag 'quickstocks-chestshops' already registered");
            } else {
                logger.warning("Could not register flag 'quickstocks-chestshops': conflict with existing flag", e);
            }
        }
    }
}
