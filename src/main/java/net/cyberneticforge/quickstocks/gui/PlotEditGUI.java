package net.cyberneticforge.quickstocks.gui;

import lombok.Getter;
import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.core.model.CompanyJob;
import net.cyberneticforge.quickstocks.core.model.CompanyPlot;
import net.cyberneticforge.quickstocks.core.model.PlotPermission;
import net.cyberneticforge.quickstocks.infrastructure.logging.PluginLogger;
import net.cyberneticforge.quickstocks.utils.ChatUT;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

/**
 * GUI for editing per-plot permissions for each job role.
 */
public class PlotEditGUI implements InventoryHolder {
    
    private static final PluginLogger logger = QuickStocksPlugin.getPluginLogger();
    
    private final Player player;
    @Getter
    private final CompanyPlot plot;
    private final Inventory inventory;
    @Getter
    private final HashMap<Integer, CompanyJob> invSlots;

    public PlotEditGUI(Player player, CompanyPlot plot) {
        this.player = player;
        this.plot = plot;
        this.inventory = Bukkit.createInventory(this, 54, ChatUT.hexComp("&6Edit Plot Permissions"));
        this.invSlots = new HashMap<>();
        setupGUI();
    }
    
    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
    
    /**
     * Sets up the GUI with job roles and their permissions.
     */
    private void setupGUI() {
        try {
            // Add plot info at the top
            addPlotInfo();
            
            // Add job roles and their permissions
            addJobPermissions();
            
            // Add navigation buttons
            addNavigationButtons();
            
        } catch (Exception e) {
            logger.warning("Error setting up Plot Edit GUI: " + e.getMessage());
            player.sendMessage("§cFailed to load plot permissions.");
        }
    }
    
    /**
     * Adds plot information display.
     */
    private void addPlotInfo() {
        ItemStack plotInfo = new ItemStack(Material.MAP);
        ItemMeta meta = plotInfo.getItemMeta();
        meta.displayName(ChatUT.hexComp("&6Plot Information"));
        
        List<Component> lore = new ArrayList<>();
        lore.add(ChatUT.hexComp("&7World: &f" + plot.getWorldName()));
        lore.add(ChatUT.hexComp("&7Chunk: &f(" + plot.getChunkX() + ", " + plot.getChunkZ() + ")"));
        lore.add(ChatUT.hexComp(""));
        lore.add(ChatUT.hexComp("&7Click on job roles below to edit permissions"));
        meta.lore(lore);
        
        plotInfo.setItemMeta(meta);
        inventory.setItem(4, plotInfo);
    }
    
    /**
     * Adds job roles with their current permissions.
     */
    private void addJobPermissions() throws Exception {
        List<CompanyJob> jobs = QuickStocksPlugin.getCompanyService().getCompanyJobs(plot.getCompanyId());
        
        int slot = 18; // Start at second row
        for (CompanyJob job : jobs) {
            if (slot >= 36) break; // Don't overflow into navigation area
            
            // Get current permissions for this job
            Optional<PlotPermission> permOpt = QuickStocksPlugin.getCompanyPlotService()
                .getPlotPermission(plot.getId(), job.getId());
            
            boolean canBuild = permOpt.map(PlotPermission::canBuild).orElse(true);
            boolean canInteract = permOpt.map(PlotPermission::canInteract).orElse(true);
            boolean canContainer = permOpt.map(PlotPermission::canContainer).orElse(true);
            
            ItemStack jobItem = createJobPermissionItem(job, canBuild, canInteract, canContainer);
            inventory.setItem(slot, jobItem);
            invSlots.put(slot, job);
            slot++;
        }
    }
    
    /**
     * Creates an item representing a job role with its permissions.
     */
    private ItemStack createJobPermissionItem(CompanyJob job, boolean canBuild, boolean canInteract, boolean canContainer) {
        ItemStack item = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ChatUT.hexComp("&e" + job.getTitle()));
        
        List<Component> lore = new ArrayList<>();
        lore.add(ChatUT.hexComp("&7Permissions:"));
        lore.add(ChatUT.hexComp((canBuild ? "&a✓" : "&c✗") + " &7Build (Break/Place)"));
        lore.add(ChatUT.hexComp((canInteract ? "&a✓" : "&c✗") + " &7Interact (Buttons/Doors)"));
        lore.add(ChatUT.hexComp((canContainer ? "&a✓" : "&c✗") + " &7Containers (Chests)"));
        lore.add(ChatUT.hexComp(""));
        lore.add(ChatUT.hexComp("&eClick to edit permissions"));
        meta.lore(lore);
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Adds navigation buttons.
     */
    private void addNavigationButtons() {
        // Close button
        ItemStack closeItem = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeItem.getItemMeta();
        closeMeta.displayName(ChatUT.hexComp("&cClose"));
        closeItem.setItemMeta(closeMeta);
        inventory.setItem(49, closeItem);
    }
}
