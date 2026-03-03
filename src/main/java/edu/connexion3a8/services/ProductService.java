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

    /**
     * Create a new product (Admin only)
     */
    public int create(Product product, User currentUser) throws SQLException {
        if (!isAdmin(currentUser)) {
            throw new SecurityException("Only administrators can create products");
        }

        String sql = "INSERT INTO product (name, description, price, currency, is_digital, download_url, " +
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
     * Update an existing product (Admin only)
     */
    public void update(Product product, User currentUser) throws SQLException {
        if (!isAdmin(currentUser)) {
            throw new SecurityException("Only administrators can update products");
        }

        String sql = "UPDATE product SET name = ?, description = ?, price = ?, currency = ?, is_digital = ?, " +
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
     * Delete a product (Admin only)
     */
    public void delete(long id, User currentUser) throws SQLException {
        if (!isAdmin(currentUser)) {
            throw new SecurityException("Only administrators can delete products");
        }

        String sql = "DELETE FROM product WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * Get all products (accessible to all users)
     */
    public List<Product> getAllProducts() throws SQLException {
        String sql = "SELECT * FROM product ORDER BY created_at DESC";
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
     * Get published products only (for public viewing)
     */
    public List<Product> getPublishedProducts() throws SQLException {
        String sql = "SELECT * FROM product WHERE status = 'published' ORDER BY created_at DESC";
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
        String sql = "SELECT * FROM product WHERE id = ?";
        
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
        String sql = "SELECT * FROM product WHERE (name LIKE ? OR category LIKE ? OR description LIKE ?) " +
                     "AND status = 'published' ORDER BY created_at DESC";
        List<Product> products = new ArrayList<>();
        
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            String searchPattern = "%" + keyword + "%";
            ps.setString(1, searchPattern);
            ps.setString(2, searchPattern);
            ps.setString(3, searchPattern);
            
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
                    rs.getInt("total_sales")
                );
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

        public int getTotal() { return total; }
        public int getPublished() { return published; }
        public int getDraft() { return draft; }
        public int getTotalViews() { return totalViews; }
        public int getTotalSales() { return totalSales; }
    }
}
