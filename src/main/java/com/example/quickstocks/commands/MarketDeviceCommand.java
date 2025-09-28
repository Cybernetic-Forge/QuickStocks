package com.example.quickstocks.commands;

import com.example.quickstocks.utils.TranslationManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * /marketdevice command implementation
 * Grants special Market Link Device items to players
 */
public class MarketDeviceCommand implements CommandExecutor, TabCompleter {
    
    private static final String PERMISSION_GIVE = "maksy.stocks.marketdevice.give";
    private static final String DEVICE_KEY = "maksy:market_device";
    private static final String OWNER_KEY = "owner_uuid";
    private static final String VERSION_KEY = "version";
    
    private final JavaPlugin plugin;
    private final TranslationManager translations;
    private final NamespacedKey deviceKey;
    private final NamespacedKey ownerKey;
    private final NamespacedKey versionKey;
    
    public MarketDeviceCommand(JavaPlugin plugin, TranslationManager translations) {
        this.plugin = plugin;
        this.translations = translations;
        this.deviceKey = new NamespacedKey(plugin, "market_device");
        this.ownerKey = new NamespacedKey(plugin, "owner_uuid");
        this.versionKey = new NamespacedKey(plugin, "version");
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // Check permission
        if (!sender.hasPermission(PERMISSION_GIVE)) {
            sender.sendMessage(translations.getMessage("commands.marketdevice.no_permission"));
            return true;
        }
        
        // Handle subcommands
        if (args.length == 0 || !args[0].equalsIgnoreCase("give")) {
            showUsage(sender);
            return true;
        }
        
        if (args[0].equalsIgnoreCase("give")) {
            handleGiveCommand(sender, args);
            return true;
        }
        
        showUsage(sender);
        return true;
    }
    
    /**
     * Handles the /marketdevice give [player] subcommand
     */
    private void handleGiveCommand(CommandSender sender, String[] args) {
        Player targetPlayer;
        
        if (args.length == 1) {
            // No target specified, give to sender if they're a player
            if (!(sender instanceof Player)) {
                sender.sendMessage(translations.getMessage("commands.marketdevice.usage"));
                return;
            }
            targetPlayer = (Player) sender;
        } else {
            // Target player specified
            String targetName = args[1];
            targetPlayer = Bukkit.getPlayer(targetName);
            
            if (targetPlayer == null) {
                sender.sendMessage(translations.getMessage("commands.marketdevice.player_not_found", "player", targetName));
                return;
            }
        }
        
        // Create and give the device
        ItemStack device = createMarketDevice(targetPlayer);
        targetPlayer.getInventory().addItem(device);
        
        // Send messages
        if (sender.equals(targetPlayer)) {
            sender.sendMessage(translations.getMessage("commands.marketdevice.self_given"));
        } else {
            sender.sendMessage(translations.getMessage("commands.marketdevice.other_given", "player", targetPlayer.getName()));
            targetPlayer.sendMessage(translations.getMessage("market.device.given", "player", targetPlayer.getName()));
        }
    }
    
    /**
     * Creates a Market Link Device item with proper NBT data
     */
    public ItemStack createMarketDevice(Player owner) {
        ItemStack device = new ItemStack(Material.RECOVERY_COMPASS);
        ItemMeta meta = device.getItemMeta();
        
        if (meta != null) {
            // Set display name
            meta.setDisplayName(translations.getMessage("market.device.name"));
            
            // Set lore
            List<String> lore = Arrays.asList(
                translations.getMessage("market.device.lore.usage"),
                translations.getMessage("market.device.lore.bound", "player", owner.getName())
            );
            meta.setLore(lore);
            
            // Set persistent data
            meta.getPersistentDataContainer().set(deviceKey, PersistentDataType.BOOLEAN, true);
            meta.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, owner.getUniqueId().toString());
            meta.getPersistentDataContainer().set(versionKey, PersistentDataType.INTEGER, 1);
            
            device.setItemMeta(meta);
        }
        
        return device;
    }
    
    /**
     * Checks if an item is a Market Device
     */
    public boolean isMarketDevice(ItemStack item) {
        if (item == null || item.getItemMeta() == null) {
            return false;
        }
        
        return item.getItemMeta().getPersistentDataContainer().has(deviceKey, PersistentDataType.BOOLEAN);
    }
    
    /**
     * Gets the owner UUID of a Market Device
     */
    public UUID getDeviceOwner(ItemStack item) {
        if (!isMarketDevice(item)) {
            return null;
        }
        
        String ownerString = item.getItemMeta().getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        if (ownerString == null) {
            return null;
        }
        
        try {
            return UUID.fromString(ownerString);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    /**
     * Shows command usage information
     */
    private void showUsage(CommandSender sender) {
        sender.sendMessage(translations.getMessage("commands.marketdevice.usage"));
    }
    
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (!sender.hasPermission(PERMISSION_GIVE)) {
            return completions;
        }
        
        if (args.length == 1) {
            if ("give".startsWith(args[0].toLowerCase())) {
                completions.add("give");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            // Tab complete player names
            String prefix = args[1].toLowerCase();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(prefix)) {
                    completions.add(player.getName());
                }
            }
        }
        
        return completions;
    }
}