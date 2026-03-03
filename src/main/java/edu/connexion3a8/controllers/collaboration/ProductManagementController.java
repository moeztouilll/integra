package edu.connexion3a8.controllers.collaboration;

import edu.connexion3a8.InvestiApp;
import javafx.fxml.FXML;

public class ProductManagementController {

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
}