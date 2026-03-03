package edu.connexion3a8.services.gamification;

import edu.connexion3a8.entities.gamification.*;
import edu.connexion3a8.interfaces.gamification.IGamification;
import edu.connexion3a8.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GamificationService implements IGamification {

    private Connection cnx;

    public GamificationService() {
        cnx = MyConnection.getInstance().getCnx();
    }

    public Connection getCnx() {
        return cnx;
    }

    /* ===== POINTS MANAGEMENT ===== */

    @Override
    public void addPoints(String userId, int points, String transactionType, String description) throws SQLException {
        cnx.setAutoCommit(false);
        try {
            // Log transaction
            String transQuery = "INSERT INTO point_transactions (user_id, points, transaction_type, description) VALUES (?, ?, ?, ?)";
            PreparedStatement transPst = cnx.prepareStatement(transQuery);
            transPst.setString(1, userId);
            transPst.setInt(2, points);
            transPst.setString(3, transactionType);
            transPst.setString(4, description);
            transPst.executeUpdate();

            // Update user points
            String updateQuery = "UPDATE user_points SET points = points + ?, total_earned_points = total_earned_points + ? WHERE user_id = ?";
            PreparedStatement updatePst = cnx.prepareStatement(updateQuery);
            updatePst.setInt(1, points);
            updatePst.setInt(2, points);
            updatePst.setString(3, userId);
            updatePst.executeUpdate();

            // Update level based on points
            updateUserLevel(userId);

            cnx.commit();
        } catch (SQLException e) {
            cnx.rollback();
            throw e;
        } finally { cnx.setAutoCommit(true); }
    }

    @Override
    public void deductPoints(String userId, int points, String transactionType, String description) throws SQLException {
         cnx.setAutoCommit(false);
        try {
            // Check if user has enough points
            UserPoints userPoints = getUserPoints(userId);
            if (userPoints.getPoints() < points) {
                throw new SQLException("Insufficient points");
            }

            // Log transaction
            String transQuery = "INSERT INTO point_transactions (user_id, points, transaction_type, description) VALUES (?, ?, ?, ?)";
            PreparedStatement transPst = cnx.prepareStatement(transQuery);
            transPst.setString(1, userId);
            transPst.setInt(2, -points);
            transPst.setString(3, transactionType);
            transPst.setString(4, description);
            transPst.executeUpdate();

            // Update user points
            String updateQuery = "UPDATE user_points SET points = points - ? WHERE user_id = ?";
            PreparedStatement updatePst = cnx.prepareStatement(updateQuery);
            updatePst.setInt(1, points);
            updatePst.setString(2, userId);
            updatePst.executeUpdate();

            cnx.commit();
        } catch (SQLException e) {
            cnx.rollback();
            throw e;
        } finally { cnx.setAutoCommit(true); }
    }

    @Override
    public UserPoints getUserPoints(String userId) throws SQLException {
        String query = "SELECT * FROM user_points WHERE user_id = ?";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setString(1, userId);
        
        ResultSet rs = pst.executeQuery();
        if (rs.next()) {
            return mapResultSetToUserPoints(rs);
        }
        
        // If no record exists, create one
        UserPoints newUserPoints = new UserPoints(userId);
        String insertQuery = "INSERT INTO user_points (user_id, points, level, total_earned_points) VALUES (?, 0, 1, 0)";
        PreparedStatement insertPst = cnx.prepareStatement(insertQuery);
        insertPst.setString(1, userId);
        insertPst.executeUpdate();
        
        return newUserPoints;
    }

    @Override
    public List<PointTransaction> getUserPointHistory(String userId) throws SQLException {
        List<PointTransaction> transactions = new ArrayList<>();
        String query = "SELECT * FROM point_transactions WHERE user_id = ? ORDER BY created_at DESC";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setString(1, userId);
        
        ResultSet rs = pst.executeQuery();
        while (rs.next()) {
            transactions.add(mapResultSetToPointTransaction(rs));
        }
        return transactions;
    }

    /* ===== BADGE MANAGEMENT ===== */

    @Override
    public void awardBadge(String userId, long badgeId) throws SQLException {
        // Check if user already has this badge
        if (hasUserEarnedBadge(userId, badgeId)) {
            return;
        }

        String query = "INSERT INTO user_badges (user_id, badge_id) VALUES (?, ?)";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setString(1, userId);
        pst.setLong(2, badgeId);
        pst.executeUpdate();
    }

    @Override
    public List<Badge> getUserBadges(String userId) throws SQLException {
        List<Badge> badges = new ArrayList<>();
        String query = "SELECT b.* FROM badges b " +
                "INNER JOIN user_badges ub ON b.id = ub.badge_id " +
                "WHERE ub.user_id = ? ORDER BY ub.earned_at DESC";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setString(1, userId);
        
        ResultSet rs = pst.executeQuery();
        while (rs.next()) {
            badges.add(mapResultSetToBadge(rs));
        }
        return badges;
    }

    @Override
    public List<Badge> getAllBadges() throws SQLException {
        List<Badge> badges = new ArrayList<>();
        String query = "SELECT * FROM badges ORDER BY points_required ASC";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(query);
        
        while (rs.next()) {
            badges.add(mapResultSetToBadge(rs));
        }
        return badges;
    }

    @Override
    public boolean hasUserEarnedBadge(String userId, long badgeId) throws SQLException {
        String query = "SELECT COUNT(*) FROM user_badges WHERE user_id = ? AND badge_id = ?";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setString(1, userId);
        pst.setLong(2, badgeId);
        
        ResultSet rs = pst.executeQuery();
        if (rs.next()) {
            return rs.getInt(1) > 0;
        }
        return false;
    }

    /* ===== QUIZ MANAGEMENT ===== */

    @Override
    public void addQuiz(Quiz quiz) throws SQLException {
        String query = "INSERT INTO quizzes (title, description, points_reward, question_count, " +
                "difficulty_level, category, time_limit, passing_score, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setString(1, quiz.getTitle());
        pst.setString(2, quiz.getDescription());
        pst.setInt(3, quiz.getPointsReward());
        pst.setInt(4, quiz.getQuestionCount());
        pst.setString(5, quiz.getDifficultyLevel() != null ? quiz.getDifficultyLevel() : "beginner");
        pst.setString(6, quiz.getCategory());
        pst.setInt(7, quiz.getTimeLimit());
        pst.setInt(8, quiz.getPassingScore() > 0 ? quiz.getPassingScore() : 70);
        pst.setString(9, quiz.getStatus() != null ? quiz.getStatus() : "active");
        
        pst.executeUpdate();
    }

    @Override
    public void updateQuiz(Quiz quiz, long id) throws SQLException {
        String query = "UPDATE quizzes SET title = ?, description = ?, points_reward = ?, " +
                "question_count = ?, difficulty_level = ?, category = ?, time_limit = ?, " +
                "passing_score = ?, status = ? WHERE id = ?";
        
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setString(1, quiz.getTitle());
        pst.setString(2, quiz.getDescription());
        pst.setInt(3, quiz.getPointsReward());
        pst.setInt(4, quiz.getQuestionCount());
        pst.setString(5, quiz.getDifficultyLevel());
        pst.setString(6, quiz.getCategory());
        pst.setInt(7, quiz.getTimeLimit());
        pst.setInt(8, quiz.getPassingScore());
        pst.setString(9, quiz.getStatus());
        pst.setLong(10, id);
        
        pst.executeUpdate();
    }

    @Override
    public void deleteQuiz(long id) throws SQLException {
        String query = "DELETE FROM quizzes WHERE id = ?";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setLong(1, id);
        pst.executeUpdate();
    }

    @Override
    public Quiz getQuizById(long id) throws SQLException {
        String query = "SELECT * FROM quizzes WHERE id = ?";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setLong(1, id);
        
        ResultSet rs = pst.executeQuery();
        if (rs.next()) {
            return mapResultSetToQuiz(rs);
        }
        return null;
    }

    @Override
    public void completeQuiz(String userId, long quizId, int score, int timeTaken) throws SQLException {
         cnx.setAutoCommit(false);
        try {
            // Get quiz details
            Quiz quiz = getQuizById(quizId);
            if (quiz == null) {
                throw new SQLException("Quiz not found");
            }

            boolean passed = score >= quiz.getPassingScore();

            // Record quiz completion
            String query = "INSERT INTO user_quizzes (user_id, quiz_id, score, time_taken, passed) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement pst = cnx.prepareStatement(query);
            pst.setString(1, userId);
            pst.setLong(2, quizId);
            pst.setInt(3, score);
            pst.setInt(4, timeTaken);
            pst.setBoolean(5, passed);
            pst.executeUpdate();

            // Award points if passed (inline to avoid nested transaction)
            if (passed) {
                // Log transaction
                String transQuery = "INSERT INTO point_transactions (user_id, points, transaction_type, description) VALUES (?, ?, ?, ?)";
                PreparedStatement transPst = cnx.prepareStatement(transQuery);
                transPst.setString(1, userId);
                transPst.setInt(2, quiz.getPointsReward());
                transPst.setString(3, "QUIZ_COMPLETION");
                transPst.setString(4, "Completed quiz: " + quiz.getTitle());
                transPst.executeUpdate();

                // Update user points
                String updateQuery = "UPDATE user_points SET points = points + ?, total_earned_points = total_earned_points + ? WHERE user_id = ?";
                PreparedStatement updatePst = cnx.prepareStatement(updateQuery);
                updatePst.setInt(1, quiz.getPointsReward());
                updatePst.setInt(2, quiz.getPointsReward());
                updatePst.setString(3, userId);
                updatePst.executeUpdate();

                // Update level based on points
                updateUserLevel(userId);
            }

            cnx.commit();
        } catch (SQLException e) {
            cnx.rollback();
            throw e;
        } finally { cnx.setAutoCommit(true); }
    }

    @Override
    public List<Quiz> getAllQuizzes() throws SQLException {
        List<Quiz> quizzes = new ArrayList<>();
        String query = "SELECT * FROM quizzes WHERE status = 'active' ORDER BY created_at DESC";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(query);
        
        while (rs.next()) {
            quizzes.add(mapResultSetToQuiz(rs));
        }
        return quizzes;
    }

    @Override
    public List<Quiz> getQuizzesByCategory(String category) throws SQLException {
        List<Quiz> quizzes = new ArrayList<>();
        String query = "SELECT * FROM quizzes WHERE category = ? AND status = 'active' ORDER BY created_at DESC";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setString(1, category);
        
        ResultSet rs = pst.executeQuery();
        while (rs.next()) {
            quizzes.add(mapResultSetToQuiz(rs));
        }
        return quizzes;
    }

    @Override
    public List<UserQuiz> getUserQuizHistory(String userId) throws SQLException {
        List<UserQuiz> userQuizzes = new ArrayList<>();
        String query = "SELECT * FROM user_quizzes WHERE user_id = ? ORDER BY completed_at DESC";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setString(1, userId);
        
        ResultSet rs = pst.executeQuery();
        while (rs.next()) {
            userQuizzes.add(mapResultSetToUserQuiz(rs));
        }
        return userQuizzes;
    }

    @Override
    public boolean hasUserCompletedQuiz(String userId, long quizId) throws SQLException {
        String query = "SELECT COUNT(*) FROM user_quizzes WHERE user_id = ? AND quiz_id = ?";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setString(1, userId);
        pst.setLong(2, quizId);
        
        ResultSet rs = pst.executeQuery();
        if (rs.next()) {
            return rs.getInt(1) > 0;
        }
        return false;
    }

    /* ===== LEADERBOARD ===== */

    @Override
    public List<UserPoints> getTopUsersByPoints(int limit) throws SQLException {
        List<UserPoints> topUsers = new ArrayList<>();
        String query = "SELECT * FROM user_points ORDER BY points DESC LIMIT ?";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setInt(1, limit);
        
        ResultSet rs = pst.executeQuery();
        while (rs.next()) {
            topUsers.add(mapResultSetToUserPoints(rs));
        }
        return topUsers;
    }

    @Override
    public List<UserPoints> getTopUsersByLevel(int limit) throws SQLException {
        List<UserPoints> topUsers = new ArrayList<>();
        String query = "SELECT * FROM user_points ORDER BY level DESC, points DESC LIMIT ?";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setInt(1, limit);
        
        ResultSet rs = pst.executeQuery();
        while (rs.next()) {
            topUsers.add(mapResultSetToUserPoints(rs));
        }
        return topUsers;
    }

    @Override
    public int getUserRank(String userId) throws SQLException {
        String query = "SELECT COUNT(*) + 1 as rank FROM user_points WHERE points > (SELECT points FROM user_points WHERE user_id = ?)";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setString(1, userId);
        
        ResultSet rs = pst.executeQuery();
        if (rs.next()) {
            return rs.getInt("rank");
        }
        return 0;
    }

    /* ===== HELPER METHODS ===== */

    private void updateUserLevel(String userId) throws SQLException {
        UserPoints userPoints = getUserPoints(userId);
        int currentPoints = userPoints.getPoints();
        int newLevel = calculateLevel(currentPoints);

        if (newLevel != userPoints.getLevel()) {
            String query = "UPDATE user_points SET level = ? WHERE user_id = ?";
            PreparedStatement pst = cnx.prepareStatement(query);
            pst.setInt(1, newLevel);
            pst.setString(2, userId);
            pst.executeUpdate();
        }
    }

    private int calculateLevel(int points) {
        if (points <= 100) return 1;
        if (points <= 250) return 2;
        if (points <= 500) return 3;
        if (points <= 1000) return 4;
        return 5;
    }

    /* ===== MAPPING METHODS ===== */

    private UserPoints mapResultSetToUserPoints(ResultSet rs) throws SQLException {
        UserPoints userPoints = new UserPoints();
        userPoints.setId(rs.getLong("id"));
        userPoints.setUserId(rs.getString("user_id"));
        userPoints.setPoints(rs.getInt("points"));
        userPoints.setLevel(rs.getInt("level"));
        userPoints.setTotalEarnedPoints(rs.getInt("total_earned_points"));
        userPoints.setUpdatedAt(rs.getTimestamp("updated_at"));
        return userPoints;
    }

    private Badge mapResultSetToBadge(ResultSet rs) throws SQLException {
        Badge badge = new Badge();
        badge.setId(rs.getLong("id"));
        badge.setName(rs.getString("name"));
        badge.setDescription(rs.getString("description"));
        badge.setPointsRequired(rs.getInt("points_required"));
        badge.setCreatedAt(rs.getTimestamp("created_at"));
        badge.setUpdatedAt(rs.getTimestamp("updated_at"));
        return badge;
    }

    private Quiz mapResultSetToQuiz(ResultSet rs) throws SQLException {
        Quiz quiz = new Quiz();
        quiz.setId(rs.getLong("id"));
        quiz.setTitle(rs.getString("title"));
        quiz.setDescription(rs.getString("description"));
        quiz.setPointsReward(rs.getInt("points_reward"));
        quiz.setQuestionCount(rs.getInt("question_count"));
        quiz.setDifficultyLevel(rs.getString("difficulty_level"));
        quiz.setCategory(rs.getString("category"));
        quiz.setTimeLimit(rs.getInt("time_limit"));
        quiz.setPassingScore(rs.getInt("passing_score"));
        quiz.setStatus(rs.getString("status"));
        quiz.setCreatedAt(rs.getTimestamp("created_at"));
        quiz.setUpdatedAt(rs.getTimestamp("updated_at"));
        return quiz;
    }

    private UserQuiz mapResultSetToUserQuiz(ResultSet rs) throws SQLException {
        UserQuiz userQuiz = new UserQuiz();
        userQuiz.setId(rs.getLong("id"));
        userQuiz.setUserId(rs.getString("user_id"));
        userQuiz.setQuizId(rs.getLong("quiz_id"));
        userQuiz.setScore(rs.getInt("score"));
        userQuiz.setCompletedAt(rs.getTimestamp("completed_at"));
        userQuiz.setTimeTaken(rs.getInt("time_taken"));
        userQuiz.setPassed(rs.getBoolean("passed"));
        return userQuiz;
    }

    private PointTransaction mapResultSetToPointTransaction(ResultSet rs) throws SQLException {
        PointTransaction transaction = new PointTransaction();
        transaction.setId(rs.getLong("id"));
        transaction.setUserId(rs.getString("user_id"));
        transaction.setPoints(rs.getInt("points"));
        transaction.setTransactionType(rs.getString("transaction_type"));
        
        long refId = rs.getLong("reference_id");
        if (!rs.wasNull()) {
            transaction.setReferenceId(refId);
        }
        
        transaction.setReferenceType(rs.getString("reference_type"));
        transaction.setDescription(rs.getString("description"));
        transaction.setCreatedAt(rs.getTimestamp("created_at"));
        return transaction;
    }

    /* ===== QUESTION MANAGEMENT ===== */

    public long addQuestion(Question question) throws SQLException {
        String query = "INSERT INTO questions (quiz_id, question_text) VALUES (?, ?)";
        
        PreparedStatement pst = cnx.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        pst.setLong(1, question.getQuizId());
        pst.setString(2, question.getQuestionText());
        
        pst.executeUpdate();
        
        ResultSet rs = pst.getGeneratedKeys();
        if (rs.next()) {
            return rs.getLong(1);
        }
        return 0;
    }

    public void addQuestionOption(QuestionOption option) throws SQLException {
        String query = "INSERT INTO question_options (question_id, option_text, is_correct, option_order) " +
                "VALUES (?, ?, ?, ?)";
        
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setLong(1, option.getQuestionId());
        pst.setString(2, option.getOptionText());
        pst.setBoolean(3, option.isCorrect());
        pst.setInt(4, option.getOptionOrder());
        
        pst.executeUpdate();
    }

    public void updateQuestion(Question question, long id) throws SQLException {
        String query = "UPDATE questions SET quiz_id = ?, question_text = ? WHERE id = ?";
        
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setLong(1, question.getQuizId());
        pst.setString(2, question.getQuestionText());
        pst.setLong(3, id);
        
        pst.executeUpdate();
    }

    public void deleteQuestion(long id) throws SQLException {
        // Options will be deleted automatically due to CASCADE
        String query = "DELETE FROM questions WHERE id = ?";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setLong(1, id);
        pst.executeUpdate();
    }

    public void deleteQuestionOption(long id) throws SQLException {
        String query = "DELETE FROM question_options WHERE id = ?";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setLong(1, id);
        pst.executeUpdate();
    }

    public Question getQuestionById(long id) throws SQLException {
        String query = "SELECT * FROM questions WHERE id = ?";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setLong(1, id);
        
        ResultSet rs = pst.executeQuery();
        if (rs.next()) {
            Question question = mapResultSetToQuestion(rs);
            question.setOptions(getQuestionOptions(id));
            return question;
        }
        return null;
    }

    public List<Question> getQuestionsByQuizId(long quizId) throws SQLException {
        List<Question> questions = new ArrayList<>();
        String query = "SELECT * FROM questions WHERE quiz_id = ? ORDER BY id ASC";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setLong(1, quizId);
        
        ResultSet rs = pst.executeQuery();
        while (rs.next()) {
            Question question = mapResultSetToQuestion(rs);
            question.setOptions(getQuestionOptions(question.getId()));
            questions.add(question);
        }
        return questions;
    }

    public List<QuestionOption> getQuestionOptions(long questionId) throws SQLException {
        List<QuestionOption> options = new ArrayList<>();
        String query = "SELECT * FROM question_options WHERE question_id = ? ORDER BY option_order ASC";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setLong(1, questionId);
        
        ResultSet rs = pst.executeQuery();
        while (rs.next()) {
            options.add(mapResultSetToQuestionOption(rs));
        }
        return options;
    }

    public int getQuestionCountByQuizId(long quizId) throws SQLException {
        String query = "SELECT COUNT(*) FROM questions WHERE quiz_id = ?";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setLong(1, quizId);
        
        ResultSet rs = pst.executeQuery();
        if (rs.next()) {
            return rs.getInt(1);
        }
        return 0;
    }

    private Question mapResultSetToQuestion(ResultSet rs) throws SQLException {
        Question question = new Question();
        question.setId(rs.getLong("id"));
        question.setQuizId(rs.getLong("quiz_id"));
        question.setQuestionText(rs.getString("question_text"));
        question.setCreatedAt(rs.getTimestamp("created_at"));
        question.setUpdatedAt(rs.getTimestamp("updated_at"));
        return question;
    }

    private QuestionOption mapResultSetToQuestionOption(ResultSet rs) throws SQLException {
        QuestionOption option = new QuestionOption();
        option.setId(rs.getLong("id"));
        option.setQuestionId(rs.getLong("question_id"));
        option.setOptionText(rs.getString("option_text"));
        option.setCorrect(rs.getBoolean("is_correct"));
        option.setOptionOrder(rs.getInt("option_order"));
        option.setCreatedAt(rs.getTimestamp("created_at"));
        return option;
    }
    
    /* ===== AUTOMATIC BADGE AWARDING ===== */
    
    /**
     * Check and award badges based on user's total points
     * Returns list of newly earned badges
     */
    public List<Badge> checkAndAwardBadges(String userId) throws SQLException {
        List<Badge> newlyEarnedBadges = new ArrayList<>();
        
        // Get user's total points
        int totalPoints = getUserTotalPoints(userId);
        System.out.println("🔍 Checking badges for user " + userId + " with " + totalPoints + " total points");
        
        // First, check how many badges exist in total
        String countQuery = "SELECT COUNT(*) as total FROM badges WHERE points_required <= ?";
        PreparedStatement countPst = cnx.prepareStatement(countQuery);
        countPst.setInt(1, totalPoints);
        ResultSet countRs = countPst.executeQuery();
        if (countRs.next()) {
            int totalEligible = countRs.getInt("total");
            System.out.println("   📊 Total badges with points_required <= " + totalPoints + ": " + totalEligible);
        }
        
        // Check how many badges user already has
        String earnedQuery = "SELECT COUNT(*) as earned FROM user_badges WHERE user_id = ?";
        PreparedStatement earnedPst = cnx.prepareStatement(earnedQuery);
        earnedPst.setString(1, userId);
        ResultSet earnedRs = earnedPst.executeQuery();
        if (earnedRs.next()) {
            int alreadyEarned = earnedRs.getInt("earned");
            System.out.println("   ✅ Badges already earned by user: " + alreadyEarned);
        }
        
        // Get all badges user hasn't earned yet
        String query = "SELECT b.* FROM badges b " +
                      "WHERE b.points_required <= ? " +
                      "AND b.id NOT IN (SELECT badge_id FROM user_badges WHERE user_id = ?) " +
                      "ORDER BY b.points_required ASC";
        
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setInt(1, totalPoints);
        pst.setString(2, userId);
        ResultSet rs = pst.executeQuery();
        
        int eligibleCount = 0;
        while (rs.next()) {
            eligibleCount++;
            Badge badge = mapResultSetToBadge(rs);
            
            System.out.println("   ✓ Eligible badge: " + badge.getName() + " (requires " + badge.getPointsRequired() + " points)");
            
            // Award the badge
            awardBadgeToUser(userId, badge.getId());
            newlyEarnedBadges.add(badge);
            
            System.out.println("   🏆 Badge awarded: " + badge.getName() + " to user " + userId);
        }
        
        if (eligibleCount == 0) {
            System.out.println("   ℹ️ No new badges to award");
        }
        
        return newlyEarnedBadges;
    }
    
    /**
     * Award a specific badge to a user
     */
    public void awardBadgeToUser(String userId, long badgeId) throws SQLException {
        String query = "INSERT IGNORE INTO user_badges (user_id, badge_id, earned_at) VALUES (?, ?, NOW())";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setString(1, userId);
        pst.setLong(2, badgeId);
        pst.executeUpdate();
    }
    
    /**
     * Get user's total earned points
     */
    public int getUserTotalPoints(String userId) throws SQLException {
        String query = "SELECT total_earned_points FROM user_points WHERE user_id = ?";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setString(1, userId);
        ResultSet rs = pst.executeQuery();
        
        if (rs.next()) {
            return rs.getInt("total_earned_points");
        }
        return 0;
    }
    
    /**
     * Check if user has a specific badge
     */
    public boolean userHasBadge(String userId, long badgeId) throws SQLException {
        String query = "SELECT COUNT(*) as count FROM user_badges WHERE user_id = ? AND badge_id = ?";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setString(1, userId);
        pst.setLong(2, badgeId);
        ResultSet rs = pst.executeQuery();
        
        if (rs.next()) {
            return rs.getInt("count") > 0;
        }
        return false;
    }
    
    /* ===== QUIZ ACCESS CONTROL ===== */
    
    /**
     * Check if user can take a quiz
     * Returns null if allowed, or error message if not allowed
     */
    public String canUserTakeQuiz(String userId, long quizId) throws SQLException {
        System.out.println("🔍 Checking quiz access for user " + userId + ", quiz " + quizId);
        
        // Check if user has passed this quiz
        String passedQuery = "SELECT passed, completed_at FROM user_quizzes " +
                            "WHERE user_id = ? AND quiz_id = ? " +
                            "ORDER BY completed_at DESC LIMIT 1";
        PreparedStatement pst = cnx.prepareStatement(passedQuery);
        pst.setString(1, userId);
        pst.setLong(2, quizId);
        ResultSet rs = pst.executeQuery();
        
        if (rs.next()) {
            boolean passed = rs.getBoolean("passed");
            java.sql.Timestamp lastAttempt = rs.getTimestamp("completed_at");
            
            System.out.println("   📝 Found previous attempt: passed=" + passed + ", time=" + lastAttempt);
            
            // If user passed, they cannot retake
            if (passed) {
                System.out.println("   ❌ User already passed - blocking access");
                return "You have already passed this quiz! ✅";
            }
            
            // If user failed, check if 3 minutes have passed
            long currentTime = System.currentTimeMillis();
            long lastAttemptTime = lastAttempt.getTime();
            long timeDiff = currentTime - lastAttemptTime;
            long threeMinutesInMillis = 3 * 60 * 1000; // 3 minutes
            
            System.out.println("   ⏱️ Time since last attempt: " + (timeDiff / 1000) + " seconds");
            
            if (timeDiff < threeMinutesInMillis) {
                long remainingSeconds = (threeMinutesInMillis - timeDiff) / 1000;
                long minutes = remainingSeconds / 60;
                long seconds = remainingSeconds % 60;
                System.out.println("   ❌ Still in cooldown period - blocking access");
                return String.format("Please wait %d:%02d before retrying this quiz ⏳", minutes, seconds);
            }
            
            System.out.println("   ✅ Cooldown period expired - allowing retry");
        } else {
            System.out.println("   ✅ No previous attempts - allowing access");
        }
        
        // User can take the quiz
        return null;
    }
    
    /**
     * Get user's last quiz attempt
     */
    public UserQuiz getLastQuizAttempt(String userId, long quizId) throws SQLException {
        String query = "SELECT * FROM user_quizzes WHERE user_id = ? AND quiz_id = ? " +
                      "ORDER BY completed_at DESC LIMIT 1";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setString(1, userId);
        pst.setLong(2, quizId);
        ResultSet rs = pst.executeQuery();
        
        if (rs.next()) {
            return mapResultSetToUserQuiz(rs);
        }
        return null;
    }
    
    /* ===== USER EMAIL METHODS ===== */
    
    /**
     * Get user's email address from personne table
     */
    public String getUserEmail(String userId) throws SQLException {
        String query = "SELECT email FROM personne WHERE id = ?";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setString(1, userId);
        ResultSet rs = pst.executeQuery();
        
        if (rs.next()) {
            return rs.getString("email");
        }
        return null;
    }
    
    /**
     * Get user's full name from personne table
     */
    public String getUserName(String userId) throws SQLException {
        String query = "SELECT nom, prenom FROM personne WHERE id = ?";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setString(1, userId);
        ResultSet rs = pst.executeQuery();
        
        if (rs.next()) {
            String nom = rs.getString("nom");
            String prenom = rs.getString("prenom");
            return prenom + " " + nom;
        }
        return "User";
    }
    
}
