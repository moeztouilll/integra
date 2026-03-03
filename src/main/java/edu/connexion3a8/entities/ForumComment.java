package edu.connexion3a8.entities;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class ForumComment {
    private String id;
    private String postId;
    private String userId;
    private String parentCommentId;
    private String content;
    private int upvotes;
    private int downvotes;
    private boolean isDeleted;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    
    // Additional fields for display (joined from users table)
    private String authorName;
    private String authorAvatar;
    
    // For nested replies
    private List<ForumComment> replies;

    public ForumComment() {
        this.replies = new ArrayList<>();
    }

    public ForumComment(String postId, String userId, String content) {
        this.postId = postId;
        this.userId = userId;
        this.content = content;
        this.upvotes = 0;
        this.downvotes = 0;
        this.isDeleted = false;
        this.replies = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getParentCommentId() {
        return parentCommentId;
    }

    public void setParentCommentId(String parentCommentId) {
        this.parentCommentId = parentCommentId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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

    public List<ForumComment> getReplies() {
        return replies;
    }

    public void setReplies(List<ForumComment> replies) {
        this.replies = replies;
    }

    public void addReply(ForumComment reply) {
        this.replies.add(reply);
    }

    public int getScore() {
        return upvotes - downvotes;
    }

    @Override
    public String toString() {
        return "ForumComment{" +
                "id='" + id + '\'' +
                ", postId='" + postId + '\'' +
                ", content='" + content + '\'' +
                ", upvotes=" + upvotes +
                ", downvotes=" + downvotes +
                '}';
    }
}
