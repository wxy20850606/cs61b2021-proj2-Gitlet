package gitlet;

import java.io.File;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static gitlet.Utils.writeContents;
import static gitlet.Utils.*;
import static gitlet.Commit.*;
import static gitlet.Index.*;
/**
 .gitlet
 -HEAD("refs/heads/branch name",current branch pointer)
 -index(staging area)
 -logs
 ----HEAD(maintain all the commit history)
 -objects(store all the blob/commit object as files)
 -refs(maintain all branch pointers,a branch is a reference to a commit)
 ----heads
 -------master(main branch's new reference commit id)
 -------other branch(other branch's new reference commit id)
 */
public class GitletRepository implements Serializable {
    public static final File CWD = new File(System.getProperty("user.dir"));
    public static final File GITLET_FOLDER = join(CWD, ".gitlet");
    public static final File HEAD_FILE = join(GITLET_FOLDER, "HEAD");
    public static final File INDEX_FILE = join(GITLET_FOLDER, "index");
    public static final File LOG_FOLDER = join(GITLET_FOLDER, "logs");
    public static final File LOG_HEAD_FILE = join(LOG_FOLDER, "HEAD");
    public static final File OBJECT_FOLDER = join(GITLET_FOLDER, "objects");
    public static final File REFS_FOLDER = join(GITLET_FOLDER, "refs");
    public static final File REFS_HEADS_FOLDER = join(REFS_FOLDER, "heads");
    public static final File REFS_HEAD_MASTER_FILE = join(REFS_HEADS_FOLDER, "master");

    /** handle the `init` command*/
    public static void init() {
        /** create all needed folder/files and initialize objects*/
        mkDir();
        initializeNeededObject();
    }

    private static void mkDir() {
        GITLET_FOLDER.mkdir();
        LOG_FOLDER.mkdir();
        OBJECT_FOLDER.mkdir();
        REFS_FOLDER.mkdir();
        REFS_HEADS_FOLDER.mkdir();
    }

    private static void initializeNeededObject() {
        /** make initial commit */
        Commit initialCommit = new Commit("initial commit");
        File file = createFile(initialCommit.getCommitID(), OBJECT_FOLDER);
        initialCommit.save();

        /** initialize index object and Serialize it */
        writeObject(INDEX_FILE, new Index());

        /** write .gitlet/HEAD file */
        writeContents(HEAD_FILE, "refs/heads/master");

        /** write .gitlet/refs/heads/master file */
        writeContents(REFS_HEAD_MASTER_FILE, initialCommit.getCommitID());

        /** write .gitlet/logs/HEAD file */
        StringBuilder initLog = new StringBuilder();
        initLog.append("===\n")
                .append("commit ")
                .append(initialCommit.getCommitID())
                .append("\n");
        initLog.append("Date: ")
                .append(initialCommit.getTimestamp().toString())
                .append("\n");
        initLog.append(initialCommit.getMessage())
                .append("\n");
        writeContents(LOG_HEAD_FILE, initLog.toString());
    }

    /** handle add command */
    public static void add(String filename) {
        /** given filename exist*/
        if (checkFileExistence(filename)) {
            handleIfFileExist(filename);
        } else {
            /** given filename not exist*/
            exit("File does not exist.");
        }
    }

    private static void handleIfFileExist(String filename) {
        Index index = readStagingArea();
        TreeSet<String> removalSet = getStageRemoval();
        Blob blob = new Blob(filename);
        String blobID = blob.getSHA1();
        Map<String, String> map = getLastCommitMap();
        /** If the current file is identical to the version in the current commit,no save blob*/
        if (map.get(filename) != null && blobID.equals(map.get(filename))) {
            if (index.getRemoval().contains(filename)) {
                index.getRemoval().remove(filename);
                index.save();
            } else {
                return;
            }
            /** if in removalSet, no need to save blob*/
        } else if (removalSet.contains(filename)) {
            index.removeFromRemoval(filename);
            /** save blob and add it to staging added map*/
        } else {
            blob.save();
            index.add(filename, blob.getSHA1());
        }
        index.save();
    }

    private static boolean checkFileExistence(String filename) {
        File file = join(CWD, filename);
        return file.exists();
    }

    /** handle commit command */
    public static void commit(String message) {
        Index index = readStagingArea();
        /** If no files have been staged, abort.*/
        if (index.stagingAreaFlag()) {
            exit("No changes added to the commit.");
        }
        /** get new commit map*/
        Map<String, String> newCommitMap = getUpdatedMap();
        /** make commit*/
        Commit newCommit = new Commit(getLastCommit().getCommitID(), message, newCommitMap);
        newCommit.makeCommit();
        index.clear();
    }

    private static Map<String, String> getUpdatedMap() {
        Index index = readStagingArea();
        /** get stage added map */
        Map<String, String> map = readStageMap();
        /** combine last commit map add stagingArea map*/
        Map<String, String> newCommitMap = combine(getLastCommitMap(), map);
        /** minus rm file */
        TreeSet<String> removalList =  index.getRemoval();
        for (String x : removalList) {
            newCommitMap.remove(x);
        }
        return newCommitMap;
    }

    /** handle remove command */
    public static void rm(String filename) {
        /** Unstage the file if it is currently staged for addition. delete*/
        Index index = readStagingArea();
        if (index.getMap().containsKey(filename)) {
            index.getMap().remove(filename);
        } else if (getLastCommitMap().containsKey(filename)) {
            /** If the file is tracked in the current commit, stage it for removal ,delete*/
            index.stageRemoval(filename);
        } else {
            /** If the file is neither staged nor tracked by the head commit */
            exit("No reason to remove the file.");
        }
        index.save();
    }

    /** handle log command */
    public static void log() {
        Commit currentCommit = getLastCommit();
        while (currentCommit != null) {
            printCommitLog(currentCommit);
            if (currentCommit.getParent1ID() != null) {
                currentCommit = currentCommit.getParent1();
            } else {
                break;
            }
        }
    }

    private static void printCommitLog(Commit x) {
        System.out.println("===");
        System.out.println("commit " + x.getCommitID());
        if (x.getParent2ID() != null) {
            String parent1ID = x.getParent1ID().substring(0, 7);
            String parent2ID = x.getParent2ID().substring(0, 7);
            System.out.println("Merge: " + parent1ID + " " + parent2ID);
        }
        System.out.println("Date: " + x.getTimestamp().toString());
        System.out.println(x.getMessage());
        System.out.println();
    }

    /** handle global-log command */
    public static void globalLog() {
        String log = readContentsAsString(LOG_HEAD_FILE);
        System.out.println(log);
    }

    /** handle find command */
    public static void find(String message) {
        List<String> fileNameList = getFileNameList(OBJECT_FOLDER);
        int count = 0;
        for (String fileName : fileNameList) {
            File file = join(OBJECT_FOLDER, fileName);
            try {
                Commit currentCommit = readObject(file, Commit.class);
                if (currentCommit.getMessage().equals(message)) {
                    System.out.println(currentCommit.getCommitID());
                    count = count + 1;
                }
            } finally {
                continue;
            }
        }
        if (count == 0) {
            exit("Found no commit with that message.");
        }
    }

    /** Loop through objects folder to get all the filenames */
    private static List<String> getFileNameList(File dir) {
        List<String> list = new ArrayList<String>();
        File[] files = dir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                String folderName = file.getName();
                File[] subFolderFiles = file.listFiles();
                for (File subFile : subFolderFiles) {
                    /** use string bulider to replace + */
                    StringBuilder fileName = new StringBuilder();
                    fileName.append(folderName)
                            .append("/")
                            .append(subFile.getName());
                    list.add(fileName.toString());
                }
            }
        }
        return list;
    }

    /** handle status function*/
    public static void status() {
        StringBuilder statusBuilder = new StringBuilder();
        /** branches */
        statusBuilder.append("=== Branches ===").append("\n");
        List<String> filenames = plainFilenamesIn(REFS_HEADS_FOLDER);
        Collections.sort(filenames);
        for (String str : filenames) {
            if (str.equals(getCurrentBranch())) {
                statusBuilder.append("*").append(str).append("\n");
            } else {
                statusBuilder.append(str).append("\n");
            }
        }
        statusBuilder.append("\n");

        /** Staged files */
        statusBuilder.append("=== Staged Files ===").append("\n");
        List<String> list = new ArrayList<String>(readStageMap().keySet());
        Collections.sort(list);
        for (String str1 : list) {
            statusBuilder.append(str1).append("\n");
        }
        statusBuilder.append("\n");

        /** removed files */
        statusBuilder.append("=== Removed Files ===").append("\n");
        for (String str2:getStageRemoval()) {
            statusBuilder.append(str2).append("\n");
        }
        statusBuilder.append("\n");

        /** modifications not staged for commit */
        statusBuilder.append("=== Modifications Not Staged For Commit ===").append("\n");
        statusBuilder.append("\n");

        /** untracked files */
        statusBuilder.append("=== Untracked Files ===").append("\n");
        List<String> untrackedList = new ArrayList<String>(untrackedFiles());
        Collections.sort(untrackedList);
        for (String str3:untrackedList) {
            if (!str3.equals(".DS_Store")) {
                statusBuilder.append(str3).append("\n");
            }
        }
        statusBuilder.append("\n");
        System.out.print(statusBuilder.toString());
    }

    private static List<String> untrackedFiles() {
        List<String> list = new ArrayList<String>();
        Map<String, String> map = getLastCommitMap();
        Map<String, String> stageMap = readStageMap();
        List<String> fileList = plainFilenamesIn(CWD);
        for (String file:fileList) {
            if (map.containsKey(file) || stageMap.containsKey(file)) {
                continue;
            } else {
                list.add(file);
            }
        }
        return list;
    }

    private static boolean haveUntrackedFiles() {
        /** have .DS_Store so the size is 1*/
        return untrackedFiles().size() >= 1;
    }

    /** handle checkout -- [file name] command*/
    public static void checkoutFilename(String filename) {
        writeFileByCommit(getLastCommit().getCommitID(), filename);
    }

    private static void writeFileByCommit(String commitID, String filename) {
        /** if filename exist in current commit */
        Commit commit = readCommit(commitID);
        Map<String, String> map = commit.getMap();
        if (map.containsKey(filename)) {
            File file = join(CWD, filename);
            /** overwrite the file if it exists in the working directory */
            String sha1 = map.get(filename);
            Blob blob = readBlob(sha1);
            if (file.exists()) {
                writeContents(file, blob.getContent());
            } else {
                /** create the file if it is not in the working directory */
                writeContents(file, blob.getContent());
            }
        } else {
            /**if filename not exist in current commit */
            exit("File does not exist in that commit.");
        }
    }

    /** handle checkout [commit id] -- [file name] command*/
    public static void checkoutCommit(String commitID, String filename) {
        /** update the uid if user input short uid */
        String newCommitID = handleShortID(commitID);
        File file = createFile(newCommitID, OBJECT_FOLDER);
        if (!file.exists()) {
            exit("No commit with that id exists.");
        } else {
            writeFileByCommit(commitID, filename);
        }
    }

    private static String handleShortID(String commitID){
        if ((commitID.length() < 40) && (commitID.length() >= 4)) {
            commitID = turnShortIDToFull(commitID);
            if (commitID.equals("w")) {
                exit("No commit with that id exists.");
            }
        } else {
            return commitID;
        }
        return commitID;
    }
    private static String turnShortIDToFull(String shortID) {
        File folder = join(OBJECT_FOLDER, shortID.substring(0, 2));
        String fullID = "w";
        Integer length = shortID.length();
        if (folder.exists()) {
            List<String> allIDs = plainFilenamesIn(folder);
            for (String id:allIDs) {
                if (shortID.substring(2, length).equals(id.substring(0, length - 2))) {
                    fullID = shortID.substring(0, 2) + id;
                }
            }
        }
        return fullID;
    }

    /** handle  checkout [branch name] command*/
    public static void checkoutBranch(String branchName) {
        /** If no branch with that name exists  */
        if (!branchExist(branchName)) {
            exit("No such branch exists.");
        } else if (ifOnCurrentBranch(branchName)) {
            /** If that branch is the current branch */
            exit("No need to checkout the current branch.");
        } else if (haveUntrackedFiles()) {
            /** If a working file is untracked in the current branch */
            String x = " ";
            x = "There is an untracked file in the way; delete it, or add and commit it first.";
            System.out.println(x);
            System.exit(0);
        } else {
            /** recover all the files */
            String headCommitID = readContentsAsString(join(REFS_HEADS_FOLDER, branchName));
            Commit headCommit = readCommit(headCommitID);
            Map<String, String> targetMap = headCommit.getMap();
            for (String filename:targetMap.keySet()) {
                writeFileByCommit(headCommitID, filename);
            }
            /** delete if exist in current branch but not in the given branch */
            for (String filename:getLastCommitMap().keySet()) {
                if (!targetMap.containsKey(filename)) {
                    restrictedDelete(join(CWD, filename));
                }
            }
            /** update the HEAD file*/
            String head = "refs/heads/" + branchName;
            writeContents(HEAD_FILE, head);
        }
    }

    /** handle branch function*/
    public static void branch(String branchName) {
        List<String> branchNameList = plainFilenamesIn(REFS_HEADS_FOLDER);
        if (branchNameList.contains(branchName)) {
            exit("A branch with that name already exists.");
        }
        Branch newbranch = new Branch(branchName);
        newbranch.create();
    }

    /** handle remove branch command */
    public static void rmBranch(String branchName) {
        /** If a branch with the given name does not exist, aborts.*/
        if (!branchExist(branchName)) {
            exit("A branch with that name does not exist.");
        } else if (ifOnCurrentBranch(branchName)) {
            /** If you try to remove the branch you’re currently on, aborts.*/
            exit("Cannot remove the current branch.");
        } else {
            /** Deletes the branch with the given name. */
            File branchFile = join(REFS_HEADS_FOLDER, branchName);
            branchFile.delete();
        }
    }

    private static boolean branchExist(String branchName) {
        List<String> branchList = plainFilenamesIn(REFS_HEADS_FOLDER);
        return branchList.contains(branchName);
    }

    private static boolean ifOnCurrentBranch(String branchName) {
        String head = readContentsAsString(HEAD_FILE);
        return head.contains(branchName);
    }

    /** handle reset command */
    public static void reset(String commitID) {
        /** handel short uid */
        if ((commitID.length() < 40) && (commitID.length() >= 4)) {
            commitID = turnShortIDToFull(commitID);
            if (commitID.equals("w")) {
                exit("No commit with that id exists.");
            }
        }
        /** If no commit with the given id exist */
        File file = createFile(commitID, OBJECT_FOLDER);
        if (haveUntrackedFiles()) {
            exit("There is an untracked file in the way; delete it, or add and commit it first.");
        } else if (!file.exists()) {
            exit("No commit with that id exists.");
        } else {
            /** If a working file is untracked in the current branch */
            /** clear cwd */
            writeContents(getHeadPointerFile(), commitID);
            Commit commit = readCommit(commitID);
            List<String> files = plainFilenamesIn(CWD);
            for (String item:files) {
                if (!commit.getMap().containsKey(item)) {
                    join(CWD, item).delete();
                }
            }
            /** recover cwd */
            for (String filename : commit.getMap().keySet()) {
                writeFileByCommit(commitID, filename);
            }
            /** clear staging area */
            readStagingArea().clear();
        }
    }

    /** handle merge command */
    public static void merge(String branchName) {
        Index index = readStagingArea();
        /** check all failure cases */
        handleFailureCases(branchName);
        Commit currentHead = getLastCommit();
        Commit targetHead = readCommitByBranchName(branchName);
        Commit splitCommit = readCommit(getSplitPointID(currentHead, targetHead));
        /** If the split point is the target branch head */
        ifSplitIsGivenBranch(splitCommit, targetHead);
        /** If the split point is the current branch head */
        ifSplitIsCurrentBranch(branchName, splitCommit, currentHead);
        /** get all filenames */
        Set<String> allFile = getfileNameSet(currentHead, targetHead, splitCommit);
        /** get new merge map according to 8 steps */
        Map<String, String> newMap = getNewMergeMap(currentHead, targetHead, splitCommit);
        /** compare to current branch head commit map to handle the difference */
        Map<String, String> currentMap = currentHead.getMap();
        for (String filename :allFile) {
            boolean inCurrent = inMap(filename, currentHead);
            boolean inNewMap = newMap.containsKey(filename);
            if (inCurrent && !inNewMap) {
                /** stage for remove ,delete file */
                index.getRemoval().add(filename);
                join(CWD, filename).delete();
            } else if (inNewMap && !sameBlobID(filename, currentMap, newMap)) {
                /** stage for add ,write new file */
                String blobID = newMap.get(filename);
                index.add(filename, blobID);
                File file = join(CWD, filename);
                Blob blob = readBlob(blobID);
                writeContents(file, blob.getContent());
            } else {
                /** no need to handle rest cases*/
                continue;
            }
        }
        /** make merge commit */
        Commit mer = new Commit(currentHead, targetHead, getCurrentBranch(), branchName, newMap);
        mer.makeCommit();
        /** clear staging area*/
        index.clear();
    }

    private static void ifSplitIsGivenBranch(Commit split, Commit target) {
        if (split.getCommitID().equals(target.getCommitID())) {
            exit("Given branch is an ancestor of the current branch.");
        }
    }
    private static void ifSplitIsCurrentBranch(String branchName, Commit split, Commit current) {
        if (split.getCommitID().equals(current.getCommitID())) {
            checkoutBranch(branchName);
            exit("Current branch fast-forwarded.");
        }
    }
    private static Commit readCommitByBranchName(String branchName) {
        String headID = readContentsAsString(join(REFS_HEADS_FOLDER, branchName));
        return readCommit(headID);
    }

    private static void handleFailureCases(String branchName) {
        Index index = readStagingArea();
        /** If there are staged additions or removals present */
        if (!index.stagingAreaFlag()) {
            exit("You have uncommitted changes.");
        }
        /** If a branch with the given name does not exist,*/
        if (!join(REFS_HEADS_FOLDER, branchName).exists()) {
            exit("A branch with that name does not exist.");
        }
        /** If attempting to merge a branch with itself */
        if (branchName.equals(getCurrentBranch())) {
            exit("Cannot merge a branch with itself.");
        }
        /** If merge generate an error because the commit that it does has no changes in it */
        //pass
        /** If an untracked file in the current commit be overwritten or deleted by the merge */
        if (haveUntrackedFiles()) {
            exit("There is an untracked file in the way; delete it, or add and commit it first.");
        }
    }
    public static Map<String, String> combine(Map<String, String> a, Map<String, String> b) {
        Set<String> keyA = a.keySet();
        Set<String> keyB = b.keySet();
        for (String x: keyB) {
            a.put(x, b.get(x));
        }
        return a;
    }

    public static File createFile(String sha1, File folder) {
        String first2 = sha1.substring(0, 2);
        String last38 = sha1.substring(2);
        File subFolder = Utils.join(folder, first2);
        subFolder.mkdir();
        File filepath = Utils.join(subFolder, last38);
        return filepath;
    }

    public static String getCurrentBranch() {
        byte[] head = readContents(HEAD_FILE);
        int startIndex = 11;
        int endIndex = head.length;
        byte[] branch = Arrays.copyOfRange(head, startIndex, endIndex);
        String branchName = new String(branch);
        return branchName;
    }

    private static String getSplitPointID(Commit currentHead, Commit targetHead) {
        Map<String, Integer> map1 = getCommitDepthMap(currentHead, 0);
        Map<String, Integer> map2 = getCommitDepthMap(targetHead, 0);
        String minKey = " ";
        Integer minDepth = Integer.MAX_VALUE;
        for (String id:map1.keySet()) {
            if (map2.containsKey(id) && map2.get(id) < minDepth) {
                minKey = id;
                minDepth = map2.get(id);
            }
        }
        return minKey;
    }
    private static Map<String, Integer> getCommitDepthMap(Commit commit, Integer i) {
        Map<String, Integer> depthMap = new HashMap<String, Integer>();
        if (!commit.havaParent1()) {
            depthMap.put(commit.getCommitID(), i);
            return depthMap;
        }
        depthMap.put(commit.getCommitID(), i);
        i = i + 1;
        for (Commit x:commit.getParent()) {
            depthMap.putAll(getCommitDepthMap(x, i));
        }
        return depthMap;
    }

    private static boolean haveConflict(String fileName, Commit cur, Commit tar, Commit spl) {
        boolean inCurrent = inMap(fileName, cur);
        boolean inSplit = inMap(fileName, spl);
        boolean inTarget = inMap(fileName, tar);
        boolean inAll = inSplit && inTarget && inCurrent;
        /**  in split commit but modified in target commit and remove in current commit */
        if (inSplit && !inCurrent && inTarget && notSameBlobID(fileName, spl, tar)) {
            return true;
            /**  in split commit but modified in current commit and remove in target commit */
        } else if (inSplit && inCurrent && !inTarget && notSameBlobID(fileName, spl, cur)) {
            return true;
            /** not in split, the contents of both are changed and different from other */
        } else if (!inSplit && inTarget && inCurrent && notSameBlobID(fileName, cur, tar)) {
            return true;
            /** in all, the contents of both are changed and different from other */
        } else if (inAll && diffInThreeCommit(fileName, cur, tar, spl)) {
            return true;
        }
        return false;
    }
    private static boolean diffInThreeCommit(String fileName, Commit cur, Commit tar, Commit spl) {
        boolean x = notSameBlobID(fileName, spl, tar);
        boolean y = notSameBlobID(fileName, spl, cur);
        boolean z = notSameBlobID(fileName, tar, cur);
        return (x && y && z);
    }

    private static Map<String, String> getNewMergeMap(Commit cur, Commit tar, Commit spl) {
        boolean conflictFlag = false;
        Map<String, String> newMap = new HashMap<>();
        /** get all filename keys through combine all three map */
        Set<String> allFileNameSet = getfileNameSet(cur, tar, spl);
        for (String fileName : allFileNameSet) {
            boolean inCurrent = inMap(fileName, cur);
            boolean inSplit = inMap(fileName, spl);
            boolean inTarget = inMap(fileName, tar);
            boolean inAll = inSplit && inTarget && inCurrent;
            /** handle conflict cases*/
            if (haveConflict(fileName, cur, tar, spl)) {
                String blobID = handelMergeConflict(fileName, cur, tar);
                newMap.put(fileName, blobID);
                conflictFlag = true;
                /** handle other cases*/
                /** in all, modified in current, not modified in target*/
            } else if (inAll && modiInCur(fileName, spl, cur) && !modiInTar(fileName, spl, tar)) {
                newMap.put(fileName, cur.getMap().get(fileName));
                /** in All, modified in target, not modified in current*/
            } else if (inAll && !modiInCur(fileName, spl, cur) && modiInTar(fileName, spl, tar)) {
                newMap.put(fileName, tar.getMap().get(fileName));
                /** not in split, not in current, but in target branch*/
            } else if (!inSplit && !inCurrent && inTarget) {
                newMap.put(fileName, tar.getMap().get(fileName));
                /** not in split, not in target, but in current branch*/
            } else if (!inSplit && inCurrent && !inTarget) {
                newMap.put(fileName, cur.getMap().get(fileName));
                /** in split, deleted in current or target branch, no need to put in new map*/
            } else {
                continue;
            }
        }
        if (conflictFlag) {
            System.out.println("Encountered a merge conflict.");
        }
        return newMap;
    }

    private static boolean modiInCur(String fileName, Commit split, Commit cur) {
        return !split.getMap().get(fileName).equals(cur.getMap().get(fileName));

    }
    private static boolean modiInTar(String fileName, Commit split, Commit tar) {
        return  !split.getMap().get(fileName).equals(tar.getMap().get(fileName));
    }

    private static boolean inMap(String filename, Commit x) {
        return x.getMap().containsKey(filename);
    }
    private static Set<String> getfileNameSet(Commit a, Commit b, Commit c) {
        Set<String> allFileNameSet = new HashSet<String>();
        allFileNameSet.addAll(a.getMap().keySet());
        allFileNameSet.addAll(b.getMap().keySet());
        allFileNameSet.addAll(c.getMap().keySet());
        return allFileNameSet;
    }
    private static String handelMergeConflict(String filename, Commit cur, Commit tar) {
        String curContent = "";
        if (cur.getMap().containsKey(filename)) {
            String blobIDInCurrentBranch = cur.getMap().get(filename);
            Blob blob = readBlob(blobIDInCurrentBranch);
            byte[] currentContent = blob.getContent().getBytes();
            curContent = new String(currentContent, StandardCharsets.UTF_8);
        }

        String tarContent = "";
        if (tar.getMap().containsKey(filename)) {
            String blobIDInCurrentBranch = tar.getMap().get(filename);
            Blob blob = readBlob(blobIDInCurrentBranch);
            byte[] targetContent = blob.getContent().getBytes();
            tarContent = new String(targetContent, StandardCharsets.UTF_8);
        }

        String contents = "<<<<<<< HEAD\n" + curContent + "=======\n" + tarContent + ">>>>>>>\n";
        File conflictFile = join(CWD, filename);
        writeContents(conflictFile, contents);

        /** create new blob*/
        Blob blob = new Blob(filename);
        blob.save();
        return blob.getSHA1();
    }

    private static Blob readBlob(String blobID) {
        File file = createFile(blobID, OBJECT_FOLDER);
        return readObject(file, Blob.class);
    }

    private static Commit readCommit(String blobID) {
        File file = createFile(blobID, OBJECT_FOLDER);
        return readObject(file, Commit.class);
    }

    private static boolean notSameBlobID(String fileName, Commit x, Commit y) {
        return !x.getMap().get(fileName).equals(y.getMap().get(fileName));
    }

    private static boolean sameBlobID(String file, Map<String, String> x, Map<String, String> y) {
        boolean a = x.containsKey(file);
        boolean b = x.containsKey(file);
        if (a && !b) {
            return false;
        } else if (!a && b) {
            return false;
        } else if (a && b) {
            return x.get(file).equals(y.get(file));
        } else {
            return false;
        }
    }
}
