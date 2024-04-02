package Server.IndexStorageBarrel.Operations;

import java.sql.*;
import java.util.Collections;
import java.util.List;

public class BarrelProcessing {
    private final Connection conn;

    public BarrelProcessing(Connection conn) {
        this.conn = conn;
    }

    // Method to calculate the TF value of a term in a document
    public double calcTF(String term, List<String> subsetTerms) {
        // Print TF values
        // System.err.println("term: " + term);
        // System.err.println("subsetTerms: " + subsetTerms);
        // System.err.println("TF - termCount/totalSubsetTerms: " + (double)
        // Collections.frequency(subsetTerms, term) / subsetTerms.size());
        int termCount = Collections.frequency(subsetTerms, term);
        int totalSubsetTerms = subsetTerms.size();
        return (double) termCount / totalSubsetTerms;
    }

    // Method to calculate the IDF value of a term
    public double calcIDF(String term, List<String> subsetTerms, int docNr) throws SQLException {
        int tokenDocnr = getTokenDocnr(term, subsetTerms);

        // Print IDF values
        // System.err.println("docNr: " + docNr);
        // System.err.println("tokenDocnr: " + tokenDocnr);
        // System.err.println("IDF - log(docNr/tokenDocnr): " + Math.log((double) docNr
        // / ((tokenDocnr == 0) ? 1 : tokenDocnr)));

        return Math.log((double) docNr / ((tokenDocnr == 0) ? 1 : tokenDocnr));
    }

    // Method to calculate the TF-IDF value of a term in a document
    public double calcTFIDF(String term, List<String> subsetTerms, int docNr) {
        double tf = calcTF(term, subsetTerms);
        double idf = 0;
        try {
            idf = calcIDF(term, subsetTerms, docNr);
        } catch (SQLException e) {
            System.err.println("Error calculating IDF: " + e.getMessage());
        }

        // Print TF-IDF value
        System.err.println("tf * idf: " + tf * idf);

        return tf * idf;
    }

    // Method to get the total number of documents
    int getDocnr() throws SQLException {
        String sql = "SELECT COUNT(DISTINCT website_id) FROM website_keywords";
        int totalDocs = 0;
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                totalDocs = rs.getInt(1);
            }
        }
        return totalDocs;
    }

    // Method to check if a document contains a specific term
    boolean docContainsToken(int websiteId, String term) throws SQLException {
        String sql = "SELECT 1 FROM website_keywords WHERE website_id = ? AND keyword_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, websiteId);
            pstmt.setInt(2, getTokenId(term));
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        }
    }

    // Method to get the number of documents containing a specific term
    private int getTokenDocnr(String term, List<String> subsetTerms) throws SQLException {
        String sql = "SELECT COUNT(DISTINCT website_id) FROM website_keywords WHERE keyword_id = ?";
        int docsWithTerm = 0;
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, getTokenId(term));
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                docsWithTerm = rs.getInt(1);
            }
        }

        if (subsetTerms.contains(term))
            docsWithTerm++;

        return docsWithTerm;
    }

    public int getTokenId(String term) throws SQLException {
        String sql = "SELECT id FROM keywords WHERE keyword = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, term);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            } else {
                // Insert the keyword into the database if it's not found
                sql = "INSERT INTO keywords(keyword) VALUES(?)";
                try (PreparedStatement insertStmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    insertStmt.setString(1, term);
                    insertStmt.executeUpdate();
                    ResultSet generatedKeys = insertStmt.getGeneratedKeys();
                    if (generatedKeys.next()) {
                        return generatedKeys.getInt(1);
                    } else {
                        throw new SQLException("Creating keyword failed, no ID obtained.");
                    }
                }
            }
        }
    }
}