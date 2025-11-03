package net.cyberneticforge.quickstocks.listeners;

import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.core.enums.Translation;
import net.cyberneticforge.quickstocks.core.model.Company;
import net.cyberneticforge.quickstocks.core.model.CompanyPlot;
import net.cyberneticforge.quickstocks.core.model.Replaceable;
import net.cyberneticforge.quickstocks.infrastructure.config.CompanyCfg;
import net.cyberneticforge.quickstocks.infrastructure.logging.PluginLogger;
import net.cyberneticforge.quickstocks.utils.ChatUT;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.time.Duration;
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
    
    // Track the last company ID each player was in for terrain messages
    private final Map<UUID, String> lastCompanyByPlayer = new HashMap<>();
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Chunk fromChunk = event.getFrom().getChunk();
        Chunk toChunk = event.getTo().getChunk();
        
        // Only process if player moved to a different chunk
        if (fromChunk.getX() == toChunk.getX() && fromChunk.getZ() == toChunk.getZ()) {
            return;
        }
        
        String playerUuid = player.getUniqueId().toString();
        String chunkKey = toChunk.getWorld().getName() + ":" + toChunk.getX() + ":" + toChunk.getZ();
        
        // Skip if we already processed this chunk for this player
        String lastChunk = lastChunkByPlayer.get(player.getUniqueId());
        if (chunkKey.equals(lastChunk)) {
            return;
        }
        
        lastChunkByPlayer.put(player.getUniqueId(), chunkKey);
        
        try {
            // Check for plot ownership and show terrain messages
            Optional<CompanyPlot> toPlot = QuickStocksPlugin.getCompanyPlotService()
                .getPlotByLocation(toChunk.getWorld().getName(), toChunk.getX(), toChunk.getZ());
            
            handleTerrainMessages(player, toPlot);
            
            // Handle auto-buy mode
            Optional<String> autoBuyCompanyId = QuickStocksPlugin.getCompanyPlotService().getAutoBuyMode(playerUuid);
            
            if (autoBuyCompanyId.isEmpty()) {
                return; // Auto-buy not enabled
            }
            
            String companyId = autoBuyCompanyId.get();
            
            // If plot is already owned, skip auto-buy
            if (toPlot.isPresent()) {
                return;
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
            logger.warning("Error in chunk transition handler: " + e.getMessage());
        }
    }
    
    /**
     * Handles terrain enter/leave messages.
     */
    private void handleTerrainMessages(Player player, Optional<CompanyPlot> currentPlot) {
        try {
            CompanyCfg config = QuickStocksPlugin.getCompanyCfg();

            if (!config.isTerrainMessagesEnabled()) {
                return;
            }

            String currentCompanyId = currentPlot.map(CompanyPlot::getCompanyId).orElse(null);
            String lastCompanyId = lastCompanyByPlayer.get(player.getUniqueId());

            // Check if we changed company territory
            if ((currentCompanyId == null && lastCompanyId == null) ||
                    (currentCompanyId != null && currentCompanyId.equals(lastCompanyId))) {
                return; // No change
            }

            // Update last company
            lastCompanyByPlayer.put(player.getUniqueId(), currentCompanyId);

            String message = null;
            String companyName = null;

            if (currentCompanyId == null) {
                // Entering wilderness
                message = config.getTerrainWildernessMessage();
            } else if (lastCompanyId == null) {
                // Entering company territory from wilderness
                message = config.getTerrainEnterMessage();
                Optional<Company> company = QuickStocksPlugin.getCompanyService().getCompanyById(currentCompanyId);
                companyName = company.map(Company::getName).orElse("Unknown");
            } else {
                // Moving from one company to another - show leave message for old, enter for new
                Optional<Company> oldCompany = QuickStocksPlugin.getCompanyService().getCompanyById(lastCompanyId);
                String oldCompanyName = oldCompany.map(Company::getName).orElse("Unknown");

                String leaveMessage = config.getTerrainLeaveMessage();
                if (leaveMessage != null && !leaveMessage.trim().isEmpty()) {
                    String formattedLeave = leaveMessage.replace("%company%", oldCompanyName);
                    sendTerrainMessage(player, formattedLeave, config.getTerrainDisplayMode());
                }

                // Then show enter message for new company
                message = config.getTerrainEnterMessage();
                Optional<Company> newCompany = QuickStocksPlugin.getCompanyService().getCompanyById(currentCompanyId);
                companyName = newCompany.map(Company::getName).orElse("Unknown");
            }

            if (message != null && !message.trim().isEmpty()) {
                if (companyName != null) {
                    message = message.replace("%company%", companyName);
                }
                sendTerrainMessage(player, message, config.getTerrainDisplayMode());
            }
        } catch (Exception e) {
            Translation.UnknownException.sendMessage(player);
        }
    }
    
    /**
     * Sends a terrain message to the player using the configured display mode.
     */
    private void sendTerrainMessage(Player player, String message, String displayMode) {
        try {
            var formattedMessage = ChatUT.hexComp(message);
            
            switch (displayMode.toUpperCase()) {
                case "ACTIONBAR":
                    player.sendActionBar(formattedMessage);
                    break;
                    
                case "TITLE":
                    player.showTitle(Title.title(formattedMessage, Component.text(""), Title.Times.times(Duration.ofMillis(10),Duration.ofMillis(40),Duration.ofMillis(10))));
                    break;
                    
                case "CHAT":
                default:
                    player.sendMessage(formattedMessage);
                    break;
            }
        } catch (Exception e) {
            logger.warning("Error sending terrain message: " + e.getMessage());
        }
    }
    
    /**
     * Prevents block breaking in company plots unless player has permission.
     */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!QuickStocksPlugin.getCompanyCfg().isPlotsEnabled()) {
            return;
        }
        
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Chunk chunk = block.getChunk();
        
        try {
            Optional<CompanyPlot> plot = QuickStocksPlugin.getCompanyPlotService()
                .getPlotByLocation(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
            
            if (plot.isEmpty()) {
                return; // Not a company plot
            }
            
            // Check if player has build permission on this plot
            if (!hasPlotPermission(player, plot.get(), "build")) {
                event.setCancelled(true);
                Translation.Company_Plot_NoPermission.sendMessage(player);
            }
        } catch (Exception e) {
            logger.warning("Error checking plot protection: " + e.getMessage());
        }
    }
    
    /**
     * Prevents block placing in company plots unless player has permission.
     */
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!QuickStocksPlugin.getCompanyCfg().isPlotsEnabled()) {
            return;
        }
        
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Chunk chunk = block.getChunk();
        
        try {
            Optional<CompanyPlot> plot = QuickStocksPlugin.getCompanyPlotService()
                .getPlotByLocation(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
            
            if (plot.isEmpty()) {
                return; // Not a company plot
            }
            
            // Check if player has build permission on this plot
            if (!hasPlotPermission(player, plot.get(), "build")) {
                event.setCancelled(true);
                Translation.Company_Plot_NoPermission.sendMessage(player);
            }
        } catch (Exception e) {
            logger.warning("Error checking plot protection: " + e.getMessage());
        }
    }
    
    /**
     * Prevents container access in company plots unless player has permission.
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!QuickStocksPlugin.getCompanyCfg().isPlotsEnabled()) {
            return;
        }
        
        if (event.getClickedBlock() == null) {
            return;
        }
        
        // Only check for container interactions
        Block block = event.getClickedBlock();
        if (!isContainer(block.getType())) {
            return;
        }
        
        Player player = event.getPlayer();
        Chunk chunk = block.getChunk();
        
        try {
            Optional<CompanyPlot> plot = QuickStocksPlugin.getCompanyPlotService()
                .getPlotByLocation(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
            
            if (plot.isEmpty()) {
                return; // Not a company plot
            }
            
            // Check if player has container permission on this plot
            if (!hasPlotPermission(player, plot.get(), "container")) {
                event.setCancelled(true);
                Translation.Company_Plot_NoPermission.sendMessage(player);
            }
        } catch (Exception e) {
            logger.warning("Error checking plot protection: " + e.getMessage());
        }
    }
    
    /**
     * Checks if a player has a specific permission on a plot using per-plot permissions.
     */
    private boolean hasPlotPermission(Player player, CompanyPlot plot, String permissionType) {
        try {
            String playerUuid = player.getUniqueId().toString();
            
            // Use the plot service method to check permission
            return QuickStocksPlugin.getCompanyPlotService()
                .hasPlotPermission(plot.getId(), playerUuid, permissionType);
        } catch (Exception e) {
            logger.warning("Error checking plot permission: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Checks if a block type is a container that should be protected.
     */
    private boolean isContainer(org.bukkit.Material material) {
        return switch (material) {
            case CHEST, TRAPPED_CHEST, BARREL, SHULKER_BOX,
                 WHITE_SHULKER_BOX, ORANGE_SHULKER_BOX, MAGENTA_SHULKER_BOX,
                 LIGHT_BLUE_SHULKER_BOX, YELLOW_SHULKER_BOX, LIME_SHULKER_BOX,
                 PINK_SHULKER_BOX, GRAY_SHULKER_BOX, LIGHT_GRAY_SHULKER_BOX,
                 CYAN_SHULKER_BOX, PURPLE_SHULKER_BOX, BLUE_SHULKER_BOX,
                 BROWN_SHULKER_BOX, GREEN_SHULKER_BOX, RED_SHULKER_BOX,
                 BLACK_SHULKER_BOX, FURNACE, BLAST_FURNACE, SMOKER,
                 DROPPER, DISPENSER, HOPPER, BREWING_STAND, ENDER_CHEST -> true;
            default -> false;
        };
    }
}
