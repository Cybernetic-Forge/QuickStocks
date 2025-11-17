package net.cyberneticforge.quickstocks.core.services.features.market;

import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.api.events.MarketCloseEvent;
import net.cyberneticforge.quickstocks.api.events.MarketOpenEvent;
import net.cyberneticforge.quickstocks.core.enums.Translation;
import net.cyberneticforge.quickstocks.infrastructure.config.MarketCfg;
import net.cyberneticforge.quickstocks.infrastructure.logging.PluginLogger;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Service for managing market hours and scheduling market open/close events.
 */
public class MarketScheduler {
    
    private static final PluginLogger logger = QuickStocksPlugin.getPluginLogger();
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final MarketCfg marketConfig = QuickStocksPlugin.getMarketCfg();

    private final boolean marketHoursEnabled = marketConfig.isMarketHoursEnabled();
    private LocalTime openTime = marketConfig.getOpenTime();
    private LocalTime closeTime = marketConfig.getCloseTime();
    private ZoneId timezone = marketConfig.getTimezone();
    private boolean marketOpen;
    
    private BukkitTask checkTask;
    
    public MarketScheduler() {
        loadConfiguration();
    }
    
    /**
     * Loads market hours configuration.
     */
    private void loadConfiguration() {

        try {
            // Determine initial market state
            marketOpen = !marketHoursEnabled || isWithinMarketHours();
            
            logger.info("Market hours configured: " + openTime + " - " + closeTime + " " + timezone);
            logger.info("Market is currently " + (marketOpen ? "OPEN" : "CLOSED"));
            
        } catch (Exception e) {
            logger.warning("Failed to parse market hours configuration, using defaults: " + e.getMessage());
            openTime = LocalTime.of(6, 0);
            closeTime = LocalTime.of(22, 0);
            timezone = ZoneId.of("UTC");
            marketOpen = true;
        }
    }
    
    /**
     * Starts the market hours scheduler.
     */
    public void start() {
        if (!marketHoursEnabled) {
            marketOpen = true;
            logger.info("Market hours disabled, market is always open");
            return;
        }
        
        // Check market hours every minute
        checkTask = new BukkitRunnable() {
            @Override
            public void run() {
                checkMarketHours();
            }
        }.runTaskTimer(QuickStocksPlugin.getInstance(), 20L, 20L * 60L); // Check every minute
        
        logger.info("Market hours scheduler started");
    }
    
    /**
     * Stops the market hours scheduler.
     */
    public void stop() {
        if (checkTask != null) {
            checkTask.cancel();
            checkTask = null;
        }
    }
    
    /**
     * Checks if current time is within market hours and fires events if state changes.
     */
    private void checkMarketHours() {
        boolean shouldBeOpen = isWithinMarketHours();
        
        if (shouldBeOpen && !marketOpen) {
            // Market should open
            openMarket();
        } else if (!shouldBeOpen && marketOpen) {
            // Market should close
            closeMarket();
        }
    }
    
    /**
     * Checks if current time is within market hours.
     */
    private boolean isWithinMarketHours() {
        ZonedDateTime now = ZonedDateTime.now(timezone);
        LocalTime currentTime = now.toLocalTime();
        
        // Handle overnight markets (e.g., 22:00 - 06:00)
        if (openTime.isBefore(closeTime)) {
            // Normal case: market opens and closes on same day
            return !currentTime.isBefore(openTime) && currentTime.isBefore(closeTime);
        } else {
            // Overnight case: market closes after midnight
            return !currentTime.isBefore(openTime) || currentTime.isBefore(closeTime);
        }
    }
    
    /**
     * Opens the market and fires MarketOpenEvent.
     */
    private void openMarket() {
        marketOpen = true;
        
        // Fire MarketOpenEvent
        MarketOpenEvent event = new MarketOpenEvent(System.currentTimeMillis());
        Bukkit.getPluginManager().callEvent(event);
        
        // Broadcast to all online players
        Translation.MarketOpens.broadcast();
        logger.info("Market opened at " + LocalTime.now(timezone));
    }
    
    /**
     * Closes the market and fires MarketCloseEvent.
     */
    private void closeMarket() {
        marketOpen = false;
        
        // Fire MarketCloseEvent
        MarketCloseEvent event = new MarketCloseEvent(System.currentTimeMillis());
        Bukkit.getPluginManager().callEvent(event);
        
        // Broadcast to all online players
        Translation.MarketCloses.broadcast();

        
        logger.info("Market closed at " + LocalTime.now(timezone));
    }
    
    /**
     * Checks if the market is currently open.
     */
    public boolean isMarketOpen() {
        if (!marketHoursEnabled) {
            return true;
        }
        return marketOpen;
    }
    
    /**
     * Gets the time until market opens (in minutes).
     * Returns -1 if market is currently open or hours are disabled.
     */
    public long getMinutesUntilOpen() {
        if (!marketHoursEnabled || marketOpen) {
            return -1;
        }
        
        ZonedDateTime now = ZonedDateTime.now(timezone);
        ZonedDateTime nextOpen = now.with(openTime);
        
        // If open time has passed today, get next day's open time
        if (now.toLocalTime().isAfter(openTime)) {
            nextOpen = nextOpen.plusDays(1);
        }
        
        return ChronoUnit.MINUTES.between(now, nextOpen);
    }
    
    /**
     * Gets the time until market closes (in minutes).
     * Returns -1 if market is currently closed or hours are disabled.
     */
    public long getMinutesUntilClose() {
        if (!marketHoursEnabled || !marketOpen) {
            return -1;
        }
        
        ZonedDateTime now = ZonedDateTime.now(timezone);
        ZonedDateTime nextClose = now.with(closeTime);
        
        // If close time has passed today, get next day's close time
        if (now.toLocalTime().isAfter(closeTime)) {
            nextClose = nextClose.plusDays(1);
        }
        
        return ChronoUnit.MINUTES.between(now, nextClose);
    }
}
