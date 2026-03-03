package edu.connexion3a8.controllers.gamification;

import edu.connexion3a8.entities.gamification.Quiz;
import edu.connexion3a8.entities.gamification.Question;
import edu.connexion3a8.entities.gamification.QuestionOption;
import edu.connexion3a8.entities.gamification.Course;
import edu.connexion3a8.services.gamification.GamificationService;
import edu.connexion3a8.services.gamification.CouseService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.scene.Scene;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class QuizController {

    @FXML private TextField titleField;
    @FXML private TextArea descriptionArea;
    @FXML private TextField pointsRewardField;
    @FXML private ComboBox<String> difficultyCombo;
    @FXML private TextField categoryField;
    @FXML private ComboBox<String> courseCombo;
    @FXML private TextField timeLimitField;
    @FXML private TextField passingScoreField;
    @FXML private ComboBox<String> statusCombo;
    @FXML private Label statusLabel;
    @FXML private VBox quizListContainer;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo;
    @FXML private Button themeToggleBtn;
    @FXML private Button autoModeBtn;
    @FXML private Button manageQuestionsBtn;

    private GamificationService gamificationService;
    private CouseService courseService;
    private Quiz selectedQuiz = null;
    private List<Quiz> allQuizzes = new ArrayList<>();
    private Map<String, Long> courseNameToIdMap = new HashMap<>();
    private boolean isDarkMode = false;
    private boolean isAutoMode = false;
    private javafx.scene.layout.Pane rootPane;

    @FXML
    public void initialize() {
        gamificationService = new GamificationService();
        courseService = new CouseService();
        
        // Initialize ComboBoxes
        difficultyCombo.setItems(FXCollections.observableArrayList(
            "beginner", "intermediate", "advanced", "expert"
        ));
        
        statusCombo.setItems(FXCollections.observableArrayList(
            "active", "inactive", "draft"
        ));
        
        // Load courses into combo box
        loadCourses();
        
        // Initialize sort combo
        if (sortCombo != null) {
            sortCombo.setItems(FXCollections.observableArrayList(
                "Title (A-Z)",
                "Title (Z-A)",
                "Points (Low to High)",
                "Points (High to Low)",
                "Questions (Few to Many)",
                "Questions (Many to Few)"
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
        
        // Load quizzes only if container is initialized
        if (quizListContainer != null) {
            loadQuizzes();
        } else {
            System.err.println("Warning: quizListContainer not initialized yet");
        }
    }

    private void loadCourses() {
        try {
            List<Course> courses = courseService.getAllCourses();
            List<String> courseNames = new ArrayList<>();
            courseNames.add("None (No course)");
            
            courseNameToIdMap.clear();
            
            for (Course course : courses) {
                String displayName = course.getTitle() + " (" + course.getCategory() + ")";
                courseNames.add(displayName);
                courseNameToIdMap.put(displayName, course.getId());
            }
            
            if (courseCombo != null) {
                courseCombo.setItems(FXCollections.observableArrayList(courseNames));
            }
            
        } catch (SQLException e) {
            System.err.println("Error loading courses: " + e.getMessage());
            e.printStackTrace();
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
    private void handleAddQuiz() {
        try {
            // Check if fields are initialized
            if (titleField == null || descriptionArea == null || pointsRewardField == null || 
                categoryField == null || timeLimitField == null || passingScoreField == null) {
                showError("Form not properly initialized!");
                return;
            }
            
            // Get text safely
            String title = titleField.getText();
            String pointsReward = pointsRewardField.getText();
            
            // Validate inputs
            if (title == null || title.trim().isEmpty()) {
                showError("Title is required!");
                return;
            }

            // Validate numeric fields
            if (pointsReward == null || pointsReward.trim().isEmpty()) {
                showError("Points Reward is required!");
                return;
            }

            // Create or update quiz object
            Quiz quiz = selectedQuiz != null ? selectedQuiz : new Quiz();
            quiz.setTitle(title.trim());
            
            String description = descriptionArea.getText();
            quiz.setDescription(description != null ? description.trim() : "");
            
            // Parse numeric fields
            try {
                int pointsRewardValue = Integer.parseInt(pointsReward.trim());
                
                String timeLimitText = timeLimitField.getText();
                int timeLimit = (timeLimitText == null || timeLimitText.trim().isEmpty()) ? 600 : Integer.parseInt(timeLimitText.trim());
                
                String passingScoreText = passingScoreField.getText();
                int passingScore = (passingScoreText == null || passingScoreText.trim().isEmpty()) ? 70 : Integer.parseInt(passingScoreText.trim());

                if (pointsRewardValue < 0 || timeLimit < 0 || passingScore < 0 || passingScore > 100) {
                    showError("Invalid numeric values! Passing score must be between 0-100.");
                    return;
                }
                
                quiz.setPointsReward(pointsRewardValue);
                quiz.setTimeLimit(timeLimit);
                quiz.setPassingScore(passingScore);
            } catch (NumberFormatException e) {
                showError("All numeric fields must be valid numbers!");
                return;
            }
            
            // Set difficulty with default
            String difficulty = difficultyCombo.getValue();
            quiz.setDifficultyLevel(difficulty != null ? difficulty : "beginner");
            
            String category = categoryField.getText();
            quiz.setCategory(category != null ? category.trim() : "");
            
            // Set status with default
            String status = statusCombo.getValue();
            quiz.setStatus(status != null ? status : "active");

            // Add or update quiz in database
            if (selectedQuiz != null) {
                // Calculate and update question count
                int questionCount = gamificationService.getQuestionCountByQuizId(selectedQuiz.getId());
                quiz.setQuestionCount(questionCount);
                
                gamificationService.updateQuiz(quiz, selectedQuiz.getId());
                
                // Update course link if changed
                updateCourseLink(selectedQuiz.getId());
                
                showSuccess("Quiz updated successfully!");
            } else {
                // New quiz starts with 0 questions
                quiz.setQuestionCount(0);
                gamificationService.addQuiz(quiz);
                
                // Get the newly created quiz ID
                List<Quiz> allQuizzes = gamificationService.getAllQuizzes();
                if (!allQuizzes.isEmpty()) {
                    Quiz newQuiz = allQuizzes.get(0); // Most recent quiz
                    
                    // Link to course if selected
                    updateCourseLink(newQuiz.getId());
                }
                
                showSuccess("Quiz added successfully!");
            }
            
            handleClearForm();
            loadQuizzes();
            
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
        selectedQuiz = null;
        titleField.clear();
        descriptionArea.clear();
        pointsRewardField.clear();
        difficultyCombo.setValue(null);
        categoryField.clear();
        courseCombo.setValue(null);
        timeLimitField.clear();
        passingScoreField.clear();
        statusCombo.setValue(null);
        statusLabel.setText("");
        
        // Disable Manage Questions button when no quiz is selected
        if (manageQuestionsBtn != null) {
            manageQuestionsBtn.setDisable(true);
        }
    }

    private void updateCourseLink(long quizId) {
        try {
            String selectedCourse = courseCombo.getValue();
            
            // First, remove any existing links for this quiz
            List<Long> existingCourseIds = getCoursesForQuiz(quizId);
            for (Long courseId : existingCourseIds) {
                courseService.unlinkQuizFromCourse(courseId, quizId);
            }
            
            // Then add new link if a course is selected
            if (selectedCourse != null && !selectedCourse.equals("None (No course)")) {
                Long courseId = courseNameToIdMap.get(selectedCourse);
                if (courseId != null) {
                    // Get current quiz count for this course to set order
                    List<Long> existingQuizzes = courseService.getQuizIdsForCourse(courseId);
                    int order = existingQuizzes.size() + 1;
                    
                    courseService.linkQuizToCourse(courseId, quizId, order, true);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error updating course link: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private List<Long> getCoursesForQuiz(long quizId) {
        List<Long> courseIds = new ArrayList<>();
        try {
            // Query to find all courses linked to this quiz
            List<Course> allCourses = courseService.getAllCourses();
            for (Course course : allCourses) {
                List<Long> quizIds = courseService.getQuizIdsForCourse(course.getId());
                if (quizIds.contains(quizId)) {
                    courseIds.add(course.getId());
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return courseIds;
    }

    @FXML
    private void handleViewAll() {
        try {
            List<Quiz> quizzes = gamificationService.getAllQuizzes();
            
            StringBuilder sb = new StringBuilder();
            sb.append("Total Quizzes: ").append(quizzes.size()).append("\n\n");
            
            for (Quiz quiz : quizzes) {
                sb.append("- ").append(quiz.getTitle())
                  .append(" (").append(quiz.getPointsReward()).append(" points, ")
                  .append(quiz.getQuestionCount()).append(" questions)\n");
            }
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("All Quizzes");
            alert.setHeaderText("Quiz List");
            alert.setContentText(sb.toString());
            alert.showAndWait();
            
        } catch (SQLException e) {
            showError("Error loading quizzes: " + e.getMessage());
        }
    }

    private void showSuccess(String message) {
        statusLabel.setStyle("-fx-text-fill: #28A745; -fx-font-size: 14px; -fx-font-weight: 600;");
        statusLabel.setText("✓ " + message);
    }

    private void showError(String message) {
        statusLabel.setStyle("-fx-text-fill: #DC3545; -fx-font-size: 14px; -fx-font-weight: 600;");
        statusLabel.setText("✗ " + message);
    }

    private void loadQuizzes() {
        try {
            if (quizListContainer == null) {
                System.err.println("quizListContainer is null - FXML not loaded properly");
                return;
            }
            
            allQuizzes = gamificationService.getAllQuizzes();
            applyFiltersAndSort();
            
        } catch (SQLException e) {
            showError("Error loading quizzes: " + e.getMessage());
        }
    }
    
    private void applyFiltersAndSort() {
        if (quizListContainer == null || allQuizzes == null) {
            return;
        }
        
        quizListContainer.getChildren().clear();
        
        // Filter quizzes based on search
        List<Quiz> filteredQuizzes = allQuizzes;
        
        if (searchField != null && searchField.getText() != null && !searchField.getText().trim().isEmpty()) {
            String searchText = searchField.getText().trim().toLowerCase();
            filteredQuizzes = allQuizzes.stream()
                .filter(quiz -> {
                    boolean matchesTitle = quiz.getTitle().toLowerCase().contains(searchText);
                    boolean matchesCategory = quiz.getCategory().toLowerCase().contains(searchText);
                    boolean matchesDifficulty = quiz.getDifficultyLevel().toLowerCase().contains(searchText);
                    boolean matchesPoints = String.valueOf(quiz.getPointsReward()).contains(searchText);
                    return matchesTitle || matchesCategory || matchesDifficulty || matchesPoints;
                })
                .collect(Collectors.toList());
        }
        
        // Sort quizzes
        if (sortCombo != null && sortCombo.getValue() != null) {
            String sortOption = sortCombo.getValue();
            
            switch (sortOption) {
                case "Title (A-Z)":
                    filteredQuizzes.sort(Comparator.comparing(Quiz::getTitle));
                    break;
                case "Title (Z-A)":
                    filteredQuizzes.sort(Comparator.comparing(Quiz::getTitle).reversed());
                    break;
                case "Points (Low to High)":
                    filteredQuizzes.sort(Comparator.comparingInt(Quiz::getPointsReward));
                    break;
                case "Points (High to Low)":
                    filteredQuizzes.sort(Comparator.comparingInt(Quiz::getPointsReward).reversed());
                    break;
                case "Questions (Few to Many)":
                    filteredQuizzes.sort(Comparator.comparingInt(Quiz::getQuestionCount));
                    break;
                case "Questions (Many to Few)":
                    filteredQuizzes.sort(Comparator.comparingInt(Quiz::getQuestionCount).reversed());
                    break;
            }
        }
        
        // Display filtered and sorted quizzes
        if (filteredQuizzes.isEmpty()) {
            Label emptyLabel = new Label(searchField != null && !searchField.getText().trim().isEmpty() 
                ? "No quizzes found matching your search." 
                : "No quizzes yet. Add your first quiz!");
            emptyLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-style: italic; -fx-font-size: 13px;");
            quizListContainer.getChildren().add(emptyLabel);
            return;
        }
        
        for (Quiz quiz : filteredQuizzes) {
            HBox quizItem = createQuizItem(quiz);
            quizListContainer.getChildren().add(quizItem);
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

    private HBox createQuizItem(Quiz quiz) {
        HBox container = new HBox(15);
        container.getStyleClass().add("item-card");
        
        // Quiz icon
        Label iconLabel = new Label("📝");
        iconLabel.setStyle("-fx-font-size: 24px;");
        
        // Quiz info
        VBox infoBox = new VBox(5);
        Label titleLabel = new Label(quiz.getTitle());
        titleLabel.getStyleClass().add("item-card-title");
        
        // Get dynamic question count
        int questionCount = 0;
        try {
            questionCount = gamificationService.getQuestionCountByQuizId(quiz.getId());
        } catch (SQLException e) {
            questionCount = quiz.getQuestionCount(); // Fallback to stored value
        }
        
        Label detailsLabel = new Label(questionCount + " questions • " + quiz.getPointsReward() + " points • " + quiz.getDifficultyLevel());
        detailsLabel.getStyleClass().add("item-card-details");
        
        infoBox.getChildren().addAll(titleLabel, detailsLabel);
        
        // Buttons
        HBox buttonBox = new HBox(10);
        
        Button editBtn = new Button("Edit");
        editBtn.getStyleClass().addAll("btn", "btn-secondary");
        editBtn.setStyle("-fx-font-size: 11px; -fx-padding: 6 14 6 14;");
        editBtn.setOnAction(e -> handleEditQuiz(quiz));
        
        Button questionsBtn = new Button("Questions");
        questionsBtn.setStyle("-fx-background-color: #9B7E46; -fx-text-fill: white; -fx-font-size: 11px; -fx-background-radius: 8px; -fx-padding: 6 14 6 14; -fx-cursor: hand;");
        questionsBtn.setOnAction(e -> openQuestionManagementDialog(quiz));
        
        Button deleteBtn = new Button("Delete");
        deleteBtn.setStyle("-fx-background-color: #DC3545; -fx-text-fill: white; -fx-font-size: 11px; -fx-background-radius: 8px; -fx-padding: 6 14 6 14; -fx-cursor: hand;");
        deleteBtn.setOnAction(e -> handleDeleteQuiz(quiz));
        
        buttonBox.getChildren().addAll(editBtn, questionsBtn, deleteBtn);
        HBox.setMargin(buttonBox, new Insets(0, 0, 0, 20));
        
        container.getChildren().addAll(iconLabel, infoBox, buttonBox);
        
        return container;
    }

    private void handleEditQuiz(Quiz quiz) {
        selectedQuiz = quiz;
        
        titleField.setText(quiz.getTitle());
        descriptionArea.setText(quiz.getDescription());
        pointsRewardField.setText(String.valueOf(quiz.getPointsReward()));
        difficultyCombo.setValue(quiz.getDifficultyLevel());
        categoryField.setText(quiz.getCategory());
        timeLimitField.setText(String.valueOf(quiz.getTimeLimit()));
        passingScoreField.setText(String.valueOf(quiz.getPassingScore()));
        statusCombo.setValue(quiz.getStatus());
        
        // Load linked course
        try {
            List<Long> linkedCourses = getCoursesForQuiz(quiz.getId());
            if (!linkedCourses.isEmpty()) {
                Long courseId = linkedCourses.get(0);
                Course course = courseService.getCourseById(courseId);
                if (course != null) {
                    String displayName = course.getTitle() + " (" + course.getCategory() + ")";
                    courseCombo.setValue(displayName);
                }
            } else {
                courseCombo.setValue("None (No course)");
            }
        } catch (SQLException e) {
            courseCombo.setValue("None (No course)");
            e.printStackTrace();
        }
        
        // Enable Manage Questions button when a quiz is selected
        if (manageQuestionsBtn != null) {
            manageQuestionsBtn.setDisable(false);
        }
        
        // Show question count dynamically
        try {
            int questionCount = gamificationService.getQuestionCountByQuizId(quiz.getId());
            statusLabel.setStyle("-fx-text-fill: #456990; -fx-font-size: 14px;");
            statusLabel.setText("✏️ Editing: " + quiz.getTitle() + " (" + questionCount + " questions)");
        } catch (SQLException e) {
            statusLabel.setStyle("-fx-text-fill: #456990; -fx-font-size: 14px;");
            statusLabel.setText("✏️ Editing: " + quiz.getTitle());
        }
    }

    private void handleDeleteQuiz(Quiz quiz) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Delete Quiz");
        confirmAlert.setHeaderText("Are you sure?");
        confirmAlert.setContentText("Do you want to delete the quiz: " + quiz.getTitle() + "?");
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    gamificationService.deleteQuiz(quiz.getId());
                    showSuccess("Quiz deleted successfully!");
                    loadQuizzes();
                    
                } catch (SQLException e) {
                    showError("Error deleting quiz: " + e.getMessage());
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
        dialog.setTitle("Quiz Statistics");
        
        VBox root = new VBox(20);
        root.setPadding(new Insets(25));
        root.setStyle("-fx-background-color: #F7F0F5;");
        
        // Title
        Label titleLabel = new Label("📊 Quiz Statistics");
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #000501;");
        
        // Statistics selector
        HBox selectorBox = new HBox(15);
        selectorBox.setAlignment(Pos.CENTER_LEFT);
        
        Label selectLabel = new Label("Select Statistic:");
        selectLabel.setStyle("-fx-font-weight: 600; -fx-text-fill: #000501; -fx-font-size: 14px;");
        
        ComboBox<String> statsCombo = new ComboBox<>();
        statsCombo.setItems(FXCollections.observableArrayList(
            "Total Quizzes",
            "Quizzes by Difficulty",
            "Quizzes by Category",
            "Quizzes by Status",
            "Average Points Reward",
            "Total Questions",
            "Average Questions per Quiz",
            "Quizzes by Points Range"
        ));
        statsCombo.setPromptText("Choose a statistic...");
        statsCombo.setPrefWidth(300);
        statsCombo.setStyle("-fx-font-size: 13px;");
        
        selectorBox.getChildren().addAll(selectLabel, statsCombo);
        
        // Results container
        VBox resultsContainer = new VBox(15);
        resultsContainer.setPadding(new Insets(20));
        resultsContainer.setStyle("-fx-background-color: white; -fx-background-radius: 10px; -fx-border-color: #456990; -fx-border-width: 2px; -fx-border-radius: 10px;");
        resultsContainer.setPrefHeight(400);
        
        ScrollPane scrollPane = new ScrollPane(resultsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        
        // Initial message
        Label initialLabel = new Label("Select a statistic to view data");
        initialLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-style: italic; -fx-font-size: 14px;");
        resultsContainer.getChildren().add(initialLabel);
        
        // Handle selection
        statsCombo.setOnAction(e -> {
            String selected = statsCombo.getValue();
            if (selected != null) {
                displayStatistic(selected, resultsContainer);
            }
        });
        
        // Close button
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

    private void displayStatistic(String statType, VBox container) {
        container.getChildren().clear();
        
        try {
            List<Quiz> allQuizzes = gamificationService.getAllQuizzes();
            
            switch (statType) {
                case "Total Quizzes":
                    displayTotalQuizzes(container, allQuizzes);
                    break;
                case "Quizzes by Difficulty":
                    displayQuizzesByDifficulty(container, allQuizzes);
                    break;
                case "Quizzes by Category":
                    displayQuizzesByCategory(container, allQuizzes);
                    break;
                case "Quizzes by Status":
                    displayQuizzesByStatus(container, allQuizzes);
                    break;
                case "Average Points Reward":
                    displayAveragePoints(container, allQuizzes);
                    break;
                case "Total Questions":
                    displayTotalQuestions(container, allQuizzes);
                    break;
                case "Average Questions per Quiz":
                    displayAverageQuestions(container, allQuizzes);
                    break;
                case "Quizzes by Points Range":
                    displayQuizzesByPointsRange(container, allQuizzes);
                    break;
            }
            
        } catch (SQLException e) {
            Label errorLabel = new Label("Error loading statistics: " + e.getMessage());
            errorLabel.setStyle("-fx-text-fill: #DC3545; -fx-font-size: 13px;");
            container.getChildren().add(errorLabel);
        }
    }

    private void displayTotalQuizzes(VBox container, List<Quiz> quizzes) {
        Label statLabel = new Label("Total Quizzes: " + quizzes.size());
        statLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #456990;");
        
        Label descLabel = new Label("Total number of quizzes in the system");
        descLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 13px;");
        
        container.getChildren().addAll(statLabel, descLabel);
    }

    private void displayQuizzesByDifficulty(VBox container, List<Quiz> quizzes) {
        Label titleLabel = new Label("Quizzes by Difficulty Level");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #000501;");
        
        java.util.Map<String, Long> difficultyCount = quizzes.stream()
            .collect(Collectors.groupingBy(Quiz::getDifficultyLevel, Collectors.counting()));
        
        VBox statsBox = new VBox(10);
        for (java.util.Map.Entry<String, Long> entry : difficultyCount.entrySet()) {
            HBox row = createStatRow(entry.getKey(), entry.getValue().intValue(), quizzes.size());
            statsBox.getChildren().add(row);
        }
        
        container.getChildren().addAll(titleLabel, new Separator(), statsBox);
    }

    private void displayQuizzesByCategory(VBox container, List<Quiz> quizzes) {
        Label titleLabel = new Label("Quizzes by Category");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #000501;");
        
        java.util.Map<String, Long> categoryCount = quizzes.stream()
            .collect(Collectors.groupingBy(Quiz::getCategory, Collectors.counting()));
        
        VBox statsBox = new VBox(10);
        for (java.util.Map.Entry<String, Long> entry : categoryCount.entrySet()) {
            HBox row = createStatRow(entry.getKey(), entry.getValue().intValue(), quizzes.size());
            statsBox.getChildren().add(row);
        }
        
        container.getChildren().addAll(titleLabel, new Separator(), statsBox);
    }

    private void displayQuizzesByStatus(VBox container, List<Quiz> quizzes) {
        Label titleLabel = new Label("Quizzes by Status");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #000501;");
        
        java.util.Map<String, Long> statusCount = quizzes.stream()
            .collect(Collectors.groupingBy(Quiz::getStatus, Collectors.counting()));
        
        VBox statsBox = new VBox(10);
        for (java.util.Map.Entry<String, Long> entry : statusCount.entrySet()) {
            HBox row = createStatRow(entry.getKey(), entry.getValue().intValue(), quizzes.size());
            statsBox.getChildren().add(row);
        }
        
        container.getChildren().addAll(titleLabel, new Separator(), statsBox);
    }

    private void displayAveragePoints(VBox container, List<Quiz> quizzes) {
        double avgPoints = quizzes.stream()
            .mapToInt(Quiz::getPointsReward)
            .average()
            .orElse(0.0);
        
        Label statLabel = new Label(String.format("%.1f points", avgPoints));
        statLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #456990;");
        
        Label descLabel = new Label("Average points reward per quiz");
        descLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 13px;");
        
        container.getChildren().addAll(statLabel, descLabel);
    }

    private void displayTotalQuestions(VBox container, List<Quiz> quizzes) throws SQLException {
        int totalQuestions = 0;
        for (Quiz quiz : quizzes) {
            totalQuestions += gamificationService.getQuestionCountByQuizId(quiz.getId());
        }
        
        Label statLabel = new Label(totalQuestions + " questions");
        statLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #456990;");
        
        Label descLabel = new Label("Total questions across all quizzes");
        descLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 13px;");
        
        container.getChildren().addAll(statLabel, descLabel);
    }

    private void displayAverageQuestions(VBox container, List<Quiz> quizzes) throws SQLException {
        int totalQuestions = 0;
        for (Quiz quiz : quizzes) {
            totalQuestions += gamificationService.getQuestionCountByQuizId(quiz.getId());
        }
        
        double avgQuestions = quizzes.isEmpty() ? 0 : (double) totalQuestions / quizzes.size();
        
        Label statLabel = new Label(String.format("%.1f questions", avgQuestions));
        statLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #456990;");
        
        Label descLabel = new Label("Average questions per quiz");
        descLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 13px;");
        
        container.getChildren().addAll(statLabel, descLabel);
    }

    private void displayQuizzesByPointsRange(VBox container, List<Quiz> quizzes) {
        Label titleLabel = new Label("Quizzes by Points Range");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #000501;");
        
        int range0_50 = (int) quizzes.stream().filter(q -> q.getPointsReward() <= 50).count();
        int range51_100 = (int) quizzes.stream().filter(q -> q.getPointsReward() > 50 && q.getPointsReward() <= 100).count();
        int range101_200 = (int) quizzes.stream().filter(q -> q.getPointsReward() > 100 && q.getPointsReward() <= 200).count();
        int range201plus = (int) quizzes.stream().filter(q -> q.getPointsReward() > 200).count();
        
        VBox statsBox = new VBox(10);
        statsBox.getChildren().addAll(
            createStatRow("0-50 points", range0_50, quizzes.size()),
            createStatRow("51-100 points", range51_100, quizzes.size()),
            createStatRow("101-200 points", range101_200, quizzes.size()),
            createStatRow("201+ points", range201plus, quizzes.size())
        );
        
        container.getChildren().addAll(titleLabel, new Separator(), statsBox);
    }

    private HBox createStatRow(String label, int count, int total) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8));
        row.setStyle("-fx-background-color: #F7F0F5; -fx-background-radius: 6px;");
        
        Label nameLabel = new Label(label);
        nameLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #000501; -fx-font-weight: 600;");
        nameLabel.setPrefWidth(180);
        
        Label countLabel = new Label(count + " quizzes");
        countLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #456990;");
        countLabel.setPrefWidth(100);
        
        // Progress bar
        double percentage = total > 0 ? (double) count / total * 100 : 0;
        ProgressBar progressBar = new ProgressBar(percentage / 100);
        progressBar.setPrefWidth(150);
        progressBar.setStyle("-fx-accent: #456990;");
        
        Label percentLabel = new Label(String.format("%.1f%%", percentage));
        percentLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6B7280;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        row.getChildren().addAll(nameLabel, countLabel, progressBar, percentLabel);
        
        return row;
    }

    @FXML
    private void handleManageQuestions() {
        if (selectedQuiz == null) {
            showError("Please select a quiz first!");
            return;
        }
        
        openQuestionManagementDialog(selectedQuiz);
    }

    private void openQuestionManagementDialog(Quiz quiz) {
        Stage dialog = new Stage();
        dialog.setTitle("Manage Questions - " + quiz.getTitle());
        
        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #F7F0F5;");
        
        // Title
        Label titleLabel = new Label("Questions for: " + quiz.getTitle());
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #000501;");
        
        // Question form
        GridPane formGrid = new GridPane();
        formGrid.setHgap(15);
        formGrid.setVgap(12);
        formGrid.setPadding(new Insets(15));
        formGrid.setStyle("-fx-background-color: white; -fx-background-radius: 8px; -fx-border-color: #456990; -fx-border-width: 2px; -fx-border-radius: 8px;");
        
        Label questionLabel = new Label("Question:");
        questionLabel.setStyle("-fx-font-weight: 600; -fx-text-fill: #000501;");
        TextArea questionTextArea = new TextArea();
        questionTextArea.setPromptText("Enter question text");
        questionTextArea.setPrefRowCount(2);
        questionTextArea.setStyle("-fx-background-color: white; -fx-border-color: #456990; -fx-border-radius: 6px; -fx-padding: 8px;");
        
        formGrid.add(questionLabel, 0, 0);
        formGrid.add(questionTextArea, 1, 0);
        GridPane.setHgrow(questionTextArea, Priority.ALWAYS);
        
        // Options container
        Label optionsLabel = new Label("Options:");
        optionsLabel.setStyle("-fx-font-weight: 600; -fx-text-fill: #000501;");
        
        VBox optionsContainer = new VBox(8);
        optionsContainer.setStyle("-fx-background-color: #F7F0F5; -fx-padding: 10px; -fx-background-radius: 6px;");
        
        // List to track option fields
        List<HBox> optionRows = new ArrayList<>();
        
        // Function to create option row
        Runnable createOptionRow = new Runnable() {
            @Override
            public void run() {
                HBox optionRow = new HBox(8);
                optionRow.setAlignment(Pos.CENTER_LEFT);
                
                CheckBox correctCheck = new CheckBox();
                correctCheck.setStyle("-fx-text-fill: #000501;");
                correctCheck.setTooltip(new Tooltip("Mark as correct answer"));
                
                TextField optionField = new TextField();
                optionField.setPromptText("Option " + (optionRows.size() + 1));
                optionField.setStyle("-fx-background-color: white; -fx-border-color: #456990; -fx-border-radius: 6px; -fx-padding: 8px;");
                HBox.setHgrow(optionField, Priority.ALWAYS);
                
                Button removeBtn = new Button("−");
                removeBtn.setStyle("-fx-background-color: #A62639; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-padding: 4 12 4 12; -fx-cursor: hand;");
                removeBtn.setTooltip(new Tooltip("Remove option"));
                removeBtn.setOnAction(e -> {
                    optionsContainer.getChildren().remove(optionRow);
                    optionRows.remove(optionRow);
                    // Update option numbers
                    for (int i = 0; i < optionRows.size(); i++) {
                        HBox row = optionRows.get(i);
                        TextField field = (TextField) row.getChildren().get(1);
                        if (field.getText().isEmpty()) {
                            field.setPromptText("Option " + (i + 1));
                        }
                    }
                });
                
                // Disable remove button if only 2 options left
                removeBtn.setDisable(optionRows.size() < 2);
                
                optionRow.getChildren().addAll(correctCheck, optionField, removeBtn);
                optionRows.add(optionRow);
                optionsContainer.getChildren().add(optionRow);
                
                // Update remove buttons state
                for (HBox row : optionRows) {
                    Button btn = (Button) row.getChildren().get(2);
                    btn.setDisable(optionRows.size() <= 2);
                }
            }
        };
        
        // Add initial 2 options
        createOptionRow.run();
        createOptionRow.run();
        
        // Add option button
        Button addOptionBtn = new Button("+ Add Option");
        addOptionBtn.setStyle("-fx-background-color: #9B7E46; -fx-text-fill: white; -fx-font-weight: 600; -fx-background-radius: 6px; -fx-padding: 8 16 8 16; -fx-cursor: hand;");
        addOptionBtn.setOnAction(e -> createOptionRow.run());
        
        VBox optionsSection = new VBox(8);
        optionsSection.getChildren().addAll(optionsLabel, optionsContainer, addOptionBtn);
        
        formGrid.add(optionsSection, 0, 1, 2, 1);
        
        // Buttons
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        
        Button addBtn = new Button("Add Question");
        addBtn.setStyle("-fx-background-color: #456990; -fx-text-fill: white; -fx-font-weight: 600; -fx-background-radius: 8px; -fx-padding: 10 20 10 20; -fx-cursor: hand;");
        
        Button clearBtn = new Button("Clear");
        clearBtn.setStyle("-fx-background-color: #9B7E46; -fx-text-fill: white; -fx-font-weight: 600; -fx-background-radius: 8px; -fx-padding: 10 20 10 20; -fx-cursor: hand;");
        
        buttonBox.getChildren().addAll(addBtn, clearBtn);
        
        // Questions list
        Label listLabel = new Label("Existing Questions:");
        listLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #000501;");
        
        VBox questionsListContainer = new VBox(10);
        ScrollPane scrollPane = new ScrollPane(questionsListContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(300);
        scrollPane.setStyle("-fx-background-color: white; -fx-border-color: #456990; -fx-border-width: 2px; -fx-border-radius: 8px;");
        
        // Load existing questions
        loadQuestionsIntoDialog(quiz.getId(), questionsListContainer);
        
        // Add question handler
        addBtn.setOnAction(e -> {
            try {
                String questionText = questionTextArea.getText();
                
                if (questionText == null || questionText.trim().isEmpty()) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Validation Error");
                    alert.setHeaderText("Missing Information");
                    alert.setContentText("Please enter question text!");
                    alert.showAndWait();
                    return;
                }
                
                // Validate options
                List<String> optionTexts = new ArrayList<>();
                List<Boolean> correctFlags = new ArrayList<>();
                
                for (HBox row : optionRows) {
                    CheckBox check = (CheckBox) row.getChildren().get(0);
                    TextField field = (TextField) row.getChildren().get(1);
                    String text = field.getText();
                    
                    if (text == null || text.trim().isEmpty()) {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Validation Error");
                        alert.setHeaderText("Missing Information");
                        alert.setContentText("Please fill in all options!");
                        alert.showAndWait();
                        return;
                    }
                    
                    optionTexts.add(text.trim());
                    correctFlags.add(check.isSelected());
                }
                
                // Check at least one correct answer
                if (!correctFlags.contains(true)) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Validation Error");
                    alert.setHeaderText("No Correct Answer");
                    alert.setContentText("Please mark at least one option as correct!");
                    alert.showAndWait();
                    return;
                }
                
                // Create question
                Question question = new Question();
                question.setQuizId(quiz.getId());
                question.setQuestionText(questionText.trim());
                
                long questionId = gamificationService.addQuestion(question);
                
                // Add options
                for (int i = 0; i < optionTexts.size(); i++) {
                    QuestionOption option = new QuestionOption();
                    option.setQuestionId(questionId);
                    option.setOptionText(optionTexts.get(i));
                    option.setCorrect(correctFlags.get(i));
                    option.setOptionOrder(i + 1);
                    gamificationService.addQuestionOption(option);
                }
                
                // Clear form
                questionTextArea.clear();
                optionsContainer.getChildren().clear();
                optionRows.clear();
                createOptionRow.run();
                createOptionRow.run();
                
                // Reload questions
                loadQuestionsIntoDialog(quiz.getId(), questionsListContainer);
                
                Alert success = new Alert(Alert.AlertType.INFORMATION);
                success.setTitle("Success");
                success.setHeaderText(null);
                success.setContentText("Question saved successfully!");
                success.showAndWait();
                
            } catch (SQLException ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Database Error");
                alert.setHeaderText("Error saving question");
                alert.setContentText(ex.getMessage());
                alert.showAndWait();
            }
        });
        
        clearBtn.setOnAction(e -> {
            questionTextArea.clear();
            optionsContainer.getChildren().clear();
            optionRows.clear();
            createOptionRow.run();
            createOptionRow.run();
        });
        
        root.getChildren().addAll(titleLabel, formGrid, buttonBox, new Separator(), listLabel, scrollPane);
        
        Scene scene = new Scene(root, 700, 750);
        dialog.setScene(scene);
        dialog.show();
    }

    private void loadQuestionsIntoDialog(long quizId, VBox container) {
        container.getChildren().clear();
        
        try {
            List<Question> questions = gamificationService.getQuestionsByQuizId(quizId);
            
            if (questions.isEmpty()) {
                Label emptyLabel = new Label("No questions yet. Add your first question!");
                emptyLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-style: italic; -fx-padding: 20px;");
                container.getChildren().add(emptyLabel);
                return;
            }
            
            int questionNum = 1;
            for (Question question : questions) {
                VBox questionCard = new VBox(8);
                questionCard.setPadding(new Insets(12));
                questionCard.setStyle("-fx-background-color: #F7F0F5; -fx-background-radius: 8px; -fx-border-color: #456990; -fx-border-width: 1px; -fx-border-radius: 8px;");
                
                Label questionLabel = new Label("Q" + questionNum + ": " + question.getQuestionText());
                questionLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #000501; -fx-font-size: 13px;");
                questionLabel.setWrapText(true);
                
                VBox optionsBox = new VBox(4);
                optionsBox.setPadding(new Insets(5, 0, 5, 15));
                
                List<QuestionOption> options = question.getOptions();
                for (int i = 0; i < options.size(); i++) {
                    QuestionOption option = options.get(i);
                    Label optLabel = new Label((i + 1) + ". " + option.getOptionText());
                    
                    if (option.isCorrect()) {
                        optLabel.setStyle("-fx-text-fill: #28A745; -fx-font-weight: 600; -fx-font-size: 12px;");
                    } else {
                        optLabel.setStyle("-fx-text-fill: #000501; -fx-font-size: 12px;");
                    }
                    optLabel.setWrapText(true);
                    optionsBox.getChildren().add(optLabel);
                }
                
                HBox buttonBox = new HBox(8);
                buttonBox.setAlignment(Pos.CENTER_RIGHT);
                
                Button deleteBtn = new Button("Delete");
                deleteBtn.setStyle("-fx-background-color: #A62639; -fx-text-fill: white; -fx-font-size: 11px; -fx-background-radius: 6px; -fx-padding: 6 12 6 12; -fx-cursor: hand;");
                deleteBtn.setOnAction(e -> {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Delete Question");
                    confirm.setHeaderText("Are you sure?");
                    confirm.setContentText("Do you want to delete this question?");
                    
                    confirm.showAndWait().ifPresent(response -> {
                        if (response == ButtonType.OK) {
                            try {
                                gamificationService.deleteQuestion(question.getId());
                                loadQuestionsIntoDialog(quizId, container);
                            } catch (SQLException ex) {
                                Alert alert = new Alert(Alert.AlertType.ERROR);
                                alert.setTitle("Error");
                                alert.setContentText("Error deleting question: " + ex.getMessage());
                                alert.showAndWait();
                            }
                        }
                    });
                });
                
                buttonBox.getChildren().add(deleteBtn);
                
                questionCard.getChildren().addAll(questionLabel, optionsBox, buttonBox);
                container.getChildren().add(questionCard);
                questionNum++;
            }
            
        } catch (SQLException e) {
            Label errorLabel = new Label("Error loading questions: " + e.getMessage());
            errorLabel.setStyle("-fx-text-fill: #DC3545; -fx-padding: 20px;");
            container.getChildren().add(errorLabel);
        }
    }
}
