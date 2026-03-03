package edu.connexion3a8.controllers.events;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import edu.connexion3a8.InvestiApp;
import edu.connexion3a8.entities.Evenement;
import edu.connexion3a8.entities.Inscription;
import edu.connexion3a8.entities.User;
import edu.connexion3a8.services.EvenementService;
import edu.connexion3a8.services.InscriptionService;
import edu.connexion3a8.tools.FileOpener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

public class MentorDashboardController implements Initializable {

    // --- EVENEMENT ---
    @FXML
    private VBox vueEvenements;
    @FXML
    private TableView<Evenement> tabEvenement;
    @FXML
    private TableColumn<Evenement, Integer> colEventId;
    @FXML
    private TableColumn<Evenement, String> colEventImage;
    @FXML
    private TableColumn<Evenement, String> colEventTitre;
    @FXML
    private TableColumn<Evenement, String> colEventLieu;
    @FXML
    private TableColumn<Evenement, LocalDateTime> colEventDateDebut;
    @FXML
    private TableColumn<Evenement, LocalDateTime> colEventDateFin;
    @FXML
    private TableColumn<Evenement, Void> colEventActions;
    @FXML
    private TextField searchEvenement;
    @FXML
    private ComboBox<String> filtreLieuEvent;
    @FXML
    private Label lblCountEvent;

    // --- INSCRIPTION ---
    @FXML
    private VBox vueInscriptions;
    @FXML
    private TableView<Inscription> tabInscription;
    @FXML
    private TableColumn<Inscription, Integer> colInscrId;
    @FXML
    private TableColumn<Inscription, String> colInscrUser;
    @FXML
    private TableColumn<Inscription, String> colInscrEvent;
    @FXML
    private TableColumn<Inscription, String> colInscrStatut;
    @FXML
    private TableColumn<Inscription, LocalDateTime> colInscrDate;
    @FXML
    private TableColumn<Inscription, Void> colInscrActions;
    @FXML
    private TextField searchInscription;
    @FXML
    private ComboBox<String> filtreStatutInscr;
    @FXML
    private ComboBox<String> filtreEventInscr;
    @FXML
    private Label lblCountInscr;

    // --- NAV BAR ---
    @FXML
    private Button btnNavEvenement;
    @FXML
    private Button btnNavInscription;
    @FXML
    private Button btnToggleTheme;
    @FXML
    private StackPane contentPane;

    private boolean isDarkTheme = false;

    private final EvenementService es = new EvenementService();
    private final InscriptionService is = new InscriptionService();

    private FilteredList<Evenement> filteredEvenements;
    private FilteredList<Inscription> filteredInscriptions;
    private ObservableList<Evenement> mentorEvents;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        initTableEvenements();
        chargerEvenements();

        initTableInscriptions();
        chargerInscriptions();

        initFiltres();
        afficherEvenements();
    }

    // ==================== NAVIGATION ====================

    @FXML
    void afficherEvenements() {
        vueEvenements.setVisible(true);
        vueEvenements.setManaged(true);
        vueInscriptions.setVisible(false);
        vueInscriptions.setManaged(false);

        btnNavEvenement.getStyleClass().setAll("nav-btn-active");
        btnNavInscription.getStyleClass().setAll("nav-btn");
    }

    @FXML
    void afficherInscriptions() {
        vueEvenements.setVisible(false);
        vueEvenements.setManaged(false);
        vueInscriptions.setVisible(true);
        vueInscriptions.setManaged(true);

        btnNavEvenement.getStyleClass().setAll("nav-btn");
        btnNavInscription.getStyleClass().setAll("nav-btn-active");
    }

    @FXML
    void navToHome(ActionEvent event) {
        try {
            InvestiApp.showHomePage();
        } catch (Exception e) {
            afficherErreur("Erreur", "Navigation échouée", e.getMessage());
        }
    }

    @FXML
    void handleLogout(ActionEvent event) {
        try {
            InvestiApp.showLoginPage();
        } catch (Exception e) {
            afficherErreur("Erreur", "Déconnexion échouée", e.getMessage());
        }
    }

    // ==================== THEME ====================

    @FXML
    void toggleTheme(ActionEvent event) {
        Scene scene = contentPane.getScene();
        if (scene != null) {
            isDarkTheme = !isDarkTheme;
            if (isDarkTheme) {
                scene.getStylesheets().clear();
                scene.getStylesheets().add(getClass().getResource("/css/dark-theme.css").toExternalForm());
                btnToggleTheme.setText("☀️ Mode Clair");
            } else {
                scene.getStylesheets().clear();
                scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
                btnToggleTheme.setText("🌙 Mode Sombre");
            }
        }
    }

    // ==================== ÉVÉNEMENTS MENTOR ====================

    private void initTableEvenements() {
        colEventId.setCellValueFactory(new PropertyValueFactory<>("idEvenement"));
        colEventTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colEventLieu.setCellValueFactory(new PropertyValueFactory<>("lieu"));

        // Format dates
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        colEventDateDebut.setCellValueFactory(new PropertyValueFactory<>("dateDebut"));
        colEventDateDebut.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime d, boolean empty) {
                super.updateItem(d, empty);
                setText(empty || d == null ? null : d.format(fmt));
            }
        });

        colEventDateFin.setCellValueFactory(new PropertyValueFactory<>("dateFin"));
        colEventDateFin.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime d, boolean empty) {
                super.updateItem(d, empty);
                setText(empty || d == null ? null : d.format(fmt));
            }
        });

        colEventImage.setCellValueFactory(new PropertyValueFactory<>("imageUrl"));
        colEventImage.setCellFactory(col -> new TableCell<>() {
            private final ImageView imageView = new ImageView();
            {
                imageView.setFitWidth(60);
                imageView.setFitHeight(40);
                imageView.setPreserveRatio(true);
                setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(String imageUrl, boolean empty) {
                super.updateItem(imageUrl, empty);
                if (empty || imageUrl == null || imageUrl.isEmpty()) {
                    setGraphic(null);
                } else {
                    try {
                        File file = new File(imageUrl);
                        if (file.exists()) {
                            Image image = new Image(file.toURI().toString(), 60, 40, true, true);
                            imageView.setImage(image);
                            setGraphic(imageView);
                        } else {
                            setGraphic(new Label("Introuvable"));
                        }
                    } catch (Exception e) {
                        setGraphic(new Label("Erreur"));
                    }
                }
            }
        });

        colEventActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = new Button("Modifier");
            private final Button btnDel = new Button("✗");
            private final HBox box = new HBox(10, btnEdit, btnDel);
            {
                box.setAlignment(Pos.CENTER);
                btnEdit.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; " +
                        "-fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 14px; -fx-padding: 5 12; -fx-background-radius: 6;");
                btnDel.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; " +
                        "-fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 14px; -fx-padding: 5 12; -fx-background-radius: 6;");
                btnEdit.setOnAction(e -> modifierEvenement(getTableView().getItems().get(getIndex())));
                btnDel.setOnAction(e -> supprimerEvenement(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void chargerEvenements() {
        try {
            User currentUser = InvestiApp.getCurrentUser();
            String currentUserIdStr = String.valueOf(currentUser.getId());

            // Filter strictly events for this mentor
            List<Evenement> allDbEvents = es.getData();
            List<Evenement> filteredByMentor = allDbEvents.stream()
                    .filter(e -> currentUserIdStr.equals(e.getIdMentor()))
                    .collect(Collectors.toList());

            mentorEvents = FXCollections.observableArrayList(filteredByMentor);
            filteredEvenements = new FilteredList<>(mentorEvents, p -> true);

            SortedList<Evenement> sortedData = new SortedList<>(filteredEvenements);
            sortedData.comparatorProperty().bind(tabEvenement.comparatorProperty());
            tabEvenement.setItems(sortedData);

            lblCountEvent.setText(filteredByMentor.size() + " événement(s)");

            // Mettre à jour les filtres
            Set<String> lieux = mentorEvents.stream().map(Evenement::getLieu).collect(Collectors.toSet());
            filtreLieuEvent.setItems(FXCollections.observableArrayList("Tous"));
            filtreLieuEvent.getItems().addAll(lieux);
            filtreLieuEvent.setValue("Tous");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void modifierEvenement(Evenement ev) {
        try {
            FXMLLoader l = new FXMLLoader(getClass().getResource("/AddEvenement.fxml"));
            Parent root = l.load();
            AddEvenementController ctrl = l.getController();
            ctrl.setEvenementAModifier(ev);
            tabEvenement.getScene().setRoot(root);
        } catch (Exception e) {
            afficherErreur("Erreur", "Ouverture modification impossible", e.getMessage());
        }
    }

    private void supprimerEvenement(Evenement ev) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation Suppression");
        alert.setHeaderText("Supprimer '" + ev.getTitre() + "' ?");
        Optional<ButtonType> res = alert.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            try {
                es.deleteEntity(ev.getIdEvenement());
                chargerEvenements();
                chargerInscriptions(); // Inscriptions depend on events
                initFiltres();
                afficherSucces("Supprimé", "L'événement a été supprimé.");
            } catch (SQLException e) {
                afficherErreur("Erreur", "Impossible de supprimer", e.getMessage());
            }
        }
    }

    @FXML
    void rechercherEvenements(KeyEvent event) {
        String query = searchEvenement.getText().toLowerCase();
        filteredEvenements.setPredicate(ev -> {
            if (query.isEmpty())
                return true;
            return ev.getTitre().toLowerCase().contains(query) ||
                    ev.getLieu().toLowerCase().contains(query);
        });
        lblCountEvent.setText(filteredEvenements.size() + " événement(s)");
    }

    @FXML
    void filtrerEvenements(ActionEvent event) {
        String lieu = filtreLieuEvent.getValue();
        filteredEvenements.setPredicate(ev -> {
            return (lieu == null || lieu.equals("Tous") || ev.getLieu().equalsIgnoreCase(lieu));
        });
        lblCountEvent.setText(filteredEvenements.size() + " événement(s)");
    }

    @FXML
    void exporterEvenementsCSV(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter Mes Événements en CSV");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        fc.setInitialFileName("mes_evenements_" + LocalDate.now() + ".csv");
        File file = fc.showSaveDialog(tabEvenement.getScene().getWindow());

        if (file != null) {
            try (FileWriter writer = new FileWriter(file, java.nio.charset.StandardCharsets.UTF_8)) {
                writer.write('\ufeff');
                writer.write("ID;Titre;Lieu;Date Début;Date Fin\n");
                for (Evenement e : mentorEvents) {
                    writer.write(String.format("%d;%s;%s;%s;%s\n",
                            e.getIdEvenement(),
                            e.getTitre().replace(";", ","),
                            e.getLieu().replace(";", ","),
                            e.getDateDebut().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                            e.getDateFin().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
                }
            } catch (Exception e) {
                afficherErreur("Erreur", "Export CSV échoué", e.getMessage());
                return;
            }
            afficherSucces("Succès", "Export CSV réussi (Format Excel) !");
            FileOpener.openFile(file);
        }
    }

    @FXML
    void exporterEvenementsPDF(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Générer Rapport Mentor PDF");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        fc.setInitialFileName("Rapport_Mentor_" + LocalDate.now() + ".pdf");
        File file = fc.showSaveDialog(tabEvenement.getScene().getWindow());

        if (file != null) {
            try {
                Document doc = new Document(PageSize.A4.rotate());
                PdfWriter.getInstance(doc, new FileOutputStream(file));
                doc.open();

                Font titleFont = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD, new BaseColor(69, 105, 144));
                Paragraph title = new Paragraph("Rapport de Mes Événements (Mentor)", titleFont);
                title.setAlignment(Element.ALIGN_CENTER);
                title.setSpacingAfter(20);
                doc.add(title);

                PdfPTable table = new PdfPTable(4);
                table.setWidthPercentage(100);
                table.setWidths(new float[] { 2f, 2f, 2f, 2f });

                String[] headers = { "Titre", "Lieu", "Début", "Fin" };
                for (String h : headers) {
                    PdfPCell cell = new PdfPCell(new Phrase(h, new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD)));
                    cell.setBackgroundColor(new BaseColor(240, 240, 240));
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    cell.setPadding(8);
                    table.addCell(cell);
                }

                for (Evenement ev : mentorEvents) {
                    table.addCell(new Phrase(ev.getTitre()));
                    table.addCell(new Phrase(ev.getLieu()));
                    table.addCell(
                            new Phrase(ev.getDateDebut().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
                    table.addCell(new Phrase(ev.getDateFin().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
                }

                doc.add(table);
                doc.close();

                afficherSucces("Succès", "Rapport PDF généré !");
                FileOpener.openFile(file);
            } catch (Exception ex) {
                afficherErreur("Erreur", "Création PDF échouée", ex.getMessage());
            }
        }
    }

    @FXML
    void navAjoutEvenement(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/AddEvenement.fxml"));
            tabEvenement.getScene().setRoot(root);
        } catch (Exception e) {
            afficherErreur("Erreur", "Ouverture ajout impossible", e.getMessage());
        }
    }

    @FXML
    void navAjoutInscription(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/AddInscription.fxml"));
            tabInscription.getScene().setRoot(root);
        } catch (Exception e) {
            afficherErreur("Erreur", "Ouverture ajout inscription impossible", e.getMessage());
        }
    }

    // ==================== INSCRIPTIONS MENTOR ====================

    private void initTableInscriptions() {
        colInscrId.setCellValueFactory(new PropertyValueFactory<>("idInscription"));
        colInscrUser.setCellValueFactory(new PropertyValueFactory<>("userName"));
        colInscrEvent.setCellValueFactory(cellData -> {
            int eventId = cellData.getValue().getIdEvenement();
            return new javafx.beans.property.SimpleStringProperty(getEventTitle(eventId));
        });

        colInscrStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colInscrStatut.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String statut, boolean empty) {
                super.updateItem(statut, empty);
                if (empty || statut == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label lbl = new Label(statut.toUpperCase());
                    lbl.setStyle(
                            "-fx-padding: 4 10; -fx-background-radius: 12; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px;");
                    switch (statut.toLowerCase()) {
                        case "confirme":
                            lbl.setStyle(lbl.getStyle() + "-fx-background-color: #10B981;");
                            break;
                        case "en_attente":
                            lbl.setStyle(lbl.getStyle() + "-fx-background-color: #F59E0B;");
                            break;
                        case "annule":
                            lbl.setStyle(lbl.getStyle() + "-fx-background-color: #EF4444;");
                            break;
                        default:
                            lbl.setStyle(lbl.getStyle() + "-fx-background-color: #6B7280;");
                            break;
                    }
                    setGraphic(lbl);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        if (colInscrDate != null) {
            colInscrDate.setCellValueFactory(new PropertyValueFactory<>("dateInscription"));
            colInscrDate.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(LocalDateTime d, boolean empty) {
                    super.updateItem(d, empty);
                    setText(empty || d == null ? null : d.format(fmt));
                }
            });
        }

        colInscrActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = new Button("Modifier");
            private final Button btnDel = new Button("✗");
            private final HBox box = new HBox(10, btnEdit, btnDel);
            {
                box.setAlignment(Pos.CENTER);
                btnEdit.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; " +
                        "-fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 14px; -fx-padding: 5 12; -fx-background-radius: 6;");
                btnDel.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; " +
                        "-fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 14px; -fx-padding: 5 12; -fx-background-radius: 6;");
                btnEdit.setOnAction(e -> modifierInscription(getTableView().getItems().get(getIndex())));
                btnDel.setOnAction(e -> supprimerInscription(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void chargerInscriptions() {
        try {
            // Get all inscriptions
            List<Inscription> allInscr = is.getData();

            // Collect the mentor's event IDs to filter inscriptions
            Set<Integer> mentorEventIds = mentorEvents.stream()
                    .map(e -> e.getIdEvenement())
                    .collect(Collectors.toSet());

            // Filter strictly inscriptions for the mentor's events
            List<Inscription> filteredByEvents = allInscr.stream()
                    .filter(i -> mentorEventIds.contains(i.getIdEvenement()))
                    .collect(Collectors.toList());

            ObservableList<Inscription> obs = FXCollections.observableArrayList(filteredByEvents);
            filteredInscriptions = new FilteredList<>(obs, p -> true);
            SortedList<Inscription> sortedData = new SortedList<>(filteredInscriptions);
            sortedData.comparatorProperty().bind(tabInscription.comparatorProperty());
            tabInscription.setItems(sortedData);

            lblCountInscr.setText(filteredByEvents.size() + " participant(s)");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void modifierInscription(Inscription ins) {
        try {
            // Note: Reuse existing AddInscription.fxml but configure routing if needed
            FXMLLoader l = new FXMLLoader(getClass().getResource("/AddInscription.fxml"));
            Parent root = l.load();
            edu.connexion3a8.controllers.events.AddInscriptionController ctrl = l.getController();
            ctrl.setInscriptionAModifier(ins);
            tabInscription.getScene().setRoot(root);
        } catch (Exception e) {
            afficherErreur("Erreur", "Ouverture modification impossible", e.getMessage());
        }
    }

    private void supprimerInscription(Inscription ins) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Annuler participation");
        alert.setHeaderText("Retirer la participation de '" + ins.getUserName() + "' ?");
        Optional<ButtonType> res = alert.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            try {
                is.deleteEntity(ins.getIdInscription());
                chargerInscriptions(); // Reload filtered inscriptions
                afficherSucces("Supprimé", "Participation retirée.");
            } catch (SQLException e) {
                afficherErreur("Erreur", "Impossible de supprimer", e.getMessage());
            }
        }
    }

    private String getEventTitle(int eventId) {
        if (mentorEvents == null)
            return "ID: " + eventId;
        return mentorEvents.stream()
                .filter(e -> e.getIdEvenement() == eventId)
                .map(Evenement::getTitre)
                .findFirst()
                .orElse("ID: " + eventId);
    }

    @FXML
    void rechercherInscriptions(KeyEvent event) {
        String query = searchInscription.getText().toLowerCase();
        filteredInscriptions.setPredicate(ins -> {
            if (query.isEmpty())
                return true;
            return (ins.getUserName() != null && ins.getUserName().toLowerCase().contains(query)) ||
                    String.valueOf(ins.getIdEvenement()).contains(query) ||
                    ins.getStatut().toLowerCase().contains(query);
        });
        lblCountInscr.setText(filteredInscriptions.size() + " participant(s)");
    }

    @FXML
    void filtrerInscriptions(ActionEvent event) {
        String statut = filtreStatutInscr.getValue();
        String eventId = filtreEventInscr.getValue();

        filteredInscriptions.setPredicate(ins -> {
            boolean matchStatut = (statut == null || statut.equals("Tous") || ins.getStatut().equalsIgnoreCase(statut));
            boolean matchEvent = (eventId == null || eventId.equals("Tous")
                    || String.valueOf(ins.getIdEvenement()).equals(eventId));
            return matchStatut && matchEvent;
        });
        lblCountInscr.setText(filteredInscriptions.size() + " participant(s)");
    }

    @FXML
    void exporterInscriptionsCSV(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter Participants en CSV");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        fc.setInitialFileName("participants_" + LocalDate.now() + ".csv");
        File file = fc.showSaveDialog(tabInscription.getScene().getWindow());

        if (file != null) {
            try (FileWriter writer = new FileWriter(file, java.nio.charset.StandardCharsets.UTF_8)) {
                writer.write('\ufeff');
                writer.write("ID;Participant;Événement;Statut;Date\n");
                for (Inscription i : filteredInscriptions) {
                    writer.write(String.format("%d;%s;%s;%s;%s\n",
                            i.getIdInscription(),
                            (i.getUserName() != null ? i.getUserName() : "N/A").replace(";", ","),
                            getEventTitle(i.getIdEvenement()).replace(";", ","),
                            i.getStatut(),
                            i.getDateInscription().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
                }
            } catch (Exception e) {
                afficherErreur("Erreur", "Export CSV échoué", e.getMessage());
                return;
            }
            afficherSucces("Succès", "Export CSV réussi (Format Excel) !");
            FileOpener.openFile(file);
        }
    }

    @FXML
    void exporterInscriptionsPDF(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter Participants en PDF");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        fc.setInitialFileName("Participants_" + LocalDate.now() + ".pdf");
        File file = fc.showSaveDialog(tabInscription.getScene().getWindow());

        if (file != null) {
            try {
                // Margins: Left, Right, Top, Bottom
                Document doc = new Document(PageSize.A4, 40, 40, 50, 50);
                PdfWriter.getInstance(doc, new FileOutputStream(file));
                doc.open();

                // ---------------- COLOR PALETTE ----------------
                BaseColor brandColor = new BaseColor(15, 23, 42); // Slate 900
                BaseColor accentColor = new BaseColor(14, 165, 233); // Sky 500
                BaseColor lightGray = new BaseColor(248, 250, 252); // Slate 50
                BaseColor borderColor = new BaseColor(226, 232, 240); // Slate 200

                BaseColor colorSuccess = new BaseColor(34, 197, 94); // Green 500
                BaseColor colorWarning = new BaseColor(245, 158, 11); // Amber 500
                BaseColor colorDanger = new BaseColor(239, 68, 68); // Red 500

                // ---------------- FONTS ----------------
                Font titleFont = new Font(Font.FontFamily.HELVETICA, 26, Font.BOLD, BaseColor.WHITE);
                Font dateFont = new Font(Font.FontFamily.HELVETICA, 12, Font.ITALIC, BaseColor.LIGHT_GRAY);
                Font sectionFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, brandColor);
                Font headerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.WHITE);
                Font cellFont = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, new BaseColor(51, 65, 85));
                Font summaryFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, new BaseColor(51, 65, 85));

                // ---------------- HEADER BANNER ----------------
                PdfPTable banner = new PdfPTable(1);
                banner.setWidthPercentage(100);
                PdfPCell bannerCell = new PdfPCell();
                bannerCell.setBackgroundColor(brandColor);
                bannerCell.setPadding(30);
                bannerCell.setBorder(Rectangle.NO_BORDER);

                Paragraph title = new Paragraph("RAPPORT PARTICIPANTS", titleFont);
                title.setAlignment(Element.ALIGN_CENTER);
                bannerCell.addElement(title);

                Paragraph subTitle = new Paragraph(
                        "Généré le : " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")), dateFont);
                subTitle.setAlignment(Element.ALIGN_CENTER);
                subTitle.setSpacingBefore(10);
                bannerCell.addElement(subTitle);

                banner.addCell(bannerCell);
                doc.add(banner);

                // Spacing
                doc.add(new Paragraph(" "));

                // ---------------- STATISTICS SUMMARY ----------------
                long total = filteredInscriptions.size();
                long confirmes = filteredInscriptions.stream().filter(i -> "CONFIRME".equalsIgnoreCase(i.getStatut()))
                        .count();
                long attente = filteredInscriptions.stream().filter(i -> "EN_ATTENTE".equalsIgnoreCase(i.getStatut()))
                        .count();

                PdfPTable statsTable = new PdfPTable(3);
                statsTable.setWidthPercentage(100);
                statsTable.setSpacingBefore(15f);
                statsTable.setSpacingAfter(25f);

                PdfPCell stTotal = new PdfPCell(new Phrase("Total : " + total, summaryFont));
                stTotal.setBackgroundColor(lightGray);
                stTotal.setBorderColor(borderColor);
                stTotal.setPadding(15);
                stTotal.setHorizontalAlignment(Element.ALIGN_CENTER);

                PdfPCell stConf = new PdfPCell(new Phrase("Confirmés : " + confirmes,
                        new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, colorSuccess)));
                stConf.setBackgroundColor(lightGray);
                stConf.setBorderColor(borderColor);
                stConf.setPadding(15);
                stConf.setHorizontalAlignment(Element.ALIGN_CENTER);

                PdfPCell stAtt = new PdfPCell(new Phrase("En Attente : " + attente,
                        new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, colorWarning)));
                stAtt.setBackgroundColor(lightGray);
                stAtt.setBorderColor(borderColor);
                stAtt.setPadding(15);
                stAtt.setHorizontalAlignment(Element.ALIGN_CENTER);

                statsTable.addCell(stTotal);
                statsTable.addCell(stConf);
                statsTable.addCell(stAtt);
                doc.add(statsTable);

                // Section Title
                Paragraph secTitle = new Paragraph("Détails des Inscriptions", sectionFont);
                secTitle.setSpacingAfter(15);
                doc.add(secTitle);

                // ---------------- DATA TABLE ----------------
                PdfPTable table = new PdfPTable(4);
                table.setWidthPercentage(100);
                table.setWidths(new float[] { 2f, 2f, 1.2f, 2f });

                // Table Headers
                String[] headers = { "Événement", "Participant", "Statut", "Date d'Inscr." };
                for (String h : headers) {
                    PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
                    cell.setBackgroundColor(accentColor);
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                    cell.setPadding(12);
                    cell.setBorderColor(BaseColor.WHITE);
                    table.addCell(cell);
                }

                // Table Rows
                boolean alternate = false;
                for (Inscription i : filteredInscriptions) {
                    BaseColor rowBg = alternate ? lightGray : BaseColor.WHITE;

                    // Row cells
                    PdfPCell[] cells = new PdfPCell[4];
                    cells[0] = new PdfPCell(new Phrase(getEventTitle(i.getIdEvenement()), cellFont));
                    cells[1] = new PdfPCell(
                            new Phrase(i.getUserName() != null ? i.getUserName() : "Inconnu", cellFont));

                    // Status Coloring
                    Font statusFont = new Font(cellFont);
                    statusFont.setStyle(Font.BOLD);
                    if ("CONFIRME".equalsIgnoreCase(i.getStatut()))
                        statusFont.setColor(colorSuccess);
                    else if ("ANNULE".equalsIgnoreCase(i.getStatut()))
                        statusFont.setColor(colorDanger);
                    else if ("EN_ATTENTE".equalsIgnoreCase(i.getStatut()))
                        statusFont.setColor(colorWarning);

                    cells[2] = new PdfPCell(new Phrase(i.getStatut(), statusFont));
                    cells[3] = new PdfPCell(new Phrase(
                            i.getDateInscription().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), cellFont));

                    // Apply styles to all row cells
                    for (PdfPCell c : cells) {
                        c.setBackgroundColor(rowBg);
                        c.setHorizontalAlignment(Element.ALIGN_CENTER);
                        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
                        c.setPaddingTop(10);
                        c.setPaddingBottom(10);
                        c.setBorderColor(borderColor);
                        table.addCell(c);
                    }
                    alternate = !alternate;
                }
                doc.add(table);

                // ---------------- FOOTER ----------------
                Paragraph footer = new Paragraph(
                        "Investi App - Dashboard Mentor © " + LocalDate.now().getYear() + "\nDocument Officiel",
                        new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, new BaseColor(148, 163, 184)));
                footer.setAlignment(Element.ALIGN_CENTER);
                footer.setSpacingBefore(40);
                doc.add(footer);

                doc.close();

                afficherSucces("Succès", "Rapport PDF (Design Premium) généré avec succès !");
                FileOpener.openFile(file);
            } catch (Exception ex) {
                afficherErreur("Erreur", "Génération PDF échouée", ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    // ==================== UTILITAIRES & FILTRES ====================

    private void initFiltres() {
        try {
            filtreStatutInscr.setItems(FXCollections.observableArrayList("Tous", "CONFIRME", "EN_ATTENTE", "ANNULE"));
            filtreStatutInscr.setValue("Tous");

            Set<String> events = mentorEvents.stream()
                    .map(e -> String.valueOf(e.getIdEvenement())).collect(Collectors.toSet());
            filtreEventInscr.setItems(FXCollections.observableArrayList("Tous"));
            filtreEventInscr.getItems().addAll(events);
            filtreEventInscr.setValue("Tous");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void afficherErreur(String titre, String header, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(titre);
        a.setHeaderText(header);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void afficherSucces(String titre, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(titre);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
