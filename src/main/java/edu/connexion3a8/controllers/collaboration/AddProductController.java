package edu.connexion3a8.controllers.collaboration;

import edu.connexion3a8.InvestiApp;
import edu.connexion3a8.entities.Product;
import edu.connexion3a8.services.ProductService;
import edu.connexion3a8.services.collaboration.NavyApiService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.function.Consumer;

public class AddProductController implements Initializable {

    @FXML
    private TextField nameField, downloadUrlField, priceField, stockField, remiseField;
    @FXML
    private TextArea fullDescField;
    @FXML
    private ComboBox<String> currencyCombo, statusCombo, categoryCombo;
    @FXML
    private CheckBox digitalCheck;
    @FXML
    private Label nameError, priceError;

    private final ProductService productService = new ProductService();
    private final NavyApiService navyApiService = new NavyApiService();
    private Consumer<Void> onProductAdded;
    private Runnable onCancel;
    private File selectedFile;
    private Product existingProduct;

    @FXML
    private Button saveBtn;
    @FXML
    private Label formTitle;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Initialize combos
        currencyCombo.getItems().addAll("USD", "TND", "EUR", "GBP");

        statusCombo.getItems().addAll("published", "draft", "retired");
        statusCombo.setValue("published");

        categoryCombo.getItems().addAll("Informatique", "Voiture", "Immobilier", "Vêtements", "Services", "Loisirs");
        categoryCombo.setEditable(true);

        // Auto-detect category when name changes
        nameField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal && !nameField.getText().trim().isEmpty()) {
                handleDetectCategory();
            }
        });

        // Detect currency by IP
        navyApiService.detectCurrencyByIP().thenAccept(currency -> {
            if (currency != null && !currency.isEmpty()) {
                Platform.runLater(() -> {
                    String finalCurrency = currency.toUpperCase().trim();
                    if (finalCurrency.equals("TUNISIA") || finalCurrency.equals("TUNISIE")
                            || finalCurrency.equals("DT"))
                        finalCurrency = "TND";

                    if (!currencyCombo.getItems().contains(finalCurrency)) {
                        currencyCombo.getItems().add(finalCurrency);
                    }
                    currencyCombo.setValue(finalCurrency);
                });
            } else {
                Platform.runLater(() -> currencyCombo.setValue("TND"));
            }
        });
    }

    public void setProductData(Product p) {
        this.existingProduct = p;
        if (p != null) {
            nameField.setText(p.getName());
            fullDescField.setText(p.getDescription());
            priceField.setText(String.valueOf(p.getPrice()));
            if (stockField != null)
                stockField.setText(String.valueOf(p.getStock()));
            currencyCombo.setValue(p.getCurrency());
            digitalCheck.setSelected(p.isDigital());
            statusCombo.setValue(p.getStatus());
            categoryCombo.setValue(p.getCategory());
            downloadUrlField.setText(p.getDownloadUrl());
            if (remiseField != null)
                remiseField.setText(String.valueOf(p.getRemise()));

            if (saveBtn != null)
                saveBtn.setText("Update Product");
            if (formTitle != null)
                formTitle.setText("Edit Product");
        }
    }

    public void setOnProductAdded(Consumer<Void> onProductAdded) {
        this.onProductAdded = onProductAdded;
    }

    public void setOnCancel(Runnable onCancel) {
        this.onCancel = onCancel;
    }

    @FXML
    private void handleBrowseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Product Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp"));

        Stage stage = (Stage) nameField.getScene().getWindow();
        selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            try {
                Path uploadDir = Paths.get("uploads");
                if (!Files.exists(uploadDir)) {
                    Files.createDirectories(uploadDir);
                }

                String fileName = UUID.randomUUID().toString() + "_" + selectedFile.getName();
                Path targetPath = uploadDir.resolve(fileName);

                Files.copy(selectedFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
                String imagePath = targetPath.toAbsolutePath().toUri().toString();

                downloadUrlField.setText(imagePath);

            } catch (IOException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Import Error", "Could not import image: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleGenerateTitle() {
        if (selectedFile == null) {
            showAlert(Alert.AlertType.WARNING, "No Image", "Veuillez d'abord sélectionner une image pour l'analyse !");
            return;
        }
        String prompt = "Identify the product in this image. " +
                "Suggest the most specific category possible in French. " +
                "Return a JSON object with 'title' (max 5 words) and 'category'. Return ONLY the JSON.";
        nameField.setPromptText("Génération en cours...");

        navyApiService.generateContentFromImage(selectedFile, prompt)
                .thenAccept(jsonStr -> {
                    Platform.runLater(() -> {
                        try {
                            String cleanedJson = cleanJsonResponse(jsonStr);
                            org.json.JSONObject result = new org.json.JSONObject(cleanedJson);
                            nameField.setText(result.optString("title", ""));
                            String cat = result.optString("category", "");
                            if (!cat.isEmpty()) {
                                if (!categoryCombo.getItems().contains(cat)) {
                                    categoryCombo.getItems().add(cat);
                                }
                                categoryCombo.setValue(cat);
                            }
                        } catch (Exception e) {
                            nameField.setText(jsonStr.replace("\"", "").trim());
                        }
                    });
                });
    }

    @FXML
    private void handleDetectCategory() {
        String name = nameField.getText().trim();
        if (name.isEmpty())
            return;

        String prompt = "Product name: '" + name
                + "'. Identify specific category in French. Return ONLY category name.";

        navyApiService.generateContent(prompt)
                .thenAccept(category -> {
                    Platform.runLater(() -> {
                        String cat = category.trim();
                        if (!categoryCombo.getItems().contains(cat)) {
                            categoryCombo.getItems().add(cat);
                        }
                        categoryCombo.setValue(cat);
                    });
                });
    }

    @FXML
    private void handleGenerateDescription() {
        if (selectedFile == null) {
            showAlert(Alert.AlertType.WARNING, "No Image", "Veuillez d'abord sélectionner une image pour l'analyse !");
            return;
        }
        String prompt = "Write a comprehensive, engaging product description based on this image. Return ONLY the description text.";
        fullDescField.setPromptText("Génération de la description en cours...");
        fullDescField.setDisable(true);

        navyApiService.generateContentFromImage(selectedFile, prompt)
                .thenAccept(desc -> {
                    Platform.runLater(() -> {
                        fullDescField.setText(desc);
                        fullDescField.setDisable(false);
                    });
                });
    }

    @FXML
    private void handleSave() {
        if (!validateForm())
            return;

        double price = Double.parseDouble(priceField.getText());
        String name = nameField.getText();
        String description = fullDescField.getText();
        String currency = currencyCombo.getValue();

        saveBtn.setText("Validating (AI)...");
        saveBtn.setDisable(true);

        if (selectedFile != null) {
            navyApiService.validateProductContext(selectedFile, name, description, price, currency)
                    .thenAccept(jsonResult -> {
                        Platform.runLater(() -> finalizeSave(jsonResult, price));
                    });
        } else {
            proceedWithSave(price);
        }
    }

    private void finalizeSave(org.json.JSONObject jsonResult, double price) {
        boolean isValid = jsonResult.optBoolean("valid", false);
        String reason = jsonResult.optString("reason", "Le produit n'est pas cohérent.");

        if (isValid) {
            proceedWithSave(price);
        } else {
            saveBtn.setDisable(false);
            saveBtn.setText(existingProduct == null ? "Create Product" : "Update Product");
            showAlert(Alert.AlertType.WARNING, "Validation Rejetée par l'IA", reason);
        }
    }

    private void proceedWithSave(double price) {
        try {
            Product p = existingProduct != null ? existingProduct : new Product();
            p.setName(nameField.getText().trim());
            p.setDescription(fullDescField.getText().trim());
            p.setPrice(price);
            p.setCurrency(currencyCombo.getValue());
            p.setDigital(digitalCheck.isSelected());
            p.setStatus(statusCombo.getValue());
            p.setCategory(categoryCombo.getEditor().getText().trim());
            p.setDownloadUrl(downloadUrlField.getText().trim());

            try {
                if (stockField != null && !stockField.getText().isEmpty()) {
                    p.setStock(Integer.parseInt(stockField.getText().trim()));
                } else {
                    p.setStock(0);
                }
            } catch (NumberFormatException ignored) {
                p.setStock(0);
            }

            try {
                if (remiseField != null && !remiseField.getText().isEmpty()) {
                    p.setRemise(Integer.parseInt(remiseField.getText().trim()));
                } else {
                    p.setRemise(0);
                }
            } catch (NumberFormatException ignored) {
                p.setRemise(0);
            }

            if (InvestiApp.getCurrentUser() != null) {
                p.setEntrepreneurId(InvestiApp.getCurrentUser().getId());
            }

            if (existingProduct == null) {
                productService.create(p, InvestiApp.getCurrentUser());
            } else {
                productService.update(p, InvestiApp.getCurrentUser());
            }

            if (onProductAdded != null) {
                onProductAdded.accept(null);
            }
        } catch (SQLException | SecurityException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Could not save product: " + e.getMessage());
        } finally {
            saveBtn.setDisable(false);
            saveBtn.setText(existingProduct == null ? "Create Product" : "Update Product");
        }
    }

    @FXML
    private void handleCancel() {
        if (onCancel != null)
            onCancel.run();
    }

    @FXML
    private void handleBack() {
        handleCancel();
    }

    private boolean validateForm() {
        boolean isValid = true;
        if (nameField.getText().isEmpty()) {
            nameError.setText("Product name is required");
            nameError.setVisible(true);
            isValid = false;
        } else {
            nameError.setVisible(false);
        }

        try {
            Double.parseDouble(priceField.getText());
            priceError.setVisible(false);
        } catch (Exception e) {
            priceError.setText("Invalid price");
            priceError.setVisible(true);
            isValid = false;
        }
        return isValid;
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.show();
    }

    private String cleanJsonResponse(String content) {
        if (content == null)
            return "{}";
        String cleaned = content.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        return cleaned.trim();
    }
}
