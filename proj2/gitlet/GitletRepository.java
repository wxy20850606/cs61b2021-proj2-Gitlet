package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

import static gitlet.Utils.writeContents;
import static gitlet.Utils.*;
import static gitlet.Index.*;
import static gitlet.Commit.*;
/**
 .gitlet
  -HEAD("ref: refs/heads/branch name",current branch pointer)
  -index(staging area,current branch's filenames to sha1s map, associates paths to blobs)
  -logs(maintain all the commit history)
    -HEAD(all commit + branch history)
    -refs
      -heads
        -master(master branch commit history)
        -other branch(other branch commit history)
  -objects(store all the blob/commit object as files)
  -refs(maintain all branch pointers,a branch is a reference to a commit)
    -heads
        -master(main branch's new reference commit id)
        -dog(other branch's new reference commit id)
 */
public class GitletRepository implements Serializable {
    /** current working directory*/
    public static final File CWD = new File(System.getProperty("user.dir"));
    public static final File GITLET_FOLDER = join(CWD,".gitlet");
    /** file to save last commit id of current branch*/
    public static final File HEAD_FILE = join(GITLET_FOLDER,"HEAD");
    public static final File INDEX_FILE = join(GITLET_FOLDER,"index");
    public static final File LOG_FOLDER = join(GITLET_FOLDER,"logs");
    public static final File LOG_HEAD_FILE = join(LOG_FOLDER,"HEAD");
    public static final File LOG_REFS_FOLDER = join(LOG_FOLDER,"refs");
    public static final File LOG_REFS_HEAD_FOLDER = join(LOG_REFS_FOLDER,"heads");
    public static final File LOG_REFS_HEAD_FOLDER_MASTER_FILE = join(LOG_REFS_HEAD_FOLDER,"master");
    public static final File OBJECT_FOLDER = join(GITLET_FOLDER,"objects");
    public static final File REFS_FOLDER = join(GITLET_FOLDER,"refs");
    public static final File REFS_HEADS_FOLDER = join(REFS_FOLDER,"heads");
    public static final File REFS_HEAD_MASTER_FILE = join(REFS_HEADS_FOLDER, "master");

    //static final File REMOVE_FOLDER = join(STEAGE_FOLDER,"remove");

    private static Index index;
    private static Commit currentCommit;
    private static Branch currentBranch;
    private static Blob currentBlob;
    private static Map<String, String> map;

/** handle the `init` command*/
    public static void init(){
        /** create all needed folder/files */
        mkDir();
        createFile();
        initializeNeededObject();
    }

    public static void add(String filename){
        if(!checkFileExistence(filename)){
            Utils.exitWithError("File does not exist.");
        };
        /** If the current working version of the file is identical to the version in the current commit, do not stage it to be added*/

        Blob blob = new Blob(filename);
        if(checkIfBlobExist(filename,blob.getSHA1())) {
            System.exit(0);
        }
        index = readObject(INDEX_FILE, Index.class);
        blob.save();
        index.add(blob.getFilename(),blob.getSHA1());
    }

    public static void commit(String message){
        /** */
        index = readObject(INDEX_FILE,Index.class);
        /** get last commit map*/
        if(index.stagingAreaFlag()){
            Utils.exitWithError("No changes added to the commit.");
        }
        map = getLastCommitMap();
        /** get staging area map*/
        Map<String,String> stagingMap = index.getMap();
        /** minus rm file*/
        TreeSet<String> removalList =  index.removal;
        for(String x : removalList){
            map.remove(x);
        }
        //System.out.println("staging map:" + stagingMap.toString());
        /** last commit map add stagingarea-added*/
        Map<String,String> newCommitMap = combine(map,stagingMap);
        //System.out.println("new map" + newCommitMap.toString());
        /** make commit*/
        Commit newCommit = new Commit(getLastCommit().getSHA1(),message,newCommitMap);
        newCommit.makeCommit();
        index.clear();
    }

    public static void rm(String filename){
        /** Unstage the file if it is currently staged for addition. delete*/
        index = readObject(INDEX_FILE,Index.class);
        currentCommit = getLastCommit();
        if(index.getMap().containsKey(filename)){
            index.stageRemoval(filename);
        }
        /** If the file is tracked in the current commit, stage it for removal ,delete*/
        else if(currentCommit.getMap().containsKey(filename)){
            index.stageRemoval(filename);
        }
        /** If the file is neither staged nor tracked by the head commit, print the error message No reason to remove the file. */
        else{
            Utils.exitWithError("No reason to remove the file.");
        }
    }

    public static void log(){
        currentCommit = getLastCommit();
        while(currentCommit != null){
            printCommitLog(currentCommit);
            if(currentCommit.getParent1SHA1() != null){
                currentCommit = currentCommit.getParent1();
            }
            else{
                break;
            }
        }
    }
    public static void globalLog(){
        String log = readContentsAsString(LOG_HEAD_FILE);
        System.out.println(log);
    }

    public static void find(String message) {
        List<String> fileNameList = getFileNameList(OBJECT_FOLDER);
        int count = 0;
        for (String fileName : fileNameList) {
            File file = join(OBJECT_FOLDER, fileName);
            try {
                currentCommit = readObject(file, Commit.class);
                if (currentCommit.getMessage().equals(message)) {
                    System.out.println(currentCommit.getSHA1());
                    count = count + 1;
                }
            } catch (IllegalArgumentException e) {
                continue;
            }
        }
        if (count == 0) {
                exitWithError("Found no commit with that message.");
            }
    }

    public static void branch(String branchName){
        Branch newbranch = new Branch(branchName);
        newbranch.create();
    }

    public static void status(){
        System.out.println("=== Branches ===");
        List<String> filenames = plainFilenamesIn(REFS_HEADS_FOLDER);
        Collections.sort(filenames);
        for (String str : filenames) {
            if(str.equals(getCurrentBranch())){
                System.out.println("*" + str);
            }
            else{
                System.out.println(str);
            }
        }
        System.out.println("\n");
        System.out.println("=== Staged Files ===");
        List<String> list = new ArrayList<>(readStagingArea().getMap().keySet());
        Collections.sort(list);
        for (String str1 : list){
            System.out.println(str1);
        }
        System.out.println("\n");
        System.out.println("=== Removed Files ===");
        for(String str2:readStagingArea().getRemoval()){
            System.out.println(str2);
        }
        System.out.println("\n");
    }
    private static void mkDir(){
        GITLET_FOLDER.mkdir();
        LOG_FOLDER.mkdir();
        LOG_REFS_FOLDER.mkdir();
        LOG_REFS_HEAD_FOLDER.mkdir();
        OBJECT_FOLDER.mkdir();
        REFS_FOLDER.mkdir();
        REFS_HEADS_FOLDER.mkdir();
    }

    private static void initializeNeededObject(){
        /** make initial commit */
        Commit initialCommit = new Commit("initial commit");
        File file = createFilepathFromSha1(initialCommit.getSHA1(),OBJECT_FOLDER);
        initialCommit.save();

        /** initialize index object and Serialize it */
        writeObject(INDEX_FILE,new Index());

        /** write .gitlet/HEAD file */
        writeContents(HEAD_FILE,"refs/heads/master");

        /** write .gitlet/refs/heads/master file */
        writeContents(REFS_HEAD_MASTER_FILE,initialCommit.getSHA1().toString());

        /** write .gitlet/logs/HEAD file */
        writeToGlobalLog(initialCommit);
    }
    private static void createFile(){
        try{
            HEAD_FILE.createNewFile();
            INDEX_FILE.createNewFile();
            LOG_HEAD_FILE.createNewFile();
            REFS_HEAD_MASTER_FILE.createNewFile();
            LOG_REFS_HEAD_FOLDER_MASTER_FILE.createNewFile();
        }
        catch(Exception e){
            System.err.println(e);
        }
    }
    public static Map<String,String> combine(Map<String,String> a,Map<String,String> b){
        Set<String> keyA = a.keySet();
        Set<String> keyB = b.keySet();
        for(String x: keyB){
            a.put(x,b.get(x));
        }
        return a;
    }
    public static File createFilepathFromSha1(String sha1,File file){
        String first2 = sha1.substring(0,2);
        String last38 = sha1.substring(2);
        File subFolder = Utils.join(file,first2);
        subFolder.mkdir();
        File filepath = Utils.join(subFolder,last38);
        return filepath;
    }
    public static File getHeadPointerFile(){
        String head = readContentsAsString(HEAD_FILE);
        //get current branch's head pointer
        File refsFile = join(GITLET_FOLDER,head);
        return refsFile;
    }

    public static String getHeadPointer(){
        File refsFile = getHeadPointerFile();
        return readContentsAsString(refsFile);
    }
    public static void updateHeadPointerFile(String sha1){
        //get current branch's head pointer
        File refsFile = getHeadPointerFile();
        writeContents(refsFile,sha1);
    }

    private static boolean checkFileExistence(String filename){
        File file = join(CWD,filename);
        return file.exists();
    }

    private static boolean checkIfBlobExist(String filename,String sha1){
        return (getLastCommit().getMap().get(filename) == sha1);
    }

    public static void printCommitLog(Commit x){
        System.out.println("===");
        System.out.println("Commit " + x.getSHA1());
        System.out.println("Date:" + x.getTimestamp().toString());
        System.out.println(x.getMessage());
        System.out.println();
    }

    public static void writeToGlobalLog(Commit x){
        String oldLog = readContentsAsString(LOG_HEAD_FILE);
        String allLog = oldLog +"\n"+"==="+"\n"+"Commit "+ x.getSHA1() + "\n" + "Date:" + x.getTimestamp().toString() + "\n" + x.getMessage() ;
        writeContents(LOG_HEAD_FILE,allLog);
    }

    public static Blob getCurrentBlob() {
        return currentBlob;
    }

    /** Loop through objects folder to get all the filenames */
    static List<String> getFileNameList(File dir) {
        List<String> list = new ArrayList<>();
        File[] files = dir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                String folderName = file.getName();
                File[] subFolderFiles = file.listFiles();
                for (File subFile : subFolderFiles) {
                    String subFileName = subFile.getName();
                    String fileName = folderName + "/" + subFileName;
                    list.add(fileName);
                }
            }
        }
        return list;
    }

    public static String getCurrentBranch(){
        byte[] HEAD = readContents(HEAD_FILE);
        int startIndex = 11;
        int endIndex = HEAD.length;
        byte[] branch = Arrays.copyOfRange(HEAD, startIndex, endIndex);
        String branchName = new String(branch);
        return branchName;
    }

    public static Index readStagingArea(){
        return readObject(INDEX_FILE, Index.class);
    }
}
