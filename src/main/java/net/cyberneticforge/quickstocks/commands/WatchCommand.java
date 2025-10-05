package net.cyberneticforge.quickstocks.commands;

import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.core.enums.Translation;
import net.cyberneticforge.quickstocks.core.model.Replaceable;
import net.cyberneticforge.quickstocks.core.services.WatchlistService;
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
            Translation.PlayersOnly.sendMessage(sender);
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
                        Translation.Watch_Usage_Add.sendMessage(player);
                        return true;
                    }
                    handleAddToWatchlist(player, playerUuid, args[1]);
                    break;
                    
                case "remove":
                case "rem":
                case "delete":
                case "del":
                    if (args.length < 2) {
                        Translation.Watch_Usage_Remove.sendMessage(player);
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
                        Translation.Watch_Usage_Info.sendMessage(player);
                        return true;
                    }
                    showWatchlistItemDetails(player, playerUuid, args[1]);
                    break;
                    
                case "clear":
                    handleClearWatchlist(player, playerUuid);
                    break;
                    
                default:
                    Translation.Watch_UnknownSubcommand.sendMessage(player);
                    break;
            }
            
        } catch (Exception e) {
            logger.warning("Error in watch command for " + player.getName() + ": " + e.getMessage());
            Translation.Watch_ErrorProcessing.sendMessage(player);
        }
        
        return true;
    }
    
    private void handleAddToWatchlist(Player player, String playerUuid, String symbol) throws Exception {
        // Get instrument ID from symbol
        String instrumentId = QuickStocksPlugin.getQueryService().getInstrumentIdBySymbol(symbol.toUpperCase());
        if (instrumentId == null) {
            Translation.Watch_NotFound.sendMessage(player,
                new Replaceable("%symbol%", symbol));
            return;
        }
        
        boolean added = QuickStocksPlugin.getWatchlistService().addToWatchlist(playerUuid, instrumentId);
        if (added) {
            // Get instrument display name for confirmation
            String displayName = QuickStocksPlugin.getQueryService().getInstrumentDisplayName(instrumentId);
            Translation.Watch_Added.sendMessage(player,
                new Replaceable("%symbol%", symbol.toUpperCase()),
                new Replaceable("%displayname%", displayName));
        } else {
            Translation.Watch_AlreadyInWatchlist.sendMessage(player,
                new Replaceable("%symbol%", symbol.toUpperCase()));
        }
    }
    
    private void handleRemoveFromWatchlist(Player player, String playerUuid, String symbol) throws Exception {
        // Get instrument ID from symbol
        String instrumentId = QuickStocksPlugin.getQueryService().getInstrumentIdBySymbol(symbol.toUpperCase());
        if (instrumentId == null) {
            Translation.Watch_NotFound.sendMessage(player,
                new Replaceable("%symbol%", symbol));
            return;
        }
        
        boolean removed = QuickStocksPlugin.getWatchlistService().removeFromWatchlist(playerUuid, instrumentId);
        if (removed) {
            Translation.Watch_Removed.sendMessage(player,
                new Replaceable("%symbol%", symbol.toUpperCase()));
        } else {
            Translation.Watch_NotInWatchlist.sendMessage(player,
                new Replaceable("%symbol%", symbol.toUpperCase()));
        }
    }
    
    private void showWatchlist(Player player, String playerUuid) throws Exception {
        List<WatchlistService.WatchlistItem> watchlist = QuickStocksPlugin.getWatchlistService().getWatchlist(playerUuid);
        
        Translation.Watch_ListHeader.sendMessage(player);
        
        if (watchlist.isEmpty()) {
            Translation.Watch_Empty.sendMessage(player);
            Translation.Watch_EmptyHint.sendMessage(player);
            return;
        }
        
        Translation.Watch_WatchingHeader.sendMessage(player,
            new Replaceable("%count%", String.valueOf(watchlist.size())));
        player.sendMessage(""); // Empty line for readability
        
        for (WatchlistService.WatchlistItem item : watchlist) {
            // Format price change colors and arrows
            String changeColor = item.getChange24h() >= 0 ? "&a" : "&c";
            String changeArrow = item.getChange24h() >= 0 ? "▲" : "▼";
            
            // Format the display line
            Translation.Watch_ListItem.sendMessage(player,
                new Replaceable("%symbol%", item.getSymbol()),
                new Replaceable("%displayname%", item.getDisplayName()),
                new Replaceable("%price%", String.format("%.2f", item.getLastPrice())),
                new Replaceable("%color%", changeColor),
                new Replaceable("%arrow%", changeArrow),
                new Replaceable("%change%", String.format("%.2f", Math.abs(item.getChange24h()))),
                new Replaceable("%change1h%", String.format("%+.2f", item.getChange1h())));
        }
        
        player.sendMessage(""); // Empty line
        Translation.Watch_InfoHint.sendMessage(player);
    }
    
    private void showWatchlistItemDetails(Player player, String playerUuid, String symbol) throws Exception {
        // Get instrument ID from symbol
        String instrumentId = QuickStocksPlugin.getQueryService().getInstrumentIdBySymbol(symbol.toUpperCase());
        if (instrumentId == null) {
            Translation.Watch_NotFound.sendMessage(player,
                new Replaceable("%symbol%", symbol));
            return;
        }
        
        // Check if it's in watchlist
        if (!QuickStocksPlugin.getWatchlistService().isInWatchlist(playerUuid, instrumentId)) {
            Translation.Watch_NotInWatchlist.sendMessage(player,
                new Replaceable("%symbol%", symbol.toUpperCase()));
            Translation.Watch_NotInWatchlistHint.sendMessage(player,
                new Replaceable("%symbol%", symbol.toLowerCase()));
            return;
        }
        
        // Get detailed information
        List<WatchlistService.WatchlistItem> watchlist = QuickStocksPlugin.getWatchlistService().getWatchlist(playerUuid);
        WatchlistService.WatchlistItem item = watchlist.stream()
            .filter(i -> i.getSymbol().equalsIgnoreCase(symbol))
            .findFirst()
            .orElse(null);
            
        if (item == null) {
            Translation.Watch_ErrorRetrieving.sendMessage(player);
            return;
        }
        
        // Format the detailed display
        Translation.Watch_DetailsHeader.sendMessage(player,
            new Replaceable("%symbol%", item.getSymbol()));
        Translation.Watch_DetailsName.sendMessage(player,
            new Replaceable("%displayname%", item.getDisplayName()));
        Translation.Watch_DetailsType.sendMessage(player,
            new Replaceable("%type%", item.getType()));
        Translation.Watch_DetailsCurrentPrice.sendMessage(player,
            new Replaceable("%price%", String.format("%.2f", item.getLastPrice())));
        
        // Change indicators with colors
        String change24hColor = item.getChange24h() >= 0 ? "&a" : "&c";
        String change1hColor = item.getChange1h() >= 0 ? "&a" : "&c";
        String arrow24h = item.getChange24h() >= 0 ? "▲" : "▼";
        String arrow1h = item.getChange1h() >= 0 ? "▲" : "▼";
        
        Translation.Watch_Details24hChange.sendMessage(player,
            new Replaceable("%color%", change24hColor),
            new Replaceable("%arrow%", arrow24h),
            new Replaceable("%change%", String.format("%.2f%%", item.getChange24h())));
        Translation.Watch_Details1hChange.sendMessage(player,
            new Replaceable("%color%", change1hColor),
            new Replaceable("%arrow%", arrow1h),
            new Replaceable("%change%", String.format("%.2f%%", item.getChange1h())));
        Translation.Watch_DetailsVolatility.sendMessage(player,
            new Replaceable("%volatility%", String.format("%.2f", item.getVolatility24h())));
        
        // Show when added
        Date addedDate = new Date(item.getAddedAt());
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm");
        Translation.Watch_DetailsAddedAt.sendMessage(player,
            new Replaceable("%date%", sdf.format(addedDate)));
    }
    
    private void handleClearWatchlist(Player player, String playerUuid) throws Exception {
        int count = QuickStocksPlugin.getWatchlistService().getWatchlistCount(playerUuid);
        if (count == 0) {
            Translation.Watch_AlreadyEmpty.sendMessage(player);
            return;
        }
        
        int removed = QuickStocksPlugin.getWatchlistService().clearWatchlist(playerUuid);
        Translation.Watch_Cleared.sendMessage(player,
            new Replaceable("%count%", String.valueOf(removed)));
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