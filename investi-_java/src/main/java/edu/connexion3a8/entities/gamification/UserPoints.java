package edu.connexion3a8.entities.gamification;

import java.sql.Timestamp;
import java.util.Objects;

public class UserPoints {
    private long id;
    private String userId;
    private int points;
    private int level;
    private int totalEarnedPoints;
    private Timestamp updatedAt;

    public UserPoints() {
    }

    public UserPoints(String userId) {
        this.userId = userId;
        this.points = 0;
        this.level = 1;
        this.totalEarnedPoints = 0;
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

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getTotalEarnedPoints() {
        return totalEarnedPoints;
    }

    public void setTotalEarnedPoints(int totalEarnedPoints) {
        this.totalEarnedPoints = totalEarnedPoints;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        UserPoints that = (UserPoints) o;
        return userId == that.userId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    @Override
    public String toString() {
        return "UserPoints{" +
                "userId=" + userId +
                ", points=" + points +
                ", level=" + level +
                ", totalEarnedPoints=" + totalEarnedPoints +
                '}';
    }
}
