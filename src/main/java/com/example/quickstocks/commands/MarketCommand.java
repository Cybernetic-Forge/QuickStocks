package com.example.quickstocks.commands;

import com.example.quickstocks.application.queries.QueryService;
import com.example.quickstocks.core.model.Company;
import com.example.quickstocks.core.services.CompanyMarketService;
import com.example.quickstocks.core.services.CompanyService;
import com.example.quickstocks.core.services.HoldingsService;
import com.example.quickstocks.core.services.TradingService;
import com.example.quickstocks.core.services.WalletService;
import com.example.quickstocks.core.services.WatchlistService;
import com.example.quickstocks.gui.MarketGUI;
import com.example.quickstocks.infrastructure.db.Db;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private final WatchlistService watchlistService;
    private final CompanyService companyService;
    private final CompanyMarketService companyMarketService;
    private final Db database;
    
    public MarketCommand(QueryService queryService, TradingService tradingService, 
                        HoldingsService holdingsService, WalletService walletService,
                        WatchlistService watchlistService, CompanyService companyService,
                        CompanyMarketService companyMarketService, Db database) {
        this.queryService = queryService;
        this.tradingService = tradingService;
        this.holdingsService = holdingsService;
        this.walletService = walletService;
        this.watchlistService = watchlistService;
        this.companyService = companyService;
        this.companyMarketService = companyMarketService;
        this.database = database;
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
                    // Unified buy command - now buys company shares
                    if (args.length < 3) {
                        player.sendMessage(ChatColor.RED + "Usage: /market buy <company> <quantity>");
                        return true;
                    }
                    handleBuyShares(player, playerUuid, args[1], args[2]);
                    break;
                    
                case "sell":
                    // Unified sell command - now sells company shares
                    if (args.length < 3) {
                        player.sendMessage(ChatColor.RED + "Usage: /market sell <company> <quantity>");
                        return true;
                    }
                    handleSellShares(player, playerUuid, args[1], args[2]);
                    break;
                    
                case "shareholders":
                    if (args.length < 2) {
                        player.sendMessage(ChatColor.RED + "Usage: /market shareholders <company>");
                        return true;
                    }
                    handleShareholders(player, args[1]);
                    break;
                    
                case "portfolio":
                case "holdings":
                    showPortfolio(player, playerUuid);
                    break;
                    
                case "history":
                    showOrderHistory(player, playerUuid);
                    break;
                    
                case "watchlist":
                case "watch":
                    showWatchlistSummary(player, playerUuid);
                    break;
                    
                default:
                    player.sendMessage(ChatColor.RED + "Unknown subcommand. Usage: /market [browse|buy|sell|shareholders|portfolio|history|watchlist]");
                    break;
            }
            
        } catch (Exception e) {
            logger.warning("Error in market command for " + player.getName() + ": " + e.getMessage());
            player.sendMessage(ChatColor.RED + "An error occurred while processing your market command.");
        }
        
        return true;
    }
    
    private void showMarketOverview(Player player) throws Exception {
        // Open the Market GUI instead of showing chat messages
        try {
            MarketGUI marketGUI = new MarketGUI(player, queryService, tradingService, holdingsService, walletService, companyService);
            marketGUI.open();
        } catch (Exception e) {
            logger.warning("Failed to open Market GUI for " + player.getName() + ": " + e.getMessage());
            // Fallback to chat-based display
            showMarketOverviewInChat(player);
        }
    }
    
    /**
     * Fallback method to show market overview in chat (when GUI fails)
     */
    private void showMarketOverviewInChat(Player player) throws Exception {
        List<com.example.quickstocks.core.model.Company> companiesOnMarket = companyService.getCompaniesOnMarket();
        
        player.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + "Market Overview" + ChatColor.GOLD + " ===");
        player.sendMessage(ChatColor.YELLOW + "Companies on Market:");
        
        if (companiesOnMarket.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "No companies are currently on the market.");
            return;
        }
        
        int rank = 1;
        for (com.example.quickstocks.core.model.Company company : companiesOnMarket) {
            String symbol = company.getSymbol();
            String displayName = company.getName();
            double balance = company.getBalance();
            double sharePrice = companyMarketService.calculateSharePrice(company);
            
            player.sendMessage(String.format(ChatColor.GRAY + "%d. " + ChatColor.GREEN + "%s " + 
                ChatColor.GRAY + "(%s) " + ChatColor.YELLOW + "Price: $%.2f " + ChatColor.GRAY + "Balance: $%.2f",
                rank++, displayName, symbol, sharePrice, balance));
        }
        
        player.sendMessage(ChatColor.GRAY + "Use " + ChatColor.WHITE + "/market buy <company> <qty>" + 
                          ChatColor.GRAY + " to purchase company shares.");
    }
    
    private void showPortfolio(Player player, String playerUuid) throws Exception {
        // Get company shares instead of old instrument holdings
        List<Map<String, Object>> companyShares = database.query(
            """
            SELECT cs.company_id, cs.shares, cs.avg_cost, c.name, c.symbol, c.balance
            FROM company_shareholders cs
            JOIN companies c ON cs.company_id = c.id
            WHERE cs.player_uuid = ? AND cs.shares > 0
            ORDER BY c.symbol
            """,
            playerUuid
        );
        
        double walletBalance = walletService.getBalance(playerUuid);
        
        // Calculate total portfolio value
        double portfolioValue = 0.0;
        for (Map<String, Object> share : companyShares) {
            String companyId = (String) share.get("company_id");
            double shares = ((Number) share.get("shares")).doubleValue();
            
            // Get company to calculate current share price
            Optional<Company> companyOpt = companyService.getCompanyById(companyId);
            if (companyOpt.isPresent()) {
                double currentPrice = companyMarketService.calculateSharePrice(companyOpt.get());
                portfolioValue += shares * currentPrice;
            }
        }
        
        player.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + "Your Portfolio" + ChatColor.GOLD + " ===");
        player.sendMessage(ChatColor.YELLOW + "Cash Balance: " + ChatColor.GREEN + "$" + String.format("%.2f", walletBalance));
        player.sendMessage(ChatColor.YELLOW + "Portfolio Value: " + ChatColor.GREEN + "$" + String.format("%.2f", portfolioValue));
        player.sendMessage(ChatColor.YELLOW + "Total Assets: " + ChatColor.GREEN + "$" + String.format("%.2f", walletBalance + portfolioValue));
        
        if (companyShares.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "No holdings found.");
            return;
        }
        
        player.sendMessage(ChatColor.YELLOW + "\nHoldings:");
        for (Map<String, Object> share : companyShares) {
            String companyId = (String) share.get("company_id");
            String name = (String) share.get("name");
            String symbol = (String) share.get("symbol");
            double shares = ((Number) share.get("shares")).doubleValue();
            double avgCost = ((Number) share.get("avg_cost")).doubleValue();
            
            // Get company to calculate current share price
            Optional<Company> companyOpt = companyService.getCompanyById(companyId);
            if (companyOpt.isEmpty()) continue;
            
            double currentPrice = companyMarketService.calculateSharePrice(companyOpt.get());
            double unrealizedPnL = (currentPrice - avgCost) * shares;
            double unrealizedPnLPercent = ((currentPrice - avgCost) / avgCost) * 100;
            
            ChatColor pnlColor = unrealizedPnL >= 0 ? ChatColor.GREEN : ChatColor.RED;
            String pnlArrow = unrealizedPnL >= 0 ? "▲" : "▼";
            
            player.sendMessage(String.format(ChatColor.WHITE + "%s (%s): " + ChatColor.GRAY + "%.2f shares @ $%.2f avg " +
                ChatColor.YELLOW + "($%.2f current) " + pnlColor + "%s$%.2f (%.1f%%)",
                name, symbol, shares, avgCost, currentPrice, pnlArrow, Math.abs(unrealizedPnL), 
                unrealizedPnLPercent));
        }
    }
    
    private void showOrderHistory(Player player, String playerUuid) throws Exception {
        // Get company share transaction history
        List<Map<String, Object>> transactions = database.query(
            """
            SELECT tx.type, tx.shares, tx.price, tx.ts, c.name, c.symbol
            FROM company_share_tx tx
            JOIN companies c ON tx.company_id = c.id
            WHERE tx.player_uuid = ?
            ORDER BY tx.ts DESC
            LIMIT 10
            """,
            playerUuid
        );
        
        player.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + "Order History" + ChatColor.GOLD + " ===");
        
        if (transactions.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "No order history found.");
            return;
        }
        
        for (Map<String, Object> tx : transactions) {
            String type = (String) tx.get("type");
            String name = (String) tx.get("name");
            String symbol = (String) tx.get("symbol");
            double shares = ((Number) tx.get("shares")).doubleValue();
            double price = ((Number) tx.get("price")).doubleValue();
            long timestamp = ((Number) tx.get("ts")).longValue();
            
            ChatColor sideColor = type.equals("BUY") ? ChatColor.GREEN : ChatColor.RED;
            java.util.Date date = new java.util.Date(timestamp);
            
            player.sendMessage(String.format(sideColor + "%s " + ChatColor.WHITE + "%s (%s): " + 
                ChatColor.GRAY + "%.2f @ $%.2f " + ChatColor.DARK_GRAY + "(%tF %<tT)",
                type, name, symbol, shares, price, date));
        }
    }
    
    private void showWatchlistSummary(Player player, String playerUuid) throws Exception {
        List<WatchlistService.WatchlistItem> watchlist = watchlistService.getWatchlist(playerUuid);
        
        player.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + "Watchlist Summary" + ChatColor.GOLD + " ===");
        
        if (watchlist.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "Your watchlist is empty.");
            player.sendMessage(ChatColor.GRAY + "Use " + ChatColor.WHITE + "/watch add <symbol>" + 
                              ChatColor.GRAY + " to add instruments to your watchlist.");
            return;
        }
        
        player.sendMessage(ChatColor.YELLOW + "Watching " + watchlist.size() + " instruments:");
        
        for (WatchlistService.WatchlistItem item : watchlist) {
            ChatColor changeColor = item.getChange24h() >= 0 ? ChatColor.GREEN : ChatColor.RED;
            String changeArrow = item.getChange24h() >= 0 ? "▲" : "▼";
            
            player.sendMessage(String.format(ChatColor.YELLOW + "★ " + ChatColor.WHITE + "%s " + 
                ChatColor.GRAY + "(%s) " + ChatColor.YELLOW + "$%.2f " + changeColor + "%s%.2f%%",
                item.getSymbol(), item.getDisplayName(), item.getLastPrice(), 
                changeArrow, Math.abs(item.getChange24h())));
        }
        
        player.sendMessage(ChatColor.GRAY + "Use " + ChatColor.WHITE + "/watch" + ChatColor.GRAY + 
                          " for detailed watchlist management.");
    }
    
    /**
     * Handles buying shares of a company.
     */
    private void handleBuyShares(Player player, String playerUuid, String companyNameOrSymbol, String qtyStr) throws Exception {
        try {
            double quantity = Double.parseDouble(qtyStr);
            if (quantity <= 0) {
                player.sendMessage(ChatColor.RED + "Quantity must be positive.");
                return;
            }
            
            // Try to find company by name or symbol
            Optional<Company> companyOpt = companyService.getCompanyByNameOrSymbol(companyNameOrSymbol);
            if (companyOpt.isEmpty()) {
                player.sendMessage(ChatColor.RED + "Company not found: " + companyNameOrSymbol);
                return;
            }
            
            Company company = companyOpt.get();
            
            // Check if company is on market
            if (!company.isOnMarket()) {
                player.sendMessage(ChatColor.RED + "Company '" + company.getName() + "' is not on the market.");
                return;
            }
            
            double sharePrice = companyMarketService.calculateSharePrice(company);
            double totalCost = quantity * sharePrice;
            
            // Execute purchase
            companyMarketService.buyShares(company.getId(), playerUuid, quantity);
            
            player.sendMessage(ChatColor.GREEN + "Successfully purchased " + String.format("%.2f", quantity) + " shares!");
            player.sendMessage(ChatColor.YELLOW + "Company: " + ChatColor.WHITE + company.getName() + 
                             ChatColor.GRAY + " (" + company.getSymbol() + ")");
            player.sendMessage(ChatColor.YELLOW + "Price per share: " + ChatColor.WHITE + "$" + String.format("%.2f", sharePrice));
            player.sendMessage(ChatColor.YELLOW + "Total cost: " + ChatColor.WHITE + "$" + String.format("%.2f", totalCost));
            
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid quantity: " + qtyStr);
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.RED + e.getMessage());
        }
    }
    
    /**
     * Handles selling shares of a company.
     */
    private void handleSellShares(Player player, String playerUuid, String companyNameOrSymbol, String qtyStr) throws Exception {
        try {
            double quantity = Double.parseDouble(qtyStr);
            if (quantity <= 0) {
                player.sendMessage(ChatColor.RED + "Quantity must be positive.");
                return;
            }
            
            // Try to find company by name or symbol
            Optional<Company> companyOpt = companyService.getCompanyByNameOrSymbol(companyNameOrSymbol);
            if (companyOpt.isEmpty()) {
                player.sendMessage(ChatColor.RED + "Company not found: " + companyNameOrSymbol);
                return;
            }
            
            Company company = companyOpt.get();
            
            // Check if company is on market
            if (!company.isOnMarket()) {
                player.sendMessage(ChatColor.RED + "Company '" + company.getName() + "' is not on the market.");
                return;
            }
            
            double sharePrice = companyMarketService.calculateSharePrice(company);
            double totalValue = quantity * sharePrice;
            
            // Execute sale
            companyMarketService.sellShares(company.getId(), playerUuid, quantity);
            
            player.sendMessage(ChatColor.GREEN + "Successfully sold " + String.format("%.2f", quantity) + " shares!");
            player.sendMessage(ChatColor.YELLOW + "Company: " + ChatColor.WHITE + company.getName() + 
                             ChatColor.GRAY + " (" + company.getSymbol() + ")");
            player.sendMessage(ChatColor.YELLOW + "Price per share: " + ChatColor.WHITE + "$" + String.format("%.2f", sharePrice));
            player.sendMessage(ChatColor.YELLOW + "Total received: " + ChatColor.WHITE + "$" + String.format("%.2f", totalValue));
            
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid quantity: " + qtyStr);
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.RED + e.getMessage());
        }
    }
    
    /**
     * Handles viewing shareholders of a company.
     */
    private void handleShareholders(Player player, String companyNameOrSymbol) throws Exception {
        // Try to find company by name or symbol
        Optional<Company> companyOpt = companyService.getCompanyByNameOrSymbol(companyNameOrSymbol);
        if (companyOpt.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Company not found: " + companyNameOrSymbol);
            return;
        }
        
        Company company = companyOpt.get();
        
        if (!company.isOnMarket()) {
            player.sendMessage(ChatColor.RED + "Company '" + company.getName() + "' is not on the market.");
            return;
        }
        
        // Get shareholders
        List<Map<String, Object>> shareholders = companyMarketService.getShareholders(company.getId());
        
        player.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + company.getName() + " Shareholders" + ChatColor.GOLD + " ===");
        player.sendMessage(ChatColor.YELLOW + "Symbol: " + ChatColor.WHITE + company.getSymbol());
        
        if (shareholders.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "No shareholders found.");
            return;
        }
        
        double totalShares = companyMarketService.getIssuedShares(company.getId());
        
        player.sendMessage(ChatColor.YELLOW + "Total Issued Shares: " + ChatColor.WHITE + String.format("%.2f", totalShares));
        player.sendMessage(ChatColor.YELLOW + "\nShareholders:");
        
        for (Map<String, Object> sh : shareholders) {
            String playerUuid = (String) sh.get("player_uuid");
            double shares = ((Number) sh.get("shares")).doubleValue();
            double percentage = (shares / totalShares) * 100;
            
            // Get player name
            org.bukkit.OfflinePlayer offlinePlayer = org.bukkit.Bukkit.getOfflinePlayer(java.util.UUID.fromString(playerUuid));
            String playerName = offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unknown";
            
            player.sendMessage(String.format(ChatColor.WHITE + "%s: " + ChatColor.GRAY + "%.2f shares (%.1f%%)",
                playerName, shares, percentage));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("browse", "buy", "sell", "shareholders", "portfolio", "history", "watchlist")
                    .stream()
                    .filter(option -> option.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        
        if (args.length == 2 && (args[0].equalsIgnoreCase("buy") || args[0].equalsIgnoreCase("sell") || 
                                  args[0].equalsIgnoreCase("shareholders"))) {
            // Show company symbols for trading commands
            try {
                List<Company> companies = companyService.getCompaniesOnMarket();
                return companies.stream()
                    .map(c -> c.getSymbol() != null ? c.getSymbol() : c.getName())
                    .filter(symbol -> symbol.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            } catch (Exception e) {
                logger.warning("Error getting company symbols for tab completion: " + e.getMessage());
            }
        }

        return null;
    }
}