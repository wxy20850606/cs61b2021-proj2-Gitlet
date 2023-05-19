package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.Arrays;

import static gitlet.GitletRepository.*;
import static gitlet.Utils.*;

public class Branch implements Serializable {
    private String name;

    public Branch(String name){
        this.name = name;
    }

    public void create(){
        //create head pointer file
        File branchFile = new File(REFS_HEADS_FOLDER,this.name);
        //create log branch history file
        try{
            branchFile.createNewFile();
            writeContents(branchFile,getHeadPointer());
        }
        catch(Exception e){
            System.out.println("A branch with that name already exists.");
        }
    }


}
