package edu.connexion3a8.controllers.events;

import edu.connexion3a8.entities.Evenement;
import edu.connexion3a8.entities.Inscription;
import edu.connexion3a8.services.EvenementService;
import edu.connexion3a8.services.InscriptionService;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import edu.connexion3a8.tools.FileOpener;
import java.io.File;
import java.io.FileWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

public class MainController {

    private final EvenementService es = new EvenementService();
    private final InscriptionService is = new InscriptionService();

    // Navigation
    @FXML
    private Button btnNavEvenement, btnNavInscription, btnNavStats, btnNavCalendrier;
    @FXML
    private VBox vueEvenements, vueInscriptions, vueStats, vueCalendrier;
    @FXML
    private Label lblDateTime;

    // Événements
    @FXML
    private TableView<Evenement> tabEvent;
    @FXML
    private TableColumn<Evenement, Integer> colEventId, colEventMentor;
    @FXML
    private TableColumn<Evenement, String> colEventTitre, colEventLieu, colEventImage;
    @FXML
    private TableColumn<Evenement, LocalDateTime> colEventDebut, colEventFin;
    @FXML
    private TableColumn<Evenement, Void> colEventActions;
    @FXML
    private TextField searchEvenement;
    @FXML
    private ComboBox<String> filtreLieuEvent, filtreMentorEvent;
    @FXML
    private Label lblCountEvent;

    private FilteredList<Evenement> filteredEvents;

    // Inscriptions
    @FXML
    private TableView<Inscription> tabInscription;
    @FXML
    private TableColumn<Inscription, Integer> colInscrId, colInscrUser, colInscrEvent;
    @FXML
    private TableColumn<Inscription, String> colInscrStatut;
    @FXML
    private TableColumn<Inscription, LocalDateTime> colInscrDate;
    @FXML
    private TableColumn<Inscription, Void> colInscrActions;
    @FXML
    private TextField searchInscription;
    @FXML
    private ComboBox<String> filtreStatutInscr, filtreEventInscr;
    @FXML
    private Label lblCountInscr;

    private FilteredList<Inscription> filteredInscriptions;

    // Stats
    @FXML
    private Label lblStatEvents, lblStatInscr, lblStatConfirm, lblStatAttente;
    @FXML
    private PieChart chartStatut;
    @FXML
    private BarChart<String, Number> chartEvents;

    // Calendrier
    @FXML
    private GridPane gridCalendrier;
    @FXML
    private Label lblMoisAnnee;
    @FXML
    private VBox vboxEventDetails;
    private YearMonth currentMonth = YearMonth.now();

    // Dark Mode
    @FXML
    private Button btnToggleTheme;
    // Theme manager removed - not needed for integration

    @FXML
    void initialize() {
        initDateTime();
        initTableEvenements();
        initTableInscriptions();
        chargerEvenements();
        chargerInscriptions();
        initFiltres();

        // Theme functionality removed for integration
    }

    // ==================== HORLOGE ====================
    private void initDateTime() {
        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            lblDateTime.setText(LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
        }), new KeyFrame(Duration.seconds(1)));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
    }

    // ==================== NAVIGATION ====================
    @FXML
    void afficherEvenements(ActionEvent e) {
        activerOnglet(btnNavEvenement, vueEvenements);
        chargerEvenements();
    }

    @FXML
    void afficherInscriptions(ActionEvent e) {
        activerOnglet(btnNavInscription, vueInscriptions);
        chargerInscriptions();
    }

    @FXML
    void afficherStats(ActionEvent e) {
        activerOnglet(btnNavStats, vueStats);
        calculerStatsAvecPourcentages();
    }

    @FXML
    void afficherCalendrier(ActionEvent e) {
        activerOnglet(btnNavCalendrier, vueCalendrier);
        afficherMoisAvecStats(currentMonth);
    }

    private void activerOnglet(Button actif, VBox vue) {
        // Désactiver tous
        btnNavEvenement.getStyleClass().setAll("nav-btn");
        btnNavInscription.getStyleClass().setAll("nav-btn");
        btnNavStats.getStyleClass().setAll("nav-btn");
        btnNavCalendrier.getStyleClass().setAll("nav-btn");

        vueEvenements.setVisible(false);
        vueEvenements.setManaged(false);
        vueInscriptions.setVisible(false);
        vueInscriptions.setManaged(false);
        vueStats.setVisible(false);
        vueStats.setManaged(false);
        vueCalendrier.setVisible(false);
        vueCalendrier.setManaged(false);

        // Activer celui cliqué
        actif.getStyleClass().setAll("nav-btn-active");
        vue.setVisible(true);
        vue.setManaged(true);
    }

    // ==================== ÉVÉNEMENTS ====================
    private void initTableEvenements() {
        colEventId.setCellValueFactory(new PropertyValueFactory<>("idEvenement"));
        colEventMentor.setCellValueFactory(new PropertyValueFactory<>("idMentor"));
        colEventTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colEventLieu.setCellValueFactory(new PropertyValueFactory<>("lieu"));

        // Image Column
        colEventImage.setCellValueFactory(new PropertyValueFactory<>("imageUrl"));
        colEventImage.setCellFactory(col -> new TableCell<>() {
            private final ImageView imageView = new ImageView();
            {
                imageView.setFitWidth(60);
                imageView.setFitHeight(40);
                imageView.setPreserveRatio(true);
            }

            @Override
            protected void updateItem(String url, boolean empty) {
                super.updateItem(url, empty);
                if (empty || url == null || url.isEmpty()) {
                    setGraphic(null);
                } else {
                    try {
                        File file = new File(url);
                        if (file.exists()) {
                            imageView.setImage(new Image(file.toURI().toString(), true));
                            setGraphic(imageView);
                        } else {
                            setGraphic(new Label("Pas d'image"));
                        }
                    } catch (Exception e) {
                        setGraphic(new Label("Erreur image"));
                    }
                }
            }
        });

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm");
        colEventDebut.setCellValueFactory(new PropertyValueFactory<>("dateDebut"));
        colEventDebut.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime d, boolean empty) {
                super.updateItem(d, empty);
                setText(empty || d == null ? null : d.format(fmt));
            }
        });

        colEventFin.setCellValueFactory(new PropertyValueFactory<>("dateFin"));
        colEventFin.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime d, boolean empty) {
                super.updateItem(d, empty);
                setText(empty || d == null ? null : d.format(fmt));
            }
        });

        colEventActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = new Button("Modifier");
            private final Button btnDel = new Button("✗");
            private final HBox box = new HBox(6, btnEdit, btnDel);
            {
                box.setAlignment(Pos.CENTER);
                btnEdit.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; " +
                        "-fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 12px; -fx-padding: 4 10; -fx-background-radius: 5;");
                btnDel.setStyle("-fx-background-color: #DC2626; -fx-text-fill: white; " +
                        "-fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 14px; -fx-padding: 4 10; -fx-background-radius: 5;");
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
            List<Evenement> list = es.getData();
            ObservableList<Evenement> obs = FXCollections.observableArrayList(list);
            filteredEvents = new FilteredList<>(obs, p -> true);
            SortedList<Evenement> sortedData = new SortedList<>(filteredEvents);
            sortedData.comparatorProperty().bind(tabEvent.comparatorProperty());
            tabEvent.setItems(sortedData);
            lblCountEvent.setText(list.size() + " événement(s)");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    void rechercherEvenements() {
        String query = searchEvenement.getText().toLowerCase();
        filteredEvents.setPredicate(ev -> {
            if (query.isEmpty())
                return true;
            return ev.getTitre().toLowerCase().contains(query) ||
                    ev.getLieu().toLowerCase().contains(query) ||
                    String.valueOf(ev.getIdMentor()).contains(query);
        });
        lblCountEvent.setText(filteredEvents.size() + " événement(s)");
    }

    @FXML
    void filtrerEvenements() {
        String lieu = filtreLieuEvent.getValue();
        String mentor = filtreMentorEvent.getValue();

        filteredEvents.setPredicate(ev -> {
            boolean matchLieu = (lieu == null || lieu.equals("Tous") || ev.getLieu().equals(lieu));
            boolean matchMentor = (mentor == null || mentor.equals("Tous") ||
                    String.valueOf(ev.getIdMentor()).equals(mentor));
            return matchLieu && matchMentor;
        });
        lblCountEvent.setText(filteredEvents.size() + " événement(s)");
    }

    @FXML
    void exporterEvenementsCSV() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter Événements");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        fc.setInitialFileName("evenements.csv");
        File file = fc.showSaveDialog(tabEvent.getScene().getWindow());
        if (file != null) {
            try (FileWriter w = new FileWriter(file)) {
                w.write("ID,Mentor,Titre,Contenu,Lieu,Debut,Fin\n");
                for (Evenement e : tabEvent.getItems()) {
                    w.write(String.format("%d,%d,\"%s\",\"%s\",\"%s\",%s,%s\n",
                            e.getIdEvenement(), e.getIdMentor(), e.getTitre(),
                            e.getContenu(), e.getLieu(), e.getDateDebut(), e.getDateFin()));
                }
                afficherSucces("Export réussi", "Fichier CSV créé : " + file.getName());
                FileOpener.openFile(file);
            } catch (Exception ex) {
                afficherErreur("Erreur", "Export échoué", ex.getMessage());
            }
        }
    }

    @FXML
    void navAjoutEvenement(ActionEvent e) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/AddEvenement.fxml"));
            tabEvent.getScene().setRoot(root);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void modifierEvenement(Evenement ev) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AddEvenement.fxml"));
            Parent root = loader.load();
            AddEvenementController ctrl = loader.getController();
            ctrl.setEvenementAModifier(ev);
            tabEvent.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void supprimerEvenement(Evenement ev) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Confirmation");
        a.setHeaderText("Supprimer : " + ev.getTitre() + " ?");
        a.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    es.deleteEntity(ev.getIdEvenement());
                    chargerEvenements();
                    afficherSucces("Succès", "Événement supprimé");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // ==================== INSCRIPTIONS ====================
    private void initTableInscriptions() {
        colInscrId.setCellValueFactory(new PropertyValueFactory<>("idInscription"));
        colInscrUser.setCellValueFactory(new PropertyValueFactory<>("idUtilisateur"));
        colInscrEvent.setCellValueFactory(new PropertyValueFactory<>("idEvenement"));
        colInscrStatut.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getStatut()));

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm");
        colInscrDate.setCellValueFactory(new PropertyValueFactory<>("dateInscription"));
        colInscrDate.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime d, boolean empty) {
                super.updateItem(d, empty);
                setText(empty || d == null ? null : d.format(fmt));
            }
        });

        colInscrActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = new Button("✏️ Modifier");
            private final Button btnDel = new Button("🗑️");
            private final HBox box = new HBox(6, btnEdit, btnDel);
            {
                box.setAlignment(Pos.CENTER);
                btnEdit.setStyle("-fx-background-color: #1565C0; -fx-text-fill: white; " +
                        "-fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 11px; -fx-padding: 4 10;");
                btnDel.setStyle("-fx-background-color: #DC2626; -fx-text-fill: white; " +
                        "-fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 11px; -fx-padding: 4 10;");
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
            List<Inscription> list = is.getData();
            ObservableList<Inscription> obs = FXCollections.observableArrayList(list);
            filteredInscriptions = new FilteredList<>(obs, p -> true);
            SortedList<Inscription> sortedData = new SortedList<>(filteredInscriptions);
            sortedData.comparatorProperty().bind(tabInscription.comparatorProperty());
            tabInscription.setItems(sortedData);
            lblCountInscr.setText(list.size() + " inscription(s)");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    void rechercherInscriptions() {
        String query = searchInscription.getText().toLowerCase();
        filteredInscriptions.setPredicate(ins -> {
            if (query.isEmpty())
                return true;
            return (ins.getUserName() != null && ins.getUserName().toLowerCase().contains(query)) ||
                    String.valueOf(ins.getIdEvenement()).contains(query) ||
                    ins.getStatut().toLowerCase().contains(query);
        });
        lblCountInscr.setText(filteredInscriptions.size() + " inscription(s)");
    }

    @FXML
    void filtrerInscriptions() {
        String statut = filtreStatutInscr.getValue();
        String event = filtreEventInscr.getValue();

        filteredInscriptions.setPredicate(ins -> {
            boolean matchStatut = (statut == null || statut.equals("Tous") || ins.getStatut().equalsIgnoreCase(statut));
            boolean matchEvent = (event == null || event.equals("Tous") ||
                    String.valueOf(ins.getIdEvenement()).equals(event));
            return matchStatut && matchEvent;
        });
        lblCountInscr.setText(filteredInscriptions.size() + " inscription(s)");
    }

    @FXML
    void exporterInscriptionsCSV() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter Inscriptions");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        fc.setInitialFileName("inscriptions.csv");
        File file = fc.showSaveDialog(tabInscription.getScene().getWindow());
        if (file != null) {
            try (FileWriter w = new FileWriter(file)) {
                w.write("ID,Utilisateur,Evenement,Statut,Date\n");
                for (Inscription i : tabInscription.getItems()) {
                    w.write(String.format("%d,%s,%d,%s,%s\n",
                            i.getIdInscription(), i.getUserName(), i.getIdEvenement(),
                            i.getStatut(), i.getDateInscription()));
                }
                afficherSucces("Export réussi", "Fichier CSV créé : " + file.getName());
                FileOpener.openFile(file);
            } catch (Exception ex) {
                afficherErreur("Erreur", "Export échoué", ex.getMessage());
            }
        }
    }

    @FXML
    void navAjoutInscription(ActionEvent e) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/AddInscription.fxml"));
            tabInscription.getScene().setRoot(root);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void modifierInscription(Inscription ins) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AddInscription.fxml"));
            Parent root = loader.load();
            AddInscriptionController ctrl = loader.getController();
            ctrl.setInscriptionAModifier(ins);
            tabInscription.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void supprimerInscription(Inscription ins) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Confirmation");
        a.setHeaderText("Supprimer inscription " + ins.getUserName() + " ?");
        a.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    is.deleteEntity(ins.getIdInscription());
                    chargerInscriptions();
                    afficherSucces("Succès", "Inscription supprimée");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // ==================== FILTRES ====================
    private void initFiltres() {
        try {
            // Filtres Événements
            Set<String> lieux = es.getData().stream().map(Evenement::getLieu).collect(Collectors.toSet());
            filtreLieuEvent.setItems(FXCollections.observableArrayList("Tous"));
            filtreLieuEvent.getItems().addAll(lieux);
            filtreLieuEvent.setValue("Tous");

            Set<String> mentors = es.getData().stream()
                    .map(e -> String.valueOf(e.getIdMentor())).collect(Collectors.toSet());
            filtreMentorEvent.setItems(FXCollections.observableArrayList("Tous"));
            filtreMentorEvent.getItems().addAll(mentors);
            filtreMentorEvent.setValue("Tous");

            // Filtres Inscriptions
            filtreStatutInscr.setItems(FXCollections.observableArrayList(
                    "Tous", "CONFIRME", "EN_ATTENTE", "ANNULE"));
            filtreStatutInscr.setValue("Tous");

            Set<String> events = is.getData().stream()
                    .map(i -> String.valueOf(i.getIdEvenement())).collect(Collectors.toSet());
            filtreEventInscr.setItems(FXCollections.observableArrayList("Tous"));
            filtreEventInscr.getItems().addAll(events);
            filtreEventInscr.setValue("Tous");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==================== STATISTIQUES ====================
    private void calculerStats() {
        try {
            List<Evenement> events = es.getData();
            List<Inscription> inscrs = is.getData();

            lblStatEvents.setText(String.valueOf(events.size()));
            lblStatInscr.setText(String.valueOf(inscrs.size()));

            long confirme = inscrs.stream().filter(i -> i.getStatut().equalsIgnoreCase("CONFIRME")).count();
            long attente = inscrs.stream().filter(i -> i.getStatut().equalsIgnoreCase("EN_ATTENTE")).count();

            lblStatConfirm.setText(String.valueOf(confirme));
            lblStatAttente.setText(String.valueOf(attente));

            // Pie Chart
            chartStatut.getData().clear();
            chartStatut.getData().add(new PieChart.Data("Confirmé", confirme));
            chartStatut.getData().add(new PieChart.Data("En attente", attente));
            long annule = inscrs.stream().filter(i -> i.getStatut().equalsIgnoreCase("ANNULE")).count();
            chartStatut.getData().add(new PieChart.Data("Annulé", annule));

            // Bar Chart
            chartEvents.getData().clear();
            Map<String, Long> parMois = events.stream()
                    .collect(Collectors.groupingBy(
                            e -> e.getDateDebut().getMonth().getDisplayName(TextStyle.SHORT, Locale.FRENCH),
                            Collectors.counting()));

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Événements");
            parMois.forEach((mois, count) -> series.getData().add(new XYChart.Data<>(mois, count)));
            chartEvents.getData().add(series);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==================== CALENDRIER ====================
    private void afficherMois(YearMonth ym) {
        gridCalendrier.getChildren().clear();
        lblMoisAnnee.setText(ym.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH) + " " + ym.getYear());

        // En-têtes jours
        String[] jours = { "Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim" };
        for (int i = 0; i < 7; i++) {
            Label l = new Label(jours[i]);
            l.setStyle("-fx-font-weight: bold; -fx-text-fill: #475569; -fx-alignment: center; " +
                    "-fx-pref-width: 80; -fx-padding: 10;");
            gridCalendrier.add(l, i, 0);
        }

        LocalDate premier = ym.atDay(1);
        int jourSemaine = premier.getDayOfWeek().getValue();

        try {
            List<Evenement> events = es.getData();
            Set<LocalDate> joursAvecEvent = events.stream()
                    .map(e -> e.getDateDebut().toLocalDate()).collect(Collectors.toSet());

            int row = 1, col = jourSemaine - 1;
            for (int jour = 1; jour <= ym.lengthOfMonth(); jour++) {
                LocalDate date = ym.atDay(jour);
                Label l = new Label(String.valueOf(jour));
                l.setStyle("-fx-alignment: center; -fx-pref-width: 80; -fx-pref-height: 60; " +
                        "-fx-border-color: #E2E8F0; " +
                        (joursAvecEvent.contains(date)
                                ? "-fx-background-color: #DBEAFE; -fx-border-width: 2; -fx-border-color: #60A5FA;"
                                : "-fx-background-color: #F8FAFC;"));
                l.setOnMouseClicked(e -> afficherEventsDuJour(date));
                gridCalendrier.add(l, col, row);

                col++;
                if (col == 7) {
                    col = 0;
                    row++;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    void moisPrecedent() {
        currentMonth = currentMonth.minusMonths(1);
        afficherMoisAvecStats(currentMonth);
    }

    @FXML
    void moisSuivant() {
        currentMonth = currentMonth.plusMonths(1);
        afficherMoisAvecStats(currentMonth);
    }

    private void afficherEventsDuJour(LocalDate date) {
        try {
            List<Evenement> events = es.getData().stream()
                    .filter(e -> e.getDateDebut().toLocalDate().equals(date))
                    .collect(Collectors.toList());

            if (events.isEmpty()) {
                afficherInfo("Aucun événement", "Pas d'événement le " +
                        date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            } else {
                String msg = events.stream()
                        .map(e -> "• " + e.getTitre() + " à " + e.getLieu())
                        .collect(Collectors.joining("\n"));
                afficherInfo("Événements du " + date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==================== UTILITAIRES ====================
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

    private void afficherInfo(String titre, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(titre);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    // ==================== PDF EXPORT ====================
    @FXML
    void exporterEvenementsPDF() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter Événements en PDF");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG (PDF Preview)", "*.png"));
        fc.setInitialFileName("evenements_export.png");
        File file = fc.showSaveDialog(tabEvent.getScene().getWindow());
        if (file != null) {
            afficherErreur("Non disponible", "Export PDF", "Fonctionnalité non disponible dans cette version");
        }
    }

    @FXML
    void exporterInscriptionsPDF() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter Inscriptions en PDF");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG (PDF Preview)", "*.png"));
        fc.setInitialFileName("inscriptions_export.png");
        File file = fc.showSaveDialog(tabInscription.getScene().getWindow());
        if (file != null) {
            afficherErreur("Non disponible", "Export PDF", "Fonctionnalité non disponible dans cette version");
        }
    }

    // ==================== ENHANCED CALENDAR WITH STATS ====================
    private void afficherMoisAvecStats(YearMonth ym) {
        gridCalendrier.getChildren().clear();
        vboxEventDetails.getChildren().clear();
        vboxEventDetails.getChildren().add(new Label("Sélectionnez un jour avec ✓ pour voir les détails"));
        lblMoisAnnee.setText(ym.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH) + " " + ym.getYear());

        // En-têtes jours
        String[] jours = { "Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim" };
        for (int i = 0; i < 7; i++) {
            Label l = new Label(jours[i]);
            l.setStyle("-fx-font-weight: bold; -fx-text-fill: #475569; -fx-alignment: center; " +
                    "-fx-pref-width: 80; -fx-padding: 10;");
            gridCalendrier.add(l, i, 0);
        }

        LocalDate premier = ym.atDay(1);
        int jourSemaine = premier.getDayOfWeek().getValue();
        LocalDate today = LocalDate.now();

        try {
            List<Evenement> events = es.getData();
            List<Inscription> inscriptions = is.getData();

            // Map events by date
            Map<LocalDate, List<Evenement>> eventsByDate = events.stream()
                    .collect(Collectors.groupingBy(e -> e.getDateDebut().toLocalDate()));

            // Map inscription counts by event
            Map<Integer, Long> inscrCountByEvent = inscriptions.stream()
                    .collect(Collectors.groupingBy(Inscription::getIdEvenement, Collectors.counting()));

            int row = 1, col = jourSemaine - 1;
            for (int jour = 1; jour <= ym.lengthOfMonth(); jour++) {
                LocalDate date = ym.atDay(jour);
                List<Evenement> dayEvents = eventsByDate.getOrDefault(date, new ArrayList<>());

                VBox dayBox = new VBox(3);
                dayBox.setAlignment(javafx.geometry.Pos.TOP_CENTER);
                dayBox.setStyle("-fx-alignment: center; -fx-pref-width: 80; -fx-pref-height: 80; " +
                        "-fx-border-color: #E2E8F0; -fx-padding: 5;");

                // Day number
                Label dayLabel = new Label(String.valueOf(jour));
                dayLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

                if (date.equals(today)) {
                    dayLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #456990;");
                    dayBox.setStyle(dayBox.getStyle()
                            + "-fx-background-color: #DBEAFE; -fx-border-width: 2; -fx-border-color: #456990;");
                } else if (!dayEvents.isEmpty()) {
                    long daysUntil = java.time.temporal.ChronoUnit.DAYS.between(today, date);

                    if (daysUntil >= 0 && daysUntil <= 7) {
                        // Urgent - within 7 days
                        dayBox.setStyle(dayBox.getStyle()
                                + "-fx-background-color: #FEE2E2; -fx-border-width: 2; -fx-border-color: #A62639;");
                    } else if (daysUntil > 7 && daysUntil <= 30) {
                        // Soon - within 30 days
                        dayBox.setStyle(dayBox.getStyle()
                                + "-fx-background-color: #FEF3C7; -fx-border-width: 2; -fx-border-color: #9B7E46;");
                    } else {
                        dayBox.setStyle(dayBox.getStyle()
                                + "-fx-background-color: #DBEAFE; -fx-border-width: 2; -fx-border-color: #60A5FA;");
                    }

                    // Event count
                    Label eventCount = new Label(dayEvents.size() + " 📅");
                    eventCount.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #456990;");

                    // Total inscriptions for this day
                    long totalInscr = dayEvents.stream()
                            .mapToLong(e -> inscrCountByEvent.getOrDefault(e.getIdEvenement(), 0L))
                            .sum();

                    if (totalInscr > 0) {
                        Label inscrLabel = new Label(totalInscr + " 👥");
                        inscrLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #9B7E46;");
                        dayBox.getChildren().addAll(dayLabel, eventCount, inscrLabel);
                    } else {
                        dayBox.getChildren().addAll(dayLabel, eventCount);
                    }

                    // Days remaining for upcoming events
                    if (daysUntil >= 0) {
                        Label daysLabel = new Label(daysUntil + "j");
                        daysLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #A62639; -fx-font-weight: bold;");
                        dayBox.getChildren().add(daysLabel);
                    }
                } else {
                    dayBox.setStyle(dayBox.getStyle() + "-fx-background-color: #F8FAFC;");
                    dayBox.getChildren().add(dayLabel);
                }

                dayBox.setOnMouseClicked(e -> afficherEventsDuJourAvecStats(date, dayEvents, inscrCountByEvent));
                gridCalendrier.add(dayBox, col, row);

                col++;
                if (col == 7) {
                    col = 0;
                    row++;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void afficherEventsDuJourAvecStats(LocalDate date, List<Evenement> events, Map<Integer, Long> inscrCounts) {
        vboxEventDetails.getChildren().clear();

        Label dateHeader = new Label("📅 " + date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")));
        dateHeader
                .setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #475569; -fx-padding: 0 0 5 0;");
        vboxEventDetails.getChildren().add(dateHeader);

        if (events.isEmpty()) {
            Label noEvent = new Label("Aucun événement prévu.");
            noEvent.setStyle("-fx-text-fill: #94A3B8; -fx-font-style: italic;");
            vboxEventDetails.getChildren().add(noEvent);
        } else {
            for (Evenement e : events) {
                VBox card = new VBox(8);
                card.setStyle("-fx-background-color: #F8FAFC; -fx-padding: 12; -fx-border-color: #E2E8F0; " +
                        "-fx-border-radius: 8; -fx-background-radius: 8;");

                Label title = new Label(e.getTitre());
                title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #1E40AF;");
                title.setWrapText(true);

                Label info = new Label(
                        "📍 " + e.getLieu() + "\n⏰ " + e.getDateDebut().format(DateTimeFormatter.ofPattern("HH:mm")) +
                                " - " + e.getDateFin().format(DateTimeFormatter.ofPattern("HH:mm")));
                info.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px;");

                long inscrCount = inscrCounts.getOrDefault(e.getIdEvenement(), 0L);
                Label participants = new Label("👥 " + inscrCount + " inscrit(s)");
                participants.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #9B7E46;");

                Button btnView = new Button("Détails");
                btnView.setStyle(
                        "-fx-background-color: #EEF2FF; -fx-text-fill: #4F46E5; -fx-font-size: 10px; -fx-padding: 4 10; -fx-cursor: hand;");
                btnView.setOnAction(ev -> modifierEvenement(e));

                card.getChildren().addAll(title, info, participants, btnView);
                vboxEventDetails.getChildren().add(card);
            }
        }
    }

    // ==================== ENHANCED STATS WITH PERCENTAGES ====================
    private void calculerStatsAvecPourcentages() {
        try {
            List<Evenement> events = es.getData();
            List<Inscription> inscrs = is.getData();

            lblStatEvents.setText(String.valueOf(events.size()));
            lblStatInscr.setText(String.valueOf(inscrs.size()));

            long confirme = inscrs.stream().filter(i -> i.getStatut().equalsIgnoreCase("CONFIRME")).count();
            long attente = inscrs.stream().filter(i -> i.getStatut().equalsIgnoreCase("EN_ATTENTE")).count();
            long annule = inscrs.stream().filter(i -> i.getStatut().equalsIgnoreCase("ANNULE")).count();

            lblStatConfirm.setText(String.valueOf(confirme));
            lblStatAttente.setText(String.valueOf(attente));

            // Calculate percentages
            double total = inscrs.size();
            if (total > 0) {
                double pctConfirme = (confirme / total) * 100;
                double pctAttente = (attente / total) * 100;
                double pctAnnule = (annule / total) * 100;

                if (lblPctConfirme != null)
                    lblPctConfirme.setText(String.format("%.1f%%", pctConfirme));
                if (lblPctAttente != null)
                    lblPctAttente.setText(String.format("%.1f%%", pctAttente));
                if (lblPctAnnule != null)
                    lblPctAnnule.setText(String.format("%.1f%%", pctAnnule));
            }

            // Pie Chart
            chartStatut.getData().clear();
            chartStatut.getData().add(new PieChart.Data("Confirmé (" + confirme + ")", confirme));
            chartStatut.getData().add(new PieChart.Data("En attente (" + attente + ")", attente));
            chartStatut.getData().add(new PieChart.Data("Annulé (" + annule + ")", annule));

            // Bar Chart
            chartEvents.getData().clear();
            Map<String, Long> parMois = events.stream()
                    .collect(Collectors.groupingBy(
                            e -> e.getDateDebut().getMonth().getDisplayName(TextStyle.SHORT, Locale.FRENCH),
                            Collectors.counting()));

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Événements");
            parMois.forEach((mois, count) -> series.getData().add(new XYChart.Data<>(mois, count)));
            chartEvents.getData().add(series);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private Label lblPctConfirme, lblPctAttente, lblPctAnnule;

    // ==================== DARK MODE TOGGLE ====================
    private boolean isDarkMode = false;
    
    @FXML
    void toggleTheme(ActionEvent e) {
        isDarkMode = !isDarkMode;
        applyTheme();
    }
    
    private void applyTheme() {
        javafx.scene.Scene scene = btnToggleTheme.getScene();
        if (scene == null) return;
        
        if (isDarkMode) {
            scene.getStylesheets().clear();
            scene.getStylesheets().add(getClass().getResource("/css/dark-theme.css").toExternalForm());
            btnToggleTheme.setText("☀️ Mode Clair");
        } else {
            scene.getStylesheets().clear();
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            btnToggleTheme.setText("🌙 Mode Sombre");
        }
    }
    
    @FXML
    void backToAdminDashboard() {
        try {
            edu.connexion3a8.InvestiApp.showAdminDashboard();
        } catch (Exception ex) {
            afficherErreur("Erreur", "Navigation", "Impossible de retourner au dashboard: " + ex.getMessage());
        }
    }
}
