package Client;

// Package imports
import Server.Controller.RMIGateway;
import Server.Controller.RMIGatewayInterface;
import Server.IndexStorageBarrel.Objects.SearchData;
// Logging
import Logger.LogUtil;

// General imports
import java.util.List;
import java.rmi.Naming;
import java.util.Scanner;
import java.io.Serializable;

// Exception imports
import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

/**
 * The RMIClient class represents a client for the RMI (Remote Method
 * Invocation) system.
 * It allows the user to interact with the RMI gateway and perform various
 * operations.
 * The client can search for queries, find linked URLs, and access the admin
 * console.
 * The client communicates with the RMI gateway using remote method calls.
 * <p>
 * The RMIClient class implements the Serializable interface to support
 * serialization.
 * <p>
 * To use the RMIClient, provide the gateway address as a command-line argument
 * using the "-ip" flag.
 * <p>
 * Example usage:
 * java RMIClient -ip 192.168.0.1
 * <p>
 * Note: This class requires the RMI gateway to be running and accessible at the
 * specified address.
 * 
 */
public class RMIClient implements Serializable {
    /**
     * The main method of the RMIClient class.
     * 
     * @param args The command line arguments.
     */
    public static void main(String[] args) {
        new RMIClient(args);
    }

    /**
     * The RMI gateway interface used to communicate with the RMI gateway.
     */
    private RMIGatewayInterface rmiGateway;
    /**
     * The scanner used to read user input.
     */
    private transient Scanner scanner;
    /**
     * The gateway address.
     */
    private String gatewayAddress;

    /**
     * Constructs a new RMIClient object.
     *
     * @param args the command line arguments passed to the program
     */
    RMIClient(String[] args) {
        if (!processArgs(args))
            return;

        try {
            rmiGateway = (RMIGatewayInterface) Naming
                    .lookup("rmi://" + gatewayAddress + ":" + RMIGateway.PORT + "/" + RMIGateway.REMOTE_REFERENCE_NAME);
            scanner = new Scanner(System.in);
            menu();
            scanner.close();
        } catch (RemoteException | NotBoundException | MalformedURLException e) {
            System.out.println("Service unavailable. Try again later.");
            e.printStackTrace();
            System.exit(-1);
        }
    }

    /**
     * Processes the command line arguments.
     *
     * @param args the command line arguments
     * @return true if the arguments are valid, false otherwise
     */
    private boolean processArgs(String[] args) {
        if (args.length != 2) {
            LogUtil.logInfo(LogUtil.ANSI_RED, RMIClient.class,
                    "Wrong number of arguments: expected -ip <gateway address>");
            return false;
        }
        // Parse the arguments
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-ip")) {
                gatewayAddress = args[++i];
            } else {
                LogUtil.logInfo(LogUtil.ANSI_RED, RMIClient.class,
                        "Unexpected argument: " + args[i]);
                return false;
            }
        }
        return true;
    }

    /**
     * Display the menu
     * 
     * @throws RemoteException       if a remote exception occurs
     * @throws MalformedURLException if a malformed URL exception occurs
     */
    private void menu() throws RemoteException, MalformedURLException {
        do {
            printMenu();
        } while (treatChoice(readChoice()));
    }

    /**
     * Treat the choice of the user
     * 
     * @param choice the choice
     * @return true if the user wants to continue, false otherwise
     * @throws RemoteException       if a remote exception occurs
     * @throws MalformedURLException if a malformed URL exception occurs
     */
    private boolean treatChoice(int choice) throws RemoteException, MalformedURLException {
        switch (choice) {
            case 1:
                String query = readQuery();
                List<SearchData> searchResults = rmiGateway.searchQuery(query, 1);

                if (searchResults.get(0).refCount() == SearchData.EXCEPTION) {
                    System.out.println(searchResults.get(0).url());
                    break;
                }

                do {
                    printSearchResults(searchResults);
                    System.out.println("Enter page number (or 0 to exit):");
                    int pageNumber = readChoice();
                    if (pageNumber == 0)
                        break;
                    searchResults = rmiGateway.searchQuery(query, pageNumber);
                } while (true);
                break;
            case 2:
                String url = readQuery().trim();
                List<String> linkedResults = rmiGateway.getWebsitesLinkingTo(url, 1);
                printList(linkedResults);
                while (true) {
                    System.out.println("Enter page number (or 0 to exit):");
                    int pageNumber = readChoice();
                    if (pageNumber == 0)
                        break;
                    linkedResults = rmiGateway.getWebsitesLinkingTo(url, pageNumber);
                    printList(linkedResults);
                }
                break;
            case 3:
                // code to handle Admin console
                System.out.println("Entering Admin console...");
                adminMenu();
                break;
            case 4:
                System.out.println("Quitting...");
                return false;
            default:
                System.out.println("----------------------------------");
                System.out.println("Bad input.");
                break;
        }
        return true;
    }

    /**
     * Prints search results
     * 
     * @param searchResults list of search data
     */
    private void printSearchResults(List<SearchData> searchResults) {
        for (SearchData searchData : searchResults) {
            System.out.println(searchData.title());
            System.out.println(searchData.url());
            System.out.println("RefCount: " + searchData.refCount());
            System.out.println("tfIdf: " + searchData.tfIdf());
            System.out.println("--------------------------------------");
        }
    }

    /**
     * Prints the elements of the given list.
     *
     * @param results the list of strings to be printed
     */
    private void printList(List<String> results) {
        for (String result : results) {
            System.out.println(result);
        }
    }

    /**
     * Treat the choice of the admin
     * 
     * @param choice the choice
     * @return true if the user wants to continue, false otherwise
     * @throws RemoteException if a remote exception occurs
     */
    private boolean treatAdminChoice(int choice) throws RemoteException {
        switch (choice) {
            case 1:
                printList(rmiGateway.mostSearched());
                break;
            case 2:
                printList(rmiGateway.barrelsStatus());
                break;
            case 3:
                System.out.println("Exiting Admin console...");
                return false;
            default:
                System.out.println("----------------------------------");
                System.out.println("Bad input.");
                break;
        }
        return true;
    }

    /**
     * Read the query from the user
     * 
     * @return the query
     */
    private String readQuery() {
        return scanner.nextLine();
    }

    /**
     * Read the choice from the user
     * 
     * @return the choice
     */
    private int readChoice() {
        String input = scanner.nextLine();
        while (!isNumeric(input.trim())) {
            System.out.println("Invalid input. Please enter a number:");
            input = scanner.nextLine();
        }
        return Integer.parseInt(input);
    }

    /**
     * Check if the given string is numeric.
     *
     * @param str the string to check
     * @return true if the string is numeric, false otherwise
     */
    private boolean isNumeric(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Displays the admin menu and handles the user's choice.
     * This method continuously prompts the user with the admin menu options
     * until the user chooses to exit.
     *
     * @throws RemoteException if there is a remote exception while executing the
     *                         method.
     */
    private void adminMenu() throws RemoteException {
        int adminChoice;
        do {
            printAdminMenu();
            adminChoice = readChoice();
        } while (treatAdminChoice(adminChoice));
    }

    /**
     * Prints the menu options for the client application.
     */
    private void printMenu() {
        System.out.println("----------------------------------");
        System.out.println("1. Search");
        System.out.println("2. Find linked URLS");
        System.out.println("3. Admin console");
        System.out.println("4. Quit");
        System.out.println("----------------------------------");
    }

    /**
     * Prints the admin menu options.
     */
    private void printAdminMenu() {
        System.out.println("----------------------------------");
        System.out.println("1. Most searched");
        System.out.println("2. Barrels status");
        System.out.println("3. Back to main menu");
        System.out.println("----------------------------------");
    }
}