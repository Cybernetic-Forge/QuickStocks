package com.example.quickstocks;

import com.example.quickstocks.application.queries.QueryService;
import com.example.quickstocks.core.services.HoldingsService;
import com.example.quickstocks.core.services.StockMarketService;
import com.example.quickstocks.core.services.TradingService;
import com.example.quickstocks.core.services.WalletService;
import com.example.quickstocks.infrastructure.db.DatabaseConfig;
import com.example.quickstocks.infrastructure.db.DatabaseManager;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Demo application for testing market trading functionality.
 */
public class MarketTradingDemo {
    
    public static void main(String[] args) {
        System.out.println("QuickStocks Market Trading Demo");
        System.out.println("===============================");
        
        try {
            // Initialize database
            DatabaseConfig config = new DatabaseConfig();
            config.setProvider("sqlite");
            config.setSqliteFile("plugins/QuickStocks/demo-data.db");
            
            DatabaseManager databaseManager = new DatabaseManager(config, true);
            databaseManager.initialize();
            
            // Initialize services
            StockMarketService stockMarketService = new StockMarketService();
            WalletService walletService = new WalletService(databaseManager.getDb());
            HoldingsService holdingsService = new HoldingsService(databaseManager.getDb());
            TradingService tradingService = new TradingService(databaseManager.getDb(), walletService, holdingsService);
            QueryService queryService = new QueryService(databaseManager.getDb());
            
            // Add some demo stocks
            stockMarketService.addStock("DEMO", "Demo Corporation", "Technology", 100.0);
            stockMarketService.addStock("TEST", "Test Industries", "Finance", 50.0);
            
            // Demo player UUID
            String playerUuid = "demo-player-uuid";
            
            // Set initial wallet balance
            walletService.setBalance(playerUuid, 10000.0);
            
            System.out.println("\nDemo Setup Complete!");
            System.out.println("Player balance: $" + String.format("%.2f", walletService.getBalance(playerUuid)));
            
            // Interactive demo
            Scanner scanner = new Scanner(System.in);
            
            while (true) {
                System.out.println("\nAvailable Commands:");
                System.out.println("1. View Market");
                System.out.println("2. Buy Stock");
                System.out.println("3. Sell Stock");
                System.out.println("4. View Portfolio");
                System.out.println("5. View Balance");
                System.out.println("6. Exit");
                System.out.print("Enter choice: ");
                
                String choice = scanner.nextLine();
                
                switch (choice) {
                    case "1":
                        viewMarket(queryService);
                        break;
                    case "2":
                        buyStock(scanner, tradingService, playerUuid);
                        break;
                    case "3":
                        sellStock(scanner, tradingService, playerUuid);
                        break;
                    case "4":
                        viewPortfolio(holdingsService, playerUuid);
                        break;
                    case "5":
                        viewBalance(walletService, playerUuid);
                        break;
                    case "6":
                        System.out.println("Exiting demo...");
                        databaseManager.shutdown();
                        return;
                    default:
                        System.out.println("Invalid choice!");
                }
            }
            
        } catch (Exception e) {
            System.err.println("Demo error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void viewMarket(QueryService queryService) throws SQLException {
        System.out.println("\n=== Market Overview ===");
        List<Map<String, Object>> stocks = queryService.getTopGainersByChange24h(10);
        for (Map<String, Object> stock : stocks) {
            String symbol = (String) stock.get("symbol");
            String name = (String) stock.get("display_name");
            double price = ((Number) stock.get("last_price")).doubleValue();
            System.out.println(symbol + " - " + name + " @ $" + String.format("%.2f", price));
        }
    }
    
    private static void buyStock(Scanner scanner, TradingService tradingService, String playerUuid) throws SQLException {
        System.out.print("Enter stock symbol to buy: ");
        String symbol = scanner.nextLine().toUpperCase();
        
        System.out.print("Enter quantity: ");
        try {
            double qty = Double.parseDouble(scanner.nextLine());
            
            // For demo, we'll use a hardcoded instrument ID
            String instrumentId = "instrument_" + symbol.toLowerCase();
            
            boolean success = tradingService.executeBuyOrder(playerUuid, instrumentId, qty);
            
            if (success) {
                System.out.println("✅ Buy order executed successfully!");
            } else {
                System.out.println("❌ Buy order failed - insufficient funds or invalid instrument");
            }
        } catch (NumberFormatException e) {
            System.out.println("❌ Invalid quantity");
        }
    }
    
    private static void sellStock(Scanner scanner, TradingService tradingService, String playerUuid) throws SQLException {
        System.out.print("Enter stock symbol to sell: ");
        String symbol = scanner.nextLine().toUpperCase();
        
        System.out.print("Enter quantity: ");
        try {
            double qty = Double.parseDouble(scanner.nextLine());
            
            // For demo, we'll use a hardcoded instrument ID
            String instrumentId = "instrument_" + symbol.toLowerCase();
            
            boolean success = tradingService.executeSellOrder(playerUuid, instrumentId, qty);
            
            if (success) {
                System.out.println("✅ Sell order executed successfully!");
            } else {
                System.out.println("❌ Sell order failed - insufficient holdings or invalid instrument");
            }
        } catch (NumberFormatException e) {
            System.out.println("❌ Invalid quantity");
        }
    }
    
    private static void viewPortfolio(HoldingsService holdingsService, String playerUuid) throws SQLException {
        System.out.println("\n=== Your Portfolio ===");
        List<Map<String, Object>> holdings = holdingsService.getHoldings(playerUuid);
        
        if (holdings.isEmpty()) {
            System.out.println("No holdings found.");
        } else {
            double totalValue = 0;
            for (Map<String, Object> holding : holdings) {
                String symbol = (String) holding.get("symbol");
                double qty = ((Number) holding.get("qty")).doubleValue();
                double avgCost = ((Number) holding.get("avg_cost")).doubleValue();
                Double currentPrice = (Double) holding.get("last_price");
                
                if (currentPrice != null) {
                    double currentValue = qty * currentPrice;
                    double pnl = currentValue - (qty * avgCost);
                    totalValue += currentValue;
                    
                    System.out.println(symbol + " - " + qty + " shares @ $" + String.format("%.2f", avgCost) + 
                                     " avg (Current: $" + String.format("%.2f", currentPrice) + 
                                     ", P&L: $" + String.format("%.2f", pnl) + ")");
                }
            }
            System.out.println("Total Portfolio Value: $" + String.format("%.2f", totalValue));
        }
    }
    
    private static void viewBalance(WalletService walletService, String playerUuid) throws SQLException {
        double balance = walletService.getBalance(playerUuid);
        System.out.println("\n💰 Current Balance: $" + String.format("%.2f", balance));
    }
}