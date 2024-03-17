package gitlet;

// TODO: any imports you need here

import java.util.Date; // TODO: You'll likely use this in this class
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.ArrayList;
import java.io.File;
import java.util.Map;
import java.util.TreeMap;

/** Represents a gitlet commit object.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author Yan Zhuang
 */
public class Commit implements Serializable, Dumpable {
    /**
     * Commit metadata should include message; timestamp; parent; blob; ID
     *
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    /** The message of this Commit. */
    private final String message;
    /** The date of this Commit. */
    private Date currentDate;
    /** The parent commit of this Commit. */
    private final String parentID;
    private final String ID;
    private final String timestamp;


    private String treeID = "";
    /** Something that keep track of what files this commit is tracking */
    public Commit(String message, String parentID) {
        this.message = message;
        this.parentID = parentID;
        if (parentID.isEmpty()) {
            this.currentDate = new Date(0);
        } else {
            this.currentDate = new Date();
        }
        this.timestamp = date2Timestamp(this.currentDate);
        this.ID = generateID();
    }

    public String getTreeID() {
        return treeID;
    }

    public String getMessage() {
        return message;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getParentID() {
        return parentID;
    }

    public String date2Timestamp(Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z", Locale.US);
        return formatter.format(date);
    }

    private String generateID() {
        // commit id should be the hashing of it's other metadata
        return Utils.sha1(this.message, this.timestamp, this.treeID, this.parentID);
    }

    public void setTreeID(String treeID) {
        this.treeID = treeID;
    }

    public String getID() {
        return ID;
    }

    public Date getCurrentDate() {
        return currentDate;
    }

    public void saveCommit() {
        // Serialize this commit on file system
        File commitFolder = Utils.join(Repository.OBJ_DIR, this.ID.substring(0, 2));
        commitFolder.mkdir();
        File commit = Utils.join(commitFolder, this.ID.substring(2));
        Utils.writeObject(commit, this);
    }

    @Override
    public void dump() {
        System.out.printf("commit%nmessage: %s%ntimestamp: %s%nID: %s%ntreeID: %s%nparentID: %s%n", message, timestamp, ID, treeID, parentID);
    }
}
