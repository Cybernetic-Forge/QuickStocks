package net.cyberneticforge.quickstocks.commands;

import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.application.queries.QueryService;
import net.cyberneticforge.quickstocks.core.services.WatchlistService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Command handler for watchlist operations (/watch).
 */
public class WatchCommand implements CommandExecutor, TabCompleter {
    
    private static final Logger logger = Logger.getLogger(WatchCommand.class.getName());
    
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
                // Show watchlist overview
                showWatchlist(player, playerUuid);
                return true;
            }
            
            String subcommand = args[0].toLowerCase();
            
            switch (subcommand) {
                case "add":
                    if (args.length < 2) {
                        player.sendMessage(ChatColor.RED + "Usage: /watch add <symbol>");
                        return true;
                    }
                    handleAddToWatchlist(player, playerUuid, args[1]);
                    break;
                    
                case "remove":
                case "rem":
                case "delete":
                case "del":
                    if (args.length < 2) {
                        player.sendMessage(ChatColor.RED + "Usage: /watch remove <symbol>");
                        return true;
                    }
                    handleRemoveFromWatchlist(player, playerUuid, args[1]);
                    break;
                    
                case "list":
                case "show":
                    showWatchlist(player, playerUuid);
                    break;
                    
                case "info":
                case "details":
                    if (args.length < 2) {
                        player.sendMessage(ChatColor.RED + "Usage: /watch info <symbol>");
                        return true;
                    }
                    showWatchlistItemDetails(player, playerUuid, args[1]);
                    break;
                    
                case "clear":
                    handleClearWatchlist(player, playerUuid);
                    break;
                    
                default:
                    player.sendMessage(ChatColor.RED + "Unknown subcommand. Usage: /watch [add|remove|list|info|clear] [symbol]");
                    break;
            }
            
        } catch (Exception e) {
            logger.warning("Error in watch command for " + player.getName() + ": " + e.getMessage());
            player.sendMessage(ChatColor.RED + "An error occurred while processing your watchlist command.");
        }
        
        return true;
    }
    
    private void handleAddToWatchlist(Player player, String playerUuid, String symbol) throws Exception {
        // Get instrument ID from symbol
        String instrumentId = QuickStocksPlugin.getQueryService().getInstrumentIdBySymbol(symbol.toUpperCase());
        if (instrumentId == null) {
            player.sendMessage(ChatColor.RED + "Instrument not found: " + symbol);
            return;
        }
        
        boolean added = QuickStocksPlugin.getWatchlistService().addToWatchlist(playerUuid, instrumentId);
        if (added) {
            // Get instrument display name for confirmation
            String displayName = QuickStocksPlugin.getQueryService().getInstrumentDisplayName(instrumentId);
            player.sendMessage(ChatColor.GREEN + "✓ Added " + ChatColor.WHITE + symbol.toUpperCase() + 
                              ChatColor.GRAY + " (" + displayName + ")" + ChatColor.GREEN + " to your watchlist.");
        } else {
            player.sendMessage(ChatColor.YELLOW + "⚠ " + symbol.toUpperCase() + " is already in your watchlist.");
        }
    }
    
    private void handleRemoveFromWatchlist(Player player, String playerUuid, String symbol) throws Exception {
        // Get instrument ID from symbol
        String instrumentId = QuickStocksPlugin.getQueryService().getInstrumentIdBySymbol(symbol.toUpperCase());
        if (instrumentId == null) {
            player.sendMessage(ChatColor.RED + "Instrument not found: " + symbol);
            return;
        }
        
        boolean removed = QuickStocksPlugin.getWatchlistService().removeFromWatchlist(playerUuid, instrumentId);
        if (removed) {
            player.sendMessage(ChatColor.GREEN + "✓ Removed " + ChatColor.WHITE + symbol.toUpperCase() + 
                              ChatColor.GREEN + " from your watchlist.");
        } else {
            player.sendMessage(ChatColor.YELLOW + "⚠ " + symbol.toUpperCase() + " was not in your watchlist.");
        }
    }
    
    private void showWatchlist(Player player, String playerUuid) throws Exception {
        List<WatchlistService.WatchlistItem> watchlist = QuickStocksPlugin.getWatchlistService().getWatchlist(playerUuid);
        
        player.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + "Your Watchlist" + ChatColor.GOLD + " ===");
        
        if (watchlist.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "Your watchlist is empty.");
            player.sendMessage(ChatColor.GRAY + "Use " + ChatColor.WHITE + "/watch add <symbol>" + 
                              ChatColor.GRAY + " to add instruments to your watchlist.");
            return;
        }
        
        player.sendMessage(ChatColor.YELLOW + "Watching " + watchlist.size() + " instruments:");
        player.sendMessage(""); // Empty line for readability
        
        for (WatchlistService.WatchlistItem item : watchlist) {
            // Format price change colors and arrows
            ChatColor changeColor = item.getChange24h() >= 0 ? ChatColor.GREEN : ChatColor.RED;
            String changeArrow = item.getChange24h() >= 0 ? "▲" : "▼";
            
            // Format the display line
            player.sendMessage(String.format(
                ChatColor.WHITE + "%s " + ChatColor.GRAY + "(%s) " + 
                ChatColor.YELLOW + "$%.2f " + changeColor + "%s%.2f%%" + 
                ChatColor.DARK_GRAY + " | 1h: " + changeColor + "%+.2f%%",
                item.getSymbol(),
                item.getDisplayName(),
                item.getLastPrice(),
                changeArrow,
                Math.abs(item.getChange24h()),
                item.getChange1h()
            ));
        }
        
        player.sendMessage(""); // Empty line
        player.sendMessage(ChatColor.GRAY + "Use " + ChatColor.WHITE + "/watch info <symbol>" + 
                          ChatColor.GRAY + " for detailed information.");
    }
    
    private void showWatchlistItemDetails(Player player, String playerUuid, String symbol) throws Exception {
        // Get instrument ID from symbol
        String instrumentId = QuickStocksPlugin.getQueryService().getInstrumentIdBySymbol(symbol.toUpperCase());
        if (instrumentId == null) {
            player.sendMessage(ChatColor.RED + "Instrument not found: " + symbol);
            return;
        }
        
        // Check if it's in watchlist
        if (!QuickStocksPlugin.getWatchlistService().isInWatchlist(playerUuid, instrumentId)) {
            player.sendMessage(ChatColor.YELLOW + "⚠ " + symbol.toUpperCase() + " is not in your watchlist.");
            player.sendMessage(ChatColor.GRAY + "Use " + ChatColor.WHITE + "/watch add " + symbol.toLowerCase() + 
                              ChatColor.GRAY + " to add it to your watchlist.");
            return;
        }
        
        // Get detailed information
        List<WatchlistService.WatchlistItem> watchlist = QuickStocksPlugin.getWatchlistService().getWatchlist(playerUuid);
        WatchlistService.WatchlistItem item = watchlist.stream()
            .filter(i -> i.getSymbol().equalsIgnoreCase(symbol))
            .findFirst()
            .orElse(null);
            
        if (item == null) {
            player.sendMessage(ChatColor.RED + "Error retrieving watchlist item details.");
            return;
        }
        
        // Format the detailed display
        player.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + item.getSymbol() + " Details" + ChatColor.GOLD + " ===");
        player.sendMessage(ChatColor.YELLOW + "Name: " + ChatColor.WHITE + item.getDisplayName());
        player.sendMessage(ChatColor.YELLOW + "Type: " + ChatColor.WHITE + item.getType());
        player.sendMessage(ChatColor.YELLOW + "Current Price: " + ChatColor.WHITE + "$" + String.format("%.2f", item.getLastPrice()));
        
        // Change indicators with colors
        ChatColor change24hColor = item.getChange24h() >= 0 ? ChatColor.GREEN : ChatColor.RED;
        ChatColor change1hColor = item.getChange1h() >= 0 ? ChatColor.GREEN : ChatColor.RED;
        String arrow24h = item.getChange24h() >= 0 ? "▲" : "▼";
        String arrow1h = item.getChange1h() >= 0 ? "▲" : "▼";
        
        player.sendMessage(ChatColor.YELLOW + "24h Change: " + change24hColor + arrow24h + String.format("%.2f%%", item.getChange24h()));
        player.sendMessage(ChatColor.YELLOW + "1h Change: " + change1hColor + arrow1h + String.format("%.2f%%", item.getChange1h()));
        player.sendMessage(ChatColor.YELLOW + "Volatility: " + ChatColor.WHITE + String.format("%.2f", item.getVolatility24h()));
        
        // Show when added
        Date addedDate = new Date(item.getAddedAt());
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm");
        player.sendMessage(ChatColor.YELLOW + "Added to watchlist: " + ChatColor.GRAY + sdf.format(addedDate));
    }
    
    private void handleClearWatchlist(Player player, String playerUuid) throws Exception {
        int count = QuickStocksPlugin.getWatchlistService().getWatchlistCount(playerUuid);
        if (count == 0) {
            player.sendMessage(ChatColor.GRAY + "Your watchlist is already empty.");
            return;
        }
        
        int removed = QuickStocksPlugin.getWatchlistService().clearWatchlist(playerUuid);
        player.sendMessage(ChatColor.GREEN + "✓ Cleared your watchlist (" + removed + " items removed).");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("add", "remove", "list", "info", "clear")
                    .stream()
                    .filter(option -> option.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        
        if (args.length == 2 && (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("info"))) {
            // For add/remove/info commands, suggest instrument symbols
            try {
                return QuickStocksPlugin.getQueryService().getInstrumentSymbols()
                        .stream()
                        .filter(symbol -> symbol.toLowerCase().startsWith(args[1].toLowerCase()))
                        .limit(20)
                        .collect(Collectors.toList());
            } catch (Exception e) {
                logger.warning("Error getting symbols for tab completion: " + e.getMessage());
                return null;
            }
        }
        
        return null;
    }
}