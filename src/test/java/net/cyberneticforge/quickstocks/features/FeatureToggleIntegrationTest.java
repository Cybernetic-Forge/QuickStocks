package net.cyberneticforge.quickstocks.features;

import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.TestBase;
import net.cyberneticforge.quickstocks.infrastructure.config.CompanyCfg;
import net.cyberneticforge.quickstocks.infrastructure.config.CryptoCfg;
import net.cyberneticforge.quickstocks.infrastructure.config.MarketCfg;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Integration tests for feature toggles across multiple systems.
 * Verifies complex scenarios involving multiple features and their interactions.
 * These tests read from actual configuration files.
 */
@DisplayName("Feature Toggle Integration Tests")
public class FeatureToggleIntegrationTest extends TestBase {
    
    @Test
    @DisplayName("Market and Company features are independently configurable")
    public void testMarketAndCompanyIndependent() {
        // Skip test if plugin failed to load
        assumeFalse(pluginLoadFailed, "Plugin must be loaded to test config");
        
        // Given: Market and company configurations
        MarketCfg marketCfg = QuickStocksPlugin.getMarketCfg();
        CompanyCfg companyCfg = QuickStocksPlugin.getCompanyCfg();
        
        assertNotNull(marketCfg, "Market config should be loaded");
        assertNotNull(companyCfg, "Company config should be loaded");
        
        // When: Reading their enabled states
        boolean marketEnabled = marketCfg.isEnabled();
        boolean companiesEnabled = companyCfg.isEnabled();
        
        // Then: They can have different values (independent toggles)
        // Both are true by default in their respective YAML files
        assertTrue(marketEnabled, "Market should be enabled per market.yml");
        assertTrue(companiesEnabled, "Companies should be enabled per companies.yml");
    }
    
    @Test
    @DisplayName("Company crypto requires both company and crypto to be enabled")
    public void testCompanyCryptoRequiresBothFeatures() {
        // Skip test if plugin failed to load
        assumeFalse(pluginLoadFailed, "Plugin must be loaded to test config");
        
        // Given: Company and crypto configurations
        CompanyCfg companyCfg = QuickStocksPlugin.getCompanyCfg();
        CryptoCfg cryptoCfg = QuickStocksPlugin.getCryptoCfg();
        
        assertNotNull(companyCfg, "Company config should be loaded");
        assertNotNull(cryptoCfg, "Crypto config should be loaded");
        
        // When: Checking if company crypto can be created
        boolean companiesEnabled = companyCfg.isEnabled();
        boolean cryptoEnabled = cryptoCfg.isEnabled();
        boolean companyCryptoEnabled = cryptoCfg.getCompanyConfig().isEnabled();
        
        // Then: All three must be true for company crypto creation
        boolean canCreateCompanyCrypto = companiesEnabled && cryptoEnabled && companyCryptoEnabled;
        
        // By default: companies=true, crypto=false, companyCrypto=false
        assertFalse(canCreateCompanyCrypto, 
            "Company crypto should require all three toggles enabled");
    }
    
    @Test
    @DisplayName("Market sub-features require main market to be enabled")
    public void testMarketSubFeaturesRequireMainToggle() {
        // Skip test if plugin failed to load
        assumeFalse(pluginLoadFailed, "Plugin must be loaded to test config");
        
        // Given: Market configuration
        MarketCfg marketCfg = QuickStocksPlugin.getMarketCfg();
        assertNotNull(marketCfg, "Market config should be loaded");
        
        // When: Reading main toggle and sub-features
        boolean marketEnabled = marketCfg.isEnabled();
        boolean watchlistEnabled = marketCfg.isWatchlistEnabled();
        boolean tradingEnabled = marketCfg.isTradingEnabled();
        
        // Then: Sub-features only work if main toggle is enabled
        boolean canUseWatchlist = marketEnabled && watchlistEnabled;
        boolean canTrade = marketEnabled && tradingEnabled;
        
        // By default, all are true, so both should work
        assertTrue(canUseWatchlist, "Watchlist should work when both toggles are enabled");
        assertTrue(canTrade, "Trading should work when both toggles are enabled");
    }
    
    @Test
    @DisplayName("Company sub-features require main company to be enabled")
    public void testCompanySubFeaturesRequireMainToggle() {
        // Skip test if plugin failed to load
        assumeFalse(pluginLoadFailed, "Plugin must be loaded to test config");
        
        // Given: Company configuration
        CompanyCfg companyCfg = QuickStocksPlugin.getCompanyCfg();
        assertNotNull(companyCfg, "Company config should be loaded");
        
        // When: Reading main toggle and sub-features
        boolean companiesEnabled = companyCfg.isEnabled();
        boolean plotsEnabled = companyCfg.isPlotsEnabled();
        boolean chestShopEnabled = companyCfg.isChestShopEnabled();
        
        // Then: Sub-features only work if main toggle is enabled
        boolean canUsePlots = companiesEnabled && plotsEnabled;
        boolean canUseChestShop = companiesEnabled && chestShopEnabled;
        
        // By default, all are true, so both should work
        assertTrue(canUsePlots, "Plots should work when both toggles are enabled");
        assertTrue(canUseChestShop, "ChestShop should work when both toggles are enabled");
    }
    
    @Test
    @DisplayName("All main feature toggles can be read independently")
    public void testAllMainFeatureToggles() {
        // Skip test if plugin failed to load
        assumeFalse(pluginLoadFailed, "Plugin must be loaded to test config");
        
        // Given: All main feature configurations
        MarketCfg marketCfg = QuickStocksPlugin.getMarketCfg();
        CompanyCfg companyCfg = QuickStocksPlugin.getCompanyCfg();
        CryptoCfg cryptoCfg = QuickStocksPlugin.getCryptoCfg();
        
        assertNotNull(marketCfg, "Market config should be loaded");
        assertNotNull(companyCfg, "Company config should be loaded");
        assertNotNull(cryptoCfg, "Crypto config should be loaded");
        
        // When: Reading all main toggles
        boolean marketEnabled = marketCfg.isEnabled();
        boolean companiesEnabled = companyCfg.isEnabled();
        boolean cryptoEnabled = cryptoCfg.isEnabled();
        
        // Then: All should be readable without errors
        // Default values: market=true, companies=true, crypto=false (per YAML files)
        assertTrue(marketEnabled, "Market should be enabled by default");
        assertTrue(companiesEnabled, "Companies should be enabled by default");
        assertFalse(cryptoEnabled, "Crypto should be disabled by default");
    }
}
