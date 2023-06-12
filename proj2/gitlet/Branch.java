package gitlet;

import java.io.File;
import java.io.Serializable;
import static gitlet.GitletRepository.*;
import static gitlet.Commit.*;
import static gitlet.Utils.*;

public class Branch implements Serializable {
    private String name;

    public Branch(String name) {
        this.name = name;
    }

    public void create() {
        /** create head pointer file */
        File branchFile = new File(REFS_HEADS_FOLDER, this.name);
        String pointer = getHeadPointer();
        if (branchFile.exists()) {
            System.out.println("A branch with that name already exists.");
        } else {
            writeContents(branchFile, pointer);
        }
    }
}
