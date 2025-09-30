package com.example.quickstocks.core.model;

/**
 * Represents an invitation to join a company.
 */
public class CompanyInvitation {
    private final String id;
    private final String companyId;
    private final String inviterUuid;
    private final String inviteeUuid;
    private final String jobId;
    private final long createdAt;
    private final long expiresAt;
    private final InvitationStatus status;
    
    public CompanyInvitation(String id, String companyId, String inviterUuid, String inviteeUuid, 
                            String jobId, long createdAt, long expiresAt, InvitationStatus status) {
        this.id = id;
        this.companyId = companyId;
        this.inviterUuid = inviterUuid;
        this.inviteeUuid = inviteeUuid;
        this.jobId = jobId;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.status = status;
    }
    
    public String getId() {
        return id;
    }
    
    public String getCompanyId() {
        return companyId;
    }
    
    public String getInviterUuid() {
        return inviterUuid;
    }
    
    public String getInviteeUuid() {
        return inviteeUuid;
    }
    
    public String getJobId() {
        return jobId;
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public long getExpiresAt() {
        return expiresAt;
    }
    
    public InvitationStatus getStatus() {
        return status;
    }
    
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
