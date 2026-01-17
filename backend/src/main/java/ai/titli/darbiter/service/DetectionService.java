package ai.titli.darbiter.service;

import ai.titli.darbiter.model.Finding;
import ai.titli.darbiter.model.RulePattern;
import ai.titli.darbiter.model.RuleSet;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DetectionService {
    private static final int MAX_BYTES = 2_000_000;

    public List<Finding> detect(Path file, RuleSet ruleSet, Set<ai.titli.darbiter.model.FindingCategory> categories) {
        List<Finding> findings = new ArrayList<>();
        if (!Files.isRegularFile(file) || isLikelyBinary(file)) {
            return findings;
        }

        List<CompiledRule> compiledRules = compile(ruleSet, categories);
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            int totalBytes = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                totalBytes += line.getBytes(StandardCharsets.UTF_8).length;
                if (totalBytes > MAX_BYTES) {
                    break;
                }
                for (CompiledRule compiledRule : compiledRules) {
                    Matcher matcher = compiledRule.pattern.matcher(line);
                    while (matcher.find()) {
                        String snippet = trimSnippet(line, matcher.start(), matcher.end());
                        findings.add(new Finding(
                                compiledRule.rule.getId(),
                                compiledRule.rule.getCategory(),
                                compiledRule.rule.getLabel(),
                                compiledRule.rule.getSeverity(),
                                file.toString(),
                                lineNumber,
                                snippet
                        ));
                    }
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read file: " + file, ex);
        }
        return findings;
    }

    public List<Finding> detectText(String content, RuleSet ruleSet,
                                    Set<ai.titli.darbiter.model.FindingCategory> categories,
                                    String sourceLabel) {
        List<Finding> findings = new ArrayList<>();
        if (content == null || content.isBlank()) {
            return findings;
        }
        List<CompiledRule> compiledRules = compile(ruleSet, categories);
        String[] lines = content.split("\\R");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int lineNumber = i + 1;
            for (CompiledRule compiledRule : compiledRules) {
                Matcher matcher = compiledRule.pattern.matcher(line);
                while (matcher.find()) {
                    String snippet = trimSnippet(line, matcher.start(), matcher.end());
                    findings.add(new Finding(
                            compiledRule.rule.getId(),
                            compiledRule.rule.getCategory(),
                            compiledRule.rule.getLabel(),
                            compiledRule.rule.getSeverity(),
                            sourceLabel,
                            lineNumber,
                            snippet
                    ));
                }
            }
        }
        return findings;
    }

    public List<Finding> detectStructured(List<ai.titli.darbiter.model.StructuredField> fields,
                                          RuleSet ruleSet,
                                          Set<ai.titli.darbiter.model.FindingCategory> categories,
                                          String sourceLabel) {
        List<Finding> findings = new ArrayList<>();
        if (fields == null || fields.isEmpty()) {
            return findings;
        }
        List<CompiledRule> compiledRules = compile(ruleSet, categories);
        for (ai.titli.darbiter.model.StructuredField field : fields) {
            String haystack = field.getPath() + "=" + field.getValue();
            for (CompiledRule compiledRule : compiledRules) {
                Matcher matcher = compiledRule.pattern.matcher(haystack);
                while (matcher.find()) {
                    String snippet = trimSnippet(haystack, matcher.start(), matcher.end());
                    int lineNumber = field.getLine() > 0 ? field.getLine() : field.getIndex();
                    findings.add(new Finding(
                            compiledRule.rule.getId(),
                            compiledRule.rule.getCategory(),
                            compiledRule.rule.getLabel(),
                            compiledRule.rule.getSeverity(),
                            sourceLabel + " :: " + field.getPath(),
                            lineNumber,
                            snippet
                    ));
                }
            }
        }
        return findings;
    }

    private List<CompiledRule> compile(RuleSet ruleSet, Set<ai.titli.darbiter.model.FindingCategory> categories) {
        List<CompiledRule> compiledRules = new ArrayList<>();
        for (RulePattern rule : ruleSet.getPatterns()) {
            if (categories != null && !categories.contains(rule.getCategory())) {
                continue;
            }
            Pattern pattern = Pattern.compile(rule.getRegex());
            compiledRules.add(new CompiledRule(rule, pattern));
        }
        return compiledRules;
    }

    private boolean isLikelyBinary(Path file) {
        try (BufferedInputStream stream = new BufferedInputStream(Files.newInputStream(file))) {
            int read = 0;
            while (read < 1024) {
                int b = stream.read();
                if (b == -1) {
                    break;
                }
                if (b == 0) {
                    return true;
                }
                read++;
            }
        } catch (IOException ex) {
            return true;
        }
        return false;
    }

    private String trimSnippet(String line, int start, int end) {
        int left = Math.max(0, start - 20);
        int right = Math.min(line.length(), end + 20);
        return line.substring(left, right).trim();
    }

    private static class CompiledRule {
        private final RulePattern rule;
        private final Pattern pattern;

        private CompiledRule(RulePattern rule, Pattern pattern) {
            this.rule = rule;
            this.pattern = pattern;
        }
    }
}
