package edu.connexion3a8.entities;

import java.time.LocalDateTime;

public class Inscription {
    private int idInscription;
    private String idUser; // UUID from users table
    private String userName; // For display
    private int idEvenement;
    private String eventTitle; // For display
    private LocalDateTime dateInscription;
    private String statut; // confirme, en_attente, annule

    public Inscription() {
    }

    public Inscription(String idUser, int idEvenement, String statut) {
        this.idUser = idUser;
        this.idEvenement = idEvenement;
        this.statut = statut;
    }

    // Getters and Setters
    public int getIdInscription() {
        return idInscription;
    }

    public void setIdInscription(int idInscription) {
        this.idInscription = idInscription;
    }

    public String getIdUser() {
        return idUser;
    }

    public void setIdUser(String idUser) {
        this.idUser = idUser;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public int getIdEvenement() {
        return idEvenement;
    }

    public void setIdEvenement(int idEvenement) {
        this.idEvenement = idEvenement;
    }

    public String getEventTitle() {
        return eventTitle;
    }

    public void setEventTitle(String eventTitle) {
        this.eventTitle = eventTitle;
    }

    public LocalDateTime getDateInscription() {
        return dateInscription;
    }

    public void setDateInscription(LocalDateTime dateInscription) {
        this.dateInscription = dateInscription;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    @Override
    public String toString() {
        return "Inscription{" +
                "idInscription=" + idInscription +
                ", userName='" + userName + '\'' +
                ", eventTitle='" + eventTitle + '\'' +
                ", statut='" + statut + '\'' +
                ", dateInscription=" + dateInscription +
                '}';
    }
}
