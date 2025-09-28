package com.example.quickstocks.commands;

import com.example.quickstocks.I18n;
import com.example.quickstocks.utils.TranslationManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Command handler for Market Link Device items.
 */
public class MarketDeviceCommand implements CommandExecutor, TabCompleter {
    
    private final JavaPlugin plugin;
    private final TranslationManager translationManager;
    
    public MarketDeviceCommand(JavaPlugin plugin, TranslationManager translationManager) {
        this.plugin = plugin;
        this.translationManager = translationManager;
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("maksy.stocks.marketdevice.give")) {
            sender.sendMessage(I18n.component("market_device.no_permission"));
            return true;
        }
        
        if (args.length == 0) {
            showUsage(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        if ("give".equals(subCommand)) {
            handleGive(sender, args);
        } else {
            showUsage(sender);
        }
        
        return true;
    }
    
    private void handleGive(CommandSender sender, String[] args) {
        Player target;
        
        if (args.length < 2) {
            if (sender instanceof Player) {
                target = (Player) sender;
            } else {
                showUsage(sender);
                return;
            }
        } else {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                Map<String, Object> placeholders = Map.of("player", args[1]);
                sender.sendMessage(I18n.component("market_device.player_not_found", placeholders));
                return;
            }
        }
        
        // Create Market Link Device item
        ItemStack device = createMarketDevice();
        target.getInventory().addItem(device);
        
        // Send messages
        target.sendMessage(I18n.component("market_device.device_received"));
        
        if (!target.equals(sender)) {
            Map<String, Object> placeholders = Map.of("player", target.getName());
            sender.sendMessage(I18n.component("market_device.other_given", placeholders));
        } else {
            Map<String, Object> placeholders = Map.of("player", target.getName());
            sender.sendMessage(I18n.component("market_device.device_given", placeholders));
        }
    }
    
    private ItemStack createMarketDevice() {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName("§bMarket Link Device");
            meta.setLore(Arrays.asList(
                "§7Right-click to access the market",
                "§7from anywhere in the world!"
            ));
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    private void showUsage(CommandSender sender) {
        sender.sendMessage("§6Usage: /marketdevice give [player]");
    }
    
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.add("give");
        } else if (args.length == 2 && "give".equalsIgnoreCase(args[0])) {
            // Add online player names
            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }
        }
        
        return completions;
    }
}