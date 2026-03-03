package edu.connexion3a8.controllers.gamification;

import edu.connexion3a8.entities.gamification.Course;
import edu.connexion3a8.entities.gamification.CourseReport;
import edu.connexion3a8.services.gamification.CouseService;
import edu.connexion3a8.services.gamification.ExcelExportService;
import edu.connexion3a8.utils.gamification.ThemeManager;
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
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.List;
import java.util.stream.Collectors;

public class CourseController {

    @FXML private TextField titleField;
    @FXML private TextField slugField;
    @FXML private TextArea descriptionArea;
    @FXML private TextField contentUrlField;
    @FXML private ComboBox<String> contentTypeCombo;
    @FXML private ComboBox<String> difficultyCombo;
    @FXML private TextField categoryField;
    @FXML private TextField languageField;
    @FXML private TextField durationField;
    @FXML private TextField rewardPointsField;
    @FXML private TextField minimumPointsField;
    @FXML private TextField thumbnailField;
    @FXML private Label statusLabel;
    @FXML private VBox courseListContainer;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo;
    @FXML private Button themeToggleBtn;
    @FXML private Button autoModeBtn;
    
    // Animation elements
    @FXML private HBox headerBox;
    @FXML private Label titleLabel;
    @FXML private VBox formContainer;
    @FXML private HBox buttonBar;
    @FXML private StackPane separatorPane;
    @FXML private Rectangle shimmerLine;
    @FXML private Label sparkleIcon;
    @FXML private VBox courseListSection;
    @FXML private Label courseCountLabel;
    @FXML private HBox statusContainer;

    private CouseService courseService;
    private Course selectedCourse = null;
    private List<Course> allCourses = new ArrayList<>();
    private boolean isDarkMode = false;
    private boolean isAutoMode = false;
    private javafx.scene.layout.Pane rootPane;

    @FXML
    public void initialize() {
        courseService = new CouseService();
        
        // Initialize ComboBoxes
        contentTypeCombo.setItems(FXCollections.observableArrayList(
            "video", "article", "interactive", "pdf", "quiz"
        ));
        
        difficultyCombo.setItems(FXCollections.observableArrayList(
            "beginner", "intermediate", "advanced", "expert"
        ));
        
        // Initialize sort combo
        if (sortCombo != null) {
            sortCombo.setItems(FXCollections.observableArrayList(
                "Title (A-Z)",
                "Title (Z-A)",
                "Points (Low to High)",
                "Points (High to Low)",
                "Duration (Short to Long)",
                "Duration (Long to Short)"
            ));
            sortCombo.setOnAction(e -> applyFiltersAndSort());
        }
        
        // Add listener for search field
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFiltersAndSort());
        }
        
        statusLabel.setText("");
        
        // Get root pane for theme switching
        if (titleField != null && titleField.getScene() != null) {
            javafx.scene.Parent root = titleField.getScene().getRoot();
            if (root instanceof javafx.scene.layout.Pane) {
                rootPane = (javafx.scene.layout.Pane) root;
            }
        }
        
        // Don't load courses on initialization - wait for "View All" click
        // Just load the count
        try {
            allCourses = courseService.getAllCourses();
            if (courseCountLabel != null) {
                courseCountLabel.setText("(" + allCourses.size() + ")");
            }
        } catch (SQLException e) {
            System.err.println("Error loading course count: " + e.getMessage());
        }
        
        // Start entrance animations after a short delay
        Timeline delayTimeline = new Timeline(new KeyFrame(Duration.millis(100), e -> playEntranceAnimations()));
        delayTimeline.play();
        
        // Start shimmer animation
        startShimmerAnimation();
        
        // Start sparkle animation
        startSparkleAnimation();
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
            if (titleField != null && titleField.getScene() != null) {
                javafx.scene.Parent root = titleField.getScene().getRoot();
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
                autoModeBtn.getStyleClass().add("auto-mode-active");
                autoModeBtn.setText("⏰ Auto ✓");
            } else {
                autoModeBtn.getStyleClass().remove("auto-mode-active");
                autoModeBtn.setText("⏰ Auto");
            }
        }
    }

    @FXML
    private void handleBrowseFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Course Media");
        
        // Set extension filters
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("All Media", "*.mp4", "*.avi", "*.mkv", "*.pdf"),
            new FileChooser.ExtensionFilter("Video Files", "*.mp4", "*.avi", "*.mkv", "*.mov", "*.wmv", "*.flv"),
            new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
        );
        
        // Show open file dialog
        Stage stage = (Stage) contentUrlField.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);
        
        if (selectedFile != null) {
            try {
                // Create media directory if it doesn't exist
                Path mediaDir = Paths.get("media", "courses");
                Files.createDirectories(mediaDir);
                
                // Generate unique filename to avoid conflicts
                String originalFileName = selectedFile.getName();
                String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
                String timestamp = String.valueOf(System.currentTimeMillis());
                String newFileName = "course_" + timestamp + fileExtension;
                
                // Copy file to media directory
                Path targetPath = mediaDir.resolve(newFileName);
                Files.copy(selectedFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
                
                // Set the relative path in the content URL field
                String relativePath = "media/courses/" + newFileName;
                contentUrlField.setText(relativePath);
                
                // Auto-detect content type
                if (fileExtension.toLowerCase().matches("\\.(mp4|avi|mkv|mov|wmv|flv)")) {
                    contentTypeCombo.setValue("video");
                } else if (fileExtension.toLowerCase().equals(".pdf")) {
                    contentTypeCombo.setValue("pdf");
                }
                
                showSuccess("File uploaded successfully: " + originalFileName);
                
            } catch (IOException e) {
                showError("Error uploading file: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleBrowseThumbnail() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Course Thumbnail");
        
        // Set extension filters for images only
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"),
            new FileChooser.ExtensionFilter("PNG Images", "*.png"),
            new FileChooser.ExtensionFilter("JPEG Images", "*.jpg", "*.jpeg")
        );
        
        // Show open file dialog
        Stage stage = (Stage) thumbnailField.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);
        
        if (selectedFile != null) {
            try {
                // Create thumbnails directory if it doesn't exist
                Path thumbnailDir = Paths.get("media", "thumbnails");
                Files.createDirectories(thumbnailDir);
                
                // Generate unique filename to avoid conflicts
                String originalFileName = selectedFile.getName();
                String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
                String timestamp = String.valueOf(System.currentTimeMillis());
                String newFileName = "thumb_" + timestamp + fileExtension;
                
                // Copy file to thumbnails directory
                Path targetPath = thumbnailDir.resolve(newFileName);
                Files.copy(selectedFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
                
                // Set the relative path in the thumbnail field
                String relativePath = "media/thumbnails/" + newFileName;
                thumbnailField.setText(relativePath);
                
                showSuccess("Thumbnail uploaded successfully: " + originalFileName);
                
            } catch (IOException e) {
                showError("Error uploading thumbnail: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleAddCourse() {
        try {
            // Check if fields are initialized
            if (titleField == null || slugField == null || descriptionArea == null || 
                contentUrlField == null || categoryField == null || languageField == null ||
                durationField == null || rewardPointsField == null || thumbnailField == null) {
                showError("Form not properly initialized!");
                return;
            }
            
            // Get text safely
            String title = titleField.getText();
            String slug = slugField.getText();
            String duration = durationField.getText();
            String rewardPoints = rewardPointsField.getText();
            
            // Validate inputs
            if (title == null || title.trim().isEmpty() || 
                slug == null || slug.trim().isEmpty()) {
                showError("Title and Slug are required!");
                return;
            }

            // Validate numeric fields
            if (duration == null || duration.trim().isEmpty() || 
                rewardPoints == null || rewardPoints.trim().isEmpty()) {
                showError("Duration and Reward Points are required!");
                return;
            }

            // Create or update course object
            Course course = selectedCourse != null ? selectedCourse : new Course();
            course.setTitle(title.trim());
            course.setSlug(slug.trim());
            
            String description = descriptionArea.getText();
            course.setDescription(description != null ? description.trim() : "");
            
            String contentUrl = contentUrlField.getText();
            course.setContentUrl(contentUrl != null ? contentUrl.trim() : "");
            
            // Set content type with default
            String contentType = contentTypeCombo.getValue();
            course.setContentType(contentType != null ? contentType : "video");
            
            // Set difficulty with default
            String difficulty = difficultyCombo.getValue();
            course.setDifficultyLevel(difficulty != null ? difficulty : "beginner");
            
            String category = categoryField.getText();
            course.setCategory(category != null ? category.trim() : "");
            
            String language = languageField.getText();
            course.setLanguage(language != null ? language.trim() : "");
            
            // Parse numeric fields
            try {
                int durationValue = Integer.parseInt(duration.trim());
                int rewardPointsValue = Integer.parseInt(rewardPoints.trim());
                
                String minimumPoints = minimumPointsField.getText();
                int minimumPointsValue = 0;
                if (minimumPoints != null && !minimumPoints.trim().isEmpty()) {
                    minimumPointsValue = Integer.parseInt(minimumPoints.trim());
                }

                if (durationValue < 0 || rewardPointsValue < 0 || minimumPointsValue < 0) {
                    showError("Duration, Reward Points, and Minimum Points must be positive numbers!");
                    return;
                }
                
                course.setEstimatedDuration(durationValue);
                course.setRewardPoints(rewardPointsValue);
                course.setMinimumPointsRequired(minimumPointsValue);
            } catch (NumberFormatException e) {
                showError("Duration, Reward Points, and Minimum Points must be valid numbers!");
                return;
            }
            
            String thumbnail = thumbnailField.getText();
            course.setThumbnailUrl(thumbnail != null ? thumbnail.trim() : "");
            course.setStatus("published");
            course.setVisibility("public");

            // Add or update course in database
            if (selectedCourse != null) {
                courseService.updateCourse(course, selectedCourse.getId());
                showSuccess("Course updated successfully!");
            } else {
                courseService.addCourse(course);
                showSuccess("Course added successfully!");
            }
            
            handleClearForm();
            
            // Reload courses if the list is visible
            if (courseListSection != null && courseListSection.isVisible()) {
                loadCourses();
            } else {
                // Just update the count
                try {
                    allCourses = courseService.getAllCourses();
                    if (courseCountLabel != null) {
                        courseCountLabel.setText("(" + allCourses.size() + ")");
                    }
                } catch (SQLException ex) {
                    System.err.println("Error updating course count: " + ex.getMessage());
                }
            }
            
        } catch (SQLException e) {
            showError("Database error: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            showError("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleClearForm() {
        selectedCourse = null;
        titleField.clear();
        slugField.clear();
        descriptionArea.clear();
        contentUrlField.clear();
        contentTypeCombo.setValue(null);
        difficultyCombo.setValue(null);
        categoryField.clear();
        languageField.clear();
        durationField.clear();
        rewardPointsField.clear();
        minimumPointsField.clear();
        thumbnailField.clear();
        statusLabel.setText("");
    }

    @FXML
    private void handleViewAll() {
        // Toggle course list section visibility
        if (courseListSection != null) {
            boolean isVisible = courseListSection.isVisible();
            
            if (!isVisible) {
                // Show the course list section
                courseListSection.setManaged(true);
                courseListSection.setVisible(true);
                
                // Load courses if not already loaded
                if (allCourses.isEmpty()) {
                    loadCourses();
                } else {
                    applyFiltersAndSort();
                }
                
                // Animate the section appearance
                courseListSection.setOpacity(0);
                courseListSection.setTranslateY(30);
                
                FadeTransition fade = new FadeTransition(Duration.millis(500), courseListSection);
                fade.setFromValue(0);
                fade.setToValue(1);
                
                TranslateTransition slide = new TranslateTransition(Duration.millis(500), courseListSection);
                slide.setFromY(30);
                slide.setToY(0);
                
                ParallelTransition anim = new ParallelTransition(fade, slide);
                anim.play();
                
                showStatus("Showing all " + allCourses.size() + " courses", "info");
            } else {
                // Hide the course list section
                FadeTransition fade = new FadeTransition(Duration.millis(300), courseListSection);
                fade.setFromValue(1);
                fade.setToValue(0);
                fade.setOnFinished(e -> {
                    courseListSection.setManaged(false);
                    courseListSection.setVisible(false);
                });
                fade.play();
                
                showStatus("Course list hidden", "info");
            }
        }
    }

    private void showSuccess(String message) {
        showStatus(message, "success");
    }

    private void showError(String message) {
        showStatus(message, "error");
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

    private void loadCourses() {
        try {
            if (courseListContainer == null) {
                System.err.println("courseListContainer is null - FXML not loaded properly");
                return;
            }
            
            allCourses = courseService.getAllCourses();
            applyFiltersAndSort();
            
        } catch (SQLException e) {
            showError("Error loading courses: " + e.getMessage());
        }
    }
    
    private void applyFiltersAndSort() {
        if (courseListContainer == null || allCourses == null) {
            return;
        }
        
        courseListContainer.getChildren().clear();
        
        // Filter courses based on search
        List<Course> filteredCourses = allCourses;
        
        if (searchField != null && searchField.getText() != null && !searchField.getText().trim().isEmpty()) {
            String searchText = searchField.getText().trim().toLowerCase();
            filteredCourses = allCourses.stream()
                .filter(course -> {
                    boolean matchesTitle = course.getTitle().toLowerCase().contains(searchText);
                    boolean matchesCategory = course.getCategory().toLowerCase().contains(searchText);
                    boolean matchesDifficulty = course.getDifficultyLevel().toLowerCase().contains(searchText);
                    return matchesTitle || matchesCategory || matchesDifficulty;
                })
                .collect(Collectors.toList());
        }
        
        // Sort courses
        if (sortCombo != null && sortCombo.getValue() != null) {
            String sortOption = sortCombo.getValue();
            
            switch (sortOption) {
                case "Title (A-Z)":
                    filteredCourses.sort(Comparator.comparing(Course::getTitle));
                    break;
                case "Title (Z-A)":
                    filteredCourses.sort(Comparator.comparing(Course::getTitle).reversed());
                    break;
                case "Points (Low to High)":
                    filteredCourses.sort(Comparator.comparingInt(Course::getRewardPoints));
                    break;
                case "Points (High to Low)":
                    filteredCourses.sort(Comparator.comparingInt(Course::getRewardPoints).reversed());
                    break;
                case "Duration (Short to Long)":
                    filteredCourses.sort(Comparator.comparingInt(Course::getEstimatedDuration));
                    break;
                case "Duration (Long to Short)":
                    filteredCourses.sort(Comparator.comparingInt(Course::getEstimatedDuration).reversed());
                    break;
            }
        }
        
        // Display filtered and sorted courses
        if (filteredCourses.isEmpty()) {
            Label emptyLabel = new Label(searchField != null && !searchField.getText().trim().isEmpty() 
                ? "No courses found matching your search." 
                : "No courses yet. Add your first course!");
            emptyLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-style: italic; -fx-font-size: 13px;");
            courseListContainer.getChildren().add(emptyLabel);
            return;
        }
        
        // Add course cards with stagger animation
        for (int i = 0; i < filteredCourses.size(); i++) {
            Course course = filteredCourses.get(i);
            HBox courseItem = createCourseItem(course);
            courseListContainer.getChildren().add(courseItem);
            
            // Entrance animation for each card
            courseItem.setOpacity(0);
            courseItem.setTranslateX(100);
            
            FadeTransition fade = new FadeTransition(Duration.millis(600), courseItem);
            fade.setFromValue(0);
            fade.setToValue(1);
            
            TranslateTransition slide = new TranslateTransition(Duration.millis(600), courseItem);
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

    private HBox createCourseItem(Course course) {
        HBox container = new HBox(15);
        container.getStyleClass().add("item-card");
        container.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        // Course icon with yellow background
        StackPane iconContainer = new StackPane();
        iconContainer.setPrefSize(48, 48);
        iconContainer.setStyle("-fx-background-color: linear-gradient(to bottom right, rgba(255,215,0,0.25), rgba(255,223,0,0.1)); -fx-background-radius: 12;");
        
        Label iconLabel = new Label("📚");
        iconLabel.setStyle("-fx-font-size: 24px; -fx-text-fill: #FFD700; -fx-effect: dropshadow(gaussian, rgba(255,215,0,0.6), 4, 0, 0, 0);");
        iconContainer.getChildren().add(iconLabel);

        
        // Course info
        VBox infoBox = new VBox(5);
        HBox.setHgrow(infoBox, javafx.scene.layout.Priority.ALWAYS);
        
        Label titleLabel = new Label(course.getTitle());
        titleLabel.getStyleClass().add("item-card-title");
        
        Label detailsLabel = new Label(course.getDifficultyLevel() + " • " + course.getRewardPoints() + " points • " + course.getEstimatedDuration() + " min");
        detailsLabel.getStyleClass().add("item-card-details");
        
        infoBox.getChildren().addAll(titleLabel, detailsLabel);
        
        // Buttons
        HBox buttonBox = new HBox(10);
        
        Button editBtn = new Button("Edit");
        editBtn.getStyleClass().addAll("btn", "btn-secondary");
        editBtn.setStyle("-fx-font-size: 11px; -fx-padding: 6 14 6 14;");
        editBtn.setOnAction(e -> handleEditCourse(course));
        
        Button deleteBtn = new Button("Delete");
        deleteBtn.setStyle("-fx-background-color: #DC3545; -fx-text-fill: white; -fx-font-size: 11px; -fx-background-radius: 8px; -fx-padding: 6 14 6 14; -fx-cursor: hand;");
        deleteBtn.setOnAction(e -> handleDeleteCourse(course));
        
        buttonBox.getChildren().addAll(editBtn, deleteBtn);
        
        container.getChildren().addAll(iconContainer, infoBox, buttonBox);
        
        // Hover animation
        container.setOnMouseEntered(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(200), iconContainer);
            scale.setToX(1.1);
            scale.setToY(1.1);
            scale.play();
        });
        
        container.setOnMouseExited(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(200), iconContainer);
            scale.setToX(1);
            scale.setToY(1);
            scale.play();
        });
        
        return container;
    }

    private void handleEditCourse(Course course) {
        selectedCourse = course;
        
        titleField.setText(course.getTitle());
        slugField.setText(course.getSlug());
        descriptionArea.setText(course.getDescription());
        contentUrlField.setText(course.getContentUrl());
        contentTypeCombo.setValue(course.getContentType());
        difficultyCombo.setValue(course.getDifficultyLevel());
        categoryField.setText(course.getCategory());
        languageField.setText(course.getLanguage());
        durationField.setText(String.valueOf(course.getEstimatedDuration()));
        rewardPointsField.setText(String.valueOf(course.getRewardPoints()));
        minimumPointsField.setText(String.valueOf(course.getMinimumPointsRequired()));
        thumbnailField.setText(course.getThumbnailUrl());
        
        statusLabel.setStyle("-fx-text-fill: #456990; -fx-font-size: 14px;");
        statusLabel.setText("✏️ Editing: " + course.getTitle());
    }

    private void handleDeleteCourse(Course course) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Delete Course");
        confirmAlert.setHeaderText("Are you sure?");
        confirmAlert.setContentText("Do you want to delete the course: " + course.getTitle() + "?");
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    courseService.deleteCourse(course.getId());
                    showSuccess("Course deleted successfully!");
                    
                    // Reload courses if the list is visible
                    if (courseListSection != null && courseListSection.isVisible()) {
                        loadCourses();
                    } else {
                        // Just update the count
                        try {
                            allCourses = courseService.getAllCourses();
                            if (courseCountLabel != null) {
                                courseCountLabel.setText("(" + allCourses.size() + ")");
                            }
                        } catch (SQLException ex) {
                            System.err.println("Error updating course count: " + ex.getMessage());
                        }
                    }
                    
                } catch (SQLException e) {
                    showError("Error deleting course: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleBack() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/gamification/MainMenu.fxml"));
            javafx.scene.Parent root = loader.load();
            
            javafx.stage.Stage stage = (javafx.stage.Stage) statusLabel.getScene().getWindow();
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
        dialog.setTitle("Course Statistics");
        
        VBox root = new VBox(20);
        root.setPadding(new Insets(25));
        root.setStyle("-fx-background-color: #F7F0F5;");
        
        Label titleLabel = new Label("📊 Course Statistics");
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #000501;");
        
        HBox selectorBox = new HBox(15);
        selectorBox.setAlignment(Pos.CENTER_LEFT);
        
        Label selectLabel = new Label("Select Statistic:");
        selectLabel.setStyle("-fx-font-weight: 600; -fx-text-fill: #000501; -fx-font-size: 14px;");
        
        ComboBox<String> statsCombo = new ComboBox<>();
        statsCombo.setItems(FXCollections.observableArrayList(
            "Total Courses",
            "Courses by Difficulty",
            "Courses by Category",
            "Courses by Status",
            "Average Reward Points",
            "Average Duration",
            "Courses by Language"
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
                displayCourseStatistic(selected, resultsContainer);
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

    private void displayCourseStatistic(String statType, VBox container) {
        container.getChildren().clear();
        
        try {
            List<Course> allCourses = courseService.getAllCourses();
            
            switch (statType) {
                case "Total Courses":
                    displayTotalCourses(container, allCourses);
                    break;
                case "Courses by Difficulty":
                    displayCoursesByDifficulty(container, allCourses);
                    break;
                case "Courses by Category":
                    displayCoursesByCategory(container, allCourses);
                    break;
                case "Courses by Status":
                    displayCoursesByStatus(container, allCourses);
                    break;
                case "Average Reward Points":
                    displayAverageRewardPoints(container, allCourses);
                    break;
                case "Average Duration":
                    displayAverageDuration(container, allCourses);
                    break;
                case "Courses by Language":
                    displayCoursesByLanguage(container, allCourses);
                    break;
            }
            
        } catch (SQLException e) {
            Label errorLabel = new Label("Error loading statistics: " + e.getMessage());
            errorLabel.setStyle("-fx-text-fill: #DC3545; -fx-font-size: 13px;");
            container.getChildren().add(errorLabel);
        }
    }

    private void displayTotalCourses(VBox container, List<Course> courses) {
        Label statLabel = new Label("Total Courses: " + courses.size());
        statLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #456990;");
        
        Label descLabel = new Label("Total number of courses in the system");
        descLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 13px;");
        
        container.getChildren().addAll(statLabel, descLabel);
    }

    private void displayCoursesByDifficulty(VBox container, List<Course> courses) {
        Label titleLabel = new Label("Courses by Difficulty Level");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #000501;");
        
        java.util.Map<String, Long> difficultyCount = courses.stream()
            .collect(Collectors.groupingBy(Course::getDifficultyLevel, Collectors.counting()));
        
        VBox statsBox = new VBox(10);
        for (java.util.Map.Entry<String, Long> entry : difficultyCount.entrySet()) {
            HBox row = createCourseStatRow(entry.getKey(), entry.getValue().intValue(), courses.size());
            statsBox.getChildren().add(row);
        }
        
        container.getChildren().addAll(titleLabel, new Separator(), statsBox);
    }

    private void displayCoursesByCategory(VBox container, List<Course> courses) {
        Label titleLabel = new Label("Courses by Category");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #000501;");
        
        java.util.Map<String, Long> categoryCount = courses.stream()
            .collect(Collectors.groupingBy(Course::getCategory, Collectors.counting()));
        
        VBox statsBox = new VBox(10);
        for (java.util.Map.Entry<String, Long> entry : categoryCount.entrySet()) {
            HBox row = createCourseStatRow(entry.getKey(), entry.getValue().intValue(), courses.size());
            statsBox.getChildren().add(row);
        }
        
        container.getChildren().addAll(titleLabel, new Separator(), statsBox);
    }

    private void displayCoursesByStatus(VBox container, List<Course> courses) {
        Label titleLabel = new Label("Courses by Status");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #000501;");
        
        java.util.Map<String, Long> statusCount = courses.stream()
            .collect(Collectors.groupingBy(Course::getStatus, Collectors.counting()));
        
        VBox statsBox = new VBox(10);
        for (java.util.Map.Entry<String, Long> entry : statusCount.entrySet()) {
            HBox row = createCourseStatRow(entry.getKey(), entry.getValue().intValue(), courses.size());
            statsBox.getChildren().add(row);
        }
        
        container.getChildren().addAll(titleLabel, new Separator(), statsBox);
    }

    private void displayAverageRewardPoints(VBox container, List<Course> courses) {
        double avgPoints = courses.stream()
            .mapToInt(Course::getRewardPoints)
            .average()
            .orElse(0.0);
        
        Label statLabel = new Label(String.format("%.1f points", avgPoints));
        statLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #456990;");
        
        Label descLabel = new Label("Average reward points per course");
        descLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 13px;");
        
        container.getChildren().addAll(statLabel, descLabel);
    }

    private void displayAverageDuration(VBox container, List<Course> courses) {
        double avgDuration = courses.stream()
            .mapToInt(Course::getEstimatedDuration)
            .average()
            .orElse(0.0);
        
        Label statLabel = new Label(String.format("%.0f minutes", avgDuration));
        statLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #456990;");
        
        Label descLabel = new Label("Average estimated duration per course");
        descLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 13px;");
        
        container.getChildren().addAll(statLabel, descLabel);
    }

    private void displayCoursesByLanguage(VBox container, List<Course> courses) {
        Label titleLabel = new Label("Courses by Language");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #000501;");
        
        java.util.Map<String, Long> languageCount = courses.stream()
            .collect(Collectors.groupingBy(Course::getLanguage, Collectors.counting()));
        
        VBox statsBox = new VBox(10);
        for (java.util.Map.Entry<String, Long> entry : languageCount.entrySet()) {
            HBox row = createCourseStatRow(entry.getKey(), entry.getValue().intValue(), courses.size());
            statsBox.getChildren().add(row);
        }
        
        container.getChildren().addAll(titleLabel, new Separator(), statsBox);
    }

    private HBox createCourseStatRow(String label, int count, int total) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8));
        row.setStyle("-fx-background-color: #F7F0F5; -fx-background-radius: 6px;");
        
        Label nameLabel = new Label(label);
        nameLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #000501; -fx-font-weight: 600;");
        nameLabel.setPrefWidth(180);
        
        Label countLabel = new Label(count + " courses");
        countLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #456990;");
        countLabel.setPrefWidth(100);
        
        double percentage = total > 0 ? (double) count / total * 100 : 0;
        ProgressBar progressBar = new ProgressBar(percentage / 100);
        progressBar.setPrefWidth(150);
        progressBar.setStyle("-fx-accent: #456990;");
        
        Label percentLabel = new Label(String.format("%.1f%%", percentage));
        percentLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6B7280;");
        
        row.getChildren().addAll(nameLabel, countLabel, progressBar, percentLabel);
        
        return row;
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
        
        // Title animation
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
            
            FadeTransition formFade = new FadeTransition(Duration.millis(800), formContainer);
            formFade.setFromValue(0);
            formFade.setToValue(1);
            
            TranslateTransition formSlide = new TranslateTransition(Duration.millis(800), formContainer);
            formSlide.setFromX(-50);
            formSlide.setToX(0);
            
            ParallelTransition formAnim = new ParallelTransition(formFade, formSlide);
            formAnim.setDelay(Duration.millis(400));
            formAnim.play();
        }
        
        // Button bar entrance animation
        if (buttonBar != null) {
            buttonBar.setOpacity(0);
            buttonBar.setTranslateY(60);
            
            FadeTransition btnFade = new FadeTransition(Duration.millis(600), buttonBar);
            btnFade.setFromValue(0);
            btnFade.setToValue(1);
            
            TranslateTransition btnSlide = new TranslateTransition(Duration.millis(600), buttonBar);
            btnSlide.setFromY(60);
            btnSlide.setToY(0);
            
            ParallelTransition btnAnim = new ParallelTransition(btnFade, btnSlide);
            btnAnim.setDelay(Duration.millis(800));
            btnAnim.play();
        }
    }
    
    private void startShimmerAnimation() {
        if (shimmerLine != null && separatorPane != null) {
            // Bind shimmer animation to the actual width of the separator pane
            separatorPane.widthProperty().addListener((obs, oldVal, newVal) -> {
                double width = newVal.doubleValue();
                Timeline shimmerAnimation = new Timeline(
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
    private void handleViewReports() {
        try {
            List<CourseReport> reports = courseService.getAllReports();
            showReportsDialog(reports);
        } catch (SQLException e) {
            showError("Error loading reports: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @FXML
    private void handleExportCourses() {
        try {
            ExcelExportService excelService = 
                new ExcelExportService();
            
            java.io.File file = excelService.exportCoursesToExcel();
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Export Successful");
            alert.setHeaderText("✅ Courses Exported");
            alert.setContentText("Courses have been exported to:\n" + file.getAbsolutePath() + 
                               "\n\nFile size: " + (file.length() / 1024) + " KB");
            alert.showAndWait();
            
            // Open file location
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(file.getParentFile());
            }
            
        } catch (Exception e) {
            showError("Error exporting courses: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @FXML
    private void handleExportAnalytics() {
        try {
            ExcelExportService excelService = 
                new ExcelExportService();
            
            java.io.File file = excelService.exportCourseAnalytics();
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Export Successful");
            alert.setHeaderText("✅ Analytics Exported");
            alert.setContentText("Course analytics have been exported to:\n" + file.getAbsolutePath() + 
                               "\n\nFile size: " + (file.length() / 1024) + " KB" +
                               "\n\nSheets included:\n" +
                               "• Course Statistics\n" +
                               "• Category Distribution\n" +
                               "• Difficulty Distribution\n" +
                               "• Course Reports");
            alert.showAndWait();
            
            // Open file location
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(file.getParentFile());
            }
            
        } catch (Exception e) {
            showError("Error exporting analytics: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void showReportsDialog(List<CourseReport> reports) {
        Stage dialog = new Stage();
        dialog.setTitle("Course Reports");
        dialog.setWidth(900);
        dialog.setHeight(600);
        
        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: " + (ThemeManager.getInstance().isDarkMode() ? "#161630" : "#F7F0F5") + ";");
        
        // Header
        Label titleLabel = new Label("📋 Course Reports (" + reports.size() + ")");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + 
                           (ThemeManager.getInstance().isDarkMode() ? "#F0F2FA" : "#000501") + ";");
        
        // Filter buttons
        HBox filterBox = new HBox(10);
        filterBox.setAlignment(Pos.CENTER_LEFT);
        
        Button allBtn = new Button("All (" + reports.size() + ")");
        Button pendingBtn = new Button("Pending");
        Button reviewedBtn = new Button("Reviewed");
        Button resolvedBtn = new Button("Resolved");
        
        allBtn.setStyle("-fx-background-color: #456990; -fx-text-fill: white; -fx-padding: 8 16; -fx-background-radius: 6;");
        pendingBtn.setStyle("-fx-background-color: #FFA500; -fx-text-fill: white; -fx-padding: 8 16; -fx-background-radius: 6;");
        reviewedBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-padding: 8 16; -fx-background-radius: 6;");
        resolvedBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-padding: 8 16; -fx-background-radius: 6;");
        
        filterBox.getChildren().addAll(allBtn, pendingBtn, reviewedBtn, resolvedBtn);
        
        // Reports list
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        
        VBox reportsList = new VBox(15);
        reportsList.setPadding(new Insets(10));
        
        // Populate reports
        for (CourseReport report : reports) {
            reportsList.getChildren().add(createReportCard(report, dialog));
        }
        
        scrollPane.setContent(reportsList);
        
        // Filter actions
        allBtn.setOnAction(e -> {
            try {
                List<CourseReport> allReports = courseService.getAllReports();
                reportsList.getChildren().clear();
                for (CourseReport r : allReports) {
                    reportsList.getChildren().add(createReportCard(r, dialog));
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });
        
        pendingBtn.setOnAction(e -> filterReports("pending", reportsList, dialog));
        reviewedBtn.setOnAction(e -> filterReports("reviewed", reportsList, dialog));
        resolvedBtn.setOnAction(e -> filterReports("resolved", reportsList, dialog));
        
        root.getChildren().addAll(titleLabel, filterBox, scrollPane);
        
        Scene scene = new Scene(root);
        dialog.setScene(scene);
        dialog.show();
    }
    
    private void filterReports(String status, VBox reportsList, Stage dialog) {
        try {
            List<CourseReport> filtered = courseService.getReportsByStatus(status);
            reportsList.getChildren().clear();
            for (CourseReport r : filtered) {
                reportsList.getChildren().add(createReportCard(r, dialog));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
    
    private HBox createReportCard(CourseReport report, Stage parentDialog) {
        HBox card = new HBox(15);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: " + (ThemeManager.getInstance().isDarkMode() ? "#12122A" : "white") + "; " +
                     "-fx-background-radius: 10; -fx-border-color: " + 
                     (ThemeManager.getInstance().isDarkMode() ? "rgba(70,70,100,0.6)" : "#E0E0E0") + "; " +
                     "-fx-border-width: 1; -fx-border-radius: 10;");
        
        // Status indicator
        String statusColor = report.getStatus().equals("pending") ? "#FFA500" :
                            report.getStatus().equals("reviewed") ? "#2196F3" :
                            report.getStatus().equals("resolved") ? "#4CAF50" : "#DC3545";
        
        VBox statusBox = new VBox();
        statusBox.setPrefWidth(10);
        statusBox.setStyle("-fx-background-color: " + statusColor + "; -fx-background-radius: 5;");
        
        // Report info
        VBox infoBox = new VBox(8);
        HBox.setHgrow(infoBox, Priority.ALWAYS);
        
        Label courseLabel = new Label("📚 " + report.getCourseName());
        courseLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + 
                            (ThemeManager.getInstance().isDarkMode() ? "#F0F2FA" : "#000501") + ";");
        
        Label reasonLabel = new Label("⚠️ " + report.getReportReason());
        reasonLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: " + statusColor + "; -fx-font-weight: 600;");
        
        Label dateLabel = new Label("📅 " + report.getCreatedAt().toString());
        dateLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + 
                          (ThemeManager.getInstance().isDarkMode() ? "#8D96A6" : "#666") + ";");
        
        Label statusLabel = new Label("Status: " + report.getStatus().toUpperCase());
        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + statusColor + "; -fx-font-weight: 600;");
        
        infoBox.getChildren().addAll(courseLabel, reasonLabel, dateLabel, statusLabel);
        
        // View details button
        Button detailsBtn = new Button("View Details");
        detailsBtn.setStyle("-fx-background-color: #456990; -fx-text-fill: white; -fx-padding: 10 20; " +
                           "-fx-background-radius: 6; -fx-cursor: hand;");
        detailsBtn.setOnAction(e -> showReportDetails(report, parentDialog));
        
        card.getChildren().addAll(statusBox, infoBox, detailsBtn);
        
        return card;
    }
    
    private void showReportDetails(CourseReport report, Stage parentDialog) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Report Details");
        alert.setHeaderText("Report #" + report.getId());
        
        String details = String.format(
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
            "📚 Course: %s\n" +
            "🆔 Course ID: %d\n\n" +
            "👤 Reported by User ID: %d\n" +
            "📅 Date: %s\n\n" +
            "⚠️ Reason: %s\n\n" +
            "📝 Description:\n%s\n\n" +
            "📊 Status: %s\n\n" +
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
            report.getCourseName(),
            report.getCourseId(),
            report.getUserId(),
            report.getCreatedAt(),
            report.getReportReason(),
            report.getDescription(),
            report.getStatus().toUpperCase()
        );
        
        alert.setContentText(details);
        
        // Add action buttons
        ButtonType updateBtn = new ButtonType("Update Status");
        ButtonType deleteBtn = new ButtonType("Delete Report");
        ButtonType closeBtn = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        
        alert.getButtonTypes().setAll(updateBtn, deleteBtn, closeBtn);
        
        alert.showAndWait().ifPresent(response -> {
            if (response == updateBtn) {
                updateReportStatus(report, parentDialog);
            } else if (response == deleteBtn) {
                deleteReport(report, parentDialog);
            }
        });
    }
    
    private void updateReportStatus(CourseReport report, Stage parentDialog) {
        ChoiceDialog<String> dialog = new ChoiceDialog<>("pending", "pending", "reviewed", "resolved", "dismissed");
        dialog.setTitle("Update Report Status");
        dialog.setHeaderText("Report #" + report.getId());
        dialog.setContentText("Select new status:");
        
        dialog.showAndWait().ifPresent(status -> {
            try {
                courseService.updateReportStatus(report.getId(), status);
                Alert success = new Alert(Alert.AlertType.INFORMATION);
                success.setTitle("Success");
                success.setHeaderText("✅ Status Updated");
                success.setContentText("Report status has been updated to: " + status.toUpperCase());
                success.showAndWait();
                
                // Refresh the reports dialog
                parentDialog.close();
                handleViewReports();
            } catch (SQLException e) {
                showError("Error updating status: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    private void deleteReport(CourseReport report, Stage parentDialog) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Report");
        confirm.setHeaderText("⚠️ Confirm Deletion");
        confirm.setContentText("Are you sure you want to delete this report?\nThis action cannot be undone.");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    courseService.deleteReport(report.getId());
                    Alert success = new Alert(Alert.AlertType.INFORMATION);
                    success.setTitle("Success");
                    success.setHeaderText("✅ Report Deleted");
                    success.setContentText("The report has been deleted successfully.");
                    success.showAndWait();
                    
                    // Refresh the reports dialog
                    parentDialog.close();
                    handleViewReports();
                } catch (SQLException e) {
                    showError("Error deleting report: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }
}
