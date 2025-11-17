package net.cyberneticforge.quickstocks.commands;

import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.core.enums.Translation;
import net.cyberneticforge.quickstocks.core.model.Replaceable;
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
 * Uses Translation system for all messages.
 */
public class CryptoCommand implements CommandExecutor, TabCompleter {

    private static final String PERMISSION_CREATE = "quickstocks.command.crypto.create";

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        // Feature toggle
        if (!net.cyberneticforge.quickstocks.QuickStocksPlugin.getMarketCfg().isCryptoCommandEnabled()) {
            Translation.FeatureDisabled.sendMessage(sender);
            return true;
        }

        if (args.length == 0) {
            showUsage(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "create" -> handleCreateCommand(sender, Arrays.copyOfRange(args, 1, args.length));
            case "company" -> handleCompanyCommand(sender, Arrays.copyOfRange(args, 1, args.length));
            default -> showUsage(sender);
        }
        return true;
    }

    /**
     * Personal crypto creation.
     */
    private void handleCreateCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            Translation.Crypto_Error_PlayerOnlyPersonal.sendMessage(sender);
            return;
        }
        if (!player.hasPermission(PERMISSION_CREATE)) {
            Translation.Crypto_Error_NoPermissionPersonal.sendMessage(sender);
            Translation.Crypto_Error_RequiredPermission.sendMessage(sender);
            return;
        }
        if (args.length < 2) {
            Translation.Crypto_Error_UsageCreate.sendMessage(sender);
            Translation.Crypto_Error_ExampleCreate.sendMessage(sender);
            return;
        }

        String symbol = args[0];
        String displayName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        try {
            var cryptoCfg = net.cyberneticforge.quickstocks.QuickStocksPlugin.getCryptoCfg();
            double cost = cryptoCfg.getPersonalConfig().getCreationCost();
            double balance = net.cyberneticforge.quickstocks.QuickStocksPlugin.getWalletService().getBalance(player.getUniqueId().toString());
            String instrumentId = QuickStocksPlugin.getCryptoService().createCustomCrypto(symbol, displayName, player.getUniqueId().toString(), null, true);

            Translation.Crypto_Create_Success.sendMessage(sender,
                new Replaceable("%symbol%", symbol.toUpperCase()),
                new Replaceable("%name%", displayName),
                new Replaceable("%startprice%", String.format("%.2f", cryptoCfg.getDefaultsConfig().getStartingPrice())),
                new Replaceable("%cost%", String.format("%.2f", cost)),
                new Replaceable("%balance%", String.format("%.2f", balance - cost)),
                new Replaceable("%id%", instrumentId)
            );
        } catch (IllegalArgumentException e) {
            Translation.Errors_Internal.sendMessage(sender, new Replaceable("%error%", e.getMessage()));
        } catch (SQLException e) {
            Translation.Errors_Database.sendMessage(sender, new Replaceable("%error%", e.getMessage()));
        } catch (Exception e) {
            Translation.Errors_Internal.sendMessage(sender, new Replaceable("%error%", e.getMessage()));
        }
    }

    /**
     * Company-owned crypto creation.
     */
    private void handleCompanyCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            Translation.Crypto_Error_PlayerOnlyCompany.sendMessage(sender);
            return;
        }
        if (!player.hasPermission(PERMISSION_CREATE)) {
            Translation.Crypto_Error_NoPermissionCompany.sendMessage(sender);
            Translation.Crypto_Error_RequiredPermission.sendMessage(sender);
            return;
        }
        if (args.length < 3) {
            Translation.Crypto_Error_UsageCompany.sendMessage(sender);
            Translation.Crypto_Error_ExampleCompany.sendMessage(sender);
            return;
        }
        String companyName = args[0];
        String symbol = args[1];
        String displayName = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        try {
            var companyService = net.cyberneticforge.quickstocks.QuickStocksPlugin.getCompanyService();
            var companyOpt = companyService.getCompanyByName(companyName);
            if (companyOpt.isEmpty()) {
                Translation.Crypto_Error_CompanyNotFound.sendMessage(sender, new Replaceable("%company%", companyName));
                return;
            }
            var company = companyOpt.get();
            var jobOpt = companyService.getPlayerJob(company.getId(), player.getUniqueId().toString());
            if (jobOpt.isEmpty() || !jobOpt.get().canManageCompany()) {
                Translation.Crypto_Error_NotCompanyManager.sendMessage(sender);
                return;
            }
            var cryptoCfg = net.cyberneticforge.quickstocks.QuickStocksPlugin.getCryptoCfg();
            String instrumentId = QuickStocksPlugin.getCryptoService().createCustomCrypto(symbol, displayName, player.getUniqueId().toString(), company.getId(), true);
            Translation.Crypto_Company_Success.sendMessage(sender,
                new Replaceable("%company%", company.getName()),
                new Replaceable("%symbol%", symbol.toUpperCase()),
                new Replaceable("%name%", displayName),
                new Replaceable("%startprice%", String.format("%.2f", cryptoCfg.getDefaultsConfig().getStartingPrice())),
                new Replaceable("%companybalance%", String.format("%.2f", company.getBalance())),
                new Replaceable("%id%", instrumentId)
            );
        } catch (IllegalArgumentException e) {
            Translation.Errors_Internal.sendMessage(sender, new Replaceable("%error%", e.getMessage()));
        } catch (SQLException e) {
            Translation.Errors_Database.sendMessage(sender, new Replaceable("%error%", e.getMessage()));
        } catch (Exception e) {
            Translation.Errors_Internal.sendMessage(sender, new Replaceable("%error%", e.getMessage()));
        }
    }

    /**
     * Shows command usage & examples via translations.
     */
    private void showUsage(CommandSender sender) {
        Translation.Crypto_Help_Usage.sendMessage(sender);
        Translation.Crypto_Help_Examples.sendMessage(sender);
        if (sender instanceof Player player) {
            boolean hasPermission = player.hasPermission(PERMISSION_CREATE);
            Translation.Crypto_Help_PermissionStatus.sendMessage(sender,
                new Replaceable("%status%", hasPermission ? "&a✅ Granted" : "&c❌ Denied")
            );
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String @NotNull [] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            if ("create".startsWith(partial)) completions.add("create");
            if ("company".startsWith(partial)) completions.add("company");
        } else if (args.length == 2 && "create".equalsIgnoreCase(args[0])) {
            completions.add("<SYMBOL>");
        } else if (args.length == 3 && "create".equalsIgnoreCase(args[0])) {
            completions.add("\"Display Name\"");
        } else if (args.length == 2 && "company".equalsIgnoreCase(args[0])) {
            try {
                var companies = net.cyberneticforge.quickstocks.QuickStocksPlugin.getCompanyService().getAllCompanies();
                for (var company : companies) {
                    if (company.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(company.getName());
                    }
                }
            } catch (Exception ignored) {}
        } else if (args.length == 3 && "company".equalsIgnoreCase(args[0])) {
            completions.add("<SYMBOL>");
        } else if (args.length == 4 && "company".equalsIgnoreCase(args[0])) {
            completions.add("\"Display Name\"");
        }
        return completions;
    }
}