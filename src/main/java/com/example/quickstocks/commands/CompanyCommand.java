package com.example.quickstocks.commands;

import com.example.quickstocks.core.model.*;
import com.example.quickstocks.core.services.CompanyService;
import com.example.quickstocks.core.services.InvitationService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
    
    private final CompanyService companyService;
    private final InvitationService invitationService;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    
    public CompanyCommand(CompanyService companyService, InvitationService invitationService) {
        this.companyService = companyService;
        this.invitationService = invitationService;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
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
                    
                default:
                    player.sendMessage(ChatColor.RED + "Unknown subcommand. Use /company for help.");
                    break;
            }
            
        } catch (Exception e) {
            logger.warning("Error in company command for " + player.getName() + ": " + e.getMessage());
            player.sendMessage(ChatColor.RED + "Error: " + e.getMessage());
        }
        
        return true;
    }
    
    private void showHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + "Company Commands" + ChatColor.GOLD + " ===");
        player.sendMessage(ChatColor.YELLOW + "/company create <name> <type>" + ChatColor.GRAY + " - Create a company");
        player.sendMessage(ChatColor.YELLOW + "/company info [name]" + ChatColor.GRAY + " - View company info");
        player.sendMessage(ChatColor.YELLOW + "/company list" + ChatColor.GRAY + " - List all companies");
        player.sendMessage(ChatColor.YELLOW + "/company invite <player> <job>" + ChatColor.GRAY + " - Invite player");
        player.sendMessage(ChatColor.YELLOW + "/company invitations" + ChatColor.GRAY + " - View your invitations");
        player.sendMessage(ChatColor.YELLOW + "/company accept <id>" + ChatColor.GRAY + " - Accept invitation");
        player.sendMessage(ChatColor.YELLOW + "/company decline <id>" + ChatColor.GRAY + " - Decline invitation");
        player.sendMessage(ChatColor.YELLOW + "/company deposit <company> <amount>" + ChatColor.GRAY + " - Deposit funds");
        player.sendMessage(ChatColor.YELLOW + "/company withdraw <company> <amount>" + ChatColor.GRAY + " - Withdraw funds");
        player.sendMessage(ChatColor.YELLOW + "/company employees <company>" + ChatColor.GRAY + " - List employees");
        player.sendMessage(ChatColor.YELLOW + "/company jobs <company>" + ChatColor.GRAY + " - List job titles");
        player.sendMessage(ChatColor.YELLOW + "/company createjob <company> <title> <perms>" + ChatColor.GRAY + " - Create job");
        player.sendMessage(ChatColor.YELLOW + "/company editjob <company> <title> <perms>" + ChatColor.GRAY + " - Edit job");
        player.sendMessage(ChatColor.YELLOW + "/company assignjob <company> <player> <job>" + ChatColor.GRAY + " - Assign job");
    }
    
    private void handleCreate(Player player, String playerUuid, String[] args) throws Exception {
        if (!player.hasPermission("quickstocks.company.create")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to create companies.");
            return;
        }
        
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /company create <name> <type>");
            player.sendMessage(ChatColor.GRAY + "Types: PRIVATE, PUBLIC, DAO");
            return;
        }
        
        String name = args[1];
        String type = args[2].toUpperCase();
        
        Company company = companyService.createCompany(playerUuid, name, type);
        
        player.sendMessage(ChatColor.GREEN + "Successfully created company: " + ChatColor.WHITE + name);
        player.sendMessage(ChatColor.GRAY + "Type: " + type + " | Balance: $0.00");
    }
    
    private void handleInfo(Player player, String playerUuid, String[] args) throws Exception {
        String companyName;
        
        if (args.length < 2) {
            // Show player's companies
            List<Company> companies = companyService.getCompaniesByPlayer(playerUuid);
            
            if (companies.isEmpty()) {
                player.sendMessage(ChatColor.YELLOW + "You are not part of any company.");
                return;
            }
            
            player.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + "Your Companies" + ChatColor.GOLD + " ===");
            for (Company company : companies) {
                Optional<CompanyJob> job = companyService.getPlayerJob(company.getId(), playerUuid);
                String jobTitle = job.isPresent() ? job.get().getTitle() : "Unknown";
                
                player.sendMessage(ChatColor.YELLOW + company.getName() + ChatColor.GRAY + 
                                 " (" + company.getType() + ") - " + ChatColor.WHITE + jobTitle);
                player.sendMessage(ChatColor.GRAY + "  Balance: " + ChatColor.GREEN + "$" + 
                                 String.format("%.2f", company.getBalance()));
            }
            return;
        }
        
        companyName = args[1];
        Optional<Company> companyOpt = companyService.getCompanyByName(companyName);
        
        if (companyOpt.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Company not found: " + companyName);
            return;
        }
        
        Company company = companyOpt.get();
        OfflinePlayer owner = Bukkit.getOfflinePlayer(UUID.fromString(company.getOwnerUuid()));
        
        player.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + company.getName() + ChatColor.GOLD + " ===");
        player.sendMessage(ChatColor.YELLOW + "Type: " + ChatColor.WHITE + company.getType());
        player.sendMessage(ChatColor.YELLOW + "Owner: " + ChatColor.WHITE + (owner.getName() != null ? owner.getName() : "Unknown"));
        player.sendMessage(ChatColor.YELLOW + "Balance: " + ChatColor.GREEN + "$" + String.format("%.2f", company.getBalance()));
        player.sendMessage(ChatColor.YELLOW + "Created: " + ChatColor.WHITE + dateFormat.format(new Date(company.getCreatedAt())));
        
        // Show player's job if they're an employee
        Optional<CompanyJob> playerJob = companyService.getPlayerJob(company.getId(), playerUuid);
        if (playerJob.isPresent()) {
            CompanyJob job = playerJob.get();
            player.sendMessage(ChatColor.YELLOW + "Your Job: " + ChatColor.WHITE + job.getTitle());
            player.sendMessage(ChatColor.GRAY + "Permissions: " + 
                             (job.canManageCompany() ? "Manage " : "") +
                             (job.canInvite() ? "Invite " : "") +
                             (job.canCreateTitles() ? "CreateJobs " : "") +
                             (job.canWithdraw() ? "Withdraw" : ""));
        }
    }
    
    private void handleList(Player player, String[] args) throws Exception {
        int page = 0;
        if (args.length > 1) {
            try {
                page = Integer.parseInt(args[1]) - 1;
                if (page < 0) page = 0;
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Invalid page number.");
                return;
            }
        }
        
        List<Company> companies = companyService.listCompanies(page, 10);
        
        if (companies.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "No companies found.");
            return;
        }
        
        player.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + "Companies (Page " + (page + 1) + ")" + ChatColor.GOLD + " ===");
        for (Company company : companies) {
            player.sendMessage(ChatColor.YELLOW + company.getName() + ChatColor.GRAY + 
                             " (" + company.getType() + ") - Balance: " + ChatColor.GREEN + "$" + 
                             String.format("%.2f", company.getBalance()));
        }
    }
    
    private void handleInvite(Player player, String playerUuid, String[] args) throws Exception {
        if (args.length < 4) {
            player.sendMessage(ChatColor.RED + "Usage: /company invite <company> <player> <job>");
            return;
        }
        
        String companyName = args[1];
        String targetPlayerName = args[2];
        String jobTitle = args[3];
        
        Optional<Company> companyOpt = companyService.getCompanyByName(companyName);
        if (companyOpt.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Company not found: " + companyName);
            return;
        }
        
        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
        if (targetPlayer == null) {
            player.sendMessage(ChatColor.RED + "Player not found or not online: " + targetPlayerName);
            return;
        }
        
        String targetUuid = targetPlayer.getUniqueId().toString();
        String companyId = companyOpt.get().getId();
        
        CompanyInvitation invitation = invitationService.createInvitation(companyId, playerUuid, targetUuid, jobTitle);
        
        player.sendMessage(ChatColor.GREEN + "Invitation sent to " + targetPlayerName);
        targetPlayer.sendMessage(ChatColor.GOLD + "You've been invited to join " + ChatColor.WHITE + companyOpt.get().getName());
        targetPlayer.sendMessage(ChatColor.GRAY + "Use " + ChatColor.YELLOW + "/company accept " + invitation.getId() + 
                               ChatColor.GRAY + " to accept");
    }
    
    private void handleAccept(Player player, String playerUuid, String[] args) throws Exception {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /company accept <invitation-id>");
            return;
        }
        
        String invitationId = args[1];
        invitationService.acceptInvitation(invitationId, playerUuid);
        
        // Get company info
        Optional<CompanyInvitation> invOpt = invitationService.getInvitationById(invitationId);
        if (invOpt.isPresent()) {
            Optional<Company> companyOpt = companyService.getCompanyById(invOpt.get().getCompanyId());
            if (companyOpt.isPresent()) {
                player.sendMessage(ChatColor.GREEN + "You've joined " + companyOpt.get().getName() + "!");
            }
        }
    }
    
    private void handleDecline(Player player, String playerUuid, String[] args) throws Exception {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /company decline <invitation-id>");
            return;
        }
        
        String invitationId = args[1];
        invitationService.declineInvitation(invitationId, playerUuid);
        
        player.sendMessage(ChatColor.YELLOW + "Invitation declined.");
    }
    
    private void handleInvitations(Player player, String playerUuid) throws Exception {
        List<CompanyInvitation> invitations = invitationService.getPendingInvitations(playerUuid);
        
        if (invitations.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "You have no pending invitations.");
            return;
        }
        
        player.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + "Pending Invitations" + ChatColor.GOLD + " ===");
        for (CompanyInvitation invitation : invitations) {
            Optional<Company> companyOpt = companyService.getCompanyById(invitation.getCompanyId());
            if (companyOpt.isPresent()) {
                Company company = companyOpt.get();
                player.sendMessage(ChatColor.YELLOW + company.getName() + ChatColor.GRAY + 
                                 " - ID: " + ChatColor.WHITE + invitation.getId());
                player.sendMessage(ChatColor.GRAY + "  Expires: " + dateFormat.format(new Date(invitation.getExpiresAt())));
            }
        }
    }
    
    private void handleDeposit(Player player, String playerUuid, String[] args) throws Exception {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /company deposit <company> <amount>");
            return;
        }
        
        String companyName = args[1];
        double amount;
        
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid amount: " + args[2]);
            return;
        }
        
        Optional<Company> companyOpt = companyService.getCompanyByName(companyName);
        if (companyOpt.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Company not found: " + companyName);
            return;
        }
        
        companyService.deposit(companyOpt.get().getId(), playerUuid, amount);
        
        player.sendMessage(ChatColor.GREEN + "Deposited $" + String.format("%.2f", amount) + 
                         " to " + companyOpt.get().getName());
    }
    
    private void handleWithdraw(Player player, String playerUuid, String[] args) throws Exception {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /company withdraw <company> <amount>");
            return;
        }
        
        String companyName = args[1];
        double amount;
        
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid amount: " + args[2]);
            return;
        }
        
        Optional<Company> companyOpt = companyService.getCompanyByName(companyName);
        if (companyOpt.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Company not found: " + companyName);
            return;
        }
        
        companyService.withdraw(companyOpt.get().getId(), playerUuid, amount);
        
        player.sendMessage(ChatColor.GREEN + "Withdrew $" + String.format("%.2f", amount) + 
                         " from " + companyOpt.get().getName());
    }
    
    private void handleEmployees(Player player, String[] args) throws Exception {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /company employees <company>");
            return;
        }
        
        String companyName = args[1];
        Optional<Company> companyOpt = companyService.getCompanyByName(companyName);
        
        if (companyOpt.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Company not found: " + companyName);
            return;
        }
        
        List<Map<String, Object>> employees = companyService.getCompanyEmployees(companyOpt.get().getId());
        
        if (employees.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "No employees found.");
            return;
        }
        
        player.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + companyOpt.get().getName() + 
                         " Employees" + ChatColor.GOLD + " ===");
        for (Map<String, Object> emp : employees) {
            String playerUuid = (String) emp.get("player_uuid");
            String title = (String) emp.get("title");
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(playerUuid));
            String playerName = offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unknown";
            
            player.sendMessage(ChatColor.YELLOW + playerName + ChatColor.GRAY + " - " + ChatColor.WHITE + title);
        }
    }
    
    private void handleJobs(Player player, String[] args) throws Exception {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /company jobs <company>");
            return;
        }
        
        String companyName = args[1];
        Optional<Company> companyOpt = companyService.getCompanyByName(companyName);
        
        if (companyOpt.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Company not found: " + companyName);
            return;
        }
        
        List<CompanyJob> jobs = companyService.getCompanyJobs(companyOpt.get().getId());
        
        if (jobs.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "No job titles found.");
            return;
        }
        
        player.sendMessage(ChatColor.GOLD + "=== " + ChatColor.WHITE + companyOpt.get().getName() + 
                         " Job Titles" + ChatColor.GOLD + " ===");
        for (CompanyJob job : jobs) {
            player.sendMessage(ChatColor.YELLOW + job.getTitle());
            player.sendMessage(ChatColor.GRAY + "  Permissions: " + 
                             (job.canManageCompany() ? "Manage " : "") +
                             (job.canInvite() ? "Invite " : "") +
                             (job.canCreateTitles() ? "CreateJobs " : "") +
                             (job.canWithdraw() ? "Withdraw" : ""));
        }
    }
    
    private void handleCreateJob(Player player, String playerUuid, String[] args) throws Exception {
        if (args.length < 4) {
            player.sendMessage(ChatColor.RED + "Usage: /company createjob <company> <title> <permissions>");
            player.sendMessage(ChatColor.GRAY + "Permissions format: invite,createjobs,withdraw,manage (comma-separated)");
            return;
        }
        
        String companyName = args[1];
        String title = args[2];
        String permsStr = args[3].toLowerCase();
        
        Optional<Company> companyOpt = companyService.getCompanyByName(companyName);
        if (companyOpt.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Company not found: " + companyName);
            return;
        }
        
        boolean canInvite = permsStr.contains("invite");
        boolean canCreateTitles = permsStr.contains("createjobs");
        boolean canWithdraw = permsStr.contains("withdraw");
        boolean canManage = permsStr.contains("manage");
        
        companyService.createJobTitle(companyOpt.get().getId(), playerUuid, title, 
                                     canInvite, canCreateTitles, canWithdraw, canManage);
        
        player.sendMessage(ChatColor.GREEN + "Created job title: " + title);
    }
    
    private void handleEditJob(Player player, String playerUuid, String[] args) throws Exception {
        if (args.length < 4) {
            player.sendMessage(ChatColor.RED + "Usage: /company editjob <company> <title> <permissions>");
            player.sendMessage(ChatColor.GRAY + "Permissions format: invite,createjobs,withdraw,manage (comma-separated)");
            return;
        }
        
        String companyName = args[1];
        String title = args[2];
        String permsStr = args[3].toLowerCase();
        
        Optional<Company> companyOpt = companyService.getCompanyByName(companyName);
        if (companyOpt.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Company not found: " + companyName);
            return;
        }
        
        boolean canInvite = permsStr.contains("invite");
        boolean canCreateTitles = permsStr.contains("createjobs");
        boolean canWithdraw = permsStr.contains("withdraw");
        boolean canManage = permsStr.contains("manage");
        
        companyService.updateJobTitle(companyOpt.get().getId(), playerUuid, title,
                                     canInvite, canCreateTitles, canWithdraw, canManage);
        
        player.sendMessage(ChatColor.GREEN + "Updated job title: " + title);
        player.sendMessage(ChatColor.GRAY + "New permissions: " + 
                         (canManage ? "Manage " : "") +
                         (canInvite ? "Invite " : "") +
                         (canCreateTitles ? "CreateJobs " : "") +
                         (canWithdraw ? "Withdraw" : ""));
    }
    
    private void handleAssignJob(Player player, String playerUuid, String[] args) throws Exception {
        if (args.length < 4) {
            player.sendMessage(ChatColor.RED + "Usage: /company assignjob <company> <player> <job>");
            return;
        }
        
        String companyName = args[1];
        String targetPlayerName = args[2];
        String jobTitle = args[3];
        
        Optional<Company> companyOpt = companyService.getCompanyByName(companyName);
        if (companyOpt.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Company not found: " + companyName);
            return;
        }
        
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetPlayerName);
        String targetUuid = targetPlayer.getUniqueId().toString();
        
        companyService.assignJobTitle(companyOpt.get().getId(), playerUuid, targetUuid, jobTitle);
        
        player.sendMessage(ChatColor.GREEN + "Assigned job " + jobTitle + " to " + targetPlayerName);
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("create", "info", "list", "invite", "accept", "decline", 
                               "invitations", "deposit", "withdraw", "employees", "jobs", 
                               "createjob", "editjob", "assignjob")
                .stream()
                .filter(option -> option.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        if (args.length == 2) {
            // For create command, suggest company types
            if (args[0].equalsIgnoreCase("create")) {
                return Arrays.asList("PRIVATE", "PUBLIC", "DAO")
                    .stream()
                    .filter(option -> option.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            }
            
            // For commands that need a company name, suggest company names
            if (args[0].equalsIgnoreCase("jobs") || args[0].equalsIgnoreCase("employees") ||
                args[0].equalsIgnoreCase("createjob") || args[0].equalsIgnoreCase("editjob") ||
                args[0].equalsIgnoreCase("assignjob") || args[0].equalsIgnoreCase("deposit") ||
                args[0].equalsIgnoreCase("withdraw") || args[0].equalsIgnoreCase("invite")) {
                return getCompanyNames(args[1]);
            }
        }
        
        if (args.length == 3) {
            // For editjob and assignjob, suggest job titles
            if (args[0].equalsIgnoreCase("editjob") || args[0].equalsIgnoreCase("assignjob")) {
                return getJobTitles(args[1], args[2]);
            }
            
            // For invite and assignjob, suggest player names
            if (args[0].equalsIgnoreCase("assignjob")) {
                return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
            }
        }
        
        if (args.length == 4) {
            // For createjob and editjob, suggest permission combinations
            if (args[0].equalsIgnoreCase("createjob") || args[0].equalsIgnoreCase("editjob")) {
                return Arrays.asList("invite", "createjobs", "withdraw", "manage", 
                                   "invite,withdraw", "invite,createjobs,withdraw", 
                                   "invite,createjobs,withdraw,manage")
                    .stream()
                    .filter(option -> option.toLowerCase().startsWith(args[3].toLowerCase()))
                    .collect(Collectors.toList());
            }
            
            // For invite and assignjob, suggest job titles
            if (args[0].equalsIgnoreCase("invite") || args[0].equalsIgnoreCase("assignjob")) {
                return getJobTitles(args[1], args[3]);
            }
        }
        
        return null;
    }
    
    private List<String> getCompanyNames(String prefix) {
        try {
            List<Company> companies = companyService.listCompanies(0, 100);
            return companies.stream()
                .map(Company::getName)
                .filter(name -> name.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
        } catch (Exception e) {
            return null;
        }
    }
    
    private List<String> getJobTitles(String companyName, String prefix) {
        try {
            Optional<Company> companyOpt = companyService.getCompanyByName(companyName);
            if (companyOpt.isEmpty()) {
                return null;
            }
            
            List<CompanyJob> jobs = companyService.getCompanyJobs(companyOpt.get().getId());
            return jobs.stream()
                .map(CompanyJob::getTitle)
                .filter(title -> title.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
        } catch (Exception e) {
            return null;
        }
    }
}
