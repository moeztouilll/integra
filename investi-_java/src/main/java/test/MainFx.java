package test;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class MainFx extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        stage.initStyle(StageStyle.UNDECORATED);
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/main-view.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("PIDEV Admin Dashboard");
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        stage.show();
    }
}
