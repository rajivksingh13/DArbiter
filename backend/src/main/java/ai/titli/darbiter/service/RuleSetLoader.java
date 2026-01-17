package ai.titli.darbiter.service;

import ai.titli.darbiter.model.RuleSet;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Component
public class RuleSetLoader {
    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    public RuleSet load(String rulesetFile) {
        String safeName = rulesetFile == null || rulesetFile.isBlank()
                ? "pii_baseline.yaml"
                : rulesetFile;
        String resourcePath = "rulesets/" + safeName;
        try (InputStream inputStream = new ClassPathResource(resourcePath).getInputStream()) {
            return mapper.readValue(inputStream, RuleSet.class);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to load ruleset: " + resourcePath, ex);
        }
    }
}
