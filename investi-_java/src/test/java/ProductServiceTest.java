import org.junit.jupiter.api.*;
import services.ProductService;
import models.Product;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ProductServiceTest {
    static ProductService ps;
    private int idProduct = -1;

    @BeforeAll
    public static void setup() {
        ps = new ProductService();
    }

    @AfterEach
    void cleanUp() throws SQLException {
        if (idProduct != -1) {
            ps.delete(idProduct);
            System.out.println("[DEBUG_LOG] Cleanup: Deleted Product with ID: " + idProduct);
            idProduct = -1;
        }
    }

    @Test
    @Order(1)
    public void testCreateProduct() {
        Product p = new Product("Test Product", "Description", 100.0, "TND", false, null, 1,
                "published", "image.png", 10, 0);
        try {
            int id = ps.create(p);
            this.idProduct = id;
            System.out.println("[DEBUG_LOG] Created Product with ID: " + id);
            assertTrue(id > 0, "Product ID should be greater than 0");
            List<Product> products = ps.read();
            assertFalse(products.isEmpty());
            boolean found = products.stream()
                    .anyMatch(prod -> prod.getId() == id && prod.getName().equals("Test Product"));
            assertTrue(found, "Product with the generated ID and name 'Test Product' should exist");
        } catch (SQLException e) {
            System.out.println("Exception in testCreateProduct: " + e.getMessage());
        }
    }

    @Test
    @Order(2)
    public void testUpdateProduct() throws SQLException {
        Product p = new Product("Update Test", "Desc", 50.0, "TND", false, null, 1, "published",
                "image.png", 10, 0);
        int id = ps.create(p);
        this.idProduct = id;

        Product updateInfo = new Product();
        updateInfo.setId(id);
        updateInfo.setName("Updated Name");
        updateInfo.setPrice(75.0);
        updateInfo.setDescription("Updated Desc");
        updateInfo.setCurrency("TND");
        updateInfo.setStatus("published");
        updateInfo.setEntrepreneurId(1);
        updateInfo.setDigital(false);
        updateInfo.setDownloadUrl(null);

        ps.update(updateInfo);
        System.out.println("[DEBUG_LOG] Updated Product ID " + id);

        List<Product> products = ps.read();
        boolean found = products.stream().anyMatch(prod -> prod.getId() == id && prod.getName().equals("Updated Name"));
        assertTrue(found, "Product with ID " + id + " should have updated name 'Updated Name'");
    }
}
