package Client;

import Server.Controller.RMIGateway.RMIGateway;
import Server.Controller.RMIGateway.RMIGatewayInterface;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Scanner;

import Logger.LogUtil;

/**
 * Client.RMIClient
 */
public class RMIClient implements RMIClientInterface, Serializable {
    public static void main(String[] args) {
        new RMIClient(args);
    }

    private RMIGatewayInterface rmiGateway;
    private transient Scanner scanner;

    private boolean onMostSearchedPage = false;
    private boolean onBarrelStatusPage = false;

    private String gatewayAddress;

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
            LogUtil.logError(LogUtil.ANSI_RED, RMIClient.class, e);
            System.out.println("RMI Gateway unavailable. Shutting down...");
            System.exit(-1);
        }
    }

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
                System.out.println("Enter query:");
                if (rmiGateway.searchQuery(readQuery()))
                    System.out.println("Links will be displayed here.");
                else
                    System.out.println("No results available.");
                break;
            case 2:
                // code to handle Admin console
                System.out.println("Entering Admin console...");
                adminMenu();
                break;
            case 3:
                System.out.println("Quitting...");
                return false;
            default:
                System.out.println("----------------------------------");
                System.out.println("Bad input.");
                break;
        }
        return true;
    }

    private boolean treatAdminChoice(int choice) throws RemoteException {
        switch (choice) {
            case 1:
                rmiGateway.addObserver(this);
                onMostSearchedPage = true;
                String mostSearched = rmiGateway.mostSearched();
                System.out.println(mostSearched);
                break;
            case 2:
                rmiGateway.addObserver(this);
                onBarrelStatusPage = true;
                String barrelsStatus = rmiGateway.barrelsStatus();
                System.out.println(barrelsStatus);
                break;
            case 3:
                rmiGateway.removeObserver(this);
                onMostSearchedPage = false;
                onBarrelStatusPage = false;
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

    private void adminMenu() throws RemoteException {
        int adminChoice;
        do {
            printAdminMenu();
            adminChoice = readChoice();
        } while (treatAdminChoice(adminChoice));
    }

    /**
     * Print the menu
     */
    private void printMenu() {
        System.out.println("----------------------------------");
        System.out.println("1. Search");
        System.out.println("2. Admin console");
        System.out.println("3. Quit");
        System.out.println("----------------------------------");
    }

    private void printAdminMenu() {
        System.out.println("----------------------------------");
        System.out.println("1. Most searched");
        System.out.println("2. Barrels status");
        System.out.println("3. Back to main menu");
        System.out.println("----------------------------------");
    }

    public void updateMostSearched() throws RemoteException {
        if (onMostSearchedPage) {
            String mostSearched = rmiGateway.mostSearched();
            System.out.println(mostSearched);
        }
    }

    public void updateBarrelStatus() throws RemoteException {
        if (onBarrelStatusPage) {
            String barrelsStatus = rmiGateway.barrelsStatus();
            System.out.println(barrelsStatus);
        }
    }

    public boolean isOnMostSearchedPage() throws RemoteException {
        return onMostSearchedPage;
    }

    public boolean isOnBarrelStatusPage() throws RemoteException {
        return onBarrelStatusPage;
    }
}