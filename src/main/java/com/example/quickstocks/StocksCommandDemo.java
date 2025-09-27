package com.example.quickstocks;

import com.example.quickstocks.application.queries.QueryService;
import com.example.quickstocks.infrastructure.db.DatabaseConfig;
import com.example.quickstocks.infrastructure.db.DatabaseManager;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;

/**
 * Demonstration of the /stocks command functionality.
 * Shows both top 10 gainers and individual stock lookups.
 */
public class StocksCommandDemo {
    
    private final QueryService queryService;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
    
    public StocksCommandDemo(QueryService queryService) {
        this.queryService = queryService;
    }
    
    public static void main(String[] args) throws SQLException {
        System.out.println("🚀 QuickStocks /stocks Command Demo");
        System.out.println("===================================");
        
        // Create database configuration
        DatabaseConfig config = new DatabaseConfig();
        config.setProvider("sqlite");
        config.setSqliteFile("/tmp/stocks_demo.db");
        
        // Initialize database
        DatabaseManager databaseManager = new DatabaseManager(config, true);
        databaseManager.initialize();
        
        try {
            QueryService queryService = new QueryService(databaseManager.getDb());
            StocksCommandDemo demo = new StocksCommandDemo(queryService);
            
            System.out.println("📊 Database initialized with seeded Minecraft items");
            System.out.println();
            
            // Show command options
            demo.showMenu();
            
            // Interactive loop
            Scanner scanner = new Scanner(System.in);
            String input;
            
            while (!(input = scanner.nextLine().trim().toLowerCase()).equals("quit")) {
                System.out.println();
                
                if (input.isEmpty() || input.equals("1")) {
                    demo.showTopGainers();
                } else if (input.equals("2")) {
                    demo.showSymbolLookup();
                } else if (input.equals("3")) {
                    demo.showMaterialLookup();
                } else if (input.equals("4")) {
                    demo.showTabCompletion();
                } else if (input.equals("help") || input.equals("menu")) {
                    demo.showMenu();
                } else {
                    // Try to lookup the input as a stock
                    demo.lookupStock(input);
                }
                
                System.out.println();
                System.out.print("Enter command (or 'help'): ");
            }
            
        } finally {
            databaseManager.shutdown();
            System.out.println("\n✅ Demo completed");
        }
    }
    
    private void showMenu() {
        System.out.println("📋 Available commands:");
        System.out.println("  1 or [ENTER] - Show top 10 gainers");
        System.out.println("  2            - Demonstrate symbol lookup");
        System.out.println("  3            - Demonstrate material lookup");
        System.out.println("  4            - Show tab completion example");
        System.out.println("  <symbol>     - Lookup specific stock");
        System.out.println("  help         - Show this menu");
        System.out.println("  quit         - Exit");
        System.out.println();
        System.out.print("Enter command (or 'help'): ");
    }
    
    /**
     * Demonstrates the top 10 gainers functionality.
     */
    private void showTopGainers() {
        try {
            System.out.println("📈 TOP 10 GAINERS (24H)");
            System.out.println("═".repeat(70));
            
            List<Map<String, Object>> gainers = queryService.getTopGainersByChange24h(10);
            
            if (gainers.isEmpty()) {
                System.out.println("📊 No stocks found in the market.");
                return;
            }
            
            // Table header
            System.out.printf("%-4s │ %-8s │ %-20s │ %10s │ %10s%n", 
                             "RANK", "SYMBOL", "NAME", "PRICE", "24H CHANGE");
            System.out.println("─".repeat(70));
            
            // Display each gainer
            for (int i = 0; i < gainers.size(); i++) {
                Map<String, Object> stock = gainers.get(i);
                displayGainerRow(i + 1, stock);
            }
            
            System.out.println("─".repeat(70));
            System.out.println("💡 Use any symbol above to see detailed information");
            
        } catch (SQLException e) {
            System.err.println("❌ Database error: " + e.getMessage());
        }
    }
    
    private void displayGainerRow(int rank, Map<String, Object> stock) {
        String symbol = (String) stock.get("symbol");
        String displayName = (String) stock.get("display_name");
        double lastPrice = ((Number) stock.get("last_price")).doubleValue();
        double change24h = ((Number) stock.get("change_24h")).doubleValue();
        
        // Truncate display name if too long
        String shortName = displayName.length() > 20 ? displayName.substring(0, 17) + "..." : displayName;
        
        // Format change with arrow
        String arrow = change24h >= 0 ? "▲" : "▼";
        
        System.out.printf("%2d   │ %-8s │ %-20s │ $%8.2f │ %s%6.2f%%%n", 
                         rank, symbol, shortName, lastPrice, arrow, change24h * 100);
    }
    
    /**
     * Demonstrates symbol lookup with MC_STONE.
     */
    private void showSymbolLookup() {
        System.out.println("🔍 Symbol Lookup Demo");
        lookupStock("MC_STONE");
    }
    
    /**
     * Demonstrates material lookup with "stone".
     */
    private void showMaterialLookup() {
        System.out.println("🔍 Material Lookup Demo");
        lookupStock("stone");
    }
    
    /**
     * Shows tab completion example.
     */
    private void showTabCompletion() {
        try {
            System.out.println("📝 Tab Completion Demo");
            System.out.println();
            
            String[] prefixes = {"MC", "ST", "IR"};
            
            for (String prefix : prefixes) {
                List<String> matches = queryService.getMatchingSymbolsAndMaterials(prefix);
                System.out.printf("Prefix '%s': %s%n", prefix, 
                                 matches.size() > 5 ? matches.subList(0, 5) + "..." : matches);
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Database error: " + e.getMessage());
        }
    }
    
    /**
     * Looks up a specific stock by symbol or material.
     */
    private void lookupStock(String query) {
        try {
            // Try to resolve query: first by symbol, then by mc_material
            Optional<Map<String, Object>> stockOpt = queryService.findBySymbol(query);
            
            if (stockOpt.isEmpty()) {
                stockOpt = queryService.findByMcMaterial(query);
            }
            
            if (stockOpt.isEmpty()) {
                System.out.println("❌ Stock not found: " + query);
                System.out.println("💡 Try one of the symbols from the top gainers list");
                return;
            }
            
            Map<String, Object> stock = stockOpt.get();
            displayStockCard(stock);
            
        } catch (SQLException e) {
            System.err.println("❌ Database error: " + e.getMessage());
        }
    }
    
    /**
     * Displays a detailed stock information card.
     */
    private void displayStockCard(Map<String, Object> stock) throws SQLException {
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
        System.out.println();
        System.out.printf("📊 %s (%s) [%s]%n", displayName, symbol, type);
        System.out.println("━".repeat(50));
        
        // Price information
        System.out.printf("💰 Price: $%.2f%n", lastPrice);
        
        // Changes
        displayChangeRow("📈 Δ1h:", change1h);
        displayChangeRow("📊 Δ24h:", change24h);
        
        // Volatility and Market Cap
        System.out.printf("⚡ Volatility 24h: %.4f%n", volatility24h);
        
        if (marketCap > 0) {
            System.out.printf("🏢 Market Cap: $%.0f%n", marketCap);
        }
        
        // Recent price history
        displayPriceHistory(id);
        System.out.println("━".repeat(50));
    }
    
    /**
     * Displays a change row with appropriate arrows.
     */
    private void displayChangeRow(String label, double change) {
        String arrow = change >= 0 ? "▲" : "▼";
        System.out.printf("%s %s%.2f%%%n", label, arrow, change * 100);
    }
    
    /**
     * Displays recent price history with mini sparkline.
     */
    private void displayPriceHistory(String instrumentId) throws SQLException {
        List<Map<String, Object>> history = queryService.getRecentPriceHistory(instrumentId, 10);
        
        if (history.isEmpty()) {
            return;
        }
        
        System.out.println("📋 Last 10 history points:");
        
        // Create mini sparkline
        String sparkline = createSparkline(history);
        System.out.println("   " + sparkline + " (latest → oldest)");
        
        // Show most recent price point details
        if (!history.isEmpty()) {
            Map<String, Object> latest = history.get(0);
            long timestamp = ((Number) latest.get("ts")).longValue();
            double price = ((Number) latest.get("price")).doubleValue();
            String reason = (String) latest.get("reason");
            
            String timeStr = dateFormat.format(new Date(timestamp));
            System.out.printf("   Latest: $%.2f at %s (%s)%n", 
                             price, timeStr, reason != null ? reason : "unknown");
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
        double[] prices = new double[history.size()];
        for (int i = 0; i < history.size(); i++) {
            prices[i] = ((Number) history.get(history.size() - 1 - i).get("price")).doubleValue();
        }
        
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        for (double price : prices) {
            min = Math.min(min, price);
            max = Math.max(max, price);
        }
        
        double range = max - min;
        if (range == 0) {
            return "▬".repeat(Math.min(10, prices.length));
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
}