package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.TreeMap;
import java.util.Map;
import static gitlet.GitletRepository.*;
import static gitlet.Utils.*;
public class Blob implements Serializable {
    private Map<String,String> blobPathToFileName = new TreeMap<>();
    private String SHA1;
    private File originalFile;
    private File blobFile;
    private String filename;
    private byte[] content;
    private String blobFileInString;

    public Blob(String filename){
        this.filename = filename;
        this.originalFile = join(GitletRepository.CWD,filename);
        this.content = readContents(this.originalFile);
        this.SHA1 = sha1(this.content);
        this.blobFile = createFilepathFromSha1(SHA1,OBJECT_FOLDER);
        this.blobFileInString = this.blobFile.toString();
    }

    public String getFilename(){
        return this.filename;
    }

    public String getSHA1(){
        return this.SHA1;
    }

    public Map getMap(){
        return this.blobPathToFileName;
    }
    public File getBlobFile(){
        return this.blobFile;
    }
    public void save(File file){
        writeContents(file,this.content);
    }
    // track map(sha-1,filename) in order to check whether a file is staged twice when needed

}
