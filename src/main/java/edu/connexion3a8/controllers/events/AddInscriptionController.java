package edu.connexion3a8.controllers.events;

import edu.connexion3a8.entities.Inscription;
import edu.connexion3a8.services.InscriptionService;
import edu.connexion3a8.services.EvenementService;
import edu.connexion3a8.services.UserService;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class AddInscriptionController {

    private InscriptionService inscriptionService = new InscriptionService();
    private EvenementService evenementService = new EvenementService();
    private UserService userService = new UserService();
    private Inscription inscriptionAModifier = null;
    // Theme manager removed - not needed for integration
    
    private Map<String, String> userMap = new HashMap<>(); // Display name -> UUID
    private Map<String, Integer> eventMap = new HashMap<>(); // Display name -> ID

    @FXML private ComboBox<String> cbUtilisateur, cbEvenement, cbStatut;
    @FXML private Button btnAction, btnAnnuler;

    @FXML
    void initialize() {
        chargerUtilisateurs();
        chargerEvenements();
        cbStatut.setItems(FXCollections.observableArrayList(
                "confirme", "en_attente", "annule"));
        cbStatut.setValue("confirme");
        btnAction.setText("Ajouter");
    }

    private void chargerUtilisateurs() {
        try {
            var users = userService.getAllUsers();
            userMap.clear();
            List<String> displayNames = new java.util.ArrayList<>();
            for (var user : users) {
                String display = user.getName() + " (" + user.getEmail() + ")";
                userMap.put(display, user.getId());
                displayNames.add(display);
            }
            cbUtilisateur.setItems(FXCollections.observableArrayList(displayNames));
            if (!displayNames.isEmpty()) {
                cbUtilisateur.setValue(displayNames.get(0));
            }
        } catch (Exception e) {
            afficherErreur("Erreur", "Impossible de charger les utilisateurs: " + e.getMessage());
        }
    }

    private void chargerEvenements() {
        try {
            var events = evenementService.getData();
            eventMap.clear();
            List<String> displayNames = new java.util.ArrayList<>();
            for (var event : events) {
                String display = event.getIdEvenement() + " - " + event.getTitre();
                eventMap.put(display, event.getIdEvenement());
                displayNames.add(display);
            }
            cbEvenement.setItems(FXCollections.observableArrayList(displayNames));
            if (!displayNames.isEmpty()) {
                cbEvenement.setValue(displayNames.get(0));
            }
        } catch (Exception e) {
            afficherErreur("Erreur", "Impossible de charger les événements: " + e.getMessage());
        }
    }

    public void setInscriptionAModifier(Inscription inscription) {
        this.inscriptionAModifier = inscription;
        
        // Find and set user
        for (Map.Entry<String, String> entry : userMap.entrySet()) {
            if (entry.getValue().equals(inscription.getIdUser())) {
                cbUtilisateur.setValue(entry.getKey());
                break;
            }
        }
        
        // Find and set event
        for (Map.Entry<String, Integer> entry : eventMap.entrySet()) {
            if (entry.getValue() == inscription.getIdEvenement()) {
                cbEvenement.setValue(entry.getKey());
                break;
            }
        }
        
        cbStatut.setValue(inscription.getStatut());
        btnAction.setText("Modifier");
    }

    @FXML
    void ajouterInscription(ActionEvent event) {
        if (!validerChamps()) return;
        try {
            String userDisplay = cbUtilisateur.getValue();
            String eventDisplay = cbEvenement.getValue();
            String idUser = userMap.get(userDisplay);
            int idEvent = eventMap.get(eventDisplay);
            String statut = cbStatut.getValue();

            if (inscriptionAModifier == null) {
                if (dejaInscrit(idUser, idEvent)) {
                    afficherErreur("Doublon", "Cet utilisateur est déjà inscrit à cet événement !"); 
                    return;
                }
                inscriptionService.addEntity(new Inscription(idUser, idEvent, statut));
                afficherSucces("Succès", "Inscription ajoutée !");
            } else {
                inscriptionAModifier.setIdUser(idUser);
                inscriptionAModifier.setIdEvenement(idEvent);
                inscriptionAModifier.setStatut(statut);
                inscriptionService.updateEntity(inscriptionAModifier);
                afficherSucces("Succès", "Inscription modifiée !");
            }
            retournerALaListe();
        } catch (Exception e) {
            afficherErreur("Erreur", e.getMessage());
        }
    }

    private boolean dejaInscrit(String idUser, int idEvent) {
        try {
            List<Inscription> list = inscriptionService.getData();
            if (inscriptionAModifier != null) {
                return list.stream().anyMatch(i ->
                        i.getIdUser().equals(idUser) && i.getIdEvenement() == idEvent &&
                                i.getIdInscription() != inscriptionAModifier.getIdInscription());
            }
            return list.stream().anyMatch(i ->
                    i.getIdUser().equals(idUser) && i.getIdEvenement() == idEvent);
        } catch (SQLException e) { return false; }
    }

    @FXML
    void annuler(ActionEvent event) { retournerALaListe(); }

    private void retournerALaListe() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/Main.fxml"));
            cbUtilisateur.getScene().setRoot(root);
        } catch (Exception e) {
            afficherErreur("Erreur", "Impossible de revenir : " + e.getMessage());
        }
    }

    private boolean validerChamps() {
        if (cbUtilisateur.getValue() == null || cbUtilisateur.getValue().isEmpty()) {
            afficherErreur("Champ vide", "Sélectionnez un utilisateur !"); return false; }
        if (cbEvenement.getValue() == null || cbEvenement.getValue().isEmpty()) {
            afficherErreur("Champ vide", "Sélectionnez un événement !"); return false; }
        if (cbStatut.getValue() == null || cbStatut.getValue().isEmpty()) {
            afficherErreur("Champ vide", "Sélectionnez un statut !"); return false; }
        return true;
    }

    private void afficherErreur(String titre, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(titre); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    private void afficherSucces(String titre, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(titre); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
}
