package Server.IndexStorageBarrel.Operations;

import ReliableMulticast.Objects.CrawlData;

import java.net.URI;
import java.net.URL;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BarrelRetriever {
    private final Connection conn;

    public BarrelRetriever(Connection conn) {
        this.conn = conn;
    }

    public List<CrawlData> retrieveObject() {
        List<CrawlData> crawlDataList = new ArrayList<>();
        String sql = "SELECT w.url, w.title, w.description, k.keyword, u.url " +
                "FROM websites w " +
                "JOIN website_keywords wk ON w.id = wk.website_id " +
                "JOIN keywords k ON wk.keyword_id = k.id " +
                "JOIN website_urls wu ON w.id = wu.website_id " +
                "JOIN urls u ON wu.url_id = u.id";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String url = rs.getString("url");
                String title = rs.getString("title");
                String description = rs.getString("description");
                String keyword = rs.getString("keyword");
                String urlString = rs.getString("url");

                List<String> tokens = Arrays.asList(keyword.split("\\s+"));
                List<URL> urls = new ArrayList<>();
                urls.add(URI.create(urlString).toURL());

                CrawlData crawlData = new CrawlData(URI.create(url).toURL(), title, description, tokens, urls);
                crawlDataList.add(crawlData);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return crawlDataList;
    }
}