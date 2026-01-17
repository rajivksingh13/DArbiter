package ai.titli.darbiter.model;

import java.util.ArrayList;
import java.util.List;

public class RuleSet {
    private String version;
    private String name;
    private List<RulePattern> patterns = new ArrayList<>();

    public RuleSet() {
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<RulePattern> getPatterns() {
        return patterns;
    }

    public void setPatterns(List<RulePattern> patterns) {
        this.patterns = patterns == null ? new ArrayList<>() : patterns;
    }
}
