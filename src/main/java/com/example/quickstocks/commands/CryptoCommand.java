package com.example.quickstocks.commands;

import com.example.quickstocks.I18n;
import com.example.quickstocks.core.services.CryptoService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * /crypto command implementation for creating custom cryptocurrency instruments.
 * Requires the permission 'maksy.stocks.crypto.create' to create crypto.
 */
public class CryptoCommand implements CommandExecutor, TabCompleter {
    
    private static final String PERMISSION_CREATE = "maksy.stocks.crypto.create";
    
    private final CryptoService cryptoService;
    
    public CryptoCommand(CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            // Show usage
            showUsage(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "create":
                handleCreateCommand(sender, Arrays.copyOfRange(args, 1, args.length));
                break;
            default:
                showUsage(sender);
                break;
        }
        
        return true;
    }
    
    /**
     * Handles the /crypto create subcommand.
     */
    private void handleCreateCommand(CommandSender sender, String[] args) {
        // Check if sender is a player
        if (!(sender instanceof Player player)) {
            sender.sendMessage(I18n.component("crypto.only_players"));
            return;
        }
        
        // Check permission
        if (!player.hasPermission(PERMISSION_CREATE)) {
            sender.sendMessage(I18n.component("crypto.no_permission"));
            Map<String, Object> placeholders = Map.of("permission", PERMISSION_CREATE);
            sender.sendMessage(I18n.component("crypto.permission_required", placeholders));
            return;
        }
        
        // Validate arguments
        if (args.length < 2) {
            sender.sendMessage(I18n.component("crypto.usage_error"));
            sender.sendMessage(I18n.component("crypto.usage_example"));
            return;
        }
        
        String symbol = args[0];
        String displayName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        
        try {
            // Create the custom crypto
            String instrumentId = cryptoService.createCustomCrypto(symbol, displayName, player.getUniqueId().toString());
            
            // Success message
            sender.sendMessage(Component.text(""));
            sender.sendMessage(I18n.component("crypto.success_title"));
            sender.sendMessage(Component.text("━".repeat(40), NamedTextColor.GRAY));
            
            Map<String, Object> symbolPlaceholders = Map.of("symbol", symbol.toUpperCase());
            sender.sendMessage(I18n.component("crypto.success_symbol", symbolPlaceholders));
            
            Map<String, Object> namePlaceholders = Map.of("name", displayName);
            sender.sendMessage(I18n.component("crypto.success_name", namePlaceholders));
            
            sender.sendMessage(I18n.component("crypto.success_price"));
            
            Map<String, Object> idPlaceholders = Map.of("id", instrumentId);
            sender.sendMessage(I18n.component("crypto.success_id", idPlaceholders));
            
            sender.sendMessage(Component.text("━".repeat(40), NamedTextColor.GRAY));
            sender.sendMessage(I18n.component("crypto.success_footer1"));
            
            Map<String, Object> footerPlaceholders = Map.of("symbol", symbol.toUpperCase());
            sender.sendMessage(I18n.component("crypto.success_footer2", footerPlaceholders));
            
        } catch (IllegalArgumentException e) {
            // Handle specific validation errors
            if (e.getMessage().contains("Invalid symbol")) {
                sender.sendMessage(I18n.component("crypto.invalid_symbol"));
            } else if (e.getMessage().contains("already exists")) {
                Map<String, Object> placeholders = Map.of("symbol", symbol);
                sender.sendMessage(I18n.component("crypto.symbol_exists", placeholders));
            } else {
                Map<String, Object> placeholders = Map.of("error", e.getMessage());
                sender.sendMessage(I18n.component("crypto.creation_error", placeholders));
            }
        } catch (SQLException e) {
            Map<String, Object> placeholders = Map.of("error", e.getMessage());
            sender.sendMessage(I18n.component("crypto.creation_error", placeholders));
        } catch (Exception e) {
            Map<String, Object> placeholders = Map.of("error", e.getMessage());
            sender.sendMessage(I18n.component("crypto.creation_error", placeholders));
        }
    }
    
    /**
     * Shows command usage information.
     */
    private void showUsage(CommandSender sender) {
        sender.sendMessage(Component.text(""));
        sender.sendMessage(I18n.component("crypto.title"));
        sender.sendMessage(Component.text("━".repeat(30), NamedTextColor.GRAY));
        
        sender.sendMessage(I18n.component("crypto.create_usage"));
        sender.sendMessage(I18n.component("crypto.create_description"));
        sender.sendMessage(Component.text(""));
        
        sender.sendMessage(I18n.component("crypto.examples_title"));
        sender.sendMessage(I18n.component("crypto.example1"));
        sender.sendMessage(I18n.component("crypto.example2"));
        sender.sendMessage(Component.text(""));
        
        if (sender instanceof Player player) {
            boolean hasPermission = player.hasPermission(PERMISSION_CREATE);
            String status = hasPermission ? I18n.tr("crypto.permission_granted") : I18n.tr("crypto.permission_denied");
            
            Map<String, Object> placeholders = Map.of(
                "permission", PERMISSION_CREATE,
                "status", status
            );
            sender.sendMessage(I18n.component("crypto.permission_status", placeholders));
        }
    }
    
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // First argument - subcommands
            String partial = args[0].toLowerCase();
            if ("create".startsWith(partial)) {
                completions.add("create");
            }
        } else if (args.length == 2 && "create".equalsIgnoreCase(args[0])) {
            // Second argument for create - suggest symbol format
            completions.add("<SYMBOL>");
        } else if (args.length == 3 && "create".equalsIgnoreCase(args[0])) {
            // Third argument for create - suggest name format
            completions.add("\"Display Name\"");
        }
        
        return completions;
    }
}