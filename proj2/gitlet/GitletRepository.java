package gitlet;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

import static gitlet.Utils.writeContents;
import static gitlet.Utils.*;

/** */
public class GitletRepository {
/** A repository for Gitlet */

    /** current working directory*/
    static final File CWD = new File(System.getProperty("user.dir"));
    /** main metadata folder */
    static final File GITLET_FOLDER = Utils.join(CWD,".gitlet");
    static final File STEAGE_FOLDER = Utils.join(GITLET_FOLDER,"staged");
    static final File ADD_FOLDER = Utils.join(STEAGE_FOLDER,"add");
    static final File REMOVE_FOLDER = Utils.join(STEAGE_FOLDER,"remove");
    static final File HEAD_FILE = Utils.join(GITLET_FOLDER,"HEAD");
    static final File OBJECT_FOLDER = Utils.join(GITLET_FOLDER,"objects");

    static final Map<String,String> filenameToBlob = new TreeMap<>();
/** handle the `init` command
    * Commit initial = new Commit("initial commit",null);
    * set up depository */
    public static void init(){
        GITLET_FOLDER.mkdir();
        STEAGE_FOLDER.mkdir();
        ADD_FOLDER.mkdir();
        REMOVE_FOLDER.mkdir();
        OBJECT_FOLDER.mkdir();
        try{
            HEAD_FILE.createNewFile();
        }
        catch(Exception e){
            System.err.println(e);
        }

        //initial commit
        Commit initial = new Commit("initial commit");
        initial.generateSHA1();

        //save initial commit
        initial.saveInitialCommit();
        //set the HEAD
        writeContents(HEAD_FILE,initial.getID().toString());
    }

    public static void add(String filename){
        //get the file
        File file = join(GitletRepository.CWD,filename);
        //read content from file
        byte[] content = readContents(file);
        //create blob object
        String blobID = sha1(content);
        //create the blob file path
        File blobFile = blobToFile(blobID);
        //save the blob content to object folder
        writeContents(blobFile,content);
        filenameToBlob.put(filename,blobID);
    }

    private File SHA1ToFile(String sha1){
        String first2 = sha1.substring(0,2);
        String last38 = sha1.substring(2);
        File fileFolder = Utils.join(OBJECT_FOLDER,first2);
        fileFolder.mkdir();
        File blobFile = Utils.join(fileFolder,last38);
        return blobFile;
    }

    private static File blobToFile(String sha1){
        String first2 = sha1.substring(0,2);
        String last38 = sha1.substring(2);
        File fileFolder = Utils.join(OBJECT_FOLDER,first2);
        fileFolder.mkdir();
        File objectFile = Utils.join(fileFolder,last38);
        return objectFile;
    }
}
