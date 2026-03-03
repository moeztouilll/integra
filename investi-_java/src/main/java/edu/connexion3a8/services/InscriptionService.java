package edu.connexion3a8.services;

import edu.connexion3a8.entities.Inscription;
import edu.connexion3a8.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InscriptionService {
    private Connection connection;

    public InscriptionService() {
        this.connection = MyConnection.getInstance().getCnx();
    }

    public void addEntity(Inscription inscription) throws SQLException {
        String query = "INSERT INTO inscription (id_user, id_evenement, statut) VALUES (?, ?, ?)";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, inscription.getIdUser());
            pst.setInt(2, inscription.getIdEvenement());
            pst.setString(3, inscription.getStatut());
            pst.executeUpdate();
            System.out.println("Registration added successfully!");
        }
    }

    public void deleteEntity(int id) throws SQLException {
        String query = "DELETE FROM inscription WHERE id_inscription = ?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, id);
            int rowsAffected = pst.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Registration deleted successfully!");
            } else {
                System.out.println("Registration not found!");
            }
        }
    }

    public void updateEntity(Inscription inscription) throws SQLException {
        String query = "UPDATE inscription SET statut=? WHERE id_inscription=?";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, inscription.getStatut());
            pst.setInt(2, inscription.getIdInscription());
            
            int rowsAffected = pst.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Registration updated successfully!");
            } else {
                System.out.println("Registration not found!");
            }
        }
    }

    public List<Inscription> getData() throws SQLException {
        List<Inscription> inscriptions = new ArrayList<>();
        String query = "SELECT i.*, u.name as user_name, e.titre as event_title " +
                "FROM inscription i " +
                "LEFT JOIN users u ON i.id_user = u.id " +
                "LEFT JOIN evenement e ON i.id_evenement = e.id_evenement " +
                "ORDER BY i.date_inscription DESC";

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            
            while (rs.next()) {
                inscriptions.add(extractInscriptionFromResultSet(rs));
            }
        }
        return inscriptions;
    }

    public Inscription getById(int id) throws SQLException {
        String query = "SELECT i.*, u.name as user_name, e.titre as event_title " +
                "FROM inscription i " +
                "LEFT JOIN users u ON i.id_user = u.id " +
                "LEFT JOIN evenement e ON i.id_evenement = e.id_evenement " +
                "WHERE i.id_inscription = ?";
        
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, id);
            ResultSet rs = pst.executeQuery();
            
            if (rs.next()) {
                return extractInscriptionFromResultSet(rs);
            }
        }
        return null;
    }

    public boolean exists(int id) throws SQLException {
        String query = "SELECT 1 FROM inscription WHERE id_inscription = ?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, id);
            ResultSet rs = pst.executeQuery();
            return rs.next();
        }
    }

    public List<Inscription> getInscriptionsByEvenement(int idEvenement) throws SQLException {
        List<Inscription> inscriptions = new ArrayList<>();
        String query = "SELECT i.*, u.name as user_name, e.titre as event_title " +
                "FROM inscription i " +
                "LEFT JOIN users u ON i.id_user = u.id " +
                "LEFT JOIN evenement e ON i.id_evenement = e.id_evenement " +
                "WHERE i.id_evenement = ? ORDER BY i.date_inscription DESC";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, idEvenement);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                inscriptions.add(extractInscriptionFromResultSet(rs));
            }
        }
        return inscriptions;
    }

    public List<Inscription> getInscriptionsByUser(String idUser) throws SQLException {
        List<Inscription> inscriptions = new ArrayList<>();
        String query = "SELECT i.*, u.name as user_name, e.titre as event_title " +
                "FROM inscription i " +
                "LEFT JOIN users u ON i.id_user = u.id " +
                "LEFT JOIN evenement e ON i.id_evenement = e.id_evenement " +
                "WHERE i.id_user = ? ORDER BY i.date_inscription DESC";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, idUser);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                inscriptions.add(extractInscriptionFromResultSet(rs));
            }
        }
        return inscriptions;
    }

    public boolean isUserRegistered(String idUser, int idEvenement) throws SQLException {
        String query = "SELECT 1 FROM inscription WHERE id_user = ? AND id_evenement = ?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, idUser);
            pst.setInt(2, idEvenement);
            ResultSet rs = pst.executeQuery();
            return rs.next();
        }
    }

    public boolean canUserRegister(String idUser) throws SQLException {
        // Check if user is verified (is_active = true)
        String query = "SELECT is_active FROM users WHERE id = ?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, idUser);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return rs.getBoolean("is_active");
            }
        }
        return false;
    }

    private Inscription extractInscriptionFromResultSet(ResultSet rs) throws SQLException {
        Inscription i = new Inscription();
        i.setIdInscription(rs.getInt("id_inscription"));
        i.setIdUser(rs.getString("id_user"));
        i.setUserName(rs.getString("user_name"));
        i.setIdEvenement(rs.getInt("id_evenement"));
        i.setEventTitle(rs.getString("event_title"));
        i.setDateInscription(rs.getTimestamp("date_inscription").toLocalDateTime());
        i.setStatut(rs.getString("statut"));
        return i;
    }
}
