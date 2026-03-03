package edu.connexion3a8.services.collaboration;

import edu.connexion3a8.entities.collaboration.Milestone;
import edu.connexion3a8.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for Milestone CRUD and rebalancing.
 * Refactored for "perfection" with try-with-resources and helper-based
 * extraction.
 */
public class MilestoneService {

    private Connection getCnx() {
        return MyConnection.getInstance().getCnx();
    }

    // ─── CRUD ────────────────────────────────────────────────────────────────────

    public Milestone addMilestone(Milestone m) throws SQLException {
        String sql = "INSERT INTO milestone (collaboration_id, title, description, weight, due_date, status, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
        try (PreparedStatement pst = getCnx().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pst.setInt(1, m.getCollaborationId());
            pst.setString(2, m.getTitle());
            pst.setString(3, m.getDescription() != null ? m.getDescription() : "");
            pst.setDouble(4, m.getWeight());
            if (m.getDueDate() != null)
                pst.setDate(5, new java.sql.Date(m.getDueDate().getTime()));
            else
                pst.setNull(5, Types.DATE);
            pst.setString(6, m.getStatus() != null ? m.getStatus() : "PENDING");
            pst.executeUpdate();

            try (ResultSet rs = pst.getGeneratedKeys()) {
                if (rs.next())
                    m.setId(rs.getInt(1));
            }
        }
        autoRebalanceWeights(m.getCollaborationId());
        return m;
    }

    public List<Milestone> getMilestonesForCollaboration(int collaborationId) {
        List<Milestone> list = new ArrayList<>();
        String sql = "SELECT * FROM milestone WHERE collaboration_id = ? ORDER BY due_date ASC, created_at ASC";
        try (PreparedStatement pst = getCnx().prepareStatement(sql)) {
            pst.setInt(1, collaborationId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next())
                    list.add(extract(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean updateMilestoneStatus(int milestoneId, String newStatus) {
        String sql = "UPDATE milestone SET status = ? WHERE id = ?";
        try (PreparedStatement pst = getCnx().prepareStatement(sql)) {
            pst.setString(1, newStatus);
            pst.setInt(2, milestoneId);
            return pst.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteMilestone(int milestoneId, int collaborationId) {
        String sql = "DELETE FROM milestone WHERE id = ?";
        try (PreparedStatement pst = getCnx().prepareStatement(sql)) {
            pst.setInt(1, milestoneId);
            boolean success = pst.executeUpdate() > 0;
            if (success) {
                autoRebalanceWeights(collaborationId);
            }
            return success;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ─── Progress Calculation ────────────────────────────────────────────────────

    public double calculateProgress(int collaborationId) {
        String sql = "SELECT COALESCE(SUM(weight), 0) FROM milestone WHERE collaboration_id = ? AND status = 'COMPLETED'";
        try (PreparedStatement pst = getCnx().prepareStatement(sql)) {
            pst.setInt(1, collaborationId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next())
                    return Math.min(100.0, rs.getDouble(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    public void autoRebalanceWeights(int collaborationId) {
        List<Milestone> milestones = getMilestonesForCollaboration(collaborationId);
        int size = milestones.size();
        if (size == 0)
            return;

        double equalWeight = Math.floor((100.0 / size) * 100) / 100.0; // Round to 2 decimals
        double totalAssigned = 0;

        String sql = "UPDATE milestone SET weight = ? WHERE id = ?";
        try (PreparedStatement pst = getCnx().prepareStatement(sql)) {
            for (int i = 0; i < size; i++) {
                Milestone m = milestones.get(i);
                double weightToAssign;
                if (i == size - 1) {
                    // Last one gets the remainder to ensure exact 100.0
                    weightToAssign = 100.0 - totalAssigned;
                } else {
                    weightToAssign = equalWeight;
                    totalAssigned += weightToAssign;
                }
                pst.setDouble(1, weightToAssign);
                pst.setInt(2, m.getId());
                pst.addBatch();
            }
            pst.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Milestone extract(ResultSet rs) throws SQLException {
        Milestone m = new Milestone();
        m.setId(rs.getInt("id"));
        m.setCollaborationId(rs.getInt("collaboration_id"));
        m.setTitle(rs.getString("title"));
        m.setDescription(rs.getString("description"));
        m.setWeight(rs.getDouble("weight"));
        java.sql.Date d = rs.getDate("due_date");
        if (d != null)
            m.setDueDate(new java.util.Date(d.getTime()));
        m.setStatus(rs.getString("status"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null)
            m.setCreatedAt(new java.util.Date(ts.getTime()));
        return m;
    }
}
