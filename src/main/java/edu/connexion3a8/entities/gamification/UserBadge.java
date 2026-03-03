package edu.connexion3a8.entities.gamification;

import java.sql.Timestamp;
import java.util.Objects;

public class UserBadge {
    private long id;
    private String userId;
    private long badgeId;
    private Timestamp earnedAt;

    public UserBadge() {
    }

    public UserBadge(String userId, long badgeId) {
        this.userId = userId;
        this.badgeId = badgeId;
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

    public long getBadgeId() {
        return badgeId;
    }

    public void setBadgeId(long badgeId) {
        this.badgeId = badgeId;
    }

    public Timestamp getEarnedAt() {
        return earnedAt;
    }

    public void setEarnedAt(Timestamp earnedAt) {
        this.earnedAt = earnedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        UserBadge userBadge = (UserBadge) o;
        return userId == userBadge.userId && badgeId == userBadge.badgeId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, badgeId);
    }

    @Override
    public String toString() {
        return "UserBadge{" +
                "userId=" + userId +
                ", badgeId=" + badgeId +
                ", earnedAt=" + earnedAt +
                '}';
    }
}
