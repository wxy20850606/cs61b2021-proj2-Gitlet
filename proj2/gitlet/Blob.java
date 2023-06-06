package gitlet;


import java.io.File;
import java.io.Serializable;
import java.util.TreeMap;
import java.util.Map;
import static gitlet.GitletRepository.*;
import static gitlet.Utils.*;
public class Blob implements Serializable {
    private Map<String, String> blobPathToFileName = new TreeMap<>();
    private String sha1;
    private File originalFile;
    private File blobFile;
    private String filename;
    private String  content;
    private String blobFileInString;

    public Blob(String filename) {
        this.filename = filename;
        this.originalFile = join(GitletRepository.CWD, filename);
        this.content = readContentsAsString(this.originalFile);
        this.sha1 = sha1(this.content);
    }

    public Blob(String filename, String content) {
        this.filename = filename;
        this.content = content;
        this.sha1 = sha1(this.content);
    }
    public String getFilename() {
        return this.filename;
    }
    public String getSHA1() {
        return this.sha1;
    }
    public String getContent() {
        return this.content;
    }
    public Map getMap() {
        return this.blobPathToFileName;
    }
    public File getBlobFile() {
        return this.blobFile;
    }
    public void save() {
        this.blobFile = createFilepathFromSha1(sha1,OBJECT_FOLDER);
        writeObject(this.getBlobFile(), this);
    }
}
