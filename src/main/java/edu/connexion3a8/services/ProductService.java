package edu.connexion3a8.services;

import edu.connexion3a8.entities.Product;
import edu.connexion3a8.entities.User;
import edu.connexion3a8.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing products with role-based access control
 * Only admins can create/update/delete products
 */
public class ProductService {

    /**
     * Get database connection
     */
    private Connection getConnection() throws SQLException {
        return MyConnection.getInstance().getCnx();
    }

    private String detectTableName() throws SQLException {
        String tableName = "`product`";
        try (Connection conn = getConnection()) {
            ResultSet trs = conn.getMetaData().getTables(null, null, "%", new String[] { "TABLE" });
            boolean productExists = false;
            boolean produitExists = false;
            while (trs.next()) {
                String t = trs.getString("TABLE_NAME");
                if ("product".equalsIgnoreCase(t))
                    productExists = true;
                if ("produit".equalsIgnoreCase(t))
                    produitExists = true;
            }
            if (!productExists && produitExists) {
                tableName = "`produit`";
            }
            trs.close();
        }
        return tableName;
    }

    /**
     * Create a new product
     */
    public int create(Product product, User user) throws SQLException, SecurityException {
        if (user == null
                || (!"admin".equalsIgnoreCase(user.getRole()) && !"entrepreneur".equalsIgnoreCase(user.getRole())
                        && !"innovator".equalsIgnoreCase(user.getRole()))) {
            throw new SecurityException("Only admins, entrepreneurs, and innovators can create products.");
        }
        product.setEntrepreneurId(user.getId());
        String tableName = detectTableName();
        String sql = "INSERT INTO " + tableName + " (name, description, price, currency, is_digital, download_url, " +
                "entrepreneur_id, category, status, stock, remise) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, product.getName());
            ps.setString(2, product.getDescription());
            ps.setDouble(3, product.getPrice());
            ps.setString(4, product.getCurrency());
            ps.setBoolean(5, product.isDigital());
            ps.setString(6, product.getDownloadUrl());
            ps.setString(7, product.getEntrepreneurId());
            ps.setString(8, product.getCategory());
            ps.setString(9, product.getStatus());
            ps.setInt(10, product.getStock());
            ps.setInt(11, product.getRemise());

            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return (int) rs.getLong(1);
            }
            return -1;
        }
    }

    /**
     * Update an existing product
     */
    public void update(Product product, User user) throws SQLException, SecurityException {
        if (!isAdmin(user) && !isOwner(product, user)) {
            throw new SecurityException("Permission denied. You must be an admin or the product owner.");
        }

        String tableName = detectTableName();
        String sql = "UPDATE " + tableName + " SET name = ?, description = ?, price = ?, currency = ?, is_digital = ?, "
                +
                "download_url = ?, entrepreneur_id = ?, category = ?, status = ?, stock = ?, remise = ?, " +
                "updated_at = CURRENT_TIMESTAMP WHERE id = ?";

        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, product.getName());
            ps.setString(2, product.getDescription());
            ps.setDouble(3, product.getPrice());
            ps.setString(4, product.getCurrency());
            ps.setBoolean(5, product.isDigital());
            ps.setString(6, product.getDownloadUrl());
            ps.setString(7, product.getEntrepreneurId());
            ps.setString(8, product.getCategory());
            ps.setString(9, product.getStatus());
            ps.setInt(10, product.getStock());
            ps.setInt(11, product.getRemise());
            ps.setLong(12, product.getId());

            ps.executeUpdate();
        }
    }

    /**
     * Delete a product
     */
    public void delete(long id, User user) throws SQLException, SecurityException {
        Product p = getProductById(id);
        if (p == null)
            return;

        if (!isAdmin(user) && !isOwner(p, user)) {
            throw new SecurityException("Permission denied. You must be an admin or the product owner.");
        }

        String tableName = detectTableName();
        String sql = "DELETE FROM " + tableName + " WHERE id = ?";
        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * Decrement product stock and increment sales count (System use)
     * No role-based checks because any user can buy a product
     */
    public void decrementStock(long productId) throws SQLException {
        String tableName = detectTableName();
        String sql = "UPDATE " + tableName
                + " SET stock = GREATEST(0, stock - 1), sales_count = sales_count + 1 WHERE id = ?";
        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, productId);
            ps.executeUpdate();
        }
    }

    /**
     * Get all products (accessible to all users)
     */
    public List<Product> getAllProducts() throws SQLException {
        List<Product> products = new ArrayList<>();
        String tableName = "`product`"; // Added backticks for safety

        // Debug and find the correct table
        try (Connection conn = getConnection()) {
            ResultSet trs = conn.getMetaData().getTables(null, null, "%", new String[] { "TABLE" });
            System.out.println("--- DB Check ---");
            boolean productExists = false;
            boolean produitExists = false;
            while (trs.next()) {
                String t = trs.getString("TABLE_NAME");
                System.out.println("Found table: " + t);
                if ("product".equalsIgnoreCase(t))
                    productExists = true;
                if ("produit".equalsIgnoreCase(t))
                    produitExists = true;
            }
            if (!productExists && produitExists) {
                tableName = "`produit`";
                System.out.println("Using alternative table: produit");
            }

            // Inspect columns of selected table
            System.out.println("Inspecting columns of " + tableName);
            try (ResultSet crs = conn.getMetaData().getColumns(null, null, tableName.replace("`", ""), "%")) {
                while (crs.next()) {
                    System.out.println(
                            " - Column: " + crs.getString("COLUMN_NAME") + " (" + crs.getString("TYPE_NAME") + ")");
                }
            }
            trs.close();
        }

        String sql = "SELECT * FROM " + tableName + " ORDER BY id DESC"; // Use id for safety
        try (Connection conn = getConnection();
                Statement statement = conn.createStatement();
                ResultSet rs = statement.executeQuery(sql)) {

            while (rs.next()) {
                products.add(mapResultSetToProduct(rs));
            }
        }
        System.out.println("Loaded " + products.size() + " products from " + tableName);
        return products;
    }

    /**
     * Get published products only (for public viewing)
     */
    public List<Product> getPublishedProducts() throws SQLException {
        String tableName = detectTableName();
        String sql = "SELECT * FROM " + tableName + " WHERE status = 'published' ORDER BY created_at DESC";
        List<Product> products = new ArrayList<>();

        try (Connection conn = getConnection();
                Statement statement = conn.createStatement();
                ResultSet rs = statement.executeQuery(sql)) {

            while (rs.next()) {
                products.add(mapResultSetToProduct(rs));
            }
        }
        return products;
    }

    /**
     * Get product by ID
     */
    public Product getProductById(long id) throws SQLException {
        String tableName = detectTableName();
        String sql = "SELECT * FROM " + tableName + " WHERE id = ?";

        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapResultSetToProduct(rs);
            }
        }
        return null;
    }

    /**
     * Search products by name or category
     */
    public List<Product> searchProducts(String keyword) throws SQLException {
        String tableName = detectTableName();
        String sql = "SELECT * FROM " + tableName + " WHERE (name LIKE ? OR category LIKE ? OR description LIKE ?) " +
                "AND status = 'published' ORDER BY id DESC";
        List<Product> products = new ArrayList<>();

        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            String pattern = "%" + keyword + "%";
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                products.add(mapResultSetToProduct(rs));
            }
        }
        return products;
    }

    /**
     * Increment product views count
     */
    public void incrementViewsCount(long id) throws SQLException {
        String sql = "UPDATE product SET views_count = views_count + 1 WHERE id = ?";
        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * Increment product sales count
     */
    public void incrementSalesCount(long id) throws SQLException {
        String sql = "UPDATE product SET sales_count = sales_count + 1 WHERE id = ?";
        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * Get product statistics
     */
    public ProductStats getProductStats() throws SQLException {
        String sql = "SELECT COUNT(*) as total, " +
                "SUM(CASE WHEN status = 'published' THEN 1 ELSE 0 END) as published, " +
                "SUM(CASE WHEN status = 'draft' THEN 1 ELSE 0 END) as draft, " +
                "SUM(views_count) as total_views, " +
                "SUM(sales_count) as total_sales " +
                "FROM product";

        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return new ProductStats(
                        rs.getInt("total"),
                        rs.getInt("published"),
                        rs.getInt("draft"),
                        rs.getInt("total_views"),
                        rs.getInt("total_sales"));
            }
        }
        return new ProductStats(0, 0, 0, 0, 0);
    }

    // Helper methods
    private Product mapResultSetToProduct(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setId(rs.getLong("id"));
        p.setName(rs.getString("name"));
        p.setDescription(rs.getString("description"));
        p.setPrice(rs.getDouble("price"));
        p.setCurrency(rs.getString("currency"));
        p.setDigital(rs.getBoolean("is_digital"));
        p.setDownloadUrl(rs.getString("download_url"));
        p.setEntrepreneurId(rs.getString("entrepreneur_id"));
        p.setCategory(rs.getString("category"));
        p.setStatus(rs.getString("status"));
        p.setViewsCount(rs.getInt("views_count"));
        p.setSalesCount(rs.getInt("sales_count"));
        p.setStock(rs.getInt("stock"));
        p.setRemise(rs.getInt("remise"));
        p.setCreatedAt(rs.getTimestamp("created_at"));
        p.setUpdatedAt(rs.getTimestamp("updated_at"));
        return p;
    }

    private boolean isAdmin(User user) {
        return user != null && "admin".equalsIgnoreCase(user.getRole());
    }

    private boolean isOwner(Product product, User user) {
        return user != null && product != null && user.getId().equals(product.getEntrepreneurId());
    }

    /**
     * Inner class for product statistics
     */
    public static class ProductStats {
        private final int total;
        private final int published;
        private final int draft;
        private final int totalViews;
        private final int totalSales;

        public ProductStats(int total, int published, int draft, int totalViews, int totalSales) {
            this.total = total;
            this.published = published;
            this.draft = draft;
            this.totalViews = totalViews;
            this.totalSales = totalSales;
        }

        public int getTotal() {
            return total;
        }

        public int getPublished() {
            return published;
        }

        public int getDraft() {
            return draft;
        }

        public int getTotalViews() {
            return totalViews;
        }

        public int getTotalSales() {
            return totalSales;
        }
    }
}
