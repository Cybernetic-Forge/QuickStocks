package com.example.quickstocks.commands;

import com.example.quickstocks.I18n;
import com.example.quickstocks.core.services.WalletService;
import org.bukkit.Bukkit;
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
 * Command handler for wallet operations.
 */
public class WalletCommand implements CommandExecutor, TabCompleter {
    
    private final WalletService walletService;
    
    public WalletCommand(WalletService walletService) {
        this.walletService = walletService;
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(I18n.component("general.only_players"));
            return true;
        }
        
        if (args.length == 0) {
            // Show balance
            showBalance(player);
        } else {
            String subCommand = args[0].toLowerCase();
            switch (subCommand) {
                case "balance" -> showBalance(player);
                case "add" -> handleAdd(sender, args);
                case "set" -> handleSet(sender, args);
                default -> showUsage(sender);
            }
        }
        
        return true;
    }
    
    private void showBalance(Player player) {
        try {
            double balance = walletService.getBalance(player.getUniqueId().toString());
            Map<String, Object> placeholders = Map.of("balance", String.format("$%.2f", balance));
            player.sendMessage(I18n.component("market.balance", placeholders));
        } catch (SQLException e) {
            Map<String, Object> placeholders = Map.of("error", e.getMessage());
            player.sendMessage(I18n.component("errors.database", placeholders));
        }
    }
    
    private void handleAdd(CommandSender sender, String[] args) {
        if (!sender.hasPermission("quickstocks.wallet.add")) {
            sender.sendMessage(I18n.component("general.no_permission"));
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage(I18n.component("general.invalid_number"));
            return;
        }
        
        try {
            double amount = Double.parseDouble(args[1]);
            String targetPlayer = sender instanceof Player ? ((Player) sender).getUniqueId().toString() : null;
            
            if (args.length >= 3) {
                Player target = Bukkit.getPlayer(args[2]);
                if (target != null) {
                    targetPlayer = target.getUniqueId().toString();
                }
            }
            
            if (targetPlayer != null) {
                walletService.addBalance(targetPlayer, amount);
                Map<String, Object> placeholders = Map.of("balance", String.format("$%.2f", amount));
                sender.sendMessage(I18n.component("market.balance", placeholders));
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(I18n.component("general.invalid_number"));
        } catch (SQLException e) {
            Map<String, Object> placeholders = Map.of("error", e.getMessage());
            sender.sendMessage(I18n.component("errors.database", placeholders));
        }
    }
    
    private void handleSet(CommandSender sender, String[] args) {
        if (!sender.hasPermission("quickstocks.wallet.set")) {
            sender.sendMessage(I18n.component("general.no_permission"));
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage(I18n.component("general.invalid_number"));
            return;
        }
        
        try {
            double amount = Double.parseDouble(args[1]);
            String targetPlayer = sender instanceof Player ? ((Player) sender).getUniqueId().toString() : null;
            
            if (args.length >= 3) {
                Player target = Bukkit.getPlayer(args[2]);
                if (target != null) {
                    targetPlayer = target.getUniqueId().toString();
                }
            }
            
            if (targetPlayer != null) {
                walletService.setBalance(targetPlayer, amount);
                Map<String, Object> placeholders = Map.of("balance", String.format("$%.2f", amount));
                sender.sendMessage(I18n.component("market.balance", placeholders));
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(I18n.component("general.invalid_number"));
        } catch (SQLException e) {
            Map<String, Object> placeholders = Map.of("error", e.getMessage());
            sender.sendMessage(I18n.component("errors.database", placeholders));
        }
    }
    
    private void showUsage(CommandSender sender) {
        sender.sendMessage(I18n.component("market.balance"));
    }
    
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(Arrays.asList("balance", "add", "set"));
        }
        
        return completions;
    }
}