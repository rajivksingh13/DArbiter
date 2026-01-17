package ai.titli.darbiter.service;

import ai.titli.darbiter.model.Finding;
import ai.titli.darbiter.model.ScanResult;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReportService {
    public String toHtml(ScanResult result) {
        StringBuilder builder = new StringBuilder();
        builder.append("<html><head><title>DArbiter Report</title></head><body>");
        builder.append("<h1>DArbiter Compliance Report</h1>");
        builder.append("<p><strong>Scan ID:</strong> ").append(result.getScanId()).append("</p>");
        builder.append("<p><strong>Ruleset:</strong> ").append(result.getRuleset()).append("</p>");
        builder.append("<p><strong>Eligibility:</strong> ").append(result.getEligibility()).append("</p>");
        builder.append("<h2>Risk Summary</h2>");
        builder.append("<ul>");
        builder.append("<li>Total: ").append(result.getRiskSummary().getTotalFindings()).append("</li>");
        builder.append("<li>Critical: ").append(result.getRiskSummary().getCritical()).append("</li>");
        builder.append("<li>High: ").append(result.getRiskSummary().getHigh()).append("</li>");
        builder.append("<li>Medium: ").append(result.getRiskSummary().getMedium()).append("</li>");
        builder.append("<li>Low: ").append(result.getRiskSummary().getLow()).append("</li>");
        builder.append("</ul>");
        builder.append("<h2>Findings</h2>");
        builder.append("<table border='1' cellpadding='6' cellspacing='0'>");
        builder.append("<tr><th>Category</th><th>Severity</th><th>Label</th><th>File</th><th>Line</th><th>Snippet</th></tr>");
        for (Finding finding : safe(result.getFindings())) {
            builder.append("<tr>");
            builder.append("<td>").append(finding.getCategory()).append("</td>");
            builder.append("<td>").append(finding.getSeverity()).append("</td>");
            builder.append("<td>").append(finding.getLabel()).append("</td>");
            builder.append("<td>").append(escape(finding.getFilePath())).append("</td>");
            builder.append("<td>").append(finding.getLineNumber()).append("</td>");
            builder.append("<td>").append(escape(finding.getSnippet())).append("</td>");
            builder.append("</tr>");
        }
        builder.append("</table>");
        if (result.getDecision() != null) {
            builder.append("<h2>Eligibility Decision</h2>");
            builder.append("<p><strong>Status:</strong> ").append(result.getDecision().getStatus()).append("</p>");
            builder.append("<p><strong>Reasons:</strong></p><ul>");
            for (String reason : result.getDecision().getReasons()) {
                builder.append("<li>").append(escape(reason)).append("</li>");
            }
            builder.append("</ul>");
            builder.append("<p><strong>Policy References:</strong></p><ul>");
            for (String policy : result.getDecision().getPolicyReferences()) {
                builder.append("<li>").append(escape(policy)).append("</li>");
            }
            builder.append("</ul>");
        }
        if (result.getRemediation() != null && !result.getRemediation().isEmpty()) {
            builder.append("<h2>Remediation Guidance</h2>");
            builder.append("<ul>");
            result.getRemediation().forEach(item -> {
                builder.append("<li>")
                        .append(escape(item.getLabel()))
                        .append("<ul>");
                item.getActions().forEach(action -> builder.append("<li>").append(escape(action)).append("</li>"));
                builder.append("</ul></li>");
            });
            builder.append("</ul>");
        }
        builder.append("</body></html>");
        return builder.toString();
    }

    private List<Finding> safe(List<Finding> findings) {
        return findings == null ? List.of() : findings;
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
