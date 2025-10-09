package net.cyberneticforge.quickstocks.commands;

import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.core.enums.Translation;
import net.cyberneticforge.quickstocks.core.model.Replaceable;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Command handler for wallet operations (/wallet).
 */
public class WalletCommand implements CommandExecutor, TabCompleter {
    
    private static final Logger logger = Logger.getLogger(WalletCommand.class.getName());
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            Translation.NoConsoleSender.sendMessage(sender);
            return true;
        }

        String playerUuid = player.getUniqueId().toString();
        
        try {
            if (args.length == 0) {
                // Show balance
                showBalance(player, playerUuid);
                return true;
            }
            
            String subcommand = args[0].toLowerCase();
            
            switch (subcommand) {
                case "balance":
                case "bal":
                    showBalance(player, playerUuid);
                    break;
                    
                case "add":
                    if (args.length < 2) {
                        Translation.Wallet_Usage.sendMessage(player);
                        return true;
                    }
                    
                    if (!player.hasPermission("quickstocks.wallet.add")) {
                        Translation.NoPermission.sendMessage(player);
                        return true;
                    }
                    
                    try {
                        double amount = Double.parseDouble(args[1]);
                        if (amount <= 0) {
                            Translation.Wallet_Error_InvalidAmount.sendMessage(player);
                            return true;
                        }

                        QuickStocksPlugin.getWalletService().addBalance(playerUuid, amount);
                        double newBalance = QuickStocksPlugin.getWalletService().getBalance(playerUuid);
                        
                        Translation.Wallet_Deposit.sendMessage(player,
                            new Replaceable("%amount%", String.format("%.2f", amount)),
                            new Replaceable("%balance%", String.format("%.2f", newBalance)));
                        
                    } catch (NumberFormatException e) {
                        Translation.Wallet_Error_InvalidAmount.sendMessage(player);
                    }
                    break;
                    
                case "set":
                    if (args.length < 2) {
                        Translation.Wallet_Usage.sendMessage(player);
                        return true;
                    }
                    
                    if (!player.hasPermission("quickstocks.wallet.set")) {
                        Translation.NoPermission.sendMessage(player);
                        return true;
                    }
                    
                    try {
                        double amount = Double.parseDouble(args[1]);
                        if (amount < 0) {
                            Translation.Wallet_Error_InvalidAmount.sendMessage(player);
                            return true;
                        }

                        QuickStocksPlugin.getWalletService().setBalance(playerUuid, amount);
                        
                        Translation.Wallet_Balance.sendMessage(player,
                            new Replaceable("%balance%", String.format("%.2f", amount)));
                        
                    } catch (NumberFormatException e) {
                        Translation.Wallet_Error_InvalidAmount.sendMessage(player);
                    }
                    break;
                    
                default:
                    Translation.Wallet_UnknownSubcommand.sendMessage(player);
                    break;
            }
            
        } catch (Exception e) {
            logger.warning("Error in wallet command for " + player.getName() + ": " + e.getMessage());
            Translation.Wallet_ErrorProcessing.sendMessage(player);
        }
        
        return true;
    }
    
    private void showBalance(Player player, String playerUuid) throws Exception {
        double balance = QuickStocksPlugin.getWalletService().getBalance(playerUuid);
        
        Translation.Wallet_Balance.sendMessage(player,
            new Replaceable("%balance%", String.format("%.2f", balance)));
    }
    
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            return Stream.of("balance", "add", "set")
                .filter(option -> option.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        return null;
    }
}