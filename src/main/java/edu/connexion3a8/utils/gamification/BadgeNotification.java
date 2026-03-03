package edu.connexion3a8.utils.gamification;

import edu.connexion3a8.entities.gamification.Badge;
import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * Beautiful animated notification for badge awards
 */
public class BadgeNotification {
    
    /**
     * Show a badge earned notification with animation
     * @param badge The badge that was earned
     * @param parentPane The pane to show the notification in
     */
    public static void show(Badge badge, Pane parentPane) {
        System.out.println("🎨 Creating badge notification:");
        System.out.println("   Name: '" + badge.getName() + "'");
        System.out.println("   Description: '" + badge.getDescription() + "'");
        System.out.println("   Points: " + badge.getPointsRequired());
        
        // Get or create overlay at Scene level
        StackPane overlay = getOrCreateOverlay(parentPane);
        
        if (overlay != null) {
            // Show both cards side by side
            showBothCards(badge, overlay);
        } else {
            System.err.println("❌ Could not create overlay for notification");
        }
    }
    
    /**
     * Show both notification cards side by side
     */
    private static void showBothCards(Badge badge, StackPane overlay) {
        // Create container for both cards
        HBox cardContainer = new HBox(20);
        cardContainer.setAlignment(Pos.CENTER);
        cardContainer.setPickOnBounds(false);
        
        // Create trophy card
        VBox trophyCard = createTrophyCard();
        
        // Create details card
        VBox detailsCard = createDetailsCard(badge);
        
        cardContainer.getChildren().addAll(trophyCard, detailsCard);
        
        // Initially invisible
        cardContainer.setOpacity(0);
        cardContainer.setScaleX(0.8);
        cardContainer.setScaleY(0.8);
        
        // Add to overlay
        overlay.getChildren().add(cardContainer);
        StackPane.setAlignment(cardContainer, Pos.TOP_CENTER);
        
        System.out.println("🎯 Both cards added to overlay side by side");
        
        // Animations
        FadeTransition fadeIn = new FadeTransition(Duration.millis(400), cardContainer);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        
        ScaleTransition scaleUp = new ScaleTransition(Duration.millis(400), cardContainer);
        scaleUp.setFromX(0.8);
        scaleUp.setFromY(0.8);
        scaleUp.setToX(1.0);
        scaleUp.setToY(1.0);
        
        // Trophy rotation
        Label trophyIcon = (Label) trophyCard.getChildren().get(0);
        RotateTransition rotate = new RotateTransition(Duration.millis(800), trophyIcon);
        rotate.setByAngle(360);
        rotate.setCycleCount(2);
        
        ParallelTransition entrance = new ParallelTransition(fadeIn, scaleUp);
        entrance.setOnFinished(e -> rotate.play());
        
        PauseTransition pause = new PauseTransition(Duration.seconds(4));
        
        FadeTransition fadeOut = new FadeTransition(Duration.millis(400), cardContainer);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> overlay.getChildren().remove(cardContainer));
        
        SequentialTransition sequence = new SequentialTransition(entrance, pause, fadeOut);
        sequence.play();
        
        // Click to dismiss
        cardContainer.setOnMouseClicked(e -> {
            sequence.stop();
            fadeOut.play();
        });
    }
    
    /**
     * Create trophy card
     */
    private static VBox createTrophyCard() {
        VBox trophyCard = new VBox(20);
        trophyCard.setAlignment(Pos.CENTER);
        trophyCard.setPadding(new Insets(40));
        trophyCard.setMinWidth(350);
        trophyCard.setMaxWidth(350);
        trophyCard.setMinHeight(400);
        trophyCard.setMaxHeight(400);
        trophyCard.setStyle(
            "-fx-background-color: linear-gradient(to bottom right, #FFD700, #FFA500);" +
            "-fx-background-radius: 20px;" +
            "-fx-border-color: #FFD700;" +
            "-fx-border-width: 3px;" +
            "-fx-border-radius: 20px;"
        );
        
        // Add drop shadow
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.5));
        shadow.setRadius(20);
        shadow.setOffsetY(10);
        trophyCard.setEffect(shadow);
        
        // Trophy icon
        Label trophyIcon = new Label("🏆");
        trophyIcon.setStyle("-fx-font-size: 120px;");
        
        // "Badge Earned!" text
        Label titleLabel = new Label("BADGE EARNED!");
        titleLabel.setStyle(
            "-fx-font-size: 28px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #FFFFFF;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 2, 0, 0, 2);"
        );
        
        trophyCard.getChildren().addAll(trophyIcon, titleLabel);
        trophyCard.setPickOnBounds(true);
        trophyCard.setMouseTransparent(false);
        
        return trophyCard;
    }
    
    /**
     * Create details card
     */
    private static VBox createDetailsCard(Badge badge) {
        VBox detailsCard = new VBox(20);
        detailsCard.setAlignment(Pos.CENTER);
        detailsCard.setPadding(new Insets(40));
        detailsCard.setMinWidth(350);
        detailsCard.setMaxWidth(350);
        detailsCard.setMinHeight(400);
        detailsCard.setMaxHeight(400);
        detailsCard.setStyle(
            "-fx-background-color: linear-gradient(to bottom right, #456990, #2C4A5E);" +
            "-fx-background-radius: 20px;" +
            "-fx-border-color: #456990;" +
            "-fx-border-width: 3px;" +
            "-fx-border-radius: 20px;"
        );
        
        // Add drop shadow
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.5));
        shadow.setRadius(20);
        shadow.setOffsetY(10);
        detailsCard.setEffect(shadow);
        
        // Badge emoji/icon
        Label badgeIcon = new Label("🎖️");
        badgeIcon.setStyle("-fx-font-size: 60px;");
        
        // Badge name
        Label nameLabel = new Label(badge.getName());
        nameLabel.setStyle(
            "-fx-font-size: 32px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #FFD700;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 3, 0, 0, 2);"
        );
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(300);
        nameLabel.setAlignment(Pos.CENTER);
        
        // Badge description
        Label descLabel = new Label(badge.getDescription());
        descLabel.setStyle(
            "-fx-font-size: 16px;" +
            "-fx-text-fill: #F7F0F5;" +
            "-fx-font-weight: 500;"
        );
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(300);
        descLabel.setAlignment(Pos.CENTER);
        
        // Separator
        Separator separator = new Separator();
        separator.setMaxWidth(250);
        separator.setStyle("-fx-background-color: rgba(255,255,255,0.3);");
        
        // Points required
        Label pointsLabel = new Label("⭐ " + badge.getPointsRequired() + " Points");
        pointsLabel.setStyle(
            "-fx-font-size: 18px;" +
            "-fx-font-weight: 600;" +
            "-fx-text-fill: #FFD700;" +
            "-fx-background-color: rgba(0,0,0,0.3);" +
            "-fx-background-radius: 15px;" +
            "-fx-padding: 10 20;"
        );
        
        detailsCard.getChildren().addAll(badgeIcon, nameLabel, descLabel, separator, pointsLabel);
        detailsCard.setPickOnBounds(true);
        detailsCard.setMouseTransparent(false);
        
        return detailsCard;
    }
    
    /**
     * Get or create an overlay StackPane at the Scene root level
     */
    private static StackPane getOrCreateOverlay(Pane parentPane) {
        try {
            javafx.scene.Scene scene = parentPane.getScene();
            if (scene == null) {
                System.err.println("❌ No scene found");
                return null;
            }
            
            javafx.scene.Parent root = scene.getRoot();
            System.out.println("🔍 Scene root type: " + root.getClass().getName());
            
            // Check if root is already a StackPane with our overlay
            if (root instanceof StackPane) {
                StackPane stackPane = (StackPane) root;
                // Check if overlay already exists
                for (javafx.scene.Node child : stackPane.getChildren()) {
                    if (child.getId() != null && child.getId().equals("badge-notification-overlay")) {
                        System.out.println("✅ Found existing overlay");
                        return (StackPane) child;
                    }
                }
            }
            
            // Create new overlay StackPane
            StackPane overlay = new StackPane();
            overlay.setId("badge-notification-overlay");
            overlay.setPickOnBounds(false); // Allow clicks to pass through
            overlay.setMouseTransparent(false); // But allow interaction with notification cards
            overlay.setAlignment(Pos.TOP_CENTER);
            overlay.setPadding(new Insets(20, 0, 0, 0));
            
            // Wrap current root in a StackPane if needed
            if (!(root instanceof StackPane)) {
                StackPane newRoot = new StackPane();
                scene.setRoot(newRoot);
                newRoot.getChildren().add(root);
                newRoot.getChildren().add(overlay);
                System.out.println("✅ Created new StackPane root with overlay");
            } else {
                ((StackPane) root).getChildren().add(overlay);
                System.out.println("✅ Added overlay to existing StackPane");
            }
            
            return overlay;
        } catch (Exception e) {
            System.err.println("❌ Error creating overlay: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Show multiple badge notifications in sequence
     */
    public static void showMultiple(java.util.List<Badge> badges, Pane parentPane) {
        if (badges.isEmpty()) {
            return;
        }
        
        // Show first badge
        show(badges.get(0), parentPane);
        
        // Show remaining badges with delay
        for (int i = 1; i < badges.size(); i++) {
            final int index = i;
            PauseTransition delay = new PauseTransition(Duration.seconds(5 * i));
            delay.setOnFinished(e -> show(badges.get(index), parentPane));
            delay.play();
        }
    }
}
