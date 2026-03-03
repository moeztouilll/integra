package edu.connexion3a8.controllers.collaboration;

import edu.connexion3a8.entities.User;
import edu.connexion3a8.entities.collaboration.Investment;
import edu.connexion3a8.entities.collaboration.Project;
import edu.connexion3a8.services.collaboration.EmailService;
import edu.connexion3a8.services.collaboration.InvestmentService;
import edu.connexion3a8.services.collaboration.ProjectService;
import edu.connexion3a8.services.UserService;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.util.Duration;

public class AdminController implements Initializable {

    private User currentUser;

    // ─── Pages ─────────────────────────────────────────────────────────────────
    @FXML
    private VBox queuePage;
    @FXML
    private VBox projectsPage;
    @FXML
    private VBox investmentsPage;
    @FXML
    private VBox statsPage;

    // ─── Nav Buttons ───────────────────────────────────────────────────────────
    @FXML
    private Button navQueue;
    @FXML
    private Button navProjects;
    @FXML
    private Button navInvestments;
    @FXML
    private Button navStats;

    // ─── Action Badge ────────────────────────────────────────────────────────────
    @FXML
    private Label actionRequiredBadge;

    // ─── Tab 1: Validation Queue ───────────────────────────────────────────
    @FXML
    private TableView<Project> projectTableQueue;
    @FXML
    private TableColumn<Project, Integer> colProjectIdQ;
    @FXML
    private TableColumn<Project, String> colProjectTitleQ;
    @FXML
    private TableColumn<Project, String> colProjectDescQ;
    @FXML
    private TableColumn<Project, Integer> colProjectRepoQ;
    @FXML
    private TableColumn<Project, String> colProjectStatusQ;

    @FXML
    private TableView<Investment> investmentTableQueue;
    @FXML
    private TableColumn<Investment, Integer> colInvIdQ;
    @FXML
    private TableColumn<Investment, String> colInvProjectQ;
    @FXML
    private TableColumn<Investment, Integer> colInvInvestorQ;
    @FXML
    private TableColumn<Investment, Double> colInvAmountQ;
    @FXML
    private TableColumn<Investment, Double> colInvEquityQ;
    @FXML
    private TableColumn<Investment, String> colInvStatusQ;

    // ─── Tab 2: All Projects + Search ─────────────────────────────────────
    @FXML
    private TableView<Project> projectTableAll;
    @FXML
    private TableColumn<Project, Integer> colProjectIdAll;
    @FXML
    private TableColumn<Project, String> colProjectTitleAll;
    @FXML
    private TableColumn<Project, String> colProjectDescAll;
    @FXML
    private TableColumn<Project, Integer> colProjectRepoAll;
    @FXML
    private TableColumn<Project, String> colProjectStatusAll;
    @FXML
    private TextField searchProjectsField;

    // ─── Tab 3: All Investments + Search ──────────────────────────────────
    @FXML
    private TableView<Investment> investmentTableAll;
    @FXML
    private TableColumn<Investment, Integer> colInvIdAll;
    @FXML
    private TableColumn<Investment, String> colInvProjectAll;
    @FXML
    private TableColumn<Investment, Integer> colInvInvestorAll;
    @FXML
    private TableColumn<Investment, Double> colInvAmountAll;
    @FXML
    private TableColumn<Investment, Double> colInvEquityAll;
    @FXML
    private TableColumn<Investment, String> colInvStatusAll;
    @FXML
    private TextField searchInvestmentsField;

    // ─── Tab 4: Statistics ────────────────────────────────────────────────
    @FXML
    private VBox statsContainer;

    private final ProjectService projectService = new ProjectService();
    private final InvestmentService investmentService = new InvestmentService();
    private final UserService userService = new UserService();

    private List<Project> allProjectsList;
    private List<Investment> allInvestmentsList;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTables();
        refreshData();

        if (searchProjectsField != null) {
            searchProjectsField.textProperty().addListener((obs, o, n) -> applyProjectSearch(n));
        }
        if (searchInvestmentsField != null) {
            searchInvestmentsField.textProperty().addListener((obs, o, n) -> applyInvestmentSearch(n));
        }
    }

    // ─── Sidebar Navigation ─────────────────────────────────────────────────────

    @FXML
    void showQueuePage() {
        togglePage(queuePage, navQueue);
    }

    @FXML
    void showProjectsPage() {
        togglePage(projectsPage, navProjects);
    }

    @FXML
    void showInvestmentsPage() {
        togglePage(investmentsPage, navInvestments);
    }

    @FXML
    void showStatsPage() {
        togglePage(statsPage, navStats);
    }

    private void togglePage(VBox pageToShow, Button activeNavBtn) {
        VBox[] pages = { queuePage, projectsPage, investmentsPage, statsPage };
        for (VBox p : pages) {
            p.setVisible(false);
            p.setManaged(false);
        }

        navQueue.getStyleClass().remove("active");
        navProjects.getStyleClass().remove("active");
        navInvestments.getStyleClass().remove("active");
        navStats.getStyleClass().remove("active");

        pageToShow.setVisible(true);
        pageToShow.setManaged(true);
        pageToShow.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(pageToShow, javafx.scene.layout.Priority.ALWAYS);
        activeNavBtn.getStyleClass().add("active");
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

    // ─── Search Logic ──────────────────────────────────────────────────────────

    private void applyProjectSearch(String keyword) {
        if (allProjectsList == null)
            return;
        if (keyword == null || keyword.isBlank()) {
            projectTableAll.setItems(FXCollections.observableArrayList(allProjectsList));
            return;
        }
        String kw = keyword.toLowerCase();
        List<Project> filtered = allProjectsList.stream()
                .filter(p -> p.getTitle().toLowerCase().contains(kw) || p.getDescription().toLowerCase().contains(kw)
                        || p.getStatus().toLowerCase().contains(kw) || String.valueOf(p.getProjectId()).contains(kw))
                .collect(Collectors.toList());
        projectTableAll.setItems(FXCollections.observableArrayList(filtered));
    }

    private void applyInvestmentSearch(String keyword) {
        if (allInvestmentsList == null)
            return;
        if (keyword == null || keyword.isBlank()) {
            investmentTableAll.setItems(FXCollections.observableArrayList(allInvestmentsList));
            return;
        }
        String kw = keyword.toLowerCase();
        List<Investment> filtered = allInvestmentsList.stream()
                .filter(i -> String.valueOf(i.getProjectId()).contains(kw)
                        || String.valueOf(i.getInvestorId()).contains(kw)
                        || i.getStatus().toLowerCase().contains(kw) || String.valueOf(i.getTotalAmount()).contains(kw))
                .collect(Collectors.toList());
        investmentTableAll.setItems(FXCollections.observableArrayList(filtered));
    }

    // ─── Table Setup ───────────────────────────────────────────────────────────

    private void setupTables() {
        setupProjectColumns(colProjectIdQ, colProjectTitleQ, colProjectDescQ, colProjectRepoQ, colProjectStatusQ);
        setupProjectColumns(colProjectIdAll, colProjectTitleAll, colProjectDescAll, colProjectRepoAll,
                colProjectStatusAll);
        setupInvestmentColumns(colInvIdQ, colInvProjectQ, colInvInvestorQ, colInvAmountQ, colInvEquityQ, colInvStatusQ);
        setupInvestmentColumns(colInvIdAll, colInvProjectAll, colInvInvestorAll, colInvAmountAll, colInvEquityAll,
                colInvStatusAll);

        applyTableStyles(projectTableQueue);
        applyTableStyles(projectTableAll);
        applyTableStyles(investmentTableQueue);
        applyTableStyles(investmentTableAll);
    }

    private <T> void applyTableStyles(TableView<T> tv) {
        tv.setRowFactory(table -> new TableRow<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                    return;
                }
                String status = "";
                if (item instanceof Project)
                    status = ((Project) item).getStatus();
                else if (item instanceof Investment)
                    status = ((Investment) item).getStatus();

                String base;
                if ("UNDER_REVIEW".equals(status))
                    base = "-fx-background-color: #fffbee;";
                else if ("OPEN".equals(status) || "ACCEPTED".equals(status) || "PENDING".equals(status))
                    base = "-fx-background-color: #eff6ff;";
                else if ("REFUSED".equals(status))
                    base = "-fx-background-color: #fef2f2; -fx-opacity: 0.8;";
                else
                    base = "";

                if (isSelected()) {
                    // Selection overrides the base with a distinct Baltic Blue highlight
                    setStyle("-fx-background-color: #2a506b; -fx-text-fill: #e6e6fa; -fx-font-weight: bold;");
                } else {
                    setStyle(base);
                }
            }

            @Override
            public void updateSelected(boolean selected) {
                super.updateSelected(selected);
                // Re-apply styles when selection changes
                updateItem(getItem(), isEmpty());
            }
        });
    }

    private void setupProjectColumns(TableColumn<Project, Integer> id, TableColumn<Project, String> title,
            TableColumn<Project, String> desc, TableColumn<Project, Integer> repo,
            TableColumn<Project, String> status) {
        if (id != null)
            id.setCellValueFactory(new PropertyValueFactory<>("projectId"));
        if (title != null)
            title.setCellValueFactory(new PropertyValueFactory<>("title"));
        if (desc != null)
            desc.setCellValueFactory(new PropertyValueFactory<>("description"));
        if (repo != null)
            repo.setCellValueFactory(new PropertyValueFactory<>("entrepreneurId"));
        if (status != null)
            status.setCellValueFactory(new PropertyValueFactory<>("status"));
    }

    private void setupInvestmentColumns(TableColumn<Investment, Integer> id, TableColumn<Investment, String> pid,
            TableColumn<Investment, Integer> iid, TableColumn<Investment, Double> amt,
            TableColumn<Investment, Double> eq, TableColumn<Investment, String> stat) {
        if (id != null)
            id.setCellValueFactory(new PropertyValueFactory<>("investmentId"));
        if (pid != null) {
            pid.setCellValueFactory(cellData -> {
                Project p = projectService.readById(cellData.getValue().getProjectId());
                return new javafx.beans.property.SimpleStringProperty(p != null ? p.getTitle() : "Unknown");
            });
        }
        if (iid != null)
            iid.setCellValueFactory(new PropertyValueFactory<>("investorId"));
        if (amt != null)
            amt.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        if (eq != null)
            eq.setCellValueFactory(new PropertyValueFactory<>("equityRequested"));
        if (stat != null)
            stat.setCellValueFactory(new PropertyValueFactory<>("status"));
    }

    // ─── Actions & Stats ───────────────────────────────────────────────────────

    @FXML
    void refreshDataAction() {
        refreshData();
    }

    private void refreshData() {
        allProjectsList = projectService.getData();
        projectTableAll.setItems(FXCollections.observableArrayList(allProjectsList));
        projectTableQueue.setItems(FXCollections.observableArrayList(
                allProjectsList.stream().filter(p -> "UNDER_REVIEW".equals(p.getStatus()))
                        .collect(Collectors.toList())));

        allInvestmentsList = investmentService.getData();
        investmentTableAll.setItems(FXCollections.observableArrayList(allInvestmentsList));
        List<Investment> pendingInvestments = allInvestmentsList.stream()
                .filter(i -> "UNDER_REVIEW".equals(i.getStatus()))
                .collect(Collectors.toList());
        investmentTableQueue.setItems(FXCollections.observableArrayList(pendingInvestments));

        // Show/hide ACTION REQUIRED badge based on actual pending count
        if (actionRequiredBadge != null) {
            boolean hasPending = !projectTableQueue.getItems().isEmpty() || !pendingInvestments.isEmpty();
            actionRequiredBadge.setVisible(hasPending);
            actionRequiredBadge.setManaged(hasPending);
        }

        loadStats();
    }

    private void loadStats() {
        if (statsContainer == null)
            return;
        statsContainer.getChildren().clear();

        int total = projectService.getTotalProjectCount();
        int funded = projectService.getFundedProjectCount();
        int open = projectService.getOpenProjectCount();
        int invCount = investmentService.getTotalInvestmentCount();
        double invVolume = investmentService.getTotalInvestedVolume();
        double fundedPct = total > 0 ? (funded * 100.0 / total) : 0;
        double avgDealSize = invCount > 0 ? (invVolume / invCount) : 0;
        long activeCollabs = allInvestmentsList.stream().filter(i -> "ACCEPTED".equals(i.getStatus())).count();

        // ── Row 1: 4 stat cards in an HBox ──────────────────────────────────
        HBox row1 = new HBox(20);
        row1.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(buildStatCard("Total Projects", total, "", "", "#2563eb"), javafx.scene.layout.Priority.ALWAYS);
        VBox c1 = buildStatCard("Total Projects", total, "", "", "#2563eb");
        VBox c2 = buildStatCard("Open Projects", open, "", "", "#16a34a");
        VBox c3 = buildStatCard("Funded Projects", funded, "", "", "#0891b2");
        VBox c4 = buildStatCard("Success Rate", fundedPct, "", "%", "#7c3aed");
        for (VBox c : new VBox[] { c1, c2, c3, c4 }) {
            c.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(c, javafx.scene.layout.Priority.ALWAYS);
        }
        row1.getChildren().addAll(c1, c2, c3, c4);

        // ── Row 2: 4 stat cards in an HBox ──────────────────────────────────
        HBox row2 = new HBox(20);
        row2.setMaxWidth(Double.MAX_VALUE);
        VBox c5 = buildStatCard("Total Investments", invCount, "", "", "#ca8a04");
        VBox c6 = buildStatCard("Capital Injected", invVolume, "$", "", "#dc2626");
        VBox c7 = buildStatCard("Avg Deal Size", avgDealSize, "$", "", "#0f766e");
        VBox c8 = buildStatCard("Active Collabs", activeCollabs, "", "", "#7c3aed");
        for (VBox c : new VBox[] { c5, c6, c7, c8 }) {
            c.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(c, javafx.scene.layout.Priority.ALWAYS);
        }
        row2.getChildren().addAll(c5, c6, c7, c8);

        // ── Bottom Row: KPI + Funding Momentum ───────────────────────────────
        HBox kpiRow = new HBox(25);
        kpiRow.setMaxWidth(Double.MAX_VALUE);

        // Ecosystem Health card
        VBox healthCard = new VBox(12);
        healthCard.getStyleClass().addAll("card-l1", "glass-card");
        healthCard.setStyle("-fx-padding: 24;");
        healthCard.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(healthCard, javafx.scene.layout.Priority.ALWAYS);
        Label healthTitle = new Label("Ecosystem Health");
        healthTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -app-text;");
        HBox kpiBox = new HBox(30);
        kpiBox.setAlignment(javafx.geometry.Pos.CENTER);
        kpiBox.setMinHeight(200);
        long openCount = allProjectsList.stream().filter(p -> "OPEN".equals(p.getStatus())).count();
        long underReviewCount = allProjectsList.stream().filter(p -> "UNDER_REVIEW".equals(p.getStatus())).count();
        long closedCount = allProjectsList.stream().filter(p -> "CLOSED".equals(p.getStatus())
                || "FUNDED".equals(p.getStatus()) || "VALIDATED".equals(p.getStatus())).count();
        long totalStr = openCount + underReviewCount + closedCount;
        double openPct = totalStr == 0 ? 0 : (double) openCount / totalStr * 100;
        double reviewPct = totalStr == 0 ? 0 : (double) underReviewCount / totalStr * 100;
        double closedPct = totalStr == 0 ? 0 : (double) closedCount / totalStr * 100;
        kpiBox.getChildren().addAll(
                buildRadialKPI(openPct, "#16a34a", "Open"),
                buildRadialKPI(reviewPct, "#ca8a04", "Reviewing"),
                buildRadialKPI(closedPct, "#2563eb", "Closed"));
        healthCard.getChildren().addAll(healthTitle, kpiBox);

        // Funding Momentum card — premium version with ⓘ button
        VBox fundingCard = new VBox(14);
        fundingCard.getStyleClass().addAll("card-l1", "glass-card");
        fundingCard.setStyle("-fx-padding: 24;");
        fundingCard.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(fundingCard, javafx.scene.layout.Priority.ALWAYS);

        // Header row with title + ⓘ info button
        HBox fundingHeader = new HBox(10);
        fundingHeader.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label fundingTitle = new Label("\uD83D\uDCB0 Funding Momentum");
        fundingTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -app-text;");
        javafx.scene.control.Button infoBtn = new javafx.scene.control.Button("\u24d8");
        infoBtn.setStyle("-fx-background-color: #e2e8f0; -fx-text-fill: #475569; -fx-font-size: 11px; " +
                "-fx-font-weight: bold; -fx-background-radius: 100; -fx-min-width: 22; -fx-min-height: 22; " +
                "-fx-max-width: 22; -fx-max-height: 22; -fx-padding: 0; -fx-cursor: hand;");
        infoBtn.setOnAction(e -> showFundingMomentumInfo());
        javafx.scene.control.Tooltip.install(infoBtn,
                new javafx.scene.control.Tooltip("What is Funding Momentum?"));
        Region hSpacer = new Region();
        HBox.setHgrow(hSpacer, javafx.scene.layout.Priority.ALWAYS);
        fundingHeader.getChildren().addAll(fundingTitle, hSpacer, infoBtn);

        double totalRequested = allProjectsList.stream().mapToDouble(Project::getAmountRequested).sum();
        double pct = totalRequested > 0 ? Math.min(100.0, invVolume / totalRequested * 100.0) : 0;
        String pctColor = pct >= 75 ? "#16a34a" : pct >= 40 ? "#ca8a04" : "#2563eb";
        String statusText = pct >= 100 ? "FULLY FUNDED" : pct >= 75 ? "STRONG" : pct >= 40 ? "GROWING" : "EARLY STAGE";

        // Status pill
        Label badge = new Label(String.format("%.1f%%  \u00b7  %s", pct, statusText));
        badge.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + pctColor +
                "; -fx-background-color: " + pctColor + "22; -fx-padding: 5 14; -fx-background-radius: 20;");

        // Arc-based radial chart — use Pane (not StackPane) so arc centers are correct
        double arcSize = 160;
        double radius = 62;
        double sw = 14;
        double cx = arcSize / 2;
        double cy = arcSize / 2;
        javafx.scene.shape.Arc track = new javafx.scene.shape.Arc(cx, cy, radius, radius, 210, -240);
        track.setType(javafx.scene.shape.ArcType.OPEN);
        track.setFill(javafx.scene.paint.Color.TRANSPARENT);
        track.setStroke(javafx.scene.paint.Color.web("#e2e8f0"));
        track.setStrokeWidth(sw);
        track.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        javafx.beans.property.DoubleProperty arcProp = new javafx.beans.property.SimpleDoubleProperty(0);
        javafx.scene.shape.Arc fillArc = new javafx.scene.shape.Arc(cx, cy, radius, radius, 210, 0);
        fillArc.setType(javafx.scene.shape.ArcType.OPEN);
        fillArc.setFill(javafx.scene.paint.Color.TRANSPARENT);
        fillArc.setStroke(javafx.scene.paint.Color.web(pctColor));
        fillArc.setStrokeWidth(sw);
        fillArc.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        arcProp.addListener((obs, o, n) -> fillArc.setLength(-(n.doubleValue() / 100.0) * 240));
        new Timeline(new KeyFrame(Duration.ZERO, new KeyValue(arcProp, 0)),
                new KeyFrame(Duration.millis(1400),
                        new KeyValue(arcProp, pct, Interpolator.SPLINE(0.25, 0.1, 0.25, 1))))
                .play();
        // Centre label placed via StackPane overlay on top of the Pane
        Label arcPctLbl = new Label("0%");
        arcPctLbl.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + pctColor + ";");
        arcProp.addListener((obs, o, n) -> arcPctLbl.setText(String.format("%.0f%%", n.doubleValue())));
        Label arcSubLbl = new Label("funded");
        arcSubLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
        VBox arcCentre = new VBox(2, arcPctLbl, arcSubLbl);
        arcCentre.setAlignment(javafx.geometry.Pos.CENTER);
        // Pane holds the arcs, StackPane overlays the text label
        javafx.scene.layout.Pane arcCanvas = new javafx.scene.layout.Pane(track, fillArc);
        arcCanvas.setPrefSize(arcSize, arcSize);
        arcCanvas.setMinSize(arcSize, arcSize);
        arcCanvas.setMaxSize(arcSize, arcSize);
        StackPane arcPane = new StackPane(arcCanvas, arcCentre);
        arcPane.setAlignment(javafx.geometry.Pos.CENTER);
        arcPane.setMinSize(arcSize, arcSize);
        arcPane.setPrefSize(arcSize, arcSize);

        // Two capital metric rows beside the arc
        VBox metricsBox = new VBox(18);
        metricsBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        HBox.setHgrow(metricsBox, javafx.scene.layout.Priority.ALWAYS);

        // Target Capital entry
        VBox targetEntry = new VBox(2);
        Label targetCaption = new Label("🎯  Total Target Capital");
        targetCaption.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        Label targetVal = new Label(String.format("%,.0f DT", totalRequested));
        targetVal.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        targetEntry.getChildren().addAll(targetCaption, targetVal);

        // Injected Capital entry
        VBox injEntry = new VBox(2);
        Label injCaption = new Label("💵  Capital Injected");
        injCaption.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        Label injVal = new Label(String.format("%,.0f DT", invVolume));
        injVal.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + pctColor + ";");
        injEntry.getChildren().addAll(injCaption, injVal);

        // Animated thin progress bar at bottom
        ProgressBar thinBar = new ProgressBar(0);
        thinBar.setMaxWidth(Double.MAX_VALUE);
        thinBar.setPrefHeight(10);
        thinBar.setStyle("-fx-background-radius: 8; -fx-accent: " + pctColor + ";");
        javafx.beans.property.DoubleProperty barProp = new javafx.beans.property.SimpleDoubleProperty(0);
        barProp.addListener((obs, o, n) -> thinBar.setProgress(n.doubleValue() / 100.0));
        new Timeline(new KeyFrame(Duration.ZERO, new KeyValue(barProp, 0)),
                new KeyFrame(Duration.millis(1400),
                        new KeyValue(barProp, pct, Interpolator.SPLINE(0.25, 0.1, 0.25, 1))))
                .play();

        metricsBox.getChildren().addAll(targetEntry, injEntry, thinBar);

        HBox arcAndMetrics = new HBox(30, arcPane, metricsBox);
        arcAndMetrics.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        arcAndMetrics.setMaxWidth(Double.MAX_VALUE);

        fundingCard.getChildren().addAll(fundingHeader, badge, arcAndMetrics);

        kpiRow.getChildren().addAll(healthCard, fundingCard);

        statsContainer.getChildren().addAll(row1, row2, kpiRow);
    }

    // buildFundingMomentumPanel removed — now built inline in loadStats()

    private VBox buildStatCard(String label, double targetValue, String prefix, String suffix, String colorCode) {
        VBox card = new VBox(5);
        card.setAlignment(Pos.CENTER);
        card.setPrefSize(260, 130);
        card.setMinSize(220, 130);
        card.setMaxSize(300, 150);
        card.getStyleClass().addAll("project-card", "card-l1", "glass-card");
        card.setCache(true);
        card.setCacheHint(javafx.scene.CacheHint.SPEED);

        javafx.scene.effect.InnerShadow glassEdge = new javafx.scene.effect.InnerShadow();
        glassEdge.setRadius(8);
        glassEdge.setColor(Color.rgb(255, 255, 255, 0.4));
        card.setEffect(glassEdge);

        Label val = new Label(prefix + "0" + suffix);
        val.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: " + colorCode + ";");
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-muted-foreground;");
        lbl.setWrapText(true);
        lbl.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        card.getChildren().addAll(val, lbl);

        // Timeline Count-Up Animation
        javafx.beans.property.DoubleProperty valueProperty = new javafx.beans.property.SimpleDoubleProperty(0);
        valueProperty.addListener((obs, oldVal, newVal) -> {
            if (targetValue % 1 == 0 && targetValue < 10000 && suffix.isEmpty()) {
                val.setText(prefix + String.format("%.0f", newVal.doubleValue()) + suffix);
            } else if (targetValue >= 10000 || prefix.equals("$")) {
                val.setText(prefix + String.format("%,.0f", newVal.doubleValue()) + suffix);
            } else {
                val.setText(prefix + String.format("%.1f", newVal.doubleValue()) + suffix);
            }
        });

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(valueProperty, 0)),
                new KeyFrame(Duration.millis(1200),
                        new KeyValue(valueProperty, targetValue, Interpolator.SPLINE(0.25, 0.1, 0.25, 1))));
        timeline.play();
        return card;
    }

    private StackPane buildRadialKPI(double percentage, String color, String title) {
        double radius = 50;
        double sw = 10;

        Arc track = new Arc(0, 0, radius, radius, 90, -360);
        track.setType(ArcType.OPEN);
        track.setFill(Color.TRANSPARENT);
        track.setStroke(Color.web("#e2e8f0"));
        track.setStrokeWidth(sw);

        Arc fill = new Arc(0, 0, radius, radius, 90, 0);
        fill.setType(ArcType.OPEN);
        fill.setFill(Color.TRANSPARENT);
        fill.setStroke(Color.web(color));
        fill.setStrokeWidth(sw);
        fill.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);

        // Spline-based motion for drawing
        Timeline drawAnim = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(fill.lengthProperty(), 0)),
                new KeyFrame(Duration.millis(1400),
                        new KeyValue(fill.lengthProperty(), -3.6 * percentage,
                                Interpolator.SPLINE(0.25, 0.1, 0.25, 1))));
        drawAnim.play();

        VBox centerLabels = new VBox(2);
        centerLabels.setAlignment(Pos.CENTER);

        Label pctNum = new Label(String.format("%.0f%%", percentage));
        pctNum.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: -app-muted-foreground; -fx-font-weight: bold;");

        centerLabels.getChildren().addAll(pctNum, titleLbl);

        javafx.scene.Group group = new javafx.scene.Group(track, fill);
        StackPane kpiPane = new StackPane(group, centerLabels);
        kpiPane.setMinSize(radius * 2 + sw * 2, radius * 2 + sw * 2);

        return kpiPane;
    }

    // ─── Queue Approvals & Deletions ───────────────────────────────────────────

    @FXML
    void validateProject() {
        Project selected = projectTableQueue.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;
        if (confirm("Validate Project", "Approve '" + selected.getTitle() + "'?")) {
            selected.setStatus("OPEN");
            if (projectService.update(selected.getProjectId(), selected)) {
                try {
                    edu.connexion3a8.entities.User entrepreneur = userService.getUserById(selected.getEntrepreneurId());
                    if (entrepreneur != null) {
                        EmailService.sendProjectValidated(entrepreneur.getEmail(), selected.getTitle());
                    } else {
                        System.err.println("[AdminController] Could not find entrepreneur for project: "
                                + selected.getProjectId());
                    }
                } catch (Exception e) {
                    System.err.println("[AdminController] Email lookup failed: " + e.getMessage());
                }
                showAlert(Alert.AlertType.INFORMATION, "Success", "Project validated and is now OPEN! ✅");
                refreshData();
            }
        }
    }

    @FXML
    void deleteProjectQueue() {
        deleteProject(projectTableQueue);
    }

    @FXML
    void deleteProjectAll() {
        deleteProject(projectTableAll);
    }

    private void deleteProject(TableView<Project> table) {
        Project selected = table.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;
        if ("FUNDED".equalsIgnoreCase(selected.getStatus()) || "CLOSED".equalsIgnoreCase(selected.getStatus())) {
            showAlert(Alert.AlertType.ERROR, "Forbidden", "Cannot delete a funded or closed project.");
            return;
        }
        if (confirm("Delete Project", "Permanently reject and delete '" + selected.getTitle() + "'?")) {
            // Send rejection email if it was still in the validation queue (not already
            // open)
            if ("UNDER_REVIEW".equalsIgnoreCase(selected.getStatus())
                    || "PENDING".equalsIgnoreCase(selected.getStatus())) {
                try {
                    edu.connexion3a8.entities.User entrepreneur = userService.getUserById(selected.getEntrepreneurId());
                    if (entrepreneur != null) {
                        EmailService.sendProjectRejected(entrepreneur.getEmail(), selected.getTitle());
                    }
                } catch (Exception e) {
                    System.err.println("[AdminController] Email lookup failed: " + e.getMessage());
                }
            }
            projectService.deleteEntity(selected);
            refreshData();
        }
    }

    @FXML
    void validateInvestment() {
        Investment selected = investmentTableQueue.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;
        if (confirm("Validate Investment", "Approve this investment?")) {
            selected.setStatus("PENDING");
            if (investmentService.update(selected.getInvestmentId(), selected)) {

                // Get project and emails for notifications
                Project p = projectService.getData().stream()
                        .filter(proj -> proj.getProjectId() == selected.getProjectId())
                        .findFirst().orElse(null);
                String pTitle = p != null ? p.getTitle() : "Investment";

                try {
                    // Notify Entrepreneur of new validated offer (look up by project owner)
                    if (p != null) {
                        edu.connexion3a8.entities.User entrepreneur = userService.getUserById(p.getEntrepreneurId());
                        if (entrepreneur != null) {
                            EmailService.sendNewInvestmentOffer(entrepreneur.getEmail(), pTitle,
                                    selected.getTotalAmount());
                        }
                    }
                    // Notify Investor that their offer passed admin review (look up by investor
                    // UUID)
                    edu.connexion3a8.entities.User investor = userService.getUserById(selected.getInvestorId());
                    if (investor != null) {
                        EmailService.sendInvestmentApprovedByAdmin(investor.getEmail(), pTitle);
                    }
                } catch (Exception e) {
                    System.err.println("[AdminController] Email lookup failed: " + e.getMessage());
                }

                showAlert(Alert.AlertType.INFORMATION, "Success", "Investment validated and sent to Entrepreneur. ✅");
                refreshData();
            }
        }
    }

    @FXML
    void deleteInvestmentQueue() {
        deleteInvestment(investmentTableQueue);
    }

    @FXML
    void deleteInvestmentAll() {
        deleteInvestment(investmentTableAll);
    }

    private void deleteInvestment(TableView<Investment> table) {
        Investment selected = table.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;
        if ("ACCEPTED".equalsIgnoreCase(selected.getStatus())) {
            showAlert(Alert.AlertType.ERROR, "Forbidden", "Cannot delete an ACCEPTED investment.");
            return;
        }
        if (confirm("Delete Investment", "Permanently reject and delete this investment?")) {

            // Send rejection email if it was in the admin queue
            if ("UNDER_REVIEW".equalsIgnoreCase(selected.getStatus())) {
                Project p = projectService.getData().stream()
                        .filter(proj -> proj.getProjectId() == selected.getProjectId())
                        .findFirst().orElse(null);
                String pTitle = p != null ? p.getTitle() : "Investment";
                try {
                    edu.connexion3a8.entities.User investor = userService.getUserById(selected.getInvestorId());
                    if (investor != null) {
                        EmailService.sendInvestmentRefused(investor.getEmail(), pTitle, selected.getTotalAmount());
                    }
                } catch (Exception e) {
                    System.err.println("[AdminController] Email lookup failed: " + e.getMessage());
                }
            }

            investmentService.deleteEntity(selected);
            refreshData();
        }
    }

    private boolean confirm(String title, String content) {
        return AlertHelper.confirm(title, content);
    }

    @FXML
    void showFundingMomentumInfo() {
        Alert alert = AlertHelper.buildAlert(Alert.AlertType.INFORMATION, "Funding Momentum",
                "Funding Momentum shows how much of the platform's total capital demand has been funded.\n\n"
                        + "🎯 Target Capital — sum of all requested funding across every project.\n\n"
                        + "💵 Capital Injected — total invested (accepted offers) across all projects.\n\n"
                        + "Status levels:\n"
                        + "  • EARLY STAGE — < 40% funded\n"
                        + "  • GROWING — 40–75% funded\n"
                        + "  • STRONG — 75–99% funded\n"
                        + "  • FULLY FUNDED — 100%+");
        alert.setHeaderText("What is Funding Momentum?");
        alert.showAndWait();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        if (type == Alert.AlertType.ERROR)
            AlertHelper.showError(title, content);
        else if (type == Alert.AlertType.WARNING)
            AlertHelper.showWarning(title, content);
        else
            AlertHelper.showInfo(title, content);
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }
}
