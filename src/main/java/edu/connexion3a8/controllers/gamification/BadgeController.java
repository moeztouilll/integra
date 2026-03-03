package edu.connexion3a8.controllers.gamification;

import edu.connexion3a8.entities.gamification.Badge;
import edu.connexion3a8.services.gamification.GamificationService;
import javafx.animation.*;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class BadgeController {

    @FXML private TextField nameField;
    @FXML private TextArea descriptionArea;
    @FXML private TextField pointsRequiredField;
    @FXML private Label statusLabel;
    @FXML private VBox badgeListContainer;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo;
    @FXML private Button themeToggleBtn;
    @FXML private Button autoModeBtn;
    
    // Animation elements
    @FXML private VBox mainContainer;
    @FXML private HBox headerBox;
    @FXML private Label titleLabel;
    @FXML private VBox formContainer;
    @FXML private HBox buttonBar;
    @FXML private HBox statusContainer;
    @FXML private VBox badgeListSection;
    @FXML private Label badgeCountLabel;
    @FXML private Label sparkleIcon;
    @FXML private Rectangle shimmerLine;
    @FXML private StackPane separatorPane;
    @FXML private ScrollPane badgeScrollPane;
    @FXML private Button addBtn;
    @FXML private Button clearBtn;
    @FXML private Button viewBtn;
    @FXML private Button backBtn;

    private GamificationService gamificationService;
    private Badge selectedBadge = null;
    private List<Badge> allBadges = new ArrayList<>();
    private boolean isDarkMode = false;
    private boolean isAutoMode = false;
    private javafx.scene.layout.Pane rootPane;
    
    private Timeline shimmerAnimation;

    @FXML
    public void initialize() {
        gamificationService = new GamificationService();
        
        // Initialize sort combo
        if (sortCombo != null) {
            sortCombo.setItems(FXCollections.observableArrayList(
                "Name (A-Z)",
                "Name (Z-A)",
                "Points (Low to High)",
                "Points (High to Low)",
                "Newest First"
            ));
            sortCombo.setOnAction(e -> applyFiltersAndSort());
        }
        
        // Add listener for search field
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFiltersAndSort());
        }
        
        // Get root pane for theme switching
        if (nameField != null && nameField.getScene() != null) {
            javafx.scene.Parent root = nameField.getScene().getRoot();
            if (root instanceof javafx.scene.layout.Pane) {
                rootPane = (javafx.scene.layout.Pane) root;
            }
        }
        
        // Don't load badges on initialization - wait for "View All" click
        // Just load the count
        try {
            allBadges = gamificationService.getAllBadges();
            if (badgeCountLabel != null) {
                badgeCountLabel.setText("(" + allBadges.size() + ")");
            }
        } catch (SQLException e) {
            System.err.println("Error loading badge count: " + e.getMessage());
        }
        
        // Start entrance animations after a short delay
        Timeline delayTimeline = new Timeline(new KeyFrame(Duration.millis(100), e -> playEntranceAnimations()));
        delayTimeline.play();
        
        // Start shimmer animation
        startShimmerAnimation();
        
        // Start sparkle animation
        startSparkleAnimation();
    }

    private void playEntranceAnimations() {
        // Header entrance animation
        if (headerBox != null) {
            headerBox.setOpacity(0);
            headerBox.setTranslateY(-40);
            
            FadeTransition headerFade = new FadeTransition(Duration.millis(800), headerBox);
            headerFade.setFromValue(0);
            headerFade.setToValue(1);
            
            TranslateTransition headerSlide = new TranslateTransition(Duration.millis(800), headerBox);
            headerSlide.setFromY(-40);
            headerSlide.setToY(0);
            
            ParallelTransition headerAnim = new ParallelTransition(headerFade, headerSlide);
            headerAnim.setDelay(Duration.millis(100));
            headerAnim.play();
        }
        
        // Title character animation (simulated)
        if (titleLabel != null) {
            titleLabel.setOpacity(0);
            titleLabel.setScaleX(0.8);
            titleLabel.setScaleY(0.8);
            
            FadeTransition titleFade = new FadeTransition(Duration.millis(600), titleLabel);
            titleFade.setFromValue(0);
            titleFade.setToValue(1);
            
            ScaleTransition titleScale = new ScaleTransition(Duration.millis(600), titleLabel);
            titleScale.setFromX(0.8);
            titleScale.setFromY(0.8);
            titleScale.setToX(1);
            titleScale.setToY(1);
            
            ParallelTransition titleAnim = new ParallelTransition(titleFade, titleScale);
            titleAnim.setDelay(Duration.millis(200));
            titleAnim.play();
        }
        
        // Form entrance animation
        if (formContainer != null) {
            formContainer.setOpacity(0);
            formContainer.setTranslateX(-50);
            formContainer.setRotate(-5);
            
            FadeTransition formFade = new FadeTransition(Duration.millis(800), formContainer);
            formFade.setFromValue(0);
            formFade.setToValue(1);
            
            TranslateTransition formSlide = new TranslateTransition(Duration.millis(800), formContainer);
            formSlide.setFromX(-50);
            formSlide.setToX(0);
            
            RotateTransition formRotate = new RotateTransition(Duration.millis(800), formContainer);
            formRotate.setFromAngle(-5);
            formRotate.setToAngle(0);
            
            ParallelTransition formAnim = new ParallelTransition(formFade, formSlide, formRotate);
            formAnim.setDelay(Duration.millis(400));
            formAnim.play();
        }
        
        // Buttons entrance animation with stagger
        if (buttonBar != null) {
            List<Button> buttons = List.of(addBtn, clearBtn, viewBtn, backBtn);
            for (int i = 0; i < buttons.size(); i++) {
                Button btn = buttons.get(i);
                if (btn != null) {
                    btn.setOpacity(0);
                    btn.setTranslateY(60);
                    btn.setScaleX(0.5);
                    btn.setScaleY(0.5);
                    
                    FadeTransition btnFade = new FadeTransition(Duration.millis(600), btn);
                    btnFade.setFromValue(0);
                    btnFade.setToValue(1);
                    
                    TranslateTransition btnSlide = new TranslateTransition(Duration.millis(600), btn);
                    btnSlide.setFromY(60);
                    btnSlide.setToY(0);
                    
                    ScaleTransition btnScale = new ScaleTransition(Duration.millis(600), btn);
                    btnScale.setFromX(0.5);
                    btnScale.setFromY(0.5);
                    btnScale.setToX(1);
                    btnScale.setToY(1);
                    
                    ParallelTransition btnAnim = new ParallelTransition(btnFade, btnSlide, btnScale);
                    btnAnim.setDelay(Duration.millis(800 + i * 100));
                    btnAnim.play();
                }
            }
        }
        
        // Badge list section entrance (skip if hidden)
        if (badgeListSection != null && badgeListSection.isVisible()) {
            badgeListSection.setOpacity(0);
            badgeListSection.setTranslateY(50);
            
            FadeTransition listFade = new FadeTransition(Duration.millis(700), badgeListSection);
            listFade.setFromValue(0);
            listFade.setToValue(1);
            
            TranslateTransition listSlide = new TranslateTransition(Duration.millis(700), badgeListSection);
            listSlide.setFromY(50);
            listSlide.setToY(0);
            
            ParallelTransition listAnim = new ParallelTransition(listFade, listSlide);
            listAnim.setDelay(Duration.millis(1000));
            listAnim.play();
        }
    }

    private void startShimmerAnimation() {
        if (shimmerLine != null && separatorPane != null) {
            // Bind shimmer animation to the actual width of the separator pane
            separatorPane.widthProperty().addListener((obs, oldVal, newVal) -> {
                if (shimmerAnimation != null) {
                    shimmerAnimation.stop();
                }
                double width = newVal.doubleValue();
                shimmerAnimation = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(shimmerLine.translateXProperty(), -width)),
                    new KeyFrame(Duration.seconds(2), new KeyValue(shimmerLine.translateXProperty(), width))
                );
                shimmerAnimation.setCycleCount(Timeline.INDEFINITE);
                shimmerAnimation.play();
            });
        }
    }

    private void startSparkleAnimation() {
        if (sparkleIcon != null) {
            ScaleTransition sparkle = new ScaleTransition(Duration.millis(1500), sparkleIcon);
            sparkle.setFromX(1);
            sparkle.setFromY(1);
            sparkle.setToX(1.2);
            sparkle.setToY(1.2);
            sparkle.setAutoReverse(true);
            sparkle.setCycleCount(Timeline.INDEFINITE);
            sparkle.play();
            
            RotateTransition rotate = new RotateTransition(Duration.millis(3000), sparkleIcon);
            rotate.setByAngle(360);
            rotate.setCycleCount(Timeline.INDEFINITE);
            rotate.play();
        }
    }

    @FXML
    private void handleThemeToggle() {
        if (isAutoMode) {
            isAutoMode = false;
            updateAutoModeButton();
        }
        
        isDarkMode = !isDarkMode;
        applyTheme();
        updateThemeButton();
    }
    
    @FXML
    private void handleAutoMode() {
        isAutoMode = !isAutoMode;
        updateAutoModeButton();
        
        if (isAutoMode) {
            applyAutoTheme();
        }
    }
    
    private void applyAutoTheme() {
        int hour = java.time.LocalTime.now().getHour();
        boolean shouldBeDark = hour >= 18 || hour < 6;
        
        if (isDarkMode != shouldBeDark) {
            isDarkMode = shouldBeDark;
            applyTheme();
            updateThemeButton();
        }
    }
    
    private void applyTheme() {
        if (rootPane == null) {
            if (nameField != null && nameField.getScene() != null) {
                javafx.scene.Parent root = nameField.getScene().getRoot();
                if (root instanceof javafx.scene.layout.Pane) {
                    rootPane = (javafx.scene.layout.Pane) root;
                }
            }
        }
        
        if (rootPane != null) {
            if (isDarkMode) {
                if (!rootPane.getStyleClass().contains("dark-mode")) {
                    rootPane.getStyleClass().add("dark-mode");
                }
            } else {
                rootPane.getStyleClass().remove("dark-mode");
            }
        }
    }
    
    private void updateThemeButton() {
        if (themeToggleBtn != null) {
            if (isDarkMode) {
                themeToggleBtn.setText("☀️ Light");
            } else {
                themeToggleBtn.setText("🌙 Dark");
            }
        }
    }
    
    private void updateAutoModeButton() {
        if (autoModeBtn != null) {
            if (isAutoMode) {
                if (!autoModeBtn.getStyleClass().contains("auto-mode-active")) {
                    autoModeBtn.getStyleClass().add("auto-mode-active");
                }
                autoModeBtn.setText("⏰ Auto ✓");
            } else {
                autoModeBtn.getStyleClass().remove("auto-mode-active");
                autoModeBtn.setText("⏰ Auto");
            }
        }
    }

    @FXML
    private void handleAddBadge() {
        try {
            if (nameField == null || descriptionArea == null || pointsRequiredField == null) {
                showStatus("Form not properly initialized!", "error");
                return;
            }
            
            String name = nameField.getText();
            String description = descriptionArea.getText();
            
            if (name == null || name.trim().isEmpty()) {
                showStatus("Badge name is required!", "error");
                return;
            }

            if (description == null || description.trim().isEmpty()) {
                showStatus("Description is required!", "error");
                return;
            }

            Badge badge = selectedBadge != null ? selectedBadge : new Badge();
            badge.setName(name.trim());
            badge.setDescription(description.trim());
            
            try {
                String pointsText = pointsRequiredField.getText();
                int pointsRequired = (pointsText == null || pointsText.trim().isEmpty()) ? 0 : 
                                    Integer.parseInt(pointsText.trim());
                
                if (pointsRequired < 0) {
                    showStatus("Points required must be 0 or positive!", "error");
                    return;
                }
                
                badge.setPointsRequired(pointsRequired);
            } catch (NumberFormatException e) {
                showStatus("Points required must be a valid number!", "error");
                return;
            }

            if (selectedBadge != null) {
                updateBadgeInDatabase(badge);
                showStatus("Badge updated successfully!", "success");
            } else {
                addBadgeToDatabase(badge);
                showStatus("Badge added successfully!", "success");
            }
            
            handleClearForm();
            
            // Reload badges if the list is visible
            if (badgeListSection != null && badgeListSection.isVisible()) {
                loadBadges();
            } else {
                // Just update the count
                try {
                    allBadges = gamificationService.getAllBadges();
                    if (badgeCountLabel != null) {
                        badgeCountLabel.setText("(" + allBadges.size() + ")");
                    }
                } catch (SQLException ex) {
                    System.err.println("Error updating badge count: " + ex.getMessage());
                }
            }
            
        } catch (Exception e) {
            showStatus("Error: " + e.getMessage(), "error");
            e.printStackTrace();
        }
    }

    private void addBadgeToDatabase(Badge badge) throws SQLException {
        String query = "INSERT INTO badges (name, description, points_required) VALUES (?, ?, ?)";
        java.sql.PreparedStatement pst = gamificationService.getCnx().prepareStatement(query);
        pst.setString(1, badge.getName());
        pst.setString(2, badge.getDescription());
        pst.setInt(3, badge.getPointsRequired());
        pst.executeUpdate();
    }

    private void updateBadgeInDatabase(Badge badge) throws SQLException {
        String query = "UPDATE badges SET name = ?, description = ?, points_required = ? WHERE id = ?";
        java.sql.PreparedStatement pst = gamificationService.getCnx().prepareStatement(query);
        pst.setString(1, badge.getName());
        pst.setString(2, badge.getDescription());
        pst.setInt(3, badge.getPointsRequired());
        pst.setLong(4, badge.getId());
        pst.executeUpdate();
    }

    @FXML
    private void handleClearForm() {
        selectedBadge = null;
        if (nameField != null) nameField.clear();
        if (descriptionArea != null) descriptionArea.clear();
        if (pointsRequiredField != null) pointsRequiredField.clear();
        hideStatus();
    }

    @FXML
    private void handleViewAll() {
        // Toggle badge list section visibility
        if (badgeListSection != null) {
            boolean isVisible = badgeListSection.isVisible();
            
            if (!isVisible) {
                // Show the badge list section
                badgeListSection.setManaged(true);
                badgeListSection.setVisible(true);
                
                // Load badges if not already loaded
                if (allBadges.isEmpty()) {
                    loadBadges();
                } else {
                    applyFiltersAndSort();
                }
                
                // Animate the section appearance
                badgeListSection.setOpacity(0);
                badgeListSection.setTranslateY(30);
                
                FadeTransition fade = new FadeTransition(Duration.millis(500), badgeListSection);
                fade.setFromValue(0);
                fade.setToValue(1);
                
                TranslateTransition slide = new TranslateTransition(Duration.millis(500), badgeListSection);
                slide.setFromY(30);
                slide.setToY(0);
                
                ParallelTransition anim = new ParallelTransition(fade, slide);
                anim.play();
                
                showStatus("Showing all " + allBadges.size() + " badges", "info");
            } else {
                // Hide the badge list section
                FadeTransition fade = new FadeTransition(Duration.millis(300), badgeListSection);
                fade.setFromValue(1);
                fade.setToValue(0);
                fade.setOnFinished(e -> {
                    badgeListSection.setManaged(false);
                    badgeListSection.setVisible(false);
                });
                fade.play();
                
                showStatus("Badge list hidden", "info");
            }
        }
    }

    private void showStatus(String message, String type) {
        if (statusLabel != null && statusContainer != null) {
            statusLabel.setText(message);
            statusContainer.setManaged(true);
            statusContainer.setVisible(true);
            
            // Apply style based on type
            statusLabel.getStyleClass().removeAll("status-success", "status-error", "status-info");
            switch (type) {
                case "success":
                    statusLabel.setStyle("-fx-background-color: rgba(40,167,69,0.1); -fx-text-fill: #28A745;");
                    break;
                case "error":
                    statusLabel.setStyle("-fx-background-color: rgba(220,53,69,0.1); -fx-text-fill: #DC3545;");
                    break;
                case "info":
                    statusLabel.setStyle("-fx-background-color: rgba(69,105,144,0.1); -fx-text-fill: #456990;");
                    break;
            }
            
            // Animate status appearance
            statusContainer.setOpacity(0);
            statusContainer.setTranslateY(-20);
            
            FadeTransition fade = new FadeTransition(Duration.millis(400), statusContainer);
            fade.setFromValue(0);
            fade.setToValue(1);
            
            TranslateTransition slide = new TranslateTransition(Duration.millis(400), statusContainer);
            slide.setFromY(-20);
            slide.setToY(0);
            
            ParallelTransition anim = new ParallelTransition(fade, slide);
            anim.play();
            
            // Auto-hide after 4 seconds
            Timeline hideTimeline = new Timeline(new KeyFrame(Duration.seconds(4), e -> hideStatus()));
            hideTimeline.play();
        }
    }

    private void hideStatus() {
        if (statusContainer != null) {
            FadeTransition fade = new FadeTransition(Duration.millis(300), statusContainer);
            fade.setFromValue(1);
            fade.setToValue(0);
            fade.setOnFinished(e -> {
                statusContainer.setManaged(false);
                statusContainer.setVisible(false);
            });
            fade.play();
        }
    }

    private void loadBadges() {
        try {
            if (badgeListContainer == null) {
                return;
            }
            
            allBadges = gamificationService.getAllBadges();
            
            // Update badge count
            if (badgeCountLabel != null) {
                badgeCountLabel.setText("(" + allBadges.size() + ")");
            }
            
            applyFiltersAndSort();
            
        } catch (SQLException e) {
            showStatus("Error loading badges: " + e.getMessage(), "error");
        }
    }
    
    private void applyFiltersAndSort() {
        if (badgeListContainer == null || allBadges == null) {
            return;
        }
        
        badgeListContainer.getChildren().clear();
        
        // Filter badges
        List<Badge> filteredBadges = allBadges;
        
        if (searchField != null && searchField.getText() != null && !searchField.getText().trim().isEmpty()) {
            String searchText = searchField.getText().trim().toLowerCase();
            filteredBadges = allBadges.stream()
                .filter(badge -> {
                    boolean matchesName = badge.getName().toLowerCase().contains(searchText);
                    boolean matchesPoints = String.valueOf(badge.getPointsRequired()).contains(searchText);
                    return matchesName || matchesPoints;
                })
                .collect(Collectors.toList());
        }
        
        // Sort badges
        if (sortCombo != null && sortCombo.getValue() != null) {
            String sortOption = sortCombo.getValue();
            
            switch (sortOption) {
                case "Name (A-Z)":
                    filteredBadges.sort(Comparator.comparing(Badge::getName));
                    break;
                case "Name (Z-A)":
                    filteredBadges.sort(Comparator.comparing(Badge::getName).reversed());
                    break;
                case "Points (Low to High)":
                    filteredBadges.sort(Comparator.comparingInt(Badge::getPointsRequired));
                    break;
                case "Points (High to Low)":
                    filteredBadges.sort(Comparator.comparingInt(Badge::getPointsRequired).reversed());
                    break;
                case "Newest First":
                default:
                    // Keep original order (newest first)
                    break;
            }
        }
        
        // Display badges
        if (filteredBadges.isEmpty()) {
            VBox emptyBox = new VBox(20);
            emptyBox.setAlignment(javafx.geometry.Pos.CENTER);
            emptyBox.setPadding(new Insets(80, 20, 80, 20));
            
            Label emptyIcon = new Label("🏆");
            emptyIcon.setStyle("-fx-font-size: 64px; -fx-opacity: 0.5;");
            
            Label emptyText = new Label("No badges found");
            emptyText.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #6B7280;");
            
            Label emptyHint = new Label("Try adjusting your search or add a new badge");
            emptyHint.setStyle("-fx-font-size: 14px; -fx-text-fill: #6B7280;");
            
            emptyBox.getChildren().addAll(emptyIcon, emptyText, emptyHint);
            badgeListContainer.getChildren().add(emptyBox);
            
            // Floating animation for empty state
            TranslateTransition floatAnim = new TranslateTransition(Duration.seconds(3), emptyIcon);
            floatAnim.setFromY(0);
            floatAnim.setToY(-15);
            floatAnim.setAutoReverse(true);
            floatAnim.setCycleCount(Timeline.INDEFINITE);
            floatAnim.play();
            
            return;
        }
        
        // Add badge cards with stagger animation
        for (int i = 0; i < filteredBadges.size(); i++) {
            Badge badge = filteredBadges.get(i);
            HBox badgeCard = createBadgeCard(badge);
            badgeListContainer.getChildren().add(badgeCard);
            
            // Entrance animation for each card
            badgeCard.setOpacity(0);
            badgeCard.setTranslateX(100);
            
            FadeTransition fade = new FadeTransition(Duration.millis(600), badgeCard);
            fade.setFromValue(0);
            fade.setToValue(1);
            
            TranslateTransition slide = new TranslateTransition(Duration.millis(600), badgeCard);
            slide.setFromX(100);
            slide.setToX(0);
            
            ParallelTransition cardAnim = new ParallelTransition(fade, slide);
            cardAnim.setDelay(Duration.millis(i * 100));
            cardAnim.play();
        }
    }
    
    @FXML
    private void handleClearSearch() {
        if (searchField != null) {
            searchField.clear();
        }
        if (sortCombo != null) {
            sortCombo.setValue(null);
        }
        applyFiltersAndSort();
    }

    private HBox createBadgeCard(Badge badge) {
        HBox container = new HBox(16);
        container.getStyleClass().add("badge-card");
        container.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        // Badge icon
        StackPane iconContainer = new StackPane();
        iconContainer.setPrefSize(48, 48);
        iconContainer.setStyle("-fx-background-color: linear-gradient(to bottom right, rgba(255,215,0,0.35), rgba(255,223,0,0.15)); -fx-background-radius: 12;");
        
        Label iconLabel = new Label("🏆");
        iconLabel.setStyle("-fx-font-size: 24px; -fx-text-fill: #FFD700; -fx-effect: dropshadow(gaussian, rgba(255,215,0,0.6), 4, 0, 0, 0);");
        iconContainer.getChildren().add(iconLabel);
        
        // Badge info
        VBox infoBox = new VBox(6);
        HBox.setHgrow(infoBox, javafx.scene.layout.Priority.ALWAYS);
        
        Label nameLabel = new Label(badge.getName());
        nameLabel.getStyleClass().add("badge-card-title");
        
        Label descLabel = new Label(badge.getDescription());
        descLabel.getStyleClass().add("badge-card-description");
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(infoBox, javafx.scene.layout.Priority.ALWAYS);
        
        infoBox.getChildren().addAll(nameLabel, descLabel);
        
        // Points badge
        Label pointsLabel = new Label(badge.getPointsRequired() == 0 ? "✨ Action" : badge.getPointsRequired() + " pts");
        pointsLabel.getStyleClass().add("badge-card-points");
        
        // Buttons
        HBox buttonBox = new HBox(10);
        
        Button editBtn = new Button("Edit");
        editBtn.setStyle("-fx-background-color: #456990; -fx-text-fill: white; -fx-font-size: 11px; -fx-background-radius: 8px; -fx-padding: 6 14 6 14; -fx-cursor: hand;");
        editBtn.setOnAction(e -> handleEditBadge(badge));
        
        Button deleteBtn = new Button("Delete");
        deleteBtn.setStyle("-fx-background-color: #DC3545; -fx-text-fill: white; -fx-font-size: 11px; -fx-background-radius: 8px; -fx-padding: 6 14 6 14; -fx-cursor: hand;");
        deleteBtn.setOnAction(e -> handleDeleteBadge(badge));
        
        buttonBox.getChildren().addAll(editBtn, deleteBtn);
        
        container.getChildren().addAll(iconContainer, infoBox, pointsLabel, buttonBox);
        
        // Hover animation
        container.setOnMouseEntered(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(200), iconContainer);
            scale.setToX(1.1);
            scale.setToY(1.1);
            scale.play();
            
            RotateTransition rotate = new RotateTransition(Duration.millis(200), pointsLabel);
            rotate.setByAngle(3);
            rotate.play();
        });
        
        container.setOnMouseExited(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(200), iconContainer);
            scale.setToX(1);
            scale.setToY(1);
            scale.play();
            
            RotateTransition rotate = new RotateTransition(Duration.millis(200), pointsLabel);
            rotate.setByAngle(-3);
            rotate.play();
        });
        
        return container;
    }

    private void handleEditBadge(Badge badge) {
        selectedBadge = badge;
        
        if (nameField != null) nameField.setText(badge.getName());
        if (descriptionArea != null) descriptionArea.setText(badge.getDescription());
        if (pointsRequiredField != null) pointsRequiredField.setText(String.valueOf(badge.getPointsRequired()));
        
        showStatus("✏️ Editing: " + badge.getName(), "info");
    }

    private void handleDeleteBadge(Badge badge) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Delete Badge");
        confirmAlert.setHeaderText("Are you sure?");
        confirmAlert.setContentText("Do you want to delete the badge: " + badge.getName() + "?");
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    String query = "DELETE FROM badges WHERE id = ?";
                    java.sql.PreparedStatement pst = gamificationService.getCnx().prepareStatement(query);
                    pst.setLong(1, badge.getId());
                    pst.executeUpdate();
                    
                    showStatus("Badge deleted successfully!", "success");
                    
                    // Reload badges if the list is visible
                    if (badgeListSection != null && badgeListSection.isVisible()) {
                        loadBadges();
                    } else {
                        // Just update the count
                        try {
                            allBadges = gamificationService.getAllBadges();
                            if (badgeCountLabel != null) {
                                badgeCountLabel.setText("(" + allBadges.size() + ")");
                            }
                        } catch (SQLException ex) {
                            System.err.println("Error updating badge count: " + ex.getMessage());
                        }
                    }
                    
                } catch (SQLException e) {
                    showStatus("Error deleting badge: " + e.getMessage(), "error");
                }
            }
        });
    }

    @FXML
    private void handleBack() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/gamification/MainMenu.fxml"));
            javafx.scene.Parent root = loader.load();
            
            javafx.stage.Stage stage = (javafx.stage.Stage) nameField.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("Gamification System - Main Menu");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleStatistics() {
        openStatisticsDialog();
    }

    private void openStatisticsDialog() {
        Stage dialog = new Stage();
        dialog.setTitle("Badge Statistics");
        
        VBox root = new VBox(20);
        root.setPadding(new Insets(25));
        root.setStyle("-fx-background-color: #F7F0F5;");
        
        Label titleLabel = new Label("📊 Badge Statistics");
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #000501;");
        
        HBox selectorBox = new HBox(15);
        selectorBox.setAlignment(Pos.CENTER_LEFT);
        
        Label selectLabel = new Label("Select Statistic:");
        selectLabel.setStyle("-fx-font-weight: 600; -fx-text-fill: #000501; -fx-font-size: 14px;");
        
        ComboBox<String> statsCombo = new ComboBox<>();
        statsCombo.setItems(FXCollections.observableArrayList(
            "Total Badges",
            "Average Points Required",
            "Badges by Points Range",
            "Badge Distribution"
        ));
        statsCombo.setPromptText("Choose a statistic...");
        statsCombo.setPrefWidth(300);
        statsCombo.setStyle("-fx-font-size: 13px;");
        
        selectorBox.getChildren().addAll(selectLabel, statsCombo);
        
        VBox resultsContainer = new VBox(15);
        resultsContainer.setPadding(new Insets(20));
        resultsContainer.setStyle("-fx-background-color: white; -fx-background-radius: 10px; -fx-border-color: #456990; -fx-border-width: 2px; -fx-border-radius: 10px;");
        resultsContainer.setPrefHeight(400);
        
        ScrollPane scrollPane = new ScrollPane(resultsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        
        Label initialLabel = new Label("Select a statistic to view data");
        initialLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-style: italic; -fx-font-size: 14px;");
        resultsContainer.getChildren().add(initialLabel);
        
        statsCombo.setOnAction(e -> {
            String selected = statsCombo.getValue();
            if (selected != null) {
                displayBadgeStatistic(selected, resultsContainer);
            }
        });
        
        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-background-color: #456990; -fx-text-fill: white; -fx-font-weight: 600; -fx-background-radius: 8px; -fx-padding: 10 30 10 30; -fx-cursor: hand; -fx-font-size: 13px;");
        closeBtn.setOnAction(e -> dialog.close());
        
        HBox closeBox = new HBox(closeBtn);
        closeBox.setAlignment(Pos.CENTER);
        
        root.getChildren().addAll(titleLabel, new Separator(), selectorBox, scrollPane, closeBox);
        
        Scene scene = new Scene(root, 650, 600);
        dialog.setScene(scene);
        dialog.show();
    }

    private void displayBadgeStatistic(String statType, VBox container) {
        container.getChildren().clear();
        
        try {
            List<Badge> allBadges = gamificationService.getAllBadges();
            
            switch (statType) {
                case "Total Badges":
                    displayTotalBadges(container, allBadges);
                    break;
                case "Average Points Required":
                    displayAveragePointsRequired(container, allBadges);
                    break;
                case "Badges by Points Range":
                    displayBadgesByPointsRange(container, allBadges);
                    break;
                case "Badge Distribution":
                    displayBadgeDistribution(container, allBadges);
                    break;
            }
            
        } catch (SQLException e) {
            Label errorLabel = new Label("Error loading statistics: " + e.getMessage());
            errorLabel.setStyle("-fx-text-fill: #DC3545; -fx-font-size: 13px;");
            container.getChildren().add(errorLabel);
        }
    }

    private void displayTotalBadges(VBox container, List<Badge> badges) {
        Label statLabel = new Label("Total Badges: " + badges.size());
        statLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #456990;");
        
        Label descLabel = new Label("Total number of badges in the system");
        descLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 13px;");
        
        container.getChildren().addAll(statLabel, descLabel);
    }

    private void displayAveragePointsRequired(VBox container, List<Badge> badges) {
        double avgPoints = badges.stream()
            .mapToInt(Badge::getPointsRequired)
            .average()
            .orElse(0.0);
        
        Label statLabel = new Label(String.format("%.1f points", avgPoints));
        statLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #456990;");
        
        Label descLabel = new Label("Average points required to earn badges");
        descLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 13px;");
        
        container.getChildren().addAll(statLabel, descLabel);
    }

    private void displayBadgesByPointsRange(VBox container, List<Badge> badges) {
        Label titleLabel = new Label("Badges by Points Range");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #000501;");
        
        int range0_100 = (int) badges.stream().filter(b -> b.getPointsRequired() <= 100).count();
        int range101_500 = (int) badges.stream().filter(b -> b.getPointsRequired() > 100 && b.getPointsRequired() <= 500).count();
        int range501_1000 = (int) badges.stream().filter(b -> b.getPointsRequired() > 500 && b.getPointsRequired() <= 1000).count();
        int range1001plus = (int) badges.stream().filter(b -> b.getPointsRequired() > 1000).count();
        
        VBox statsBox = new VBox(10);
        statsBox.getChildren().addAll(
            createBadgeStatRow("0-100 points", range0_100, badges.size()),
            createBadgeStatRow("101-500 points", range101_500, badges.size()),
            createBadgeStatRow("501-1000 points", range501_1000, badges.size()),
            createBadgeStatRow("1001+ points", range1001plus, badges.size())
        );
        
        container.getChildren().addAll(titleLabel, new Separator(), statsBox);
    }

    private void displayBadgeDistribution(VBox container, List<Badge> badges) {
        Label titleLabel = new Label("Badge Distribution");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #000501;");
        
        VBox statsBox = new VBox(10);
        for (Badge badge : badges) {
            HBox row = createBadgeStatRow(badge.getName(), badge.getPointsRequired(), 
                badges.stream().mapToInt(Badge::getPointsRequired).max().orElse(1));
            statsBox.getChildren().add(row);
        }
        
        container.getChildren().addAll(titleLabel, new Separator(), statsBox);
    }

    private HBox createBadgeStatRow(String label, int value, int maxValue) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8));
        row.setStyle("-fx-background-color: #F7F0F5; -fx-background-radius: 6px;");
        
        Label nameLabel = new Label(label);
        nameLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #000501; -fx-font-weight: 600;");
        nameLabel.setPrefWidth(180);
        
        Label countLabel = new Label(value + " badges");
        countLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #456990;");
        countLabel.setPrefWidth(100);
        
        double percentage = maxValue > 0 ? (double) value / maxValue * 100 : 0;
        ProgressBar progressBar = new ProgressBar(percentage / 100);
        progressBar.setPrefWidth(150);
        progressBar.setStyle("-fx-accent: #456990;");
        
        Label percentLabel = new Label(String.format("%.1f%%", percentage));
        percentLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6B7280;");
        
        row.getChildren().addAll(nameLabel, countLabel, progressBar, percentLabel);
        
        return row;
    }
}
