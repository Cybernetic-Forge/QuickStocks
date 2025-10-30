package net.cyberneticforge.quickstocks.listeners;

import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.core.enums.Translation;
import net.cyberneticforge.quickstocks.core.model.Company;
import net.cyberneticforge.quickstocks.core.model.CompanyPlot;
import net.cyberneticforge.quickstocks.core.model.Replaceable;
import net.cyberneticforge.quickstocks.infrastructure.logging.PluginLogger;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Listener for company plot-related events.
 */
public class CompanyPlotListener implements Listener {
    
    private static final PluginLogger logger = QuickStocksPlugin.getPluginLogger();
    
    // Track the last chunk each player was in to avoid duplicate purchases
    private final Map<UUID, String> lastChunkByPlayer = new HashMap<>();
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Chunk fromChunk = event.getFrom().getChunk();
        Chunk toChunk = event.getTo().getChunk();
        
        // Only process if player moved to a different chunk
        if (fromChunk.getX() == toChunk.getX() && fromChunk.getZ() == toChunk.getZ()) {
            return;
        }
        
        // Check if player has auto-buy mode enabled
        String playerUuid = player.getUniqueId().toString();
        String chunkKey = toChunk.getWorld().getName() + ":" + toChunk.getX() + ":" + toChunk.getZ();
        
        // Skip if we already processed this chunk for this player
        String lastChunk = lastChunkByPlayer.get(player.getUniqueId());
        if (chunkKey.equals(lastChunk)) {
            return;
        }
        
        lastChunkByPlayer.put(player.getUniqueId(), chunkKey);
        
        try {
            Optional<String> autoBuyCompanyId = QuickStocksPlugin.getCompanyPlotService().getAutoBuyMode(playerUuid);
            
            if (autoBuyCompanyId.isEmpty()) {
                return; // Auto-buy not enabled
            }
            
            String companyId = autoBuyCompanyId.get();
            
            // Check if plot is already owned
            Optional<CompanyPlot> existingPlot = QuickStocksPlugin.getCompanyPlotService()
                .getPlotByLocation(toChunk.getWorld().getName(), toChunk.getX(), toChunk.getZ());
            
            if (existingPlot.isPresent()) {
                return; // Plot already owned, don't notify
            }
            
            // Try to buy the plot
            try {
                CompanyPlot plot = QuickStocksPlugin.getCompanyPlotService().buyPlot(
                    companyId, playerUuid, player.getLocation()
                );
                
                // Get company name for message
                Optional<Company> companyOpt = QuickStocksPlugin.getCompanyService().getCompanyById(companyId);
                String companyName = companyOpt.map(Company::getName).orElse("Unknown");
                
                Translation.Company_Plot_Purchased.sendMessage(player,
                    new Replaceable("%company%", companyName),
                    new Replaceable("%world%", plot.getWorldName()),
                    new Replaceable("%x%", String.valueOf(plot.getChunkX())),
                    new Replaceable("%z%", String.valueOf(plot.getChunkZ())),
                    new Replaceable("%price%", String.format("%.2f", plot.getBuyPrice())));
                    
            } catch (IllegalArgumentException | IllegalStateException e) {
                // Silent fail - don't spam player with errors
                // Only log if it's not an "already owned" or "insufficient funds" message
                if (!e.getMessage().contains("already owned") && !e.getMessage().contains("Insufficient")) {
                    logger.debug("Auto-buy failed for player " + player.getName() + ": " + e.getMessage());
                }
                
                // If insufficient funds, disable auto-buy mode and notify player
                if (e.getMessage().contains("Insufficient")) {
                    QuickStocksPlugin.getCompanyPlotService().setAutoBuyMode(playerUuid, null, false);
                    Translation.Company_Plot_AutoBuyDisabled.sendMessage(player);
                    Translation.Company_Plot_InsufficientFunds.sendMessage(player);
                }
            }
        } catch (Exception e) {
            logger.warning("Error in auto-buy handler: " + e.getMessage());
        }
    }
}
