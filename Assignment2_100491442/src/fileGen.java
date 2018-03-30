import java.io.File;

public class fileGen {
    private File actualFile;
    private String fileName;

    public fileGen(String fileName) {
        this.fileName = fileName;
    }
    public fileGen(File actualFile) {
        this.actualFile = actualFile;
        this.fileName = actualFile.getName();
    }
    public String getFileName() {
        //return this.fileName;
        return fileName;
    }
    public File getActualFile() {
        return actualFile;
    }
}
