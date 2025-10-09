package net.cyberneticforge.quickstocks.core.model;

/**
 * Represents an invitation to join a company.
 */
public record CompanyInvitation(String id, String companyId, String inviterUuid, String inviteeUuid, String jobId,
                                long createdAt, long expiresAt,
                                net.cyberneticforge.quickstocks.core.model.CompanyInvitation.InvitationStatus status) {

    /**
     * Status of a company invitation.
     */
    public enum InvitationStatus {
        PENDING,
        ACCEPTED,
        DECLINED,
        EXPIRED,
        CANCELLED
    }
}
