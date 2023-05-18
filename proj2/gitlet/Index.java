package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.*;

import gitlet.GitletRepository.*;

import static gitlet.GitletRepository.*;
import static gitlet.Utils.*;


public class Index implements Serializable {
    public final Map<String,String> added;
    public final HashSet<String> removal;

    public Index(){
        added = new HashMap<>();
        removal = new HashSet<>();
    }

    public void add(String filename,String sha1){
        added.put(filename,sha1);
        save();
    }

    public void remove(String filename){
        added.remove(filename);
        save();
        /** remove the file in the working directory */
        File removalFile = join(CWD,filename);
        removalFile.delete();
    }

    public void stageRemoval(String filename){
        removal.add(filename);
        save();
        File removalFile = join(CWD,filename);
        removalFile.delete();
    }

    public void unStage(String filename){
        if(addedFilesNamesSet().contains(filename)){
        added.remove(filename);
        save();
        }
        return;
    }
    public Set<String> addedFilesNamesSet(){
        Set<String> keySet = added.keySet();
        return keySet;
    }

    public static Index fromFile(){
        return readObject(INDEX_FILE, Index.class);
    }
    public void save(){
        writeObject(INDEX_FILE,this);
    }

    public Map<String,String> getMap(){
        return this.added;
    }

    public void clear(){
        added.clear();
        save();
    }
    public boolean stagingAreaFlag(){
        return (added.isEmpty() && removal.isEmpty());
    }
}
