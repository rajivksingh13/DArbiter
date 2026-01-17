package ai.titli.darbiter.service;

import ai.titli.darbiter.model.AIEligibilityStatus;
import ai.titli.darbiter.model.EligibilityDecision;
import ai.titli.darbiter.model.Finding;
import ai.titli.darbiter.model.FindingCategory;
import ai.titli.darbiter.model.RiskLevel;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EligibilityEvaluator {
    public EligibilityDecision evaluate(List<Finding> findings, boolean approvedForAi) {
        boolean hasSecrets = findings.stream().anyMatch(f -> f.getCategory() == FindingCategory.SECRET);
        boolean hasPii = findings.stream().anyMatch(f -> f.getCategory() == FindingCategory.PII);
        boolean hasCriticalConfig = findings.stream()
                .anyMatch(f -> f.getCategory() == FindingCategory.CONFIG_RISK
                        && f.getSeverity() == RiskLevel.CRITICAL);

        java.util.List<String> reasons = new java.util.ArrayList<>();
        java.util.List<String> policies = new java.util.ArrayList<>();

        if (hasSecrets) {
            reasons.add("Secrets detected in content or configuration.");
            policies.add("POL-SEC-001 Secrets must be removed before AI usage.");
            return new EligibilityDecision(AIEligibilityStatus.NOT_AI_SAFE, reasons, policies);
        }
        if (hasPii && !approvedForAi) {
            reasons.add("PII detected without explicit approval.");
            policies.add("POL-PII-002 PII requires approval for AI usage.");
            return new EligibilityDecision(AIEligibilityStatus.CONDITIONAL, reasons, policies);
        }
        if (hasCriticalConfig) {
            reasons.add("Critical config risks detected.");
            policies.add("POL-CONFIG-003 Secure configuration required for AI usage.");
            return new EligibilityDecision(AIEligibilityStatus.RESTRICTED, reasons, policies);
        }
        reasons.add("No blocking findings detected.");
        policies.add("POL-BASE-000 Baseline eligibility passed.");
        return new EligibilityDecision(AIEligibilityStatus.AI_SAFE, reasons, policies);
    }
}
