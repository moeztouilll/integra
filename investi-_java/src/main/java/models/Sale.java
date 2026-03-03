package models;

import java.sql.Timestamp;

public class Sale {
    private long id;
    private String reference;
    private long customerId;
    private long productId;
    private double totalAmount;
    private String currency;
    private String status;
    private String paymentMethod;
    private String paymentStatus;
    private String transactionId;
    private String shippingAddress;
    private String billingAddress;
    private String notes;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public Sale() {
    }

    public Sale(long id, String reference, long customerId, long productId, double totalAmount, String currency,
            String status,
            String paymentMethod, String paymentStatus, String transactionId, String shippingAddress,
            String billingAddress, String notes, Timestamp createdAt, Timestamp updatedAt) {
        this.id = id;
        this.reference = reference;
        this.customerId = customerId;
        this.productId = productId;
        this.totalAmount = totalAmount;
        this.currency = currency;
        this.status = status;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = paymentStatus;
        this.transactionId = transactionId;
        this.shippingAddress = shippingAddress;
        this.billingAddress = billingAddress;
        this.notes = notes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Constructor for creation
    public Sale(String reference, long customerId, long productId, double totalAmount, String currency, String status,
            String paymentMethod, String paymentStatus, String transactionId, String shippingAddress,
            String billingAddress, String notes) {
        this.reference = reference;
        this.customerId = customerId;
        this.productId = productId;
        this.totalAmount = totalAmount;
        this.currency = currency;
        this.status = status;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = paymentStatus;
        this.transactionId = transactionId;
        this.shippingAddress = shippingAddress;
        this.billingAddress = billingAddress;
        this.notes = notes;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(long customerId) {
        this.customerId = customerId;
    }

    public long getProductId() {
        return productId;
    }

    public void setProductId(long productId) {
        this.productId = productId;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public String getBillingAddress() {
        return billingAddress;
    }

    public void setBillingAddress(String billingAddress) {
        this.billingAddress = billingAddress;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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

    @Override
    public String toString() {
        return "Sale{" +
                "id=" + id +
                ", reference='" + reference + '\'' +
                ", totalAmount=" + totalAmount +
                ", status='" + status + '\'' +
                '}';
    }
}
