package Server.IndexStorageBarrel;

// Database imports
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.io.IOException;
// Util imports
import java.io.Serializable;
// Multicast imports
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;

/**
 * Server.IndexStorageBarrel
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
            connectDatabase();
            setupDatabase();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            multicastSocket.close();
        }
    }

    private void connectDatabase() throws IOException {
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

            listenMulticast(conn);
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
                        id		 INTEGER,
                        url	 TEXT,
                        title	 TEXT,
                        description TEXT,
                        visit_date	 DATE,
                        PRIMARY KEY(id)
                    );
                    """;

            String keywords = """
                    CREATE TABLE IF NOT EXISTS keywords (
                    	text TEXT,
                    	PRIMARY KEY(text)
                    );
                    """;

            String websites_keywords = """
                    CREATE TABLE IF NOT EXISTS websites_keywords (
                        websites_id	 INTEGER,
                        keywords_text TEXT,
                        PRIMARY KEY(websites_id,keywords_text)
                    );
                    ALTER TABLE websites_keywords ADD CONSTRAINT websites_keywords_fk1 FOREIGN KEY (websites_id) REFERENCES websites(id);
                    ALTER TABLE websites_keywords ADD CONSTRAINT websites_keywords_fk2 FOREIGN KEY (keywords_text) REFERENCES keywords(text);
                    """;

            conn.createStatement().execute(websites);
            conn.createStatement().execute(keywords);
            conn.createStatement().execute(websites_keywords);

            System.out.println("Tables created successfully");
        } catch (SQLException e) {
            // TODO: Treat exception better
            System.out.println(e.getMessage());
        }
    }

    private void listenMulticast(Connection conn) throws IOException, SQLException {
        multicastSocket = new MulticastSocket(PORT); // create socket and bind it
        InetAddress mcastaddr = InetAddress.getByName(MULTICAST_ADDRESS);
        multicastSocket.joinGroup(new InetSocketAddress(mcastaddr, 0),
                NetworkInterface.getByIndex(0));

        while (true) {
            byte[] buffer = new byte[256];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            multicastSocket.receive(packet);
            System.out.println("Received packet from " +
                    packet.getAddress().getHostAddress() + ":"
                    + packet.getPort() + " with query:");
            String query = new String(packet.getData(), 0, packet.getLength());
            System.out.println(query);
            // TODO: Try inserting values into a table through the admin console
            /*
             * ResultSet rs = conn.createStatement().executeQuery(query);
             * while (rs.next())
             * // Go to next row by calling next() method
             * displayData(rs);
             * rs.close();
             */
        }
    }

    private static void displayData(ResultSet rs) throws SQLException {
        System.out.println("id:" + rs.getInt(1));
        System.out.println("url:" + rs.getString(2));
        System.out.println("title:" + rs.getString(3));
        System.out.println("description:" + rs.getString(4));
        System.out.println("keywords:" + rs.getString(5));
        System.out.println("date:" + rs.getString(6));
        System.out.println("");
    }
}