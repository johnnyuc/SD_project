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
            int keywordId = 0;
            sql = "SELECT id FROM keywords WHERE keyword = ?";
            try (PreparedStatement pstmt1 = conn.prepareStatement(sql)) {
                pstmt1.setString(1, token);
                ResultSet rs = pstmt1.executeQuery();
                if (rs.next()) {
                    keywordId = rs.getInt("id");
                } else {
                    sql = "INSERT INTO keywords(keyword) VALUES(?)";
                    try (PreparedStatement pstmt2 = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                        pstmt2.setString(1, token);
                        pstmt2.executeUpdate();
                        ResultSet rs2 = pstmt2.getGeneratedKeys();
                        if (rs2.next()) {
                            keywordId = rs2.getInt(1);
                        }
                    }
                }
                // Calculate TF and IDF for the token
                double tf = barrelProcessing.calculateTf(token);
                double idf = barrelProcessing.calculateIdf(token);

                // Calculate TF-IDF by multiplying TF and IDF
                double tfIdf = tf * idf;

                // Link the keyword to the website in the website_keywords table and store the TF-IDF score
                sql = "INSERT INTO website_keywords(website_id, keyword_id, tf_idf) VALUES(?,?,?)";
                try (PreparedStatement pstmt3 = conn.prepareStatement(sql)) {
                    pstmt3.setInt(1, websiteId);
                    pstmt3.setInt(2, keywordId);
                    pstmt3.setDouble(3, tfIdf);
                    pstmt3.executeUpdate();
                } catch (SQLException e) {
                    System.out.println(e.getMessage());
                }
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }

        // Insert each URL into urls table and link it to the website in website_urls table
        for (URL urlItem : urls) {
            int urlId = 0;
            sql = "SELECT id FROM urls WHERE url = ?";
            try (PreparedStatement pstmt4 = conn.prepareStatement(sql)) {
                pstmt4.setString(1, String.valueOf(urlItem));
                ResultSet rs = pstmt4.executeQuery();
                if (rs.next()) {
                    urlId = rs.getInt("id");
                } else {
                    sql = "INSERT INTO urls(url) VALUES(?)";
                    try (PreparedStatement pstmt5 = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                        pstmt5.setString(1, String.valueOf(urlItem));
                        pstmt5.executeUpdate();
                        ResultSet rs2 = pstmt5.getGeneratedKeys();
                        if (rs2.next()) {
                            urlId = rs2.getInt(1);
                        }
                    }
                }
                // Link the URL to the website in the website_urls table
                sql = "INSERT INTO website_urls(website_id, url_id) VALUES(?,?)";
                try (PreparedStatement pstmt6 = conn.prepareStatement(sql)) {
                    pstmt6.setInt(1, websiteId);
                    pstmt6.setInt(2, urlId);
                    pstmt6.executeUpdate();
                } catch (SQLException e) {
                    System.out.println(e.getMessage());
                }
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }
    }
}