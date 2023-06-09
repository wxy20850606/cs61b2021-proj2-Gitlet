package gitlet;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import java.io.File;
import java.io.Serializable;
import static gitlet.Utils.*;

import java.util.*;

import static gitlet.Utils.sha1;
import static gitlet.GitletRepository.*;

public class Commit implements Serializable {
    static final File COMMIT_FOLDER = Utils.join(GitletRepository.GITLET_FOLDER, "objects");

    /**
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.

    /** java.util.Date and java.util.Formatter are useful for getting and formatting times.*/
    private String timestamp;
    private String parent1;
    private String parent2;
    private String message;
    private String sha1;
    private File commitFile;
    private String commitFilePath;
    private Map<String, String> fileToIDMap;

    /** The constructor of first Commit. */
    public Commit(String message) {
        this.timestamp = formatDate();
        this.message = message;
        this.sha1 = sha1(this.message);
        this.fileToIDMap = new HashMap<>();
        this.commitFile = createFile(sha1, OBJECT_FOLDER);
        this.commitFilePath = commitFile.toString();
    }

    public Commit(String parent1, String message, Map<String, String> map) {
        this.message = message;
        this.parent1 = parent1;
        this.timestamp = formatDate();
        this.fileToIDMap = map;
        String content = parent1 + message + timestamp.toString() + fileToIDMap.toString();
        this.sha1 = sha1(content);
        this.commitFile = createFile(sha1, OBJECT_FOLDER);
    }
    public Commit(Commit x, Commit y, String branchA, String branchB, Map<String, String> a) {
        this.message = "Merged " + branchB + " into " + branchA + ".";
        this.parent1 = x.sha1;
        this.parent2 = y.sha1;
        this.fileToIDMap = a;
        this.timestamp = formatDate();
        this.sha1 = sha1(parent1 + parent2 + message + timestamp.toString() + a.toString());
        this.commitFile = createFile(this.sha1, OBJECT_FOLDER);
    }
    public String getMessage() {
        return this.message;
    }

    public String getTimestamp() {
        return this.timestamp;
    }

    public String getParent1SHA1() {
        return this.parent1;
    }
    public Commit getParent1() {
        File parent1File = createFile(this.parent1, OBJECT_FOLDER);
        return readObject(parent1File, Commit.class);
    }
    public List<Commit> getParent() {
        List<Commit> parentList = new ArrayList<>();
        if (this.havaParent1()) {
            parentList.add(getParent1());
        }
        if (this.havaParent2()) {
            parentList.add(getParent2());
        }
        return parentList;
    }
    public Commit getParent2() {
        File parent1File = createFile(this.parent2, OBJECT_FOLDER);
        return readObject(parent1File, Commit.class);
    }

    public Boolean havaParent1() {
        return this.parent1 != null;
    }
    public Boolean havaParent2() {
        return this.parent2 != null;
    }
    public String getParent2ID() {
        return this.parent2;
    }
    public String getParent1ID() {
        return this.parent1;
    }
    public String getSecondParent() {
        return this.parent2;
    }

    public String getSHA1() {
        return this.sha1;
    }

    public Map<String, String> getMap() {
        return this.fileToIDMap;
    }

    public void makeCommit() {
        /** write back to log file */
        writeToGlobalLog(this);
        /** update master pointer */
        updateHeadPointerFile(this.getSHA1());
        /** save the commit object */
        save();
    }
    public Map<String, String> removeFromCommit(String filename) {
        Commit lastCommit = getLastCommit();
        Map<String, String> lastcommitMap = lastCommit.getMap();
        lastcommitMap.remove(filename);
        return lastcommitMap;
    }
    /** transient fields will not be serialized
    // when back in and deserialized, will be set to their default values.*/
    public void save() {
        writeObject(this.commitFile, this);
    }

    public static Commit getLastCommit() {
        //get current branch's head pointer
        String lastCommitSHA1 = getHeadPointer();
        //read lastCommit as object to get needed information
        File lastCommitFile = createFile(lastCommitSHA1, OBJECT_FOLDER);
        Commit lastCommit = readObject(lastCommitFile, Commit.class);
        return lastCommit;
    }
    public static Map<String, String> getLastCommitMap() {
        return getLastCommit().getMap();
    }
    public static String formatDate() {
        OffsetDateTime currentDateTime = OffsetDateTime.now(ZoneOffset.UTC);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss yyyy Z");
        String formattedDateTime = currentDateTime.format(formatter);
        return formattedDateTime;
    }
}
