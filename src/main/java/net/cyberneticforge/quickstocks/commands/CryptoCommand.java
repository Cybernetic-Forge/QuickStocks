package net.cyberneticforge.quickstocks.commands;

import net.cyberneticforge.quickstocks.core.services.CryptoService;
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
import java.util.Arrays;
import java.util.List;

/**
 * /crypto command implementation for creating custom cryptocurrency instruments.
 * Requires the permission 'maksy.stocks.crypto.create' to create crypto.
 */
public class CryptoCommand implements CommandExecutor, TabCompleter {
    
    private static final String PERMISSION_CREATE = "maksy.stocks.crypto.create";
    
    private final CryptoService cryptoService;
    
    public CryptoCommand(CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        // Check if crypto command is enabled
        if (!net.cyberneticforge.quickstocks.QuickStocksPlugin.getMarketCfg().isCryptoCommandEnabled()) {
            if (sender instanceof Player player) {
                net.cyberneticforge.quickstocks.core.enums.Translation.FeatureDisabled.sendMessage(player);
            } else {
                sender.sendMessage("§cThis feature is currently disabled.");
            }
            return true;
        }
        
        if (args.length == 0) {
            // Show usage
            showUsage(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("create")) {
            handleCreateCommand(sender, Arrays.copyOfRange(args, 1, args.length));
        } else if (subCommand.equals("company")) {
            handleCompanyCommand(sender, Arrays.copyOfRange(args, 1, args.length));
        } else {
            showUsage(sender);
        }
        
        return true;
    }
    
    /**
     * Handles the /crypto create subcommand.
     */
    private void handleCreateCommand(CommandSender sender, String[] args) {
        // Check if sender is a player
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("❌ Only players can create custom crypto.", NamedTextColor.RED));
            return;
        }
        
        // Check permission
        if (!player.hasPermission(PERMISSION_CREATE)) {
            sender.sendMessage(Component.text("❌ You don't have permission to create custom crypto.", NamedTextColor.RED));
            sender.sendMessage(Component.text("💡 Required permission: " + PERMISSION_CREATE, NamedTextColor.GRAY));
            return;
        }
        
        // Validate arguments
        if (args.length < 2) {
            sender.sendMessage(Component.text("❌ Usage: /crypto create <symbol> <name>", NamedTextColor.RED));
            sender.sendMessage(Component.text("💡 Example: /crypto create MYCOIN \"My Custom Coin\"", NamedTextColor.GRAY));
            return;
        }
        
        String symbol = args[0];
        String displayName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        
        try {
            // Get crypto configuration
            var cryptoCfg = net.cyberneticforge.quickstocks.QuickStocksPlugin.getCryptoCfg();
            
            // Show cost information before creation
            double cost = cryptoCfg.getPersonalConfig().getCreationCost();
            double balance = net.cyberneticforge.quickstocks.QuickStocksPlugin.getWalletService()
                .getBalance(player.getUniqueId().toString());
            
            // Create the custom crypto (with balance check)
            String instrumentId = cryptoService.createCustomCrypto(symbol, displayName, player.getUniqueId().toString(), null, true);
            
            // Success message
            sender.sendMessage(Component.text(""));
            sender.sendMessage(Component.text("🎉 Custom Crypto Created Successfully!", NamedTextColor.GREEN, TextDecoration.BOLD));
            sender.sendMessage(Component.text("━".repeat(40), NamedTextColor.GRAY));
            
            sender.sendMessage(Component.text()
                    .append(Component.text("💰 Symbol: ", NamedTextColor.YELLOW))
                    .append(Component.text(symbol.toUpperCase(), NamedTextColor.DARK_AQUA, TextDecoration.BOLD))
                    .build());
            
            sender.sendMessage(Component.text()
                    .append(Component.text("📝 Name: ", NamedTextColor.YELLOW))
                    .append(Component.text(displayName, NamedTextColor.WHITE))
                    .build());
            
            sender.sendMessage(Component.text()
                    .append(Component.text("💲 Starting Price: ", NamedTextColor.YELLOW))
                    .append(Component.text("$" + String.format("%.2f", cryptoCfg.getDefaultsConfig().getStartingPrice()), NamedTextColor.GOLD))
                    .build());
            
            sender.sendMessage(Component.text()
                    .append(Component.text("💵 Cost: ", NamedTextColor.YELLOW))
                    .append(Component.text("$" + String.format("%.2f", cost), NamedTextColor.RED))
                    .build());
            
            sender.sendMessage(Component.text()
                    .append(Component.text("💳 Remaining Balance: ", NamedTextColor.YELLOW))
                    .append(Component.text("$" + String.format("%.2f", balance - cost), NamedTextColor.GREEN))
                    .build());
            
            sender.sendMessage(Component.text()
                    .append(Component.text("🆔 Instrument ID: ", NamedTextColor.YELLOW))
                    .append(Component.text(instrumentId, NamedTextColor.DARK_GRAY))
                    .build());
            
            sender.sendMessage(Component.text("━".repeat(40), NamedTextColor.GRAY));
            sender.sendMessage(Component.text("💡 Your crypto is now tradeable on the market!", NamedTextColor.GREEN));
            sender.sendMessage(Component.text("💡 Use /stocks " + symbol.toUpperCase() + " to view details", NamedTextColor.GRAY));
            sender.sendMessage(Component.text("💡 Use /market to start trading!", NamedTextColor.GRAY));
            
        } catch (IllegalArgumentException e) {
            sender.sendMessage(Component.text("❌ " + e.getMessage(), NamedTextColor.RED));
        } catch (SQLException e) {
            sender.sendMessage(Component.text("❌ Database error: " + e.getMessage(), NamedTextColor.RED));
        } catch (Exception e) {
            sender.sendMessage(Component.text("❌ Unexpected error: " + e.getMessage(), NamedTextColor.RED));
        }
    }
    
    /**
     * Handles the /crypto company subcommand for creating company-owned crypto.
     */
    private void handleCompanyCommand(CommandSender sender, String[] args) {
        // Check if sender is a player
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("❌ Only players can manage company crypto.", NamedTextColor.RED));
            return;
        }
        
        // Check permission
        if (!player.hasPermission(PERMISSION_CREATE)) {
            sender.sendMessage(Component.text("❌ You don't have permission to create company crypto.", NamedTextColor.RED));
            sender.sendMessage(Component.text("💡 Required permission: " + PERMISSION_CREATE, NamedTextColor.GRAY));
            return;
        }
        
        // Validate arguments
        if (args.length < 3) {
            sender.sendMessage(Component.text("❌ Usage: /crypto company <company-name> <symbol> <name>", NamedTextColor.RED));
            sender.sendMessage(Component.text("💡 Example: /crypto company \"MyCompany\" MYCOIN \"My Company Coin\"", NamedTextColor.GRAY));
            return;
        }
        
        String companyName = args[0];
        String symbol = args[1];
        String displayName = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        
        try {
            // Get the company
            var companyService = net.cyberneticforge.quickstocks.QuickStocksPlugin.getCompanyService();
            var companyOpt = companyService.getCompanyByName(companyName);
            
            if (companyOpt.isEmpty()) {
                sender.sendMessage(Component.text("❌ Company '" + companyName + "' not found", NamedTextColor.RED));
                return;
            }
            
            var company = companyOpt.get();
            
            // Check if player has permission to manage company
            var jobOpt = companyService.getPlayerJob(company.getId(), player.getUniqueId().toString());
            if (jobOpt.isEmpty() || !jobOpt.get().canManageCompany()) {
                sender.sendMessage(Component.text("❌ You don't have permission to create crypto for this company", NamedTextColor.RED));
                sender.sendMessage(Component.text("💡 Only company managers can create company crypto", NamedTextColor.GRAY));
                return;
            }
            
            // Get crypto configuration
            var cryptoCfg = net.cyberneticforge.quickstocks.QuickStocksPlugin.getCryptoCfg();
            
            // Get threshold for company type
            double threshold = cryptoCfg.getCompanyConfig().getBalanceThresholds()
                .getOrDefault(company.getType(), cryptoCfg.getCompanyConfig().getBalanceThreshold());
            
            // Create the company crypto (with balance check)
            String instrumentId = cryptoService.createCustomCrypto(
                symbol, displayName, player.getUniqueId().toString(), company.getId(), true);
            
            // Success message
            sender.sendMessage(Component.text(""));
            sender.sendMessage(Component.text("🎉 Company Crypto Created Successfully!", NamedTextColor.GREEN, TextDecoration.BOLD));
            sender.sendMessage(Component.text("━".repeat(40), NamedTextColor.GRAY));
            
            sender.sendMessage(Component.text()
                    .append(Component.text("🏢 Company: ", NamedTextColor.YELLOW))
                    .append(Component.text(company.getName(), NamedTextColor.AQUA, TextDecoration.BOLD))
                    .build());
            
            sender.sendMessage(Component.text()
                    .append(Component.text("💰 Symbol: ", NamedTextColor.YELLOW))
                    .append(Component.text(symbol.toUpperCase(), NamedTextColor.DARK_AQUA, TextDecoration.BOLD))
                    .build());
            
            sender.sendMessage(Component.text()
                    .append(Component.text("📝 Name: ", NamedTextColor.YELLOW))
                    .append(Component.text(displayName, NamedTextColor.WHITE))
                    .build());
            
            sender.sendMessage(Component.text()
                    .append(Component.text("💲 Starting Price: ", NamedTextColor.YELLOW))
                    .append(Component.text("$" + String.format("%.2f", cryptoCfg.getDefaultsConfig().getStartingPrice()), NamedTextColor.GOLD))
                    .build());
            
            sender.sendMessage(Component.text()
                    .append(Component.text("💼 Company Balance: ", NamedTextColor.YELLOW))
                    .append(Component.text("$" + String.format("%.2f", company.getBalance()), NamedTextColor.GREEN))
                    .build());
            
            sender.sendMessage(Component.text()
                    .append(Component.text("🆔 Instrument ID: ", NamedTextColor.YELLOW))
                    .append(Component.text(instrumentId, NamedTextColor.DARK_GRAY))
                    .build());
            
            sender.sendMessage(Component.text("━".repeat(40), NamedTextColor.GRAY));
            sender.sendMessage(Component.text("💡 Your company crypto is now tradeable on the market!", NamedTextColor.GREEN));
            sender.sendMessage(Component.text("💡 Use /stocks " + symbol.toUpperCase() + " to view details", NamedTextColor.GRAY));
            sender.sendMessage(Component.text("💡 Use /market to start trading!", NamedTextColor.GRAY));
            
        } catch (IllegalArgumentException e) {
            sender.sendMessage(Component.text("❌ " + e.getMessage(), NamedTextColor.RED));
        } catch (SQLException e) {
            sender.sendMessage(Component.text("❌ Database error: " + e.getMessage(), NamedTextColor.RED));
        } catch (Exception e) {
            sender.sendMessage(Component.text("❌ Unexpected error: " + e.getMessage(), NamedTextColor.RED));
        }
    }
    
    /**
     * Shows command usage information.
     */
    private void showUsage(CommandSender sender) {
        sender.sendMessage(Component.text(""));
        sender.sendMessage(Component.text("🪙 Crypto Commands", NamedTextColor.GOLD, TextDecoration.BOLD));
        sender.sendMessage(Component.text("━".repeat(30), NamedTextColor.GRAY));
        
        sender.sendMessage(Component.text()
                .append(Component.text("• /crypto create ", NamedTextColor.YELLOW))
                .append(Component.text("<symbol> <name>", NamedTextColor.AQUA))
                .build());
        
        sender.sendMessage(Component.text("  Creates a personal cryptocurrency", NamedTextColor.GRAY));
        
        var cryptoCfg = net.cyberneticforge.quickstocks.QuickStocksPlugin.getCryptoCfg();
        sender.sendMessage(Component.text("  Cost: $" + String.format("%.2f", cryptoCfg.getPersonalConfig().getCreationCost()), NamedTextColor.GRAY));
        sender.sendMessage(Component.text(""));
        
        sender.sendMessage(Component.text()
                .append(Component.text("• /crypto company ", NamedTextColor.YELLOW))
                .append(Component.text("<company> <symbol> <name>", NamedTextColor.AQUA))
                .build());
        
        sender.sendMessage(Component.text("  Creates a company-owned cryptocurrency", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  Requires company management permission", NamedTextColor.GRAY));
        sender.sendMessage(Component.text(""));
        
        sender.sendMessage(Component.text("Examples:", NamedTextColor.WHITE, TextDecoration.BOLD));
        sender.sendMessage(Component.text("  /crypto create MYCOIN \"My Custom Coin\"", NamedTextColor.DARK_AQUA));
        sender.sendMessage(Component.text("  /crypto company MyCompany CCOIN \"Company Coin\"", NamedTextColor.DARK_AQUA));
        sender.sendMessage(Component.text(""));
        
        if (sender instanceof Player player) {
            boolean hasPermission = player.hasPermission(PERMISSION_CREATE);
            Component permissionStatus = Component.text()
                    .append(Component.text("Permission: ", NamedTextColor.YELLOW))
                    .append(Component.text(PERMISSION_CREATE, NamedTextColor.GRAY))
                    .append(Component.text(" - ", NamedTextColor.GRAY))
                    .append(Component.text(hasPermission ? "✅ Granted" : "❌ Denied", 
                            hasPermission ? NamedTextColor.GREEN : NamedTextColor.RED))
                    .build();
            
            sender.sendMessage(permissionStatus);
        }
    }
    
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String @NotNull [] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // First argument - subcommands
            String partial = args[0].toLowerCase();
            if ("create".startsWith(partial)) {
                completions.add("create");
            }
            if ("company".startsWith(partial)) {
                completions.add("company");
            }
        } else if (args.length == 2 && "create".equalsIgnoreCase(args[0])) {
            // Second argument for create - suggest symbol format
            completions.add("<SYMBOL>");
        } else if (args.length == 3 && "create".equalsIgnoreCase(args[0])) {
            // Third argument for create - suggest name format
            completions.add("\"Display Name\"");
        } else if (args.length == 2 && "company".equalsIgnoreCase(args[0])) {
            // Second argument for company - suggest company names
            try {
                var companies = net.cyberneticforge.quickstocks.QuickStocksPlugin.getCompanyService().getAllCompanies();
                for (var company : companies) {
                    if (company.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(company.getName());
                    }
                }
            } catch (Exception e) {
                // Ignore exceptions in tab completion
            }
        } else if (args.length == 3 && "company".equalsIgnoreCase(args[0])) {
            // Third argument for company - suggest symbol format
            completions.add("<SYMBOL>");
        } else if (args.length == 4 && "company".equalsIgnoreCase(args[0])) {
            // Fourth argument for company - suggest name format
            completions.add("\"Display Name\"");
        }
        
        return completions;
    }
}