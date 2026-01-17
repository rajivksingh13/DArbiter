package ai.titli.darbiter.service;

import ai.titli.darbiter.model.Finding;
import ai.titli.darbiter.model.ScanResult;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;

@Service
public class PdfSummaryReportService {
    private static final int MAX_FINDINGS = 100;

    public byte[] generate(ScanResult result) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Document document = new Document();
        PdfWriter.getInstance(document, outputStream);
        document.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
        Font h2Font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

        document.add(new Paragraph("DArbiter Summary Report", titleFont));
        document.add(new Paragraph(" ", bodyFont));

        document.add(new Paragraph("Scan ID: " + result.getScanId(), bodyFont));
        document.add(new Paragraph("Ruleset: " + result.getRuleset(), bodyFont));
        document.add(new Paragraph("Usage: " + result.getUsage(), bodyFont));
        document.add(new Paragraph("Eligibility: " + result.getEligibility(), bodyFont));
        document.add(new Paragraph("Started: " + result.getStartedAt(), bodyFont));
        document.add(new Paragraph("Finished: " + result.getFinishedAt(), bodyFont));

        document.add(new Paragraph(" ", bodyFont));
        document.add(new Paragraph("Risk Summary", h2Font));
        document.add(new Paragraph("Overall: " + result.getRiskSummary().getOverall(), bodyFont));
        document.add(new Paragraph("Total Findings: " + result.getRiskSummary().getTotalFindings(), bodyFont));
        document.add(new Paragraph("Critical: " + result.getRiskSummary().getCritical(), bodyFont));
        document.add(new Paragraph("High: " + result.getRiskSummary().getHigh(), bodyFont));
        document.add(new Paragraph("Medium: " + result.getRiskSummary().getMedium(), bodyFont));
        document.add(new Paragraph("Low: " + result.getRiskSummary().getLow(), bodyFont));

        if (result.getDecision() != null) {
            document.add(new Paragraph(" ", bodyFont));
            document.add(new Paragraph("Eligibility Decision", h2Font));
            result.getDecision().getReasons().forEach(reason ->
                    document.add(new Paragraph("- " + reason, bodyFont)));
            if (!result.getDecision().getPolicyReferences().isEmpty()) {
                document.add(new Paragraph("Policy References:", bodyFont));
                result.getDecision().getPolicyReferences().forEach(policy ->
                        document.add(new Paragraph("- " + policy, bodyFont)));
            }
        }

        document.add(new Paragraph(" ", bodyFont));
        document.add(new Paragraph("Findings (top " + MAX_FINDINGS + ")", h2Font));
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2.2f, 1.2f, 2.4f, 4.2f});
        addHeader(table, bodyFont, "Category");
        addHeader(table, bodyFont, "Severity");
        addHeader(table, bodyFont, "Location");
        addHeader(table, bodyFont, "Label");

        List<Finding> findings = result.getFindings();
        int count = 0;
        for (Finding finding : findings) {
            if (count >= MAX_FINDINGS) {
                break;
            }
            table.addCell(cell(bodyFont, String.valueOf(finding.getCategory())));
            table.addCell(cell(bodyFont, String.valueOf(finding.getSeverity())));
            table.addCell(cell(bodyFont, safe(finding.getFilePath())));
            table.addCell(cell(bodyFont, safe(finding.getLabel())));
            count++;
        }
        document.add(table);

        document.close();
        return outputStream.toByteArray();
    }

    private void addHeader(PdfPTable table, Font font, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(6f);
        table.addCell(cell);
    }

    private PdfPCell cell(Font font, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5f);
        return cell;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
