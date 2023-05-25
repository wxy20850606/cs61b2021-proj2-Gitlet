package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static gitlet.GitletRepository.*;
import static gitlet.Utils.*;

public class Branch implements Serializable {
    private String name;
    private List commitIDHistory;

    public Branch(String name){
        this.name = name;
        //this.commitIDHistory = new ArrayList<>();
    }

    public String getBranchName(){
        return this.name;
    }

    /**

    public List getCommitIDHistory(){
        return this.commitIDHistory;
    }
     */
    public void create(){
        /** create head pointer file */
        File branchFile = new File(REFS_HEADS_FOLDER,this.name);
        File branchHistoryFile = new File(LOG_REFS_HEAD_FOLDER,this.name);
        String pointer = getHeadPointer();
        try{
            /** create refs/heads/branchName file to record branch pointer*/
            branchFile.createNewFile();
            /** create logs/refs/heads/branchName to record commit history for each branch */
            branchHistoryFile.createNewFile();
            writeContents(branchFile,pointer);
            String historyCommit = readContentsAsString(join(LOG_REFS_HEAD_FOLDER,getCurrentBranch()));
            writeContents(branchHistoryFile,historyCommit);
        }
        catch(Exception e){
            System.out.println("A branch with that name already exists.");
        }
    }
}
