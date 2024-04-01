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
            CrawlData crawlData = new CrawlData(
                    URI.create("http://example.com").toURL(),
                    "Example Title2", "Example Description",
                    Arrays.asList("example",
                            "token",
                            "test",
                            "test",
                            "test"
                    ),
                    Arrays.asList(new URL("http://example.com/link1"),
                            new URL("http://example.com/link2")
                    )
            );

            // Insert another one
            CrawlData crawlData2 = new CrawlData(
                    URI.create("http://examplo.com").toURL(),
                    "Example Title2", "Example Description",
                    Arrays.asList("example",
                            "token",
                            "toast",
                            "task",
                            "test"
                    ),
                    Arrays.asList(new URL("http://example.com/link3"),
                            new URL("http://example.com/link4")
                    )
            );

            // Insert another one
            CrawlData crawlData3 = new CrawlData(
                    URI.create("http://exampli.com").toURL(),
                    "Example Title2", "Example Description",
                    Arrays.asList("cough",
                            "cloth",
                            "coast"
                    ),
                    Arrays.asList(new URL("http://example.com/link5"),
                            new URL("http://example.com/link6")
                    )
            );

            System.out.println("Inserting data: " + crawlData);
            indexStorageBarrel.insertData(crawlData);

            System.out.println("Inserting data: " + crawlData2);
            indexStorageBarrel.insertData(crawlData2);

            System.out.println("Inserting data: " + crawlData3);
            indexStorageBarrel.insertData(crawlData3);

            // Retrieve data from the barrel
            List<CrawlData> retrievedData = indexStorageBarrel.retrieveObject();
            System.out.println("Retrieved data: " + retrievedData);

            // Retrieve and rank data from the barrel
            List<SearchData> searchData = indexStorageBarrel.retrieveAndRankData("example test");
            System.out.println("Search results: " + searchData);

        } catch (MalformedURLException e) {
            System.out.println(e.getMessage());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}