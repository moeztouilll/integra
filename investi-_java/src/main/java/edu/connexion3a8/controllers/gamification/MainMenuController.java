package edu.connexion3a8.controllers.gamification;

import edu.connexion3a8.entities.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainMenuController {

    @FXML
    private BorderPane mainContainer;
    
    @FXML
    private Label subtitleLabel;
    
    @FXML
    private VBox userMenuContainer;
    
    @FXML
    private VBox adminMenuContainer;
    
    private String userId;
    private String userRole;

    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public void setUser(User user) {
        this.userId = user.getId();
        this.userRole = user.getRole();
        
        // Show admin menu only for admin users
        if ("admin".equalsIgnoreCase(userRole)) {
            adminMenuContainer.setVisible(true);
            adminMenuContainer.setManaged(true);
            subtitleLabel.setText("Admin Dashboard");
        } else {
            adminMenuContainer.setVisible(false);
            adminMenuContainer.setManaged(false);
            subtitleLabel.setText("Explore Courses & Quizzes");
        }
    }

    @FXML
    public void initialize() {
        // Initialization if needed
    }

    @FXML
    private void openCourseForm() {
        // Admin only - manage courses
        if (!"admin".equalsIgnoreCase(userRole)) {
            showAccessDenied();
            return;
        }
        loadView("/gamification/CourseForm.fxml", "Course Management");
    }

    @FXML
    private void openQuizForm() {
        // Admin only - manage quizzes
        if (!"admin".equalsIgnoreCase(userRole)) {
            showAccessDenied();
            return;
        }
        loadView("/gamification/QuizForm.fxml", "Quiz Management");
    }

    @FXML
    private void openBadgeForm() {
        // Admin only - manage badges
        if (!"admin".equalsIgnoreCase(userRole)) {
            showAccessDenied();
            return;
        }
        loadView("/gamification/BadgeForm.fxml", "Badge Management");
    }

    @FXML
    private void openCourseCatalog() {
        // All users can browse courses
        loadView("/gamification/CourseCatalogView.fxml", "Course Catalog");
    }
    
    @FXML
    private void openMyBadges() {
        // All users can view their badges
        loadView("/gamification/BadgeForm.fxml", "My Badges");
    }

    @FXML
    private void openUserView() {
        // Admin only
        if (!"admin".equalsIgnoreCase(userRole)) {
            showAccessDenied();
            return;
        }
        showComingSoon("User Management");
    }

    private void loadView(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();
            
            // Set the loaded view as the center of the BorderPane
            mainContainer.setCenter(view);
            
            // Update window title
            Stage stage = (Stage) mainContainer.getScene().getWindow();
            stage.setTitle("Learning Center - " + title);
            
        } catch (Exception e) {
            e.printStackTrace();
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR
            );
            alert.setTitle("Error");
            alert.setHeaderText("Failed to load view");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void showMainMenu() {
        // Clear center to show default main menu content from FXML
        mainContainer.setCenter(null);
        
        try {
            Stage stage = (Stage) mainContainer.getScene().getWindow();
            stage.setTitle("Learning Center - Main Menu");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @FXML
    private void goBackToHome() {
        try {
            edu.connexion3a8.InvestiApp.showHomePage();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void showAccessDenied() {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.WARNING
        );
        alert.setTitle("Access Denied");
        alert.setHeaderText("Admin Access Required");
        alert.setContentText("This feature is only available to administrators.");
        alert.showAndWait();
    }

    private void showComingSoon(String feature) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.INFORMATION
        );
        alert.setTitle("Coming Soon");
        alert.setHeaderText(feature);
        alert.setContentText("This feature is coming soon!");
        alert.showAndWait();
    }
}
