package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
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
        /** update logs/refs/heads/branchName file to record the commit history */
        updateCommitHistory(newCommit.getSHA1());
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
        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println("\n");
        System.out.println("=== Untracked Files ===");
        List<String> untrackedList = new ArrayList<>(untrackedFiles());
        Collections.sort(untrackedList);
        for(String str3:untrackedList){
            System.out.println(str3);
        }
        System.out.println("\n");
    }
    public static void checkoutFilename(String filename){
        checkoutHelper(getLastCommit(),filename);
    }

    public static void checkoutCommit(String commitID,String filename){
        File file = createFilepathFromSha1(commitID,OBJECT_FOLDER);
        if(!file.exists()){
            exitWithError("No commit with that id exists.");
        }
        else{
            Commit commit = readObject(file,Commit.class);
            checkoutHelper(commit,filename);
        }
    }

    public static void checkoutBranch(String branchName){
        /** If no branch with that name exists  */
        if(!branchExist(branchName)){
            exitWithError("No such branch exists.");
        }
        /** If that branch is the current branch */
        if(ifOnCurrentBranch(branchName)){
            exitWithError("No need to checkout the current branch.");
        }
        /** If a working file is untracked in the current branch and would be overwritten by the checkout  */
        if(haveUntrackedFiles()){
            exitWithError("There is an untracked file in the way; delete it, or add and commit it first.");
        }
        /** recover all the files */
        String headCommitID = readContentsAsString(join(REFS_HEADS_FOLDER,branchName));
        Commit headCommit = readObject(createFilepathFromSha1(headCommitID,OBJECT_FOLDER),Commit.class);
        for(String filename:headCommit.getMap().keySet()){
        checkoutHelper(headCommit,filename);
        }
        /** update the HEAD file*/
        String head = "refs/heads/" + branchName;
        writeContents(HEAD_FILE,head);
    }

    public static void rmBranch(String branchName){
        /** If a branch with the given name does not exist, aborts.*/
        if(!branchExist(branchName)){
            exitWithError("A branch with that name does not exist.");
        }
        /** If you try to remove the branch you’re currently on, aborts.*/
        else if(ifOnCurrentBranch(branchName)){
            exitWithError("Cannot remove the current branch.");
        }
        /** Deletes the branch with the given name. */
        else{
             File branchFile = join(REFS_HEADS_FOLDER,branchName);
             branchFile.delete();
        }
    }

    public static void reset(String commitID){
        /** If no commit with the given id exist */
        File file = createFilepathFromSha1(commitID,OBJECT_FOLDER);
        if(!file.exists()){
            exitWithError("No commit with that id exists.");
        }
        /** If a working file is untracked in the current branch */
        if(haveUntrackedFiles()){
            exitWithError("There is an untracked file in the way; delete it, or add and commit it first.");
        }
        writeContents(HEAD_FILE,commitID);
        Commit commit = readObject(createFilepathFromSha1(commitID,OBJECT_FOLDER),Commit.class);
        for(String filename:commit.getMap().keySet()){
            checkoutHelper(commit,filename);
        }
    }

    public static void merge(String branchName){
      /** precheck */
        /** If there are staged additions or removals present */
        if(!readStagingArea().stagingAreaFlag()){
            exitWithError("You have uncommitted changes.");
        }
        /** If a branch with the given name does not exist,*/
        if(!join(REFS_HEADS_FOLDER,branchName).exists())
        {
            exitWithError("A branch with that name does not exist.");
        }
        /** If attempting to merge a branch with itself */
        if(branchName.equals(getCurrentBranch())){
            exitWithError("Cannot merge a branch with itself.");
        }
        /** If merge would generate an error because the commit that it does has no changes in it,just let the normal commit error message for this go through.  */
        //pass
        /** If an untracked file in the current commit would be overwritten or deleted by the merge */
        if(untrackedFiles() != null)
        {
            exitWithError("There is an untracked file in the way; delete it, or add and commit it first.");
        }
        /** If the split point is the same commit as the given branch */
        String splitPoint = getSplitCommit(branchName);
        String headOfGivenBranch = readContentsAsString(join(REFS_HEADS_FOLDER,branchName));
        String headOfCurrentBranch = getHeadPointer();
        if(splitPoint.equals(headOfGivenBranch)){
            exitWithError("Given branch is an ancestor of the current branch.");
        }
        /** If the split point is the current branch */
        if(splitPoint.equals(headOfCurrentBranch)){
            checkoutBranch(branchName);
            exitWithError("Current branch fast-forwarded.");
        }
        /** get three commits in order to process 8 steps */
        Commit splitPointCommit = readObject(createFilepathFromSha1(splitPoint,OBJECT_FOLDER),Commit.class);
        Commit currentHeadCommit = getLastCommit();
        Commit targetBranchCommit = readObject(join(REFS_HEADS_FOLDER,branchName),Commit.class);
        Map<String,String> splitPointCommitMap = splitPointCommit.getMap();
        Map<String,String> currentHeadCommitMap = currentHeadCommit.getMap();
        Map<String,String> targetBranchCommitMap = targetBranchCommit.getMap();
        /** get all filename keys through combine all three map*/
        Set<String> allFileNameSet = new HashSet<>();
        allFileNameSet.addAll(splitPointCommit.getMap().keySet());
        allFileNameSet.addAll(currentHeadCommit.getMap().keySet());
        allFileNameSet.addAll(targetBranchCommit.getMap().keySet());
        /** get new merge map according to 8 steps */
        Map<String, String> newMap = getNewMergeMap(branchName);
        /** compare to current branch head commit map to get the difference*/
        /** commit */
    }
    private static void checkoutHelper(Commit commit,String filename){
        /** if filename exist in current commit */
        Map<String, String> map = commit.getMap();
        if(map.containsKey(filename)){
            File file = join(CWD,filename);
            /** overwrite the file if it exists in the working directory */
            String Sha1 = map.get(filename);
            Blob blob = readObject(createFilepathFromSha1(Sha1,OBJECT_FOLDER),Blob.class);
            if(file.exists()){
                file.delete();
                String sha1 = map.get(filename);
                try{
                    file.createNewFile();
                }
                catch(Exception e){
                    System.err.println(e);
                }
                writeContents(file,blob.getContent());
            }
            /** create the file if it is not in the working directory */
            else{
                writeContents(file,blob.getContent());
            }
        }
        /**if filename not exist in current commit */
        else{
            exitWithError("File does not exist in that commit.");
        }
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
    public static File createFilepathFromSha1(String sha1,File folder){
        String first2 = sha1.substring(0,2);
        String last38 = sha1.substring(2);
        File subFolder = Utils.join(folder,first2);
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

    private static boolean branchExist(String branchName){
        List<String> branchList = plainFilenamesIn(REFS_HEADS_FOLDER);
        return branchList.contains(branchName);
    }

    private static boolean ifOnCurrentBranch(String branchName){
        String HEAD = readContentsAsString(HEAD_FILE);
        return HEAD.contains(branchName);
    }

    private static List<String> untrackedFiles(){
        List<String> list = new ArrayList<>();
        map = getLastCommit().getMap();
        Map<String,String> stagingMap = readStagingArea().getMap();
        List<String> fileList = plainFilenamesIn(CWD);
        for(String file:fileList){
            if(map.containsKey(file) || stagingMap.containsKey(file)){
                continue;
            }
            else{
                list.add(file);
            }
        }
        return list;
    }

    private static boolean haveUntrackedFiles(){
        return untrackedFiles() != null;
    }

    public static void updateCommitHistory(String commitID){
        File file = join(LOG_REFS_HEAD_FOLDER,getCurrentBranch());
        String oldHistory = readContentsAsString(file);
        String newHistory = commitID + "\n"+ oldHistory;
        writeContents(file,newHistory);
    }
    private static String getSplitCommit(String branchName){
        String splitPoint = new String();
        String currentBranch = getCurrentBranch();
        List<String> currentBranchHistory = readCommitHistoryToList(join(LOG_REFS_HEAD_FOLDER,currentBranch));
        String mergeBranch = branchName;
        List<String> mergeBranchHistory = readCommitHistoryToList(join(LOG_REFS_HEAD_FOLDER,mergeBranch));
        for(String commitId:currentBranchHistory){
            if(mergeBranchHistory.contains(commitId)){
                splitPoint = commitId;
                break;
            }
        }
        return splitPoint;
    }

    private static List<String> readCommitHistoryToList(File file){
        List commitHistoryList = new ArrayList();
        Path filePath = file.toPath();
        List<String> lines = readLinesFromFile(filePath);
        for (String line : lines) {
            commitHistoryList.add(line);
        }
        return commitHistoryList;
    }

    private static List<String> readLinesFromFile(Path filePath) {
        List<String> lines = null;
        try {
            lines = Files.readAllLines(filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lines;
    }

    private static Map<String,String> getNewMergeMap(String branchName) {
        Map<String, String> newMap = new HashMap<>();
        /** get three commits in order to process 8 steps */
        String splitPoint = getSplitCommit(branchName);
        Commit splitPointCommit = readObject(createFilepathFromSha1(splitPoint, OBJECT_FOLDER), Commit.class);
        Commit currentHeadCommit = getLastCommit();
        Commit targetBranchCommit = readObject(join(REFS_HEADS_FOLDER, branchName), Commit.class);
        Map<String, String> splitPointCommitMap = splitPointCommit.getMap();
        Map<String, String> currentHeadCommitMap = currentHeadCommit.getMap();
        Map<String, String> targetBranchCommitMap = targetBranchCommit.getMap();
        /** get all filename keys through combine all three map*/
        Set<String> allFileNameSet = new HashSet<>();
        allFileNameSet.addAll(splitPointCommit.getMap().keySet());
        allFileNameSet.addAll(currentHeadCommit.getMap().keySet());
        allFileNameSet.addAll(targetBranchCommit.getMap().keySet());
        /** check each file according to 8 steps */
        for (String fileName : allFileNameSet) {
            /**if in splitPoint commit */
            if (splitPointCommitMap.containsKey(fileName)) {
                /** if removed in current branch head commit or removed in target branch head commit */
                if (!currentHeadCommitMap.containsKey(fileName) || !targetBranchCommitMap.containsKey(fileName)) {
                    newMap.put(fileName, null);
                } else {
                    /** files in two head commits are different from splitPoint commit, means there is a conflict*/
                    boolean x = splitPointCommitMap.get(fileName) != currentHeadCommitMap.get(fileName);
                    boolean y = splitPointCommitMap.get(fileName) != targetBranchCommitMap.get(fileName);
                    if (x && y) {
                        //handle conflict
                        continue;
                    } else if (x) {
                        newMap.put(fileName, currentHeadCommitMap.get(fileName));
                    } else if (y) {
                        newMap.put(fileName, targetBranchCommitMap.get(fileName));
                    }
                }
            }
            /** if not exist in splitPoint commit*/
            else {
                if (currentHeadCommitMap.containsKey(fileName)) {
                    newMap.put(fileName, currentHeadCommitMap.get(fileName));
                } else {
                    newMap.put(fileName, targetBranchCommitMap.get(fileName));
                }
            }
        }
    }
}
