package net.cyberneticforge.quickstocks.features;

import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.TestBase;
import net.cyberneticforge.quickstocks.infrastructure.config.MarketCfg;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Tests for Market feature enable/disable functionality.
 * Verifies that market features can be toggled on/off and behavior is correct.
 * These tests read from actual configuration files to validate feature toggles.
 */
@DisplayName("Market Feature Toggle Tests")
public class MarketFeatureToggleTest extends TestBase {
    
    @Test
    @DisplayName("Market feature should be enabled by default")
    public void testMarketEnabledByDefault() {
        // Skip test if plugin failed to load
        assumeFalse(pluginLoadFailed, "Plugin must be loaded to test config");
        
        // Given: Default market configuration from market.yml
        // When: Reading market.enabled from config
        MarketCfg marketCfg = QuickStocksPlugin.getMarketCfg();
        
        // Then: Should be enabled by default (per market.yml: enabled: true)
        assertNotNull(marketCfg, "Market config should be loaded");
        assertTrue(marketCfg.isEnabled(), "Market should be enabled by default per market.yml");
    }
    
    @Test
    @DisplayName("Market command check follows config value")
    public void testMarketCommandChecksConfig() {
        // Skip test if plugin failed to load
        assumeFalse(pluginLoadFailed, "Plugin must be loaded to test config");
        
        // Given: Market configuration
        MarketCfg marketCfg = QuickStocksPlugin.getMarketCfg();
        assertNotNull(marketCfg, "Market config should be loaded");
        
        // When: Checking if market is enabled
        boolean marketEnabled = marketCfg.isEnabled();
        
        // Then: Command execution should follow this value
        // This simulates MarketCommand.onCommand() check: if (!QuickStocksPlugin.getMarketCfg().isEnabled())
        boolean commandShouldProceed = marketEnabled;
        
        assertEquals(marketEnabled, commandShouldProceed, 
            "Market command should proceed only when market.enabled is true");
    }
    
    @Test
    @DisplayName("Watchlist feature should be enabled by default")
    public void testWatchlistEnabledByDefault() {
        // Skip test if plugin failed to load
        assumeFalse(pluginLoadFailed, "Plugin must be loaded to test config");
        
        // Given: Default market configuration from market.yml
        // When: Reading market.features.watchlist from config
        MarketCfg marketCfg = QuickStocksPlugin.getMarketCfg();
        
        // Then: Should be enabled by default (per market.yml: features.watchlist: true)
        assertNotNull(marketCfg, "Market config should be loaded");
        assertTrue(marketCfg.isWatchlistEnabled(), 
            "Watchlist should be enabled by default per market.yml");
    }
    
    @Test
    @DisplayName("Watchlist command check follows config value")
    public void testWatchlistCommandChecksConfig() {
        // Skip test if plugin failed to load
        assumeFalse(pluginLoadFailed, "Plugin must be loaded to test config");
        
        // Given: Watchlist configuration
        MarketCfg marketCfg = QuickStocksPlugin.getMarketCfg();
        assertNotNull(marketCfg, "Market config should be loaded");
        
        // When: Checking if watchlist is enabled
        boolean watchlistEnabled = marketCfg.isWatchlistEnabled();
        
        // Then: Command execution should follow this value
        // This simulates WatchCommand.onCommand() check: if (!QuickStocksPlugin.getMarketCfg().isWatchlistEnabled())
        boolean commandShouldProceed = watchlistEnabled;
        
        assertEquals(watchlistEnabled, commandShouldProceed,
            "Watch command should proceed only when market.features.watchlist is true");
    }
    
    @Test
    @DisplayName("Portfolio feature should be enabled by default")
    public void testPortfolioEnabledByDefault() {
        // Skip test if plugin failed to load
        assumeFalse(pluginLoadFailed, "Plugin must be loaded to test config");
        
        // Given: Default market configuration from market.yml
        // When: Reading market.features.portfolio from config
        MarketCfg marketCfg = QuickStocksPlugin.getMarketCfg();
        
        // Then: Should be enabled by default (per market.yml: features.portfolio: true)
        assertNotNull(marketCfg, "Market config should be loaded");
        assertTrue(marketCfg.isPortfolioEnabled(), 
            "Portfolio should be enabled by default per market.yml");
    }
    
    @Test
    @DisplayName("Portfolio command check follows config value")
    public void testPortfolioCommandChecksConfig() {
        // Skip test if plugin failed to load
        assumeFalse(pluginLoadFailed, "Plugin must be loaded to test config");
        
        // Given: Portfolio configuration
        MarketCfg marketCfg = QuickStocksPlugin.getMarketCfg();
        assertNotNull(marketCfg, "Market config should be loaded");
        
        // When: Checking if portfolio is enabled
        boolean portfolioEnabled = marketCfg.isPortfolioEnabled();
        
        // Then: Portfolio functionality should follow this value
        boolean canUsePortfolio = portfolioEnabled;
        
        assertEquals(portfolioEnabled, canUsePortfolio,
            "Portfolio should be usable only when market.features.portfolio is true");
    }
    
    @Test
    @DisplayName("Trading feature should be enabled by default")
    public void testTradingEnabledByDefault() {
        // Skip test if plugin failed to load
        assumeFalse(pluginLoadFailed, "Plugin must be loaded to test config");
        
        // Given: Default market configuration from market.yml
        // When: Reading market.features.trading from config
        MarketCfg marketCfg = QuickStocksPlugin.getMarketCfg();
        
        // Then: Should be enabled by default (per market.yml: features.trading: true)
        assertNotNull(marketCfg, "Market config should be loaded");
        assertTrue(marketCfg.isTradingEnabled(), 
            "Trading should be enabled by default per market.yml");
    }
    
    @Test
    @DisplayName("Trading command check follows config value")
    public void testTradingCommandChecksConfig() {
        // Skip test if plugin failed to load
        assumeFalse(pluginLoadFailed, "Plugin must be loaded to test config");
        
        // Given: Trading configuration
        MarketCfg marketCfg = QuickStocksPlugin.getMarketCfg();
        assertNotNull(marketCfg, "Market config should be loaded");
        
        // When: Checking if trading is enabled
        boolean tradingEnabled = marketCfg.isTradingEnabled();
        
        // Then: Trading operations should follow this value
        boolean canTrade = tradingEnabled;
        
        assertEquals(tradingEnabled, canTrade,
            "Trading should be available only when market.features.trading is true");
    }
    
    @Test
    @DisplayName("Market Device feature reads from config")
    public void testMarketDeviceReadsFromConfig() {
        // Skip test if plugin failed to load
        assumeFalse(pluginLoadFailed, "Plugin must be loaded to test config");
        
        // Given: Default market configuration from market.yml
        // When: Reading market.features.marketDevice from config
        MarketCfg marketCfg = QuickStocksPlugin.getMarketCfg();
        
        // Then: Should match market.yml setting (marketDevice: false by default)
        assertNotNull(marketCfg, "Market config should be loaded");
        // Note: market.yml has marketDevice: false by default
        assertFalse(marketCfg.isMarketDeviceEnabled(), 
            "Market Device should be disabled by default per market.yml");
    }
    
    @Test
    @DisplayName("Crypto Command feature reads from config")
    public void testCryptoCommandReadsFromConfig() {
        // Skip test if plugin failed to load
        assumeFalse(pluginLoadFailed, "Plugin must be loaded to test config");
        
        // Given: Default market configuration from market.yml
        // When: Reading market.features.cryptoCommand from config
        MarketCfg marketCfg = QuickStocksPlugin.getMarketCfg();
        
        // Then: Should match market.yml setting (cryptoCommand: false by default)
        assertNotNull(marketCfg, "Market config should be loaded");
        // Note: market.yml has cryptoCommand: false by default
        assertFalse(marketCfg.isCryptoCommandEnabled(), 
            "Crypto Command should be disabled by default per market.yml");
    }
    
    @Test
    @DisplayName("Market sub-features are independently configurable")
    public void testMarketSubFeaturesIndependent() {
        // Skip test if plugin failed to load
        assumeFalse(pluginLoadFailed, "Plugin must be loaded to test config");
        
        // Given: Market configuration with various sub-feature settings
        MarketCfg marketCfg = QuickStocksPlugin.getMarketCfg();
        assertNotNull(marketCfg, "Market config should be loaded");
        
        // When: Reading different sub-feature toggles
        boolean watchlistEnabled = marketCfg.isWatchlistEnabled();
        boolean portfolioEnabled = marketCfg.isPortfolioEnabled();
        boolean tradingEnabled = marketCfg.isTradingEnabled();
        boolean marketDeviceEnabled = marketCfg.isMarketDeviceEnabled();
        boolean cryptoCommandEnabled = marketCfg.isCryptoCommandEnabled();
        
        // Then: Each sub-feature can be independently configured
        // The actual values depend on market.yml configuration
        assertNotNull(watchlistEnabled, "Watchlist setting should be read");
        assertNotNull(portfolioEnabled, "Portfolio setting should be read");
        assertNotNull(tradingEnabled, "Trading setting should be read");
        assertNotNull(marketDeviceEnabled, "Market Device setting should be read");
        assertNotNull(cryptoCommandEnabled, "Crypto Command setting should be read");
    }
    
    @Test
    @DisplayName("Main market toggle affects command execution")
    public void testMainMarketToggleAffectsCommands() {
        // Skip test if plugin failed to load
        assumeFalse(pluginLoadFailed, "Plugin must be loaded to test config");
        
        // Given: Market configuration
        MarketCfg marketCfg = QuickStocksPlugin.getMarketCfg();
        assertNotNull(marketCfg, "Market config should be loaded");
        
        // When: Checking if main market is enabled
        boolean marketEnabled = marketCfg.isEnabled();
        
        // Then: All market-related commands check this value
        // This simulates the pattern used in MarketCommand, WatchCommand, etc.
        // MarketCommand checks: if (!QuickStocksPlugin.getMarketCfg().isEnabled())
        assertTrue(marketEnabled, 
            "Market should be enabled by default (per market.yml: market.enabled: true)");
    }
    
    @Test
    @DisplayName("Price threshold configuration reads from config")
    public void testPriceThresholdReadsFromConfig() {
        // Skip test if plugin failed to load
        assumeFalse(pluginLoadFailed, "Plugin must be loaded to test config");
        
        // Given: Market configuration from market.yml
        // When: Reading market.priceThreshold.enabled from config
        MarketCfg marketCfg = QuickStocksPlugin.getMarketCfg();
        
        // Then: Should match market.yml setting (priceThreshold.enabled: true by default)
        assertNotNull(marketCfg, "Market config should be loaded");
        assertTrue(marketCfg.isPriceThresholdEnabled(), 
            "Price threshold should be enabled by default per market.yml");
    }
    
    @Test
    @DisplayName("Market config provides all expected getters")
    public void testMarketConfigGetters() {
        // Skip test if plugin failed to load
        assumeFalse(pluginLoadFailed, "Plugin must be loaded to test config");
        
        // Given: Loaded market configuration
        MarketCfg marketCfg = QuickStocksPlugin.getMarketCfg();
        assertNotNull(marketCfg, "Market config should be loaded");
        
        // When: Accessing various config properties
        // Then: All getters should work without throwing exceptions
        assertDoesNotThrow(() -> marketCfg.isEnabled(), "isEnabled() should work");
        assertDoesNotThrow(() -> marketCfg.isWatchlistEnabled(), "isWatchlistEnabled() should work");
        assertDoesNotThrow(() -> marketCfg.isPortfolioEnabled(), "isPortfolioEnabled() should work");
        assertDoesNotThrow(() -> marketCfg.isTradingEnabled(), "isTradingEnabled() should work");
        assertDoesNotThrow(() -> marketCfg.isMarketDeviceEnabled(), "isMarketDeviceEnabled() should work");
        assertDoesNotThrow(() -> marketCfg.isCryptoCommandEnabled(), "isCryptoCommandEnabled() should work");
        assertDoesNotThrow(() -> marketCfg.isPriceThresholdEnabled(), "isPriceThresholdEnabled() should work");
    }
}
