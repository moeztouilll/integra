package edu.connexion3a8.entities.collaboration;

import java.util.Date;

public class Milestone {

    private int id;
    private int collaborationId;
    private String title;
    private String description;
    private double weight; // Contribution to total progress (0-100)
    private Date dueDate;
    private String status; // PENDING, IN_PROGRESS, COMPLETED
    private Date createdAt;

    public Milestone() {
        this.status = "PENDING";
        this.weight = 10.0;
    }

    public Milestone(int collaborationId, String title, String description, double weight, Date dueDate) {
        this.collaborationId = collaborationId;
        this.title = title;
        this.description = description;
        this.weight = weight;
        this.dueDate = dueDate;
        this.status = "PENDING";
    }

    // ─── Getters & Setters ──────────────────────────────

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCollaborationId() {
        return collaborationId;
    }

    public void setCollaborationId(int collaborationId) {
        this.collaborationId = collaborationId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isCompleted() {
        return "COMPLETED".equals(status);
    }

    public boolean isInProgress() {
        return "IN_PROGRESS".equals(status);
    }

    @Override
    public String toString() {
        return "Milestone{id=" + id + ", title='" + title + "', weight=" + weight + ", status='" + status + "'}";
    }
}
