package edu.connexion3a8.services.collaboration;

import edu.connexion3a8.entities.collaboration.Project;
import edu.connexion3a8.interfaces.IService;
import edu.connexion3a8.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for Project CRUD and filtering.
 * Refactored for "perfection" with try-with-resources and helper-based
 * extraction.
 */
public class ProjectService implements IService<Project> {

    private Connection getConnection() throws SQLException {
        return MyConnection.getInstance().getCnx();
    }

    @Override
    public void addEntity(Project p) throws SQLException {
        String sql = "INSERT INTO project " +
                "(entrepreneur_id, title, description, amountRequested, equityOffered, status, project_date, category) "
                +
                "VALUES (?, ?, ?, ?, ?, ?, CURRENT_DATE, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pst = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pst.setString(1, p.getEntrepreneurId());
            pst.setString(2, p.getTitle());
            pst.setString(3, p.getDescription());
            pst.setDouble(4, p.getAmountRequested());
            pst.setDouble(5, p.getEquityOffered());
            pst.setString(6, p.getStatus());
            pst.setString(7, p.getCategory());
            pst.executeUpdate();

            try (ResultSet rs = pst.getGeneratedKeys()) {
                if (rs.next()) {
                    p.setProjectId(rs.getInt(1));
                }
            }
        }
    }

    @Override
    public void deleteEntity(Project p) {
        String sqlInvestments = "DELETE FROM investment WHERE project_id = ?";
        String sqlProject = "DELETE FROM project WHERE project_id = ?";

        try (Connection cnx = getConnection()) {
            cnx.setAutoCommit(false);
            try (PreparedStatement pstInv = cnx.prepareStatement(sqlInvestments)) {
                pstInv.setInt(1, p.getProjectId());
                pstInv.executeUpdate();
            }
            try (PreparedStatement pstProj = cnx.prepareStatement(sqlProject)) {
                pstProj.setInt(1, p.getProjectId());
                pstProj.executeUpdate();
            }
            cnx.commit();
            cnx.setAutoCommit(true);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean update(int id, Project p) {
        String sql = "UPDATE project SET title = ?, description = ?, amountRequested = ?, equityOffered = ?, status = ?, category = ? WHERE project_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, p.getTitle());
            pst.setString(2, p.getDescription());
            pst.setDouble(3, p.getAmountRequested());
            pst.setDouble(4, p.getEquityOffered());
            pst.setString(5, p.getStatus());
            pst.setString(6, p.getCategory());
            pst.setInt(7, id);
            return pst.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public List<Project> getData() {
        List<Project> list = new ArrayList<>();
        String sql = "SELECT * FROM project ORDER BY project_date DESC";
        try (Connection conn = getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Project> getProjectsByStatus(String status) {
        List<Project> list = new ArrayList<>();
        String sql = "SELECT * FROM project WHERE status = ? ORDER BY project_date DESC";
        try (Connection conn = getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, status);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public Project readById(int projectId) {
        String sql = "SELECT * FROM project WHERE project_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, projectId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next())
                    return mapRow(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Project> getProjectsByEntrepreneur(String entrepreneurId) {
        List<Project> list = new ArrayList<>();
        String sql = "SELECT * FROM project WHERE entrepreneur_id = ? ORDER BY project_date DESC";
        try (Connection conn = getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, entrepreneurId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean existsByTitleOrDescription(String title, String description) {
        String sql = "SELECT COUNT(*) FROM project WHERE LOWER(title) = ? OR LOWER(description) = ?";
        try (Connection conn = getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, title.toLowerCase().trim());
            pst.setString(2, description.toLowerCase().trim());
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next())
                    return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Project> searchProjects(String keyword) {
        if (keyword == null || keyword.isBlank())
            return getData();
        List<Project> list = new ArrayList<>();
        String sql = "SELECT * FROM project WHERE title LIKE ? OR description LIKE ? ORDER BY project_date DESC";
        try (Connection conn = getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            String pattern = "%" + keyword.trim() + "%";
            pst.setString(1, pattern);
            pst.setString(2, pattern);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Project> getProjectsByCategory(String category) {
        if (category == null || category.equalsIgnoreCase("All"))
            return getData();
        List<Project> list = new ArrayList<>();
        String sql = "SELECT * FROM project WHERE category = ? ORDER BY project_date DESC";
        try (Connection conn = getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, category);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public int getTotalProjectCount() {
        try (Connection conn = getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM project")) {
            if (rs.next())
                return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int getFundedProjectCount() {
        return getCountByStatus("FUNDED");
    }

    public int getOpenProjectCount() {
        return getCountByStatus("OPEN");
    }

    private int getCountByStatus(String status) {
        String sql = "SELECT COUNT(*) FROM project WHERE status = ?";
        try (Connection conn = getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, status);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next())
                    return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private Project mapRow(ResultSet rs) throws SQLException {
        Project p = new Project();
        p.setProjectId(rs.getInt("project_id"));
        p.setEntrepreneurId(rs.getString("entrepreneur_id"));
        p.setTitle(rs.getString("title"));
        p.setDescription(rs.getString("description"));
        p.setAmountRequested(rs.getDouble("amountRequested"));
        p.setEquityOffered(rs.getDouble("equityOffered"));
        p.setStatus(rs.getString("status"));
        p.setProjectDate(rs.getDate("project_date"));
        try {
            p.setCategory(rs.getString("category"));
        } catch (SQLException ignored) {
            p.setCategory("Other");
        }
        return p;
    }
}
