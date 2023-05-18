package gitlet;

import java.io.File;
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
    public static final File LOG_REFS_HEAD_FOLDER_MAIN_FILE = join(LOG_REFS_HEAD_FOLDER,"master");
    public static final File OBJECT_FOLDER = join(GITLET_FOLDER,"objects");
    public static final File REFS_FOLDER = join(GITLET_FOLDER,"refs");
    public static final File REFS_HEADS_FOLDER = join(REFS_FOLDER,"heads");
    public static final File LOG_REFS_MASTER_FILE = join(REFS_HEADS_FOLDER, "master");

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
        Index index = readObject(INDEX_FILE, Index.class);
        Blob blob = new Blob(filename);
        blob.save();
        index.add(blob.getFilename(),blob.getSHA1());
        /**another approach */
        //add to staging area
        //File source = join(CWD,filename);
        //byte[] content = readContents(source);
        //String SHA1 = sha1(content);
        //Index index = readObject(INDEX_FILE, Index.class);
        //index.add(filename,SHA1);
        //writeObject(INDEX_FILE,index);
    }

    public static void commit(String message){
        /** */
        index = readObject(INDEX_FILE,Index.class);
        /** get last commit map*/
        map = getLastCommitMap();
        /** get staging area map*/
        Map<String,String> stagingMap = index.getMap();
        /** minus rm file*/
        HashSet<String> removalList =  index.removal;
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
            index.remove(filename);
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

        /** write .gitlet/HEAD file */
        writeContents(LOG_REFS_MASTER_FILE,initialCommit.getSHA1().toString());

    }
    private static void createFile(){
        try{
            HEAD_FILE.createNewFile();
            INDEX_FILE.createNewFile();
            LOG_HEAD_FILE.createNewFile();
            LOG_REFS_MASTER_FILE.createNewFile();
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

    public static void updateHeadPointerFile(String sha1){
        //get current branch's head pointer
        File refsFile = getHeadPointerFile();
        writeContents(refsFile,sha1);
    }
}
