package net.cyberneticforge.quickstocks.gui;

import lombok.Getter;
import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.core.model.CompanyJob;
import net.cyberneticforge.quickstocks.core.model.CompanyPlot;
import net.cyberneticforge.quickstocks.core.model.PlotPermission;
import net.cyberneticforge.quickstocks.core.model.Replaceable;
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
        int guiSize = QuickStocksPlugin.getGuiConfig().getConfig().getInt("plot_edit.size", 54);
        Component title = QuickStocksPlugin.getGuiConfig().getTitle("plot_edit");
        this.inventory = Bukkit.createInventory(this, guiSize, title);
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
            String errorMsg = QuickStocksPlugin.getGuiConfig().getConfig().getString("plot_edit.error_message", "&cFailed to load plot permissions.");
            player.sendMessage(ChatUT.hexComp(errorMsg));
        }
    }
    
    /**
     * Adds plot information display.
     */
    private void addPlotInfo() {
        Material material = QuickStocksPlugin.getGuiConfig().getItemMaterial("plot_edit.plot_info", Material.MAP);
        int slot = QuickStocksPlugin.getGuiConfig().getItemSlot("plot_edit.plot_info", 4);
        
        ItemStack plotInfo = new ItemStack(material);
        ItemMeta meta = plotInfo.getItemMeta();
        meta.displayName(QuickStocksPlugin.getGuiConfig().getItemName("plot_edit.plot_info"));
        
        List<Component> lore = QuickStocksPlugin.getGuiConfig().getItemLore("plot_edit.plot_info",
            new Replaceable("{world_name}", plot.getWorldName()),
            new Replaceable("{chunk_x}", String.valueOf(plot.getChunkX())),
            new Replaceable("{chunk_z}", String.valueOf(plot.getChunkZ()))
        );
        meta.lore(lore);
        
        plotInfo.setItemMeta(meta);
        inventory.setItem(slot, plotInfo);
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
        Material material = QuickStocksPlugin.getGuiConfig().getItemMaterial("plot_edit.job_item", Material.NAME_TAG);
        String enabledIcon = QuickStocksPlugin.getGuiConfig().getConfig().getString("plot_edit.job_item.permission_enabled", "&a✓");
        String disabledIcon = QuickStocksPlugin.getGuiConfig().getConfig().getString("plot_edit.job_item.permission_disabled", "&c✗");
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(QuickStocksPlugin.getGuiConfig().getItemName("plot_edit.job_item",
            new Replaceable("{job_title}", job.getTitle())
        ));
        
        List<Component> lore = QuickStocksPlugin.getGuiConfig().getItemLore("plot_edit.job_item",
            new Replaceable("{build_status}", canBuild ? enabledIcon : disabledIcon),
            new Replaceable("{interact_status}", canInteract ? enabledIcon : disabledIcon),
            new Replaceable("{container_status}", canContainer ? enabledIcon : disabledIcon)
        );
        meta.lore(lore);
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Adds navigation buttons.
     */
    private void addNavigationButtons() {
        // Close button
        Material material = QuickStocksPlugin.getGuiConfig().getItemMaterial("plot_edit.close", Material.BARRIER);
        int slot = QuickStocksPlugin.getGuiConfig().getItemSlot("plot_edit.close", 49);
        
        ItemStack closeItem = new ItemStack(material);
        ItemMeta closeMeta = closeItem.getItemMeta();
        closeMeta.displayName(QuickStocksPlugin.getGuiConfig().getItemName("plot_edit.close"));
        List<Component> lore = QuickStocksPlugin.getGuiConfig().getItemLore("plot_edit.close");
        closeMeta.lore(lore);
        closeItem.setItemMeta(closeMeta);
        inventory.setItem(slot, closeItem);
    }
}
