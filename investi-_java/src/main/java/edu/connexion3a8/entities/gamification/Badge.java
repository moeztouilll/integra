package edu.connexion3a8.entities.gamification;

import java.sql.Timestamp;
import java.util.Objects;

public class Badge {
    private long id;
    private String name;
    private String description;
    private int pointsRequired;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public Badge() {
    }

    public Badge(String name, String description, int pointsRequired) {
        this.name = name;
        this.description = description;
        this.pointsRequired = pointsRequired;
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

    public int getPointsRequired() {
        return pointsRequired;
    }

    public void setPointsRequired(int pointsRequired) {
        this.pointsRequired = pointsRequired;
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
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Badge badge = (Badge) o;
        return id == badge.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Badge{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", pointsRequired=" + pointsRequired +
                '}';
    }
}
