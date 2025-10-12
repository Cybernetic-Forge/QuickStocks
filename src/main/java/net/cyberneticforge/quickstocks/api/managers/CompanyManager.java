package net.cyberneticforge.quickstocks.api.managers;

import net.cyberneticforge.quickstocks.core.model.Company;
import net.cyberneticforge.quickstocks.core.model.CompanyJob;
import net.cyberneticforge.quickstocks.core.services.CompanyMarketService;
import net.cyberneticforge.quickstocks.core.services.CompanyService;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * API Manager for company operations.
 * Provides a high-level interface for external plugins to interact with the company system.
 */
@SuppressWarnings("unused")
public record CompanyManager(CompanyService companyService, CompanyMarketService companyMarketService) {

    /**
     * Creates a new company.
     *
     * @param playerUuid UUID of the player creating the company
     * @param name       Name of the company
     * @param type       Type of company (PRIVATE, PUBLIC, DAO)
     * @return The created company
     * @throws SQLException if database error occurs
     */
    public Company createCompany(String playerUuid, String name, String type) throws SQLException {
        return companyService.createCompany(playerUuid, name, type);
    }

    /**
     * Gets a company by ID.
     *
     * @param companyId The company ID
     * @return Optional containing the company if found
     * @throws SQLException if database error occurs
     */
    public Optional<Company> getCompanyById(String companyId) throws SQLException {
        return companyService.getCompanyById(companyId);
    }

    /**
     * Gets a company by name.
     *
     * @param name The company name
     * @return Optional containing the company if found
     * @throws SQLException if database error occurs
     */
    public Optional<Company> getCompanyByName(String name) throws SQLException {
        return companyService.getCompanyByName(name);
    }

    /**
     * Gets all companies.
     *
     * @return List of all companies
     * @throws SQLException if database error occurs
     */
    public List<Company> getAllCompanies() throws SQLException {
        return companyService.getAllCompanies();
    }

    /**
     * Deposits funds into a company.
     *
     * @param companyId  The company ID
     * @param playerUuid UUID of the player depositing
     * @param amount     Amount to deposit
     * @throws SQLException if database error occurs
     */
    public void deposit(String companyId, String playerUuid, double amount) throws SQLException {
        companyService.deposit(companyId, playerUuid, amount);
    }

    /**
     * Withdraws funds from a company.
     *
     * @param companyId  The company ID
     * @param playerUuid UUID of the player withdrawing
     * @param amount     Amount to withdraw
     * @throws SQLException if database error occurs
     */
    public void withdraw(String companyId, String playerUuid, double amount) throws SQLException {
        companyService.withdraw(companyId, playerUuid, amount);
    }

    /**
     * Gets all employees of a company.
     *
     * @param companyId The company ID
     * @return List of employee data
     * @throws SQLException if database error occurs
     */
    public List<Map<String, Object>> getEmployees(String companyId) throws SQLException {
        return companyService.getCompanyEmployees(companyId);
    }

    /**
     * Gets all job titles for a company.
     *
     * @param companyId The company ID
     * @return List of job titles
     * @throws SQLException if database error occurs
     */
    public List<CompanyJob> getJobs(String companyId) throws SQLException {
        return companyService.getCompanyJobs(companyId);
    }

    /**
     * Creates a new job title in a company.
     *
     * @param companyId        The company ID
     * @param creatorUuid      UUID of the player creating the job
     * @param title            Job title
     * @param canInvite        Can invite permission
     * @param canCreateTitles  Can create titles permission
     * @param canWithdraw      Can withdraw permission
     * @param canManageCompany Can manage company permission
     * @return The created job
     * @throws SQLException if database error occurs
     */
    public CompanyJob createJob(String companyId, String creatorUuid, String title,
                                boolean canInvite, boolean canCreateTitles,
                                boolean canWithdraw, boolean canManageCompany, boolean canManageSalaries, boolean canManageChestShops) throws SQLException {
        return companyService.createJobTitle(companyId, creatorUuid, title,
                canInvite, canCreateTitles, canWithdraw, canManageCompany, canManageSalaries, canManageChestShops);
    }

    /**
     * Enables the market for a company (IPO).
     *
     * @param companyId The company ID
     * @param actorUuid UUID of the player initiating the IPO
     * @throws SQLException if database error occurs
     */
    public void enableMarket(String companyId, String actorUuid) throws SQLException {
        companyMarketService.enableMarket(companyId, actorUuid);
    }

    /**
     * Disables the market for a company (delist).
     *
     * @param companyId The company ID
     * @param actorUuid UUID of the player delisting
     * @throws SQLException if database error occurs
     */
    public void disableMarket(String companyId, String actorUuid) throws SQLException {
        companyMarketService.disableMarket(companyId, actorUuid);
    }

    /**
     * Buys shares of a company.
     *
     * @param companyId The company ID
     * @param buyerUuid UUID of the buyer
     * @param quantity  Number of shares to buy
     * @throws SQLException if database error occurs
     */
    public void buyShares(String companyId, String buyerUuid, int quantity) throws SQLException {
        companyMarketService.buyShares(companyId, buyerUuid, quantity);
    }

    /**
     * Sells shares of a company.
     *
     * @param companyId  The company ID
     * @param sellerUuid UUID of the seller
     * @param quantity   Number of shares to sell
     * @throws SQLException if database error occurs
     */
    public void sellShares(String companyId, String sellerUuid, int quantity) throws SQLException {
        companyMarketService.sellShares(companyId, sellerUuid, quantity);
    }

    /**
     * Gets all shareholders of a company.
     *
     * @param companyId The company ID
     * @return List of shareholder data
     * @throws SQLException if database error occurs
     */
    public List<Map<String, Object>> getShareholders(String companyId) throws SQLException {
        return companyMarketService.getShareholders(companyId);
    }
}
