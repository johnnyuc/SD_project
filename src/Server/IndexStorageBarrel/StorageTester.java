package Server.IndexStorageBarrel;

import ReliableMulticast.Objects.CrawlData;
import Server.IndexStorageBarrel.Objects.SearchData;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

public class StorageTester {
    public static void main(String[] args) {
        try {
            // Create an instance of IndexStorageBarrel
            IndexStorageBarrel indexStorageBarrel = new IndexStorageBarrel();

            // Insert data into the barrel
            CrawlData crawlData = new CrawlData(URI.create("http://example.com").toURL(), "Example Title", "Example Description", Arrays.asList("example", "token"), Arrays.asList(new URL("http://example.com/link1"), new URL("http://example.com/link2")));
            indexStorageBarrel.insertData(crawlData);
            System.out.println("Inserted data: " + crawlData);

            // Retrieve data from the barrel
            List<CrawlData> retrievedData = indexStorageBarrel.retrieveObject();
            System.out.println("Retrieved data: " + retrievedData);

            // Retrieve and rank data from the barrel
            List<SearchData> searchData = indexStorageBarrel.retrieveAndRankData("example token");
            System.out.println("Search results: " + searchData);

        } catch (MalformedURLException e) {
            System.out.println(e.getMessage());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}