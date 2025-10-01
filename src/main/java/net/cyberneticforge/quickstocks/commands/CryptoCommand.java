package net.cyberneticforge.quickstocks.commands;

import net.cyberneticforge.quickstocks.core.services.CryptoService;
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
            sender.sendMessage(Component.text("❌ Only players can create custom crypto.", NamedTextColor.RED));
            return;
        }
        
        // Check permission
        if (!player.hasPermission(PERMISSION_CREATE)) {
            sender.sendMessage(Component.text("❌ You don't have permission to create custom crypto.", NamedTextColor.RED));
            sender.sendMessage(Component.text("💡 Required permission: " + PERMISSION_CREATE, NamedTextColor.GRAY));
            return;
        }
        
        // Validate arguments
        if (args.length < 2) {
            sender.sendMessage(Component.text("❌ Usage: /crypto create <symbol> <name>", NamedTextColor.RED));
            sender.sendMessage(Component.text("💡 Example: /crypto create MYCOIN \"My Custom Coin\"", NamedTextColor.GRAY));
            return;
        }
        
        String symbol = args[0];
        String displayName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        
        try {
            // Create the custom crypto
            String instrumentId = cryptoService.createCustomCrypto(symbol, displayName, player.getUniqueId().toString());
            
            // Success message
            sender.sendMessage(Component.text(""));
            sender.sendMessage(Component.text("🎉 Custom Crypto Created Successfully!", NamedTextColor.GREEN, TextDecoration.BOLD));
            sender.sendMessage(Component.text("━".repeat(40), NamedTextColor.GRAY));
            
            sender.sendMessage(Component.text()
                    .append(Component.text("💰 Symbol: ", NamedTextColor.YELLOW))
                    .append(Component.text(symbol.toUpperCase(), NamedTextColor.DARK_AQUA, TextDecoration.BOLD))
                    .build());
            
            sender.sendMessage(Component.text()
                    .append(Component.text("📝 Name: ", NamedTextColor.YELLOW))
                    .append(Component.text(displayName, NamedTextColor.WHITE))
                    .build());
            
            sender.sendMessage(Component.text()
                    .append(Component.text("💲 Starting Price: ", NamedTextColor.YELLOW))
                    .append(Component.text("$1.00", NamedTextColor.GOLD))
                    .build());
            
            sender.sendMessage(Component.text()
                    .append(Component.text("🆔 Instrument ID: ", NamedTextColor.YELLOW))
                    .append(Component.text(instrumentId, NamedTextColor.DARK_GRAY))
                    .build());
            
            sender.sendMessage(Component.text("━".repeat(40), NamedTextColor.GRAY));
            sender.sendMessage(Component.text("💡 Your crypto is now tradeable on the market!", NamedTextColor.GREEN));
            sender.sendMessage(Component.text("💡 Use /stocks " + symbol.toUpperCase() + " to view details", NamedTextColor.GRAY));
            
        } catch (IllegalArgumentException e) {
            sender.sendMessage(Component.text("❌ " + e.getMessage(), NamedTextColor.RED));
        } catch (SQLException e) {
            sender.sendMessage(Component.text("❌ Database error: " + e.getMessage(), NamedTextColor.RED));
        } catch (Exception e) {
            sender.sendMessage(Component.text("❌ Unexpected error: " + e.getMessage(), NamedTextColor.RED));
        }
    }
    
    /**
     * Shows command usage information.
     */
    private void showUsage(CommandSender sender) {
        sender.sendMessage(Component.text(""));
        sender.sendMessage(Component.text("🪙 Crypto Commands", NamedTextColor.GOLD, TextDecoration.BOLD));
        sender.sendMessage(Component.text("━".repeat(30), NamedTextColor.GRAY));
        
        sender.sendMessage(Component.text()
                .append(Component.text("• /crypto create ", NamedTextColor.YELLOW))
                .append(Component.text("<symbol> <name>", NamedTextColor.AQUA))
                .build());
        
        sender.sendMessage(Component.text("  Creates a custom cryptocurrency", NamedTextColor.GRAY));
        sender.sendMessage(Component.text(""));
        
        sender.sendMessage(Component.text("Examples:", NamedTextColor.WHITE, TextDecoration.BOLD));
        sender.sendMessage(Component.text("  /crypto create MYCOIN \"My Custom Coin\"", NamedTextColor.DARK_AQUA));
        sender.sendMessage(Component.text("  /crypto create GOLD \"Digital Gold\"", NamedTextColor.DARK_AQUA));
        sender.sendMessage(Component.text(""));
        
        if (sender instanceof Player player) {
            boolean hasPermission = player.hasPermission(PERMISSION_CREATE);
            Component permissionStatus = Component.text()
                    .append(Component.text("Permission: ", NamedTextColor.YELLOW))
                    .append(Component.text(PERMISSION_CREATE, NamedTextColor.GRAY))
                    .append(Component.text(" - ", NamedTextColor.GRAY))
                    .append(Component.text(hasPermission ? "✅ Granted" : "❌ Denied", 
                            hasPermission ? NamedTextColor.GREEN : NamedTextColor.RED))
                    .build();
            
            sender.sendMessage(permissionStatus);
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