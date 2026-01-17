package ai.titli.darbiter.controller;

import ai.titli.darbiter.model.AIUsage;
import ai.titli.darbiter.model.Certificate;
import ai.titli.darbiter.model.FindingCategory;
import ai.titli.darbiter.model.PathScanRequest;
import ai.titli.darbiter.model.RuleSetInfo;
import ai.titli.darbiter.model.ScanResult;
import ai.titli.darbiter.model.TextScanRequest;
import ai.titli.darbiter.service.PdfCertificateService;
import ai.titli.darbiter.service.PdfSummaryReportService;
import ai.titli.darbiter.service.ReportService;
import ai.titli.darbiter.service.RuleSetCatalog;
import ai.titli.darbiter.service.ScanService;
import ai.titli.darbiter.service.ScanStore;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ScanController {
    private final ScanService scanService;
    private final ScanStore scanStore;
    private final RuleSetCatalog ruleSetCatalog;
    private final ReportService reportService;
    private final PdfCertificateService pdfCertificateService;
    private final PdfSummaryReportService pdfSummaryReportService;

    public ScanController(ScanService scanService,
                          ScanStore scanStore,
                          RuleSetCatalog ruleSetCatalog,
                          ReportService reportService,
                          PdfCertificateService pdfCertificateService,
                          PdfSummaryReportService pdfSummaryReportService) {
        this.scanService = scanService;
        this.scanStore = scanStore;
        this.ruleSetCatalog = ruleSetCatalog;
        this.reportService = reportService;
        this.pdfCertificateService = pdfCertificateService;
        this.pdfSummaryReportService = pdfSummaryReportService;
    }

    @PostMapping("/scan/path")
    public ScanResult scanPath(@Valid @RequestBody PathScanRequest request) {
        return scanService.scanPath(request);
    }

    @PostMapping(value = "/scan/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ScanResult scanFiles(@RequestParam("files") List<MultipartFile> files,
                                @RequestParam(value = "approvedForAi", defaultValue = "false") boolean approvedForAi,
                                @RequestParam(value = "ruleset", defaultValue = "combined_baseline.yaml") String ruleset,
                                @RequestParam(value = "usage", defaultValue = "INFERENCE") AIUsage usage,
                                @RequestParam(value = "categories", required = false) String categories) {
        return scanService.scanFiles(files, approvedForAi, ruleset, parseCategories(categories), usage);
    }

    @PostMapping("/scan/text")
    public ScanResult scanText(@Valid @RequestBody TextScanRequest request) {
        return scanService.scanText(request);
    }

    @GetMapping("/scan/{scanId}")
    public ResponseEntity<ScanResult> getScan(@PathVariable String scanId) {
        return scanStore.find(scanId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/rulesets")
    public List<RuleSetInfo> listRuleSets() {
        return ruleSetCatalog.list();
    }

    @GetMapping(value = "/report/{scanId}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> report(@PathVariable String scanId) {
        return scanStore.find(scanId)
                .map(result -> ResponseEntity.ok(reportService.toHtml(result)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/certify/{scanId}")
    public ResponseEntity<Certificate> certify(@PathVariable String scanId) {
        return scanStore.find(scanId)
                .map(result -> ResponseEntity.ok(new Certificate(
                        result.getScanId(),
                        "local-scan",
                        result.getEligibility(),
                        result.getUsage(),
                        result.getRuleset(),
                        Instant.now()
                )))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(value = "/certify/{scanId}/pdf", produces = "application/pdf")
    public ResponseEntity<byte[]> certifyPdf(@PathVariable String scanId) {
        return scanStore.find(scanId)
                .map(result -> ResponseEntity.ok()
                        .header("Content-Disposition", "attachment; filename=\"darbiter-certificate.pdf\"")
                        .body(pdfCertificateService.generate(result)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(value = "/summary/{scanId}/pdf", produces = "application/pdf")
    public ResponseEntity<byte[]> summaryPdf(@PathVariable String scanId) {
        return scanStore.find(scanId)
                .map(result -> ResponseEntity.ok()
                        .header("Content-Disposition", "attachment; filename=\"darbiter-summary.pdf\"")
                        .body(pdfSummaryReportService.generate(result)))
                .orElse(ResponseEntity.notFound().build());
    }

    private Set<FindingCategory> parseCategories(String categories) {
        if (categories == null || categories.isBlank()) {
            return EnumSet.allOf(FindingCategory.class);
        }
        Set<FindingCategory> parsed = EnumSet.noneOf(FindingCategory.class);
        for (String entry : categories.split(",")) {
            parsed.add(FindingCategory.valueOf(entry.trim().toUpperCase()));
        }
        return parsed;
    }
}
