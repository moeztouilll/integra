package edu.connexion3a8.controllers.collaboration.Project;

import edu.connexion3a8.entities.collaboration.Project;
import edu.connexion3a8.services.collaboration.ProjectService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import edu.connexion3a8.controllers.collaboration.ActionButtonsController;
import edu.connexion3a8.controllers.collaboration.AlertHelper;

import java.io.IOException;
import java.util.List;

public class ShowProjectController {

    private final ProjectService ps = new ProjectService();
    private ObservableList<Project> list;

    @FXML
    private TableView<Project> projectTable;
    @FXML
    private TableColumn<Project, Integer> idCol;
    @FXML
    private TableColumn<Project, String> titleCol;
    @FXML
    private TableColumn<Project, String> descCol;
    @FXML
    private TableColumn<Project, Double> amountCol;
    @FXML
    private TableColumn<Project, Double> equityCol;
    @FXML
    private TableColumn<Project, String> statusCol;
    @FXML
    private TableColumn<Project, Void> actionCol;

    @FXML
    void initialize() {
        idCol.setCellValueFactory(new PropertyValueFactory<>("projectId"));
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        amountCol.setCellValueFactory(new PropertyValueFactory<>("amountRequested"));
        equityCol.setCellValueFactory(new PropertyValueFactory<>("equityOffered"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        loadData();
        addActionButtons();
    }

    private void loadData() {
        List<Project> projects = ps.getData();
        list = FXCollections.observableArrayList(projects);
        projectTable.setItems(list);
    }

    private void addActionButtons() {
        actionCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    try {
                        javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                                getClass().getResource("/ActionButtons.fxml"));
                        javafx.scene.layout.HBox pane = loader.load();
                        ActionButtonsController controller = loader.getController();

                        controller.getEditBtn().setOnAction(e -> {
                            Project p = getTableView().getItems().get(getIndex());
                            if ("FUNDED".equalsIgnoreCase(p.getStatus()) || "CLOSED".equalsIgnoreCase(p.getStatus())) {
                                AlertHelper.showError("Modification Denied",
                                        "Cannot edit a " + p.getStatus() + " project.");
                                return;
                            }
                            try {
                                javafx.fxml.FXMLLoader editLoader = new javafx.fxml.FXMLLoader(
                                        getClass().getResource("/UpdateProject.fxml"));
                                Parent root = editLoader.load();

                                UpdateProjectController updateController = editLoader.getController();
                                updateController.initData(p);

                                Stage stage = new Stage();
                                stage.setScene(new Scene(root));
                                stage.initModality(Modality.APPLICATION_MODAL);
                                stage.showAndWait();
                                loadData(); // Refresh after edit
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        });

                        controller.getDeleteBtn().setOnAction(e -> {
                            Project p = getTableView().getItems().get(getIndex());
                            if (AlertHelper.confirm("Delete Project", "Delete '" + p.getTitle() + "'?")) {
                                ps.deleteEntity(p);
                                loadData();
                            }
                        });

                        setGraphic(pane);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                        setGraphic(null);
                    }
                }
            }
        });
    }

    @FXML
    void goMain(javafx.event.ActionEvent event) {
        navigate(event, "/Main.fxml");
    }

    @FXML
    void goAdd(javafx.event.ActionEvent event) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/AddProject.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
            loadData(); // Refresh after add
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void navigate(javafx.event.ActionEvent event, String fxmlPath) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource(fxmlPath));
            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = (javafx.stage.Stage) ((javafx.scene.Node) event.getSource()).getScene()
                    .getWindow();
            stage.setScene(new javafx.scene.Scene(root));
            stage.show();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }
}
