package edu.connexion3a8;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class UserGUIApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/User.fxml"));
            Scene scene = new Scene(root);
            primaryStage.setTitle("Gestion des Utilisateurs");
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
