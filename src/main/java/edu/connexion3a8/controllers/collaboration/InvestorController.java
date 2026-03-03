package edu.connexion3a8.controllers.collaboration;

import edu.connexion3a8.entities.collaboration.Investment;
import edu.connexion3a8.entities.collaboration.Milestone;
import edu.connexion3a8.entities.collaboration.Project;
import edu.connexion3a8.services.collaboration.AiService;
import edu.connexion3a8.services.collaboration.CollaborationService;
import edu.connexion3a8.services.collaboration.CurrencyService;
import edu.connexion3a8.services.collaboration.InvestmentService;
import edu.connexion3a8.services.collaboration.MilestoneService;
import edu.connexion3a8.services.collaboration.PdfExportService;
import edu.connexion3a8.services.collaboration.ProjectService;
import edu.connexion3a8.services.UserService;

import edu.connexion3a8.controllers.collaboration.investment.AddInvestmentController;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.awt.Desktop;
import java.io.File;

public class InvestorController implements Initializable {

    // ─── Main Pages ────────────────────────────────────────────────────────────
    @FXML
    private VBox browsePage;
    @FXML
    private VBox collaborationPage;

    // ─── Browse Page Components ──────────────────────────────────────────────────
    @FXML
    private FlowPane projectsContainer;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> categoryFilterBox;
    @FXML
    private ComboBox<String> sortBox;
    @FXML
    private ComboBox<String> currencyBox;

    // ─── AI Advisor Components ───────────────────────────────────────────────
    @FXML
    private Button btnAiAdvisor;
    @FXML
    private Label aiAdvisorOutput;

    // ─── Collaboration Page Components ───────────────────────────────────────────
    @FXML
    private VBox collaborationContainer;

    // ─── Detail Overlay Components ───────────────────────────────────────────────
    @FXML
    private VBox detailOverlay;
    @FXML
    private Label detailTitle;
    @FXML
    private Label detailCategory;
    @FXML
    private Label detailStatus;
    @FXML
    private Label detailGoal;
    @FXML
    private Label detailEquity;
    @FXML
    private Label detailCreator;
    @FXML
    private Label detailDesc;
    @FXML
    private Button btnAiExplain;
    @FXML
    private Button btnInvestNow;
    @FXML
    private Button btnTrackProgress;
    @FXML
    private Button btnDealRoom;
    @FXML
    private Button btnCancelOffer;

    private final ProjectService projectService = new ProjectService();
    private final InvestmentService investmentService = new InvestmentService();
    private final MilestoneService milestoneService = new MilestoneService();
    private final CollaborationService collaborationService = new CollaborationService();
    private final UserService userService = new UserService();
    private final List<Integer> notifiedInvestments = new ArrayList<>();

    private edu.connexion3a8.entities.User currentUser;
    private String currentInvestorId = "2"; // Default, will be replaced by setCurrentUser

    private List<Project> allProjects;
    private List<Investment> myInvestments;

    private Project selectedOverlayProject = null;
    private Project focusedProjectForCollab = null;

    public void setCurrentUser(edu.connexion3a8.entities.User user) {
        this.currentUser = user;
        if (user != null) {
            this.currentInvestorId = user.getId();
            // Refresh data with actual user
            refreshProjects();
            refreshPortfolio();
            buildCollaborationPage();
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        // Setup filter boxes
        categoryFilterBox
                .setItems(FXCollections.observableArrayList("All", "Tech", "Health", "Education", "Finance", "Other"));
        categoryFilterBox.setValue("All");

        sortBox.setItems(FXCollections.observableArrayList("Newest First", "Amount: High→Low", "Amount: Low→High",
                "Equity: High→Low", "Equity: Low→High"));
        sortBox.setValue("Newest First");

        currencyBox.setItems(FXCollections.observableArrayList(CurrencyService.getSupportedCurrencies()));
        currencyBox.setValue("USD");

        // Listeners for dynamic filtering
        searchField.textProperty().addListener((obs, o, n) -> applyFilters());
        categoryFilterBox.setOnAction(e -> applyFilters());
        sortBox.setOnAction(e -> applyFilters());
        currencyBox.setOnAction(e -> applyFilters());
        // NOTE: Do NOT call data loaders here — currentInvestorId is null until
        // setCurrentUser() is called by InvestiApp after the FXML loads.
    }

    // ─── Navigation Methods ──────────────────────────────────────────────

    @FXML
    void showBrowsePage() {
        focusedProjectForCollab = null;
        togglePage(browsePage);
    }

    @FXML
    void showCollaborationPage() {
        if (focusedProjectForCollab == null) {
            AlertHelper.showWarning("Select a Project",
                    "Please select 'Track 📊' on an accepted offer to view collaborations.");
            showBrowsePage();
            return;
        }
        hideProjectDetails();
        togglePage(collaborationPage);
        buildCollaborationPage();
    }

    private void togglePage(VBox pageToShow) {
        browsePage.setVisible(false);
        browsePage.setManaged(false);
        collaborationPage.setVisible(false);
        collaborationPage.setManaged(false);

        pageToShow.setVisible(true);
        pageToShow.setManaged(true);
    }

    @FXML
    void logout() {
        // Mock logout
        System.out.println("Logging out...");
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

    // ─── Detail Overlay Logic ───────────────────────────────────────────────────

    private void showProjectDetails(Project p, Investment interaction) {
        selectedOverlayProject = p;
        detailTitle.setText(p.getTitle());
        detailCategory.setText(p.getCategory() != null ? p.getCategory() : "Other");
        detailStatus.setText(p.getStatus());

        String dispCurr = currencyBox.getValue() != null ? currencyBox.getValue() : "USD";
        double convAmt = CurrencyService.convertFromUSD(p.getAmountRequested(), dispCurr);
        detailGoal.setText(CurrencyService.format(convAmt, dispCurr));

        detailEquity.setText(p.getEquityOffered() + "%");
        detailCreator.setText("#" + p.getEntrepreneurId());
        detailDesc.setText(p.getDescription());

        // Buttons configuration
        btnInvestNow.setManaged(true);
        btnInvestNow.setVisible(true);
        btnInvestNow.setDisable(false);
        btnInvestNow.setText("💼 Add Investment Offer");

        btnAiExplain.setManaged(true);
        btnAiExplain.setVisible(true);
        btnAiExplain.setDisable(false);
        btnAiExplain.setText("🤖 AI Setup Explanation & PDF");

        btnTrackProgress.setManaged(false);
        btnTrackProgress.setVisible(false);

        btnDealRoom.setManaged(false);
        btnDealRoom.setVisible(false);

        btnCancelOffer.setManaged(false);
        btnCancelOffer.setVisible(false);

        if (interaction != null) {
            btnInvestNow.setDisable(true);
            btnInvestNow.setText(interaction.getStatus() + " OFFER");

            btnAiExplain.setManaged(false);
            btnAiExplain.setVisible(false);

            if ("ACCEPTED".equalsIgnoreCase(interaction.getStatus())) {
                btnTrackProgress.setManaged(true);
                btnTrackProgress.setVisible(true);
                btnTrackProgress.setOnAction(e -> {
                    focusedProjectForCollab = p;
                    showCollaborationPage();
                });

                btnDealRoom.setManaged(true);
                btnDealRoom.setVisible(true);
                btnDealRoom.setOnAction(e -> openDealRoom(interaction));
            } else {
                btnDealRoom.setManaged(true);
                btnDealRoom.setVisible(true);
                btnDealRoom.setOnAction(e -> openDealRoom(interaction));

                if ("REFUSED".equalsIgnoreCase(interaction.getStatus())) {
                    // Refused: hide Deal Room and Cancel, show refused badge
                    btnDealRoom.setManaged(false);
                    btnDealRoom.setVisible(false);
                    btnCancelOffer.setManaged(false);
                    btnCancelOffer.setVisible(false);
                } else {
                    btnCancelOffer.setManaged(true);
                    btnCancelOffer.setVisible(true);
                    btnCancelOffer.setOnAction(e -> {
                        if (confirm("Cancel Offer", "Are you sure you want to cancel this offer?")) {
                            investmentService.deleteEntity(interaction);
                            refreshProjects();
                            hideProjectDetails();
                        }
                    });
                }
            }

        } else if (!"OPEN".equals(p.getStatus())) {
            btnInvestNow.setDisable(true);
            btnInvestNow.setText("Unavailable (" + p.getStatus() + ")");
        }

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

    @FXML
    void investInOverlayProject() {
        if (selectedOverlayProject != null) {
            openInvestDialog(selectedOverlayProject);
        }
    }

    @FXML
    void generateAiPdfReport() {
        if (selectedOverlayProject == null)
            return;

        btnAiExplain.setDisable(true);
        btnAiExplain.setText("Generating Report... ⏳");

        // Background thread to perform API call & PDF creation without freezing UI
        new Thread(() -> {
            String explanation = AiService.generateProjectExplanation(selectedOverlayProject);
            String pdfPath = PdfExportService.generateProjectAiReport(selectedOverlayProject, explanation);

            Platform.runLater(() -> {
                btnAiExplain.setDisable(false);
                btnAiExplain.setText("✅ Open PDF Report");
                btnAiExplain.setOnAction(e -> {
                    if (pdfPath != null) {
                        try {
                            Desktop.getDesktop().open(new File(pdfPath));
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                });

                // Alert completion
                AlertHelper.showInfo("PDF Ready", "PDF generation successful! Click the button again to view it.");
            });
        }).start();
    }

    // ─── Filtering & Project Cards ──────────────────────────────────────────────

    private void applyFilters() {
        if (allProjects == null)
            return;
        String keyword = searchField.getText().trim().toLowerCase();
        String category = categoryFilterBox.getValue();
        String sort = sortBox.getValue();

        List<Project> filtered = allProjects.stream()
                .filter(p -> "OPEN".equals(p.getStatus())
                        || (myInvestments != null
                                && myInvestments.stream().anyMatch(inv -> inv.getProjectId() == p.getProjectId())))
                .filter(p -> keyword.isEmpty() || p.getTitle().toLowerCase().contains(keyword)
                        || p.getDescription().toLowerCase().contains(keyword))
                .filter(p -> category == null || category.equals("All") || category.equals(p.getCategory()))
                .collect(Collectors.toList());

        if (sort != null) {
            switch (sort) {
                case "Amount: High→Low" ->
                    filtered.sort((a, b) -> Double.compare(b.getAmountRequested(), a.getAmountRequested()));
                case "Amount: Low→High" ->
                    filtered.sort((a, b) -> Double.compare(a.getAmountRequested(), b.getAmountRequested()));
                case "Equity: High→Low" ->
                    filtered.sort((a, b) -> Double.compare(b.getEquityOffered(), a.getEquityOffered()));
                case "Equity: Low→High" ->
                    filtered.sort((a, b) -> Double.compare(a.getEquityOffered(), b.getEquityOffered()));
            }
        }
        renderProjectCards(filtered);
    }

    @FXML
    void refreshProjects() {
        allProjects = projectService.getData();
        refreshPortfolio(); // Must refresh FIRST so myInvestments is up to date for card rendering
        applyFilters(); // This calls renderProjectCards which reads myInvestments
    }

    private void renderProjectCards(List<Project> projects) {
        projectsContainer.getChildren().clear();
        String dispCurr = currencyBox.getValue() != null ? currencyBox.getValue() : "USD";

        if (projects.isEmpty()) {
            projectsContainer.getChildren().add(new Label("No projects found under this search."));
            return;
        }

        for (Project p : projects) {
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

            java.util.Optional<Investment> interaction = myInvestments.stream()
                    .filter(inv -> inv.getProjectId() == p.getProjectId()).findFirst();

            HBox header = new HBox(10);
            Label catBadge = new Label(p.getCategory() != null ? p.getCategory() : "Other");
            catBadge.getStyleClass().add("category-badge");

            Label title = new Label(p.getTitle());
            title.getStyleClass().add("card-title");

            if (interaction.isPresent()) {
                Label interactBadge = new Label(interaction.get().getStatus());
                interactBadge.setStyle(
                        "-fx-font-weight: bold; -fx-padding: 2 6; -fx-background-radius: 4; -fx-text-fill: white; -fx-background-color: "
                                + ("ACCEPTED".equals(interaction.get().getStatus()) ? "#16a34a"
                                        : ("PENDING".equals(interaction.get().getStatus()) ? "#b45309" : "#A62639"))
                                + ";");
                header.getChildren().addAll(catBadge, interactBadge, title);
            } else {
                header.getChildren().addAll(catBadge, title);
            }

            // Momentum badges
            long offerCount = investmentService.getInvestmentsByStatus("PENDING").stream()
                    .filter(inv -> inv.getProjectId() == p.getProjectId()).count();
            if ("OPEN".equals(p.getStatus())) {
                if (offerCount >= 2) {
                    Label hotBadge = new Label("🔥 HOT");
                    hotBadge.getStyleClass().add("badge-hot");
                    header.getChildren().add(hotBadge);
                }
            }

            Label desc = new Label(p.getDescription());
            desc.getStyleClass().add("card-desc");
            desc.setWrapText(true);
            desc.setMinHeight(Region.USE_PREF_SIZE);

            double convAmt = CurrencyService.convertFromUSD(p.getAmountRequested(), dispCurr);
            Label goal = new Label("Goal: " + CurrencyService.format(convAmt, dispCurr));
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
                    new KeyFrame(Duration.millis((projects.indexOf(p) * 100) + 200), e -> auroraAnim.play()));
            delay.play();

            StackPane goalContainer = new StackPane(goal);
            goalContainer.setAlignment(Pos.CENTER_LEFT);

            if (!interaction.isPresent()) {
                HBox actionBox = new HBox(15);
                actionBox.setAlignment(Pos.CENTER_LEFT);

                Button aiAdvisorBtn = new Button("Quick AI Advice?");
                aiAdvisorBtn.setStyle(
                        "-fx-background-color: transparent; -fx-text-fill: #1d4ed8; -fx-font-weight: bold; -fx-font-size: 13px; -fx-cursor: hand; -fx-underline: true;");
                aiAdvisorBtn.setOnAction(e -> {
                    aiAdvisorBtn.setText("Thinking...");
                    aiAdvisorBtn.setDisable(true);
                    new Thread(() -> {
                        String advice = AiService.evaluateProjectForInvestor(p);
                        Platform.runLater(() -> {
                            aiAdvisorBtn.setText(advice);
                            aiAdvisorBtn.setWrapText(true);
                        });
                    }).start();
                });

                actionBox.getChildren().addAll(aiAdvisorBtn);
                card.getChildren().addAll(header, desc, goalContainer, actionBox);
            } else {
                card.getChildren().addAll(header, desc, goalContainer);
            }

            applyMagneticHover(card);

            card.setOnMouseClicked(e -> showProjectDetails(p, interaction.isPresent() ? interaction.get() : null));
            projectsContainer.getChildren().add(card);
        }
    }

    // ─── Portfolio Logic ──────────────────────────────────────────────────

    @FXML
    void refreshPortfolio() {
        myInvestments = investmentService.getInvestmentsByInvestor(currentInvestorId);
    }

    @FXML
    void askAiAdvisor() {
        btnAiAdvisor.setText("Thinking... ⏳");
        btnAiAdvisor.setDisable(true);
        aiAdvisorOutput.setVisible(true);
        aiAdvisorOutput.setManaged(true);
        aiAdvisorOutput.setStyle("-fx-text-fill: #1d4ed8; -fx-font-weight: bold; -fx-font-size: 14px;");
        aiAdvisorOutput.setText("Analyzing your portfolio and the current market...");

        new Thread(() -> {
            StringBuilder pastContext = new StringBuilder();
            if (myInvestments.isEmpty()) {
                pastContext.append(
                        "The investor has no past investments yet. They are looking for their very first opportunity.");
            } else {
                for (Investment i : myInvestments) {
                    if ("REFUSED".equalsIgnoreCase(i.getStatus())) {
                        continue; // Do not tell AI about refused investments to prevent bad advice
                    }
                    Project p = allProjects.stream().filter(proj -> proj.getProjectId() == i.getProjectId()).findFirst()
                            .orElse(null);
                    if (p != null) {
                        pastContext.append("- ").append(p.getTitle()).append(" (Category: ")
                                .append(p.getCategory() != null ? p.getCategory() : "Other").append("). Invested: $")
                                .append(i.getTotalAmount()).append(" (Status: ").append(i.getStatus()).append(")\n");
                    }
                }
            }

            StringBuilder openContext = new StringBuilder();

            // Collect IDs of projects this investor was refused on
            List<Integer> refusedProjectIds = myInvestments.stream()
                    .filter(i -> "REFUSED".equalsIgnoreCase(i.getStatus()))
                    .map(Investment::getProjectId)
                    .collect(Collectors.toList());

            List<Project> openProjects = allProjects.stream()
                    .filter(p -> "OPEN".equals(p.getStatus()))
                    .filter(p -> !refusedProjectIds.contains(p.getProjectId()))
                    .limit(10)
                    .collect(Collectors.toList());

            if (openProjects.isEmpty()) {
                openContext.append("There are currently no open projects available for investment.");
            } else {
                for (Project p : openProjects) {
                    openContext.append("- ").append(p.getTitle())
                            .append(" (Category: ").append(p.getCategory() != null ? p.getCategory() : "Other")
                            .append(")\n");
                }
            }

            String advice = AiService.getPortfolioRecommendations(pastContext.toString(), openContext.toString());

            Platform.runLater(() -> {
                btnAiAdvisor.setText("🤖 Refresh AI Advice");
                btnAiAdvisor.setDisable(false);
                aiAdvisorOutput.setText(advice);
            });
        }).start();
    }

    // ─── Collaboration Page Logic ───────────────────────────────────────────────

    private void buildCollaborationPage() {
        if (collaborationContainer == null)
            return;
        collaborationContainer.getChildren().clear();

        List<Investment> activeCollabs = myInvestments.stream()
                .filter(i -> "ACCEPTED".equalsIgnoreCase(i.getStatus()))
                .filter(i -> focusedProjectForCollab == null
                        || i.getProjectId() == focusedProjectForCollab.getProjectId())
                .collect(Collectors.toList());

        if (focusedProjectForCollab != null) {
            Button showAllBtn = new Button("⬅ Back to Browse");
            showAllBtn.getStyleClass().add("ai-btn");
            showAllBtn.setOnAction(e -> {
                focusedProjectForCollab = null;
                showBrowsePage();
            });
            collaborationContainer.getChildren().add(showAllBtn);
        }

        if (activeCollabs.isEmpty()) {
            collaborationContainer.getChildren()
                    .add(new Label("You have no active collaborations yet. Find a project to fund!"));
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd");

        for (Investment i : activeCollabs) {
            // ─── Get or ensure collaboration record (Safeguard) ────────────────
            edu.connexion3a8.entities.collaboration.Collaboration collab = null;
            try {
                collab = collaborationService.getCollaborationByInvestment(i.getInvestmentId(), i.getInvestorId());
                if (collab == null) {
                    // Logic from EntrepreneurController: Ensure it exists if investment is accepted
                    Project project = projectService.readById(i.getProjectId());
                    edu.connexion3a8.entities.collaboration.Collaboration newCollab = new edu.connexion3a8.entities.collaboration.Collaboration(
                            0, i.getInvestmentId(), project.getEntrepreneurId(), i.getInvestorId(), null, "ACTIVE",
                            100.0, 0.0);
                    collab = collaborationService.createCollaboration(newCollab);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            final int collabId = collab != null ? collab.getId() : -1;

            // ─── Load milestones & progress ───────────────────────────────────
            List<Milestone> milestones = collabId > 0
                    ? milestoneService.getMilestonesForCollaboration(collabId)
                    : new java.util.ArrayList<>();
            double progress = collabId > 0 ? milestoneService.calculateProgress(collabId) : i.getProgressPercentage();

            // ─── Card ─────────────────────────────────────────────────────────
            VBox card = new VBox(20);
            card.getStyleClass().add("progress-card");

            Project proj = allProjects.stream().filter(p -> p.getProjectId() == i.getProjectId()).findFirst()
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

            // ─── Animated Completion Ring (read-only) ─────────────────────────
            StackPane ringPane = buildCompletionRing(progress, pulseColor);

            // ─── Milestone Timeline (read-only, editable=false) ───────────────
            Label msHeader = new Label(
                    "📌 Milestone Timeline" + (milestones.isEmpty() ? "  (awaiting entrepreneur setup)" : ""));
            msHeader.getStyleClass().add("milestone-section-title");

            HBox timelineHBox = buildMilestoneTimeline(milestones, sdf);
            ScrollPane timelineScroll = new ScrollPane(timelineHBox);
            timelineScroll.setFitToWidth(false);
            timelineScroll.setFitToHeight(true);
            timelineScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            timelineScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            timelineScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
            timelineScroll.setPrefHeight(160);

            // ─── Financial + Payment section ──────────────────────────────────
            Separator sep = new Separator();
            GridPane finGrid = new GridPane();
            finGrid.setHgap(30);
            finGrid.setVgap(10);

            finGrid.add(new Label("Total Investment:"), 0, 0);
            Label v1 = new Label("$" + String.format("%.2f", i.getTotalAmount()));
            v1.setStyle("-fx-font-weight: bold;");
            finGrid.add(v1, 1, 0);

            finGrid.add(new Label("Payment Months:"), 0, 1);
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

                if (isLate && !notifiedInvestments.contains(i.getInvestmentId())) {
                    if (proj != null) {
                        String recipientEmail = currentUser.getEmail(); // Notify the current investor
                        edu.connexion3a8.services.collaboration.EmailService.sendLatePaymentWarning(
                                recipientEmail,
                                proj.getTitle(), i.getAmountPerPeriod());
                        notifiedInvestments.add(i.getInvestmentId());
                    }
                }

                Button btnPay = new Button("Mark Month Paid ✅");
                btnPay.getStyleClass().add("ai-btn");

                if (isLate) {
                    Label lateWarning = new Label("⚠️ LATE PAYMENT");
                    lateWarning.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold; -fx-font-size: 11px;");
                    paymentBox.getChildren().add(lateWarning);

                    // Force pulse color to red if late
                    pulseColor = "#dc2626";
                    healthBadge.setText("🔴 Payment Late");
                    healthBadge.getStyleClass().setAll("health-danger");
                }

                if (alreadyPaidThisMonth && lastPay != null) {
                    // Compute and show the next payment window starting date
                    java.time.LocalDate nextDue = lastPay.plusMonths(1)
                            .withDayOfMonth(Math.min(anniversaryDay, lastPay.plusMonths(1).lengthOfMonth()));
                    btnPay.setText("Next payment window: " + nextDue.format(
                            java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy")));
                    btnPay.setDisable(true);
                    btnPay.setStyle("-fx-font-size: 11px;");
                }
                btnPay.setOnAction(e -> {
                    if (confirm("Log Payment", "Mark the next month as paid for this investment?")) {
                        if (investmentService.markPaymentDone(i.getInvestmentId(), monthsPaid + 1)) {
                            if (proj != null) {
                                String entrepreneurEmail = "entrepreneur@example.com";
                                try {
                                    edu.connexion3a8.entities.User entrepreneur = userService
                                            .getUserById(proj.getEntrepreneurId());
                                    if (entrepreneur != null)
                                        entrepreneurEmail = entrepreneur.getEmail();
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }

                                edu.connexion3a8.services.collaboration.EmailService.sendPaymentConfirmation(
                                        entrepreneurEmail, currentUser.getEmail(),
                                        proj.getTitle(), i.getAmountPerPeriod());
                            }
                        }
                        refreshPortfolio();
                        buildCollaborationPage();
                    }
                });
                paymentBox.getChildren().add(btnPay);
            }

            finGrid.add(paymentBox, 1, 1);
            finGrid.add(new Label("Expected Equity:"), 0, 2);
            Label v3 = new Label(i.getEquityRequested() + "% Ownership");
            v3.setStyle("-fx-font-weight: bold; -fx-text-fill: #9B7E46;");
            finGrid.add(v3, 1, 2);

            Button btnDealRoom = new Button("Enter Deal Room 🤝");
            btnDealRoom.getStyleClass().add("ai-btn");
            btnDealRoom.setOnAction(e -> openDealRoom(i));
            finGrid.add(btnDealRoom, 0, 3);

            card.getChildren().addAll(titleRow, ringPane, msHeader, timelineScroll, sep, finGrid);
            collaborationContainer.getChildren().add(card);
        }
    }

    // ─── Completion Ring (same as Entrepreneur) ─────────────────────
    private StackPane buildCompletionRing(double progress, String color) {
        Arc track = new Arc(0, 0, 56, 56, 90, -360);
        track.setType(ArcType.OPEN);
        track.setFill(Color.TRANSPARENT);
        track.setStroke(Color.web("#5a7b93", 0.3)); // Muted Baltic Blue
        track.setStrokeWidth(10);

        Arc fill = new Arc(0, 0, 56, 56, 90, 0);
        fill.setType(ArcType.OPEN);
        fill.setFill(Color.TRANSPARENT);
        fill.setStroke(Color.web(color));
        fill.setStrokeWidth(10);
        fill.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);

        new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(fill.lengthProperty(), 0)),
                new KeyFrame(Duration.millis(900),
                        new KeyValue(fill.lengthProperty(), -3.6 * progress, Interpolator.EASE_OUT)))
                .play();

        VBox center = new VBox(2);
        center.setAlignment(Pos.CENTER);
        Label pct = new Label("0%");
        pct.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");

        // Count-up label animation (synced with arc)
        javafx.beans.property.DoubleProperty pctProp = new javafx.beans.property.SimpleDoubleProperty(0);
        pctProp.addListener((obs, ov, nv) -> pct.setText(String.format("%.0f%%", nv.doubleValue())));
        Timeline countUp = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(pctProp, 0)),
                new KeyFrame(Duration.millis(900), new KeyValue(pctProp, progress, Interpolator.EASE_OUT)));
        countUp.play();

        Label sub = new Label("Complete");
        sub.setStyle("-fx-font-size: 11px; -fx-text-fill: #5a7b93;");
        center.getChildren().addAll(pct, sub);

        // Wrap in a Group so the Arc doesn't shift its center when animating
        javafx.scene.Group group = new javafx.scene.Group(track, fill);

        StackPane ring = new StackPane(group, center);
        ring.setAlignment(Pos.CENTER);
        ring.setMinSize(140, 140);
        ring.setMaxSize(140, 140);
        return ring;
    }

    // ─── Read-only Milestone Timeline ────────────────────────────────────────
    private HBox buildMilestoneTimeline(List<Milestone> milestones, SimpleDateFormat sdf) {
        HBox timeline = new HBox();
        timeline.setAlignment(Pos.CENTER_LEFT);
        timeline.setPadding(new javafx.geometry.Insets(20, 10, 10, 10));
        timeline.setSpacing(0);

        if (milestones.isEmpty()) {
            Label emptyLbl = new Label("No milestones set yet — entrepreneur will add them.");
            emptyLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-style: italic;");
            timeline.getChildren().add(emptyLbl);
            return timeline;
        }

        for (int idx = 0; idx < milestones.size(); idx++) {
            Milestone m = milestones.get(idx);
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

                    nodeLabel = new Label("▶");
                    nodeLabel.setStyle("-fx-text-fill: #c07c4b; -fx-font-size: 14px; -fx-font-weight: bold;");
                }
                default -> {
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
            timeline.getChildren().add(nodeCol);

            if (idx < milestones.size() - 1) {
                Line connector = new Line(0, 0, 50, 0);
                connector.setStrokeWidth(3);
                connector.setStroke("COMPLETED".equals(m.getStatus()) ? Color.web("#2a506b") : Color.web("#e2e8f0"));
                VBox connWrapper = new VBox(connector);
                connWrapper.setAlignment(Pos.CENTER);
                connWrapper.setPadding(new javafx.geometry.Insets(0, 0, 30, 0));
                timeline.getChildren().add(connWrapper);
            }
        }
        return timeline;
    }

    private void openInvestDialog(Project p) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/collaboration/AddInvestment.fxml"));
            Parent root = loader.load();
            Object controller = loader.getController();
            if (controller instanceof AddInvestmentController) {
                AddInvestmentController addInvController = (AddInvestmentController) controller;
                addInvController.setTargetProjectId(p.getProjectId());
                addInvController.setCurrentUser(currentUser);
            }
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Invest in " + p.getTitle());
            stage.showAndWait();
            hideProjectDetails();
            refreshPortfolio();
            refreshProjects();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean confirm(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initStyle(javafx.stage.StageStyle.UNDECORATED);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(content);
        alert.getDialogPane().getStylesheets().add(
                getClass().getResource("/collaboration/styles_premium.css").toExternalForm());
        alert.getDialogPane().getStyleClass().add("styled-dialog");
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private void openDealRoom(Investment inv) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/collaboration/DealRoom.fxml"));
            javafx.scene.Parent root = loader.load();
            edu.connexion3a8.controllers.collaboration.DealRoomController controller = loader.getController();

            Project p = allProjects.stream().filter(proj -> proj.getProjectId() == inv.getProjectId()).findFirst()
                    .orElse(null);
            if (p == null) {
                new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR,
                        "Could not find project #" + inv.getProjectId() + " in the loaded projects list.")
                        .showAndWait();
                return;
            }
            controller.initData(inv, p, currentInvestorId);
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setScene(new javafx.scene.Scene(root));
            stage.setTitle("Deal Room - " + p.getTitle());
            stage.showAndWait();
            refreshPortfolio();
            buildCollaborationPage();
        } catch (Exception e) {
            e.printStackTrace();
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR,
                    "Could not open Deal Room:\n" + e.getClass().getSimpleName() + ": " + e.getMessage()).showAndWait();
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
