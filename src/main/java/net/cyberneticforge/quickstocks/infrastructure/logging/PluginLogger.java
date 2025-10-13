package net.cyberneticforge.quickstocks.infrastructure.logging;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Centralized logging system for QuickStocks plugin.
 * Provides configurable debug levels to control what gets logged.
 * 
 * Debug levels:
 * - 0: OFF - No debug messages (only errors/warnings)
 * - 1: INFO - Basic operational messages
 * - 2: DEBUG - Detailed debug information
 * - 3: TRACE - Very verbose tracing information
 */
public class PluginLogger {
    
    private final Logger internalLogger;
    private final int debugLevel;
    
    /**
     * Creates a new PluginLogger instance.
     * 
     * @param plugin The JavaPlugin instance to get the logger from
     * @param debugLevel The debug level (0-3)
     */
    public PluginLogger(JavaPlugin plugin, int debugLevel) {
        this.internalLogger = plugin.getLogger();
        this.debugLevel = Math.max(0, Math.min(3, debugLevel)); // Clamp between 0-3
    }
    
    /**
     * Logs a severe error message. Always logged regardless of debug level.
     * 
     * @param message The message to log
     */
    public void severe(String message) {
        internalLogger.severe(message);
    }
    
    /**
     * Logs a severe error message with throwable. Always logged regardless of debug level.
     * 
     * @param message The message to log
     * @param throwable The exception to log
     */
    public void severe(String message, Throwable throwable) {
        internalLogger.log(Level.SEVERE, message, throwable);
    }
    
    /**
     * Logs a warning message. Always logged regardless of debug level.
     * 
     * @param message The message to log
     */
    public void warning(String message) {
        internalLogger.warning(message);
    }
    
    /**
     * Logs a warning message with throwable. Always logged regardless of debug level.
     * 
     * @param message The message to log
     * @param throwable The exception to log
     */
    public void warning(String message, Throwable throwable) {
        internalLogger.log(Level.WARNING, message, throwable);
    }
    
    /**
     * Logs an info message. Only logged if debug level >= 1.
     * 
     * @param message The message to log
     */
    public void info(String message) {
        if (debugLevel >= 1) {
            internalLogger.info(message);
        }
    }
    
    /**
     * Logs a debug message. Only logged if debug level >= 2.
     * 
     * @param message The message to log
     */
    public void debug(String message) {
        if (debugLevel >= 2) {
            internalLogger.info("[DEBUG] " + message);
        }
    }
    
    /**
     * Logs a trace message. Only logged if debug level >= 3.
     * 
     * @param message The message to log
     */
    public void trace(String message) {
        if (debugLevel >= 3) {
            internalLogger.info("[TRACE] " + message);
        }
    }
    
    /**
     * Gets the current debug level.
     * 
     * @return The debug level (0-3)
     */
    public int getDebugLevel() {
        return debugLevel;
    }
    
    /**
     * Checks if info logging is enabled.
     * 
     * @return true if debug level >= 1
     */
    public boolean isInfoEnabled() {
        return debugLevel >= 1;
    }
    
    /**
     * Checks if debug logging is enabled.
     * 
     * @return true if debug level >= 2
     */
    public boolean isDebugEnabled() {
        return debugLevel >= 2;
    }
    
    /**
     * Checks if trace logging is enabled.
     * 
     * @return true if debug level >= 3
     */
    public boolean isTraceEnabled() {
        return debugLevel >= 3;
    }
}
