package edu.connexion3a8.tools;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyConnection {
    private String url;
    private String login;
    private String pwd;
    private Connection cnx;
    public static MyConnection instance;

    private MyConnection() {
        // Load from config
        java.util.Properties props = new java.util.Properties();
        try (java.io.InputStream input = getClass().getResourceAsStream("/config.properties")) {
            if (input != null) {
                props.load(input);
                this.url = props.getProperty("db.url", "jdbc:mysql://localhost:3306/3a8");
                this.login = props.getProperty("db.user", "root");
                this.pwd = props.getProperty("db.password", "");
                System.out.println("Loaded DB config: " + this.url);
            } else {
                // Fallback
                this.url = "jdbc:mysql://localhost:3306/3a8";
                this.login = "root";
                this.pwd = "";
                System.out.println("No config.properties found, using default 3a8");
            }
        } catch (java.io.IOException ex) {
            ex.printStackTrace();
            this.url = "jdbc:mysql://localhost:3306/3a8";
            this.login = "root";
            this.pwd = "";
        }

        try {
            cnx = DriverManager.getConnection(url, login, pwd);
            System.out.println("Connexion établie!");

            // Auto-patch Database constraints that shouldn't be unique
            try (java.sql.Statement st = cnx.createStatement()) {
                java.sql.ResultSet rs = st.executeQuery(
                        "SHOW INDEX FROM investment WHERE Column_name = 'project_id' AND Non_unique = 0 AND Key_name != 'PRIMARY'");
                java.util.List<String> indexesToDrop = new java.util.ArrayList<>();
                while (rs.next()) {
                    indexesToDrop.add(rs.getString("Key_name"));
                }
                for (String idx : indexesToDrop) {
                    st.executeUpdate("ALTER TABLE investment DROP INDEX " + idx);
                    System.out.println("Patched DB: Dropped unique index " + idx + " on investment table.");
                }

                // Auto-patch missing Idea 5 columns (checked via INFORMATION_SCHEMA)
                String[][] newCols = {
                        { "progressPercentage",
                                "ALTER TABLE investment ADD COLUMN progressPercentage int(11) DEFAULT 0" },
                        { "latestProgressLog",
                                "ALTER TABLE investment ADD COLUMN latestProgressLog varchar(255) DEFAULT NULL" },
                        { "paymentMonthsCompleted",
                                "ALTER TABLE investment ADD COLUMN paymentMonthsCompleted int(11) DEFAULT 0" },
                        { "lastPaymentDate",
                                "ALTER TABLE investment ADD COLUMN lastPaymentDate DATETIME DEFAULT NULL" }
                };
                for (String[] col : newCols) {
                    try {
                        java.sql.ResultSet chk = st.executeQuery(
                                "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'investment' AND COLUMN_NAME = '"
                                        + col[0] + "'");
                        if (chk.next() && chk.getInt(1) == 0) {
                            st.executeUpdate(col[1]);
                            System.out.println("Patched DB: Added column " + col[0] + " to investment.");
                        }
                    } catch (Exception ignored) {
                    }
                }

                // Auto-create Governance Layer tables
                String createCollaborationTable = "CREATE TABLE IF NOT EXISTS collaboration (" +
                        "id INT PRIMARY KEY AUTO_INCREMENT, " +
                        "investment_id INT NOT NULL, " +
                        "entrepreneur_id VARCHAR(36) NOT NULL, " +
                        "investor_id VARCHAR(36) NOT NULL, " +
                        "start_date DATETIME, " +
                        "status VARCHAR(50) DEFAULT 'ACTIVE', " +
                        "health_score DOUBLE DEFAULT 100, " +
                        "default_probability DOUBLE DEFAULT 0, " +
                        "fairness_score DOUBLE DEFAULT 100, " +
                        "fairness_status VARCHAR(50) DEFAULT 'BALANCED', " +
                        "ideal_equity DOUBLE DEFAULT 0, " +
                        "equity_deviation DOUBLE DEFAULT 0)";

                // sender_id must be VARCHAR(36) to hold UUID strings (CollaborationService uses
                // setString)
                String createCollabMessageTable = "CREATE TABLE IF NOT EXISTS collaboration_message (" +
                        "id INT PRIMARY KEY AUTO_INCREMENT, " +
                        "investment_id INT, " +
                        "collaboration_id INT, " +
                        "sender_id VARCHAR(36) NOT NULL, " +
                        "message TEXT NOT NULL, " +
                        "type VARCHAR(50) DEFAULT 'GENERAL', " +
                        "created_at DATETIME DEFAULT CURRENT_TIMESTAMP)";

                try {
                    st.executeUpdate(createCollaborationTable);
                    System.out.println("Patched DB: Ensured collaboration table exists.");

                    // Migrate old individual-project schema: add missing columns if table already
                    // existed
                    // with the old schema (project_id, no investment_id/entrepreneur_id)
                    String[] migrationPatches = {
                            // Add investment_id if missing (old schema used project_id)
                            "ALTER TABLE collaboration ADD COLUMN investment_id INT NOT NULL DEFAULT 0",
                            // Add entrepreneur_id if missing (old schema didn't have it)
                            "ALTER TABLE collaboration ADD COLUMN entrepreneur_id VARCHAR(36) NOT NULL DEFAULT ''",
                            // Ensure investor_id is VARCHAR(36) for UUID support
                            "ALTER TABLE collaboration MODIFY COLUMN investor_id VARCHAR(36) NOT NULL",
                            // Auto-patch fairness columns if table already existed without them
                            "ALTER TABLE collaboration ADD COLUMN fairness_score DOUBLE DEFAULT 100",
                            "ALTER TABLE collaboration ADD COLUMN fairness_status VARCHAR(50) DEFAULT 'BALANCED'",
                            "ALTER TABLE collaboration ADD COLUMN ideal_equity DOUBLE DEFAULT 0",
                            "ALTER TABLE collaboration ADD COLUMN equity_deviation DOUBLE DEFAULT 0"
                    };
                    for (String patch : migrationPatches) {
                        try {
                            st.executeUpdate(patch);
                        } catch (Exception ignored) {
                            // Column already exists or type already correct — safe to ignore
                        }
                    }

                    // Migrate sender_id in collaboration_message from INT to VARCHAR(36) if needed
                    try {
                        st.executeUpdate(
                                "ALTER TABLE collaboration_message MODIFY COLUMN sender_id VARCHAR(36) NOT NULL");
                        System.out.println("Patched DB: Migrated collaboration_message.sender_id to VARCHAR(36).");
                    } catch (Exception ignored) {
                    }
                } catch (Exception e) {
                }

                try {
                    st.executeUpdate(createCollabMessageTable);
                    System.out.println("Patched DB: Ensured collaboration_message table exists.");
                } catch (Exception e) {
                }

            } catch (Exception e) {
                // Ignore silent db patch errors
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public Connection getCnx() {
        try {
            // Check if connection is closed or null, reconnect if needed
            if (cnx == null || cnx.isClosed()) {
                System.out.println("Reconnecting to database...");
                cnx = DriverManager.getConnection(url, login, pwd);
                System.out.println("Reconnection successful!");
            }
        } catch (SQLException e) {
            System.out.println("Error checking/reconnecting: " + e.getMessage());
        }
        return cnx;
    }

    public static MyConnection getInstance() {
        if (instance == null)
            instance = new MyConnection();
        return instance;
    }
}
