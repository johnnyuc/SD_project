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
        return (double) termCounts.getOrDefault(term, 0) / totalTerms;
    }

    public double calculateIdf(String term) throws SQLException {
        String sql = "SELECT COUNT(*) FROM websites WHERE description LIKE ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "%" + term + "%");
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                int docsWithTerm = rs.getInt(1);
                return Math.log((double) totalTerms / (1 + docsWithTerm));
            }
        }
        return 0;
    }
}