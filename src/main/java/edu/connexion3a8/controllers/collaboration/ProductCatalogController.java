package edu.connexion3a8.controllers.collaboration;

import edu.connexion3a8.InvestiApp;
import edu.connexion3a8.entities.Product;
import edu.connexion3a8.entities.collaboration.Sale;
import edu.connexion3a8.entities.User;
import edu.connexion3a8.services.ProductService;
import edu.connexion3a8.services.collaboration.SaleService;
import edu.connexion3a8.services.collaboration.NavyApiService;
import edu.connexion3a8.services.collaboration.ExportService;
import edu.connexion3a8.tools.QRCodeUtils;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class ProductCatalogController implements Initializable {

    @FXML
    private ScrollPane productsPage;
    @FXML
    private FlowPane productGrid;
    @FXML
    private VBox cartPage, cartItemsContainer;
    @FXML
    private HBox mainHeader, filtersContainer, categoryContainer, saleFiltersContainer, saleCategoryContainer,
            historyHeader;
    @FXML
    private Label cartCountLabel, totalProductsLabel, totalRevenueLabel, totalSalesCountLabel;
    @FXML
    private Button btnAddNewProduct, btnCompare;
    @FXML
    private VBox addProductPage;
    @FXML
    private HBox titleBar;
    @FXML
    private StackPane rootStack;
    @FXML
    private VBox detailOverlay;
    @FXML
    private ImageView detailImage;
    @FXML
    private Label detailTitle, detailShortDesc, detailStatus, detailPrice, detailCurrency;
    @FXML
    private Label detailViews, detailSales, detailCreated, detailUpdated;
    @FXML
    private TextFlow detailFullDesc;
    @FXML
    private VBox orderHistoryPage, ordersContainer;
    @FXML
    private Button navProducts, navHistory, navCart, navSettings;
    @FXML
    private ToggleButton tglMyProducts;

    private final ProductService productService = new ProductService();
    private final SaleService saleService = new SaleService();
    private final NavyApiService navyApiService = new NavyApiService();
    private final ExportService exportService = new ExportService();
    private User currentUser;

    private List<Product> allProducts = new ArrayList<>();
    private List<Product> selectedProducts = new ArrayList<>();
    private List<Sale> allSales = new ArrayList<>();
    private String activeCategory = "All";

    // Offset for window dragging
    private double xOffset = 0;
    private double yOffset = 0;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupWindowDragging();
        currentUser = InvestiApp.getCurrentUser();
        if (currentUser == null) {
            System.err.println("ProductCatalogController initialized with null user!");
        }

        // Apply role-based button visibility
        if (btnAddNewProduct != null) {
            // Investors don't create products generally
            boolean isInvestor = currentUser != null && "investor".equalsIgnoreCase(currentUser.getRole());
            btnAddNewProduct.setVisible(!isInvestor);
            btnAddNewProduct.setManaged(!isInvestor);
        }

        loadDatabaseData(); // Fetch from DB first
        // Listeners
        if (tglMyProducts != null) {
            tglMyProducts.selectedProperty().addListener((obs, oldV, newV) -> renderProducts());
        }
    }

    private void setupWindowDragging() {
        if (titleBar != null) {
            titleBar.setOnMousePressed(event -> {
                xOffset = event.getSceneX();
                yOffset = event.getSceneY();
            });
            titleBar.setOnMouseDragged(event -> {
                Stage stage = (Stage) rootStack.getScene().getWindow();
                stage.setX(event.getScreenX() - xOffset);
                stage.setY(event.getScreenY() - yOffset);
            });
        }
    }

    private void loadDatabaseData() {
        Task<Void> loadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                try {
                    allProducts = productService.getAllProducts();
                } catch (Exception e) {
                    System.err.println("Failed to load products: " + e.getMessage());
                    e.printStackTrace();
                }

                try {
                    allSales = saleService.read();
                } catch (Exception e) {
                    System.err.println("Failed to load sales: " + e.getMessage());
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void succeeded() {
                System.out.println("Successfully loaded " + allProducts.size() + " products from DB.");
                renderProducts(); // Render first!
                updateStats();
                setupFilters();
            }

            @Override
            protected void failed() {
                Throwable e = getException();
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Database Error", "Could not load data: " + e.getMessage());
            }
        };
        Thread thread = new Thread(loadTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void updateStats() {
        if (totalProductsLabel != null)
            totalProductsLabel.setText(String.valueOf(allProducts.size()));

        double revenue = allSales.stream()
                .filter(s -> s.getStatus() != null
                        && (s.getStatus().equalsIgnoreCase("paid") || s.getStatus().equalsIgnoreCase("completed")))
                .mapToDouble(Sale::getTotalAmount)
                .sum();
        if (totalRevenueLabel != null)
            totalRevenueLabel.setText(String.format("%.2f TND", revenue));

        if (totalSalesCountLabel != null)
            totalSalesCountLabel.setText(String.valueOf(allSales.size()));
    }

    private void setupFilters() {
        if (categoryContainer != null) {
            categoryContainer.getChildren().clear();
            List<String> categories = new ArrayList<>(
                    Arrays.asList("All", "Technology", "Finance", "Health", "Education", "Agriculture",
                            "Real Estate", "Retail", "Manufacturing", "Services", "Other"));

            for (String cat : categories) {
                Button btn = new Button(cat);
                btn.getStyleClass().add("category-btn");
                if (cat.equals(activeCategory))
                    btn.getStyleClass().add("active");
                btn.setOnAction(e -> {
                    activeCategory = cat;
                    renderProducts();
                    setupFilters(); // Refresh UI to update active state
                });
                categoryContainer.getChildren().add(btn);
            }
        }
    }

    private void renderProducts() {
        if (productGrid == null)
            return;
        productGrid.getChildren().clear();

        List<Product> filtered = allProducts.stream()
                .filter(p -> activeCategory.equals("All")
                        || (p.getCategory() != null && p.getCategory().equalsIgnoreCase(activeCategory)))
                .filter(p -> {
                    if (tglMyProducts != null && tglMyProducts.isSelected()) {
                        User user = InvestiApp.getCurrentUser();
                        return user != null && p.getEntrepreneurId() != null
                                && p.getEntrepreneurId().equals(user.getId());
                    }
                    return true;
                })
                .collect(Collectors.toList());

        for (Product p : filtered) {
            productGrid.getChildren().add(createProductCard(p));
        }
    }

    private VBox createProductCard(Product p) {
        VBox card = new VBox();
        card.getStyleClass().add("product-card");
        card.setSpacing(0);

        // Image Container
        StackPane imgContainer = new StackPane();
        imgContainer.getStyleClass().add("card-img-container");

        ImageView iv = new ImageView();
        try {
            iv.setImage(new Image(p.getImage(), true));
        } catch (Exception e) {
            // Placeholder if image fails
        }
        iv.setFitWidth(280);
        iv.setFitHeight(180);
        iv.setPreserveRatio(true);

        Label catBadge = new Label(p.getCategory());
        catBadge.getStyleClass().add("category-badge");
        StackPane.setAlignment(catBadge, Pos.TOP_LEFT);

        // Click to show details
        imgContainer.setOnMouseClicked(e -> showProductDetails(p));
        imgContainer.getChildren().addAll(iv, catBadge);

        // Content
        VBox content = new VBox(12);
        content.getStyleClass().add("card-content");

        Label title = new Label(p.getName());
        title.getStyleClass().add("card-title");

        Label desc = new Label(p.getDescription());
        desc.getStyleClass().add("card-desc");
        desc.setWrapText(true);
        desc.setMaxHeight(45);

        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_LEFT);

        VBox priceBox = new VBox(2);
        Label priceLabel = new Label(String.format("%.2f %s", p.getFinalPrice(), p.getCurrency()));
        priceLabel.getStyleClass().add("card-price");
        priceBox.getChildren().add(priceLabel);

        if (p.getRemise() > 0) {
            Label oldPrice = new Label(String.format("%.2f", p.getPrice()));
            oldPrice.getStyleClass().add("meta-label");
            oldPrice.setStyle("-fx-strikethrough: true;");

            Label remiseBadge = new Label("-" + p.getRemise() + "%");
            remiseBadge.setStyle(
                    "-fx-background-color: #ef4444; -fx-text-fill: white; -fx-padding: 2 6; -fx-background-radius: 4; -fx-font-size: 10px; -fx-font-weight: bold;");

            priceBox.getChildren().addAll(oldPrice, remiseBadge);
        }

        Button btnShowQR = new Button();
        try {
            btnShowQR.setGraphic(new FontIcon("fth-maximize"));
        } catch (Exception e) {
            // Fallback
        }
        btnShowQR.getStyleClass().addAll("card-action-btn", "qr");
        btnShowQR.setOnAction(e -> handleShowProductQR(p));

        User user = currentUser;
        boolean isAdmin = user != null && "admin".equalsIgnoreCase(user.getRole());
        boolean isOwner = user != null && p.getEntrepreneurId() != null
                && p.getEntrepreneurId().equals(user.getId());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);

        if (isAdmin || isOwner) {
            Button btnEdit = new Button();
            btnEdit.setGraphic(new FontIcon("fth-edit-2"));
            btnEdit.getStyleClass().addAll("card-action-btn", "edit");
            btnEdit.setOnAction(e -> handleEditProduct(p));
            Tooltip.install(btnEdit, new Tooltip("Modifier"));

            Button btnDel = new Button();
            btnDel.setGraphic(new FontIcon("fth-trash-2"));
            btnDel.getStyleClass().addAll("card-action-btn", "delete");
            btnDel.setOnAction(e -> handleDeleteProduct(p));
            Tooltip.install(btnDel, new Tooltip("Supprimer"));

            actions.getChildren().addAll(btnShowQR, btnEdit, btnDel);
        } else {
            Button btnBuy = new Button("Buy Now");
            btnBuy.setGraphic(new FontIcon("fth-shopping-cart"));
            btnBuy.getStyleClass().addAll("primary-btn"); // Use primary-btn for prominence
            btnBuy.setStyle("-fx-padding: 8 16; -fx-font-size: 13;");
            btnBuy.setOnAction(e -> handleBuyProduct(p));

            actions.getChildren().addAll(btnShowQR, btnBuy);
        }

        // Compare Checkbox
        CheckBox compareCheck = new CheckBox();
        compareCheck.getStyleClass().add("compare-checkbox");
        compareCheck.setOnAction(e -> {
            if (compareCheck.isSelected()) {
                if (selectedProducts.size() < 2) {
                    selectedProducts.add(p);
                } else {
                    compareCheck.setSelected(false);
                    showAlert(Alert.AlertType.WARNING, "Comparison", "You can only compare 2 products at a time.");
                }
            } else {
                selectedProducts.remove(p);
            }
            updateCompareButton();
        });
        actions.getChildren().add(0, compareCheck);

        footer.getChildren().addAll(priceBox, spacer, actions);
        content.getChildren().addAll(title, desc, footer);
        card.getChildren().addAll(imgContainer, content);

        return card;
    }

    private void updateCompareButton() {
        if (btnCompare != null) {
            btnCompare.setVisible(!selectedProducts.isEmpty());
            btnCompare.setManaged(!selectedProducts.isEmpty());
            btnCompare.setText("Compare (" + selectedProducts.size() + "/2)");
        }
    }

    @FXML
    private void handleCompareAction() {
        if (selectedProducts.size() < 2) {
            showAlert(Alert.AlertType.INFORMATION, "Comparison", "Select 2 products to compare.");
            return;
        }

        Alert loading = new Alert(Alert.AlertType.INFORMATION, "AI is analyzing technical specifications...");
        loading.show();

        navyApiService.compareProducts(selectedProducts.get(0), selectedProducts.get(1))
                .thenAccept(comparison -> {
                    Platform.runLater(() -> {
                        loading.close();
                        showComparisonResult(comparison);
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        loading.close();
                        showAlert(Alert.AlertType.ERROR, "AI Error", "Comparison failed: " + ex.getMessage());
                    });
                    return null;
                });
    }

    private void showComparisonResult(String content) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("AI Product Comparison");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        WebView wv = new WebView();
        wv.getEngine().loadContent(
                "<body style='font-family:sans-serif; padding:20px; line-height:1.6;'>" + content + "</body>");
        wv.setPrefSize(700, 500);

        dialog.getDialogPane().setContent(wv);
        dialog.showAndWait();
    }

    @FXML
    private void handleAiSearch() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("AI Semantic Search");
        dialog.setHeaderText("Describe what you are looking for (e.g., 'a powerful laptop for gaming')");
        dialog.setContentText("Description:");

        dialog.showAndWait().ifPresent(query -> {
            Alert loading = new Alert(Alert.AlertType.INFORMATION, "AI is searching your catalog...");
            loading.show();

            navyApiService.searchProductsByText(query, allProducts)
                    .thenAccept(ids -> {
                        Platform.runLater(() -> {
                            loading.close();
                            filterGridByIds(ids);
                        });
                    });
        });
    }

    private void filterGridByIds(List<Long> ids) {
        productGrid.getChildren().clear();
        for (Product p : allProducts) {
            if (ids.contains(p.getId())) {
                productGrid.getChildren().add(createProductCard(p));
            }
        }
    }

    @FXML
    private void handleImageSearch() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Image for AI Search");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.jpg", "*.png", "*.jpeg", "*.webp"));
        File file = fc.showOpenDialog(rootStack.getScene().getWindow());

        if (file != null) {
            Alert loading = new Alert(Alert.AlertType.INFORMATION, "AI is analyzing image landmarks...");
            loading.show();

            navyApiService.searchProductsByImage(file, allProducts)
                    .thenAccept(ids -> {
                        Platform.runLater(() -> {
                            loading.close();
                            filterGridByIds(ids);
                        });
                    });
        }
    }

    @FXML
    private void handleQRSearch() {
        FileChooser fc = new FileChooser();
        File file = fc.showOpenDialog(rootStack.getScene().getWindow());
        if (file != null) {
            String decoded = QRCodeUtils.decodeQRCode(file);
            if (decoded != null && decoded.startsWith("ID: ")) {
                try {
                    long id = Long.parseLong(decoded.split("\n")[0].substring(4).trim());
                    filterGridByIds(Collections.singletonList(id));
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "QR Error", "Invalid QR format.");
                }
            } else {
                showAlert(Alert.AlertType.WARNING, "QR Search", "Could not decode or find ID.");
            }
        }
    }

    @FXML
    private void handleNewProduct() {
        openProductForm(null);
    }

    private void openProductForm(Product p) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/collaboration/AddProduct.fxml"));
            VBox form = loader.load();
            AddProductController ctrl = loader.getController();
            ctrl.setProductData(p);
            ctrl.setOnProductAdded(v -> {
                loadDatabaseData();
                renderProducts();
                hideForm();
            });
            ctrl.setOnCancel(this::hideForm);

            addProductPage.getChildren().setAll(form);
            addProductPage.setVisible(true);
            addProductPage.setManaged(true);
            productsPage.setVisible(false);
            productsPage.setManaged(false);
            filtersContainer.setVisible(false);
            filtersContainer.setManaged(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void hideForm() {
        addProductPage.setVisible(false);
        addProductPage.setManaged(false);
        productsPage.setVisible(true);
        productsPage.setManaged(true);
        filtersContainer.setVisible(true);
        filtersContainer.setManaged(true);
        orderHistoryPage.setVisible(false);
        orderHistoryPage.setManaged(false);
    }

    private void handleBuyProduct(Product p) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/collaboration/AddSale.fxml"));
            VBox form = loader.load();
            AddSaleController ctrl = loader.getController();
            ctrl.setProduct(p);
            ctrl.setOnSaleCreated(v -> {
                loadDatabaseData();
                handleHistoryClick(); // Go to history
            });
            ctrl.setOnCancel(this::hideForm);

            addProductPage.getChildren().setAll(form);
            addProductPage.setVisible(true);
            addProductPage.setManaged(true);
            productsPage.setVisible(false);
            productsPage.setManaged(false);
            filtersContainer.setVisible(false);
            filtersContainer.setManaged(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleEditProduct(Product p) {
        openProductForm(p);
    }

    private void handleDeleteProduct(Product p) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer '" + p.getName() + "' ?");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    productService.delete(p.getId(), InvestiApp.getCurrentUser());
                    loadDatabaseData();
                    renderProducts();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @FXML
    private void handleHistoryClick() {
        hideForm();
        productsPage.setVisible(false);
        productsPage.setManaged(false);
        filtersContainer.setVisible(false);
        filtersContainer.setManaged(false);

        orderHistoryPage.setVisible(true);
        orderHistoryPage.setManaged(true);
        historyHeader.setVisible(true);
        historyHeader.setManaged(true);
        mainHeader.setVisible(false);
        mainHeader.setManaged(false);

        renderOrders();
    }

    private void renderOrders() {
        if (ordersContainer == null)
            return;
        ordersContainer.getChildren().clear();

        User user = InvestiApp.getCurrentUser();
        boolean isAdmin = user != null && "admin".equalsIgnoreCase(user.getRole());

        List<Sale> filtered = allSales.stream()
                .filter(s -> isAdmin || (user != null && s.getCustomerId().equals(user.getId())))
                .collect(Collectors.toList());

        for (Sale s : filtered) {
            ordersContainer.getChildren().add(createOrderCard(s));
        }
    }

    private VBox createOrderCard(Sale s) {
        VBox card = new VBox(15);
        card.getStyleClass().add("order-card");

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        VBox info = new VBox(5);
        Label ref = new Label("Order #" + s.getReference());
        ref.getStyleClass().add("order-ref");
        Label date = new Label("Made on " + s.getCreatedAt());
        date.getStyleClass().add("order-date");
        info.getChildren().addAll(ref, date);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Label status = new Label(s.getStatus().toUpperCase());
        status.getStyleClass().addAll("status-chip", "status-" + s.getStatus().toLowerCase());

        header.getChildren().addAll(info, sp, status);

        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_LEFT);

        Label total = new Label(String.format("Total: %.2f %s", s.getTotalAmount(), s.getCurrency()));
        total.getStyleClass().add("order-total-amount");

        Region sp2 = new Region();
        HBox.setHgrow(sp2, Priority.ALWAYS);

        Button invoiceBtn = new Button("Facture");
        invoiceBtn.setGraphic(new FontIcon("fth-download"));
        invoiceBtn.getStyleClass().addAll("secondary-btn");
        invoiceBtn.setStyle("-fx-padding: 6 12; -fx-font-size: 12;");
        invoiceBtn.setOnAction(e -> handleExportInvoice(s));

        if ("unpaid".equalsIgnoreCase(s.getStatus())) {
            Button payBtn = new Button("Payer");
            payBtn.setGraphic(new FontIcon("fth-credit-card"));
            payBtn.getStyleClass().addAll("primary-btn");
            payBtn.setStyle("-fx-padding: 6 12; -fx-font-size: 12; -fx-background-color: #f59e0b;");
            payBtn.setOnAction(e -> handlePaySale(s));
            footer.getChildren().addAll(total, sp2, payBtn, invoiceBtn);
        } else {
            footer.getChildren().addAll(total, sp2, invoiceBtn);
        }

        card.getChildren().addAll(header, new Separator(), footer);
        return card;
    }

    private void handlePaySale(Sale s) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Paiement");
        confirm.setHeaderText(
                "Confirmer le paiement de " + String.format("%.2f %s", s.getTotalAmount(), s.getCurrency()));
        confirm.setContentText("Voulez-vous procéder au paiement ?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    saleService.paySale(s.getId());
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "Paiement effectué avec succès !");
                    loadDatabaseData(); // Refresh history
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Échec du paiement : " + e.getMessage());
                }
            }
        });
    }

    private void handleExportInvoice(Sale s) {
        FileChooser fc = new FileChooser();
        fc.setInitialFileName("Invoice_" + s.getReference() + ".pdf");
        File file = fc.showSaveDialog(rootStack.getScene().getWindow());
        if (file != null) {
            try {
                exportService.exportSaleToPdf(s, file);
                showAlert(Alert.AlertType.INFORMATION, "Success", "Invoice saved.");
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Export", "Failed: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleShowSalesMap() {
        Stage stage = new Stage();
        WebView wv = new WebView();
        // Simplified map logic for now - it expects resources in /
        try {
            wv.getEngine().load(getClass().getResource("/sales_map.html").toExternalForm());
        } catch (Exception e) {
        }

        stage.setScene(new Scene(new StackPane(wv), 800, 600));
        stage.show();
    }

    @FXML
    private void handleCartClick() {
        // Simple switch
        productsPage.setVisible(!productsPage.isVisible());
        cartPage.setVisible(!productsPage.isVisible());
    }

    private void showProductDetails(Product p) {
        if (detailOverlay == null)
            return;

        detailTitle.setText(p.getName());
        detailShortDesc.setText(p.getCategory());
        detailPrice.setText(String.format("%.2f", p.getFinalPrice()));
        detailCurrency.setText(p.getCurrency());
        detailViews.setText(String.valueOf(p.getViewsCount()));
        detailSales.setText(String.valueOf(p.getSalesCount()));
        detailStatus.setText(p.getStatus());

        detailImage.setImage(new Image(p.getImage(), true));

        detailFullDesc.getChildren().clear();
        Text t = new Text(p.getDescription());
        t.setFill(Color.web("#64748b"));
        detailFullDesc.getChildren().add(t);

        detailOverlay.setUserData(p);
        detailOverlay.setVisible(true);
        detailOverlay.toFront();

        // Track view
        try {
            productService.incrementViewsCount(p.getId());
        } catch (Exception e) {
        }
    }

    @FXML
    private void hideProductDetails() {
        detailOverlay.setVisible(false);
    }

    @FXML
    private void handleBuyFromDetails() {
        Product p = (Product) detailOverlay.getUserData();
        if (p != null) {
            hideProductDetails();
            handleBuyProduct(p);
        }
    }

    // Window controls
    @FXML
    private void handleWindowMinimize() {
        ((Stage) rootStack.getScene().getWindow()).setIconified(true);
    }

    @FXML
    private void handleWindowMaximize() {
        Stage s = (Stage) rootStack.getScene().getWindow();
        s.setMaximized(!s.isMaximized());
    }

    @FXML
    private void handleWindowClose() {
        ((Stage) rootStack.getScene().getWindow()).close();
    }

    @FXML
    private void handleShowProductQR(Product p) {
        String qrContent = "ID: " + p.getId() + "\nName: " + p.getName() + "\nPrice: " + p.getFinalPrice() + " "
                + p.getCurrency();
        Image qrImage = QRCodeUtils.generateQRCode(qrContent, 300, 300);

        if (qrImage != null) {
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Product QR Code - " + p.getName());
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

            ImageView iv = new ImageView(qrImage);
            iv.setFitWidth(300);
            iv.setFitHeight(300);
            iv.setPreserveRatio(true);

            VBox box = new VBox(20, new Label("Scan to identify product:"), iv);
            box.setAlignment(Pos.CENTER);
            box.setStyle("-fx-padding: 30;");

            dialog.getDialogPane().setContent(box);
            dialog.showAndWait();
        } else {
            showAlert(Alert.AlertType.ERROR, "QR Error", "Failed to generate QR code.");
        }
    }

    @FXML
    private void handleBackToDashboard() {
        try {
            InvestiApp.showCollaborationModule(currentUser);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Could not return to dashboard: " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.show();
    }
}
