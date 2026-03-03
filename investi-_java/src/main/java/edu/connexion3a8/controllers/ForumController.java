package edu.connexion3a8.controllers;

import edu.connexion3a8.InvestiApp;
import edu.connexion3a8.entities.User;
import edu.connexion3a8.entities.ForumPost;
import edu.connexion3a8.entities.ForumComment;
import edu.connexion3a8.services.ForumPostService;
import edu.connexion3a8.tools.MentionParser;
import edu.connexion3a8.tools.SummarizationService;
import edu.connexion3a8.tools.ThemeManager;
import edu.connexion3a8.tools.TranslationService;
import edu.connexion3a8.tools.TranslationService.Language;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;

public class ForumController implements Initializable {

    @FXML private VBox postsContainer;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private ToggleButton allPostsTab;
    @FXML private ToggleButton myPostsTab;
    @FXML private ToggleButton myActivityTab;
    @FXML private ComboBox<String> userSelector;
    @FXML private Label currentUserLabel;
    @FXML private Label userAvatarLabel;
    @FXML private Label composeAvatarLabel;
    @FXML private TextArea composeTextArea;
    @FXML private BorderPane rootPane;
    @FXML private VBox sidebarPane;
    @FXML private VBox centerPane;
    @FXML private HBox headerPane;
    @FXML private Label homeTitleLabel;
    @FXML private Button themeToggleBtn;
    @FXML private ToggleButton savedPostsTab;
    @FXML private ScrollPane feedScroll;
    @FXML private Button homeButton;

    private ForumPostService forumService;
    private String currentUserId;
    private String currentUserName;
    private String currentUserRole;
    private String currentFilter = "all";
    private String currentCategory = "All";
    private List<String[]> allUsers;
    private List<String> pendingImages = new ArrayList<>();
    private Button notificationBell;
    private Label notificationBadge;
    private User currentUser;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        forumService = new ForumPostService();
        setupNotificationBell();
        
        // Hide user selector - we'll use session user
        if (userSelector != null) {
            userSelector.setVisible(false);
            userSelector.setManaged(false);
        }
        
        setupCategoryFilter();
        setupTabGroup();
        setupDynamicSearch();

        // Register the main scene for theme management after it's available
        javafx.application.Platform.runLater(() -> {
            if (rootPane.getScene() != null) {
                ThemeManager.registerScene(rootPane.getScene());
                applyThemeToMainUI();
            }
        });
    }
    
    /**
     * Set the current user from the session
     * This replaces the old user selector dropdown
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user != null) {
            this.currentUserId = user.getId();
            this.currentUserName = user.getName();
            this.currentUserRole = user.getRole();
            
            if (currentUserLabel != null) {
                currentUserLabel.setText(currentUserName);
            }
            
            String initials = getInitials(currentUserName);
            if (userAvatarLabel != null) {
                userAvatarLabel.setText(initials);
            }
            if (composeAvatarLabel != null) {
                composeAvatarLabel.setText(initials);
            }
            
            updateNotificationBadge();
            loadPosts();
        }
    }

    @FXML
    private void handleToggleTheme() {
        ThemeManager.toggle();
        applyThemeToMainUI();
        loadPosts(); // Rebuild post cards with correct inline colors
    }

    /** Applies current theme colors to all inline-styled elements in the main view. */
    private void applyThemeToMainUI() {
        String bg = ThemeManager.bg();
        String border = ThemeManager.border();
        String text = ThemeManager.text();
        String headerBg = ThemeManager.headerBg();
        String card = ThemeManager.card();
        String textSec = ThemeManager.textSec();
        String inputBg = ThemeManager.inputBg();
        String textMuted = ThemeManager.textMuted();

        // Main layout panes
        rootPane.setStyle("-fx-background-color: " + bg + ";");
        sidebarPane.setStyle("-fx-background-color: " + bg + "; -fx-padding: 20 15; -fx-min-width: 260; -fx-max-width: 260;");
        centerPane.setStyle("-fx-background-color: " + bg + "; -fx-border-color: " + border + "; -fx-border-width: 0 0 0 1;");
        headerPane.setStyle("-fx-padding: 15 25; -fx-background-color: " + headerBg + "; -fx-border-color: " + border + "; -fx-border-width: 0 0 1 0;");
        homeTitleLabel.setStyle("-fx-text-fill: " + text + "; -fx-font-size: 20px; -fx-font-weight: bold;");
        currentUserLabel.setStyle("-fx-text-fill: " + text + "; -fx-font-weight: bold; -fx-font-size: 14px;");

        // Feed scroll and posts container
        feedScroll.setStyle("-fx-background: " + bg + "; -fx-background-color: " + bg + ";");
        postsContainer.setStyle("-fx-background-color: " + bg + ";");

        // Search field — -fx-control-inner-background forces internal bg color
        searchField.setStyle(
            "-fx-background-color: " + inputBg + "; -fx-text-fill: " + text + "; " +
            "-fx-prompt-text-fill: " + textMuted + "; -fx-background-radius: 22; " +
            "-fx-padding: 10 20; -fx-border-color: " + border + "; -fx-border-radius: 22; " +
            "-fx-border-width: 1; -fx-control-inner-background: " + inputBg + ";"
        );

        // Compose textarea — -fx-control-inner-background forces internal bg color
        composeTextArea.setStyle(
            "-fx-background-color: " + inputBg + "; -fx-text-fill: " + text + "; " +
            "-fx-prompt-text-fill: " + textMuted + "; -fx-font-size: 17px; " +
            "-fx-border-width: 1; -fx-border-color: " + border + "; " +
            "-fx-background-radius: 14; -fx-border-radius: 14; -fx-padding: 14; " +
            "-fx-control-inner-background: " + inputBg + ";"
        );

        // Category filter — ButtonCell ensures the displayed cell respects theme
        categoryFilter.setStyle(
            "-fx-background-color: " + inputBg + "; -fx-background-radius: 20; " +
            "-fx-padding: 8 15; -fx-border-color: " + border + "; -fx-border-radius: 20;"
        );
        categoryFilter.setButtonCell(new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setStyle("-fx-background-color: transparent; -fx-text-fill: " + text + "; -fx-padding: 0;");
            }
        });

        // User selector — ButtonCell ensures the displayed cell respects theme
        userSelector.setStyle("-fx-background-color: transparent; -fx-font-size: 13px;");
        userSelector.setButtonCell(new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setStyle("-fx-background-color: transparent; -fx-text-fill: " + textSec + "; -fx-padding: 0;");
            }
        });

        // Force-style internal child nodes after two layout passes for reliability
        javafx.application.Platform.runLater(() -> javafx.application.Platform.runLater(() -> {
            // TextArea internal nodes
            composeTextArea.lookupAll(".content").forEach(n ->
                n.setStyle("-fx-background-color: " + inputBg + ";")
            );
            composeTextArea.lookupAll(".scroll-pane").forEach(n ->
                n.setStyle("-fx-background-color: " + inputBg + ";")
            );
            composeTextArea.lookupAll(".viewport").forEach(n ->
                n.setStyle("-fx-background-color: " + inputBg + ";")
            );

            // Category combo internal nodes
            categoryFilter.lookupAll(".list-cell").forEach(n ->
                n.setStyle("-fx-background-color: transparent; -fx-text-fill: " + text + ";")
            );
            categoryFilter.lookupAll(".arrow-button").forEach(n ->
                n.setStyle("-fx-background-color: transparent;")
            );
            categoryFilter.lookupAll(".arrow").forEach(n ->
                n.setStyle("-fx-background-color: " + textSec + ";")
            );

            // User selector internal nodes
            userSelector.lookupAll(".list-cell").forEach(n ->
                n.setStyle("-fx-background-color: transparent; -fx-text-fill: " + textSec + ";")
            );
            userSelector.lookupAll(".arrow-button").forEach(n ->
                n.setStyle("-fx-background-color: transparent;")
            );
            userSelector.lookupAll(".arrow").forEach(n ->
                n.setStyle("-fx-background-color: " + textSec + ";")
            );

            // Search field internal
            searchField.lookupAll(".content").forEach(n ->
                n.setStyle("-fx-background-color: " + inputBg + ";")
            );
        }));

        // Update sidebar children: separators, labels, user row
        for (javafx.scene.Node node : sidebarPane.getChildren()) {
            if (node instanceof Separator) {
                node.setStyle("-fx-background-color: " + border + ";");
            } else if (node instanceof Label) {
                Label lbl = (Label) node;
                if (lbl != currentUserLabel && lbl != userAvatarLabel && lbl != composeAvatarLabel) {
                    lbl.setStyle("-fx-text-fill: " + textSec + "; -fx-font-size: 13px; -fx-padding: 10 12 5 12;");
                }
            } else if (node instanceof VBox) {
                VBox vbox = (VBox) node;
                for (javafx.scene.Node child : vbox.getChildren()) {
                    if (child instanceof Separator) {
                        child.setStyle("-fx-background-color: " + border + ";");
                    } else if (child instanceof HBox) {
                        child.setStyle("-fx-padding: 10; -fx-background-color: " + ThemeManager.userRowBg() + "; -fx-background-radius: 50; -fx-cursor: hand;");
                    }
                }
            }
        }

        // Update compose box area
        for (javafx.scene.Node node : centerPane.getChildren()) {
            if (node instanceof VBox && node.getStyleClass().contains("compose-box")) {
                node.setStyle("-fx-background-color: " + (ThemeManager.isDark() ? bg : card) + "; -fx-padding: 18 25; -fx-border-color: " + border + "; -fx-border-width: 0 0 1 0;");
            }
        }

        // Update theme toggle button text
        if (ThemeManager.isDark()) {
            themeToggleBtn.setText("☀  Light Mode");
        } else {
            themeToggleBtn.setText("🌙  Dark Mode");
        }
    }

    private void setupNotificationBell() {
        // Create notification bell button with badge
        StackPane bellContainer = new StackPane();
        bellContainer.setAlignment(Pos.CENTER);

        notificationBell = new Button("🔔");
        notificationBell.setStyle("-fx-background-color: transparent; -fx-font-size: 18px; -fx-cursor: hand; -fx-padding: 6 10;");
        notificationBell.setTooltip(new Tooltip("Notifications"));
        notificationBell.setOnAction(e -> showNotificationsPopup());

        notificationBadge = new Label("0");
        notificationBadge.setStyle(
            "-fx-background-color: #A62639; -fx-text-fill: white; -fx-font-size: 10px; " +
            "-fx-font-weight: bold; -fx-min-width: 18; -fx-min-height: 18; " +
            "-fx-max-width: 18; -fx-max-height: 18; -fx-background-radius: 9; -fx-alignment: center;"
        );
        notificationBadge.setVisible(false);
        StackPane.setAlignment(notificationBadge, Pos.TOP_RIGHT);

        bellContainer.getChildren().addAll(notificationBell, notificationBadge);

        // Add bell to header, before the search field
        headerPane.getChildren().add(1, bellContainer);
    }

    private void updateNotificationBadge() {
        if (notificationBadge == null || currentUserId == null) return;
        try {
            int count = forumService.getUnreadNotificationCount(currentUserId);
            if (count > 0) {
                notificationBadge.setText(count > 9 ? "9+" : String.valueOf(count));
                notificationBadge.setVisible(true);
            } else {
                notificationBadge.setVisible(false);
            }
        } catch (SQLException e) {
            // Silently ignore
        }
    }

    private void showNotificationsPopup() {
        try {
            List<String[]> notifications = forumService.getUnreadNotifications(currentUserId);

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initStyle(StageStyle.UNDECORATED);

            VBox root = new VBox(0);
            root.setStyle("-fx-background-color: " + ThemeManager.bg() + "; -fx-border-color: " + ThemeManager.border() +
                    "; -fx-border-radius: 16; -fx-background-radius: 16; -fx-border-width: 1;");
            root.setPrefWidth(420);

            // Header
            HBox header = new HBox(10);
            header.setAlignment(Pos.CENTER_LEFT);
            header.setStyle("-fx-padding: 15 20; -fx-border-color: " + ThemeManager.border() + "; -fx-border-width: 0 0 1 0;");

            Button closeBtn = new Button("✕");
            closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: " + ThemeManager.text() + "; -fx-font-size: 18px; -fx-cursor: hand;");
            closeBtn.setOnAction(e -> dialog.close());

            Label titleLbl = new Label("🔔 Notifications");
            titleLbl.setStyle("-fx-text-fill: " + ThemeManager.text() + "; -fx-font-size: 18px; -fx-font-weight: bold;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button markAllBtn = new Button("Mark all read");
            markAllBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #456990; -fx-cursor: hand; -fx-font-size: 13px;");
            markAllBtn.setOnAction(e -> {
                try {
                    forumService.markAllNotificationsRead(currentUserId);
                    updateNotificationBadge();
                    dialog.close();
                } catch (SQLException ex) {
                    showError("Failed: " + ex.getMessage());
                }
            });

            header.getChildren().addAll(closeBtn, titleLbl, spacer, markAllBtn);
            root.getChildren().add(header);

            // Notification list
            VBox listBox = new VBox(0);
            ScrollPane scroll = new ScrollPane(listBox);
            scroll.setFitToWidth(true);
            scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
            scroll.setPrefHeight(350);

            if (notifications.isEmpty()) {
                Label emptyLbl = new Label("No new notifications");
                emptyLbl.setStyle("-fx-text-fill: " + ThemeManager.textSec() + "; -fx-font-size: 14px; -fx-padding: 30;");
                emptyLbl.setAlignment(Pos.CENTER);
                emptyLbl.setMaxWidth(Double.MAX_VALUE);
                listBox.getChildren().add(emptyLbl);
            } else {
                for (String[] notif : notifications) {
                    // notif: [id, senderName, postId, commentId, message, createdAt]
                    HBox row = new HBox(10);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setStyle("-fx-padding: 12 20; -fx-border-color: " + ThemeManager.border() + "; -fx-border-width: 0 0 1 0; -fx-cursor: hand;");

                    Label icon = new Label("@");
                    icon.setStyle("-fx-background-color: #456990; -fx-text-fill: white; -fx-font-weight: bold; " +
                            "-fx-min-width: 32; -fx-min-height: 32; -fx-max-width: 32; -fx-max-height: 32; " +
                            "-fx-background-radius: 16; -fx-alignment: center; -fx-font-size: 14px;");

                    VBox textCol = new VBox(2);
                    HBox.setHgrow(textCol, Priority.ALWAYS);

                    Label msgLbl = new Label(notif[4]);
                    msgLbl.setStyle("-fx-text-fill: " + ThemeManager.text() + "; -fx-font-size: 13px;");
                    msgLbl.setWrapText(true);

                    Label timeLbl = new Label(notif[5]);
                    timeLbl.setStyle("-fx-text-fill: " + ThemeManager.textSec() + "; -fx-font-size: 11px;");

                    textCol.getChildren().addAll(msgLbl, timeLbl);
                    row.getChildren().addAll(icon, textCol);

                    // Click to open the post and mark as read
                    final String notifId = notif[0];
                    final String postId = notif[2];
                    row.setOnMouseClicked(e -> {
                        try {
                            forumService.markNotificationRead(notifId);
                            updateNotificationBadge();
                            dialog.close();
                            ForumPost post = forumService.getPostById(postId);
                            if (post != null) {
                                showPostDetails(post);
                            }
                        } catch (SQLException ex) {
                            showError("Failed: " + ex.getMessage());
                        }
                    });

                    // Hover effect
                    String hoverBg = ThemeManager.isDark() ? "#1a1d23" : "#f0f2f5";
                    row.setOnMouseEntered(e -> row.setStyle("-fx-padding: 12 20; -fx-border-color: " + ThemeManager.border() +
                            "; -fx-border-width: 0 0 1 0; -fx-cursor: hand; -fx-background-color: " + hoverBg + ";"));
                    row.setOnMouseExited(e -> row.setStyle("-fx-padding: 12 20; -fx-border-color: " + ThemeManager.border() +
                            "; -fx-border-width: 0 0 1 0; -fx-cursor: hand;"));

                    listBox.getChildren().add(row);
                }
            }

            root.getChildren().add(scroll);

            Scene scene = new Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            dialog.setScene(scene);
            ThemeManager.registerScene(scene);
            dialog.showAndWait();

        } catch (SQLException e) {
            showError("Failed to load notifications: " + e.getMessage());
        }
    }

    /**
     * Process @mentions in text and create notifications for mentioned users.
     * @param text the text containing @mentions
     * @param postId the post ID where the mention occurred
     * @param commentId the comment ID (null if mention is in a post, not a comment)
     */
    private void processMentionNotifications(String text, String postId, String commentId) {
        if (text == null || !MentionParser.hasMentions(text)) return;

        List<String> mentionedNames = MentionParser.extractMentions(text);
        for (String name : mentionedNames) {
            try {
                String mentionedUserId = forumService.findUserIdByName(name);
                if (mentionedUserId != null && !mentionedUserId.equals(currentUserId)) {
                    String context = commentId != null ? "a comment" : "a post";
                    String message = currentUserName + " tagged you in " + context;
                    forumService.createMentionNotification(mentionedUserId, currentUserId, postId, commentId, message);
                }
            } catch (SQLException e) {
                System.out.println("Note: Could not create mention notification for @" + name + ": " + e.getMessage());
            }
        }
    }


    private void setupDynamicSearch() {
        // Add listener for dynamic search as user types
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.trim().isEmpty()) {
                loadPosts();
            } else {
                performSearch(newValue.trim());
            }
        });
    }

    private void performSearch(String searchTerm) {
        try {
            List<ForumPost> posts = forumService.searchPosts(searchTerm);
            displayPosts(posts);
        } catch (SQLException e) {
            // Silently fail for dynamic search
        }
    }

    private String capitalizeRole(String role) {
        if (role == null || role.isEmpty()) return "User";
        return role.substring(0, 1).toUpperCase() + role.substring(1);
    }

    private boolean isAdmin() {
        return "admin".equalsIgnoreCase(currentUserRole);
    }
    
    @FXML
    private void handleHome() {
        try {
            InvestiApp.showHomePage(currentUser);
        } catch (Exception e) {
            showError("Failed to navigate to home: " + e.getMessage());
        }
    }
    
    @FXML
    private void onHomeButtonHover() {
        if (homeButton != null) {
            homeButton.setStyle("-fx-background-color: #5a7fa0; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 8 20; -fx-font-size: 13px; -fx-cursor: hand; -fx-font-weight: bold;");
        }
    }
    
    @FXML
    private void onHomeButtonExit() {
        if (homeButton != null) {
            homeButton.setStyle("-fx-background-color: #456990; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 8 20; -fx-font-size: 13px; -fx-cursor: hand; -fx-font-weight: bold;");
        }
    }

    private void setupCategoryFilter() {
        categoryFilter.getItems().addAll(
            "All", "Tips & Advice", "Success Stories", 
            "Investor Insights", "Collaboration", "Announcements", "General"
        );
        categoryFilter.setValue("All");
        categoryFilter.setOnAction(e -> {
            currentCategory = categoryFilter.getValue();
            loadPosts();
        });
    }

    private void setupTabGroup() {
        ToggleGroup tabGroup = new ToggleGroup();
        allPostsTab.setToggleGroup(tabGroup);
        myPostsTab.setToggleGroup(tabGroup);
        myActivityTab.setToggleGroup(tabGroup);
        savedPostsTab.setToggleGroup(tabGroup);
        allPostsTab.setSelected(true);
    }

    @FXML
    private void handleAllPosts() {
        currentFilter = "all";
        loadPosts();
    }

    @FXML
    private void handleMyPosts() {
        currentFilter = "myPosts";
        loadPosts();
    }

    @FXML
    private void handleMyActivity() {
        currentFilter = "myActivity";
        loadPosts();
    }

    @FXML
    private void handleSavedPosts() {
        currentFilter = "saved";
        loadPosts();
    }

    @FXML
    private void handleSearch() {
        String searchTerm = searchField.getText().trim();
        if (searchTerm.isEmpty()) {
            loadPosts();
            return;
        }
        performSearch(searchTerm);
    }

    @FXML
    private void handleCreatePost() {
        showCreatePostDialog();
    }

    @FXML
    private void handleAddImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Images");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        Stage stage = (Stage) composeTextArea.getScene().getWindow();
        List<File> files = fileChooser.showOpenMultipleDialog(stage);
        if (files != null) {
            pendingImages.clear();
            for (File file : files) {
                pendingImages.add(file.getAbsolutePath());
            }
            showSuccess("Added " + files.size() + " image(s)");
        }
    }

    @FXML
    private void handleQuickPost() {
        String content = composeTextArea.getText().trim();
        if (content.isEmpty() && pendingImages.isEmpty()) {
            return;
        }
        
        ForumPost post = new ForumPost(currentUserId, null, content.isEmpty() ? null : content, "General");
        for (String img : pendingImages) {
            post.addImagePath(img);
        }
        
        try {
            // Validate post before saving
            String validationError = forumService.validatePost(post);
            if (validationError != null) {
                showError(validationError);
                return;
            }
            
            String newPostId = forumService.addPost(post);
            // Process @mention notifications
            if (newPostId != null) {
                processMentionNotifications(content, newPostId, null);
            }
            composeTextArea.clear();
            pendingImages.clear();
            loadPosts();
            updateNotificationBadge();
            showSuccess("Post published!");
        } catch (SQLException e) {
            showError("Failed to post: " + e.getMessage());
        }
    }

    private void loadPosts() {
        try {
            List<ForumPost> posts;
            
            switch (currentFilter) {
                case "myPosts":
                    posts = forumService.getPostsByUser(currentUserId);
                    break;
                case "myActivity":
                    posts = forumService.getPostsCommentedByUser(currentUserId);
                    List<ForumPost> votedPosts = forumService.getPostsVotedByUser(currentUserId);
                    for (ForumPost p : votedPosts) {
                        if (posts.stream().noneMatch(post -> post.getId().equals(p.getId()))) {
                            posts.add(p);
                        }
                    }
                    // Include posts where user was @mentioned
                    List<ForumPost> mentionedPosts = forumService.getPostsWhereMentioned(currentUserId);
                    for (ForumPost p : mentionedPosts) {
                        if (posts.stream().noneMatch(post -> post.getId().equals(p.getId()))) {
                            posts.add(p);
                        }
                    }
                    break;
                case "saved":
                    posts = forumService.getBookmarkedPosts(currentUserId);
                    break;
                default:
                    if (currentCategory.equals("All")) {
                        posts = forumService.getAllPosts();
                    } else {
                        posts = forumService.getPostsByCategory(currentCategory);
                    }
            }
            
            displayPosts(posts);
            
        } catch (SQLException e) {
            showError("Failed to load posts: " + e.getMessage());
        }
    }


    private void displayPosts(List<ForumPost> posts) {
        postsContainer.getChildren().clear();
        
        if (posts.isEmpty()) {
            VBox emptyState = new VBox(10);
            emptyState.setAlignment(Pos.CENTER);
            emptyState.setStyle("-fx-padding: 50;");
            Label emptyLabel = new Label("No posts yet");
            emptyLabel.setStyle("-fx-text-fill: " + ThemeManager.text() + "; -fx-font-size: 20px; -fx-font-weight: bold;");
            Label subLabel = new Label("Be the first to share something!");
            subLabel.setStyle("-fx-text-fill: " + ThemeManager.textSec() + "; -fx-font-size: 14px;");
            emptyState.getChildren().addAll(emptyLabel, subLabel);
            postsContainer.getChildren().add(emptyState);
            return;
        }
        
        for (ForumPost post : posts) {
            VBox postCard = createPostCard(post);
            postsContainer.getChildren().add(postCard);
        }
    }

    private VBox createPostCard(ForumPost post) {
        VBox card = new VBox(10);
        card.getStyleClass().add("post-card");
        
        String normalBg = ThemeManager.bg();
        String hoverBg = ThemeManager.isDark() ? "#141720" : "#f0f2f5";
        String borderColor = ThemeManager.border();
        String baseStyle = "-fx-padding: 18 25; -fx-border-color: " + borderColor + "; -fx-border-width: 0 0 1 0; -fx-cursor: hand;";
        card.setStyle("-fx-background-color: " + normalBg + "; " + baseStyle);
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: " + hoverBg + "; " + baseStyle));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: " + normalBg + "; " + baseStyle));
        
        // Main content row
        HBox mainRow = new HBox(12);
        
        // Avatar
        Label avatar = new Label(getInitials(post.getAuthorName()));
        avatar.getStyleClass().add("user-avatar");
        
        // Content column
        VBox contentCol = new VBox(4);
        HBox.setHgrow(contentCol, Priority.ALWAYS);
        
        // Header: name, username, time
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label nameLabel = new Label(post.getAuthorName() != null ? post.getAuthorName() : "Anonymous");
        nameLabel.setStyle("-fx-text-fill: " + ThemeManager.text() + "; -fx-font-weight: bold; -fx-font-size: 15px;");
        
        Label timeLabel = new Label("· " + getRelativeTime(post.getCreatedAt()));
        timeLabel.setStyle("-fx-text-fill: " + ThemeManager.textSec() + "; -fx-font-size: 14px;");
        
        if (post.getCategory() != null) {
            Label categoryLabel = new Label(post.getCategory());
            categoryLabel.setStyle("-fx-background-color: #456990; -fx-text-fill: white; " +
                    "-fx-padding: 2 8; -fx-background-radius: 10; -fx-font-size: 11px;");
            header.getChildren().addAll(nameLabel, timeLabel, categoryLabel);
        } else {
            header.getChildren().addAll(nameLabel, timeLabel);
        }
        
        contentCol.getChildren().add(header);
        
        // Title (if exists)
        if (post.getTitle() != null && !post.getTitle().isEmpty()) {
            if (MentionParser.hasMentions(post.getTitle())) {
                TextFlow titleFlow = MentionParser.createStyledText(post.getTitle(), ThemeManager.text(), "16px");
                titleFlow.setStyle("-fx-font-weight: bold;");
                contentCol.getChildren().add(titleFlow);
            } else {
                Label titleLabel = new Label(post.getTitle());
                titleLabel.setStyle("-fx-text-fill: " + ThemeManager.text() + "; -fx-font-size: 16px; -fx-font-weight: bold;");
                titleLabel.setWrapText(true);
                contentCol.getChildren().add(titleLabel);
            }
        }
        
        // TL;DR Summary (for long posts)
        String fullContent = (post.getTitle() != null ? post.getTitle() + " " : "") + 
                            (post.getContent() != null ? post.getContent() : "");
        if (SummarizationService.shouldSummarize(fullContent)) {
            VBox summaryBox = createSummaryBox(post);
            contentCol.getChildren().add(summaryBox);
        }
        
        // Content
        if (post.getContent() != null && !post.getContent().isEmpty()) {
            if (MentionParser.hasMentions(post.getContent())) {
                TextFlow contentFlow = MentionParser.createStyledText(post.getContent(), ThemeManager.text(), "15px");
                contentCol.getChildren().add(contentFlow);
            } else {
                Label contentLabel = new Label(post.getContent());
                contentLabel.setStyle("-fx-text-fill: " + ThemeManager.text() + "; -fx-font-size: 15px;");
                contentLabel.setWrapText(true);
                contentCol.getChildren().add(contentLabel);
            }
        }
        
        // Images
        if (post.hasImages()) {
            HBox imagesBox = createImagesPreview(post.getImagePaths());
            contentCol.getChildren().add(imagesBox);
        }
        
        // Actions row
        HBox actions = createActionsRow(post);
        contentCol.getChildren().add(actions);
        
        mainRow.getChildren().addAll(avatar, contentCol);
        card.getChildren().add(mainRow);
        
        // Click handler
        card.setOnMouseClicked(e -> {
            if (e.getTarget() instanceof Button) return;
            showPostDetails(post);
        });
        
        return card;
    }

    /**
     * Create TL;DR button that opens a popup with summary
     */
    private VBox createSummaryBox(ForumPost post) {
        VBox box = new VBox(5);
        
        String normalStyle = "-fx-background-color: " + ThemeManager.summaryBg() + "; -fx-text-fill: #9B7E46; " +
                "-fx-border-color: #9B7E46; -fx-border-radius: 8; -fx-background-radius: 8; " +
                "-fx-padding: 8 15; -fx-cursor: hand; -fx-font-weight: bold;";
        String hoverStyle = "-fx-background-color: #9B7E46; -fx-text-fill: white; " +
                "-fx-border-color: #9B7E46; -fx-border-radius: 8; -fx-background-radius: 8; " +
                "-fx-padding: 8 15; -fx-cursor: hand; -fx-font-weight: bold;";
        
        Button summaryBtn = new Button("✨ TL;DR - Generate Summary");
        summaryBtn.setStyle(normalStyle);
        
        summaryBtn.setOnMouseEntered(e -> summaryBtn.setStyle(hoverStyle));
        summaryBtn.setOnMouseExited(e -> summaryBtn.setStyle(normalStyle));
        
        summaryBtn.setOnAction(e -> {
            e.consume();
            showSummaryPopup(post);
        });
        
        box.getChildren().add(summaryBtn);
        return box;
    }

    /**
     * Show summary in a popup dialog
     */
    private void showSummaryPopup(ForumPost post) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.setTitle("AI Summary");

        VBox root = new VBox(15);
        root.setStyle("-fx-background-color: " + ThemeManager.bg() + "; -fx-border-color: #9B7E46; " + "-fx-border-radius: 16; -fx-background-radius: 16; -fx-padding: 25; -fx-border-width: 2;");
        root.setPrefWidth(550);
        root.setMinWidth(500);

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: " + ThemeManager.text() + "; " + "-fx-font-size: 18px; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> dialog.close());

        Label titleLbl = new Label("✨ AI Summary");
        titleLbl.setStyle("-fx-text-fill: #9B7E46; -fx-font-size: 20px; -fx-font-weight: bold;");

        header.getChildren().addAll(closeBtn, titleLbl);

        // Loading state
        Label loadingLabel = new Label("🔄 Generating summary with AI...");
        loadingLabel.setStyle("-fx-text-fill: #71767b; -fx-font-size: 14px;");

        ProgressIndicator progress = new ProgressIndicator();
        progress.setStyle("-fx-progress-color: #9B7E46;");
        progress.setPrefSize(40, 40);

        VBox loadingBox = new VBox(15);
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.setPadding(new Insets(30));
        loadingBox.getChildren().addAll(progress, loadingLabel);

        root.getChildren().addAll(header, loadingBox);

        Scene scene = new Scene(root, 550, 550);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        dialog.setScene(scene);
        ThemeManager.registerScene(scene);
        dialog.setResizable(true);
        dialog.show();

        // Generate summary in background
        new Thread(() -> {
            String textToSummarize = (post.getTitle() != null ? post.getTitle() + ". " : "") +
                    (post.getContent() != null ? post.getContent() : "");
            String summary = SummarizationService.summarize(textToSummarize);
            
            System.out.println("[Summarization] Summary to display: " + (summary != null ? summary.substring(0, Math.min(50, summary.length())) + "..." : "null"));

            javafx.application.Platform.runLater(() -> {
                root.getChildren().remove(loadingBox);

                if (summary != null && !summary.isEmpty()) {
                    // Original text section
                    Label origLabel = new Label("📝 Original:");
                    origLabel.setStyle("-fx-text-fill: #71767b; -fx-font-size: 12px; -fx-padding: 0 0 5 0;");

                    Label origText = new Label(textToSummarize.length() > 150 ? 
                            textToSummarize.substring(0, 150) + "..." : textToSummarize);
                    origText.setStyle("-fx-text-fill: " + ThemeManager.textSec() + "; -fx-font-size: 12px;");
                    origText.setWrapText(true);
                    origText.setMaxWidth(480);

                    VBox origBox = new VBox(5);
                    origBox.setStyle("-fx-background-color: " + ThemeManager.card() + "; -fx-padding: 12; -fx-background-radius: 8;");
                    origBox.getChildren().addAll(origLabel, origText);

                    // Summary section
                    Label summaryLabel = new Label("✨ AI-Generated Summary:");
                    summaryLabel.setStyle("-fx-text-fill: #9B7E46; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 0 0 5 0;");

                    Label summaryText = new Label(summary);
                    summaryText.setStyle("-fx-text-fill: " + ThemeManager.text() + "; -fx-font-size: 14px; -fx-line-spacing: 3;");
                    summaryText.setWrapText(true);
                    summaryText.setMaxWidth(480);

                    ScrollPane summaryScroll = new ScrollPane(summaryText);
                    summaryScroll.setFitToWidth(true);
                    summaryScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent;");
                    summaryScroll.setPrefHeight(300);
                    summaryScroll.setMaxHeight(400);

                    VBox summaryBox = new VBox(8);
                    summaryBox.setStyle("-fx-background-color: " + ThemeManager.summaryBg() + "; -fx-padding: 15; " + "-fx-background-radius: 8; -fx-border-color: #9B7E46; -fx-border-radius: 8; -fx-border-width: 1;");
                    VBox.setVgrow(summaryBox, javafx.scene.layout.Priority.ALWAYS);
                    summaryBox.getChildren().addAll(summaryLabel, summaryScroll);

                    // Close button
                    Button okBtn = new Button("Got it!");
                    okBtn.setStyle("-fx-background-color: #9B7E46; -fx-text-fill: white; " +
                            "-fx-padding: 10 40; -fx-background-radius: 20; -fx-cursor: hand; -fx-font-weight: bold; -fx-font-size: 14px;");
                    okBtn.setOnAction(e -> dialog.close());

                    HBox btnBox = new HBox();
                    btnBox.setAlignment(Pos.CENTER);
                    btnBox.setPadding(new Insets(10, 0, 0, 0));
                    btnBox.getChildren().add(okBtn);

                    root.getChildren().addAll(origBox, summaryBox, btnBox);
                    
                    // Resize dialog to fit content
                    dialog.sizeToScene();
                } else {
                    Label errorLabel = new Label("❌ Could not generate summary");
                    errorLabel.setStyle("-fx-text-fill: #A62639; -fx-font-size: 14px;");

                    Button retryBtn = new Button("Close");
                    retryBtn.setStyle("-fx-background-color: #A62639; -fx-text-fill: white; " +
                            "-fx-padding: 10 30; -fx-background-radius: 20; -fx-cursor: hand;");
                    retryBtn.setOnAction(e -> dialog.close());

                    root.getChildren().addAll(errorLabel, retryBtn);
                }
            });
        }).start();
    }

    private HBox createImagesPreview(List<String> imagePaths) {
        HBox box = new HBox(8);
        box.setStyle("-fx-padding: 10 0;");
        
        // For multiple images, create a horizontal scrollable container
        if (imagePaths.size() > 1) {
            ScrollPane scrollPane = new ScrollPane();
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scrollPane.setStyle("-fx-background-color: transparent; -fx-border-width: 0;");
            scrollPane.setFitToHeight(true);
            scrollPane.setPrefHeight(220);
            scrollPane.setMaxHeight(220);
            
            HBox imagesRow = new HBox(8);
            imagesRow.setStyle("-fx-background-color: transparent;");
            
            for (int i = 0; i < imagePaths.size(); i++) {
                try {
                    ImageView img = new ImageView();
                    File file = new File(imagePaths.get(i));
                    if (file.exists()) {
                        img.setImage(new Image(file.toURI().toString()));
                    }
                    img.setFitHeight(200);
                    img.setFitWidth(200);
                    img.setPreserveRatio(true);
                    img.setStyle("-fx-cursor: hand;");
                    
                    // Rounded corners effect
                    javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(200, 200);
                    clip.setArcWidth(16);
                    clip.setArcHeight(16);
                    img.setClip(clip);
                    
                    final int idx = i;
                    img.setOnMouseClicked(e -> {
                        e.consume();
                        showFullImage(imagePaths.get(idx));
                    });
                    
                    imagesRow.getChildren().add(img);
                } catch (Exception e) {
                    // Skip
                }
            }
            
            scrollPane.setContent(imagesRow);
            box.getChildren().add(scrollPane);
            
            // Add image count indicator
            Label countLabel = new Label(imagePaths.size() + " images");
            countLabel.setStyle("-fx-text-fill: " + ThemeManager.textSec() + "; -fx-font-size: 12px; -fx-padding: 5 0 0 0;");
            
            VBox wrapper = new VBox(5);
            wrapper.getChildren().addAll(scrollPane, countLabel);
            box.getChildren().clear();
            box.getChildren().add(wrapper);
        } else {
            // Single image - display normally
            for (int i = 0; i < imagePaths.size(); i++) {
                try {
                    ImageView img = new ImageView();
                    File file = new File(imagePaths.get(i));
                    if (file.exists()) {
                        img.setImage(new Image(file.toURI().toString()));
                    }
                    img.setFitHeight(300);
                    img.setFitWidth(400);
                    img.setPreserveRatio(true);
                    img.setStyle("-fx-cursor: hand;");
                    
                    // Rounded corners effect
                    javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(400, 300);
                    clip.setArcWidth(16);
                    clip.setArcHeight(16);
                    img.setClip(clip);
                    
                    final int idx = i;
                    img.setOnMouseClicked(e -> {
                        e.consume();
                        showFullImage(imagePaths.get(idx));
                    });
                    
                    box.getChildren().add(img);
                } catch (Exception e) {
                    // Skip
                }
            }
        }
        
        return box;
    }

    private HBox createActionsRow(ForumPost post) {
        HBox actions = new HBox(4);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setStyle("-fx-padding: 10 0 0 0;");
        
        // Comments
        int commentCount = 0;
        try {
            commentCount = forumService.getCommentCountByPost(post.getId());
        } catch (SQLException e) {}
        
        Button commentBtn = new Button("\uD83D\uDCAC  " + commentCount);
        commentBtn.getStyleClass().addAll("icon-btn", "icon-btn-comment");
        commentBtn.setTooltip(new Tooltip("Comments"));
        commentBtn.setOnAction(e -> showPostDetails(post));
        
        // Upvote
        Button upvoteBtn = new Button("\u25B2  " + post.getUpvotes());
        upvoteBtn.getStyleClass().addAll("icon-btn", "icon-btn-upvote");
        upvoteBtn.setTooltip(new Tooltip("Upvote"));
        try {
            String vote = forumService.getUserVoteOnPost(post.getId(), currentUserId);
            if ("upvote".equals(vote)) {
                upvoteBtn.getStyleClass().add("icon-btn-upvote-active");
            }
        } catch (SQLException e) {}
        upvoteBtn.setOnAction(e -> {
            e.consume();
            handleVote(post, "upvote");
        });
        
        // Downvote
        Button downvoteBtn = new Button("\u25BC  " + post.getDownvotes());
        downvoteBtn.getStyleClass().addAll("icon-btn", "icon-btn-downvote");
        downvoteBtn.setTooltip(new Tooltip("Downvote"));
        try {
            String vote = forumService.getUserVoteOnPost(post.getId(), currentUserId);
            if ("downvote".equals(vote)) {
                downvoteBtn.getStyleClass().add("icon-btn-downvote-active");
            }
        } catch (SQLException e) {}
        downvoteBtn.setOnAction(e -> {
            e.consume();
            handleVote(post, "downvote");
        });
        
        // Views
        Label viewsLabel = new Label("\uD83D\uDC41  " + post.getViews());
        viewsLabel.setStyle("-fx-text-fill: " + ThemeManager.textMuted() + "; -fx-font-size: 14px; -fx-padding: 6 14;");
        
        // Translate button with dropdown
        MenuButton translateBtn = new MenuButton("\uD83C\uDF10  Translate");
        translateBtn.getStyleClass().addAll("icon-btn", "icon-btn-translate");
        translateBtn.setTooltip(new Tooltip("Translate post"));
        
        MenuItem toEnglish = new MenuItem("\uD83C\uDDEC\uD83C\uDDE7  English");
        MenuItem toFrench = new MenuItem("\uD83C\uDDEB\uD83C\uDDF7  Français");
        MenuItem toArabic = new MenuItem("\uD83C\uDDF8\uD83C\uDDE6  العربية");
        
        toEnglish.setOnAction(e -> showTranslatedPost(post, Language.ENGLISH));
        toFrench.setOnAction(e -> showTranslatedPost(post, Language.FRENCH));
        toArabic.setOnAction(e -> showTranslatedPost(post, Language.ARABIC));
        
        translateBtn.getItems().addAll(toEnglish, toFrench, toArabic);
        
        // Bookmark button
        Button bookmarkBtn = new Button("\uD83D\uDD16");
        bookmarkBtn.getStyleClass().addAll("icon-btn", "icon-btn-comment");
        bookmarkBtn.setTooltip(new Tooltip("Save post"));
        try {
            if (forumService.isBookmarked(post.getId(), currentUserId)) {
                bookmarkBtn.setText("\uD83D\uDD16  Saved");
                bookmarkBtn.setStyle("-fx-text-fill: #9B7E46; -fx-font-weight: bold;");
            } else {
                bookmarkBtn.setText("\uD83D\uDD16  Save");
            }
        } catch (SQLException e) {}
        bookmarkBtn.setOnAction(e -> {
            e.consume();
            try {
                forumService.toggleBookmark(post.getId(), currentUserId);
                loadPosts();
            } catch (SQLException ex) {
                showError("Bookmark failed: " + ex.getMessage());
            }
        });
        
        // Share button with dropdown
        MenuButton shareBtn = new MenuButton("\uD83D\uDD17  Share");
        shareBtn.getStyleClass().addAll("icon-btn", "icon-btn-comment");
        shareBtn.setTooltip(new Tooltip("Share post"));
        
        String shareTitle = post.getTitle() != null ? post.getTitle() : "";
        String shareContent = post.getContent() != null ? post.getContent() : "";
        String shareText = (shareTitle.isEmpty() ? "" : shareTitle + "\n\n") + shareContent + "\n\n— Shared from INVESTI Forum";
        
        MenuItem shareFacebook = new MenuItem("\uD83D\uDCD8  Facebook");
        shareFacebook.setOnAction(e -> {
            try {
                // Facebook doesn't support pre-filled text via URL, so we copy to clipboard and open Facebook
                javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
                javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
                cc.putString(shareText);
                clipboard.setContent(cc);
                java.awt.Desktop.getDesktop().browse(new URI("https://www.facebook.com/"));
                showSuccess("Post content copied! Paste it on Facebook.");
            } catch (Exception ex) {
                showError("Could not open browser: " + ex.getMessage());
            }
        });
        
        MenuItem shareX = new MenuItem("\uD835\uDD4F  X (Twitter)");
        shareX.setOnAction(e -> {
            try {
                String tweetText = shareText.length() > 280 ? shareText.substring(0, 277) + "..." : shareText;
                String url = "https://twitter.com/intent/tweet?text=" + URLEncoder.encode(tweetText, "UTF-8");
                java.awt.Desktop.getDesktop().browse(new URI(url));
            } catch (Exception ex) {
                showError("Could not open browser: " + ex.getMessage());
            }
        });
        
        MenuItem shareLinkedIn = new MenuItem("\uD83D\uDCBC  LinkedIn");
        shareLinkedIn.setOnAction(e -> {
            try {
                // LinkedIn doesn't support pre-filled text via URL, so we copy to clipboard and open LinkedIn share
                javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
                javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
                cc.putString(shareText);
                clipboard.setContent(cc);
                java.awt.Desktop.getDesktop().browse(new URI("https://www.linkedin.com/feed/"));
                showSuccess("Post content copied! Paste it on LinkedIn.");
            } catch (Exception ex) {
                showError("Could not open browser: " + ex.getMessage());
            }
        });
        
        MenuItem copyText = new MenuItem("\uD83D\uDCCB  Copy Text");
        copyText.setOnAction(e -> {
            javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
            cc.putString(shareText);
            clipboard.setContent(cc);
            showSuccess("Post content copied to clipboard!");
        });
        
        shareBtn.getItems().addAll(shareFacebook, shareX, shareLinkedIn, copyText);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        actions.getChildren().addAll(commentBtn, upvoteBtn, downvoteBtn, viewsLabel, translateBtn, bookmarkBtn, shareBtn, spacer);
        
        // Edit/Delete for own posts OR if user is admin
        boolean isOwner = post.getUserId() != null && post.getUserId().equals(currentUserId);
        boolean canModerate = isAdmin();
        
        if (isOwner) {
            Button editBtn = new Button("\u270E  Edit");
            editBtn.getStyleClass().addAll("icon-btn", "icon-btn-edit");
            editBtn.setTooltip(new Tooltip("Edit post"));
            editBtn.setOnAction(e -> {
                e.consume();
                showEditPostDialog(post);
            });
            actions.getChildren().add(editBtn);
        }
        
        if (isOwner || canModerate) {
            Button deleteBtn = new Button("\uD83D\uDDD1  Delete");
            deleteBtn.getStyleClass().addAll("icon-btn", "icon-btn-delete");
            if (canModerate && !isOwner) {
                deleteBtn.setTooltip(new Tooltip("Delete (Admin)"));
            } else {
                deleteBtn.setTooltip(new Tooltip("Delete post"));
            }
            deleteBtn.setOnAction(e -> {
                e.consume();
                handleDeletePost(post);
            });
            actions.getChildren().add(deleteBtn);
        }
        
        return actions;
    }

    private void handleVote(ForumPost post, String voteType) {
        try {
            // Record view when user interacts with post
            forumService.recordPostView(post.getId(), currentUserId);
            forumService.votePost(post.getId(), currentUserId, voteType);
            loadPosts();
        } catch (SQLException e) {
            showError("Vote failed: " + e.getMessage());
        }
    }

    private void showTranslatedPost(ForumPost post, Language targetLang) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.setTitle("Translation");

        VBox root = new VBox(15);
        root.setStyle("-fx-background-color: " + ThemeManager.bg() + "; -fx-border-color: " + ThemeManager.border() + "; -fx-border-radius: 16; -fx-background-radius: 16; -fx-padding: 20;");
        root.setMaxWidth(600);

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: " + ThemeManager.text() + "; -fx-font-size: 18px; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> dialog.close());
        
        Label titleLbl = new Label("🌐 Translation to " + targetLang.getDisplayName());
        titleLbl.setStyle("-fx-text-fill: " + ThemeManager.text() + "; -fx-font-size: 18px; -fx-font-weight: bold;");
        
        header.getChildren().addAll(closeBtn, titleLbl);

        // Loading indicator
        Label loadingLabel = new Label("Translating...");
        loadingLabel.setStyle("-fx-text-fill: #71767b; -fx-font-size: 14px;");
        
        root.getChildren().addAll(header, loadingLabel);

        Scene scene = new Scene(root, 550, 400);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        dialog.setScene(scene);
        ThemeManager.registerScene(scene);
        dialog.show();

        // Run translation in background thread
        new Thread(() -> {
            String translatedTitle = post.getTitle() != null ? 
                    TranslationService.translate(post.getTitle(), targetLang) : null;
            String translatedContent = post.getContent() != null ? 
                    TranslationService.translate(post.getContent(), targetLang) : null;

            // Update UI on JavaFX thread
            javafx.application.Platform.runLater(() -> {
                root.getChildren().remove(loadingLabel);

                // Original section
                Label origLabel = new Label("Original:");
                origLabel.setStyle("-fx-text-fill: #71767b; -fx-font-size: 12px;");
                
                VBox origBox = new VBox(5);
                origBox.setStyle("-fx-background-color: " + ThemeManager.card() + "; -fx-padding: 10; -fx-background-radius: 8;");
                
                if (post.getTitle() != null && !post.getTitle().isEmpty()) {
                    Label origTitle = new Label(post.getTitle());
                    origTitle.setStyle("-fx-text-fill: " + ThemeManager.text() + "; -fx-font-weight: bold;");
                    origTitle.setWrapText(true);
                    origBox.getChildren().add(origTitle);
                }
                if (post.getContent() != null && !post.getContent().isEmpty()) {
                    Label origContent = new Label(post.getContent());
                    origContent.setStyle("-fx-text-fill: " + ThemeManager.text() + ";");
                    origContent.setWrapText(true);
                    origBox.getChildren().add(origContent);
                }

                // Translated section
                Label transLabel = new Label("Translated (" + targetLang.getDisplayName() + "):");
                transLabel.setStyle("-fx-text-fill: #456990; -fx-font-size: 12px; -fx-padding: 10 0 0 0;");
                
                VBox transBox = new VBox(5);
                transBox.setStyle("-fx-background-color: " + ThemeManager.card() + "; -fx-padding: 10; -fx-background-radius: 8; -fx-border-color: #456990; -fx-border-radius: 8;");
                
                if (translatedTitle != null && !translatedTitle.isEmpty()) {
                    Label transTitle = new Label(translatedTitle);
                    transTitle.setStyle("-fx-text-fill: " + ThemeManager.text() + "; -fx-font-weight: bold;");
                    transTitle.setWrapText(true);
                    transBox.getChildren().add(transTitle);
                }
                if (translatedContent != null && !translatedContent.isEmpty()) {
                    Label transContent = new Label(translatedContent);
                    transContent.setStyle("-fx-text-fill: " + ThemeManager.text() + ";");
                    transContent.setWrapText(true);
                    transBox.getChildren().add(transContent);
                }

                ScrollPane scrollPane = new ScrollPane();
                VBox contentBox = new VBox(10);
                contentBox.getChildren().addAll(origLabel, origBox, transLabel, transBox);
                scrollPane.setContent(contentBox);
                scrollPane.setFitToWidth(true);
                scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
                VBox.setVgrow(scrollPane, Priority.ALWAYS);

                root.getChildren().add(scrollPane);
            });
        }).start();
    }

    private void showFullImage(String imagePath) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UNDECORATED);
        
        try {
            ImageView imageView = new ImageView();
            File file = new File(imagePath);
            if (file.exists()) {
                imageView.setImage(new Image(file.toURI().toString()));
            }
            imageView.setPreserveRatio(true);
            imageView.setFitWidth(900);
            imageView.setFitHeight(700);
            
            StackPane root = new StackPane(imageView);
            root.setStyle("-fx-background-color: rgba(0,0,0,0.9); -fx-cursor: hand;");
            root.setOnMouseClicked(e -> dialog.close());
            
            Scene scene = new Scene(root, 900, 700);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            dialog.setScene(scene);
        ThemeManager.registerScene(scene);
            dialog.showAndWait();
        } catch (Exception e) {
            showError("Failed to load image");
        }
    }


    private void showCreatePostDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.setTitle("Create Post");
        
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: " + ThemeManager.bg() + "; -fx-border-color: " + ThemeManager.border() + "; -fx-border-radius: 16; -fx-background-radius: 16;");
        root.setMaxWidth(600);
        
        // Header
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-padding: 10 15; -fx-border-color: " + ThemeManager.border() + "; -fx-border-width: 0 0 1 0;");
        
        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: " + ThemeManager.text() + "; -fx-font-size: 18px; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> dialog.close());
        
        header.getChildren().add(closeBtn);
        
        // Content
        VBox content = new VBox(15);
        content.setStyle("-fx-padding: 20;");
        
        HBox composeRow = new HBox(12);
        Label avatar = new Label(getInitials(currentUserName));
        avatar.getStyleClass().add("user-avatar");
        
        VBox inputCol = new VBox(10);
        HBox.setHgrow(inputCol, Priority.ALWAYS);
        
        TextField titleField = new TextField();
        titleField.setPromptText("Title (optional)");
        titleField.getStyleClass().add("text-input-dark");
        
        TextArea contentArea = new TextArea();
        contentArea.setPromptText("What's on your mind?");
        contentArea.setPrefRowCount(5);
        contentArea.getStyleClass().add("text-area-dark");
        contentArea.setWrapText(true);
        
        ComboBox<String> categoryBox = new ComboBox<>();
        categoryBox.getItems().addAll("General", "Tips & Advice", "Success Stories", 
                "Investor Insights", "Collaboration", "Announcements");
        categoryBox.setValue("General");
        categoryBox.setStyle("-fx-background-color: " + ThemeManager.inputBg() + "; -fx-text-fill: " + ThemeManager.text() + ";");
        categoryBox.setMaxWidth(Double.MAX_VALUE);
        
        // Images preview
        List<String> selectedImages = new ArrayList<>();
        HBox imagesPreview = new HBox(8);
        imagesPreview.setStyle("-fx-padding: 10 0;");
        
        Button addImgBtn = new Button("🖼 Add Photos");
        addImgBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #456990; -fx-cursor: hand;");
        addImgBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));
            List<File> files = fc.showOpenMultipleDialog(dialog);
            if (files != null) {
                selectedImages.clear();
                imagesPreview.getChildren().clear();
                for (File f : files) {
                    selectedImages.add(f.getAbsolutePath());
                    try {
                        ImageView thumb = new ImageView(new Image(f.toURI().toString()));
                        thumb.setFitHeight(60);
                        thumb.setPreserveRatio(true);
                        imagesPreview.getChildren().add(thumb);
                    } catch (Exception ex) {}
                }
            }
        });
        
        inputCol.getChildren().addAll(titleField, contentArea, categoryBox, addImgBtn, imagesPreview);
        composeRow.getChildren().addAll(avatar, inputCol);
        
        // Footer
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setStyle("-fx-padding: 15 20; -fx-border-color: " + ThemeManager.border() + "; -fx-border-width: 1 0 0 0;");
        
        Button postBtn = new Button("Post");
        postBtn.getStyleClass().add("btn-primary");
        postBtn.setOnAction(e -> {
            String title = titleField.getText().trim();
            String postContent = contentArea.getText().trim();
            
            if (title.isEmpty() && postContent.isEmpty() && selectedImages.isEmpty()) {
                showError("Add some content to your post");
                return;
            }
            
            ForumPost newPost = new ForumPost(currentUserId,
                    title.isEmpty() ? null : title,
                    postContent.isEmpty() ? null : postContent,
                    categoryBox.getValue());
            
            for (String img : selectedImages) {
                newPost.addImagePath(img);
            }
            
            try {
                // Validate post before saving
                String validationError = forumService.validatePost(newPost);
                if (validationError != null) {
                    showError(validationError);
                    return;
                }
                
                String newPostId = forumService.addPost(newPost);
                // Process @mention notifications
                if (newPostId != null) {
                    String fullText = (title.isEmpty() ? "" : title + " ") + postContent;
                    processMentionNotifications(fullText, newPostId, null);
                }
                dialog.close();
                loadPosts();
                updateNotificationBadge();
                showSuccess("Post published!");
            } catch (SQLException ex) {
                showError("Failed to create post: " + ex.getMessage());
            }
        });
        
        footer.getChildren().add(postBtn);
        
        content.getChildren().add(composeRow);
        root.getChildren().addAll(header, content, footer);
        
        Scene scene = new Scene(root, 600, 450);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        dialog.setScene(scene);
        ThemeManager.registerScene(scene);
        dialog.showAndWait();
    }

    private void showEditPostDialog(ForumPost post) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UNDECORATED);
        
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: " + ThemeManager.bg() + "; -fx-border-color: " + ThemeManager.border() + "; -fx-border-radius: 16; -fx-background-radius: 16;");
        root.setMaxWidth(600);
        
        // Header
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-padding: 10 15; -fx-border-color: " + ThemeManager.border() + "; -fx-border-width: 0 0 1 0;");
        
        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: " + ThemeManager.text() + "; -fx-font-size: 18px; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> dialog.close());
        
        Label titleLbl = new Label("Edit Post");
        titleLbl.setStyle("-fx-text-fill: " + ThemeManager.text() + "; -fx-font-size: 18px; -fx-font-weight: bold; -fx-padding: 0 0 0 15;");
        
        header.getChildren().addAll(closeBtn, titleLbl);
        
        // Content
        VBox content = new VBox(15);
        content.setStyle("-fx-padding: 20;");
        
        TextField titleField = new TextField(post.getTitle() != null ? post.getTitle() : "");
        titleField.setPromptText("Title (optional)");
        titleField.getStyleClass().add("text-input-dark");
        
        TextArea contentArea = new TextArea(post.getContent() != null ? post.getContent() : "");
        contentArea.setPromptText("What's on your mind?");
        contentArea.setPrefRowCount(5);
        contentArea.getStyleClass().add("text-area-dark");
        contentArea.setWrapText(true);
        
        ComboBox<String> categoryBox = new ComboBox<>();
        categoryBox.getItems().addAll("General", "Tips & Advice", "Success Stories", 
                "Investor Insights", "Collaboration", "Announcements");
        categoryBox.setValue(post.getCategory() != null ? post.getCategory() : "General");
        categoryBox.setStyle("-fx-background-color: " + ThemeManager.inputBg() + "; -fx-text-fill: " + ThemeManager.text() + ";");
        categoryBox.setMaxWidth(Double.MAX_VALUE);
        
        content.getChildren().addAll(titleField, contentArea, categoryBox);
        
        // Footer
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setStyle("-fx-padding: 15 20; -fx-border-color: " + ThemeManager.border() + "; -fx-border-width: 1 0 0 0;");
        
        Button saveBtn = new Button("Save");
        saveBtn.getStyleClass().add("btn-primary");
        saveBtn.setOnAction(e -> {
            post.setTitle(titleField.getText().trim().isEmpty() ? null : titleField.getText().trim());
            post.setContent(contentArea.getText().trim().isEmpty() ? null : contentArea.getText().trim());
            post.setCategory(categoryBox.getValue());
            
            try {
                forumService.updatePost(post.getId(), post);
                dialog.close();
                loadPosts();
            } catch (SQLException ex) {
                showError("Failed to update: " + ex.getMessage());
            }
        });
        
        footer.getChildren().add(saveBtn);
        root.getChildren().addAll(header, content, footer);
        
        Scene scene = new Scene(root, 600, 400);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        dialog.setScene(scene);
        ThemeManager.registerScene(scene);
        dialog.showAndWait();
    }

    private void handleDeletePost(ForumPost post) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Post");
        confirm.setHeaderText("Delete this post?");
        confirm.setContentText("This can't be undone.");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    forumService.deletePost(post.getId());
                    loadPosts();
                } catch (SQLException e) {
                    showError("Failed to delete: " + e.getMessage());
                }
            }
        });
    }


    private void showPostDetails(ForumPost post) {
        // Record view for current user (only counts once per user)
        try {
            forumService.recordPostView(post.getId(), currentUserId);
        } catch (SQLException e) {}
        
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Post");
        
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + ThemeManager.bg() + ";");
        
        // Header
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-padding: 15 20; -fx-background-color: " + ThemeManager.headerBg() + "; -fx-border-color: " + ThemeManager.border() + "; -fx-border-width: 0 0 1 0;");
        
        Button backBtn = new Button("← Back");
        backBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: " + ThemeManager.text() + "; -fx-font-size: 15px; -fx-cursor: hand;");
        backBtn.setOnAction(e -> dialog.close());
        
        Label titleLbl = new Label("Post");
        titleLbl.setStyle("-fx-text-fill: " + ThemeManager.text() + "; -fx-font-size: 18px; -fx-font-weight: bold; -fx-padding: 0 0 0 20;");
        
        header.getChildren().addAll(backBtn, titleLbl);
        root.setTop(header);
        
        // Content scroll
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("feed-scroll");
        scroll.setStyle("-fx-background: " + ThemeManager.bg() + "; -fx-background-color: " + ThemeManager.bg() + ";");
        
        // Build content and set it
        scroll.setContent(buildPostDetailsContent(post, dialog, scroll));
        root.setCenter(scroll);
        
        Scene scene = new Scene(root, 700, 700);
        dialog.setScene(scene);
        ThemeManager.registerScene(scene);
        dialog.showAndWait();
        
        loadPosts();
    }

    /**
     * Refreshes the post details content inside the existing dialog without closing/reopening.
     */
    private void refreshPostDetails(ForumPost post, Stage dialog, ScrollPane scroll) {
        try {
            // Re-fetch the post to get updated vote counts
            ForumPost refreshedPost = forumService.getPostById(post.getId());
            if (refreshedPost == null) refreshedPost = post;
            scroll.setContent(buildPostDetailsContent(refreshedPost, dialog, scroll));
            scroll.setVvalue(0);
        } catch (SQLException e) {
            scroll.setContent(buildPostDetailsContent(post, dialog, scroll));
        }
    }

    /**
     * Builds the VBox content for the post details view.
     */
    private VBox buildPostDetailsContent(ForumPost post, Stage dialog, ScrollPane scroll) {
        VBox content = new VBox(0);
        content.setStyle("-fx-background-color: " + ThemeManager.bg() + ";");
        
        // Post
        VBox postBox = new VBox(10);
        postBox.setStyle("-fx-padding: 20; -fx-border-color: " + ThemeManager.border() + "; -fx-border-width: 0 0 1 0;");
        
        HBox postHeader = new HBox(12);
        Label avatar = new Label(getInitials(post.getAuthorName()));
        avatar.getStyleClass().add("user-avatar");
        
        VBox authorInfo = new VBox(2);
        Label authorName = new Label(post.getAuthorName() != null ? post.getAuthorName() : "Anonymous");
        authorName.setStyle("-fx-text-fill: " + ThemeManager.text() + "; -fx-font-weight: bold; -fx-font-size: 15px;");
        Label postTime = new Label(formatDate(post.getCreatedAt()));
        postTime.setStyle("-fx-text-fill: " + ThemeManager.textSec() + "; -fx-font-size: 13px;");
        authorInfo.getChildren().addAll(authorName, postTime);
        
        postHeader.getChildren().addAll(avatar, authorInfo);
        postBox.getChildren().add(postHeader);
        
        if (post.getTitle() != null && !post.getTitle().isEmpty()) {
            if (MentionParser.hasMentions(post.getTitle())) {
                TextFlow titleFlow = MentionParser.createStyledText(post.getTitle(), ThemeManager.text(), "22px");
                titleFlow.setStyle("-fx-font-weight: bold;");
                postBox.getChildren().add(titleFlow);
            } else {
                Label title = new Label(post.getTitle());
                title.setStyle("-fx-text-fill: " + ThemeManager.text() + "; -fx-font-size: 22px; -fx-font-weight: bold;");
                title.setWrapText(true);
                postBox.getChildren().add(title);
            }
        }
        
        if (post.getContent() != null && !post.getContent().isEmpty()) {
            if (MentionParser.hasMentions(post.getContent())) {
                TextFlow contentFlow = MentionParser.createStyledText(post.getContent(), ThemeManager.text(), "16px");
                postBox.getChildren().add(contentFlow);
            } else {
                Label contentLbl = new Label(post.getContent());
                contentLbl.setStyle("-fx-text-fill: " + ThemeManager.text() + "; -fx-font-size: 16px;");
                contentLbl.setWrapText(true);
                postBox.getChildren().add(contentLbl);
            }
        }
        
        if (post.hasImages()) {
            HBox images = createImagesPreview(post.getImagePaths());
            postBox.getChildren().add(images);
        }
        
        // Actions
        HBox actions = createActionsRow(post);
        postBox.getChildren().add(actions);
        
        content.getChildren().add(postBox);
        
        // Add comment box
        VBox addCommentBox = new VBox(10);
        addCommentBox.setStyle("-fx-padding: 15 20; -fx-border-color: " + ThemeManager.border() + "; -fx-border-width: 0 0 1 0;");
        
        HBox commentRow = new HBox(12);
        Label commentAvatar = new Label(getInitials(currentUserName));
        commentAvatar.getStyleClass().add("user-avatar-small");
        
        TextArea commentArea = new TextArea();
        commentArea.setPromptText("Post your reply");
        commentArea.setPrefRowCount(2);
        commentArea.getStyleClass().add("text-area-dark");
        commentArea.setWrapText(true);
        HBox.setHgrow(commentArea, Priority.ALWAYS);
        
        Button replyBtn = new Button("Reply");
        replyBtn.getStyleClass().add("btn-primary");
        replyBtn.setOnAction(e -> {
            if (commentArea.getText().trim().isEmpty()) return;
            
            // Validate comment for bad words
            String validationError = forumService.validateComment(commentArea.getText().trim());
            if (validationError != null) {
                showError(validationError);
                return;
            }
            
            ForumComment comment = new ForumComment(post.getId(), currentUserId, commentArea.getText().trim());
            try {
                String commentId = forumService.addComment(comment);
                // Process @mention notifications in comment
                processMentionNotifications(commentArea.getText().trim(), post.getId(), commentId);
                updateNotificationBadge();
                refreshPostDetails(post, dialog, scroll);
            } catch (SQLException ex) {
                showError("Failed to reply: " + ex.getMessage());
            }
        });
        
        VBox replyCol = new VBox(8);
        replyCol.getChildren().addAll(commentArea, replyBtn);
        HBox.setHgrow(replyCol, Priority.ALWAYS);
        
        commentRow.getChildren().addAll(commentAvatar, replyCol);
        addCommentBox.getChildren().add(commentRow);
        content.getChildren().add(addCommentBox);
        
        // Comments
        try {
            List<ForumComment> comments = forumService.getCommentsByPost(post.getId());
            for (ForumComment comment : comments) {
                VBox commentCard = createCommentCard(comment, post, dialog, scroll, 0);
                content.getChildren().add(commentCard);
            }
            
            if (comments.isEmpty()) {
                Label noComments = new Label("No replies yet");
                noComments.setStyle("-fx-text-fill: " + ThemeManager.textSec() + "; -fx-font-size: 14px; -fx-padding: 20;");
                content.getChildren().add(noComments);
            }
        } catch (SQLException e) {
            Label error = new Label("Failed to load comments");
            error.setStyle("-fx-text-fill: #f4212e;");
            content.getChildren().add(error);
        }
        
        return content;
    }

    private VBox createCommentCard(ForumComment comment, ForumPost post, Stage dialog, ScrollPane scroll, int depth) {
        VBox card = new VBox(8);
        card.getStyleClass().add(depth > 0 ? "comment-reply" : "comment-card");
        card.setStyle(card.getStyle() + "-fx-padding: 15 20;");
        
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label avatar = new Label(getInitials(comment.getAuthorName()));
        avatar.setStyle("-fx-background-color: #456990; -fx-background-radius: 15; " +
                "-fx-min-width: 32; -fx-min-height: 32; -fx-max-width: 32; -fx-max-height: 32; " +
                "-fx-alignment: center; -fx-text-fill: white; -fx-font-size: 11px;");
        
        Label name = new Label(comment.getAuthorName() != null ? comment.getAuthorName() : "Anonymous");
        name.setStyle("-fx-text-fill: " + ThemeManager.text() + "; -fx-font-weight: bold;");
        
        Label time = new Label("· " + getRelativeTime(comment.getCreatedAt()));
        time.setStyle("-fx-text-fill: " + ThemeManager.textSec() + "; -fx-font-size: 13px;");
        
        header.getChildren().addAll(avatar, name, time);
        
        Label contentLbl;
        TextFlow contentFlow = null;
        if (MentionParser.hasMentions(comment.getContent())) {
            contentFlow = MentionParser.createStyledText(comment.getContent(), ThemeManager.text(), "14px");
        } else {
            contentLbl = new Label(comment.getContent());
            contentLbl.setStyle("-fx-text-fill: " + ThemeManager.text() + "; -fx-font-size: 14px;");
            contentLbl.setWrapText(true);
        }
        
        // Actions
        HBox actions = new HBox(4);
        actions.setStyle("-fx-padding: 8 0 0 0;");
        
        Button upBtn = new Button("\u25B2  " + comment.getUpvotes());
        upBtn.getStyleClass().addAll("icon-btn", "icon-btn-upvote");
        upBtn.setTooltip(new Tooltip("Upvote"));
        upBtn.setOnAction(e -> {
            try {
                forumService.voteComment(comment.getId(), currentUserId, "upvote");
                ForumComment updated = forumService.getCommentById(comment.getId());
                if (updated != null) {
                    upBtn.setText("\u25B2  " + updated.getUpvotes());
                    ((Button)actions.getChildren().get(1)).setText("\u25BC  " + updated.getDownvotes());
                }
            } catch (SQLException ex) {}
        });
        
        Button downBtn = new Button("\u25BC  " + comment.getDownvotes());
        downBtn.getStyleClass().addAll("icon-btn", "icon-btn-downvote");
        downBtn.setTooltip(new Tooltip("Downvote"));
        downBtn.setOnAction(e -> {
            try {
                forumService.voteComment(comment.getId(), currentUserId, "downvote");
                ForumComment updated = forumService.getCommentById(comment.getId());
                if (updated != null) {
                    ((Button)actions.getChildren().get(0)).setText("\u25B2  " + updated.getUpvotes());
                    downBtn.setText("\u25BC  " + updated.getDownvotes());
                }
            } catch (SQLException ex) {}
        });
        
        Button replyBtn = new Button("\u21A9  Reply");
        replyBtn.getStyleClass().addAll("icon-btn", "icon-btn-reply");
        replyBtn.setTooltip(new Tooltip("Reply"));
        replyBtn.setOnAction(e -> showReplyDialog(comment, post, dialog, scroll));
        
        actions.getChildren().addAll(upBtn, downBtn, replyBtn);
        
        // Delete button for own comments OR if user is admin
        boolean isCommentOwner = comment.getUserId() != null && comment.getUserId().equals(currentUserId);
        if (isCommentOwner || isAdmin()) {
            Button deleteBtn = new Button("\uD83D\uDDD1  Delete");
            deleteBtn.getStyleClass().addAll("icon-btn", "icon-btn-delete");
            if (isAdmin() && !isCommentOwner) {
                deleteBtn.setTooltip(new Tooltip("Delete (Admin)"));
            } else {
                deleteBtn.setTooltip(new Tooltip("Delete comment"));
            }
            deleteBtn.setOnAction(e -> {
                try {
                    forumService.deleteComment(comment.getId());
                    refreshPostDetails(post, dialog, scroll);
                } catch (SQLException ex) {}
            });
            actions.getChildren().add(deleteBtn);
        }
        
        if (contentFlow != null) {
            card.getChildren().addAll(header, contentFlow, actions);
        } else {
            contentLbl = new Label(comment.getContent());
            contentLbl.setStyle("-fx-text-fill: " + ThemeManager.text() + "; -fx-font-size: 14px;");
            contentLbl.setWrapText(true);
            card.getChildren().addAll(header, contentLbl, actions);
        }
        
        // Nested replies
        if (comment.getReplies() != null && !comment.getReplies().isEmpty()) {
            for (ForumComment reply : comment.getReplies()) {
                VBox replyCard = createCommentCard(reply, post, dialog, scroll, depth + 1);
                card.getChildren().add(replyCard);
            }
        }
        
        return card;
    }

    private void showReplyDialog(ForumComment parent, ForumPost post, Stage parentDialog, ScrollPane parentScroll) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UNDECORATED);
        
        VBox root = new VBox(15);
        root.setStyle("-fx-background-color: " + ThemeManager.bg() + "; -fx-border-color: " + ThemeManager.border() + "; -fx-border-radius: 16; -fx-background-radius: 16; -fx-padding: 20;");
        
        Label title = new Label("Reply to " + (parent.getAuthorName() != null ? parent.getAuthorName() : "comment"));
        title.setStyle("-fx-text-fill: " + ThemeManager.text() + "; -fx-font-size: 16px; -fx-font-weight: bold;");
        
        TextArea replyArea = new TextArea();
        replyArea.setPromptText("Write your reply...");
        replyArea.setPrefRowCount(3);
        replyArea.getStyleClass().add("text-area-dark");
        
        HBox buttons = new HBox(10);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        
        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-secondary");
        cancelBtn.setOnAction(e -> dialog.close());
        
        Button submitBtn = new Button("Reply");
        submitBtn.getStyleClass().add("btn-primary");
        submitBtn.setOnAction(e -> {
            if (replyArea.getText().trim().isEmpty()) return;
            
            // Validate reply for bad words
            String validationError = forumService.validateComment(replyArea.getText().trim());
            if (validationError != null) {
                showError(validationError);
                return;
            }
            
            ForumComment reply = new ForumComment(post.getId(), currentUserId, replyArea.getText().trim());
            reply.setParentCommentId(parent.getId());
            
            try {
                String replyId = forumService.addComment(reply);
                // Process @mention notifications in reply
                processMentionNotifications(replyArea.getText().trim(), post.getId(), replyId);
                updateNotificationBadge();
                dialog.close();
                refreshPostDetails(post, parentDialog, parentScroll);
            } catch (SQLException ex) {
                showError("Failed: " + ex.getMessage());
            }
        });
        
        buttons.getChildren().addAll(cancelBtn, submitBtn);
        root.getChildren().addAll(title, replyArea, buttons);
        
        Scene scene = new Scene(root, 400, 200);
        dialog.setScene(scene);
        ThemeManager.registerScene(scene);
        dialog.showAndWait();
    }

    private String getInitials(String name) {
        if (name == null || name.isEmpty()) return "?";
        String[] parts = name.split(" ");
        if (parts.length >= 2) {
            return (parts[0].charAt(0) + "" + parts[1].charAt(0)).toUpperCase();
        }
        return name.substring(0, Math.min(2, name.length())).toUpperCase();
    }

    private String formatDate(java.sql.Timestamp timestamp) {
        if (timestamp == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");
        return sdf.format(timestamp);
    }

    private String getRelativeTime(java.sql.Timestamp timestamp) {
        if (timestamp == null) return "";
        long diff = System.currentTimeMillis() - timestamp.getTime();
        
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
        if (minutes < 1) return "now";
        if (minutes < 60) return minutes + "m";
        
        long hours = TimeUnit.MILLISECONDS.toHours(diff);
        if (hours < 24) return hours + "h";
        
        long days = TimeUnit.MILLISECONDS.toDays(diff);
        if (days < 7) return days + "d";
        
        return new SimpleDateFormat("MMM dd").format(timestamp);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
