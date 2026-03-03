package edu.connexion3a8.entities;

import java.sql.Timestamp;

public class ForumPostVote {
    private String id;
    private String postId;
    private String userId;
    private String voteType; // "upvote" or "downvote"
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public ForumPostVote() {
    }

    public ForumPostVote(String postId, String userId, String voteType) {
        this.postId = postId;
        this.userId = userId;
        this.voteType = voteType;
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

    public String getVoteType() {
        return voteType;
    }

    public void setVoteType(String voteType) {
        this.voteType = voteType;
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
    public String toString() {
        return "ForumPostVote{" +
                "id='" + id + '\'' +
                ", postId='" + postId + '\'' +
                ", userId='" + userId + '\'' +
                ", voteType='" + voteType + '\'' +
                '}';
    }
}
