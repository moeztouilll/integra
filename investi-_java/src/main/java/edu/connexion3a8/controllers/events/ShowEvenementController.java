package edu.connexion3a8.controllers.events;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import edu.connexion3a8.entities.Evenement;
import edu.connexion3a8.services.EvenementService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.util.Callback;

public class ShowEvenementController {
    private final EvenementService  es = new EvenementService();
    private ObservableList<Evenement> observableList ;
    // Theme manager removed - not needed for integration

    @FXML private ResourceBundle resources;
    @FXML private URL location;
    @FXML private TableColumn<Evenement, Void> actCol;
    @FXML private TableColumn<Evenement, String> contenuCol;
    @FXML private TableColumn<Evenement, LocalDateTime> debCol;
    @FXML private TableColumn<Evenement, LocalDateTime> finCol;
    @FXML private TableColumn<Evenement, Integer> id_mentorCol;
    @FXML private TableColumn<Evenement, String> lieuCol;
    @FXML private TableView<Evenement> tabEvent;
    @FXML private TableColumn<Evenement, String> titreCol;

    @FXML
    void initialize() {
        id_mentorCol.setCellValueFactory(new PropertyValueFactory<>("idMentor"));
        titreCol.setCellValueFactory(new PropertyValueFactory<>("titre"));
        contenuCol.setCellValueFactory(new PropertyValueFactory<>("contenu"));
        lieuCol.setCellValueFactory(new PropertyValueFactory<>("lieu"));
        debCol.setCellValueFactory(new PropertyValueFactory<>("dateDebut"));
        finCol.setCellValueFactory(new PropertyValueFactory<>("dateFin"));

        ajouterBoutonsActions();
        refreshData();
        
        // Apply current theme when scene is ready
        // Theme functionality removed for integration
    }

    private void ajouterBoutonsActions() {
        Callback<TableColumn<Evenement, Void>, TableCell<Evenement, Void>> cellFactory =
                new Callback<>() {
                    @Override
                    public TableCell<Evenement, Void> call(final TableColumn<Evenement, Void> param) {
                        final TableCell<Evenement, Void> cell = new TableCell<>() {

                            private final Button btnEdit = new Button("✏️Modifier");
                            private final Button btnDelete = new Button("🗑️Supprimer");

                            {
                                btnEdit.setStyle("-fx-background-color: #F97316; -fx-text-fill: white; " +
                                        "-fx-cursor: hand; -fx-font-weight: bold;");
                                btnDelete.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; " +
                                        "-fx-cursor: hand; -fx-font-weight: bold;");

                                btnEdit.setOnAction((ActionEvent event) -> {
                                    Evenement evenement = getTableView().getItems().get(getIndex());
                                    modifierEvenement(evenement);
                                });

                                btnDelete.setOnAction((ActionEvent event) -> {
                                    Evenement evenement = getTableView().getItems().get(getIndex());
                                    supprimerEvenement(evenement);
                                });
                            }

                            @Override
                            public void updateItem(Void item, boolean empty) {
                                super.updateItem(item, empty);
                                if (empty) {
                                    setGraphic(null);
                                } else {
                                    HBox hbox = new HBox(10);  // Espacement de 10px
                                    hbox.setAlignment(Pos.CENTER);
                                    hbox.getChildren().addAll(btnEdit, btnDelete);
                                    setGraphic(hbox);
                                }
                            }
                        };
                        return cell;
                    }
        };
        actCol.setCellFactory(cellFactory);
    }

    public void refreshData(){
        try {
            List<Evenement> evenements = es.getData();
            observableList = FXCollections.observableList(evenements);
            tabEvent.setItems(observableList);
        } catch (Exception e) {
            afficherErreur("Erreur", "Erreur de chargement des données", e.getMessage());
        }

    }

    @FXML
    void navAjout(ActionEvent event) {
        try {
            Parent root = new FXMLLoader(getClass().getResource("/AddEvenement.fxml")).load();
            tabEvent.getScene().setRoot(root);
        } catch (Exception e) {
            afficherErreur("Erreur", "Erreur de navigation !", e.getMessage());
        }
    }

    private void modifierEvenement(Evenement evenement) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AddEvenement.fxml"));
            Parent root = loader.load();
            AddEvenementController controller = loader.getController();
            controller.setEvenementAModifier(evenement);
            tabEvent.getScene().setRoot(root);

        } catch (Exception e) {
            afficherErreur("Erreur", "Erreur lors de la modification", e.getMessage());
        }
    }

    private void supprimerEvenement(Evenement evenement) {

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation de suppression");
        confirmation.setHeaderText("Êtes-vous sûr de vouloir supprimer cet événement ?");
        confirmation.setContentText("Titre : " + evenement.getTitre() + "\nLieu : " + evenement.getLieu());

        Optional<ButtonType> result = confirmation.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                es.deleteEntity(evenement.getIdEvenement());
                refreshData();
                afficherSucces("Succès", "Événement supprimé avec succès !");

            } catch (Exception e) {
                afficherErreur("Erreur", "Impossible de supprimer l'événement", e.getMessage());
            }
        } else {
            System.out.println("Suppression annulée");
        }
    }

    private void afficherErreur(String titre, String header, String contenu) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titre);
        alert.setHeaderText(header);
        alert.setContentText(contenu);
        alert.showAndWait();
    }

    private void afficherSucces(String titre, String contenu) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(contenu);
        alert.showAndWait();
    }

}

        /*assert contenuCol != null : "fx:id=\"contenuCol\" was not injected: check your FXML file 'ShowEvenement.fxml'.";
        assert debCol != null : "fx:id=\"debCol\" was not injected: check your FXML file 'ShowEvenement.fxml'.";
        assert finCol != null : "fx:id=\"finCol\" was not injected: check your FXML file 'ShowEvenement.fxml'.";
        assert id_eventCol != null : "fx:id=\"id_eventCol\" was not injected: check your FXML file 'ShowEvenement.fxml'.";
        assert id_mentorCol != null : "fx:id=\"id_mentorCol\" was not injected: check your FXML file 'ShowEvenement.fxml'.";
        assert lieuCol != null : "fx:id=\"lieuCol\" was not injected: check your FXML file 'ShowEvenement.fxml'.";
        assert tabEvent != null : "fx:id=\"tabEvent\" was not injected: check your FXML file 'ShowEvenement.fxml'.";
        assert titreCol != null : "fx:id=\"titreCol\" was not injected: check your FXML file 'ShowEvenement.fxml'.";*/
