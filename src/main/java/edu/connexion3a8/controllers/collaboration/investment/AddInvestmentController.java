package edu.connexion3a8.controllers.collaboration.investment;

import edu.connexion3a8.entities.User;
import edu.connexion3a8.entities.collaboration.Investment;
import edu.connexion3a8.services.collaboration.InvestmentService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import edu.connexion3a8.controllers.collaboration.AlertHelper;

import java.sql.SQLException;

public class AddInvestmentController {

    private final InvestmentService is = new InvestmentService();
    private User currentUser;

    @FXML
    private TextField projectTf;

    @FXML
    private TextField amountTf;
    @FXML
    private TextField durationTf;
    @FXML
    private TextField equityTf;

    public void setTargetProjectId(int id) {
        projectTf.setText(String.valueOf(id));
        projectTf.setEditable(false);
    }

    @FXML
    void saveInvestment(ActionEvent event) {
        if (!validateInvestment())
            return;

        if (currentUser == null) {
            AlertHelper.showError("Error", "User session not found. Please log in again.");
            return;
        }

        double total = Double.parseDouble(amountTf.getText());
        int duration = Integer.parseInt(durationTf.getText());

        String investorId = currentUser.getId();

        Investment i = new Investment(
                Integer.parseInt(projectTf.getText()),
                investorId,
                total,
                duration,
                total / duration,
                Double.parseDouble(equityTf.getText()),
                "UNDER_REVIEW"); // Initial status for Admin validation

        try {
            is.addEntity(i);

            // Email notification moved to Admin approval stage
            AlertHelper.showInfo("Success", "Investment added successfully and sent to Admin for review.");
            closeStage(event);
        } catch (SQLException e) {
            AlertHelper.showError("Error", e.getMessage());
        }
    }

    private boolean validateInvestment() {
        boolean isValid = true;
        clearErrorStyles();
        StringBuilder errors = new StringBuilder();

        // Check Project ID
        if (projectTf.getText().isEmpty() || !isNumeric(projectTf.getText())) {
            setErrorStyle(projectTf);
            errors.append("- Project ID must be a valid number.\n");
            isValid = false;
        }

        // Check Total Amount
        if (amountTf.getText().isEmpty() || !isNumeric(amountTf.getText())) {
            setErrorStyle(amountTf);
            errors.append("- Total Amount must be a valid number.\n");
            isValid = false;
        } else {
            // Only check range if it's a valid number
            double amount = Double.parseDouble(amountTf.getText());
            if (amount <= 0) {
                setErrorStyle(amountTf);
                errors.append("- Total Amount must be positive.\n");
                isValid = false;
            }
        }

        // Check Duration
        if (durationTf.getText().isEmpty() || !isNumeric(durationTf.getText())) {
            setErrorStyle(durationTf);
            errors.append("- Duration must be a valid number.\n");
            isValid = false;
        } else {
            // Only check range if it's a valid number
            int duration = Integer.parseInt(durationTf.getText());
            if (duration <= 0) {
                setErrorStyle(durationTf);
                errors.append("- Duration must be positive.\n");
                isValid = false;
            }
        }

        // Check Equity
        if (equityTf.getText().isEmpty() || !isNumeric(equityTf.getText())) {
            setErrorStyle(equityTf);
            errors.append("- Equity must be a valid number.\n");
            isValid = false;
        } else {
            // Only check range if it's a valid number
            double equity = Double.parseDouble(equityTf.getText());
            if (equity <= 0 || equity > 100) {
                setErrorStyle(equityTf);
                errors.append("- Equity must be between 0 and 100%.\n");
                isValid = false;
            }
        }

        if (!isValid) {
            AlertHelper.showError("Validation Error", "Please correct the following:\n" + errors.toString());
        }
        return isValid;
    }

    private void setErrorStyle(TextField tf) {
        if (!tf.getStyleClass().contains("error"))
            tf.getStyleClass().add("error");
    }

    private void clearErrorStyles() {
        projectTf.getStyleClass().remove("error");

        amountTf.getStyleClass().remove("error");
        durationTf.getStyleClass().remove("error");
        equityTf.getStyleClass().remove("error");
    }

    private boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @FXML
    void goMain(javafx.event.ActionEvent event) {
        closeStage(event);
    }

    private void closeStage(ActionEvent event) {
        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }
}
