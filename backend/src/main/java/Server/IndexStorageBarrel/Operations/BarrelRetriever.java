package Server.IndexStorageBarrel.Operations;

// Package imports
import Server.IndexStorageBarrel.Objects.SearchData;

// Logging imports
import Logger.LogUtil;

// General imports
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.web.util.HtmlUtils;

/**
 * The BarrelRetriever class is responsible for retrieving data from the
 * database related to website crawling and indexing.
 */
public class BarrelRetriever {
    /**
     * The database connection used to retrieve data.
     */
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
     * Retrieves a list of websites linking to the target URL.
     *
     * @param targetUrl  the target URL
     * @param pageNumber the page number
     * @return a list of websites linking to the target URL
     */
    public List<SearchData> getWebsitesLinkingTo(String targetUrl, int pageNumber) {
        List<SearchData> websites = new ArrayList<>();

        String sql = "SELECT w.url, w.title, w.description " +
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
                String url = rs.getString("url");
                String title = rs.getString("title");
                String description = rs.getString("description");

                websites.add(new SearchData(url, title, description, 0, 0));
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
                topSearches.add(HtmlUtils.htmlEscape(rs.getString("keyword")));
            }
        } catch (SQLException e) {
            LogUtil.logError(LogUtil.ANSI_RED, BarrelRetriever.class, e);
        }
        LogUtil.logInfo(LogUtil.ANSI_YELLOW, BarrelRetriever.class, "top searches: " + topSearches);
        return topSearches;
    }

    /**
     * Retrieves a list of search data objects from the database based on the given
     * query and page number.
     *
     * @param query      the search query
     * @param pageNumber the page number
     * @param tfIdfSort  whether to sort by tf-idf or ref count
     * @return a list of search data objects
     */
    public List<SearchData> retrieveAndRankData(String query, int pageNumber, boolean tfIdfSort) {
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

        if (tfIdfSort) {
            // sort search data by tf_idf
            searchDataList.sort((data1, data2) -> Double.compare(data2.tfIdf(), data1.tfIdf()));
        } else {
            // sort search data by ref_count
            searchDataList.sort((data1, data2) -> Integer.compare(data2.refCount(), data1.refCount()));
        }

        // Calculate the start and end indices of the page
        int start = (pageNumber - 1) * 10;
        int end = Math.min(start + 10, searchDataList.size());

        // If start exceed list, return empty list
        if (start >= searchDataList.size())
            return new ArrayList<>();

        // Return the page of search results
        return new ArrayList<>(searchDataList.subList(start, end));
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
                LogUtil.logInfo(LogUtil.ANSI_RED, BarrelRetriever.class, "Database connection is closed.");
            }
        }

        return lastIDs;
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
        return getMaps(startID, sql);
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
        String sql = "SELECT * FROM " + table +
                " WHERE id > ?";
        return getMaps(startID, sql);
    }

    /**
     * Retrieves the rows from the strong table with a start ID greater than the
     * specified value.
     *
     * @param startID the start ID
     * @param sql     the SQL query
     * @return a list of rows from the strong table
     */
    private List<Map<String, Object>> getMaps(int startID, String sql) {
        List<Map<String, Object>> rows = null;
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, startID);
            ResultSet rs = pstmt.executeQuery();
            rows = resultSetToRowList(rs);
            rs.close();
        } catch (SQLException e) {
            LogUtil.logError(LogUtil.ANSI_RED, BarrelRetriever.class, e);
        }
        return rows;
    }

    /**
     * Converts the ResultSet to a list of rows.
     *
     * @param rs the ResultSet
     * @return a list of rows
     */
    private List<Map<String, Object>> resultSetToRowList(ResultSet rs) {
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