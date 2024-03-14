import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Scanner;

/**
 * AdminConsole
 */
public class AdminConsole {
    public static void main(String[] args) {
        new AdminConsole();
    }

    private String MULTICAST_ADDRESS = "224.67.68.70";
    private int PORT = 6002;
    private MulticastSocket multicastSocket;
    private Scanner scanner;

    AdminConsole() {
        try {
            // Create socket without binding it (only for sending)
            multicastSocket = new MulticastSocket();
            scanner = new Scanner(System.in);
            menu();
            scanner.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
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
                sendMulticast(readMessage());
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

    private void sendMulticast(String message) throws IOException {
        byte[] buffer = message.getBytes();

        InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, PORT);
        multicastSocket.send(packet);
    }
}