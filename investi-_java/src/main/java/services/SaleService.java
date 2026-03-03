package services;

import models.Sale;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SaleService implements IService<Sale> {

    private Connection connection;

    public SaleService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public int create(Sale sale) throws SQLException {
        String sql = "INSERT INTO sale (reference, customer_id, product_id, total_amount, currency, status, payment_method, payment_status, transaction_id, shipping_address, billing_address, notes) "
                +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, sale.getReference());
        ps.setLong(2, sale.getCustomerId());
        ps.setLong(3, sale.getProductId());
        ps.setDouble(4, sale.getTotalAmount());
        ps.setString(5, sale.getCurrency());
        ps.setString(6, sale.getStatus());
        ps.setString(7, sale.getPaymentMethod());
        ps.setString(8, sale.getPaymentStatus());
        ps.setString(9, sale.getTransactionId());
        ps.setString(10, sale.getShippingAddress());
        ps.setString(11, sale.getBillingAddress());
        ps.setString(12, sale.getNotes());

        ps.executeUpdate();
        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
            int saleId = (int) rs.getLong(1);
            // Increment logic
            try {
                new ProductService().incrementSalesCount(sale.getProductId());
            } catch (Exception e) {
                System.err.println("Failed to increment sales count: " + e.getMessage());
            }
            return saleId;
        }
        return -1;
    }

    @Override
    public void update(Sale sale) throws SQLException {
        String sql = "UPDATE sale SET reference = ?, customer_id = ?, product_id = ?, total_amount = ?, currency = ?, status = ?, payment_method = ?, payment_status = ?, transaction_id = ?, shipping_address = ?, billing_address = ?, notes = ? WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, sale.getReference());
        ps.setLong(2, sale.getCustomerId());
        ps.setLong(3, sale.getProductId());
        ps.setDouble(4, sale.getTotalAmount());
        ps.setString(5, sale.getCurrency());
        ps.setString(6, sale.getStatus());
        ps.setString(7, sale.getPaymentMethod());
        ps.setString(8, sale.getPaymentStatus());
        ps.setString(9, sale.getTransactionId());
        ps.setString(10, sale.getShippingAddress());
        ps.setString(11, sale.getBillingAddress());
        ps.setString(12, sale.getNotes());
        ps.setLong(13, sale.getId());

        ps.executeUpdate();
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM sale WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setLong(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<Sale> read() throws SQLException {
        String sql = "SELECT * FROM sale";
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(sql);
        List<Sale> sales = new ArrayList<>();
        while (rs.next()) {
            Sale s = new Sale();
            s.setId(rs.getLong("id"));
            s.setReference(rs.getString("reference"));
            s.setCustomerId(rs.getLong("customer_id"));
            s.setProductId(rs.getLong("product_id"));
            s.setTotalAmount(rs.getDouble("total_amount"));
            s.setCurrency(rs.getString("currency"));
            s.setStatus(rs.getString("status"));
            s.setPaymentMethod(rs.getString("payment_method"));
            s.setPaymentStatus(rs.getString("payment_status"));
            s.setTransactionId(rs.getString("transaction_id"));
            s.setShippingAddress(rs.getString("shipping_address"));
            s.setBillingAddress(rs.getString("billing_address"));
            s.setNotes(rs.getString("notes"));
            s.setCreatedAt(rs.getTimestamp("created_at"));
            s.setUpdatedAt(rs.getTimestamp("updated_at"));
            sales.add(s);
        }
        return sales;
    }
}
