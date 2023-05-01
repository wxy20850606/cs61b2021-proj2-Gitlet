package gitlet;

// TODO: any imports you need here

import java.io.File;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;


/** Represents a gitlet commit object.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author TODO
 */
//a log message, timestamp, a mapping of file names to blob references,
// a parent reference, and (for merges) a second parent reference.
//Distinguishing somehow between hashes for commits and hashes for blobs
public class Commit implements Serializable {
    static final File COMMIT_FOLDER = Utils.join(GitletRepository.GITLET_FOLDER,"objects");

    /**
     * TODO: add instance variables here.
     *
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */
    private String author;

    /** java.util.Date and java.util.Formatter are useful for getting and formatting times.*/
    private Date timestamp;
    private String parent;

    /** The message of this Commit. */
    private String message;

    /**
     *Map<String, String> map = new TreeMap<>();
     *map filename(string) to different versions(as different files) eg:map hello.txt to file version( .gitlet/objects/66/ABCD) */
    private TreeMap<String, File> map = null;

    /** The constructor of first Commit. */
    public Commit(String message,String parent){
        Date d = new Date();
        this.timestamp = d;
        this.message = message;
        this.parent = parent;
    }

    public Commit(String author,String parent,String message,TreeMap<String, File> map){
        this.author = author;
        this.parent= parent;
        this.message = message;
    }
    public String getMessage(){
        return this.message;
    }

    public Date getTimestamp(){
        return this.timestamp;
    }

    public String getParent(){
        return this.parent;
    }
    /** transient fields will not be serialized
    // when back in and deserialized, will be set to their default values.*/
    // private transient MyCommitType parent1;

    private File SHA1ToFile(String sha1){
        String first2 = sha1.substring(0,2);
        String last38 = sha1.substring(2);
        File fileFolder = Utils.join(GitletRepository.OBJECT_FILE,first2);
        File objectFile = Utils.join(fileFolder,last38);
        return objectFile;
    }
    public void saveInitialCommit(String x){
        File initialCommit = SHA1ToFile(x);
        Utils.writeObject(initialCommit,this);
    }
    public void saveCommit(){
       //clone the head commit
       //modify the message and timestamp
       //modify files according to staging area.
       //write back any new objects
       throw new UnsupportedOperationException();
   }
}
