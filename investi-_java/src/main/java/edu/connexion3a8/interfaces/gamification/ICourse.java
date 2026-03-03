package edu.connexion3a8.interfaces.gamification;

import edu.connexion3a8.entities.gamification.Course;
import java.sql.SQLException;
import java.util.List;

public interface ICourse {

    /* ===== CREATE ===== */
    void addCourse(Course course) throws SQLException;
    void addDraftCourse(Course course) throws SQLException;

    /* ===== READ ===== */
    Course getCourseById(long id) throws SQLException;
    Course getCourseBySlug(String slug) throws SQLException;
    List<Course> getAllCourses() throws SQLException;

    List<Course> getCoursesByDifficulty(String difficulty) throws SQLException;
    List<Course> getCoursesByCategory(String category) throws SQLException;
    List<Course> getCoursesByStatus(String status) throws SQLException;
    List<Course> getPublicCourses() throws SQLException;


    void updateCourse(Course course, long id) throws SQLException;
    void updateCourseStatus(long id, String status) throws SQLException;
    void publishCourse(long id) throws SQLException;


    void deleteCourse(long id) throws SQLException;
    void archiveCourse(long id) throws SQLException;
}
