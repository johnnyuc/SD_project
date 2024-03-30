package Server.IndexStorageBarrel;

// Database imports
import java.sql.*;
import java.io.IOException;

// Util imports
import java.io.Serializable;

// Multicast imports
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;

import static Server.IndexStorageBarrel.Operations.BarrelSetup.*;

public class IndexStorageBarrel implements Serializable {
    private static Connection conn;
    private static MulticastSocket multicastSocket;
    private static String multicastGroupAddress;
    private static int multicastPort;
    private int id;

    public static void main(String[] args) {
        IndexStorageBarrel indexStorageBarrel = new IndexStorageBarrel(args);
        indexStorageBarrel.populateTables();
        indexStorageBarrel.displayTables();
    }

    IndexStorageBarrel(String[] args) {
        if (!processArgs(args))
            return;

        try {
            conn = connectDatabase();
            databaseIntegrity(conn);
            joinMulticastGroup(multicastGroupAddress, multicastPort);
            synchronizeDatabase();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                System.out.println(ex.getMessage());
            }
            multicastSocket.close();
        }
    }

    private boolean processArgs(String[] args) {
        if (args.length != 6) {
            System.err.println("Wrong number of arguments: expected -id <barrel id> -mcast <multicast group address> -port <port number>");
            return false;
        }

        // Parse the arguments
        try {
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "-id" -> id = Integer.parseInt(args[++i]);
                    case "-mcast" -> multicastGroupAddress = args[++i];
                    case "-port" -> multicastPort = Integer.parseInt(args[++i]);
                    default -> {
                        System.err.println("Unexpected argument: " + args[i]);
                        return false;
                    }
                }
            }
        } catch (NumberFormatException e) {
            System.err.println("Wrong type of argument: expected int for barrel id and port number");
            return false;
        }

        return true;
    }

    private Connection connectDatabase() {
        Connection conn;
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:data/testBarrel.db");
            System.out.println("Connected to database");
        } catch (SQLException e) {
            throw new Error("Problem connecting to database", e);
        }
        return conn;
    }

    private void joinMulticastGroup(String multicastGroupAddress, int port) throws IOException {
        multicastSocket = new MulticastSocket(port); // create socket and bind it
        InetAddress mcastaddr = InetAddress.getByName(multicastGroupAddress);
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

    private void populateTables() {
        String url = "jdbc:sqlite:data/testBarrel.db";

        try (Connection conn = DriverManager.getConnection(url)) {
            // Insert data into websites table
            String sql = "INSERT INTO websites(url, title, description) VALUES(?,?,?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                for (int i = 0; i < 5; i++) { // Insert 100 rows of random data
                    pstmt.setString(1, "www.site" + i + ".com");
                    pstmt.setString(2, "Title " + i);
                    pstmt.setString(3, "Description " + i);
                    pstmt.executeUpdate();

                    // Get the generated website id
                    try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            int websiteId = generatedKeys.getInt(1);

                            // Insert data into urls and website_urls tables
                            String urlSql = "INSERT OR IGNORE INTO urls(url) VALUES(?)";
                            String websiteUrlSql = "INSERT INTO website_urls(website_id, url_id) VALUES(?,?)";
                            String selectUrlSql = "SELECT id FROM urls WHERE url = ?";
                            try (PreparedStatement urlPstmt = conn.prepareStatement(urlSql, Statement.RETURN_GENERATED_KEYS);
                                 PreparedStatement websiteUrlPstmt = conn.prepareStatement(websiteUrlSql);
                                 PreparedStatement selectUrlPstmt = conn.prepareStatement(selectUrlSql)) {
                                for (int k = 0; k < 5; k++) { // Insert 10 urls for each website
                                    String urlToInsert = "www.url" + i + "_" + k + ".com"; // Make URLs unique for each website
                                    urlPstmt.setString(1, urlToInsert);
                                    urlPstmt.executeUpdate();

                                    // Get the id of the url
                                    selectUrlPstmt.setString(1, urlToInsert);
                                    ResultSet rs = selectUrlPstmt.executeQuery();
                                    if (rs.next()) {
                                        int urlId = rs.getInt(1);

                                        // Insert into website_urls table
                                        websiteUrlPstmt.setInt(1, websiteId);
                                        websiteUrlPstmt.setInt(2, urlId);
                                        websiteUrlPstmt.executeUpdate();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private void displayTables() {
        String url = "jdbc:sqlite:data/testBarrel.db";

        try (Connection conn = DriverManager.getConnection(url)) {
            // Display data in tables
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT * FROM websites");
                System.out.println("Websites:");
                displayData(rs);

                rs = stmt.executeQuery("SELECT * FROM keywords");
                System.out.println("Keywords:");
                displayData(rs);

                rs = stmt.executeQuery("SELECT * FROM urls");
                System.out.println("URLs:");
                displayData(rs);

                rs = stmt.executeQuery("SELECT * FROM website_keywords");
                System.out.println("Website Keywords:");
                displayData(rs);

                rs = stmt.executeQuery("SELECT * FROM website_urls");
                System.out.println("Website URLs:");
                displayData(rs);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void displayData(ResultSet rs) throws SQLException {
        ResultSetMetaData rsmd = rs.getMetaData();
        int columnsNumber = rsmd.getColumnCount();
        while (rs.next()) {
            for (int i = 1; i <= columnsNumber; i++) {
                if (i > 1) System.out.print(",  ");
                String columnValue = rs.getString(i);
                System.out.print(rsmd.getColumnName(i) + ": " + columnValue);
            }
            System.out.println();
        }
    }
}