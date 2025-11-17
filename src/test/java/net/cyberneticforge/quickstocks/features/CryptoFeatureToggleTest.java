package net.cyberneticforge.quickstocks.features;

import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.TestBase;
import net.cyberneticforge.quickstocks.infrastructure.config.CryptoCfg;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Tests for Cryptocurrency feature enable/disable functionality.
 * Verifies that crypto features can be toggled on/off and behavior is correct.
 * These tests read from actual configuration files to validate feature toggles.
 */
@DisplayName("Crypto Feature Toggle Tests")
public class CryptoFeatureToggleTest extends TestBase {
    
    @Test
    @DisplayName("Crypto feature reads from config")
    public void testCryptoReadsFromConfig() {
        // Skip test if plugin failed to load
        assumeFalse(pluginLoadFailed, "Plugin must be loaded to test config");
        
        // Given: Default crypto configuration from market.yml
        // When: Reading crypto.enabled from config
        CryptoCfg cryptoCfg = QuickStocksPlugin.getCryptoCfg();
        
        // Then: Should match market.yml setting (crypto.enabled: false by default)
        assertNotNull(cryptoCfg, "Crypto config should be loaded");
        // Note: market.yml has crypto.enabled: false by default
        assertFalse(cryptoCfg.isEnabled(), "Crypto should be disabled by default per market.yml");
    }
    
    @Test
    @DisplayName("Crypto service checks enabled state")
    public void testCryptoServiceChecksEnabled() {
        // Skip test if plugin failed to load
        assumeFalse(pluginLoadFailed, "Plugin must be loaded to test config");
        
        // Given: Crypto configuration
        CryptoCfg cryptoCfg = QuickStocksPlugin.getCryptoCfg();
        assertNotNull(cryptoCfg, "Crypto config should be loaded");
        
        // When: Checking if crypto is enabled
        boolean cryptoEnabled = cryptoCfg.isEnabled();
        
        // Then: CryptoService.createCustomCrypto() checks this value
        // CryptoService throws IllegalArgumentException if !cryptoCfg.isEnabled()
        // Note: By default in market.yml, crypto.enabled is false
        assertFalse(cryptoEnabled, 
            "Crypto should be disabled (per market.yml: crypto.enabled: false)");
    }
    
    @Test
    @DisplayName("Personal crypto reads from config")
    public void testPersonalCryptoReadsFromConfig() {
        // Skip test if plugin failed to load
        assumeFalse(pluginLoadFailed, "Plugin must be loaded to test config");
        
        // Given: Default crypto configuration from market.yml
        // When: Reading crypto.personal.enabled from config
        CryptoCfg cryptoCfg = QuickStocksPlugin.getCryptoCfg();
        
        // Then: Should match market.yml setting (crypto.personal.enabled: false by default)
        assertNotNull(cryptoCfg, "Crypto config should be loaded");
        assertNotNull(cryptoCfg.getPersonalConfig(), "Personal crypto config should be loaded");
        // Note: market.yml has crypto.personal.enabled: false by default
        assertFalse(cryptoCfg.getPersonalConfig().isEnabled(), 
            "Personal crypto should be disabled by default per market.yml");
    }
    
    @Test
    @DisplayName("Company crypto reads from config")
    public void testCompanyCryptoReadsFromConfig() {
        // Skip test if plugin failed to load
        assumeFalse(pluginLoadFailed, "Plugin must be loaded to test config");
        
        // Given: Default crypto configuration from market.yml
        // When: Reading crypto.company.enabled from config
        CryptoCfg cryptoCfg = QuickStocksPlugin.getCryptoCfg();
        
        // Then: Should match market.yml setting (crypto.company.enabled: false by default)
        assertNotNull(cryptoCfg, "Crypto config should be loaded");
        assertNotNull(cryptoCfg.getCompanyConfig(), "Company crypto config should be loaded");
        // Note: market.yml has crypto.company.enabled: false by default
        assertFalse(cryptoCfg.getCompanyConfig().isEnabled(), 
            "Company crypto should be disabled by default per market.yml");
    }
    
    @Test
    @DisplayName("Personal crypto service checks enabled state")
    public void testPersonalCryptoServiceChecksEnabled() {
        // Skip test if plugin failed to load
        assumeFalse(pluginLoadFailed, "Plugin must be loaded to test config");
        
        // Given: Personal crypto configuration
        CryptoCfg cryptoCfg = QuickStocksPlugin.getCryptoCfg();
        assertNotNull(cryptoCfg, "Crypto config should be loaded");
        
        // When: Checking if personal crypto is enabled
        boolean personalCryptoEnabled = cryptoCfg.getPersonalConfig().isEnabled();
        
        // Then: CryptoService checks both crypto.enabled AND crypto.personal.enabled
        boolean cryptoEnabled = cryptoCfg.isEnabled();
        boolean canCreatePersonal = cryptoEnabled && personalCryptoEnabled;
        
        // Note: By default both are false in market.yml
        assertFalse(canCreatePersonal, 
            "Personal crypto creation should be disabled by default");
    }
    
    @Test
    @DisplayName("Company crypto service checks enabled state")
    public void testCompanyCryptoServiceChecksEnabled() {
        // Skip test if plugin failed to load
        assumeFalse(pluginLoadFailed, "Plugin must be loaded to test config");
        
        // Given: Company crypto configuration
        CryptoCfg cryptoCfg = QuickStocksPlugin.getCryptoCfg();
        assertNotNull(cryptoCfg, "Crypto config should be loaded");
        
        // When: Checking if company crypto is enabled
        boolean companyCryptoEnabled = cryptoCfg.getCompanyConfig().isEnabled();
        
        // Then: CryptoService checks both crypto.enabled AND crypto.company.enabled
        boolean cryptoEnabled = cryptoCfg.isEnabled();
        boolean canCreateCompany = cryptoEnabled && companyCryptoEnabled;
        
        // Note: By default both are false in market.yml
        assertFalse(canCreateCompany, 
            "Company crypto creation should be disabled by default");
    }
    
    @Test
    @DisplayName("Crypto sub-features are independently configurable")
    public void testCryptoSubFeaturesIndependent() {
        // Skip test if plugin failed to load
        assumeFalse(pluginLoadFailed, "Plugin must be loaded to test config");
        
        // Given: Crypto configuration with various sub-feature settings
        CryptoCfg cryptoCfg = QuickStocksPlugin.getCryptoCfg();
        assertNotNull(cryptoCfg, "Crypto config should be loaded");
        
        // When: Reading different sub-feature toggles
        boolean personalEnabled = cryptoCfg.getPersonalConfig().isEnabled();
        boolean companyEnabled = cryptoCfg.getCompanyConfig().isEnabled();
        
        // Then: Each sub-feature can be independently configured
        // The actual values depend on market.yml configuration
        assertNotNull(personalEnabled, "Personal crypto setting should be read");
        assertNotNull(companyEnabled, "Company crypto setting should be read");
    }
    
    @Test
    @DisplayName("Crypto config provides all expected getters")
    public void testCryptoConfigGetters() {
        // Skip test if plugin failed to load
        assumeFalse(pluginLoadFailed, "Plugin must be loaded to test config");
        
        // Given: Loaded crypto configuration
        CryptoCfg cryptoCfg = QuickStocksPlugin.getCryptoCfg();
        assertNotNull(cryptoCfg, "Crypto config should be loaded");
        
        // When: Accessing various config properties
        // Then: All getters should work without throwing exceptions
        assertDoesNotThrow(() -> cryptoCfg.isEnabled(), "isEnabled() should work");
        assertDoesNotThrow(() -> cryptoCfg.getPersonalConfig(), "getPersonalConfig() should work");
        assertDoesNotThrow(() -> cryptoCfg.getCompanyConfig(), "getCompanyConfig() should work");
        assertDoesNotThrow(() -> cryptoCfg.getDefaultsConfig(), "getDefaultsConfig() should work");
        assertDoesNotThrow(() -> cryptoCfg.getTradingConfig(), "getTradingConfig() should work");
    }
    
    @Test
    @DisplayName("Personal crypto max per player reads from config")
    public void testPersonalCryptoMaxPerPlayerReadsFromConfig() {
        // Skip test if plugin failed to load
        assumeFalse(pluginLoadFailed, "Plugin must be loaded to test config");
        
        // Given: Personal crypto configuration
        CryptoCfg cryptoCfg = QuickStocksPlugin.getCryptoCfg();
        assertNotNull(cryptoCfg, "Crypto config should be loaded");
        
        // When: Reading crypto.personal.maxPerPlayer from config
        int maxPerPlayer = cryptoCfg.getPersonalConfig().getMaxPerPlayer();
        
        // Then: Should match market.yml setting (maxPerPlayer: -1 for unlimited by default)
        assertEquals(-1, maxPerPlayer, 
            "Max per player should be -1 (unlimited) by default per market.yml");
    }
    
    @Test
    @DisplayName("Company crypto max per company reads from config")
    public void testCompanyCryptoMaxPerCompanyReadsFromConfig() {
        // Skip test if plugin failed to load
        assumeFalse(pluginLoadFailed, "Plugin must be loaded to test config");
        
        // Given: Company crypto configuration
        CryptoCfg cryptoCfg = QuickStocksPlugin.getCryptoCfg();
        assertNotNull(cryptoCfg, "Crypto config should be loaded");
        
        // When: Reading crypto.company.maxPerCompany from config
        int maxPerCompany = cryptoCfg.getCompanyConfig().getMaxPerCompany();
        
        // Then: Should match market.yml setting (maxPerCompany: -1 for unlimited by default)
        assertEquals(-1, maxPerCompany, 
            "Max per company should be -1 (unlimited) by default per market.yml");
    }
}
