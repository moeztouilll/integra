package services;

import models.Product;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductService implements IService<Product> {

    private Connection connection;

    public ProductService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public int create(Product product) throws SQLException {
        String sql = "INSERT INTO product (name, description, price, currency, is_digital, download_url, entrepreneur_id, category, status, stock, remise) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, product.getName());
        ps.setString(2, product.getDescription());
        ps.setDouble(3, product.getPrice());
        ps.setString(4, product.getCurrency());
        ps.setBoolean(5, product.isDigital());
        ps.setString(6, product.getDownloadUrl());
        ps.setLong(7, product.getEntrepreneurId());
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

    @Override
    public void update(Product product) throws SQLException {
        String sql = "UPDATE product SET name = ?, description = ?, price = ?, currency = ?, is_digital = ?, download_url = ?, entrepreneur_id = ?, category = ?, status = ?, stock = ?, remise = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, product.getName());
        ps.setString(2, product.getDescription());
        ps.setDouble(3, product.getPrice());
        ps.setString(4, product.getCurrency());
        ps.setBoolean(5, product.isDigital());
        ps.setString(6, product.getDownloadUrl());
        ps.setLong(7, product.getEntrepreneurId());
        ps.setString(8, product.getCategory());
        ps.setString(9, product.getStatus());
        ps.setInt(10, product.getStock());
        ps.setInt(11, product.getRemise());
        ps.setLong(12, product.getId());

        ps.executeUpdate();
    }

    public void incrementViewsCount(long id) throws SQLException {
        String sql = "UPDATE product SET views_count = views_count + 1 WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setLong(1, id);
        ps.executeUpdate();
    }

    public void incrementSalesCount(long id) throws SQLException {
        String sql = "UPDATE product SET sales_count = sales_count + 1 WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setLong(1, id);
        ps.executeUpdate();
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM product WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setLong(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<Product> read() throws SQLException {
        String sql = "SELECT * FROM product";
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(sql);
        List<Product> products = new ArrayList<>();
        while (rs.next()) {
            Product p = new Product();
            p.setId(rs.getLong("id"));
            p.setName(rs.getString("name"));
            p.setDescription(rs.getString("description"));
            p.setPrice(rs.getDouble("price"));
            p.setCurrency(rs.getString("currency"));
            p.setDigital(rs.getBoolean("is_digital"));
            p.setDownloadUrl(rs.getString("download_url"));
            p.setEntrepreneurId(rs.getLong("entrepreneur_id"));
            p.setCategory(rs.getString("category"));
            p.setStatus(rs.getString("status"));
            p.setViewsCount(rs.getInt("views_count"));
            p.setSalesCount(rs.getInt("sales_count"));
            try {
                p.setStock(rs.getInt("stock"));
            } catch (SQLException ignored) {
            }
            try {
                p.setRemise(rs.getInt("remise"));
            } catch (SQLException ignored) {
            }
            p.setCreatedAt(rs.getTimestamp("created_at"));
            p.setUpdatedAt(rs.getTimestamp("updated_at"));
            products.add(p);
        }
        return products;
    }
}
