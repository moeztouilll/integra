package edu.connexion3a8.services;

import edu.connexion3a8.entities.ForumPost;
import edu.connexion3a8.entities.ForumComment;
import edu.connexion3a8.tools.BadWordsFilter;
import edu.connexion3a8.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ForumPostService {

    public ForumPostService() {
        ensureBookmarksTable();
        ensureNotificationsTable();
    }
    
    private Connection getConnection() throws SQLException {
        return MyConnection.getInstance().getCnx();
    }

    private void ensureNotificationsTable() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS forum_notifications (" +
                "id CHAR(36) PRIMARY KEY DEFAULT (UUID()), " +
                "recipient_user_id CHAR(36) NOT NULL, " +
                "sender_user_id CHAR(36) NOT NULL, " +
                "post_id CHAR(36) NOT NULL, " +
                "comment_id CHAR(36) NULL, " +
                "type VARCHAR(50) NOT NULL DEFAULT 'mention', " +
                "message TEXT NOT NULL, " +
                "is_read BOOLEAN DEFAULT FALSE, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "INDEX idx_forum_notif_recipient (recipient_user_id), " +
                "INDEX idx_forum_notif_read (recipient_user_id, is_read)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
            );
        } catch (SQLException e) {
            System.out.println("Note: Could not ensure notifications table: " + e.getMessage());
        }
    }

    private void ensureBookmarksTable() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS forum_bookmarks (" +
                "id CHAR(36) PRIMARY KEY DEFAULT (UUID()), " +
                "post_id CHAR(36) NOT NULL, " +
                "user_id CHAR(36) NOT NULL, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "UNIQUE KEY unique_bookmark (post_id, user_id), " +
                "INDEX idx_forum_bookmarks_user_id (user_id), " +
                "INDEX idx_forum_bookmarks_post_id (post_id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
            );
        } catch (SQLException e) {
            System.out.println("Note: Could not ensure bookmarks table: " + e.getMessage());
        }
    }

    // ========== POST CRUD OPERATIONS ==========
    
    public String addPost(ForumPost post) throws SQLException {
        // Check if user is verified before allowing post creation
        if (!UserAuthService.canPerformWriteOperations(post.getUserId())) {
            UserAuthService.UserVerificationStatus status = UserAuthService.getUserVerificationStatus(post.getUserId());
            throw new SQLException("Cannot create post: " + (status != null ? status.getStatusMessage() : "User not verified"));
        }
        
        String query = "INSERT INTO forum_posts (user_id, title, content, category, upvotes, downvotes, views, is_pinned, is_deleted) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        String postId = null;
        try (Connection conn = getConnection(); PreparedStatement pst = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pst.setString(1, post.getUserId());
            pst.setString(2, post.getTitle());
            pst.setString(3, post.getContent());
            pst.setString(4, post.getCategory());
            pst.setInt(5, post.getUpvotes());
            pst.setInt(6, post.getDownvotes());
            pst.setInt(7, post.getViews());
            pst.setBoolean(8, post.isPinned());
            pst.setBoolean(9, post.isDeleted());
            
            pst.executeUpdate();
        }
        
        // Get the generated UUID using a separate connection
        String selectQuery = "SELECT id FROM forum_posts WHERE user_id=? ORDER BY created_at DESC LIMIT 1";
        try (Connection conn = getConnection(); PreparedStatement selectPst = conn.prepareStatement(selectQuery)) {
            selectPst.setString(1, post.getUserId());
            ResultSet rs = selectPst.executeQuery();
            if (rs.next()) {
                postId = rs.getString("id");
            }
        }
        
        // Add images if any
        if (postId != null && post.hasImages()) {
            addPostImages(postId, post.getImagePaths());
        }
        
        return postId;
    }

    public void addPostImages(String postId, List<String> imagePaths) throws SQLException {
        String query = "INSERT INTO forum_post_images (post_id, image_path, image_order) VALUES (?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pst = conn.prepareStatement(query)) {
            for (int i = 0; i < imagePaths.size(); i++) {
                pst.setString(1, postId);
                pst.setString(2, imagePaths.get(i));
                pst.setInt(3, i);
                pst.addBatch();
            }
            pst.executeBatch();
        }
    }

    public List<String> getPostImages(String postId) throws SQLException {
        List<String> images = new ArrayList<>();
        String query = "SELECT image_path FROM forum_post_images WHERE post_id=? ORDER BY image_order";
        try (Connection conn = getConnection(); PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setString(1, postId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                images.add(rs.getString("image_path"));
            }
        }
        return images;
    }

    public void deletePostImages(String postId) throws SQLException {
        String query = "DELETE FROM forum_post_images WHERE post_id=?";
        try (Connection conn = getConnection(); PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setString(1, postId);
            pst.executeUpdate();
        }
    }

    public void updatePost(String id, ForumPost post) throws SQLException {
        String query = "UPDATE forum_posts SET title=?, content=?, category=?, is_pinned=? WHERE id=? AND is_deleted=FALSE";
        
        try (Connection conn = getConnection(); PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setString(1, post.getTitle());
            pst.setString(2, post.getContent());
            pst.setString(3, post.getCategory());
            pst.setBoolean(4, post.isPinned());
            pst.setString(5, id);
            
            pst.executeUpdate();
        }
        
        // Update images - delete old and add new
        if (post.hasImages()) {
            deletePostImages(id);
            addPostImages(id, post.getImagePaths());
        }
    }

    public void deletePost(String id) throws SQLException {
        String query = "UPDATE forum_posts SET is_deleted=TRUE WHERE id=?";
        
        try (Connection conn = getConnection(); PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setString(1, id);
            pst.executeUpdate();
        }
    }

    public void hardDeletePost(String id) throws SQLException {
        // Images will be deleted by CASCADE
        String query = "DELETE FROM forum_posts WHERE id=?";
        
        try (Connection conn = getConnection(); PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setString(1, id);
            pst.executeUpdate();
        }
    }

    public ForumPost getPostById(String id) throws SQLException {
        String query = "SELECT p.*, u.name as author_name, u.avatar_url as author_avatar " +
                "FROM forum_posts p " +
                "LEFT JOIN users u ON p.user_id = u.id " +
                "WHERE p.id=? AND p.is_deleted=FALSE";
        
        try (Connection conn = getConnection(); PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setString(1, id);
            ResultSet rs = pst.executeQuery();
            
            if (rs.next()) {
                return extractPostFromResultSet(rs);
            }
        }
        return null;
    }


    public List<ForumPost> getAllPosts() throws SQLException {
        List<ForumPost> posts = new ArrayList<>();
        String query = "SELECT p.*, u.name as author_name, u.avatar_url as author_avatar " +
                "FROM forum_posts p " +
                "LEFT JOIN users u ON p.user_id = u.id " +
                "WHERE p.is_deleted=FALSE " +
                "ORDER BY p.is_pinned DESC, p.created_at DESC";
        
        try (Connection conn = getConnection(); Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            
            while (rs.next()) {
                posts.add(extractPostFromResultSet(rs));
            }
        }
        
        loadImagesForPosts(posts);
        return posts;
    }

    public List<ForumPost> getPostsByCategory(String category) throws SQLException {
        List<ForumPost> posts = new ArrayList<>();
        String query = "SELECT p.*, u.name as author_name, u.avatar_url as author_avatar " +
                "FROM forum_posts p " +
                "LEFT JOIN users u ON p.user_id = u.id " +
                "WHERE p.category=? AND p.is_deleted=FALSE " +
                "ORDER BY p.is_pinned DESC, p.created_at DESC";
        
        try (Connection conn = getConnection(); PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setString(1, category);
            ResultSet rs = pst.executeQuery();
            
            while (rs.next()) {
                posts.add(extractPostFromResultSet(rs));
            }
        }
        
        loadImagesForPosts(posts);
        return posts;
    }

    public List<ForumPost> getPostsByUser(String userId) throws SQLException {
        List<ForumPost> posts = new ArrayList<>();
        String query = "SELECT p.*, u.name as author_name, u.avatar_url as author_avatar " +
                "FROM forum_posts p " +
                "LEFT JOIN users u ON p.user_id = u.id " +
                "WHERE p.user_id=? AND p.is_deleted=FALSE " +
                "ORDER BY p.created_at DESC";
        
        try (Connection conn = getConnection(); PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setString(1, userId);
            ResultSet rs = pst.executeQuery();
            
            while (rs.next()) {
                posts.add(extractPostFromResultSet(rs));
            }
        }
        loadImagesForPosts(posts);
        return posts;
    }

    public List<ForumPost> getTrendingPosts() throws SQLException {
        List<ForumPost> posts = new ArrayList<>();
        String query = "SELECT p.*, u.name as author_name, u.avatar_url as author_avatar " +
                "FROM forum_posts p " +
                "LEFT JOIN users u ON p.user_id = u.id " +
                "WHERE p.is_deleted=FALSE " +
                "ORDER BY (p.upvotes - p.downvotes) DESC, p.views DESC LIMIT 20";
        
        try (Connection conn = getConnection(); Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            
            while (rs.next()) {
                posts.add(extractPostFromResultSet(rs));
            }
        }
        loadImagesForPosts(posts);
        return posts;
    }

    public void incrementViews(String postId) throws SQLException {
        String query = "UPDATE forum_posts SET views = views + 1 WHERE id=?";
        try (Connection conn = getConnection(); PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setString(1, postId);
            pst.executeUpdate();
        }
    }

    /**
     * Records a view for a post by a specific user.
     * Each user can only count as one view per post.
     * Returns true if this is a new view, false if user already viewed.
     */
    public boolean recordPostView(String postId, String userId) throws SQLException {
        // Check if user already viewed this post
        String checkQuery = "SELECT id FROM forum_post_views WHERE post_id=? AND user_id=?";
        try (Connection conn = getConnection(); PreparedStatement checkPst = conn.prepareStatement(checkQuery)) {
            checkPst.setString(1, postId);
            checkPst.setString(2, userId);
            ResultSet rs = checkPst.executeQuery();
            if (rs.next()) {
                return false; // Already viewed
            }
        }
        
        // Record the view
        String insertQuery = "INSERT INTO forum_post_views (post_id, user_id) VALUES (?, ?)";
        try (Connection conn = getConnection(); PreparedStatement insertPst = conn.prepareStatement(insertQuery)) {
            insertPst.setString(1, postId);
            insertPst.setString(2, userId);
            insertPst.executeUpdate();
        }
        
        // Increment the view count on the post
        incrementViews(postId);
        return true;
    }

    /**
     * Check if a user has viewed a post
     */
    public boolean hasUserViewedPost(String postId, String userId) throws SQLException {
        String query = "SELECT id FROM forum_post_views WHERE post_id=? AND user_id=?";
        try (Connection conn = getConnection(); PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setString(1, postId);
            pst.setString(2, userId);
            ResultSet rs = pst.executeQuery();
            return rs.next();
        }
    }

    // ========== POST VOTE OPERATIONS ==========
    
    public String getUserVoteOnPost(String postId, String userId) throws SQLException {
        String query = "SELECT vote_type FROM forum_post_votes WHERE post_id=? AND user_id=?";
        try (Connection conn = getConnection(); PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setString(1, postId);
            pst.setString(2, userId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return rs.getString("vote_type");
            }
        }
        return null;
    }

    public void votePost(String postId, String userId, String voteType) throws SQLException {
        // Check if user is verified before allowing vote
        if (!UserAuthService.canPerformWriteOperations(userId)) {
            UserAuthService.UserVerificationStatus status = UserAuthService.getUserVerificationStatus(userId);
            throw new SQLException("Cannot vote: " + (status != null ? status.getStatusMessage() : "User not verified"));
        }
        
        String existingVote = getUserVoteOnPost(postId, userId);
        
        if (existingVote != null) {
            if (existingVote.equals(voteType)) {
                // Remove vote (toggle off)
                removePostVote(postId, userId, existingVote);
            } else {
                // Change vote
                updatePostVote(postId, userId, existingVote, voteType);
            }
        } else {
            // New vote
            addPostVote(postId, userId, voteType);
        }
    }

    private void addPostVote(String postId, String userId, String voteType) throws SQLException {
        String insertQuery = "INSERT INTO forum_post_votes (post_id, user_id, vote_type) VALUES (?, ?, ?)";
        String updateQuery = voteType.equals("upvote") 
            ? "UPDATE forum_posts SET upvotes = upvotes + 1 WHERE id=?"
            : "UPDATE forum_posts SET downvotes = downvotes + 1 WHERE id=?";
        
        try (Connection conn = getConnection(); PreparedStatement insertPst = conn.prepareStatement(insertQuery);
             PreparedStatement updatePst = conn.prepareStatement(updateQuery)) {
            
            insertPst.setString(1, postId);
            insertPst.setString(2, userId);
            insertPst.setString(3, voteType);
            insertPst.executeUpdate();
            
            updatePst.setString(1, postId);
            updatePst.executeUpdate();
        }
    }

    private void removePostVote(String postId, String userId, String voteType) throws SQLException {
        String deleteQuery = "DELETE FROM forum_post_votes WHERE post_id=? AND user_id=?";
        String updateQuery = voteType.equals("upvote")
            ? "UPDATE forum_posts SET upvotes = upvotes - 1 WHERE id=?"
            : "UPDATE forum_posts SET downvotes = downvotes - 1 WHERE id=?";
        
        try (Connection conn = getConnection(); PreparedStatement deletePst = conn.prepareStatement(deleteQuery);
             PreparedStatement updatePst = conn.prepareStatement(updateQuery)) {
            
            deletePst.setString(1, postId);
            deletePst.setString(2, userId);
            deletePst.executeUpdate();
            
            updatePst.setString(1, postId);
            updatePst.executeUpdate();
        }
    }

    private void updatePostVote(String postId, String userId, String oldVote, String newVote) throws SQLException {
        String updateVoteQuery = "UPDATE forum_post_votes SET vote_type=? WHERE post_id=? AND user_id=?";
        String updatePostQuery = oldVote.equals("upvote")
            ? "UPDATE forum_posts SET upvotes = upvotes - 1, downvotes = downvotes + 1 WHERE id=?"
            : "UPDATE forum_posts SET upvotes = upvotes + 1, downvotes = downvotes - 1 WHERE id=?";
        
        try (Connection conn = getConnection(); PreparedStatement updateVotePst = conn.prepareStatement(updateVoteQuery);
             PreparedStatement updatePostPst = conn.prepareStatement(updatePostQuery)) {
            
            updateVotePst.setString(1, newVote);
            updateVotePst.setString(2, postId);
            updateVotePst.setString(3, userId);
            updateVotePst.executeUpdate();
            
            updatePostPst.setString(1, postId);
            updatePostPst.executeUpdate();
        }
    }


    // ========== COMMENT CRUD OPERATIONS ==========
    
    public String addComment(ForumComment comment) throws SQLException {
        // Check if user is verified before allowing comment creation
        if (!UserAuthService.canPerformWriteOperations(comment.getUserId())) {
            UserAuthService.UserVerificationStatus status = UserAuthService.getUserVerificationStatus(comment.getUserId());
            throw new SQLException("Cannot create comment: " + (status != null ? status.getStatusMessage() : "User not verified"));
        }
        
        String query = "INSERT INTO forum_comments (post_id, user_id, parent_comment_id, content, upvotes, downvotes, is_deleted) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        String commentId = null;
        try (Connection conn = getConnection(); PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setString(1, comment.getPostId());
            pst.setString(2, comment.getUserId());
            pst.setString(3, comment.getParentCommentId());
            pst.setString(4, comment.getContent());
            pst.setInt(5, comment.getUpvotes());
            pst.setInt(6, comment.getDownvotes());
            pst.setBoolean(7, comment.isDeleted());
            
            pst.executeUpdate();
        }
        
        // Get the generated UUID using a separate connection
        String selectQuery = "SELECT id FROM forum_comments WHERE post_id=? AND user_id=? ORDER BY created_at DESC LIMIT 1";
        try (Connection conn = getConnection(); PreparedStatement selectPst = conn.prepareStatement(selectQuery)) {
            selectPst.setString(1, comment.getPostId());
            selectPst.setString(2, comment.getUserId());
            ResultSet rs = selectPst.executeQuery();
            if (rs.next()) {
                commentId = rs.getString("id");
            }
        }
        
        return commentId;
    }

    public void updateComment(String id, String content) throws SQLException {
        String query = "UPDATE forum_comments SET content=? WHERE id=? AND is_deleted=FALSE";
        
        try (Connection conn = getConnection(); PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setString(1, content);
            pst.setString(2, id);
            pst.executeUpdate();
        }
    }

    public void deleteComment(String id) throws SQLException {
        String query = "UPDATE forum_comments SET is_deleted=TRUE WHERE id=?";
        
        try (Connection conn = getConnection(); PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setString(1, id);
            pst.executeUpdate();
        }
    }

    public void hardDeleteComment(String id) throws SQLException {
        String query = "DELETE FROM forum_comments WHERE id=?";
        
        try (Connection conn = getConnection(); PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setString(1, id);
            pst.executeUpdate();
        }
    }

    public ForumComment getCommentById(String id) throws SQLException {
        String query = "SELECT c.*, u.name as author_name, u.avatar_url as author_avatar " +
                "FROM forum_comments c " +
                "LEFT JOIN users u ON c.user_id = u.id " +
                "WHERE c.id=? AND c.is_deleted=FALSE";
        
        try (Connection conn = getConnection(); PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setString(1, id);
            ResultSet rs = pst.executeQuery();
            
            if (rs.next()) {
                return extractCommentFromResultSet(rs);
            }
        }
        return null;
    }

    public List<ForumComment> getCommentsByPost(String postId) throws SQLException {
        List<ForumComment> comments = new ArrayList<>();
        String query = "SELECT c.*, u.name as author_name, u.avatar_url as author_avatar " +
                "FROM forum_comments c " +
                "LEFT JOIN users u ON c.user_id = u.id " +
                "WHERE c.post_id=? AND c.is_deleted=FALSE " +
                "ORDER BY c.created_at ASC";
        
        try (Connection conn = getConnection(); PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setString(1, postId);
            ResultSet rs = pst.executeQuery();
            
            while (rs.next()) {
                comments.add(extractCommentFromResultSet(rs));
            }
        }
        return buildCommentTree(comments);
    }

    public List<ForumComment> getCommentsByUser(String userId) throws SQLException {
        List<ForumComment> comments = new ArrayList<>();
        String query = "SELECT c.*, u.name as author_name, u.avatar_url as author_avatar " +
                "FROM forum_comments c " +
                "LEFT JOIN users u ON c.user_id = u.id " +
                "WHERE c.user_id=? AND c.is_deleted=FALSE " +
                "ORDER BY c.created_at DESC";
        
        try (Connection conn = getConnection(); PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setString(1, userId);
            ResultSet rs = pst.executeQuery();
            
            while (rs.next()) {
                comments.add(extractCommentFromResultSet(rs));
            }
        }
        return comments;
    }

    public int getCommentCountByPost(String postId) throws SQLException {
        String query = "SELECT COUNT(*) as count FROM forum_comments WHERE post_id=? AND is_deleted=FALSE";
        try (Connection conn = getConnection(); PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setString(1, postId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return rs.getInt("count");
            }
        }
        return 0;
    }

    private List<ForumComment> buildCommentTree(List<ForumComment> flatComments) {
        List<ForumComment> rootComments = new ArrayList<>();
        java.util.Map<String, ForumComment> commentMap = new java.util.HashMap<>();
        
        // First pass: create map
        for (ForumComment comment : flatComments) {
            commentMap.put(comment.getId(), comment);
        }
        
        // Second pass: build tree
        for (ForumComment comment : flatComments) {
            if (comment.getParentCommentId() == null) {
                rootComments.add(comment);
            } else {
                ForumComment parent = commentMap.get(comment.getParentCommentId());
                if (parent != null) {
                    parent.addReply(comment);
                }
            }
        }
        
        return rootComments;
    }


    // ========== COMMENT VOTE OPERATIONS ==========
    
    public String getUserVoteOnComment(String commentId, String userId) throws SQLException {
        String query = "SELECT vote_type FROM forum_comment_votes WHERE comment_id=? AND user_id=?";
        try (Connection conn = getConnection(); PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setString(1, commentId);
            pst.setString(2, userId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return rs.getString("vote_type");
            }
        }
        return null;
    }

    public void voteComment(String commentId, String userId, String voteType) throws SQLException {
        // Check if user is verified before allowing vote
        if (!UserAuthService.canPerformWriteOperations(userId)) {
            UserAuthService.UserVerificationStatus status = UserAuthService.getUserVerificationStatus(userId);
            throw new SQLException("Cannot vote: " + (status != null ? status.getStatusMessage() : "User not verified"));
        }
        
        String existingVote = getUserVoteOnComment(commentId, userId);
        
        if (existingVote != null) {
            if (existingVote.equals(voteType)) {
                removeCommentVote(commentId, userId, existingVote);
            } else {
                updateCommentVote(commentId, userId, existingVote, voteType);
            }
        } else {
            addCommentVote(commentId, userId, voteType);
        }
    }

    private void addCommentVote(String commentId, String userId, String voteType) throws SQLException {
        String insertQuery = "INSERT INTO forum_comment_votes (comment_id, user_id, vote_type) VALUES (?, ?, ?)";
        String updateQuery = voteType.equals("upvote")
            ? "UPDATE forum_comments SET upvotes = upvotes + 1 WHERE id=?"
            : "UPDATE forum_comments SET downvotes = downvotes + 1 WHERE id=?";
        
        try (Connection conn = getConnection(); PreparedStatement insertPst = conn.prepareStatement(insertQuery);
             PreparedStatement updatePst = conn.prepareStatement(updateQuery)) {
            
            insertPst.setString(1, commentId);
            insertPst.setString(2, userId);
            insertPst.setString(3, voteType);
            insertPst.executeUpdate();
            
            updatePst.setString(1, commentId);
            updatePst.executeUpdate();
        }
    }

    private void removeCommentVote(String commentId, String userId, String voteType) throws SQLException {
        String deleteQuery = "DELETE FROM forum_comment_votes WHERE comment_id=? AND user_id=?";
        String updateQuery = voteType.equals("upvote")
            ? "UPDATE forum_comments SET upvotes = upvotes - 1 WHERE id=?"
            : "UPDATE forum_comments SET downvotes = downvotes - 1 WHERE id=?";
        
        try (Connection conn = getConnection(); PreparedStatement deletePst = conn.prepareStatement(deleteQuery);
             PreparedStatement updatePst = conn.prepareStatement(updateQuery)) {
            
            deletePst.setString(1, commentId);
            deletePst.setString(2, userId);
            deletePst.executeUpdate();
            
            updatePst.setString(1, commentId);
            updatePst.executeUpdate();
        }
    }

    private void updateCommentVote(String commentId, String userId, String oldVote, String newVote) throws SQLException {
        String updateVoteQuery = "UPDATE forum_comment_votes SET vote_type=? WHERE comment_id=? AND user_id=?";
        String updateCommentQuery = oldVote.equals("upvote")
            ? "UPDATE forum_comments SET upvotes = upvotes - 1, downvotes = downvotes + 1 WHERE id=?"
            : "UPDATE forum_comments SET upvotes = upvotes + 1, downvotes = downvotes - 1 WHERE id=?";
        
        try (Connection conn = getConnection(); PreparedStatement updateVotePst = conn.prepareStatement(updateVoteQuery);
             PreparedStatement updateCommentPst = conn.prepareStatement(updateCommentQuery)) {
            
            updateVotePst.setString(1, newVote);
            updateVotePst.setString(2, commentId);
            updateVotePst.setString(3, userId);
            updateVotePst.executeUpdate();
            
            updateCommentPst.setString(1, commentId);
            updateCommentPst.executeUpdate();
        }
    }

    // ========== USER ACTIVITY OPERATIONS ==========
    
    public List<ForumPost> getPostsVotedByUser(String userId) throws SQLException {
        List<ForumPost> posts = new ArrayList<>();
        String query = "SELECT p.*, u.name as author_name, u.avatar_url as author_avatar, v.vote_type " +
                "FROM forum_posts p " +
                "INNER JOIN forum_post_votes v ON p.id = v.post_id " +
                "LEFT JOIN users u ON p.user_id = u.id " +
                "WHERE v.user_id=? AND p.is_deleted=FALSE " +
                "ORDER BY v.created_at DESC";
        
        try (Connection conn = getConnection(); PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setString(1, userId);
            ResultSet rs = pst.executeQuery();
            
            while (rs.next()) {
                posts.add(extractPostFromResultSet(rs));
            }
        }
        loadImagesForPosts(posts);
        return posts;
    }

    public List<ForumPost> getPostsCommentedByUser(String userId) throws SQLException {
        List<ForumPost> posts = new ArrayList<>();
        String query = "SELECT DISTINCT p.*, u.name as author_name, u.avatar_url as author_avatar " +
                "FROM forum_posts p " +
                "INNER JOIN forum_comments c ON p.id = c.post_id " +
                "LEFT JOIN users u ON p.user_id = u.id " +
                "WHERE c.user_id=? AND p.is_deleted=FALSE AND c.is_deleted=FALSE " +
                "ORDER BY p.created_at DESC";
        
        try (Connection conn = getConnection(); PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setString(1, userId);
            ResultSet rs = pst.executeQuery();
            
            while (rs.next()) {
                posts.add(extractPostFromResultSet(rs));
            }
        }
        loadImagesForPosts(posts);
        return posts;
    }

    // ========== SEARCH OPERATIONS ==========
    
    public List<ForumPost> searchPosts(String searchTerm) throws SQLException {
        List<ForumPost> posts = new ArrayList<>();
        String query = "SELECT p.*, u.name as author_name, u.avatar_url as author_avatar " +
                "FROM forum_posts p " +
                "LEFT JOIN users u ON p.user_id = u.id " +
                "WHERE p.is_deleted=FALSE AND (p.title LIKE ? OR p.content LIKE ?) " +
                "ORDER BY p.created_at DESC";
        
        try (Connection conn = getConnection(); PreparedStatement pst = conn.prepareStatement(query)) {
            String searchPattern = "%" + searchTerm + "%";
            pst.setString(1, searchPattern);
            pst.setString(2, searchPattern);
            ResultSet rs = pst.executeQuery();
            
            while (rs.next()) {
                posts.add(extractPostFromResultSet(rs));
            }
        }
        loadImagesForPosts(posts);
        return posts;
    }

    // ========== HELPER METHODS ==========
    
    private void loadImagesForPosts(List<ForumPost> posts) {
        for (ForumPost post : posts) {
            try {
                List<String> images = getPostImages(post.getId());
                post.setImagePaths(images);
            } catch (SQLException e) {
                // Ignore if images table doesn't exist yet
            }
        }
    }
    
    private ForumPost extractPostFromResultSet(ResultSet rs) throws SQLException {
        ForumPost post = new ForumPost();
        post.setId(rs.getString("id"));
        post.setUserId(rs.getString("user_id"));
        post.setTitle(rs.getString("title"));
        post.setContent(rs.getString("content"));
        post.setCategory(rs.getString("category"));
        post.setUpvotes(rs.getInt("upvotes"));
        post.setDownvotes(rs.getInt("downvotes"));
        post.setViews(rs.getInt("views"));
        post.setPinned(rs.getBoolean("is_pinned"));
        post.setDeleted(rs.getBoolean("is_deleted"));
        post.setCreatedAt(rs.getTimestamp("created_at"));
        post.setUpdatedAt(rs.getTimestamp("updated_at"));
        
        try {
            post.setAuthorName(rs.getString("author_name"));
            post.setAuthorAvatar(rs.getString("author_avatar"));
        } catch (SQLException e) {
            // Columns might not exist in all queries
        }
        
        // Don't load images here - will be loaded separately to avoid ResultSet conflicts
        // Images will be loaded after the main ResultSet is closed
        
        return post;
    }

    private ForumComment extractCommentFromResultSet(ResultSet rs) throws SQLException {
        ForumComment comment = new ForumComment();
        comment.setId(rs.getString("id"));
        comment.setPostId(rs.getString("post_id"));
        comment.setUserId(rs.getString("user_id"));
        comment.setParentCommentId(rs.getString("parent_comment_id"));
        comment.setContent(rs.getString("content"));
        comment.setUpvotes(rs.getInt("upvotes"));
        comment.setDownvotes(rs.getInt("downvotes"));
        comment.setDeleted(rs.getBoolean("is_deleted"));
        comment.setCreatedAt(rs.getTimestamp("created_at"));
        comment.setUpdatedAt(rs.getTimestamp("updated_at"));
        
        try {
            comment.setAuthorName(rs.getString("author_name"));
            comment.setAuthorAvatar(rs.getString("author_avatar"));
        } catch (SQLException e) {
            // Columns might not exist in all queries
        }
        
        return comment;
    }

    // ========== USER OPERATIONS (for temporary login) ==========
    
    public List<String[]> getAllUsers() throws SQLException {
        List<String[]> users = new ArrayList<>();
        String query = "SELECT id, name, email, role FROM users WHERE is_active=TRUE ORDER BY name";
        
        try (Connection conn = getConnection(); Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                users.add(new String[]{
                    rs.getString("id"),
                    rs.getString("name"),
                    rs.getString("email"),
                    rs.getString("role")
                });
            }
        }
        return users;
    }

    public String[] getUserById(String userId) throws SQLException {
        String query = "SELECT id, name, email, role FROM users WHERE id=?";
        try (Connection conn = getConnection(); PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setString(1, userId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return new String[]{
                    rs.getString("id"),
                    rs.getString("name"),
                    rs.getString("email"),
                    rs.getString("role")
                };
            }
        }
        return null;
    }

    // ========== INPUT VALIDATION OPERATIONS ==========

    /**
     * Check if a duplicate post exists (same title + content by same user)
     * Returns true if duplicate exists
     */
    public boolean isDuplicatePost(String userId, String title, String content) throws SQLException {
        String query = "SELECT id FROM forum_posts WHERE user_id=? AND is_deleted=FALSE " +
                "AND (title=? OR (title IS NULL AND ? IS NULL)) " +
                "AND (content=? OR (content IS NULL AND ? IS NULL))";
        
        try (Connection conn = getConnection(); PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setString(1, userId);
            pst.setString(2, title);
            pst.setString(3, title);
            pst.setString(4, content);
            pst.setString(5, content);
            ResultSet rs = pst.executeQuery();
            return rs.next();
        }
    }

    /**
     * Validate post ID format (UUID format)
     */
    public boolean isValidPostId(String postId) {
        if (postId == null || postId.trim().isEmpty()) {
            return false;
        }
        // UUID format: 8-4-4-4-12 hex characters
        String uuidRegex = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
        return postId.matches(uuidRegex);
    }

    /**
     * Check if post ID exists in database
     */
    public boolean postExists(String postId) throws SQLException {
        if (!isValidPostId(postId)) {
            return false;
        }
        String query = "SELECT id FROM forum_posts WHERE id=? AND is_deleted=FALSE";
        try (Connection conn = getConnection(); PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setString(1, postId);
            ResultSet rs = pst.executeQuery();
            return rs.next();
        }
    }

    /**
     * Validate post title (not empty, max 500 chars, no spam patterns)
     */
    public String validateTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            return null; // Title is optional
        }
        title = title.trim();
        if (title.length() > 500) {
            return "Title must be less than 500 characters";
        }
        // Check for spam patterns (repeated characters)
        if (title.matches(".*(.)(\\1{5,}).*")) {
            return "Title contains invalid repeated characters";
        }
        return null; // Valid
    }

    /**
     * Validate post content (not empty if no title, max 10000 chars)
     */
    public String validateContent(String content, String title, boolean hasImages) {
        if ((content == null || content.trim().isEmpty()) && 
            (title == null || title.trim().isEmpty()) && !hasImages) {
            return "Post must have title, content, or images";
        }
        if (content != null && content.length() > 10000) {
            return "Content must be less than 10000 characters";
        }
        return null; // Valid
    }

    /**
     * Validate category
     */
    public String validateCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return "Category is required";
        }
        List<String> validCategories = List.of(
            "General", "Tips & Advice", "Success Stories", 
            "Investor Insights", "Collaboration", "Announcements"
        );
        if (!validCategories.contains(category)) {
            return "Invalid category";
        }
        return null; // Valid
    }

    /**
     * Full post validation - returns error message or null if valid
     */
    public String validatePost(ForumPost post) throws SQLException {
        // Check title
        String titleError = validateTitle(post.getTitle());
        if (titleError != null) return titleError;

        // Check content
        String contentError = validateContent(post.getContent(), post.getTitle(), post.hasImages());
        if (contentError != null) return contentError;

        // Check category
        String categoryError = validateCategory(post.getCategory());
        if (categoryError != null) return categoryError;

        // Check for bad words in title
        if (BadWordsFilter.containsBadWords(post.getTitle())) {
            return "Your title contains inappropriate language";
        }

        // Check for bad words in content
        if (BadWordsFilter.containsBadWords(post.getContent())) {
            return "Your post contains inappropriate language";
        }

        // Check for duplicate
        if (isDuplicatePost(post.getUserId(), post.getTitle(), post.getContent())) {
            return "You have already posted this content";
        }

        return null; // All valid
    }

    /**
     * Validate comment content for bad words
     */
    public String validateComment(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "Comment cannot be empty";
        }
        if (content.length() > 5000) {
            return "Comment must be less than 5000 characters";
        }
        if (BadWordsFilter.containsBadWords(content)) {
            return "Your comment contains inappropriate language";
        }
        return null; // Valid
    }
    // =====================================================
    // Bookmark / Save Methods
    // =====================================================

    public void toggleBookmark(String postId, String userId) throws SQLException {
        if (isBookmarked(postId, userId)) {
            removeBookmark(postId, userId);
        } else {
            addBookmark(postId, userId);
        }
    }

    public void addBookmark(String postId, String userId) throws SQLException {
        String query = "INSERT IGNORE INTO forum_bookmarks (id, post_id, user_id) VALUES (UUID(), ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, postId);
            ps.setString(2, userId);
            ps.executeUpdate();
        }
    }

    public void removeBookmark(String postId, String userId) throws SQLException {
        String query = "DELETE FROM forum_bookmarks WHERE post_id = ? AND user_id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, postId);
            ps.setString(2, userId);
            ps.executeUpdate();
        }
    }

    public boolean isBookmarked(String postId, String userId) throws SQLException {
        String query = "SELECT COUNT(*) FROM forum_bookmarks WHERE post_id = ? AND user_id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, postId);
            ps.setString(2, userId);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    public List<ForumPost> getBookmarkedPosts(String userId) throws SQLException {
        List<ForumPost> posts = new ArrayList<>();
        String query = "SELECT p.*, u.name as author_name, u.avatar_url as author_avatar " +
                "FROM forum_posts p " +
                "JOIN users u ON p.user_id = u.id " +
                "JOIN forum_bookmarks b ON b.post_id = p.id " +
                "WHERE b.user_id = ? AND p.is_deleted = FALSE " +
                "ORDER BY b.created_at DESC";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                posts.add(extractPostFromResultSet(rs));
            }
        }
        loadImagesForPosts(posts);
        return posts;
    }

    // =====================================================
    // Notification Methods (for @mention tagging)
    // =====================================================

    /**
     * Find user ID by name (case-insensitive match).
     * Returns null if no user found.
     */
    public String findUserIdByName(String name) throws SQLException {
        String query = "SELECT id FROM users WHERE LOWER(name) = LOWER(?) AND is_active = TRUE";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, name.trim());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("id");
            }
        }
        return null;
    }

    /**
     * Create a mention notification for a user.
     */
    public void createMentionNotification(String recipientUserId, String senderUserId,
                                           String postId, String commentId, String message) throws SQLException {
        String query = "INSERT INTO forum_notifications (recipient_user_id, sender_user_id, post_id, comment_id, type, message) " +
                "VALUES (?, ?, ?, ?, 'mention', ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, recipientUserId);
            ps.setString(2, senderUserId);
            ps.setString(3, postId);
            ps.setString(4, commentId);
            ps.setString(5, message);
            ps.executeUpdate();
        }
    }

    /**
     * Get all unread notifications for a user.
     * Returns list of: [notificationId, senderName, postId, commentId, message, createdAt]
     */
    public List<String[]> getUnreadNotifications(String userId) throws SQLException {
        List<String[]> notifications = new ArrayList<>();
        String query = "SELECT n.id, u.name as sender_name, n.post_id, n.comment_id, n.message, n.created_at " +
                "FROM forum_notifications n " +
                "JOIN users u ON n.sender_user_id = u.id " +
                "WHERE n.recipient_user_id = ? AND n.is_read = FALSE " +
                "ORDER BY n.created_at DESC";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                notifications.add(new String[]{
                    rs.getString("id"),
                    rs.getString("sender_name"),
                    rs.getString("post_id"),
                    rs.getString("comment_id"),
                    rs.getString("message"),
                    rs.getTimestamp("created_at").toString()
                });
            }
        }
        return notifications;
    }

    /**
     * Get count of unread notifications for a user.
     */
    public int getUnreadNotificationCount(String userId) throws SQLException {
        String query = "SELECT COUNT(*) FROM forum_notifications WHERE recipient_user_id = ? AND is_read = FALSE";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /**
     * Mark a single notification as read.
     */
    public void markNotificationRead(String notificationId) throws SQLException {
        String query = "UPDATE forum_notifications SET is_read = TRUE WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, notificationId);
            ps.executeUpdate();
        }
    }

    /**
     * Mark all notifications as read for a user.
     */
    public void markAllNotificationsRead(String userId) throws SQLException {
        String query = "UPDATE forum_notifications SET is_read = TRUE WHERE recipient_user_id = ? AND is_read = FALSE";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, userId);
            ps.executeUpdate();
        }
    }

    /**
     * Get posts where a user has been mentioned (for Activity tab).
     */
    public List<ForumPost> getPostsWhereMentioned(String userId) throws SQLException {
        List<ForumPost> posts = new ArrayList<>();
        String query = "SELECT DISTINCT p.*, u.name as author_name, u.avatar_url as author_avatar " +
                "FROM forum_posts p " +
                "JOIN users u ON p.user_id = u.id " +
                "JOIN forum_notifications n ON n.post_id = p.id " +
                "WHERE n.recipient_user_id = ? AND n.type = 'mention' AND p.is_deleted = FALSE " +
                "ORDER BY n.created_at DESC";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                posts.add(extractPostFromResultSet(rs));
            }
        }
        loadImagesForPosts(posts);
        return posts;
    }
}

