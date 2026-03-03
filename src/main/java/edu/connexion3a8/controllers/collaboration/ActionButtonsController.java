package edu.connexion3a8.controllers.collaboration;

import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class ActionButtonsController {

    @FXML
    private Button editBtn;

    @FXML
    private Button deleteBtn;

    public Button getEditBtn() {
        return editBtn;
    }

    public Button getDeleteBtn() {
        return deleteBtn;
    }
}
