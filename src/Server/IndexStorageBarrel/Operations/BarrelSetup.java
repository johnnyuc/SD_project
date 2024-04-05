package Server.IndexStorageBarrel.Operations;

import java.sql.*;
import java.util.*;

import Logger.LogUtil;

/**
 * The BarrelSetup class provides methods for setting up and maintaining the
 * integrity of the database tables.
 * It includes methods for checking the consistency of the tables and dropping
 * them if necessary, as well as
 * methods for creating the necessary tables in the database.
 */

public class BarrelSetup {

    /**
     * Checks the integrity of the database by comparing the existing tables with
     * the expected tables.
     * If the number of tables is inconsistent or certain table names are not
     * present, the method drops
     * the existing tables and sets up the database again.
     *
     * @param conn   the database connection
     * @param dbPath the path to the database
     */
    public static void databaseIntegrity(Connection conn, String dbPath) {
        try {
            // Define the expected table names and the expected number of tables
            List<String> expectedTableNames = Arrays.asList("websites", "keywords", "urls", "website_keywords",
                    "website_urls");
            int expectedTableCount = expectedTableNames.size();

            // Retrieve all table names
            DatabaseMetaData dbm = conn.getMetaData();
            ResultSet tables = dbm.getTables(null, null, "%", new String[] { "TABLE" });

            List<String> tableNames = new ArrayList<>();
            while (tables.next())
                tableNames.add(tables.getString(3)); // Get the table name
            tables.close(); // Close ResultSet

            // Check if the number of tables is inconsistent or if certain table names are
            // not present
            if (tableNames.size() != expectedTableCount || !new HashSet<>(tableNames).containsAll(expectedTableNames)) {
                // Print the inconsistency message
                System.err
                        .println("Inconsistent tables found in database. Dropping " + tableNames.size() + " tables...");

                // Drop each table
                for (String tableName : tableNames) {
                    String dropTableSQL = "DROP TABLE IF EXISTS " + tableName;

                    try (Statement stmt = conn.createStatement()) {
                        // Execute the SQL statement to drop the table
                        stmt.execute(dropTableSQL);
                    } catch (SQLException e) {
                        System.err.println("Error dropping table " + tableName + ": " + e.getMessage());
                    }
                }

                // Set up the database again
                LogUtil.logInfo(LogUtil.ANSI_YELLOW, BarrelSetup.class, "Setting up database...");
                setupDatabase(dbPath);
            }
        } catch (SQLException e) {
            LogUtil.logError(LogUtil.ANSI_RED, BarrelSetup.class, e);
        }
    }

    /**
     * Sets up the database with the required tables for website indexing.
     *
     * @param dbPath the path of the database file
     */
    public static void setupDatabase(String dbPath) {
        String url = "jdbc:sqlite:data/" + dbPath + ".db";
        try (Connection conn = DriverManager.getConnection(url)) {

            // SQL statement for creating websites table
            String websites = """
                    CREATE TABLE IF NOT EXISTS websites (
                        id INTEGER PRIMARY KEY,
                        url TEXT,
                        title TEXT,
                        description TEXT,
                        ref_count INTEGER DEFAULT 0
                    );
                    """;

            // SQL statement for creating keywords table
            String keywords = """
                    CREATE TABLE IF NOT EXISTS keywords (
                        id INTEGER PRIMARY KEY,
                        keyword TEXT UNIQUE,
                        searches INTEGER DEFAULT 0
                    );
                    """;

            // SQL statement for creating urls table
            String urls = """
                    CREATE TABLE IF NOT EXISTS urls (
                        id INTEGER PRIMARY KEY,
                        url TEXT UNIQUE
                    );
                    """;

            // SQL statement for creating website_keywords table
            String website_keywords = """
                    CREATE TABLE IF NOT EXISTS website_keywords (
                        website_id INTEGER,
                        keyword_id INTEGER,
                        count INTEGER,
                        tf_idf REAL,
                        PRIMARY KEY(website_id, keyword_id),
                        FOREIGN KEY(website_id) REFERENCES websites(id),
                        FOREIGN KEY(keyword_id) REFERENCES keywords(id)
                    );
                    """;

            // SQL statement for creating website_urls table
            String website_urls = """
                    CREATE TABLE IF NOT EXISTS website_urls (
                        website_id INTEGER,
                        url_id INTEGER,
                        PRIMARY KEY(website_id, url_id),
                        FOREIGN KEY(website_id) REFERENCES websites(id),
                        FOREIGN KEY(url_id) REFERENCES urls(id)
                    );
                    """;

            // Execute SQL statements to create tables
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(websites);
                stmt.execute(keywords);
                stmt.execute(urls);
                stmt.execute(website_keywords);
                stmt.execute(website_urls);
            }

            System.out.println("Tables created successfully");
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

}