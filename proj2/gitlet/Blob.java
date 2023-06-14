package gitlet;


import java.io.File;
import java.io.Serializable;
import static gitlet.GitletRepository.*;
import static gitlet.Utils.*;

public class Blob implements Serializable {
    //private Map<String, String> fileNametoBlobMap= new TreeMap<>();
    private String sha1;
    private File originalFile;
    private File blobFile;
    private String filename;
    private String  content;


    public Blob(String filename) {
        this.filename = filename;
        this.originalFile = join(GitletRepository.CWD, filename);
        this.content = readContentsAsString(this.originalFile);
        this.sha1 = sha1(this.content);
    }

    public String getSHA1() {
        return this.sha1;
    }
    public String getContent() {
        return this.content;
    }
    public void save() {
        this.blobFile = createFile(sha1, OBJECT_FOLDER);
        writeObject(this.getBlobFile(), this);
    }
    private File getBlobFile() {
        return this.blobFile;
    }
}
