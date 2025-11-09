package net.cyberneticforge.quickstocks.core.services.features.companies;

import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.core.model.CompanyInvitation;
import net.cyberneticforge.quickstocks.core.model.CompanyJob;
import net.cyberneticforge.quickstocks.infrastructure.db.Db;
import net.cyberneticforge.quickstocks.infrastructure.logging.PluginLogger;

import java.sql.SQLException;
import java.util.*;

/**
 * Service for managing company invitations.
 */
@SuppressWarnings("unused")
public class InvitationService {
    
    private static final PluginLogger logger = QuickStocksPlugin.getPluginLogger();
    private static final long INVITATION_EXPIRY_MS = 7 * 24 * 60 * 60 * 1000L; // 7 days
    
    private final Db database = QuickStocksPlugin.getDatabaseManager().getDb();
    
    /**
     * Creates an invitation to join a company.
     */
    @SuppressWarnings("UnusedReturnValue")
    public CompanyInvitation createInvitation(String companyId, String inviterUuid,
                                              String inviteeUuid, String jobTitle) throws SQLException {
        // Check if inviter has permission
        Optional<CompanyJob> inviterJob = QuickStocksPlugin.getCompanyService().getPlayerJob(companyId, inviterUuid);
        if (inviterJob.isEmpty() || !inviterJob.get().canInvite()) {
            throw new IllegalArgumentException("Player does not have permission to invite");
        }
        
        // Check if target job exists
        Optional<CompanyJob> targetJob = QuickStocksPlugin.getCompanyService().getJobByTitle(companyId, jobTitle);
        if (targetJob.isEmpty()) {
            throw new IllegalArgumentException("Job title does not exist");
        }
        
        // Check if player is already an employee
        Optional<CompanyJob> existingJob = QuickStocksPlugin.getCompanyService().getPlayerJob(companyId, inviteeUuid);
        if (existingJob.isPresent()) {
            throw new IllegalArgumentException("Player is already an employee of this company");
        }
        
        // Check for existing pending invitation
        List<Map<String, Object>> existing = database.query(
            "SELECT id FROM company_invitations WHERE company_id = ? AND invitee_uuid = ? AND status = 'PENDING'",
            companyId, inviteeUuid
        );
        if (!existing.isEmpty()) {
            throw new IllegalArgumentException("Player already has a pending invitation to this company");
        }
        
        // Create invitation
        String invitationId = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();
        long expiresAt = now + INVITATION_EXPIRY_MS;
        
        database.execute(
            "INSERT INTO company_invitations (id, company_id, inviter_uuid, invitee_uuid, job_id, created_at, expires_at, status) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            invitationId, companyId, inviterUuid, inviteeUuid, targetJob.get().getId(), 
            now, expiresAt, "PENDING"
        );
        
        logger.info("Created invitation " + invitationId + " for player " + inviteeUuid + " to company " + companyId);
        
        return new CompanyInvitation(invitationId, companyId, inviterUuid, inviteeUuid, 
                                    targetJob.get().getId(), now, expiresAt, 
                                    CompanyInvitation.InvitationStatus.PENDING);
    }
    
    /**
     * Accepts an invitation.
     */
    public void acceptInvitation(String invitationId, String playerUuid) throws SQLException {
        // Get invitation
        Optional<CompanyInvitation> invitationOpt = getInvitationById(invitationId);
        if (invitationOpt.isEmpty()) {
            throw new IllegalArgumentException("Invitation not found");
        }
        
        CompanyInvitation invitation = invitationOpt.get();
        
        // Verify it's for this player
        if (!invitation.inviteeUuid().equals(playerUuid)) {
            throw new IllegalArgumentException("This invitation is not for you");
        }
        
        // Check status
        if (invitation.status() != CompanyInvitation.InvitationStatus.PENDING) {
            throw new IllegalArgumentException("Invitation is no longer active");
        }
        
        // Check expiry
        if (System.currentTimeMillis() > invitation.expiresAt()) {
            updateInvitationStatus(invitationId, CompanyInvitation.InvitationStatus.EXPIRED);
            throw new IllegalArgumentException("Invitation has expired");
        }
        
        // Add player as employee
        long now = System.currentTimeMillis();
        database.execute(
            "INSERT INTO company_employees (company_id, player_uuid, job_id, joined_at) VALUES (?, ?, ?, ?)",
            invitation.companyId(), playerUuid, invitation.jobId(), now
        );
        
        // Update invitation status
        updateInvitationStatus(invitationId, CompanyInvitation.InvitationStatus.ACCEPTED);
        
        logger.info("Player " + playerUuid + " accepted invitation to company " + invitation.companyId());
    }
    
    /**
     * Declines an invitation.
     */
    public void declineInvitation(String invitationId, String playerUuid) throws SQLException {
        // Get invitation
        Optional<CompanyInvitation> invitationOpt = getInvitationById(invitationId);
        if (invitationOpt.isEmpty()) {
            throw new IllegalArgumentException("Invitation not found");
        }
        
        CompanyInvitation invitation = invitationOpt.get();
        
        // Verify it's for this player
        if (!invitation.inviteeUuid().equals(playerUuid)) {
            throw new IllegalArgumentException("This invitation is not for you");
        }
        
        // Update invitation status
        updateInvitationStatus(invitationId, CompanyInvitation.InvitationStatus.DECLINED);
        
        logger.info("Player " + playerUuid + " declined invitation to company " + invitation.companyId());
    }
    
    /**
     * Cancels an invitation.
     */
    public void cancelInvitation(String invitationId, String cancellerUuid) throws SQLException {
        // Get invitation
        Optional<CompanyInvitation> invitationOpt = getInvitationById(invitationId);
        if (invitationOpt.isEmpty()) {
            throw new IllegalArgumentException("Invitation not found");
        }
        
        CompanyInvitation invitation = invitationOpt.get();
        
        // Check if canceller has permission (must be inviter or have manage permission)
        Optional<CompanyJob> job = QuickStocksPlugin.getCompanyService().getPlayerJob(invitation.companyId(), cancellerUuid);
        boolean isInviter = invitation.inviterUuid().equals(cancellerUuid);
        boolean canManage = job.isPresent() && job.get().canManageCompany();
        
        if (!isInviter && !canManage) {
            throw new IllegalArgumentException("You do not have permission to cancel this invitation");
        }
        
        // Update invitation status
        updateInvitationStatus(invitationId, CompanyInvitation.InvitationStatus.CANCELLED);
        
        logger.info("Invitation " + invitationId + " cancelled by " + cancellerUuid);
    }
    
    /**
     * Gets pending invitations for a player.
     */
    public List<CompanyInvitation> getPendingInvitations(String playerUuid) throws SQLException {
        List<Map<String, Object>> results = database.query(
            "SELECT id, company_id, inviter_uuid, invitee_uuid, job_id, created_at, expires_at, status " +
            "FROM company_invitations WHERE invitee_uuid = ? AND status = 'PENDING' AND expires_at > ?",
            playerUuid, System.currentTimeMillis()
        );
        
        List<CompanyInvitation> invitations = new ArrayList<>();
        for (Map<String, Object> row : results) {
            invitations.add(new CompanyInvitation(
                (String) row.get("id"),
                (String) row.get("company_id"),
                (String) row.get("inviter_uuid"),
                (String) row.get("invitee_uuid"),
                (String) row.get("job_id"),
                ((Number) row.get("created_at")).longValue(),
                ((Number) row.get("expires_at")).longValue(),
                CompanyInvitation.InvitationStatus.valueOf((String) row.get("status"))
            ));
        }
        
        return invitations;
    }
    
    /**
     * Gets an invitation by ID.
     */
    public Optional<CompanyInvitation> getInvitationById(String invitationId) throws SQLException {
        List<Map<String, Object>> results = database.query(
            "SELECT id, company_id, inviter_uuid, invitee_uuid, job_id, created_at, expires_at, status " +
            "FROM company_invitations WHERE id = ?",
            invitationId
        );
        
        if (results.isEmpty()) {
            return Optional.empty();
        }
        
        Map<String, Object> row = results.getFirst();
        return Optional.of(new CompanyInvitation(
            (String) row.get("id"),
            (String) row.get("company_id"),
            (String) row.get("inviter_uuid"),
            (String) row.get("invitee_uuid"),
            (String) row.get("job_id"),
            ((Number) row.get("created_at")).longValue(),
            ((Number) row.get("expires_at")).longValue(),
            CompanyInvitation.InvitationStatus.valueOf((String) row.get("status"))
        ));
    }
    
    /**
     * Updates invitation status.
     */
    private void updateInvitationStatus(String invitationId, CompanyInvitation.InvitationStatus status) throws SQLException {
        database.execute(
            "UPDATE company_invitations SET status = ? WHERE id = ?",
            status.name(), invitationId
        );
    }
    
    /**
     * Cleans up expired invitations.
     */
    public void cleanupExpiredInvitations() throws SQLException {
        long now = System.currentTimeMillis();
        database.execute(
            "UPDATE company_invitations SET status = 'EXPIRED' WHERE status = 'PENDING' AND expires_at < ?",
            now
        );
    }
}
