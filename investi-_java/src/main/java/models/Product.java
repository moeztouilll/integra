package models;

import java.sql.Timestamp;

public class Product {
    private long id;
    private String name;
    private String description;
    private double price;
    private String currency;
    private boolean isDigital;
    private String downloadUrl;
    private long entrepreneurId;
    private String category;
    private String status;
    private int viewsCount;
    private int salesCount;
    private int stock;
    private int remise;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public Product() {
    }

    public Product(long id, String name, String description, double price, String currency,
            boolean isDigital, String downloadUrl, long entrepreneurId, String category, String status,
            int viewsCount, int salesCount, int stock, int remise, Timestamp createdAt,
            Timestamp updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.currency = currency;
        this.isDigital = isDigital;
        this.downloadUrl = downloadUrl;
        this.entrepreneurId = entrepreneurId;
        this.category = category;
        this.status = status;
        this.viewsCount = viewsCount;
        this.salesCount = salesCount;
        this.stock = stock;
        this.remise = remise;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Constructor for creation (without ID and timestamps)
    public Product(String name, String description, double price, String currency,
            boolean isDigital, String downloadUrl, long entrepreneurId, String category,
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
    }

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
        this.isDigital = digital;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public long getEntrepreneurId() {
        return entrepreneurId;
    }

    public void setEntrepreneurId(long entrepreneurId) {
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

    public String getCategoryName() {
        return category != null ? category : "Uncategorized";
    }

    public String getTitle() {
        return name;
    }

    public String getImage() {
        return downloadUrl; // Repurposing downloadUrl for images as requested
    }

    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", status='" + status + '\'' +
                ", price=" + price +
                ", currency='" + currency + '\'' +
                '}';
    }
}
