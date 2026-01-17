package ai.titli.darbiter.service;

import ai.titli.darbiter.model.AIUsage;
import ai.titli.darbiter.model.EligibilityDecision;
import ai.titli.darbiter.model.Finding;
import ai.titli.darbiter.model.PathScanRequest;
import ai.titli.darbiter.model.RuleSet;
import ai.titli.darbiter.model.ScanResult;
import ai.titli.darbiter.model.TextScanRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class ScanService {
    private final RuleSetLoader ruleSetLoader;
    private final DetectionService detectionService;
    private final RiskClassifier riskClassifier;
    private final EligibilityEvaluator eligibilityEvaluator;
    private final RemediationService remediationService;
    private final FileContentExtractor fileContentExtractor;
    private final ScanStore scanStore;

    public ScanService(RuleSetLoader ruleSetLoader,
                       DetectionService detectionService,
                       RiskClassifier riskClassifier,
                       EligibilityEvaluator eligibilityEvaluator,
                       RemediationService remediationService,
                       FileContentExtractor fileContentExtractor,
                       ScanStore scanStore) {
        this.ruleSetLoader = ruleSetLoader;
        this.detectionService = detectionService;
        this.riskClassifier = riskClassifier;
        this.eligibilityEvaluator = eligibilityEvaluator;
        this.remediationService = remediationService;
        this.fileContentExtractor = fileContentExtractor;
        this.scanStore = scanStore;
    }

    public ScanResult scanPath(PathScanRequest request) {
        RuleSet ruleSet = ruleSetLoader.load(request.getRuleset());
        List<Finding> findings = new ArrayList<>();
        Path root = Path.of(request.getPath());
        if (Files.isDirectory(root)) {
            try (Stream<Path> paths = request.isRecursive() ? Files.walk(root) : Files.list(root)) {
                paths.filter(Files::isRegularFile)
                        .forEach(file -> {
                            List<ai.titli.darbiter.model.StructuredField> fields =
                                    fileContentExtractor.extractStructured(file);
                            if (fields != null && !fields.isEmpty()) {
                                findings.addAll(detectionService.detectStructured(
                                        fields, ruleSet, request.getCategories(), file.toString()));
                            } else {
                                String content = fileContentExtractor.extract(file);
                                findings.addAll(detectionService.detectText(
                                        content, ruleSet, request.getCategories(), file.toString()));
                            }
                        });
            } catch (IOException ex) {
                throw new IllegalStateException("Failed to scan directory: " + root, ex);
            }
        } else if (Files.isRegularFile(root)) {
            List<ai.titli.darbiter.model.StructuredField> fields =
                    fileContentExtractor.extractStructured(root);
            if (fields != null && !fields.isEmpty()) {
                findings.addAll(detectionService.detectStructured(
                        fields, ruleSet, request.getCategories(), root.toString()));
            } else {
                String content = fileContentExtractor.extract(root);
                findings.addAll(detectionService.detectText(
                        content, ruleSet, request.getCategories(), root.toString()));
            }
        }
        return buildResult(ruleSet, findings, request.isApprovedForAi(), request.getUsage());
    }

    public ScanResult scanFiles(List<MultipartFile> files, boolean approvedForAi, String ruleset,
                                java.util.Set<ai.titli.darbiter.model.FindingCategory> categories,
                                AIUsage usage) {
        RuleSet ruleSet = ruleSetLoader.load(ruleset);
        List<Finding> findings = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }
            try {
                Path tempFile = Files.createTempFile("darbiter-upload-", "-" + file.getOriginalFilename());
                file.transferTo(tempFile);
                List<ai.titli.darbiter.model.StructuredField> fields =
                        fileContentExtractor.extractStructured(tempFile);
                if (fields != null && !fields.isEmpty()) {
                    findings.addAll(detectionService.detectStructured(
                            fields, ruleSet, categories, file.getOriginalFilename()));
                } else {
                    String content = fileContentExtractor.extract(tempFile);
                    findings.addAll(detectionService.detectText(
                            content, ruleSet, categories, file.getOriginalFilename()));
                }
                Files.deleteIfExists(tempFile);
            } catch (IOException ex) {
                throw new IllegalStateException("Failed to scan upload: " + file.getOriginalFilename(), ex);
            }
        }
        return buildResult(ruleSet, findings, approvedForAi, usage);
    }

    public ScanResult scanText(TextScanRequest request) {
        RuleSet ruleSet = ruleSetLoader.load(request.getRuleset());
        List<Finding> findings = detectionService.detectText(
                request.getContent(),
                ruleSet,
                request.getCategories(),
                "stdin"
        );
        return buildResult(ruleSet, findings, request.isApprovedForAi(), request.getUsage());
    }

    private ScanResult buildResult(RuleSet ruleSet, List<Finding> findings,
                                   boolean approvedForAi, AIUsage usage) {
        ScanResult result = new ScanResult();
        result.setScanId(UUID.randomUUID().toString());
        result.setRuleset(ruleSet.getName() + " (" + ruleSet.getVersion() + ")");
        result.setUsage(usage);
        result.setStartedAt(Instant.now());
        result.setFinishedAt(Instant.now());
        result.setFindings(findings);
        result.setRiskSummary(riskClassifier.summarize(findings));
        EligibilityDecision decision = eligibilityEvaluator.evaluate(findings, approvedForAi);
        result.setDecision(decision);
        result.setEligibility(decision.getStatus());
        result.setRemediation(remediationService.recommend(findings));
        scanStore.save(result);
        return result;
    }
}
