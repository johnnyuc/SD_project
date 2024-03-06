package IndexStorageBarrel;

// Database imports
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

// Util imports
import java.io.Serializable;

/**
 * IndexStorageBarrel
 */
public class IndexStorageBarrel implements Serializable {
    public static void main(String[] args) {
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
            throw new Error("Problem connecting to database", e);
        }

        // Setting up database
        setupDatabase();
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
            System.out.println(e.getMessage());
        }
    }
}