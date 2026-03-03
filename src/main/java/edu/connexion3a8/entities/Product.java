package edu.connexion3a8.entities;

import java.sql.Timestamp;

/**
 * Product entity for e-commerce/marketplace functionality
 * Integrated from Moez's gestion product module
 */
public class Product {
    private long id;
    private String name;
    private String description;
    private double price;
    private String currency;
    private boolean isDigital;
    private String downloadUrl;
    private String entrepreneurId; // Changed to String to match User.id type
    private String category;
    private String status; // draft, published, archived
    private int viewsCount;
    private int salesCount;
    private int stock;
    private int remise; // discount percentage
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public Product() {
        this.currency = "TND";
        this.status = "draft";
        this.viewsCount = 0;
        this.salesCount = 0;
        this.stock = 0;
        this.remise = 0;
    }

    public Product(String name, String description, double price, String currency,
                   boolean isDigital, String downloadUrl, String entrepreneurId, String category,
                   String status, int stock, int remise) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.currency = currency;
        this.isDigital = isDigital;
        this.downloadUrl = downloadUrl;
        this.entrepreneurId = entrepreneurId;
        this.category = category;
        this.status = status;
        this.stock = stock;
        this.remise = remise;
        this.viewsCount = 0;
        this.salesCount = 0;
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public boolean isDigital() {
        return isDigital;
    }

    public void setDigital(boolean digital) {
        isDigital = digital;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getEntrepreneurId() {
        return entrepreneurId;
    }

    public void setEntrepreneurId(String entrepreneurId) {
        this.entrepreneurId = entrepreneurId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getViewsCount() {
        return viewsCount;
    }

    public void setViewsCount(int viewsCount) {
        this.viewsCount = viewsCount;
    }

    public int getSalesCount() {
        return salesCount;
    }

    public void setSalesCount(int salesCount) {
        this.salesCount = salesCount;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public int getRemise() {
        return remise;
    }

    public void setRemise(int remise) {
        this.remise = remise;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Helper methods
    public String getCategoryName() {
        return category != null ? category : "Uncategorized";
    }

    public String getTitle() {
        return name;
    }

    public String getImage() {
        return downloadUrl;
    }

    public double getFinalPrice() {
        if (remise > 0) {
            return price * (1 - remise / 100.0);
        }
        return price;
    }

    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", status='" + status + '\'' +
                ", price=" + price +
                ", currency='" + currency + '\'' +
                ", stock=" + stock +
                '}';
    }
}
