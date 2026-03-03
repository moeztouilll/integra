package edu.connexion3a8;

import edu.connexion3a8.entities.User;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class InvestiApp extends Application {

    private static Stage primaryStage;
    private static User currentUser;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;

        // Set app icon
        try {
            Image icon = new Image(getClass().getResourceAsStream("/INVESTI.png"));
            stage.getIcons().add(icon);
        } catch (Exception e) {
            System.out.println("Could not load app icon");
        }

        // Maximize the window
        stage.setMaximized(true);

        // Load login page
        showLoginPage();

        stage.setTitle("INVESTI - Where Innovation Meets Investment");
        stage.show();
    }

    public static void showLoginPage() throws Exception {
        currentUser = null;
        FXMLLoader loader = new FXMLLoader(InvestiApp.class.getResource("/Login.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
    }

    public static void showHomePage() throws Exception {
        FXMLLoader loader = new FXMLLoader(InvestiApp.class.getResource("/Home.fxml"));
        Parent root = loader.load();

        // Pass current user to controller
        if (loader.getController() != null) {
            edu.connexion3a8.controllers.HomeController controller = loader.getController();
            controller.setCurrentUser(currentUser);
        }

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
    }

    public static void showHomePage(User user) throws Exception {
        currentUser = user;
        showHomePage();
    }

    public static void showEventsPage(User user) throws Exception {
        currentUser = user;
        FXMLLoader loader = new FXMLLoader(InvestiApp.class.getResource("/EventsPage.fxml"));
        Parent root = loader.load();

        if (loader.getController() != null) {
            edu.connexion3a8.controllers.EventsPageController controller = loader.getController();
            controller.setCurrentUser(currentUser);
        }

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
    }

    public static void showEventManagement() throws Exception {
        FXMLLoader loader = new FXMLLoader(InvestiApp.class.getResource("/EventManagement.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
    }

    public static void showAdminDashboard() throws Exception {
        FXMLLoader loader = new FXMLLoader(InvestiApp.class.getResource("/AdminDashboard.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    // ============================================
    // Collaboration Module Navigation Methods with Role-Based Access
    // ============================================

    public static void showCollaborationModule(User user) throws Exception {
        currentUser = user;

        if (user == null) {
            showLoginPage();
            return;
        }

        // Route based on role
        switch (user.getRole().toLowerCase()) {
            case "admin":
                showCollaborationAdmin(user);
                break;
            case "investor":
                showInvestorDashboard(user);
                break;
            case "innovator":
                showEntrepreneurDashboard(user);
                break;
            default:
                // Other roles don't have access to collaboration
                showAlert("Access Denied", "You don't have access to the collaboration module.");
                showHomePage(user);
        }
    }

    public static void showCollaborationMain(User user) throws Exception {
        currentUser = user;
        FXMLLoader loader = new FXMLLoader(InvestiApp.class.getResource("/collaboration/Main.fxml"));
        Parent root = loader.load();

        if (loader.getController() != null) {
            edu.connexion3a8.controllers.collaboration.MainController controller = loader.getController();
            controller.setCurrentUser(currentUser);
        }

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
    }

    public static void showEntrepreneurDashboard(User user) throws Exception {
        currentUser = user;

        // Check role
        if (!user.getRole().equalsIgnoreCase("innovator") && !user.getRole().equalsIgnoreCase("admin")) {
            showAlert("Access Denied", "Only innovators can access the entrepreneur dashboard.");
            showHomePage(user);
            return;
        }

        FXMLLoader loader = new FXMLLoader(InvestiApp.class.getResource("/collaboration/EntrepreneurDashboard.fxml"));
        Parent root = loader.load();

        if (loader.getController() != null) {
            edu.connexion3a8.controllers.collaboration.EntrepreneurController controller = loader.getController();
            controller.setCurrentUser(currentUser);
        }

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
    }

    public static void showInvestorDashboard(User user) throws Exception {
        currentUser = user;

        // Check role
        if (!user.getRole().equalsIgnoreCase("investor") && !user.getRole().equalsIgnoreCase("admin")) {
            showAlert("Access Denied", "Only investors can access the investor dashboard.");
            showHomePage(user);
            return;
        }

        FXMLLoader loader = new FXMLLoader(InvestiApp.class.getResource("/collaboration/InvestorDashboard.fxml"));
        Parent root = loader.load();

        if (loader.getController() != null) {
            edu.connexion3a8.controllers.collaboration.InvestorController controller = loader.getController();
            controller.setCurrentUser(currentUser);
        }

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
    }

    public static void showCollaborationAdmin(User user) throws Exception {
        currentUser = user;

        // Check role
        if (!user.getRole().equalsIgnoreCase("admin")) {
            showAlert("Access Denied", "Only administrators can access the admin dashboard.");
            showHomePage(user);
            return;
        }

        FXMLLoader loader = new FXMLLoader(InvestiApp.class.getResource("/collaboration/AdminDashboard.fxml"));
        Parent root = loader.load();

        if (loader.getController() != null) {
            edu.connexion3a8.controllers.collaboration.AdminController controller = loader.getController();
            controller.setCurrentUser(currentUser);
        }

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
    }

    public static void showProductManagement() throws Exception {
        FXMLLoader loader = new FXMLLoader(InvestiApp.class.getResource("/collaboration/ProductManagement.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
    }

    public static void showProductCatalog() throws Exception {
        FXMLLoader loader = new FXMLLoader(InvestiApp.class.getResource("/collaboration/ProductCatalog.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
    }

    public static void showGamificationMenu() throws Exception {
        FXMLLoader loader = new FXMLLoader(InvestiApp.class.getResource("/gamification/MainMenu.fxml"));
        Parent root = loader.load();

        if (loader.getController() != null && currentUser != null) {
            edu.connexion3a8.controllers.gamification.MainMenuController controller = loader.getController();
            controller.setUser(currentUser);
        }

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
    }

    public static void showGamificationMenu(User user) throws Exception {
        currentUser = user;
        showGamificationMenu();
    }

    public static void showForum() throws Exception {
        FXMLLoader loader = new FXMLLoader(InvestiApp.class.getResource("/Forum.fxml"));
        Parent root = loader.load();

        if (loader.getController() != null && currentUser != null) {
            // Assuming ForumController has setCurrentUser method
            try {
                edu.connexion3a8.controllers.ForumController controller = loader.getController();
                controller.setCurrentUser(currentUser);
            } catch (Exception e) {
                System.out.println("Forum controller doesn't have setCurrentUser method or doesn't exist");
            }
        }

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
    }

    public static void showForum(User user) throws Exception {
        currentUser = user;
        showForum();
    }

    private static void showAlert(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
