package com.example.quickstocks.core.services;

import com.example.quickstocks.core.model.Company;
import com.example.quickstocks.infrastructure.config.CompanyConfig;
import com.example.quickstocks.infrastructure.db.Db;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Service for managing company market operations (shares, IPO, trading).
 */
public class CompanyMarketService {
    
    private static final Logger logger = Logger.getLogger(CompanyMarketService.class.getName());
    
    private final Db database;
    private final CompanyService companyService;
    private final WalletService walletService;
    private final CompanyConfig config;
    
    public CompanyMarketService(Db database, CompanyService companyService, 
                               WalletService walletService, CompanyConfig config) {
        this.database = database;
        this.companyService = companyService;
        this.walletService = walletService;
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
     */
    public void enableMarket(String companyId, String actorUuid) throws SQLException {
        Optional<Company> companyOpt = companyService.getCompanyById(companyId);
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
        
        // Enable market
        database.execute("UPDATE companies SET on_market = 1 WHERE id = ?", companyId);
        
        // Notify all employees
        notifyEmployees(companyId, "MARKET_ENABLED", 
            "Company " + company.getName() + " is now trading on the market as " + company.getSymbol());
        
        logger.info("Enabled market for company " + companyId);
    }
    
    /**
     * Disables the market for a company (delist).
     */
    public void disableMarket(String companyId, String actorUuid) throws SQLException {
        Optional<Company> companyOpt = companyService.getCompanyById(companyId);
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
        
        // Get all shareholders and pay them out
        List<Map<String, Object>> shareholders = database.query(
            "SELECT player_uuid, shares, avg_cost FROM company_shareholders WHERE company_id = ?", companyId);
        
        double sharePrice = calculateSharePrice(company);
        
        for (Map<String, Object> holder : shareholders) {
            String playerUuid = (String) holder.get("player_uuid");
            double shares = ((Number) holder.get("shares")).doubleValue();
            double payout = shares * sharePrice;
            
            // Pay out the shareholder
            walletService.deposit(playerUuid, payout);
            
            // Record transaction
            String txId = UUID.randomUUID().toString();
            database.execute(
                "INSERT INTO company_share_tx (id, company_id, player_uuid, type, shares, price, ts) VALUES (?, ?, ?, ?, ?, ?, ?)",
                txId, companyId, playerUuid, "SELL", shares, sharePrice, System.currentTimeMillis()
            );
            
            // Notify shareholder
            notifyPlayer(playerUuid, "MARKET_DISABLED",
                "Company " + company.getName() + " has delisted. Your " + String.format("%.2f", shares) + 
                " shares were sold for $" + String.format("%.2f", payout));
        }
        
        // Remove all shareholders
        database.execute("DELETE FROM company_shareholders WHERE company_id = ?", companyId);
        
        // Disable market
        database.execute("UPDATE companies SET on_market = 0 WHERE id = ?", companyId);
        
        logger.info("Disabled market for company " + companyId + ", paid out " + shareholders.size() + " shareholders");
    }
    
    /**
     * Updates market settings for a company.
     */
    public void updateMarketSettings(String companyId, String actorUuid, 
                                    Double marketPercentage, Boolean allowBuyout) throws SQLException {
        Optional<Company> companyOpt = companyService.getCompanyById(companyId);
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
     * Buys shares of a company.
     */
    public void buyShares(String companyId, String playerUuid, double quantity) throws SQLException {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        
        Optional<Company> companyOpt = companyService.getCompanyById(companyId);
        if (companyOpt.isEmpty()) {
            throw new IllegalArgumentException("Company not found");
        }
        
        Company company = companyOpt.get();
        
        // Check if on market
        if (!company.isOnMarket()) {
            throw new IllegalArgumentException("Company is not on the market");
        }
        
        // Calculate share price
        double sharePrice = calculateSharePrice(company);
        double totalCost = quantity * sharePrice;
        
        // Check player balance
        double balance = walletService.getBalance(playerUuid);
        if (balance < totalCost) {
            throw new IllegalArgumentException("Insufficient funds. Required: $" + String.format("%.2f", totalCost));
        }
        
        // Calculate available shares
        double totalShares = calculateTotalShares(company);
        double availableShares = totalShares - getIssuedShares(companyId);
        
        if (quantity > availableShares) {
            throw new IllegalArgumentException("Only " + String.format("%.2f", availableShares) + " shares available");
        }
        
        // Check for buyout scenario
        if (!company.isAllowBuyout()) {
            double playerShares = getPlayerShares(companyId, playerUuid);
            if (playerShares + quantity > totalShares * 0.5) {
                throw new IllegalArgumentException("Cannot buy more than 50% of company (buyout protection enabled)");
            }
        }
        
        // Withdraw from player
        walletService.withdraw(playerUuid, totalCost);
        
        // Add to company balance
        database.execute("UPDATE companies SET balance = balance + ? WHERE id = ?", totalCost, companyId);
        
        // Update or create shareholder record
        List<Map<String, Object>> existing = database.query(
            "SELECT id, shares, avg_cost FROM company_shareholders WHERE company_id = ? AND player_uuid = ?",
            companyId, playerUuid);
        
        if (existing.isEmpty()) {
            // Create new shareholder
            String holdingId = UUID.randomUUID().toString();
            database.execute(
                "INSERT INTO company_shareholders (id, company_id, player_uuid, shares, avg_cost, purchased_at) VALUES (?, ?, ?, ?, ?, ?)",
                holdingId, companyId, playerUuid, quantity, sharePrice, System.currentTimeMillis()
            );
        } else {
            // Update existing shareholder
            Map<String, Object> holder = existing.get(0);
            double currentShares = ((Number) holder.get("shares")).doubleValue();
            double currentAvgCost = ((Number) holder.get("avg_cost")).doubleValue();
            
            // Calculate new average cost
            double newAvgCost = ((currentShares * currentAvgCost) + (quantity * sharePrice)) / (currentShares + quantity);
            
            database.execute(
                "UPDATE company_shareholders SET shares = shares + ?, avg_cost = ? WHERE company_id = ? AND player_uuid = ?",
                quantity, newAvgCost, companyId, playerUuid
            );
        }
        
        // Record transaction
        String txId = UUID.randomUUID().toString();
        database.execute(
            "INSERT INTO company_share_tx (id, company_id, player_uuid, type, shares, price, ts) VALUES (?, ?, ?, ?, ?, ?, ?)",
            txId, companyId, playerUuid, "BUY", quantity, sharePrice, System.currentTimeMillis()
        );
        
        // Check if player now owns majority and buyout is allowed
        if (company.isAllowBuyout()) {
            double playerShares = getPlayerShares(companyId, playerUuid);
            if (playerShares > totalShares * 0.5 && !playerUuid.equals(company.getOwnerUuid())) {
                // Transfer ownership
                database.execute("UPDATE companies SET owner_uuid = ? WHERE id = ?", playerUuid, companyId);
                
                // Update employee record to CEO
                Optional<com.example.quickstocks.core.model.CompanyJob> ceoJob = 
                    companyService.getJobByTitle(companyId, "CEO");
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
     * Sells shares of a company.
     */
    public void sellShares(String companyId, String playerUuid, double quantity) throws SQLException {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        
        Optional<Company> companyOpt = companyService.getCompanyById(companyId);
        if (companyOpt.isEmpty()) {
            throw new IllegalArgumentException("Company not found");
        }
        
        Company company = companyOpt.get();
        
        // Check if on market
        if (!company.isOnMarket()) {
            throw new IllegalArgumentException("Company is not on the market");
        }
        
        // Check player shares
        double playerShares = getPlayerShares(companyId, playerUuid);
        if (playerShares < quantity) {
            throw new IllegalArgumentException("You only have " + String.format("%.2f", playerShares) + " shares");
        }
        
        // Calculate share price
        double sharePrice = calculateSharePrice(company);
        double totalValue = quantity * sharePrice;
        
        // Deduct from company balance (company buys back shares)
        if (company.getBalance() < totalValue) {
            throw new IllegalArgumentException("Company has insufficient balance to buy back shares");
        }
        
        database.execute("UPDATE companies SET balance = balance - ? WHERE id = ?", totalValue, companyId);
        
        // Pay player
        walletService.deposit(playerUuid, totalValue);
        
        // Update shareholder record
        database.execute(
            "UPDATE company_shareholders SET shares = shares - ? WHERE company_id = ? AND player_uuid = ?",
            quantity, companyId, playerUuid);
        
        // Remove shareholder if no shares left
        database.execute(
            "DELETE FROM company_shareholders WHERE company_id = ? AND player_uuid = ? AND shares <= 0",
            companyId, playerUuid);
        
        // Record transaction
        String txId = UUID.randomUUID().toString();
        database.execute(
            "INSERT INTO company_share_tx (id, company_id, player_uuid, type, shares, price, ts) VALUES (?, ?, ?, ?, ?, ?, ?)",
            txId, companyId, playerUuid, "SELL", quantity, sharePrice, System.currentTimeMillis()
        );
        
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
    private double calculateTotalShares(Company company) {
        // If market percentage is 70%, and company has $10,000, then total value is $10,000 / 0.7 = $14,285.71
        // We'll use a fixed share count of 10,000 for simplicity and adjust valuation
        return 10000.0;
    }
    
    /**
     * Gets total issued shares.
     */
    private double getIssuedShares(String companyId) throws SQLException {
        List<Map<String, Object>> results = database.query(
            "SELECT SUM(shares) as total FROM company_shareholders WHERE company_id = ?", companyId);
        
        if (results.isEmpty() || results.get(0).get("total") == null) {
            return 0.0;
        }
        
        return ((Number) results.get(0).get("total")).doubleValue();
    }
    
    /**
     * Gets shares owned by a player.
     */
    public double getPlayerShares(String companyId, String playerUuid) throws SQLException {
        List<Map<String, Object>> results = database.query(
            "SELECT shares FROM company_shareholders WHERE company_id = ? AND player_uuid = ?",
            companyId, playerUuid);
        
        if (results.isEmpty()) {
            return 0.0;
        }
        
        return ((Number) results.get(0).get("shares")).doubleValue();
    }
    
    /**
     * Gets all shareholders of a company.
     */
    public List<Map<String, Object>> getShareholders(String companyId) throws SQLException {
        return database.query(
            "SELECT player_uuid, shares, avg_cost, purchased_at FROM company_shareholders WHERE company_id = ? ORDER BY shares DESC",
            companyId);
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
