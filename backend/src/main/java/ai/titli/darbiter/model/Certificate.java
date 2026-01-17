package ai.titli.darbiter.model;

import java.time.Instant;

public class Certificate {
    private String scanId;
    private String scope;
    private AIEligibilityStatus status;
    private AIUsage usage;
    private String ruleset;
    private Instant issuedAt;

    public Certificate() {
    }

    public Certificate(String scanId, String scope, AIEligibilityStatus status, AIUsage usage, String ruleset,
                       Instant issuedAt) {
        this.scanId = scanId;
        this.scope = scope;
        this.status = status;
        this.usage = usage;
        this.ruleset = ruleset;
        this.issuedAt = issuedAt;
    }

    public String getScanId() {
        return scanId;
    }

    public void setScanId(String scanId) {
        this.scanId = scanId;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public AIEligibilityStatus getStatus() {
        return status;
    }

    public void setStatus(AIEligibilityStatus status) {
        this.status = status;
    }

    public AIUsage getUsage() {
        return usage;
    }

    public void setUsage(AIUsage usage) {
        this.usage = usage;
    }

    public String getRuleset() {
        return ruleset;
    }

    public void setRuleset(String ruleset) {
        this.ruleset = ruleset;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Instant issuedAt) {
        this.issuedAt = issuedAt;
    }
}
