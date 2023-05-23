package gitlet;

// TODO: any imports you need here
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import java.io.File;
import java.io.Serializable;
import static gitlet.Utils.*;

import java.util.*;

import static gitlet.Utils.sha1;
import static gitlet.GitletRepository.*;

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

    /** java.util.Date and java.util.Formatter are useful for getting and formatting times.*/
    private String timestamp;
    private String parent1;
    private String parent2;
    private String message;
    private String SHA1;
    private File commitFile;
    private String commitFilePath;

    /**
     *Map<String, String> map = new TreeMap<>();
     *map filename(string) to different versions(as different files) eg:map hello.txt to file version( .gitlet/objects/66/ABCD) */
    private Map<String, String> filenameTofilePathMap;

    /** The constructor of first Commit. */
    public Commit(String message){
        this.timestamp = formatDate();
        this.message = message;
        this.SHA1 = sha1(this.message);
        this.filenameTofilePathMap = new HashMap<>();
        this.commitFile = createFilepathFromSha1(SHA1,OBJECT_FOLDER);
        this.commitFilePath = commitFile.toString();
    }

    public Commit(String parent1,String message,Map<String,String> map){
        this.message = message;
        this.parent1 = parent1;
        this.timestamp = formatDate();
        this.filenameTofilePathMap = map;
        this.SHA1 = sha1(parent1 + message + timestamp.toString() + filenameTofilePathMap.toString());
        this.commitFile = createFilepathFromSha1(SHA1,OBJECT_FOLDER);
    }
    public Commit(Commit currentHeadCommit,Commit targetBranchHead,String currentBranch,String targetBranch,Map<String,String> newmap){
        this.message = "Merged "+ targetBranch + " into " + currentBranch +".";
        this.parent1 = currentHeadCommit.SHA1;
        this.parent2 = targetBranchHead.SHA1;
        this.filenameTofilePathMap = newmap;
        this.timestamp = formatDate();
        this.SHA1 = sha1(this.parent1 +this.parent2 + this.message + this.timestamp.toString() + filenameTofilePathMap.toString());
        this.commitFile = createFilepathFromSha1(this.SHA1,OBJECT_FOLDER);
    }
    public String getMessage(){
        return this.message;
    }

    public String getTimestamp(){
        return this.timestamp;
    }

    public String getParent1SHA1(){
        return this.parent1;
    }
    public Commit getParent1() {
        File parent1File= createFilepathFromSha1(this.parent1,OBJECT_FOLDER);
        return readObject(parent1File,Commit.class);
    }
    public String getSecondParent(){
        return this.parent2;
    }

    public String getSHA1(){
        return this.SHA1;
    }

    public Map<String,String> getMap(){
        return this.filenameTofilePathMap;
    }

    public void makeCommit() {
        /** write back to log file */
        writeToGlobalLog(this);
        /** update master pointer */
        updateHeadPointerFile(this.getSHA1());
        /** save the commit object */
        save();
        //modify files according to staging area.
        //write back any new objects
    }
    public Map<String, String> removeFromCommit(String filename){
        Commit lastCommit = getLastCommit();
        Map<String, String> lastcommitMap = lastCommit.getMap();
        lastcommitMap.remove(filename);
        return lastcommitMap;
    }
    /** transient fields will not be serialized
    // when back in and deserialized, will be set to their default values.*/
    public void save(){
        writeObject(this.commitFile,this);
    }


    public void writeCommitLog(){
        throw new UnsupportedOperationException();
    }
    public static Commit getLastCommit(){
        //get current branch's head pointer
        File refsFile = getHeadPointerFile();
        String lastCommitSHA1 = readContentsAsString(refsFile);
        //read lastCommit as object to get needed information
        File lastCommitFile = createFilepathFromSha1(lastCommitSHA1,OBJECT_FOLDER);
        Commit lastCommit = readObject(lastCommitFile,Commit.class);
        return lastCommit;
    }
    public static Map<String, String> getLastCommitMap(){
        return getLastCommit().getMap();
    }

   public static String formatDate(){
       OffsetDateTime currentDateTime = OffsetDateTime.now(ZoneOffset.UTC);
       DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss yyyy Z");
       String formattedDateTime = currentDateTime.format(formatter);
       return formattedDateTime;
   }


}
