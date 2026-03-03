package edu.connexion3a8.entities;

import java.time.LocalDateTime;

public class Evenement {
    private int idEvenement;
    private String idMentor; // UUID from users table
    private String mentorName; // For display
    private String titre;
    private String contenu;
    private String lieu;
    private Double lieuLatitude;
    private Double lieuLongitude;
    private String imageUrl;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private LocalDateTime createdAt;

    public Evenement() {
    }

    public Evenement(String idMentor, String titre, String contenu, String lieu,
            LocalDateTime dateDebut, LocalDateTime dateFin) {
        this.idMentor = idMentor;
        this.titre = titre;
        this.contenu = contenu;
        this.lieu = lieu;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
    }

    public Evenement(String idMentor, String titre, String contenu, String lieu,
            Double lieuLatitude, Double lieuLongitude, String imageUrl,
            LocalDateTime dateDebut, LocalDateTime dateFin) {
        this.idMentor = idMentor;
        this.titre = titre;
        this.contenu = contenu;
        this.lieu = lieu;
        this.lieuLatitude = lieuLatitude;
        this.lieuLongitude = lieuLongitude;
        this.imageUrl = imageUrl;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
    }

    // Getters and Setters
    public int getIdEvenement() {
        return idEvenement;
    }

    public void setIdEvenement(int idEvenement) {
        this.idEvenement = idEvenement;
    }

    public String getIdMentor() {
        return idMentor;
    }

    public void setIdMentor(String idMentor) {
        this.idMentor = idMentor;
    }

    public String getMentorName() {
        return mentorName;
    }

    public void setMentorName(String mentorName) {
        this.mentorName = mentorName;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public String getLieu() {
        return lieu;
    }

    public void setLieu(String lieu) {
        this.lieu = lieu;
    }

    public LocalDateTime getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDateTime dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDateTime getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDateTime dateFin) {
        this.dateFin = dateFin;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Double getLieuLatitude() {
        return lieuLatitude;
    }

    public void setLieuLatitude(Double lieuLatitude) {
        this.lieuLatitude = lieuLatitude;
    }

    public Double getLieuLongitude() {
        return lieuLongitude;
    }

    public void setLieuLongitude(Double lieuLongitude) {
        this.lieuLongitude = lieuLongitude;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Evenement{" +
                "idEvenement=" + idEvenement +
                ", mentorName='" + mentorName + '\'' +
                ", titre='" + titre + '\'' +
                ", lieu='" + lieu + '\'' +
                ", dateDebut=" + dateDebut +
                ", dateFin=" + dateFin +
                '}';
    }
}
