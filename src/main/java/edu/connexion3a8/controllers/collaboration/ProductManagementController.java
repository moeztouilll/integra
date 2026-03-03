package edu.connexion3a8.controllers.collaboration;

import edu.connexion3a8.InvestiApp;
import edu.connexion3a8.entities.Product;
import edu.connexion3a8.entities.User;
import edu.connexion3a8.services.ProductService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.util.Optional;
import java.util.ResourceBundle;

public class ProductManagementController implements Initializable {

    private final ProductService productService = new ProductService();
    private final ObservableList<Product> productList = FXCollections.observableArrayList();
    private FilteredList<Product> filteredList;
    private Product editingProduct = null; // null = add mode, non-null = edit mode

    // ── Table ──────────────────────────────
    @FXML
    private TableView<Product> productTable;
    @FXML
    private TableColumn<Product, Long> colId;
    @FXML
    private TableColumn<Product, String> colName;
    @FXML
    private TableColumn<Product, String> colCategory;
    @FXML
    private TableColumn<Product, Double> colPrice;
    @FXML
    private TableColumn<Product, String> colCurrency;
    @FXML
    private TableColumn<Product, String> colStatus;
    @FXML
    private TableColumn<Product, Integer> colStock;
    @FXML
    private TableColumn<Product, Integer> colRemise;
    @FXML
    private TableColumn<Product, Integer> colViews;
    @FXML
    private TableColumn<Product, Integer> colSales;

    // ── Toolbar ────────────────────────────
    @FXML
    private TextField searchField;

    // ── Form container ─────────────────────
    @FXML
    private javafx.scene.layout.VBox formContainer;
    @FXML
    private TextField nameField;
    @FXML
    private TextArea descriptionField;
    @FXML
    private TextField priceField;
    @FXML
    private ComboBox<String> currencyCombo;
    @FXML
    private ComboBox<String> categoryCombo;
    @FXML
    private ComboBox<String> statusCombo;
    @FXML
    private TextField stockField;
    @FXML
    private TextField remiseField;
    @FXML
    private CheckBox digitalCheckBox;
    @FXML
    private TextField imageUrlField;
    @FXML
    private Button saveButton;
    @FXML
    private Button cancelButton;

    // ───────────────────────────────────────
    // Lifecycle
    // ───────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTable();
        setupForm();
        setupSearch();
        hideForm();
        loadProducts();
    }

    // ───────────────────────────────────────
    // Table setup
    // ───────────────────────────────────────

    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colCurrency.setCellValueFactory(new PropertyValueFactory<>("currency"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));
        colRemise.setCellValueFactory(new PropertyValueFactory<>("remise"));
        colViews.setCellValueFactory(new PropertyValueFactory<>("viewsCount"));
        colSales.setCellValueFactory(new PropertyValueFactory<>("salesCount"));

        // Colour-coded status badge
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label badge = new Label(item);
                badge.setStyle(
                        "-fx-padding: 3 10; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold;"
                                + switch (item.toLowerCase()) {
                                    case "published" -> "-fx-background-color: #E8F5E9; -fx-text-fill: #2E7D32;";
                                    case "draft" -> "-fx-background-color: #FFF8E1; -fx-text-fill: #9B7E46;";
                                    case "archived" -> "-fx-background-color: #EEEEEE; -fx-text-fill: #555555;";
                                    default -> "-fx-background-color: #E3F2FD; -fx-text-fill: #1565C0;";
                                });
                setGraphic(badge);
                setText(null);
            }
        });

        filteredList = new FilteredList<>(productList, p -> true);
        productTable.setItems(filteredList);

        // Show placeholder when empty
        productTable.setPlaceholder(new Label("No products found."));
    }

    // ───────────────────────────────────────
    // Form setup
    // ───────────────────────────────────────

    private void setupForm() {
        currencyCombo.setItems(FXCollections.observableArrayList("TND", "USD", "EUR", "GBP", "MAD", "DZD"));
        currencyCombo.setValue("TND");

        categoryCombo.setItems(FXCollections.observableArrayList(
                "Technology", "Finance", "Health", "Education", "Agriculture",
                "Real Estate", "Retail", "Manufacturing", "Services", "Other"));
        categoryCombo.setValue("Technology");

        statusCombo.setItems(FXCollections.observableArrayList("draft", "published", "archived"));
        statusCombo.setValue("draft");
    }

    // ───────────────────────────────────────
    // Search
    // ───────────────────────────────────────

    private void setupSearch() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String kw = newVal == null ? "" : newVal.toLowerCase().trim();
            filteredList.setPredicate(p -> kw.isEmpty()
                    || p.getName().toLowerCase().contains(kw)
                    || (p.getCategory() != null && p.getCategory().toLowerCase().contains(kw))
                    || String.valueOf(p.getId()).contains(kw));
        });
    }

    // ───────────────────────────────────────
    // Data loading
    // ───────────────────────────────────────

    private void loadProducts() {
        new Thread(() -> {
            try {
                var products = productService.getAllProducts();
                Platform.runLater(() -> productList.setAll(products));
            } catch (SQLException e) {
                Platform.runLater(() -> showError("Load error: " + e.getMessage()));
                e.printStackTrace();
            }
        }).start();
    }

    // ───────────────────────────────────────
    // FXML handlers
    // ───────────────────────────────────────

    @FXML
    private void handleAddProduct() {
        editingProduct = null;
        clearForm();
        saveButton.setText("💾 Save");
        showForm();
    }

    @FXML
    private void handleRefresh() {
        loadProducts();
    }

    @FXML
    private void handleEditProduct() {
        Product selected = productTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select a product to edit.");
            return;
        }
        editingProduct = selected;
        populateForm(selected);
        saveButton.setText("💾 Update");
        showForm();
    }

    @FXML
    private void handleDeleteProduct() {
        Product selected = productTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select a product to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Product");
        confirm.setHeaderText("Delete \"" + selected.getName() + "\"?");
        confirm.setContentText("This action cannot be undone.");
        ButtonType yes = new ButtonType("Yes, Delete", ButtonBar.ButtonData.OK_DONE);
        ButtonType no = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(yes, no);

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == yes) {
            User currentUser = InvestiApp.getCurrentUser();
            new Thread(() -> {
                try {
                    productService.delete(selected.getId(), currentUser);
                    Platform.runLater(() -> {
                        productList.remove(selected);
                        showSuccess("Product deleted successfully!");
                    });
                } catch (SQLException | SecurityException e) {
                    Platform.runLater(() -> showError("Delete error: " + e.getMessage()));
                }
            }).start();
        }
    }

    @FXML
    private void handleSave() {
        // Validation
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            showError("Product name is required.");
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceField.getText().trim());
            if (price < 0)
                throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showError("Please enter a valid price (≥ 0).");
            return;
        }

        int stock = 0, remise = 0;
        try {
            if (!stockField.getText().trim().isEmpty())
                stock = Integer.parseInt(stockField.getText().trim());
            if (!remiseField.getText().trim().isEmpty())
                remise = Integer.parseInt(remiseField.getText().trim());
        } catch (NumberFormatException e) {
            showError("Stock and discount must be whole numbers.");
            return;
        }

        Product p = (editingProduct != null) ? editingProduct : new Product();
        p.setName(name);
        p.setDescription(descriptionField.getText().trim());
        p.setPrice(price);
        p.setCurrency(currencyCombo.getValue());
        p.setCategory(categoryCombo.getValue());
        p.setStatus(statusCombo.getValue());
        p.setStock(stock);
        p.setRemise(remise);
        p.setDigital(digitalCheckBox.isSelected());
        p.setDownloadUrl(imageUrlField.getText().trim());

        // Set entrepreneur id from current user
        User currentUser = InvestiApp.getCurrentUser();
        if (currentUser != null)
            p.setEntrepreneurId(currentUser.getId());

        saveButton.setDisable(true);
        new Thread(() -> {
            try {
                if (editingProduct == null) {
                    int newId = productService.create(p, currentUser);
                    p.setId(newId);
                    Platform.runLater(() -> {
                        productList.add(p);
                        showSuccess("Product added successfully!");
                        hideForm();
                        saveButton.setDisable(false);
                    });
                } else {
                    productService.update(p, currentUser);
                    Platform.runLater(() -> {
                        // Refresh the list to reflect changes
                        int idx = productList.indexOf(editingProduct);
                        if (idx >= 0)
                            productList.set(idx, p);
                        showSuccess("Product updated successfully!");
                        hideForm();
                        editingProduct = null;
                        saveButton.setDisable(false);
                    });
                }
            } catch (SQLException | SecurityException e) {
                Platform.runLater(() -> {
                    showError("Save error: " + e.getMessage());
                    saveButton.setDisable(false);
                });
            }
        }).start();
    }

    @FXML
    private void handleCancel() {
        editingProduct = null;
        hideForm();
    }

    @FXML
    private void handleBrowseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Product Image");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.webp"));
        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            imageUrlField.setText(selectedFile.getAbsolutePath());
        }
    }

    @FXML
    private void handleGoHome() {
        try {
            InvestiApp.showHomePage();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleGoAdminDashboard() {
        try {
            InvestiApp.showAdminDashboard();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ───────────────────────────────────────
    // Helpers
    // ───────────────────────────────────────

    private void showForm() {
        formContainer.setVisible(true);
        formContainer.setManaged(true);
    }

    private void hideForm() {
        formContainer.setVisible(false);
        formContainer.setManaged(false);
    }

    private void clearForm() {
        nameField.clear();
        descriptionField.clear();
        priceField.clear();
        stockField.clear();
        remiseField.clear();
        imageUrlField.clear();
        digitalCheckBox.setSelected(false);
        currencyCombo.setValue("TND");
        categoryCombo.setValue("Technology");
        statusCombo.setValue("draft");
    }

    private void populateForm(Product p) {
        nameField.setText(p.getName());
        descriptionField.setText(p.getDescription() != null ? p.getDescription() : "");
        priceField.setText(String.valueOf(p.getPrice()));
        stockField.setText(String.valueOf(p.getStock()));
        remiseField.setText(String.valueOf(p.getRemise()));
        imageUrlField.setText(p.getDownloadUrl() != null ? p.getDownloadUrl() : "");
        digitalCheckBox.setSelected(p.isDigital());
        currencyCombo.setValue(p.getCurrency() != null ? p.getCurrency() : "TND");
        categoryCombo.setValue(p.getCategory() != null ? p.getCategory() : "Technology");
        statusCombo.setValue(p.getStatus() != null ? p.getStatus() : "draft");
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText("⚠ " + msg);
        alert.showAndWait();
    }

    private void showSuccess(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText("✓ " + msg);
        alert.showAndWait();
    }

    /** Optional: allows passing current user explicitly from InvestiApp */
    public void setCurrentUser(User user) {
        InvestiApp.setCurrentUser(user);
    }

    @FXML
    private void handleViewCatalog() {
        try {
            InvestiApp.showProductCatalog();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Could not load catalog: " + e.getMessage());
        }
    }
}
