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
    private static String MULTICAST_ADDRESS = "224.67.68.70";
    private static int PORT = 6002;
    private MulticastSocket multicastSocket;
    private Connection conn;
    private int id;

    public static void main(String[] args) {
        new IndexStorageBarrel(args);
    }

    /**
     * Constructs an instance of IndexStorageBarrel with the provided command-line
     * arguments.
     * Initializes the database connection and sets up the necessary database
     * schema.
     * 
     * @param args the command-line arguments passed to the program
     */
    IndexStorageBarrel(String[] args) {
        if (!processArgs(args))
            return;

        try {
            connectDatabase();
            setupDatabase();
            joinMulticastGroup();
            synchronizeDatabase();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            multicastSocket.close();
        }
    }

    /**
     * Parses the command line arguments.
     * 
     * @param args array of arguments passed from the command line
     * @return true if parsing was successful, false otherwise
     */
    private boolean processArgs(String[] args) {
        if (args.length != 2) {
            System.err.println("Wrong number of arguments: expected -id <barrel id>");
            return false;
        }

        // Parse the arguments
        try {
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "-id":
                        this.id = Integer.parseInt(args[++i]);
                        break;
                    default:
                        System.err.println("Unexpected argument: " + args[i]);
                        return false;
                }
            }
        } catch (NumberFormatException e) {
            System.err.println("Wrong type of argument: expected int for barrel id");
            return false;
        }

        return true;
    }

    /**
     * Establishes a connection to the database for the storage barrel.
     * 
     * @throws IOException if an I/O error occurs while connecting to the database
     */
    private void connectDatabase() throws IOException {
        try {
            this.conn = DriverManager.getConnection("jdbc:sqlite:data/testBarrel.db");
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

    /**
     * Sets up the database schema for the barrel application.
     */
    private static void setupDatabase() {
        String url = "jdbc:sqlite:data/testBarrel.db";
        try (Connection conn = DriverManager.getConnection(url)) {
            // SQL statements for creating tables
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

            // Execute SQL statements to create tables
            conn.createStatement().execute(websites);
            conn.createStatement().execute(keywords);
            conn.createStatement().execute(websites_keywords);

            System.out.println("Tables created successfully");
        } catch (SQLException e) {
            // TODO: Treat exception better
            System.out.println(e.getMessage());
        }
    }

    private void joinMulticastGroup() throws IOException {
        multicastSocket = new MulticastSocket(PORT); // create socket and bind it
        InetAddress mcastaddr = InetAddress.getByName(MULTICAST_ADDRESS);
        multicastSocket.joinGroup(new InetSocketAddress(mcastaddr, 0),
                NetworkInterface.getByIndex(0));
    }

    private void synchronizeDatabase() {
        // TODO: Send multicast to see if there is data to get

        // TODO: If there is data, get it

        // TODO: A thread must be running listening to multicast before trying to
        // synchronize
        return;
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