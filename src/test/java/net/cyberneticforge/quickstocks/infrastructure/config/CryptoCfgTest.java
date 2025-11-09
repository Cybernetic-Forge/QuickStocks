package net.cyberneticforge.quickstocks.infrastructure.config;

import net.cyberneticforge.quickstocks.TestBase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CryptoCfg configuration loading and defaults.
 */
class CryptoCfgTest extends TestBase {
    
    @Test
    void testCryptoConfigDefaults() {
        // Given: Default configuration values
        // When: Creating a new CryptoCfg (loads crypto section from market.yml)
        // Note: In test environment, config may not load properly, but we can test the structure
        
        // Then: Verify the config class has expected structure
        CryptoCfg config = new CryptoCfg();
        assertNotNull(config, "CryptoCfg should be instantiated");
        assertNotNull(config.getPersonalConfig(), "Personal config should be available");
        assertNotNull(config.getCompanyConfig(), "Company config should be available");
        assertNotNull(config.getDefaultsConfig(), "Defaults config should be available");
        assertNotNull(config.getTradingConfig(), "Trading config should be available");
    }
    
    @Test
    void testPersonalCryptoConfigDefaults() {
        // Given: A CryptoCfg instance
        CryptoCfg config = new CryptoCfg();
        
        // When: Getting personal crypto configuration
        CryptoCfg.PersonalCryptoConfig personalConfig = config.getPersonalConfig();
        
        // Then: Verify default values
        assertTrue(personalConfig.isEnabled(), "Personal crypto should be enabled by default");
        assertEquals(500000.0, personalConfig.getCreationCost(), 
            "Default creation cost should be 500,000");
        assertEquals(-1, personalConfig.getMaxPerPlayer(), 
            "Default max per player should be unlimited (-1)");
    }
    
    @Test
    void testCompanyCryptoConfigDefaults() {
        // Given: A CryptoCfg instance
        CryptoCfg config = new CryptoCfg();
        
        // When: Getting company crypto configuration
        CryptoCfg.CompanyCryptoConfig companyConfig = config.getCompanyConfig();
        
        // Then: Verify default values
        assertTrue(companyConfig.isEnabled(), "Company crypto should be enabled by default");
        assertEquals(100000.0, companyConfig.getBalanceThreshold(), 
            "Default balance threshold should be 100,000");
        assertEquals(-1, companyConfig.getMaxPerCompany(), 
            "Default max per company should be unlimited (-1)");
        
        // Verify balance thresholds by type
        assertNotNull(companyConfig.getBalanceThresholds(), "Balance thresholds map should not be null");
        assertTrue(companyConfig.getBalanceThresholds().containsKey("PRIVATE"), 
            "Should have PRIVATE company threshold");
        assertTrue(companyConfig.getBalanceThresholds().containsKey("PUBLIC"), 
            "Should have PUBLIC company threshold");
        assertTrue(companyConfig.getBalanceThresholds().containsKey("DAO"), 
            "Should have DAO company threshold");
    }
    
    @Test
    void testDefaultsConfigDefaults() {
        // Given: A CryptoCfg instance
        CryptoCfg config = new CryptoCfg();
        
        // When: Getting defaults configuration
        CryptoCfg.DefaultsConfig defaultsConfig = config.getDefaultsConfig();
        
        // Then: Verify default values
        assertEquals(1.0, defaultsConfig.getStartingPrice(), 
            "Default starting price should be 1.0");
        assertEquals(8, defaultsConfig.getDecimals(), 
            "Default decimals should be 8 (crypto standard)");
        assertEquals(0.0, defaultsConfig.getInitialVolume(), 
            "Default initial volume should be 0.0");
    }
    
    @Test
    void testTradingConfigDefaults() {
        // Given: A CryptoCfg instance
        CryptoCfg config = new CryptoCfg();
        
        // When: Getting trading configuration
        CryptoCfg.TradingConfig tradingConfig = config.getTradingConfig();
        
        // Then: Verify default values
        assertEquals(0.00000001, tradingConfig.getMinPrice(), 
            "Default min price should be 1 satoshi");
        assertEquals(1000000.0, tradingConfig.getMaxPrice(), 
            "Default max price should be 1,000,000");
    }
    
    @Test
    void testCryptoEnabledFlag() {
        // Given: A CryptoCfg instance
        CryptoCfg config = new CryptoCfg();
        
        // When: Checking if crypto is enabled
        // Then: Should be enabled by default
        assertTrue(config.isEnabled(), "Crypto should be enabled by default");
    }
}
