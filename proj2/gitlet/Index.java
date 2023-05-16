package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import gitlet.GitletRepository.*;

import static gitlet.GitletRepository.*;
import static gitlet.Utils.readObject;
import static gitlet.Utils.writeObject;


public class Index implements Serializable {
    public final Map<String,String> added;
    //public final ArrayList<String> removal;

    public Index(){
        added = new HashMap<>();
        //removal = new ArrayList<>();
    }

    public void add(String filename,String sha1){
        added.put(filename,sha1);
    }

    public Set<String> AddedFilesNamesSet(){
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
        writeObject(INDEX_FILE,this);
    }
}
