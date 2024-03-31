package Server.IndexStorageBarrel.Operations;

import ReliableMulticast.Objects.CrawlData;
import Server.IndexStorageBarrel.Objects.SearchData;

import java.net.URI;
import java.net.URL;
import java.sql.*;
import java.util.*;

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

    public List<SearchData> retrieveAndRankData(String query) {
        Map<String, SearchData> searchDataMap = new HashMap<>();
        String[] keywords = query.split("\\s+");

        for (String keyword : keywords) {
            String sql = "SELECT w.url, w.title, w.description, wk.tf_idf " +
                    "FROM websites w " +
                    "JOIN website_keywords wk ON w.id = wk.website_id " +
                    "JOIN keywords k ON wk.keyword_id = k.id " +
                    "WHERE k.keyword = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, keyword);
                ResultSet rs = pstmt.executeQuery();

                while (rs.next()) {
                    String url = rs.getString("url");
                    String title = rs.getString("title");
                    String description = rs.getString("description");
                    double tfIdf = rs.getDouble("tf_idf");

                    if (searchDataMap.containsKey(url)) {
                        SearchData existingData = searchDataMap.get(url);
                        double newTfIdf = existingData.tfIdf() + tfIdf;
                        SearchData newData = new SearchData(url, title, description, newTfIdf);
                        searchDataMap.put(url, newData);
                    } else {
                        SearchData searchData = new SearchData(url, title, description, tfIdf);
                        searchDataMap.put(url, searchData);
                    }
                }
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }

        List<SearchData> searchDataList = new ArrayList<>(searchDataMap.values());
        searchDataList.sort((data1, data2) -> Double.compare(data2.tfIdf(), data1.tfIdf()));

        return searchDataList;
    }
}