package edu.connexion3a8.services.collaboration;

import edu.connexion3a8.entities.collaboration.Investment;
import edu.connexion3a8.interfaces.IService;
import edu.connexion3a8.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for Investment CRUD and logic operations.
 * Optimized with try-with-resources and helper methods for "perfection".
 */
public class InvestmentService implements IService<Investment> {

    private Connection getConnection() throws SQLException {
        return MyConnection.getInstance().getCnx();
    }

    @Override
    public void addEntity(Investment i) throws SQLException {
        String sql = "INSERT INTO investment " +
                "(project_id, investor_id, totalAmount, durationMonths, amountPerPeriod, equityRequested, status, investment_date) "
                +
                "VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_DATE)";

        try (Connection conn = getConnection();
             PreparedStatement pst = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pst.setInt(1, i.getProjectId());
            pst.setString(2, i.getInvestorId());
            pst.setDouble(3, i.getTotalAmount());
            pst.setInt(4, i.getDurationMonths());
            pst.setDouble(5, i.getAmountPerPeriod());
            pst.setDouble(6, i.getEquityRequested());
            pst.setString(7, i.getStatus());
            pst.executeUpdate();

            try (ResultSet rs = pst.getGeneratedKeys()) {
                if (rs.next()) {
                    i.setInvestmentId(rs.getInt(1));
                }
            }
        }
    }

    @Override
    public void deleteEntity(Investment i) {
        String sqlDelete = "DELETE FROM investment WHERE investment_id = ?";
        String sqlUpdateProject = "UPDATE project SET status = 'OPEN' WHERE project_id = ?";

        try (Connection cnx = getConnection()) {
            if ("ACCEPTED".equalsIgnoreCase(i.getStatus())) {
                cnx.setAutoCommit(false);
                try (PreparedStatement pstProj = cnx.prepareStatement(sqlUpdateProject)) {
                    pstProj.setInt(1, i.getProjectId());
                    pstProj.executeUpdate();
                }
                try (PreparedStatement pstDel = cnx.prepareStatement(sqlDelete)) {
                    pstDel.setInt(1, i.getInvestmentId());
                    pstDel.executeUpdate();
                }
                cnx.commit();
                cnx.setAutoCommit(true);
            } else {
                try (PreparedStatement pst = cnx.prepareStatement(sqlDelete)) {
                    pst.setInt(1, i.getInvestmentId());
                    pst.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean update(int id, Investment i) {
        String sql = "UPDATE investment SET totalAmount = ?, durationMonths = ?, amountPerPeriod = ?, equityRequested = ?, status = ? WHERE investment_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, i.getTotalAmount());
            ps.setInt(2, i.getDurationMonths());
            ps.setDouble(3, i.getAmountPerPeriod());
            ps.setDouble(4, i.getEquityRequested());
            ps.setString(5, i.getStatus());
            ps.setInt(6, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public List<Investment> getData() {
        List<Investment> list = new ArrayList<>();
        String sql = "SELECT * FROM investment";
        try (Connection conn = getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(extractInvestment(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Investment> getInvestmentsByStatus(String status) {
        List<Investment> list = new ArrayList<>();
        String sql = "SELECT * FROM investment WHERE status = ?";
        try (Connection conn = getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, status);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    list.add(extractInvestment(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Investment> getInvestmentsByProject(int projectId) {
        List<Investment> list = new ArrayList<>();
        String sql = "SELECT * FROM investment WHERE project_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, projectId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    list.add(extractInvestment(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Investment> getInvestmentsByInvestor(String investorId) {
        List<Investment> list = new ArrayList<>();
        String sql = "SELECT * FROM investment WHERE investor_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, investorId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    list.add(extractInvestment(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean acceptInvestment(int investmentId, int projectId) {
        try (Connection cnx = getConnection()) {
            cnx.setAutoCommit(false);
            try (PreparedStatement pstAccept = cnx
                    .prepareStatement("UPDATE investment SET status = 'ACCEPTED' WHERE investment_id = ?")) {
                pstAccept.setInt(1, investmentId);
                pstAccept.executeUpdate();
            }
            try (PreparedStatement pstProject = cnx
                    .prepareStatement("UPDATE project SET status = 'FUNDED' WHERE project_id = ?")) {
                pstProject.setInt(1, projectId);
                pstProject.executeUpdate();
            }
            try (PreparedStatement pstRefuse = cnx.prepareStatement(
                    "UPDATE investment SET status = 'REFUSED' WHERE project_id = ? AND investment_id != ? AND status = 'PENDING'")) {
                pstRefuse.setInt(1, projectId);
                pstRefuse.setInt(2, investmentId);
                pstRefuse.executeUpdate();
            }
            cnx.commit();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateProgress(int investmentId, int percentage, String log, int payments) {
        String sql = "UPDATE investment SET progressPercentage = ?, latestProgressLog = ?, paymentMonthsCompleted = ? WHERE investment_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, percentage);
            ps.setString(2, log);
            ps.setInt(3, payments);
            ps.setInt(4, investmentId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean markPaymentDone(int investmentId, int payments) {
        String sql = "UPDATE investment SET paymentMonthsCompleted = ?, lastPaymentDate = CURRENT_TIMESTAMP() WHERE investment_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, payments);
            ps.setInt(2, investmentId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public double getTotalInvestedVolume() {
        try (Connection conn = getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT SUM(totalAmount) FROM investment WHERE status = 'ACCEPTED'")) {
            if (rs.next())
                return rs.getDouble(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int getTotalInvestmentCount() {
        try (Connection conn = getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM investment")) {
            if (rs.next())
                return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public boolean syncProgressFromMilestones(int investmentId, double milestoneProgress) {
        String sql = "UPDATE investment SET progressPercentage = ? WHERE investment_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, (int) Math.round(milestoneProgress));
            ps.setInt(2, investmentId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean checkIsLate(Investment i, java.time.LocalDate today) {
        if (i.getInvestmentDate() == null)
            return false;
        java.time.LocalDate investDate = new java.sql.Date(i.getInvestmentDate().getTime()).toLocalDate();
        int anniversaryDay = investDate.getDayOfMonth();
        java.time.LocalDate currentPeriodStart = today.withDayOfMonth(Math.min(anniversaryDay, today.lengthOfMonth()));
        if (today.isBefore(currentPeriodStart)) {
            currentPeriodStart = currentPeriodStart.minusMonths(1);
        }
        java.time.LocalDate lateTriggerDate = currentPeriodStart.plusDays(7);
        java.time.LocalDate lastPay = i.getLastPaymentDate() != null
                ? new java.sql.Date(i.getLastPaymentDate().getTime()).toLocalDate()
                : null;
        boolean alreadyPaidThisMonth = lastPay != null && lastPay.getYear() == today.getYear()
                && lastPay.getMonthValue() == today.getMonthValue();
        return today.isAfter(lateTriggerDate) && !alreadyPaidThisMonth;
    }

    public Investment getInvestmentById(int investmentId) {
        String sql = "SELECT * FROM investment WHERE investment_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, investmentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return extractInvestment(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Investment extractInvestment(ResultSet rs) throws SQLException {
        Investment i = new Investment();
        i.setInvestmentId(rs.getInt("investment_id"));
        i.setProjectId(rs.getInt("project_id"));
        i.setInvestorId(rs.getString("investor_id"));
        i.setTotalAmount(rs.getDouble("totalAmount"));
        i.setDurationMonths(rs.getInt("durationMonths"));
        i.setAmountPerPeriod(rs.getDouble("amountPerPeriod"));
        i.setEquityRequested(rs.getDouble("equityRequested"));
        i.setStatus(rs.getString("status"));
        i.setInvestmentDate(rs.getDate("investment_date"));
        try {
            i.setProgressPercentage(rs.getInt("progressPercentage"));
            i.setLatestProgressLog(rs.getString("latestProgressLog"));
            i.setPaymentMonthsCompleted(rs.getInt("paymentMonthsCompleted"));
            Timestamp ts = rs.getTimestamp("lastPaymentDate");
            if (ts != null)
                i.setLastPaymentDate(new java.util.Date(ts.getTime()));
        } catch (SQLException ignored) {
        }
        return i;
    }
}
