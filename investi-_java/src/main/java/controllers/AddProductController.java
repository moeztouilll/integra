package controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import models.Product;
import services.ProductService;
import services.SessionManager;

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
    private TextField nameField, downloadUrlField, priceField, stockField;
    @FXML
    private TextArea fullDescField;
    @FXML
    private ComboBox<String> currencyCombo, statusCombo, categoryCombo;
    @FXML
    private CheckBox digitalCheck;
    @FXML
    private Label nameError, priceError;

    private final ProductService productService = new ProductService();
    private final services.NavyApiService navyApiService = new services.NavyApiService();
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
        // No hardcoded default here, let IP detection set it below

        statusCombo.getItems().addAll("published", "draft", "retired");
        statusCombo.setValue("published");

        categoryCombo.getItems().addAll("Informatique", "Voiture", "Immobilier", "Vêtements", "Services", "Loisirs");
        categoryCombo.setEditable(true);

        // Auto-detect category when name changes (lost focus or enter)
        nameField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal && !nameField.getText().trim().isEmpty()) {
                handleDetectCategory();
            }
        });

        // Detect currency by IP
        navyApiService.detectCurrencyByIP().thenAccept(currency -> {
            if (currency != null && !currency.isEmpty()) {
                javafx.application.Platform.runLater(() -> {
                    // Normalize (e.g. if API returns DT or something else)
                    String finalCurrency = currency.toUpperCase().trim();
                    if (finalCurrency.equals("TUNISIA") || finalCurrency.equals("TUNISIE")
                            || finalCurrency.equals("DT"))
                        finalCurrency = "TND";

                    if (!currencyCombo.getItems().contains(finalCurrency)) {
                        currencyCombo.getItems().add(finalCurrency);
                    }
                    currencyCombo.setValue(finalCurrency);
                    System.out.println("LOG: Initial currency auto-set to: " + finalCurrency);
                });
            } else {
                javafx.application.Platform.runLater(() -> currencyCombo.setValue("USD")); // Fallback if detection
                                                                                           // fails completely
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
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif"));

        Stage stage = (Stage) nameField.getScene().getWindow();
        selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            try {
                // Ensure uploads directory exists
                Path uploadDir = Paths.get("uploads");
                if (!Files.exists(uploadDir)) {
                    Files.createDirectories(uploadDir);
                }

                // Generative unique filename to avoid overwrites
                String fileName = UUID.randomUUID().toString() + "_" + selectedFile.getName();
                Path targetPath = uploadDir.resolve(fileName);

                Files.copy(selectedFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
                String imagePath = targetPath.toAbsolutePath().toUri().toString();

                // Update the URL field with the local path
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
                "Suggest the most specific category possible in French (ignore any previous list). " +
                "Return a JSON object with 'title' (max 5 words) and 'category' (the suggested category). Return ONLY the JSON.";
        nameField.setPromptText("Génération en cours...");

        navyApiService.generateContentFromImage(selectedFile, prompt)
                .thenAccept(jsonStr -> {
                    javafx.application.Platform.runLater(() -> {
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

        String prompt = "Given this product name: '" + name
                + "', identify the most accurate and specific category in French (ignore any previous predefined list). Return ONLY the category name.";

        navyApiService.generateContent(prompt)
                .thenAccept(category -> {
                    javafx.application.Platform.runLater(() -> {
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
                    javafx.application.Platform.runLater(() -> {
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

        // If we have a file, validate with it. Otherwise validate with text.
        if (selectedFile != null) {
            navyApiService.validateProductContext(selectedFile, name, description, price, currency)
                    .thenAccept(jsonResult -> {
                        javafx.application.Platform.runLater(() -> finalizeSave(jsonResult, price));
                    });
        } else {
            // Proceed directly or implement text-only validation
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

            p.setEntrepreneurId(SessionManager.getCurrentUser().getId());

            if (existingProduct == null) {
                productService.create(p);
            } else {
                productService.update(p);
            }

            if (onProductAdded != null) {
                onProductAdded.accept(null);
            }
        } catch (SQLException e) {
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
            nameError.setManaged(true);
            isValid = false;
        } else {
            nameError.setVisible(false);
            nameError.setManaged(false);
        }

        try {
            Double.parseDouble(priceField.getText());
            priceError.setVisible(false);
            priceError.setManaged(false);
        } catch (Exception e) {
            priceError.setText("Invalid price");
            priceError.setVisible(true);
            priceError.setManaged(true);
            isValid = false;
        }
        return isValid;
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        // Already handled TTS in finalizeSave or calling manually if needed
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);

        Label label = new Label(content);
        label.setWrapText(true);
        // High contrast: dark text on light background, premium font
        label.setStyle("-fx-font-size: 14px; " +
                "-fx-text-fill: #1a1a1a; " +
                "-fx-font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; " +
                "-fx-padding: 15;");

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setContent(label);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportHeight(250);
        scrollPane.setPrefViewportWidth(500);

        // Premium look: simple border and clean background
        scrollPane.setStyle("-fx-background-color: #ffffff; " +
                "-fx-background: #ffffff; " +
                "-fx-border-color: #e0e0e0; " +
                "-fx-border-width: 1; " +
                "-fx-border-radius: 4; " +
                "-fx-background-radius: 4;");

        alert.getDialogPane().setContent(scrollPane);
        alert.getDialogPane().setPrefWidth(550);
        alert.getDialogPane().setStyle("-fx-background-color: #f8f9fa;");

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
