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
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:data/testBarrel.db")) {
            System.out.println("Connected to DBbarrel.db database");

            // Check if table exists
            DatabaseMetaData dbm = conn.getMetaData();
            ResultSet tables = dbm.getTables(null, null, "TEST", null);
            if (tables.next()) {
                System.out.println("Table exists");
            } else {
                System.out.println("Table does not exist");
            }

        } catch (SQLException e) {
            throw new Error("Problem connecting to DBbarrel.db database", e);
        }
    }
}