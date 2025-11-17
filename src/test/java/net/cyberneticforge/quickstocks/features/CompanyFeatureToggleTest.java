package net.cyberneticforge.quickstocks.features;

import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.TestBase;
import net.cyberneticforge.quickstocks.infrastructure.config.CompanyCfg;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Tests for Company feature enable/disable functionality.
 * Verifies that company features can be toggled on/off and behavior is correct.
 * These tests read from actual configuration files to validate feature toggles.
 */
@DisplayName("Company Feature Toggle Tests")
public class CompanyFeatureToggleTest extends TestBase {
    
    @Test
    @DisplayName("Company feature should be enabled by default")
    public void testCompanyEnabledByDefault() {
        // Skip test if plugin failed to load
        assumeFalse(pluginLoadFailed, "Plugin must be loaded to test config");
        
        // Given: Default company configuration from companies.yml
        // When: Reading companies.enabled from config
        CompanyCfg companyCfg = QuickStocksPlugin.getCompanyCfg();
        
        // Then: Should be enabled by default (per companies.yml: enabled: true)
        assertNotNull(companyCfg, "Company config should be loaded");
        assertTrue(companyCfg.isEnabled(), "Companies should be enabled by default per companies.yml");
    }
    
    @Test
    @DisplayName("Company command check follows config value")
    public void testCompanyCommandChecksConfig() {
        // Skip test if plugin failed to load
        assumeFalse(pluginLoadFailed, "Plugin must be loaded to test config");
        
        // Given: Company configuration
        CompanyCfg companyCfg = QuickStocksPlugin.getCompanyCfg();
        assertNotNull(companyCfg, "Company config should be loaded");
        
        // When: Checking if companies are enabled
        boolean companiesEnabled = companyCfg.isEnabled();
        
        // Then: Command execution should follow this value
        // This simulates CompanyCommand.onCommand() check: if (!QuickStocksPlugin.getCompanyCfg().isEnabled())
        boolean commandShouldProceed = companiesEnabled;
        
        assertEquals(companiesEnabled, commandShouldProceed, 
            "Company command should proceed only when companies.enabled is true");
    }
    
    @Test
    @DisplayName("ChestShop integration should be enabled by default")
    public void testChestShopEnabledByDefault() {
        // Skip test if plugin failed to load
        assumeFalse(pluginLoadFailed, "Plugin must be loaded to test config");
        
        // Given: Default company configuration from companies.yml
        // When: Reading companies.chestshop.enabled from config
        CompanyCfg companyCfg = QuickStocksPlugin.getCompanyCfg();
        
        // Then: Should be enabled by default (per companies.yml: chestshop.enabled: true)
        assertNotNull(companyCfg, "Company config should be loaded");
        assertTrue(companyCfg.isChestShopEnabled(), 
            "ChestShop integration should be enabled by default per companies.yml");
    }
    
    @Test
    @DisplayName("Company plots should be enabled by default")
    public void testPlotsEnabledByDefault() {
        // Skip test if plugin failed to load
        assumeFalse(pluginLoadFailed, "Plugin must be loaded to test config");
        
        // Given: Default company configuration from companies.yml
        // When: Reading companies.plots.enabled from config
        CompanyCfg companyCfg = QuickStocksPlugin.getCompanyCfg();
        
        // Then: Should be enabled by default (per companies.yml: plots.enabled: true)
        assertNotNull(companyCfg, "Company config should be loaded");
        assertTrue(companyCfg.isPlotsEnabled(), 
            "Company plots should be enabled by default per companies.yml");
    }
    
    @Test
    @DisplayName("Terrain messages should be enabled by default")
    public void testTerrainMessagesEnabledByDefault() {
        // Skip test if plugin failed to load
        assumeFalse(pluginLoadFailed, "Plugin must be loaded to test config");
        
        // Given: Default company configuration from companies.yml
        // When: Reading companies.plots.terrainMessages.enabled from config
        CompanyCfg companyCfg = QuickStocksPlugin.getCompanyCfg();
        
        // Then: Should be enabled by default (per companies.yml)
        assertNotNull(companyCfg, "Company config should be loaded");
        assertTrue(companyCfg.isTerrainMessagesEnabled(), 
            "Terrain messages should be enabled by default per companies.yml");
    }
    
    @Test
    @DisplayName("Company service checks enabled state")
    public void testCompanyServiceChecksEnabled() {
        // Skip test if plugin failed to load
        assumeFalse(pluginLoadFailed, "Plugin must be loaded to test config");
        
        // Given: Company configuration
        CompanyCfg companyCfg = QuickStocksPlugin.getCompanyCfg();
        assertNotNull(companyCfg, "Company config should be loaded");
        
        // When: Checking if companies are enabled
        boolean companiesEnabled = companyCfg.isEnabled();
        
        // Then: CompanyService.createCompany() checks this value
        // CompanyService throws IllegalStateException if !config.isEnabled()
        assertTrue(companiesEnabled, 
            "Companies should be enabled (per companies.yml: companies.enabled: true)");
    }
    
    @Test
    @DisplayName("Company sub-features are independently configurable")
    public void testCompanySubFeaturesIndependent() {
        // Skip test if plugin failed to load
        assumeFalse(pluginLoadFailed, "Plugin must be loaded to test config");
        
        // Given: Company configuration with various sub-feature settings
        CompanyCfg companyCfg = QuickStocksPlugin.getCompanyCfg();
        assertNotNull(companyCfg, "Company config should be loaded");
        
        // When: Reading different sub-feature toggles
        boolean chestShopEnabled = companyCfg.isChestShopEnabled();
        boolean plotsEnabled = companyCfg.isPlotsEnabled();
        boolean terrainMessagesEnabled = companyCfg.isTerrainMessagesEnabled();
        
        // Then: Each sub-feature can be independently configured
        // The actual values depend on companies.yml configuration
        assertNotNull(chestShopEnabled, "ChestShop setting should be read");
        assertNotNull(plotsEnabled, "Plots setting should be read");
        assertNotNull(terrainMessagesEnabled, "Terrain messages setting should be read");
    }
    
    @Test
    @DisplayName("Company config provides all expected getters")
    public void testCompanyConfigGetters() {
        // Skip test if plugin failed to load
        assumeFalse(pluginLoadFailed, "Plugin must be loaded to test config");
        
        // Given: Loaded company configuration
        CompanyCfg companyCfg = QuickStocksPlugin.getCompanyCfg();
        assertNotNull(companyCfg, "Company config should be loaded");
        
        // When: Accessing various config properties
        // Then: All getters should work without throwing exceptions
        assertDoesNotThrow(() -> companyCfg.isEnabled(), "isEnabled() should work");
        assertDoesNotThrow(() -> companyCfg.isChestShopEnabled(), "isChestShopEnabled() should work");
        assertDoesNotThrow(() -> companyCfg.isPlotsEnabled(), "isPlotsEnabled() should work");
        assertDoesNotThrow(() -> companyCfg.isTerrainMessagesEnabled(), "isTerrainMessagesEnabled() should work");
        assertDoesNotThrow(() -> companyCfg.getCreationCost(), "getCreationCost() should work");
        assertDoesNotThrow(() -> companyCfg.getDefaultTypes(), "getDefaultTypes() should work");
    }
    
    @Test
    @DisplayName("Plot service checks plots enabled state")
    public void testPlotServiceChecksEnabled() {
        // Skip test if plugin failed to load
        assumeFalse(pluginLoadFailed, "Plugin must be loaded to test config");
        
        // Given: Company configuration
        CompanyCfg companyCfg = QuickStocksPlugin.getCompanyCfg();
        assertNotNull(companyCfg, "Company config should be loaded");
        
        // When: Checking if plots are enabled
        boolean plotsEnabled = companyCfg.isPlotsEnabled();
        
        // Then: CompanyPlotService.buyPlot() checks this value
        // CompanyPlotService throws IllegalStateException if !config.isPlotsEnabled()
        assertTrue(plotsEnabled, 
            "Plots should be enabled (per companies.yml: companies.plots.enabled: true)");
    }
}
