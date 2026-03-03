package edu.connexion3a8.entities.gamification;

import java.sql.Timestamp;

public class CourseReport {
    private long id;
    private long courseId;
    private String userId;
    private String reportReason;
    private String description;
    private String status; // pending, reviewed, resolved, dismissed
    private Timestamp createdAt;
    private Timestamp updatedAt;
    
    // For display purposes
    private String courseName;
    private String userName;

    public CourseReport() {
    }

    public CourseReport(long courseId, String userId, String reportReason, String description) {
        this.courseId = courseId;
        this.userId = userId;
        this.reportReason = reportReason;
        this.description = description;
        this.status = "pending";
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getCourseId() {
        return courseId;
    }

    public void setCourseId(long courseId) {
        this.courseId = courseId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getReportReason() {
        return reportReason;
    }

    public void setReportReason(String reportReason) {
        this.reportReason = reportReason;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
