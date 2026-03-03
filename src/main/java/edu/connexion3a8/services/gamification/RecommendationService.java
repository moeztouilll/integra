package edu.connexion3a8.services.gamification;

import edu.connexion3a8.entities.gamification.Course;
import edu.connexion3a8.tools.MyConnection;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Recommendation engine for suggesting courses to users
 * Uses collaborative filtering and content-based filtering
 */
public class RecommendationService {
    
    private Connection cnx;
    private CouseService courseService;
    
    public RecommendationService() {
        cnx = MyConnection.getInstance().getCnx();
        courseService = new CouseService();
    }
    
    /**
     * Get personalized course recommendations for a user
     * Combines multiple recommendation strategies
     */
    public List<Course> getRecommendationsForUser(String userId, int limit) throws SQLException {
        System.out.println("=== Getting personalized recommendations for user " + userId + " ===");
        
        Set<Long> recommendedCourseIds = new LinkedHashSet<>();
        
        // Strategy 1: Based on user's course history (content-based)
        System.out.println("Strategy 1: Content-based filtering...");
        List<Long> historyBased = getRecommendationsBasedOnHistory(userId, limit);
        System.out.println("  Found " + historyBased.size() + " history-based recommendations");
        recommendedCourseIds.addAll(historyBased);
        
        // Strategy 2: Based on similar users (collaborative filtering)
        System.out.println("Strategy 2: Collaborative filtering...");
        List<Long> collaborativeBased = getCollaborativeRecommendations(userId, limit);
        System.out.println("  Found " + collaborativeBased.size() + " collaborative recommendations");
        recommendedCourseIds.addAll(collaborativeBased);
        
        // Strategy 3: Popular courses in user's preferred categories
        System.out.println("Strategy 3: Popular in preferred categories...");
        List<Long> popularBased = getPopularInPreferredCategories(userId, limit);
        System.out.println("  Found " + popularBased.size() + " popular category recommendations");
        recommendedCourseIds.addAll(popularBased);
        
        // Strategy 4: Trending courses (most viewed recently)
        System.out.println("Strategy 4: Trending courses...");
        List<Long> trendingBased = getTrendingCourses(limit);
        System.out.println("  Found " + trendingBased.size() + " trending recommendations");
        recommendedCourseIds.addAll(trendingBased);
        
        System.out.println("Total unique course IDs: " + recommendedCourseIds.size());
        
        // NOTE: We don't remove viewed courses - users can revisit courses!
        // Only remove if we had a "completed" flag in the future
        
        // Convert IDs to Course objects
        List<Course> recommendations = new ArrayList<>();
        for (Long courseId : recommendedCourseIds) {
            if (recommendations.size() >= limit) break;
            Course course = courseService.getCourseById(courseId);
            if (course != null) {
                recommendations.add(course);
                System.out.println("  Added: " + course.getTitle());
            } else {
                System.out.println("  ⚠️ Course ID " + courseId + " not found");
            }
        }
        
        System.out.println("Final recommendations count: " + recommendations.size());
        
        // Fallback: if no personalized recommendations, use new user recommendations
        if (recommendations.isEmpty()) {
            System.out.println("⚠️ No personalized recommendations found, falling back to popular courses...");
            return getRecommendationsForNewUser(limit);
        }
        
        return recommendations;
    }
    
    /**
     * Content-based filtering: Recommend courses similar to what user has viewed
     */
    private List<Long> getRecommendationsBasedOnHistory(String userId, int limit) throws SQLException {
        // Get user's most viewed categories and difficulty levels
        String query = "SELECT c.category, c.difficulty_level, COUNT(*) as view_count " +
                      "FROM course_history ch " +
                      "JOIN course c ON ch.course_id = c.id " +
                      "WHERE ch.user_id = ? " +
                      "GROUP BY c.category, c.difficulty_level " +
                      "ORDER BY view_count DESC " +
                      "LIMIT 3";
        
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setString(1, userId);
        ResultSet rs = pst.executeQuery();
        
        List<Long> recommendations = new ArrayList<>();
        
        while (rs.next()) {
            String category = rs.getString("category");
            String difficulty = rs.getString("difficulty_level");
            
            // Find courses in same category and difficulty
            String courseQuery = "SELECT id FROM course " +
                               "WHERE category = ? AND difficulty_level = ? " +
                               "AND status = 'published' " +
                               "ORDER BY created_at DESC " +
                               "LIMIT ?";
            
            PreparedStatement coursePst = cnx.prepareStatement(courseQuery);
            coursePst.setString(1, category);
            coursePst.setString(2, difficulty);
            coursePst.setInt(3, limit);
            
            ResultSet courseRs = coursePst.executeQuery();
            while (courseRs.next()) {
                recommendations.add(courseRs.getLong("id"));
            }
        }
        
        return recommendations;
    }
    
    /**
     * Collaborative filtering: Find similar users and recommend their courses
     */
    private List<Long> getCollaborativeRecommendations(String userId, int limit) throws SQLException {
        // Find users with similar course viewing patterns
        String query = "SELECT ch2.user_id, COUNT(DISTINCT ch2.course_id) as common_courses " +
                      "FROM course_history ch1 " +
                      "JOIN course_history ch2 ON ch1.course_id = ch2.course_id " +
                      "WHERE ch1.user_id = ? AND ch2.user_id != ? " +
                      "GROUP BY ch2.user_id " +
                      "ORDER BY common_courses DESC " +
                      "LIMIT 5";
        
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setString(1, userId);
        pst.setString(2, userId);
        ResultSet rs = pst.executeQuery();
        
        List<String> similarUsers = new ArrayList<>();
        while (rs.next()) {
            similarUsers.add(rs.getString("user_id"));
        }
        
        if (similarUsers.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Get courses viewed by similar users
        String placeholders = String.join(",", Collections.nCopies(similarUsers.size(), "?"));
        String courseQuery = "SELECT course_id, COUNT(*) as view_count " +
                           "FROM course_history " +
                           "WHERE user_id IN (" + placeholders + ") " +
                           "GROUP BY course_id " +
                           "ORDER BY view_count DESC " +
                           "LIMIT ?";
        
        PreparedStatement coursePst = cnx.prepareStatement(courseQuery);
        for (int i = 0; i < similarUsers.size(); i++) {
            coursePst.setString(i + 1, similarUsers.get(i));
        }
        coursePst.setInt(similarUsers.size() + 1, limit);
        
        ResultSet courseRs = coursePst.executeQuery();
        List<Long> recommendations = new ArrayList<>();
        while (courseRs.next()) {
            recommendations.add(courseRs.getLong("course_id"));
        }
        
        return recommendations;
    }
    
    /**
     * Get popular courses in user's preferred categories
     */
    private List<Long> getPopularInPreferredCategories(String userId, int limit) throws SQLException {
        // Get user's top categories
        String categoryQuery = "SELECT c.category, COUNT(*) as view_count " +
                             "FROM course_history ch " +
                             "JOIN course c ON ch.course_id = c.id " +
                             "WHERE ch.user_id = ? " +
                             "GROUP BY c.category " +
                             "ORDER BY view_count DESC " +
                             "LIMIT 2";
        
        PreparedStatement pst = cnx.prepareStatement(categoryQuery);
        pst.setString(1, userId);
        ResultSet rs = pst.executeQuery();
        
        List<String> preferredCategories = new ArrayList<>();
        while (rs.next()) {
            preferredCategories.add(rs.getString("category"));
        }
        
        if (preferredCategories.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Get popular courses in these categories
        String placeholders = String.join(",", Collections.nCopies(preferredCategories.size(), "?"));
        String courseQuery = "SELECT c.id, COUNT(ch.id) as popularity " +
                           "FROM course c " +
                           "LEFT JOIN course_history ch ON c.id = ch.course_id " +
                           "WHERE c.category IN (" + placeholders + ") " +
                           "AND c.status = 'published' " +
                           "GROUP BY c.id " +
                           "ORDER BY popularity DESC " +
                           "LIMIT ?";
        
        PreparedStatement coursePst = cnx.prepareStatement(courseQuery);
        for (int i = 0; i < preferredCategories.size(); i++) {
            coursePst.setString(i + 1, preferredCategories.get(i));
        }
        coursePst.setInt(preferredCategories.size() + 1, limit);
        
        ResultSet courseRs = coursePst.executeQuery();
        List<Long> recommendations = new ArrayList<>();
        while (courseRs.next()) {
            recommendations.add(courseRs.getLong("id"));
        }
        
        return recommendations;
    }
    
    /**
     * Get trending courses (most viewed in last 30 days)
     */
    private List<Long> getTrendingCourses(int limit) throws SQLException {
        String query = "SELECT course_id, COUNT(*) as recent_views " +
                      "FROM course_history " +
                      "WHERE visited_at >= DATE_SUB(NOW(), INTERVAL 30 DAY) " +
                      "GROUP BY course_id " +
                      "ORDER BY recent_views DESC " +
                      "LIMIT ?";
        
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setInt(1, limit);
        ResultSet rs = pst.executeQuery();
        
        List<Long> trending = new ArrayList<>();
        while (rs.next()) {
            trending.add(rs.getLong("course_id"));
        }
        
        return trending;
    }
    
    /**
     * Get courses user has already completed
     */
    private Set<Long> getUserCompletedCourses(String userId) throws SQLException {
        String query = "SELECT DISTINCT course_id FROM course_history WHERE user_id = ?";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setString(1, userId);
        ResultSet rs = pst.executeQuery();
        
        Set<Long> completed = new HashSet<>();
        while (rs.next()) {
            long courseId = rs.getLong("course_id");
            completed.add(courseId);
            System.out.println("  User has viewed course ID: " + courseId);
        }
        
        return completed;
    }
    
    /**
     * Get "Students who took this also took" recommendations
     */
    public List<Course> getSimilarCourseRecommendations(long courseId, int limit) throws SQLException {
        String query = "SELECT ch2.course_id, COUNT(*) as co_occurrence " +
                      "FROM course_history ch1 " +
                      "JOIN course_history ch2 ON ch1.user_id = ch2.user_id " +
                      "WHERE ch1.course_id = ? AND ch2.course_id != ? " +
                      "GROUP BY ch2.course_id " +
                      "ORDER BY co_occurrence DESC " +
                      "LIMIT ?";
        
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setLong(1, courseId);
        pst.setLong(2, courseId);
        pst.setInt(3, limit);
        
        ResultSet rs = pst.executeQuery();
        List<Course> recommendations = new ArrayList<>();
        
        while (rs.next()) {
            long recCourseId = rs.getLong("course_id");
            Course course = courseService.getCourseById(recCourseId);
            if (course != null) {
                recommendations.add(course);
            }
        }
        
        return recommendations;
    }
    
    /**
     * Get recommended courses for beginners (no history)
     */
    public List<Course> getRecommendationsForNewUser(int limit) throws SQLException {
        System.out.println("=== Getting recommendations for new user ===");
        
        // First try: most popular published courses
        String query = "SELECT c.id, COUNT(ch.id) as popularity, " +
                      "COUNT(DISTINCT CASE WHEN ci.interaction_type = 'like' THEN ci.id END) as likes " +
                      "FROM course c " +
                      "LEFT JOIN course_history ch ON c.id = ch.course_id " +
                      "LEFT JOIN course_interactions ci ON c.id = ci.course_id " +
                      "WHERE c.status = 'published' " +
                      "GROUP BY c.id " +
                      "ORDER BY popularity DESC, likes DESC " +
                      "LIMIT ?";
        
        System.out.println("Executing query for published courses...");
        
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setInt(1, limit);
        ResultSet rs = pst.executeQuery();
        
        List<Course> recommendations = new ArrayList<>();
        while (rs.next()) {
            long courseId = rs.getLong("id");
            int popularity = rs.getInt("popularity");
            int likes = rs.getInt("likes");
            System.out.println("Found course ID: " + courseId + " (popularity: " + popularity + ", likes: " + likes + ")");
            
            Course course = courseService.getCourseById(courseId);
            if (course != null) {
                recommendations.add(course);
                System.out.println("  Added: " + course.getTitle());
            } else {
                System.out.println("  ⚠️ Course not found in courseService");
            }
        }
        
        // Fallback: if no published courses, get any courses
        if (recommendations.isEmpty()) {
            System.out.println("No published courses found, trying all courses...");
            String fallbackQuery = "SELECT id FROM course ORDER BY created_at DESC LIMIT ?";
            PreparedStatement fallbackPst = cnx.prepareStatement(fallbackQuery);
            fallbackPst.setInt(1, limit);
            ResultSet fallbackRs = fallbackPst.executeQuery();
            
            while (fallbackRs.next()) {
                long courseId = fallbackRs.getLong("id");
                Course course = courseService.getCourseById(courseId);
                if (course != null) {
                    recommendations.add(course);
                    System.out.println("  Fallback added: " + course.getTitle());
                }
            }
        }
        
        System.out.println("Total recommendations: " + recommendations.size());
        return recommendations;
    }
    
    /**
     * Calculate recommendation score for debugging
     */
    public Map<String, Double> getRecommendationScores(String userId, long courseId) throws SQLException {
        Map<String, Double> scores = new HashMap<>();
        
        // Category match score
        scores.put("category_match", calculateCategoryMatchScore(userId, courseId));
        
        // Difficulty match score
        scores.put("difficulty_match", calculateDifficultyMatchScore(userId, courseId));
        
        // Popularity score
        scores.put("popularity", calculatePopularityScore(courseId));
        
        // Collaborative score
        scores.put("collaborative", calculateCollaborativeScore(userId, courseId));
        
        return scores;
    }
    
    private double calculateCategoryMatchScore(String userId, long courseId) throws SQLException {
        // Implementation for category matching
        return 0.0; // Placeholder
    }
    
    private double calculateDifficultyMatchScore(String userId, long courseId) throws SQLException {
        // Implementation for difficulty matching
        return 0.0; // Placeholder
    }
    
    private double calculatePopularityScore(long courseId) throws SQLException {
        String query = "SELECT COUNT(*) as views FROM course_history WHERE course_id = ?";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setLong(1, courseId);
        ResultSet rs = pst.executeQuery();
        
        if (rs.next()) {
            return rs.getInt("views") / 100.0; // Normalize
        }
        return 0.0;
    }
    
    private double calculateCollaborativeScore(String userId, long courseId) throws SQLException {
        // Implementation for collaborative filtering score
        return 0.0; // Placeholder
    }
}
