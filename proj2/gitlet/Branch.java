package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.List;

import static gitlet.GitletRepository.*;
import static gitlet.Utils.*;

public class Branch implements Serializable {
    private String name;
    private List commitIDHistory;

    public Branch(String name) {
        this.name = name;
    }

    public String getBranchName() {
        return this.name;
    }

    public void create(){
        /** create head pointer file */
        File branchFile = new File(REFS_HEADS_FOLDER,this.name);
        String pointer = getHeadPointer();
        try{
            /** create refs/heads/branchName file to record branch pointer*/
            branchFile.createNewFile();
            /** create logs/refs/heads/branchName to record commit history for each branch */
            writeContents(branchFile,pointer);
        }
        catch(Exception e){
            System.out.println("A branch with that name already exists.");
        }
    }
}
