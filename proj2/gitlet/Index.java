package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.*;
import static gitlet.GitletRepository.*;
import static gitlet.Utils.*;

public class Index implements Serializable {

    private final Map<String, String> added;
    private final TreeSet<String> removal;

    public Index() {
        added = new HashMap<String, String>();
        removal = new TreeSet<String>();
    }

    public void add(String filename, String sha1) {
        added.put(filename, sha1);
        save();
    }

    /** use in condition: git added then remove, then add again*/
    public void removeFromRemoval(String filename) {
        removal.remove(filename);
        save();
    }
    public void remove(String filename) {
        added.remove(filename);
        save();
        /** remove the file in the working directory */
        File removalFile = join(CWD, filename);
        restrictedDelete(removalFile);
    }

    public void stageRemoval(String filename) {
        removal.add(filename);
        save();
        File removalFile = join(CWD, filename);
        restrictedDelete(removalFile);
    }

    public void save() {
        writeObject(INDEX_FILE, this);
    }

    public Map<String, String> getMap() {
        return this.added;
    }

    public TreeSet<String> getRemoval() {
        return this.removal;
    }

    public void clear() {
        added.clear();
        removal.clear();
        save();
    }

    public boolean stagingAreaFlag() {
        return (added.isEmpty() && removal.isEmpty());
    }

    public static Index readStagingArea() {
        return readObject(INDEX_FILE, Index.class);
    }

    public static Map<String, String> readStageMap() {
        return readStagingArea().getMap();
    }

    public static TreeSet<String> getStageRemoval() {
        return readStagingArea().getRemoval();
    }
}
