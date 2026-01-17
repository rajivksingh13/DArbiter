package ai.titli.darbiter.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ScanResult {
    private String scanId;
    private String ruleset;
    private AIUsage usage;
    private Instant startedAt;
    private Instant finishedAt;
    private List<Finding> findings = new ArrayList<>();
    private RiskSummary riskSummary;
    private AIEligibilityStatus eligibility;
    private EligibilityDecision decision;
    private List<RemediationItem> remediation = new ArrayList<>();

    public String getScanId() {
        return scanId;
    }

    public void setScanId(String scanId) {
        this.scanId = scanId;
    }

    public String getRuleset() {
        return ruleset;
    }

    public void setRuleset(String ruleset) {
        this.ruleset = ruleset;
    }

    public AIUsage getUsage() {
        return usage;
    }

    public void setUsage(AIUsage usage) {
        this.usage = usage;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    public List<Finding> getFindings() {
        return findings;
    }

    public void setFindings(List<Finding> findings) {
        this.findings = findings == null ? new ArrayList<>() : findings;
    }

    public RiskSummary getRiskSummary() {
        return riskSummary;
    }

    public void setRiskSummary(RiskSummary riskSummary) {
        this.riskSummary = riskSummary;
    }

    public AIEligibilityStatus getEligibility() {
        return eligibility;
    }

    public void setEligibility(AIEligibilityStatus eligibility) {
        this.eligibility = eligibility;
    }

    public EligibilityDecision getDecision() {
        return decision;
    }

    public void setDecision(EligibilityDecision decision) {
        this.decision = decision;
    }

    public List<RemediationItem> getRemediation() {
        return remediation;
    }

    public void setRemediation(List<RemediationItem> remediation) {
        this.remediation = remediation == null ? new ArrayList<>() : remediation;
    }
}
