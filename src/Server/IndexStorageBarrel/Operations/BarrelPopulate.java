package Server.IndexStorageBarrel.Operations;

import ReliableMulticast.Objects.CrawlData;

import java.net.URL;
import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BarrelPopulate {
    private final Connection conn;
    private final Map<String, Integer> termCounts;
    private int totalTerms;
    private BarrelProcessing barrelProcessing;

    public BarrelPopulate(Connection conn) {
        this.conn = conn;
        this.termCounts = new HashMap<>();
        this.totalTerms = 0;
        this.barrelProcessing = new BarrelProcessing(conn, termCounts, totalTerms);
    }

    public void insertData(CrawlData crawlData) {
        // Extract data from CrawlData object
        String url = String.valueOf(crawlData.getUrl());
        String title = crawlData.getTitle();
        String description = crawlData.getDescription();
        List<String> tokens = crawlData.getTokens();
        List<URL> urls = crawlData.getUrlStrings();

        // Update term counts and total terms
        for (String token : tokens) {
            termCounts.put(token, termCounts.getOrDefault(token, 0) + 1);
            totalTerms++;
        }

        // Update BarrelProcessing with new term counts and total terms
        barrelProcessing = new BarrelProcessing(conn, termCounts, totalTerms);

        // Insert data into websites table and get the inserted row ID
        String sql = "INSERT INTO websites(url, title, description) VALUES(?,?,?)";
        int websiteId = 0;
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, url);
            pstmt.setString(2, title);
            pstmt.setString(3, description);
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                websiteId = rs.getInt(1);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        // Insert each token into keywords table, link it to the website in website_keywords table, and calculate TF-IDF
        for (String token : tokens) {
            sql = "INSERT INTO keywords(keyword) VALUES(?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, token);
                pstmt.executeUpdate();
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    int keywordId = rs.getInt(1);
                    double tf = barrelProcessing.calculateTf(token);
                    double idf = barrelProcessing.calculateIdf(token);
                    double tfidf = tf * idf;
                    sql = "INSERT INTO website_keywords(website_id, keyword_id, tf_idf) VALUES(?,?,?)";
                    try (PreparedStatement pstmt2 = conn.prepareStatement(sql)) {
                        pstmt2.setInt(1, websiteId);
                        pstmt2.setInt(2, keywordId);
                        pstmt2.setDouble(3, tfidf);
                        pstmt2.executeUpdate();
                    }
                }
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }

        // Insert each URL into urls table and link it to the website in website_urls table
        for (URL urlItem : urls) {
            sql = "INSERT INTO urls(url) VALUES(?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, String.valueOf(urlItem));
                pstmt.executeUpdate();
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    int urlId = rs.getInt(1);
                    sql = "INSERT INTO website_urls(website_id, url_id) VALUES(?,?)";
                    try (PreparedStatement pstmt2 = conn.prepareStatement(sql)) {
                        pstmt2.setInt(1, websiteId);
                        pstmt2.setInt(2, urlId);
                        pstmt2.executeUpdate();
                    }
                }
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }
    }
}