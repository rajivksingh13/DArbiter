package ai.titli.darbiter.service;

import ai.titli.darbiter.model.Finding;
import ai.titli.darbiter.model.FindingCategory;
import ai.titli.darbiter.model.RemediationItem;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RemediationService {
    public List<RemediationItem> recommend(List<Finding> findings) {
        List<RemediationItem> recommendations = new ArrayList<>();
        for (Finding finding : findings) {
            List<String> actions = new ArrayList<>();
            if (finding.getCategory() == FindingCategory.SECRET) {
                actions.add("Rotate or revoke secret.");
                actions.add("Mask value in files.");
                actions.add("Exclude file from AI datasets.");
            } else if (finding.getCategory() == FindingCategory.PII) {
                actions.add("Mask or tokenize sensitive fields.");
                actions.add("Replace with synthetic data.");
                actions.add("Limit access to approved usage.");
            } else if (finding.getCategory() == FindingCategory.CONFIG_RISK) {
                actions.add("Harden configuration defaults.");
                actions.add("Disable insecure flags.");
                actions.add("Move secrets to vault.");
            }
            recommendations.add(new RemediationItem(finding.getId(), finding.getLabel(), actions));
        }
        return recommendations;
    }
}
