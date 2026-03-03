package edu.connexion3a8.controllers.collaboration.Project;

import edu.connexion3a8.entities.collaboration.Project;
import edu.connexion3a8.services.collaboration.ProjectService;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.Stage;
import edu.connexion3a8.controllers.collaboration.AlertHelper;

import java.net.URL;
import java.util.ResourceBundle;

public class UpdateProjectController implements Initializable {

    private final ProjectService ps = new ProjectService();

    @FXML
    private TextField idTf;
    @FXML
    private TextField titleTf;
    @FXML
    private TextField descTf;
    @FXML
    private TextField amountTf;
    @FXML
    private TextField equityTf;
    @FXML
    private TextField statusTf;
    @FXML
    private ComboBox<String> categoryBox;

    private String originalStatus;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        categoryBox.setItems(FXCollections.observableArrayList(
                "Tech", "Health", "Education", "Finance", "Other"));
    }

    public void initData(Project p) {
        this.originalStatus = p.getStatus();
        idTf.setText(String.valueOf(p.getProjectId()));
        idTf.setDisable(true);
        titleTf.setText(p.getTitle());
        descTf.setText(p.getDescription());
        amountTf.setText(String.valueOf(p.getAmountRequested()));
        equityTf.setText(String.valueOf(p.getEquityOffered()));
        statusTf.setText(p.getStatus());
        statusTf.setDisable(true);
        categoryBox.setValue(p.getCategory() != null ? p.getCategory() : "Other");
    }

    @FXML
    void updateProject(ActionEvent event) {
        if (!validateUpdate())
            return;

        try {
            Project p = new Project();
            p.setProjectId(Integer.parseInt(idTf.getText()));
            p.setTitle(titleTf.getText());
            p.setDescription(descTf.getText());
            p.setAmountRequested(Double.parseDouble(amountTf.getText()));
            p.setEquityOffered(Double.parseDouble(equityTf.getText()));
            p.setStatus("UNDER_REVIEW"); // Reset for re-validation
            p.setCategory(categoryBox.getValue());

            boolean success = ps.update(p.getProjectId(), p);
            if (success) {
                AlertHelper.showInfo("Success",
                        "Project updated. It will need to be re-validated by admin.");
                closeStage(event);
            } else {
                AlertHelper.showError("Error", "Project update failed.");
            }
        } catch (NumberFormatException e) {
            AlertHelper.showError("Error", "Invalid number format in Amount or Equity fields.");
        }
    }

    private boolean validateUpdate() {
        boolean isValid = true;
        StringBuilder errors = new StringBuilder();
        clearErrorStyles();

        if (titleTf.getText().isEmpty()) {
            setErrorStyle(titleTf);
            errors.append("- Title cannot be empty.\n");
            isValid = false;
        }
        if (descTf.getText().isEmpty()) {
            setErrorStyle(descTf);
            errors.append("- Description cannot be empty.\n");
            isValid = false;
        }

        if (amountTf.getText().isEmpty() || !isNumeric(amountTf.getText())) {
            setErrorStyle(amountTf);
            errors.append("- Amount must be a valid number.\n");
            isValid = false;
        } else if (Double.parseDouble(amountTf.getText()) <= 0) {
            setErrorStyle(amountTf);
            errors.append("- Amount must be positive.\n");
            isValid = false;
        }
        if (equityTf.getText().isEmpty() || !isNumeric(equityTf.getText())) {
            setErrorStyle(equityTf);
            errors.append("- Equity must be a valid number.\n");
            isValid = false;
        } else {
            double equity = Double.parseDouble(equityTf.getText());
            if (equity <= 0 || equity > 100) {
                setErrorStyle(equityTf);
                errors.append("- Equity must be 0-100%.\n");
                isValid = false;
            }
        }

        if (!isValid)
            AlertHelper.showError("Validation Error", "Please correct:\n" + errors);
        return isValid;
    }

    private void setErrorStyle(TextField tf) {
        if (!tf.getStyleClass().contains("error"))
            tf.getStyleClass().add("error");
    }

    private void clearErrorStyles() {
        idTf.getStyleClass().remove("error");
        titleTf.getStyleClass().remove("error");
        descTf.getStyleClass().remove("error");
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

    @FXML
    void goMain(ActionEvent event) {
        closeStage(event);
    }

    private void closeStage(ActionEvent event) {
        Node source = (Node) event.getSource();
        ((Stage) source.getScene().getWindow()).close();
    }
}
