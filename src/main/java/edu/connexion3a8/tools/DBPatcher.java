package edu.connexion3a8.tools;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class DBPatcher {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/pi";
        String user = "root";
        String pass = "";

        try (Connection cnx = DriverManager.getConnection(url, user, pass);
                Statement st = cnx.createStatement()) {

            System.out.println("Connected to DB.");
            try {
                st.executeUpdate("ALTER TABLE investment ADD COLUMN lastPaymentDate DATETIME DEFAULT NULL");
                System.out.println("Success: Added lastPaymentDate DATETIME");
            } catch (Exception e) {
                System.out.println("Catch 1: " + e.getMessage());
            }

            try {
                st.executeUpdate("ALTER TABLE investment MODIFY COLUMN lastPaymentDate DATETIME DEFAULT NULL");
                System.out.println("Success: Modified lastPaymentDate to DATETIME");
            } catch (Exception e) {
                System.out.println("Catch 2: " + e.getMessage());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
