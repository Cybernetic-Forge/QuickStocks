package net.cyberneticforge.quickstocks.infrastructure.hooks;

import com.Acrobot.Breeze.Utils.NameUtil;
import com.Acrobot.ChestShop.UUIDs.NameManager;
import net.cyberneticforge.quickstocks.core.model.Company;
import net.cyberneticforge.quickstocks.core.services.CompanyService;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides ChestShop with company account information.
 * This allows ChestShop to recognize company names as valid shop owners.
 */
public class ChestShopAccountProvider {
    
    private static final Logger logger = Logger.getLogger(ChestShopAccountProvider.class.getName());
    
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
            // Register a custom name getter that checks for companies
            NameManager.registerAccountProvider(name -> {
                try {
                    Optional<Company> companyOpt = companyService.getCompanyByName(name);
                    if (companyOpt.isPresent()) {
                        // Return a deterministic UUID for this company
                        return getCompanyUUID(companyOpt.get().getId());
                    }
                } catch (SQLException e) {
                    logger.log(Level.WARNING, "Error checking company name: " + name, e);
                }
                return null; // Not a company, let ChestShop check for player
            });
            
            logger.info("Registered company account provider with ChestShop");
        } catch (Exception e) {
            logger.log(Level.WARNING, "Could not register account provider with ChestShop - using fallback method", e);
        }
    }
    
    /**
     * Gets a deterministic UUID for a company based on its ID.
     * This ensures the same company always gets the same UUID.
     */
    private UUID getCompanyUUID(String companyId) {
        // Create a deterministic UUID from the company ID
        // This ensures the same company always has the same UUID
        return UUID.nameUUIDFromBytes((COMPANY_NAMESPACE.toString() + companyId).getBytes());
    }
    
    /**
     * Checks if a name is a company.
     */
    public boolean isCompany(String name) {
        try {
            return companyService.getCompanyByName(name).isPresent();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Error checking if name is company: " + name, e);
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
            logger.log(Level.WARNING, "Error getting company UUID: " + name, e);
        }
        return null;
    }
}
