package edu.connexion3a8.controllers.collaboration;

import edu.connexion3a8.InvestiApp;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import java.io.File;

public class ProductManagementController {

    @FXML
    private TextField imageUrlField;

    @FXML
    private void handleAddProduct() {
        // TODO: Implement add product functionality
        System.out.println("Add product clicked");
    }

    @FXML
    private void handleRefresh() {
        // TODO: Implement refresh functionality
        System.out.println("Refresh clicked");
    }

    @FXML
    private void handleEditProduct() {
        // TODO: Implement edit product functionality
        System.out.println("Edit product clicked");
    }

    @FXML
    private void handleDeleteProduct() {
        // TODO: Implement delete product functionality
        System.out.println("Delete product clicked");
    }

    @FXML
    private void handleGoHome() {
        try {
            InvestiApp.showHomePage();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleGoAdminDashboard() {
        try {
            InvestiApp.showAdminDashboard();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBrowseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Product Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.webp"));
        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null && imageUrlField != null) {
            imageUrlField.setText(selectedFile.getAbsolutePath());
        }
    }

    @FXML
    private void handleSave() {
        // TODO: Implement save product functionality
        System.out.println("Save product clicked");
    }

    @FXML
    private void handleCancel() {
        // TODO: Implement cancel functionality
        System.out.println("Cancel clicked");
    }
}