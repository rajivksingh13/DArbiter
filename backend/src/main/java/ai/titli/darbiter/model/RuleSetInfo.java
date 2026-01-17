package ai.titli.darbiter.model;

public class RuleSetInfo {
    private String file;
    private String name;
    private String version;

    public RuleSetInfo() {
    }

    public RuleSetInfo(String file, String name, String version) {
        this.file = file;
        this.name = name;
        this.version = version;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
