package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;

import static gitlet.GitletRepository.*;

import static gitlet.Commit.getLastCommit;

public class Log implements Serializable {
    private LinkedList<Commit> log;
    private Integer index = 0;

    private class Node{
        private String date;
        private String message;
        private String id;
        private Commit prev;
        public Node(Commit x){
            this.date = x.getTimestamp().toString();
            this.id = x.getSHA1();
            this.message = x.getMessage();
        }
    }
    public Log(){
        LinkedList<Commit> log = new LinkedList<Commit>();
    }

    public void add(Commit x){
        log.add(x);
        index = index + 1;
    }

}
