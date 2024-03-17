package gitlet;

import gitlet.Repository;
import static gitlet.Utils.*;
import java.io.File;
import java.util.List;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author Yan Zhuang
 */

public class Main {
    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */

    public static final int ID_LENGTH = 40;
    public static void main(String[] args) {
        if (args.length == 0) {
            Utils.message("Please enter a command.");
            System.exit(0);
        }
        String firstArg = args[0];
        switch(firstArg) {
            case "init":
                /**
                 * Usage: java gitlet.Main init
                 * Creates a new Gitlet version-control system in the current directory.
                 * Start with "initial commit"
                 * 1. create persistent structure
                 * 2. create initial commit
                 * 3. serialize initial commit
                 */
                Utils.operandCheck(firstArg, args, 1);
                Repository.init();
                break;
            case "add":
                /**
                 * Usage: java gitlet.Main add [file name]
                 *
                 */
                Utils.checkGITLET();
                Utils.operandCheck(firstArg, args, 2);
                Repository.addFile2Staging(args[1]);
                break;
            case "commit":
                /**
                 * Usage: java gitlet.Main commit [message]
                 */
                Utils.checkGITLET();
                Utils.operandCheck(firstArg, args, 2);
                String message = args[1];
                if (message.isEmpty()) {
                    Utils.message("Please enter a commit message.");
                    System.exit(0);
                }
                Repository.commit(message);
                break;
            case "rm":
                /**
                 * Usage: java gitlet.Main rm [file name]
                 */
                Utils.checkGITLET();
                Utils.operandCheck(firstArg, args, 2);
                String fileName = args[1];
                Repository.remove(fileName);
                break;
            case "log":
                /**
                 * Usage: java gitlet.Main log
                 */
                Utils.checkGITLET();
                Utils.operandCheck(firstArg, args, 1);
                Repository.log();
                break;
            case "global-log":
                /**
                 * Usage: java gitlet.Main global-log
                 */
                Utils.checkGITLET();
                Utils.operandCheck(firstArg, args, 1);
                Repository.global_log();
                break;
            case "find":
                /**
                 * Usage: java gitlet.Main find [message]
                 */
                Utils.checkGITLET();
                Utils.operandCheck(firstArg, args, 2);
                List<String> ids = Repository.find(args[1]);
                if (ids.isEmpty()) {
                    Utils.message("Found no commit with that message.");
                    System.exit(0);
                }
                for (String id: ids) {
                    System.out.println(id);
                }
                break;
            case "status":
                /**
                 * Usage: java gitlet.Main status
                 */
                Utils.checkGITLET();
                Utils.operandCheck(firstArg, args, 1);
                Repository.status();
                break;
            case "checkout":
                Utils.checkGITLET();
                int length = args.length;
                if (length != 2 && length != 3 && length != 4) {
                    Utils.message(String.format("Invalid number of arguments for: %s.", args[0]));
                    System.exit(0);
                }
                switch(length){
                    case 2:
                        // Usage: java gitlet.Main checkout [branch name]
                        Repository.checkoutBranch(args[1]);
                        break;
                    case 3:
                        // Usage: java gitlet.Main checkout -- [file name]
                        if (!args[1].equals("--")) {
                            Utils.message("Incorrect operands.");
                            System.exit(0);
                        }
                        Repository.checkoutFile(args[2]);
                        break;
                    case 4:
                        // Usage: java gitlet.Main checkout [commit id] -- [file name]
                        if (!args[2].equals("--")) {
                            Utils.message("Incorrect operands.");
                            System.exit(0);
                        }
                        String fullID = args[1];
                        if (args[1].length() != ID_LENGTH) {
                            // Short uid provided, find the matching uid
                            fullID = short2FullID(args[1]);
                            if (fullID.equals("")) {
                                message("No commit with that id exists.");
                                System.exit(0);
                            }
                        }
                        Repository.checkout(fullID, args[3]);
                        break;
                }
                break;
            case "branch":
                /**
                 * Usage: java gitlet.Main branch [branch name]
                 */
                Utils.checkGITLET();
                Utils.operandCheck(firstArg, args, 2);
                Repository.branch(args[1]);
                break;
            case "rm-branch":
                /**
                 * Usage: java gitlet.Main rm-branch [branch name]
                 */
                Utils.checkGITLET();
                Utils.operandCheck(firstArg, args, 2);
                Repository.rmBranch(args[1]);
                break;
            case "reset":
                /**
                 * Usage: java gitlet.Main reset [commit id]
                 */
                Utils.checkGITLET();
                Utils.operandCheck(firstArg, args, 2);
                String fullID = args[1];
                if (args[1].length() != ID_LENGTH) {
                    // Short uid provided, find the matching uid
                    fullID = short2FullID(args[1]);
                    if (fullID.equals("")) {
                        message("No commit with that id exists.");
                        System.exit(0);
                    }
                }
                Repository.reset(fullID);
                break;
            case "merge":
                Utils.checkGITLET();
                Utils.operandCheck(firstArg, args, 1);
                // TODO: handle the `merge` command
                break;
            default:
                Utils.message("No command with that name exists.");
                System.exit(0);
        }
    }
}
