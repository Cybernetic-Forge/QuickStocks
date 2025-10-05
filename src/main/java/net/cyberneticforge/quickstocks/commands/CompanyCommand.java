package net.cyberneticforge.quickstocks.commands;

import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.core.enums.Translation;
import net.cyberneticforge.quickstocks.core.model.Company;
import net.cyberneticforge.quickstocks.core.model.CompanyInvitation;
import net.cyberneticforge.quickstocks.core.model.CompanyJob;
import net.cyberneticforge.quickstocks.core.model.Replaceable;
import net.cyberneticforge.quickstocks.gui.CompanySettingsGUI;
import net.cyberneticforge.quickstocks.infrastructure.hooks.HookType;
import net.cyberneticforge.quickstocks.utils.ChatUT;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Command handler for company operations (/company).
 */
public class CompanyCommand implements CommandExecutor, TabCompleter {
    
    private static final Logger logger = Logger.getLogger(CompanyCommand.class.getName());

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            Translation.NoConsoleSender.sendMessage(sender);
            return true;
        }
        
        Player player = (Player) sender;
        String playerUuid = player.getUniqueId().toString();
        
        try {
            if (args.length == 0) {
                showHelp(player);
                return true;
            }
            
            String subcommand = args[0].toLowerCase();
            
            switch (subcommand) {
                case "create":
                    handleCreate(player, playerUuid, args);
                    break;
                    
                case "info":
                    handleInfo(player, playerUuid, args);
                    break;
                    
                case "list":
                    handleList(player, args);
                    break;
                    
                case "invite":
                    handleInvite(player, playerUuid, args);
                    break;
                    
                case "accept":
                    handleAccept(player, playerUuid, args);
                    break;
                    
                case "decline":
                    handleDecline(player, playerUuid, args);
                    break;
                    
                case "invitations":
                    handleInvitations(player, playerUuid);
                    break;
                    
                case "deposit":
                    handleDeposit(player, playerUuid, args);
                    break;
                    
                case "withdraw":
                    handleWithdraw(player, playerUuid, args);
                    break;
                    
                case "employees":
                    handleEmployees(player, args);
                    break;
                    
                case "jobs":
                    handleJobs(player, args);
                    break;
                    
                case "createjob":
                    handleCreateJob(player, playerUuid, args);
                    break;
                    
                case "assignjob":
                    handleAssignJob(player, playerUuid, args);
                    break;
                    
                case "editjob":
                    handleEditJob(player, playerUuid, args);
                    break;
                    
                case "settings":
                    handleSettings(player, playerUuid, args);
                    break;
                    
                case "setsymbol":
                    handleSetSymbol(player, playerUuid, args);
                    break;
                    
                case "market":
                    handleMarket(player, playerUuid, args);
                    break;
                    
                case "notifications":
                    handleNotifications(player, playerUuid);
                    break;
                    
                case "leave":
                    handleLeave(player, playerUuid, args);
                    break;
                    
                case "transferownership":
                    handleTransferOwnership(player, playerUuid, args);
                    break;
                    
                case "fire":
                    handleFire(player, playerUuid, args);
                    break;
                    
                case "salary":
                    handleSalary(player, playerUuid, args);
                    break;
                    
                default:
                    showHelp(player);
                    break;
            }
            
        } catch (Exception e) {
            logger.warning("Error in company command for " + player.getName() + ": " + e.getMessage());
            Translation.Errors_Internal.sendMessage(player, new Replaceable("%error%", e.getMessage()));
        }
        
        return true;
    }
    
    private void showHelp(Player player) {
        Translation.Company_Help_Header.sendMessage(player);
        Translation.Company_Help_Create.sendMessage(player);
        Translation.Company_Help_Info.sendMessage(player);
        Translation.Company_Help_List.sendMessage(player);
        Translation.Company_Help_Invite.sendMessage(player);
        Translation.Company_Help_Invitations.sendMessage(player);
        Translation.Company_Help_Accept.sendMessage(player);
        Translation.Company_Help_Decline.sendMessage(player);
        Translation.Company_Help_Deposit.sendMessage(player);
        Translation.Company_Help_Withdraw.sendMessage(player);
        Translation.Company_Help_Employees.sendMessage(player);
        Translation.Company_Help_Jobs.sendMessage(player);
        Translation.Company_Help_CreateJob.sendMessage(player);
        Translation.Company_Help_EditJob.sendMessage(player);
        Translation.Company_Help_AssignJob.sendMessage(player);
        Translation.Company_Help_Leave.sendMessage(player);
        Translation.Company_Help_Fire.sendMessage(player);
        Translation.Company_Help_Transfer.sendMessage(player);
        Translation.Company_Help_SettingsGUI.sendMessage(player);
        Translation.Company_Help_Symbol.sendMessage(player);
        Translation.Company_Help_EnableMarket.sendMessage(player);
        Translation.Company_Help_DisableMarket.sendMessage(player);
        Translation.Company_Help_Settings.sendMessage(player);
        Translation.Company_Help_Notifications.sendMessage(player);
        player.sendMessage("§e/company salary §7- Manage employee salaries");
    }
    
    private void handleCreate(Player player, String playerUuid, String[] args) throws Exception {
        if (!player.hasPermission("quickstocks.company.create")) {
            Translation.NoPermission.sendMessage(player);
            return;
        }
        
        if (args.length < 3) {
            Translation.CommandSyntax.sendMessage(player, new Replaceable("%command%", "/company create <name> <type>"));
            return;
        }
        
        String name = args[1];
        String type = args[2].toUpperCase();
        
        Company company = QuickStocksPlugin.getCompanyService().createCompany(playerUuid, name, type);
        
        Translation.Company_Created.sendMessage(player, new Replaceable("%company%", name));
        Translation.Company_InfoType.sendMessage(player, new Replaceable("%type%", type));
    }
    
    private void handleInfo(Player player, String playerUuid, String[] args) throws Exception {
        String companyName;
        
        if (args.length < 2) {
            // Show player's companies
            List<Company> companies = QuickStocksPlugin.getCompanyService().getCompaniesByPlayer(playerUuid);
            
            if (companies.isEmpty()) {
                Translation.Company_Error_NotEmployee.sendMessage(player, new Replaceable("%company%", "any"));
                return;
            }
            
            Translation.Company_ListHeader.sendMessage(player);
            for (Company company : companies) {
                Optional<CompanyJob> job = QuickStocksPlugin.getCompanyService().getPlayerJob(company.getId(), playerUuid);
                String jobTitle = job.isPresent() ? job.get().getTitle() : "Unknown";
                
                Translation.Company_ListItem.sendMessage(player, 
                    new Replaceable("%company%", company.getName()),
                    new Replaceable("%type%", company.getType() + " - " + jobTitle));
                Translation.Company_InfoBalance.sendMessage(player,
                    new Replaceable("%balance%", String.format("%.2f", company.getBalance())));
            }
            return;
        }
        
        companyName = args[1];
        Optional<Company> companyOpt = QuickStocksPlugin.getCompanyService().getCompanyByName(companyName);
        
        if (companyOpt.isEmpty()) {
            Translation.Company_Error_CompanyNotFound.sendMessage(player, new Replaceable("%company%", companyName));
            return;
        }
        
        Company company = companyOpt.get();
        OfflinePlayer owner = Bukkit.getOfflinePlayer(UUID.fromString(company.getOwnerUuid()));
        
        Translation.Company_InfoHeader.sendMessage(player, new Replaceable("%company%", company.getName()));
        Translation.Company_InfoType.sendMessage(player, new Replaceable("%type%", company.getType()));
        Translation.Company_InfoOwner.sendMessage(player, new Replaceable("%owner%", owner.getName() != null ? owner.getName() : "Unknown"));
        Translation.Company_InfoBalance.sendMessage(player, new Replaceable("%balance%", String.format("%.2f", company.getBalance())));
        Translation.Company_InfoCreated.sendMessage(player, new Replaceable("%date%", dateFormat.format(new Date(company.getCreatedAt()))));
        
        // Show player's job if they're an employee
        Optional<CompanyJob> playerJob = QuickStocksPlugin.getCompanyService().getPlayerJob(company.getId(), playerUuid);
        if (playerJob.isPresent()) {
            CompanyJob job = playerJob.get();
            String permissions = (job.canManageCompany() ? "Manage " : "") +
                             (job.canInvite() ? "Invite " : "") +
                             (job.canCreateTitles() ? "CreateJobs " : "") +
                             (job.canWithdraw() ? "Withdraw " : "") +
                             (job.canManageChestShop() ? "ChestShop" : "");
            Translation.Company_JobItem.sendMessage(player, new Replaceable("%job%", job.getTitle()));
            Translation.Company_JobPermissions.sendMessage(player, new Replaceable("%permissions%", permissions));
        }
    }
    
    private void handleList(Player player, String[] args) throws Exception {
        int page = 0;
        if (args.length > 1) {
            try {
                page = Integer.parseInt(args[1]) - 1;
                if (page < 0) page = 0;
            } catch (NumberFormatException e) {
                Translation.InvalidNumber.sendMessage(player);
                return;
            }
        }
        
        List<Company> companies = QuickStocksPlugin.getCompanyService().listCompanies(page, 10);
        
        if (companies.isEmpty()) {
            Translation.Company_NoNotifications.sendMessage(player);
            return;
        }
        
        Translation.Company_ListHeader.sendMessage(player);
        for (Company company : companies) {
            Translation.Company_ListItem.sendMessage(player,
                new Replaceable("%company%", company.getName()),
                new Replaceable("%type%", company.getType()));
            Translation.Company_InfoBalance.sendMessage(player,
                new Replaceable("%balance%", String.format("%.2f", company.getBalance())));
        }
    }
    
    private void handleInvite(Player player, String playerUuid, String[] args) throws Exception {
        if (args.length < 4) {
            Translation.CommandSyntax.sendMessage(player, new Replaceable("%command%", "/company invite <company> <player> <job>"));
            return;
        }
        
        String companyName = args[1];
        String targetPlayerName = args[2];
        String jobTitle = args[3];
        
        Optional<Company> companyOpt = QuickStocksPlugin.getCompanyService().getCompanyByName(companyName);
        if (companyOpt.isEmpty()) {
            Translation.Company_Error_CompanyNotFound.sendMessage(player, new Replaceable("%company%", companyName));
            return;
        }
        
        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
        if (targetPlayer == null) {
            Translation.Company_Error_PlayerNotFound.sendMessage(player, new Replaceable("%player%", targetPlayerName));
            return;
        }
        
        String targetUuid = targetPlayer.getUniqueId().toString();
        String companyId = companyOpt.get().getId();
        
        CompanyInvitation invitation = QuickStocksPlugin.getInvitationService().createInvitation(companyId, playerUuid, targetUuid, jobTitle);
        
        Translation.Company_InviteSent.sendMessage(player,
            new Replaceable("%player%", targetPlayerName),
            new Replaceable("%job%", jobTitle));
        Translation.Company_InviteReceived.sendMessage(targetPlayer,
            new Replaceable("%company%", companyOpt.get().getName()),
            new Replaceable("%job%", jobTitle));
        Translation.Company_InviteAcceptPrompt.sendMessage(targetPlayer,
            new Replaceable("%id%", companyOpt.get().getName()));
    }
    
    private void handleAccept(Player player, String playerUuid, String[] args) throws Exception {
        if (args.length < 2) {
            Translation.CommandSyntax.sendMessage(player, new Replaceable("%command%", "/company accept <company>"));
            return;
        }
        
        String companyName = args[1];
        
        // Get company by name
        Optional<Company> companyOpt = QuickStocksPlugin.getCompanyService().getCompanyByName(companyName);
        if (companyOpt.isEmpty()) {
            Translation.Company_Error_CompanyNotFound.sendMessage(player);
            return;
        }
        
        // Find pending invitation for this company
        List<CompanyInvitation> invitations = QuickStocksPlugin.getInvitationService().getPendingInvitations(playerUuid);
        CompanyInvitation targetInvitation = null;
        
        for (CompanyInvitation inv : invitations) {
            if (inv.getCompanyId().equals(companyOpt.get().getId())) {
                targetInvitation = inv;
                break;
            }
        }
        
        if (targetInvitation == null) {
            Translation.Company_NoInvitations.sendMessage(player);
            return;
        }
        
        QuickStocksPlugin.getInvitationService().acceptInvitation(targetInvitation.getId(), playerUuid);
        
        // Show job details
        Optional<CompanyJob> jobOpt = QuickStocksPlugin.getCompanyService().getPlayerJob(companyOpt.get().getId(), playerUuid);
        if (jobOpt.isPresent()) {
            CompanyJob job = jobOpt.get();
            String permissions = (job.canManageCompany() ? "Manage " : "") +
                             (job.canInvite() ? "Invite " : "") +
                             (job.canCreateTitles() ? "CreateJobs " : "") +
                             (job.canWithdraw() ? "Withdraw " : "") +
                             (job.canManageChestShop() ? "ChestShop" : "");
            Translation.Company_JoinedWithJob.sendMessage(player,
                new Replaceable("%company%", companyOpt.get().getName()),
                new Replaceable("%job%", job.getTitle()),
                new Replaceable("%permissions%", permissions));
        } else {
            Translation.Company_JoinedWithoutJob.sendMessage(player,
                new Replaceable("%company%", companyOpt.get().getName()));
        }
    }
    
    private void handleDecline(Player player, String playerUuid, String[] args) throws Exception {
        if (args.length < 2) {
            Translation.CommandSyntax.sendMessage(player, new Replaceable("%command%", "/company decline <company>"));
            return;
        }
        
        String companyName = args[1];
        
        // Get company by name
        Optional<Company> companyOpt = QuickStocksPlugin.getCompanyService().getCompanyByName(companyName);
        if (companyOpt.isEmpty()) {
            Translation.Company_Error_CompanyNotFound.sendMessage(player);
            return;
        }
        
        // Find pending invitation for this company
        List<CompanyInvitation> invitations = QuickStocksPlugin.getInvitationService().getPendingInvitations(playerUuid);
        CompanyInvitation targetInvitation = null;
        
        for (CompanyInvitation inv : invitations) {
            if (inv.getCompanyId().equals(companyOpt.get().getId())) {
                targetInvitation = inv;
                break;
            }
        }
        
        if (targetInvitation == null) {
            Translation.Company_NoInvitations.sendMessage(player);
            return;
        }
        
        QuickStocksPlugin.getInvitationService().declineInvitation(targetInvitation.getId(), playerUuid);
        Translation.Company_InviteDeclined.sendMessage(player, new Replaceable("%company%", companyName));
    }
    
    private void handleInvitations(Player player, String playerUuid) throws Exception {
        List<CompanyInvitation> invitations = QuickStocksPlugin.getInvitationService().getPendingInvitations(playerUuid);
        
        if (invitations.isEmpty()) {
            Translation.Company_NoInvitations.sendMessage(player);
            return;
        }
        
        Translation.Company_InvitationsHeader.sendMessage(player);
        for (CompanyInvitation invitation : invitations) {
            Optional<Company> companyOpt = QuickStocksPlugin.getCompanyService().getCompanyById(invitation.getCompanyId());
            Optional<CompanyJob> jobOpt = QuickStocksPlugin.getCompanyService().getJobById(invitation.getJobId());
            if (companyOpt.isPresent() && jobOpt.isPresent()) {
                Company company = companyOpt.get();
                CompanyJob job = jobOpt.get();
                Translation.Company_InvitationDetails.sendMessage(player,
                    new Replaceable("%company%", company.getName()),
                    new Replaceable("%job%", job.getTitle()),
                    new Replaceable("%date%", dateFormat.format(new Date(invitation.getExpiresAt()))));
            }
        }
    }
    
    private void handleDeposit(Player player, String playerUuid, String[] args) throws Exception {
        if (args.length < 3) {
            Translation.CommandSyntax.sendMessage(player, new Replaceable("%command%", "/company deposit <company> <amount>"));
            return;
        }
        
        String companyName = args[1];
        double amount;
        
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            Translation.InvalidNumber.sendMessage(player);
            return;
        }
        
        Optional<Company> companyOpt = QuickStocksPlugin.getCompanyService().getCompanyByName(companyName);
        if (companyOpt.isEmpty()) {
            Translation.Company_Error_CompanyNotFound.sendMessage(player);
            return;
        }
        
        QuickStocksPlugin.getCompanyService().deposit(companyOpt.get().getId(), playerUuid, amount);
        Translation.Company_Deposited.sendMessage(player, new Replaceable("%company%", companyName), new Replaceable("%amount%", String.format("%.2f", amount)));
    }
    
    private void handleWithdraw(Player player, String playerUuid, String[] args) throws Exception {
        if (args.length < 3) {
            Translation.CommandSyntax.sendMessage(player, new Replaceable("%command%", "/company withdraw <company> <amount>"));
            return;
        }
        
        String companyName = args[1];
        double amount;
        
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            Translation.InvalidNumber.sendMessage(player);
            return;
        }
        
        Optional<Company> companyOpt = QuickStocksPlugin.getCompanyService().getCompanyByName(companyName);
        if (companyOpt.isEmpty()) {
            Translation.Company_Error_CompanyNotFound.sendMessage(player);
            return;
        }
        
        QuickStocksPlugin.getCompanyService().withdraw(companyOpt.get().getId(), playerUuid, amount);
        Translation.Company_Withdrawn.sendMessage(player, new Replaceable("%company%", companyName), new Replaceable("%amount%", String.format("%.2f", amount)));
    }
    
    private void handleEmployees(Player player, String[] args) throws Exception {
        if (args.length < 2) {
            Translation.CommandSyntax.sendMessage(player, new Replaceable("%command%", "/company employees <company>"));
            return;
        }
        
        String companyName = args[1];
        Optional<Company> companyOpt = QuickStocksPlugin.getCompanyService().getCompanyByName(companyName);
        
        if (companyOpt.isEmpty()) {
            Translation.Company_Error_CompanyNotFound.sendMessage(player);
            return;
        }
        
        List<Map<String, Object>> employees = QuickStocksPlugin.getCompanyService().getCompanyEmployees(companyOpt.get().getId());
        
        if (employees.isEmpty()) {
            Translation.Company_NoEmployees.sendMessage(player);
            return;
        }
        
        Translation.Company_EmployeesHeader.sendMessage(player,
            new Replaceable("%company%", companyOpt.get().getName()));
        for (Map<String, Object> emp : employees) {
            String playerUuid = (String) emp.get("player_uuid");
            String title = (String) emp.get("title");
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(playerUuid));
            String playerName = offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unknown";
            
            Translation.Company_EmployeeItem.sendMessage(player,
                new Replaceable("%player%", playerName),
                new Replaceable("%job%", title));
        }
    }
    
    private void handleJobs(Player player, String[] args) throws Exception {
        if (args.length < 2) {
            Translation.CommandSyntax.sendMessage(player, new Replaceable("%command%", "/company jobs <company>"));
            return;
        }
        
        String companyName = args[1];
        Optional<Company> companyOpt = QuickStocksPlugin.getCompanyService().getCompanyByName(companyName);
        
        if (companyOpt.isEmpty()) {
            Translation.Company_Error_CompanyNotFound.sendMessage(player);
            return;
        }
        
        List<CompanyJob> jobs = QuickStocksPlugin.getCompanyService().getCompanyJobs(companyOpt.get().getId());
        
        if (jobs.isEmpty()) {
            Translation.Company_NoJobs.sendMessage(player);
            return;
        }
        
        Translation.Company_JobsHeader.sendMessage(player,
            new Replaceable("%company%", companyOpt.get().getName()));
        for (CompanyJob job : jobs) {
            String permissions = (job.canManageCompany() ? "Manage " : "") +
                             (job.canInvite() ? "Invite " : "") +
                             (job.canCreateTitles() ? "CreateJobs " : "") +
                             (job.canWithdraw() ? "Withdraw " : "") +
                             (job.canManageChestShop() ? "ChestShop" : "");
            Translation.Company_JobDetails.sendMessage(player,
                new Replaceable("%job%", job.getTitle()),
                new Replaceable("%permissions%", permissions));
        }
    }
    
    private void handleCreateJob(Player player, String playerUuid, String[] args) throws Exception {
        if (args.length < 4) {
            Translation.CommandSyntax.sendMessage(player, new Replaceable("%command%", "/company createjob <company> <title> <permissions>"));
            player.sendMessage(ChatUT.hexComp(String.format("&cPermissions format: invite,createjobs,withdraw,manage,salaries%s (comma-separated)", QuickStocksPlugin.getHookManager().isHooked(HookType.ChestShop) ? ",chestshop" : "")));
            return;
        }
        
        String companyName = args[1];
        String title = args[2];
        String permsStr = args[3].toLowerCase();
        
        Optional<Company> companyOpt = QuickStocksPlugin.getCompanyService().getCompanyByName(companyName);
        if (companyOpt.isEmpty()) {
            Translation.Company_Error_CompanyNotFound.sendMessage(player);
            return;
        }
        
        boolean canInvite = permsStr.contains("invite");
        boolean canCreateTitles = permsStr.contains("createjobs");
        boolean canWithdraw = permsStr.contains("withdraw");
        boolean canManage = permsStr.contains("manage");
        boolean canChestShop = permsStr.contains("chestshop");
        boolean canManageSalaries = permsStr.contains("salaries");
        
        QuickStocksPlugin.getCompanyService().createJobTitle(companyOpt.get().getId(), playerUuid, title, 
                                     canInvite, canCreateTitles, canWithdraw, canManage, canChestShop, canManageSalaries);
        Translation.Company_JobCreated.sendMessage(player, new Replaceable("%company%", companyName), new Replaceable("%job%", title));
    }
    
    private void handleEditJob(Player player, String playerUuid, String[] args) throws Exception {
        if (args.length < 4) {
            Translation.CommandSyntax.sendMessage(player, new Replaceable("%command%", "/company editjob <company> <title> <permissions>"));
            player.sendMessage(ChatUT.hexComp(String.format("&cPermissions format: invite,createjobs,withdraw,manage,salaries%s (comma-separated)", QuickStocksPlugin.getHookManager().isHooked(HookType.ChestShop) ? ",chestshop" : "")));
            return;
        }
        
        String companyName = args[1];
        String title = args[2];
        String permsStr = args[3].toLowerCase();
        
        Optional<Company> companyOpt = QuickStocksPlugin.getCompanyService().getCompanyByName(companyName);
        if (companyOpt.isEmpty()) {
            Translation.Company_Error_CompanyNotFound.sendMessage(player);
            return;
        }
        
        boolean canInvite = permsStr.contains("invite");
        boolean canCreateTitles = permsStr.contains("createjobs");
        boolean canWithdraw = permsStr.contains("withdraw");
        boolean canManage = permsStr.contains("manage");
        boolean canChestShop = permsStr.contains("chestshop");
        boolean canManageSalaries = permsStr.contains("salaries");
        
        QuickStocksPlugin.getCompanyService().updateJobTitle(companyOpt.get().getId(), playerUuid, title,
                                     canInvite, canCreateTitles, canWithdraw, canManage, canChestShop, canManageSalaries);

        String permissions = (canManage ? "Manage " : "") +
                         (canInvite ? "Invite " : "") +
                         (canCreateTitles ? "CreateJobs " : "") +
                         (canWithdraw ? "Withdraw " : "") +
                         (canManageSalaries ? "Salaries " : "") +
                         (canChestShop ? "ChestShop" : "");
        Translation.Company_JobEditedWithPerms.sendMessage(player,
            new Replaceable("%job%", title),
            new Replaceable("%permissions%", permissions));
    }
    
    private void handleAssignJob(Player player, String playerUuid, String[] args) throws Exception {
        if (args.length < 4) {
            Translation.CommandSyntax.sendMessage(player, new Replaceable("%command%", "/company assignjob <company> <player> <job>"));
            return;
        }
        
        String companyName = args[1];
        String targetPlayerName = args[2];
        String jobTitle = args[3];
        
        Optional<Company> companyOpt = QuickStocksPlugin.getCompanyService().getCompanyByName(companyName);
        if (companyOpt.isEmpty()) {
            Translation.Company_Error_CompanyNotFound.sendMessage(player);
            return;
        }
        
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetPlayerName);
        String targetUuid = targetPlayer.getUniqueId().toString();
        
        QuickStocksPlugin.getCompanyService().assignJobTitle(companyOpt.get().getId(), playerUuid, targetUuid, jobTitle);
        Translation.Company_JobAssigned.sendMessage(player, new Replaceable("%company%", companyName), new Replaceable("%player%", targetPlayerName), new Replaceable("%job%", jobTitle));
    }
    
    private void handleSettings(Player player, String playerUuid, String[] args) throws Exception {
        String companyName;
        
        if (args.length < 2) {
            // Show settings for player's first company
            List<Company> companies = QuickStocksPlugin.getCompanyService().getCompaniesByPlayer(playerUuid);
            
            if (companies.isEmpty()) {
                Translation.Company_Error_NotEmployee.sendMessage(player);
                Translation.CommandSyntax.sendMessage(player, new Replaceable("%command%", "/company settings <company-name>"));
                return;
            }
            
            // Use first company
            companyName = companies.get(0).getName();
        } else {
            companyName = args[1];
        }
        
        Optional<Company> companyOpt = QuickStocksPlugin.getCompanyService().getCompanyByName(companyName);
        
        if (companyOpt.isEmpty()) {
            Translation.Company_Error_CompanyNotFound.sendMessage(player);
            return;
        }
        
        Company company = companyOpt.get();
        
        // Check if player is an employee
        Optional<CompanyJob> playerJob = QuickStocksPlugin.getCompanyService().getPlayerJob(company.getId(), playerUuid);
        if (playerJob.isEmpty()) {
            Translation.Company_Error_NotEmployee.sendMessage(player);
            return;
        }
        
        // Open the GUI
        CompanySettingsGUI gui = new CompanySettingsGUI(player, company);
        gui.open();
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return null;
        }
        
        Player player = (Player) sender;
        String playerUuid = player.getUniqueId().toString();
        
        try {
            // Main subcommands
            if (args.length == 1) {
                return Arrays.asList("create", "info", "list", "invite", "accept", "decline", 
                                   "invitations", "deposit", "withdraw", "employees", "jobs", 
                                   "createjob", "editjob", "assignjob", "settings",
                                   "setsymbol", "market", "notifications", "leave", "transferownership", "fire", "salary")
                    .stream()
                    .filter(option -> option.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
            }
            
            // Salary subcommands
            if (args.length == 2 && args[0].equalsIgnoreCase("salary")) {
                return Arrays.asList("set", "setplayer", "removeplayer", "cycle", "reset", "info")
                    .stream()
                    .filter(option -> option.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            }
            
            // Market subcommands
            if (args.length == 2 && args[0].equalsIgnoreCase("market")) {
                return Arrays.asList("enable", "disable", "settings")
                    .stream()
                    .filter(option -> option.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            }
            
            // Company types for create command
            if (args.length == 3 && args[0].equalsIgnoreCase("create")) {
                return Arrays.asList("PRIVATE", "PUBLIC", "DAO")
                    .stream()
                    .filter(option -> option.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
            }
            
            // Company names for commands that need them
            if (args.length == 2) {
                String subcommand = args[0].toLowerCase();
                if (subcommand.equals("info") || subcommand.equals("employees") || subcommand.equals("jobs") ||
                    subcommand.equals("deposit") || subcommand.equals("withdraw") || subcommand.equals("settings") ||
                    subcommand.equals("setsymbol")) {
                    return getCompanyNames(args[1]);
                }
                
                // For invite, createjob, editjob, assignjob, leave, fire, transferownership - suggest player's companies
                if (subcommand.equals("invite") || subcommand.equals("createjob") || subcommand.equals("editjob") || 
                    subcommand.equals("assignjob") || subcommand.equals("leave") || subcommand.equals("fire") || 
                    subcommand.equals("transferownership")) {
                    return getPlayerCompanyNames(playerUuid, args[1]);
                }
            }
            
            // Company names for salary commands (3rd arg)
            if (args.length == 3 && args[0].equalsIgnoreCase("salary")) {
                return getPlayerCompanyNames(playerUuid, args[2]);
            }
            
            // Cycle options for salary cycle command
            if (args.length == 4 && args[0].equalsIgnoreCase("salary") && args[1].equalsIgnoreCase("cycle")) {
                return Arrays.asList("1h", "24h", "1w", "2w", "1m")
                    .stream()
                    .filter(option -> option.toLowerCase().startsWith(args[3].toLowerCase()))
                    .collect(Collectors.toList());
            }
            
            // Company names for market commands (3rd arg)
            if (args.length == 3 && args[0].equalsIgnoreCase("market")) {
                return getCompanyNames(args[2]);
            }
            
            // Market settings subcommands (4th arg)
            if (args.length == 4 && args[0].equalsIgnoreCase("market") && args[1].equalsIgnoreCase("settings")) {
                return Arrays.asList("percentage", "buyout")
                    .stream()
                    .filter(option -> option.toLowerCase().startsWith(args[3].toLowerCase()))
                    .collect(Collectors.toList());
            }
            
            // Player names for invite command (3rd arg)
            if (args.length == 3 && args[0].equalsIgnoreCase("invite")) {
                return getOnlinePlayerNames(args[2]);
            }
            
            // Job titles for invite command (4th arg)
            if (args.length == 4 && args[0].equalsIgnoreCase("invite")) {
                String companyName = args[1];
                return getJobTitles(companyName, args[3]);
            }
            
            // Player names for assignjob, fire, and transferownership commands (3rd arg)
            if (args.length == 3) {
                String subcommand = args[0].toLowerCase();
                if (subcommand.equals("assignjob") || subcommand.equals("fire") || subcommand.equals("transferownership")) {
                    return getOnlinePlayerNames(args[2]);
                }
            }
            
            // Job titles for assignjob command (4th arg)
            if (args.length == 4 && args[0].equalsIgnoreCase("assignjob")) {
                String companyName = args[1];
                return getJobTitles(companyName, args[3]);
            }
            
            // Job titles for editjob command (3rd arg)
            if (args.length == 3 && args[0].equalsIgnoreCase("editjob")) {
                String companyName = args[1];
                return getJobTitles(companyName, args[2]);
            }
            
            // Permission suggestions for createjob and editjob (4th arg)
            if (args.length == 4 && (args[0].equalsIgnoreCase("createjob") || args[0].equalsIgnoreCase("editjob"))) {
                List<String> permissions = Arrays.asList("invite", "createjobs", "withdraw", "manage", "invite,createjobs", "invite,withdraw", "manage,invite,createjobs,withdraw");

                if(QuickStocksPlugin.getHookManager().isHooked(HookType.ChestShop)) {
                    permissions.remove("manage,invite,createjobs,withdraw");
                    permissions.add("manage,invite,createjobs,withdraw, chestshop");
                    permissions.add("chestshop");
                }

                return permissions.stream().filter(option -> option.toLowerCase().startsWith(args[3].toLowerCase())).collect(Collectors.toList());
            }
        } catch (Exception e) {
            // Silently fail for tab completion
            logger.fine("Error in tab completion: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Gets all company names starting with the given prefix
     */
    private List<String> getCompanyNames(String prefix) {
        try {
            List<Company> companies = QuickStocksPlugin.getCompanyService().listCompanies(0, 100);
            return companies.stream()
                .map(Company::getName)
                .filter(name -> name.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
    
    /**
     * Gets company names where the player is an employee
     */
    private List<String> getPlayerCompanyNames(String playerUuid, String prefix) {
        try {
            List<Company> companies = QuickStocksPlugin.getCompanyService().getCompaniesByPlayer(playerUuid);
            return companies.stream()
                .map(Company::getName)
                .filter(name -> name.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
    
    /**
     * Gets job titles for a company
     */
    private List<String> getJobTitles(String companyName, String prefix) {
        try {
            Optional<Company> companyOpt = QuickStocksPlugin.getCompanyService().getCompanyByName(companyName);
            if (companyOpt.isEmpty()) {
                return new ArrayList<>();
            }
            
            List<CompanyJob> jobs = QuickStocksPlugin.getCompanyService().getCompanyJobs(companyOpt.get().getId());
            return jobs.stream()
                .map(CompanyJob::getTitle)
                .filter(title -> title.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
    
    /**
     * Gets online player names starting with the given prefix
     */
    private List<String> getOnlinePlayerNames(String prefix) {
        return Bukkit.getOnlinePlayers().stream()
            .map(Player::getName)
            .filter(name -> name.toLowerCase().startsWith(prefix.toLowerCase()))
            .collect(Collectors.toList());
    }
    
    /**
     * Handles setting the trading symbol for a company.
     */
    private void handleSetSymbol(Player player, String playerUuid, String[] args) throws Exception {
        if (args.length < 3) {
            Translation.CommandSyntax.sendMessage(player, new Replaceable("%command%", "/company setsymbol <company> <symbol>"));
            return;
        }
        
        String companyName = args[1];
        String symbol = args[2];
        
        Optional<Company> companyOpt = QuickStocksPlugin.getCompanyService().getCompanyByName(companyName);
        if (companyOpt.isEmpty()) {
            Translation.Company_Error_CompanyNotFound.sendMessage(player);
            return;
        }
        
        Company company = companyOpt.get();
        
        // Check permission
        if (!company.getOwnerUuid().equals(playerUuid)) {
            Translation.Company_Error_OwnerOnly.sendMessage(player);
            return;
        }

        QuickStocksPlugin.getCompanyMarketService().setSymbol(company.getId(), symbol);
        Translation.Company_SymbolSet.sendMessage(player, new Replaceable("%symbol%", symbol));
    }
    
    /**
     * Handles market operations (enable/disable/settings).
     */
    private void handleMarket(Player player, String playerUuid, String[] args) throws Exception {
        if (args.length < 3) {
            Translation.CommandSyntax.sendMessage(player, new Replaceable("%command%", "/company market <enable|disable|settings> <company> [options]"));
            return;
        }
        
        String action = args[1].toLowerCase();
        String companyName = args[2];
        
        Optional<Company> companyOpt = QuickStocksPlugin.getCompanyService().getCompanyByName(companyName);
        if (companyOpt.isEmpty()) {
            Translation.Company_Error_CompanyNotFound.sendMessage(player);
            return;
        }
        
        Company company = companyOpt.get();
        
        switch (action) {
            case "enable":
                QuickStocksPlugin.getCompanyMarketService().enableMarket(company.getId(), playerUuid);
                Translation.Company_MarketEnabledDetails.sendMessage(player,
                    new Replaceable("%symbol%", company.getSymbol()),
                    new Replaceable("%company%", companyName));
                break;
                
            case "disable":
                QuickStocksPlugin.getCompanyMarketService().disableMarket(company.getId(), playerUuid);
                Translation.Company_MarketDisabledDetails.sendMessage(player);
                break;
                
            case "settings":
                if (args.length < 4) {
                    // Show current settings
                    double sharePrice = QuickStocksPlugin.getCompanyMarketService().calculateSharePrice(company);
                    Translation.Company_MarketSettings.sendMessage(player,
                        new Replaceable("%company%", company.getName()),
                        new Replaceable("%status%", company.isOnMarket() ? "Yes" : "No"),
                        new Replaceable("%symbol%", company.getSymbol() != null ? company.getSymbol() : "Not set"),
                        new Replaceable("%percentage%", String.format("%.1f", company.getMarketPercentage())),
                        new Replaceable("%buyout%", company.isAllowBuyout() ? "Disabled" : "Enabled"),
                        new Replaceable("%price%", String.format("%.2f", sharePrice)));
                } else {
                    // Update settings: /company market settings <company> <percentage|buyout> <value>
                    String setting = args[3].toLowerCase();
                    
                    if (setting.equals("percentage") && args.length >= 5) {
                        double percentage = Double.parseDouble(args[4]);
                        QuickStocksPlugin.getCompanyMarketService().updateMarketSettings(company.getId(), playerUuid, percentage, null);
                        Translation.Company_MarketPercentageUpdated.sendMessage(player,
                            new Replaceable("%percentage%", String.format("%.1f", percentage)));
                    } else if (setting.equals("buyout") && args.length >= 5) {
                        boolean allowBuyout = Boolean.parseBoolean(args[4]);
                        QuickStocksPlugin.getCompanyMarketService().updateMarketSettings(company.getId(), playerUuid, null, allowBuyout);
                        Translation.Company_MarketBuyoutUpdated.sendMessage(player,
                            new Replaceable("%status%", allowBuyout ? "disabled" : "enabled"));
                    } else {
                        Translation.CommandSyntax.sendMessage(player,
                            new Replaceable("%command%", "/company market settings <company> <percentage|buyout> <value>"));
                    }
                }
                break;
                
            default:
                Translation.Company_UnknownMarketAction.sendMessage(player);
                break;
        }
    }
    
    /**
     * Handles viewing player notifications.
     */
    private void handleNotifications(Player player, String playerUuid) throws Exception {
        List<Map<String, Object>> notifications = QuickStocksPlugin.getCompanyMarketService().getUnreadNotifications(playerUuid);
        
        if (notifications.isEmpty()) {
            Translation.Company_NoNotifications.sendMessage(player);
            return;
        }
        
        Translation.Company_NotificationsHeader.sendMessage(player);
        
        for (Map<String, Object> notif : notifications) {
            String message = (String) notif.get("message");
            long createdAt = ((Number) notif.get("created_at")).longValue();
            String timeStr = dateFormat.format(new Date(createdAt));
            
            Translation.Company_NotificationItem.sendMessage(player,
                new Replaceable("%time%", timeStr),
                new Replaceable("%message%", message));
        }
        
        // Mark all as read
        QuickStocksPlugin.getCompanyMarketService().markAllNotificationsRead(playerUuid);
        Translation.Company_NotificationsRead.sendMessage(player);
    }
    
    /**
     * Handles leaving a company.
     */
    private void handleLeave(Player player, String playerUuid, String[] args) throws Exception {
        if (args.length < 2) {
            Translation.CommandSyntax.sendMessage(player, new Replaceable("%command%", "/company leave <company>"));
            return;
        }
        
        String companyName = args[1];
        Optional<Company> companyOpt = QuickStocksPlugin.getCompanyService().getCompanyByName(companyName);
        
        if (companyOpt.isEmpty()) {
            Translation.Company_Error_CompanyNotFound.sendMessage(player);
            return;
        }
        
        Company company = companyOpt.get();
        
        // Try to leave the company
        try {
            QuickStocksPlugin.getCompanyService().removeEmployee(company.getId(), playerUuid);
            Translation.Company_Left.sendMessage(player, new Replaceable("%company%", company.getName()));
        } catch (IllegalArgumentException e) {
            Translation.Errors_Internal.sendMessage(player);
        }
    }
    
    /**
     * Handles transferring company ownership.
     */
    private void handleTransferOwnership(Player player, String playerUuid, String[] args) throws Exception {
        if (args.length < 3) {
            Translation.CommandSyntax.sendMessage(player, new Replaceable("%command%", "/company transferownership <company> <player>"));
            return;
        }
        
        String companyName = args[1];
        String targetPlayerName = args[2];
        
        Optional<Company> companyOpt = QuickStocksPlugin.getCompanyService().getCompanyByName(companyName);
        if (companyOpt.isEmpty()) {
            Translation.Company_Error_CompanyNotFound.sendMessage(player);
            return;
        }
        
        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
        if (targetPlayer == null) {
            Translation.InvalidPlayer.sendMessage(player);
            return;
        }
        
        String targetUuid = targetPlayer.getUniqueId().toString();
        Company company = companyOpt.get();
        
        try {
            QuickStocksPlugin.getCompanyService().transferOwnership(company.getId(), playerUuid, targetUuid);
            Translation.Company_TransferredOwnership.sendMessage(player, new Replaceable("%company%", company.getName()), new Replaceable("%player%", targetPlayerName));
            Translation.Company_TransferredOwnership.sendMessage(targetPlayer, new Replaceable("%company%", company.getName()), new Replaceable("%player%", targetPlayerName));
        } catch (IllegalArgumentException e) {
            Translation.Errors_Internal.sendMessage(player);
        }
    }
    
    /**
     * Handles firing an employee.
     */
    private void handleFire(Player player, String playerUuid, String[] args) throws Exception {
        if (args.length < 3) {
            Translation.CommandSyntax.sendMessage(player, new Replaceable("%command%", "/company fire <company> <player>"));
            return;
        }
        
        String companyName = args[1];
        String targetPlayerName = args[2];
        
        Optional<Company> companyOpt = QuickStocksPlugin.getCompanyService().getCompanyByName(companyName);
        if (companyOpt.isEmpty()) {
            Translation.Company_Error_CompanyNotFound.sendMessage(player);
            return;
        }
        
        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
        if (targetPlayer == null) {
            Translation.InvalidPlayer.sendMessage(player);
            return;
        }
        
        String targetUuid = targetPlayer.getUniqueId().toString();
        Company company = companyOpt.get();
        
        try {
            QuickStocksPlugin.getCompanyService().fireEmployee(company.getId(), playerUuid, targetUuid);
            Translation.Company_Fired.sendMessage(player, new Replaceable("%company%", company.getName()), new Replaceable("%player%", targetPlayerName));
            Translation.Company_FiredOther.sendMessage(targetPlayer, new Replaceable("%company%", company.getName()), new Replaceable("%player%", targetPlayerName));
        } catch (IllegalArgumentException e) {
            Translation.Errors_Internal.sendMessage(player);
        }
    }
    
    /**
     * Handles salary management commands.
     */
    private void handleSalary(Player player, String playerUuid, String[] args) throws Exception {
        if (args.length < 2) {
            showSalaryHelp(player);
            return;
        }
        
        String subcommand = args[1].toLowerCase();
        
        switch (subcommand) {
            case "set":
                handleSalarySet(player, playerUuid, args);
                break;
                
            case "setplayer":
                handleSalarySetPlayer(player, playerUuid, args);
                break;
                
            case "removeplayer":
                handleSalaryRemovePlayer(player, playerUuid, args);
                break;
                
            case "cycle":
                handleSalaryCycle(player, playerUuid, args);
                break;
                
            case "reset":
                handleSalaryReset(player, playerUuid, args);
                break;
                
            case "info":
                handleSalaryInfo(player, playerUuid, args);
                break;
                
            default:
                showSalaryHelp(player);
                break;
        }
    }
    
    private void showSalaryHelp(Player player) {
        player.sendMessage("§e§l=== Company Salary Commands ===");
        player.sendMessage("§e/company salary set <company> <job> <amount> §7- Set job salary");
        player.sendMessage("§e/company salary setplayer <company> <player> <amount> §7- Set player salary");
        player.sendMessage("§e/company salary removeplayer <company> <player> §7- Remove player salary override");
        player.sendMessage("§e/company salary cycle <company> <cycle> §7- Set payment cycle (1h, 24h, 1w, 2w, 1m)");
        player.sendMessage("§e/company salary reset <company> <job> <amount> §7- Reset job salary");
        player.sendMessage("§e/company salary info <company> §7- View salary configuration");
    }
    
    private void handleSalarySet(Player player, String playerUuid, String[] args) throws Exception {
        if (args.length < 5) {
            player.sendMessage("§cUsage: /company salary set <company> <job> <amount>");
            return;
        }
        
        String companyName = args[2];
        String jobTitle = args[3];
        double amount;
        
        try {
            amount = Double.parseDouble(args[4]);
        } catch (NumberFormatException e) {
            Translation.InvalidNumber.sendMessage(player);
            return;
        }
        
        if (amount < 0) {
            player.sendMessage("§cSalary amount cannot be negative");
            return;
        }
        
        Optional<Company> companyOpt = QuickStocksPlugin.getCompanyService().getCompanyByName(companyName);
        if (companyOpt.isEmpty()) {
            Translation.Company_Error_CompanyNotFound.sendMessage(player);
            return;
        }
        
        Company company = companyOpt.get();
        
        // Check permission
        Optional<CompanyJob> playerJob = QuickStocksPlugin.getCompanyService().getPlayerJob(company.getId(), playerUuid);
        if (playerJob.isEmpty() || !playerJob.get().canManageSalaries()) {
            player.sendMessage("§cYou don't have permission to manage salaries in this company");
            return;
        }
        
        // Get target job
        Optional<CompanyJob> targetJob = QuickStocksPlugin.getCompanyService().getJobByTitle(company.getId(), jobTitle);
        if (targetJob.isEmpty()) {
            player.sendMessage("§cJob title not found: " + jobTitle);
            return;
        }
        
        QuickStocksPlugin.getSalaryService().setJobSalary(targetJob.get().getId(), amount);
        player.sendMessage("§aSet salary for job §e" + jobTitle + "§a to §e$" + String.format("%.2f", amount));
    }
    
    private void handleSalarySetPlayer(Player player, String playerUuid, String[] args) throws Exception {
        if (args.length < 5) {
            player.sendMessage("§cUsage: /company salary setplayer <company> <player> <amount>");
            return;
        }
        
        String companyName = args[2];
        String targetPlayerName = args[3];
        double amount;
        
        try {
            amount = Double.parseDouble(args[4]);
        } catch (NumberFormatException e) {
            Translation.InvalidNumber.sendMessage(player);
            return;
        }
        
        if (amount < 0) {
            player.sendMessage("§cSalary amount cannot be negative");
            return;
        }
        
        Optional<Company> companyOpt = QuickStocksPlugin.getCompanyService().getCompanyByName(companyName);
        if (companyOpt.isEmpty()) {
            Translation.Company_Error_CompanyNotFound.sendMessage(player);
            return;
        }
        
        Company company = companyOpt.get();
        
        // Check permission
        Optional<CompanyJob> playerJob = QuickStocksPlugin.getCompanyService().getPlayerJob(company.getId(), playerUuid);
        if (playerJob.isEmpty() || !playerJob.get().canManageSalaries()) {
            player.sendMessage("§cYou don't have permission to manage salaries in this company");
            return;
        }
        
        // Get target player
        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
        if (targetPlayer == null) {
            Translation.InvalidPlayer.sendMessage(player);
            return;
        }
        
        String targetUuid = targetPlayer.getUniqueId().toString();
        
        // Check if target is an employee
        Optional<CompanyJob> targetJob = QuickStocksPlugin.getCompanyService().getPlayerJob(company.getId(), targetUuid);
        if (targetJob.isEmpty()) {
            player.sendMessage("§cPlayer is not an employee of this company");
            return;
        }
        
        QuickStocksPlugin.getSalaryService().setPlayerSalary(company.getId(), targetUuid, amount, playerUuid);
        player.sendMessage("§aSet custom salary for §e" + targetPlayerName + "§a to §e$" + String.format("%.2f", amount));
    }
    
    private void handleSalaryRemovePlayer(Player player, String playerUuid, String[] args) throws Exception {
        if (args.length < 4) {
            player.sendMessage("§cUsage: /company salary removeplayer <company> <player>");
            return;
        }
        
        String companyName = args[2];
        String targetPlayerName = args[3];
        
        Optional<Company> companyOpt = QuickStocksPlugin.getCompanyService().getCompanyByName(companyName);
        if (companyOpt.isEmpty()) {
            Translation.Company_Error_CompanyNotFound.sendMessage(player);
            return;
        }
        
        Company company = companyOpt.get();
        
        // Check permission
        Optional<CompanyJob> playerJob = QuickStocksPlugin.getCompanyService().getPlayerJob(company.getId(), playerUuid);
        if (playerJob.isEmpty() || !playerJob.get().canManageSalaries()) {
            player.sendMessage("§cYou don't have permission to manage salaries in this company");
            return;
        }
        
        // Get target player
        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
        if (targetPlayer == null) {
            Translation.InvalidPlayer.sendMessage(player);
            return;
        }
        
        String targetUuid = targetPlayer.getUniqueId().toString();
        
        QuickStocksPlugin.getSalaryService().removePlayerSalary(company.getId(), targetUuid);
        player.sendMessage("§aRemoved custom salary for §e" + targetPlayerName + "§a. They will now receive their job salary.");
    }
    
    private void handleSalaryCycle(Player player, String playerUuid, String[] args) throws Exception {
        if (args.length < 4) {
            player.sendMessage("§cUsage: /company salary cycle <company> <cycle>");
            player.sendMessage("§7Available cycles: 1h, 24h, 1w, 2w, 1m");
            return;
        }
        
        String companyName = args[2];
        String cycle = args[3];
        
        Optional<Company> companyOpt = QuickStocksPlugin.getCompanyService().getCompanyByName(companyName);
        if (companyOpt.isEmpty()) {
            Translation.Company_Error_CompanyNotFound.sendMessage(player);
            return;
        }
        
        Company company = companyOpt.get();
        
        // Check permission
        Optional<CompanyJob> playerJob = QuickStocksPlugin.getCompanyService().getPlayerJob(company.getId(), playerUuid);
        if (playerJob.isEmpty() || !playerJob.get().canManageSalaries()) {
            player.sendMessage("§cYou don't have permission to manage salaries in this company");
            return;
        }
        
        try {
            QuickStocksPlugin.getSalaryService().setPaymentCycle(company.getId(), cycle);
            player.sendMessage("§aSet payment cycle for §e" + companyName + "§a to §e" + cycle);
        } catch (IllegalArgumentException e) {
            player.sendMessage("§c" + e.getMessage());
        }
    }
    
    private void handleSalaryReset(Player player, String playerUuid, String[] args) throws Exception {
        if (args.length < 5) {
            player.sendMessage("§cUsage: /company salary reset <company> <job> <amount>");
            return;
        }
        
        String companyName = args[2];
        String jobTitle = args[3];
        double amount;
        
        try {
            amount = Double.parseDouble(args[4]);
        } catch (NumberFormatException e) {
            Translation.InvalidNumber.sendMessage(player);
            return;
        }
        
        if (amount < 0) {
            player.sendMessage("§cSalary amount cannot be negative");
            return;
        }
        
        Optional<Company> companyOpt = QuickStocksPlugin.getCompanyService().getCompanyByName(companyName);
        if (companyOpt.isEmpty()) {
            Translation.Company_Error_CompanyNotFound.sendMessage(player);
            return;
        }
        
        Company company = companyOpt.get();
        
        // Check permission
        Optional<CompanyJob> playerJob = QuickStocksPlugin.getCompanyService().getPlayerJob(company.getId(), playerUuid);
        if (playerJob.isEmpty() || !playerJob.get().canManageSalaries()) {
            player.sendMessage("§cYou don't have permission to manage salaries in this company");
            return;
        }
        
        // Get target job
        Optional<CompanyJob> targetJob = QuickStocksPlugin.getCompanyService().getJobByTitle(company.getId(), jobTitle);
        if (targetJob.isEmpty()) {
            player.sendMessage("§cJob title not found: " + jobTitle);
            return;
        }
        
        QuickStocksPlugin.getSalaryService().setJobSalary(targetJob.get().getId(), amount);
        player.sendMessage("§aReset salary for job §e" + jobTitle + "§a to §e$" + String.format("%.2f", amount));
    }
    
    private void handleSalaryInfo(Player player, String playerUuid, String[] args) throws Exception {
        if (args.length < 3) {
            player.sendMessage("§cUsage: /company salary info <company>");
            return;
        }
        
        String companyName = args[2];
        
        Optional<Company> companyOpt = QuickStocksPlugin.getCompanyService().getCompanyByName(companyName);
        if (companyOpt.isEmpty()) {
            Translation.Company_Error_CompanyNotFound.sendMessage(player);
            return;
        }
        
        Company company = companyOpt.get();
        
        // Check if player is an employee
        Optional<CompanyJob> playerJob = QuickStocksPlugin.getCompanyService().getPlayerJob(company.getId(), playerUuid);
        if (playerJob.isEmpty()) {
            player.sendMessage("§cYou are not an employee of this company");
            return;
        }
        
        // Get payment cycle
        String cycle = QuickStocksPlugin.getSalaryService().getPaymentCycle(company.getId());
        long lastPayment = QuickStocksPlugin.getSalaryService().getLastPaymentTime(company.getId());
        
        player.sendMessage("§e§l=== Salary Info for " + companyName + " ===");
        player.sendMessage("§7Payment Cycle: §e" + cycle);
        if (lastPayment > 0) {
            player.sendMessage("§7Last Payment: §e" + dateFormat.format(new Date(lastPayment)));
        } else {
            player.sendMessage("§7Last Payment: §eNever");
        }
        player.sendMessage("");
        
        // Get salary info for all employees
        List<Map<String, Object>> salaryInfo = QuickStocksPlugin.getSalaryService().getCompanySalaryInfo(company.getId());
        
        player.sendMessage("§e§lEmployee Salaries:");
        for (Map<String, Object> info : salaryInfo) {
            String empUuid = (String) info.get("player_uuid");
            String jobTitle = (String) info.get("job_title");
            
            Object jobSalaryObj = info.get("job_salary");
            Object playerSalaryObj = info.get("player_salary");
            
            double jobSalary = jobSalaryObj != null ? ((Number) jobSalaryObj).doubleValue() : 0.0;
            Double playerSalary = playerSalaryObj != null ? ((Number) playerSalaryObj).doubleValue() : null;
            
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(empUuid));
            String playerName = offlinePlayer.getName();
            if (playerName == null) {
                playerName = empUuid.substring(0, 8);
            }
            
            if (playerSalary != null) {
                player.sendMessage("§7  " + playerName + " (" + jobTitle + "): §e$" + 
                    String.format("%.2f", playerSalary) + " §7(custom)");
            } else {
                player.sendMessage("§7  " + playerName + " (" + jobTitle + "): §e$" + 
                    String.format("%.2f", jobSalary) + " §7(from job)");
            }
        }
    }
}
