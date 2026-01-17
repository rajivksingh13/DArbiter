package ai.titli.darbiter.model;

public class Finding {
    private String id;
    private FindingCategory category;
    private String label;
    private RiskLevel severity;
    private String filePath;
    private int lineNumber;
    private String snippet;

    public Finding() {
    }

    public Finding(String id, FindingCategory category, String label, RiskLevel severity,
                   String filePath, int lineNumber, String snippet) {
        this.id = id;
        this.category = category;
        this.label = label;
        this.severity = severity;
        this.filePath = filePath;
        this.lineNumber = lineNumber;
        this.snippet = snippet;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public FindingCategory getCategory() {
        return category;
    }

    public void setCategory(FindingCategory category) {
        this.category = category;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public RiskLevel getSeverity() {
        return severity;
    }

    public void setSeverity(RiskLevel severity) {
        this.severity = severity;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getSnippet() {
        return snippet;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }
}
