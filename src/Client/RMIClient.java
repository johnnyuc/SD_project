package Client;

import Server.Controller.RMIGateway.RMIGateway;
import Server.Controller.RMIGateway.RMIGatewayInterface;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.Scanner;

import Logger.LogUtil;

/**
 * Client.RMIClient
 */
public class RMIClient {
    public static void main(String[] args) {
        new RMIClient();
    }

    private RMIGatewayInterface rmiGateway;
    private Scanner scanner;

    RMIClient() {
        try {
            rmiGateway = (RMIGatewayInterface) LocateRegistry.getRegistry(RMIGateway.PORT)
                    .lookup(RMIGateway.REMOTE_REFERENCE_NAME);
            scanner = new Scanner(System.in);
            menu();
            scanner.close();
        } catch (RemoteException | NotBoundException e) {
            LogUtil.logError(LogUtil.ANSI_RED, RMIClient.class, e);
        }
    }

    /**
     * Display the menu
     * 
     * @throws RemoteException
     */
    private void menu() throws RemoteException {
        do {
            printMenu();
        } while (treatChoice(readChoice()));
    }

    /**
     * Treat the choice of the user
     * 
     * @param choice the choice
     * @return true if the user wants to continue, false otherwise
     * @throws RemoteException
     */
    private boolean treatChoice(int choice) throws RemoteException {
        switch (choice) {
            case 1:
                System.out.println("Enter query:");
                if (rmiGateway.searchQuery(readQuery()))
                    System.out.println("Links will be displayed here.");
                else
                    System.out.println("No results available.");
                break;
            case 2:
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
     * Print the menu
     */
    private void printMenu() {
        System.out.println("----------------------------------");
        System.out.println("1. Search");
        System.out.println("2. Quit");
        System.out.println("----------------------------------");
    }
}