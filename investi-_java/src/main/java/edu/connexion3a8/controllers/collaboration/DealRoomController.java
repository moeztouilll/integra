package edu.connexion3a8.controllers.collaboration;

import edu.connexion3a8.entities.collaboration.Collaboration;
import edu.connexion3a8.entities.collaboration.CollaborationMessage;
import edu.connexion3a8.entities.collaboration.Investment;
import edu.connexion3a8.entities.collaboration.Project;
import edu.connexion3a8.services.collaboration.CollaborationService;
import edu.connexion3a8.services.collaboration.GovernanceEngineAPI;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class DealRoomController {

    @FXML
    private Label lbState;
    @FXML
    private Label lbCapital;
    @FXML
    private Label lbEquity;
    @FXML
    private Label lbDuration;

    @FXML
    private VBox activeMetricsBox;
    @FXML
    private Label lbTemperature;
    @FXML
    private ProgressBar pbHealth;
    @FXML
    private Label lbHealthVal;
    @FXML
    private Label lbRisk;

    // Financial Intelligence 2.0
    @FXML
    private ProgressBar pbVelocity;
    @FXML
    private Label lbVelocityVal;
    @FXML
    private Label lbBurnRate;
    @FXML
    private Label lbRunway;

    // Fairness Visuals
    @FXML
    private Slider sliderFairness;
    @FXML
    private Label lbFairnessScore;
    @FXML
    private ProgressBar pbIdealEquity;
    @FXML
    private Label lbIdealVal;
    @FXML
    private ProgressBar pbActualEquity;
    @FXML
    private Label lbActualVal;
    @FXML
    private VBox driftAlertBox;

    @FXML
    private VBox negotiationBox;
    @FXML
    private HBox actionButtonsBox;
    @FXML
    private ProgressBar pbCompatibility;
    @FXML
    private Label lbCompatibility;

    @FXML
    private ListView<String> chatList;
    @FXML
    private TextField chatInput;

    private Investment currentInvestment;
    private Project currentProject;
    private Collaboration currentCollaboration;
    private String currentUserId; // User UUID, passed from caller

    private final CollaborationService cs = new CollaborationService();
    private ObservableList<String> chatData = FXCollections.observableArrayList();

    public void initData(Investment inv, Project proj, String loggedInUserId) {
        this.currentInvestment = inv;
        this.currentProject = proj;
        this.currentUserId = loggedInUserId;

        chatList.setItems(chatData);
        // Make cells wrap text so long AI messages aren't cut off
        chatList.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
            private final javafx.scene.control.Label label = new javafx.scene.control.Label();
            {
                label.setWrapText(true);
                label.setMaxWidth(Double.MAX_VALUE);
                label.setStyle("-fx-padding: 6 10;");
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    label.setText(item);
                    label.prefWidthProperty().bind(lv.widthProperty().subtract(20));
                    setGraphic(label);
                }
            }
        });

        // Populate terms
        lbCapital.setText(String.format("%.2f DT", inv.getTotalAmount()));
        lbEquity.setText(inv.getEquityRequested() + "%");
        lbDuration.setText(inv.getDurationMonths() + " Months");

        try {
            // Check if collaboration already exists (i.e. post-acceptance)
            currentCollaboration = cs.getCollaborationByInvestment(inv.getInvestmentId(), inv.getInvestorId());

            if (currentCollaboration != null || "ACCEPTED".equalsIgnoreCase(inv.getStatus())) {
                // ACTIVE COLLABORATION MODE
                if (currentCollaboration == null) {
                    // Create it if it was accepted but not initialized in DB yet
                    Collaboration newCollab = new Collaboration(0, inv.getInvestmentId(), proj.getEntrepreneurId(),
                            inv.getInvestorId(), null,
                            "ACTIVE", 100.0, 0.0);
                    currentCollaboration = cs.createCollaboration(newCollab);
                }
                setupActiveMode();
                loadMessages(true);
            } else {
                // PRE-ACCEPTANCE MODE (UNDER_REVIEW / OPEN offer)
                setupNegotiationMode();
                loadMessages(false);
            }
        } catch (Exception e) {
            System.err.println(
                    "[DealRoom] ERROR during initData: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupNegotiationMode() {
        lbState.setText("State: PRE-ACCEPTANCE DEAL ROOM");
        lbState.setStyle("-fx-font-weight: bold; -fx-text-fill: #b45309;");
        activeMetricsBox.setVisible(false);
        activeMetricsBox.setManaged(false);
        negotiationBox.setVisible(true);
        negotiationBox.setManaged(true);

        // Run AI Compatibility logic
        double compatScore = GovernanceEngineAPI.calculateCompatibilityIndex(currentInvestment, currentProject);
        pbCompatibility.setProgress(compatScore / 100.0);
        lbCompatibility.setText(String.format("%.1f / 100", compatScore));

        if (compatScore >= 80)
            pbCompatibility.setStyle("-fx-accent: #16a34a;");
        else if (compatScore >= 50)
            pbCompatibility.setStyle("-fx-accent: #ca8a04;");
        else
            pbCompatibility.setStyle("-fx-accent: #dc2626;");

        // Build Role-Based Action Buttons
        if (actionButtonsBox != null) {
            actionButtonsBox.getChildren().clear();

            if ("REFUSED".equalsIgnoreCase(currentInvestment.getStatus())) {
                Label blocked = new Label("Deal Refused. Negotiation Closed.");
                blocked.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold; -fx-font-size: 14px;");
                actionButtonsBox.getChildren().add(blocked);
                chatInput.setDisable(true); // Lock chat if refused
            } else {
                if (currentUserId.equals(currentProject.getEntrepreneurId())) {
                    // Entrepreneur View: Can only request a change
                    Button btnRequestChange = new Button("Request Change");
                    btnRequestChange.setStyle(
                            "-fx-background-color: #f59e0b; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
                    btnRequestChange.setOnAction(e -> handleRequestChange());
                    actionButtonsBox.getChildren().add(btnRequestChange);
                } else {
                    // Investor View: Can Edit
                    Button btnEdit = new Button("Edit Offer");
                    btnEdit.setStyle(
                            "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
                    btnEdit.setOnAction(e -> editOffer());

                    actionButtonsBox.getChildren().addAll(btnEdit);
                }
            }
        }
    }

    private void setupActiveMode() {
        lbState.setText("State: ACTIVE COLLABORATION");
        lbState.setStyle("-fx-font-weight: bold; -fx-text-fill: #16a34a;");
        negotiationBox.setVisible(false);
        negotiationBox.setManaged(false);
        activeMetricsBox.setVisible(true);
        activeMetricsBox.setManaged(true);

        // Calculate active metrics
        double health = GovernanceEngineAPI.calculateHealthScore(currentInvestment);
        double risk = GovernanceEngineAPI.calculateDefaultProbability(currentInvestment);
        String temp = GovernanceEngineAPI.getTemperature(health);

        lbTemperature.setText(temp);
        if ("HOT".equals(temp))
            lbTemperature.setStyle("-fx-font-weight: bold; -fx-text-fill: #16a34a;");
        else if ("WARM".equals(temp))
            lbTemperature.setStyle("-fx-font-weight: bold; -fx-text-fill: #ca8a04;");
        else
            lbTemperature.setStyle("-fx-font-weight: bold; -fx-text-fill: #dc2626;");

        pbHealth.setProgress(health / 100.0);
        lbHealthVal.setText(String.format("%.1f/100", health));
        if (health >= 80)
            pbHealth.setStyle("-fx-accent: #16a34a;");
        else if (health >= 60)
            pbHealth.setStyle("-fx-accent: #ca8a04;");
        else
            pbHealth.setStyle("-fx-accent: #dc2626;");

        lbRisk.setText(String.format("%.1f%%", risk));
        if (risk < 20)
            lbRisk.setStyle("-fx-font-weight: bold; -fx-text-fill: #16a34a;");
        else if (risk < 50)
            lbRisk.setStyle("-fx-font-weight: bold; -fx-text-fill: #ca8a04;");
        else
            lbRisk.setStyle("-fx-font-weight: bold; -fx-text-fill: #dc2626;");

        // Execute Financial Intelligence 2.0
        double burnRate = GovernanceEngineAPI.calculateBurnRate(currentInvestment.getTotalAmount(),
                currentInvestment.getDurationMonths());
        // Use remaining capital (not total) for a realistic runway estimate
        double remainingCapital = GovernanceEngineAPI.calculateRemainingCapital(currentInvestment);
        double runway = GovernanceEngineAPI.calculateRunwayMonths(remainingCapital, burnRate);
        double capitalVelocity = GovernanceEngineAPI.calculateCapitalVelocity(currentInvestment, currentProject);

        lbBurnRate.setText(String.format("%.0f DT/mo", burnRate));
        if (runway >= 999.0) {
            lbRunway.setText("∞ (Fully Funded)");
            lbRunway.setStyle("-fx-font-weight: bold; -fx-text-fill: #16a34a;");
        } else {
            lbRunway.setText(String.format("%.1f Months", runway));
            if (runway < 3.0) {
                lbRunway.setStyle("-fx-font-weight: bold; -fx-text-fill: #dc2626;");
                lbRunway.setText(lbRunway.getText() + " (HIGH RISK)");
            } else if (runway < 6.0) {
                lbRunway.setStyle("-fx-font-weight: bold; -fx-text-fill: #ca8a04;");
            } else {
                lbRunway.setStyle("-fx-font-weight: bold; -fx-text-fill: #16a34a;");
            }
        }

        pbVelocity.setProgress(capitalVelocity / 100.0);
        lbVelocityVal.setText(String.format("%.1f/100", capitalVelocity));
        if (capitalVelocity >= 70)
            pbVelocity.setStyle("-fx-accent: #2a506b;");
        else if (capitalVelocity >= 40)
            pbVelocity.setStyle("-fx-accent: #c07c4b;");
        else
            pbVelocity.setStyle("-fx-accent: #dc2626;");

        // Execute Fairness Engine
        GovernanceEngineAPI.FairnessReport fReport = GovernanceEngineAPI.evaluateFairnessDrift(currentInvestment);

        // Update DB with active governance + fairness stats
        currentCollaboration.setHealthScore(health);
        currentCollaboration.setDefaultProbability(risk);
        currentCollaboration.setFairnessScore(fReport.fairnessScore);
        currentCollaboration.setFairnessStatus(fReport.status);
        currentCollaboration.setIdealEquity(fReport.idealEquity);
        currentCollaboration.setEquityDeviation(fReport.deviation);
        cs.updateCollaborationScores(currentCollaboration);

        // Update Dual Bars
        pbIdealEquity.setProgress(fReport.idealEquity / 100.0);
        lbIdealVal.setText(String.format("%.1f%%", fReport.idealEquity));

        double actualEquity = currentInvestment.getEquityRequested();
        pbActualEquity.setProgress(actualEquity / 100.0);
        lbActualVal.setText(String.format("%.1f%%", actualEquity));

        // Update Compass (50 is balanced)
        double score = fReport.fairnessScore;
        lbFairnessScore.setText(String.format("Score: %.1f", score));

        // Map slider visuals based on advantage:
        // If Actual > Ideal -> Investor Advantage (Slider goes Right > 50)
        // If Actual < Ideal -> Entrepreneur Advantage (Slider goes Left < 50)
        // Ratio of deviation mapped to a 0-100 gauge where 50 is center
        double sliderVal = 50.0;
        if (actualEquity > fReport.idealEquity) {
            sliderVal = 50.0 + (50.0 * (1.0 - (score / 100.0)));
        } else if (actualEquity < fReport.idealEquity) {
            sliderVal = 50.0 - (50.0 * (1.0 - (score / 100.0)));
        } else {
            sliderVal = 50.0; // Perfect match
        }
        sliderFairness.setValue(Math.max(0, Math.min(100, sliderVal)));

        if (score >= 80)
            lbFairnessScore.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #16a34a;");
        else if (score >= 60)
            lbFairnessScore.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #ca8a04;");
        else
            lbFairnessScore.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #dc2626;");

        // Display Alert if deviation > 15% (which means fairness drops severely)
        if ("UNBALANCED".equals(fReport.status) || fReport.deviation > 15.0) {
            driftAlertBox.setVisible(true);
            driftAlertBox.setManaged(true);
        } else {
            driftAlertBox.setVisible(false);
            driftAlertBox.setManaged(false);
        }
    }

    @FXML
    void editOffer() {
        if ("ACCEPTED".equalsIgnoreCase(currentInvestment.getStatus()) || currentCollaboration != null) {
            AlertHelper.showError("Cannot Edit", "Cannot edit an accepted or active deal.");
            return;
        }

        // Only Investor or Entrepreneur can edit, but typically investor made the offer
        Optional<String> amtResult = AlertHelper.input("Edit Offer", "Update Investment Amount",
                String.valueOf(currentInvestment.getTotalAmount()));
        if (amtResult.isEmpty() || amtResult.get().isBlank())
            return;

        Optional<String> eqResult = AlertHelper.input("Edit Offer", "Update Equity Requested (%)",
                String.valueOf(currentInvestment.getEquityRequested()));
        if (eqResult.isEmpty() || eqResult.get().isBlank())
            return;

        Optional<String> durResult = AlertHelper.input("Edit Offer", "Update Duration (Months)",
                String.valueOf(currentInvestment.getDurationMonths()));
        if (durResult.isEmpty() || durResult.get().isBlank())
            return;

        try {
            double newAmount = Double.parseDouble(amtResult.get().trim());
            double newEquity = Double.parseDouble(eqResult.get().trim());
            int newDuration = Integer.parseInt(durResult.get().trim());

            if (newAmount <= 0) {
                AlertHelper.showError("Validation Error", "Amount must be positive.");
                return;
            }
            if (newEquity <= 0 || newEquity > 100) {
                AlertHelper.showError("Validation Error", "Equity must be between 0 and 100.");
                return;
            }
            if (newDuration <= 0) {
                AlertHelper.showError("Validation Error", "Duration must be positive.");
                return;
            }

            // Update local object
            currentInvestment.setTotalAmount(newAmount);
            currentInvestment.setEquityRequested(newEquity);
            currentInvestment.setDurationMonths(newDuration);

            // Calculate new amount per period
            currentInvestment.setAmountPerPeriod(newAmount / newDuration);

            // Save to DB
            edu.connexion3a8.services.collaboration.InvestmentService is = new edu.connexion3a8.services.collaboration.InvestmentService();
            if (is.update(currentInvestment.getInvestmentId(), currentInvestment)) {

                // Post a system message to the chat
                CollaborationMessage cm = new CollaborationMessage();
                cm.setSenderId(currentUserId);
                cm.setMessage("[System] Offer updated to " + newAmount + " DT, " + newEquity + "% equity, for "
                        + newDuration + " months.");
                cm.setType("GENERAL");
                cm.setInvestmentId(currentInvestment.getInvestmentId());
                cs.sendMessage(cm);

                // Update UI terms
                lbCapital.setText(String.format("%.2f DT", newAmount));
                lbEquity.setText(newEquity + "%");
                lbDuration.setText(newDuration + " Months");

                // Recalculate AI Compatibility
                double compatScore = GovernanceEngineAPI.calculateCompatibilityIndex(currentInvestment, currentProject);
                pbCompatibility.setProgress(compatScore / 100.0);
                lbCompatibility.setText(String.format("%.1f / 100", compatScore));

                if (compatScore >= 80)
                    pbCompatibility.setStyle("-fx-accent: #16a34a;");
                else if (compatScore >= 50)
                    pbCompatibility.setStyle("-fx-accent: #ca8a04;");
                else
                    pbCompatibility.setStyle("-fx-accent: #dc2626;");

                loadMessages(false); // Refresh chat to show system log

                AlertHelper.showInfo("Offer Updated", "Offer successfully updated.");
            } else {
                AlertHelper.showError("Database Error", "Failed to update offer in DB.");
            }

        } catch (NumberFormatException ex) {
            AlertHelper.showError("Invalid Input", "Invalid number entered.");
        } catch (java.sql.SQLException exsql) {
            exsql.printStackTrace();
            AlertHelper.showError("Database Error", "Database error recording message.");
        }
    }

    private long lastChangeRequestTime = 0;

    private void handleRequestChange() {
        long now = System.currentTimeMillis();
        if (now - lastChangeRequestTime < 60000) { // 60 seconds cooldown
            AlertHelper.showWarning("Please Wait", "Please wait before requesting another change.");
            return;
        }
        lastChangeRequestTime = now;
        chatInput.setText("I would like to request a change to the current offer terms. Let's discuss.");
        sendMessage();
    }

    private void loadMessages(boolean isActive) {
        chatData.clear();
        List<CollaborationMessage> msgs;
        if (isActive) {
            msgs = cs.getMessagesForCollaboration(currentCollaboration.getId());
        } else {
            msgs = cs.getMessagesForNegotiation(currentInvestment.getInvestmentId());
        }

        for (CollaborationMessage m : msgs) {
            String role = (m.getSenderId().equals(currentProject.getEntrepreneurId())) ? "Entrepreneur" : "Investor";
            String prefix = (m.getSenderId().equals(currentUserId)) ? "You (" + role + "): " : role + ": ";
            chatData.add(prefix + m.getMessage());
        }

        Platform.runLater(() -> chatList.scrollTo(chatData.size() - 1));
    }

    @FXML
    void sendMessage() {
        String txt = chatInput.getText();
        if (txt == null || txt.trim().isEmpty())
            return;

        CollaborationMessage cm = new CollaborationMessage();
        cm.setSenderId(currentUserId);
        cm.setMessage(txt.trim());
        cm.setType("GENERAL");

        if (currentCollaboration != null) {
            cm.setCollaborationId(currentCollaboration.getId());
        } else {
            cm.setInvestmentId(currentInvestment.getInvestmentId());
        }

        try {
            cs.sendMessage(cm);
            String role = (currentUserId.equals(currentProject.getEntrepreneurId())) ? "Entrepreneur" : "Investor";
            chatData.add("You (" + role + "): " + txt.trim());
            chatInput.clear();
            Platform.runLater(() -> chatList.scrollTo(chatData.size() - 1));
        } catch (SQLException e) {
            e.printStackTrace();
            AlertHelper.showError("Send Error", "Failed to send message.");
        }
    }

    @FXML
    void closeWindow() {
        Stage stage = (Stage) lbState.getScene().getWindow();
        stage.close();
    }

    // ─── Info Popup Handlers
    // ───────────────────────────────────────────────────────────

    private void showInfoAlert(String title, String message) {
        AlertHelper.showInfo(title, message);
    }

    @FXML
    void showCapitalVelocityInfo() {
        showInfoAlert("📊 Capital Velocity",
                "Capital Velocity measures how efficiently injected capital is being converted into tangible project progress.\n\n"
                        +
                        "A score above 70 is excellent — it means the startup is using funding productively to hit milestones.\n"
                        +
                        "A score below 40 may indicate delays or inefficient use of funds.");
    }

    @FXML
    void showBurnRateInfo() {
        showInfoAlert("🔥 Burn Rate",
                "Burn Rate is the estimated monthly amount of capital the startup is spending to run operations.\n\n" +
                        "It is calculated as:\n  Total Capital ÷ Duration (months)\n\n" +
                        "A high burn rate with low revenue is a risk signal.");
    }

    @FXML
    void showRunwayInfo() {
        showInfoAlert("🛫 Est. Runway",
                "Runway tells you how many months the startup can continue operating before running out of capital, based on the current Burn Rate.\n\n"
                        +
                        "Runway < 3 months = HIGH RISK — immediate action required.\n" +
                        "Runway > 12 months = Very stable funding position.");
    }

    @FXML
    void showAdaptiveDealInfo() {
        showInfoAlert("⚖️ Adaptive Deal Balance",
                "The Adaptive Deal Balance is an AI-powered fairness score that evaluates whether the current equity split is fair for both sides.\n\n"
                        +
                        "Score 50 = Perfectly balanced.\n" +
                        "Score > 50 = Investor has more advantage.\n" +
                        "Score < 50 = Entrepreneur has more leverage.\n\n" +
                        "It factors in: project risk, investor reputation, and milestone progress.");
    }

    @FXML
    void showTemperatureInfo() {
        showInfoAlert("Temperature",
                "The Temperature is a quick-glance indicator of how healthy and active the collaboration is.\n\n" +
                        "HOT: Health score >= 75. Everything is on track. Milestones are being hit and payments are flowing.\n\n"
                        +
                        "WARM: Health score 50-74. Minor delays or slight concerns. Watch closely.\n\n" +
                        "COLD: Health score < 50. Significant issues detected. Renegotiation or intervention recommended.");
    }

    @FXML
    void showHealthScoreInfo() {
        showInfoAlert("Health Score",
                "The Health Score is an overall 0-100 rating of how well the collaboration is performing.\n\n" +
                        "It is calculated using:\n" +
                        "  - Milestone completion rate vs. expected pace\n" +
                        "  - Payment punctuality (months paid on time)\n" +
                        "  - Time elapsed vs. duration contracted\n\n" +
                        "80-100 = Excellent\n60-79 = Good\nBelow 60 = Needs attention");
    }

    @FXML
    void showDefaultRiskInfo() {
        showInfoAlert("Default Risk",
                "Default Risk is the estimated probability (%) that the startup may fail to fulfill its obligations during the investment period.\n\n"
                        +
                        "It accounts for:\n" +
                        "  - How much of the contracted duration has passed\n" +
                        "  - How many payments have been completed\n" +
                        "  - Milestone completion pace\n\n" +
                        "< 20% = LOW - the project is performing well.\n" +
                        "20-50% = MEDIUM - monitor closely.\n" +
                        "> 50% = HIGH - serious risk of non-delivery.");
    }

    @FXML
    void showCompatibilityInfo() {
        showInfoAlert("Compatibility Index",
                "The Compatibility Index is a pre-acceptance score (0-100) that measures how well-matched the investor offer is with the entrepreneur project needs.\n\n"
                        +
                        "It evaluates:\n" +
                        "  - Valuation alignment (offer vs. project target)\n" +
                        "  - Equity fairness relative to funding stage\n" +
                        "  - Duration suitability for the project type\n\n" +
                        "A score above 70 is a strong match.\nBelow 40 suggests significant misalignment.");
    }
}
