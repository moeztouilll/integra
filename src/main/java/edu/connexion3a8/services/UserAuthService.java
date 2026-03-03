package edu.connexion3a8.services;

import edu.connexion3a8.entities.User;
import edu.connexion3a8.tools.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Service for user authentication and authorization checks
 */
public class UserAuthService {

    /**
     * Check if a user is verified (active and email verified)
     * @param userId The user ID to check
     * @return true if user is verified, false otherwise
     */
    public static boolean isUserVerified(String userId) throws SQLException {
        String query = "SELECT is_active, email_verified FROM users WHERE id = ?";
        
        try (Connection conn = MyConnection.getInstance().getCnx();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, userId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                boolean isActive = rs.getBoolean("is_active");
                boolean emailVerified = rs.getBoolean("email_verified");
                return isActive && emailVerified;
            }
            
            return false;
        }
    }

    /**
     * Check if a user can access the forum (investor or innovator role)
     * @param userId The user ID to check
     * @return true if user can access forum, false otherwise
     */
    public static boolean canAccessForum(String userId) throws SQLException {
        String query = "SELECT role FROM users WHERE id = ?";
        
        try (Connection conn = MyConnection.getInstance().getCnx();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, userId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                String role = rs.getString("role");
                return "investor".equalsIgnoreCase(role) || 
                       "innovator".equalsIgnoreCase(role) ||
                       "admin".equalsIgnoreCase(role);
            }
            
            return false;
        }
    }

    /**
     * Check if a user can perform write operations (post, comment, vote)
     * User must be verified AND have appropriate role
     * @param userId The user ID to check
     * @return true if user can perform write operations, false otherwise
     */
    public static boolean canPerformWriteOperations(String userId) throws SQLException {
        String query = "SELECT role, is_active, email_verified FROM users WHERE id = ?";
        
        try (Connection conn = MyConnection.getInstance().getCnx();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, userId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                String role = rs.getString("role");
                boolean isActive = rs.getBoolean("is_active");
                boolean emailVerified = rs.getBoolean("email_verified");
                
                boolean hasValidRole = "investor".equalsIgnoreCase(role) || 
                                      "innovator".equalsIgnoreCase(role) ||
                                      "admin".equalsIgnoreCase(role);
                
                return hasValidRole && isActive && emailVerified;
            }
            
            return false;
        }
    }

    /**
     * Get user verification status with detailed information
     * @param userId The user ID to check
     * @return UserVerificationStatus object with detailed status
     */
    public static UserVerificationStatus getUserVerificationStatus(String userId) throws SQLException {
        String query = "SELECT role, is_active, email_verified, name FROM users WHERE id = ?";
        
        try (Connection conn = MyConnection.getInstance().getCnx();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, userId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                String role = rs.getString("role");
                boolean isActive = rs.getBoolean("is_active");
                boolean emailVerified = rs.getBoolean("email_verified");
                String name = rs.getString("name");
                
                return new UserVerificationStatus(userId, name, role, isActive, emailVerified);
            }
            
            return null;
        }
    }

    /**
     * Inner class to hold user verification status details
     */
    public static class UserVerificationStatus {
        private final String userId;
        private final String name;
        private final String role;
        private final boolean isActive;
        private final boolean emailVerified;

        public UserVerificationStatus(String userId, String name, String role, boolean isActive, boolean emailVerified) {
            this.userId = userId;
            this.name = name;
            this.role = role;
            this.isActive = isActive;
            this.emailVerified = emailVerified;
        }

        public String getUserId() {
            return userId;
        }

        public String getName() {
            return name;
        }

        public String getRole() {
            return role;
        }

        public boolean isActive() {
            return isActive;
        }

        public boolean isEmailVerified() {
            return emailVerified;
        }

        public boolean isVerified() {
            return isActive && emailVerified;
        }

        public boolean canAccessForum() {
            return "investor".equalsIgnoreCase(role) || 
                   "innovator".equalsIgnoreCase(role) ||
                   "admin".equalsIgnoreCase(role);
        }

        public boolean canPerformWriteOperations() {
            return canAccessForum() && isVerified();
        }

        public String getStatusMessage() {
            if (!canAccessForum()) {
                return "Your account role (" + role + ") does not have access to the forum.";
            }
            if (!isActive) {
                return "Your account is not active. Please contact support.";
            }
            if (!emailVerified) {
                return "Please verify your email address to post, comment, or vote.";
            }
            return "Verified";
        }
    }
}
