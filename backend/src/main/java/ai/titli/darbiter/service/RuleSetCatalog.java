package ai.titli.darbiter.service;

import ai.titli.darbiter.model.RuleSet;
import ai.titli.darbiter.model.RuleSetInfo;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RuleSetCatalog {
    private final RuleSetLoader loader;
    private final List<String> defaultFiles = List.of(
            "combined_baseline.yaml",
            "pii_baseline.yaml",
            "secrets_baseline.yaml",
            "config_risk_baseline.yaml"
    );

    public RuleSetCatalog(RuleSetLoader loader) {
        this.loader = loader;
    }

    public List<RuleSetInfo> list() {
        List<RuleSetInfo> infos = new ArrayList<>();
        for (String file : defaultFiles) {
            RuleSet ruleSet = loader.load(file);
            infos.add(new RuleSetInfo(file, ruleSet.getName(), ruleSet.getVersion()));
        }
        return infos;
    }
}
