package com.example.quickstocks.commands;

import com.example.quickstocks.core.services.CryptoService;
import com.example.quickstocks.infrastructure.db.DatabaseConfig;
import com.example.quickstocks.infrastructure.db.DatabaseManager;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Scanner;
import java.util.UUID;

/**
 * Demonstration of CryptoCommand functionality without Paper API dependencies.
 * This shows how the command would work in-game with proper permission checking.
 */
public class CryptoCommandDemo {
    
    private static final String PERMISSION_CREATE = "maksy.stocks.crypto.create";
    
    private final CryptoService cryptoService;
    
    public CryptoCommandDemo(CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }
    
    public static void main(String[] args) throws SQLException {
        System.out.println("🎮 CryptoCommand Demo - Interactive Command Simulation");
        System.out.println("=====================================================\n");
        
        // Set up database
        DatabaseConfig config = new DatabaseConfig();
        config.setProvider("sqlite");
        config.setSqliteFile("/tmp/crypto_command_demo.db");
        
        DatabaseManager databaseManager = new DatabaseManager(config, true);
        databaseManager.initialize();
        
        try {
            CryptoService cryptoService = new CryptoService(databaseManager.getDb());
            CryptoCommandDemo demo = new CryptoCommandDemo(cryptoService);
            
            Scanner scanner = new Scanner(System.in);
            
            System.out.println("Welcome to the Crypto Command Demo!");
            System.out.println("This simulates how the /crypto command would work in-game.\n");
            
            // Simulate different players and permissions
            System.out.println("📋 Available test scenarios:");
            System.out.println("1. Player with permission tries to create crypto");
            System.out.println("2. Player without permission tries to create crypto");
            System.out.println("3. Invalid command usage");
            System.out.println("4. Duplicate symbol creation attempt");
            System.out.println("5. Interactive mode\n");
            
            System.out.print("Choose scenario (1-5): ");
            String choice = scanner.nextLine().trim();
            
            switch (choice) {
                case "1":
                    demo.simulateScenario1();  
                    break;
                case "2":
                    demo.simulateScenario2();
                    break;
                case "3":
                    demo.simulateScenario3();
                    break;
                case "4":
                    demo.simulateScenario4();
                    break;
                case "5":
                    demo.interactiveMode(scanner);
                    break;
                default:
                    System.out.println("❌ Invalid choice. Running scenario 1.");
                    demo.simulateScenario1();
            }
            
        } finally {
            databaseManager.shutdown();
            System.out.println("\n🎉 Demo completed!");
        }
    }
    
    private void simulateScenario1() {
        System.out.println("\n📊 Scenario 1: Player WITH permission creates crypto");
        System.out.println("Player: TestPlayer (UUID: " + UUID.randomUUID().toString().substring(0, 8) + "...)");
        System.out.println("Permission: " + PERMISSION_CREATE + " = true");
        System.out.println("Command: /crypto create TESTCOIN \"Test Coin\"\n");
        
        MockPlayer player = new MockPlayer("TestPlayer", true);
        String[] args = {"create", "TESTCOIN", "Test", "Coin"};
        
        executeCommand(player, args);
    }
    
    private void simulateScenario2() {
        System.out.println("\n📊 Scenario 2: Player WITHOUT permission tries to create crypto");
        System.out.println("Player: NoPermPlayer (UUID: " + UUID.randomUUID().toString().substring(0, 8) + "...)");
        System.out.println("Permission: " + PERMISSION_CREATE + " = false");
        System.out.println("Command: /crypto create DENIED \"Denied Coin\"\n");
        
        MockPlayer player = new MockPlayer("NoPermPlayer", false);
        String[] args = {"create", "DENIED", "Denied", "Coin"};
        
        executeCommand(player, args);
    }
    
    private void simulateScenario3() {
        System.out.println("\n📊 Scenario 3: Invalid command usage");
        System.out.println("Player: TestPlayer (UUID: " + UUID.randomUUID().toString().substring(0, 8) + "...)");
        System.out.println("Permission: " + PERMISSION_CREATE + " = true");
        System.out.println("Command: /crypto create INVALID (missing name)\n");
        
        MockPlayer player = new MockPlayer("TestPlayer", true);
        String[] args = {"create", "INVALID"};
        
        executeCommand(player, args);
    }
    
    private void simulateScenario4() {
        System.out.println("\n📊 Scenario 4: Duplicate symbol creation");
        
        // First, create a crypto
        MockPlayer player1 = new MockPlayer("Player1", true);
        String[] args1 = {"create", "DUPLICATE", "First", "Coin"};
        System.out.println("First creation:");
        executeCommand(player1, args1);
        
        System.out.println("\nSecond creation attempt:");
        MockPlayer player2 = new MockPlayer("Player2", true);
        String[] args2 = {"create", "DUPLICATE", "Second", "Coin"};
        executeCommand(player2, args2);
    }
    
    private void interactiveMode(Scanner scanner) {
        System.out.println("\n🎮 Interactive Mode");
        System.out.println("Type commands like: create SYMBOL Display Name");
        System.out.println("Type 'exit' to quit\n");
        
        MockPlayer player = new MockPlayer("InteractivePlayer", true);
        
        while (true) {
            System.out.print("crypto> ");
            String input = scanner.nextLine().trim();
            
            if ("exit".equalsIgnoreCase(input)) {
                break;
            }
            
            if (input.isEmpty()) {
                continue;
            }
            
            String[] args = input.split("\\s+");
            executeCommand(player, args);
            System.out.println();
        }
    }
    
    /**
     * Simulates the CryptoCommand.onCommand method execution.
     */
    private void executeCommand(MockPlayer player, String[] args) {
        if (args.length == 0) {
            showUsage(player);
            return;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "create":
                handleCreateCommand(player, Arrays.copyOfRange(args, 1, args.length));
                break;
            default:
                showUsage(player);
                break;
        }
    }
    
    /**
     * Simulates the create command handling with permission checks and validation.
     */
    private void handleCreateCommand(MockPlayer player, String[] args) {
        // Check permission
        if (!player.hasPermission(PERMISSION_CREATE)) {
            player.sendMessage("❌ You don't have permission to create custom crypto.");
            player.sendMessage("💡 Required permission: " + PERMISSION_CREATE);
            return;
        }
        
        // Validate arguments
        if (args.length < 2) {
            player.sendMessage("❌ Usage: /crypto create <symbol> <name>");
            player.sendMessage("💡 Example: /crypto create MYCOIN \"My Custom Coin\"");
            return;
        }
        
        String symbol = args[0];
        String displayName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        
        try {
            // Create the custom crypto
            String instrumentId = cryptoService.createCustomCrypto(symbol, displayName, player.getUniqueId());
            
            // Success message
            player.sendMessage("");
            player.sendMessage("🎉 Custom Crypto Created Successfully!");
            player.sendMessage("━".repeat(40));
            player.sendMessage("💰 Symbol: " + symbol.toUpperCase());
            player.sendMessage("📝 Name: " + displayName);
            player.sendMessage("💲 Starting Price: $1.00");
            player.sendMessage("🆔 Instrument ID: " + instrumentId);
            player.sendMessage("━".repeat(40));
            player.sendMessage("💡 Your crypto is now tradeable on the market!");
            player.sendMessage("💡 Use /stocks " + symbol.toUpperCase() + " to view details");
            
        } catch (IllegalArgumentException e) {
            player.sendMessage("❌ " + e.getMessage());
        } catch (Exception e) {
            player.sendMessage("❌ Unexpected error: " + e.getMessage());
        }
    }
    
    /**
     * Shows command usage information.
     */
    private void showUsage(MockPlayer player) {
        player.sendMessage("");
        player.sendMessage("🪙 Crypto Commands");
        player.sendMessage("━".repeat(30));
        player.sendMessage("• /crypto create <symbol> <name>");
        player.sendMessage("  Creates a custom cryptocurrency");
        player.sendMessage("");
        player.sendMessage("Examples:");
        player.sendMessage("  /crypto create MYCOIN \"My Custom Coin\"");
        player.sendMessage("  /crypto create GOLD \"Digital Gold\"");
        player.sendMessage("");
        
        boolean hasPermission = player.hasPermission(PERMISSION_CREATE);
        String permissionStatus = "Permission: " + PERMISSION_CREATE + " - " + 
                                (hasPermission ? "✅ Granted" : "❌ Denied");
        player.sendMessage(permissionStatus);
    }
    
    /**
     * Mock player class to simulate Bukkit Player without dependencies.
     */
    private static class MockPlayer {
        private final String name;
        private final String uuid;
        private final boolean hasCreatePermission;
        
        public MockPlayer(String name, boolean hasCreatePermission) {
            this.name = name;
            this.uuid = UUID.randomUUID().toString();
            this.hasCreatePermission = hasCreatePermission;
        }
        
        public String getName() {
            return name;
        }
        
        public String getUniqueId() {
            return uuid;
        }
        
        public boolean hasPermission(String permission) {
            if (PERMISSION_CREATE.equals(permission)) {
                return hasCreatePermission;
            }
            return false; // Default deny other permissions
        }
        
        public void sendMessage(String message) {
            System.out.println("[" + name + "] " + message);
        }
    }
}