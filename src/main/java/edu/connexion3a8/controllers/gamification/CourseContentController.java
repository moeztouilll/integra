package edu.connexion3a8.controllers.gamification;

import edu.connexion3a8.entities.gamification.Course;
import edu.connexion3a8.entities.gamification.Quiz;
import edu.connexion3a8.services.gamification.CouseService;
import edu.connexion3a8.services.gamification.GamificationService;
import edu.connexion3a8.services.gamification.LibreTranslateService;
import edu.connexion3a8.services.gamification.TextToSpeechService;
import edu.connexion3a8.services.gamification.RecommendationService;
import edu.connexion3a8.utils.gamification.ThemeManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;

import java.io.File;
import java.sql.SQLException;
import java.util.List;

public class CourseContentController {

    @FXML private Label courseTitleLabel;
    @FXML private Label difficultyLabel;
    @FXML private Label pointsLabel;
    @FXML private Label durationLabel;
    @FXML private Label categoryLabel;
    @FXML private VBox mediaContainer;
    @FXML private VBox quizzesContainer;
    @FXML private Label quizCountLabel;
    @FXML private Button nightModeToggle;
    @FXML private VBox similarCoursesSection;
    @FXML private HBox similarCoursesContainer;

    private CouseService courseService;
    private GamificationService gamificationService;
    private RecommendationService recommendationService;
    private Course currentCourse;
    private MediaPlayer mediaPlayer; // Keep reference to stop when leaving
    private TextToSpeechService ttsService; // Text-to-Speech service
    private String currentUserId = "1"; // TODO: Get from session/login

    public void initialize() {
        courseService = new CouseService();
        gamificationService = new GamificationService();
        recommendationService = new RecommendationService();
        ttsService = new TextToSpeechService();
    }

    public void setCourse(Course course) {
        this.currentCourse = course;
        
        // Log course visit
        try {
            courseService.addCourseVisit(currentUserId, course.getId());
            System.out.println("Course visit logged for user " + currentUserId + ", course " + course.getId());
        } catch (SQLException e) {
            System.err.println("Error logging course visit: " + e.getMessage());
            e.printStackTrace();
        }
        
        loadCourseContent();
        // Apply theme after content is loaded with a longer delay
        javafx.application.Platform.runLater(() -> {
            try {
                Thread.sleep(100); // Small delay to ensure scene is fully loaded
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (ThemeManager.getInstance().isDarkMode()) {
                System.out.println("Applying dark mode after course content loaded");
                applyTheme();
                updateThemeButton();
            }
        });
    }
    
    public void setDarkMode(boolean darkMode) {
        System.out.println("CourseContent setDarkMode called with: " + darkMode);
        ThemeManager.getInstance().setDarkMode(darkMode);
        // Don't apply theme here - wait for setCourse to finish loading
    }
    
    @FXML
    private void toggleNightMode() {
        ThemeManager.getInstance().toggleDarkMode();
        System.out.println("CourseContent toggleNightMode clicked. New isDarkMode = " + ThemeManager.getInstance().isDarkMode());
        applyTheme();
        updateThemeButton();
    }
    
    private void applyTheme() {
        System.out.println("CourseContent applyTheme called. isDarkMode = " + ThemeManager.getInstance().isDarkMode());
        
        // Find the ScrollPane root
        if (courseTitleLabel != null && courseTitleLabel.getScene() != null) {
            javafx.scene.Parent root = courseTitleLabel.getScene().getRoot();
            System.out.println("CourseContent root type: " + root.getClass().getName());
            
            if (root instanceof ScrollPane) {
                ScrollPane scrollPane = (ScrollPane) root;
                VBox vbox = null;
                
                if (scrollPane.getContent() instanceof VBox) {
                    vbox = (VBox) scrollPane.getContent();
                    System.out.println("Found VBox in ScrollPane");
                }
                
                // Apply to ScrollPane with !important equivalent (multiple properties)
                if (ThemeManager.getInstance().isDarkMode()) {
                    String darkStyle = "-fx-background: #0A0A18; -fx-background-color: #0A0A18; -fx-border-color: transparent;";
                    scrollPane.setStyle(darkStyle);
                    if (!scrollPane.getStyleClass().contains("dark-mode")) {
                        scrollPane.getStyleClass().add("dark-mode");
                    }
                    System.out.println("Applied dark mode to ScrollPane with style: " + darkStyle);
                    System.out.println("ScrollPane actual style: " + scrollPane.getStyle());
                } else {
                    scrollPane.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
                    scrollPane.getStyleClass().remove("dark-mode");
                    System.out.println("Applied light mode to ScrollPane");
                }
                
                // Apply to VBox
                if (vbox != null) {
                    if (ThemeManager.getInstance().isDarkMode()) {
                        if (!vbox.getStyleClass().contains("dark-mode")) {
                            vbox.getStyleClass().add("dark-mode");
                        }
                        String vboxStyle = "-fx-background: linear-gradient(to bottom right, #12122A, #0A0A18, #100F22); -fx-background-color: #0A0A18;";
                        vbox.setStyle(vboxStyle);
                        System.out.println("Applied dark mode to VBox");
                        System.out.println("VBox actual style: " + vbox.getStyle());
                    } else {
                        vbox.getStyleClass().remove("dark-mode");
                        vbox.setStyle("");
                        System.out.println("Applied light mode to VBox");
                    }
                }
            } else {
                System.out.println("Root is not a ScrollPane!");
            }
        } else {
            System.out.println("courseTitleLabel or scene is null!");
        }
    }
    
    private void updateThemeButton() {
        if (nightModeToggle != null) {
            if (ThemeManager.getInstance().isDarkMode()) {
                nightModeToggle.setText("☀️ Light Mode");
            } else {
                nightModeToggle.setText("🌙 Night Mode");
            }
        }
    }

    private void loadCourseContent() {
        if (currentCourse == null) return;

        // Set course title and info
        courseTitleLabel.setText(currentCourse.getTitle());
        difficultyLabel.setText("📊 " + currentCourse.getDifficultyLevel());
        pointsLabel.setText("⭐ " + currentCourse.getRewardPoints() + " pts");
        durationLabel.setText("⏱️ " + currentCourse.getEstimatedDuration() + " min");
        categoryLabel.setText("📂 " + currentCourse.getCategory());

        // Load media
        loadMedia();

        // Load related quizzes
        loadRelatedQuizzes();
        
        // Load similar courses
        loadSimilarCourses();
    }

    private void loadMedia() {
        mediaContainer.getChildren().clear();

        String contentUrl = currentCourse.getContentUrl();

        if (contentUrl == null || contentUrl.trim().isEmpty()) {
            Label noMediaLabel = new Label("No media available for this course.");
            noMediaLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-style: italic; -fx-font-size: 14px;");
            mediaContainer.getChildren().add(noMediaLabel);
            return;
        }

        // Check if it's a YouTube URL
        if (isYouTubeUrl(contentUrl)) {
            loadYouTubeVideo(contentUrl);
            return;
        }

        File mediaFile = new File(contentUrl);

        if (mediaFile.exists()) {
            String fileExtension = contentUrl.substring(contentUrl.lastIndexOf(".")).toLowerCase();

            if (fileExtension.matches("\\.(mp4|avi|mkv|mov|wmv|flv)")) {
                // Video file
                loadVideoPlayer(mediaFile);
            } else if (fileExtension.equals(".pdf")) {
                // PDF file - embed in page
                loadPDFViewer(mediaFile);
            } else {
                Label unsupportedLabel = new Label("Unsupported media format: " + fileExtension);
                unsupportedLabel.setStyle("-fx-text-fill: #DC3545; -fx-font-size: 13px;");
                mediaContainer.getChildren().add(unsupportedLabel);
            }
        } else {
            // External URL (not YouTube)
            Label urlLabel = new Label("External Content:");
            urlLabel.setStyle("-fx-font-weight: 600; -fx-text-fill: #456990; -fx-font-size: 14px;");

            Hyperlink link = new Hyperlink(contentUrl);
            link.setStyle("-fx-font-size: 13px;");
            link.setOnAction(e -> {
                try {
                    java.awt.Desktop.getDesktop().browse(new java.net.URI(contentUrl));
                } catch (Exception ex) {
                    showError("Could not open URL: " + ex.getMessage());
                }
            });

            VBox urlBox = new VBox(8, urlLabel, link);
            urlBox.setPadding(new Insets(15));
            urlBox.setStyle("-fx-background-color: white; -fx-background-radius: 8px; -fx-border-color: #456990; -fx-border-width: 2px; -fx-border-radius: 8px;");
            mediaContainer.getChildren().add(urlBox);
        }
    }

    private boolean isYouTubeUrl(String url) {
        return url.contains("youtube.com") || url.contains("youtu.be");
    }

    private String extractYouTubeVideoId(String url) {
        // Handle different YouTube URL formats
        // https://www.youtube.com/watch?v=VIDEO_ID
        // https://youtu.be/VIDEO_ID
        // https://www.youtube.com/embed/VIDEO_ID
        
        try {
            if (url.contains("youtu.be/")) {
                String[] parts = url.split("youtu.be/");
                if (parts.length > 1) {
                    String videoId = parts[1].split("[?&]")[0];
                    return videoId;
                }
            } else if (url.contains("youtube.com/watch")) {
                String[] parts = url.split("v=");
                if (parts.length > 1) {
                    String videoId = parts[1].split("[&]")[0];
                    return videoId;
                }
            } else if (url.contains("youtube.com/embed/")) {
                String[] parts = url.split("embed/");
                if (parts.length > 1) {
                    String videoId = parts[1].split("[?&]")[0];
                    return videoId;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return null;
    }

    private void loadYouTubeVideo(String youtubeUrl) {
        String videoId = extractYouTubeVideoId(youtubeUrl);
        
        if (videoId == null || videoId.isEmpty()) {
            Label errorLabel = new Label("Invalid YouTube URL format");
            errorLabel.setStyle("-fx-text-fill: #DC3545; -fx-font-size: 13px;");
            mediaContainer.getChildren().add(errorLabel);
            return;
        }

        try {
            // Create container with prominent "Watch on YouTube" option
            VBox videoContainer = new VBox(20);
            videoContainer.setAlignment(Pos.CENTER);
            videoContainer.setPadding(new Insets(40));
            videoContainer.setStyle("-fx-background-color: " + 
                (ThemeManager.getInstance().isDarkMode() ? "#161630" : "white") + "; " +
                "-fx-background-radius: 12px; -fx-border-color: " +
                (ThemeManager.getInstance().isDarkMode() ? "rgba(70,70,100,0.6)" : "#456990") + "; " +
                "-fx-border-width: 2px; -fx-border-radius: 12px;");
            
            // YouTube icon
            Label youtubeIcon = new Label("▶️");
            youtubeIcon.setStyle("-fx-font-size: 64px;");
            
            // Title
            Label titleLabel = new Label("YouTube Video");
            titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: 600; " +
                              "-fx-text-fill: " + (ThemeManager.getInstance().isDarkMode() ? "#F0F2FA" : "#000501") + ";");
            
            // Info message
            Label infoLabel = new Label("Due to technical limitations with embedded players,\nplease watch this video on YouTube.");
            infoLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: " + 
                (ThemeManager.getInstance().isDarkMode() ? "#8D96A6" : "#6B7280") + "; " +
                "-fx-text-alignment: center;");
            infoLabel.setWrapText(true);
            infoLabel.setMaxWidth(600);
            infoLabel.setAlignment(Pos.CENTER);
            
            // Large "Watch on YouTube" button
            Button watchBtn = new Button("🎬 Watch on YouTube");
            watchBtn.setStyle("-fx-background-color: #DC3545; -fx-text-fill: white; " +
                           "-fx-font-weight: 600; -fx-background-radius: 8px; " +
                           "-fx-padding: 16 32; -fx-cursor: hand; -fx-font-size: 16px;");
            watchBtn.setPrefWidth(250);
            watchBtn.setOnAction(e -> {
                try {
                    java.awt.Desktop.getDesktop().browse(new java.net.URI(youtubeUrl));
                } catch (Exception ex) {
                    showError("Could not open browser: " + ex.getMessage());
                }
            });
            
            watchBtn.setOnMouseEntered(e -> {
                watchBtn.setStyle("-fx-background-color: #c82333; -fx-text-fill: white; " +
                               "-fx-font-weight: 600; -fx-background-radius: 8px; " +
                               "-fx-padding: 16 32; -fx-cursor: hand; -fx-font-size: 16px;");
            });
            
            watchBtn.setOnMouseExited(e -> {
                watchBtn.setStyle("-fx-background-color: #DC3545; -fx-text-fill: white; " +
                               "-fx-font-weight: 600; -fx-background-radius: 8px; " +
                               "-fx-padding: 16 32; -fx-cursor: hand; -fx-font-size: 16px;");
            });
            
            // Video URL display
            Label urlLabel = new Label("Video URL:");
            urlLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; " +
                            "-fx-text-fill: " + (ThemeManager.getInstance().isDarkMode() ? "#6189B0" : "#456990") + ";");
            
            TextField urlField = new TextField(youtubeUrl);
            urlField.setEditable(false);
            urlField.setPrefWidth(500);
            urlField.setStyle("-fx-background-color: " + 
                (ThemeManager.getInstance().isDarkMode() ? "#0A0A18" : "#F7F0F5") + "; " +
                "-fx-text-fill: " + (ThemeManager.getInstance().isDarkMode() ? "#E8E8E8" : "#000501") + "; " +
                "-fx-font-size: 11px; -fx-padding: 8;");
            
            // Copy URL button
            Button copyBtn = new Button("📋 Copy URL");
            copyBtn.setStyle("-fx-background-color: " + 
                (ThemeManager.getInstance().isDarkMode() ? "#2A2A3E" : "#E5E7EB") + "; " +
                "-fx-text-fill: " + (ThemeManager.getInstance().isDarkMode() ? "#E8E8E8" : "#000501") + "; " +
                "-fx-font-weight: 600; -fx-background-radius: 6px; " +
                "-fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 12px;");
            copyBtn.setOnAction(e -> {
                javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
                javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                content.putString(youtubeUrl);
                clipboard.setContent(content);
                
                copyBtn.setText("✅ Copied!");
                javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2));
                pause.setOnFinished(ev -> copyBtn.setText("📋 Copy URL"));
                pause.play();
            });
            
            HBox urlBox = new HBox(10, urlField, copyBtn);
            urlBox.setAlignment(Pos.CENTER);
            
            VBox urlContainer = new VBox(5, urlLabel, urlBox);
            urlContainer.setAlignment(Pos.CENTER);
            
            // Video ID display
            Label videoIdLabel = new Label("Video ID: " + videoId);
            videoIdLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: " + 
                (ThemeManager.getInstance().isDarkMode() ? "#6B7280" : "#9CA3AF") + ";");
            
            videoContainer.getChildren().addAll(youtubeIcon, titleLabel, infoLabel, watchBtn, 
                                               new Separator(), urlContainer, videoIdLabel);
            mediaContainer.getChildren().add(videoContainer);
            
        } catch (Exception e) {
            // Fallback error display
            VBox errorContainer = new VBox(15);
            errorContainer.setAlignment(Pos.CENTER);
            errorContainer.setPadding(new Insets(30));
            errorContainer.setStyle("-fx-background-color: " + 
                (ThemeManager.getInstance().isDarkMode() ? "#161630" : "white") + "; " +
                "-fx-background-radius: 12px; -fx-border-color: #DC3545; " +
                "-fx-border-width: 2px; -fx-border-radius: 12px;");
            
            Label errorIcon = new Label("⚠️");
            errorIcon.setStyle("-fx-font-size: 48px;");
            
            Label errorLabel = new Label("Unable to load YouTube video");
            errorLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 600; " +
                              "-fx-text-fill: " + (ThemeManager.getInstance().isDarkMode() ? "#F0F2FA" : "#000501") + ";");
            
            Button openBtn = new Button("🎬 Watch on YouTube");
            openBtn.setStyle("-fx-background-color: #DC3545; -fx-text-fill: white; " +
                           "-fx-font-weight: 600; -fx-background-radius: 8px; " +
                           "-fx-padding: 12 24; -fx-cursor: hand; -fx-font-size: 14px;");
            openBtn.setOnAction(ev -> {
                try {
                    java.awt.Desktop.getDesktop().browse(new java.net.URI(youtubeUrl));
                } catch (Exception ex) {
                    showError("Could not open browser: " + ex.getMessage());
                }
            });
            
            errorContainer.getChildren().addAll(errorIcon, errorLabel, openBtn);
            mediaContainer.getChildren().add(errorContainer);
            
            e.printStackTrace();
        }
    }

    private void loadVideoPlayer(File videoFile) {
        try {
            Media media = new Media(videoFile.toURI().toString());
            mediaPlayer = new MediaPlayer(media);
            MediaView mediaView = new MediaView(mediaPlayer);

            mediaView.setFitWidth(800);
            mediaView.setPreserveRatio(true);

            // Control buttons
            HBox controls = new HBox(12);
            controls.setAlignment(Pos.CENTER);
            controls.setPadding(new Insets(12));
            controls.getStyleClass().add("media-controls");

            Button playBtn = new Button("▶ Play");
            Button pauseBtn = new Button("⏸ Pause");
            Button stopBtn = new Button("⏹ Stop");
            Button rewindBtn = new Button("⏪ Rewind 10s");
            Button forwardBtn = new Button("⏩ Forward 10s");

            playBtn.getStyleClass().addAll("btn", "btn-success");
            pauseBtn.getStyleClass().addAll("btn", "btn-warning");
            stopBtn.getStyleClass().addAll("btn", "btn-danger");
            rewindBtn.getStyleClass().addAll("btn", "btn-secondary");
            forwardBtn.getStyleClass().addAll("btn", "btn-secondary");

            playBtn.setOnAction(e -> mediaPlayer.play());
            pauseBtn.setOnAction(e -> mediaPlayer.pause());
            stopBtn.setOnAction(e -> {
                mediaPlayer.stop();
                mediaPlayer.seek(mediaPlayer.getStartTime());
            });
            rewindBtn.setOnAction(e -> {
                mediaPlayer.seek(mediaPlayer.getCurrentTime().subtract(javafx.util.Duration.seconds(10)));
            });
            forwardBtn.setOnAction(e -> {
                mediaPlayer.seek(mediaPlayer.getCurrentTime().add(javafx.util.Duration.seconds(10)));
            });

            controls.getChildren().addAll(playBtn, pauseBtn, stopBtn, rewindBtn, forwardBtn);

            VBox videoContainer = new VBox(12, mediaView, controls);
            videoContainer.setAlignment(Pos.CENTER);
            videoContainer.getStyleClass().add("video-container");

            mediaContainer.getChildren().add(videoContainer);

        } catch (Exception e) {
            Label errorLabel = new Label("Error loading video: " + e.getMessage());
            errorLabel.setStyle("-fx-text-fill: #DC3545; -fx-font-size: 13px;");
            mediaContainer.getChildren().add(errorLabel);
            e.printStackTrace();
        }
    }

    private void loadPDFViewer(File pdfFile) {
        VBox pdfContainer = new VBox(20);
        pdfContainer.getStyleClass().add("pdf-container");
        pdfContainer.setAlignment(Pos.TOP_CENTER);
        pdfContainer.setPadding(new Insets(30));
        pdfContainer.setStyle("-fx-background-color: " + 
            (ThemeManager.getInstance().isDarkMode() ? "#161630" : "white") + "; " +
            "-fx-background-radius: 12px; -fx-border-color: " + 
            (ThemeManager.getInstance().isDarkMode() ? "rgba(70,70,100,0.6)" : "#DC3545") + "; " +
            "-fx-border-width: 2px; -fx-border-radius: 12px;");

        // PDF Header
        HBox headerBox = new HBox(15);
        headerBox.setAlignment(Pos.CENTER);
        
        Label pdfIcon = new Label("📄");
        pdfIcon.setStyle("-fx-font-size: 48px;");

        VBox infoBox = new VBox(5);
        infoBox.setAlignment(Pos.CENTER_LEFT);
        
        Label titleLabel = new Label("PDF Document");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; " +
            "-fx-text-fill: " + (ThemeManager.getInstance().isDarkMode() ? "#F0F2FA" : "#000501") + ";");
        
        Label fileNameLabel = new Label(pdfFile.getName());
        fileNameLabel.setStyle("-fx-font-size: 14px; " +
            "-fx-text-fill: " + (ThemeManager.getInstance().isDarkMode() ? "#8D96A6" : "#6B7280") + ";");
        
        long fileSizeBytes = pdfFile.length();
        String fileSize = formatFileSize(fileSizeBytes);
        Label fileSizeLabel = new Label("Size: " + fileSize);
        fileSizeLabel.setStyle("-fx-font-size: 13px; " +
            "-fx-text-fill: " + (ThemeManager.getInstance().isDarkMode() ? "#8D96A6" : "#6B7280") + ";");
        
        infoBox.getChildren().addAll(titleLabel, fileNameLabel, fileSizeLabel);
        headerBox.getChildren().addAll(pdfIcon, infoBox);

        // Extract text button
        Button extractBtn = new Button("📖 Extract & Read Text");
        extractBtn.getStyleClass().addAll("btn", "btn-primary");
        extractBtn.setPrefWidth(200);
        extractBtn.setStyle("-fx-background-color: #456990; -fx-text-fill: white; " +
            "-fx-font-weight: 600; -fx-background-radius: 8px; " +
            "-fx-padding: 12 24; -fx-cursor: hand; -fx-font-size: 14px;");
        
        // Open PDF button
        Button openPdfBtn = new Button("🔗 Open in PDF Viewer");
        openPdfBtn.getStyleClass().addAll("btn", "btn-danger");
        openPdfBtn.setPrefWidth(200);
        openPdfBtn.setStyle("-fx-background-color: #DC3545; -fx-text-fill: white; " +
            "-fx-font-weight: 600; -fx-background-radius: 8px; " +
            "-fx-padding: 12 24; -fx-cursor: hand; -fx-font-size: 14px;");
        openPdfBtn.setOnAction(e -> {
            try {
                java.awt.Desktop.getDesktop().open(pdfFile);
            } catch (Exception ex) {
                showError("Could not open PDF: " + ex.getMessage());
            }
        });

        HBox buttonBox = new HBox(15, extractBtn, openPdfBtn);
        buttonBox.setAlignment(Pos.CENTER);

        // Text area for extracted content (initially hidden)
        VBox textContainer = new VBox(10);
        textContainer.setVisible(false);
        textContainer.setManaged(false);
        
        Label textLabel = new Label("📝 Extracted Text:");
        textLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 600; " +
            "-fx-text-fill: " + (ThemeManager.getInstance().isDarkMode() ? "#F0F2FA" : "#000501") + ";");
        
        ScrollPane textScrollPane = new ScrollPane();
        textScrollPane.setFitToWidth(true);
        textScrollPane.setPrefHeight(400);
        textScrollPane.setMaxHeight(600);
        textScrollPane.setStyle("-fx-background-color: transparent; -fx-border-color: " +
            (ThemeManager.getInstance().isDarkMode() ? "rgba(70,70,100,0.6)" : "#E5E7EB") + "; " +
            "-fx-border-width: 1px; -fx-border-radius: 8px;");
        
        TextArea textArea = new TextArea();
        textArea.setWrapText(true);
        textArea.setEditable(false);
        textArea.setPrefHeight(400);
        textArea.setStyle("-fx-control-inner-background: " + 
            (ThemeManager.getInstance().isDarkMode() ? "#0A0A18" : "#F7F0F5") + "; " +
            "-fx-text-fill: " + (ThemeManager.getInstance().isDarkMode() ? "#E8E8E8" : "#000501") + "; " +
            "-fx-font-size: 13px; -fx-font-family: 'Segoe UI', Arial, sans-serif; " +
            "-fx-padding: 15;");
        
        textScrollPane.setContent(textArea);
        
        // Translation controls
        HBox translationBox = new HBox(10);
        translationBox.setAlignment(Pos.CENTER_LEFT);
        translationBox.setPadding(new Insets(10, 0, 0, 0));
        translationBox.setVisible(false);
        translationBox.setManaged(false);
        
        Label translateLabel = new Label("🌐 Translate to:");
        translateLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; " +
            "-fx-text-fill: " + (ThemeManager.getInstance().isDarkMode() ? "#F0F2FA" : "#000501") + ";");
        
        ComboBox<LibreTranslateService.Language> languageCombo = new ComboBox<>();
        languageCombo.getItems().addAll(LibreTranslateService.getSupportedLanguages());
        languageCombo.setValue(new LibreTranslateService.Language("en", "English"));
        languageCombo.setPrefWidth(150);
        
        Button translateBtn = new Button("🔄 Translate");
        translateBtn.setStyle("-fx-background-color: #9B7E46; -fx-text-fill: white; " +
            "-fx-font-weight: 600; -fx-background-radius: 6px; " +
            "-fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 12px;");
        
        Button showOriginalBtn = new Button("📄 Show Original");
        showOriginalBtn.setStyle("-fx-background-color: #6B7280; -fx-text-fill: white; " +
            "-fx-font-weight: 600; -fx-background-radius: 6px; " +
            "-fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 12px;");
        showOriginalBtn.setVisible(false);
        showOriginalBtn.setManaged(false);
        
        translationBox.getChildren().addAll(translateLabel, languageCombo, translateBtn, showOriginalBtn);
        
        // Text-to-Speech controls
        HBox ttsBox = new HBox(10);
        ttsBox.setAlignment(Pos.CENTER_LEFT);
        ttsBox.setPadding(new Insets(10, 0, 0, 0));
        ttsBox.setVisible(false);
        ttsBox.setManaged(false);
        
        Label ttsLabel = new Label("🔊 Listen:");
        ttsLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; " +
            "-fx-text-fill: " + (ThemeManager.getInstance().isDarkMode() ? "#F0F2FA" : "#000501") + ";");
        
        Button playBtn = new Button("▶️ Play");
        playBtn.setStyle("-fx-background-color: #28A745; -fx-text-fill: white; " +
            "-fx-font-weight: 600; -fx-background-radius: 6px; " +
            "-fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 12px;");
        
        Button stopBtn = new Button("⏹️ Stop");
        stopBtn.setStyle("-fx-background-color: #DC3545; -fx-text-fill: white; " +
            "-fx-font-weight: 600; -fx-background-radius: 6px; " +
            "-fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 12px;");
        stopBtn.setDisable(true);
        
        Label speedLabel = new Label("Speed:");
        speedLabel.setStyle("-fx-font-size: 12px; " +
            "-fx-text-fill: " + (ThemeManager.getInstance().isDarkMode() ? "#8D96A6" : "#6B7280") + ";");
        
        ComboBox<String> speedCombo = new ComboBox<>();
        speedCombo.getItems().addAll("Slow (100)", "Normal (150)", "Fast (200)");
        speedCombo.setValue("Normal (150)");
        speedCombo.setPrefWidth(120);
        speedCombo.setStyle("-fx-font-size: 11px;");
        
        ttsBox.getChildren().addAll(ttsLabel, playBtn, stopBtn, speedLabel, speedCombo);
        
        textContainer.getChildren().addAll(textLabel, textScrollPane, translationBox, ttsBox);
        
        // Store original text for "Show Original" button
        final String[] originalText = {""};
        
        // Translate button action
        translateBtn.setOnAction(e -> {
            String textToTranslate = textArea.getText();
            if (textToTranslate == null || textToTranslate.trim().isEmpty()) {
                showError("No text to translate. Please extract text first.");
                return;
            }
            
            // Remove the header section before translation (lines starting with ═)
            String[] lines = textToTranslate.split("\n");
            StringBuilder cleanText = new StringBuilder();
            boolean headerEnded = false;
            
            for (String line : lines) {
                if (!headerEnded && line.contains("═══════")) {
                    continue; // Skip header separator lines
                }
                if (!headerEnded && (line.startsWith("PDF Document:") || 
                                    line.startsWith("Total Pages:") || 
                                    line.startsWith("Extraction Method:"))) {
                    continue; // Skip header info lines
                }
                headerEnded = true;
                cleanText.append(line).append("\n");
            }
            
            String finalText = cleanText.toString().trim();
            if (finalText.isEmpty()) {
                showError("No translatable text found.");
                return;
            }
            
            // Store original if not already stored
            if (originalText[0].isEmpty()) {
                originalText[0] = textToTranslate;
            }
            
            LibreTranslateService.Language targetLang = languageCombo.getValue();
            if (targetLang == null) {
                showError("Please select a target language.");
                return;
            }
            
            translateBtn.setDisable(true);
            translateBtn.setText("⏳ Translating...");
            
            // Translate in background
            new Thread(() -> {
                try {
                    LibreTranslateService translateService = new LibreTranslateService();
                    String translated = translateService.translate(finalText, "auto", targetLang.code);
                    
                    javafx.application.Platform.runLater(() -> {
                        textArea.setText(translated);
                        translateBtn.setText("✅ Translated");
                        translateBtn.setStyle("-fx-background-color: #28A745; -fx-text-fill: white; " +
                            "-fx-font-weight: 600; -fx-background-radius: 6px; " +
                            "-fx-padding: 8 16; -fx-font-size: 12px;");
                        showOriginalBtn.setVisible(true);
                        showOriginalBtn.setManaged(true);
                        
                        // Reset button after 2 seconds
                        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2));
                        pause.setOnFinished(ev -> {
                            translateBtn.setText("🔄 Translate");
                            translateBtn.setStyle("-fx-background-color: #9B7E46; -fx-text-fill: white; " +
                                "-fx-font-weight: 600; -fx-background-radius: 6px; " +
                                "-fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 12px;");
                        });
                        pause.play();
                        
                        translateBtn.setDisable(false);
                    });
                } catch (Exception ex) {
                    javafx.application.Platform.runLater(() -> {
                        translateBtn.setText("❌ Failed");
                        translateBtn.setStyle("-fx-background-color: #DC3545; -fx-text-fill: white; " +
                            "-fx-font-weight: 600; -fx-background-radius: 6px; " +
                            "-fx-padding: 8 16; -fx-font-size: 12px;");
                        translateBtn.setDisable(false);
                        
                        String errorMsg = ex.getMessage();
                        showError("Translation failed: " + (errorMsg != null ? errorMsg : "Unknown error. Please try again."));
                        
                        // Reset button after 3 seconds
                        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(3));
                        pause.setOnFinished(ev -> {
                            translateBtn.setText("🔄 Translate");
                            translateBtn.setStyle("-fx-background-color: #9B7E46; -fx-text-fill: white; " +
                                "-fx-font-weight: 600; -fx-background-radius: 6px; " +
                                "-fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 12px;");
                        });
                        pause.play();
                    });
                }
            }).start();
        });
        
        // Show original button action
        showOriginalBtn.setOnAction(e -> {
            textArea.setText(originalText[0]);
            showOriginalBtn.setVisible(false);
            showOriginalBtn.setManaged(false);
        });
        
        // TTS Play button action
        playBtn.setOnAction(e -> {
            String textToSpeak = textArea.getText();
            if (textToSpeak == null || textToSpeak.trim().isEmpty()) {
                showError("No text to read. Please extract text first.");
                return;
            }
            
            if (!ttsService.isAvailable()) {
                showError("Text-to-Speech is not available. Please check your system configuration.");
                return;
            }
            
            // Set speed based on selection
            String speed = speedCombo.getValue();
            if (speed.contains("100")) {
                ttsService.setRate(100);
            } else if (speed.contains("200")) {
                ttsService.setRate(200);
            } else {
                ttsService.setRate(150); // Normal
            }
            
            // Start speaking
            playBtn.setDisable(true);
            stopBtn.setDisable(false);
            playBtn.setText("🔊 Speaking...");
            
            new Thread(() -> {
                ttsService.speak(textToSpeak);
                
                // Re-enable play button after speech completes
                javafx.application.Platform.runLater(() -> {
                    playBtn.setDisable(false);
                    stopBtn.setDisable(true);
                    playBtn.setText("▶️ Play");
                });
            }).start();
        });
        
        // TTS Stop button action
        stopBtn.setOnAction(e -> {
            ttsService.stop();
            playBtn.setDisable(false);
            stopBtn.setDisable(true);
            playBtn.setText("▶️ Play");
        });

        // Extract button action
        extractBtn.setOnAction(e -> {
            extractBtn.setDisable(true);
            extractBtn.setText("⏳ Extracting...");
            
            // Extract in background thread
            new Thread(() -> {
                try {
                    String extractedText = extractTextFromPDF(pdfFile);
                    
                    // Update UI on JavaFX thread
                    javafx.application.Platform.runLater(() -> {
                        if (extractedText != null && !extractedText.trim().isEmpty()) {
                            textArea.setText(extractedText);
                            originalText[0] = extractedText; // Store original
                            textContainer.setVisible(true);
                            textContainer.setManaged(true);
                            translationBox.setVisible(true);
                            translationBox.setManaged(true);
                            ttsBox.setVisible(true);
                            ttsBox.setManaged(true);
                            extractBtn.setText("✅ Text Extracted");
                            extractBtn.setStyle("-fx-background-color: #28A745; -fx-text-fill: white; " +
                                "-fx-font-weight: 600; -fx-background-radius: 8px; " +
                                "-fx-padding: 12 24; -fx-font-size: 14px;");
                        } else {
                            extractBtn.setText("❌ No Text Found");
                            extractBtn.setStyle("-fx-background-color: #DC3545; -fx-text-fill: white; " +
                                "-fx-font-weight: 600; -fx-background-radius: 8px; " +
                                "-fx-padding: 12 24; -fx-font-size: 14px;");
                            showError("Could not extract text from PDF. The PDF might be image-based or encrypted.");
                        }
                        extractBtn.setDisable(false);
                    });
                    
                } catch (Exception ex) {
                    javafx.application.Platform.runLater(() -> {
                        extractBtn.setText("❌ Extraction Failed");
                        extractBtn.setStyle("-fx-background-color: #DC3545; -fx-text-fill: white; " +
                            "-fx-font-weight: 600; -fx-background-radius: 8px; " +
                            "-fx-padding: 12 24; -fx-font-size: 14px;");
                        extractBtn.setDisable(false);
                        showError("Error extracting text: " + ex.getMessage());
                    });
                    ex.printStackTrace();
                }
            }).start();
        });

        pdfContainer.getChildren().addAll(headerBox, new Separator(), buttonBox, textContainer);
        mediaContainer.getChildren().add(pdfContainer);
    }
    
    /**
     * Extracts text from PDF using Apache PDFBox
     * If no text found, tries AI OCR with Tesseract
     */
    private String extractTextFromPDF(File pdfFile) throws Exception {
        org.apache.pdfbox.pdmodel.PDDocument document = null;
        try {
            document = org.apache.pdfbox.pdmodel.PDDocument.load(pdfFile);
            
            if (document.isEncrypted()) {
                throw new Exception("PDF is encrypted and cannot be read");
            }
            
            org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();
            
            // Extract text from all pages
            String text = stripper.getText(document);
            
            // Get page count
            int pageCount = document.getNumberOfPages();
            
            // If no text found, try OCR (for scanned PDFs)
            if (text.trim().isEmpty() || text.trim().length() < 50) {
                System.out.println("No text found with PDFBox, trying AI OCR...");
                document.close();
                document = null;
                
                String ocrText = extractTextWithOCR(pdfFile);
                if (ocrText != null && !ocrText.trim().isEmpty()) {
                    String header = "═══════════════════════════════════════════════\n";
                    header += "PDF Document: " + pdfFile.getName() + "\n";
                    header += "Total Pages: " + pageCount + "\n";
                    header += "Extraction Method: AI OCR (Tesseract)\n";
                    header += "═══════════════════════════════════════════════\n\n";
                    return header + ocrText;
                }
            }
            
            // Add header with page count
            String header = "═══════════════════════════════════════════════\n";
            header += "PDF Document: " + pdfFile.getName() + "\n";
            header += "Total Pages: " + pageCount + "\n";
            header += "Extraction Method: Standard Text Extraction\n";
            header += "═══════════════════════════════════════════════\n\n";
            
            return header + text;
            
        } finally {
            if (document != null) {
                document.close();
            }
        }
    }
    
    /**
     * Extracts text from scanned PDF using AI OCR (Tesseract)
     * This is a fallback when PDFBox finds no text
     */
    private String extractTextWithOCR(File pdfFile) {
        try {
            // Convert PDF pages to images and run OCR
            org.apache.pdfbox.pdmodel.PDDocument document = org.apache.pdfbox.pdmodel.PDDocument.load(pdfFile);
            org.apache.pdfbox.rendering.PDFRenderer pdfRenderer = new org.apache.pdfbox.rendering.PDFRenderer(document);
            
            StringBuilder allText = new StringBuilder();
            int pageCount = document.getNumberOfPages();
            
            // Limit to first 10 pages for performance (can be adjusted)
            int maxPages = Math.min(pageCount, 10);
            
            net.sourceforge.tess4j.Tesseract tesseract = new net.sourceforge.tess4j.Tesseract();
            
            // Try to find tessdata in multiple locations
            String[] possiblePaths = {
                "E:\\Tesseract\\tessdata",  // User's custom installation
                "C:\\Program Files\\Tesseract-OCR\\tessdata",
                "C:\\Program Files (x86)\\Tesseract-OCR\\tessdata",
                System.getenv("TESSDATA_PREFIX"),
                System.getenv("PROGRAMFILES") + "\\Tesseract-OCR\\tessdata",
                System.getenv("PROGRAMFILES(X86)") + "\\Tesseract-OCR\\tessdata",
                "tessdata",
                "./tessdata",
                "../tessdata"
            };
            
            boolean tessdataFound = false;
            String foundPath = null;
            
            for (String path : possiblePaths) {
                if (path != null) {
                    java.io.File tessdataDir = new java.io.File(path);
                    java.io.File engFile = new java.io.File(tessdataDir, "eng.traineddata");
                    
                    if (tessdataDir.exists() && engFile.exists()) {
                        tesseract.setDatapath(path);
                        tessdataFound = true;
                        foundPath = path;
                        System.out.println("✅ Using tessdata from: " + path);
                        break;
                    }
                }
            }
            
            if (!tessdataFound) {
                System.err.println("❌ Tessdata not found in any location!");
                System.err.println("Searched locations:");
                for (String path : possiblePaths) {
                    if (path != null) {
                        System.err.println("  - " + path);
                    }
                }
                System.err.println("");
                System.err.println("Please run SETUP_TESSERACT.bat to configure Tesseract automatically.");
                document.close();
                return null;
            }
            
            tesseract.setLanguage("eng"); // English
            tesseract.setPageSegMode(1); // Automatic page segmentation with OSD
            tesseract.setOcrEngineMode(1); // Neural nets LSTM engine only
            
            System.out.println("Starting AI OCR extraction...");
            System.out.println("Processing " + maxPages + " pages (this may take 30-60 seconds)...");
            
            for (int page = 0; page < maxPages; page++) {
                try {
                    // Render PDF page to image at 300 DPI for better OCR
                    java.awt.image.BufferedImage image = pdfRenderer.renderImageWithDPI(page, 300);
                    
                    // Run OCR on the image
                    String pageText = tesseract.doOCR(image);
                    
                    if (pageText != null && !pageText.trim().isEmpty()) {
                        allText.append("\n--- Page ").append(page + 1).append(" ---\n");
                        allText.append(pageText);
                        allText.append("\n");
                    }
                    
                    System.out.println("✅ OCR completed for page " + (page + 1) + "/" + maxPages);
                    
                } catch (Exception e) {
                    System.err.println("❌ Error processing page " + (page + 1) + ": " + e.getMessage());
                }
            }
            
            document.close();
            
            if (maxPages < pageCount) {
                allText.append("\n\n[Note: Only first ").append(maxPages)
                       .append(" pages were processed. Full document has ")
                       .append(pageCount).append(" pages.]\n");
            }
            
            if (allText.length() == 0) {
                System.err.println("❌ No text extracted from any page");
                return null;
            }
            
            System.out.println("✅ AI OCR extraction complete! Extracted " + allText.length() + " characters.");
            return allText.toString();
            
        } catch (Exception e) {
            System.err.println("❌ OCR extraction failed: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    private void loadRelatedQuizzes() {
        quizzesContainer.getChildren().clear();

        try {
            List<Long> quizIds = courseService.getQuizIdsForCourse(currentCourse.getId());

            if (quizIds.isEmpty()) {
                Label noQuizLabel = new Label("No quizzes available for this course yet.");
                noQuizLabel.getStyleClass().add("empty-state");
                quizzesContainer.getChildren().add(noQuizLabel);
                quizCountLabel.setText("(0)");
            } else {
                quizCountLabel.setText("(" + quizIds.size() + ")");

                for (Long quizId : quizIds) {
                    Quiz quiz = gamificationService.getQuizById(quizId);
                    if (quiz != null) {
                        HBox quizCard = createQuizCard(quiz);
                        quizzesContainer.getChildren().add(quizCard);
                    }
                }
            }

        } catch (SQLException e) {
            Label errorLabel = new Label("Error loading quizzes: " + e.getMessage());
            errorLabel.setStyle("-fx-text-fill: #DC3545; -fx-font-size: 13px;");
            quizzesContainer.getChildren().add(errorLabel);
            e.printStackTrace();
        }
    }

    private HBox createQuizCard(Quiz quiz) {
        HBox card = new HBox(15);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("quiz-card");

        // Check quiz access status
        String accessError = null;
        try {
            accessError = gamificationService.canUserTakeQuiz(currentUserId, quiz.getId());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        boolean isPassed = accessError != null && accessError.contains("already passed");
        boolean isLocked = accessError != null && accessError.contains("wait");

        // Quiz icon
        StackPane iconContainer = new StackPane();
        iconContainer.setPrefSize(50, 50);
        
        String iconBg = isPassed ? "rgba(76,175,80,0.25)" : 
                       isLocked ? "rgba(220,53,69,0.25)" : 
                       "rgba(155,126,70,0.25)";
        iconContainer.setStyle("-fx-background-color: linear-gradient(to bottom right, " + iconBg + ", " + iconBg + "); -fx-background-radius: 10;");

        String iconEmoji = isPassed ? "✅" : isLocked ? "🔒" : "📝";
        Label iconLabel = new Label(iconEmoji);
        iconLabel.setStyle("-fx-font-size: 24px;");
        iconContainer.getChildren().add(iconLabel);

        // Quiz info
        VBox quizInfo = new VBox(5);
        HBox.setHgrow(quizInfo, Priority.ALWAYS);

        Label quizTitle = new Label(quiz.getTitle());
        quizTitle.getStyleClass().add("quiz-title");

        String statusText = isPassed ? " (Completed ✓)" : isLocked ? " (Locked 🔒)" : "";
        Label quizDetails = new Label(quiz.getQuestionCount() + " questions • " + 
                                     quiz.getPointsReward() + " points • " + 
                                     quiz.getDifficultyLevel() + statusText);
        quizDetails.getStyleClass().add("quiz-details");

        quizInfo.getChildren().addAll(quizTitle, quizDetails);

        // Take quiz button
        Button takeQuizBtn = new Button(isPassed ? "Completed" : isLocked ? "Locked" : "Take Quiz");
        takeQuizBtn.getStyleClass().addAll("btn", "btn-primary");
        
        if (isPassed) {
            takeQuizBtn.setStyle("-fx-background-color: #4CAF50; -fx-cursor: not-allowed;");
            takeQuizBtn.setDisable(true);
        } else if (isLocked) {
            takeQuizBtn.setStyle("-fx-background-color: #DC3545; -fx-cursor: not-allowed;");
            takeQuizBtn.setDisable(false); // Keep enabled to show message
        }
        
        takeQuizBtn.setOnAction(e -> openQuiz(quiz));

        card.getChildren().addAll(iconContainer, quizInfo, takeQuizBtn);

        return card;
    }

    private void openQuiz(Quiz quiz) {
        try {
            // Check if user can take this quiz
            String accessError = gamificationService.canUserTakeQuiz(currentUserId, quiz.getId());
            
            if (accessError != null) {
                // User cannot take quiz - show error
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Quiz Access Restricted");
                alert.setHeaderText("Cannot Take Quiz");
                alert.setContentText(accessError);
                
                // Style the dialog
                styleDialog(alert);
                
                alert.showAndWait();
                return;
            }
            
            // User can take quiz - proceed
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/gamification/QuizTakingView.fxml"));
            javafx.scene.Parent root = loader.load();
            
            QuizTakingController controller = loader.getController();
            controller.setQuizAndCourse(quiz, currentCourse);
            controller.setDarkMode(ThemeManager.getInstance().isDarkMode());

            javafx.stage.Stage stage = (javafx.stage.Stage) courseTitleLabel.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("Quiz - " + quiz.getTitle());
        } catch (SQLException e) {
            showError("Error checking quiz access: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            showError("Error loading quiz: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Style dialog based on current theme
     */
    private void styleDialog(Alert alert) {
        if (ThemeManager.getInstance().isDarkMode()) {
            javafx.scene.control.DialogPane dialogPane = alert.getDialogPane();
            dialogPane.setStyle(
                "-fx-background-color: #161630; " +
                "-fx-border-color: rgba(70,70,100,0.6); " +
                "-fx-border-width: 2px; " +
                "-fx-border-radius: 12px; " +
                "-fx-background-radius: 12px;"
            );
            
            // Style header
            if (dialogPane.lookup(".header-panel") != null) {
                dialogPane.lookup(".header-panel").setStyle(
                    "-fx-background-color: #12122A; " +
                    "-fx-background-radius: 12px 12px 0 0;"
                );
            }
            
            // Style labels
            for (javafx.scene.Node node : dialogPane.getChildren()) {
                if (node instanceof javafx.scene.control.Label) {
                    node.setStyle("-fx-text-fill: #F0F2FA;");
                }
            }
            
            // Style content area
            if (dialogPane.lookup(".content") != null) {
                dialogPane.lookup(".content").setStyle("-fx-background-color: #161630;");
            }
            
            // Style buttons
            for (javafx.scene.control.ButtonType buttonType : alert.getButtonTypes()) {
                javafx.scene.control.Button button = (javafx.scene.control.Button) dialogPane.lookupButton(buttonType);
                if (button != null) {
                    button.setStyle(
                        "-fx-background-color: linear-gradient(to bottom, #E4C45E, #C8A84E, #9B7E46); " +
                        "-fx-text-fill: white; " +
                        "-fx-font-weight: bold; " +
                        "-fx-background-radius: 6px; " +
                        "-fx-padding: 8 20;"
                    );
                }
            }
        } else {
            // Light mode styling
            javafx.scene.control.DialogPane dialogPane = alert.getDialogPane();
            dialogPane.setStyle(
                "-fx-background-color: white; " +
                "-fx-border-color: #456990; " +
                "-fx-border-width: 2px; " +
                "-fx-border-radius: 12px; " +
                "-fx-background-radius: 12px;"
            );
            
            // Style buttons
            for (javafx.scene.control.ButtonType buttonType : alert.getButtonTypes()) {
                javafx.scene.control.Button button = (javafx.scene.control.Button) dialogPane.lookupButton(buttonType);
                if (button != null) {
                    button.setStyle(
                        "-fx-background-color: linear-gradient(to bottom, #E4C45E, #C8A84E, #9B7E46); " +
                        "-fx-text-fill: white; " +
                        "-fx-font-weight: bold; " +
                        "-fx-background-radius: 6px; " +
                        "-fx-padding: 8 20;"
                    );
                }
            }
        }
    }

    @FXML
    private void handleBack() {
        // Stop media player if running
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
        }
        
        // Stop TTS if speaking
        if (ttsService != null) {
            ttsService.stop();
        }

        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/gamification/CourseCatalogView.fxml"));
            javafx.scene.Parent root = loader.load();
            
            // Pass the dark mode state back to catalog
            CourseCatalogController controller = loader.getController();
            controller.setDarkMode(ThemeManager.getInstance().isDarkMode());

            javafx.stage.Stage stage = (javafx.stage.Stage) courseTitleLabel.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("Course Catalog");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void loadSimilarCourses() {
        if (similarCoursesContainer == null || currentCourse == null) return;
        
        similarCoursesContainer.getChildren().clear();
        
        try {
            List<Course> similarCourses = recommendationService.getSimilarCourseRecommendations(
                currentCourse.getId(), 5);
            
            if (similarCourses.isEmpty()) {
                // Hide similar courses section if no recommendations
                if (similarCoursesSection != null) {
                    similarCoursesSection.setVisible(false);
                    similarCoursesSection.setManaged(false);
                }
                return;
            }
            
            // Show similar courses section
            if (similarCoursesSection != null) {
                similarCoursesSection.setVisible(true);
                similarCoursesSection.setManaged(true);
            }
            
            // Create compact cards for similar courses
            for (Course course : similarCourses) {
                VBox similarCard = createSimilarCourseCard(course);
                similarCoursesContainer.getChildren().add(similarCard);
            }
            
        } catch (SQLException e) {
            System.err.println("Error loading similar courses: " + e.getMessage());
            e.printStackTrace();
            // Hide similar courses section on error
            if (similarCoursesSection != null) {
                similarCoursesSection.setVisible(false);
                similarCoursesSection.setManaged(false);
            }
        }
    }
    
    private VBox createSimilarCourseCard(Course course) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(12));
        card.setPrefWidth(220);
        card.setMaxWidth(220);
        card.setMinHeight(280);
        
        // Apply styling
        if (ThemeManager.getInstance().isDarkMode()) {
            card.setStyle("-fx-background-color: #161630; -fx-background-radius: 10px; " +
                         "-fx-border-color: rgba(69,105,144,0.6); -fx-border-width: 2px; -fx-border-radius: 10px; " +
                         "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 12, 0, 0, 4);");
        } else {
            card.setStyle("-fx-background-color: white; -fx-background-radius: 10px; " +
                         "-fx-border-color: #456990; -fx-border-width: 2px; -fx-border-radius: 10px; " +
                         "-fx-effect: dropshadow(gaussian, rgba(69,105,144,0.2), 10, 0, 0, 3);");
        }
        
        // Thumbnail
        String thumbnailUrl = course.getThumbnailUrl();
        if (thumbnailUrl != null && !thumbnailUrl.trim().isEmpty()) {
            File thumbnailFile = new File(thumbnailUrl);
            if (thumbnailFile.exists()) {
                try {
                    javafx.scene.image.Image image = new javafx.scene.image.Image(thumbnailFile.toURI().toString());
                    javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView(image);
                    imageView.setFitWidth(196);
                    imageView.setFitHeight(110);
                    imageView.setPreserveRatio(false);
                    
                    javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(196, 110);
                    clip.setArcWidth(12);
                    clip.setArcHeight(12);
                    imageView.setClip(clip);
                    
                    card.getChildren().add(imageView);
                } catch (Exception e) {
                    addSmallThumbnailPlaceholder(card);
                }
            } else {
                addSmallThumbnailPlaceholder(card);
            }
        } else {
            addSmallThumbnailPlaceholder(card);
        }
        
        // Title
        Label titleLabel = new Label(course.getTitle());
        titleLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; " +
                           "-fx-text-fill: " + (ThemeManager.getInstance().isDarkMode() ? "#F0F2FA" : "#000501") + ";");
        titleLabel.setWrapText(true);
        titleLabel.setMaxHeight(40);
        
        // Metadata
        HBox metaBox = new HBox(8);
        Label pointsLabel = new Label("⭐ " + course.getRewardPoints());
        Label difficultyLabel = new Label("📊 " + course.getDifficultyLevel());
        
        String pointsColor = ThemeManager.getInstance().isDarkMode() ? "#E4C45E" : "#9B7E46";
        String metaColor = ThemeManager.getInstance().isDarkMode() ? "#8D96A6" : "#6B7280";
        
        pointsLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: " + pointsColor + ";");
        difficultyLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: " + metaColor + ";");
        metaBox.getChildren().addAll(pointsLabel, difficultyLabel);
        
        // Spacer
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        
        // View button
        Button viewBtn = new Button("View Course");
        viewBtn.setStyle("-fx-background-color: linear-gradient(to bottom, #6189B0, #456990); " +
                        "-fx-text-fill: white; -fx-font-weight: 600; -fx-background-radius: 6px; " +
                        "-fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 11px;");
        viewBtn.setPrefWidth(196);
        viewBtn.setOnAction(e -> {
            // Navigate to the similar course
            setCourse(course);
        });
        
        card.getChildren().addAll(titleLabel, metaBox, spacer, viewBtn);
        
        return card;
    }
    
    private void addSmallThumbnailPlaceholder(VBox card) {
        StackPane placeholder = new StackPane();
        placeholder.setPrefSize(196, 110);
        placeholder.setMaxSize(196, 110);
        placeholder.setStyle("-fx-background-color: linear-gradient(to bottom right, #456990, #9B7E46); " +
                            "-fx-background-radius: 8px;");
        
        Label placeholderLabel = new Label("📚");
        placeholderLabel.setStyle("-fx-font-size: 36px;");
        
        placeholder.getChildren().add(placeholderLabel);
        card.getChildren().add(placeholder);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("An error occurred");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
