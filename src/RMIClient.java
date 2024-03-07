import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.Scanner;

/**
 * RMIClient
 */
public class RMIClient {
    public static void main(String[] args) {
        new RMIClient();
    }

    private RMIGatewayInterface rmiGateway;
    private Scanner scanner;

    RMIClient() {
        try {
            rmiGateway = (RMIGatewayInterface) LocateRegistry.getRegistry(6001)
                    .lookup("rmigateway");
            scanner = new Scanner(System.in);
            menu();
            scanner.close();
        } catch (RemoteException | NotBoundException e) {
            // TODO: Treat exception better
            System.out.println("Exception in RMIClient: " + e);
            e.printStackTrace();
        }
    }

    private void menu() throws RemoteException {
        do {
            printMenu();
        } while (treatChoice(readChoice()));
    }

    private boolean treatChoice(int choice) throws RemoteException {
        switch (choice) {
            case 1:
                System.out.println("Write below the message to be sent:");
                rmiGateway.sendMessage(readMessage());
                System.out.println("Message sent.");
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

    private String readMessage() {
        return scanner.nextLine();
    }

    private int readChoice() {
        return Integer.parseInt(scanner.nextLine());
    }

    private void printMenu() {
        System.out.println("----------------------------------");
        System.out.println("1. Send message");
        System.out.println("2. Quit");
        System.out.println("----------------------------------");
    }
}