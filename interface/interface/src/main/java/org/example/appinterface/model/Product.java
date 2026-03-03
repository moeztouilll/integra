package org.example.appinterface.model;

import java.sql.Timestamp;

public class Product {
    private int id;
    private String title; // Matches user's 'name'
    private String description;
    private String shortDescription;
    private double price;
    private String currency;
    private boolean isDigital;
    private String downloadUrl;
    private long projectId;
    private long entrepreneurId;
    private long categoryId;
    private String status;
    private int viewsCount;
    private int salesCount;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Original fields for UI styling
    private String image;
    private String category;
    private String gradient;

    public Product(int id, String title, double price, String image, String category, String gradient,
            String description, String shortDescription, String currency, boolean isDigital,
            String downloadUrl, long projectId, long entrepreneurId, long categoryId,
            String status, int viewsCount, int salesCount, Timestamp createdAt, Timestamp updatedAt) {
        this.id = id;
        this.title = title;
        this.price = price;
        this.image = image;
        this.category = category;
        this.gradient = gradient;
        this.description = description;
        this.shortDescription = shortDescription;
        this.currency = currency;
        this.isDigital = isDigital;
        this.downloadUrl = downloadUrl;
        this.projectId = projectId;
        this.entrepreneurId = entrepreneurId;
        this.categoryId = categoryId;
        this.status = status;
        this.viewsCount = viewsCount;
        this.salesCount = salesCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public double getPrice() {
        return price;
    }

    public String getImage() {
        return image;
    }

    public String getCategory() {
        return category;
    }

    public String getGradient() {
        return gradient;
    }

    public String getDescription() {
        return description;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public String getCurrency() {
        return currency;
    }

    public boolean isDigital() {
        return isDigital;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public long getProjectId() {
        return projectId;
    }

    public long getEntrepreneurId() {
        return entrepreneurId;
    }

    public long getCategoryId() {
        return categoryId;
    }

    public String getStatus() {
        return status;
    }

    public int getViewsCount() {
        return viewsCount;
    }

    public int getSalesCount() {
        return salesCount;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }
}
