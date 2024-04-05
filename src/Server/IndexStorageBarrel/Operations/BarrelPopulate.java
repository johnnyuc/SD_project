package Server.IndexStorageBarrel.Operations;

import ReliableMulticast.Objects.CrawlData;
import Server.IndexStorageBarrel.Tools.SyncData;

import java.net.URL;
import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import Logger.LogUtil;

/**
 * This class represents the BarrelPopulate class, which is responsible for
 * populating the database with data from SyncData and CrawlData objects.
 * It provides methods to insert sync data into the database and handle the
 * insertion of crawl data.
 * The class uses a connection to the database and an instance of
 * BarrelProcessing for processing the data.
 */

public class BarrelPopulate {
    private final Connection conn;
    private BarrelProcessing barrelProcessing;

    /**
     * Constructs a new BarrelPopulate object with the specified database
     * connection.
     *
     * @param conn the database connection to be used by the BarrelPopulate object
     */
    public BarrelPopulate(Connection conn) {
        this.conn = conn;
        this.barrelProcessing = new BarrelProcessing(conn);
    }

    /**
     * Inserts the sync data into the database.
     *
     * @param syncData The sync data to be inserted.
     */
    public void insertSyncData(SyncData syncData) {
        LogUtil.logInfo(LogUtil.ANSI_GREEN, BarrelPopulate.class, "Inserting sync data into database...");

        // Start a transaction
        try {
            conn.setAutoCommit(false);
        } catch (SQLException e) {
            LogUtil.logError(LogUtil.ANSI_RED, BarrelPopulate.class, e);
            return;
        }

        for (String table : syncData.tableResults().keySet()) {
            List<Map<String, Object>> rows = syncData.tableResults().get(table);
            switch (table) {
                case "websites" -> {
                    if (!insertWebsites(rows)) {
                        stopTransaction();
                        return;
                    }
                }
                case "keywords" -> {
                    if (!insertKeywords(rows)) {
                        stopTransaction();
                        return;
                    }
                }
                case "urls" -> {
                    if (!insertUrls(rows)) {
                        stopTransaction();
                        return;
                    }
                }
                case "website_keywords" -> {
                    if (!insertWebsiteKeywords(rows)) {
                        stopTransaction();
                        return;
                    }
                }
                case "website_urls" -> {
                    if (!insertWebsiteUrl(rows)) {
                        stopTransaction();
                        return;
                    }
                }
            }
        }
        // Commit the transaction
        try {
            conn.commit();
            conn.setAutoCommit(true);
        } catch (SQLException e) {
            LogUtil.logError(LogUtil.ANSI_RED, BarrelPopulate.class, e);
        }
    }

    /**
     * Stops the current transaction by rolling back any changes made and setting
     * auto-commit to true.
     * If an SQLException occurs during the rollback, it is logged as an error.
     */
    private void stopTransaction() {
        try {
            conn.rollback();
            conn.setAutoCommit(true);
        } catch (SQLException rollbackException) {
            LogUtil.logError(LogUtil.ANSI_RED, BarrelPopulate.class, rollbackException);
        }
    }

    /**
     * Inserts a list of websites into the database.
     *
     * @param rows the list of websites to insert
     * @return true if the websites were successfully inserted, false otherwise
     */
    private boolean insertWebsites(List<Map<String, Object>> rows) {
        String sql = "INSERT INTO websites(url, title, description, ref_count) VALUES(?,?,?,?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (Map<String, Object> row : rows) {
                pstmt.setString(1, (String) row.get("url"));
                pstmt.setString(2, (String) row.get("title"));
                pstmt.setString(3, (String) row.get("description"));
                pstmt.setInt(4, (int) row.get("ref_count"));
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        } catch (SQLException e) {
            LogUtil.logError(LogUtil.ANSI_RED, BarrelPopulate.class, e);
            return false;
        }
        return true;
    }

    /**
     * Inserts a list of keywords into the "keywords" table in the database.
     * 
     * @param rows a list of maps containing the keyword, id, and searches values to
     *             be inserted
     * @return true if the keywords were successfully inserted, false otherwise
     */
    private boolean insertKeywords(List<Map<String, Object>> rows) {
        String sql = "INSERT INTO keywords(keyword, id, searches) VALUES(?,?,?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (Map<String, Object> row : rows) {
                pstmt.setString(1, (String) row.get("keyword"));
                pstmt.setInt(2, (int) row.get("id"));
                pstmt.setInt(3, (int) row.get("searches"));
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        } catch (SQLException e) {
            LogUtil.logError(LogUtil.ANSI_RED, BarrelPopulate.class, e);
            return false;
        }
        return true;
    }

    /**
     * Inserts a list of URLs into the database.
     *
     * @param rows the list of rows containing the URLs and their corresponding IDs
     * @return true if the URLs were successfully inserted, false otherwise
     */
    private boolean insertUrls(List<Map<String, Object>> rows) {
        String sql = "INSERT INTO urls(url, id) VALUES(?,?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (Map<String, Object> row : rows) {
                pstmt.setString(1, (String) row.get("url"));
                pstmt.setInt(2, (int) row.get("id"));
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        } catch (SQLException e) {
            LogUtil.logError(LogUtil.ANSI_RED, BarrelPopulate.class, e);
            return false;
        }
        return true;
    }

    /**
     * Inserts website keywords into the database.
     *
     * @param rows a list of maps containing the website ID, keyword ID, and TF-IDF
     *             values
     * @return true if the insertion is successful, false otherwise
     */
    private boolean insertWebsiteKeywords(List<Map<String, Object>> rows) {
        String sql = "INSERT INTO website_keywords(website_id, keyword_id, tf_idf) VALUES(?,?,?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (Map<String, Object> row : rows) {
                pstmt.setInt(1, (int) row.get("website_id"));
                pstmt.setInt(2, (int) row.get("keyword_id"));
                pstmt.setDouble(3, (double) row.get("tf_idf"));
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        } catch (SQLException e) {
            LogUtil.logError(LogUtil.ANSI_RED, BarrelPopulate.class, e);
            return false;
        }
        return true;
    }

    /**
     * Inserts website URLs into the database.
     * 
     * @param rows a list of maps containing website_id and url_id values
     * @return true if the insertion is successful, false otherwise
     */
    private boolean insertWebsiteUrl(List<Map<String, Object>> rows) {
        String sql = "INSERT INTO website_urls(website_id, url_id) VALUES(?,?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (Map<String, Object> row : rows) {
                pstmt.setInt(1, (int) row.get("website_id"));
                pstmt.setInt(2, (int) row.get("url_id"));
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        } catch (SQLException e) {
            LogUtil.logError(LogUtil.ANSI_RED, BarrelPopulate.class, e);
            return false;
        }
        return true;
    }

    /**
     * Inserts crawl data into the database.
     *
     * @param crawlData The crawl data to be inserted.
     * @throws SQLException If an error occurs while inserting the crawl data.
     */
    public void insertCrawlData(CrawlData crawlData) throws SQLException {
        // Extract data from CrawlData object
        String url = crawlData.getUrl().toString();
        String title = crawlData.getTitle();
        String description = crawlData.getDescription();
        List<String> tokens = crawlData.getTokens();
        List<URL> urls = crawlData.getUrlStrings();

        barrelProcessing = new BarrelProcessing(conn);

        // Assuming barrelProcessing is properly initialized
        int websiteId = handleWebsiteInsertOrUpdate(url, title, description);
        Map<String, Integer> keywordIdMap = handleKeywordBatchInsertion(tokens);
        handleWebsiteKeywordBatchInsertion(websiteId, keywordIdMap, tokens);
        handleUrlBatchInsertion(websiteId, urls);
    }

    /**
     * Handles the insertion or update of a website record in the database.
     * If the URL already exists in the database, the existing record is updated
     * with the provided title and description.
     * If the URL does not exist, a new record is inserted with the provided URL,
     * title, and description.
     *
     * @param url         the URL of the website
     * @param title       the title of the website
     * @param description the description of the website
     * @return the ID of the inserted or updated website record
     */
    private int handleWebsiteInsertOrUpdate(String url, String title, String description) {
        String sql = "SELECT id FROM websites WHERE url = ?";
        int websiteId = 0;
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, url);
            ResultSet rs1 = pstmt.executeQuery();
            if (rs1.next()) {
                websiteId = rs1.getInt("id");
                // URL exists, update the existing record
                sql = "UPDATE websites SET title = ?, description = ? WHERE id = ?";
                try (PreparedStatement pstmt2 = conn.prepareStatement(sql)) {
                    pstmt2.setString(1, title);
                    pstmt2.setString(2, description);
                    pstmt2.setInt(3, websiteId);
                    pstmt2.executeUpdate();
                }
            } else {
                // URL does not exist, insert a new record
                sql = "INSERT INTO websites(url, title, description) VALUES(?,?,?)";
                try (PreparedStatement pstmt2 = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    pstmt2.setString(1, url);
                    pstmt2.setString(2, title);
                    pstmt2.setString(3, description);
                    pstmt2.executeUpdate();
                    ResultSet rs2 = pstmt2.getGeneratedKeys();
                    if (rs2.next()) {
                        websiteId = rs2.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            LogUtil.logError(LogUtil.ANSI_RED, BarrelPopulate.class, e);
        }
        return websiteId;
    }

    /**
     * Inserts a batch of keywords into the database and retrieves their
     * corresponding IDs.
     * 
     * @param tokens the list of keywords to be inserted
     * @return a map containing the keywords as keys and their corresponding IDs as
     *         values
     * @throws SQLException if an error occurs while executing the SQL statements
     */
    private Map<String, Integer> handleKeywordBatchInsertion(List<String> tokens) throws SQLException {
        Map<String, Integer> keywordIdMap = new HashMap<>();
        String insertSql = "INSERT OR IGNORE INTO keywords (keyword) VALUES (?)";

        try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
            for (String token : tokens) {
                pstmt.setString(1, token);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        } catch (SQLException e) {
            LogUtil.logError(LogUtil.ANSI_RED, BarrelPopulate.class, e);
        }
        // Select every inserted keyword from the database
        String selectSql = "SELECT id, keyword FROM keywords WHERE keyword IN ("
                + tokens.stream().map(token -> "?").collect(Collectors.joining(",")) + ")";
        try (PreparedStatement pstmt = conn.prepareStatement(selectSql)) {
            for (int i = 0; i < tokens.size(); i++) {
                pstmt.setString(i + 1, tokens.get(i));
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String keyword = rs.getString("keyword");
                    keywordIdMap.put(keyword, id);
                }
            }
        } catch (SQLException e) {
            LogUtil.logError(LogUtil.ANSI_RED, BarrelPopulate.class, e);
        }

        return keywordIdMap;
    }

    /**
     * Inserts a batch of website keywords into the database.
     *
     * @param websiteId    the ID of the website
     * @param keywordIdMap a map containing the keyword IDs
     * @param tokens       a list of tokens to be inserted
     */
    private void handleWebsiteKeywordBatchInsertion(int websiteId, Map<String, Integer> keywordIdMap,
            List<String> tokens) {
        // Prepare SQL for batch insert
        String sql = "INSERT OR IGNORE INTO website_keywords (website_id, keyword_id, tf_idf) VALUES (?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (String token : tokens) {
                Integer keywordId = keywordIdMap.get(token);
                if (keywordId == null) {
                    continue;
                }
                double tfIdf = barrelProcessing.calcTFIDF(token, tokens, barrelProcessing.getDocnr());

                pstmt.setInt(1, websiteId);
                pstmt.setInt(2, keywordId);
                pstmt.setDouble(3, tfIdf);
                pstmt.addBatch();
            }

            pstmt.executeBatch();
        } catch (SQLException e) {
            LogUtil.logError(LogUtil.ANSI_RED, BarrelPopulate.class, e);
        }
    }

    /**
     * Handles the batch insertion of URLs for a given website.
     *
     * @param websiteId The ID of the website.
     * @param urls      The list of URLs to be inserted.
     */
    private void handleUrlBatchInsertion(int websiteId, List<URL> urls) {
        String insertUrlSql = "INSERT OR IGNORE INTO urls(url) VALUES(?)";
        String selectUrlIdSql = "SELECT id FROM urls WHERE url = ?";
        String insertWebsiteUrlSql = "INSERT OR IGNORE INTO website_urls(website_id, url_id) VALUES(?, ?)";

        try (PreparedStatement insertUrlPstmt = conn.prepareStatement(insertUrlSql);
                PreparedStatement selectUrlIdPstmt = conn.prepareStatement(selectUrlIdSql);
                PreparedStatement insertWebsiteUrlPstmt = conn.prepareStatement(insertWebsiteUrlSql)) {

            // Batch insert URLs
            for (URL url : urls) {
                insertUrlPstmt.setString(1, url.toString());
                insertUrlPstmt.addBatch();
            }
            insertUrlPstmt.executeBatch();

            // For each URL, get its ID and batch insert the website-url relationship
            for (URL url : urls) {
                selectUrlIdPstmt.setString(1, url.toString());
                try (ResultSet rs = selectUrlIdPstmt.executeQuery()) {
                    if (rs.next()) {
                        int urlId = rs.getInt(1);
                        insertWebsiteUrlPstmt.setInt(1, websiteId);
                        insertWebsiteUrlPstmt.setInt(2, urlId);
                        insertWebsiteUrlPstmt.addBatch();

                        // Update references count for the referenced website
                        String updateSql = "UPDATE websites SET ref_count = ref_count + 1 WHERE id = ?";
                        try (PreparedStatement updatePstmt = conn.prepareStatement(updateSql)) {
                            updatePstmt.setInt(1, urlId);
                            updatePstmt.executeUpdate();
                        }
                    }
                }
            }
            insertWebsiteUrlPstmt.executeBatch();

        } catch (SQLException e) {
            LogUtil.logError(LogUtil.ANSI_RED, BarrelPopulate.class, e);
        }
    }

    /**
     * Increments the search count for a given keyword in the database.
     * If the keyword already exists, the search count is incremented by 1.
     * If the keyword does not exist, it is added to the database with a search
     * count of 1.
     *
     * @param keyword the keyword to increment the search count for
     */
    public void incrementSearches(String keyword) {
        String selectSql = "SELECT * FROM keywords WHERE keyword = ?";
        try (PreparedStatement selectPstmt = conn.prepareStatement(selectSql)) {
            selectPstmt.setString(1, keyword);
            ResultSet rs = selectPstmt.executeQuery();
            if (rs.next()) {
                // The keyword exists, increment the searches count
                String updateSql = "UPDATE keywords SET searches = searches + 1 WHERE keyword = ?";
                try (PreparedStatement updatePstmt = conn.prepareStatement(updateSql)) {
                    updatePstmt.setString(1, keyword);
                    updatePstmt.executeUpdate();
                }
            } else {
                // Add the keyword to the database with search count 1
                String insertSql = "INSERT INTO keywords(keyword, searches) VALUES(?, 1)";
                try (PreparedStatement insertPstmt = conn.prepareStatement(insertSql)) {
                    insertPstmt.setString(1, keyword);
                    insertPstmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            LogUtil.logError(LogUtil.ANSI_RED, BarrelPopulate.class, e);
        }
    }
}