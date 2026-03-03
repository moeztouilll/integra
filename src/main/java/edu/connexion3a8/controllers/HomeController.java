package edu.connexion3a8.controllers;

import edu.connexion3a8.InvestiApp;
import edu.connexion3a8.entities.User;
import edu.connexion3a8.services.UserService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;

public class HomeController {

    @FXML
    private Button adminDashboardBtn;
    @FXML
    private HBox kycBanner;
    @FXML
    private HBox kycPendingBanner;
    @FXML
    private Button uploadIdBtn;

    private User currentUser;
    private UserService userService = new UserService();

    public void setCurrentUser(User user) {
        this.currentUser = user;

        // Show dashboard button for admin, innovator, and investor
        if (user != null && ("admin".equalsIgnoreCase(user.getRole()) ||
                "innovator".equalsIgnoreCase(user.getRole()) ||
                "investor".equalsIgnoreCase(user.getRole()))) {
            adminDashboardBtn.setVisible(true);
            adminDashboardBtn.setManaged(true);

            // Update button text based on role if needed
            if ("admin".equalsIgnoreCase(user.getRole())) {
                adminDashboardBtn.setText("⚙ Admin Dashboard");
            } else {
                adminDashboardBtn.setText("⚙ My Dashboard");
            }
        } else {
            adminDashboardBtn.setVisible(false);
            adminDashboardBtn.setManaged(false);
        }

        // KYC banner logic
        if (user != null && !user.isActive() && !"admin".equals(user.getRole())) {
            if (user.getIdImageUrl() != null && !user.getIdImageUrl().isEmpty()) {
                // ID already uploaded, pending review
                kycPendingBanner.setVisible(true);
                kycPendingBanner.setManaged(true);
                kycBanner.setVisible(false);
                kycBanner.setManaged(false);
            } else {
                // Needs to upload ID
                kycBanner.setVisible(true);
                kycBanner.setManaged(true);
                kycPendingBanner.setVisible(false);
                kycPendingBanner.setManaged(false);
            }
        } else {
            kycBanner.setVisible(false);
            kycBanner.setManaged(false);
            kycPendingBanner.setVisible(false);
            kycPendingBanner.setManaged(false);
        }
    }

    @FXML
    private void handleUploadId() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select your ID image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.bmp"));

        File selectedFile = fileChooser.showOpenDialog(uploadIdBtn.getScene().getWindow());
        if (selectedFile != null) {
            try {
                // Create uploads directory if it doesn't exist
                Path uploadsDir = Paths.get("uploads", "kyc");
                Files.createDirectories(uploadsDir);

                // Copy file with unique name: userId_filename
                String fileName = currentUser.getId() + "_" + selectedFile.getName();
                Path destination = uploadsDir.resolve(fileName);
                Files.copy(selectedFile.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);

                // Save path in DB
                userService.updateIdImageUrl(currentUser.getId(), destination.toString());
                currentUser.setIdImageUrl(destination.toString());

                // Switch to pending banner
                kycBanner.setVisible(false);
                kycBanner.setManaged(false);
                kycPendingBanner.setVisible(true);
                kycPendingBanner.setManaged(true);

            } catch (IOException | SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleEvents() {
        try {
            InvestiApp.showEventsPage(currentUser);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCollaboration() {
        try {
            InvestiApp.showCollaborationModule(currentUser);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAdminDashboard() {
        try {
            InvestiApp.showCollaborationModule(currentUser);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCourses() {
        try {
            InvestiApp.showGamificationMenu(currentUser);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleForum() {
        try {
            InvestiApp.showForum(currentUser);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout() {
        try {
            InvestiApp.showLoginPage();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
