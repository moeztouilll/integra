package controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.web.*;
import javafx.scene.image.*;
import javafx.scene.text.TextFlow;
import javafx.scene.paint.Color;
import javafx.scene.effect.DropShadow;
import javafx.geometry.Pos;
import models.Product;
import models.Sale;
import services.ProductService;
import services.SaleService;
import services.ExportService;
import services.SessionManager;
import models.UserSession;
import org.kordamp.ikonli.javafx.FontIcon;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import java.io.File;
import javafx.scene.Scene;
import javafx.concurrent.Worker;
import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.exception.StripeException;
import java.net.URL;
import java.sql.SQLException;
import java.util.*;
import utils.QRCodeUtils;
import java.util.stream.Collectors;

public class MainController implements Initializable {

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
    private Button btnAddNewProduct;
    @FXML
    private VBox addProductPage;
    @FXML
    private HBox titleBar;
    @FXML
    private ComboBox<UserSession> userSelector;
    @FXML
    private ToggleButton tglMyProducts;

    private final ProductService productService = new ProductService();
    private final SaleService saleService = new SaleService();
    private final services.NavyApiService navyApiService = new services.NavyApiService();
    private double xOffset = 0;
    private double yOffset = 0;

    // Sidebar items
    @FXML
    private Button navProducts, navHistory, navCart, navSettings;

    // Details Overlay
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

    // Order History Page
    @FXML
    private VBox orderHistoryPage, ordersContainer;

    private List<Product> allProducts = new ArrayList<>();
    private List<Product> selectedProducts = new ArrayList<>();
    private List<Sale> allSales = new ArrayList<>();
    private String activeCategory = "All";
    private String activeSaleStatus = "All";
    private ExportService exportService = new ExportService();

    @FXML
    private Button btnCompare;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupWindowDragging();
        try {
            setupUserSession();
            loadDatabaseData();
            setupCategories();
            setupSaleFilters();
            setupSidebar();

            // Setup My Products toggle
            if (tglMyProducts != null) {
                tglMyProducts.setOnAction(e -> renderProducts());
            }

            renderProducts();

            // Ensure overlay is hidden initially
            if (detailOverlay != null)
                detailOverlay.setVisible(false);
        } catch (Throwable t) {
            System.err.println("FATAL ERROR during initialization:");
            t.printStackTrace();
        }
    }

    private void loadDatabaseData() {
        try {
            if (productService == null) {
                System.err.println("DEBUG: productService is NULL!");
                return;
            }
            allProducts = productService.read();

            if (saleService == null) {
                System.err.println("DEBUG: saleService is NULL!");
            } else {
                allSales = saleService.read();
            }

            System.out.println("DEBUG: Loaded " + (allProducts != null ? allProducts.size() : 0) + " products.");
            System.out.println("DEBUG: Loaded " + (allSales != null ? allSales.size() : 0) + " sales.");

            // Update stats
            if (totalProductsLabel != null && allProducts != null)
                totalProductsLabel.setText(String.valueOf(allProducts.size()));
            if (totalSalesCountLabel != null && allSales != null)
                totalSalesCountLabel.setText(String.valueOf(allSales.size()));
            if (totalRevenueLabel != null && allSales != null) {
                double total = allSales.stream().mapToDouble(Sale::getTotalAmount).sum();
                totalRevenueLabel.setText(String.format("%.2f", total));
            }
        } catch (SQLException e) {
            System.err.println("DEBUG: SQL Error: " + e.getMessage());
            e.printStackTrace();
        } catch (Throwable t) {
            System.err.println("DEBUG: Unexpected Error in loadDatabaseData:");
            t.printStackTrace();
        }
    }

    private void setupUserSession() {
        if (userSelector != null) {
            userSelector.getItems().setAll(SessionManager.getAllUsers());
            userSelector.setValue(SessionManager.getCurrentUser());
            userSelector.setOnAction(e -> {
                SessionManager.setCurrentUser(userSelector.getValue());
                // Reset toggle on user switch
                if (tglMyProducts != null)
                    tglMyProducts.setSelected(false);
                renderProducts();
                renderOrders();

                // Refresh top stats
                long productCount = allProducts.stream()
                        .filter(p -> SessionManager.getCurrentUser().getRole() == UserSession.Role.ADMIN
                                || p.getEntrepreneurId() == SessionManager.getCurrentUser().getId())
                        .count();
                if (totalProductsLabel != null)
                    totalProductsLabel.setText(String.valueOf(productCount));

                List<Sale> visibleSales = allSales.stream()
                        .filter(s -> SessionManager.getCurrentUser().getRole() == UserSession.Role.ADMIN
                                || s.getCustomerId() == SessionManager.getCurrentUser().getId())
                        .collect(Collectors.toList());

                if (totalSalesCountLabel != null)
                    totalSalesCountLabel.setText(String.valueOf(visibleSales.size()));
                if (totalRevenueLabel != null) {
                    double total = visibleSales.stream().mapToDouble(Sale::getTotalAmount).sum();
                    totalRevenueLabel.setText(String.format("$%.2f", total));
                }
            });
        }
    }

    private void setupCategories() {
        String[] cats = { "All", "Informatique", "Voiture", "Immobilier", "Vêtements", "Services", "Loisirs" };
        categoryContainer.getChildren().clear();
        for (String cat : cats) {
            Button btn = new Button(cat);
            btn.getStyleClass().add("category-btn");
            if (cat.equals(activeCategory))
                btn.getStyleClass().add("active");

            btn.setOnAction(e -> {
                activeCategory = cat;
                setupCategories();
                renderProducts();
            });
            categoryContainer.getChildren().add(btn);
        }
    }

    private void setupSaleFilters() {
        if (saleCategoryContainer == null)
            return;
        String[] statuses = { "All", "Paid", "Completed", "Pending", "Cancelled" };
        saleCategoryContainer.getChildren().clear();
        for (String status : statuses) {
            Button btn = new Button(status);
            btn.getStyleClass().add("category-btn");
            if (status.equals(activeSaleStatus))
                btn.getStyleClass().add("active");

            btn.setOnAction(e -> {
                activeSaleStatus = status;
                setupSaleFilters();
                renderOrders();
            });
            saleCategoryContainer.getChildren().add(btn);
        }
    }

    private void setupSidebar() {
        navProducts.setOnAction(e -> switchTab("products", navProducts));
        navHistory.setOnAction(e -> switchTab("history", navHistory));
        // navCart is no longer used for shopping, maybe for something else later?
        if (navCart != null) {
            navCart.setVisible(false);
            navCart.setManaged(false);
        }
    }

    @FXML
    private void handleHistoryClick() {
        switchTab("history", navHistory);
    }

    private void handleEditSale(Sale s) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/add-sale.fxml"));
            VBox form = loader.load();

            AddSaleController controller = loader.getController();
            controller.setSaleData(s);
            controller.setOnSaleCreated(v -> {
                loadDatabaseData();
                renderOrders();
                switchTab("history", navHistory);
            });
            controller.setOnCancel(() -> switchTab("history", navHistory));

            addProductPage.getChildren().setAll(form);
            switchTab("add-sale", navHistory);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    private void handleDeleteSale(Sale s) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Sale");
        alert.setHeaderText("Are you sure you want to delete this sale record?");
        alert.setContentText("Reference: " + s.getReference());

        if (alert.showAndWait().get() == ButtonType.OK) {
            try {
                new services.SaleService().delete((int) s.getId());
                loadDatabaseData();
                renderOrders();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void switchTab(String tab, Button activeBtn) {

        // Reset nav styles
        navProducts.getStyleClass().remove("active");
        navCart.getStyleClass().remove("active");
        if (navHistory != null)
            navHistory.getStyleClass().remove("active");
        if (navSettings != null)
            navSettings.getStyleClass().remove("active");

        activeBtn.getStyleClass().add("active");

        // Hide all pages
        if (productsPage != null) {
            productsPage.setVisible(false);
            productsPage.setManaged(false);
        }
        if (cartPage != null) {
            cartPage.setVisible(false);
            cartPage.setManaged(false);
        }
        if (orderHistoryPage != null) {
            orderHistoryPage.setVisible(false);
            orderHistoryPage.setManaged(false);
        }
        if (addProductPage != null) {
            addProductPage.setVisible(false);
            addProductPage.setManaged(false);
        }

        // Show selected
        if (tab.equals("products") && productsPage != null) {
            productsPage.setVisible(true);
            productsPage.setManaged(true);
        } else if (tab.equals("history") && orderHistoryPage != null) {
            orderHistoryPage.setVisible(true);
            orderHistoryPage.setManaged(true);
            renderOrders();
            // Hide product filters
            if (filtersContainer != null) {
                filtersContainer.setVisible(false);
                filtersContainer.setManaged(false);
            }
            // Show sale filters
            if (saleFiltersContainer != null) {
                saleFiltersContainer.setVisible(true);
                saleFiltersContainer.setManaged(true);
            }
            // Switch Headers
            if (mainHeader != null) {
                mainHeader.setVisible(false);
                mainHeader.setManaged(false);
            }
            if (historyHeader != null) {
                historyHeader.setVisible(true);
                historyHeader.setManaged(true);
            }
        } else if (tab.equals("add-product") || tab.equals("add-sale")) {
            addProductPage.setVisible(true);
            addProductPage.setManaged(true);
            if (mainHeader != null) {
                mainHeader.setVisible(false);
                mainHeader.setManaged(false);
            }
            if (historyHeader != null) {
                historyHeader.setVisible(false);
                historyHeader.setManaged(false);
            }
            if (filtersContainer != null) {
                filtersContainer.setVisible(false);
                filtersContainer.setManaged(false);
            }
            if (saleFiltersContainer != null) {
                saleFiltersContainer.setVisible(false);
                saleFiltersContainer.setManaged(false);
            }
        }

        // Restore header/filters if on products page
        if (tab.equals("products")) {
            if (mainHeader != null) {
                mainHeader.setVisible(true);
                mainHeader.setManaged(true);
            }
            if (historyHeader != null) {
                historyHeader.setVisible(false);
                historyHeader.setManaged(false);
            }
            if (filtersContainer != null) {
                filtersContainer.setVisible(true);
                filtersContainer.setManaged(true);
            }
            if (saleFiltersContainer != null) {
                saleFiltersContainer.setVisible(false);
                saleFiltersContainer.setManaged(false);
            }
        }
    }

    @FXML
    private void handleNewProduct() {
        openProductForm(null);
    }

    private void openProductForm(Product existing) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/add-product.fxml"));
            VBox form = loader.load();

            AddProductController controller = loader.getController();
            controller.setProductData(existing);
            controller.setOnProductAdded(v -> {
                loadDatabaseData();
                renderProducts();
                switchTab("products", navProducts);
            });
            controller.setOnCancel(() -> {
                switchTab("products", navProducts);
            });

            addProductPage.getChildren().setAll(form);
            switchTab("add-product", btnAddNewProduct);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAiSearch() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Assistant de Recherche IA");

        // Custom Header with Gradient
        VBox header = new VBox(10);
        header.setStyle("-fx-background-color: linear-gradient(to right, #6366f1, #a855f7); -fx-padding: 20;");
        Label headerTitle = new Label("✨ Recherche Sémantique");
        headerTitle.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
        Label headerSub = new Label("Décrivez ce que vous cherchez, l'IA s'occupe du reste.");
        headerSub.setStyle("-fx-text-fill: rgba(255,255,255,0.8); -fx-font-size: 12px;");
        header.getChildren().addAll(headerTitle, headerSub);

        // Content Area
        VBox content = new VBox(15);
        content.setStyle("-fx-padding: 25; -fx-background-color: white;");

        TextArea textArea = new TextArea();
        textArea.setPromptText("Exemple: Je cherche un cadeau pour un gamer passionné de rétro...");
        textArea.setWrapText(true);
        textArea.setPrefRowCount(3);
        textArea.setStyle(
                "-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #e0e0e0; -fx-font-size: 14px;");

        content.getChildren().addAll(new Label("Votre description :"), textArea);

        dialog.getDialogPane().setHeader(header);
        dialog.getDialogPane().setContent(content);

        ButtonType searchBtnType = new ButtonType("Lancer la recherche", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(searchBtnType, ButtonType.CANCEL);

        Button searchBtn = (Button) dialog.getDialogPane().lookupButton(searchBtnType);
        searchBtn.setStyle(
                "-fx-background-color: linear-gradient(to right, #6366f1, #a855f7); -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 6;");

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == searchBtnType)
                return textArea.getText();
            return null;
        });

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(requirement -> {
            if (requirement.trim().isEmpty())
                return;

            System.out.println("LOG: AI Search for: " + requirement);

            navyApiService.searchProductsByText(requirement, allProducts)
                    .thenAccept(matchingIds -> {
                        Platform.runLater(() -> {
                            if (matchingIds.isEmpty()) {
                                showAlert(Alert.AlertType.INFORMATION, "Aucun Résultat",
                                        "L'IA n'a trouvé aucun produit correspondant à votre description.");
                                return;
                            }
                            renderFilteredProducts(matchingIds);
                        });
                    })
                    .exceptionally(ex -> {
                        ex.printStackTrace();
                        return null;
                    });
        });
    }

    private void toggleProductSelection(Product p, FontIcon icon) {
        if (selectedProducts.contains(p)) {
            selectedProducts.remove(p);
            icon.setIconLiteral("fth-square");
        } else {
            if (selectedProducts.size() >= 2) {
                showAlert(Alert.AlertType.WARNING, "Maximum atteint",
                        "Vous ne pouvez comparer que deux produits à la fois.");
                return;
            }
            selectedProducts.add(p);
            icon.setIconLiteral("fth-check-square");
        }
        updateCompareButton();
    }

    private void updateCompareButton() {
        if (btnCompare != null) {
            int count = selectedProducts.size();
            btnCompare.setText("Compare (" + count + "/2)");
            btnCompare.setVisible(count > 0);
            btnCompare.setManaged(count > 0);

            // Highlight if exactly 2
            if (count == 2) {
                btnCompare.setStyle("-fx-background-color: linear-gradient(to right, #6366f1, #a855f7);");
            } else {
                btnCompare.setStyle("-fx-background-color: #10b981;");
            }
        }
    }

    @FXML
    private void handleCompareAction() {
        if (selectedProducts.size() < 2) {
            showAlert(Alert.AlertType.INFORMATION, "Action Requise",
                    "Veuillez sélectionner deux produits pour lancer la comparaison.");
            return;
        }

        showComparisonOverlay(selectedProducts.get(0), selectedProducts.get(1));
    }

    private void showComparisonOverlay(Product p1, Product p2) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Comparaison & Assistant IA");
        dialog.getDialogPane().setPrefSize(1000, 800);

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #f8fafc;");

        // Header
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: linear-gradient(to right, #6366f1, #a855f7); -fx-padding: 20;");
        Label headerTitle = new Label("✨ Assistant Comparatif IA");
        headerTitle.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button closeBtn = new Button("Fermer");
        closeBtn.setStyle(
                "-fx-background-color: rgba(255,255,255,0.2); -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 8 15;");
        closeBtn.setOnAction(e -> dialog.close());
        header.getChildren().addAll(headerTitle, spacer, closeBtn);

        // Add hidden button type to allow closing
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        javafx.scene.Node cancelBtn = dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelBtn.setVisible(false);
        cancelBtn.setManaged(false);

        // Content Split: Comparison (Left) | Chatbot (Right)
        HBox mainContent = new HBox(0);
        VBox.setVgrow(mainContent, Priority.ALWAYS);

        // Left Side: Products and Analysis
        VBox leftSide = new VBox(20);
        leftSide.setPrefWidth(550);
        leftSide.setStyle("-fx-padding: 25; -fx-background-color: white;");

        HBox productsBox = new HBox(20);
        productsBox.setAlignment(Pos.CENTER);
        productsBox.getChildren().addAll(createSimpleProductView(p1), new Label("VS"), createSimpleProductView(p2));
        ((Label) productsBox.getChildren().get(1))
                .setStyle("-fx-font-weight: bold; -fx-font-size: 24px; -fx-text-fill: #cbd5e1;");

        VBox analysisBox = new VBox(10);
        analysisBox.setStyle("-fx-padding: 20; -fx-background-color: #f1f5f9; -fx-background-radius: 12;");
        Label analysisTitle = new Label("🔍 Analyse de l'Expert IA");
        analysisTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #1e293b;");

        ScrollPane scrollAnalysis = new ScrollPane();
        scrollAnalysis.setFitToWidth(true);
        scrollAnalysis.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        Label analysisContent = new Label("Analyse en cours...");
        analysisContent.setWrapText(true);
        analysisContent.setStyle("-fx-font-size: 14px; -fx-text-fill: #334155; -fx-line-spacing: 5;");
        scrollAnalysis.setContent(analysisContent);

        analysisBox.getChildren().addAll(analysisTitle, scrollAnalysis);
        leftSide.getChildren().addAll(productsBox, analysisBox);

        // Right Side: Chatbot
        VBox rightSide = new VBox(0);
        HBox.setHgrow(rightSide, Priority.ALWAYS);
        rightSide.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; -fx-border-width: 0 0 0 1;");

        Label chatHeader = new Label("💬 Chatbot Produits");
        chatHeader.setStyle(
                "-fx-padding: 15; -fx-font-weight: bold; -fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-width: 0 0 1 0;");
        chatHeader.setMaxWidth(Double.MAX_VALUE);

        VBox messageContainer = new VBox(15);
        messageContainer.setStyle("-fx-padding: 20;");
        ScrollPane chatScroll = new ScrollPane(messageContainer);
        chatScroll.setFitToWidth(true);
        VBox.setVgrow(chatScroll, Priority.ALWAYS);
        chatScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        // Auto-scroll logic
        messageContainer.heightProperty().addListener((obs, oldVal, newVal) -> chatScroll.setVvalue(1.0));

        HBox inputZone = new HBox(10);
        inputZone.setStyle(
                "-fx-padding: 15; -fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-width: 1 0 0 0;");
        TextField chatInput = new TextField();
        chatInput.setPromptText("Posez une question...");
        HBox.setHgrow(chatInput, Priority.ALWAYS);
        chatInput.setStyle("-fx-background-radius: 20; -fx-padding: 10 15;");

        Button sendBtn = new Button();
        sendBtn.setGraphic(new FontIcon("fth-send"));
        sendBtn.setStyle(
                "-fx-background-color: #6366f1; -fx-text-fill: white; -fx-background-radius: 50%; -fx-min-width: 40; -fx-min-height: 40;");
        inputZone.getChildren().addAll(chatInput, sendBtn);

        rightSide.getChildren().addAll(chatHeader, chatScroll, inputZone);
        mainContent.getChildren().addAll(leftSide, rightSide);

        root.getChildren().addAll(header, mainContent);
        dialog.getDialogPane().setContent(root);

        // Actions
        navyApiService.compareProducts(p1, p2).thenAccept(res -> {
            Platform.runLater(() -> analysisContent.setText(cleanAiText(res)));
        });

        addChatMessage(messageContainer, "IA",
                "Bonjour ! Je suis prêt à répondre à vos questions sur ces deux produits.");

        Runnable sendAction = () -> {
            String text = chatInput.getText().trim();
            if (text.isEmpty())
                return;

            addChatMessage(messageContainer, "Vous", text);
            chatInput.clear();

            navyApiService.chatAboutProducts(p1, p2, text).thenAccept(reply -> {
                Platform.runLater(() -> addChatMessage(messageContainer, "IA", cleanAiText(reply)));
            });
        };

        sendBtn.setOnAction(e -> sendAction.run());
        chatInput.setOnAction(e -> sendAction.run());

        dialog.show();
    }

    private void addChatMessage(VBox container, String sender, String message) {
        Platform.runLater(() -> {
            VBox bubbleBox = new VBox(5);
            bubbleBox.setAlignment(sender.equals("Vous") ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

            Label label = new Label(message);
            label.setWrapText(true);
            label.setMaxWidth(300);

            if (sender.equals("Vous")) {
                label.setStyle(
                        "-fx-background-color: #6366f1; -fx-text-fill: white; -fx-padding: 10 15; -fx-background-radius: 15 15 2 15;");
            } else {
                label.setStyle(
                        "-fx-background-color: #e2e8f0; -fx-text-fill: #1e293b; -fx-padding: 10 15; -fx-background-radius: 15 15 15 2;");
            }

            bubbleBox.getChildren().add(label);
            container.getChildren().add(bubbleBox);
        });
    }

    private String cleanAiText(String text) {
        if (text == null)
            return "";
        return text.replace("**", "").replace("*", "").replace("###", "").trim();
    }

    private VBox createSimpleProductView(Product p) {
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        box.setStyle(
                "-fx-padding: 15; -fx-background-color: #f8fafc; -fx-background-radius: 12; -fx-border-color: #e2e8f0; -fx-border-radius: 12;");
        box.setPrefWidth(220);

        ImageView iv = new ImageView();
        try {
            iv.setImage(new Image(p.getImage(), true));
        } catch (Exception e) {
            iv.setImage(new Image("https://via.placeholder.com/100", true));
        }
        iv.setFitWidth(100);
        iv.setFitHeight(100);
        iv.setPreserveRatio(true);

        Label name = new Label(p.getName());
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        name.setWrapText(true);
        name.setAlignment(Pos.CENTER);

        Label price = new Label(p.getCurrency() + " " + p.getPrice());
        price.setStyle("-fx-text-fill: #6366f1; -fx-font-weight: bold;");

        box.getChildren().addAll(iv, name, price);
        return box;
    }

    private void renderFilteredProducts(List<Long> matchingIds) {
        productGrid.getChildren().clear();
        for (Product p : allProducts) {
            if (matchingIds.contains((long) p.getId())) {
                VBox card = createProductCard(p);
                productGrid.getChildren().add(card);
            }
        }
    }

    private void renderProducts() {
        productGrid.getChildren().clear();

        UserSession currentUser = SessionManager.getCurrentUser();
        boolean showOnlyMine = (tglMyProducts != null && tglMyProducts.isSelected());

        List<Product> filtered = allProducts.stream()
                .filter(p -> activeCategory.equals("All")
                        || (p.getCategory() != null && p.getCategory().equalsIgnoreCase(activeCategory)))
                .filter(p -> !showOnlyMine || p.getEntrepreneurId() == currentUser.getId())
                .collect(Collectors.toList());

        System.out.println("DEBUG: Rendering " + filtered.size() + " products for category: " + activeCategory);
        filtered.forEach(p -> productGrid.getChildren().add(createProductCard(p)));
    }

    private VBox createProductCard(Product p) {
        VBox card = new VBox();
        card.getStyleClass().add("product-card");
        card.setCursor(javafx.scene.Cursor.HAND);
        card.setOnMouseClicked(e -> showProductDetails(p));

        StackPane imgContainer = new StackPane();
        imgContainer.getStyleClass().add("card-img-container");
        imgContainer.setStyle("-fx-background-color: transparent;");

        String imageUrl = p.getImage();
        if (imageUrl == null || imageUrl.isEmpty()) {
            imageUrl = "https://via.placeholder.com/180"; // Fallback URL
        }

        ImageView iv = new ImageView();
        try {
            iv.setImage(new Image(imageUrl, true));
        } catch (Exception e) {
            System.err.println("DEBUG: Failed to load image: " + imageUrl);
            iv.setImage(new Image("https://via.placeholder.com/180", true));
        }

        iv.setFitWidth(180);
        iv.setFitHeight(180);
        iv.setPreserveRatio(true);
        iv.setEffect(new DropShadow(15, Color.rgb(0, 0, 0, 0.2)));

        imgContainer.getChildren().add(iv);

        // Selection Marker (for comparison)
        FontIcon selectIcon = new FontIcon(selectedProducts.contains(p) ? "fth-check-square" : "fth-square");
        selectIcon.getStyleClass().add("selection-marker");
        selectIcon.setStyle("-fx-icon-color: #a855f7; -fx-icon-size: 20;");
        StackPane.setAlignment(selectIcon, Pos.TOP_RIGHT);
        StackPane.setMargin(selectIcon, new javafx.geometry.Insets(10));

        selectIcon.setCursor(javafx.scene.Cursor.HAND);
        selectIcon.setOnMouseClicked(e -> {
            e.consume(); // Prevent card details from opening
            toggleProductSelection(p, selectIcon);
        });

        imgContainer.getChildren().add(selectIcon);

        VBox content = new VBox(15);
        content.getStyleClass().add("card-content");

        Label title = new Label(p.getTitle());
        title.getStyleClass().add("card-title");

        // Short description removed.

        VBox footer = new VBox(10);
        footer.getStyleClass().add("card-footer");

        VBox priceAndStockBox = new VBox(5);
        priceAndStockBox.setAlignment(Pos.CENTER_LEFT);

        HBox priceBox = new HBox(8);
        priceBox.setAlignment(Pos.CENTER_LEFT);

        if (p.getRemise() > 0) {
            Label originalPrice = new Label(p.getCurrency() + " " + p.getPrice());
            originalPrice.setStyle("-fx-strikethrough: true; -fx-text-fill: gray; -fx-font-size: 11px;");

            double discountedAmount = p.getPrice() * (1 - p.getRemise() / 100.0);
            Label newPrice = new Label(p.getCurrency() + " " + String.format("%.2f", discountedAmount));
            newPrice.getStyleClass().add("card-price");
            newPrice.setStyle("-fx-text-fill: #e11d48;"); // Highlighted red for discount

            Label discountBadge = new Label("-" + p.getRemise() + "%");
            discountBadge.setStyle(
                    "-fx-background-color: #e11d48; -fx-text-fill: white; -fx-padding: 3 6; -fx-background-radius: 4; -fx-font-size: 11px; -fx-font-weight: bold;");

            VBox discountBox = new VBox(2);
            discountBox.getChildren().addAll(originalPrice, newPrice);

            priceBox.getChildren().addAll(discountBadge, discountBox);
        } else {
            Label price = new Label(p.getCurrency() + " " + p.getPrice());
            price.getStyleClass().add("card-price");
            priceBox.getChildren().add(price);
        }

        Label stockLabel = new Label("Stock: " + p.getStock());
        stockLabel.setStyle(p.getStock() > 0 ? "-fx-text-fill: #16a34a; -fx-font-weight: bold; -fx-font-size: 12px;"
                : "-fx-text-fill: #dc2626; -fx-font-weight: bold; -fx-font-size: 12px;");

        priceAndStockBox.getChildren().addAll(priceBox, stockLabel);

        HBox actionBox = new HBox(8);
        actionBox.setAlignment(Pos.CENTER_RIGHT);

        UserSession currentUser = SessionManager.getCurrentUser();
        boolean isOwner = (p.getEntrepreneurId() == currentUser.getId());
        boolean isAdmin = (currentUser.getRole() == UserSession.Role.ADMIN);

        Button buyBtn = new Button("Buy");
        buyBtn.getStyleClass().add("add-btn");
        if (p.getStock() <= 0) {
            buyBtn.setDisable(true);
            buyBtn.setText("Out of Stock");
        }
        FontIcon bagIcon = new FontIcon("fth-shopping-bag");
        buyBtn.setGraphic(bagIcon);
        buyBtn.setOnAction(e -> {
            e.consume();
            hideProductDetails(); // Close details if open
            handleBuyProduct(p);
        });
        buyBtn.setOnMouseClicked(javafx.scene.input.MouseEvent::consume);

        Button qrBtn = new Button();
        qrBtn.getStyleClass().addAll("card-action-btn", "qr");
        qrBtn.setGraphic(new FontIcon("fth-maximize"));
        qrBtn.setOnAction(e -> {
            e.consume();
            showQRCodeDialog(p);
        });
        qrBtn.setOnMouseClicked(javafx.scene.input.MouseEvent::consume);

        actionBox.getChildren().addAll(qrBtn);

        if (isOwner || isAdmin) {
            Button remiseBtn = new Button();
            remiseBtn.getStyleClass().addAll("card-action-btn", "remise");
            remiseBtn.setGraphic(new FontIcon("fth-tag"));
            remiseBtn.setOnAction(e -> {
                e.consume();
                handleRemiseDialog(p);
            });
            remiseBtn.setOnMouseClicked(javafx.scene.input.MouseEvent::consume);

            Button editBtn = new Button();
            editBtn.getStyleClass().addAll("card-action-btn", "edit");
            editBtn.setGraphic(new FontIcon("fth-edit-2"));
            editBtn.setOnAction(e -> {
                e.consume();
                handleEditProduct(p);
            });
            editBtn.setOnMouseClicked(javafx.scene.input.MouseEvent::consume);

            Button deleteBtn = new Button();
            deleteBtn.getStyleClass().addAll("card-action-btn", "delete");
            deleteBtn.setGraphic(new FontIcon("fth-trash-2"));
            deleteBtn.setOnAction(e -> {
                e.consume();
                handleDeleteProduct(p);
            });
            deleteBtn.setOnMouseClicked(javafx.scene.input.MouseEvent::consume);

            actionBox.getChildren().addAll(remiseBtn, editBtn, deleteBtn);
        }

        // Only show buy button if not the owner AND not an admin
        if (!isOwner && !isAdmin) {
            actionBox.getChildren().add(buyBtn);
        }

        footer.getChildren().addAll(priceAndStockBox, actionBox);
        content.getChildren().addAll(title, footer);
        card.getChildren().addAll(imgContainer, content);

        return card;
    }

    private void handleBuyProduct(Product p) {
        if (p.getStock() > 0) {
            openSaleForm(p);
        } else {
            showAlert(Alert.AlertType.WARNING, "Out of Stock", "This product is currently out of stock.");
        }
    }

    private void handleRemiseDialog(Product p) {
        if (p.getRemise() > 0) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Gestion de Remise");
            alert.setHeaderText("Remise actuelle : " + p.getRemise() + "%");
            alert.setContentText("Voulez-vous supprimer la remise ou la modifier ?");

            ButtonType btnDelete = new ButtonType("Supprimer");
            ButtonType btnEdit = new ButtonType("Modifier");
            ButtonType btnCancel = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);

            alert.getButtonTypes().setAll(btnDelete, btnEdit, btnCancel);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == btnDelete) {
                p.setRemise(0);
                try {
                    productService.update(p);
                    loadDatabaseData(); // Reload to reflect changes
                    renderProducts();
                } catch (SQLException e) {
                    showAlert(Alert.AlertType.ERROR, "Erreur BD", "Impossible de supprimer la remise.");
                }
            } else if (result.isPresent() && result.get() == btnEdit) {
                askForRemise(p);
            }
        } else {
            askForRemise(p);
        }
    }

    private void askForRemise(Product p) {
        TextInputDialog dialog = new TextInputDialog("");
        dialog.setTitle("Ajouter une Remise");
        dialog.setHeaderText("Ajouter une remise pour: " + p.getName());
        dialog.setContentText("Pourcentage de remise (%) :");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                int remiseValue = Integer.parseInt(result.get());
                if (remiseValue > 0 && remiseValue <= 100) {
                    p.setRemise(remiseValue);
                    productService.update(p);
                    // Send Email to other users
                    services.EmailService.sendDiscountNotification(p, SessionManager.getCurrentUser());
                    loadDatabaseData();
                    renderProducts();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "La remise doit être entre 1 et 100.");
                }
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Veuillez entrer un nombre valide.");
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur BD", "Impossible de sauvegarder la remise.");
            }
        }
    }

    private void showQRCodeDialog(Product p) {
        String qrData = String.format("ID: %d\nName: %s\nPrice: %.2f %s",
                p.getId(), p.getName(), p.getPrice(), p.getCurrency());

        Image qrImage = QRCodeUtils.generateQRCode(qrData, 250, 250);

        if (qrImage != null) {
            Alert alert = new Alert(Alert.AlertType.NONE); // Use NONE to avoid default icons
            alert.setTitle("QR Code");
            alert.getButtonTypes().add(ButtonType.CLOSE);

            ImageView iv = new ImageView(qrImage);
            iv.setPreserveRatio(true);

            StackPane container = new StackPane(iv);
            container.setPadding(new javafx.geometry.Insets(10));
            container.setAlignment(Pos.CENTER);

            alert.getDialogPane().setContent(container);
            alert.getDialogPane().setHeaderText(null);
            alert.getDialogPane().setGraphic(null);

            alert.showAndWait();
        }
    }

    @FXML
    private void handleImageSearch() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Sélectionner une image pour la recherche");
        fileChooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.webp"));

        File file = fileChooser.showOpenDialog(rootStack.getScene().getWindow());
        if (file != null) {
            Alert loadingAlert = new Alert(Alert.AlertType.INFORMATION,
                    "L'IA analyse l'image par rapport à votre catalogue (Recherche Sémantique)...");
            loadingAlert.setTitle("Recherche Intelligente par Image");
            loadingAlert.show();

            navyApiService.searchProductsByImage(file, allProducts)
                    .thenAccept(matchingIds -> {
                        Platform.runLater(() -> {
                            loadingAlert.setResult(ButtonType.OK);
                            loadingAlert.close();

                            System.out.println("DEBUG: AI Image Semantic Search found matching IDs -> " + matchingIds);

                            List<Product> results = allProducts.stream()
                                    .filter(p -> matchingIds.contains(p.getId()))
                                    .collect(Collectors.toList());

                            productGrid.getChildren().clear();
                            if (results.isEmpty()) {
                                showAlert(Alert.AlertType.WARNING, "Aucun résultat",
                                        "L'IA n'a trouvé aucun produit ressemblant dans le catalogue.");
                                // Fallback to reload all
                                renderProducts();
                            } else {
                                showAlert(Alert.AlertType.INFORMATION, "Résultat de recherche",
                                        "L'IA a trouvé " + matchingIds.size() + " correspondance(s) !");
                                results.forEach(p -> productGrid.getChildren().add(createProductCard(p)));
                            }
                        });
                    })
                    .exceptionally(ex -> {
                        Platform.runLater(() -> {
                            loadingAlert.setResult(ButtonType.OK);
                            loadingAlert.close();
                            showAlert(Alert.AlertType.ERROR, "Erreur IA",
                                    "L'analyse d'image a échoué : " + ex.getMessage());
                        });
                        return null;
                    });
        }
    }

    @FXML
    private void handleShowSalesMap() {
        if (allSales == null || allSales.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "Information", "Aucune vente à afficher sur la carte.");
            return;
        }

        Stage stage = new Stage();
        stage.setTitle("Carte des Ventes");
        stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

        WebView wv = new WebView();
        WebEngine engine = wv.getEngine();
        wv.setMinSize(1000, 700);

        try {
            java.nio.file.Path tempDir = java.nio.file.Files.createTempDirectory("sales_map_");
            tempDir.toFile().deleteOnExit();

            String[] resources = {
                    "sales_map.html", "leaflet.css", "leaflet.js",
                    "marker-icon.png", "marker-icon-2x.png", "marker-shadow.png"
            };

            for (String res : resources) {
                try (java.io.InputStream is = getClass().getResourceAsStream("/" + res)) {
                    if (is != null) {
                        java.nio.file.Files.copy(is, tempDir.resolve(res),
                                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }

            java.nio.file.Path htmlPath = tempDir.resolve("sales_map.html");
            URL mapUrl = htmlPath.toUri().toURL();

            // Build JSON
            List<Sale> filteredSales = allSales.stream()
                    .filter(s -> activeSaleStatus.equals("All") || s.getStatus().equalsIgnoreCase(activeSaleStatus))
                    .collect(Collectors.toList());

            org.json.JSONArray salesArray = new org.json.JSONArray();
            for (Sale s : filteredSales) {
                if (s.getShippingAddress() != null && !s.getShippingAddress().isEmpty()) {
                    org.json.JSONObject obj = new org.json.JSONObject();
                    obj.put("id", s.getId());
                    obj.put("ref", s.getReference());
                    obj.put("amount", s.getTotalAmount());
                    obj.put("currency", s.getCurrency());
                    obj.put("status", s.getStatus());
                    obj.put("address", s.getShippingAddress());
                    salesArray.put(obj);
                }
            }

            String jsonOutput = salesArray.toString().replace("'", "\\'"); // Escape quotes

            engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == Worker.State.SUCCEEDED) {
                    Platform.runLater(() -> {
                        engine.executeScript("loadSales('" + jsonOutput + "');");
                        engine.executeScript("if(typeof invalidateMap === 'function') invalidateMap();");
                    });
                }
            });

            engine.load(mapUrl.toExternalForm());

            AnchorPane root = new AnchorPane(wv);
            AnchorPane.setTopAnchor(wv, 0.0);
            AnchorPane.setBottomAnchor(wv, 0.0);
            AnchorPane.setLeftAnchor(wv, 0.0);
            AnchorPane.setRightAnchor(wv, 0.0);
            wv.prefWidthProperty().bind(root.widthProperty());
            wv.prefHeightProperty().bind(root.heightProperty());

            stage.setScene(new Scene(root, 1000, 700));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir la carte : " + e.getMessage());
        }
    }

    @FXML
    private void handleQRSearch() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Select QR Code Image");
        fileChooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));

        File file = fileChooser.showOpenDialog(rootStack.getScene().getWindow());
        if (file != null) {
            String decodedText = QRCodeUtils.decodeQRCode(file);
            if (decodedText != null) {
                System.out.println("Decoded QR: " + decodedText);
                // Simple search logic: look for the ID in the decoded text
                try {
                    String[] lines = decodedText.split("\n");
                    if (lines.length > 0 && lines[0].startsWith("ID: ")) {
                        long id = Long.parseLong(lines[0].replace("ID: ", "").trim());

                        List<Product> results = allProducts.stream()
                                .filter(prod -> prod.getId() == id)
                                .collect(Collectors.toList());

                        if (!results.isEmpty()) {
                            productGrid.getChildren().clear();
                            results.forEach(prod -> productGrid.getChildren().add(createProductCard(prod)));
                        } else {
                            showAlert(Alert.AlertType.WARNING, "Not Found",
                                    "No product matching this QR code was found.");
                        }
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Invalid QR",
                                "This QR code does not contain valid product information.");
                    }
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to process QR code content.");
                }
            } else {
                showAlert(Alert.AlertType.ERROR, "Decoding Failed", "Could not read QR code from the selected image.");
            }
        }
    }

    private void openSaleForm(Product p) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/add-sale.fxml"));
            VBox form = loader.load();

            AddSaleController controller = loader.getController();
            controller.setProduct(p);
            controller.setOnSaleCreated(v -> {
                loadDatabaseData();
                renderOrders();
                switchTab("history", navHistory);
            });
            controller.setOnCancel(() -> switchTab("products", navProducts));

            addProductPage.getChildren().setAll(form);
            switchTab("add-sale", navProducts); // Using navProducts as active but we are in form
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    private void handleEditProduct(Product p) {
        openProductForm(p);
    }

    private void handleDeleteProduct(Product p) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Delete");
        alert.setHeaderText("Delete Product");
        alert.setContentText("Are you sure you want to delete '" + p.getName() + "'?");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    productService.delete((int) p.getId());
                    loadDatabaseData();
                    renderProducts();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void showProductDetails(Product p) {
        if (detailOverlay == null)
            return;

        // Increment views
        try {
            productService.incrementViewsCount(p.getId());
            p.setViewsCount(p.getViewsCount() + 1);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        detailImage.setImage(new Image(p.getImage(), true));
        detailTitle.setText(p.getName());
        detailShortDesc.setText(""); // Removed short_desc
        detailStatus.setText(p.getStatus());
        if (p.getRemise() > 0) {
            double discountedAmount = p.getPrice() * (1 - p.getRemise() / 100.0);
            detailPrice.setText(
                    String.format("%.2f (Remise -%d%%, Ancien: %.2f)", discountedAmount, p.getRemise(), p.getPrice()));
            detailPrice.setStyle("-fx-text-fill: #e11d48; -fx-font-weight: bold;");
        } else {
            detailPrice.setText(String.valueOf(p.getPrice()));
            detailPrice.setStyle("");
        }
        detailCurrency.setText(p.getCurrency());
        detailViews.setText(String.valueOf(p.getViewsCount()));
        detailSales.setText(String.valueOf(p.getSalesCount()));

        if (p.getCreatedAt() != null) {
            detailCreated.setText(p.getCreatedAt().toLocalDateTime().toLocalDate().toString());
        }
        if (p.getUpdatedAt() != null) {
            detailUpdated.setText(p.getUpdatedAt().toLocalDateTime().toLocalDate().toString());
        } else {
            detailUpdated.setText("N/A");
        }

        detailFullDesc.getChildren().clear();
        Label fullDescLabel = new Label(p.getDescription());
        fullDescLabel.getStyleClass().add("detail-description");
        fullDescLabel.setWrapText(true);
        fullDescLabel.prefWidthProperty().bind(detailFullDesc.widthProperty().subtract(20)); // Fix overflow
        detailFullDesc.getChildren().add(fullDescLabel);

        // Add Buy Button to detailed view if not present or just handle it
        // We'll add it to the footer in FXML or here
        // For simplicity, let's ensure we have a reference to a button in FXML
        // or just create one here if needed.
        // I will add it to the FXML for better styling, but I need to set the action
        // here.

        detailOverlay.setUserData(p); // Store product in overlay for the buy button action

        detailOverlay.setVisible(true);
        detailOverlay.toFront();
    }

    @FXML
    private void handleBuyFromDetails() {
        Product p = (Product) detailOverlay.getUserData();
        if (p != null) {
            hideProductDetails();
            handleBuyProduct(p);
        }
    }

    @FXML
    private void hideProductDetails() {
        if (detailOverlay != null)
            detailOverlay.setVisible(false);
    }

    private void handleExportSale(Sale s) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save PDF Invoice");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        fileChooser.setInitialFileName("Invoice_" + s.getReference() + ".pdf");
        File file = fileChooser.showSaveDialog(rootStack.getScene().getWindow());

        if (file != null) {
            try {
                exportService.exportSaleToPdf(s, file);
                showAlert(Alert.AlertType.INFORMATION, "Export Successful",
                        "Invoice saved to:\n" + file.getAbsolutePath());
                if (java.awt.Desktop.isDesktopSupported()) {
                    new Thread(() -> {
                        try {
                            java.awt.Desktop.getDesktop().open(file);
                        } catch (java.io.IOException ex) {
                            ex.printStackTrace();
                        }
                    }).start();
                }
            } catch (Exception e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Export Failed", "Could not export PDF: " + e.getMessage());
            }
        }
    }

    private void renderOrders() {
        if (ordersContainer == null) {
            System.err.println("DEBUG: ordersContainer is NULL!");
            return;
        }
        System.out.println("DEBUG: Rendering filtered sales...");
        ordersContainer.getChildren().clear();

        UserSession currentUser = SessionManager.getCurrentUser();
        boolean isAdmin = (currentUser.getRole() == UserSession.Role.ADMIN);

        List<Sale> roleFilteredSales = allSales.stream()
                .filter(s -> isAdmin || s.getCustomerId() == currentUser.getId())
                .collect(Collectors.toList());

        List<Sale> finalFilteredSales = roleFilteredSales.stream()
                .filter(s -> activeSaleStatus.equals("All") || s.getStatus().equalsIgnoreCase(activeSaleStatus))
                .collect(Collectors.toList());

        for (Sale s : finalFilteredSales) {
            VBox card = new VBox(20);
            card.getStyleClass().add("order-card");

            // Header
            HBox header = new HBox();
            header.getStyleClass().add("order-header");

            VBox refBox = new VBox(5);
            Label ref = new Label(s.getReference());
            ref.getStyleClass().add("order-ref");
            Label date = new Label("Ordered on "
                    + (s.getCreatedAt() != null ? s.getCreatedAt().toLocalDateTime().toLocalDate() : "N/A"));
            date.getStyleClass().add("order-date");
            refBox.getChildren().addAll(ref, date);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label status = new Label(s.getStatus().toUpperCase());
            status.getStyleClass().add("status-chip");
            if (s.getStatus().equalsIgnoreCase("Completed") || s.getStatus().equalsIgnoreCase("Paid"))
                status.getStyleClass().add("status-completed");
            else if (s.getStatus().equalsIgnoreCase("Pending"))
                status.getStyleClass().add("status-pending");
            else
                status.getStyleClass().add("status-cancelled");

            header.getChildren().addAll(refBox, spacer, status);

            // Details Grid
            GridPane grid = new GridPane();
            grid.getStyleClass().add("order-grid");

            ColumnConstraints c1 = new ColumnConstraints();
            c1.setPercentWidth(33.33);
            ColumnConstraints c2 = new ColumnConstraints();
            c2.setPercentWidth(33.33);
            ColumnConstraints c3 = new ColumnConstraints();
            c3.setPercentWidth(33.33);
            grid.getColumnConstraints().addAll(c1, c2, c3);

            int r = 0;
            addDetailItem(grid, 0, r, 1, "fth-credit-card", "Payment Method", s.getPaymentMethod());
            addDetailItem(grid, 1, r, 1, "fth-info", "Payment Status", s.getPaymentStatus());
            addDetailItem(grid, 2, r++, 1, "fth-refresh-cw", "Last Updated",
                    s.getUpdatedAt() != null ? s.getUpdatedAt().toLocalDateTime().toLocalDate().toString() : "N/A");

            addDetailItem(grid, 0, r, 1, "fth-map-pin", "Shipping Address", s.getShippingAddress());
            addDetailItem(grid, 1, r++, 2, "fth-map", "Billing Address", s.getBillingAddress());

            addDetailItem(grid, 0, r++, 3, "fth-file-text", "Additional Notes",
                    (s.getNotes() != null && !s.getNotes().trim().isEmpty()) ? s.getNotes()
                            : "No additional information provided.");

            // Footer
            HBox footer = new HBox();
            footer.getStyleClass().add("order-total-box");

            HBox actionBox = new HBox(10);
            actionBox.setAlignment(Pos.CENTER_LEFT);

            Button exportBtn = new Button();
            exportBtn.getStyleClass().addAll("card-action-btn");
            exportBtn.setGraphic(new FontIcon("fth-download")); // A download icon
            exportBtn.setOnAction(e -> handleExportSale(s));
            actionBox.getChildren().add(exportBtn);

            if (isAdmin) {
                Button editBtn = new Button();
                editBtn.getStyleClass().addAll("card-action-btn", "edit");
                editBtn.setGraphic(new FontIcon("fth-edit-2"));
                editBtn.setOnAction(e -> handleEditSale(s));

                Button deleteBtn = new Button();
                deleteBtn.getStyleClass().addAll("card-action-btn", "delete");
                deleteBtn.setGraphic(new FontIcon("fth-trash-2"));
                deleteBtn.setOnAction(e -> handleDeleteSale(s));

                actionBox.getChildren().addAll(editBtn, deleteBtn);
            }

            Region fSpacer = new Region();
            HBox.setHgrow(fSpacer, Priority.ALWAYS);

            Label totalLabel = new Label("Total Paid: ");
            totalLabel.getStyleClass().add("order-total-label");
            Label totalAmount = new Label((s.getCurrency() != null ? s.getCurrency() : "USD") + " "
                    + String.format("%.2f", s.getTotalAmount()));
            totalAmount.getStyleClass().add("order-total-amount");

            HBox payBox = new HBox(10);
            payBox.setAlignment(Pos.CENTER_RIGHT);
            payBox.getChildren().addAll(totalLabel, totalAmount);

            if (!isAdmin && !s.getStatus().equalsIgnoreCase("Paid") && !s.getStatus().equalsIgnoreCase("Completed")) {
                Button payBtn = new Button("Pay");
                payBtn.getStyleClass().add("add-btn"); // using the same template style
                payBtn.setGraphic(new FontIcon("fth-credit-card"));
                payBtn.setOnAction(e -> {
                    try {
                        // --- Stripe API Integration ---
                        String secretKey = "sk_test_51T5BAlB0ojdtmLx7CEyWmjYOrmyQG6RuoSOovrjpDBM5UflCjV2M9AxP3YsTCiKcJwK2nRhSui3eGt0DsDEFEb1q00ztnxrFpe";
                        Stripe.apiKey = secretKey;

                        // Stripe amount is in cents, so multiply by 100
                        long amountInCents = Math.round(s.getTotalAmount() * 100);

                        // Note: If TND is not supported on your Stripe account, using "usd" for
                        // testing.
                        String currency = s.getCurrency().toLowerCase();
                        if (currency.equals("tnd"))
                            currency = "usd";

                        SessionCreateParams params = SessionCreateParams.builder()
                                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                                .setMode(SessionCreateParams.Mode.PAYMENT)
                                .setSuccessUrl("https://example.com/success")
                                .setCancelUrl("https://example.com/cancel")
                                .addLineItem(
                                        SessionCreateParams.LineItem.builder()
                                                .setQuantity(1L)
                                                .setPriceData(
                                                        SessionCreateParams.LineItem.PriceData.builder()
                                                                .setCurrency(currency)
                                                                .setUnitAmount(amountInCents)
                                                                .setProductData(
                                                                        SessionCreateParams.LineItem.PriceData.ProductData
                                                                                .builder()
                                                                                .setName("Payment for Order "
                                                                                        + s.getReference())
                                                                                .build())
                                                                .build())
                                                .build())
                                .build();

                        Session session = Session.create(params);
                        String paymentUrl = session.getUrl();

                        System.out.println("DEBUG Stripe Session URL: " + paymentUrl);

                        // Open payment page in embedded WebView
                        openPaymentWebView(paymentUrl, s);

                    } catch (StripeException ex) {
                        ex.printStackTrace();
                        showAlert(Alert.AlertType.ERROR, "Payment Error",
                                "Stripe initialization failed: " + ex.getMessage());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        showAlert(Alert.AlertType.ERROR, "System Error", "An unexpected error occurred.");
                    }
                });
                payBox.getChildren().add(payBtn);
            }

            footer.getChildren().addAll(actionBox, fSpacer, payBox);

            card.getChildren().addAll(header, new Separator(), grid, new Separator(), footer);
            ordersContainer.getChildren().add(card);
        }
    }

    private void addDetailItem(GridPane grid, int col, int row, int colSpan, String iconLiteral, String labelText,
            String valueText) {
        HBox box = new HBox(12);
        box.getStyleClass().add("grid-item-box");

        FontIcon icon = new FontIcon(iconLiteral);
        icon.getStyleClass().add("grid-icon");

        VBox texts = new VBox(2);
        Label label = new Label(labelText);
        label.getStyleClass().add("grid-label");
        Label value = new Label(valueText != null ? valueText : "N/A");
        value.getStyleClass().add("grid-value");
        value.setWrapText(true);
        value.setMaxWidth(Double.MAX_VALUE);

        texts.getChildren().addAll(label, value);
        box.getChildren().addAll(icon, texts);

        grid.add(box, col, row, colSpan, 1);
        GridPane.setHgrow(box, Priority.ALWAYS);
    }

    /**
     * Opens the Paymee payment gateway in an embedded WebView dialog.
     * Automatically detects payment completion by watching for the return_url
     * redirect.
     */
    private void openPaymentWebView(String paymentUrl, Sale sale) {
        Stage payStage = new Stage();
        payStage.setTitle("💳  Paiement par Carte — Paymee");
        payStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        payStage.setResizable(true);

        // --- WebView setup ---
        WebView webView = new WebView();
        WebEngine engine = webView.getEngine();
        webView.setPrefSize(900, 650);

        // Loading indicator
        ProgressBar progressBar = new ProgressBar();
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.progressProperty().bind(engine.getLoadWorker().progressProperty());
        progressBar.visibleProperty().bind(
                engine.getLoadWorker().stateProperty().isEqualTo(
                        Worker.State.RUNNING));

        // Status label
        Label statusLabel = new Label("Chargement de la page de paiement...");
        statusLabel.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12px; -fx-padding: 4 8;");
        statusLabel.textProperty().bind(engine.locationProperty());

        // Watch for redirect to Stripe success/cancel URLs
        engine.locationProperty().addListener((obs, oldUrl, newUrl) -> {
            if (newUrl != null && (newUrl.contains("example.com/success") || newUrl.contains("example.com/cancel"))) {
                // Payment completed or cancelled
                payStage.close();

                if (newUrl.contains("success")) {
                    Platform.runLater(() -> {
                        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                        confirm.setTitle("Paiement Stripe");
                        confirm.setHeaderText("Paiement effectué ?");
                        confirm.setContentText("Avez-vous complété le paiement par carte avec succès ?");
                        confirm.getButtonTypes().setAll(
                                new ButtonType("Oui, marquer comme Payé", ButtonBar.ButtonData.YES),
                                new ButtonType("Non / Annulé", ButtonBar.ButtonData.NO));

                        confirm.showAndWait().ifPresent(btn -> {
                            if (btn.getButtonData() == ButtonBar.ButtonData.YES) {
                                try {
                                    sale.setStatus("Paid");
                                    sale.setPaymentStatus("Paid");
                                    saleService.update(sale);
                                    loadDatabaseData();
                                    renderOrders();
                                    showAlert(Alert.AlertType.INFORMATION, "Paiement confirmé",
                                            "La vente a été marquée comme payée avec Stripe.");
                                } catch (SQLException ex) {
                                    ex.printStackTrace();
                                    showAlert(Alert.AlertType.ERROR, "Erreur",
                                            "Impossible de mettre à jour le statut : " + ex.getMessage());
                                }
                            }
                        });
                    });
                }
            }
        });

        // Top bar with back/close button
        Button closeBtn = new Button("✕  Annuler le paiement");
        closeBtn.setStyle(
                "-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold;" +
                        "-fx-background-radius: 6; -fx-padding: 6 16; -fx-cursor: hand;");
        closeBtn.setOnAction(ev -> payStage.close());

        HBox topBar = new HBox(10, closeBtn);
        topBar.setStyle("-fx-background-color: #1e293b; -fx-padding: 10 16;");
        topBar.setAlignment(Pos.CENTER_LEFT);

        VBox root = new VBox(topBar, progressBar, webView);
        VBox.setVgrow(webView, Priority.ALWAYS);

        javafx.scene.Scene scene = new javafx.scene.Scene(root, 920, 700);
        payStage.setScene(scene);

        // Load the Paymee gateway page
        engine.load(paymentUrl);
        payStage.show();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.show();
    }

    @FXML
    private void handleCartClick() {
        switchTab("cart", navCart);
    }

    private void setupWindowDragging() {
        if (titleBar == null)
            return;
        titleBar.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        titleBar.setOnMouseDragged(event -> {
            Stage stage = (Stage) titleBar.getScene().getWindow();
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });
    }

    @FXML
    private void handleWindowClose() {
        System.exit(0);
    }

    @FXML
    private void handleWindowMinimize() {
        Stage stage = (Stage) titleBar.getScene().getWindow();
        stage.setIconified(true);
    }

    @FXML
    private void handleWindowMaximize() {
        Stage stage = (Stage) titleBar.getScene().getWindow();
        if (stage.isMaximized()) {
            stage.setMaximized(false);
        } else {
            stage.setMaximized(true);
        }
    }
}
