package ai.titli.darbiter.model;

import jakarta.validation.constraints.NotBlank;

import java.util.EnumSet;
import java.util.Set;

public class TextScanRequest {
    @NotBlank
    private String content;
    private boolean approvedForAi;
    private String ruleset = "combined_baseline.yaml";
    private AIUsage usage = AIUsage.INFERENCE;
    private Set<FindingCategory> categories = EnumSet.allOf(FindingCategory.class);

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isApprovedForAi() {
        return approvedForAi;
    }

    public void setApprovedForAi(boolean approvedForAi) {
        this.approvedForAi = approvedForAi;
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

    public Set<FindingCategory> getCategories() {
        return categories;
    }

    public void setCategories(Set<FindingCategory> categories) {
        this.categories = categories == null || categories.isEmpty()
                ? EnumSet.allOf(FindingCategory.class)
                : categories;
    }
}
