package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class Tree implements Serializable, Dumpable {

    private String treeID;
    private final TreeMap<String, String> tree = new TreeMap<>();

    public void setTreeID(String treeID) {
        this.treeID = treeID;
    }

    public String getTreeID() {
        return treeID;
    }

    public void put(String hash, String fileName) {
        tree.put(hash, fileName);
    }

    public String get(String hash) {
        return tree.get(hash);
    }

    public boolean containsKey(String hash) {
        return tree.containsKey(hash);
    }

    public ArrayList<String> keys() {
        return new ArrayList<>(tree.keySet());
    }

    public void clear() {
        tree.clear();
    }
    public void saveTree() {
        File treeFolder = Utils.join(Repository.OBJ_DIR, this.treeID.substring(0, 2));
        treeFolder.mkdir();
        File treeFile = Utils.join(treeFolder, treeID.substring(2));
        Utils.writeObject(treeFile, this);
    }
    public boolean isEmpty() {
        return tree.isEmpty();
    }
    public void generateID() {
        ArrayList<String> keys = keys();
        String[] fileNames = new String[keys.size()];
        for (int i = 0; i < keys.size(); i++) {
            fileNames[i] = get(keys.get(i));
        }
        treeID = Utils.sha1(keys.toString(), Arrays.toString(fileNames));
    }

    public void remove(String hash) {
        tree.remove(hash);
    }
    
    public List<String> values() {
        return new ArrayList<>(tree.values());
    }
    @Override
    public void dump() {
        System.out.printf("tree%ntreeID: %s%ntreeContent: %s%n", treeID, tree.toString());
    }
}
