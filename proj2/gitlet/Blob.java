package gitlet;

import java.io.File;
import java.util.TreeMap;
import java.util.Map;
public class Blob {
    private String ID;
    private File file;
    //private Map<String,String> IDtoFileName = new TreeMap<>();
    private String filename;
    private byte[] content;

    public Blob(){

    }
}
