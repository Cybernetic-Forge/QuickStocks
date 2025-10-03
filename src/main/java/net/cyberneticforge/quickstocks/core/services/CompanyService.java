package net.cyberneticforge.quickstocks.core.services;

import net.cyberneticforge.quickstocks.core.model.Company;
import net.cyberneticforge.quickstocks.core.model.CompanyJob;
import net.cyberneticforge.quickstocks.infrastructure.config.CompanyConfig;
import net.cyberneticforge.quickstocks.infrastructure.db.Db;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Service for managing companies and their operations.
 */
public class CompanyService {
    
    private static final Logger logger = Logger.getLogger(CompanyService.class.getName());
    
    private final Db database;
    private final WalletService walletService;
    private final CompanyConfig config;
    
    public CompanyService(Db database, WalletService walletService, CompanyConfig config) {
        this.database = database;
        this.walletService = walletService;
        this.config = config;
    }
    
    /**
     * Creates a new company.
     */
    public Company createCompany(String playerUuid, String name, String type) throws SQLException {
        if (!config.isEnabled()) {
            throw new IllegalStateException("Company system is not enabled");
        }
        
        // Check name uniqueness
        List<Map<String, Object>> existing = database.query(
            "SELECT id FROM companies WHERE name = ?", name);
        if (!existing.isEmpty()) {
            throw new IllegalArgumentException("Company name already exists");
        }
        
        // Validate type
        if (!config.getDefaultTypes().contains(type)) {
            throw new IllegalArgumentException("Invalid company type: " + type);
        }
        
        // Charge creation cost
        if (config.getCreationCost() > 0) {
            double balance = walletService.getBalance(playerUuid);
            if (balance < config.getCreationCost()) {
                throw new IllegalArgumentException("Insufficient funds. Required: $" + 
                    String.format("%.2f", config.getCreationCost()));
            }
            walletService.removeBalance(playerUuid, config.getCreationCost());
        }
        
        // Create company
        String companyId = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();
        
        database.execute(
            "INSERT INTO companies (id, name, type, owner_uuid, balance, created_at) VALUES (?, ?, ?, ?, ?, ?)",
            companyId, name, type, playerUuid, 0.0, now
        );
        
        // Create default job titles
        Map<String, String> jobIdMap = new HashMap<>();
        for (String title : config.getDefaultJobTitles()) {
            CompanyConfig.JobPermissions perms = config.getPermissionsByTitle().get(title);
            if (perms == null) {
                perms = new CompanyConfig.JobPermissions();
            }
            
            String jobId = UUID.randomUUID().toString();
            jobIdMap.put(title, jobId);
            
            database.execute(
                "INSERT INTO company_jobs (id, company_id, title, can_invite, can_create_titles, can_withdraw, can_manage_company, can_manage_chestshop) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                jobId, companyId, title, 
                perms.isCanInvite() ? 1 : 0,
                perms.isCanCreateJobTitles() ? 1 : 0,
                perms.isCanWithdraw() ? 1 : 0,
                perms.isCanManageCompany() ? 1 : 0,
                perms.isCanManageChestShop() ? 1 : 0
            );
        }
        
        // Add creator as CEO
        String ceoJobId = jobIdMap.get("CEO");
        if (ceoJobId != null) {
            database.execute(
                "INSERT INTO company_employees (company_id, player_uuid, job_id, joined_at) VALUES (?, ?, ?, ?)",
                companyId, playerUuid, ceoJobId, now
            );
        }
        
        logger.info("Created company '" + name + "' (ID: " + companyId + ") for player " + playerUuid);
        
        return new Company(companyId, name, type, playerUuid, 0.0, now);
    }
    
    /**
     * Gets a company by name.
     */
    public Optional<Company> getCompanyByName(String name) throws SQLException {
        List<Map<String, Object>> results = database.query(
            "SELECT id, name, type, owner_uuid, balance, created_at, symbol, on_market, market_percentage, allow_buyout FROM companies WHERE name = ?", name);
        
        if (results.isEmpty()) {
            return Optional.empty();
        }
        
        Map<String, Object> row = results.get(0);
        return Optional.of(new Company(
            (String) row.get("id"),
            (String) row.get("name"),
            (String) row.get("type"),
            (String) row.get("owner_uuid"),
            ((Number) row.get("balance")).doubleValue(),
            ((Number) row.get("created_at")).longValue(),
            (String) row.get("symbol"),
            ((Number) row.get("on_market")).intValue() != 0,
            ((Number) row.get("market_percentage")).doubleValue(),
            ((Number) row.get("allow_buyout")).intValue() != 0
        ));
    }
    
    /**
     * Gets a company by ID.
     */
    public Optional<Company> getCompanyById(String companyId) throws SQLException {
        List<Map<String, Object>> results = database.query(
            "SELECT id, name, type, owner_uuid, balance, created_at, symbol, on_market, market_percentage, allow_buyout FROM companies WHERE id = ?", companyId);
        
        if (results.isEmpty()) {
            return Optional.empty();
        }
        
        Map<String, Object> row = results.get(0);
        return Optional.of(new Company(
            (String) row.get("id"),
            (String) row.get("name"),
            (String) row.get("type"),
            (String) row.get("owner_uuid"),
            ((Number) row.get("balance")).doubleValue(),
            ((Number) row.get("created_at")).longValue(),
            (String) row.get("symbol"),
            ((Number) row.get("on_market")).intValue() != 0,
            ((Number) row.get("market_percentage")).doubleValue(),
            ((Number) row.get("allow_buyout")).intValue() != 0
        ));
    }
    
    /**
     * Gets a company by trading symbol.
     */
    public Optional<Company> getCompanyBySymbol(String symbol) throws SQLException {
        if (symbol == null || symbol.trim().isEmpty()) {
            return Optional.empty();
        }
        
        List<Map<String, Object>> results = database.query(
            "SELECT id, name, type, owner_uuid, balance, created_at, symbol, on_market, market_percentage, allow_buyout FROM companies WHERE UPPER(symbol) = UPPER(?)", 
            symbol.trim());
        
        if (results.isEmpty()) {
            return Optional.empty();
        }
        
        Map<String, Object> row = results.get(0);
        return Optional.of(new Company(
            (String) row.get("id"),
            (String) row.get("name"),
            (String) row.get("type"),
            (String) row.get("owner_uuid"),
            ((Number) row.get("balance")).doubleValue(),
            ((Number) row.get("created_at")).longValue(),
            (String) row.get("symbol"),
            ((Number) row.get("on_market")).intValue() != 0,
            ((Number) row.get("market_percentage")).doubleValue(),
            ((Number) row.get("allow_buyout")).intValue() != 0
        ));
    }
    
    /**
     * Gets a company by name or symbol (tries symbol first, then name).
     */
    public Optional<Company> getCompanyByNameOrSymbol(String nameOrSymbol) throws SQLException {
        // Try symbol first
        Optional<Company> company = getCompanyBySymbol(nameOrSymbol);
        if (company.isPresent()) {
            return company;
        }
        
        // Fall back to name
        return getCompanyByName(nameOrSymbol);
    }

    /**
     * Gets all companies.
     */
    public List<Company> getAllCompanies() throws SQLException {
        List<Map<String, Object>> results = database.query(
            "SELECT id, name, type, owner_uuid, balance, created_at, symbol, on_market, market_percentage, allow_buyout FROM companies ORDER BY created_at DESC",
            new Object[0]
        );

        List<Company> companies = new ArrayList<>();
        for (Map<String, Object> row : results) {
            companies.add(new Company(
                (String) row.get("id"),
                (String) row.get("name"),
                (String) row.get("type"),
                (String) row.get("owner_uuid"),
                ((Number) row.get("balance")).doubleValue(),
                ((Number) row.get("created_at")).longValue(),
                (String) row.get("symbol"),
                ((Number) row.get("on_market")).intValue() != 0,
                ((Number) row.get("market_percentage")).doubleValue(),
                ((Number) row.get("allow_buyout")).intValue() != 0
            ));
        }

        return companies;
    }

    /**
     * Gets companies where the player is an employee.
     */
    public List<Company> getCompaniesByPlayer(String playerUuid) throws SQLException {
        List<Map<String, Object>> results = database.query(
            "SELECT c.id, c.name, c.type, c.owner_uuid, c.balance, c.created_at, c.symbol, c.on_market, c.market_percentage, c.allow_buyout " +
            "FROM companies c " +
            "INNER JOIN company_employees ce ON c.id = ce.company_id " +
            "WHERE ce.player_uuid = ?",
            playerUuid
        );
        
        List<Company> companies = new ArrayList<>();
        for (Map<String, Object> row : results) {
            companies.add(new Company(
                (String) row.get("id"),
                (String) row.get("name"),
                (String) row.get("type"),
                (String) row.get("owner_uuid"),
                ((Number) row.get("balance")).doubleValue(),
                ((Number) row.get("created_at")).longValue(),
                (String) row.get("symbol"),
                ((Number) row.get("on_market")).intValue() != 0,
                ((Number) row.get("market_percentage")).doubleValue(),
                ((Number) row.get("allow_buyout")).intValue() != 0
            ));
        }
        
        return companies;
    }
    
    /**
     * Lists all companies with pagination.
     */
    public List<Company> listCompanies(int page, int pageSize) throws SQLException {
        int offset = page * pageSize;
        
        List<Map<String, Object>> results = database.query(
            "SELECT id, name, type, owner_uuid, balance, created_at, symbol, on_market, market_percentage, allow_buyout FROM companies " +
            "ORDER BY created_at DESC LIMIT ? OFFSET ?",
            pageSize, offset
        );
        
        List<Company> companies = new ArrayList<>();
        for (Map<String, Object> row : results) {
            companies.add(new Company(
                (String) row.get("id"),
                (String) row.get("name"),
                (String) row.get("type"),
                (String) row.get("owner_uuid"),
                ((Number) row.get("balance")).doubleValue(),
                ((Number) row.get("created_at")).longValue(),
                (String) row.get("symbol"),
                ((Number) row.get("on_market")).intValue() != 0,
                ((Number) row.get("market_percentage")).doubleValue(),
                ((Number) row.get("allow_buyout")).intValue() != 0
            ));
        }
        
        return companies;
    }
    
    /**
     * Deposits money into a company.
     */
    public void deposit(String companyId, String playerUuid, double amount) throws SQLException {
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }
        
        // Withdraw from player wallet
        walletService.removeBalance(playerUuid, amount);
        
        // Add to company balance
        database.execute(
            "UPDATE companies SET balance = balance + ? WHERE id = ?",
            amount, companyId
        );
        
        // Record transaction
        String txId = UUID.randomUUID().toString();
        database.execute(
            "INSERT INTO company_tx (id, company_id, player_uuid, type, amount, ts) VALUES (?, ?, ?, ?, ?, ?)",
            txId, companyId, playerUuid, "DEPOSIT", amount, System.currentTimeMillis()
        );
        
        logger.fine("Player " + playerUuid + " deposited $" + amount + " to company " + companyId);
    }
    
    /**
     * Withdraws money from a company.
     */
    public void withdraw(String companyId, String playerUuid, double amount) throws SQLException {
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive");
        }
        
        // Check company balance
        Optional<Company> companyOpt = getCompanyById(companyId);
        if (companyOpt.isEmpty()) {
            throw new IllegalArgumentException("Company not found");
        }
        
        Company company = companyOpt.get();
        if (company.getBalance() < amount) {
            throw new IllegalArgumentException("Insufficient company funds");
        }
        
        // Check permissions
        if (!canPlayerWithdraw(companyId, playerUuid)) {
            throw new IllegalArgumentException("Player does not have permission to withdraw");
        }
        
        // Deduct from company balance
        database.execute(
            "UPDATE companies SET balance = balance - ? WHERE id = ?",
            amount, companyId
        );
        
        // Add to player wallet
        walletService.addBalance(playerUuid, amount);
        
        // Record transaction
        String txId = UUID.randomUUID().toString();
        database.execute(
            "INSERT INTO company_tx (id, company_id, player_uuid, type, amount, ts) VALUES (?, ?, ?, ?, ?, ?)",
            txId, companyId, playerUuid, "WITHDRAW", amount, System.currentTimeMillis()
        );
        
        logger.fine("Player " + playerUuid + " withdrew $" + amount + " from company " + companyId);
    }
    
    /**
     * Adds funds directly to a company balance without wallet interaction.
     * This is used for external integrations like ChestShop.
     */
    public void addDirectToBalance(String companyId, double amount, String reason) throws SQLException {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        
        // Add to company balance
        database.execute(
            "UPDATE companies SET balance = balance + ? WHERE id = ?",
            amount, companyId
        );
        
        // Record transaction with system UUID
        String txId = UUID.randomUUID().toString();
        database.execute(
            "INSERT INTO company_tx (id, company_id, player_uuid, type, amount, ts) VALUES (?, ?, ?, ?, ?, ?)",
            txId, companyId, "00000000-0000-0000-0000-000000000000", "DEPOSIT", amount, System.currentTimeMillis()
        );
        
        logger.fine("Added $" + amount + " directly to company " + companyId + " - Reason: " + reason);
    }
    
    /**
     * Removes funds directly from a company balance without wallet interaction.
     * This is used for external integrations like ChestShop.
     */
    public void removeDirectFromBalance(String companyId, double amount, String reason) throws SQLException {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        
        // Check company balance
        Optional<Company> companyOpt = getCompanyById(companyId);
        if (companyOpt.isEmpty()) {
            throw new IllegalArgumentException("Company not found");
        }
        
        Company company = companyOpt.get();
        if (company.getBalance() < amount) {
            throw new IllegalArgumentException("Insufficient company funds");
        }
        
        // Deduct from company balance
        database.execute(
            "UPDATE companies SET balance = balance - ? WHERE id = ?",
            amount, companyId
        );
        
        // Record transaction with system UUID
        String txId = UUID.randomUUID().toString();
        database.execute(
            "INSERT INTO company_tx (id, company_id, player_uuid, type, amount, ts) VALUES (?, ?, ?, ?, ?, ?)",
            txId, companyId, "00000000-0000-0000-0000-000000000000", "WITHDRAW", amount, System.currentTimeMillis()
        );
        
        logger.fine("Removed $" + amount + " directly from company " + companyId + " - Reason: " + reason);
    }
    
    /**
     * Checks if a player can withdraw from a company.
     */
    public boolean canPlayerWithdraw(String companyId, String playerUuid) throws SQLException {
        List<Map<String, Object>> results = database.query(
            "SELECT cj.can_withdraw FROM company_employees ce " +
            "INNER JOIN company_jobs cj ON ce.job_id = cj.id " +
            "WHERE ce.company_id = ? AND ce.player_uuid = ?",
            companyId, playerUuid
        );
        
        if (results.isEmpty()) {
            return false;
        }
        
        int canWithdraw = ((Number) results.get(0).get("can_withdraw")).intValue();
        return canWithdraw != 0;
    }
    
    /**
     * Gets the job of a player in a company.
     */
    public Optional<CompanyJob> getPlayerJob(String companyId, String playerUuid) throws SQLException {
        List<Map<String, Object>> results = database.query(
            "SELECT cj.id, cj.company_id, cj.title, cj.can_invite, cj.can_create_titles, cj.can_withdraw, cj.can_manage_company, cj.can_manage_chestshop " +
            "FROM company_employees ce " +
            "INNER JOIN company_jobs cj ON ce.job_id = cj.id " +
            "WHERE ce.company_id = ? AND ce.player_uuid = ?",
            companyId, playerUuid
        );
        
        if (results.isEmpty()) {
            return Optional.empty();
        }
        
        Map<String, Object> row = results.get(0);
        return Optional.of(new CompanyJob(
            (String) row.get("id"),
            (String) row.get("company_id"),
            (String) row.get("title"),
            ((Number) row.get("can_invite")).intValue() != 0,
            ((Number) row.get("can_create_titles")).intValue() != 0,
            ((Number) row.get("can_withdraw")).intValue() != 0,
            ((Number) row.get("can_manage_company")).intValue() != 0,
            ((Number) row.get("can_manage_chestshop")).intValue() != 0
        ));
    }
    
    /**
     * Creates a new job title in a company.
     */
    public CompanyJob createJobTitle(String companyId, String creatorUuid, String title, 
                                     boolean canInvite, boolean canCreateTitles, 
                                     boolean canWithdraw, boolean canManageCompany, 
                                     boolean canManageChestShop) throws SQLException {
        // Check if creator has permission
        Optional<CompanyJob> creatorJob = getPlayerJob(companyId, creatorUuid);
        if (creatorJob.isEmpty() || !creatorJob.get().canCreateTitles()) {
            throw new IllegalArgumentException("Player does not have permission to create job titles");
        }
        
        // Check if title already exists
        List<Map<String, Object>> existing = database.query(
            "SELECT id FROM company_jobs WHERE company_id = ? AND title = ?",
            companyId, title
        );
        if (!existing.isEmpty()) {
            throw new IllegalArgumentException("Job title already exists");
        }
        
        // Create job
        String jobId = UUID.randomUUID().toString();
        database.execute(
            "INSERT INTO company_jobs (id, company_id, title, can_invite, can_create_titles, can_withdraw, can_manage_company, can_manage_chestshop) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            jobId, companyId, title,
            canInvite ? 1 : 0,
            canCreateTitles ? 1 : 0,
            canWithdraw ? 1 : 0,
            canManageCompany ? 1 : 0,
            canManageChestShop ? 1 : 0
        );
        
        logger.info("Created job title '" + title + "' in company " + companyId);
        
        return new CompanyJob(jobId, companyId, title, canInvite, canCreateTitles, canWithdraw, canManageCompany, canManageChestShop);
    }
    
    /**
     * Gets all job titles for a company.
     */
    public List<CompanyJob> getCompanyJobs(String companyId) throws SQLException {
        List<Map<String, Object>> results = database.query(
            "SELECT id, company_id, title, can_invite, can_create_titles, can_withdraw, can_manage_company, can_manage_chestshop " +
            "FROM company_jobs WHERE company_id = ? ORDER BY title",
            companyId
        );
        
        List<CompanyJob> jobs = new ArrayList<>();
        for (Map<String, Object> row : results) {
            jobs.add(new CompanyJob(
                (String) row.get("id"),
                (String) row.get("company_id"),
                (String) row.get("title"),
                ((Number) row.get("can_invite")).intValue() != 0,
                ((Number) row.get("can_create_titles")).intValue() != 0,
                ((Number) row.get("can_withdraw")).intValue() != 0,
                ((Number) row.get("can_manage_company")).intValue() != 0,
                ((Number) row.get("can_manage_chestshop")).intValue() != 0
            ));
        }
        
        return jobs;
    }
    
    /**
     * Gets a job by title.
     */
    public Optional<CompanyJob> getJobByTitle(String companyId, String title) throws SQLException {
        List<Map<String, Object>> results = database.query(
            "SELECT id, company_id, title, can_invite, can_create_titles, can_withdraw, can_manage_company, can_manage_chestshop " +
            "FROM company_jobs WHERE company_id = ? AND title = ?",
            companyId, title
        );
        
        if (results.isEmpty()) {
            return Optional.empty();
        }
        
        Map<String, Object> row = results.get(0);
        return Optional.of(new CompanyJob(
            (String) row.get("id"),
            (String) row.get("company_id"),
            (String) row.get("title"),
            ((Number) row.get("can_invite")).intValue() != 0,
            ((Number) row.get("can_create_titles")).intValue() != 0,
            ((Number) row.get("can_withdraw")).intValue() != 0,
            ((Number) row.get("can_manage_company")).intValue() != 0,
            ((Number) row.get("can_manage_chestshop")).intValue() != 0
        ));
    }
    
    /**
     * Updates an existing job title's permissions.
     */
    public CompanyJob updateJobTitle(String companyId, String actorUuid, String title,
                                     boolean canInvite, boolean canCreateTitles,
                                     boolean canWithdraw, boolean canManageCompany, 
                                     boolean canManageChestShop) throws SQLException {
        // Check if actor has permission
        Optional<CompanyJob> actorJob = getPlayerJob(companyId, actorUuid);
        if (actorJob.isEmpty() || !actorJob.get().canCreateTitles()) {
            throw new IllegalArgumentException("Player does not have permission to edit job titles");
        }
        
        // Get the job to update
        Optional<CompanyJob> jobOpt = getJobByTitle(companyId, title);
        if (jobOpt.isEmpty()) {
            throw new IllegalArgumentException("Job title does not exist");
        }
        
        CompanyJob job = jobOpt.get();
        
        // Update job permissions
        database.execute(
            "UPDATE company_jobs SET can_invite = ?, can_create_titles = ?, can_withdraw = ?, can_manage_company = ?, can_manage_chestshop = ? " +
            "WHERE id = ?",
            canInvite ? 1 : 0,
            canCreateTitles ? 1 : 0,
            canWithdraw ? 1 : 0,
            canManageCompany ? 1 : 0,
            canManageChestShop ? 1 : 0,
            job.getId()
        );
        
        logger.info("Updated job title '" + title + "' in company " + companyId);
        
        return new CompanyJob(job.getId(), companyId, title, canInvite, canCreateTitles, canWithdraw, canManageCompany, canManageChestShop);
    }
    
    /**
     * Assigns a job title to a player.
     */
    public void assignJobTitle(String companyId, String actorUuid, String targetUuid, String title) throws SQLException {
        // Check if actor has permission
        Optional<CompanyJob> actorJob = getPlayerJob(companyId, actorUuid);
        if (actorJob.isEmpty() || !actorJob.get().canManageCompany()) {
            throw new IllegalArgumentException("Player does not have permission to assign job titles");
        }
        
        // Prevent CEO from demoting themselves
        if (actorUuid.equals(targetUuid)) {
            Optional<CompanyJob> currentJob = getPlayerJob(companyId, targetUuid);
            if (currentJob.isPresent() && "CEO".equalsIgnoreCase(currentJob.get().getTitle())) {
                Optional<CompanyJob> newJob = getJobByTitle(companyId, title);
                if (newJob.isPresent() && !"CEO".equalsIgnoreCase(newJob.get().getTitle())) {
                    throw new IllegalArgumentException("CEO cannot demote themselves. Transfer ownership first.");
                }
            }
        }
        
        // Get the job
        Optional<CompanyJob> job = getJobByTitle(companyId, title);
        if (job.isEmpty()) {
            throw new IllegalArgumentException("Job title does not exist");
        }
        
        // Update employee's job
        database.execute(
            "UPDATE company_employees SET job_id = ? WHERE company_id = ? AND player_uuid = ?",
            job.get().getId(), companyId, targetUuid
        );
        
        logger.info("Assigned job '" + title + "' to player " + targetUuid + " in company " + companyId);
    }
    
    /**
     * Gets all employees of a company.
     */
    public List<Map<String, Object>> getCompanyEmployees(String companyId) throws SQLException {
        return database.query(
            "SELECT ce.player_uuid, ce.joined_at, cj.title, cj.id as job_id " +
            "FROM company_employees ce " +
            "INNER JOIN company_jobs cj ON ce.job_id = cj.id " +
            "WHERE ce.company_id = ? " +
            "ORDER BY ce.joined_at",
            companyId
        );
    }
    
    /**
     * Gets all companies that are on the market (have shares enabled).
     */
    public List<Company> getCompaniesOnMarket() throws SQLException {
        List<Map<String, Object>> results = database.query(
            "SELECT id, name, type, owner_uuid, balance, created_at, symbol, on_market, market_percentage, allow_buyout " +
            "FROM companies " +
            "WHERE on_market = 1 AND symbol IS NOT NULL " +
            "ORDER BY name",
            new Object[0]
        );
        
        List<Company> companies = new ArrayList<>();
        for (Map<String, Object> row : results) {
            companies.add(new Company(
                (String) row.get("id"),
                (String) row.get("name"),
                (String) row.get("type"),
                (String) row.get("owner_uuid"),
                ((Number) row.get("balance")).doubleValue(),
                ((Number) row.get("created_at")).longValue(),
                (String) row.get("symbol"),
                ((Number) row.get("on_market")).intValue() != 0,
                ((Number) row.get("market_percentage")).doubleValue(),
                ((Number) row.get("allow_buyout")).intValue() != 0
            ));
        }
        
        return companies;
    }
}
