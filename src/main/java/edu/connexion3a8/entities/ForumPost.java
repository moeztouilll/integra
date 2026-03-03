package edu.connexion3a8.entities;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class ForumPost {
    private String id;
    private String userId;
    private String title;
    private String content;
    private String category;
    private int upvotes;
    private int downvotes;
    private int views;
    private boolean isPinned;
    private boolean isDeleted;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    
    // Additional fields for display (joined from users table)
    private String authorName;
    private String authorAvatar;
    
    // Images for the post
    private List<String> imagePaths;

    public ForumPost() {
        this.imagePaths = new ArrayList<>();
    }

    public ForumPost(String userId, String title, String content, String category) {
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.category = category;
        this.upvotes = 0;
        this.downvotes = 0;
        this.views = 0;
        this.isPinned = false;
        this.isDeleted = false;
        this.imagePaths = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getUpvotes() {
        return upvotes;
    }

    public void setUpvotes(int upvotes) {
        this.upvotes = upvotes;
    }

    public int getDownvotes() {
        return downvotes;
    }

    public void setDownvotes(int downvotes) {
        this.downvotes = downvotes;
    }

    public int getViews() {
        return views;
    }

    public void setViews(int views) {
        this.views = views;
    }

    public boolean isPinned() {
        return isPinned;
    }

    public void setPinned(boolean pinned) {
        isPinned = pinned;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
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

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getAuthorAvatar() {
        return authorAvatar;
    }

    public void setAuthorAvatar(String authorAvatar) {
        this.authorAvatar = authorAvatar;
    }

    public int getScore() {
        return upvotes - downvotes;
    }

    public List<String> getImagePaths() {
        return imagePaths;
    }

    public void setImagePaths(List<String> imagePaths) {
        this.imagePaths = imagePaths;
    }

    public void addImagePath(String imagePath) {
        if (this.imagePaths == null) {
            this.imagePaths = new ArrayList<>();
        }
        this.imagePaths.add(imagePath);
    }

    public boolean hasImages() {
        return imagePaths != null && !imagePaths.isEmpty();
    }

    @Override
    public String toString() {
        return "ForumPost{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", upvotes=" + upvotes +
                ", downvotes=" + downvotes +
                ", views=" + views +
                ", isPinned=" + isPinned +
                '}';
    }
}
