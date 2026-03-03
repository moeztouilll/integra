package edu.connexion3a8.controllers.collaboration;

import edu.connexion3a8.entities.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;

public class MainController implements javafx.fxml.Initializable {

    private User currentUser;

    @FXML
    private StackPane contentArea;

    @Override
    public void initialize(URL url, java.util.ResourceBundle resourceBundle) {
        showHome(null);
    }

    @FXML
    void showHome(ActionEvent event) {
        loadView("/collaboration/Welcome.fxml");
    }

    @FXML
    void showAdminDashboard(ActionEvent event) {
        loadView("/collaboration/AdminDashboard.fxml");
    }

    @FXML
    void showEntrepreneurDashboard(ActionEvent event) {
        loadView("/collaboration/EntrepreneurDashboard.fxml");
    }

    @FXML
    void showInvestorDashboard(ActionEvent event) {
        loadView("/collaboration/InvestorDashboard.fxml");
    }

    private void loadView(String fxmlPath) {
        try {
            URL resource = getClass().getResource(fxmlPath);
            if (resource == null) {
                System.err.println("Error: FXML file not found: " + fxmlPath);
                return;
            }

            FXMLLoader loader = new FXMLLoader(resource);
            Parent view = loader.load();

            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error loading view: " + fxmlPath);
        }
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }
}
