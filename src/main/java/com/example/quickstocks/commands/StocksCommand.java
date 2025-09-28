package com.example.quickstocks.commands;

import com.example.quickstocks.I18n;
import com.example.quickstocks.application.queries.QueryService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * /stocks command implementation with pretty Adventure Components output.
 * Supports both top 10 gainers display and individual stock lookup.
 */
public class StocksCommand implements CommandExecutor, TabCompleter {
    
    private final QueryService queryService;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
    
    public StocksCommand(QueryService queryService) {
        this.queryService = queryService;
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            // No args → show top 10 gainers
            showTopGainers(sender);
        } else {
            // With arg → show specific stock card
            String query = String.join(" ", args);
            showStockCard(sender, query);
        }
        return true;
    }
    
    /**
     * Shows top 10 gainers (24h) across all instrument types.
     */
    private void showTopGainers(CommandSender sender) {
        try {
            List<Map<String, Object>> gainers = queryService.getTopGainersByChange24h(10);
            
            if (gainers.isEmpty()) {
                sender.sendMessage(I18n.component("stocks.no_stocks"));
                return;
            }
            
            // Header
            sender.sendMessage(I18n.component("stocks.top10_header"));
            sender.sendMessage(Component.text("═".repeat(60), NamedTextColor.GRAY));
            
            // Table header
            Component header = Component.text()
                    .append(Component.text(I18n.tr("stocks.header_rank"), NamedTextColor.WHITE, TextDecoration.BOLD))
                    .append(Component.text(" │ ", NamedTextColor.GRAY))
                    .append(Component.text(I18n.tr("stocks.header_symbol"), NamedTextColor.WHITE, TextDecoration.BOLD))
                    .append(Component.text(" │ ", NamedTextColor.GRAY))
                    .append(Component.text(I18n.tr("stocks.header_name"), NamedTextColor.WHITE, TextDecoration.BOLD))
                    .append(Component.text(" │ ", NamedTextColor.GRAY))
                    .append(Component.text(I18n.tr("stocks.header_price"), NamedTextColor.WHITE, TextDecoration.BOLD))
                    .append(Component.text(" │ ", NamedTextColor.GRAY))
                    .append(Component.text(I18n.tr("stocks.header_change"), NamedTextColor.WHITE, TextDecoration.BOLD))
                    .build();
            
            sender.sendMessage(header);
            sender.sendMessage(Component.text("─".repeat(60), NamedTextColor.GRAY));
            
            // Display each gainer
            for (int i = 0; i < gainers.size(); i++) {
                Map<String, Object> stock = gainers.get(i);
                displayGainerRow(sender, i + 1, stock);
            }
            
            sender.sendMessage(Component.text("─".repeat(60), NamedTextColor.GRAY));
            sender.sendMessage(I18n.component("stocks.detail_hint"));
            
        } catch (SQLException e) {
            Map<String, Object> placeholders = Map.of("error", e.getMessage());
            sender.sendMessage(I18n.component("general.database_error", placeholders));
        }
    }
    
    /**
     * Displays a single row in the top gainers table.
     */
    private void displayGainerRow(CommandSender sender, int rank, Map<String, Object> stock) {
        String symbol = (String) stock.get("symbol");
        String displayName = (String) stock.get("display_name");
        double lastPrice = ((Number) stock.get("last_price")).doubleValue();
        double change24h = ((Number) stock.get("change_24h")).doubleValue();
        
        // Truncate display name if too long
        String shortName = displayName.length() > 15 ? displayName.substring(0, 12) + "..." : displayName;
        
        // Format change with color and arrow
        TextColor changeColor = change24h >= 0 ? NamedTextColor.GREEN : NamedTextColor.RED;
        String arrow = change24h >= 0 ? "▲" : "▼";
        
        Component row = Component.text()
                .append(Component.text(String.format("%2d", rank), NamedTextColor.YELLOW))
                .append(Component.text("   │ ", NamedTextColor.GRAY))
                .append(Component.text(String.format("%-6s", symbol), NamedTextColor.DARK_BLUE))
                .append(Component.text(" │ ", NamedTextColor.GRAY))
                .append(Component.text(String.format("%-15s", shortName), NamedTextColor.WHITE))
                .append(Component.text(" │ ", NamedTextColor.GRAY))
                .append(Component.text(String.format("$%8.2f", lastPrice), NamedTextColor.GOLD))
                .append(Component.text(" │ ", NamedTextColor.GRAY))
                .append(Component.text(arrow + String.format("%6.2f%%", change24h * 100), changeColor))
                .build();
        
        sender.sendMessage(row);
    }
    
    /**
     * Shows detailed information card for a specific stock.
     */
    private void showStockCard(CommandSender sender, String query) {
        try {
            // Try to resolve query: first by symbol, then by mc_material
            Optional<Map<String, Object>> stockOpt = queryService.findBySymbol(query);
            
            if (stockOpt.isEmpty()) {
                stockOpt = queryService.findByMcMaterial(query);
            }
            
            if (stockOpt.isEmpty()) {
                Map<String, Object> placeholders = Map.of("query", query);
                sender.sendMessage(I18n.component("stocks.not_found", placeholders));
                sender.sendMessage(I18n.component("stocks.not_found_hint"));
                return;
            }
            
            Map<String, Object> stock = stockOpt.get();
            displayStockCard(sender, stock);
            
        } catch (SQLException e) {
            Map<String, Object> placeholders = Map.of("error", e.getMessage());
            sender.sendMessage(I18n.component("general.database_error", placeholders));
        }
    }
    
    /**
     * Displays a detailed stock information card.
     */
    private void displayStockCard(CommandSender sender, Map<String, Object> stock) throws SQLException {
        String symbol = (String) stock.get("symbol");
        String displayName = (String) stock.get("display_name");
        String type = (String) stock.get("type");
        String id = (String) stock.get("id");
        double lastPrice = ((Number) stock.get("last_price")).doubleValue();
        double change1h = ((Number) stock.get("change_1h")).doubleValue();
        double change24h = ((Number) stock.get("change_24h")).doubleValue();
        double volatility24h = ((Number) stock.get("volatility_24h")).doubleValue();
        double marketCap = ((Number) stock.get("market_cap")).doubleValue();
        
        // Header with stock name and symbol
        sender.sendMessage(Component.text(""));
        Map<String, Object> headerPlaceholders = Map.of(
            "name", displayName,
            "symbol", symbol,
            "type", type
        );
        sender.sendMessage(I18n.component("stocks.card_header", headerPlaceholders));
        sender.sendMessage(Component.text("━".repeat(50), NamedTextColor.GRAY));
        
        // Price information
        Map<String, Object> pricePlaceholders = Map.of("price", String.format("%.2f", lastPrice));
        sender.sendMessage(I18n.component("stocks.current_price", pricePlaceholders));
        
        // Changes
        displayChangeRow(sender, I18n.tr("stocks.change_1h") + ":", change1h);
        displayChangeRow(sender, I18n.tr("stocks.change_24h") + ":", change24h);
        
        // Volatility and Market Cap
        Map<String, Object> volatilityPlaceholders = Map.of("volatility", String.format("%.4f", volatility24h));
        sender.sendMessage(I18n.component("stocks.volatility_24h", volatilityPlaceholders));
        
        if (marketCap > 0) {
            Map<String, Object> marketCapPlaceholders = Map.of("marketCap", String.format("%.0f", marketCap));
            sender.sendMessage(I18n.component("stocks.market_cap", marketCapPlaceholders));
        }
        
        // Recent price history
        displayPriceHistory(sender, id);
        sender.sendMessage(Component.text("━".repeat(50), NamedTextColor.GRAY));
    }
    
    /**
     * Displays a change row with appropriate coloring and arrows.
     */
    private void displayChangeRow(CommandSender sender, String label, double change) {
        TextColor changeColor = change >= 0 ? NamedTextColor.GREEN : NamedTextColor.RED;
        String arrow = change >= 0 ? "▲" : "▼";
        
        sender.sendMessage(Component.text()
                .append(Component.text(label + " ", NamedTextColor.YELLOW))
                .append(Component.text(arrow + String.format("%.2f%%", change * 100), changeColor))
                .build());
    }
    
    /**
     * Displays recent price history with mini sparkline.
     */
    private void displayPriceHistory(CommandSender sender, String instrumentId) throws SQLException {
        List<Map<String, Object>> history = queryService.getRecentPriceHistory(instrumentId, 10);
        
        if (history.isEmpty()) {
            return;
        }
        
        sender.sendMessage(I18n.component("stocks.history_header"));
        
        // Create mini sparkline
        String sparkline = createSparkline(history);
        
        Component historyComponent = Component.text()
                .append(Component.text("   ", NamedTextColor.GRAY))
                .append(Component.text(sparkline, NamedTextColor.AQUA))
                .append(Component.text(" (latest → oldest)", NamedTextColor.DARK_GRAY))
                .build();
        
        sender.sendMessage(historyComponent);
        
        // Show most recent price point details
        if (!history.isEmpty()) {
            Map<String, Object> latest = history.get(0);
            long timestamp = ((Number) latest.get("ts")).longValue();
            double price = ((Number) latest.get("price")).doubleValue();
            String reason = (String) latest.get("reason");
            
            String timeStr = dateFormat.format(new Date(timestamp));
            
            Map<String, Object> placeholders = Map.of(
                "price", String.format("%.2f", price),
                "time", timeStr,
                "reason", reason != null ? reason : "unknown"
            );
            sender.sendMessage(I18n.component("stocks.history_latest", placeholders));
        }
    }
    
    /**
     * Creates a simple ASCII sparkline from price history.
     */
    private String createSparkline(List<Map<String, Object>> history) {
        if (history.size() < 2) {
            return "▬";
        }
        
        // Get prices in chronological order (reverse the list)
        List<Double> prices = new ArrayList<>();
        for (int i = history.size() - 1; i >= 0; i--) {
            prices.add(((Number) history.get(i).get("price")).doubleValue());
        }
        
        double min = prices.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
        double max = prices.stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
        double range = max - min;
        
        if (range == 0) {
            return "▬".repeat(Math.min(10, prices.size()));
        }
        
        String[] chars = {"▁", "▂", "▃", "▄", "▅", "▆", "▇", "█"};
        StringBuilder sparkline = new StringBuilder();
        
        for (double price : prices) {
            double normalized = (price - min) / range;
            int index = Math.min(chars.length - 1, (int) (normalized * chars.length));
            sparkline.append(chars[index]);
        }
        
        return sparkline.toString();
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