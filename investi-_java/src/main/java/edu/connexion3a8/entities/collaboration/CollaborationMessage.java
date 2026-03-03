package edu.connexion3a8.entities.collaboration;

import java.sql.Timestamp;

public class CollaborationMessage {
    private int id;
    private int investmentId; // For pre-acceptance negotiation
    private int collaborationId; // For post-acceptance messages
    private String senderId; // Changed from int to String (UUID)
    private String message;
    private String type; // 'GENERAL', 'NEGOTIATION'
    private Timestamp createdAt;

    public CollaborationMessage() {
    }

    public CollaborationMessage(int id, int investmentId, int collaborationId, String senderId, String message,
            String type, Timestamp createdAt) {
        this.id = id;
        this.investmentId = investmentId;
        this.collaborationId = collaborationId;
        this.senderId = senderId;
        this.message = message;
        this.type = type;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getInvestmentId() {
        return investmentId;
    }

    public void setInvestmentId(int investmentId) {
        this.investmentId = investmentId;
    }

    public int getCollaborationId() {
        return collaborationId;
    }

    public void setCollaborationId(int collaborationId) {
        this.collaborationId = collaborationId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
