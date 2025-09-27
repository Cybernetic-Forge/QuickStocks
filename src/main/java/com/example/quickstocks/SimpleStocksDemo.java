package com.example.quickstocks;

import com.example.quickstocks.application.queries.QueryService;
import com.example.quickstocks.infrastructure.db.DatabaseConfig;
import com.example.quickstocks.infrastructure.db.DatabaseManager;
import com.example.quickstocks.infrastructure.db.Db;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Simple demonstration of the /stocks command functionality.
 */
public class SimpleStocksDemo {
    
    public static void main(String[] args) throws SQLException {
        System.out.println("🚀 QuickStocks /stocks Command Demo");
        System.out.println("===================================\n");
        
        // Create database configuration
        DatabaseConfig config = new DatabaseConfig();
        config.setProvider("sqlite");
        config.setSqliteFile("/tmp/simple_stocks_demo.db");
        
        // Initialize database
        DatabaseManager databaseManager = new DatabaseManager(config, true);
        databaseManager.initialize();
        
        try {
            QueryService queryService = new QueryService(databaseManager.getDb());
            
            // Add some realistic test data with price movements
            addTestDataWithMovements(databaseManager.getDb());
            
            System.out.println("📊 Demonstration of /stocks command functionality:\n");
            
            // Demo 1: Top 10 gainers (equivalent to "/stocks" with no args)
            System.out.println("1️⃣  Command: /stocks (no arguments)");
            System.out.println("   Shows top 10 gainers sorted by 24h change DESC\n");
            
            showTopGainers(queryService);
            
            System.out.println("\n" + "=".repeat(60) + "\n");
            
            // Demo 2: Symbol lookup
            System.out.println("2️⃣  Command: /stocks MC_STONE");
            System.out.println("   Looks up stock by exact symbol match (case-insensitive)\n");
            
            showStockCard(queryService, "MC_STONE");
            
            System.out.println("\n" + "=".repeat(60) + "\n");
            
            // Demo 3: Material lookup  
            System.out.println("3️⃣  Command: /stocks stone");
            System.out.println("   Looks up stock by Minecraft material (case-insensitive)\n");
            
            showStockCard(queryService, "stone");
            
            System.out.println("\n" + "=".repeat(60) + "\n");
            
            // Demo 4: Tab completion
            System.out.println("4️⃣  Tab Completion Examples");
            System.out.println("   Shows symbols and materials matching prefix\n");
            
            showTabCompletionExamples(queryService);
            
            // Verify acceptance criteria
            System.out.println("\n" + "=".repeat(60) + "\n");
            System.out.println("✅ Acceptance Criteria Verification:");
            
            verifyAcceptanceCriteria(queryService);
            
        } finally {
            databaseManager.shutdown();
            System.out.println("\n🎉 Demo completed successfully!");
        }
    }
    
    private static void addTestDataWithMovements(Db db) throws SQLException {
        // Add some test data with actual price movements
        long now = System.currentTimeMillis();
        
        // Update existing items to have some price movements
        db.execute("""
            UPDATE instrument_state 
            SET change_24h = CASE 
                WHEN EXISTS (SELECT 1 FROM instruments WHERE id = instrument_state.instrument_id AND symbol = 'MC_DIAMOND') THEN 0.15
                WHEN EXISTS (SELECT 1 FROM instruments WHERE id = instrument_state.instrument_id AND symbol = 'MC_GOLD') THEN 0.12
                WHEN EXISTS (SELECT 1 FROM instruments WHERE id = instrument_state.instrument_id AND symbol = 'MC_IRON') THEN 0.08
                WHEN EXISTS (SELECT 1 FROM instruments WHERE id = instrument_state.instrument_id AND symbol = 'MC_STONE') THEN 0.05
                WHEN EXISTS (SELECT 1 FROM instruments WHERE id = instrument_state.instrument_id AND symbol = 'MC_COAL') THEN -0.02
                ELSE change_24h 
            END,
            change_1h = CASE
                WHEN EXISTS (SELECT 1 FROM instruments WHERE id = instrument_state.instrument_id AND symbol = 'MC_DIAMOND') THEN 0.03
                WHEN EXISTS (SELECT 1 FROM instruments WHERE id = instrument_state.instrument_id AND symbol = 'MC_GOLD') THEN 0.02
                WHEN EXISTS (SELECT 1 FROM instruments WHERE id = instrument_state.instrument_id AND symbol = 'MC_IRON') THEN 0.01
                WHEN EXISTS (SELECT 1 FROM instruments WHERE id = instrument_state.instrument_id AND symbol = 'MC_STONE') THEN 0.005
                WHEN EXISTS (SELECT 1 FROM instruments WHERE id = instrument_state.instrument_id AND symbol = 'MC_COAL') THEN -0.01
                ELSE change_1h
            END,
            volatility_24h = CASE
                WHEN EXISTS (SELECT 1 FROM instruments WHERE id = instrument_state.instrument_id AND symbol = 'MC_DIAMOND') THEN 0.25
                WHEN EXISTS (SELECT 1 FROM instruments WHERE id = instrument_state.instrument_id AND symbol = 'MC_GOLD') THEN 0.18
                WHEN EXISTS (SELECT 1 FROM instruments WHERE id = instrument_state.instrument_id AND symbol = 'MC_IRON') THEN 0.15
                WHEN EXISTS (SELECT 1 FROM instruments WHERE id = instrument_state.instrument_id AND symbol = 'MC_STONE') THEN 0.12
                WHEN EXISTS (SELECT 1 FROM instruments WHERE id = instrument_state.instrument_id AND symbol = 'MC_COAL') THEN 0.20
                ELSE volatility_24h
            END
            WHERE EXISTS (
                SELECT 1 FROM instruments i 
                WHERE i.id = instrument_state.instrument_id 
                AND i.symbol IN ('MC_DIAMOND', 'MC_GOLD', 'MC_IRON', 'MC_STONE', 'MC_COAL')
            )
            """);
        
        // Add some price history for MC_STONE
        String stoneId = db.queryValue("""
            SELECT id FROM instruments WHERE symbol = 'MC_STONE'
            """);
        
        if (stoneId != null) {
            for (int i = 0; i < 10; i++) {
                db.execute("""
                    INSERT INTO instrument_price_history (id, instrument_id, ts, price, volume, reason)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """, 
                    "hist_stone_" + i, 
                    stoneId, 
                    now - (i * 3600000), // 1 hour intervals
                    1.0 + (Math.random() * 0.2 - 0.1), // Random price around 1.0
                    100 + (int)(Math.random() * 50),
                    "Market volatility");
            }
        }
    }
    
    private static void showTopGainers(QueryService queryService) throws SQLException {
        List<Map<String, Object>> gainers = queryService.getTopGainersByChange24h(10);
        
        System.out.println("📈 TOP 10 GAINERS (24H)");
        System.out.println("═".repeat(70));
        System.out.printf("%-4s │ %-10s │ %-15s │ %10s │ %12s%n", 
                         "RANK", "SYMBOL", "NAME", "PRICE", "24H CHANGE");
        System.out.println("─".repeat(70));
        
        for (int i = 0; i < Math.min(10, gainers.size()); i++) {
            Map<String, Object> stock = gainers.get(i);
            String symbol = (String) stock.get("symbol");
            String displayName = (String) stock.get("display_name");
            double lastPrice = ((Number) stock.get("last_price")).doubleValue();
            double change24h = ((Number) stock.get("change_24h")).doubleValue();
            
            String shortName = displayName.length() > 15 ? displayName.substring(0, 12) + "..." : displayName;
            String arrow = change24h > 0 ? "▲" : change24h < 0 ? "▼" : "−";
            
            System.out.printf("%2d   │ %-10s │ %-15s │ $%8.2f │ %s%9.2f%%%n",
                             i + 1, symbol, shortName, lastPrice, arrow, change24h * 100);
        }
        
        System.out.println("─".repeat(70));
        System.out.println("💡 Use /stocks <symbol> for detailed information");
    }
    
    private static void showStockCard(QueryService queryService, String query) throws SQLException {
        // Try to resolve query: first by symbol, then by mc_material
        Optional<Map<String, Object>> stockOpt = queryService.findBySymbol(query);
        
        if (stockOpt.isEmpty()) {
            stockOpt = queryService.findByMcMaterial(query);
        }
        
        if (stockOpt.isEmpty()) {
            System.out.println("❌ Stock not found: " + query);
            return;
        }
        
        Map<String, Object> stock = stockOpt.get();
        String symbol = (String) stock.get("symbol");
        String displayName = (String) stock.get("display_name");
        String type = (String) stock.get("type");
        String id = (String) stock.get("id");
        double lastPrice = ((Number) stock.get("last_price")).doubleValue();
        double change1h = ((Number) stock.get("change_1h")).doubleValue();
        double change24h = ((Number) stock.get("change_24h")).doubleValue();
        double volatility24h = ((Number) stock.get("volatility_24h")).doubleValue();
        double marketCap = ((Number) stock.get("market_cap")).doubleValue();
        
        // Display stock card
        System.out.printf("📊 %s (%s) [%s]%n", displayName, symbol, type);
        System.out.println("━".repeat(50));
        System.out.printf("💰 Price: $%.2f%n", lastPrice);
        
        String arrow1h = change1h >= 0 ? "▲" : "▼";
        String arrow24h = change24h >= 0 ? "▲" : "▼";
        
        System.out.printf("📈 Δ1h: %s%.2f%%%n", arrow1h, change1h * 100);
        System.out.printf("📊 Δ24h: %s%.2f%%%n", arrow24h, change24h * 100);
        System.out.printf("⚡ Volatility 24h: %.4f%n", volatility24h);
        
        if (marketCap > 0) {
            System.out.printf("🏢 Market Cap: $%.0f%n", marketCap);
        }
        
        // Show price history if available
        List<Map<String, Object>> history = queryService.getRecentPriceHistory(id, 10);
        if (!history.isEmpty()) {
            System.out.println("📋 Last 10 history points:");
            System.out.printf("   %s (latest → oldest)%n", createSparkline(history));
        }
        
        System.out.println("━".repeat(50));
    }
    
    private static void showTabCompletionExamples(QueryService queryService) throws SQLException {
        String[] prefixes = {"MC", "ST", "IR", "GO"};
        
        for (String prefix : prefixes) {
            List<String> matches = queryService.getMatchingSymbolsAndMaterials(prefix);
            System.out.printf("Prefix '%s': %s%n", prefix, 
                             matches.size() > 5 ? matches.subList(0, 5) + " ..." : matches);
        }
    }
    
    private static void verifyAcceptanceCriteria(QueryService queryService) throws SQLException {
        // Test 1: /stocks renders lines with correct ordering
        List<Map<String, Object>> gainers = queryService.getTopGainersByChange24h(10);
        boolean correctOrdering = true;
        
        for (int i = 0; i < gainers.size() - 1; i++) {
            double current = ((Number) gainers.get(i).get("change_24h")).doubleValue();
            double next = ((Number) gainers.get(i + 1).get("change_24h")).doubleValue();
            if (current < next) {
                correctOrdering = false;
                break;
            }
        }
        
        System.out.printf("1. Top gainers correctly ordered by change_24h DESC: %s%n", 
                         correctOrdering ? "✅ PASS" : "❌ FAIL");
        
        // Test 2: MC_STONE and stone both resolve to stone material
        Optional<Map<String, Object>> bySymbol = queryService.findBySymbol("MC_STONE");
        Optional<Map<String, Object>> byMaterial = queryService.findByMcMaterial("stone");
        
        boolean symbolResolved = bySymbol.isPresent();
        boolean materialResolved = byMaterial.isPresent();
        
        System.out.printf("2. /stocks MC_STONE resolves: %s%n", 
                         symbolResolved ? "✅ PASS" : "❌ FAIL");
        System.out.printf("3. /stocks stone resolves: %s%n", 
                         materialResolved ? "✅ PASS" : "❌ FAIL");
        
        if (symbolResolved && materialResolved) {
            String symbolMaterial = (String) bySymbol.get().get("mc_material");
            String materialMaterial = (String) byMaterial.get().get("mc_material");
            boolean bothStone = "STONE".equals(symbolMaterial) && "STONE".equals(materialMaterial);
            
            System.out.printf("4. Both resolve to STONE material: %s%n", 
                             bothStone ? "✅ PASS" : "❌ FAIL");
        }
        
        // Test tab completion
        List<String> tabResults = queryService.getMatchingSymbolsAndMaterials("MC");
        boolean hasTabCompletion = !tabResults.isEmpty();
        
        System.out.printf("5. Tab completion works: %s%n", 
                         hasTabCompletion ? "✅ PASS" : "❌ FAIL");
    }
    
    private static String createSparkline(List<Map<String, Object>> history) {
        if (history.size() < 2) {
            return "▬";
        }
        
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