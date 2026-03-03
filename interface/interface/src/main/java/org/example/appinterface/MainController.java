package org.example.appinterface;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.*;
import javafx.scene.text.TextFlow;
import javafx.scene.paint.Color;
import javafx.scene.effect.DropShadow;
import javafx.geometry.Pos;
import org.example.appinterface.model.Product;
import org.example.appinterface.model.Order;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.sql.Timestamp;
import java.util.*;

public class MainController implements Initializable {

    @FXML
    private ScrollPane productsPage;
    @FXML
    private FlowPane productGrid;
    @FXML
    private VBox cartPage, cartItemsContainer;
    @FXML
    private HBox categoryContainer;
    @FXML
    private Label cartCountLabel;

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
    private Label detailProjectId, detailEntrepreneurId, detailViews, detailSales, detailCreated;
    @FXML
    private TextFlow detailFullDesc;
    @FXML
    private Button buyButton;

    // Order History Page
    @FXML
    private VBox orderHistoryPage, ordersContainer;

    private List<Product> allProducts = new ArrayList<>();
    private List<Order> allOrders = new ArrayList<>();
    private String activeCategory = "All";
    private String activeTab = "products";

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadMockData();
        setupCategories();
        setupSidebar();
        renderProducts();

        // Ensure overlay is hidden initially
        if (detailOverlay != null)
            detailOverlay.setVisible(false);
    }

    private void loadMockData() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        allProducts = Arrays.asList(
                new Product(1, "Premium Template", 49.0,
                        "https://images.unsplash.com/photo-1460925895917-afdab827c52f?w=500", "Software",
                        "purple-600",
                        "A fully responsive SaaS landing page template built with React and Tailwind CSS.",
                        "Modern SaaS Landing Page", "USD", true, "https://example.com/dl", 101, 201, 1, "Active", 1250,
                        45, now, now),
                new Product(2, "AI Dashboard", 79.0, "https://images.unsplash.com/photo-1551288049-bebda4e38f71?w=500",
                        "Analytics",
                        "blue-600",
                        "Complete analytics dashboard with AI-driven insights and real-time data visualization.",
                        "Next-gen Analytics UI", "USD", true, "https://example.com/dl", 102, 202, 2, "Featured", 3400,
                        120, now, now),
                new Product(3, "Fitness App UI", 29.0,
                        "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?w=500", "Mobile",
                        "violet-600",
                        "Premium mobile app UI kit for health and fitness enthusiasts. Includes 40+ screens.",
                        "Health & Fitness UI Kit", "USD", true, "https://example.com/dl", 103, 203, 3, "New", 890, 12,
                        now, now),
                new Product(4, "E-commerce Kit", 59.0,
                        "https://images.unsplash.com/photo-1472851294608-062f824d29cc?w=500", "Web",
                        "orange-600",
                        "Scalable e-commerce solution with integrated payment gateways and inventory management.",
                        "Full-stack E-com Kit", "USD", false, null, 104, 204, 4, "Active", 2100, 67, now, now),
                new Product(5, "Creative Portfolio", 39.0,
                        "https://images.unsplash.com/photo-1507238691740-187a5b1d37b8?w=500", "Design",
                        "green-600",
                        "Minimalist portfolio template for designers and photographers to showcase their work.",
                        "Elegant Portfolio Theme", "USD", true, "https://example.com/dl", 105, 205, 5, "Rising", 1560,
                        31, now, now),
                new Product(6, "Social Media Manager", 99.0,
                        "https://images.unsplash.com/photo-1611162617474-5b21e879e113?w=500", "Marketing",
                        "pink-600", "Automated social media scheduling and engagement tool with built-in CRM features.",
                        "All-in-one Marketing Tool", "USD", true, "https://example.com/dl", 106, 206, 6, "Stable", 5600,
                        245, now, now));

        allOrders = Arrays.asList(
                new Order("REF-2024-001", 1001, 129.0, "USD", "Completed", "Credit Card", "Paid", "TXN-78901",
                        "123 Tech Lane, Silicon Valley, CA", "123 Tech Lane, Silicon Valley, CA", "Deliver after 5 PM",
                        now, now),
                new Order("REF-2024-002", 1001, 49.0, "USD", "Pending", "PayPal", "Awaiting Payment", "TXN-78902",
                        "123 Tech Lane, Silicon Valley, CA", "123 Tech Lane, Silicon Valley, CA",
                        "Fragile handle with care", now, now),
                new Order("REF-2024-003", 1001, 299.0, "USD", "Cancelled", "Bank Transfer", "Refunded", "TXN-78903",
                        "123 Tech Lane, Silicon Valley, CA", "123 Tech Lane, Silicon Valley, CA",
                        "Customer requested cancellation", now, now),
                new Order("REF-2024-004", 1001, 850.0, "USD", "Completed", "Crypto", "Paid", "TXN-78904",
                        "456 Blockchain Ave, Meta City, NY", "456 Blockchain Ave, Meta City, NY",
                        "Fast delivery requested", now, now),
                new Order("REF-2024-005", 1001, 15.0, "USD", "Pending", "Credit Card", "Verifying", "TXN-78905",
                        "789 Standard Rd, Regular Town, TX", "789 Standard Rd, Regular Town, TX",
                        null, now, now),
                new Order("REF-2024-006", 1001, 1200.0, "USD", "Completed", "Credit Card", "Paid", "TXN-78906",
                        "101 Elite Plaza, Luxury Hills, CA", "101 Elite Plaza, Luxury Hills, CA",
                        "Include gift wrapping", now, now),
                new Order("REF-2024-007", 1001, 25.99, "EUR", "Pending", "Stripe", "Auth Successful", "TXN-78907",
                        "Rue de la Paix, Paris, FR", "Rue de la Paix, Paris, FR",
                        "International shipping", now, now),
                new Order("REF-2024-008", 1001, 0.0, "USD", "Completed", "Coupon", "Free", "TXN-FREE-99",
                        "Digital Delivery", "N/A",
                        "Free trial conversion", now, now));
    }

    private void setupCategories() {
        String[] cats = { "All", "Software", "Analytics", "Web", "Design", "Mobile" };
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

    private void setupSidebar() {
        navProducts.setOnAction(e -> switchTab("products", navProducts));
        navHistory.setOnAction(e -> switchTab("history", navHistory));
        navCart.setOnAction(e -> switchTab("cart", navCart));
    }

    @FXML
    private void handleHistoryClick() {
        switchTab("history", navHistory);
    }

    private void switchTab(String tab, Button activeBtn) {
        activeTab = tab;

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

        // Show selected
        if (tab.equals("products") && productsPage != null) {
            productsPage.setVisible(true);
            productsPage.setManaged(true);
        } else if (tab.equals("cart") && cartPage != null) {
            cartPage.setVisible(true);
            cartPage.setManaged(true);
        } else if (tab.equals("history") && orderHistoryPage != null) {
            orderHistoryPage.setVisible(true);
            orderHistoryPage.setManaged(true);
            renderOrders();
        }
    }

    private void renderProducts() {
        productGrid.getChildren().clear();
        allProducts.stream()
                .filter(p -> activeCategory.equals("All") || p.getCategory().equals(activeCategory))
                .forEach(p -> productGrid.getChildren().add(createProductCard(p)));
    }

    private VBox createProductCard(Product p) {
        VBox card = new VBox();
        card.getStyleClass().add("product-card");
        card.setCursor(javafx.scene.Cursor.HAND);
        card.setOnMouseClicked(e -> showProductDetails(p));

        StackPane imgContainer = new StackPane();
        imgContainer.getStyleClass().add("card-img-container");
        imgContainer.setStyle("-fx-background-color: " + mapTailwindGradient(p.getGradient()) + ";");

        ImageView iv = new ImageView(new Image(p.getImage(), true));
        iv.setFitWidth(180);
        iv.setFitHeight(180);
        iv.setPreserveRatio(true);
        iv.setEffect(new DropShadow(15, Color.rgb(0, 0, 0, 0.2)));

        Label badge = new Label(p.getCategory());
        badge.getStyleClass().add("category-badge");
        StackPane.setAlignment(badge, Pos.TOP_RIGHT);

        imgContainer.getChildren().addAll(iv, badge);

        VBox content = new VBox(15);
        content.getStyleClass().add("card-content");

        Label title = new Label(p.getTitle());
        title.getStyleClass().add("card-title");

        Label desc = new Label(p.getShortDescription());
        desc.getStyleClass().add("card-desc");
        desc.setWrapText(true);

        HBox footer = new HBox();
        footer.getStyleClass().add("card-footer");
        footer.setAlignment(Pos.CENTER_LEFT);

        Label price = new Label("$" + p.getPrice());
        price.getStyleClass().add("card-price");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addBtn = new Button("Add");
        addBtn.getStyleClass().add("add-btn");
        FontIcon cartIcon = new FontIcon("fth-shopping-cart");
        addBtn.setGraphic(cartIcon);
        addBtn.setOnAction(e -> {
            e.consume();
            addToCart(p);
        });

        footer.getChildren().addAll(price, spacer, addBtn);
        content.getChildren().addAll(title, desc, footer);
        card.getChildren().addAll(imgContainer, content);

        return card;
    }

    private void showProductDetails(Product p) {
        if (detailOverlay == null)
            return;

        detailImage.setImage(new Image(p.getImage(), true));
        detailTitle.setText(p.getTitle());
        detailShortDesc.setText(p.getShortDescription());
        detailStatus.setText(p.getStatus());
        detailPrice.setText(String.valueOf(p.getPrice()));
        detailCurrency.setText(p.getCurrency());
        detailProjectId.setText("#" + p.getProjectId());
        detailEntrepreneurId.setText("#" + p.getEntrepreneurId());
        detailViews.setText(String.valueOf(p.getViewsCount()));
        detailSales.setText(String.valueOf(p.getSalesCount()));

        if (p.getCreatedAt() != null) {
            detailCreated.setText(p.getCreatedAt().toLocalDateTime().toLocalDate().toString());
        }

        detailFullDesc.getChildren().clear();
        Label fullDescLabel = new Label(p.getDescription());
        fullDescLabel.getStyleClass().add("detail-description");
        fullDescLabel.setWrapText(true);
        detailFullDesc.getChildren().add(fullDescLabel);

        detailOverlay.setVisible(true);
        detailOverlay.toFront();
    }

    @FXML
    private void hideProductDetails() {
        if (detailOverlay != null)
            detailOverlay.setVisible(false);
    }

    private void renderOrders() {
        if (ordersContainer == null) {
            System.err.println("DEBUG: ordersContainer is NULL!");
            return;
        }
        System.out.println("DEBUG: Rendering " + allOrders.size() + " orders...");
        ordersContainer.getChildren().clear();

        for (Order o : allOrders) {
            VBox card = new VBox(20);
            card.getStyleClass().add("order-card");

            // Header
            HBox header = new HBox();
            header.getStyleClass().add("order-header");

            VBox refBox = new VBox(5);
            Label ref = new Label(o.getReference());
            ref.getStyleClass().add("order-ref");
            Label date = new Label("Ordered on " + o.getCreatedAt().toLocalDateTime().toLocalDate());
            date.getStyleClass().add("order-date");
            refBox.getChildren().addAll(ref, date);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label status = new Label(o.getStatus().toUpperCase());
            status.getStyleClass().add("status-chip");
            if (o.getStatus().equalsIgnoreCase("Completed"))
                status.getStyleClass().add("status-completed");
            else if (o.getStatus().equalsIgnoreCase("Pending"))
                status.getStyleClass().add("status-pending");
            else
                status.getStyleClass().add("status-cancelled");

            header.getChildren().addAll(refBox, spacer, status);

            // Details Grid
            GridPane grid = new GridPane();
            grid.getStyleClass().add("order-grid");

            addGridRow(grid, 0, "Payment Method", o.getPaymentMethod());
            addGridRow(grid, 1, "Transaction ID", o.getTransactionId());
            addGridRow(grid, 2, "Shipping Address", o.getShippingAddress());
            addGridRow(grid, 3, "Payment Status", o.getPaymentStatus());

            // Footer
            HBox footer = new HBox();
            footer.getStyleClass().add("order-total-box");
            Label totalLabel = new Label("Total Amount: ");
            totalLabel.getStyleClass().add("order-total-label");
            Label totalAmount = new Label(o.getCurrency() + " " + String.format("%.2f", o.getTotalAmount()));
            totalAmount.getStyleClass().add("order-total-amount");
            footer.getChildren().addAll(totalLabel, totalAmount);

            card.getChildren().addAll(header, new Separator(), grid, new Separator(), footer);
            ordersContainer.getChildren().add(card);
        }
    }

    private void addGridRow(GridPane grid, int row, String labelText, String valueText) {
        Label label = new Label(labelText);
        label.getStyleClass().add("grid-label");
        Label value = new Label(valueText);
        value.getStyleClass().add("grid-value");
        grid.add(label, 0, row);
        grid.add(value, 1, row);
    }

    private String mapTailwindGradient(String tw) {
        if (tw.contains("blue-600") || tw.contains("purple-600"))
            return "linear-gradient(to bottom right, #f0f4f8, #d9e2ec)"; // Slate variations
        if (tw.contains("orange-600") || tw.contains("yellow-600"))
            return "linear-gradient(to bottom right, #fdfaf3, #f5ead3)"; // Gold variations
        if (tw.contains("pink-600") || tw.contains("red-600"))
            return "linear-gradient(to bottom right, #faf5f6, #f2e3e5)"; // Rose variations
        return "linear-gradient(to bottom right, #ffffff, #f7f0f5)";
    }

    private void addToCart(Product p) {
        int currentCount = Integer.parseInt(cartCountLabel.getText());
        cartCountLabel.setText(String.valueOf(currentCount + 1));
    }

    @FXML
    private void handleCartClick() {
        switchTab("cart", navCart);
    }
}
