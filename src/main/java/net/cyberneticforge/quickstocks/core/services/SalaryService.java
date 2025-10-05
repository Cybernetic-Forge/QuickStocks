package net.cyberneticforge.quickstocks.core.services;

import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.infrastructure.config.CompanyConfig;
import net.cyberneticforge.quickstocks.infrastructure.db.Db;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Service for managing employee salaries and payments.
 */
public class SalaryService {
    
    private static final Logger logger = Logger.getLogger(SalaryService.class.getName());
    
    private final Db database;
    private final CompanyConfig config;
    private final CompanyService companyService;
    
    public SalaryService(Db database, CompanyConfig config, CompanyService companyService) {
        this.database = database;
        this.config = config;
        this.companyService = companyService;
    }
    
    /**
     * Set salary for a job title.
     */
    public void setJobSalary(String jobId, double amount) throws SQLException {
        if (amount < 0) {
            throw new IllegalArgumentException("Salary amount cannot be negative");
        }
        
        database.execute(
            "INSERT OR REPLACE INTO company_job_salaries (job_id, salary_amount) VALUES (?, ?)",
            jobId, amount
        );
        
        logger.fine("Set job salary for job " + jobId + " to $" + amount);
    }
    
    /**
     * Get salary for a job title.
     */
    public double getJobSalary(String jobId) throws SQLException {
        List<Map<String, Object>> results = database.query(
            "SELECT salary_amount FROM company_job_salaries WHERE job_id = ?", jobId
        );
        
        if (results.isEmpty()) {
            return config.getDefaultJobSalary();
        }
        
        return ((Number) results.get(0).get("salary_amount")).doubleValue();
    }
    
    /**
     * Set player-specific salary (overrides job salary).
     */
    public void setPlayerSalary(String companyId, String playerUuid, double amount, String setByUuid) throws SQLException {
        if (amount < 0) {
            throw new IllegalArgumentException("Salary amount cannot be negative");
        }
        
        long now = System.currentTimeMillis();
        
        database.execute(
            "INSERT OR REPLACE INTO company_employee_salaries (company_id, player_uuid, salary_amount, set_at, set_by_uuid) " +
            "VALUES (?, ?, ?, ?, ?)",
            companyId, playerUuid, amount, now, setByUuid
        );
        
        logger.fine("Set player salary for " + playerUuid + " in company " + companyId + " to $" + amount);
    }
    
    /**
     * Get player-specific salary (returns null if no override is set).
     */
    public Double getPlayerSalary(String companyId, String playerUuid) throws SQLException {
        List<Map<String, Object>> results = database.query(
            "SELECT salary_amount FROM company_employee_salaries WHERE company_id = ? AND player_uuid = ?",
            companyId, playerUuid
        );
        
        if (results.isEmpty()) {
            return null;
        }
        
        return ((Number) results.get(0).get("salary_amount")).doubleValue();
    }
    
    /**
     * Remove player-specific salary override.
     */
    public void removePlayerSalary(String companyId, String playerUuid) throws SQLException {
        database.execute(
            "DELETE FROM company_employee_salaries WHERE company_id = ? AND player_uuid = ?",
            companyId, playerUuid
        );
        
        logger.fine("Removed player salary override for " + playerUuid + " in company " + companyId);
    }
    
    /**
     * Get effective salary for a player (checks player override first, then job salary).
     */
    public double getEffectiveSalary(String companyId, String playerUuid) throws SQLException {
        // Check for player-specific salary
        Double playerSalary = getPlayerSalary(companyId, playerUuid);
        if (playerSalary != null) {
            return playerSalary;
        }
        
        // Get player's job
        List<Map<String, Object>> employeeResults = database.query(
            "SELECT job_id FROM company_employees WHERE company_id = ? AND player_uuid = ?",
            companyId, playerUuid
        );
        
        if (employeeResults.isEmpty()) {
            return 0.0;
        }
        
        String jobId = (String) employeeResults.get(0).get("job_id");
        return getJobSalary(jobId);
    }
    
    /**
     * Set payment cycle for a company.
     */
    public void setPaymentCycle(String companyId, String cycle) throws SQLException {
        if (!config.getPaymentCycles().contains(cycle)) {
            throw new IllegalArgumentException("Invalid payment cycle. Must be one of: " + 
                String.join(", ", config.getPaymentCycles()));
        }
        
        database.execute(
            "INSERT OR REPLACE INTO company_salary_config (company_id, payment_cycle, last_payment) VALUES (?, ?, ?)",
            companyId, cycle, System.currentTimeMillis()
        );
        
        logger.fine("Set payment cycle for company " + companyId + " to " + cycle);
    }
    
    /**
     * Get payment cycle for a company.
     */
    public String getPaymentCycle(String companyId) throws SQLException {
        List<Map<String, Object>> results = database.query(
            "SELECT payment_cycle FROM company_salary_config WHERE company_id = ?", companyId
        );
        
        if (results.isEmpty()) {
            return "24h"; // Default cycle
        }
        
        return (String) results.get(0).get("payment_cycle");
    }
    
    /**
     * Get last payment time for a company.
     */
    public long getLastPaymentTime(String companyId) throws SQLException {
        List<Map<String, Object>> results = database.query(
            "SELECT last_payment FROM company_salary_config WHERE company_id = ?", companyId
        );
        
        if (results.isEmpty()) {
            return 0;
        }
        
        return ((Number) results.get(0).get("last_payment")).longValue();
    }
    
    /**
     * Process salary payments for a company (if due).
     * Returns number of payments made.
     */
    public int processSalaryPayments(String companyId) throws SQLException {
        String cycle = getPaymentCycle(companyId);
        long lastPayment = getLastPaymentTime(companyId);
        long now = System.currentTimeMillis();
        
        // Calculate cycle duration in milliseconds
        long cycleDuration = parseCycleDuration(cycle);
        
        // Check if payment is due
        if (now - lastPayment < cycleDuration) {
            return 0; // Not yet time for payment
        }
        
        // Get company balance
        List<Map<String, Object>> companyResults = database.query(
            "SELECT balance FROM companies WHERE id = ?", companyId
        );
        
        if (companyResults.isEmpty()) {
            return 0;
        }
        
        double companyBalance = ((Number) companyResults.get(0).get("balance")).doubleValue();
        
        // Get all employees of the company
        List<Map<String, Object>> employees = database.query(
            "SELECT player_uuid FROM company_employees WHERE company_id = ?", companyId
        );
        
        int paymentsSuccessful = 0;
        double totalPaid = 0.0;
        
        for (Map<String, Object> employee : employees) {
            String playerUuid = (String) employee.get("player_uuid");
            double salary = getEffectiveSalary(companyId, playerUuid);
            
            if (salary <= 0) {
                continue; // No salary configured
            }
            
            if (companyBalance < salary) {
                logger.warning("Insufficient company balance to pay salary to " + playerUuid + 
                              " in company " + companyId);
                continue;
            }
            
            // Process payment
            try {
                // Deduct from company balance
                companyService.removeDirectFromBalance(companyId, salary, 
                    "Salary payment to " + playerUuid);
                companyBalance -= salary;
                
                // Add to player wallet
                QuickStocksPlugin.getWalletService().addBalance(playerUuid, salary);
                
                // Record payment
                String paymentId = UUID.randomUUID().toString();
                database.execute(
                    "INSERT INTO company_salary_payments (id, company_id, player_uuid, amount, payment_ts, cycle) " +
                    "VALUES (?, ?, ?, ?, ?, ?)",
                    paymentId, companyId, playerUuid, salary, now, cycle
                );
                
                paymentsSuccessful++;
                totalPaid += salary;
                
                logger.fine("Paid salary of $" + salary + " to " + playerUuid + " from company " + companyId);
                
            } catch (Exception e) {
                logger.warning("Failed to pay salary to " + playerUuid + ": " + e.getMessage());
            }
        }
        
        // Update last payment time
        database.execute(
            "UPDATE company_salary_config SET last_payment = ? WHERE company_id = ?",
            now, companyId
        );
        
        if (paymentsSuccessful > 0) {
            logger.info("Processed " + paymentsSuccessful + " salary payments totaling $" + 
                       String.format("%.2f", totalPaid) + " for company " + companyId);
        }
        
        return paymentsSuccessful;
    }
    
    /**
     * Parse cycle duration string to milliseconds.
     */
    private long parseCycleDuration(String cycle) {
        return switch (cycle) {
            case "1h" -> 60 * 60 * 1000L; // 1 hour
            case "24h" -> 24 * 60 * 60 * 1000L; // 24 hours
            case "1w" -> 7 * 24 * 60 * 60 * 1000L; // 1 week
            case "2w" -> 14 * 24 * 60 * 60 * 1000L; // 2 weeks
            case "1m" -> 30L * 24 * 60 * 60 * 1000L; // 30 days (approximate month)
            default -> 24 * 60 * 60 * 1000L; // Default to 24 hours
        };
    }
    
    /**
     * Get payment history for a company.
     */
    public List<Map<String, Object>> getPaymentHistory(String companyId, int limit) throws SQLException {
        return database.query(
            "SELECT * FROM company_salary_payments WHERE company_id = ? ORDER BY payment_ts DESC LIMIT ?",
            companyId, limit
        );
    }
    
    /**
     * Get payment history for a player in a company.
     */
    public List<Map<String, Object>> getPlayerPaymentHistory(String companyId, String playerUuid, int limit) throws SQLException {
        return database.query(
            "SELECT * FROM company_salary_payments WHERE company_id = ? AND player_uuid = ? ORDER BY payment_ts DESC LIMIT ?",
            companyId, playerUuid, limit
        );
    }
    
    /**
     * Get all employees with their configured salaries.
     */
    public List<Map<String, Object>> getCompanySalaryInfo(String companyId) throws SQLException {
        // Get all employees with their job info
        List<Map<String, Object>> results = database.query(
            "SELECT ce.player_uuid, cj.id as job_id, cj.title as job_title, " +
            "cjs.salary_amount as job_salary, ces.salary_amount as player_salary " +
            "FROM company_employees ce " +
            "JOIN company_jobs cj ON ce.job_id = cj.id " +
            "LEFT JOIN company_job_salaries cjs ON cj.id = cjs.job_id " +
            "LEFT JOIN company_employee_salaries ces ON ce.company_id = ces.company_id AND ce.player_uuid = ces.player_uuid " +
            "WHERE ce.company_id = ?",
            companyId
        );
        
        return results;
    }
}
