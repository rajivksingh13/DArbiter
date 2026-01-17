package ai.titli.darbiter.service;

import ai.titli.darbiter.model.Finding;
import ai.titli.darbiter.model.RiskLevel;
import ai.titli.darbiter.model.RiskSummary;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RiskClassifier {
    public RiskSummary summarize(List<Finding> findings) {
        RiskSummary summary = new RiskSummary();
        int critical = 0;
        int high = 0;
        int medium = 0;
        int low = 0;
        for (Finding finding : findings) {
            if (finding.getSeverity() == RiskLevel.CRITICAL) {
                critical++;
            } else if (finding.getSeverity() == RiskLevel.HIGH) {
                high++;
            } else if (finding.getSeverity() == RiskLevel.MEDIUM) {
                medium++;
            } else {
                low++;
            }
        }
        summary.setCritical(critical);
        summary.setHigh(high);
        summary.setMedium(medium);
        summary.setLow(low);
        summary.setTotalFindings(findings.size());
        summary.setOverall(overall(critical, high, medium, low));
        return summary;
    }

    private RiskLevel overall(int critical, int high, int medium, int low) {
        if (critical > 0) {
            return RiskLevel.CRITICAL;
        }
        if (high > 0) {
            return RiskLevel.HIGH;
        }
        if (medium > 0) {
            return RiskLevel.MEDIUM;
        }
        return low > 0 ? RiskLevel.LOW : RiskLevel.LOW;
    }
}
