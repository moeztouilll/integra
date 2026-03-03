package test;

import models.Sale;
import services.SaleService;
import models.Product;
import services.ProductService;

import java.sql.SQLException;

public class Main {
    public static void main(String[] args) {
        ProductService ps = new ProductService();

        try {
            // 1. Create a Product
            Product p1 = new Product("Smartphone XYZ", "A high-end smartphone", 999.99, "TND", false,
                    null, 1, "published", "default.png", 10, 0);
            int id = ps.create(p1);
            System.out.println("Product created with ID: " + id);

            // 2. Read all Products
            System.out.println("List of products:");
            for (Product p : ps.read()) {
                System.out.println(p);
            }

            // 3. Update the created product
            p1.setId(id);
            p1.setName("Smartphone XYZ - Updated");
            p1.setPrice(899.99);
            ps.update(p1);
            System.out.println("Product updated!");

            // 4. Verify update
            System.out.println("Updated product version:");
            for (Product p : ps.read()) {
                if (p.getId() == id) {
                    System.out.println(p);
                }
            }

            // ps.delete(id);
            // System.out.println("Product deleted!");

            System.out.println("--- Sale Tests ---");
            SaleService ss = new SaleService();
            // 1. Create a Sale
            Sale s1 = new Sale("REF-2024-001", 123L, 1L, 150.50, "TND", "pending", "Credit Card", "unpaid", null,
                    "123 Main St", "123 Main St", "Test sale");
            int saleId = ss.create(s1);
            System.out.println("Sale created with ID: " + saleId);

            // 2. Read all Sales
            System.out.println("List of sales:");
            for (Sale s : ss.read()) {
                System.out.println(s);
            }

            // 3. Update Sale
            s1.setId(saleId);
            s1.setStatus("paid");
            s1.setPaymentStatus("paid");
            ss.update(s1);
            System.out.println("Sale updated!");

        } catch (SQLException e) {
            System.err.println("SQL Error: " + e.getMessage());
        }
    }
}
