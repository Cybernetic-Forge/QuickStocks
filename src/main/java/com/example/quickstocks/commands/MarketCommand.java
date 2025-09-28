package com.example.quickstocks.commands;

import com.example.quickstocks.application.queries.QueryService;
import com.example.quickstocks.utils.TranslationManager;
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
import java.util.List;
import java.util.Map;

/**
 * /market command implementation - provides market interface
 * This serves as the GUI/interface that Market Device opens
 */
public class MarketCommand implements CommandExecutor, TabCompleter {
    
    private final QueryService queryService;
    private final TranslationManager translations;
    
    public MarketCommand(QueryService queryService, TranslationManager translations) {
        this.queryService = queryService;
        this.translations = translations;
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            // Show market overview
            showMarketOverview(player);
        } else {
            // Show specific stock details
            String query = String.join(" ", args);
            showStockDetails(player, query);
        }
        
        return true;
    }
    
    /**
     * Shows the market overview with top gainers
     */
    private void showMarketOverview(Player player) {
        try {
            List<Map<String, Object>> topGainers = queryService.getTopGainersByChange24h(10);
            
            player.sendMessage(Component.text(""));
            player.sendMessage(Component.text("═══════════════════════════════════════", NamedTextColor.GOLD, TextDecoration.BOLD));
            player.sendMessage(Component.text("              📈 MARKET OVERVIEW 📈", NamedTextColor.GOLD, TextDecoration.BOLD));
            player.sendMessage(Component.text("═══════════════════════════════════════", NamedTextColor.GOLD, TextDecoration.BOLD));
            player.sendMessage(Component.text(""));
            
            if (topGainers.isEmpty()) {
                player.sendMessage(Component.text("📊 No market data available at the moment.", NamedTextColor.GRAY));
                return;
            }
            
            player.sendMessage(Component.text("🏆 TOP GAINERS (24H)", NamedTextColor.YELLOW, TextDecoration.BOLD));
            player.sendMessage(Component.text("─".repeat(40), NamedTextColor.GRAY));
            
            for (int i = 0; i < topGainers.size(); i++) {
                Map<String, Object> stock = topGainers.get(i);
                displayStockRow(player, i + 1, stock);
            }
            
            player.sendMessage(Component.text(""));
            player.sendMessage(Component.text("💡 Use /market <symbol> for detailed information", NamedTextColor.GRAY));
            player.sendMessage(Component.text("═══════════════════════════════════════", NamedTextColor.GOLD));
            
        } catch (SQLException e) {
            player.sendMessage(Component.text("❌ Database error: " + e.getMessage(), NamedTextColor.RED));
        }
    }
    
    /**
     * Shows detailed information for a specific stock
     */
    private void showStockDetails(Player player, String query) {
        try {
            // Try to find by symbol first
            var stockOpt = queryService.findBySymbol(query);
            
            if (stockOpt.isEmpty()) {
                // Try by material if symbol not found
                stockOpt = queryService.findByMcMaterial(query);
            }
            
            if (stockOpt.isEmpty()) {
                player.sendMessage(Component.text("❌ Stock not found: " + query, NamedTextColor.RED));
                player.sendMessage(Component.text("💡 Try /market to see available stocks", NamedTextColor.GRAY));
                return;
            }
            
            Map<String, Object> stock = stockOpt.get();
            displayDetailedStockCard(player, stock);
            
        } catch (SQLException e) {
            player.sendMessage(Component.text("❌ Database error: " + e.getMessage(), NamedTextColor.RED));
        }
    }
    
    /**
     * Displays a stock row in the market overview
     */
    private void displayStockRow(Player player, int rank, Map<String, Object> stock) {
        String symbol = (String) stock.get("symbol");
        String displayName = (String) stock.get("display_name");
        double lastPrice = ((Number) stock.get("last_price")).doubleValue();
        double change24h = ((Number) stock.get("change_24h")).doubleValue();
        
        // Truncate long names
        if (displayName.length() > 20) {
            displayName = displayName.substring(0, 17) + "...";
        }
        
        NamedTextColor changeColor = change24h >= 0 ? NamedTextColor.GREEN : NamedTextColor.RED;
        String arrow = change24h >= 0 ? "▲" : "▼";
        
        Component rankComponent = Component.text(String.format("%2d. ", rank), NamedTextColor.WHITE);
        Component symbolComponent = Component.text(String.format("%-8s ", symbol), NamedTextColor.AQUA);
        Component nameComponent = Component.text(String.format("%-20s ", displayName), NamedTextColor.WHITE);
        Component priceComponent = Component.text(String.format("$%8.2f ", lastPrice), NamedTextColor.GOLD);
        Component changeComponent = Component.text(String.format("%s%+6.2f%%", arrow, change24h), changeColor);
        
        player.sendMessage(Component.text()
                .append(rankComponent)
                .append(symbolComponent)
                .append(nameComponent)
                .append(priceComponent)
                .append(changeComponent)
                .build());
    }
    
    /**
     * Displays detailed stock information card
     */
    private void displayDetailedStockCard(Player player, Map<String, Object> stock) {
        String symbol = (String) stock.get("symbol");
        String displayName = (String) stock.get("display_name");
        String type = (String) stock.get("type");
        double lastPrice = ((Number) stock.get("last_price")).doubleValue();
        double change1h = ((Number) stock.get("change_1h")).doubleValue();
        double change24h = ((Number) stock.get("change_24h")).doubleValue();
        double volatility24h = ((Number) stock.get("volatility_24h")).doubleValue();
        double marketCap = ((Number) stock.get("market_cap")).doubleValue();
        
        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("═══ STOCK DETAILS ═══", NamedTextColor.GOLD, TextDecoration.BOLD));
        
        player.sendMessage(Component.text()
                .append(Component.text("📊 ", NamedTextColor.YELLOW))
                .append(Component.text(displayName, NamedTextColor.WHITE, TextDecoration.BOLD))
                .append(Component.text(" (" + symbol + ")", NamedTextColor.GRAY))
                .build());
        
        player.sendMessage(Component.text()
                .append(Component.text("🏷️ Type: ", NamedTextColor.YELLOW))
                .append(Component.text(type, NamedTextColor.WHITE))
                .build());
        
        player.sendMessage(Component.text()
                .append(Component.text("💰 Price: ", NamedTextColor.YELLOW))
                .append(Component.text(String.format("$%.2f", lastPrice), NamedTextColor.GOLD, TextDecoration.BOLD))
                .build());
        
        displayChangeRow(player, "📈 Δ1h:", change1h);
        displayChangeRow(player, "📊 Δ24h:", change24h);
        
        player.sendMessage(Component.text()
                .append(Component.text("⚡ Volatility: ", NamedTextColor.YELLOW))
                .append(Component.text(String.format("%.4f", volatility24h), NamedTextColor.WHITE))
                .build());
        
        if (marketCap > 0) {
            player.sendMessage(Component.text()
                    .append(Component.text("🏢 Market Cap: ", NamedTextColor.YELLOW))
                    .append(Component.text(String.format("$%.0f", marketCap), NamedTextColor.WHITE))
                    .build());
        }
        
        player.sendMessage(Component.text("═".repeat(25), NamedTextColor.GOLD));
    }
    
    /**
     * Displays a change row with appropriate color
     */
    private void displayChangeRow(Player player, String label, double change) {
        NamedTextColor color = change >= 0 ? NamedTextColor.GREEN : NamedTextColor.RED;
        String arrow = change >= 0 ? "▲" : "▼";
        
        player.sendMessage(Component.text()
                .append(Component.text(label + " ", NamedTextColor.YELLOW))
                .append(Component.text(String.format("%s %.2f%%", arrow, change), color))
                .build());
    }
    
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length <= 1) {
            try {
                String prefix = args.length == 0 ? "" : args[0];
                List<String> suggestions = queryService.getMatchingSymbolsAndMaterials(prefix);
                
                // Limit to 20 suggestions to avoid spam
                return suggestions.size() > 20 ? suggestions.subList(0, 20) : suggestions;
                
            } catch (SQLException e) {
                // If database error, return empty list
                return new ArrayList<>();
            }
        }
        
        return new ArrayList<>();
    }
}