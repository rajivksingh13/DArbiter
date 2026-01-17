package ai.titli.darbiter.service;

import ai.titli.darbiter.model.StructuredField;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Service
public class FileContentExtractor {
    private static final int MAX_BYTES = 2_000_000;
    private final StructuredDataExtractor structuredDataExtractor;

    public FileContentExtractor(StructuredDataExtractor structuredDataExtractor) {
        this.structuredDataExtractor = structuredDataExtractor;
    }

    public String extract(Path path) {
        String ext = extension(path.getFileName().toString());
        try {
            return switch (ext) {
                case "pdf" -> extractPdf(path);
                case "docx" -> extractDocx(path);
                case "xlsx" -> extractXlsx(path);
                case "json", "csv", "xml", "yaml", "yml", "toml", "txt", "log", "env", "properties", "conf" ->
                        extractText(path);
                default -> extractText(path);
            };
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to extract content: " + path, ex);
        }
    }

    public List<StructuredField> extractStructured(Path path) {
        String ext = extension(path.getFileName().toString());
        try {
            return switch (ext) {
                case "json", "yaml", "yml", "toml" -> structuredDataExtractor.extractStructured(path);
                case "csv" -> structuredDataExtractor.extractCsv(path);
                case "xlsx" -> structuredDataExtractor.extractXlsx(path);
                case "xml" -> structuredDataExtractor.extractXml(path);
                case "properties", "env", "conf" -> structuredDataExtractor.extractKeyValue(path);
                default -> Collections.emptyList();
            };
        } catch (IOException ex) {
            return Collections.emptyList();
        }
    }

    private String extractText(Path path) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        if (bytes.length > MAX_BYTES) {
            return new String(bytes, 0, MAX_BYTES, StandardCharsets.UTF_8);
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private String extractPdf(Path path) throws IOException {
        try (PDDocument document = PDDocument.load(path.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String extractDocx(Path path) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path);
             XWPFDocument document = new XWPFDocument(inputStream)) {
            StringBuilder builder = new StringBuilder();
            document.getParagraphs().forEach(p -> builder.append(p.getText()).append("\n"));
            return builder.toString();
        }
    }

    private String extractXlsx(Path path) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path);
             XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                XSSFSheet sheet = workbook.getSheetAt(i);
                for (Row row : sheet) {
                    for (Cell cell : row) {
                        builder.append(cell.toString()).append("\t");
                    }
                    builder.append("\n");
                }
            }
            return builder.toString();
        }
    }

    private String extension(String filename) {
        int idx = filename.lastIndexOf(".");
        if (idx == -1) {
            return "";
        }
        return filename.substring(idx + 1).toLowerCase(Locale.ROOT);
    }
}
