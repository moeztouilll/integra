package edu.connexion3a8.controllers.collaboration;

import edu.connexion3a8.controllers.collaboration.Project.AddProjectController;
import edu.connexion3a8.controllers.collaboration.Project.UpdateProjectController;
import edu.connexion3a8.entities.collaboration.Investment;
import edu.connexion3a8.entities.collaboration.Milestone;
import edu.connexion3a8.entities.collaboration.Project;
import edu.connexion3a8.services.collaboration.AiService;
import edu.connexion3a8.services.collaboration.CollaborationService;
import edu.connexion3a8.services.collaboration.EmailService;
import edu.connexion3a8.services.collaboration.InvestmentService;
import edu.connexion3a8.services.collaboration.MilestoneService;
import edu.connexion3a8.services.collaboration.ProjectService;
import edu.connexion3a8.services.UserService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.animation.*;
import javafx.util.Duration;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class EntrepreneurController implements Initializable {

    // ─── Main Pages ────────────────────────────────────────────────────────────
    @FXML
    private VBox projectsPage;
    @FXML
    private VBox offersPage;
    @FXML
    private VBox collaborationPage;

    // ─── Sidebar Navigation Buttons ─────────────────────────────────────────────
    // Removed navProjects and navCollaboration as sidebars are eliminated.

    // ─── Projects Page Components ───────────────────────────────────────────────
    @FXML
    private FlowPane projectsContainer;

    // ─── Offers Page Components ─────────────────────────────────────────────────
    @FXML
    private TableView<Investment> offersTable;
    @FXML
    private TableColumn<Investment, Integer> colOfferId;
    @FXML
    private TableColumn<Investment, Integer> colOfferProject;
    @FXML
    private TableColumn<Investment, Integer> colOfferInvestor;
    @FXML
    private TableColumn<Investment, Double> colOfferReputation;
    @FXML
    private TableColumn<Investment, Double> colOfferAmount;
    @FXML
    private TableColumn<Investment, Double> colOfferEquity;
    @FXML
    private TableColumn<Investment, Integer> colOfferDuration;
    @FXML
    private TableColumn<Investment, String> colOfferStatus;

    @FXML
    private Label offersPageTitle;
    @FXML
    private ComboBox<String> statusFilterBox;
    @FXML
    private ComboBox<String> sortOffersBox;

    // AI Mentor components
    @FXML
    private VBox aiMentorBox;
    @FXML
    private Button btnMentor;
    @FXML
    private Label aiMentorAdvice;
    @FXML
    private Button btnAccept;
    @FXML
    private Button btnDecline;

    // ─── Collaboration Page Components ───────────────────────────────────────────
    @FXML
    private VBox collaborationContainer;

    // ─── Detail Overlay Components ───────────────────────────────────────────────
    @FXML
    private VBox detailOverlay;
    @FXML
    private Label detailCategory;
    @FXML
    private Label detailStatus;
    @FXML
    private Label detailTitle;
    @FXML
    private Label detailGoal;
    @FXML
    private Label detailEquity;
    @FXML
    private Label detailDesc;

    @FXML
    private Button btnEditProject;
    @FXML
    private Button btnViewOffers;
    @FXML
    private Button btnTrackProgress;
    @FXML
    private Button btnDeleteProject;
    @FXML
    private Button btnDealRoomOverlay;

    private final ProjectService projectService = new ProjectService();
    private final InvestmentService investmentService = new InvestmentService();
    private final MilestoneService milestoneService = new MilestoneService();
    private final CollaborationService collaborationService = new CollaborationService();
    private final UserService userService = new UserService();

    private edu.connexion3a8.entities.User currentUser;
    private String currentEntrepreneurId = "1"; // Default, will be replaced by setCurrentUser

    private List<Investment> allRelevantOffers = new ArrayList<>();
    private List<Project> myProjects = new ArrayList<>();
    private Project focusedProjectForOffers = null;
    private Project focusedProjectForCollab = null;

    public void setCurrentUser(edu.connexion3a8.entities.User user) {
        this.currentUser = user;
        if (user != null) {
            this.currentEntrepreneurId = user.getId();
            // Refresh data with actual user
            refreshData();
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupOffersTable();
        applyTableStyles(offersTable);

        statusFilterBox.setItems(FXCollections.observableArrayList("All", "PENDING", "ACCEPTED", "REFUSED"));
        statusFilterBox.setValue("All");

        sortOffersBox.setItems(FXCollections.observableArrayList(
                "Newest First", "Amount: High→Low", "Amount: Low→High", "Equity: High→Low", "Equity: Low→High"));
        sortOffersBox.setValue("Newest First");

        statusFilterBox.setOnAction(e -> applyOfferFilters());
        sortOffersBox.setOnAction(e -> applyOfferFilters());

        offersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                aiMentorBox.setVisible(true);
                aiMentorAdvice.setText("");
                btnMentor.setDisable(false);
                btnMentor.setText("💡 Ask AI Mentor");

                boolean isPending = "PENDING".equalsIgnoreCase(newV.getStatus());
                btnAccept.setDisable(!isPending);
                btnDecline.setDisable(!isPending);
            } else {
                aiMentorBox.setVisible(false);
            }
        });
        // NOTE: Do NOT call refreshData() here — currentEntrepreneurId is null until
        // setCurrentUser() is called by InvestiApp after the FXML loads.
    }

    // ─── Navigation Logic ───────────────────────────────────────────────────────

    @FXML
    void showProjectsPage() {
        focusedProjectForOffers = null;
        togglePage(projectsPage);
    }

    @FXML
    void showOffersPage() {
        if (focusedProjectForOffers == null) {
            showAlert(Alert.AlertType.WARNING, "No Project Selected",
                    "Please select a project from 'My Projects' to view its offers.");
            showProjectsPage();
            return;
        }
        offersPageTitle.setText("Offers for: " + focusedProjectForOffers.getTitle());
        hideProjectDetails();
        togglePage(offersPage);
    }

    @FXML
    void showCollaborationPage() {
        if (focusedProjectForCollab == null) {
            showAlert(Alert.AlertType.WARNING, "No Project Selected",
                    "Please select 'Track Progress' on a specific funding project to view collaborations.");
            showProjectsPage();
            return;
        }
        hideProjectDetails();
        togglePage(collaborationPage);
        buildCollaborationPage();
    }

    private void togglePage(VBox pageToShow) {
        projectsPage.setVisible(false);
        projectsPage.setManaged(false);
        offersPage.setVisible(false);
        offersPage.setManaged(false);
        collaborationPage.setVisible(false);
        collaborationPage.setManaged(false);

        pageToShow.setVisible(true);
        pageToShow.setManaged(true);
    }

    @FXML
    void logout() {
        Platform.exit();
    }

    @FXML
    void goHome() {
        try {
            edu.connexion3a8.InvestiApp.showHomePage(currentUser);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleProducts() {
        try {
            edu.connexion3a8.InvestiApp.showProductCatalog();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ─── Data & Projects Rendering ──────────────────────────────────────────────

    @FXML
    void refreshData() {
        if (currentEntrepreneurId == null)
            return; // not yet initialized by setCurrentUser()
        myProjects = projectService.getProjectsByEntrepreneur(currentEntrepreneurId);

        allRelevantOffers.clear();
        for (Investment inv : concat(
                investmentService.getInvestmentsByStatus("PENDING") != null
                        ? investmentService.getInvestmentsByStatus("PENDING")
                        : new ArrayList<>(),
                investmentService.getInvestmentsByStatus("ACCEPTED") != null
                        ? investmentService.getInvestmentsByStatus("ACCEPTED")
                        : new ArrayList<>(),
                investmentService.getInvestmentsByStatus("REFUSED") != null
                        ? investmentService.getInvestmentsByStatus("REFUSED")
                        : new ArrayList<>())) {
            if (myProjects.stream().anyMatch(p -> p.getProjectId() == inv.getProjectId())) {
                allRelevantOffers.add(inv);
            }
        }

        renderProjectCards();
        applyOfferFilters();
        if (collaborationPage.isVisible())
            buildCollaborationPage();
    }

    // ─── Detail Overlay Logic ───────────────────────────────────────────────────

    private void showProjectDetails(Project p) {
        detailTitle.setText(p.getTitle());
        detailCategory.setText(p.getCategory() != null ? p.getCategory() : "Other");
        detailStatus.setText(p.getStatus());
        detailStatus.setStyle("-fx-background-color: " + ("OPEN".equals(p.getStatus()) ? "#456990" : "#A62639") + ";");

        detailGoal.setText("$" + String.format("%.2f", p.getAmountRequested()));
        detailEquity.setText(p.getEquityOffered() + "%");
        detailDesc.setText(p.getDescription());

        // Configure actions buttons visibility
        btnEditProject.setManaged(true);
        btnEditProject.setVisible(true);
        btnViewOffers.setManaged(true);
        btnViewOffers.setVisible(true);
        btnTrackProgress.setManaged(true);
        btnTrackProgress.setVisible(true);
        btnDealRoomOverlay.setManaged(true);
        btnDealRoomOverlay.setVisible(true);
        btnDeleteProject.setManaged(true);
        btnDeleteProject.setVisible(true);

        boolean isLocked = "FUNDED".equalsIgnoreCase(p.getStatus()) || "CLOSED".equalsIgnoreCase(p.getStatus());

        // Show edit/delete buttons for more flexibility as per user request
        btnEditProject.setManaged(true);
        btnEditProject.setVisible(true);
        btnEditProject.setDisable(isLocked);
        btnEditProject.setOpacity(isLocked ? 0.5 : 1.0);

        btnDeleteProject.setManaged(true);
        btnDeleteProject.setVisible(true);
        btnDeleteProject.setDisable(isLocked);
        btnDeleteProject.setOpacity(isLocked ? 0.5 : 1.0);

        if (!"OPEN".equalsIgnoreCase(p.getStatus())) {
            btnViewOffers.setManaged(false);
            btnViewOffers.setVisible(false);
        }

        if (!"FUNDED".equalsIgnoreCase(p.getStatus())) {
            btnTrackProgress.setManaged(false);
            btnTrackProgress.setVisible(false);
            btnDealRoomOverlay.setManaged(false);
            btnDealRoomOverlay.setVisible(false);
        }

        btnEditProject.setOnAction(e -> {
            hideProjectDetails();
            editProject(p);
        });
        btnViewOffers.setOnAction(e -> {
            focusedProjectForOffers = p;
            showOffersPage();
        });
        btnTrackProgress.setOnAction(e -> {
            focusedProjectForCollab = p;
            showCollaborationPage();
        });
        btnDealRoomOverlay.setOnAction(e -> {
            Investment acceptedOffer = allRelevantOffers.stream()
                    .filter(inv -> inv.getProjectId() == p.getProjectId()
                            && "ACCEPTED".equalsIgnoreCase(inv.getStatus()))
                    .findFirst().orElse(null);
            if (acceptedOffer != null) {
                openDealRoom(acceptedOffer);
            }
        });
        btnDeleteProject.setOnAction(e -> {
            deleteProject(p);
        });

        detailOverlay.setOpacity(0);
        detailOverlay.setVisible(true);
        detailOverlay.setManaged(true);

        FadeTransition ft = new FadeTransition(Duration.millis(250), detailOverlay);
        ft.setToValue(1);
        ScaleTransition st = new ScaleTransition(Duration.millis(250), detailOverlay.getChildren().get(0));
        st.setFromX(0.95);
        st.setFromY(0.95);
        st.setToX(1.0);
        st.setToY(1.0);
        new ParallelTransition(ft, st).play();
    }

    @FXML
    void hideProjectDetails() {
        FadeTransition ft = new FadeTransition(Duration.millis(200), detailOverlay);
        ft.setToValue(0);
        ScaleTransition st = new ScaleTransition(Duration.millis(200), detailOverlay.getChildren().get(0));
        st.setToX(0.95);
        st.setToY(0.95);
        ParallelTransition pt = new ParallelTransition(ft, st);
        pt.setOnFinished(e -> {
            detailOverlay.setVisible(false);
            detailOverlay.setManaged(false);
        });
        pt.play();
    }

    private void renderProjectCards() {
        projectsContainer.getChildren().clear();
        if (myProjects.isEmpty()) {
            projectsContainer.getChildren().add(new Label("You haven't created any projects yet."));
            return;
        }

        for (Project p : myProjects) {
            boolean isFunded = "FUNDED".equalsIgnoreCase(p.getStatus());

            VBox card = new VBox(15);
            card.getStyleClass().addAll("project-card", "card-l1", "glass-card");
            card.setCache(true);
            card.setCacheHint(javafx.scene.CacheHint.SPEED);

            javafx.scene.effect.InnerShadow glassEdge = new javafx.scene.effect.InnerShadow();
            glassEdge.setRadius(8);
            glassEdge.setColor(Color.rgb(255, 255, 255, 0.4));
            card.setEffect(glassEdge);

            // ─── Funded Project: Animated Ambient Glow Ring ────────────────────
            if (isFunded) {
                javafx.scene.effect.DropShadow plasmaLayer2 = new javafx.scene.effect.DropShadow();
                plasmaLayer2.setColor(Color.web("#c07c4b", 0.4)); // Faded Copper ambient
                plasmaLayer2.setSpread(0.5);
                plasmaLayer2.setRadius(20);

                javafx.scene.effect.DropShadow plasmaLayer1 = new javafx.scene.effect.DropShadow();
                plasmaLayer1.setColor(Color.web("#2a506b", 0.8)); // Baltic Blue core
                plasmaLayer1.setSpread(0.2);
                plasmaLayer1.setRadius(5);
                plasmaLayer1.setInput(plasmaLayer2);

                card.setEffect(plasmaLayer1);

                Timeline plasmaAnim = new Timeline(
                        new KeyFrame(Duration.ZERO,
                                new KeyValue(plasmaLayer1.radiusProperty(), 5, Interpolator.EASE_BOTH),
                                new KeyValue(plasmaLayer2.radiusProperty(), 20, Interpolator.EASE_BOTH)),
                        new KeyFrame(Duration.seconds(2.0),
                                new KeyValue(plasmaLayer1.radiusProperty(), 15, Interpolator.EASE_BOTH),
                                new KeyValue(plasmaLayer2.radiusProperty(), 40, Interpolator.EASE_BOTH)));
                plasmaAnim.setAutoReverse(true);
                plasmaAnim.setCycleCount(Animation.INDEFINITE);
                plasmaAnim.play();

                card.setStyle(card.getStyle()
                        + "; -fx-border-color: #2a506b; -fx-border-width: 1px; -fx-border-radius: 12px;");
            }

            HBox header = new HBox(10);
            Label catBadge = new Label(p.getCategory() != null ? p.getCategory() : "Other");
            catBadge.getStyleClass().add("category-badge");

            Label statusBadge = new Label(p.getStatus());
            statusBadge.getStyleClass().add("category-badge");
            statusBadge
                    .setStyle("-fx-background-color: " + ("OPEN".equals(p.getStatus()) ? "#456990" : "#A62639") + ";");

            Label title = new Label(p.getTitle());
            title.getStyleClass().add("card-title");

            header.getChildren().addAll(catBadge, statusBadge, title);

            long offerCount = allRelevantOffers.stream().filter(inv -> inv.getProjectId() == p.getProjectId()).count();
            if ("OPEN".equals(p.getStatus())) {
                if (offerCount >= 2) {
                    Label hotBadge = new Label("🔥 HOT");
                    hotBadge.getStyleClass().add("badge-hot");
                    header.getChildren().add(hotBadge);
                } else if (offerCount == 0 && myProjects.indexOf(p) >= myProjects.size() - 2) {
                    Label newBadge = new Label("🆕 NEW");
                    newBadge.getStyleClass().add("badge-new");
                    header.getChildren().add(newBadge);
                }
            }

            Label desc = new Label(p.getDescription());
            desc.getStyleClass().add("card-desc");
            desc.setWrapText(true);
            desc.setMinHeight(Region.USE_PREF_SIZE);

            Label goal = new Label("Requested: $" + String.format("%.2f", p.getAmountRequested()));
            goal.getStyleClass().add("card-price");

            // ─── Momentum Badge: Aurora Breathing Pill ────────────────────────
            goal.setStyle(
                    "-fx-background-color: #2a506b; -fx-text-fill: #e6e6fa; -fx-padding: 6 12; -fx-background-radius: 20;");

            javafx.scene.effect.DropShadow auroraShadow = new javafx.scene.effect.DropShadow();
            auroraShadow.setColor(Color.web("#c07c4b", 0.6));
            auroraShadow.setRadius(5);
            auroraShadow.setSpread(0.2);
            goal.setEffect(auroraShadow);

            Timeline auroraAnim = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(auroraShadow.radiusProperty(), 5, Interpolator.EASE_BOTH),
                            new KeyValue(auroraShadow.colorProperty(), Color.web("#c07c4b", 0.6),
                                    Interpolator.EASE_BOTH)),
                    new KeyFrame(Duration.seconds(2.0),
                            new KeyValue(auroraShadow.radiusProperty(), 15, Interpolator.EASE_BOTH),
                            new KeyValue(auroraShadow.colorProperty(), Color.web("#8b3a3a", 0.4),
                                    Interpolator.EASE_BOTH)));
            auroraAnim.setAutoReverse(true);
            auroraAnim.setCycleCount(Animation.INDEFINITE);

            Timeline delay = new Timeline(
                    new KeyFrame(Duration.millis((myProjects.indexOf(p) * 100) + 200), e -> auroraAnim.play()));
            delay.play();

            StackPane goalContainer = new StackPane(goal);
            goalContainer.setAlignment(Pos.CENTER_LEFT);

            card.getChildren().addAll(header, desc, goalContainer);
            applyMagneticHover(card);
            card.setOnMouseClicked(e -> showProjectDetails(p));
            projectsContainer.getChildren().add(card);
        }
    }

    // ─── AI Mentor Logic (Idea 3) ───────────────────────────────────────────────

    @FXML
    void askAiMentor() {
        Investment selectedOffer = offersTable.getSelectionModel().getSelectedItem();
        if (selectedOffer == null)
            return;

        Optional<Project> projOpt = myProjects.stream().filter(p -> p.getProjectId() == selectedOffer.getProjectId())
                .findFirst();
        if (projOpt.isEmpty())
            return;
        Project p = projOpt.get();

        btnMentor.setText("AI is analyzing... ⏳");
        btnMentor.setDisable(true);

        new Thread(() -> {
            String advice = AiService.evaluateLogicForEntrepreneur(selectedOffer.getTotalAmount(),
                    selectedOffer.getEquityRequested(), p.getDescription());
            Platform.runLater(() -> {
                btnMentor.setText("💡 Mentor Responded");
                aiMentorAdvice.setStyle("-fx-text-fill: #1d4ed8; -fx-font-weight: bold; -fx-font-size: 14px;");
                aiMentorAdvice.setWrapText(true);
                aiMentorAdvice.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
                aiMentorAdvice.setText("AI Mentor: " + advice);
            });
        }).start();
    }

    // ─── Collaboration Tracker Logic (Idea 5) ───────────────────────────────────

    private void buildCollaborationPage() {
        if (collaborationContainer == null)
            return;
        collaborationContainer.getChildren().clear();

        List<Investment> activeCollabs = allRelevantOffers.stream()
                .filter(i -> "ACCEPTED".equalsIgnoreCase(i.getStatus()))
                .filter(i -> focusedProjectForCollab == null
                        || i.getProjectId() == focusedProjectForCollab.getProjectId())
                .collect(Collectors.toList());

        if (focusedProjectForCollab != null) {
            Button showAllBtn = new Button("⬅ Back to Projects");
            showAllBtn.getStyleClass().add("ai-btn");
            showAllBtn.setOnAction(e -> {
                focusedProjectForCollab = null;
                showProjectsPage();
            });
            collaborationContainer.getChildren().add(showAllBtn);
        }

        if (activeCollabs.isEmpty()) {
            collaborationContainer.getChildren()
                    .add(new Label("No active collaborations. Accept an offer to start tracking progress."));
            return;
        }

        for (Investment i : activeCollabs) {
            // ─── Get or ensure collaboration record ──────────────────────────
            edu.connexion3a8.entities.collaboration.Collaboration collab = null;
            try {
                collab = collaborationService.getCollaborationByInvestment(i.getInvestmentId(), i.getInvestorId());
                if (collab == null) {
                    // Get entrepreneur ID from project
                    Project project = projectService.readById(i.getProjectId());
                    edu.connexion3a8.entities.collaboration.Collaboration newCollab = new edu.connexion3a8.entities.collaboration.Collaboration(
                            0, i.getInvestmentId(), project.getEntrepreneurId(), i.getInvestorId(), null, "ACTIVE",
                            100.0, 0.0);
                    collab = collaborationService.createCollaboration(newCollab);
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            if (collab == null)
                continue;
            final int collabId = collab.getId();
            final int investmentId = i.getInvestmentId();

            // ─── Load milestones & calculate progress ───────────────────────
            List<Milestone> milestones = milestoneService.getMilestonesForCollaboration(collabId);
            double progress = milestoneService.calculateProgress(collabId);
            investmentService.syncProgressFromMilestones(investmentId, progress);

            // ─── Card container ─────────────────────────────────────────────
            VBox card = new VBox(20);
            card.getStyleClass().add("progress-card");

            // ─── Title & Health Badge ────────────────────────────────────────
            Project proj = myProjects.stream().filter(p -> p.getProjectId() == i.getProjectId()).findFirst()
                    .orElse(null);
            String projName = proj != null ? proj.getTitle() : "Project #" + i.getProjectId();
            Label title = new Label("📁 " + projName);
            title.getStyleClass().add("progress-title");

            int monthsPaid = i.getPaymentMonthsCompleted();
            int duration = i.getDurationMonths();
            double expectedPct = duration > 0 ? ((double) monthsPaid / duration) * 100 : 100;
            if (expectedPct == 0)
                expectedPct = 15; // Set strict threshold for new projects

            Label healthBadge = new Label();
            String pulseColor;
            if (milestones.isEmpty()) {
                healthBadge.setText("⚪ Not Started");
                healthBadge.getStyleClass().add("health-warning");
                pulseColor = "#94a3b8";
            } else if (progress >= expectedPct - 10) {
                healthBadge.setText("🟢 Healthy Pulse");
                healthBadge.getStyleClass().add("health-healthy");
                pulseColor = "#16a34a";
            } else if (progress >= expectedPct - 30) {
                healthBadge.setText("🟡 Minor Delays");
                healthBadge.getStyleClass().add("health-warning");
                pulseColor = "#ca8a04";
            } else {
                healthBadge.setText("🔴 Action Needed");
                healthBadge.getStyleClass().add("health-danger");
                pulseColor = "#dc2626";
            }

            javafx.scene.effect.DropShadow glow = new javafx.scene.effect.DropShadow();
            glow.setColor(Color.web(pulseColor));
            glow.setSpread(0.12);
            card.setEffect(glow);
            Timeline pulseTimeline = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(glow.radiusProperty(), 8)),
                    new KeyFrame(Duration.seconds(2.5), new KeyValue(glow.radiusProperty(), 28)));
            pulseTimeline.setAutoReverse(true);
            pulseTimeline.setCycleCount(Animation.INDEFINITE);
            pulseTimeline.play();

            HBox titleRow = new HBox(15, title, healthBadge);
            titleRow.setAlignment(Pos.CENTER_LEFT);

            // ─── Animated Completion Ring ────────────────────────────────────
            StackPane ringPane = buildCompletionRing(progress, pulseColor);

            // ─── Milestone Timeline ──────────────────────────────────────────
            Label msHeader = new Label("📌 Milestone Timeline");
            msHeader.getStyleClass().add("milestone-section-title");

            ScrollPane timelineScroll = new ScrollPane(
                    buildMilestoneTimeline(milestones, collabId, investmentId, true));
            timelineScroll.setFitToWidth(false);
            timelineScroll.setFitToHeight(true);
            timelineScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            timelineScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            timelineScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
            timelineScroll.setPrefHeight(160);

            // ─── Add Milestone Button ────────────────────────────────────────
            Button addMilestone = new Button("＋ Add Milestone");
            addMilestone.getStyleClass().add("milestone-add-btn");
            addMilestone.setOnAction(e -> openAddMilestoneDialog(collabId, investmentId));

            // ─── Financial + Payment Info ────────────────────────────────────
            Separator sep = new Separator();
            GridPane finGrid = new GridPane();
            finGrid.setHgap(30);
            finGrid.setVgap(10);

            finGrid.add(new Label("Total Funding:"), 0, 0);
            Label lv1 = new Label("$" + String.format("%.2f", i.getTotalAmount()));
            lv1.setStyle("-fx-font-weight: bold;");
            finGrid.add(lv1, 1, 0);

            finGrid.add(new Label("Investor Payments:"), 0, 1);
            HBox paymentBox = new HBox(15);
            paymentBox.setAlignment(Pos.CENTER_LEFT);

            // Build pill-style month tracker
            HBox pillRow = new HBox(5);
            pillRow.setAlignment(Pos.CENTER_LEFT);
            int maxShow = Math.min(duration, 12); // cap at 12 pills rows
            for (int m = 1; m <= maxShow; m++) {
                Label pill = new Label(m <= monthsPaid ? "✓" : String.valueOf(m));
                pill.setPrefSize(28, 22);
                pill.setAlignment(Pos.CENTER);
                pill.setStyle(
                        "-fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 6; -fx-background-color: "
                                + (m <= monthsPaid ? "#16a34a; -fx-text-fill: white;"
                                        : "#e2e8f0; -fx-text-fill: #64748b;"));
                pillRow.getChildren().add(pill);
            }
            if (duration > 12) {
                Label more = new Label("..." + (duration - 12) + " more");
                more.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8;");
                pillRow.getChildren().add(more);
            }
            Label v2 = new Label(monthsPaid + " / " + duration + " Paid");
            v2.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: "
                    + (monthsPaid >= duration ? "#16a34a" : "#64748b") + ";");
            paymentBox.getChildren().addAll(pillRow, v2);

            if (monthsPaid < duration) {
                java.time.LocalDate today = java.time.LocalDate.now();
                java.time.LocalDate lastPay = i.getLastPaymentDate() != null
                        ? new java.sql.Date(i.getLastPaymentDate().getTime()).toLocalDate()
                        : null;
                boolean alreadyPaidThisMonth = lastPay != null && lastPay.getYear() == today.getYear()
                        && lastPay.getMonthValue() == today.getMonthValue();

                // Calculate if late using centralized service logic
                boolean isLate = investmentService.checkIsLate(i, today);
                int anniversaryDay = 1;
                if (i.getInvestmentDate() != null) {
                    anniversaryDay = new java.sql.Date(i.getInvestmentDate().getTime()).toLocalDate().getDayOfMonth();
                }

                if (isLate) {
                    Label lateWarning = new Label("⚠️ INVESTOR LATE ON PAYMENT");
                    lateWarning.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold; -fx-font-size: 11px;");
                    paymentBox.getChildren().add(lateWarning);

                    // Force pulse color to red if late
                    pulseColor = "#dc2626";
                    healthBadge.setText("🔴 Payment Late");
                    healthBadge.getStyleClass().setAll("health-danger");
                }

                if (alreadyPaidThisMonth && lastPay != null) {
                    java.time.LocalDate nextDue = lastPay.plusMonths(1)
                            .withDayOfMonth(Math.min(anniversaryDay, lastPay.plusMonths(1).lengthOfMonth()));
                    Label nextPayLabel = new Label("Next payment window: " + nextDue.format(
                            java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy")));
                    nextPayLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8; -fx-font-weight: bold;");
                    paymentBox.getChildren().add(nextPayLabel);
                }
            }
            finGrid.add(paymentBox, 1, 1);

            finGrid.add(new Label("Equity Offered:"), 0, 2);
            Label lv3 = new Label(i.getEquityRequested() + "% Ownership");
            lv3.setStyle("-fx-font-weight: bold; -fx-text-fill: #9B7E46;");
            finGrid.add(lv3, 1, 2);

            Button btnDealRoom = new Button("Enter Deal Room 🤝");
            btnDealRoom.getStyleClass().add("ai-btn");
            btnDealRoom.setOnAction(e -> openDealRoom(i));
            finGrid.add(btnDealRoom, 0, 3);

            card.getChildren().addAll(titleRow, ringPane, msHeader, timelineScroll, addMilestone, sep, finGrid);
            collaborationContainer.getChildren().add(card);
        }
    }

    // ─── Completion Ring Builder ───────────────────────────────────────────────
    private StackPane buildCompletionRing(double progress, String color) {
        double radius = 56;
        double sw = 10;

        Arc track = new Arc(0, 0, radius, radius, 90, -360);
        track.setType(ArcType.OPEN);
        track.setFill(Color.TRANSPARENT);
        track.setStroke(Color.web("#5a7b93", 0.3)); // Muted Baltic Blue
        track.setStrokeWidth(sw);

        Arc fill = new Arc(0, 0, radius, radius, 90, 0);
        fill.setType(ArcType.OPEN);
        fill.setFill(Color.TRANSPARENT);
        fill.setStroke(Color.web(color));
        fill.setStrokeWidth(sw);
        fill.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);

        // Animate from 0 to target
        Timeline ringAnim = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(fill.lengthProperty(), 0)),
                new KeyFrame(Duration.millis(900),
                        new KeyValue(fill.lengthProperty(), -3.6 * progress, Interpolator.EASE_OUT)));
        ringAnim.play();

        VBox centerContent = new VBox(2);
        centerContent.setAlignment(Pos.CENTER);
        Label pctLabel = new Label("0%");
        pctLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        Label subLabel = new Label("Complete");
        subLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #5a7b93;");
        centerContent.getChildren().addAll(pctLabel, subLabel);

        // Count-up label animation (synced with arc)
        javafx.beans.property.DoubleProperty pctProp = new javafx.beans.property.SimpleDoubleProperty(0);
        pctProp.addListener((obs, ov, nv) -> pctLabel.setText(String.format("%.0f%%", nv.doubleValue())));
        Timeline countUp = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(pctProp, 0)),
                new KeyFrame(Duration.millis(900), new KeyValue(pctProp, progress, Interpolator.EASE_OUT)));
        countUp.play();

        // Wrap in a Group so the Arc doesn't shift its center when animating
        javafx.scene.Group group = new javafx.scene.Group(track, fill);

        StackPane ring = new StackPane(group, centerContent);
        ring.setAlignment(Pos.CENTER);
        ring.setMinSize(140, 140);
        ring.setMaxSize(140, 140);
        return ring;
    }

    // ─── Milestone Timeline Builder ────────────────────────────────────────────
    private HBox buildMilestoneTimeline(List<Milestone> milestones, int collabId, int investmentId, boolean editable) {
        HBox timeline = new HBox();
        timeline.setAlignment(Pos.CENTER_LEFT);
        timeline.setPadding(new javafx.geometry.Insets(20, 10, 10, 10));
        timeline.setSpacing(0);

        if (milestones.isEmpty()) {
            Label emptyLbl = new Label("No milestones yet. Click '＋ Add Milestone' to get started.");
            emptyLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-style: italic;");
            timeline.getChildren().add(emptyLbl);
            return timeline;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd");
        for (int idx = 0; idx < milestones.size(); idx++) {
            Milestone m = milestones.get(idx);

            // Node StackPane
            double nodeR = 18;
            Circle nodeCircle = new Circle(nodeR);
            Label nodeLabel;

            switch (m.getStatus()) {
                case "COMPLETED" -> {
                    nodeCircle.setFill(Color.web("#2a506b"));
                    nodeCircle.setStroke(Color.web("#2a506b"));
                    nodeCircle.setStrokeWidth(3);
                    nodeCircle.setEffect(new javafx.scene.effect.DropShadow(10, Color.web("#2a506b")));
                    nodeLabel = new Label("✓");
                    nodeLabel.setStyle("-fx-text-fill: #e6e6fa; -fx-font-weight: bold; -fx-font-size: 14px;");
                }
                case "IN_PROGRESS" -> {
                    nodeCircle.setFill(Color.TRANSPARENT);
                    nodeCircle.setStroke(Color.web("#c07c4b"));
                    nodeCircle.setStrokeWidth(3);
                    javafx.scene.effect.DropShadow glow = new javafx.scene.effect.DropShadow(12, Color.web("#c07c4b"));
                    nodeCircle.setEffect(glow);

                    Timeline pulse = new Timeline(
                            new KeyFrame(Duration.ZERO, new KeyValue(glow.radiusProperty(), 5)),
                            new KeyFrame(Duration.seconds(1), new KeyValue(glow.radiusProperty(), 15)));
                    pulse.setAutoReverse(true);
                    pulse.setCycleCount(Animation.INDEFINITE);
                    pulse.play();

                    nodeLabel = new Label(""); // User requested no inner dot/symbol for IN_PROGRESS
                }
                default -> {
                    // PENDING state
                    nodeCircle.setFill(Color.TRANSPARENT);
                    nodeCircle.setStroke(Color.web("#5a7b93"));
                    nodeCircle.setStrokeWidth(3);
                    nodeLabel = new Label(String.valueOf(idx + 1));
                    nodeLabel.setStyle("-fx-text-fill: #5a7b93; -fx-font-size: 11px; -fx-font-weight: bold;");
                }
            }

            StackPane nodeSP = new StackPane(nodeCircle, nodeLabel);
            nodeSP.setMinSize(nodeR * 2, nodeR * 2);
            nodeSP.setMaxSize(nodeR * 2, nodeR * 2);

            // Labels below/above node
            Label milLabel = new Label(m.getTitle());
            milLabel.setStyle(
                    "-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #1e293b; -fx-wrap-text: true; -fx-max-width: 80px; -fx-text-alignment: center;");

            Label weightLabel = new Label(String.format("%.0f%%", m.getWeight()));
            weightLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #5a7b93;");

            Label dateLabel = new Label(m.getDueDate() != null ? sdf.format(m.getDueDate()) : "");
            dateLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #5a7b93;");

            VBox nodeCol = new VBox(4, dateLabel, nodeSP, milLabel, weightLabel);
            nodeCol.setAlignment(Pos.TOP_CENTER);
            nodeCol.setMinWidth(90);

            if (editable) {
                final Milestone finalM = m;
                // Allow all nodes to be clickable to enable backwards progress updates
                nodeSP.setStyle("-fx-cursor: hand;");
                nodeSP.setOnMouseClicked(e -> openMilestoneStatusDialog(finalM, collabId, investmentId));
                nodeSP.setOnMouseEntered(e -> nodeCircle.setOpacity(0.8));
                nodeSP.setOnMouseExited(e -> nodeCircle.setOpacity(1.0));
            }

            timeline.getChildren().add(nodeCol);

            // Connector line between nodes
            if (idx < milestones.size() - 1) {
                boolean nextDone = "COMPLETED".equals(m.getStatus());
                Line connector = new Line(0, 0, 50, 0);
                connector.setStrokeWidth(3);
                connector.setStroke(nextDone ? Color.web("#2a506b") : Color.web("#e2e8f0"));
                VBox connWrapper = new VBox(connector);
                connWrapper.setAlignment(Pos.CENTER);
                connWrapper.setPadding(new javafx.geometry.Insets(0, 0, 30, 0));
                timeline.getChildren().add(connWrapper);
            }
        }
        return timeline;
    }

    // ─── Add Milestone Dialog ─────────────────────────────────────────────────
    private void openAddMilestoneDialog(int collabId, int investmentId) {
        Dialog<Milestone> dialog = new Dialog<>();
        dialog.setTitle("Add Milestone");
        dialog.setHeaderText("Create a New Project Milestone");

        ButtonType createBtnType = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createBtnType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(15);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        TextField titleField = new TextField();
        titleField.setPromptText("Enter milestone title");

        DatePicker datePicker = new DatePicker();
        datePicker.setPromptText("Select due date");
        // Disable past dates
        datePicker.setDayCellFactory(picker -> new javafx.scene.control.DateCell() {
            public void updateItem(java.time.LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                java.time.LocalDate today = java.time.LocalDate.now();
                setDisable(empty || date.compareTo(today) < 0);
            }
        });

        grid.add(new Label("Title:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Due Date:"), 0, 1);
        grid.add(datePicker, 1, 1);

        // Fetch existing to check for duplicate names
        List<Milestone> existing = milestoneService.getMilestonesForCollaboration(collabId);

        javafx.scene.Node createButton = dialog.getDialogPane().lookupButton(createBtnType);
        createButton.setDisable(true);

        // Auto-disable button if title is empty, duplicate, OR date not picked
        Runnable validateDialog = () -> {
            String newVal = titleField.getText();
            boolean isDuplicate = existing.stream()
                    .anyMatch(m -> m.getTitle().trim().equalsIgnoreCase(newVal.trim()));
            if (isDuplicate) {
                titleField.setStyle("-fx-border-color: red");
            } else {
                titleField.setStyle("");
            }
            createButton.setDisable(
                    isDuplicate || newVal.trim().isEmpty() || datePicker.getValue() == null);
        };

        titleField.textProperty().addListener((obs, oldVal, newVal) -> validateDialog.run());
        datePicker.valueProperty().addListener((obs, oldVal, newVal) -> validateDialog.run());
        validateDialog.run(); // initial state

        dialog.getDialogPane().setContent(grid);
        Platform.runLater(titleField::requestFocus);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createBtnType) {
                java.util.Date dDate = null;
                if (datePicker.getValue() != null) {
                    dDate = java.sql.Date.valueOf(datePicker.getValue());
                }
                return new Milestone(collabId, titleField.getText().trim(), "", 0.0, dDate);
            }
            return null;
        });

        Optional<Milestone> result = dialog.showAndWait();
        result.ifPresent(m -> {
            try {
                milestoneService.addMilestone(m);

                // Notification to Investor
                Investment inv = investmentService.getInvestmentById(investmentId);
                if (inv != null) {
                    Project p = projectService.getData().stream()
                            .filter(proj -> proj.getProjectId() == inv.getProjectId())
                            .findFirst().orElse(null);
                    String pTitle = p != null ? p.getTitle() : "Project #" + inv.getProjectId();

                    // Fetch real investor email
                    String investorEmail = "investor@example.com";
                    try {
                        edu.connexion3a8.entities.User investor = userService.getUserById(inv.getInvestorId());
                        if (investor != null)
                            investorEmail = investor.getEmail();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }

                    EmailService.sendMilestoneAdded(investorEmail, pTitle, m.getTitle());
                }

                refreshData();
                buildCollaborationPage();
            } catch (SQLException ex) {
                ex.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "DB Error", "Could not save milestone: " + ex.getMessage());
            }
        });
    }

    // ─── Milestone Status Popup ────────────────────────────────────────────────
    private void openMilestoneStatusDialog(Milestone m, int collabId, int investmentId) {
        if ("COMPLETED".equalsIgnoreCase(m.getStatus())) {
            // Locking the milestone
            showAlert(Alert.AlertType.INFORMATION, "Milestone Locked",
                    "This milestone has already been finished and cannot be reverted.");
            return;
        }

        ChoiceDialog<String> dlg = new ChoiceDialog<>(m.getStatus(),
                "PENDING", "IN_PROGRESS", "COMPLETED");
        dlg.setTitle("Update Milestone");
        dlg.setHeaderText("🎯 " + m.getTitle());
        dlg.setContentText("Select new status:");

        Optional<String> result = dlg.showAndWait();
        result.ifPresent(newStatus -> {
            boolean wasNotCompleted = !"COMPLETED".equals(m.getStatus());
            if (milestoneService.updateMilestoneStatus(m.getId(), newStatus)) {
                // Recalculate progress and sync
                double newProgress = milestoneService.calculateProgress(collabId);
                investmentService.syncProgressFromMilestones(investmentId, newProgress);

                // Notification to Investor if completed
                if (wasNotCompleted && "COMPLETED".equals(newStatus)) {
                    Investment inv = investmentService.getInvestmentById(investmentId);
                    if (inv != null) {
                        Project p = projectService.getData().stream()
                                .filter(proj -> proj.getProjectId() == inv.getProjectId())
                                .findFirst().orElse(null);
                        String pTitle = p != null ? p.getTitle() : "Project #" + inv.getProjectId();

                        // Fetch real investor email
                        String investorEmail = "investor@example.com";
                        try {
                            edu.connexion3a8.entities.User investor = userService.getUserById(inv.getInvestorId());
                            if (investor != null)
                                investorEmail = investor.getEmail();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }

                        EmailService.sendMilestoneCompleted(investorEmail, pTitle, m.getTitle());
                    }
                }

                refreshData();
                buildCollaborationPage();

                // ─── Confetti Explosion on 100% Milestone Completion ──────────
                if (wasNotCompleted && "COMPLETED".equals(newStatus) && newProgress >= 99.9) {
                    javafx.scene.Node root = projectsContainer.getScene().getRoot();
                    if (root instanceof Pane rootPane) {
                        edu.connexion3a8.utils.ConfettiEngine.fireConfetti(
                                rootPane,
                                rootPane.getWidth() / 2,
                                rootPane.getHeight() / 2);
                    }
                }
            }
        });
    }

    // ─── Offers Table Logic ─────────────────────────────────────────────────────

    private void setupOffersTable() {
        colOfferId.setCellValueFactory(new PropertyValueFactory<>("investmentId"));
        colOfferProject.setCellValueFactory(new PropertyValueFactory<>("projectId"));
        colOfferInvestor.setCellValueFactory(new PropertyValueFactory<>("investorId"));
        colOfferAmount.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        colOfferEquity.setCellValueFactory(new PropertyValueFactory<>("equityRequested"));
        colOfferDuration.setCellValueFactory(new PropertyValueFactory<>("durationMonths"));
        colOfferStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        colOfferReputation.setCellValueFactory(cellData -> {
            try {
                // Fetch investor's history to calculate reputation
                String investorId = cellData.getValue().getInvestorId();
                java.util.List<Investment> history = investmentService.getInvestmentsByInvestor(investorId);
                double score = edu.connexion3a8.services.collaboration.GovernanceEngineAPI
                        .calculateInvestorReputation(history);
                return new javafx.beans.property.SimpleDoubleProperty(score).asObject();
            } catch (Exception e) {
                return new javafx.beans.property.SimpleDoubleProperty(0.0).asObject();
            }
        });

        colOfferReputation.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Double score, boolean empty) {
                super.updateItem(score, empty);
                if (empty || score == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.format("%.1f / 100", score));
                    if (score >= 80)
                        setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold;"); // Green
                    else if (score >= 50)
                        setStyle("-fx-text-fill: #ca8a04; -fx-font-weight: bold;"); // Yellow
                    else
                        setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;"); // Red
                }
            }
        });

        offersTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Investment item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                    return;
                }
                switch (item.getStatus()) {
                    case "ACCEPTED" -> setStyle("-fx-background-color: #e8f5e9;");
                    case "REFUSED" -> setStyle("-fx-background-color: #fff5f5; -fx-opacity: 0.7;");
                    default -> setStyle("");
                }
            }
        });
    }

    private void applyOfferFilters() {
        if (focusedProjectForOffers == null)
            return;

        String statusFilt = statusFilterBox.getValue();
        String sort = sortOffersBox.getValue();

        List<Investment> filtered = allRelevantOffers.stream()
                .filter(i -> i.getProjectId() == focusedProjectForOffers.getProjectId())
                .filter(i -> statusFilt == null || statusFilt.equals("All") || statusFilt.equals(i.getStatus()))
                .collect(Collectors.toList());

        if (sort != null) {
            switch (sort) {
                case "Amount: High→Low" ->
                    filtered.sort((a, b) -> Double.compare(b.getTotalAmount(), a.getTotalAmount()));
                case "Amount: Low→High" ->
                    filtered.sort((a, b) -> Double.compare(a.getTotalAmount(), b.getTotalAmount()));
                case "Equity: High→Low" ->
                    filtered.sort((a, b) -> Double.compare(b.getEquityRequested(), a.getEquityRequested()));
                case "Equity: Low→High" ->
                    filtered.sort((a, b) -> Double.compare(a.getEquityRequested(), b.getEquityRequested()));
            }
        }
        offersTable.setItems(FXCollections.observableArrayList(filtered));
        offersTable.refresh();
    }

    // ─── Actions ────────────────────────────────────────────────────────────────

    @FXML
    void createNewProject() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/collaboration/AddProject.fxml"));
            Parent root = loader.load();
            Object controller = loader.getController();
            if (controller instanceof AddProjectController) {
                ((AddProjectController) controller).setCurrentUser(currentUser);
            }
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Create New Project");
            stage.showAndWait();
            refreshData();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", e.getMessage());
        }
    }

    @FXML
    void acceptOffer() {
        Investment selected = offersTable.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;
        boolean alreadyFunded = allRelevantOffers.stream()
                .anyMatch(inv -> inv.getProjectId() == selected.getProjectId() && "ACCEPTED".equals(inv.getStatus()));
        if (alreadyFunded) {
            showAlert(Alert.AlertType.ERROR, "Project Funded", "This project already has an accepted investment.");
            return;
        }
        if (confirm("Accept Offer", "This will FUND your project and REJECT all other pending offers.")) {
            if (investmentService.acceptInvestment(selected.getInvestmentId(), selected.getProjectId())) {
                String pTitle = myProjects.stream().filter(p -> p.getProjectId() == selected.getProjectId())
                        .map(Project::getTitle).findFirst().orElse("");
                try {
                    // Entrepreneur email = the currently logged-in user
                    String entrepreneurEmail = currentUser != null ? currentUser.getEmail() : null;
                    // Investor email = look up by investorId (UUID)
                    edu.connexion3a8.entities.User investor = userService.getUserById(selected.getInvestorId());
                    String investorEmail = investor != null ? investor.getEmail() : null;
                    if (entrepreneurEmail != null && investorEmail != null) {
                        EmailService.sendInvestmentAccepted(entrepreneurEmail, investorEmail, pTitle,
                                selected.getTotalAmount());
                    }
                } catch (Exception e) {
                    System.err.println("[EntrepreneurController] Email lookup failed: " + e.getMessage());
                }
                showAlert(Alert.AlertType.INFORMATION, "Congratulations!",
                        "Investment accepted. Project is now FUNDED! 🎉");
                refreshData();
            }
        }
    }

    @FXML
    void declineOffer() {
        Investment selected = offersTable.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;
        if (confirm("Decline Offer", "Are you sure you want to decline this offer?")) {
            selected.setStatus("REFUSED");
            if (investmentService.update(selected.getInvestmentId(), selected)) {
                String pTitle = myProjects.stream().filter(p -> p.getProjectId() == selected.getProjectId())
                        .map(Project::getTitle).findFirst().orElse("");
                try {
                    // Investor email = look up by investorId (UUID)
                    edu.connexion3a8.entities.User investor = userService.getUserById(selected.getInvestorId());
                    if (investor != null) {
                        EmailService.sendInvestmentRefused(investor.getEmail(), pTitle, selected.getTotalAmount());
                    }
                } catch (Exception e) {
                    System.err.println("[EntrepreneurController] Email lookup failed: " + e.getMessage());
                }
                refreshData();
            }
        }
    }

    private void editProject(Project p) {
        if ("FUNDED".equalsIgnoreCase(p.getStatus()) || "CLOSED".equalsIgnoreCase(p.getStatus())) {
            showAlert(Alert.AlertType.ERROR, "Modification Denied",
                    "This project is already " + p.getStatus() + ". Its terms cannot be modified.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/collaboration/UpdateProject.fxml"));
            Parent root = loader.load();
            UpdateProjectController controller = loader.getController();
            controller.initData(p);
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Update Project");
            stage.showAndWait();
            refreshData();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", e.getMessage());
        }
    }

    private void deleteProject(Project p) {
        if ("FUNDED".equalsIgnoreCase(p.getStatus()) || "CLOSED".equalsIgnoreCase(p.getStatus())) {
            showAlert(Alert.AlertType.ERROR, "Forbidden", "Cannot delete a funded or closed project.");
            return;
        }
        if (confirm("Delete Project", "Delete '" + p.getTitle() + "'?")) {
            projectService.deleteEntity(p);
            refreshData();
        }
    }

    @SafeVarargs
    private <T> java.util.List<T> concat(java.util.List<T>... lists) {
        java.util.List<T> result = new java.util.ArrayList<>();
        for (java.util.List<T> l : lists)
            result.addAll(l);
        return result;
    }

    private <T> void applyTableStyles(javafx.scene.control.TableView<T> tv) {
        tv.setRowFactory(table -> new javafx.scene.control.TableRow<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                    return;
                }
                String status = "";
                if (item instanceof edu.connexion3a8.entities.collaboration.Project)
                    status = ((edu.connexion3a8.entities.collaboration.Project) item).getStatus();
                else if (item instanceof edu.connexion3a8.entities.collaboration.Investment)
                    status = ((edu.connexion3a8.entities.collaboration.Investment) item).getStatus();

                String base;
                if ("PENDING".equals(status) || "UNDER_REVIEW".equals(status)) {
                    base = "-fx-background-color: #fef08a;"; // yellow
                } else if ("ACCEPTED".equals(status) || "OPEN".equals(status) || "FUNDED".equals(status)
                        || "VALIDATED".equals(status)) {
                    base = "-fx-background-color: #bfdbfe;"; // blue
                } else if ("REFUSED".equals(status) || "CLOSED".equals(status)) {
                    base = "-fx-background-color: #fecaca;"; // red
                } else {
                    base = ""; // default
                }

                if (isSelected()) {
                    setStyle("-fx-background-color: #2a506b; -fx-text-fill: #e6e6fa; -fx-font-weight: bold;");
                } else {
                    setStyle(base);
                }
            }

            @Override
            public void updateSelected(boolean selected) {
                super.updateSelected(selected);
                updateItem(getItem(), isEmpty());
            }
        });
    }

    private boolean confirm(String title, String content) {
        return AlertHelper.confirm(title, content);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        if (type == Alert.AlertType.ERROR)
            AlertHelper.showError(title, content);
        else if (type == Alert.AlertType.WARNING)
            AlertHelper.showWarning(title, content);
        else
            AlertHelper.showInfo(title, content);
    }

    @FXML
    void openPreAcceptanceDealRoom() {
        Investment selected = offersTable.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;
        openDealRoom(selected);
    }

    private void openDealRoom(Investment inv) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/collaboration/DealRoom.fxml"));
            javafx.scene.Parent root = loader.load();
            edu.connexion3a8.controllers.collaboration.DealRoomController controller = loader.getController();

            // Find project
            Project p = myProjects.stream().filter(proj -> proj.getProjectId() == inv.getProjectId()).findFirst()
                    .orElse(null);
            if (p == null) {
                showAlert(Alert.AlertType.ERROR, "Error",
                        "Could not find project #" + inv.getProjectId() + " in loaded list.");
                return;
            }
            controller.initData(inv, p, p.getEntrepreneurId()); // logged as entrepreneur
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setScene(new javafx.scene.Scene(root));
            stage.setTitle("Deal Room - " + p.getTitle());
            stage.showAndWait();
            refreshData();
            buildCollaborationPage();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Deal Room Error",
                    "Could not open Deal Room:\n" + e.getMessage());
        }
    }

    // ─── Magnetic Hover Engine ────────────────────────────────────────────────
    private void applyMagneticHover(javafx.scene.Node node) {
        node.setOnMouseEntered(e -> {
            node.setCache(true);
            node.setCacheHint(javafx.scene.CacheHint.SPEED);

            TranslateTransition tt = new TranslateTransition(Duration.millis(200), node);
            tt.setToY(-8);
            tt.setInterpolator(Interpolator.SPLINE(0.25, 0.1, 0.25, 1)); // Apple-like easing

            if (node.getEffect() instanceof javafx.scene.effect.DropShadow ds) {
                Timeline shadowGlow = new Timeline(
                        new KeyFrame(Duration.millis(200),
                                new KeyValue(ds.radiusProperty(), 40, Interpolator.EASE_OUT),
                                new KeyValue(ds.offsetYProperty(), 15, Interpolator.EASE_OUT)));
                shadowGlow.play();
            }
            tt.play();
        });

        node.setOnMouseExited(e -> {
            TranslateTransition tt = new TranslateTransition(Duration.millis(250), node);
            tt.setToY(0);
            tt.setOnFinished(ev -> node.setCache(false));

            if (node.getEffect() instanceof javafx.scene.effect.DropShadow ds) {
                Timeline shadowShrink = new Timeline(
                        new KeyFrame(Duration.millis(250),
                                new KeyValue(ds.radiusProperty(), 30, Interpolator.EASE_IN),
                                new KeyValue(ds.offsetYProperty(), 10, Interpolator.EASE_IN)));
                shadowShrink.play();
            }
            tt.play();
        });
    }
}
