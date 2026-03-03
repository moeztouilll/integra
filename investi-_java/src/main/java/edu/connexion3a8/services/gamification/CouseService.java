package edu.connexion3a8.services.gamification;

import edu.connexion3a8.entities.gamification.Course;
import edu.connexion3a8.entities.gamification.CourseInteraction;
import edu.connexion3a8.entities.gamification.CourseHistory;
import edu.connexion3a8.entities.gamification.CourseReport;
import edu.connexion3a8.interfaces.gamification.ICourse;
import edu.connexion3a8.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CouseService implements ICourse {

    private Connection cnx;

    public CouseService() {
        cnx = MyConnection.getInstance().getCnx();
    }

    @Override
    public void addCourse(Course course) throws SQLException {
        String query = "INSERT INTO course (title, slug, description, content_url, content_type, " +
                "difficulty_level, category, language, estimated_duration, reward_points, " +
                "minimum_points_required, status, visibility, thumbnail_url, published_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";
        
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setString(1, course.getTitle());
        pst.setString(2, course.getSlug());
        pst.setString(3, course.getDescription());
        pst.setString(4, course.getContentUrl());
        pst.setString(5, course.getContentType() != null ? course.getContentType() : "video");
        pst.setString(6, course.getDifficultyLevel() != null ? course.getDifficultyLevel() : "beginner");
        pst.setString(7, course.getCategory());
        pst.setString(8, course.getLanguage());
        pst.setInt(9, course.getEstimatedDuration());
        pst.setInt(10, course.getRewardPoints());
        pst.setInt(11, course.getMinimumPointsRequired());
        pst.setString(12, course.getStatus() != null ? course.getStatus() : "published");
        pst.setString(13, course.getVisibility() != null ? course.getVisibility() : "public");
        pst.setString(14, course.getThumbnailUrl());
        
        pst.executeUpdate();
    }

    @Override
    public void addDraftCourse(Course course) throws SQLException {
        String query = "INSERT INTO course (title, slug, description, content_url, content_type, " +
                "difficulty_level, category, language, estimated_duration, reward_points, " +
                "minimum_points_required, status, visibility, thumbnail_url) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'draft', 'private', ?)";
        
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setString(1, course.getTitle());
        pst.setString(2, course.getSlug());
        pst.setString(3, course.getDescription());
        pst.setString(4, course.getContentUrl());
        pst.setString(5, course.getContentType());
        pst.setString(6, course.getDifficultyLevel());
        pst.setString(7, course.getCategory());
        pst.setString(8, course.getLanguage());
        pst.setInt(9, course.getEstimatedDuration());
        pst.setInt(10, course.getRewardPoints());
        pst.setInt(11, course.getMinimumPointsRequired());
        pst.setString(12, course.getThumbnailUrl());
        
        pst.executeUpdate();
    }

    @Override
    public Course getCourseById(long id) throws SQLException {
        String query = "SELECT * FROM course WHERE id = ?";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setLong(1, id);
        
        ResultSet rs = pst.executeQuery();
        if (rs.next()) {
            return mapResultSetToCourse(rs);
        }
        return null;
    }

    @Override
    public Course getCourseBySlug(String slug) throws SQLException {
        String query = "SELECT * FROM course WHERE slug = ?";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setString(1, slug);
        
        ResultSet rs = pst.executeQuery();
        if (rs.next()) {
            return mapResultSetToCourse(rs);
        }
        return null;
    }

    @Override
    public List<Course> getAllCourses() throws SQLException {
        List<Course> courses = new ArrayList<>();
        String query = "SELECT * FROM course";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(query);
        
        while (rs.next()) {
            courses.add(mapResultSetToCourse(rs));
        }
        return courses;
    }

    @Override
    public List<Course> getCoursesByDifficulty(String difficulty) throws SQLException {
        List<Course> courses = new ArrayList<>();
        String query = "SELECT * FROM course WHERE difficulty_level = ?";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setString(1, difficulty);
        
        ResultSet rs = pst.executeQuery();
        while (rs.next()) {
            courses.add(mapResultSetToCourse(rs));
        }
        return courses;
    }

    @Override
    public List<Course> getCoursesByCategory(String category) throws SQLException {
        List<Course> courses = new ArrayList<>();
        String query = "SELECT * FROM course WHERE category = ?";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setString(1, category);
        
        ResultSet rs = pst.executeQuery();
        while (rs.next()) {
            courses.add(mapResultSetToCourse(rs));
        }
        return courses;
    }

    @Override
    public List<Course> getCoursesByStatus(String status) throws SQLException {
        List<Course> courses = new ArrayList<>();
        String query = "SELECT * FROM course WHERE status = ?";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setString(1, status);
        
        ResultSet rs = pst.executeQuery();
        while (rs.next()) {
            courses.add(mapResultSetToCourse(rs));
        }
        return courses;
    }

    @Override
    public List<Course> getPublicCourses() throws SQLException {
        List<Course> courses = new ArrayList<>();
        String query = "SELECT * FROM course WHERE visibility = 'public' AND status = 'published'";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(query);
        
        while (rs.next()) {
            courses.add(mapResultSetToCourse(rs));
        }
        return courses;
    }

    @Override
    public void updateCourse(Course course, long id) throws SQLException {
        String query = "UPDATE course SET title = ?, slug = ?, description = ?, content_url = ?, " +
                "content_type = ?, difficulty_level = ?, category = ?, language = ?, " +
                "estimated_duration = ?, reward_points = ?, minimum_points_required = ?, status = ?, visibility = ?, " +
                "thumbnail_url = ? WHERE id = ?";
        
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setString(1, course.getTitle());
        pst.setString(2, course.getSlug());
        pst.setString(3, course.getDescription());
        pst.setString(4, course.getContentUrl());
        pst.setString(5, course.getContentType());
        pst.setString(6, course.getDifficultyLevel());
        pst.setString(7, course.getCategory());
        pst.setString(8, course.getLanguage());
        pst.setInt(9, course.getEstimatedDuration());
        pst.setInt(10, course.getRewardPoints());
        pst.setInt(11, course.getMinimumPointsRequired());
        pst.setString(12, course.getStatus());
        pst.setString(13, course.getVisibility());
        pst.setString(14, course.getThumbnailUrl());
        pst.setLong(15, id);
        
        pst.executeUpdate();
    }

    @Override
    public void updateCourseStatus(long id, String status) throws SQLException {
        String query = "UPDATE course SET status = ? WHERE id = ?";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setString(1, status);
        pst.setLong(2, id);
        pst.executeUpdate();
    }

    @Override
    public void publishCourse(long id) throws SQLException {
        String query = "UPDATE course SET status = 'published', visibility = 'public', " +
                "published_at = NOW() WHERE id = ?";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setLong(1, id);
        pst.executeUpdate();
    }

    @Override
    public void deleteCourse(long id) throws SQLException {
        String query = "DELETE FROM course WHERE id = ?";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setLong(1, id);
        pst.executeUpdate();
    }

    @Override
    public void archiveCourse(long id) throws SQLException {
        String query = "UPDATE course SET status = 'archived', visibility = 'private' WHERE id = ?";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setLong(1, id);
        pst.executeUpdate();
    }

    private Course mapResultSetToCourse(ResultSet rs) throws SQLException {
        Course course = new Course();
        course.setId(rs.getLong("id"));
        course.setTitle(rs.getString("title"));
        course.setSlug(rs.getString("slug"));
        course.setDescription(rs.getString("description"));
        course.setContentUrl(rs.getString("content_url"));
        course.setContentType(rs.getString("content_type"));
        course.setDifficultyLevel(rs.getString("difficulty_level"));
        course.setCategory(rs.getString("category"));
        course.setLanguage(rs.getString("language"));
        course.setEstimatedDuration(rs.getInt("estimated_duration"));
        course.setRewardPoints(rs.getInt("reward_points"));
        course.setMinimumPointsRequired(rs.getInt("minimum_points_required"));
        course.setStatus(rs.getString("status"));
        course.setVisibility(rs.getString("visibility"));
        course.setThumbnailUrl(rs.getString("thumbnail_url"));
        return course;
    }

    /* ===== COURSE INTERACTIONS ===== */

    public void addCourseInteraction(CourseInteraction interaction) throws SQLException {
        // Remove existing interaction of same type
        String deleteQuery = "DELETE FROM course_interactions WHERE user_id = ? AND course_id = ? AND interaction_type = ?";
        PreparedStatement deletePst = cnx.prepareStatement(deleteQuery);
        deletePst.setString(1, interaction.getUserId());
        deletePst.setLong(2, interaction.getCourseId());
        deletePst.setString(3, interaction.getInteractionType());
        deletePst.executeUpdate();

        // Add new interaction
        String query = "INSERT INTO course_interactions (user_id, course_id, interaction_type, report_reason) VALUES (?, ?, ?, ?)";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setString(1, interaction.getUserId());
        pst.setLong(2, interaction.getCourseId());
        pst.setString(3, interaction.getInteractionType());
        pst.setString(4, interaction.getReportReason());
        pst.executeUpdate();
    }

    public void removeCourseInteraction(String userId, long courseId, String interactionType) throws SQLException {
        String query = "DELETE FROM course_interactions WHERE user_id = ? AND course_id = ? AND interaction_type = ?";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setString(1, userId);
        pst.setLong(2, courseId);
        pst.setString(3, interactionType);
        pst.executeUpdate();
    }

    public int getCourseInteractionCount(long courseId, String interactionType) throws SQLException {
        String query = "SELECT COUNT(*) FROM course_interactions WHERE course_id = ? AND interaction_type = ?";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setLong(1, courseId);
        pst.setString(2, interactionType);
        
        ResultSet rs = pst.executeQuery();
        if (rs.next()) {
            return rs.getInt(1);
        }
        return 0;
    }

    public boolean hasUserInteracted(String userId, long courseId, String interactionType) throws SQLException {
        String query = "SELECT COUNT(*) FROM course_interactions WHERE user_id = ? AND course_id = ? AND interaction_type = ?";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setString(1, userId);
        pst.setLong(2, courseId);
        pst.setString(3, interactionType);
        
        ResultSet rs = pst.executeQuery();
        if (rs.next()) {
            return rs.getInt(1) > 0;
        }
        return false;
    }

    /* ===== COURSE-QUIZ LINKING ===== */

    public void linkQuizToCourse(long courseId, long quizId, int order, boolean isRequired) throws SQLException {
        String query = "INSERT INTO course_quizzes (course_id, quiz_id, quiz_order, is_required) VALUES (?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE quiz_order = ?, is_required = ?";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setLong(1, courseId);
        pst.setLong(2, quizId);
        pst.setInt(3, order);
        pst.setBoolean(4, isRequired);
        pst.setInt(5, order);
        pst.setBoolean(6, isRequired);
        pst.executeUpdate();
    }

    public void unlinkQuizFromCourse(long courseId, long quizId) throws SQLException {
        String query = "DELETE FROM course_quizzes WHERE course_id = ? AND quiz_id = ?";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setLong(1, courseId);
        pst.setLong(2, quizId);
        pst.executeUpdate();
    }

    public List<Long> getQuizIdsForCourse(long courseId) throws SQLException {
        List<Long> quizIds = new ArrayList<>();
        String query = "SELECT quiz_id FROM course_quizzes WHERE course_id = ? ORDER BY quiz_order ASC";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setLong(1, courseId);
        
        ResultSet rs = pst.executeQuery();
        while (rs.next()) {
            quizIds.add(rs.getLong("quiz_id"));
        }
        return quizIds;
    }

    /* ===== COURSE HISTORY ===== */

    public void addCourseVisit(String userId, long courseId) throws SQLException {
        // Check if user has visited this course before
        String checkQuery = "SELECT id FROM course_history WHERE user_id = ? AND course_id = ?";
        PreparedStatement checkPst = cnx.prepareStatement(checkQuery);
        checkPst.setString(1, userId);
        checkPst.setLong(2, courseId);
        ResultSet rs = checkPst.executeQuery();
        
        if (rs.next()) {
            // Update existing record with new visit time
            String updateQuery = "UPDATE course_history SET visited_at = NOW() WHERE user_id = ? AND course_id = ?";
            PreparedStatement updatePst = cnx.prepareStatement(updateQuery);
            updatePst.setString(1, userId);
            updatePst.setLong(2, courseId);
            updatePst.executeUpdate();
        } else {
            // Insert new visit record
            String insertQuery = "INSERT INTO course_history (user_id, course_id, visited_at) VALUES (?, ?, NOW())";
            PreparedStatement insertPst = cnx.prepareStatement(insertQuery);
            insertPst.setString(1, userId);
            insertPst.setLong(2, courseId);
            insertPst.executeUpdate();
        }
    }

    public List<CourseHistory> getUserCourseHistory(String userId, int limit) throws SQLException {
        List<CourseHistory> history = new ArrayList<>();
        String query = "SELECT ch.*, c.* FROM course_history ch " +
                      "JOIN course c ON ch.course_id = c.id " +
                      "WHERE ch.user_id = ? " +
                      "ORDER BY ch.visited_at DESC " +
                      "LIMIT ?";
        
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setString(1, userId);
        pst.setInt(2, limit);
        
        ResultSet rs = pst.executeQuery();
        while (rs.next()) {
            CourseHistory historyItem = new CourseHistory();
            historyItem.setId(rs.getLong("ch.id"));
            historyItem.setUserId(rs.getString("ch.user_id"));
            historyItem.setCourseId(rs.getLong("ch.course_id"));
            historyItem.setVisitedAt(rs.getTimestamp("ch.visited_at"));
            historyItem.setLastPosition(rs.getInt("ch.last_position"));
            historyItem.setCompletionPercentage(rs.getInt("ch.completion_percentage"));
            
            // Map course data
            Course course = new Course();
            course.setId(rs.getLong("c.id"));
            course.setTitle(rs.getString("c.title"));
            course.setSlug(rs.getString("c.slug"));
            course.setDescription(rs.getString("c.description"));
            course.setContentUrl(rs.getString("c.content_url"));
            course.setContentType(rs.getString("c.content_type"));
            course.setDifficultyLevel(rs.getString("c.difficulty_level"));
            course.setCategory(rs.getString("c.category"));
            course.setLanguage(rs.getString("c.language"));
            course.setEstimatedDuration(rs.getInt("c.estimated_duration"));
            course.setRewardPoints(rs.getInt("c.reward_points"));
            course.setMinimumPointsRequired(rs.getInt("c.minimum_points_required"));
            course.setStatus(rs.getString("c.status"));
            course.setVisibility(rs.getString("c.visibility"));
            course.setThumbnailUrl(rs.getString("c.thumbnail_url"));
            
            historyItem.setCourse(course);
            history.add(historyItem);
        }
        
        return history;
    }

    public void updateCourseProgress(String userId, long courseId, int completionPercentage) throws SQLException {
        String query = "UPDATE course_history SET completion_percentage = ? WHERE user_id = ? AND course_id = ?";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setInt(1, completionPercentage);
        pst.setString(2, userId);
        pst.setLong(3, courseId);
        pst.executeUpdate();
    }

    public int getCourseVisitCount(String userId, long courseId) throws SQLException {
        String query = "SELECT COUNT(*) as count FROM course_history WHERE user_id = ? AND course_id = ?";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setString(1, userId);
        pst.setLong(2, courseId);
        
        ResultSet rs = pst.executeQuery();
        if (rs.next()) {
            return rs.getInt("count");
        }
        return 0;
    }

    public void clearUserHistory(String userId) throws SQLException {
        String query = "DELETE FROM course_history WHERE user_id = ?";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setString(1, userId);
        pst.executeUpdate();
    }
    
    /* ===== COURSE REPORTS ===== */
    
    public void submitCourseReport(CourseReport report) throws SQLException {
        String query = "INSERT INTO course_reports (course_id, user_id, report_reason, description, status) " +
                      "VALUES (?, ?, ?, ?, 'pending')";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setLong(1, report.getCourseId());
        pst.setString(2, report.getUserId());
        pst.setString(3, report.getReportReason());
        pst.setString(4, report.getDescription());
        pst.executeUpdate();
    }
    
    public List<CourseReport> getAllReports() throws SQLException {
        List<CourseReport> reports = new ArrayList<>();
        String query = "SELECT cr.*, c.title as course_name " +
                      "FROM course_reports cr " +
                      "JOIN course c ON cr.course_id = c.id " +
                      "ORDER BY cr.created_at DESC";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(query);
        
        while (rs.next()) {
            CourseReport report = mapResultSetToReport(rs);
            report.setCourseName(rs.getString("course_name"));
            reports.add(report);
        }
        return reports;
    }
    
    public List<CourseReport> getReportsByStatus(String status) throws SQLException {
        List<CourseReport> reports = new ArrayList<>();
        String query = "SELECT cr.*, c.title as course_name " +
                      "FROM course_reports cr " +
                      "JOIN course c ON cr.course_id = c.id " +
                      "WHERE cr.status = ? " +
                      "ORDER BY cr.created_at DESC";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setString(1, status);
        ResultSet rs = pst.executeQuery();
        
        while (rs.next()) {
            CourseReport report = mapResultSetToReport(rs);
            report.setCourseName(rs.getString("course_name"));
            reports.add(report);
        }
        return reports;
    }
    
    public CourseReport getReportById(long id) throws SQLException {
        String query = "SELECT cr.*, c.title as course_name " +
                      "FROM course_reports cr " +
                      "JOIN course c ON cr.course_id = c.id " +
                      "WHERE cr.id = ?";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setLong(1, id);
        ResultSet rs = pst.executeQuery();
        
        if (rs.next()) {
            CourseReport report = mapResultSetToReport(rs);
            report.setCourseName(rs.getString("course_name"));
            return report;
        }
        return null;
    }
    
    public void updateReportStatus(long reportId, String status) throws SQLException {
        String query = "UPDATE course_reports SET status = ? WHERE id = ?";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setString(1, status);
        pst.setLong(2, reportId);
        pst.executeUpdate();
    }
    
    public void deleteReport(long reportId) throws SQLException {
        String query = "DELETE FROM course_reports WHERE id = ?";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setLong(1, reportId);
        pst.executeUpdate();
    }
    
    public int getReportCountByCourse(long courseId) throws SQLException {
        String query = "SELECT COUNT(*) as count FROM course_reports WHERE course_id = ?";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setLong(1, courseId);
        ResultSet rs = pst.executeQuery();
        
        if (rs.next()) {
            return rs.getInt("count");
        }
        return 0;
    }
    
    /* ===== COURSE ANALYTICS ===== */
    
    public int getCourseViewCount(long courseId) throws SQLException {
        String query = "SELECT COUNT(*) as count FROM course_history WHERE course_id = ?";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setLong(1, courseId);
        ResultSet rs = pst.executeQuery();
        
        if (rs.next()) {
            return rs.getInt("count");
        }
        return 0;
    }
    
    public int getCourseLikeCount(long courseId) throws SQLException {
        String query = "SELECT COUNT(*) as count FROM course_interactions WHERE course_id = ? AND interaction_type = 'like'";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setLong(1, courseId);
        ResultSet rs = pst.executeQuery();
        
        if (rs.next()) {
            return rs.getInt("count");
        }
        return 0;
    }
    
    public int getCourseDislikeCount(long courseId) throws SQLException {
        String query = "SELECT COUNT(*) as count FROM course_interactions WHERE course_id = ? AND interaction_type = 'dislike'";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setLong(1, courseId);
        ResultSet rs = pst.executeQuery();
        
        if (rs.next()) {
            return rs.getInt("count");
        }
        return 0;
    }
    
    private CourseReport mapResultSetToReport(ResultSet rs) throws SQLException {
        CourseReport report = new CourseReport();
        report.setId(rs.getLong("id"));
        report.setCourseId(rs.getLong("course_id"));
        report.setUserId(rs.getString("user_id"));
        report.setReportReason(rs.getString("report_reason"));
        report.setDescription(rs.getString("description"));
        report.setStatus(rs.getString("status"));
        report.setCreatedAt(rs.getTimestamp("created_at"));
        report.setUpdatedAt(rs.getTimestamp("updated_at"));
        return report;
    }
}
