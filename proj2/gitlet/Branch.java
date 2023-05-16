package gitlet;

import java.io.File;
import java.io.Serializable;
import static gitlet.GitletRepository.*;

public class Branch implements Serializable {
    private String name;
    private String HEAD;

    public Branch(String name){
        this.name = name;
    }

    public File create(){
        //create head pointer file
        File branchFile = new File(REFS_HEADS_FOLDER,this.name);
        //create log branch history file
        File logFile = new File(LOG_REFS_FOLDER,this.name);
        try{
            branchFile.createNewFile();
            logFile.createNewFile();
        }
        catch(Exception e){
            System.err.println(e);
        }
        return branchFile;
    }

}
