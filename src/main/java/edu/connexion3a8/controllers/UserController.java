package edu.connexion3a8.controllers;

import edu.connexion3a8.entities.User;
import edu.connexion3a8.services.UserService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.util.Callback;

import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserController implements Initializable {
    
    private UserService userService = new UserService();
    private ObservableList<User> userList = FXCollections.observableArrayList();
    private User selectedUser = null;
    
    @FXML
    private TableView<User> userTable;
    @FXML
    private TableColumn<User, String> colId;
    @FXML
    private TableColumn<User, String> colName;
    @FXML
    private TableColumn<User, String> colEmail;
    @FXML
    private TableColumn<User, String> colRole;
    @FXML
    private TableColumn<User, Integer> colPoints;
    @FXML
    private TableColumn<User, Integer> colLevel;
    @FXML
    private TableColumn<User, String> colActions;
    
    @FXML
    private TextField emailField;
    @FXML
    private TextField nameField;
    @FXML
    private TextField passwordField;
    @FXML
    private TextField roleField;
    @FXML
    private TextField bioField;
    
    @FXML
    private Button btnRefresh;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadData();
    }
    
    private void loadData() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colPoints.setCellValueFactory(new PropertyValueFactory<>("points"));
        colLevel.setCellValueFactory(new PropertyValueFactory<>("level"));
        
        Callback<TableColumn<User, String>, TableCell<User, String>> cellFactory = 
            (TableColumn<User, String> param) -> {
            final TableCell<User, String> cell = new TableCell<User, String>() {
                @Override
                public void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                        setText(null);
                    } else {
                        Button deleteBtn = new Button("Supprimer");
                        Button editBtn = new Button("Modifier");
                        
                        deleteBtn.setStyle("-fx-cursor: hand; -fx-background-color: #665E5F; -fx-text-fill: white;");
                        editBtn.setStyle("-fx-cursor: hand; -fx-background-color: #730800; -fx-text-fill: white;");
                        
                        deleteBtn.setOnMouseClicked((MouseEvent event) -> {
                            User user = getTableView().getItems().get(getIndex());
                            try {
                                userService.deleteUser(user.getId());
                                refreshTable();
                                clearFields();
                                selectedUser = null;
                                showAlert("Succès", "Utilisateur supprimé avec succès", Alert.AlertType.INFORMATION);
                            } catch (SQLException ex) {
                                Logger.getLogger(UserController.class.getName()).log(Level.SEVERE, null, ex);
                                showAlert("Erreur", "Erreur lors de la suppression", Alert.AlertType.ERROR);
                            }
                        });
                        
                        editBtn.setOnMouseClicked((MouseEvent event) -> {
                            User user = getTableView().getItems().get(getIndex());
                            selectedUser = user;
                            setTextFields(user);
                        });
                        
                        HBox manageBtn = new HBox(deleteBtn, editBtn);
                        manageBtn.setStyle("-fx-alignment:center");
                        HBox.setMargin(deleteBtn, new Insets(2, 2, 0, 3));
                        HBox.setMargin(editBtn, new Insets(2, 3, 0, 2));
                        
                        setGraphic(manageBtn);
                        setText(null);
                    }
                }
            };
            return cell;
        };
        
        colActions.setCellFactory(cellFactory);
        refreshTable();
    }
    
    @FXML
    private void refreshTable() {
        userList.clear();
        try {
            userList.addAll(userService.getAllUsers());
            userTable.setItems(userList);
        } catch (SQLException ex) {
            Logger.getLogger(UserController.class.getName()).log(Level.SEVERE, null, ex);
            showAlert("Erreur", "Erreur lors du chargement des utilisateurs", Alert.AlertType.ERROR);
        }
    }
    
    @FXML
    private void addUser(ActionEvent event) {
        String email = emailField.getText();
        String name = nameField.getText();
        String password = passwordField.getText();
        String role = roleField.getText();
        String bio = bioField.getText();
        
        if (email.trim().isEmpty() || name.trim().isEmpty() || role.trim().isEmpty()) {
            showAlert("Erreur", "Veuillez remplir tous les champs obligatoires", Alert.AlertType.ERROR);
            return;
        }
        
        try {
            if (selectedUser != null) {
                // Mode modification
                selectedUser.setEmail(email);
                selectedUser.setName(name);
                selectedUser.setRole(role);
                selectedUser.setBio(bio.trim().isEmpty() ? null : bio);
                
                userService.updateUser(selectedUser.getId(), selectedUser);
                showAlert("Succès", "Utilisateur modifié avec succès", Alert.AlertType.INFORMATION);
                selectedUser = null;
            } else {
                // Mode ajout
                if (password.trim().isEmpty()) {
                    showAlert("Erreur", "Le mot de passe est obligatoire pour un nouvel utilisateur", Alert.AlertType.ERROR);
                    return;
                }
                
                User user = new User(email, password, name, role);
                if (!bio.trim().isEmpty()) {
                    user.setBio(bio);
                }
                
                userService.addUser(user);
                showAlert("Succès", "Utilisateur ajouté avec succès", Alert.AlertType.INFORMATION);
            }
            
            clearFields();
            refreshTable();
        } catch (SQLException ex) {
            Logger.getLogger(UserController.class.getName()).log(Level.SEVERE, null, ex);
            showAlert("Erreur", "Erreur lors de l'opération", Alert.AlertType.ERROR);
        }
    }
    
    private void setTextFields(User user) {
        emailField.setText(user.getEmail());
        nameField.setText(user.getName());
        roleField.setText(user.getRole());
        bioField.setText(user.getBio() != null ? user.getBio() : "");
    }
    
    private void clearFields() {
        emailField.clear();
        nameField.clear();
        passwordField.clear();
        roleField.clear();
        bioField.clear();
        selectedUser = null;
    }
    
    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
