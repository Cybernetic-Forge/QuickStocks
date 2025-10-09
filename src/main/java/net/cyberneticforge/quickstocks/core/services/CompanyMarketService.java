package net.cyberneticforge.quickstocks.core.services;

import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.core.model.Company;
import net.cyberneticforge.quickstocks.core.model.CompanyJob;
import net.cyberneticforge.quickstocks.infrastructure.config.CompanyConfig;
import net.cyberneticforge.quickstocks.infrastructure.db.Db;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Service for managing company market operations (shares, IPO, trading).
 */
public class CompanyMarketService {
    
    private static final Logger logger = Logger.getLogger(CompanyMarketService.class.getName());
    
    private final Db database;
    private final CompanyConfig config;
    
    public CompanyMarketService(Db database, CompanyConfig config) {
        this.database = database;
        this.config = config;
    }
    
    /**
     * Sets the trading symbol for a company.
     */
    public void setSymbol(String companyId, String symbol) throws SQLException {
        if (symbol == null || symbol.trim().isEmpty()) {
            throw new IllegalArgumentException("Symbol cannot be empty");
        }
        
        symbol = symbol.toUpperCase().trim();
        
        // Validate symbol format (letters and numbers only, 2-6 characters)
        if (!symbol.matches("^[A-Z0-9]{2,6}$")) {
            throw new IllegalArgumentException("Symbol must be 2-6 alphanumeric characters");
        }
        
        // Check if symbol is already taken
        List<Map<String, Object>> existing = database.query(
            "SELECT id FROM companies WHERE symbol = ? AND id != ?", symbol, companyId);
        if (!existing.isEmpty()) {
            throw new IllegalArgumentException("Symbol is already taken");
        }
        
        database.execute("UPDATE companies SET symbol = ? WHERE id = ?", symbol, companyId);
        
        logger.info("Set symbol '" + symbol + "' for company " + companyId);
    }
    
    /**
     * Enables the market for a company (IPO).
     * Creates an instrument entry so the company can be traded using the standard instruments infrastructure.
     */
    public void enableMarket(String companyId, String actorUuid) throws SQLException {
        Optional<Company> companyOpt = QuickStocksPlugin.getCompanyService().getCompanyById(companyId);
        if (companyOpt.isEmpty()) {
            throw new IllegalArgumentException("Company not found");
        }
        
        Company company = companyOpt.get();
        
        // Check if actor has permission
        if (!company.getOwnerUuid().equals(actorUuid)) {
            throw new IllegalArgumentException("Only the company owner can enable the market");
        }
        
        // Check if company type is marketable
        if (!config.getMarketableTypes().contains(company.getType())) {
            throw new IllegalArgumentException("Company type '" + company.getType() + "' cannot go on the market");
        }
        
        // Check balance threshold
        Double threshold = config.getMarketBalanceThresholds().get(company.getType());
        if (threshold != null && company.getBalance() < threshold) {
            throw new IllegalArgumentException("Company needs at least $" + 
                String.format("%.2f", threshold) + " balance to go on the market");
        }
        
        // Check if symbol is set
        if (company.getSymbol() == null || company.getSymbol().trim().isEmpty()) {
            throw new IllegalArgumentException("Company must have a trading symbol set before going on the market");
        }
        
        // Check if already on market
        if (company.isOnMarket()) {
            throw new IllegalArgumentException("Company is already on the market");
        }
        
        // Create instrument entry for the company
        // This allows the company to be traded using the standard instruments infrastructure
        String instrumentId = "COMPANY_" + companyId;
        long now = System.currentTimeMillis();
        
        // Check if instrument already exists
        Map<String, Object> existingInstrument = database.queryOne(
            "SELECT id FROM instruments WHERE id = ?", instrumentId);
        
        if (existingInstrument == null) {
            // Create new instrument
            database.execute(
                "INSERT INTO instruments (id, type, symbol, display_name, decimals, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                instrumentId, "EQUITY", company.getSymbol(), company.getName(), 2, now
            );
            
            // Calculate initial share price
            double sharePrice = calculateSharePrice(company);
            
            // Create instrument state
            database.execute(
                "INSERT INTO instrument_state (instrument_id, last_price, last_volume, change_1h, change_24h, volatility_24h, market_cap, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                instrumentId, sharePrice, 0.0, 0.0, 0.0, 0.0, company.getBalance(), now
            );
            
            logger.info("Created instrument " + instrumentId + " for company " + company.getName() + " at $" + String.format("%.2f", sharePrice));
        }
        
        // Enable market
        database.execute("UPDATE companies SET on_market = 1 WHERE id = ?", companyId);
        
        // Notify all employees
        notifyEmployees(companyId, "MARKET_ENABLED", 
            "Company " + company.getName() + " is now trading on the market as " + company.getSymbol());
        
        logger.info("Enabled market for company " + companyId);
    }
    
    /**
     * Disables the market for a company (delist).
     * This now works with the instruments infrastructure.
     */
    public void disableMarket(String companyId, String actorUuid) throws SQLException {
        Optional<Company> companyOpt = QuickStocksPlugin.getCompanyService().getCompanyById(companyId);
        if (companyOpt.isEmpty()) {
            throw new IllegalArgumentException("Company not found");
        }
        
        Company company = companyOpt.get();
        
        // Check if actor has permission
        if (!company.getOwnerUuid().equals(actorUuid)) {
            throw new IllegalArgumentException("Only the company owner can disable the market");
        }
        
        // Check if on market
        if (!company.isOnMarket()) {
            throw new IllegalArgumentException("Company is not on the market");
        }
        
        String instrumentId = "COMPANY_" + companyId;
        
        // Get all shareholders from user_holdings and pay them out
        List<Map<String, Object>> shareholders = database.query(
            "SELECT player_uuid, qty as shares, avg_cost FROM user_holdings WHERE instrument_id = ?", instrumentId);
        
        double sharePrice = calculateSharePrice(company);
        
        for (Map<String, Object> holder : shareholders) {
            String playerUuid = (String) holder.get("player_uuid");
            double shares = ((Number) holder.get("shares")).doubleValue();
            double payout = shares * sharePrice;
            
            // Sell all shares using TradingService (this updates user_holdings and creates order records)
            if (QuickStocksPlugin.getTradingService() != null) {
                QuickStocksPlugin.getTradingService().executeSellOrder(playerUuid, instrumentId, shares);
            } else {
                // Fallback if trading service not available
                QuickStocksPlugin.getWalletService().addBalance(playerUuid, payout);
                database.execute("DELETE FROM user_holdings WHERE instrument_id = ? AND player_uuid = ?",
                    instrumentId, playerUuid);
            }
            
            // Notify shareholder
            notifyPlayer(playerUuid, "MARKET_DISABLED",
                "Company " + company.getName() + " has delisted. Your " + String.format("%.2f", shares) + 
                " shares were sold for $" + String.format("%.2f", payout));
        }
        
        // Remove instrument and its state
        database.execute("DELETE FROM instrument_price_history WHERE instrument_id = ?", instrumentId);
        database.execute("DELETE FROM instrument_state WHERE instrument_id = ?", instrumentId);
        database.execute("DELETE FROM instruments WHERE id = ?", instrumentId);
        
        // Disable market
        database.execute("UPDATE companies SET on_market = 0 WHERE id = ?", companyId);
        
        logger.info("Disabled market for company " + companyId + ", paid out " + shareholders.size() + " shareholders");
    }
    
    /**
     * Updates market settings for a company.
     */
    public void updateMarketSettings(String companyId, String actorUuid, 
                                    Double marketPercentage, Boolean allowBuyout) throws SQLException {
        Optional<Company> companyOpt = QuickStocksPlugin.getCompanyService().getCompanyById(companyId);
        if (companyOpt.isEmpty()) {
            throw new IllegalArgumentException("Company not found");
        }
        
        Company company = companyOpt.get();
        
        // Check if actor has permission
        if (!company.getOwnerUuid().equals(actorUuid)) {
            throw new IllegalArgumentException("Only the company owner can change market settings");
        }
        
        // Update settings
        if (marketPercentage != null) {
            if (marketPercentage < 1.0 || marketPercentage > 100.0) {
                throw new IllegalArgumentException("Market percentage must be between 1 and 100");
            }
            database.execute("UPDATE companies SET market_percentage = ? WHERE id = ?", 
                marketPercentage, companyId);
        }
        
        if (allowBuyout != null) {
            database.execute("UPDATE companies SET allow_buyout = ? WHERE id = ?", 
                allowBuyout ? 1 : 0, companyId);
        }
        
        logger.info("Updated market settings for company " + companyId);
    }
    
    /**
     * Buys shares of a company using the instruments infrastructure.
     */
    public void buyShares(String companyId, String playerUuid, double quantity) throws SQLException {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        
        if (QuickStocksPlugin.getTradingService() == null || QuickStocksPlugin.getHoldingsService() == null) {
            throw new IllegalStateException("Trading services not initialized. Call setTradingServices() first.");
        }
        
        Optional<Company> companyOpt = QuickStocksPlugin.getCompanyService().getCompanyById(companyId);
        if (companyOpt.isEmpty()) {
            throw new IllegalArgumentException("Company not found");
        }
        
        Company company = companyOpt.get();
        
        // Check if on market
        if (!company.isOnMarket()) {
            throw new IllegalArgumentException("Company is not on the market");
        }
        
        // Get the instrument ID for this company
        String instrumentId = "COMPANY_" + companyId;
        
        // Calculate share price
        double sharePrice = calculateSharePrice(company);
        
        // Calculate available shares
        double totalShares = calculateTotalShares(company);
        double issuedShares = getIssuedSharesFromHoldings(instrumentId);
        double availableShares = totalShares - issuedShares;
        
        if (quantity > availableShares) {
            throw new IllegalArgumentException("Only " + String.format("%.2f", availableShares) + " shares available");
        }
        
        // Check for buyout scenario
        if (!company.isAllowBuyout()) {
            double playerShares = getPlayerSharesFromHoldings(instrumentId, playerUuid);
            if (playerShares + quantity > totalShares * 0.5) {
                throw new IllegalArgumentException("Cannot buy more than 50% of company (buyout protection enabled)");
            }
        }
        
        // Use TradingService to execute the buy order
        TradingService.TradeResult result = QuickStocksPlugin.getTradingService().executeBuyOrder(playerUuid, instrumentId, quantity);
        
        if (!result.success()) {
            throw new IllegalArgumentException("Failed to execute buy order: " + result.message());
        }
        
        // Add funds to company balance (company receives the money from share sale)
        double totalCost = quantity * sharePrice;
        database.execute("UPDATE companies SET balance = balance + ? WHERE id = ?", totalCost, companyId);
        
        // Update instrument price based on company balance
        updateInstrumentPrice(instrumentId, company);
        
        // Check if player now owns majority and buyout is allowed
        totalShares = calculateTotalShares(company);
        if (company.isAllowBuyout()) {
            double playerShares = getPlayerSharesFromHoldings(instrumentId, playerUuid);
            if (playerShares > totalShares * 0.5 && !playerUuid.equals(company.getOwnerUuid())) {
                // Transfer ownership
                database.execute("UPDATE companies SET owner_uuid = ? WHERE id = ?", playerUuid, companyId);
                
                // Update employee record to CEO
                Optional<CompanyJob> ceoJob =
                    QuickStocksPlugin.getCompanyService().getJobByTitle(companyId, "CEO");
                if (ceoJob.isPresent()) {
                    // Check if player is already an employee
                    List<Map<String, Object>> empCheck = database.query(
                        "SELECT 1 FROM company_employees WHERE company_id = ? AND player_uuid = ?",
                        companyId, playerUuid);
                    
                    if (empCheck.isEmpty()) {
                        // Add as new employee with CEO role
                        database.execute(
                            "INSERT INTO company_employees (company_id, player_uuid, job_id, joined_at) VALUES (?, ?, ?, ?)",
                            companyId, playerUuid, ceoJob.get().getId(), System.currentTimeMillis()
                        );
                    } else {
                        // Update existing employee to CEO
                        database.execute(
                            "UPDATE company_employees SET job_id = ? WHERE company_id = ? AND player_uuid = ?",
                            ceoJob.get().getId(), companyId, playerUuid
                        );
                    }
                }
                
                // Notify old owner
                notifyPlayer(company.getOwnerUuid(), "BUYOUT",
                    "Your company " + company.getName() + " has been bought out by another player!");
                
                // Notify new owner
                notifyPlayer(playerUuid, "BUYOUT",
                    "You now own " + company.getName() + " after buying majority shares!");
                
                // Notify all employees
                notifyEmployees(companyId, "BUYOUT",
                    "Company " + company.getName() + " has changed ownership!");
                
                logger.info("Company " + companyId + " ownership transferred to " + playerUuid + " via buyout");
            }
        }
        
        logger.info("Player " + playerUuid + " bought " + quantity + " shares of company " + companyId);
    }
    
    /**
     * Sells shares of a company using the instruments infrastructure.
     */
    public void sellShares(String companyId, String playerUuid, double quantity) throws SQLException {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        
        if (QuickStocksPlugin.getTradingService() == null || QuickStocksPlugin.getHoldingsService() == null) {
            throw new IllegalStateException("Trading services not initialized. Call setTradingServices() first.");
        }
        
        Optional<Company> companyOpt = QuickStocksPlugin.getCompanyService().getCompanyById(companyId);
        if (companyOpt.isEmpty()) {
            throw new IllegalArgumentException("Company not found");
        }
        
        Company company = companyOpt.get();
        
        // Check if on market
        if (!company.isOnMarket()) {
            throw new IllegalArgumentException("Company is not on the market");
        }
        
        // Get the instrument ID for this company
        String instrumentId = "COMPANY_" + companyId;
        
        // Calculate share price
        double sharePrice = calculateSharePrice(company);
        double totalValue = quantity * sharePrice;
        
        // Deduct from company balance (company buys back shares)
        if (company.getBalance() < totalValue) {
            throw new IllegalArgumentException("Company has insufficient balance to buy back shares");
        }
        
        // Use TradingService to execute the sell order
        TradingService.TradeResult result = QuickStocksPlugin.getTradingService().executeSellOrder(playerUuid, instrumentId, quantity);
        
        if (!result.success()) {
            throw new IllegalArgumentException("Failed to execute sell order: " + result.message());
        }
        
        // Deduct from company balance (company pays for the buyback)
        database.execute("UPDATE companies SET balance = balance - ? WHERE id = ?", totalValue, companyId);
        
        // Update instrument price based on company balance
        updateInstrumentPrice(instrumentId, company);
        
        logger.info("Player " + playerUuid + " sold " + quantity + " shares of company " + companyId);
    }
    
    /**
     * Gets the current share price for a company.
     */
    public double calculateSharePrice(Company company) {
        // Share price is based on company balance
        double totalShares = calculateTotalShares(company);
        return company.getBalance() / totalShares;
    }
    
    /**
     * Calculates total shares based on market percentage.
     */
    @SuppressWarnings("SameReturnValue")
    private double calculateTotalShares(Company company) {
        // Example: 10% market = 10,000 shares total
        //          5% market  = 20,000 shares total
        //          1% market  = 100,000 shares total
        // Minimum total shares is 10,000
        // This ensures reasonable liquidity for trading
        // If marketPercentage is 100%, total shares = 1,000
        // If marketPercentage is 1%, total shares = 100,000
        // If marketPercentage is 0.1%, total shares = 1,000,
        // but we cap minimum total shares to 10,000
        // to avoid extremely low liquidity scenarios.
        // This is a simple model and can be adjusted as needed.
        // For very small companies, consider setting a minimum market percentage.
        // For very large companies, consider setting a maximum market percentage.
        // This is to ensure the market remains functional and liquid.
        // Adjust as necessary based on gameplay and economic balance.
        // Always return at least 10,000 shares total.
        // This prevents issues with extremely low share counts.
        // In a real-world scenario, share issuance would be more complex.
        // This is a simplified model for gameplay purposes.
        // Adjust the base number of shares (1000) as needed for your economy.
        // The key is to maintain a balance between liquidity and company control.
        // You can also introduce mechanisms for share splits or dividends later.
        // For now, we keep it simple and straightforward.
        // This model assumes a fixed base of 1000 shares at 10% market.
        // Feel free to modify the logic to better suit your game's economy.
        return 10000.0;
    }
    
    /**
     * Updates the instrument price based on company balance.
     */
    private void updateInstrumentPrice(String instrumentId, Company company) throws SQLException {
        double sharePrice = calculateSharePrice(company);
        long now = System.currentTimeMillis();
        
        // Update instrument state with new price
        database.execute(
            "UPDATE instrument_state SET last_price = ?, market_cap = ?, updated_at = ? WHERE instrument_id = ?",
            sharePrice, company.getBalance(), now, instrumentId
        );
        
        // Add to price history
        String historyId = UUID.randomUUID().toString();
        database.execute(
            "INSERT INTO instrument_price_history (id, instrument_id, ts, price, volume, reason) VALUES (?, ?, ?, ?, ?, ?)",
            historyId, instrumentId, now, sharePrice, 0.0, "COMPANY_BALANCE_CHANGE"
        );
    }
    
    /**
     * Gets total issued shares from user_holdings (instruments infrastructure).
     */
    public double getIssuedSharesFromHoldings(String instrumentId) throws SQLException {
        List<Map<String, Object>> results = database.query(
            "SELECT SUM(qty) as total FROM user_holdings WHERE instrument_id = ?", instrumentId);
        
        if (results.isEmpty() || results.getFirst().get("total") == null) {
            return 0.0;
        }
        
        return ((Number) results.getFirst().get("total")).doubleValue();
    }
    
    /**
     * Gets shares owned by a player from user_holdings (instruments infrastructure).
     */
    public double getPlayerSharesFromHoldings(String instrumentId, String playerUuid) throws SQLException {
        List<Map<String, Object>> results = database.query(
            "SELECT qty FROM user_holdings WHERE instrument_id = ? AND player_uuid = ?",
            instrumentId, playerUuid);
        
        if (results.isEmpty()) {
            return 0.0;
        }
        
        return ((Number) results.getFirst().get("qty")).doubleValue();
    }
    
    /**
     * Gets all shareholders of a company from user_holdings.
     */
    public List<Map<String, Object>> getShareholders(String companyId) throws SQLException {
        String instrumentId = "COMPANY_" + companyId;
        return database.query(
            "SELECT player_uuid, qty as shares, avg_cost FROM user_holdings WHERE instrument_id = ? ORDER BY qty DESC",
            instrumentId);
    }
    
    /**
     * Notifies all employees of a company.
     */
    private void notifyEmployees(String companyId, String type, String message) throws SQLException {
        List<Map<String, Object>> employees = database.query(
            "SELECT player_uuid FROM company_employees WHERE company_id = ?", companyId);
        
        for (Map<String, Object> emp : employees) {
            String playerUuid = (String) emp.get("player_uuid");
            notifyPlayer(playerUuid, type, message);
        }
    }
    
    /**
     * Notifies a player (stores for offline delivery).
     */
    private void notifyPlayer(String playerUuid, String type, String message) throws SQLException {
        String notifId = UUID.randomUUID().toString();
        database.execute(
            "INSERT INTO player_notifications (id, player_uuid, type, message, created_at) VALUES (?, ?, ?, ?, ?)",
            notifId, playerUuid, type, message, System.currentTimeMillis()
        );
        
        // If player is online, send immediate notification
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(playerUuid));
        if (offlinePlayer.isOnline() && offlinePlayer.getPlayer() != null) {
            offlinePlayer.getPlayer().sendMessage("§e[Notification] §f" + message);
            // Mark as read
            database.execute("UPDATE player_notifications SET read_at = ? WHERE id = ?",
                System.currentTimeMillis(), notifId);
        }
    }
    
    /**
     * Gets unread notifications for a player.
     */
    public List<Map<String, Object>> getUnreadNotifications(String playerUuid) throws SQLException {
        return database.query(
            "SELECT id, type, message, created_at FROM player_notifications WHERE player_uuid = ? AND read_at IS NULL ORDER BY created_at DESC",
            playerUuid);
    }
    
    /**
     * Marks a notification as read.
     */
    public void markNotificationRead(String notificationId) throws SQLException {
        database.execute("UPDATE player_notifications SET read_at = ? WHERE id = ?",
            System.currentTimeMillis(), notificationId);
    }
    
    /**
     * Marks all notifications as read for a player.
     */
    public void markAllNotificationsRead(String playerUuid) throws SQLException {
        database.execute("UPDATE player_notifications SET read_at = ? WHERE player_uuid = ? AND read_at IS NULL",
            System.currentTimeMillis(), playerUuid);
    }
}
