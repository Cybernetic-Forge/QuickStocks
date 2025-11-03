package net.cyberneticforge.quickstocks.commands;

import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.infrastructure.logging.PluginLogger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Command handler for QuickStocks plugin management (/quickstocks).
 * Provides reload functionality for all configurations and services.
 */
public class QuickStocksCommand implements CommandExecutor, TabCompleter {
    
    /**
     * Gets the current logger instance dynamically to support logger reinitialization during reload.
     */
    private PluginLogger getLogger() {
        return QuickStocksPlugin.getPluginLogger();
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }
        
        String subcommand = args[0].toLowerCase();
        
        switch (subcommand) {
            case "reload":
                if (!sender.hasPermission("quickstocks.admin.reload")) {
                    sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
                    return true;
                }
                reloadPlugin(sender);
                break;
                
            default:
                sendUsage(sender);
                break;
        }
        
        return true;
    }
    
    /**
     * Reloads all plugin configurations and restarts services.
     */
    private void reloadPlugin(CommandSender sender) {
        sender.sendMessage(Component.text("Reloading QuickStocks...", NamedTextColor.YELLOW));
        
        try {
            QuickStocksPlugin plugin = QuickStocksPlugin.getInstance();
            
            // Stop running services
            sender.sendMessage(Component.text("Stopping services...", NamedTextColor.GRAY));
            
            // Stop simulation engine
            if (QuickStocksPlugin.getSimulationEngine() != null) {
                QuickStocksPlugin.getSimulationEngine().stop();
                getLogger().info("Simulation engine stopped for reload");
            }
            
            // Cancel all Bukkit scheduler tasks for this plugin
            plugin.getServer().getScheduler().cancelTasks(plugin);
            getLogger().info("All scheduled tasks cancelled for reload");
            
            // Reload main config
            plugin.reloadConfig();
            sender.sendMessage(Component.text("Reloaded config.yml", NamedTextColor.GRAY));
            
            // Reinitialize logger with new debug level
            plugin.reinitializeLogger();
            
            // Reload all configuration files
            sender.sendMessage(Component.text("Reloading configuration files...", NamedTextColor.GRAY));
            
            if (QuickStocksPlugin.getMarketCfg() != null) {
                QuickStocksPlugin.getMarketCfg().reload();
                getLogger().info("Market configuration reloaded");
            }
            
            if (QuickStocksPlugin.getTradingCfg() != null) {
                QuickStocksPlugin.getTradingCfg().reload();
                getLogger().info("Trading configuration reloaded");
            }
            
            if (QuickStocksPlugin.getCompanyCfg() != null) {
                QuickStocksPlugin.getCompanyCfg().reload();
                getLogger().info("Company configuration reloaded");
            }
            
            if (QuickStocksPlugin.getGuiConfig() != null) {
                QuickStocksPlugin.getGuiConfig().reload();
                getLogger().info("GUI configuration reloaded");
            }
            
            sender.sendMessage(Component.text("Configuration files reloaded", NamedTextColor.GRAY));
            
            // Restart services
            sender.sendMessage(Component.text("Restarting services...", NamedTextColor.GRAY));
            
            // Restart simulation engine
            if (QuickStocksPlugin.getSimulationEngine() != null) {
                QuickStocksPlugin.getSimulationEngine().start();
                getLogger().info("Simulation engine restarted");
            }
            
            // Restart salary payment scheduler
            plugin.startSalaryPaymentScheduler();
            getLogger().info("Salary payment scheduler restarted");
            
            // Restart rent collection scheduler
            plugin.startRentCollectionScheduler();
            getLogger().info("Rent collection scheduler restarted");
            
            sender.sendMessage(Component.text("QuickStocks reloaded successfully!", NamedTextColor.GREEN));
            getLogger().info("QuickStocks reload completed successfully");
            
        } catch (Exception e) {
            sender.sendMessage(Component.text("Error reloading QuickStocks: " + e.getMessage(), NamedTextColor.RED));
            getLogger().severe("Failed to reload QuickStocks: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Sends command usage information to the sender.
     */
    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text("QuickStocks Commands:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/quickstocks reload", NamedTextColor.YELLOW)
            .append(Component.text(" - Reload all configurations and restart services", NamedTextColor.GRAY)));
    }
    
    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            if (sender.hasPermission("quickstocks.admin.reload")) {
                completions.add("reload");
            }
        }
        
        // Filter based on what the user has typed so far
        String input = args[args.length - 1].toLowerCase();
        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(input))
            .toList();
    }
}
