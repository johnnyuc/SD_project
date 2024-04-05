package Server.IndexStorageBarrel.Operations;

import ReliableMulticast.Objects.CrawlData;
import Server.IndexStorageBarrel.Objects.SearchData;

import java.net.URI;
import java.net.URL;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

import Logger.LogUtil;

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
            LogUtil.logError(LogUtil.ANSI_RED, BarrelRetriever.class, e);
        }

        return crawlDataList;
    }

    public List<SearchData> retrieveAndRankData(String query, int pageNumber) {
        Map<String, SearchData> searchDataMap = new HashMap<>();
        String[] keywords = query.split("\\s+");

        List<String> keywordList = Arrays.asList(keywords);
        String sql = "SELECT w.url, w.title, w.description, wk.tf_idf, w.ref_count " +
                "FROM websites w " +
                "JOIN website_keywords wk ON w.id = wk.website_id " +
                "JOIN keywords k ON wk.keyword_id = k.id " +
                "WHERE k.keyword IN (" + keywordList.stream().map(token -> "?").collect(Collectors.joining(",")) + ")" +
                "LIMIT 10 OFFSET ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < keywordList.size(); i++) {
                pstmt.setString(i + 1, keywordList.get(i));
            }
            pstmt.setInt(keywordList.size() + 1, (pageNumber - 1) * 10);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String url = rs.getString("url");
                String title = rs.getString("title");
                String description = rs.getString("description");
                double tfIdf = rs.getDouble("tf_idf");
                int refCount = rs.getInt("ref_count");

                if (searchDataMap.containsKey(url)) {
                    SearchData existingData = searchDataMap.get(url);
                    double newTfIdf = existingData.tfIdf() + tfIdf;
                    SearchData newData = new SearchData(url, title, description, newTfIdf, refCount);
                    searchDataMap.put(url, newData);
                } else {
                    SearchData searchData = new SearchData(url, title, description, tfIdf, refCount);
                    searchDataMap.put(url, searchData);
                }
            }
        } catch (SQLException e) {
            LogUtil.logError(LogUtil.ANSI_RED, BarrelRetriever.class, e);
        }

        List<SearchData> searchDataList = new ArrayList<>(searchDataMap.values());
        // searchDataList.sort((data1, data2) -> Double.compare(data2.tfIdf(),
        // data1.tfIdf()));

        // sort search data by ref_count
        searchDataList.sort((data1, data2) -> Integer.compare(data2.refCount(), data1.refCount()));

        return searchDataList;
    }

    public HashMap<String, Integer> getLastIDs() {
        HashMap<String, Integer> lastIDs = new HashMap<>();
        String[] tables = { "websites", "keywords", "urls" };

        for (String table : tables) {
            String sql = "SELECT MAX(id) FROM " + table;

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                ResultSet rs = pstmt.executeQuery();
                if (rs.next())
                    lastIDs.put(table, rs.getInt(1));
            } catch (SQLException e) {
                LogUtil.logError(LogUtil.ANSI_RED, BarrelRetriever.class, e);
            }
        }

        return lastIDs;
    }

    public ResultSet getTable(String table) {
        String sql = "SELECT * FROM ?";
        ResultSet rs = null;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, table);
            rs = pstmt.executeQuery();

        } catch (SQLException e) {
            LogUtil.logError(LogUtil.ANSI_RED, BarrelRetriever.class, e);
        }
        return rs;
    }

    public List<Map<String, Object>> getWeakTableWithStartID(String weakTable, int startID) {
        String sql = "SELECT * FROM " + weakTable +
                " WHERE website_id > ?";
        List<Map<String, Object>> rows = null;
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, startID);
            ResultSet rs = pstmt.executeQuery();
            rows = resultSetToRowList(weakTable, rs);
            rs.close();
        } catch (SQLException e) {
            LogUtil.logError(LogUtil.ANSI_RED, BarrelRetriever.class, e);
        }
        return rows;
    }

    public List<Map<String, Object>> getTableWithStartID(String table, int startID) {
        // TODO: No need to fear SQL injection because these values can't be received
        // from a user SERÁ???
        String sql = "SELECT * FROM " + table +
                " WHERE id > ?";
        List<Map<String, Object>> rows = null;
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, startID);
            ResultSet rs = pstmt.executeQuery();
            rows = resultSetToRowList(table, rs);
            rs.close();
        } catch (SQLException e) {
            LogUtil.logError(LogUtil.ANSI_RED, BarrelRetriever.class, e);
        }
        return rows;
    }

    private List<Map<String, Object>> resultSetToRowList(String table, ResultSet rs) {
        // Create a list to hold the rows
        List<Map<String, Object>> rows = new ArrayList<>();

        try {
            // Get the ResultSetMetaData. This will be used to get column names
            ResultSetMetaData rsmd = rs.getMetaData();

            while (rs.next()) {
                // Create a map to hold the row data
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= rsmd.getColumnCount(); i++) {
                    row.put(rsmd.getColumnName(i), rs.getObject(i));
                }
                rows.add(row);
            }
        } catch (SQLException e) {
            LogUtil.logError(LogUtil.ANSI_RED, BarrelSync.class, e);
        }
        return rows;
    }

}