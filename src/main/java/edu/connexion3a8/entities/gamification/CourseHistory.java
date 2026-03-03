package edu.connexion3a8.entities.gamification;

import java.sql.Timestamp;
import java.util.Objects;

public class CourseHistory {
    private long id;
    private String userId;
    private long courseId;
    private Timestamp visitedAt;
    private int lastPosition;
    private int completionPercentage;
    
    // For joined queries
    private Course course;

    public CourseHistory() {
    }

    public CourseHistory(String userId, long courseId) {
        this.userId = userId;
        this.courseId = courseId;
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

    public Timestamp getVisitedAt() {
        return visitedAt;
    }

    public void setVisitedAt(Timestamp visitedAt) {
        this.visitedAt = visitedAt;
    }

    public int getLastPosition() {
        return lastPosition;
    }

    public void setLastPosition(int lastPosition) {
        this.lastPosition = lastPosition;
    }

    public int getCompletionPercentage() {
        return completionPercentage;
    }

    public void setCompletionPercentage(int completionPercentage) {
        this.completionPercentage = completionPercentage;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        CourseHistory that = (CourseHistory) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "CourseHistory{" +
                "id=" + id +
                ", userId=" + userId +
                ", courseId=" + courseId +
                ", visitedAt=" + visitedAt +
                ", completionPercentage=" + completionPercentage +
                '}';
    }
}
