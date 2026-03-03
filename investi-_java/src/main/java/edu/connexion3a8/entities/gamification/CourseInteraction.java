package edu.connexion3a8.entities.gamification;

import java.sql.Timestamp;
import java.util.Objects;

public class CourseInteraction {
    private long id;
    private String userId;
    private long courseId;
    private String interactionType; // like, dislike, report
    private String reportReason;
    private Timestamp createdAt;

    public CourseInteraction() {
    }

    public CourseInteraction(String userId, long courseId, String interactionType) {
        this.userId = userId;
        this.courseId = courseId;
        this.interactionType = interactionType;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public long getCourseId() {
        return courseId;
    }

    public void setCourseId(long courseId) {
        this.courseId = courseId;
    }

    public String getInteractionType() {
        return interactionType;
    }

    public void setInteractionType(String interactionType) {
        this.interactionType = interactionType;
    }

    public String getReportReason() {
        return reportReason;
    }

    public void setReportReason(String reportReason) {
        this.reportReason = reportReason;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        CourseInteraction that = (CourseInteraction) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "CourseInteraction{" +
                "id=" + id +
                ", userId=" + userId +
                ", courseId=" + courseId +
                ", interactionType='" + interactionType + '\'' +
                '}';
    }
}
