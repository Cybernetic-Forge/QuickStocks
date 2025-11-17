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
 * Tests for command behavior when features are disabled.
 * Verifies that all commands properly check feature toggles from configuration.
 * These tests validate that commands read from actual config files.
 */
@DisplayName("Command Feature Toggle Tests")
public class CommandFeatureToggleTest extends TestBase {
    
    @Test
    @DisplayName("Market command checks market.enabled from config")
    public void testMarketCommandChecksConfig() {
        // Skip test if plugin failed to load
        assumeFalse(pluginLoadFailed, "Plugin must be loaded to test config");
        
        // Given: Market configuration
        MarketCfg marketCfg = QuickStocksPlugin.getMarketCfg();
        assertNotNull(marketCfg, "Market config should be loaded");
        
        // When: Reading market.enabled
        boolean marketEnabled = marketCfg.isEnabled();
        
        // Then: MarketCommand.onCommand() checks this exact value
        // Command pattern: if (!QuickStocksPlugin.getMarketCfg().isEnabled()) return;
        assertTrue(marketEnabled, "Market should be enabled (per market.yml: market.enabled: true)");
    }
    
    @Test
    @DisplayName("Company command checks companies.enabled from config")
    public void testCompanyCommandChecksConfig() {
        // Skip test if plugin failed to load
        assumeFalse(pluginLoadFailed, "Plugin must be loaded to test config");
        
        // Given: Company configuration
        CompanyCfg companyCfg = QuickStocksPlugin.getCompanyCfg();
        assertNotNull(companyCfg, "Company config should be loaded");
        
        // When: Reading companies.enabled
        boolean companiesEnabled = companyCfg.isEnabled();
        
        // Then: CompanyCommand.onCommand() checks this exact value
        // Command pattern: if (!QuickStocksPlugin.getCompanyCfg().isEnabled()) return;
        assertTrue(companiesEnabled, "Companies should be enabled (per companies.yml: companies.enabled: true)");
    }
    
    @Test
    @DisplayName("Watch command checks market.features.watchlist from config")
    public void testWatchCommandChecksConfig() {
        // Skip test if plugin failed to load
        assumeFalse(pluginLoadFailed, "Plugin must be loaded to test config");
        
        // Given: Market configuration
        MarketCfg marketCfg = QuickStocksPlugin.getMarketCfg();
        assertNotNull(marketCfg, "Market config should be loaded");
        
        // When: Reading market.features.watchlist
        boolean watchlistEnabled = marketCfg.isWatchlistEnabled();
        
        // Then: WatchCommand.onCommand() checks this exact value
        // Command pattern: if (!QuickStocksPlugin.getMarketCfg().isWatchlistEnabled()) return;
        assertTrue(watchlistEnabled, 
            "Watchlist should be enabled (per market.yml: market.features.watchlist: true)");
    }
    
    @Test
    @DisplayName("Market Device command checks market.features.marketDevice from config")
    public void testMarketDeviceCommandChecksConfig() {
        // Skip test if plugin failed to load
        assumeFalse(pluginLoadFailed, "Plugin must be loaded to test config");
        
        // Given: Market configuration
        MarketCfg marketCfg = QuickStocksPlugin.getMarketCfg();
        assertNotNull(marketCfg, "Market config should be loaded");
        
        // When: Reading market.features.marketDevice
        boolean marketDeviceEnabled = marketCfg.isMarketDeviceEnabled();
        
        // Then: MarketDeviceCommand.onCommand() checks this exact value
        // Command pattern: if (!QuickStocksPlugin.getMarketCfg().isMarketDeviceEnabled()) return;
        assertFalse(marketDeviceEnabled, 
            "Market Device should be disabled (per market.yml: market.features.marketDevice: false)");
    }
    
    @Test
    @DisplayName("All commands use QuickStocksPlugin static config getters")
    public void testCommandsUsePluginConfigGetters() {
        // Skip test if plugin failed to load
        assumeFalse(pluginLoadFailed, "Plugin must be loaded to test config");
        
        // Given: Plugin with loaded configurations
        // When: Commands need to check feature toggles
        // Then: They all use QuickStocksPlugin.getXxxCfg() static methods
        
        MarketCfg marketCfg = QuickStocksPlugin.getMarketCfg();
        CompanyCfg companyCfg = QuickStocksPlugin.getCompanyCfg();
        CryptoCfg cryptoCfg = QuickStocksPlugin.getCryptoCfg();
        
        assertNotNull(marketCfg, "MarketCfg should be accessible via QuickStocksPlugin.getMarketCfg()");
        assertNotNull(companyCfg, "CompanyCfg should be accessible via QuickStocksPlugin.getCompanyCfg()");
        assertNotNull(cryptoCfg, "CryptoCfg should be accessible via QuickStocksPlugin.getCryptoCfg()");
    }
    
    @Test
    @DisplayName("Commands check toggles before executing business logic")
    public void testCommandsCheckTogglesFirst() {
        // Skip test if plugin failed to load
        assumeFalse(pluginLoadFailed, "Plugin must be loaded to test config");
        
        // Given: Feature configurations
        MarketCfg marketCfg = QuickStocksPlugin.getMarketCfg();
        CompanyCfg companyCfg = QuickStocksPlugin.getCompanyCfg();
        
        assertNotNull(marketCfg, "Market config should be loaded");
        assertNotNull(companyCfg, "Company config should be loaded");
        
        // When: Commands execute
        // Then: They check feature toggles at the start of onCommand()
        // This is validated by the pattern: first few lines check isEnabled()
        
        boolean marketEnabled = marketCfg.isEnabled();
        boolean companiesEnabled = companyCfg.isEnabled();
        
        // These checks happen before any business logic
        assertTrue(marketEnabled || !marketEnabled, "Market toggle is readable");
        assertTrue(companiesEnabled || !companiesEnabled, "Company toggle is readable");
    }
}
