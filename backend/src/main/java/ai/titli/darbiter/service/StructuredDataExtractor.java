package ai.titli.darbiter.service;

import ai.titli.darbiter.model.StructuredField;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.dataformat.toml.TomlFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class StructuredDataExtractor {
    private static final int MAX_CELLS = 20_000;
    private static final int MAX_ROWS = 5_000;
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private final ObjectMapper tomlMapper = new ObjectMapper(new TomlFactory());
    private final JsonFactory jsonFactory = new JsonFactory();

    public List<StructuredField> extractStructured(Path path) throws IOException {
        String filename = path.getFileName().toString().toLowerCase();
        if (filename.endsWith(".yaml") || filename.endsWith(".yml")) {
            Object node = yamlMapper.readValue(path.toFile(), Object.class);
            return flatten(node);
        }
        if (filename.endsWith(".toml")) {
            Object node = tomlMapper.readValue(path.toFile(), Object.class);
            return flatten(node);
        }
        return extractJsonStructured(path);
    }

    public List<StructuredField> extractCsv(Path path) throws IOException {
        List<StructuredField> fields = new ArrayList<>();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build().parse(reader)) {
            AtomicInteger index = new AtomicInteger(1);
            for (CSVRecord record : parser) {
                if (record.getRecordNumber() > MAX_ROWS) {
                    break;
                }
                for (Map.Entry<String, String> entry : record.toMap().entrySet()) {
                    int line = (int) record.getRecordNumber() + 1;
                    String pathLabel = "row:" + record.getRecordNumber() + ".col:" + entry.getKey();
                    fields.add(new StructuredField(pathLabel, entry.getValue(), index.getAndIncrement(), line, 1));
                }
            }
        } catch (IllegalArgumentException ex) {
            return extractCsvNoHeader(path);
        }
        return fields;
    }

    private List<StructuredField> extractCsvNoHeader(Path path) throws IOException {
        List<StructuredField> fields = new ArrayList<>();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.parse(reader)) {
            AtomicInteger index = new AtomicInteger(1);
            for (CSVRecord record : parser) {
                if (record.getRecordNumber() > MAX_ROWS) {
                    break;
                }
                for (int i = 0; i < record.size(); i++) {
                    int line = (int) record.getRecordNumber();
                    String pathLabel = "row:" + record.getRecordNumber() + ".col:" + i;
                    fields.add(new StructuredField(pathLabel, record.get(i), index.getAndIncrement(), line, i + 1));
                }
            }
        }
        return fields;
    }

    public List<StructuredField> extractXlsx(Path path) throws IOException {
        List<StructuredField> fields = new ArrayList<>();
        try (InputStream inputStream = Files.newInputStream(path);
             XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            AtomicInteger index = new AtomicInteger(1);
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                XSSFSheet sheet = workbook.getSheetAt(i);
                List<String> headers = new ArrayList<>();
                for (Row row : sheet) {
                    if (row.getRowNum() == 0) {
                        for (Cell cell : row) {
                            headers.add(cell.toString());
                        }
                        continue;
                    }
                    if (row.getRowNum() > MAX_ROWS) {
                        break;
                    }
                    for (Cell cell : row) {
                        if (fields.size() > MAX_CELLS) {
                            break;
                        }
                        String header = cell.getColumnIndex() < headers.size()
                                ? headers.get(cell.getColumnIndex())
                                : "col:" + cell.getColumnIndex();
                        String pathLabel = "sheet:" + sheet.getSheetName()
                                + ".row:" + (row.getRowNum() + 1)
                                + ".col:" + header;
                        fields.add(new StructuredField(
                                pathLabel,
                                cell.toString(),
                                index.getAndIncrement(),
                                row.getRowNum() + 1,
                                cell.getColumnIndex() + 1
                        ));
                    }
                }
            }
        }
        return fields;
    }

    public List<StructuredField> extractXml(Path path) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path)) {
            Document doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(inputStream);
            doc.getDocumentElement().normalize();
            List<StructuredField> fields = new ArrayList<>();
            AtomicInteger index = new AtomicInteger(1);
            walkXml(doc.getDocumentElement(), doc.getDocumentElement().getNodeName(), fields, index);
            return fields;
        } catch (Exception ex) {
            throw new IOException("Failed to parse XML", ex);
        }
    }

    public List<StructuredField> extractKeyValue(Path path) throws IOException {
        List<StructuredField> fields = new ArrayList<>();
        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(path)) {
            properties.load(inputStream);
        }
        AtomicInteger index = new AtomicInteger(1);
        for (String name : properties.stringPropertyNames()) {
            fields.add(new StructuredField(name, properties.getProperty(name), index.getAndIncrement(), -1, -1));
        }
        return fields;
    }

    private List<StructuredField> flatten(Object node) {
        List<StructuredField> fields = new ArrayList<>();
        AtomicInteger index = new AtomicInteger(1);
        walk(node, "$", fields, index);
        return fields;
    }

    private void walk(Object node, String path, List<StructuredField> fields, AtomicInteger index) {
        if (node == null) {
            return;
        }
        if (node instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                walk(entry.getValue(), path + "." + key, fields, index);
            }
        } else if (node instanceof List<?> list) {
            for (int i = 0; i < list.size(); i++) {
                walk(list.get(i), path + "[" + i + "]", fields, index);
            }
        } else {
            fields.add(new StructuredField(path, String.valueOf(node), index.getAndIncrement(), -1, -1));
        }
    }

    private void walkXml(Node node, String path, List<StructuredField> fields, AtomicInteger index) {
        if (node.hasAttributes() && node.getAttributes() != null) {
            for (int i = 0; i < node.getAttributes().getLength(); i++) {
                Node attr = node.getAttributes().item(i);
                String attrPath = path + ".@" + attr.getNodeName();
                fields.add(new StructuredField(attrPath, attr.getNodeValue(), index.getAndIncrement(), -1, -1));
            }
        }
        NodeList children = node.getChildNodes();
        boolean hasElementChild = false;
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
                hasElementChild = true;
                break;
            }
        }
        if (!hasElementChild) {
            String text = node.getTextContent();
            if (text != null && !text.isBlank()) {
                fields.add(new StructuredField(path, text.trim(), index.getAndIncrement(), -1, -1));
            }
        } else {
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    walkXml(child, path + "." + child.getNodeName(), fields, index);
                }
            }
        }
    }

    private List<StructuredField> extractJsonStructured(Path path) throws IOException {
        List<StructuredField> fields = new ArrayList<>();
        AtomicInteger index = new AtomicInteger(1);
        Deque<String> stack = new ArrayDeque<>();
        try (InputStream inputStream = Files.newInputStream(path);
             JsonParser parser = jsonFactory.createParser(inputStream)) {
            String currentField = null;
            while (parser.nextToken() != null) {
                JsonToken token = parser.currentToken();
                if (token == JsonToken.FIELD_NAME) {
                    currentField = parser.currentName();
                } else if (token == JsonToken.START_OBJECT) {
                    if (currentField != null) {
                        stack.addLast(currentField);
                        currentField = null;
                    }
                } else if (token == JsonToken.END_OBJECT) {
                    if (!stack.isEmpty()) {
                        stack.removeLast();
                    }
                } else if (token == JsonToken.START_ARRAY) {
                    if (currentField != null) {
                        stack.addLast(currentField);
                        currentField = null;
                    }
                    stack.addLast("[0]");
                } else if (token == JsonToken.END_ARRAY) {
                    if (!stack.isEmpty()) {
                        stack.removeLast();
                    }
                    if (!stack.isEmpty() && stack.peekLast().startsWith("[")) {
                        stack.removeLast();
                    }
                } else if (token.isScalarValue()) {
                    String pathLabel = buildPath(stack, currentField);
                    int line = parser.currentLocation().getLineNr();
                    int col = parser.currentLocation().getColumnNr();
                    fields.add(new StructuredField(
                            pathLabel,
                            parser.getValueAsString(),
                            index.getAndIncrement(),
                            line,
                            col
                    ));
                    currentField = null;
                    bumpArrayIndex(stack);
                }
            }
        }
        return fields;
    }

    private String buildPath(Deque<String> stack, String currentField) {
        StringBuilder builder = new StringBuilder("$");
        for (String segment : stack) {
            if (segment.startsWith("[")) {
                builder.append(segment);
            } else {
                builder.append(".").append(segment);
            }
        }
        if (currentField != null) {
            builder.append(".").append(currentField);
        }
        return builder.toString();
    }

    private void bumpArrayIndex(Deque<String> stack) {
        if (stack.isEmpty()) {
            return;
        }
        String last = stack.peekLast();
        if (last != null && last.startsWith("[")) {
            int value = Integer.parseInt(last.substring(1, last.length() - 1));
            stack.removeLast();
            stack.addLast("[" + (value + 1) + "]");
        }
    }
}
