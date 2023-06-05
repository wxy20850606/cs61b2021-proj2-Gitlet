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

    public static void add(String filename) {
        /** given filename exist*/
        if (checkFileExistence(filename)) {
            Blob blob = new Blob(filename);
            index = readStagingArea();
            /** If the current working version of the file is identical to the version in the current commit, do not stage it to be added*/
            if (getLastCommit().getMap().get(filename) != null && blob.getSHA1().equals(getLastCommit().getMap().get(filename))) {
                if (index.getRemoval().contains(filename)) {
                    index.getRemoval().remove(filename);
                    index.save();
                } else {
                    return;
                }
            }
            else if (index.getRemoval().contains(filename)) {
                index.removal.remove(filename);
            }
            else {
                blob.save();
                index.add(filename, blob.getSHA1());
            }
        }
        else{
            exit("File does not exist.");
        }
    }

    public static void commit(String message){
        index = readObject(INDEX_FILE,Index.class);
        /** If no files have been staged, abort.*/
        if(index.stagingAreaFlag()){
            exit("No changes added to the commit.");
        }
        Map<String,String> stagingMap = index.getMap();
        /** last commit map add stagingArea map*/
        Map<String,String> newCommitMap = combine(getLastCommitMap(),stagingMap);
        /** minus rm file*/
        TreeSet<String> removalList =  index.removal;
        for(String x : removalList){
            newCommitMap.remove(x);
        }
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
            index.getMap().remove(filename);
            index.save();
            //index.unStage(filename);
        }
        /** If the file is tracked in the current commit, stage it for removal ,delete*/
        else if(currentCommit.getMap().containsKey(filename)){
            index.stageRemoval(filename);
            index.save();
        }
        /** If the file is neither staged nor tracked by the head commit, print the error message No reason to remove the file. */
        else{
            exit("No reason to remove the file.");
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
            exit("Found no commit with that message.");
        }
    }

    public static void branch(String branchName){
        List<String> branchNameList = plainFilenamesIn(REFS_HEADS_FOLDER);
        if(branchNameList.contains(branchName)){
            exit("A branch with that name already exists.");
        }
        Branch newbranch = new Branch(branchName);
        newbranch.create();
    }

    public static void status(){
        StringBuilder statusBuilder = new StringBuilder();
        // branches
        statusBuilder.append("=== Branches ===").append("\n");
        List<String> filenames = plainFilenamesIn(REFS_HEADS_FOLDER);
        Collections.sort(filenames);
        for (String str : filenames) {
            if(str.equals(getCurrentBranch())){
                statusBuilder.append("*").append(str).append("\n");
            }
            else{
                statusBuilder.append(str).append("\n");
            }
        }
        statusBuilder.append("\n");

        // Staged files
        statusBuilder.append("=== Staged Files ===").append("\n");
        List<String> list = new ArrayList<>(readStagingArea().getMap().keySet());
        Collections.sort(list);
        for (String str1 : list){
            statusBuilder.append(str1).append("\n");
        }
        statusBuilder.append("\n");

        // removed files
        statusBuilder.append("=== Removed Files ===").append("\n");
        for(String str2:readStagingArea().getRemoval()){
            statusBuilder.append(str2).append("\n");
        }
        statusBuilder.append("\n");

        // modifications not staged for commit
        statusBuilder.append("=== Modifications Not Staged For Commit ===").append("\n");
        statusBuilder.append("\n");

        // untracked files
        statusBuilder.append("=== Untracked Files ===").append("\n");
        List<String> untrackedList = new ArrayList<>(untrackedFiles());
        Collections.sort(untrackedList);
        for(String str3:untrackedList){
            if(!str3.equals(".DS_Store")){
                statusBuilder.append(str3).append("\n");
            }
        }
        statusBuilder.append("\n");

        System.out.print(statusBuilder);
    }
    public static void checkoutFilename(String filename){
        checkoutHelper(getLastCommit(),filename);
    }

    public static void checkoutCommit(String commitID,String filename){
        File file = createFilepathFromSha1(commitID,OBJECT_FOLDER);
        if(!file.exists()){
            exit("No commit with that id exists.");
        }
        else{
            Commit commit = readObject(file,Commit.class);
            checkoutHelper(commit,filename);
        }
    }

    public static void checkoutBranch(String branchName){

        /** If no branch with that name exists  */
        if(!branchExist(branchName)){
            exit("No such branch exists.");
        }
        /** If that branch is the current branch */
        if(ifOnCurrentBranch(branchName)){
            exit("No need to checkout the current branch.");
        }
        /** If a working file is untracked in the current branch and would be overwritten by the checkout  */
        if(haveUntrackedFiles()){
            //exit("There is an untracked file in the way;" + " delete it, or add and commit it first.");
            System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
            System.exit(0);
        }
        /** recover all the files */
        else{
            String headCommitID = readContentsAsString(join(REFS_HEADS_FOLDER,branchName));
            Commit headCommit = readObject(createFilepathFromSha1(headCommitID,OBJECT_FOLDER),Commit.class);
            Map<String,String> targetMap = headCommit.getMap();
            for(String filename:targetMap.keySet()){
                checkoutHelper(headCommit,filename);
            }
            /** delete if exist in current branch but not in the given branch */
            Commit currentCommit = getLastCommit();
            for(String filename:currentCommit.getMap().keySet()){
                if(!targetMap.containsKey(filename)){
                    restrictedDelete(join(CWD,filename));
                }
            }
            /** update the HEAD file*/
            String head = "refs/heads/" + branchName;
            writeContents(HEAD_FILE,head);}
    }

    public static void rmBranch(String branchName){
        /** If a branch with the given name does not exist, aborts.*/
        if(!branchExist(branchName)){
            exit("A branch with that name does not exist.");
        }
        /** If you try to remove the branch you’re currently on, aborts.*/
        else if(ifOnCurrentBranch(branchName)){
            exit("Cannot remove the current branch.");
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
        if(haveUntrackedFiles()){
            exit("There is an untracked file in the way; delete it, or add and commit it first.");
        }
        else if(!file.exists()){
            exit("No commit with that id exists.");
        }
        /** If a working file is untracked in the current branch */
        else{
            /** clear cwd */
            writeContents(HEAD_FILE, commitID);
            Commit commit = readObject(createFilepathFromSha1(commitID, OBJECT_FOLDER), Commit.class);
            List<String> files = plainFilenamesIn(CWD);
            for(String item:files){
                if(!commit.getMap().containsKey(item))
                    join(CWD,item).delete();
            }
            /** recover cwd */
            for (String filename : commit.getMap().keySet()) {
                checkoutHelper(commit, filename);
            }
        }
    }

    public static void merge(String branchName){
        index = readStagingArea();
        /** precheck */
        /** If there are staged additions or removals present */
        if(!readStagingArea().stagingAreaFlag()){
            exit("You have uncommitted changes.");
        }
        /** If a branch with the given name does not exist,*/
        if(!join(REFS_HEADS_FOLDER,branchName).exists())
        {
            exit("A branch with that name does not exist.");
        }
        /** If attempting to merge a branch with itself */
        if(branchName.equals(getCurrentBranch())){
            exit("Cannot merge a branch with itself.");
        }
        /** If merge would generate an error because the commit that it does has no changes in it,just let the normal commit error message for this go through.  */
        //pass
        /** If an untracked file in the current commit would be overwritten or deleted by the merge */
        if(haveUntrackedFiles())
        {
            //exit("There is an untracked file in the way;" + " delete it, or add and commit it first.");
            System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
            System.exit(0);
        }
        /** If the split point is the same commit as the given branch */
        Commit currentHeadCommit = getLastCommit();
        String headOfGivenBranch = readContentsAsString(join(REFS_HEADS_FOLDER,branchName));
        Commit targetBranchCommit = readObject(createFilepathFromSha1(headOfGivenBranch,OBJECT_FOLDER),Commit.class);
        String splitPoint = getSplitPointID(currentHeadCommit,targetBranchCommit);
        //String headOfGivenBranch = readContentsAsString(join(REFS_HEADS_FOLDER,branchName));
        String headOfCurrentBranch = getHeadPointer();
        if(splitPoint.equals(headOfGivenBranch)){
            exit("Given branch is an ancestor of the current branch.");
        }
        /** If the split point is the current branch */
        if(splitPoint.equals(headOfCurrentBranch)){
            checkoutBranch(branchName);
            exit("Current branch fast-forwarded.");
        }
        /** get three commits in order to process 8 steps */
        Commit splitPointCommit = readObject(createFilepathFromSha1(splitPoint,OBJECT_FOLDER),Commit.class);
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
        for(String filename:allFileNameSet){
            boolean existInCurrentHead = currentHeadCommitMap.containsKey(filename);
            boolean existInNewMap = newMap.containsKey(filename);
            if(existInCurrentHead && !existInNewMap){
                index.removal.add(filename);
                join(CWD,filename).delete();
            }
            /** stage for add ,create new file*/
            else if(!existInCurrentHead && existInNewMap){
                String sha1 = newMap.get(filename);
                index.add(filename,sha1);
                File file = join(CWD,filename);
                try{
                    file.createNewFile();
                }
                catch(Exception e){
                    System.err.println(e);
                }
                Blob blob = readObject(createFilepathFromSha1(sha1,OBJECT_FOLDER),Blob.class);
                writeContents(file,blob.getContent());
            }
            /** stage for add ,replace file*/
            else if(existInCurrentHead && existInNewMap && !currentHeadCommitMap.get(filename).equals(newMap.get(filename))){
                String sha1 = newMap.get(filename);
                index.add(filename,sha1);
                File file = join(CWD,filename);
                Blob blob = readObject(createFilepathFromSha1(sha1,OBJECT_FOLDER),Blob.class);
                writeContents(file,blob.getContent());
            }
            else{
                continue;
            }
        }
        /** make merge commit */
        Commit mergeCommit = new Commit(currentHeadCommit,targetBranchCommit,getCurrentBranch(),branchName,newMap);
        mergeCommit.makeCommit();
        /** update logs/refs/heads file*/
        updateCommitHistory(mergeCommit.getSHA1());
        //Commit currentHeadCommit,Commit targetBranchHead,String currentBranch,String targetBranch,Map<String,String> newmap
        /** clear staging area*/
        index.clear();
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
                writeContents(file,blob.getContent());
            }
            /** create the file if it is not in the working directory */
            else{
                try{
                    file.createNewFile();
                }
                catch(Exception e){
                    System.err.println(e);
                }
                writeContents(file,blob.getContent());
            }
        }
        /**if filename not exist in current commit */
        else{
            exit("File does not exist in that commit.");
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
        writeContents(REFS_HEAD_MASTER_FILE,initialCommit.getSHA1());

        /** write .gitlet/logs/HEAD file */
        writeToGlobalLog(initialCommit);
        /** write .gitlet/logs/refs/heads/master file */
        writeContents(LOG_REFS_HEAD_FOLDER_MASTER_FILE,initialCommit.getSHA1());
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
        System.out.println("commit " + x.getSHA1());
        if(x.getParent2ID() != null){
            System.out.println("Merge: " + x.getParent1ID().substring(0,7) + " " + x.getParent2ID().substring(0,7));
        }
        System.out.println("Date: " + x.getTimestamp().toString());
        System.out.println(x.getMessage());
        System.out.println();
    }

    public static void writeToGlobalLog(Commit x){
        String oldLog = readContentsAsString(LOG_HEAD_FILE);
        String allLog = oldLog +"\n"+"==="+"\n"+"commit "+ x.getSHA1() + "\n" + "Date: " + x.getTimestamp().toString() + "\n" + x.getMessage()+ "\n"  ;
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
        /**if(fileList.contains(".DS_Store")){
         fileList.remove(".DS_Store");
         } */
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
        /** have .DS_Store so the size is 1*/
        return untrackedFiles().size() > 1;
    }

    public static void updateCommitHistory(String commitID){
        File file = join(LOG_REFS_HEAD_FOLDER,getCurrentBranch());
        String oldHistory = readContentsAsString(file);
        String newHistory = commitID + "\n"+ oldHistory;
        writeContents(file,newHistory);
    }

    private static String getSplitPointID(Commit currentHead,Commit targetHead){
        Map<String,Integer> map1 = getCommitDepthMap(currentHead,0);
        Map<String,Integer> map2 = getCommitDepthMap(targetHead,0);
        String minKey = " ";
        Integer minDepth = Integer.MAX_VALUE;
        for(String id:map1.keySet()){
            if(map2.containsKey(id) && map2.get(id) < minDepth){
                minKey= id;
                minDepth = map2.get(id);
            }
        }
        return minKey;
    }
    private static Map<String,Integer> getCommitDepthMap(Commit commit,Integer i){
        Map<String,Integer> map = new HashMap<>();
        if(!commit.havaParent1()){
            map.put(commit.getSHA1(),i);
            return map;
        }
        map.put(commit.getSHA1(),i);
        i = i + 1;
        for(Commit x:commit.getParent()){
            map.putAll(getCommitDepthMap(x,i));
        }
        return map;
    }

    private static Map<String,String> getNewMergeMap(String branchName) {
        boolean conflictFlag = false;
        Map<String, String> newMap = new HashMap<>();
        /** get three commits in order to process 8 steps */
        String targetBranchID = readContentsAsString(join(REFS_HEADS_FOLDER, branchName));
        Commit currentHeadCommit = getLastCommit();
        Commit targetBranchCommit = readObject(createFilepathFromSha1(targetBranchID, OBJECT_FOLDER), Commit.class);
        String splitPoint = getSplitPointID(currentHeadCommit,targetBranchCommit);
        Commit splitPointCommit = readObject(createFilepathFromSha1(splitPoint, OBJECT_FOLDER), Commit.class);
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
                    continue;
                } else {
                    /** files in two head commits are different from splitPoint commit, means there is a conflict*/
                    boolean x = (splitPointCommitMap.get(fileName) != currentHeadCommitMap.get(fileName));
                    boolean y = (splitPointCommitMap.get(fileName) != targetBranchCommitMap.get(fileName));
                    if (x && y) {
                        newMap.put(fileName,handelMergeConflict(fileName,currentHeadCommitMap,targetBranchCommitMap));
                        conflictFlag = true;
                    } else if (x) {
                        newMap.put(fileName, currentHeadCommitMap.get(fileName));
                    } else if (y) {
                        newMap.put(fileName, targetBranchCommitMap.get(fileName));
                    }
                }
            }
            /** if not exist in splitPoint commit*/
            else {
                if (currentHeadCommitMap.containsKey(fileName) && !targetBranchCommitMap.containsKey(fileName)) {
                    newMap.put(fileName, currentHeadCommitMap.get(fileName));
                }
                else if(!currentHeadCommitMap.containsKey(fileName) && targetBranchCommitMap.containsKey(fileName)) {
                    newMap.put(fileName, targetBranchCommitMap.get(fileName));
                }
                else{
                    newMap.put(fileName,handelMergeConflict(fileName,currentHeadCommitMap,targetBranchCommitMap));
                    conflictFlag = true;
                }
            }
        }
        if(conflictFlag == true){
            System.out.println("Encountered a merge conflict.");}
        return newMap;

    }

    private static String handelMergeConflict(String filename,Map<String,String> a,Map<String,String> b){
        String commitIDInCurrentBranch = a.get(filename);
        String commitIDInTargetBranch = b.get(filename);
        Blob currentBranchBlob = readObject(createFilepathFromSha1(commitIDInCurrentBranch,OBJECT_FOLDER),Blob.class);
        Blob targetBranchBlob = readObject(createFilepathFromSha1(commitIDInTargetBranch,OBJECT_FOLDER),Blob.class);
        String conflictContent = "<<<<<<< HEAD" + "\n" + currentBranchBlob.getContent()+"\n"  +"======="+ "\n" + targetBranchBlob.getContent() +"\n"+">>>>>>>"+"\n";
        /** create new blob*/
        Blob blob = new Blob(filename,conflictContent);
        blob.save();
        return blob.getSHA1();
    }

    private static void replaceCWD(String filename,String sha1){
        /** overwrite exist file*/
        File file = join(CWD,filename);
        Blob blob = readObject(createFilepathFromSha1(sha1,OBJECT_FOLDER),Blob.class);
        String content = blob.getContent();
        if(!file.exists()){
            try{
                file.createNewFile();
            }
            catch(Exception e){
                System.err.println(e);
            }
        }
        writeContents(file,content);
    }

    private static Map<String,String> removeNullValue(Map<String,String> map){
        Iterator<Map.Entry<String, String>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            if (entry.getValue() == null) {
                iterator.remove();
            }
        }
        return map;
    }
}
