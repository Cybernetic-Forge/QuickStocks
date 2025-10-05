package net.cyberneticforge.quickstocks.core.enums;

import lombok.Getter;
import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.core.model.Replaceable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.command.CommandSender;

@Getter
@SuppressWarnings("unused")
public enum Translation {

    HoverPlaceholder("HoverPlaceholder"),

    NoConsoleSender("General.NoConsoleSender"),
    NoPermission("General.NoPermission"),
    CommandSyntax("General.CommandSyntax"),
    InvalidPlayer("General.InvalidPlayer"),
    InvalidItem("General.InvalidItem"),
    InvalidNumber("General.InvalidNumber"),
    NegativeValue("General.NegativeValue"),
    HigherZero("General.HigherZero"),
    UnknownException("General.UnknownException"),
    Reload("General.Reload"),
    ReloadFinished("General.ReloadFinished"),

    Market_Device_Name("Market.Device.Name"),
    Market_Device_Given("Market.Device.Given"),
    Market_Device_Opened("Market.Device.Opened"),
    Market_Device_Cooldown("Market.Device.Cooldown"),
    Market_Device_WrongOwner("Market.Device.WrongOwner"),
    Market_Device_DropPrevented("Market.Device.DropPrevented"),
    Market_Device_SelfGiven("Market.Device.SelfGiven"),
    Market_Device_OtherGiven("Market.Device.OtherGiven"),
    Market_Device_Lore_Usage("Market.Device.Lore.Usage"),
    Market_Device_Lore_Bound("Market.Device.Lore.Bound"),

    Errors_Database("Errors.Database"),
    Errors_Internal("Errors.Internal");




    private final String path;

    Translation(String path) {
        this.path = path;
    }

    public Component asComponent(Replaceable... replaceables) {
        return QuickStocksPlugin.getTranslationService().message(this, replaceables);
    }

    public void sendMessage(CommandSender sender, Replaceable... replaceables) {
        sender.sendMessage(QuickStocksPlugin.getTranslationService().message(this, replaceables));
    }

    public void sendActionBar(CommandSender sender, Replaceable... replaceables) {
        sender.sendActionBar(QuickStocksPlugin.getTranslationService().message(this, replaceables));
    }

    public void sendTitle(CommandSender sender, Component subtitle, Title.Times times) {
        Title tile = Title.title(QuickStocksPlugin.getTranslationService().message(this), subtitle, times);
        sender.showTitle(tile);
    }
}
