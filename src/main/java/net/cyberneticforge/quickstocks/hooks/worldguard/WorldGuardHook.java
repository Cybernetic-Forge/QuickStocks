package net.cyberneticforge.quickstocks.hooks.worldguard;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.infrastructure.logging.PluginLogger;
import org.bukkit.entity.Player;

/**
 * Integration hook for WorldGuard plugin to handle region-based permissions.
 * This class provides utility methods for checking custom QuickStocks flags in WorldGuard regions.
 */
public class WorldGuardHook {
    
    private static final PluginLogger logger = QuickStocksPlugin.getPluginLogger();
    
    private final WorldGuardPlugin worldGuardPlugin;
    private final RegionContainer regionContainer;
    
    public WorldGuardHook() {
        this.worldGuardPlugin = WorldGuardPlugin.inst();
        this.regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
    }
    
    /**
     * Checks if a player can buy plots at the given location based on WorldGuard regions.
     * 
     * @param player The player attempting to buy a plot
     * @param location The Bukkit location where the plot is being purchased
     * @return true if the player can buy plots, false if blocked by a region flag
     */
    public boolean canBuyPlot(Player player, org.bukkit.Location location) {
        if (WorldGuardFlags.QUICKSTOCKS_PLOTS == null) {
            // Flag not registered, allow by default
            return true;
        }
        
        return checkFlag(player, location, WorldGuardFlags.QUICKSTOCKS_PLOTS);
    }
    
    /**
     * Checks if a player can trade stocks at the given location based on WorldGuard regions.
     * 
     * @param player The player attempting to trade
     * @param location The Bukkit location where trading is being attempted
     * @return true if the player can trade, false if blocked by a region flag
     */
    public boolean canTrade(Player player, org.bukkit.Location location) {
        if (WorldGuardFlags.QUICKSTOCKS_TRADING == null) {
            // Flag not registered, allow by default
            return true;
        }
        
        return checkFlag(player, location, WorldGuardFlags.QUICKSTOCKS_TRADING);
    }
    
    /**
     * Generic method to check a StateFlag at a location for a player.
     * 
     * @param player The player to check
     * @param location The Bukkit location to check
     * @param flag The StateFlag to query
     * @return true if allowed, false if denied
     */
    private boolean checkFlag(Player player, org.bukkit.Location location, StateFlag flag) {
        try {
            // Convert Bukkit location to WorldEdit location
            Location weLocation = BukkitAdapter.adapt(location);
            
            // Get the region query
            RegionQuery query = regionContainer.createQuery();
            
            // Convert Bukkit player to WorldGuard LocalPlayer
            LocalPlayer localPlayer = worldGuardPlugin.wrapPlayer(player);
            
            // Query the flag - if null (not set), defaults to allow
            StateFlag.State state = query.queryState(weLocation, localPlayer, flag);
            
            // If state is null, default to allow (permissive by default)
            if (state == null) {
                return true;
            }
            
            return state == StateFlag.State.ALLOW;
            
        } catch (Exception e) {
            logger.warning("Error checking WorldGuard flag '" + flag.getName() + "' at location " + 
                          location.getWorld().getName() + " (" + location.getBlockX() + ", " + 
                          location.getBlockY() + ", " + location.getBlockZ() + ")", e);
            // On error, default to allow
            return true;
        }
    }
    
    /**
     * Gets the applicable regions at a location.
     * Useful for debugging or informational purposes.
     * 
     * @param location The Bukkit location to check
     * @return The set of regions at this location
     */
    public ApplicableRegionSet getRegions(org.bukkit.Location location) {
        try {
            Location weLocation = BukkitAdapter.adapt(location);
            RegionQuery query = regionContainer.createQuery();
            return query.getApplicableRegions(weLocation);
        } catch (Exception e) {
            logger.warning("Error getting regions at location", e);
            return null;
        }
    }
}
