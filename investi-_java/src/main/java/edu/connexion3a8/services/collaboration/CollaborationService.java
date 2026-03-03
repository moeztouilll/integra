package edu.connexion3a8.services.collaboration;

import edu.connexion3a8.entities.collaboration.Collaboration;
import edu.connexion3a8.entities.collaboration.CollaborationMessage;
import edu.connexion3a8.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for Collaboration and Messaging operations.
 * Optimized with try-with-resources and helper methods for "perfection".
 */
public class CollaborationService {

    private Connection getCnx() {
        return MyConnection.getInstance().getCnx();
    }

    // ─── Collaboration Operations ──────────────────────────────────────────────

    public Collaboration createCollaboration(Collaboration c) throws SQLException {
        String query = "INSERT INTO collaboration (investment_id, entrepreneur_id, investor_id, start_date, status, health_score, default_probability, fairness_score, fairness_status, ideal_equity, equity_deviation) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pst = getCnx().prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pst.setInt(1, c.getInvestmentId());
            pst.setString(2, c.getEntrepreneurId());
            pst.setString(3, c.getInvestorId());
            pst.setTimestamp(4, c.getStartDate() != null ? new Timestamp(c.getStartDate().getTime())
                    : new Timestamp(System.currentTimeMillis()));
            pst.setString(5, c.getStatus() != null ? c.getStatus() : "ACTIVE");
            pst.setDouble(6, c.getHealthScore() > 0 ? c.getHealthScore() : 100.0);
            pst.setDouble(7, c.getDefaultProbability());
            pst.setDouble(8, c.getFairnessScore() > 0 ? c.getFairnessScore() : 100.0);
            pst.setString(9, c.getFairnessStatus() != null ? c.getFairnessStatus() : "BALANCED");
            pst.setDouble(10, c.getIdealEquity());
            pst.setDouble(11, c.getEquityDeviation());
            pst.executeUpdate();

            try (ResultSet rs = pst.getGeneratedKeys()) {
                if (rs.next()) {
                    c.setId(rs.getInt(1));
                    return c;
                }
            }
        }
        return null;
    }

    public Collaboration getCollaborationByInvestment(int investmentId, String investorId) throws SQLException {
        String query = "SELECT * FROM collaboration WHERE investment_id = ? AND investor_id = ?";
        try (PreparedStatement pst = getCnx().prepareStatement(query)) {
            pst.setInt(1, investmentId);
            pst.setString(2, investorId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return extractCollaboration(rs);
                }
            }
        }
        return null;
    }

    public void updateCollaborationScores(Collaboration collab) {
        String query = "UPDATE collaboration SET health_score = ?, default_probability = ?, status = ?, fairness_score = ?, fairness_status = ?, ideal_equity = ?, equity_deviation = ? WHERE id = ?";
        try (PreparedStatement pst = getCnx().prepareStatement(query)) {
            pst.setDouble(1, collab.getHealthScore());
            pst.setDouble(2, collab.getDefaultProbability());
            pst.setString(3, collab.getStatus());
            pst.setDouble(4, collab.getFairnessScore());
            pst.setString(5, collab.getFairnessStatus());
            pst.setDouble(6, collab.getIdealEquity());
            pst.setDouble(7, collab.getEquityDeviation());
            pst.setInt(8, collab.getId());
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ─── Messaging Operations ──────────────────────────────────────────────────

    public void sendMessage(CollaborationMessage msg) throws SQLException {
        String query = "INSERT INTO collaboration_message (investment_id, collaboration_id, sender_id, message, type, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pst = getCnx().prepareStatement(query)) {
            if (msg.getInvestmentId() > 0)
                pst.setInt(1, msg.getInvestmentId());
            else
                pst.setNull(1, Types.INTEGER);

            if (msg.getCollaborationId() > 0)
                pst.setInt(2, msg.getCollaborationId());
            else
                pst.setNull(2, Types.INTEGER);

            pst.setString(3, msg.getSenderId());
            pst.setString(4, msg.getMessage());
            pst.setString(5, msg.getType() != null ? msg.getType() : "GENERAL");
            pst.setTimestamp(6, new Timestamp(System.currentTimeMillis()));

            pst.executeUpdate();
        }
    }

    public List<CollaborationMessage> getMessagesForNegotiation(int investmentId) {
        return fetchMessages("SELECT * FROM collaboration_message WHERE investment_id = ? ORDER BY created_at ASC",
                investmentId);
    }

    public List<CollaborationMessage> getMessagesForCollaboration(int collaborationId) {
        return fetchMessages("SELECT * FROM collaboration_message WHERE collaboration_id = ? ORDER BY created_at ASC",
                collaborationId);
    }

    private List<CollaborationMessage> fetchMessages(String query, int idParam) {
        List<CollaborationMessage> list = new ArrayList<>();
        try (PreparedStatement pst = getCnx().prepareStatement(query)) {
            pst.setInt(1, idParam);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    CollaborationMessage msg = new CollaborationMessage();
                    msg.setId(rs.getInt("id"));
                    msg.setInvestmentId(rs.getInt("investment_id"));
                    msg.setCollaborationId(rs.getInt("collaboration_id"));
                    msg.setSenderId(rs.getString("sender_id"));
                    msg.setMessage(rs.getString("message"));
                    msg.setType(rs.getString("type"));
                    msg.setCreatedAt(rs.getTimestamp("created_at"));
                    list.add(msg);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private Collaboration extractCollaboration(ResultSet rs) throws SQLException {
        Collaboration c = new Collaboration();
        c.setId(rs.getInt("id"));
        c.setInvestmentId(rs.getInt("investment_id"));
        c.setEntrepreneurId(rs.getString("entrepreneur_id"));
        c.setInvestorId(rs.getString("investor_id"));
        c.setStartDate(rs.getTimestamp("start_date"));
        c.setStatus(rs.getString("status"));
        c.setHealthScore(rs.getDouble("health_score"));
        c.setDefaultProbability(rs.getDouble("default_probability"));

        try {
            c.setFairnessScore(rs.getDouble("fairness_score"));
            c.setFairnessStatus(rs.getString("fairness_status"));
            c.setIdealEquity(rs.getDouble("ideal_equity"));
            c.setEquityDeviation(rs.getDouble("equity_deviation"));
        } catch (SQLException ignored) {
        }
        return c;
    }
}
