package net.cyberneticforge.quickstocks.commands;

import net.cyberneticforge.quickstocks.core.services.WalletService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Command handler for wallet operations (/wallet).
 */
public class WalletCommand implements CommandExecutor, TabCompleter {
    
    private static final Logger logger = Logger.getLogger(WalletCommand.class.getName());
    
    private final WalletService walletService;
    
    public WalletCommand(WalletService walletService) {
        this.walletService = walletService;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }
        
        Player player = (Player) sender;
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
                        player.sendMessage(ChatColor.RED + "Usage: /wallet add <amount>");
                        return true;
                    }
                    
                    if (!player.hasPermission("quickstocks.wallet.add")) {
                        player.sendMessage(ChatColor.RED + "You don't have permission to add money.");
                        return true;
                    }
                    
                    try {
                        double amount = Double.parseDouble(args[1]);
                        if (amount <= 0) {
                            player.sendMessage(ChatColor.RED + "Amount must be positive.");
                            return true;
                        }
                        
                        walletService.addBalance(playerUuid, amount);
                        double newBalance = walletService.getBalance(playerUuid);
                        
                        player.sendMessage(ChatColor.GREEN + "Added $" + String.format("%.2f", amount) + 
                                         " to your wallet. New balance: $" + String.format("%.2f", newBalance));
                        
                    } catch (NumberFormatException e) {
                        player.sendMessage(ChatColor.RED + "Invalid amount: " + args[1]);
                    }
                    break;
                    
                case "set":
                    if (args.length < 2) {
                        player.sendMessage(ChatColor.RED + "Usage: /wallet set <amount>");
                        return true;
                    }
                    
                    if (!player.hasPermission("quickstocks.wallet.set")) {
                        player.sendMessage(ChatColor.RED + "You don't have permission to set wallet balance.");
                        return true;
                    }
                    
                    try {
                        double amount = Double.parseDouble(args[1]);
                        if (amount < 0) {
                            player.sendMessage(ChatColor.RED + "Amount cannot be negative.");
                            return true;
                        }
                        
                        walletService.setBalance(playerUuid, amount);
                        
                        player.sendMessage(ChatColor.GREEN + "Set wallet balance to $" + String.format("%.2f", amount));
                        
                    } catch (NumberFormatException e) {
                        player.sendMessage(ChatColor.RED + "Invalid amount: " + args[1]);
                    }
                    break;
                    
                default:
                    player.sendMessage(ChatColor.RED + "Unknown subcommand. Usage: /wallet [balance|add|set] [amount]");
                    break;
            }
            
        } catch (Exception e) {
            logger.warning("Error in wallet command for " + player.getName() + ": " + e.getMessage());
            player.sendMessage(ChatColor.RED + "An error occurred while processing your wallet command.");
        }
        
        return true;
    }
    
    private void showBalance(Player player, String playerUuid) throws Exception {
        double balance = walletService.getBalance(playerUuid);
        
        player.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + "Wallet Balance" + ChatColor.GOLD + " ===");
        player.sendMessage(ChatColor.YELLOW + "Balance: " + ChatColor.GREEN + "$" + String.format("%.2f", balance));
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("balance", "add", "set")
                .stream()
                .filter(option -> option.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        return null;
    }
}