package com.example.quickstocks.commands;

import com.example.quickstocks.I18n;
import com.example.quickstocks.application.queries.QueryService;
import com.example.quickstocks.core.services.HoldingsService;
import com.example.quickstocks.core.services.TradingService;
import com.example.quickstocks.core.services.WalletService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Command handler for market trading operations.
 */
public class MarketCommand implements CommandExecutor, TabCompleter {
    
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
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(I18n.component("general.only_players"));
            return true;
        }
        
        if (args.length == 0) {
            // Show market browse by default
            showBrowse(player);
        } else {
            String subCommand = args[0].toLowerCase();
            switch (subCommand) {
                case "browse" -> showBrowse(player);
                case "buy" -> handleBuy(player, args);
                case "sell" -> handleSell(player, args);
                case "portfolio" -> showPortfolio(player);
                case "history" -> showHistory(player);
                default -> showUsage(player);
            }
        }
        
        return true;
    }
    
    private void showBrowse(Player player) {
        // TODO: Implement market browse functionality
        player.sendMessage(I18n.component("market.open_title_items"));
    }
    
    private void handleBuy(Player player, String[] args) {
        // TODO: Implement buy functionality
        player.sendMessage(I18n.component("market.buy_title"));
    }
    
    private void handleSell(Player player, String[] args) {
        // TODO: Implement sell functionality
        player.sendMessage(I18n.component("market.sell_title"));
    }
    
    private void showPortfolio(Player player) {
        // TODO: Implement portfolio view
        player.sendMessage(I18n.component("market.balance"));
    }
    
    private void showHistory(Player player) {
        // TODO: Implement history view
        player.sendMessage(I18n.component("market.order_logged"));
    }
    
    private void showUsage(Player player) {
        player.sendMessage(I18n.component("market.open_title_items"));
    }
    
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(Arrays.asList("browse", "buy", "sell", "portfolio", "history"));
        }
        
        return completions;
    }
}