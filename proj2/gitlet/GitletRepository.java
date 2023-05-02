package gitlet;

import java.io.File;

import static gitlet.Utils.writeContents;

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
    static final File OBJECT_FILE = Utils.join(GITLET_FOLDER,"objects");

/** handle the `init` command
    * Commit initial = new Commit("initial commit",null);
    * set up depository */
    public static void init(){
        GITLET_FOLDER.mkdir();
        STEAGE_FOLDER.mkdir();
        ADD_FOLDER.mkdir();
        REMOVE_FOLDER.mkdir();
        OBJECT_FILE.mkdir();
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
}
