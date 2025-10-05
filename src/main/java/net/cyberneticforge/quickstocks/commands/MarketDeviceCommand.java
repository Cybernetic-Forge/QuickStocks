package net.cyberneticforge.quickstocks.commands;

import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.core.enums.Translation;
import net.cyberneticforge.quickstocks.core.model.Replaceable;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * /marketdevice command implementation
 * Grants special Market Link Device items to players
 */
public class MarketDeviceCommand implements CommandExecutor, TabCompleter {

    private static final String PERMISSION_GIVE = "maksy.stocks.marketdevice.give";
    private static final String DEVICE_KEY = "maksy:market_device";
    private static final String OWNER_KEY = "owner_uuid";
    private static final String VERSION_KEY = "version";

    public static final NamespacedKey deviceKey = new NamespacedKey(QuickStocksPlugin.getInstance(), "market_device");
    public static final NamespacedKey ownerKey = new NamespacedKey(QuickStocksPlugin.getInstance(), "owner_uuid");
    public static final NamespacedKey versionKey = new NamespacedKey(QuickStocksPlugin.getInstance(), "version");

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // Check permission
        if (!sender.hasPermission(PERMISSION_GIVE)) {
            Translation.NoPermission.sendMessage(sender);
            return true;
        }

        // Handle subcommands
        if (args.length == 0 || !args[0].equalsIgnoreCase("give")) {
            showUsage(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("give")) {
            handleGiveCommand(sender, args);
            return true;
        }

        showUsage(sender);
        return true;
    }

    /**
     * Handles the /marketdevice give [player] subcommand
     */
    private void handleGiveCommand(CommandSender sender, String[] args) {
        Player targetPlayer;

        if (args.length == 1) {
            // No target specified, give to sender if they're a player
            if (!(sender instanceof Player)) {
                Translation.CommandSyntax.sendMessage(sender, new Replaceable("%command%", "/marketdevice give [player]"));
                return;
            }
            targetPlayer = (Player) sender;
        } else {
            // Target player specified
            String targetName = args[1];
            targetPlayer = Bukkit.getPlayer(targetName);

            if (targetPlayer == null) {
                Translation.InvalidPlayer.sendMessage(sender);
                return;
            }
        }

        // Create and give the device
        ItemStack device = createMarketDevice(targetPlayer);
        targetPlayer.getInventory().addItem(device);

        // Send messages
        if (sender.equals(targetPlayer)) {
            Translation.Market_Device_SelfGiven.sendMessage(sender);
        } else {
            Translation.Market_Device_OtherGiven.sendMessage(sender);
            Translation.Market_Device_Given.sendMessage(targetPlayer);
        }
    }

    /**
     * Creates a Market Link Device item with proper NBT data
     */
    public ItemStack createMarketDevice(Player owner) {
        ItemStack device = new ItemStack(Material.RECOVERY_COMPASS);
        ItemMeta meta = device.getItemMeta();

        if (meta != null) {
            // Set display name
            meta.displayName(Translation.Market_Device_Name.asComponent());

            // Set lore
            List<Component> lore = Arrays.asList(Translation.Market_Device_Lore_Usage.asComponent(), Translation.Market_Device_Lore_Bound.asComponent(new Replaceable("%player%", owner.getName())));
            meta.lore(lore);

            // Set persistent data
            meta.getPersistentDataContainer().set(deviceKey, PersistentDataType.BOOLEAN, true);
            meta.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, owner.getUniqueId().toString());
            meta.getPersistentDataContainer().set(versionKey, PersistentDataType.INTEGER, 1);

            device.setItemMeta(meta);
        }

        return device;
    }

    /**
     * Checks if an item is a Market Device
     */
    public static boolean isMarketDevice(ItemStack item) {
        if (item == null || item.getItemMeta() == null) {
            return false;
        }

        return item.getItemMeta().getPersistentDataContainer().has(deviceKey, PersistentDataType.BOOLEAN);
    }

    /**
     * Gets the owner UUID of a Market Device
     */
    public static UUID getDeviceOwner(ItemStack item) {
        if (!isMarketDevice(item)) {
            return null;
        }

        String ownerString = item.getItemMeta().getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        if (ownerString == null) {
            return null;
        }

        try {
            return UUID.fromString(ownerString);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Shows command usage information
     */
    private void showUsage(CommandSender sender) {
        Translation.CommandSyntax.sendMessage(sender, new Replaceable("%command%", "/marketdevice give [player]"));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (!sender.hasPermission(PERMISSION_GIVE)) {
            return completions;
        }

        if (args.length == 1) {
            if ("give".startsWith(args[0].toLowerCase())) {
                completions.add("give");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            // Tab complete player names
            String prefix = args[1].toLowerCase();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(prefix)) {
                    completions.add(player.getName());
                }
            }
        }

        return completions;
    }
}