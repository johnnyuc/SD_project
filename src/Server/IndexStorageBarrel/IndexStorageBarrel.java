package Server.IndexStorageBarrel;

import ReliableMulticast.Objects.CrawlData;
import Server.IndexStorageBarrel.Objects.SearchData;
import Server.IndexStorageBarrel.Operations.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

public class IndexStorageBarrel {
    private Connection conn;
    private BarrelPopulate barrelPopulate;
    private BarrelRetriever barrelRetriever;
    private BarrelSetup barrelSetup;

    public IndexStorageBarrel() {
        try {
            this.conn = DriverManager.getConnection("jdbc:sqlite:data/testBarrel.db");
            BarrelSetup.databaseIntegrity(conn); // Check database integrity
            this.barrelPopulate = new BarrelPopulate(conn);
            this.barrelRetriever = new BarrelRetriever(conn);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void insertData(CrawlData crawlData) throws SQLException {
        barrelPopulate.insertData(crawlData);
    }

    public List<CrawlData> retrieveObject() {
        return barrelRetriever.retrieveObject();
    }

    public List<SearchData> retrieveAndRankData(String query) {
        return barrelRetriever.retrieveAndRankData(query);
    }
}