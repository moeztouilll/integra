package edu.connexion3a8.interfaces.gamification;

import edu.connexion3a8.entities.gamification.*;
import java.sql.SQLException;
import java.util.List;

public interface IGamification {

    /* ===== POINTS MANAGEMENT ===== */
    void addPoints(String userId, int points, String transactionType, String description) throws SQLException;
    void deductPoints(String userId, int points, String transactionType, String description) throws SQLException;
    UserPoints getUserPoints(String userId) throws SQLException;
    List<PointTransaction> getUserPointHistory(String userId) throws SQLException;
    
    /* ===== BADGE MANAGEMENT ===== */
    void awardBadge(String userId, long badgeId) throws SQLException;
    List<Badge> getUserBadges(String userId) throws SQLException;
    List<Badge> getAllBadges() throws SQLException;
    boolean hasUserEarnedBadge(String userId, long badgeId) throws SQLException;
    
    /* ===== QUIZ MANAGEMENT ===== */
    void addQuiz(Quiz quiz) throws SQLException;
    void updateQuiz(Quiz quiz, long id) throws SQLException;
    void deleteQuiz(long id) throws SQLException;
    Quiz getQuizById(long id) throws SQLException;
    void completeQuiz(String userId, long quizId, int score, int timeTaken) throws SQLException;
    List<Quiz> getAllQuizzes() throws SQLException;
    List<Quiz> getQuizzesByCategory(String category) throws SQLException;
    List<UserQuiz> getUserQuizHistory(String userId) throws SQLException;
    boolean hasUserCompletedQuiz(String userId, long quizId) throws SQLException;
    
    /* ===== LEADERBOARD ===== */
    List<UserPoints> getTopUsersByPoints(int limit) throws SQLException;
    List<UserPoints> getTopUsersByLevel(int limit) throws SQLException;
    int getUserRank(String userId) throws SQLException;
}
