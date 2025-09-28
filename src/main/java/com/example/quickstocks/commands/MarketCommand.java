package com.example.quickstocks.commands;

import com.example.quickstocks.application.queries.QueryService;
import com.example.quickstocks.core.services.HoldingsService;
import com.example.quickstocks.core.services.TradingService;
import com.example.quickstocks.core.services.WalletService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Command handler for market operations (/market).
 */
public class MarketCommand implements CommandExecutor, TabCompleter {
    
    private static final Logger logger = Logger.getLogger(MarketCommand.class.getName());
    
    private final QueryService queryService;
    private final TradingService tradingService;
    private final HoldingsService holdingsService;
    private final WalletService walletService;
    
    public MarketCommand(QueryService queryService, TradingService tradingService, 
                        HoldingsService holdingsService, WalletService walletService) {
        this.queryService = queryService;
        this.tradingService = tradingService;
        this.holdingsService = holdingsService;
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
                // Show market overview
                showMarketOverview(player);
                return true;
            }
            
            String subcommand = args[0].toLowerCase();
            
            switch (subcommand) {
                case "browse":
                case "list":
                    showMarketOverview(player);
                    break;
                    
                case "buy":
                    if (args.length < 3) {
                        player.sendMessage(ChatColor.RED + "Usage: /market buy <symbol> <quantity>");
                        return true;
                    }
                    handleBuyOrder(player, playerUuid, args[1], args[2]);
                    break;
                    
                case "sell":
                    if (args.length < 3) {
                        player.sendMessage(ChatColor.RED + "Usage: /market sell <symbol> <quantity>");
                        return true;
                    }
                    handleSellOrder(player, playerUuid, args[1], args[2]);
                    break;
                    
                case "portfolio":
                case "holdings":
                    showPortfolio(player, playerUuid);
                    break;
                    
                case "history":
                    showOrderHistory(player, playerUuid);
                    break;
                    
                case "confirm":
                    if (args.length < 4) {
                        player.sendMessage(ChatColor.RED + "Usage: /market confirm <buy|sell> <symbol> <quantity>");
                        return true;
                    }
                    handleConfirmOrder(player, playerUuid, args[1], args[2], args[3]);
                    break;
                    
                default:
                    player.sendMessage(ChatColor.RED + "Unknown subcommand. Usage: /market [browse|buy|sell|portfolio|history|confirm]");
                    break;
            }
            
        } catch (Exception e) {
            logger.warning("Error in market command for " + player.getName() + ": " + e.getMessage());
            player.sendMessage(ChatColor.RED + "An error occurred while processing your market command.");
        }
        
        return true;
    }
    
    private void showMarketOverview(Player player) throws Exception {
        List<Map<String, Object>> topGainers = queryService.getTopGainers(10);
        
        player.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + "Market Overview" + ChatColor.GOLD + " ===");
        player.sendMessage(ChatColor.YELLOW + "Top 10 Gainers (24h):");
        
        if (topGainers.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "No market data available.");
            return;
        }
        
        int rank = 1;
        for (Map<String, Object> instrument : topGainers) {
            String symbol = (String) instrument.get("symbol");
            String displayName = (String) instrument.get("display_name");
            Double price = (Double) instrument.get("last_price");
            Double change24h = (Double) instrument.get("change_24h");
            
            ChatColor changeColor = change24h >= 0 ? ChatColor.GREEN : ChatColor.RED;
            String changeArrow = change24h >= 0 ? "▲" : "▼";
            
            player.sendMessage(String.format(ChatColor.GRAY + "%d. " + ChatColor.WHITE + "%s " + 
                ChatColor.GRAY + "(%s) " + ChatColor.YELLOW + "$%.2f " + changeColor + "%s%.2f%%",
                rank++, displayName, symbol, price, changeArrow, Math.abs(change24h)));
        }
        
        player.sendMessage(ChatColor.GRAY + "Use " + ChatColor.WHITE + "/market buy <symbol> <qty>" + 
                          ChatColor.GRAY + " to purchase shares.");
    }
    
    private void handleBuyOrder(Player player, String playerUuid, String symbol, String qtyStr) throws Exception {
        try {
            double qty = Double.parseDouble(qtyStr);
            if (qty <= 0) {
                player.sendMessage(ChatColor.RED + "Quantity must be positive.");
                return;
            }
            
            // Get instrument ID from symbol
            String instrumentId = queryService.getInstrumentIdBySymbol(symbol.toUpperCase());
            if (instrumentId == null) {
                player.sendMessage(ChatColor.RED + "Instrument not found: " + symbol);
                return;
            }
            
            // Show confirmation
            Double currentPrice = queryService.getCurrentPrice(instrumentId);
            if (currentPrice == null) {
                player.sendMessage(ChatColor.RED + "Price data not available for " + symbol);
                return;
            }
            
            double totalCost = qty * currentPrice;
            double currentBalance = walletService.getBalance(playerUuid);
            
            player.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + "Buy Order Confirmation" + ChatColor.GOLD + " ===");
            player.sendMessage(ChatColor.YELLOW + "Symbol: " + ChatColor.WHITE + symbol);
            player.sendMessage(ChatColor.YELLOW + "Quantity: " + ChatColor.WHITE + String.format("%.2f", qty));
            player.sendMessage(ChatColor.YELLOW + "Price: " + ChatColor.WHITE + "$" + String.format("%.2f", currentPrice));
            player.sendMessage(ChatColor.YELLOW + "Total Cost: " + ChatColor.WHITE + "$" + String.format("%.2f", totalCost));
            player.sendMessage(ChatColor.YELLOW + "Current Balance: " + ChatColor.WHITE + "$" + String.format("%.2f", currentBalance));
            
            if (currentBalance < totalCost) {
                player.sendMessage(ChatColor.RED + "Insufficient funds! Need $" + String.format("%.2f", totalCost - currentBalance) + " more.");
                return;
            }
            
            player.sendMessage(ChatColor.GREEN + "Type " + ChatColor.WHITE + "/market confirm buy " + symbol + " " + qtyStr + 
                              ChatColor.GREEN + " to execute this order.");
            
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid quantity: " + qtyStr);
        }
    }
    
    private void handleSellOrder(Player player, String playerUuid, String symbol, String qtyStr) throws Exception {
        try {
            double qty = Double.parseDouble(qtyStr);
            if (qty <= 0) {
                player.sendMessage(ChatColor.RED + "Quantity must be positive.");
                return;
            }
            
            // Get instrument ID from symbol
            String instrumentId = queryService.getInstrumentIdBySymbol(symbol.toUpperCase());
            if (instrumentId == null) {
                player.sendMessage(ChatColor.RED + "Instrument not found: " + symbol);
                return;
            }
            
            // Check holdings
            HoldingsService.Holding holding = holdingsService.getHolding(playerUuid, instrumentId);
            if (holding == null || holding.getQty() < qty) {
                double availableQty = holding != null ? holding.getQty() : 0;
                player.sendMessage(ChatColor.RED + "Insufficient shares! You have " + 
                                  String.format("%.2f", availableQty) + " shares of " + symbol);
                return;
            }
            
            // Show confirmation
            Double currentPrice = queryService.getCurrentPrice(instrumentId);
            if (currentPrice == null) {
                player.sendMessage(ChatColor.RED + "Price data not available for " + symbol);
                return;
            }
            
            double totalValue = qty * currentPrice;
            
            player.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + "Sell Order Confirmation" + ChatColor.GOLD + " ===");
            player.sendMessage(ChatColor.YELLOW + "Symbol: " + ChatColor.WHITE + symbol);
            player.sendMessage(ChatColor.YELLOW + "Quantity: " + ChatColor.WHITE + String.format("%.2f", qty));
            player.sendMessage(ChatColor.YELLOW + "Price: " + ChatColor.WHITE + "$" + String.format("%.2f", currentPrice));
            player.sendMessage(ChatColor.YELLOW + "Total Value: " + ChatColor.WHITE + "$" + String.format("%.2f", totalValue));
            player.sendMessage(ChatColor.YELLOW + "Available Shares: " + ChatColor.WHITE + String.format("%.2f", holding.getQty()));
            
            player.sendMessage(ChatColor.GREEN + "Type " + ChatColor.WHITE + "/market confirm sell " + symbol + " " + qtyStr + 
                              ChatColor.GREEN + " to execute this order.");
            
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid quantity: " + qtyStr);
        }
    }
    
    private void showPortfolio(Player player, String playerUuid) throws Exception {
        List<HoldingsService.Holding> holdings = holdingsService.getHoldings(playerUuid);
        double portfolioValue = holdingsService.getPortfolioValue(playerUuid);
        double walletBalance = walletService.getBalance(playerUuid);
        
        player.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + "Your Portfolio" + ChatColor.GOLD + " ===");
        player.sendMessage(ChatColor.YELLOW + "Cash Balance: " + ChatColor.GREEN + "$" + String.format("%.2f", walletBalance));
        player.sendMessage(ChatColor.YELLOW + "Portfolio Value: " + ChatColor.GREEN + "$" + String.format("%.2f", portfolioValue));
        player.sendMessage(ChatColor.YELLOW + "Total Assets: " + ChatColor.GREEN + "$" + String.format("%.2f", walletBalance + portfolioValue));
        
        if (holdings.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "No holdings found.");
            return;
        }
        
        player.sendMessage(ChatColor.YELLOW + "\nHoldings:");
        for (HoldingsService.Holding holding : holdings) {
            ChatColor pnlColor = holding.getUnrealizedPnL() >= 0 ? ChatColor.GREEN : ChatColor.RED;
            String pnlArrow = holding.getUnrealizedPnL() >= 0 ? "▲" : "▼";
            
            player.sendMessage(String.format(ChatColor.WHITE + "%s: " + ChatColor.GRAY + "%.2f shares @ $%.2f avg " +
                ChatColor.YELLOW + "($%.2f current) " + pnlColor + "%s$%.2f (%.1f%%)",
                holding.getSymbol(), holding.getQty(), holding.getAvgCost(), 
                holding.getCurrentPrice(), pnlArrow, Math.abs(holding.getUnrealizedPnL()), 
                holding.getUnrealizedPnLPercent()));
        }
    }
    
    private void showOrderHistory(Player player, String playerUuid) throws Exception {
        List<TradingService.Order> orders = tradingService.getOrderHistory(playerUuid, 10);
        
        player.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + "Order History" + ChatColor.GOLD + " ===");
        
        if (orders.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "No order history found.");
            return;
        }
        
        for (TradingService.Order order : orders) {
            ChatColor sideColor = order.getSide().equals("BUY") ? ChatColor.GREEN : ChatColor.RED;
            java.util.Date date = new java.util.Date(order.getTimestamp());
            
            player.sendMessage(String.format(sideColor + "%s " + ChatColor.WHITE + "%s: " + 
                ChatColor.GRAY + "%.2f @ $%.2f " + ChatColor.DARK_GRAY + "(%tF %<tT)",
                order.getSide(), order.getSymbol(), order.getQty(), 
                order.getPrice(), date));
        }
    }
    
    private void handleConfirmOrder(Player player, String playerUuid, String side, String symbol, String qtyStr) throws Exception {
        try {
            double qty = Double.parseDouble(qtyStr);
            if (qty <= 0) {
                player.sendMessage(ChatColor.RED + "Quantity must be positive.");
                return;
            }
            
            // Get instrument ID from symbol
            String instrumentId = queryService.getInstrumentIdBySymbol(symbol.toUpperCase());
            if (instrumentId == null) {
                player.sendMessage(ChatColor.RED + "Instrument not found: " + symbol);
                return;
            }
            
            TradingService.TradeResult result;
            
            if (side.equalsIgnoreCase("buy")) {
                result = tradingService.executeBuyOrder(playerUuid, instrumentId, qty);
            } else if (side.equalsIgnoreCase("sell")) {
                result = tradingService.executeSellOrder(playerUuid, instrumentId, qty);
            } else {
                player.sendMessage(ChatColor.RED + "Invalid side. Use 'buy' or 'sell'.");
                return;
            }
            
            if (result.isSuccess()) {
                player.sendMessage(ChatColor.GREEN + "✓ Order executed successfully!");
                player.sendMessage(ChatColor.YELLOW + result.getMessage());
            } else {
                player.sendMessage(ChatColor.RED + "✗ Order failed: " + result.getMessage());
            }
            
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid quantity: " + qtyStr);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("browse", "buy", "sell", "portfolio", "history", "confirm")
                    .stream()
                    .filter(option -> option.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return null;
    }
}