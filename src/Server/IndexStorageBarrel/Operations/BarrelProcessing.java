package Server.IndexStorageBarrel.Operations;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public class BarrelProcessing {
    private final Connection conn;
    private final Map<String, Integer> termCounts;
    private final int totalTerms;

    public BarrelProcessing(Connection conn, Map<String, Integer> termCounts, int totalTerms) {
        this.conn = conn;
        this.termCounts = termCounts;
        this.totalTerms = totalTerms;
    }

    public double calculateTf(String term) {
        System.err.println("termCounts: " + termCounts);
        System.err.println("term: " + term);
        System.err.println("termCounts.getOrDefault(term, 0): " + termCounts.getOrDefault(term, 0));
        return (double) termCounts.getOrDefault(term, 0) / totalTerms;
    }

    public double calculateIdf(String term) throws SQLException {
        // Get keyword_id for the given term
        String sql = "SELECT id FROM keywords WHERE keyword = ?";
        int keywordId = 0;
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, term);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                keywordId = rs.getInt("id");
            }
        }

        // Use keyword_id to get the count of distinct website_id
        sql = "SELECT COUNT(DISTINCT website_id) FROM website_keywords WHERE keyword_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, keywordId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                int docsWithTerm = rs.getInt(1);
                System.err.println("docsWithTerm: " + docsWithTerm);
                System.err.println("totalTerms: " + totalTerms);
                System.err.println("Math.log((double) totalTerms / (1 + docsWithTerm)): " + Math.log((double) totalTerms / (1 + docsWithTerm)));
                return Math.log((double) totalTerms / (1 + docsWithTerm));
            }
        }
        return 0;
    }

    public double calculateTfIdf(String term) {
        double tf = calculateTf(term);
        double idf = 0;
        try {
            idf = calculateIdf(term);
        } catch (SQLException e) {
            System.err.println("Error calculating IDF: " + e.getMessage());
        }
        System.err.println("tf: " + tf);
        System.err.println("idf: " + idf);
        System.err.println("tf * idf: " + tf * idf);
        return tf * idf;
    }
}