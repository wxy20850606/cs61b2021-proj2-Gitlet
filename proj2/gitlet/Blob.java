package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.TreeMap;
import java.util.Map;
import static gitlet.Utils.*;
public class Blob implements Serializable {
    //private String ID;
    //private File file;
    //private String filename;
    private byte[] content;

    public Blob(byte[] content){
        //this.filename = filename;
        //this.file = join(GitletRepository.CWD,filename);
        this.content = content;
        //this.ID = sha1(this.content);
    }

    /** Driver class for Gitlet, a subset of the Git version-control system.


    public String getFilename(){
        return this.filename;
    }

    public String getID(){
        return this.ID;
    }

    public File getfile(){
        return this.file;
    }
    // track map(sha-1,filename) in order to check whether a file is staged twice when needed
    public Map<String,String> BlobIDtoFileName = new TreeMap<>();
    public void saveBlob(){


    }
     */
}
