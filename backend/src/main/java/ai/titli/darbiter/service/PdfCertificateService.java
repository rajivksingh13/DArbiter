package ai.titli.darbiter.service;

import ai.titli.darbiter.model.ScanResult;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
public class PdfCertificateService {
    public byte[] generate(ScanResult result) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Document document = new Document();
        PdfWriter.getInstance(document, outputStream);
        document.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
        Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 12);

        document.add(new Paragraph("DArbiter AI Readiness Certificate", titleFont));
        document.add(new Paragraph(" ", bodyFont));
        document.add(new Paragraph("Scan ID: " + result.getScanId(), bodyFont));
        document.add(new Paragraph("Ruleset: " + result.getRuleset(), bodyFont));
        document.add(new Paragraph("Usage: " + result.getUsage(), bodyFont));
        document.add(new Paragraph("Eligibility: " + result.getEligibility(), bodyFont));
        document.add(new Paragraph("Issued: " + result.getFinishedAt(), bodyFont));

        document.close();
        return outputStream.toByteArray();
    }
}
