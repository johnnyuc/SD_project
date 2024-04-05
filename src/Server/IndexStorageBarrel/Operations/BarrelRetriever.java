package Server.IndexStorageBarrel.Operations;

import ReliableMulticast.Objects.CrawlData;
import Server.IndexStorageBarrel.Objects.SearchData;

import java.net.URI;
import java.net.URL;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

import Logger.LogUtil;

/**
 * The BarrelRetriever class is responsible for retrieving data from the
 * database related to website crawling and indexing.
 */
public class BarrelRetriever {
    private final Connection conn;

    /**
     * Constructs a BarrelRetriever object with the given database connection.
     *
     * @param conn the database connection
     */
    public BarrelRetriever(Connection conn) {
        this.conn = conn;
    }

    /**
     * Retrieves a list of crawl data objects from the database.
     *
     * @return a list of crawl data objects
     */
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

    /**
     * Retrieves a list of websites linking to the target URL.
     *
     * @param targetUrl  the target URL
     * @param pageNumber the page number
     * @return a list of websites linking to the target URL
     */
    public List<String> getWebsitesLinkingTo(String targetUrl, int pageNumber) {
        List<String> websites = new ArrayList<>();

        String sql = "SELECT w.url " +
                "FROM websites w " +
                "JOIN website_urls wu ON w.id = wu.website_id " +
                "JOIN urls u ON wu.url_id = u.id " +
                "WHERE u.url = ?" +
                "LIMIT 10 OFFSET ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, targetUrl);
            pstmt.setInt(2, pageNumber);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                LogUtil.logInfo(LogUtil.ANSI_YELLOW, BarrelRetriever.class, "Found website linking to target url");
                websites.add(rs.getString("url"));
            }
        } catch (SQLException e) {
            LogUtil.logError(LogUtil.ANSI_RED, BarrelRetriever.class, e);
        }

        return websites;
    }

    /**
     * Retrieves a list of top searches from the database.
     *
     * @return a list of top searches
     */
    public List<String> getTopSearches() {
        List<String> topSearches = new ArrayList<>();
        String sql = "SELECT keyword " +
                "FROM keywords " +
                "ORDER BY searches DESC " +
                "LIMIT 10";

        try (Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                topSearches.add(rs.getString("keyword"));
            }
        } catch (SQLException e) {
            LogUtil.logError(LogUtil.ANSI_RED, BarrelRetriever.class, e);
        }
        LogUtil.logInfo(LogUtil.ANSI_YELLOW, BarrelRetriever.class, "top searches: " + topSearches.toString());
        return topSearches;
    }

    /**
     * Retrieves a list of search data objects from the database based on the given
     * query and page number.
     *
     * @param query      the search query
     * @param pageNumber the page number
     * @return a list of search data objects
     */
    public List<SearchData> retrieveAndRankData(String query, int pageNumber) {
        Map<String, SearchData> searchDataMap = new HashMap<>();
        String[] keywords = query.split("\\s+");

        List<String> keywordList = Arrays.asList(keywords);
        String sql = "SELECT w.url, w.title, w.description, wk.tf_idf, w.ref_count " +
                "FROM websites w " +
                "JOIN website_keywords wk ON w.id = wk.website_id " +
                "JOIN keywords k ON wk.keyword_id = k.id " +
                "WHERE k.keyword IN (" + keywordList.stream().map(token -> "?").collect(Collectors.joining(",")) + ")";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < keywordList.size(); i++) {
                pstmt.setString(i + 1, keywordList.get(i));
            }
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

        // Calculate the start and end indices of the page
        int start = (pageNumber - 1) * 10;
        int end = Math.min(start + 10, searchDataList.size());

        // If start exceed list, return empty list
        if (start >= searchDataList.size())
            return new ArrayList<>();

        // Return the page of search results
        return searchDataList.subList(start, end);
    }

    /**
     * Retrieves the last IDs of the tables in the database.
     *
     * @return a HashMap containing the last IDs of the tables
     */
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

    /**
     * Retrieves the data from the specified table in the database.
     *
     * @param table the table name
     * @return the ResultSet containing the data from the table
     */
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

    /**
     * Retrieves the rows from the weak table with a start ID greater than the
     * specified value.
     *
     * @param weakTable the weak table name
     * @param startID   the start ID
     * @return a list of rows from the weak table
     */
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

    /**
     * Retrieves the rows from the specified table with a start ID greater than the
     * specified value.
     *
     * @param table   the table name
     * @param startID the start ID
     * @return a list of rows from the table
     */
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

    /**
     * Converts the ResultSet to a list of rows.
     *
     * @param table the table name
     * @param rs    the ResultSet
     * @return a list of rows
     */
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