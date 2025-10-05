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

    // Trading Messages
    Trading_Balance("Trading.Balance"),
    Trading_Refreshed("Trading.Refreshed"),
    Trading_CompanyNotFound("Trading.CompanyNotFound"),
    Trading_NotOnMarket("Trading.NotOnMarket"),
    Trading_EnterAmount("Trading.EnterAmount"),
    Trading_UseCommand("Trading.UseCommand"),
    Trading_InsufficientFunds("Trading.InsufficientFunds"),
    Trading_BuySuccess("Trading.BuySuccess"),
    Trading_SellSuccess("Trading.SellSuccess"),
    Trading_NewBalance("Trading.NewBalance"),
    Trading_BuyFailed("Trading.BuyFailed"),
    Trading_SellFailed("Trading.SellFailed"),
    Trading_NoShares("Trading.NoShares"),
    Trading_CompanyInfoHeader("Trading.CompanyInfoHeader"),
    Trading_SharePrice("Trading.SharePrice"),
    Trading_CompanyBalance("Trading.CompanyBalance"),
    Trading_MarketPercentage("Trading.MarketPercentage"),
    Trading_ClickToTrade("Trading.ClickToTrade"),
    Trading_ErrorProcessing("Trading.ErrorProcessing"),
    Trading_FailedToOpen("Trading.FailedToOpen"),

    // Wallet Messages
    Wallet_Balance("Wallet.Balance"),
    Wallet_Deposit("Wallet.Deposit"),
    Wallet_Withdraw("Wallet.Withdraw"),
    Wallet_Pay("Wallet.Pay"),
    Wallet_Received("Wallet.Received"),
    Wallet_Error_InsufficientFunds("Wallet.Error.InsufficientFunds"),
    Wallet_Error_InvalidAmount("Wallet.Error.InvalidAmount"),
    Wallet_Error_PlayerNotFound("Wallet.Error.PlayerNotFound"),

    // Watch Messages
    Watch_Added("Watch.Added"),
    Watch_Removed("Watch.Removed"),
    Watch_ListHeader("Watch.ListHeader"),
    Watch_ListItem("Watch.ListItem"),
    Watch_Empty("Watch.Empty"),
    Watch_AlreadyExists("Watch.AlreadyExists"),
    Watch_NotFound("Watch.NotFound"),
    Watch_Alert("Watch.Alert"),

    // Company Messages
    Company_Help_Header("Company.Help.Header"),
    Company_Help_Create("Company.Help.Create"),
    Company_Help_Info("Company.Help.Info"),
    Company_Help_List("Company.Help.List"),
    Company_Help_Invite("Company.Help.Invite"),
    Company_Help_Accept("Company.Help.Accept"),
    Company_Help_Decline("Company.Help.Decline"),
    Company_Help_Invitations("Company.Help.Invitations"),
    Company_Help_Deposit("Company.Help.Deposit"),
    Company_Help_Withdraw("Company.Help.Withdraw"),
    Company_Help_Employees("Company.Help.Employees"),
    Company_Help_Jobs("Company.Help.Jobs"),
    Company_Help_CreateJob("Company.Help.CreateJob"),
    Company_Help_AssignJob("Company.Help.AssignJob"),
    Company_Help_EditJob("Company.Help.EditJob"),
    Company_Help_Fire("Company.Help.Fire"),
    Company_Help_Leave("Company.Help.Leave"),
    Company_Help_Transfer("Company.Help.Transfer"),
    Company_Help_Settings("Company.Help.Settings"),
    Company_Help_SettingsGUI("Company.Help.SettingsGUI"),
    Company_Help_Notifications("Company.Help.Notifications"),
    Company_Help_Symbol("Company.Help.Symbol"),
    Company_Help_EnableMarket("Company.Help.EnableMarket"),
    Company_Help_DisableMarket("Company.Help.DisableMarket"),
    Company_Created("Company.Created"),
    Company_CreationCost("Company.CreationCost"),
    Company_InfoHeader("Company.InfoHeader"),
    Company_InfoType("Company.InfoType"),
    Company_InfoBalance("Company.InfoBalance"),
    Company_InfoOwner("Company.InfoOwner"),
    Company_InfoCreated("Company.InfoCreated"),
    Company_InfoEmployees("Company.InfoEmployees"),
    Company_ListHeader("Company.ListHeader"),
    Company_ListItem("Company.ListItem"),
    Company_InviteSent("Company.InviteSent"),
    Company_InviteReceived("Company.InviteReceived"),
    Company_InviteAcceptPrompt("Company.InviteAcceptPrompt"),
    Company_InviteAccepted("Company.InviteAccepted"),
    Company_InviteDeclined("Company.InviteDeclined"),
    Company_NoInvitations("Company.NoInvitations"),
    Company_InvitationsHeader("Company.InvitationsHeader"),
    Company_InvitationItem("Company.InvitationItem"),
    Company_InvitationExpires("Company.InvitationExpires"),
    Company_Deposited("Company.Deposited"),
    Company_Withdrawn("Company.Withdrawn"),
    Company_EmployeesHeader("Company.EmployeesHeader"),
    Company_EmployeeItem("Company.EmployeeItem"),
    Company_JobsHeader("Company.JobsHeader"),
    Company_JobItem("Company.JobItem"),
    Company_JobPermissions("Company.JobPermissions"),
    Company_JobCreated("Company.JobCreated"),
    Company_JobAssigned("Company.JobAssigned"),
    Company_JobEdited("Company.JobEdited"),
    Company_Fired("Company.Fired"),
    Company_FiredOther("Company.FiredOther"),
    Company_Left("Company.Left"),
    Company_TransferredOwnership("Company.TransferredOwnership"),
    Company_SymbolSet("Company.SymbolSet"),
    Company_MarketEnabled("Company.MarketEnabled"),
    Company_MarketDisabled("Company.MarketDisabled"),
    Company_NotificationsHeader("Company.NotificationsHeader"),
    Company_NotificationsRead("Company.NotificationsRead"),
    Company_NoNotifications("Company.NoNotifications"),
    Company_NoEmployees("Company.NoEmployees"),
    Company_Error_CompanyNotFound("Company.Error.CompanyNotFound"),
    Company_Error_AlreadyExists("Company.Error.AlreadyExists"),
    Company_Error_InsufficientFunds("Company.Error.InsufficientFunds"),
    Company_Error_NotEmployee("Company.Error.NotEmployee"),
    Company_Error_NoPermission("Company.Error.NoPermission"),
    Company_Error_InvalidInvitation("Company.Error.InvalidInvitation"),
    Company_Error_InvitationExpired("Company.Error.InvitationExpired"),
    Company_Error_PlayerNotFound("Company.Error.PlayerNotFound"),
    Company_Error_InvalidAmount("Company.Error.InvalidAmount"),
    Company_Error_InvalidJob("Company.Error.InvalidJob"),
    Company_Error_SymbolTaken("Company.Error.SymbolTaken"),
    Company_Error_NotOnMarket("Company.Error.NotOnMarket"),
    Company_Error_OwnerOnly("Company.Error.OwnerOnly"),

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
