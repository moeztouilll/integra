package edu.connexion3a8.services;

import edu.connexion3a8.entities.User;
import edu.connexion3a8.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserService {
    public UserService() {
    }

    private Connection getConnection() {
        return MyConnection.getInstance().getCnx();
    }

    public void addUser(User user) throws SQLException {
        String query = "INSERT INTO users (email, password_hash, name, role, avatar_url, bio, points, level, is_active, email_verified) "
                +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pst = getConnection().prepareStatement(query)) {
            pst.setString(1, user.getEmail());
            pst.setString(2, user.getPasswordHash());
            pst.setString(3, user.getName());
            pst.setString(4, user.getRole());
            pst.setString(5, user.getAvatarUrl());
            pst.setString(6, user.getBio());
            pst.setInt(7, user.getPoints());
            pst.setInt(8, user.getLevel());
            pst.setBoolean(9, user.isActive());
            pst.setBoolean(10, user.isEmailVerified());

            pst.executeUpdate();
            System.out.println("User added successfully!");
        }
    }

    public void updateUser(String id, User user) throws SQLException {
        String query = "UPDATE users SET email=?, name=?, role=?, avatar_url=?, bio=?, points=?, level=?, is_active=?, email_verified=? "
                +
                "WHERE id=?";

        try (PreparedStatement pst = getConnection().prepareStatement(query)) {
            pst.setString(1, user.getEmail());
            pst.setString(2, user.getName());
            pst.setString(3, user.getRole());
            pst.setString(4, user.getAvatarUrl());
            pst.setString(5, user.getBio());
            pst.setInt(6, user.getPoints());
            pst.setInt(7, user.getLevel());
            pst.setBoolean(8, user.isActive());
            pst.setBoolean(9, user.isEmailVerified());
            pst.setString(10, id);

            int rowsAffected = pst.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("User updated successfully!");
            } else {
                System.out.println("User not found!");
            }
        }
    }

    public void deleteUser(String id) throws SQLException {
        String query = "DELETE FROM users WHERE id=?";

        try (PreparedStatement pst = getConnection().prepareStatement(query)) {
            pst.setString(1, id);
            int rowsAffected = pst.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("User deleted successfully!");
            } else {
                System.out.println("User not found!");
            }
        }
    }

    public User getUserById(String id) throws SQLException {
        String query = "SELECT * FROM users WHERE id=?";

        try (PreparedStatement pst = getConnection().prepareStatement(query)) {
            pst.setString(1, id);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                return extractUserFromResultSet(rs);
            }
        }
        return null;
    }

    public User getUserByEmail(String email) throws SQLException {
        String query = "SELECT * FROM users WHERE email=?";

        try (PreparedStatement pst = getConnection().prepareStatement(query)) {
            pst.setString(1, email);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                return extractUserFromResultSet(rs);
            }
        }
        return null;
    }

    public List<User> getAllUsers() throws SQLException {
        List<User> users = new ArrayList<>();
        String query = "SELECT * FROM users ORDER BY created_at DESC";

        try (Statement st = getConnection().createStatement();
                ResultSet rs = st.executeQuery(query)) {

            while (rs.next()) {
                users.add(extractUserFromResultSet(rs));
            }
        }
        return users;
    }

    public List<User> getUsersByRole(String role) throws SQLException {
        List<User> users = new ArrayList<>();
        String query = "SELECT * FROM users WHERE role=? ORDER BY created_at DESC";

        try (PreparedStatement pst = getConnection().prepareStatement(query)) {
            pst.setString(1, role);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                users.add(extractUserFromResultSet(rs));
            }
        }
        return users;
    }

    private User extractUserFromResultSet(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getString("id"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setName(rs.getString("name"));
        user.setRole(rs.getString("role"));
        user.setAvatarUrl(rs.getString("avatar_url"));
        user.setBio(rs.getString("bio"));
        user.setPoints(rs.getInt("points"));
        user.setLevel(rs.getInt("level"));
        user.setCreatedAt(rs.getTimestamp("created_at"));
        user.setUpdatedAt(rs.getTimestamp("updated_at"));
        user.setLastLogin(rs.getTimestamp("last_login"));
        user.setActive(rs.getBoolean("is_active"));
        user.setEmailVerified(rs.getBoolean("email_verified"));
        user.setIdImageUrl(rs.getString("id_image_url"));
        return user;
    }

    public void updateIdImageUrl(String userId, String imageUrl) throws SQLException {
        String query = "UPDATE users SET id_image_url=? WHERE id=?";
        try (PreparedStatement pst = getConnection().prepareStatement(query)) {
            pst.setString(1, imageUrl);
            pst.setString(2, userId);
            pst.executeUpdate();
        }
    }

    public void setUserActive(String userId, boolean active) throws SQLException {
        String query = "UPDATE users SET is_active=? WHERE id=?";
        try (PreparedStatement pst = getConnection().prepareStatement(query)) {
            pst.setBoolean(1, active);
            pst.setString(2, userId);
            pst.executeUpdate();
        }
    }

    public List<User> getPendingKycUsers() throws SQLException {
        List<User> users = new ArrayList<>();
        String query = "SELECT * FROM users WHERE is_active = FALSE AND id_image_url IS NOT NULL AND role != 'admin' ORDER BY created_at DESC";
        try (Statement st = getConnection().createStatement();
                ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                users.add(extractUserFromResultSet(rs));
            }
        }
        return users;
    }
}
