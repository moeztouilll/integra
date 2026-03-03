package edu.connexion3a8.controllers.collaboration.Project;

import edu.connexion3a8.controllers.collaboration.AlertHelper;
import edu.connexion3a8.entities.User;
import edu.connexion3a8.entities.collaboration.Project;
import edu.connexion3a8.services.collaboration.CurrencyService;
import edu.connexion3a8.services.collaboration.ProjectService;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class AddProjectController implements Initializable {

    private final ProjectService ps = new ProjectService();
    private User currentUser;

    @FXML
    private TextField titleTf;
    @FXML
    private TextField descriptionTf;
    @FXML
    private TextField amountTf;
    @FXML
    private TextField equityTf;
    @FXML
    private ComboBox<String> categoryBox;
    @FXML
    private ComboBox<String> currencyBox;
    @FXML
    private Label convertedAmountLabel;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Category options
        categoryBox.setItems(FXCollections.observableArrayList(
                "Tech", "Health", "Education", "Finance", "Other"));
        categoryBox.setValue("Tech");

        // Currency options
        currencyBox.setItems(FXCollections.observableArrayList(
                CurrencyService.getSupportedCurrencies()));
        currencyBox.setValue("USD");

        // Live conversion preview
        amountTf.textProperty().addListener((obs, oldVal, newVal) -> updateConversionPreview());
        currencyBox.setOnAction(e -> updateConversionPreview());
    }

    private void updateConversionPreview() {
        try {
            double amount = Double.parseDouble(amountTf.getText());
            String currency = currencyBox.getValue();
            if (currency != null && !currency.equals("USD")) {
                double converted = CurrencyService.convertFromUSD(amount, currency);
                convertedAmountLabel.setText("≈ " + CurrencyService.format(converted, currency));
            } else {
                convertedAmountLabel.setText("");
            }
        } catch (NumberFormatException e) {
            convertedAmountLabel.setText("");
        }
    }

    @FXML
    void saveProject(ActionEvent event) {
        if (!validateProjectInputs())
            return;

        if (currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "User session not found. Please log in again.");
            return;
        }

        try {
            String entrepreneurId = currentUser.getId();

            // Convert entered amount to USD for storage
            double enteredAmount = Double.parseDouble(amountTf.getText());
            String selectedCurrency = currencyBox.getValue();
            double amountUSD = CurrencyService.convertToUSD(enteredAmount, selectedCurrency);

            Project p = new Project(
                    entrepreneurId,
                    titleTf.getText(),
                    descriptionTf.getText(),
                    amountUSD,
                    Double.parseDouble(equityTf.getText()),
                    "UNDER_REVIEW",
                    categoryBox.getValue());

            ps.addEntity(p);
            showAlert(Alert.AlertType.INFORMATION, "Success",
                    "Project submitted for review. Amount stored as $" + String.format("%.2f", amountUSD) + " USD.");
            closeStage(event);
        } catch (SQLException | NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Error", e.getMessage());
        }
    }

    private boolean validateProjectInputs() {
        boolean isValid = true;
        clearErrorStyles();
        StringBuilder errors = new StringBuilder();

        if (titleTf.getText().length() < 3) {
            setErrorStyle(titleTf);
            errors.append("- Title must be at least 3 characters.\n");
            isValid = false;
        }
        if (descriptionTf.getText().length() < 10) {
            setErrorStyle(descriptionTf);
            errors.append("- Description must be at least 10 characters.\n");
            isValid = false;
        }
        if (amountTf.getText().isEmpty() || !isNumeric(amountTf.getText())) {
            setErrorStyle(amountTf);
            errors.append("- Target Amount must be a valid number.\n");
            isValid = false;
        } else {
            double amount = Double.parseDouble(amountTf.getText());
            if (amount <= 0) {
                setErrorStyle(amountTf);
                errors.append("- Target Amount must be positive.\n");
                isValid = false;
            }
        }
        if (equityTf.getText().isEmpty() || !isNumeric(equityTf.getText())) {
            setErrorStyle(equityTf);
            errors.append("- Equity must be a valid number.\n");
            isValid = false;
        } else {
            double equity = Double.parseDouble(equityTf.getText());
            if (equity <= 0 || equity > 100) {
                setErrorStyle(equityTf);
                errors.append("- Equity must be between 0 and 100%.\n");
                isValid = false;
            }
        }

        if (isValid) {
            boolean exists = new edu.connexion3a8.services.collaboration.ProjectService()
                    .existsByTitleOrDescription(titleTf.getText(), descriptionTf.getText());
            if (exists) {
                setErrorStyle(titleTf);
                setErrorStyle(descriptionTf);
                errors.append("- A project with this exact Title OR Description already exists.\n");
                isValid = false;
            }
        }

        if (!isValid) {
            showAlert(Alert.AlertType.ERROR, "Validation Error",
                    "Please correct the following:\n" + errors.toString());
        }
        return isValid;
    }

    private void setErrorStyle(TextField tf) {
        if (!tf.getStyleClass().contains("error"))
            tf.getStyleClass().add("error");
    }

    private void clearErrorStyles() {
        titleTf.getStyleClass().remove("error");
        descriptionTf.getStyleClass().remove("error");
        amountTf.getStyleClass().remove("error");
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

    private void showAlert(Alert.AlertType type, String title, String msg) {
        if (type == Alert.AlertType.ERROR)
            AlertHelper.showError(title, msg);
        else if (type == Alert.AlertType.WARNING)
            AlertHelper.showWarning(title, msg);
        else
            AlertHelper.showInfo(title, msg);
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
