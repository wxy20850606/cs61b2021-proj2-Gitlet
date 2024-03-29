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
    /** */
    private String timestamp;
    private String parent1;
    private String parent2;
    private String message;
    private String commitID;
    private Map<String, String> fileToIDMap;

    /** The constructor of first Commit. */
    public Commit(String message) {
        this.timestamp = formatDate();
        this.message = message;
        this.commitID = sha1(this.message);
        this.fileToIDMap = new HashMap<>();
    }

    public Commit(String parent1, String message, Map<String, String> map) {
        this.message = message;
        this.parent1 = parent1;
        this.timestamp = formatDate();
        this.fileToIDMap = map;
        String content = parent1 + message + timestamp.toString() + fileToIDMap.toString();
        this.commitID = sha1(content);
    }
    public Commit(Commit x, Commit y, String branchA, String branchB, Map<String, String> a) {
        this.message = "Merged " + branchB + " into " + branchA + ".";
        this.parent1 = x.commitID;
        this.parent2 = y.commitID;
        this.fileToIDMap = a;
        this.timestamp = formatDate();
        this.commitID = sha1(parent1 + parent2 + message + timestamp.toString() + a.toString());
    }
    public String getMessage() {
        return this.message;
    }

    public String getTimestamp() {
        return this.timestamp;
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

    public String getCommitID() {
        return this.commitID;
    }

    public Map<String, String> getMap() {
        return this.fileToIDMap;
    }

    public void makeCommit() {
        /** write back to log file */
        writeToGlobalLog(this);
        /** update master pointer */
        updateHeadPointerFile(this.getCommitID());
        /** save the commit object */
        save();
    }

    public void save() {
        File file = createFile(this.getCommitID(), OBJECT_FOLDER);
        writeObject(file, this);
    }

    private static String formatDate() {
        OffsetDateTime currentDateTime = OffsetDateTime.now(ZoneOffset.UTC);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss yyyy Z");
        String formattedDateTime = currentDateTime.format(formatter);
        return formattedDateTime;
    }

    private static void writeToGlobalLog(Commit x) {
        StringBuilder logBuilder = new StringBuilder();
        String oldLog = readContentsAsString(LOG_HEAD_FILE);
        logBuilder.append(oldLog).append("\n").append("===\n");
        logBuilder.append("commit ").append(x.getCommitID()).append("\n");
        logBuilder.append("Date: ").append(x.getTimestamp().toString());
        logBuilder.append("\n").append(x.getMessage()).append("\n");
        writeContents(LOG_HEAD_FILE, logBuilder.toString());
    }

    private static void updateHeadPointerFile(String commitID) {
        File refsFile = getHeadPointerFile();
        writeContents(refsFile, commitID);
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

    public static File getHeadPointerFile() {
        String head = readContentsAsString(HEAD_FILE);
        File refsFile = join(GITLET_FOLDER, head);
        return refsFile;
    }

    public static String getHeadPointer() {
        File refsFile = getHeadPointerFile();
        return readContentsAsString(refsFile);
    }
}
