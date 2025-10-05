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

    // Market Messages
    Market_Overview_Header("Market.Overview.Header"),
    Market_Overview_CompaniesHeader("Market.Overview.CompaniesHeader"),
    Market_Overview_NoCompanies("Market.Overview.NoCompanies"),
    Market_Overview_CompanyItem("Market.Overview.CompanyItem"),
    Market_Overview_BuyHint("Market.Overview.BuyHint"),
    Market_Portfolio_Header("Market.Portfolio.Header"),
    Market_Portfolio_CashBalance("Market.Portfolio.CashBalance"),
    Market_Portfolio_PortfolioValue("Market.Portfolio.PortfolioValue"),
    Market_Portfolio_TotalAssets("Market.Portfolio.TotalAssets"),
    Market_Portfolio_NoHoldings("Market.Portfolio.NoHoldings"),
    Market_Portfolio_HoldingsHeader("Market.Portfolio.HoldingsHeader"),
    Market_Portfolio_HoldingItem("Market.Portfolio.HoldingItem"),
    Market_Portfolio_ProfitLoss("Market.Portfolio.ProfitLoss"),
    Market_History_Header("Market.History.Header"),
    Market_History_NoHistory("Market.History.NoHistory"),
    Market_History_TransactionItem("Market.History.TransactionItem"),
    Market_Shareholders_Header("Market.Shareholders.Header"),
    Market_Shareholders_NoShareholders("Market.Shareholders.NoShareholders"),
    Market_Shareholders_ShareholderItem("Market.Shareholders.ShareholderItem"),
    Market_Buy_Usage("Market.Buy.Usage"),
    Market_Buy_Success("Market.Buy.Success"),
    Market_Buy_Details("Market.Buy.Details"),
    Market_Buy_CompanyNotFound("Market.Buy.CompanyNotFound"),
    Market_Buy_NotOnMarket("Market.Buy.NotOnMarket"),
    Market_Buy_InvalidQuantity("Market.Buy.InvalidQuantity"),
    Market_Buy_QuantityPositive("Market.Buy.QuantityPositive"),
    Market_Buy_Error("Market.Buy.Error"),
    Market_Sell_Usage("Market.Sell.Usage"),
    Market_Sell_Success("Market.Sell.Success"),
    Market_Sell_Details("Market.Sell.Details"),
    Market_Sell_Error("Market.Sell.Error"),
    Market_Shareholders_Usage("Market.Shareholders_Usage"),
    Market_UnknownSubcommand("Market.UnknownSubcommand"),
    Market_ErrorProcessing("Market.ErrorProcessing"),

    // Wallet Messages
    Wallet_Usage("Wallet.Usage"),
    Wallet_Balance("Wallet.Balance"),
    Wallet_Deposit("Wallet.Deposit"),
    Wallet_Withdraw("Wallet.Withdraw"),
    Wallet_Pay("Wallet.Pay"),
    Wallet_Received("Wallet.Received"),
    Wallet_UnknownSubcommand("Wallet.UnknownSubcommand"),
    Wallet_ErrorProcessing("Wallet.ErrorProcessing"),
    Wallet_Error_InsufficientFunds("Wallet.Error.InsufficientFunds"),
    Wallet_Error_InvalidAmount("Wallet.Error.InvalidAmount"),
    Wallet_Error_PlayerNotFound("Wallet.Error.PlayerNotFound"),

    // Watch Messages
    Watch_Usage_Add("Watch.Usage_Add"),
    Watch_Usage_Remove("Watch.Usage_Remove"),
    Watch_Usage_Clear("Watch.Usage_Clear"),
    Watch_Added("Watch.Added"),
    Watch_Removed("Watch.Removed"),
    Watch_Cleared("Watch.Cleared"),
    Watch_ListHeader("Watch.ListHeader"),
    Watch_ListItem("Watch.ListItem"),
    Watch_DetailItem("Watch.DetailItem"),
    Watch_Empty("Watch.Empty"),
    Watch_EmptyHint("Watch.EmptyHint"),
    Watch_WatchingHeader("Watch.WatchingHeader"),
    Watch_CompanyItem("Watch.CompanyItem"),
    Watch_AlreadyExists("Watch.AlreadyExists"),
    Watch_NotFound("Watch.NotFound"),
    Watch_NotInWatchlist("Watch.NotInWatchlist"),
    Watch_UnknownSubcommand("Watch.UnknownSubcommand"),
    Watch_ErrorProcessing("Watch.ErrorProcessing"),
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
    Company_InvitationDetails("Company.InvitationDetails"),
    Company_JoinedWithJob("Company.JoinedWithJob"),
    Company_JoinedWithoutJob("Company.JoinedWithoutJob"),
    Company_Deposited("Company.Deposited"),
    Company_Withdrawn("Company.Withdrawn"),
    Company_EmployeesHeader("Company.EmployeesHeader"),
    Company_EmployeeItem("Company.EmployeeItem"),
    Company_JobsHeader("Company.JobsHeader"),
    Company_JobItem("Company.JobItem"),
    Company_JobPermissions("Company.JobPermissions"),
    Company_JobDetails("Company.JobDetails"),
    Company_NoJobs("Company.NoJobs"),
    Company_JobCreated("Company.JobCreated"),
    Company_JobAssigned("Company.JobAssigned"),
    Company_JobEdited("Company.JobEdited"),
    Company_JobEditedWithPerms("Company.JobEditedWithPerms"),
    Company_Fired("Company.Fired"),
    Company_FiredOther("Company.FiredOther"),
    Company_Left("Company.Left"),
    Company_TransferredOwnership("Company.TransferredOwnership"),
    Company_SymbolSet("Company.SymbolSet"),
    Company_MarketEnabled("Company.MarketEnabled"),
    Company_MarketEnabledDetails("Company.MarketEnabledDetails"),
    Company_MarketDisabled("Company.MarketDisabled"),
    Company_MarketDisabledDetails("Company.MarketDisabledDetails"),
    Company_MarketSettings("Company.MarketSettings"),
    Company_MarketPercentageUpdated("Company.MarketPercentageUpdated"),
    Company_MarketBuyoutUpdated("Company.MarketBuyoutUpdated"),
    Company_NotificationsHeader("Company.NotificationsHeader"),
    Company_NotificationsRead("Company.NotificationsRead"),
    Company_NoNotifications("Company.NoNotifications"),
    Company_NotificationItem("Company.NotificationItem"),
    Company_UnknownMarketAction("Company.UnknownMarketAction"),
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
    Company_Error_OwnerDemote("Company.Error.OwnerDemote"),

    // GUI Messages
    GUI_CompanySettings_Refresh_Success("GUI.CompanySettings.Refresh_Success"),
    GUI_CompanySettings_Refresh_Error("GUI.CompanySettings.Refresh_Error"),
    GUI_Portfolio_Refresh_Success("GUI.Portfolio.Refresh_Success"),
    GUI_Portfolio_Refresh_Error("GUI.Portfolio.Refresh_Error"),
    GUI_Market_Refresh_Success("GUI.Market.Refresh_Success"),
    GUI_Market_Refresh_Error("GUI.Market.Refresh_Error"),

    // ChestShop Messages
    ChestShop_Created("ChestShop.Created"),
    ChestShop_Removed("ChestShop.Removed"),
    ChestShop_NoPermission("ChestShop.NoPermission"),
    ChestShop_NotEmployee("ChestShop.NotEmployee"),
    ChestShop_Transaction_Buy("ChestShop.Transaction_Buy"),
    ChestShop_Transaction_Sell("ChestShop.Transaction_Sell"),
    ChestShop_Error("ChestShop.Error"),

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
