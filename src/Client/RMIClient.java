package Client;

import Server.Controller.RMIGateway.RMIGateway;
import Server.Controller.RMIGateway.RMIGatewayInterface;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Scanner;

import Logger.LogUtil;

/**
 * The RMIClient class represents a client for the RMI (Remote Method
 * Invocation) system.
 * It allows the user to interact with the RMI gateway and perform various
 * operations.
 * The client can search for queries, find linked URLs, and access the admin
 * console.
 * The client communicates with the RMI gateway using remote method calls.
 * 
 * The RMIClient class implements the Serializable interface to support
 * serialization.
 * 
 * To use the RMIClient, provide the gateway address as a command-line argument
 * using the "-ip" flag.
 * 
 * Example usage:
 * java RMIClient -ip 192.168.0.1
 * 
 * Note: This class requires the RMI gateway to be running and accessible at the
 * specified address.
 * 
 */
public class RMIClient implements Serializable {
    public static void main(String[] args) {
        new RMIClient(args);
    }

    private RMIGatewayInterface rmiGateway;
    private transient Scanner scanner;

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
            switch (args[i]) {
                case "-ip" -> gatewayAddress = args[++i];
                default -> {
                    LogUtil.logInfo(LogUtil.ANSI_RED, RMIClient.class,
                            "Unexpected argument: " + args[i]);
                    return false;
                }
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
     * @throws MalformedURLException
     */
    private boolean treatChoice(int choice) throws RemoteException, MalformedURLException {
        switch (choice) {
            case 1:
                String query = readQuery();
                List<String> results = rmiGateway.searchQuery(query, 1);
                printList(results);
                while (true) {
                    System.out.println("Enter page number (or 0 to exit):");
                    int pageNumber = readChoice();
                    if (pageNumber == 0)
                        break;
                    results = rmiGateway.searchQuery(query, pageNumber);
                    printList(results);
                }
                break;
            case 2:
                String url = readQuery().trim();
                results = rmiGateway.getWebsitesLinkingTo(url, 1);
                printList(results);
                while (true) {
                    System.out.println("Enter page number (or 0 to exit):");
                    int pageNumber = readChoice();
                    if (pageNumber == 0)
                        break;
                    results = rmiGateway.getWebsitesLinkingTo(url, pageNumber);
                    printList(results);
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
                String barrelsStatus = rmiGateway.barrelsStatus();
                System.out.println(barrelsStatus);
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
        return Integer.parseInt(scanner.nextLine());
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