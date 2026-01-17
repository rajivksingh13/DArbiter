package ai.titli.darbiter.model;

import java.util.ArrayList;
import java.util.List;

public class RemediationItem {
    private String findingId;
    private String label;
    private List<String> actions = new ArrayList<>();

    public RemediationItem() {
    }

    public RemediationItem(String findingId, String label, List<String> actions) {
        this.findingId = findingId;
        this.label = label;
        this.actions = actions == null ? new ArrayList<>() : actions;
    }

    public String getFindingId() {
        return findingId;
    }

    public void setFindingId(String findingId) {
        this.findingId = findingId;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public List<String> getActions() {
        return actions;
    }

    public void setActions(List<String> actions) {
        this.actions = actions == null ? new ArrayList<>() : actions;
    }
}
