package ai.titli.darbiter.model;

public class StructuredField {
    private String path;
    private String value;
    private int index;
    private int line;
    private int column;

    public StructuredField(String path, String value, int index) {
        this.path = path;
        this.value = value;
        this.index = index;
        this.line = -1;
        this.column = -1;
    }

    public StructuredField(String path, String value, int index, int line, int column) {
        this.path = path;
        this.value = value;
        this.index = index;
        this.line = line;
        this.column = column;
    }

    public String getPath() {
        return path;
    }

    public String getValue() {
        return value;
    }

    public int getIndex() {
        return index;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }
}
