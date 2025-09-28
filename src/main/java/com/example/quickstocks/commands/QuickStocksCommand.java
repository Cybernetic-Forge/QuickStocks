package com.example.quickstocks.commands;

import com.example.quickstocks.I18n;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Main QuickStocks command for administration tasks.
 * Handles /quickstocks reload and other admin functions.
 */
public class QuickStocksCommand implements CommandExecutor, TabCompleter {
    
    private static final String PERMISSION_RELOAD = "quickstocks.admin.reload";
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            showUsage(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "reload":
                handleReloadCommand(sender);
                break;
            default:
                showUsage(sender);
                break;
        }
        
        return true;
    }
    
    /**
     * Handles the reload subcommand.
     */
    private void handleReloadCommand(CommandSender sender) {
        // Check permission
        if (sender instanceof Player player && !player.hasPermission(PERMISSION_RELOAD)) {
            sender.sendMessage(I18n.component("general.no_permission"));
            return;
        }
        
        try {
            // Reload translations
            I18n.reload();
            sender.sendMessage(I18n.component("general.translations_reloaded"));
        } catch (Exception e) {
            sender.sendMessage(I18n.component("general.database_error", 
                java.util.Map.of("error", "Failed to reload: " + e.getMessage())));
        }
    }
    
    /**
     * Shows command usage.
     */
    private void showUsage(CommandSender sender) {
        sender.sendMessage(I18n.component("quickstocks.usage_title"));
        sender.sendMessage(I18n.component("quickstocks.reload_usage"));
        sender.sendMessage(I18n.component("quickstocks.reload_description"));
    }
    
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            if ("reload".startsWith(partial)) {
                completions.add("reload");
            }
        }
        
        return completions;
    }
}