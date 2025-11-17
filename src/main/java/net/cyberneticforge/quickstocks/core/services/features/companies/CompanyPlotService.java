package net.cyberneticforge.quickstocks.core.services.features.companies;

import lombok.Getter;
import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.core.model.Company;
import net.cyberneticforge.quickstocks.core.model.CompanyJob;
import net.cyberneticforge.quickstocks.core.model.CompanyPlot;
import net.cyberneticforge.quickstocks.core.model.PlotPermission;
import net.cyberneticforge.quickstocks.core.enums.HookType;
import net.cyberneticforge.quickstocks.infrastructure.config.CompanyCfg;
import net.cyberneticforge.quickstocks.infrastructure.db.Db;
import net.cyberneticforge.quickstocks.infrastructure.logging.PluginLogger;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.*;

/**
 * Service for managing company plots and land ownership.
 */
public class CompanyPlotService {
    
    private static final PluginLogger logger = QuickStocksPlugin.getPluginLogger();
    
    private final Db database = QuickStocksPlugin.getDatabaseManager().getDb();
    @Getter
    private final CompanyCfg config = QuickStocksPlugin.getCompanyCfg();
    
    /**
     * Buys a plot for a company at the specified location.
     */
    public CompanyPlot buyPlot(String companyId, String playerUuid, Location location, Player player) throws SQLException {
        if (!config.isPlotsEnabled()) {
            throw new IllegalStateException("Plot system is not enabled");
        }
        
        // Check WorldGuard region permissions if WorldGuard is hooked
        if (QuickStocksPlugin.getHookManager().isHooked(HookType.WorldGuard)) {
            if (!QuickStocksPlugin.getWorldGuardHook().canBuyPlot(player, location)) {
                throw new IllegalArgumentException("You cannot buy plots in this WorldGuard region");
            }
        }
        
        Chunk chunk = location.getChunk();
        String worldName = chunk.getWorld().getName();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();
        
        // Check if plot is already owned
        Optional<CompanyPlot> existingPlot = getPlotByLocation(worldName, chunkX, chunkZ);
        if (existingPlot.isPresent()) {
            throw new IllegalArgumentException("This plot is already owned by another company");
        }
        
        // Check if player has permission to buy plots
        Optional<CompanyJob> playerJob = QuickStocksPlugin.getCompanyService().getPlayerJob(companyId, playerUuid);
        if (playerJob.isEmpty() || !playerJob.get().canManagePlots()) {
            throw new IllegalArgumentException("You don't have permission to buy plots for this company");
        }
        
        // Get company and check balance
        Optional<Company> companyOpt = QuickStocksPlugin.getCompanyService().getCompanyById(companyId);
        if (companyOpt.isEmpty()) {
            throw new IllegalArgumentException("Company not found");
        }
        
        Company company = companyOpt.get();
        double buyPrice = config.getBuyPlotPrice();
        
        // Try to deduct from company balance with debt allowance
        boolean success = QuickStocksPlugin.getCompanyService().removeWithDebtAllowance(
            companyId, buyPrice, "PLOT_PURCHASE", "companyPlots"
        );
        
        if (!success) {
            double minAllowedBalance = config.getAllowedDebtPlots();
            throw new IllegalArgumentException("Insufficient company funds. Required: $" + 
                String.format("%.2f", buyPrice) + ", Available: $" + String.format("%.2f", company.getBalance()) +
                " (Min allowed: $" + String.format("%.2f", minAllowedBalance) + ")");
        }
        
        // Create plot record
        String plotId = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();
        double rentAmount = config.getPlotRent();
        String rentInterval = config.getPlotRentInterval();
        
        database.execute(
            "INSERT INTO company_plots (id, company_id, world_name, chunk_x, chunk_z, buy_price, purchased_at, rent_amount, rent_interval, last_rent_payment) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            plotId, companyId, worldName, chunkX, chunkZ, buyPrice, now, rentAmount, rentInterval, now
        );
        
        // Transaction was already recorded by removeWithDebtAllowance
        logger.info("Company " + companyId + " purchased plot at " + worldName + " (" + chunkX + ", " + chunkZ + ") for $" + buyPrice);
        
        // Apply default plot permissions
        applyDefaultPlotPermissions(plotId, companyId);
        
        return new CompanyPlot(plotId, companyId, worldName, chunkX, chunkZ, buyPrice, now, rentAmount, rentInterval, now);
    }
    
    /**
     * Sells a plot owned by a company at the specified location.
     */
    public void sellPlot(String companyId, String playerUuid, Location location) throws SQLException {
        if (!config.isPlotsEnabled()) {
            throw new IllegalStateException("Plot system is not enabled");
        }
        
        Chunk chunk = location.getChunk();
        String worldName = chunk.getWorld().getName();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();
        
        // Check if plot exists and is owned by this company
        Optional<CompanyPlot> plotOpt = getPlotByLocation(worldName, chunkX, chunkZ);
        if (plotOpt.isEmpty()) {
            throw new IllegalArgumentException("This plot is not owned by any company");
        }
        
        CompanyPlot plot = plotOpt.get();
        if (!plot.getCompanyId().equals(companyId)) {
            throw new IllegalArgumentException("This plot is not owned by your company");
        }
        
        // Check if player has permission to sell plots
        Optional<CompanyJob> playerJob = QuickStocksPlugin.getCompanyService().getPlayerJob(companyId, playerUuid);
        if (playerJob.isEmpty() || !playerJob.get().canManagePlots()) {
            throw new IllegalArgumentException("You don't have permission to sell plots for this company");
        }
        
        // Calculate refund amount
        double refundAmount = config.getSellPlotPrice();
        
        // Add refund to company balance
        database.execute(
            "UPDATE companies SET balance = balance + ? WHERE id = ?",
            refundAmount, companyId
        );
        
        // Delete plot record
        database.execute(
            "DELETE FROM company_plots WHERE id = ?",
            plot.getId()
        );
        
        // Record transaction
        String txId = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();
        database.execute(
            "INSERT INTO company_tx (id, company_id, player_uuid, type, amount, ts) VALUES (?, ?, ?, ?, ?, ?)",
            txId, companyId, playerUuid, "PLOT_SALE", refundAmount, now
        );
        
        logger.info("Company " + companyId + " sold plot at " + worldName + " (" + chunkX + ", " + chunkZ + ") for $" + refundAmount);
    }
    
    /**
     * Gets a plot by its location.
     */
    public Optional<CompanyPlot> getPlotByLocation(String worldName, int chunkX, int chunkZ) throws SQLException {
        List<Map<String, Object>> results = database.query(
            "SELECT id, company_id, world_name, chunk_x, chunk_z, buy_price, purchased_at, rent_amount, rent_interval, last_rent_payment " +
            "FROM company_plots WHERE world_name = ? AND chunk_x = ? AND chunk_z = ?",
            worldName, chunkX, chunkZ
        );
        
        if (results.isEmpty()) {
            return Optional.empty();
        }
        
        Map<String, Object> row = results.getFirst();
        return Optional.of(mapToPlot(row));
    }
    
    /**
     * Gets all plots owned by a company.
     */
    public List<CompanyPlot> getCompanyPlots(String companyId) throws SQLException {
        List<Map<String, Object>> results = database.query(
            "SELECT id, company_id, world_name, chunk_x, chunk_z, buy_price, purchased_at, rent_amount, rent_interval, last_rent_payment " +
            "FROM company_plots WHERE company_id = ? ORDER BY purchased_at DESC",
            companyId
        );
        
        List<CompanyPlot> plots = new ArrayList<>();
        for (Map<String, Object> row : results) {
            plots.add(mapToPlot(row));
        }
        
        return plots;
    }
    
    /**
     * Gets all plots that have rent due.
     */
    public List<CompanyPlot> getPlotsWithRentDue() throws SQLException {
        List<Map<String, Object>> results = database.query(
            "SELECT id, company_id, world_name, chunk_x, chunk_z, buy_price, purchased_at, rent_amount, rent_interval, last_rent_payment " +
            "FROM company_plots WHERE rent_amount >= 0 ORDER BY last_rent_payment ASC"
        );
        
        List<CompanyPlot> plotsWithRentDue = new ArrayList<>();
        for (Map<String, Object> row : results) {
            CompanyPlot plot = mapToPlot(row);
            if (plot.isRentDue()) {
                plotsWithRentDue.add(plot);
            }
        }
        
        return plotsWithRentDue;
    }
    
    /**
     * Collects rent for a plot.
     */
    public void collectRent(CompanyPlot plot) throws SQLException {
        if (!plot.hasRent()) {
            return; // No rent to collect
        }
        
        String companyId = plot.getCompanyId();
        double rentAmount = plot.getRentAmount();
        
        // Get company
        Optional<Company> companyOpt = QuickStocksPlugin.getCompanyService().getCompanyById(companyId);
        if (companyOpt.isEmpty()) {
            logger.warning("Company " + companyId + " not found for plot rent collection");
            return;
        }
        
        Company company = companyOpt.get();
        
        // Try to deduct rent from company balance with debt allowance
        boolean success = QuickStocksPlugin.getCompanyService().removeWithDebtAllowance(
            companyId, rentAmount, "PLOT_RENT", "companyPlots"
        );
        
        if (!success) {
            // Company cannot afford rent, trigger debt management
            logger.warning("Company " + companyId + " cannot afford rent for plot " + plot.getId() + ". Initiating plot seizure.");
            handleUnpaidRent(plot, company);
            return;
        }
        
        // Update last rent payment time
        long now = System.currentTimeMillis();
        database.execute(
            "UPDATE company_plots SET last_rent_payment = ? WHERE id = ?",
            now, plot.getId()
        );
        
        // Transaction was already recorded by removeWithDebtAllowance
        logger.debug("Collected $" + rentAmount + " rent from company " + companyId + " for plot " + plot.getId());
    }
    
    /**
     * Handles unpaid rent by seizing plots in order of purchase date (oldest first).
     */
    private void handleUnpaidRent(CompanyPlot plot, Company company) throws SQLException {
        // Get all plots for this company sorted by purchase date (oldest first)
        List<CompanyPlot> companyPlots = database.query(
            "SELECT id, company_id, world_name, chunk_x, chunk_z, buy_price, purchased_at, rent_amount, rent_interval, last_rent_payment " +
            "FROM company_plots WHERE company_id = ? ORDER BY purchased_at ASC",
            company.getId()
        ).stream().map(this::mapToPlot).toList();
        
        double minAllowedBalance = config.getAllowedDebtPlots();
        double currentBalance = company.getBalance();
        
        // Seize plots until company is within debt allowance
        for (CompanyPlot seizablePlot : companyPlots) {
            if (currentBalance >= minAllowedBalance) {
                break; // Company is now within debt allowance
            }
            
            // Delete plot (no refund for unpaid rent)
            database.execute(
                "DELETE FROM company_plots WHERE id = ?",
                seizablePlot.getId()
            );
            
            logger.info("Seized plot " + seizablePlot.getId() + " from company " + company.getId() + " due to unpaid rent");
            
            // If the plot had rent, add that back to the balance calculation
            // (since we won't be charging rent for it anymore)
            if (seizablePlot.hasRent()) {
                currentBalance += seizablePlot.getRentAmount();
            }
        }
    }
    
    /**
     * Processes rent collection for all plots.
     * Should be called periodically by a scheduled task.
     */
    public void processRentCollection() {
        try {
            List<CompanyPlot> plotsWithRentDue = getPlotsWithRentDue();
            
            for (CompanyPlot plot : plotsWithRentDue) {
                try {
                    collectRent(plot);
                } catch (Exception e) {
                    logger.warning("Error collecting rent for plot " + plot.getId() + ": " + e.getMessage());
                }
            }
            
            if (!plotsWithRentDue.isEmpty()) {
                logger.debug("Processed rent collection for " + plotsWithRentDue.size() + " plots");
            }
        } catch (Exception e) {
            logger.severe("Error in rent collection process: " + e.getMessage());
        }
    }
    
    /**
     * Toggles auto-buy mode for a player.
     */
    public void setAutoBuyMode(String playerUuid, String companyId, boolean enabled) throws SQLException {
        if (enabled) {
            // Insert or update
            database.execute(
                "INSERT OR REPLACE INTO player_auto_buy_mode (player_uuid, company_id, enabled) VALUES (?, ?, ?)",
                playerUuid, companyId, 1
            );
        } else {
            // Delete or set to 0
            database.execute(
                "DELETE FROM player_auto_buy_mode WHERE player_uuid = ?",
                playerUuid
            );
        }
        
        logger.debug("Set auto-buy mode to " + enabled + " for player " + playerUuid + " with company " + companyId);
    }
    
    /**
     * Gets the auto-buy mode status for a player.
     */
    public Optional<String> getAutoBuyMode(String playerUuid) throws SQLException {
        List<Map<String, Object>> results = database.query(
            "SELECT company_id, enabled FROM player_auto_buy_mode WHERE player_uuid = ?",
            playerUuid
        );
        
        if (results.isEmpty()) {
            return Optional.empty();
        }
        
        Map<String, Object> row = results.getFirst();
        int enabled = ((Number) row.get("enabled")).intValue();
        if (enabled == 0) {
            return Optional.empty();
        }
        
        return Optional.of((String) row.get("company_id"));
    }
    
    /**
     * Maps a database row to a CompanyPlot object.
     */
    private CompanyPlot mapToPlot(Map<String, Object> row) {
        Object lastRentPaymentObj = row.get("last_rent_payment");
        Long lastRentPayment = lastRentPaymentObj != null ? ((Number) lastRentPaymentObj).longValue() : null;
        
        return new CompanyPlot(
            (String) row.get("id"),
            (String) row.get("company_id"),
            (String) row.get("world_name"),
            ((Number) row.get("chunk_x")).intValue(),
            ((Number) row.get("chunk_z")).intValue(),
            ((Number) row.get("buy_price")).doubleValue(),
            ((Number) row.get("purchased_at")).longValue(),
            ((Number) row.get("rent_amount")).doubleValue(),
            (String) row.get("rent_interval"),
            lastRentPayment
        );
    }
    
    /**
     * Gets plot permissions for a specific job on a plot.
     */
    public Optional<PlotPermission> getPlotPermission(String plotId, String jobId) throws SQLException {
        List<Map<String, Object>> results = database.query(
            "SELECT id, plot_id, job_id, can_build, can_interact, can_container " +
            "FROM plot_permissions WHERE plot_id = ? AND job_id = ?",
            plotId, jobId
        );
        
        if (results.isEmpty()) {
            return Optional.empty();
        }
        
        Map<String, Object> row = results.getFirst();
        return Optional.of(new PlotPermission(
            (String) row.get("id"),
            (String) row.get("plot_id"),
            (String) row.get("job_id"),
            ((Number) row.get("can_build")).intValue() != 0,
            ((Number) row.get("can_interact")).intValue() != 0,
            ((Number) row.get("can_container")).intValue() != 0
        ));
    }
    
    /**
     * Gets all plot permissions for a plot.
     */
    public List<net.cyberneticforge.quickstocks.core.model.PlotPermission> getPlotPermissions(String plotId) throws SQLException {
        List<Map<String, Object>> results = database.query(
            "SELECT id, plot_id, job_id, can_build, can_interact, can_container " +
            "FROM plot_permissions WHERE plot_id = ? ORDER BY job_id",
            plotId
        );
        
        List<net.cyberneticforge.quickstocks.core.model.PlotPermission> permissions = new ArrayList<>();
        for (Map<String, Object> row : results) {
            permissions.add(new net.cyberneticforge.quickstocks.core.model.PlotPermission(
                (String) row.get("id"),
                (String) row.get("plot_id"),
                (String) row.get("job_id"),
                ((Number) row.get("can_build")).intValue() != 0,
                ((Number) row.get("can_interact")).intValue() != 0,
                ((Number) row.get("can_container")).intValue() != 0
            ));
        }
        
        return permissions;
    }
    
    /**
     * Sets plot permission for a specific job.
     */
    public void setPlotPermission(String plotId, String jobId, boolean canBuild, boolean canInteract, boolean canContainer) throws SQLException {
        // Check if permission already exists
        Optional<net.cyberneticforge.quickstocks.core.model.PlotPermission> existing = getPlotPermission(plotId, jobId);
        
        if (existing.isPresent()) {
            // Update existing permission
            database.execute(
                "UPDATE plot_permissions SET can_build = ?, can_interact = ?, can_container = ? " +
                "WHERE plot_id = ? AND job_id = ?",
                canBuild ? 1 : 0,
                canInteract ? 1 : 0,
                canContainer ? 1 : 0,
                plotId,
                jobId
            );
        } else {
            // Create new permission
            String permissionId = UUID.randomUUID().toString();
            database.execute(
                "INSERT INTO plot_permissions (id, plot_id, job_id, can_build, can_interact, can_container) " +
                "VALUES (?, ?, ?, ?, ?, ?)",
                permissionId,
                plotId,
                jobId,
                canBuild ? 1 : 0,
                canInteract ? 1 : 0,
                canContainer ? 1 : 0
            );
        }
        
        logger.debug("Set plot permission for job " + jobId + " on plot " + plotId);
    }
    
    /**
     * Checks if a player has a specific permission on a plot.
     */
    public boolean hasPlotPermission(String plotId, String playerUuid, String permissionType) throws SQLException {
        // Get player's job
        CompanyPlot plot = getPlotById(plotId).orElse(null);
        if (plot == null) {
            return false;
        }
        
        Optional<CompanyJob> playerJob = QuickStocksPlugin.getCompanyService().getPlayerJob(plot.getCompanyId(), playerUuid);
        if (playerJob.isEmpty()) {
            return false; // Not an employee
        }
        
        // Get plot permission for this job
        Optional<net.cyberneticforge.quickstocks.core.model.PlotPermission> permission = getPlotPermission(plotId, playerJob.get().getId());

        // No specific permission set, use default (allow all for employees)
        return permission.map(plotPermission -> switch (permissionType.toLowerCase()) {
            case "build" -> plotPermission.canBuild();
            case "interact" -> plotPermission.canInteract();
            case "container" -> plotPermission.canContainer();
            default -> false;
        }).orElse(true);
        
        // Check specific permission
    }

    private Optional<CompanyPlot> getPlotById(String plotId) {
        try {
            List<Map<String, Object>> results = database.query(
                "SELECT id, company_id, world_name, chunk_x, chunk_z, buy_price, purchased_at, rent_amount, rent_interval, last_rent_payment " +
                "FROM company_plots WHERE id = ?",
                plotId
            );

            if (results.isEmpty()) {
                return Optional.empty();
            }

            Map<String, Object> row = results.getFirst();
            return Optional.of(mapToPlot(row));
        } catch (SQLException e) {
            logger.severe("Error retrieving plot by ID: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Gets nearby plots within a radius (in chunks).
     */
    public List<CompanyPlot> getNearbyPlots(Location location, int radiusChunks) throws SQLException {
        Chunk centerChunk = location.getChunk();
        String worldName = centerChunk.getWorld().getName();
        int centerX = centerChunk.getX();
        int centerZ = centerChunk.getZ();
        
        List<CompanyPlot> nearbyPlots = new ArrayList<>();
        
        // Query plots within the radius
        for (int x = centerX - radiusChunks; x <= centerX + radiusChunks; x++) {
            for (int z = centerZ - radiusChunks; z <= centerZ + radiusChunks; z++) {
                Optional<CompanyPlot> plot = getPlotByLocation(worldName, x, z);
                plot.ifPresent(nearbyPlots::add);
            }
        }
        
        return nearbyPlots;
    }
    
    /**
     * Applies default plot permissions when a plot is bought.
     */
    public void applyDefaultPlotPermissions(String plotId, String companyId) throws SQLException {
        // Get all jobs for the company
        List<CompanyJob> jobs = QuickStocksPlugin.getCompanyService().getCompanyJobs(companyId);
        
        // Apply default permissions (all allowed for all jobs by default)
        for (CompanyJob job : jobs) {
            setPlotPermission(plotId, job.getId(), true, true, true);
        }
        
        logger.debug("Applied default plot permissions for plot " + plotId);
    }
}
