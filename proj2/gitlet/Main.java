package gitlet;

import java.io.File;


/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author TODO
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {

        // TODO: what if args is empty?
        if (args.length == 0) {
            Utils.exitWithError("Please enter a command.");
        }
        String firstArg = args[0];
        switch(firstArg) {
            case "init":
                validateNumArgs(args, 1);
                validateNotInited();
                GitletRepository.init();
                break;
            case "add":
                // TODO: handle the `add [filename]` command
                validateNumArgs(args, 2);
                validateIfInitialized();
                String filename = args[1];
                GitletRepository.add(filename);
                break;
            // TODO: FILL THE REST IN
            case "commit":
                validateMessage(args, 2);
                validateIfInitialized();
                GitletRepository.commit(args[1]);
                break;
            case "rm":
                validateNumArgs(args, 2);
                validateIfInitialized();
                GitletRepository.rm(args[1]);
                break;
            case "log":
                validateNumArgs(args, 1);
                validateIfInitialized();
                GitletRepository.log();
                break;
            case "global-log":
                validateNumArgs(args, 1);
                validateIfInitialized();
                GitletRepository.globalLog();
                break;
            case "find":
                validateNumArgs(args, 2);
                validateIfInitialized();
                GitletRepository.find(args[1]);
                break;
            case "status":
                //TODO
                break;
            case "checkout":
                //TODO
                break;
            case "branch":
                //TODO
                break;
            case "rm-branch":
                //TODO
                break;
            case "reset":
                //TODO
                break;
            case "merge":
                //TODO
                break;
            default:
                Utils.exitWithError(String.format("No command with %s exists.", args[0]));
        }
        return;
    }
    /**
     * Checks the number of arguments versus the expected number,
     * throws a RuntimeException if they do not match.
     *
     * @param cmd Name of command you are validating
     * @param args Argument array from command line
     * @param n Number of expected arguments
     */
    public static void validateNumArgs(String[] args, int n) {
        if (args.length != n) {
            Utils.exitWithError("Incorrect operands.");
        }
    }

    public static void validateMessage(String[] args, int n) {
        if (args.length != n) {
            Utils.exitWithError("Please enter a commit message.");
        }
    }

    //case:Not in an initialized Gitlet directory.(check if current directory containing a .gitlet subdirectory)TODO
    public static void validateIfInitialized() {
        if (!GitletRepository.GITLET_FOLDER.exists()) {
            Utils.exitWithError("Not in an initialized Gitlet directory.");
        }
    }

    public static void validateNotInited() {
        if (GitletRepository.GITLET_FOLDER.exists()) {
            Utils.exitWithError("A Gitlet version-control system already exists in the current directory.");
        }
    }
}
