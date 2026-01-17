package ai.titli.darbiter.model;

public class RulePattern {
    private String id;
    private String label;
    private String regex;
    private RiskLevel severity;
    private FindingCategory category;

    public RulePattern() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getRegex() {
        return regex;
    }

    public void setRegex(String regex) {
        this.regex = regex;
    }

    public RiskLevel getSeverity() {
        return severity;
    }

    public void setSeverity(RiskLevel severity) {
        this.severity = severity;
    }

    public FindingCategory getCategory() {
        return category;
    }

    public void setCategory(FindingCategory category) {
        this.category = category;
    }
}
