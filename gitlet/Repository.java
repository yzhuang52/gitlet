package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static gitlet.Utils.*;
// TODO: any imports you need here

/** Represents a gitlet repository.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author Yan Zhuang
 */
public class Repository {
    /**
     * TODO: add instance variables here.
     * TODO: Refactor addstaging and rmstaging, blob should be written into objects, not addstaging, index should be stored in staging folder (add and rm)
     *
     *
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    public static final File REFS_DIR = join(GITLET_DIR, "refs");
    public static final File OBJ_DIR = join(GITLET_DIR, "objects");
    public static final File HEAD = join(GITLET_DIR, "HEAD");
    public static final File HEADS_DIR = join(REFS_DIR, "heads");
    public static final File ADD_STAGING = join(GITLET_DIR, "addStaging");
    public static final File RM_STAGING = join(GITLET_DIR, "rmStaging");
    public static final File MASTER = join(HEADS_DIR, "master");
    public static final File ADD_INDEX = join(ADD_STAGING, "addIndex");
    public static final File RM_INDEX = join(RM_STAGING, "rmIndex");
    public static final File LOG = join(GITLET_DIR, "log");
    public static final String UNIT_SEPARATOR = Character.toString( 31 );
    public static final File BRANCH_PTR = join(GITLET_DIR, "branch");
    public static boolean checkGITLET() {
        return GITLET_DIR.exists();
    }
    public static void setupPersistence() {
        /**
         * └── .gitlet
         *     └── objects
         *         ├── commits
         *         ├── blobs
         *         ├── trees
         *     ├── refs
         *         ├── heads
         *             ├── master
         *     ├── HEAD
         *     ├── addStaging
         *         ├── addIndex: stores the hash: filename pairs of currently staged file
         *     ├── rmStaging
         *         ├── rmIndex: stores the hash: filename paris of removed file
         *     ├── log
         *     ├── branch
         * Create Such .gitlet structure.
         * HEAD is the pointer to current commit. It stores the commit id.
         * Master is the pointer to the last commit of that master branch
         * index store the hash value and filename in Treemap
         * */
        if (!GITLET_DIR.exists()) {
            GITLET_DIR.mkdir();
            REFS_DIR.mkdir();
            OBJ_DIR.mkdir();
            HEADS_DIR.mkdir();
            ADD_STAGING.mkdir();
            RM_STAGING.mkdir();
            try {
                ADD_INDEX.createNewFile();
                RM_INDEX.createNewFile();
                HEAD.createNewFile();
                MASTER.createNewFile();
                LOG.createNewFile();
                BRANCH_PTR.createNewFile();
                Tree addTree = new Tree();
                Tree rmTree = new Tree();
                writeObject(ADD_INDEX, addTree);
                writeObject(RM_INDEX, rmTree);
                writeContents(BRANCH_PTR, MASTER.getName());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            message("A Gitlet version-control system already exists in the current directory.");
            System.exit(0);
        }
    }

    public static void seralizeCommit(Commit commit) {
        // Update HEAD and MASTER pointer, then seralize commit
        writeContents(HEAD, commit.getID());
        writeContents(join(HEADS_DIR, readContentsAsString(BRANCH_PTR)), commit.getID());
        commit.saveCommit();
    }
    public static void init() {
        // Delagated logic of init command
        Repository.setupPersistence();
        Commit initial = new Commit("initial commit", "");
        // Initialize a master branch and point it to initial commit
        updateLog(initial);
        seralizeCommit(initial);
    }

    public static Tree getTreeByID(String treeID) {
        if (treeID.isEmpty()) {
            return null;
        }
        File treeFolder = join(OBJ_DIR, treeID.substring(0, 2));
        File treeFile = join(treeFolder, treeID.substring(2));
        Tree tree = readObject(treeFile, Tree.class);
        return tree;
    }
    public static void addFile2Staging(String fileName) {
        Tree addTree = readObject(ADD_INDEX, Tree.class);
        /**
         * Compare file content id, if not staged, stage it
         * If same hash file exists, check if name are same, if not, change filename
         * If same file name exists, but content is different, update the hash: fileName pair
         */
        // Check if this file is removed. If so, restore it from removeIndex
        Tree rmTree = readObject(RM_INDEX, Tree.class);
        for (String hash: rmTree.keys()) {
            if (rmTree.get(hash).equals(fileName)) {
                File restore = new File(rmTree.get(hash));
                Blob blob = Utils.readObject(Utils.join(Utils.join(OBJ_DIR, hash.substring(0, 2)), hash.substring(2)), Blob.class);
                Utils.writeContents(restore, new String(blob.getFileContent()));
                rmTree.remove(hash);
                writeObject(RM_INDEX, rmTree);
                return;
            }
        }
        File file = new File(fileName);
        // Failure case
        if (!file.exists() || file.isDirectory()) {
            Utils.message("File does not exist.");
            System.exit(0);
        }
        // Create blob for file and serialize it
        Blob blob = new Blob(file.getName(), readContents(file));
        // Compare staging file to current tracked file
        Tree tree = getTreeByID(getCurrentCommit().getTreeID());
        if (tree != null) {
            // Check is not initial commit
            for (String hash: tree.keys()) {
                if (hash.equals(blob.getFileID())) {
                    if (tree.get(hash).equals(file.getName())) {
                        // Same name, same file content, already tracked
                        System.exit(0);
                    }
                }
            }
        }
        for (String hash: addTree.keys()) {
            if(addTree.get(hash).equals(file.getName())) {
                // different hash, same file name, update hash: filename pair
                addTree.remove(hash);
                addTree.put(blob.getFileID(), file.getName());
            }
        }
        // Compare staging file to current staged file
        // No case match, means different hash with different file name
        addTree.put(blob.getFileID(), file.getName());
        blob.saveBlob();
        writeObject(ADD_INDEX, addTree);
    }
    public static String getHash(File file) {
        String firstTwoHash = file.getName();
        String fullHash = firstTwoHash.concat(Objects.requireNonNull(file.listFiles())[0].getName());
        return fullHash;
    }

    public static Commit getParentCommit(Commit commit) {
        String parentID = commit.getParentID();
        if (parentID.isEmpty()) {
            return null;
        }
        File[] files = OBJ_DIR.listFiles();
        for (int i = 0; i < files.length; i++) {
            File fileFolder = files[i];
            String fullHash = getHash(fileFolder);
            if (parentID.equals(fullHash)) {
                File currentCommit = join(fileFolder, Objects.requireNonNull(fileFolder.listFiles())[0].getName());
                return readObject(currentCommit, Commit.class);
            }
        }
        return null;
    }

    public static Commit getCurrentCommit() {
        // Get the commit that HEAD points at
        String id = readContentsAsString(HEAD);
        File[] files = OBJ_DIR.listFiles();
        for (int i = 0; i < files.length; i++) {
            File fileFolder = files[i];
            String fullHash = getHash(fileFolder);
            if (id.equals(fullHash)) {
                File currentCommit = join(fileFolder, fullHash.substring(2));
                return readObject(currentCommit, Commit.class);
            }
        }
        return null;
    }

    public static void updateLog(Commit commit) {
        String log = readContentsAsString(LOG);
        final String UNIT_SEPARATOR = Character.toString( 31 ) ;
        String appendLog = String.format("%s%s%s%s%s%n", commit.getID(), UNIT_SEPARATOR, commit.getTimestamp(), UNIT_SEPARATOR, commit.getMessage());
        if (log.isEmpty()) {
            log = appendLog;
        } else {
            log = log.concat(appendLog);
        }
        writeContents(LOG, log);
    }

    public static void commit(String message) {
        Commit currentCommit = getCurrentCommit();
        assert currentCommit != null;

        Commit commit = new Commit(message, currentCommit.getID());
        Tree tree = new Tree();
        // Get the tree object from parent commit, compare with staging to decide added file or unchanged file

        /**
         * Hash: FileName in parentTree
         * 1. hash and filename not found in addIndex, means an unchanged file, put it in the new tree
         * 2. hash found in addIndex, but filename is different, means same file content but different name, change filename and put the pairs in new tree
         * 3. hash not found in addIndex, but filename found, means same file but content is changed, new blob and filename pair is added to the new tree
         */
        Tree addTree = readObject(ADD_INDEX, Tree.class);
        Tree rmTree = readObject(RM_INDEX, Tree.class);
        String parentTreeID = currentCommit.getTreeID();
        if (!parentTreeID.equals("")) {
            Tree parentTree = getTreeByID(parentTreeID);
            for (String hash: parentTree.keys()) {
                String fileName = parentTree.get(hash);
                if (!addTree.values().contains(fileName)) {
//                    System.out.println(hash + " " + fileName);
                    tree.put(hash, fileName);
                }
            }
        }

        if (addTree.isEmpty() && rmTree.isEmpty()) {
            message("No changes added to the commit.");
            System.exit(0);
        }
        if (!addTree.isEmpty()) {
            // Process addIndex
            ArrayList<String> addKeys = addTree.keys();
            for (String hash : addKeys) {
                tree.put(hash, addTree.get(hash));
            }
            addTree.clear();
            writeObject(ADD_INDEX, addTree);
        }
        if (!rmTree.isEmpty()) {
            // Process rmTree
            for (String hash: rmTree.keys()) {
                tree.remove(hash);
            }
            rmTree.clear();
            writeObject(RM_INDEX, rmTree);
        }
        // Generate treeID
        tree.generateID();
        commit.setTreeID(tree.getTreeID());
        // Serialize tree and commit
        updateLog(commit);
        seralizeCommit(commit);
        tree.saveTree();
    }

    public static void remove(String fileName) {
        /**
         * 1. If a file is added into staging but not committed, delete its blob
         * 2. If a file is committed, delete this file on working directory, and record this change on next commit
         * 3. If a file is committed, but not found in working directory, just record this change on rmIndex
         */
        Tree addTree = readObject(ADD_INDEX, Tree.class);
        Tree rmTree = readObject(RM_INDEX, Tree.class);
        for (String hash: addTree.keys()) {
            if (addTree.get(hash).equals(fileName)) {
                addTree.remove(hash);
                File blob = join(join(OBJ_DIR, hash.substring(0, 2)), hash.substring(2));
                blob.delete();
                Utils.join(OBJ_DIR, hash.substring(0, 2)).delete();
                writeObject(ADD_INDEX, addTree);
                return;
            }
        }
        Commit commit = getCurrentCommit();
        assert commit != null;
        String treeID = commit.getTreeID();
        Tree tree = getTreeByID(treeID);
        if (tree == null) {
            // Initial commit, no files are tracked
            message("No reason to remove the file.");
            System.exit(0);
        }
        for (String hash: tree.keys()) {
            if (tree.get(hash).equals(fileName)) {
                rmTree.put(hash, fileName);
                if (join(CWD, fileName).exists()) {
                    join(CWD, fileName).delete();
                }
                writeObject(RM_INDEX, rmTree);
                return;
            }
        }
    }

    public static void log() {
        Commit commit = getCurrentCommit();
        while (!commit.getParentID().isEmpty()) {
            System.out.println("===");
            System.out.printf("commit %s%nDate: %s%n%s%n", commit.getID(), commit.getTimestamp(), commit.getMessage());
            commit = getParentCommit(commit);
            System.out.println();
        }
        // Print initial commit
        System.out.println("===");
        System.out.printf("commit %s%nDate: %s%n%s%n", commit.getID(), commit.getTimestamp(), commit.getMessage());
        System.out.println();
    }

    public static void global_log() {
        String log = readContentsAsString(LOG);
        String[] logs = log.split("\n");
        for (String l: logs) {
            System.out.println(l);
            String[] infos = l.split(UNIT_SEPARATOR);
            System.out.println("===");
            // The foramt of log is commit ID timestamp message
            System.out.printf("commit %s%nDate: %s%n%s%n", infos[0], infos[1], infos[2]);
            System.out.println();
        }
    }

    public static List<String> find(String message) {
        List<String> result = new ArrayList<>();
        String log = readContentsAsString(LOG);

        String[] logs = log.split("\n");
        for (String l: logs) {
            String[] infos = l.split(UNIT_SEPARATOR);
            if (infos[2].strip().equals(message)) {
                result.add(infos[0]);
            }
        }
        return result;
    }

    public static void status() {
        System.out.println("=== Branches ===");
        // Print branches
        List<String> branches = Utils.plainFilenamesIn(HEADS_DIR);
        assert branches != null;
        for (String branch: branches) {

            if (branch.equals(readContentsAsString(BRANCH_PTR))) {
                System.out.println("*" + branch);
            } else {
                System.out.println(branch);
            }
        }
        System.out.println();
        System.out.println("=== Staged Files ===");
        // Print staged files
        Tree addTree = Utils.readObject(ADD_INDEX, Tree.class);
        List<String> fileNames = addTree.values();
        if (!fileNames.isEmpty()) {
            Collections.sort(fileNames);
            for (String fileName: fileNames) {
                System.out.println(fileName);
            }
        }
        System.out.println();
        System.out.println("=== Removed Files ===");
        // Print Removed files
        Tree rmTree = Utils.readObject(RM_INDEX, Tree.class);
        List<String> rmfileNames = rmTree.values();
        if (!rmfileNames.isEmpty()) {
            Collections.sort(rmfileNames);
            for (String fileName: rmfileNames) {
                System.out.println(fileName);
            }
        }
        System.out.println();
        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println();
        System.out.println("=== Untracked Files ===");
        System.out.println();
    }

    public static void branch(String branchName) {
        File branch = Utils.join(HEADS_DIR, branchName);
        if (branch.exists()) {
            Utils.message("A branch with that name already exists.");
            System.exit(0);
        }
        try {
            branch.createNewFile();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String currentCommitID = Utils.readContentsAsString(HEAD);
        Utils.writeContents(branch, currentCommitID);
    }

    public static void rmBranch(String branchName) {
        List<String> branches = Utils.plainFilenamesIn(HEADS_DIR);
        for (String branch: branches) {
            if (branch.equals(branchName)) {
                // Check deleted branch is not current branch
                String currentHash = Utils.readContentsAsString(Utils.join(HEADS_DIR, branch));
                if (currentHash.equals(Utils.readContentsAsString(HEAD))) {
                    Utils.message("Cannot remove the current branch.");
                    System.exit(0);
                }
                Utils.join(HEADS_DIR, branch).delete();
                return;
            }
        }
        Utils.message("A branch with that name does not exist.");
        System.exit(0);
    }

    public static Commit getCommitByID(String ID) {
        return readObject(join(join(OBJ_DIR, ID.substring(0, 2)), ID.substring(2)), Commit.class);
    }

    public static String readBlobByID(String ID) {
        return new String(readObject(join(join(OBJ_DIR), ID.substring(0, 2), ID.substring(2)), Blob.class).getFileContent());
    }

    public static void clearStaging() {
        Tree addTree = readObject(ADD_INDEX, Tree.class);
        Tree rmTree = readObject(RM_INDEX, Tree.class);
        addTree.clear();
        rmTree.clear();
        Utils.writeObject(ADD_INDEX, addTree);
        Utils.writeObject(RM_INDEX, rmTree);
    }

    public static void switchBetweenCommit(Commit checkedCommit, Commit currentCommit) {
        if (currentCommit.getMessage().equals("initial commit")) {
            if (checkedCommit.getMessage().equals("initial commit")) {
                // Both are initial commit, do nothing
                return;
            } else {
                Tree checkedTree = getTreeByID(checkedCommit.getTreeID());
                for (String hash: checkedTree.keys()) {
                    String fileName = checkedTree.get(hash);
                    File file = new File(fileName);
                    if (file.exists()) {
                        message("There is an untracked file in the way; delete it, or add and commit it first.");
                        System.exit(0);
                    }
                    writeContents(file, readBlobByID(hash));
                }
                return;
            }
        }
        Tree currentTree = getTreeByID(currentCommit.getTreeID());
        if (checkedCommit.getMessage().equals("initial commit")) {
            // Check to initial commit, no files are tracked
            for (String hash: currentTree.keys()) {
                String fileName = currentTree.get(hash);
                File file = new File(fileName);
                file.delete();
            }
            return;
        }
        /**
         * There are three types of file
         * 1. Being tracked by current commit, not tracked by checked commit. Delete it
         * 2. Being tracked by checked commit, not tracked by current commit. Just write it in fs
         * 3. Being tracked by both commit, if the blob is the same, ignore it, otherwise write checked commit version
         */
        Tree checkedTree = getTreeByID(checkedCommit.getTreeID());
        TreeMap<String, String> cache = new TreeMap<>();
        for (String hash: currentTree.keys()) {
            String fileName = currentTree.get(hash);
            if (!checkedTree.values().contains(fileName)) {
                File file = new File(fileName);
                file.delete();
            } else {
                cache.put(fileName, hash);
            }
        }
        for (String hash: checkedTree.keys()) {
            String fileName = checkedTree.get(hash);
            if (!currentTree.values().contains(fileName)) {
                File file = new File(fileName);
//                System.out.println(currentTree.values());
//                System.out.println(checkedTree.values());
                if (file.exists()) {
//                    System.out.println(fileName);
                    message("There is an untracked file in the way; delete it, or add and commit it first.");
                    System.exit(0);
                }
                writeContents(file, readBlobByID(hash));
            } else {
                String currentHash = cache.get(fileName);
                if (!hash.equals(currentHash)) {
                    writeContents(new File(fileName), readBlobByID(hash));
                }
            }
        }
    }
    public static void checkoutBranch(String branchName) {
        if (readContentsAsString(BRANCH_PTR).equals(branchName)) {
            message("No need to checkout the current branch.");
            System.exit(0);
        }
        List<String> branches =  plainFilenamesIn(HEADS_DIR);
        for (String branch: branches) {
            if (branch.equals(branchName)) {
                // Found branch, processing the file

                Commit currentCommit = getCurrentCommit();
                String checkedCommitID = readContentsAsString(join(HEADS_DIR, branchName));
                Commit checkedCommit = getCommitByID(checkedCommitID);
                assert currentCommit != null;
                switchBetweenCommit(checkedCommit, currentCommit);
                writeContents(BRANCH_PTR, branchName);
                writeContents(HEAD, readContentsAsString(join(HEADS_DIR, branchName)));
                // Clear staging area
                clearStaging();
                return;
            }
        }
        // Not find branch
        message("No such branch exists.");
        System.exit(0);
    }

    public static String readFileFromCommit(Commit commit, String fileName) {
        String treeID = commit.getTreeID();
        Tree tree = getTreeByID(treeID);
        for (String hash: tree.keys()) {
            if (tree.get(hash).equals(fileName)) {
                // Found tracked file, overwrite it if it exists, otherwise write to the CWD
                Blob blob = Utils.readObject(Utils.join(Utils.join(OBJ_DIR, hash.substring(0, 2)), hash.substring(2)), Blob.class);
                return new String(blob.getFileContent());
            }
        }
        return null;
    }
    public static void checkoutFile(String fileName) {
        Commit commit = getCurrentCommit();
        if (commit.getMessage().equals("initial commit")) {
            // Initial commit doesn't track any files
            Utils.message("File does not exist in that commit.");
            System.exit(0);
        }
        String fileContent = readFileFromCommit(commit, fileName);
        if (fileContent == null) {
            // Not Found such file
            Utils.message("File does not exist in that commit.");
            System.exit(0);
        }
        Utils.writeContents(new File(fileName), fileContent);
    }

    public static void checkout(String commitID, String fileName) {
        String logs = Utils.readContentsAsString(LOG);
        for (String log: logs.split("\n")) {
            if (log.strip().split(UNIT_SEPARATOR)[0].equals(commitID)) {
                Commit commit = Utils.readObject(Utils.join(Utils.join(OBJ_DIR, commitID.substring(0, 2)), commitID.substring(2)), Commit.class);
                String fileContent = readFileFromCommit(commit, fileName);
                if (fileContent == null) {
                    // Not Found such file
                    Utils.message("File does not exist in that commit.");
                    System.exit(0);
                }
                Utils.writeContents(new File(fileName), fileContent);
                return;
            }
        }
        Utils.message("No commit with that id exists.");
        System.exit(0);
    }

    public static void reset(String commitID) {
        Commit currentCommit = getCurrentCommit();
        String logs = Utils.readContentsAsString(LOG);
        for (String log: logs.split("\n")) {
            if (log.strip().split(UNIT_SEPARATOR)[0].equals(commitID)) {
                Commit checkedCommit = Utils.readObject(Utils.join(Utils.join(OBJ_DIR, commitID.substring(0, 2)), commitID.substring(2)), Commit.class);
                switchBetweenCommit(checkedCommit, currentCommit);
                writeContents(HEAD, commitID);
                writeContents(join(HEADS_DIR, readContentsAsString(BRANCH_PTR)), commitID);
                clearStaging();
                return;
            }
        }
        message("No commit with that id exists.");
        System.exit(0);
    }
}
