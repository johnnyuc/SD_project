package Server.Controller;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.util.Scanner;

import Logger.LogUtil;
import Server.Controller.RMIGateway.RMIGateway;
import Server.Controller.RMIGateway.RMIGatewayInterface;

/**
 * Server.Controller.AdminConsole
 */
public class AdminConsole {
    public static void main(String[] args) {
        new AdminConsole();
    }

    private RMIGatewayInterface rmiGateway;
    private Scanner scanner;

    AdminConsole() {
        try {
            rmiGateway = (RMIGatewayInterface) Naming
                    .lookup("rmi://localhost:" + RMIGateway.PORT + "/" + RMIGateway.REMOTE_REFERENCE_NAME);
            scanner = new Scanner(System.in);
            menu();
            scanner.close();
        } catch (IOException | NotBoundException e) {
            LogUtil.logError(LogUtil.ANSI_RED, AdminConsole.class, e);
        }

    }

    private void menu() throws IOException {
        do {
            printMenu();
        } while (treatChoice(readChoice()));
    }

    private boolean treatChoice(int choice) throws IOException {
        switch (choice) {
            case 1:
                System.out.println("Write below the SQL query to be sent:");
                System.out.println("SQL query sent.");
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
        System.out.println("1. Send SQL query");
        System.out.println("2. Quit");
        System.out.println("----------------------------------");
    }
}