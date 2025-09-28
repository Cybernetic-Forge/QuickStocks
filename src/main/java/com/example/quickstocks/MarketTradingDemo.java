package com.example.quickstocks;

import com.example.quickstocks.application.queries.QueryService;
import com.example.quickstocks.core.services.HoldingsService;
import com.example.quickstocks.core.services.TradingServiceStandalone;
import com.example.quickstocks.core.services.WalletService;
import com.example.quickstocks.infrastructure.db.DatabaseConfig;
import com.example.quickstocks.infrastructure.db.DatabaseManager;
import com.example.quickstocks.infrastructure.db.Db;

import java.util.List;
import java.util.UUID;

/**
 * Demo program to test the market trading functionality.
 */
public class MarketTradingDemo {
    
    public static void main(String[] args) {
        try {
            System.out.println("=== QuickStocks Market Trading Demo ===\n");
            
            // Setup database
            DatabaseConfig config = new DatabaseConfig();
            config.setProvider("sqlite");
            config.setSqliteFile("demo_trading.db");
            
            DatabaseManager dbManager = new DatabaseManager(config, true);
            dbManager.initialize();
            Db db = dbManager.getDb();
            
            // Initialize services
            WalletService walletService = new WalletService(db);
            HoldingsService holdingsService = new HoldingsService(db);
            TradingServiceStandalone tradingService = new TradingServiceStandalone(db, walletService, holdingsService);
            QueryService queryService = new QueryService(db);
            
            // Test player UUID
            String testPlayerUuid = UUID.randomUUID().toString();
            System.out.println("Test Player UUID: " + testPlayerUuid.substring(0, 8) + "...\n");
            
            // 1. Test Wallet Operations
            System.out.println("1. Testing Wallet Operations:");
            System.out.println("Initial balance: $" + String.format("%.2f", walletService.getBalance(testPlayerUuid)));
            
            walletService.setBalance(testPlayerUuid, 10000.0);
            System.out.println("Set balance to: $10,000.00");
            System.out.println("Current balance: $" + String.format("%.2f", walletService.getBalance(testPlayerUuid)));
            System.out.println();
            
            // 2. Check if we have instruments
            System.out.println("2. Checking Available Instruments:");
            var topInstruments = queryService.getTopGainers(5);
            if (topInstruments.isEmpty()) {
                System.out.println("No instruments found. Creating test instrument...");
                
                // Insert a test instrument directly
                String testInstructId = UUID.randomUUID().toString();
                db.execute(
                    "INSERT INTO instruments (id, type, symbol, display_name, decimals, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                    testInstructId, "EQUITY", "TEST", "Test Stock", 2, System.currentTimeMillis()
                );
                
                // Insert initial state
                db.execute(
                    "INSERT INTO instrument_state (instrument_id, last_price, updated_at) VALUES (?, ?, ?)",
                    testInstructId, 50.0, System.currentTimeMillis()
                );
                
                System.out.println("Created TEST stock at $50.00");
                topInstruments = queryService.getTopGainers(5);
            }
            
            for (var instrument : topInstruments) {
                System.out.println(String.format("- %s (%s): $%.2f", 
                    instrument.get("display_name"), 
                    instrument.get("symbol"), 
                    instrument.get("last_price")));
            }
            System.out.println();
            
            // 3. Test Buy Order
            System.out.println("3. Testing Buy Order:");
            String testSymbol = (String) topInstruments.get(0).get("symbol");
            String instrumentId = queryService.getInstrumentIdBySymbol(testSymbol);
            
            TradingServiceStandalone.TradeResult buyResult = tradingService.executeBuyOrder(testPlayerUuid, instrumentId, 10.0);
            System.out.println("Buy Result: " + (buyResult.isSuccess() ? "SUCCESS" : "FAILED"));
            System.out.println("Message: " + buyResult.getMessage());
            
            System.out.println("Balance after buy: $" + String.format("%.2f", walletService.getBalance(testPlayerUuid)));
            System.out.println();
            
            // 4. Check Holdings
            System.out.println("4. Checking Holdings:");
            List<HoldingsService.Holding> holdings = holdingsService.getHoldings(testPlayerUuid);
            if (holdings.isEmpty()) {
                System.out.println("No holdings found.");
            } else {
                for (HoldingsService.Holding holding : holdings) {
                    System.out.println(String.format("- %s: %.2f shares @ $%.2f avg (Current: $%.2f, P&L: $%.2f)", 
                        holding.getSymbol(), holding.getQty(), holding.getAvgCost(), 
                        holding.getCurrentPrice(), holding.getUnrealizedPnL()));
                }
            }
            System.out.println("Portfolio Value: $" + String.format("%.2f", holdingsService.getPortfolioValue(testPlayerUuid)));
            System.out.println();
            
            // 5. Test Sell Order
            System.out.println("5. Testing Sell Order:");
            TradingServiceStandalone.TradeResult sellResult = tradingService.executeSellOrder(testPlayerUuid, instrumentId, 5.0);
            System.out.println("Sell Result: " + (sellResult.isSuccess() ? "SUCCESS" : "FAILED"));
            System.out.println("Message: " + sellResult.getMessage());
            
            System.out.println("Balance after sell: $" + String.format("%.2f", walletService.getBalance(testPlayerUuid)));
            System.out.println();
            
            // 6. Check Updated Holdings
            System.out.println("6. Updated Holdings:");
            holdings = holdingsService.getHoldings(testPlayerUuid);
            if (holdings.isEmpty()) {
                System.out.println("No holdings found.");
            } else {
                for (HoldingsService.Holding holding : holdings) {
                    System.out.println(String.format("- %s: %.2f shares @ $%.2f avg (Current: $%.2f, P&L: $%.2f)", 
                        holding.getSymbol(), holding.getQty(), holding.getAvgCost(), 
                        holding.getCurrentPrice(), holding.getUnrealizedPnL()));
                }
            }
            System.out.println();
            
            // 7. Order History
            System.out.println("7. Order History:");
            List<TradingServiceStandalone.Order> orders = tradingService.getOrderHistory(testPlayerUuid, 10);
            for (TradingServiceStandalone.Order order : orders) {
                System.out.println(String.format("%s %s: %.2f @ $%.2f (Total: $%.2f) - %s", 
                    order.getSide(), order.getSymbol(), order.getQty(), order.getPrice(), 
                    order.getTotalValue(), new java.util.Date(order.getTimestamp())));
            }
            
            System.out.println("\n=== Demo Completed Successfully ===");
            
            // Cleanup
            dbManager.shutdown();
            
        } catch (Exception e) {
            System.err.println("Demo failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}