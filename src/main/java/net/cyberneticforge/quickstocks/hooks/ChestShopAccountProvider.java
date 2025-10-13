package net.cyberneticforge.quickstocks.hooks;

import com.Acrobot.ChestShop.UUIDs.NameManager;
import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.core.model.Company;
import net.cyberneticforge.quickstocks.core.services.CompanyService;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import net.cyberneticforge.quickstocks.infrastructure.logging.PluginLogger;

/**
 * Provides ChestShop with company account information.
 * This allows ChestShop to recognize company names as valid shop owners.
 */
@SuppressWarnings("unused")
public class ChestShopAccountProvider {
    
    private static final PluginLogger logger = QuickStocksPlugin.getPluginLogger();
    
    // Special UUID namespace for companies to avoid conflicts with player UUIDs
    private static final UUID COMPANY_NAMESPACE = UUID.fromString("12345678-1234-5678-1234-567812345678");
    
    private final CompanyService companyService;
    
    public ChestShopAccountProvider(CompanyService companyService) {
        this.companyService = companyService;
    }
    
    /**
     * Registers company name handling with ChestShop's NameManager.
     * This allows ChestShop to validate company names as valid shop owners.
     */
    public void registerWithChestShop() {
        try {
            // Register all existing companies
            for(Company company : companyService.getAllCompanies()) {
                registerCompany(company);
            }
            logger.info("Registered " + companyService.getAllCompanies().size() + " companies with ChestShop");
        } catch (Exception e) {
            logger.warning("Could not register companies with ChestShop", e);
        }
    }
    
    /**
     * Registers a single company with ChestShop.
     * This should be called when a shop is created with a company name.
     */
    public void registerCompany(Company company) {
        try {
            UUID companyUuid = getCompanyUUID(company.getId());
            NameManager.getOrCreateAccount(companyUuid, company.getName());
            logger.debug("Registered company '" + company.getName() + "' (UUID: " + companyUuid + ") with ChestShop");
        } catch (Exception e) {
            logger.warning("Failed to register company '" + company.getName() + "' with ChestShop", e);
        }
    }
    
    /**
     * Registers a company by name with ChestShop if it exists.
     * Returns true if successful, false otherwise.
     */
    public boolean registerCompanyByName(String companyName) {
        try {
            Optional<Company> companyOpt = companyService.getCompanyByName(companyName);
            if (companyOpt.isPresent()) {
                registerCompany(companyOpt.get());
                return true;
            }
        } catch (SQLException e) {
            logger.warning("Error registering company by name: " + companyName, e);
        }
        return false;
    }
    
    /**
     * Gets a deterministic UUID for a company based on its ID.
     * This ensures the same company always gets the same UUID.
     */
    private UUID getCompanyUUID(String companyId) {
        // Create a deterministic UUID from the company ID
        // This ensures the same company always has the same UUID
        return UUID.nameUUIDFromBytes((COMPANY_NAMESPACE + companyId).getBytes());
    }
    
    /**
     * Checks if a name is a company.
     */
    public boolean isCompany(String name) {
        try {
            return companyService.getCompanyByName(name).isPresent();
        } catch (SQLException e) {
            logger.warning("Error checking if name is company: " + name, e);
            return false;
        }
    }
    
    /**
     * Gets the company UUID for a company name.
     * Returns null if the name is not a company.
     */
    public UUID getCompanyUUIDByName(String name) {
        try {
            Optional<Company> companyOpt = companyService.getCompanyByName(name);
            if (companyOpt.isPresent()) {
                return getCompanyUUID(companyOpt.get().getId());
            }
        } catch (SQLException e) {
            logger.warning("Error getting company UUID: " + name, e);
        }
        return null;
    }
}
