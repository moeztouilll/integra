package edu.connexion3a8.services;

import edu.connexion3a8.entities.Evenement;
import edu.connexion3a8.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EvenementService {
    private Connection connection;

    public EvenementService() {
        this.connection = MyConnection.getInstance().getCnx();
    }

    public void addEntity(Evenement evenement) throws SQLException {
        String query = "INSERT INTO evenement (id_mentor, titre, contenu, lieu, lieu_latitude, lieu_longitude, image_url, date_debut, date_fin) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, evenement.getIdMentor());
            pst.setString(2, evenement.getTitre());
            pst.setString(3, evenement.getContenu());
            pst.setString(4, evenement.getLieu());
            
            if (evenement.getLieuLatitude() != null) {
                pst.setDouble(5, evenement.getLieuLatitude());
            } else {
                pst.setNull(5, Types.DOUBLE);
            }
            
            if (evenement.getLieuLongitude() != null) {
                pst.setDouble(6, evenement.getLieuLongitude());
            } else {
                pst.setNull(6, Types.DOUBLE);
            }
            
            pst.setString(7, evenement.getImageUrl());
            pst.setTimestamp(8, Timestamp.valueOf(evenement.getDateDebut()));
            pst.setTimestamp(9, Timestamp.valueOf(evenement.getDateFin()));

            pst.executeUpdate();
            System.out.println("Event added successfully!");
        }
    }

    public void deleteEntity(int id) throws SQLException {
        String query = "DELETE FROM evenement WHERE id_evenement = ?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, id);
            int rowsAffected = pst.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Event deleted successfully!");
            } else {
                System.out.println("Event not found!");
            }
        }
    }

    public void updateEntity(Evenement evenement) throws SQLException {
        String query = "UPDATE evenement SET id_mentor=?, titre=?, contenu=?, lieu=?, " +
                "lieu_latitude=?, lieu_longitude=?, image_url=?, date_debut=?, date_fin=? WHERE id_evenement=?";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, evenement.getIdMentor());
            pst.setString(2, evenement.getTitre());
            pst.setString(3, evenement.getContenu());
            pst.setString(4, evenement.getLieu());
            
            if (evenement.getLieuLatitude() != null) {
                pst.setDouble(5, evenement.getLieuLatitude());
            } else {
                pst.setNull(5, Types.DOUBLE);
            }
            
            if (evenement.getLieuLongitude() != null) {
                pst.setDouble(6, evenement.getLieuLongitude());
            } else {
                pst.setNull(6, Types.DOUBLE);
            }
            
            pst.setString(7, evenement.getImageUrl());
            pst.setTimestamp(8, Timestamp.valueOf(evenement.getDateDebut()));
            pst.setTimestamp(9, Timestamp.valueOf(evenement.getDateFin()));
            pst.setInt(10, evenement.getIdEvenement());

            int rowsAffected = pst.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Event updated successfully!");
            } else {
                System.out.println("Event not found!");
            }
        }
    }

    public List<Evenement> getData() throws SQLException {
        List<Evenement> evenements = new ArrayList<>();
        String query = "SELECT e.*, u.name as mentor_name FROM evenement e " +
                "LEFT JOIN users u ON e.id_mentor = u.id " +
                "ORDER BY e.date_debut DESC";

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            
            while (rs.next()) {
                evenements.add(extractEvenementFromResultSet(rs));
            }
        }
        return evenements;
    }

    public Evenement getById(int id) throws SQLException {
        String query = "SELECT e.*, u.name as mentor_name FROM evenement e " +
                "LEFT JOIN users u ON e.id_mentor = u.id " +
                "WHERE e.id_evenement = ?";
        
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, id);
            ResultSet rs = pst.executeQuery();
            
            if (rs.next()) {
                return extractEvenementFromResultSet(rs);
            }
        }
        return null;
    }

    public boolean exists(int id) throws SQLException {
        String query = "SELECT 1 FROM evenement WHERE id_evenement = ?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, id);
            ResultSet rs = pst.executeQuery();
            return rs.next();
        }
    }

    public List<Evenement> getEvenementsByMentor(String idMentor) throws SQLException {
        List<Evenement> evenements = new ArrayList<>();
        String query = "SELECT e.*, u.name as mentor_name FROM evenement e " +
                "LEFT JOIN users u ON e.id_mentor = u.id " +
                "WHERE e.id_mentor = ? ORDER BY e.date_debut DESC";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, idMentor);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                evenements.add(extractEvenementFromResultSet(rs));
            }
        }
        return evenements;
    }

    public List<Evenement> getUpcomingEvents() throws SQLException {
        List<Evenement> evenements = new ArrayList<>();
        String query = "SELECT e.*, u.name as mentor_name FROM evenement e " +
                "LEFT JOIN users u ON e.id_mentor = u.id " +
                "WHERE e.date_debut > NOW() ORDER BY e.date_debut ASC";

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            
            while (rs.next()) {
                evenements.add(extractEvenementFromResultSet(rs));
            }
        }
        return evenements;
    }

    private Evenement extractEvenementFromResultSet(ResultSet rs) throws SQLException {
        Evenement e = new Evenement();
        e.setIdEvenement(rs.getInt("id_evenement"));
        e.setIdMentor(rs.getString("id_mentor"));
        e.setMentorName(rs.getString("mentor_name"));
        e.setTitre(rs.getString("titre"));
        e.setContenu(rs.getString("contenu"));
        e.setLieu(rs.getString("lieu"));
        
        Double lat = rs.getObject("lieu_latitude", Double.class);
        Double lon = rs.getObject("lieu_longitude", Double.class);
        e.setLieuLatitude(lat);
        e.setLieuLongitude(lon);
        e.setImageUrl(rs.getString("image_url"));
        
        e.setDateDebut(rs.getTimestamp("date_debut").toLocalDateTime());
        e.setDateFin(rs.getTimestamp("date_fin").toLocalDateTime());
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            e.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        return e;
    }
}
