package ai.titli.darbiter.model;

import jakarta.validation.constraints.NotBlank;

public class PathScanRequest {
    @NotBlank
    private String path;
    private boolean recursive = true;
    private boolean approvedForAi;
    private String ruleset = "combined_baseline.yaml";
    private AIUsage usage = AIUsage.INFERENCE;
    private java.util.Set<FindingCategory> categories = java.util.EnumSet.allOf(FindingCategory.class);

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isRecursive() {
        return recursive;
    }

    public void setRecursive(boolean recursive) {
        this.recursive = recursive;
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

    public java.util.Set<FindingCategory> getCategories() {
        return categories;
    }

    public void setCategories(java.util.Set<FindingCategory> categories) {
        this.categories = categories == null || categories.isEmpty()
                ? java.util.EnumSet.allOf(FindingCategory.class)
                : categories;
    }
}
