package IndexStorageBarrel;

// Database imports
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.io.IOException;
// Util imports
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;

/**
 * IndexStorageBarrel
 */
public class IndexStorageBarrel implements Serializable {
    private String MULTICAST_ADDRESS = "224.67.68.70";
    private int PORT = 6002;
    private MulticastSocket multicastSocket;

    public static void main(String[] args) {
        new IndexStorageBarrel();
    }

    IndexStorageBarrel() {
        try {
            /*
             * multicastSocket = new MulticastSocket(PORT); // create socket and bind it
             * InetAddress mcastaddr = InetAddress.getByName(MULTICAST_ADDRESS);
             * multicastSocket.joinGroup(new InetSocketAddress(mcastaddr, 0),
             * NetworkInterface.getByIndex(0));
             * byte[] buffer = new byte[256];
             * DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
             * multicastSocket.receive(packet);
             * 
             * System.out.println("Received packet from " +
             * packet.getAddress().getHostAddress() + ":"
             * + packet.getPort() + " with message:");
             * String message = new String(packet.getData(), 0, packet.getLength());
             * System.out.println(message);
             */
            connectDatabase();
            setupDatabase();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            multicastSocket.close();
        }
    }

    private void connectDatabase() {
        // Confirming SQLite JDBC driver and setting up database
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:data/testBarrel.db")) {
            System.out.println("Connected to database");

            // Checking if database is empty
            DatabaseMetaData dbm = conn.getMetaData();
            ResultSet tables = dbm.getTables(null, null, "websites", null);
            if (tables.next()) {
                System.out.println("Database is not empty");
            } else {
                System.out.println("Database is empty");
            }

        } catch (SQLException e) {
            // TODO: Treat exception better
            throw new Error("Problem connecting to database", e);
        }
    }

    // Function to create setup database if it does not exist
    public static void setupDatabase() {
        String url = "jdbc:sqlite:data/testBarrel.db";
        try (Connection conn = DriverManager.getConnection(url)) {
            // Create tables
            String websites = """
                    CREATE TABLE IF NOT EXISTS websites (
                     id INTEGER PRIMARY KEY,
                     url TEXT NOT NULL UNIQUE,
                     title TEXT NOT NULL,
                     description TEXT NOT NULL,
                     keywords TEXT,
                     date TEXT NOT NULL
                    );
                    """;
            String last_visited = """
                    CREATE TABLE IF NOT EXISTS last_visited (
                    id INTEGER PRIMARY KEY,
                    website_id INTEGER NOT NULL,
                    date TEXT NOT NULL,
                    FOREIGN KEY (website_id) REFERENCES websites(id)
                    );
                    """;

            // Create virtual table for full text search
            String website_index = """
                    CREATE VIRTUAL TABLE IF NOT EXISTS website_index USING fts5(
                    id,
                    url,
                    title,
                    description,
                    keywords,
                    date,
                    content="websites",
                    content_rowid="id"
                    );
                    """;

            conn.createStatement().execute(websites);
            conn.createStatement().execute(last_visited);
            conn.createStatement().execute(website_index);

            System.out.println("Table created successfully");
        } catch (SQLException e) {
            // TODO: Treat exception better
            System.out.println(e.getMessage());
        }
    }
}