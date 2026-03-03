package edu.connexion3a8.entities;

import java.sql.Timestamp;

public class ForumCommentVote {
    private String id;
    private String commentId;
    private String userId;
    private String voteType; // "upvote" or "downvote"
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public ForumCommentVote() {
    }

    public ForumCommentVote(String commentId, String userId, String voteType) {
        this.commentId = commentId;
        this.userId = userId;
        this.voteType = voteType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
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
        return "ForumCommentVote{" +
                "id='" + id + '\'' +
                ", commentId='" + commentId + '\'' +
                ", userId='" + userId + '\'' +
                ", voteType='" + voteType + '\'' +
                '}';
    }
}
