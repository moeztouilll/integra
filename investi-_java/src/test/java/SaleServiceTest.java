import org.junit.jupiter.api.*;
import services.SaleService;
import models.Sale;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SaleServiceTest {
    static SaleService ss;
    private int idSale = -1;

    @BeforeAll
    public static void setup() {
        ss = new SaleService();
    }

    @AfterEach
    void cleanUp() throws SQLException {
        if (idSale != -1) {
            ss.delete(idSale);
            System.out.println("[DEBUG_LOG] Cleanup: Deleted Sale with ID: " + idSale);
            idSale = -1;
        }
    }

    @Test
    @Order(1)
    public void testCreateSale() {
        Sale s = new Sale("REF-TEST", 1L, 200.0, "TND", "pending", "Cash", "unpaid", null, "Addr1", "Addr2", "Notes");
        try {
            int id = ss.create(s);
            this.idSale = id;
            System.out.println("[DEBUG_LOG] Created Sale with ID: " + id);
            assertTrue(id > 0, "Sale ID should be greater than 0");
            List<Sale> sales = ss.read();
            assertFalse(sales.isEmpty());
            boolean found = sales.stream()
                    .anyMatch(sale -> sale.getId() == id && sale.getReference().equals("REF-TEST"));
            assertTrue(found, "Sale with the generated ID and reference 'REF-TEST' should exist");
        } catch (SQLException e) {
            System.out.println("Exception in testCreateSale: " + e.getMessage());
        }
    }

    @Test
    @Order(2)
    public void testUpdateSale() throws SQLException {
        Sale s = new Sale("REF-UPDATE", 1L, 300.0, "TND", "pending", "Card", "unpaid", null, "Addr1", "Addr2", "Notes");
        int id = ss.create(s);
        this.idSale = id;

        Sale updateInfo = new Sale();
        updateInfo.setId(id);
        updateInfo.setReference("REF-UPDATED");
        updateInfo.setStatus("completed");
        updateInfo.setPaymentStatus("paid");
        updateInfo.setTotalAmount(350.0);
        updateInfo.setCurrency("TND");
        updateInfo.setCustomerId(1L);
        updateInfo.setPaymentMethod("Card");
        updateInfo.setTransactionId("TX123");
        updateInfo.setShippingAddress("Addr1");
        updateInfo.setBillingAddress("Addr2");
        updateInfo.setNotes("Notes updated");

        ss.update(updateInfo);
        System.out.println("[DEBUG_LOG] Updated Sale ID " + id);

        List<Sale> sales = ss.read();
        boolean found = sales.stream()
                .anyMatch(sale -> sale.getId() == id && sale.getReference().equals("REF-UPDATED"));
        assertTrue(found, "Sale with ID " + id + " should have updated reference 'REF-UPDATED'");
    }
}
