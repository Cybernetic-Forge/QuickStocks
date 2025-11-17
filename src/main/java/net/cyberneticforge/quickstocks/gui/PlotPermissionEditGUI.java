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

import java.util.List;
import java.util.Optional;

/**
 * GUI for editing specific permissions for a job role on a plot.
 */
public class PlotPermissionEditGUI implements InventoryHolder {
    
    private static final PluginLogger logger = QuickStocksPlugin.getPluginLogger();
    
    private final Player player;
    @Getter
    private final CompanyPlot plot;
    @Getter
    private final CompanyJob job;
    private final Inventory inventory;
    
    public PlotPermissionEditGUI(Player player, CompanyPlot plot, CompanyJob job) {
        this.player = player;
        this.plot = plot;
        this.job = job;
        int guiSize = QuickStocksPlugin.getGuiConfig().getConfig().getInt("plot_permission_edit.size", 27);
        Component title = QuickStocksPlugin.getGuiConfig().getTitle("plot_permission_edit",
            new Replaceable("{job_title}", job.getTitle())
        );
        this.inventory = Bukkit.createInventory(this, guiSize, title);
        setupGUI();
    }
    
    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
    
    /**
     * Sets up the GUI with permission toggles.
     */
    private void setupGUI() {
        try {
            // Get current permissions
            Optional<PlotPermission> permOpt = QuickStocksPlugin.getCompanyPlotService()
                .getPlotPermission(plot.getId(), job.getId());
            
            boolean canBuild = permOpt.map(PlotPermission::canBuild).orElse(true);
            boolean canInteract = permOpt.map(PlotPermission::canInteract).orElse(true);
            boolean canContainer = permOpt.map(PlotPermission::canContainer).orElse(true);
            
            // Add permission toggles
            addPermissionToggle("build_permission", canBuild);
            addPermissionToggle("interact_permission", canInteract);
            addPermissionToggle("container_permission", canContainer);
            
            // Add navigation buttons
            addNavigationButtons();
            
        } catch (Exception e) {
            logger.warning("Error setting up Plot Permission Edit GUI: " + e.getMessage());
            String errorMsg = QuickStocksPlugin.getGuiConfig().getConfig().getString("plot_permission_edit.error_message", "&cFailed to load permissions.");
            player.sendMessage(ChatUT.hexComp(errorMsg));
        }
    }
    
    /**
     * Adds a permission toggle button.
     */
    private void addPermissionToggle(String configKey, boolean enabled) {
        String path = "plot_permission_edit." + configKey;
        
        // Get defaults based on permission type
        Material defaultMaterial = getDefaultMaterial(configKey);
        int defaultSlot = getDefaultSlot(configKey);
        
        Material material = QuickStocksPlugin.getGuiConfig().getItemMaterial(path, defaultMaterial);
        int slot = QuickStocksPlugin.getGuiConfig().getItemSlot(path, defaultSlot);
        
        String statusKey = enabled ? path + ".status_enabled" : path + ".status_disabled";
        String statusText = QuickStocksPlugin.getGuiConfig().getConfig().getString(statusKey, enabled ? "&a✓ Enabled" : "&c✗ Disabled");
        
        String description = QuickStocksPlugin.getGuiConfig().getConfig().getString(path + ".description", "");
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(QuickStocksPlugin.getGuiConfig().getItemName(path));
        
        List<Component> lore = QuickStocksPlugin.getGuiConfig().getItemLore(path,
            new Replaceable("{description}", description),
            new Replaceable("{status}", statusText)
        );
        meta.lore(lore);
        
        item.setItemMeta(meta);
        inventory.setItem(slot, item);
    }
    
    /**
     * Gets the default material for a permission type.
     */
    private Material getDefaultMaterial(String configKey) {
        return switch (configKey) {
            case "build_permission" -> Material.IRON_PICKAXE;
            case "interact_permission" -> Material.LEVER;
            case "container_permission" -> Material.CHEST;
            default -> Material.STONE;
        };
    }
    
    /**
     * Gets the default slot for a permission type.
     */
    private int getDefaultSlot(String configKey) {
        return switch (configKey) {
            case "build_permission" -> 10;
            case "interact_permission" -> 13;
            case "container_permission" -> 16;
            default -> 0;
        };
    }
    
    /**
     * Adds navigation buttons.
     */
    private void addNavigationButtons() {
        // Back button
        Material material = QuickStocksPlugin.getGuiConfig().getItemMaterial("plot_permission_edit.back", Material.ARROW);
        int slot = QuickStocksPlugin.getGuiConfig().getItemSlot("plot_permission_edit.back", 22);
        
        ItemStack backItem = new ItemStack(material);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.displayName(QuickStocksPlugin.getGuiConfig().getItemName("plot_permission_edit.back"));
        List<Component> lore = QuickStocksPlugin.getGuiConfig().getItemLore("plot_permission_edit.back");
        backMeta.lore(lore);
        backItem.setItemMeta(backMeta);
        inventory.setItem(slot, backItem);
    }
}
