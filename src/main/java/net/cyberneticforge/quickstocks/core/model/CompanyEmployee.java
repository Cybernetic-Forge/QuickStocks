package net.cyberneticforge.quickstocks.core.model;

/**
 * Represents an employee/member of a company.
 */
public class CompanyEmployee {
    private final String companyId;
    private final String playerUuid;
    private final String jobId;
    private final long joinedAt;
    
    public CompanyEmployee(String companyId, String playerUuid, String jobId, long joinedAt) {
        this.companyId = companyId;
        this.playerUuid = playerUuid;
        this.jobId = jobId;
        this.joinedAt = joinedAt;
    }
    
    public String getCompanyId() {
        return companyId;
    }
    
    public String getPlayerUuid() {
        return playerUuid;
    }
    
    public String getJobId() {
        return jobId;
    }
    
    public long getJoinedAt() {
        return joinedAt;
    }
}
