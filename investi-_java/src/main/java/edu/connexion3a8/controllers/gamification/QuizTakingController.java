package edu.connexion3a8.controllers.gamification;

import edu.connexion3a8.entities.gamification.Course;
import edu.connexion3a8.entities.gamification.Question;
import edu.connexion3a8.entities.gamification.QuestionOption;
import edu.connexion3a8.entities.gamification.Quiz;
import edu.connexion3a8.entities.gamification.Badge;
import edu.connexion3a8.services.gamification.GamificationService;
import edu.connexion3a8.utils.gamification.ThemeManager;
import edu.connexion3a8.utils.gamification.BadgeNotification;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuizTakingController {

    @FXML private Label quizTitleLabel;
    @FXML private Label quizInfoLabel;
    @FXML private Label timerLabel;
    @FXML private Label progressLabel;
    @FXML private Label scoreLabel;
    @FXML private ProgressBar progressBar;
    @FXML private Label questionNumberLabel;
    @FXML private Label questionTextLabel;
    @FXML private VBox optionsContainer;
    @FXML private Button previousBtn;
    @FXML private Button nextBtn;
    @FXML private Button submitBtn;
    @FXML private Button nightModeToggle;

    private GamificationService gamificationService;
    private Quiz currentQuiz;
    private Course currentCourse;
    private List<Question> questions;
    private int currentQuestionIndex = 0;
    private Map<Long, Long> userAnswers; // questionId -> selectedOptionId
    private Timeline timer;
    private int remainingSeconds;
    private String currentUserId = "1"; // TODO: Get from session

    public void initialize() {
        gamificationService = new GamificationService();
        userAnswers = new HashMap<>();
    }

    public void setQuizAndCourse(Quiz quiz, Course course) {
        this.currentQuiz = quiz;
        this.currentCourse = course;
        loadQuizData();
    }

    public void setDarkMode(boolean darkMode) {
        ThemeManager.getInstance().setDarkMode(darkMode);
        javafx.application.Platform.runLater(() -> {
            applyTheme();
            updateThemeButton();
        });
    }

    private void loadQuizData() {
        try {
            // Load questions
            questions = gamificationService.getQuestionsByQuizId(currentQuiz.getId());
            
            if (questions.isEmpty()) {
                showError("This quiz has no questions yet.");
                handleBack();
                return;
            }

            // Load options for each question
            for (Question question : questions) {
                List<QuestionOption> options = gamificationService.getQuestionOptions(question.getId());
                question.setOptions(options);
            }

            // Set quiz info
            quizTitleLabel.setText(currentQuiz.getTitle());
            quizInfoLabel.setText(questions.size() + " questions • " + 
                                 currentQuiz.getPointsReward() + " points • " +
                                 currentQuiz.getDifficultyLevel());

            // Initialize timer
            remainingSeconds = currentQuiz.getTimeLimit() * 60; // Convert minutes to seconds
            System.out.println("Quiz time limit: " + currentQuiz.getTimeLimit() + " minutes = " + remainingSeconds + " seconds");
            
            if (remainingSeconds <= 0) {
                System.out.println("WARNING: Quiz has no time limit or invalid time limit. Setting default to 10 minutes.");
                remainingSeconds = 600; // Default 10 minutes
            }
            
            startTimer();

            // Display first question
            displayQuestion(0);

        } catch (SQLException e) {
            showError("Error loading quiz: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void startTimer() {
        updateTimerDisplay();
        
        timer = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            remainingSeconds--;
            updateTimerDisplay();
            
            System.out.println("Timer tick: " + remainingSeconds + " seconds remaining");
            
            if (remainingSeconds <= 0) {
                System.out.println("Time's up! Auto-submitting quiz...");
                timer.stop();
                // Use Platform.runLater to ensure UI updates happen on JavaFX thread
                javafx.application.Platform.runLater(() -> {
                    System.out.println("Executing auto-submit...");
                    autoSubmitQuiz();
                });
            }
        }));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();
        System.out.println("Timer started with " + remainingSeconds + " seconds");
    }

    private void updateTimerDisplay() {
        int minutes = remainingSeconds / 60;
        int seconds = remainingSeconds % 60;
        String timeText = String.format("%02d:%02d", minutes, seconds);
        timerLabel.setText(timeText);
        
        // Change color when time is running out
        if (remainingSeconds <= 60) {
            timerLabel.setStyle("-fx-text-fill: #DC3545; -fx-font-weight: bold;");
        } else if (remainingSeconds <= 300) {
            timerLabel.setStyle("-fx-text-fill: #FFA500; -fx-font-weight: bold;");
        }
    }

    private void displayQuestion(int index) {
        currentQuestionIndex = index;
        Question question = questions.get(index);

        // Update progress
        progressLabel.setText("Question " + (index + 1) + " of " + questions.size());
        progressBar.setProgress((double) (index + 1) / questions.size());

        // Update score display
        int answeredCount = userAnswers.size();
        scoreLabel.setText("Answered: " + answeredCount + "/" + questions.size());

        // Update question display
        questionNumberLabel.setText("Question " + (index + 1));
        questionTextLabel.setText(question.getQuestionText());

        // Display options
        displayOptions(question);

        // Update navigation buttons
        previousBtn.setDisable(index == 0);
        
        if (index == questions.size() - 1) {
            nextBtn.setVisible(false);
            submitBtn.setVisible(true);
        } else {
            nextBtn.setVisible(true);
            submitBtn.setVisible(false);
        }
    }

    private void displayOptions(Question question) {
        optionsContainer.getChildren().clear();
        
        ToggleGroup optionGroup = new ToggleGroup();
        Long selectedOptionId = userAnswers.get(question.getId());

        for (QuestionOption option : question.getOptions()) {
            RadioButton radioButton = new RadioButton(option.getOptionText());
            radioButton.setToggleGroup(optionGroup);
            radioButton.setUserData(option.getId());
            radioButton.getStyleClass().add("option-radio");
            
            // Restore previous selection if exists
            if (selectedOptionId != null && selectedOptionId == option.getId()) {
                radioButton.setSelected(true);
            }

            // Style the radio button container
            HBox optionBox = new HBox(radioButton);
            optionBox.getStyleClass().add("option-box");
            optionBox.setPadding(new Insets(15));
            optionBox.setAlignment(Pos.CENTER_LEFT);
            
            // Add hover effect
            optionBox.setOnMouseEntered(e -> {
                if (!radioButton.isSelected()) {
                    optionBox.setStyle(optionBox.getStyle() + "-fx-background-color: " + 
                        (ThemeManager.getInstance().isDarkMode() ? "rgba(155,126,70,0.1)" : "#F7F0F5") + ";");
                }
            });
            optionBox.setOnMouseExited(e -> {
                if (!radioButton.isSelected()) {
                    optionBox.setStyle("");
                }
            });

            // Handle selection
            radioButton.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                if (isSelected) {
                    userAnswers.put(question.getId(), (Long) radioButton.getUserData());
                    updateScoreDisplay();
                    
                    // Update all option boxes styling
                    for (javafx.scene.Node node : optionsContainer.getChildren()) {
                        if (node instanceof HBox) {
                            HBox box = (HBox) node;
                            RadioButton rb = (RadioButton) box.getChildren().get(0);
                            if (rb.isSelected()) {
                                box.getStyleClass().add("option-selected");
                            } else {
                                box.getStyleClass().remove("option-selected");
                            }
                        }
                    }
                }
            });

            // Set initial selected style
            if (radioButton.isSelected()) {
                optionBox.getStyleClass().add("option-selected");
            }

            optionsContainer.getChildren().add(optionBox);
        }
    }

    private void updateScoreDisplay() {
        int answeredCount = userAnswers.size();
        scoreLabel.setText("Answered: " + answeredCount + "/" + questions.size());
    }

    @FXML
    private void handlePrevious() {
        if (currentQuestionIndex > 0) {
            displayQuestion(currentQuestionIndex - 1);
        }
    }

    @FXML
    private void handleNext() {
        if (currentQuestionIndex < questions.size() - 1) {
            displayQuestion(currentQuestionIndex + 1);
        }
    }

    @FXML
    private void handleSubmit() {
        // Check if all questions are answered
        if (userAnswers.size() < questions.size()) {
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Incomplete Quiz");
            confirmAlert.setHeaderText("⚠️ You haven't answered all questions");
            confirmAlert.setContentText("You've answered " + userAnswers.size() + " out of " + 
                                       questions.size() + " questions.\n\nDo you want to submit anyway?");
            
            // Style the dialog
            styleDialog(confirmAlert);
            
            confirmAlert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    submitQuiz();
                }
            });
        } else {
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Submit Quiz");
            confirmAlert.setHeaderText("✅ Ready to submit?");
            confirmAlert.setContentText("You've answered all " + questions.size() + 
                                       " questions.\n\nAre you sure you want to submit your quiz?");
            
            // Style the dialog
            styleDialog(confirmAlert);
            
            confirmAlert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    submitQuiz();
                }
            });
        }
    }

    private void autoSubmitQuiz() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Time's Up!");
        alert.setHeaderText("⏰ Quiz time has expired");
        alert.setContentText("Your quiz will be submitted automatically.\n\n" +
                           "Answered: " + userAnswers.size() + "/" + questions.size() + " questions");
        
        // Style the dialog
        styleDialog(alert);
        
        alert.showAndWait();
        
        submitQuiz();
    }

    private void submitQuiz() {
        System.out.println("=== SUBMIT QUIZ CALLED ===");
        
        if (timer != null) {
            timer.stop();
        }

        // Calculate score
        int correctAnswers = 0;
        for (Question question : questions) {
            Long selectedOptionId = userAnswers.get(question.getId());
            if (selectedOptionId != null) {
                for (QuestionOption option : question.getOptions()) {
                    if (option.getId() == selectedOptionId && option.isCorrect()) {
                        correctAnswers++;
                        break;
                    }
                }
            }
        }

        int totalQuestions = questions.size();
        int scorePercentage = (int) ((correctAnswers * 100.0) / totalQuestions);
        boolean passed = scorePercentage >= currentQuiz.getPassingScore();
        int pointsEarned = passed ? currentQuiz.getPointsReward() : 0;

        System.out.println("📊 Score: " + scorePercentage + "% (" + correctAnswers + "/" + totalQuestions + ")");
        System.out.println("🎯 Passed: " + passed + " (passing score: " + currentQuiz.getPassingScore() + "%)");
        System.out.println("🎯 Points to earn: " + pointsEarned);

        // Save quiz result to database
        try {
            // Calculate time taken (in seconds)
            int timeTaken = (currentQuiz.getTimeLimit() * 60) - remainingSeconds;
            
            // Save quiz completion record
            gamificationService.completeQuiz(currentUserId, currentQuiz.getId(), scorePercentage, timeTaken);
            System.out.println("✅ Quiz result saved to database");
            
            // Check and award badges if passed
            if (passed) {
                List<Badge> newBadges = gamificationService.checkAndAwardBadges(currentUserId);
                
                System.out.println("🏆 Badges check complete. New badges earned: " + newBadges.size());
                for (Badge badge : newBadges) {
                    System.out.println("   - " + badge.getName() + " (" + badge.getPointsRequired() + " points)");
                }
                
                // Send email notifications for new badges
                if (!newBadges.isEmpty()) {
                    sendBadgeEmails(newBadges);
                }
                
                // Show badge notifications if any were earned
                if (!newBadges.isEmpty()) {
                    javafx.application.Platform.runLater(() -> {
                        try {
                            // Get the root pane to show notifications
                            javafx.scene.Parent root = optionsContainer.getScene().getRoot();
                            System.out.println("📱 Root type: " + root.getClass().getName());
                            
                            javafx.scene.layout.Pane targetPane = null;
                            
                            // Handle different root types
                            if (root instanceof javafx.scene.layout.Pane) {
                                targetPane = (javafx.scene.layout.Pane) root;
                            } else if (root instanceof javafx.scene.control.ScrollPane) {
                                // Get the content of ScrollPane
                                javafx.scene.control.ScrollPane scrollPane = (javafx.scene.control.ScrollPane) root;
                                if (scrollPane.getContent() instanceof javafx.scene.layout.Pane) {
                                    targetPane = (javafx.scene.layout.Pane) scrollPane.getContent();
                                    System.out.println("📱 Using ScrollPane content: " + targetPane.getClass().getName());
                                }
                            }
                            
                            if (targetPane != null) {
                                System.out.println("✅ Showing badge notifications...");
                                BadgeNotification.showMultiple(newBadges, targetPane);
                            } else {
                                System.err.println("❌ Could not find suitable Pane for notification");
                            }
                        } catch (Exception e) {
                            System.err.println("❌ Error showing badge notification: " + e.getMessage());
                            e.printStackTrace();
                        }
                    });
                } else {
                    System.out.println("ℹ️ No new badges to show");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Show results
        showResults(correctAnswers, totalQuestions, scorePercentage, pointsEarned);
    }
    
    /**
     * Send email notifications for newly earned badges
     */
    private void sendBadgeEmails(List<Badge> badges) {
        try {
            // Get user email and name from database
            String userEmail = gamificationService.getUserEmail(currentUserId);
            String userName = gamificationService.getUserName(currentUserId);
            
            if (userEmail != null && !userEmail.isEmpty()) {
                edu.connexion3a8.services.gamification.EmailService emailService = 
                    new edu.connexion3a8.services.gamification.EmailService();
                
                for (Badge badge : badges) {
                    emailService.sendBadgeAchievementEmail(userEmail, userName, badge);
                    System.out.println("📧 Sending badge email to: " + userEmail);
                }
            } else {
                System.out.println("⚠️ No email address found for user " + currentUserId);
            }
        } catch (Exception e) {
            System.err.println("❌ Error sending badge emails: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showResults(int correct, int total, int percentage, int points) {
        Alert resultAlert = new Alert(Alert.AlertType.INFORMATION);
        resultAlert.setTitle("Quiz Results");
        
        boolean passed = percentage >= currentQuiz.getPassingScore();
        String emoji = passed ? "🎉" : "📚";
        String status = passed ? "CONGRATULATIONS!" : "Keep Learning!";
        
        resultAlert.setHeaderText(emoji + " " + status);
        
        String resultText = String.format(
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
            "📊 Your Score: %d%%\n\n" +
            "✓ Correct Answers: %d / %d\n" +
            "✗ Wrong Answers: %d\n\n" +
            "🎯 Passing Score: %d%%\n" +
            "📈 Status: %s\n\n" +
            "⭐ Points Earned: %d\n\n" +
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
            percentage,
            correct, total,
            total - correct,
            currentQuiz.getPassingScore(),
            passed ? "✅ PASSED" : "❌ NOT PASSED",
            points
        );
        
        resultAlert.setContentText(resultText);
        
        // Style the dialog
        styleDialog(resultAlert);
        
        resultAlert.showAndWait();
        
        // Return to course content page
        handleBack();
    }
    
    private void styleDialog(Alert alert) {
        // Apply dark mode styling to dialog if dark mode is active
        if (ThemeManager.getInstance().isDarkMode()) {
            javafx.scene.control.DialogPane dialogPane = alert.getDialogPane();
            dialogPane.setStyle(
                "-fx-background-color: #161630; " +
                "-fx-border-color: rgba(70,70,100,0.6); " +
                "-fx-border-width: 2px; " +
                "-fx-border-radius: 12px; " +
                "-fx-background-radius: 12px;"
            );
            
            // Style header
            dialogPane.lookup(".header-panel").setStyle(
                "-fx-background-color: #12122A; " +
                "-fx-background-radius: 12px 12px 0 0;"
            );
            
            // Style labels
            for (javafx.scene.Node node : dialogPane.getChildren()) {
                if (node instanceof javafx.scene.control.Label) {
                    node.setStyle("-fx-text-fill: #F0F2FA;");
                }
            }
            
            // Style content area
            dialogPane.lookup(".content").setStyle("-fx-background-color: #161630;");
            
            // Style buttons
            for (javafx.scene.control.ButtonType buttonType : alert.getButtonTypes()) {
                javafx.scene.control.Button button = (javafx.scene.control.Button) dialogPane.lookupButton(buttonType);
                if (button != null) {
                    if (buttonType == ButtonType.OK || buttonType == ButtonType.YES) {
                        button.setStyle(
                            "-fx-background-color: linear-gradient(to bottom, #E4C45E, #C8A84E, #9B7E46); " +
                            "-fx-text-fill: white; " +
                            "-fx-font-weight: bold; " +
                            "-fx-background-radius: 6px; " +
                            "-fx-padding: 8 20;"
                        );
                    } else {
                        button.setStyle(
                            "-fx-background-color: #2A2A3E; " +
                            "-fx-text-fill: #E8E8E8; " +
                            "-fx-font-weight: bold; " +
                            "-fx-background-radius: 6px; " +
                            "-fx-padding: 8 20;"
                        );
                    }
                }
            }
        } else {
            // Light mode styling
            javafx.scene.control.DialogPane dialogPane = alert.getDialogPane();
            dialogPane.setStyle(
                "-fx-background-color: white; " +
                "-fx-border-color: #456990; " +
                "-fx-border-width: 2px; " +
                "-fx-border-radius: 12px; " +
                "-fx-background-radius: 12px;"
            );
            
            // Style buttons
            for (javafx.scene.control.ButtonType buttonType : alert.getButtonTypes()) {
                javafx.scene.control.Button button = (javafx.scene.control.Button) dialogPane.lookupButton(buttonType);
                if (button != null) {
                    if (buttonType == ButtonType.OK || buttonType == ButtonType.YES) {
                        button.setStyle(
                            "-fx-background-color: linear-gradient(to bottom, #E4C45E, #C8A84E, #9B7E46); " +
                            "-fx-text-fill: white; " +
                            "-fx-font-weight: bold; " +
                            "-fx-background-radius: 6px; " +
                            "-fx-padding: 8 20;"
                        );
                    } else {
                        button.setStyle(
                            "-fx-background-color: #E5E7EB; " +
                            "-fx-text-fill: #000501; " +
                            "-fx-font-weight: bold; " +
                            "-fx-background-radius: 6px; " +
                            "-fx-padding: 8 20;"
                        );
                    }
                }
            }
        }
    }

    @FXML
    private void toggleNightMode() {
        ThemeManager.getInstance().toggleDarkMode();
        applyTheme();
        updateThemeButton();
        
        // Refresh current question display to update option styling
        displayQuestion(currentQuestionIndex);
    }

    private void applyTheme() {
        if (quizTitleLabel != null && quizTitleLabel.getScene() != null) {
            javafx.scene.Parent root = quizTitleLabel.getScene().getRoot();
            
            if (root instanceof ScrollPane) {
                ScrollPane scrollPane = (ScrollPane) root;
                VBox vbox = null;
                
                if (scrollPane.getContent() instanceof VBox) {
                    vbox = (VBox) scrollPane.getContent();
                }
                
                if (ThemeManager.getInstance().isDarkMode()) {
                    // Apply dark mode with multiple properties to ensure it takes effect
                    String darkStyle = "-fx-background: #0A0A18; -fx-background-color: #0A0A18; -fx-border-color: transparent;";
                    scrollPane.setStyle(darkStyle);
                    if (!scrollPane.getStyleClass().contains("dark-mode")) {
                        scrollPane.getStyleClass().add("dark-mode");
                    }
                    
                    if (vbox != null) {
                        if (!vbox.getStyleClass().contains("dark-mode")) {
                            vbox.getStyleClass().add("dark-mode");
                        }
                        // Apply gradient background with fallback
                        String vboxStyle = "-fx-background: linear-gradient(to bottom right, #12122A, #0A0A18, #100F22); -fx-background-color: #0A0A18;";
                        vbox.setStyle(vboxStyle);
                    }
                } else {
                    scrollPane.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
                    scrollPane.getStyleClass().remove("dark-mode");
                    
                    if (vbox != null) {
                        vbox.getStyleClass().remove("dark-mode");
                        vbox.setStyle("");
                    }
                }
            }
        }
    }

    private void updateThemeButton() {
        if (nightModeToggle != null) {
            if (ThemeManager.getInstance().isDarkMode()) {
                nightModeToggle.setText("☀️ Light Mode");
            } else {
                nightModeToggle.setText("🌙 Night Mode");
            }
        }
    }

    @FXML
    private void handleBack() {
        if (timer != null) {
            timer.stop();
        }

        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/CourseContentView.fxml"));
            javafx.scene.Parent root = loader.load();
            
            CourseContentController controller = loader.getController();
            controller.setCourse(currentCourse);
            controller.setDarkMode(ThemeManager.getInstance().isDarkMode());

            javafx.stage.Stage stage = (javafx.stage.Stage) quizTitleLabel.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("Course Content - " + currentCourse.getTitle());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("❌ An error occurred");
        alert.setContentText(message);
        
        // Style the dialog
        styleDialog(alert);
        
        alert.showAndWait();
    }
}
