package edu.connexion3a8.entities.gamification;

import java.sql.Timestamp;
import java.util.Objects;

public class Course {

    private long id;
    private String title;
    private String slug;
    private String description;

    private String contentUrl;
    private String contentType;

    private String difficultyLevel;
    private String category;
    private String language;

    private int estimatedDuration;
    private int rewardPoints;
    private int minimumPointsRequired;

    private String status;
    private String visibility;
    private String thumbnailUrl;

    private Timestamp createdAt;
    private Timestamp updatedAt;
    private Timestamp publishedAt;


    public Course() {
    }

    public Course(String title, String slug, String contentUrl) {
        this.title = title;
        this.slug = slug;
        this.contentUrl = contentUrl;
    }

    public Course(long id, String title, String slug, String description,
                  String contentUrl, String contentType, String difficultyLevel,
                  String category, String language, int estimatedDuration,
                  int rewardPoints, String status, String visibility,
                  String thumbnailUrl) {

        this.id = id;
        this.title = title;
        this.slug = slug;
        this.description = description;
        this.contentUrl = contentUrl;
        this.contentType = contentType;
        this.difficultyLevel = difficultyLevel;
        this.category = category;
        this.language = language;
        this.estimatedDuration = estimatedDuration;
        this.rewardPoints = rewardPoints;
        this.status = status;
        this.visibility = visibility;
        this.thumbnailUrl = thumbnailUrl;
    }



    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContentUrl() {
        return contentUrl;
    }

    public void setContentUrl(String contentUrl) {
        this.contentUrl = contentUrl;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(String difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public int getEstimatedDuration() {
        return estimatedDuration;
    }

    public void setEstimatedDuration(int estimatedDuration) {
        this.estimatedDuration = estimatedDuration;
    }

    public int getRewardPoints() {
        return rewardPoints;
    }

    public void setRewardPoints(int rewardPoints) {
        this.rewardPoints = rewardPoints;
    }

    public int getMinimumPointsRequired() {
        return minimumPointsRequired;
    }

    public void setMinimumPointsRequired(int minimumPointsRequired) {
        this.minimumPointsRequired = minimumPointsRequired;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public Timestamp getPublishedAt() {
        return publishedAt;
    }



    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Course course = (Course) o;
        return id == course.id && Objects.equals(slug, course.slug);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, slug);
    }
}
