package edu.connexion3a8.controllers;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import edu.connexion3a8.InvestiApp;
import edu.connexion3a8.entities.User;
import edu.connexion3a8.services.UserService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ResourceBundle;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class AdminDashboardController implements Initializable {

    private UserService userService = new UserService();
    private ObservableList<User> userList = FXCollections.observableArrayList();
    private FilteredList<User> filteredData;
    private User selectedUser = null;

    // User table
    @FXML
    private TableView<User> userTable;
    @FXML
    private TableColumn<User, String> colName;
    @FXML
    private TableColumn<User, String> colEmail;
    @FXML
    private TableColumn<User, String> colRole;
    @FXML
    private TableColumn<User, String> colStatus;
    @FXML
    private TableColumn<User, Integer> colPoints;
    @FXML
    private TableColumn<User, Integer> colLevel;

    @FXML
    private TableColumn<User, Timestamp> colCreatedAt;
    @FXML
    private TableColumn<User, String> colActions;

    // Form fields
    @FXML
    private TextField emailField;
    @FXML
    private TextField nameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField roleField;
    @FXML
    private TextArea bioField;

    // Search + Filter
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> filterCombo;
    @FXML
    private Label emptyStateLabel;
    @FXML
    private Label userCountLabel;
    @FXML
    private ProgressIndicator loadingIndicator;

    // Buttons
    @FXML
    private Button btnAdd;
    @FXML
    private Button btnRefresh;
    @FXML
    private Button btnClear;

    // Stats
    @FXML
    private Label statTotal;
    @FXML
    private Label statVerified;
    @FXML
    private Label statPending;
    @FXML
    private Label statUnverified;

    // KYC
    @FXML
    private TableView<User> kycTable;
    @FXML
    private TableColumn<User, String> kycColName;
    @FXML
    private TableColumn<User, String> kycColEmail;
    @FXML
    private TableColumn<User, String> kycColRole;
    @FXML
    private TableColumn<User, String> kycColActions;
    @FXML
    private Label kycEmptyLabel;

    private ObservableList<User> kycList = FXCollections.observableArrayList();

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-zÀ-ÿ\\s'-]{2,50}$");
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupFilterCombo();
        setupValidation();
        loadData();
        setupSearchAndFilter();
        loadKycData();
    }

    // ==================== FILTER COMBO ====================

    private void setupFilterCombo() {
        filterCombo.setItems(FXCollections.observableArrayList(
                "All Users", "Verified (Active)", "Pending KYC", "Unverified (No ID)"));
        filterCombo.setValue("All Users");
        filterCombo.setOnAction(e -> applyFilters());
    }

    // ==================== VALIDATION ====================

    private void setupValidation() {
        emailField.textProperty().addListener((obs, o, n) -> {
            if (!n.isEmpty() && !EMAIL_PATTERN.matcher(n).matches())
                emailField.getStyleClass().add("input-error");
            else
                emailField.getStyleClass().remove("input-error");
        });
        nameField.textProperty().addListener((obs, o, n) -> {
            if (!n.isEmpty() && !NAME_PATTERN.matcher(n).matches())
                nameField.getStyleClass().add("input-error");
            else
                nameField.getStyleClass().remove("input-error");
        });
        passwordField.textProperty().addListener((obs, o, n) -> {
            if (n.length() > 0 && n.length() < 6)
                passwordField.getStyleClass().add("input-error");
            else
                passwordField.getStyleClass().remove("input-error");
        });
    }

    // ==================== SEARCH + FILTER ====================

    private void setupSearchAndFilter() {
        filteredData = new FilteredList<>(userList, p -> true);
        searchField.textProperty().addListener((obs, o, n) -> applyFilters());
        userTable.setItems(filteredData);
    }

    private void applyFilters() {
        String searchText = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
        String filterValue = filterCombo.getValue() == null ? "All Users" : filterCombo.getValue();

        Predicate<User> searchPredicate = user -> {
            if (searchText.isEmpty())
                return true;
            return user.getName().toLowerCase().contains(searchText)
                    || user.getEmail().toLowerCase().contains(searchText)
                    || user.getRole().toLowerCase().contains(searchText);
        };

        Predicate<User> filterPredicate = user -> {
            switch (filterValue) {
                case "Verified (Active)":
                    return user.isActive();
                case "Pending KYC":
                    return !user.isActive() && user.getIdImageUrl() != null && !user.getIdImageUrl().isEmpty();
                case "Unverified (No ID)":
                    return !user.isActive() && (user.getIdImageUrl() == null || user.getIdImageUrl().isEmpty());
                default:
                    return true;
            }
        };

        filteredData.setPredicate(searchPredicate.and(filterPredicate));

        if (emptyStateLabel != null) {
            emptyStateLabel.setVisible(filteredData.isEmpty());
            emptyStateLabel.setManaged(filteredData.isEmpty());
        }
        if (userCountLabel != null) {
            userCountLabel.setText("Showing " + filteredData.size() + " of " + userList.size() + " users");
        }
    }

    // ==================== TABLE SETUP ====================

    private void loadData() {
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colPoints.setCellValueFactory(new PropertyValueFactory<>("points"));
        colLevel.setCellValueFactory(new PropertyValueFactory<>("level"));
        colCreatedAt.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

        // Status column with colored badges
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    User user = getTableRow().getItem();
                    Label badge = new Label();
                    badge.setStyle(
                            "-fx-padding: 4 12; -fx-background-radius: 20; -fx-font-size: 11px; -fx-font-weight: bold;");
                    if (user.isActive()) {
                        badge.setText("✓ Active");
                        badge.setStyle(badge.getStyle() + "-fx-background-color: #E8F5E9; -fx-text-fill: #2E7D32;");
                    } else if (user.getIdImageUrl() != null && !user.getIdImageUrl().isEmpty()) {
                        badge.setText("⏳ Pending");
                        badge.setStyle(badge.getStyle() + "-fx-background-color: #FFF8E1; -fx-text-fill: #9B7E46;");
                    } else {
                        badge.setText("✗ Unverified");
                        badge.setStyle(badge.getStyle() + "-fx-background-color: #FFEBEE; -fx-text-fill: #A62639;");
                    }
                    setGraphic(badge);
                }
            }
        });

        // Created at column formatted
        colCreatedAt.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Timestamp item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : DATE_FMT.format(item));
            }
        });

        // Actions column: Edit, Delete, PDF
        colActions.setCellFactory(col -> new TableCell<>() {
            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    User user = getTableView().getItems().get(getIndex());

                    Button editBtn = new Button("✏ Edit");
                    editBtn.setStyle(
                            "-fx-padding: 6 12; -fx-font-size: 11px; -fx-background-color: #456990; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand;");

                    Button deleteBtn = new Button("🗑 Delete");
                    deleteBtn.setStyle(
                            "-fx-padding: 6 12; -fx-font-size: 11px; -fx-background-color: #A62639; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand;");

                    Button pdfBtn = new Button("📄 PDF");
                    pdfBtn.setStyle(
                            "-fx-padding: 6 12; -fx-font-size: 11px; -fx-background-color: #9B7E46; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand;");

                    editBtn.setOnMouseClicked((MouseEvent e) -> {
                        selectedUser = user;
                        setTextFields(user);
                        btnAdd.setText("💾 Update User");
                    });

                    deleteBtn.setOnMouseClicked((MouseEvent e) -> handleDeleteUser(user));
                    pdfBtn.setOnMouseClicked((MouseEvent e) -> exportUserPdf(user));

                    HBox box = new HBox(6, editBtn, deleteBtn, pdfBtn);
                    box.setStyle("-fx-alignment: center;");
                    setGraphic(box);
                }
            }
        });

        refreshTable();
    }

    // ==================== REFRESH + STATS ====================

    @FXML
    private void refreshTable() {
        showLoading(true);
        new Thread(() -> {
            try {
                Thread.sleep(200);
                var users = userService.getAllUsers();
                Platform.runLater(() -> {
                    userList.setAll(users);
                    applyFilters();
                    updateStats();
                    showLoading(false);
                });
            } catch (SQLException | InterruptedException ex) {
                Platform.runLater(() -> {
                    showErrorAlert("Error loading users: " + ex.getMessage());
                    showLoading(false);
                });
            }
        }).start();
    }

    private void updateStats() {
        long total = userList.size();
        long verified = userList.stream().filter(User::isActive).count();
        long pending = userList.stream()
                .filter(u -> !u.isActive() && u.getIdImageUrl() != null && !u.getIdImageUrl().isEmpty()).count();
        long unverified = userList.stream()
                .filter(u -> !u.isActive() && (u.getIdImageUrl() == null || u.getIdImageUrl().isEmpty())).count();

        statTotal.setText(String.valueOf(total));
        statVerified.setText(String.valueOf(verified));
        statPending.setText(String.valueOf(pending));
        statUnverified.setText(String.valueOf(unverified));
    }

    // ==================== ADD / UPDATE USER ====================

    @FXML
    private void addUser(ActionEvent event) {
        String email = emailField.getText().trim();
        String name = nameField.getText().trim();
        String password = passwordField.getText();
        String role = roleField.getText().trim();
        String bio = bioField != null ? bioField.getText().trim() : "";

        if (email.isEmpty() || name.isEmpty() || role.isEmpty()) {
            showErrorAlert("Please fill in all required fields");
            return;
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            showErrorAlert("Please enter a valid email address");
            return;
        }
        if (!NAME_PATTERN.matcher(name).matches()) {
            showErrorAlert("Name must contain only letters (2-50 characters)");
            return;
        }

        btnAdd.setDisable(true);
        String originalText = btnAdd.getText();
        btnAdd.setText("⏳ Saving...");

        new Thread(() -> {
            try {
                if (selectedUser != null) {
                    selectedUser.setEmail(email);
                    selectedUser.setName(name);
                    selectedUser.setRole(role);
                    selectedUser.setBio(bio.isEmpty() ? null : bio);
                    userService.updateUser(selectedUser.getId(), selectedUser);
                    Platform.runLater(() -> {
                        showSuccessAlert("User updated successfully!");
                        selectedUser = null;
                        btnAdd.setText("💾 Save User");
                        clearFields();
                        refreshTable();
                        refreshKycTable();
                        btnAdd.setDisable(false);
                    });
                } else {
                    if (password.isEmpty() || password.length() < 6) {
                        Platform.runLater(() -> {
                            showErrorAlert(password.isEmpty() ? "Password is required"
                                    : "Password must be at least 6 characters");
                            btnAdd.setText(originalText);
                            btnAdd.setDisable(false);
                        });
                        return;
                    }
                    User user = new User(email, password, name, role);
                    if (!bio.isEmpty())
                        user.setBio(bio);
                    userService.addUser(user);
                    Platform.runLater(() -> {
                        showSuccessAlert("User added successfully!");
                        clearFields();
                        refreshTable();
                        btnAdd.setText(originalText);
                        btnAdd.setDisable(false);
                    });
                }
            } catch (SQLException ex) {
                Platform.runLater(() -> {
                    showErrorAlert("Error: " + ex.getMessage());
                    btnAdd.setText(originalText);
                    btnAdd.setDisable(false);
                });
            }
        }).start();
    }

    private void handleDeleteUser(User user) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete " + user.getName() + "?");
        confirm.setContentText("This action cannot be undone.");
        ButtonType btnYes = new ButtonType("Yes, Delete", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnNo = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(btnYes, btnNo);
        confirm.showAndWait().ifPresent(response -> {
            if (response == btnYes) {
                try {
                    userService.deleteUser(user.getId());
                    refreshTable();
                    refreshKycTable();
                    clearFields();
                    showSuccessAlert("User deleted!");
                } catch (SQLException ex) {
                    showErrorAlert("Error deleting user: " + ex.getMessage());
                }
            }
        });
    }

    // ==================== PDF EXPORT ====================

    private void exportUserPdf(User user) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save User PDF");
        fileChooser.setInitialFileName(user.getName().replaceAll("\\s+", "_") + "_profile.pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = fileChooser.showSaveDialog(userTable.getScene().getWindow());

        if (file == null)
            return;

        try {
            Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
            PdfWriter.getInstance(doc, new FileOutputStream(file));
            doc.open();

            // Title
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 22, Font.BOLD, new BaseColor(69, 105, 144));
            Paragraph title = new Paragraph("INVESTI - User Profile", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            doc.add(title);

            // Separator
            doc.add(new Paragraph(" "));

            // User info table
            Font headerFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, BaseColor.WHITE);
            Font cellFont = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, BaseColor.DARK_GRAY);

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setWidths(new float[] { 1, 2.5f });
            table.setSpacingBefore(10);

            BaseColor headerBg = new BaseColor(69, 105, 144);

            addPdfRow(table, "Name", user.getName(), headerFont, cellFont, headerBg);
            addPdfRow(table, "Email", user.getEmail(), headerFont, cellFont, headerBg);
            addPdfRow(table, "Role", user.getRole(), headerFont, cellFont, headerBg);
            addPdfRow(table, "Status", user.isActive() ? "Active (Verified)" : "Inactive (Pending)", headerFont,
                    cellFont, headerBg);
            addPdfRow(table, "Email Verified", user.isEmailVerified() ? "Yes" : "No", headerFont, cellFont, headerBg);
            addPdfRow(table, "Bio", user.getBio() != null ? user.getBio() : "N/A", headerFont, cellFont, headerBg);
            addPdfRow(table, "Points", String.valueOf(user.getPoints()), headerFont, cellFont, headerBg);
            addPdfRow(table, "Level", String.valueOf(user.getLevel()), headerFont, cellFont, headerBg);
            addPdfRow(table, "Joined", user.getCreatedAt() != null ? DATE_FMT.format(user.getCreatedAt()) : "N/A",
                    headerFont, cellFont, headerBg);
            addPdfRow(table, "Last Login", user.getLastLogin() != null ? DATE_FMT.format(user.getLastLogin()) : "Never",
                    headerFont, cellFont, headerBg);

            doc.add(table);

            // ID Image
            if (user.getIdImageUrl() != null && !user.getIdImageUrl().isEmpty()) {
                File imgFile = new File(user.getIdImageUrl());
                if (imgFile.exists()) {
                    doc.add(new Paragraph(" "));
                    Font idTitle = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, new BaseColor(155, 126, 70));
                    Paragraph idHeader = new Paragraph("ID Document", idTitle);
                    idHeader.setSpacingBefore(15);
                    idHeader.setSpacingAfter(10);
                    doc.add(idHeader);

                    com.itextpdf.text.Image idImg = com.itextpdf.text.Image.getInstance(imgFile.getAbsolutePath());
                    idImg.scaleToFit(400, 300);
                    idImg.setAlignment(Element.ALIGN_CENTER);
                    doc.add(idImg);
                }
            }

            // Footer
            doc.add(new Paragraph(" "));
            Font footerFont = new Font(Font.FontFamily.HELVETICA, 9, Font.ITALIC, BaseColor.GRAY);
            Paragraph footer = new Paragraph(
                    "Generated by INVESTI Admin Dashboard — " + DATE_FMT.format(new java.util.Date()), footerFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(30);
            doc.add(footer);

            doc.close();
            showSuccessAlert("PDF exported: " + file.getName());

        } catch (Exception e) {
            showErrorAlert("PDF export error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void addPdfRow(PdfPTable table, String label, String value, Font headerFont, Font cellFont,
            BaseColor headerBg) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, headerFont));
        labelCell.setBackgroundColor(headerBg);
        labelCell.setPadding(8);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, cellFont));
        valueCell.setPadding(8);
        valueCell.setBackgroundColor(BaseColor.WHITE);
        table.addCell(valueCell);
    }

    // ==================== KYC ====================

    private void loadKycData() {
        kycColName.setCellValueFactory(new PropertyValueFactory<>("name"));
        kycColEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        kycColRole.setCellValueFactory(new PropertyValueFactory<>("role"));

        kycColActions.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    User user = getTableView().getItems().get(getIndex());

                    Button viewBtn = new Button("🖼 View ID");
                    viewBtn.setStyle(
                            "-fx-padding: 6 14; -fx-font-size: 12px; -fx-background-color: #456990; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand;");

                    Button approveBtn = new Button("✓ Approve");
                    approveBtn.setStyle(
                            "-fx-padding: 6 14; -fx-font-size: 12px; -fx-background-color: #2E7D32; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand;");

                    Button rejectBtn = new Button("✗ Reject");
                    rejectBtn.setStyle(
                            "-fx-padding: 6 14; -fx-font-size: 12px; -fx-background-color: #A62639; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand;");

                    viewBtn.setOnAction(e -> showIdImage(user));
                    approveBtn.setOnAction(e -> approveKyc(user));
                    rejectBtn.setOnAction(e -> rejectKyc(user));

                    HBox box = new HBox(8, viewBtn, approveBtn, rejectBtn);
                    box.setStyle("-fx-alignment: center;");
                    setGraphic(box);
                }
            }
        });

        refreshKycTable();
    }

    @FXML
    private void refreshKycTable() {
        new Thread(() -> {
            try {
                var pending = userService.getPendingKycUsers();
                Platform.runLater(() -> {
                    kycList.setAll(pending);
                    kycTable.setItems(kycList);
                    kycEmptyLabel.setVisible(kycList.isEmpty());
                    kycEmptyLabel.setManaged(kycList.isEmpty());
                    kycTable.setVisible(!kycList.isEmpty());
                    kycTable.setManaged(!kycList.isEmpty());
                });
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    private void showIdImage(User user) {
        try {
            File file = new File(user.getIdImageUrl());
            if (!file.exists()) {
                showErrorAlert("ID image not found: " + user.getIdImageUrl());
                return;
            }
            Image img = new Image(file.toURI().toString(), 600, 400, true, true);
            ImageView iv = new ImageView(img);
            iv.setPreserveRatio(true);
            Alert dialog = new Alert(Alert.AlertType.INFORMATION);
            dialog.setTitle("ID Document - " + user.getName());
            dialog.setHeaderText(user.getName() + " (" + user.getEmail() + ")");
            dialog.getDialogPane().setContent(new VBox(10, iv));
            dialog.getDialogPane().setPrefWidth(650);
            dialog.showAndWait();
        } catch (Exception e) {
            showErrorAlert("Error loading image: " + e.getMessage());
        }
    }

    private void approveKyc(User user) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Approve KYC");
        confirm.setHeaderText("Approve " + user.getName() + "?");
        confirm.setContentText("This will activate the user's account.");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    userService.setUserActive(user.getId(), true);
                    showSuccessAlert(user.getName() + " approved!");
                    refreshKycTable();
                    refreshTable();
                } catch (SQLException ex) {
                    showErrorAlert("Error: " + ex.getMessage());
                }
            }
        });
    }

    private void rejectKyc(User user) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Reject KYC");
        confirm.setHeaderText("Reject " + user.getName() + "'s ID?");
        confirm.setContentText("User will need to re-upload.");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    userService.updateIdImageUrl(user.getId(), null);
                    showSuccessAlert("KYC rejected. User will re-upload.");
                    refreshKycTable();
                    refreshTable();
                } catch (SQLException ex) {
                    showErrorAlert("Error: " + ex.getMessage());
                }
            }
        });
    }

    // ==================== HELPERS ====================

    private void setTextFields(User user) {
        emailField.setText(user.getEmail());
        nameField.setText(user.getName());
        roleField.setText(user.getRole());
        if (bioField != null)
            bioField.setText(user.getBio() != null ? user.getBio() : "");
        passwordField.clear();
    }

    private void clearFields() {
        emailField.clear();
        nameField.clear();
        passwordField.clear();
        roleField.clear();
        if (bioField != null)
            bioField.clear();
        selectedUser = null;
        btnAdd.setText("💾 Save User");
    }

    @FXML
    private void handleClear() {
        clearFields();
    }

    private void showLoading(boolean show) {
        if (loadingIndicator != null) {
            Platform.runLater(() -> {
                loadingIndicator.setVisible(show);
                loadingIndicator.setManaged(show);
            });
        }
    }

    private void showSuccessAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText("✓ " + message);
        alert.showAndWait();
    }

    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText("⚠ " + message);
        alert.showAndWait();
    }

    private void showError(String title, String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText("✓ " + message);
        alert.showAndWait();
    }

    @FXML
    private void handleEventManagement() {
        try {
            InvestiApp.showEventManagement();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleGamification() {
        try {
            User user = InvestiApp.getCurrentUser();
            if (user != null) {
                InvestiApp.showGamificationMenu(user);
            } else {
                InvestiApp.showGamificationMenu();
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error", "Failed to open Gamification",
                    "Could not load gamification interface: " + e.getMessage());
        }
    }

    @FXML
    private void handleProducts() {
        try {
            InvestiApp.showProductCatalog();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error", "Failed to open Products", "Could not load products interface: " + e.getMessage());
        }
    }

    @FXML
    private void handleUserManagement() {
        // Already on the admin dashboard which shows user management
        // Just scroll to top or refresh the view
        try {
            refreshTable();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleForum() {
        try {
            InvestiApp.showForum();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error", "Failed to open Forum", "Could not load forum interface: " + e.getMessage());
        }
    }

    @FXML
    private void handleViewCollaboration() {
        try {
            edu.connexion3a8.entities.User user = edu.connexion3a8.InvestiApp.getCurrentUser();
            if (user != null) {
                InvestiApp.showCollaborationAdmin(user);
            } else {
                InvestiApp.showHomePage();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleViewHome() {
        try {
            InvestiApp.showHomePage();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout() {
        try {
            InvestiApp.showLoginPage();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
