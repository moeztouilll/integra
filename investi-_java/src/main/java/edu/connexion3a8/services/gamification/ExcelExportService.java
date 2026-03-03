package edu.connexion3a8.services.gamification;

import edu.connexion3a8.entities.gamification.Course;
import edu.connexion3a8.entities.gamification.CourseReport;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service for exporting data to Excel files using Apache POI
 */
public class ExcelExportService {
    
    private CouseService courseService;
    
    public ExcelExportService() {
        this.courseService = new CouseService();
    }
    
    /**
     * Export all courses to Excel file
     */
    public File exportCoursesToExcel() throws SQLException, IOException {
        List<Course> courses = courseService.getAllCourses();
        
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Courses");
        
        // Create header style
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        
        // Create header row
        Row headerRow = sheet.createRow(0);
        String[] headers = {"ID", "Title", "Category", "Difficulty", "Duration (min)", 
                           "Reward Points", "Status", "Visibility", "Created At"};
        
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // Fill data rows
        int rowNum = 1;
        for (Course course : courses) {
            Row row = sheet.createRow(rowNum++);
            
            createCell(row, 0, course.getId(), dataStyle);
            createCell(row, 1, course.getTitle(), dataStyle);
            createCell(row, 2, course.getCategory(), dataStyle);
            createCell(row, 3, course.getDifficultyLevel(), dataStyle);
            createCell(row, 4, course.getEstimatedDuration(), dataStyle);
            createCell(row, 5, course.getRewardPoints(), dataStyle);
            createCell(row, 6, course.getStatus(), dataStyle);
            createCell(row, 7, course.getVisibility(), dataStyle);
            createCell(row, 8, course.getCreatedAt() != null ? course.getCreatedAt().toString() : "", dataStyle);
        }
        
        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
        
        // Save to file
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        File file = new File("exports/courses_" + timestamp + ".xlsx");
        file.getParentFile().mkdirs();
        
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            workbook.write(outputStream);
        }
        
        workbook.close();
        System.out.println("✅ Courses exported to: " + file.getAbsolutePath());
        return file;
    }
    
    /**
     * Export course analytics to Excel file
     */
    public File exportCourseAnalytics() throws SQLException, IOException {
        Workbook workbook = new XSSFWorkbook();
        
        // Sheet 1: Course Statistics
        createCourseStatisticsSheet(workbook);
        
        // Sheet 2: Category Distribution
        createCategoryDistributionSheet(workbook);
        
        // Sheet 3: Difficulty Distribution
        createDifficultyDistributionSheet(workbook);
        
        // Sheet 4: Course Reports
        createCourseReportsSheet(workbook);
        
        // Save to file
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        File file = new File("exports/course_analytics_" + timestamp + ".xlsx");
        file.getParentFile().mkdirs();
        
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            workbook.write(outputStream);
        }
        
        workbook.close();
        System.out.println("✅ Analytics exported to: " + file.getAbsolutePath());
        return file;
    }
    
    /**
     * Create course statistics sheet
     */
    private void createCourseStatisticsSheet(Workbook workbook) throws SQLException {
        Sheet sheet = workbook.createSheet("Course Statistics");
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        
        List<Course> courses = courseService.getAllCourses();
        
        // Headers
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Course Title", "Category", "Difficulty", "Views", "Likes", 
                           "Dislikes", "Reports", "Engagement Rate"};
        
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // Data rows
        int rowNum = 1;
        for (Course course : courses) {
            Row row = sheet.createRow(rowNum++);
            
            int views = courseService.getCourseViewCount(course.getId());
            int likes = courseService.getCourseLikeCount(course.getId());
            int dislikes = courseService.getCourseDislikeCount(course.getId());
            int reports = courseService.getReportCountByCourse(course.getId());
            
            double engagementRate = views > 0 ? ((likes + dislikes) * 100.0 / views) : 0;
            
            createCell(row, 0, course.getTitle(), dataStyle);
            createCell(row, 1, course.getCategory(), dataStyle);
            createCell(row, 2, course.getDifficultyLevel(), dataStyle);
            createCell(row, 3, views, dataStyle);
            createCell(row, 4, likes, dataStyle);
            createCell(row, 5, dislikes, dataStyle);
            createCell(row, 6, reports, dataStyle);
            createCell(row, 7, String.format("%.2f%%", engagementRate), dataStyle);
        }
        
        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }
    
    /**
     * Create category distribution sheet
     */
    private void createCategoryDistributionSheet(Workbook workbook) throws SQLException {
        Sheet sheet = workbook.createSheet("Category Distribution");
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        
        // Headers
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Category");
        headerRow.createCell(1).setCellValue("Course Count");
        headerRow.createCell(2).setCellValue("Percentage");
        headerRow.getCell(0).setCellStyle(headerStyle);
        headerRow.getCell(1).setCellStyle(headerStyle);
        headerRow.getCell(2).setCellStyle(headerStyle);
        
        // Get category distribution
        List<Course> courses = courseService.getAllCourses();
        java.util.Map<String, Integer> categoryCount = new java.util.HashMap<>();
        
        for (Course course : courses) {
            String category = course.getCategory();
            categoryCount.put(category, categoryCount.getOrDefault(category, 0) + 1);
        }
        
        int total = courses.size();
        int rowNum = 1;
        
        for (java.util.Map.Entry<String, Integer> entry : categoryCount.entrySet()) {
            Row row = sheet.createRow(rowNum++);
            double percentage = (entry.getValue() * 100.0) / total;
            
            createCell(row, 0, entry.getKey(), dataStyle);
            createCell(row, 1, entry.getValue(), dataStyle);
            createCell(row, 2, String.format("%.2f%%", percentage), dataStyle);
        }
        
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);
    }
    
    /**
     * Create difficulty distribution sheet
     */
    private void createDifficultyDistributionSheet(Workbook workbook) throws SQLException {
        Sheet sheet = workbook.createSheet("Difficulty Distribution");
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        
        // Headers
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Difficulty Level");
        headerRow.createCell(1).setCellValue("Course Count");
        headerRow.createCell(2).setCellValue("Percentage");
        headerRow.getCell(0).setCellStyle(headerStyle);
        headerRow.getCell(1).setCellStyle(headerStyle);
        headerRow.getCell(2).setCellStyle(headerStyle);
        
        // Get difficulty distribution
        List<Course> courses = courseService.getAllCourses();
        java.util.Map<String, Integer> difficultyCount = new java.util.HashMap<>();
        
        for (Course course : courses) {
            String difficulty = course.getDifficultyLevel();
            difficultyCount.put(difficulty, difficultyCount.getOrDefault(difficulty, 0) + 1);
        }
        
        int total = courses.size();
        int rowNum = 1;
        
        for (java.util.Map.Entry<String, Integer> entry : difficultyCount.entrySet()) {
            Row row = sheet.createRow(rowNum++);
            double percentage = (entry.getValue() * 100.0) / total;
            
            createCell(row, 0, entry.getKey(), dataStyle);
            createCell(row, 1, entry.getValue(), dataStyle);
            createCell(row, 2, String.format("%.2f%%", percentage), dataStyle);
        }
        
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);
    }
    
    /**
     * Create course reports sheet
     */
    private void createCourseReportsSheet(Workbook workbook) throws SQLException {
        Sheet sheet = workbook.createSheet("Course Reports");
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        
        List<CourseReport> reports = courseService.getAllReports();
        
        // Headers
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Report ID", "Course Name", "User ID", "Reason", 
                           "Description", "Status", "Date"};
        
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // Data rows
        int rowNum = 1;
        for (CourseReport report : reports) {
            Row row = sheet.createRow(rowNum++);
            
            createCell(row, 0, report.getId(), dataStyle);
            createCell(row, 1, report.getCourseName(), dataStyle);
            createCell(row, 2, report.getUserId(), dataStyle);
            createCell(row, 3, report.getReportReason(), dataStyle);
            createCell(row, 4, report.getDescription(), dataStyle);
            createCell(row, 5, report.getStatus(), dataStyle);
            createCell(row, 6, report.getCreatedAt() != null ? report.getCreatedAt().toString() : "", dataStyle);
        }
        
        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }
    
    /**
     * Create header cell style
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }
    
    /**
     * Create data cell style
     */
    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setWrapText(false);
        return style;
    }
    
    /**
     * Helper method to create cells with different data types
     */
    private void createCell(Row row, int column, Object value, CellStyle style) {
        Cell cell = row.createCell(column);
        
        if (value instanceof String) {
            cell.setCellValue((String) value);
        } else if (value instanceof Integer) {
            cell.setCellValue((Integer) value);
        } else if (value instanceof Long) {
            cell.setCellValue((Long) value);
        } else if (value instanceof Double) {
            cell.setCellValue((Double) value);
        } else if (value != null) {
            cell.setCellValue(value.toString());
        }
        
        cell.setCellStyle(style);
    }
}
