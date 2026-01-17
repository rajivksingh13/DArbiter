package ai.titli.darbiter.model;

public class RiskSummary {
    private int totalFindings;
    private int critical;
    private int high;
    private int medium;
    private int low;
    private RiskLevel overall;

    public int getTotalFindings() {
        return totalFindings;
    }

    public void setTotalFindings(int totalFindings) {
        this.totalFindings = totalFindings;
    }

    public int getCritical() {
        return critical;
    }

    public void setCritical(int critical) {
        this.critical = critical;
    }

    public int getHigh() {
        return high;
    }

    public void setHigh(int high) {
        this.high = high;
    }

    public int getMedium() {
        return medium;
    }

    public void setMedium(int medium) {
        this.medium = medium;
    }

    public int getLow() {
        return low;
    }

    public void setLow(int low) {
        this.low = low;
    }

    public RiskLevel getOverall() {
        return overall;
    }

    public void setOverall(RiskLevel overall) {
        this.overall = overall;
    }
}
