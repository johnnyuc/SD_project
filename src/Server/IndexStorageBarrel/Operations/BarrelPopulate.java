package Server.IndexStorageBarrel.Operations;

import ReliableMulticast.Objects.CrawlData;
import Server.IndexStorageBarrel.Tools.SyncData;

import java.net.URL;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import Logger.LogUtil;

public class BarrelPopulate {
    private final Connection conn;
    private BarrelProcessing barrelProcessing;

    public BarrelPopulate(Connection conn) {
        this.conn = conn;
        this.barrelProcessing = new BarrelProcessing(conn);
    }

    public void insertSyncData(SyncData syncData) {
        for (String table : syncData.tableResults().keySet()) {
            ResultSet rs = syncData.tableResults().get(table);
            try {
                while (rs.next()) {
                    switch (table) {
                        case "websites" -> {
                            String url = rs.getString("url");
                            String title = rs.getString("title");
                            String description = rs.getString("description");
                            insertWebsite(url, title, description);
                        }
                        case "keywords" -> {
                            String keyword = rs.getString("keyword");
                            int keywordId = rs.getInt("id");
                            insertKeyword(keyword, keywordId);
                        }
                        case "urls" -> {
                            String urlString = rs.getString("url");
                            int urlId = rs.getInt("id");
                            insertUrl(urlString, urlId);
                        }
                        case "website_keywords" -> {
                            int websiteId = rs.getInt("website_id");
                            int keywordId = rs.getInt("keyword_id");
                            int count = rs.getInt("count");
                            double tfIdf = rs.getDouble("tf_idf");
                            insertWebsiteKeyword(websiteId, keywordId, count, tfIdf);
                        }
                        case "website_urls" -> {
                            int websiteId = rs.getInt("website_id");
                            int urlId = rs.getInt("url_id");
                            insertWebsiteUrl(websiteId, urlId);
                        }
                    }
                }
            } catch (SQLException e) {
                LogUtil.logError(LogUtil.ANSI_RED, BarrelPopulate.class, e);
            }
        }
    }

    private void insertWebsite(String url, String title, String description) {
        String sql = "INSERT INTO websites(url, title, description) VALUES(?,?,?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, url);
            pstmt.setString(2, title);
            pstmt.setString(3, description);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LogUtil.logError(LogUtil.ANSI_RED, BarrelPopulate.class, e);
        }
    }

    private void insertKeyword(String keyword, int keywordId) {
        String sql = "INSERT INTO keywords(keyword, id) VALUES(?,?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, keyword);
            pstmt.setInt(2, keywordId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LogUtil.logError(LogUtil.ANSI_RED, BarrelPopulate.class, e);
        }
    }

    private void insertUrl(String urlString, int urlId) {
        String sql = "INSERT INTO urls(url, id) VALUES(?,?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, urlString);
            pstmt.setInt(2, urlId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LogUtil.logError(LogUtil.ANSI_RED, BarrelPopulate.class, e);
        }
    }

    private void insertWebsiteKeyword(int websiteId, int keywordId, int count, double tfIdf) {
        String sql = "INSERT INTO website_keywords(website_id, keyword_id, count, tf_idf) VALUES(?,?,?,?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, websiteId);
            pstmt.setInt(2, keywordId);
            pstmt.setInt(3, count);
            pstmt.setDouble(4, tfIdf);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LogUtil.logError(LogUtil.ANSI_RED, BarrelPopulate.class, e);
        }
    }

    private void insertWebsiteUrl(int websiteId, int urlId) {
        String sql = "INSERT INTO website_urls(website_id, url_id) VALUES(?,?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, websiteId);
            pstmt.setInt(2, urlId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LogUtil.logError(LogUtil.ANSI_RED, BarrelPopulate.class, e);
        }
    }

    public void insertCrawlData(CrawlData crawlData) throws SQLException {
        // Extract data from CrawlData object
        String url = String.valueOf(crawlData.getUrl());
        String title = crawlData.getTitle();
        String description = crawlData.getDescription();
        List<String> tokens = crawlData.getTokens();
        List<URL> urls = crawlData.getUrlStrings();

        // Update BarrelProcessing with new term counts and total terms
        barrelProcessing = new BarrelProcessing(conn);

        // Check if URL already exists in websites table
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

        // Insert each token into keywords table, link it to the website in
        // website_keywords table, and calculate TF-IDF
        int docNr = barrelProcessing.getDocnr();
        if (!barrelProcessing.docContainsToken(websiteId, String.valueOf(tokens)))
            docNr++;
        for (String token : tokens) {
            int keywordId = 0;
            sql = "SELECT id FROM keywords WHERE keyword = ?";
            try (PreparedStatement pstmt1 = conn.prepareStatement(sql)) {
                pstmt1.setString(1, token);
                ResultSet rs3 = pstmt1.executeQuery();
                if (rs3.next()) {
                    keywordId = rs3.getInt("id");
                } else {
                    sql = "INSERT INTO keywords(keyword) VALUES(?)";
                    try (PreparedStatement pstmt2 = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                        pstmt2.setString(1, token);
                        pstmt2.executeUpdate();
                        ResultSet rs4 = pstmt2.getGeneratedKeys();
                        if (rs4.next()) {
                            keywordId = rs4.getInt(1);
                        }
                    }
                }

                // Calculate TF-IDF by multiplying TF and IDF
                double tfIdf = barrelProcessing.calcTFIDF(token, new ArrayList<>(tokens), docNr);

                // Check if the combination of website_id and keyword_id already exists in the
                // website_keywords table
                sql = "SELECT 1 FROM website_keywords WHERE website_id = ? AND keyword_id = ?";
                try (PreparedStatement pstmt3 = conn.prepareStatement(sql)) {
                    pstmt3.setInt(1, websiteId);
                    pstmt3.setInt(2, keywordId);
                    ResultSet rs5 = pstmt3.executeQuery();
                    if (!rs5.next()) {
                        // Link the keyword to the website in the website_keywords table and store the
                        // TF-IDF score
                        sql = "INSERT INTO website_keywords(website_id, keyword_id, tf_idf) VALUES(?,?,?)";
                        try (PreparedStatement pstmt4 = conn.prepareStatement(sql)) {
                            pstmt4.setInt(1, websiteId);
                            pstmt4.setInt(2, keywordId);
                            pstmt4.setDouble(3, tfIdf);
                            pstmt4.executeUpdate();
                        }
                    }
                } catch (SQLException e) {
                    System.out.println(e.getMessage());
                }
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }

        // Insert each URL into urls table and link it to the website in website_urls
        // table
        for (URL urlItem : urls) {
            int urlId = 0;
            sql = "SELECT id FROM urls WHERE url = ?";
            try (PreparedStatement pstmt4 = conn.prepareStatement(sql)) {
                pstmt4.setString(1, String.valueOf(urlItem));
                ResultSet rs6 = pstmt4.executeQuery();
                if (rs6.next()) {
                    urlId = rs6.getInt("id");
                } else {
                    sql = "INSERT INTO urls(url) VALUES(?)";
                    try (PreparedStatement pstmt5 = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                        pstmt5.setString(1, String.valueOf(urlItem));
                        pstmt5.executeUpdate();
                        ResultSet rs7 = pstmt5.getGeneratedKeys();
                        if (rs7.next()) {
                            urlId = rs7.getInt(1);
                        }
                    }
                }

                // Check if the combination of website_id and url_id already exists in the
                // website_urls table
                sql = "SELECT 1 FROM website_urls WHERE website_id = ? AND url_id = ?";
                try (PreparedStatement pstmt5 = conn.prepareStatement(sql)) {
                    pstmt5.setInt(1, websiteId);
                    pstmt5.setInt(2, urlId);
                    ResultSet rs8 = pstmt5.executeQuery();
                    if (!rs8.next()) {
                        // Link the URL to the website in the website_urls table
                        sql = "INSERT INTO website_urls(website_id, url_id) VALUES(?,?)";
                        try (PreparedStatement pstmt6 = conn.prepareStatement(sql)) {
                            pstmt6.setInt(1, websiteId);
                            pstmt6.setInt(2, urlId);
                            pstmt6.executeUpdate();
                        }
                    }
                } catch (SQLException e) {
                    System.out.println(e.getMessage());
                }
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }
    }
}