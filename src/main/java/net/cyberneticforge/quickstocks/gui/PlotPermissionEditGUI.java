package net.cyberneticforge.quickstocks.gui;

import lombok.Getter;
import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.core.model.CompanyJob;
import net.cyberneticforge.quickstocks.core.model.CompanyPlot;
import net.cyberneticforge.quickstocks.core.model.PlotPermission;
import net.cyberneticforge.quickstocks.infrastructure.logging.PluginLogger;
import net.cyberneticforge.quickstocks.utils.ChatUT;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
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
        this.inventory = Bukkit.createInventory(this, 27, ChatUT.hexComp("&6Edit: &e" + job.getTitle()));
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
            addPermissionToggle(10, "Build", "Break and place blocks", canBuild, Material.IRON_PICKAXE);
            addPermissionToggle(13, "Interact", "Use buttons, levers, doors", canInteract, Material.LEVER);
            addPermissionToggle(16, "Container", "Access chests and containers", canContainer, Material.CHEST);
            
            // Add navigation buttons
            addNavigationButtons();
            
        } catch (Exception e) {
            logger.warning("Error setting up Plot Permission Edit GUI: " + e.getMessage());
            player.sendMessage("§cFailed to load permissions.");
        }
    }
    
    /**
     * Adds a permission toggle button.
     */
    private void addPermissionToggle(int slot, String name, String description, boolean enabled, Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ChatUT.hexComp("&e" + name));
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatUT.hexComp("&7" + description).content());
        lore.add("");
        lore.add(ChatUT.hexComp("&7Status: " + (enabled ? "&a✓ Enabled" : "&c✗ Disabled")).content());
        lore.add("");
        lore.add(ChatUT.hexComp("&eClick to toggle").content());
        meta.setLore(lore);
        
        item.setItemMeta(meta);
        inventory.setItem(slot, item);
    }
    
    /**
     * Adds navigation buttons.
     */
    private void addNavigationButtons() {
        // Back button
        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.displayName(ChatUT.hexComp("&eBack"));
        backItem.setItemMeta(backMeta);
        inventory.setItem(22, backItem);
    }
}
