package gitlet;

import java.io.File;
import static gitlet.Utils.*;


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
            exit("Please enter a command.");
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
                validateNumArgs(args, 1);
                validateIfInitialized();
                GitletRepository.status();
                break;
            case "checkout":
                switch(args.length){
                    case 3:
                        if(!args[1].equals("--")){
                            exit("Incorrect operands.");
                        }
                        GitletRepository.checkoutFilename(args[2]);
                        break;
                    case 4:
                        if(!args[2].equals("--")){
                            exit("Incorrect operands.");
                        }
                        GitletRepository.checkoutCommit(args[1],args[3]);
                        break;
                    case 2:
                        GitletRepository.checkoutBranch(args[1]);
                        break;
                    default:
                        exit("Incorrect operands.");
                }
                break;
            case "branch":
                validateNumArgs(args, 2);
                validateIfInitialized();
                GitletRepository.branch(args[1]);
                break;
            case "rm-branch":
                validateNumArgs(args, 2);
                validateIfInitialized();
                GitletRepository.rmBranch(args[1]);
                break;
            case "reset":
                validateNumArgs(args, 2);
                validateIfInitialized();
                GitletRepository.reset(args[1]);
                break;
            case "merge":
                validateNumArgs(args, 2);
                validateIfInitialized();
                GitletRepository.merge(args[1]);
                break;
            default:
                exit(String.format("No command with %s exists.", args[0]));
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
            exit("Incorrect operands.");
        }
    }

    public static void validateMessage(String[] args, int n) {
        if (args.length != n) {
            exit("Please enter a commit message.");
        }
    }

    //case:Not in an initialized Gitlet directory.(check if current directory containing a .gitlet subdirectory)TODO
    public static void validateIfInitialized() {
        if (!GitletRepository.GITLET_FOLDER.exists()) {
            exit("Not in an initialized Gitlet directory.");
        }
    }

    public static void validateNotInited() {
        if (GitletRepository.GITLET_FOLDER.exists()) {
            exit("A Gitlet version-control system already exists in the current directory.");
        }
    }
}
