package gitlet;

// TODO: any imports you need here
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import java.io.File;
import java.io.Serializable;
import static gitlet.Utils.*;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

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
        this.filenameTofilePathMap = new TreeMap<>();
        this.commitFile = createFilepathFromSha1(SHA1,OBJECT_FOLDER);
        this.commitFilePath = commitFile.toString();
    }

    public Commit(String parent1,String message,Map<String,String> map){
        this.message = message;
        this.timestamp = formatDate();
        this.filenameTofilePathMap = map;
        this.SHA1 = sha1(parent1+ message + timestamp + map.toString());
    }
    public String getMessage(){
        return this.message;
    }

    public String getTimestamp(){
        return this.timestamp;
    }

    public String getParent() {
        return this.parent1;
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

    public static Commit getLastCommit(){
        //get .gitlet/HEAD file
        String head = readContentsAsString(HEAD_FILE);
        //get current branch's head pointer
        File refsFile = join(GITLET_FOLDER,head);
        System.out.println(refsFile.toString());
        String lastCommitSHA1 = readContentsAsString(refsFile);
        //read lastCommit as object to get needed information
        File lastCommitFile = createFilepathFromSha1(lastCommitSHA1,OBJECT_FOLDER);
        Commit lastCommit = readObject(lastCommitFile,Commit.class);
        return lastCommit;
    }
    public void makeCommit(String message,Map<String,String> map){
        this.message = message;
        this.timestamp = formatDate();
        this.filenameTofilePathMap = map;
        this.SHA1 = sha1(parent1+ message + timestamp + map.toString());
        System.out.println(this.filenameTofilePathMap.toString());

        //clear the staging area
        //write the staging are object

        //update log history file
        //update master pointer


    }
    /** transient fields will not be serialized
    // when back in and deserialized, will be set to their default values.*/
    // private transient MyCommitType parent1;


    //public void lastCommitObject(){}
    //public void createCommit(){}
    public void save(){
        writeObject(this.commitFile,this);
    }


    public void writeCommitLog(){
        throw new UnsupportedOperationException();
    }
    public void save(File file){
       //clone the head commit

       //modify the message and timestamp

       //modify files according to staging area.
       //write back any new objects
       throw new UnsupportedOperationException();
       //write back to log file
   }

   public static String formatDate(){
       OffsetDateTime currentDateTime = OffsetDateTime.now(ZoneOffset.UTC);
       DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss yyyy Z");
       String formattedDateTime = currentDateTime.format(formatter);
       return formattedDateTime;
   }


}
