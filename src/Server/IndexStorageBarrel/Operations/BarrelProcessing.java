package Server.IndexStorageBarrel.Operations;

import java.sql.*;
import java.util.Collections;
import java.util.List;

/**
 * The BarrelProcessing class provides methods for calculating TF, IDF, and
 * TF-IDF values of terms in documents.
 */
public class BarrelProcessing {
    private final Connection conn;

    /**
     * Constructs a BarrelProcessing object with the given database connection.
     *
     * @param conn the database connection
     */
    public BarrelProcessing(Connection conn) {
        this.conn = conn;
    }

    /**
     * Calculates the Term Frequency (TF) value of a term in a document.
     *
     * @param term        the term to calculate the TF value for
     * @param subsetTerms the list of terms in the document
     * @return the TF value of the term
     */
    public double calcTF(String term, List<String> subsetTerms) {
        int termCount = Collections.frequency(subsetTerms, term);
        int totalSubsetTerms = subsetTerms.size();

        // Print TF values
        System.err.println("term: " + term);
        System.err.println("subsetTerms: " + subsetTerms);
        System.err.println("TF - termCount/totalSubsetTerms: "
                + (double) Collections.frequency(subsetTerms, term) / subsetTerms.size());

        return (double) termCount / totalSubsetTerms;
    }

    /**
     * Calculates the Inverse Document Frequency (IDF) value of a term.
     *
     * @param term        the term to calculate the IDF value for
     * @param subsetTerms the list of terms in the document
     * @param docNr       the total number of documents
     * @return the IDF value of the term
     * @throws SQLException if an error occurs while accessing the database
     */
    public double calcIDF(String term, List<String> subsetTerms, int docNr) throws SQLException {
        int tokenDocnr = getTokenDocnr(term, subsetTerms);

        // Print IDF values
        System.err.println("docNr: " + docNr);
        System.err.println("tokenDocnr: " + tokenDocnr);
        System.err.println("IDF - log(1 + docNr/tokenDocnr): " + Math.log(1 + (double) (docNr/tokenDocnr)));

        return Math.log(1 + (double) (docNr/tokenDocnr));
    }

    /**
     * Calculates the TF-IDF value of a term in a document.
     *
     * @param term        the term to calculate the TF-IDF value for
     * @param subsetTerms the list of terms in the document
     * @return the TF-IDF value of the term
     */
    public double calcTFIDF(String term, List<String> subsetTerms, boolean newUrl) throws SQLException {
        double tf = calcTF(term, subsetTerms);
        double idf = 0;

        int docNr = newUrl ? getDocnr() + 1 : getDocnr();

        try {
            idf = calcIDF(term, subsetTerms, docNr);
        } catch (SQLException e) {
            System.err.println("Error calculating IDF: " + e.getMessage());
        }

        // Print TF-IDF value
        System.err.println("tf * idf: " + tf * idf);
        System.err.println();

        return tf * idf;
    }

    /**
     * Gets the total number of documents.
     *
     * @return the total number of documents
     * @throws SQLException if an error occurs while accessing the database
     */
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

    /**
     * Gets the number of documents containing a specific term.
     *
     * @param term        the term to count documents for
     * @param subsetTerms the list of terms in the document
     * @return the number of documents containing the term
     * @throws SQLException if an error occurs while accessing the database
     */
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

    /**
     * Gets the ID of a token (term) from the database.
     * If the token is not found, it is inserted into the database.
     *
     * @param term the term to get the ID for
     * @return the ID of the token
     * @throws SQLException if an error occurs while accessing the database
     */
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