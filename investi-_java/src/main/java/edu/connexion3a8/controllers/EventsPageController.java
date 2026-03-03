package edu.connexion3a8.controllers;

import edu.connexion3a8.InvestiApp;
import edu.connexion3a8.entities.Evenement;
import edu.connexion3a8.entities.User;
import edu.connexion3a8.services.EvenementService;
import edu.connexion3a8.services.InscriptionService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class EventsPageController implements Initializable {

    @FXML private VBox eventsContainer;
    @FXML private Label userStatusLabel;
    @FXML private TextField searchField;

    private User currentUser;
    private EvenementService evenementService = new EvenementService();
    private InscriptionService inscriptionService = new InscriptionService();
    private ObservableList<Evenement> allEvents = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadEvents();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        System.out.println("DEBUG: setCurrentUser called");
        System.out.println("DEBUG: User = " + (user != null ? user.getName() : "NULL"));
        System.out.println("DEBUG: User isActive = " + (user != null ? user.isActive() : "N/A"));
        updateUserStatus();
        loadEvents(); // Reload events to update button states
    }

    private void updateUserStatus() {
        if (currentUser != null && !currentUser.isActive()) {
            userStatusLabel.setText("⚠ Complete KYC verification to register for events");
            userStatusLabel.setStyle("-fx-text-fill: #9B7E46; -fx-font-weight: bold;");
        } else {
            userStatusLabel.setText("");
        }
    }

    private void loadEvents() {
        try {
            List<Evenement> events = evenementService.getData();
            allEvents.setAll(events);
            displayEvents(events);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void displayEvents(List<Evenement> events) {
        eventsContainer.getChildren().clear();

        if (events.isEmpty()) {
            Label emptyLabel = new Label("No events available");
            emptyLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #666;");
            eventsContainer.getChildren().add(emptyLabel);
            return;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

        for (Evenement event : events) {
            VBox eventCard = new VBox(15);
            eventCard.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 20; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");

            HBox headerBox = new HBox(15);
            headerBox.setAlignment(Pos.CENTER_LEFT);

            if (event.getImageUrl() != null && !event.getImageUrl().isEmpty()) {
                try {
                    File imgFile = new File(event.getImageUrl());
                    if (imgFile.exists()) {
                        ImageView imageView = new ImageView(new Image(imgFile.toURI().toString()));
                        imageView.setFitWidth(120);
                        imageView.setFitHeight(80);
                        imageView.setPreserveRatio(true);
                        imageView.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 5, 0, 0, 1);");
                        headerBox.getChildren().add(imageView);
                    }
                } catch (Exception e) {
                    // Skip image if error
                }
            }

            VBox infoBox = new VBox(8);
            Label titleLabel = new Label(event.getTitre());
            titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #000501;");

            Label mentorLabel = new Label("👤 Mentor: " + (event.getMentorName() != null ? event.getMentorName() : "Unknown"));
            mentorLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #456990;");

            Label locationLabel = new Label("📍 " + event.getLieu());
            locationLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #666;");

            Label dateLabel = new Label("📅 " + event.getDateDebut().format(formatter) + " - " + event.getDateFin().format(formatter));
            dateLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #666;");

            infoBox.getChildren().addAll(titleLabel, mentorLabel, locationLabel, dateLabel);
            headerBox.getChildren().add(infoBox);

            Label contentLabel = new Label(event.getContenu());
            contentLabel.setWrapText(true);
            contentLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #333;");

            Button registerBtn = new Button("Register");
            registerBtn.setStyle("-fx-background-color: #456990; -fx-text-fill: white; -fx-font-size: 14px; " +
                    "-fx-padding: 10 30; -fx-background-radius: 8; -fx-cursor: hand;");

            System.out.println("DEBUG: Creating button for event: " + event.getTitre());
            System.out.println("DEBUG: currentUser = " + (currentUser != null ? currentUser.getName() : "NULL"));
            System.out.println("DEBUG: currentUser.isActive() = " + (currentUser != null ? currentUser.isActive() : "N/A"));

            if (currentUser != null && currentUser.isActive()) {
                System.out.println("DEBUG: User is active, checking registration status");
                try {
                    if (inscriptionService.isUserRegistered(currentUser.getId(), event.getIdEvenement())) {
                        System.out.println("DEBUG: User already registered");
                        registerBtn.setText("✓ Registered");
                        registerBtn.setDisable(true);
                        registerBtn.setStyle("-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-size: 14px; " +
                                "-fx-padding: 10 30; -fx-background-radius: 8;");
                    } else {
                        System.out.println("DEBUG: User not registered, enabling button");
                        registerBtn.setDisable(false); // Explicitly enable the button
                        registerBtn.setOnAction(e -> registerForEvent(event));
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("DEBUG: User is NOT active or NULL, showing KYC Required");
                registerBtn.setText("KYC Required");
                registerBtn.setDisable(true);
                registerBtn.setStyle("-fx-background-color: #9B7E46; -fx-text-fill: white; -fx-font-size: 14px; " +
                        "-fx-padding: 10 30; -fx-background-radius: 8;");
            }

            eventCard.getChildren().addAll(headerBox, contentLabel, registerBtn);
            eventsContainer.getChildren().add(eventCard);
        }
    }

    private void registerForEvent(Evenement event) {
        try {
            edu.connexion3a8.entities.Inscription inscription = new edu.connexion3a8.entities.Inscription(
                    currentUser.getId(), event.getIdEvenement(), "confirme");
            inscriptionService.addEntity(inscription);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText(null);
            alert.setContentText("Successfully registered for: " + event.getTitre());
            alert.showAndWait();

            loadEvents();
        } catch (SQLException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Registration Failed");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText().toLowerCase();
        if (query.isEmpty()) {
            displayEvents(allEvents);
        } else {
            List<Evenement> filtered = allEvents.filtered(e ->
                    e.getTitre().toLowerCase().contains(query) ||
                    e.getLieu().toLowerCase().contains(query) ||
                    (e.getMentorName() != null && e.getMentorName().toLowerCase().contains(query))
            );
            displayEvents(filtered);
        }
    }

    @FXML
    private void handleBack() {
        try {
            InvestiApp.showHomePage(currentUser);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
