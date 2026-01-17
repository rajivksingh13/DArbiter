package ai.titli.darbiter.model;

import java.util.ArrayList;
import java.util.List;

public class EligibilityDecision {
    private AIEligibilityStatus status;
    private List<String> reasons = new ArrayList<>();
    private List<String> policyReferences = new ArrayList<>();

    public EligibilityDecision() {
    }

    public EligibilityDecision(AIEligibilityStatus status, List<String> reasons, List<String> policyReferences) {
        this.status = status;
        this.reasons = reasons == null ? new ArrayList<>() : reasons;
        this.policyReferences = policyReferences == null ? new ArrayList<>() : policyReferences;
    }

    public AIEligibilityStatus getStatus() {
        return status;
    }

    public void setStatus(AIEligibilityStatus status) {
        this.status = status;
    }

    public List<String> getReasons() {
        return reasons;
    }

    public void setReasons(List<String> reasons) {
        this.reasons = reasons == null ? new ArrayList<>() : reasons;
    }

    public List<String> getPolicyReferences() {
        return policyReferences;
    }

    public void setPolicyReferences(List<String> policyReferences) {
        this.policyReferences = policyReferences == null ? new ArrayList<>() : policyReferences;
    }
}
