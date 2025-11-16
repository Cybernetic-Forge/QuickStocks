package net.cyberneticforge.quickstocks.commands;

import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.core.enums.Translation;
import net.cyberneticforge.quickstocks.core.model.Company;
import net.cyberneticforge.quickstocks.core.model.Replaceable;
import net.cyberneticforge.quickstocks.core.services.features.portfolio.WatchlistService;
import net.cyberneticforge.quickstocks.gui.MarketGUI;
import net.cyberneticforge.quickstocks.infrastructure.db.Db;
import net.cyberneticforge.quickstocks.infrastructure.logging.PluginLogger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Command handler for market operations (/market).
 */
public class MarketCommand implements CommandExecutor, TabCompleter {
    
    private static final PluginLogger logger = QuickStocksPlugin.getPluginLogger();
    
    private final Db database;
    
    public MarketCommand(Db database) {
        this.database = database;
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            Translation.NoConsoleSender.sendMessage(sender);
            return true;
        }
        
        // Check if market feature is enabled
        if (!QuickStocksPlugin.getMarketCfg().isEnabled()) {
            Translation.MarketDisabled.sendMessage(player);
            return true;
        }

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
                    if (!QuickStocksPlugin.getMarketCfg().isTradingEnabled()) {
                        Translation.FeatureDisabled.sendMessage(player);
                        return true;
                    }
                    if (args.length < 3) {
                        Translation.Market_Buy_Usage.sendMessage(player);
                        return true;
                    }
                    handleBuyShares(player, playerUuid, args[1], args[2]);
                    break;
                    
                case "sell":
                    // Unified sell command - now sells company shares
                    if (!QuickStocksPlugin.getMarketCfg().isTradingEnabled()) {
                        Translation.FeatureDisabled.sendMessage(player);
                        return true;
                    }
                    if (args.length < 3) {
                        Translation.Market_Sell_Usage.sendMessage(player);
                        return true;
                    }
                    handleSellShares(player, playerUuid, args[1], args[2]);
                    break;
                    
                case "shareholders":
                    if (args.length < 2) {
                        Translation.Market_Shareholders_Usage.sendMessage(player);
                        return true;
                    }
                    handleShareholders(player, args[1]);
                    break;
                    
                case "portfolio":
                case "holdings":
                    if (!QuickStocksPlugin.getMarketCfg().isPortfolioEnabled()) {
                        Translation.FeatureDisabled.sendMessage(player);
                        return true;
                    }
                    showPortfolio(player, playerUuid);
                    break;
                    
                case "history":
                    if (!QuickStocksPlugin.getMarketCfg().isPortfolioEnabled()) {
                        Translation.FeatureDisabled.sendMessage(player);
                        return true;
                    }
                    showOrderHistory(player, playerUuid);
                    break;
                    
                case "watchlist":
                case "watch":
                    if (!QuickStocksPlugin.getMarketCfg().isWatchlistEnabled()) {
                        Translation.FeatureDisabled.sendMessage(player);
                        return true;
                    }
                    showWatchlistSummary(player, playerUuid);
                    break;
                    
                default:
                    Translation.Market_UnknownSubcommand.sendMessage(player);
                    break;
            }
            
        } catch (Exception e) {
            logger.warning("Error in market command for " + player.getName() + ": " + e.getMessage());
            Translation.Market_ErrorProcessing.sendMessage(player);
        }
        
        return true;
    }
    
    private void showMarketOverview(Player player) throws Exception {
        // Open the Market GUI instead of showing chat messages
        try {
            MarketGUI marketGUI = new MarketGUI(player);
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
        List<Company> companiesOnMarket = QuickStocksPlugin.getCompanyService().getCompaniesOnMarket();
        
        Translation.Market_Overview_Header.sendMessage(player);
        Translation.Market_Overview_CompaniesHeader.sendMessage(player);
        
        if (companiesOnMarket.isEmpty()) {
            Translation.Market_Overview_NoCompanies.sendMessage(player);
            return;
        }
        
        int rank = 1;
        for (Company company : companiesOnMarket) {
            String symbol = company.getSymbol();
            String displayName = company.getName();
            double balance = company.getBalance();
            double sharePrice = QuickStocksPlugin.getCompanyMarketService().calculateSharePrice(company);
            
            Translation.Market_Overview_CompanyItem.sendMessage(player,
                new Replaceable("%num%", String.valueOf(rank++)),
                new Replaceable("%company%", displayName),
                new Replaceable("%symbol%", symbol),
                new Replaceable("%price%", String.format("%.2f", sharePrice)),
                new Replaceable("%balance%", String.format("%.2f", balance)));
        }
        
        Translation.Market_Overview_BuyHint.sendMessage(player);
    }
    
    private void showPortfolio(Player player, String playerUuid) throws Exception {
        // Get company shares from user_holdings (instruments infrastructure)
        List<Map<String, Object>> companyShares = database.query(
            """
            SELECT\s
                uh.instrument_id, uh.qty as shares, uh.avg_cost,\s
                i.symbol, i.display_name as name,
                c.id as company_id, c.balance
            FROM user_holdings uh
            JOIN instruments i ON uh.instrument_id = i.id
            LEFT JOIN companies c ON i.id = 'COMPANY_' || c.id
            WHERE uh.player_uuid = ? AND uh.qty > 0 AND i.type = 'EQUITY'
            ORDER BY i.symbol
           \s""",
            playerUuid
        );
        
        double walletBalance = QuickStocksPlugin.getWalletService().getBalance(playerUuid);
        
        // Calculate total portfolio value
        double portfolioValue = 0.0;
        for (Map<String, Object> share : companyShares) {
            String companyId = (String) share.get("company_id");
            double shares = ((Number) share.get("shares")).doubleValue();
            
            if (companyId != null) {
                // Get company to calculate current share price
                Optional<Company> companyOpt = QuickStocksPlugin.getCompanyService().getCompanyById(companyId);
                if (companyOpt.isPresent()) {
                    double currentPrice = QuickStocksPlugin.getCompanyMarketService().calculateSharePrice(companyOpt.get());
                    portfolioValue += shares * currentPrice;
                }
            }
        }
        
        Translation.Market_Portfolio_Header.sendMessage(player);
        Translation.Market_Portfolio_CashBalance.sendMessage(player,
            new Replaceable("%balance%", String.format("%.2f", walletBalance)));
        Translation.Market_Portfolio_PortfolioValue.sendMessage(player,
            new Replaceable("%value%", String.format("%.2f", portfolioValue)));
        Translation.Market_Portfolio_TotalAssets.sendMessage(player,
            new Replaceable("%total%", String.format("%.2f", walletBalance + portfolioValue)));
        
        if (companyShares.isEmpty()) {
            Translation.Market_Portfolio_NoHoldings.sendMessage(player);
            return;
        }
        
        Translation.Market_Portfolio_HoldingsHeader.sendMessage(player);
        for (Map<String, Object> share : companyShares) {
            String companyId = (String) share.get("company_id");
            String name = (String) share.get("name");
            String symbol = (String) share.get("symbol");
            double shares = ((Number) share.get("shares")).doubleValue();
            double avgCost = ((Number) share.get("avg_cost")).doubleValue();
            
            // Get company to calculate current share price
            Optional<Company> companyOpt = QuickStocksPlugin.getCompanyService().getCompanyById(companyId);
            if (companyOpt.isEmpty()) continue;
            
            double currentPrice = QuickStocksPlugin.getCompanyMarketService().calculateSharePrice(companyOpt.get());
            double unrealizedPnL = (currentPrice - avgCost) * shares;
            double unrealizedPnLPercent = ((currentPrice - avgCost) / avgCost) * 100;
            
            String pnlColor = unrealizedPnL >= 0 ? "&a" : "&c";
            String pnlArrow = unrealizedPnL >= 0 ? "▲" : "▼";
            
            Translation.Market_Portfolio_HoldingItem.sendMessage(player,
                new Replaceable("%company%", name),
                new Replaceable("%symbol%", symbol),
                new Replaceable("%shares%", String.format("%.2f", shares)),
                new Replaceable("%avgcost%", String.format("%.2f", avgCost)),
                new Replaceable("%current%", String.format("%.2f", currentPrice)),
                new Replaceable("%arrow%", pnlArrow),
                new Replaceable("%pnlcolor%", pnlColor),
                new Replaceable("%pnl%", String.format("%.2f", Math.abs(unrealizedPnL))),
                new Replaceable("%percent%", String.format("%.1f", unrealizedPnLPercent)));
        }
    }
    
    private void showOrderHistory(Player player, String playerUuid) throws Exception {
        // Get company share transaction history from orders table (instruments infrastructure)
        List<Map<String, Object>> transactions = database.query(
            """
            SELECT\s
                o.side as type, o.qty as shares, o.price, o.ts,
                i.display_name as name, i.symbol
            FROM orders o
            JOIN instruments i ON o.instrument_id = i.id
            WHERE o.player_uuid = ? AND i.type = 'EQUITY'
            ORDER BY o.ts DESC
            LIMIT 10
           \s""",
            playerUuid
        );
        
        Translation.Market_History_Header.sendMessage(player);
        
        if (transactions.isEmpty()) {
            Translation.Market_History_NoHistory.sendMessage(player);
            return;
        }
        
        for (Map<String, Object> tx : transactions) {
            String type = (String) tx.get("type");
            String name = (String) tx.get("name");
            double price = ((Number) tx.get("price")).doubleValue();
            long timestamp = ((Number) tx.get("ts")).longValue();
            
            java.util.Date date = new java.util.Date(timestamp);
            String timeStr = String.format("%tF %<tT", date);
            
            Translation.Market_History_TransactionItem.sendMessage(player,
                new Replaceable("%time%", timeStr),
                new Replaceable("%type%", type),
                new Replaceable("%company%", name + " @ $" + String.format("%.2f", price)),
                new Replaceable("%price%", String.format("%.2f", price)));
        }
    }
    
    private void showWatchlistSummary(Player player, String playerUuid) throws Exception {
        List<WatchlistService.WatchlistItem> watchlist = QuickStocksPlugin.getWatchlistService().getWatchlist(playerUuid);
        
        Translation.Watch_ListHeader.sendMessage(player);
        
        if (watchlist.isEmpty()) {
            Translation.Watch_Empty.sendMessage(player);
            Translation.Watch_EmptyHint.sendMessage(player);
            return;
        }
        
        Translation.Watch_WatchingHeader.sendMessage(player,
            new Replaceable("%count%", String.valueOf(watchlist.size())));
        
        for (WatchlistService.WatchlistItem item : watchlist) {
            Translation.Watch_CompanyItem.sendMessage(player,
                new Replaceable("%company%", item.displayName()),
                new Replaceable("%symbol%", item.symbol()),
                new Replaceable("%price%", String.format("%.2f", item.lastPrice())));
        }
    }
    
    /**
     * Handles buying shares of a company.
     */
    private void handleBuyShares(Player player, String playerUuid, String companyNameOrSymbol, String qtyStr) throws Exception {
        try {
            double quantity = Double.parseDouble(qtyStr);
            if (quantity <= 0) {
                Translation.Market_Buy_QuantityPositive.sendMessage(player);
                return;
            }
            
            // Try to find company by name or symbol
            Optional<Company> companyOpt = QuickStocksPlugin.getCompanyService().getCompanyByNameOrSymbol(companyNameOrSymbol);
            if (companyOpt.isEmpty()) {
                Translation.Market_Buy_CompanyNotFound.sendMessage(player,
                    new Replaceable("%company%", companyNameOrSymbol));
                return;
            }
            
            Company company = companyOpt.get();
            
            // Check if company is on market
            if (!company.isOnMarket()) {
                Translation.Market_Buy_NotOnMarket.sendMessage(player,
                    new Replaceable("%company%", company.getName()));
                return;
            }
            
            double sharePrice = QuickStocksPlugin.getCompanyMarketService().calculateSharePrice(company);
            double totalCost = quantity * sharePrice;
            
            // Execute purchase
            QuickStocksPlugin.getCompanyMarketService().buyShares(company.getId(), playerUuid, quantity);
            
            Translation.Market_Buy_Success.sendMessage(player,
                new Replaceable("%qty%", String.format("%.2f", quantity)));
            Translation.Market_Buy_Details.sendMessage(player,
                new Replaceable("%company%", company.getName()),
                new Replaceable("%symbol%", company.getSymbol()),
                new Replaceable("%price%", String.format("%.2f", sharePrice)),
                new Replaceable("%total%", String.format("%.2f", totalCost)));
            
        } catch (NumberFormatException e) {
            Translation.Market_Buy_InvalidQuantity.sendMessage(player,
                new Replaceable("%qty%", qtyStr));
        } catch (IllegalArgumentException e) {
            Translation.Market_Buy_Error.sendMessage(player,
                new Replaceable("%error%", e.getMessage()));
        }
    }
    
    /**
     * Handles selling shares of a company.
     */
    private void handleSellShares(Player player, String playerUuid, String companyNameOrSymbol, String qtyStr) throws Exception {
        try {
            double quantity = Double.parseDouble(qtyStr);
            if (quantity <= 0) {
                Translation.Market_Buy_QuantityPositive.sendMessage(player);
                return;
            }
            
            // Try to find company by name or symbol
            Optional<Company> companyOpt = QuickStocksPlugin.getCompanyService().getCompanyByNameOrSymbol(companyNameOrSymbol);
            if (companyOpt.isEmpty()) {
                Translation.Market_Buy_CompanyNotFound.sendMessage(player,
                    new Replaceable("%company%", companyNameOrSymbol));
                return;
            }
            
            Company company = companyOpt.get();
            
            // Check if company is on market
            if (!company.isOnMarket()) {
                Translation.Market_Buy_NotOnMarket.sendMessage(player,
                    new Replaceable("%company%", company.getName()));
                return;
            }
            
            double sharePrice = QuickStocksPlugin.getCompanyMarketService().calculateSharePrice(company);
            double totalValue = quantity * sharePrice;
            
            // Execute sale
            QuickStocksPlugin.getCompanyMarketService().sellShares(company.getId(), playerUuid, quantity);
            
            Translation.Market_Sell_Success.sendMessage(player,
                new Replaceable("%qty%", String.format("%.2f", quantity)));
            Translation.Market_Sell_Details.sendMessage(player,
                new Replaceable("%company%", company.getName()),
                new Replaceable("%symbol%", company.getSymbol()),
                new Replaceable("%price%", String.format("%.2f", sharePrice)),
                new Replaceable("%total%", String.format("%.2f", totalValue)));
            
        } catch (NumberFormatException e) {
            Translation.Market_Buy_InvalidQuantity.sendMessage(player,
                new Replaceable("%qty%", qtyStr));
        } catch (IllegalArgumentException e) {
            Translation.Market_Sell_Error.sendMessage(player,
                new Replaceable("%error%", e.getMessage()));
        }
    }
    
    /**
     * Handles viewing shareholders of a company.
     */
    private void handleShareholders(Player player, String companyNameOrSymbol) throws Exception {
        // Try to find company by name or symbol
        Optional<Company> companyOpt = QuickStocksPlugin.getCompanyService().getCompanyByNameOrSymbol(companyNameOrSymbol);
        if (companyOpt.isEmpty()) {
            Translation.Market_Buy_CompanyNotFound.sendMessage(player,
                new Replaceable("%company%", companyNameOrSymbol));
            return;
        }
        
        Company company = companyOpt.get();
        
        if (!company.isOnMarket()) {
            Translation.Market_Buy_NotOnMarket.sendMessage(player,
                new Replaceable("%company%", company.getName()));
            return;
        }
        
        // Get shareholders
        List<Map<String, Object>> shareholders = QuickStocksPlugin.getCompanyMarketService().getShareholders(company.getId());
        
        Translation.Market_Shareholders_Header.sendMessage(player,
            new Replaceable("%company%", company.getName()));
        
        if (shareholders.isEmpty()) {
            Translation.Market_Shareholders_NoShareholders.sendMessage(player);
            return;
        }
        
        double totalShares = QuickStocksPlugin.getCompanyMarketService().getIssuedSharesFromHoldings(company.getId());
        
        for (Map<String, Object> sh : shareholders) {
            String playerUuid = (String) sh.get("player_uuid");
            double shares = ((Number) sh.get("shares")).doubleValue();
            double percentage = (shares / totalShares) * 100;
            
            // Get player name
            org.bukkit.OfflinePlayer offlinePlayer = org.bukkit.Bukkit.getOfflinePlayer(java.util.UUID.fromString(playerUuid));
            String playerName = offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unknown";
            
            Translation.Market_Shareholders_ShareholderItem.sendMessage(player,
                new Replaceable("%player%", playerName),
                new Replaceable("%shares%", String.format("%.2f", shares)),
                new Replaceable("%percentage%", String.format("%.1f", percentage)));
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            return Stream.of("browse", "buy", "sell", "shareholders", "portfolio", "history", "watchlist")
                    .filter(option -> option.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        
        if (args.length == 2 && (args[0].equalsIgnoreCase("buy") || args[0].equalsIgnoreCase("sell") || 
                                  args[0].equalsIgnoreCase("shareholders"))) {
            // Show company symbols for trading commands
            try {
                List<Company> companies = QuickStocksPlugin.getCompanyService().getCompaniesOnMarket();
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