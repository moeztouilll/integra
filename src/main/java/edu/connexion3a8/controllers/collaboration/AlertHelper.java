package edu.connexion3a8.controllers.collaboration;

import javafx.scene.control.*;
import javafx.stage.StageStyle;

import java.util.Optional;

/**
 * Centralised premium-styled dialog helper.
 * Every dialog uses UNDECORATED + styles_premium.css + styled-dialog class.
 */
public class AlertHelper {

    private static final String CSS = AlertHelper.class.getResource("/collaboration/styles_premium.css").toExternalForm();

    // ─── Core show methods ────────────────────────────────────────────────────

    public static void showInfo(String title, String message) {
        show(Alert.AlertType.INFORMATION, title, message);
    }

    public static void showWarning(String title, String message) {
        show(Alert.AlertType.WARNING, title, message);
    }

    public static void showError(String title, String message) {
        show(Alert.AlertType.ERROR, title, message);
    }

    /** Returns true if user pressed OK/YES. */
    public static boolean confirm(String title, String message) {
        Alert a = buildAlert(Alert.AlertType.CONFIRMATION, title, message);
        return a.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    /**
     * Shows a text-input dialog. Returns the entered text or empty if cancelled.
     */
    public static Optional<String> input(String title, String prompt, String defaultValue) {
        TextInputDialog dlg = new TextInputDialog(defaultValue == null ? "" : defaultValue);
        dlg.initStyle(StageStyle.UNDECORATED);
        dlg.setTitle(title);
        dlg.setHeaderText(prompt);
        dlg.setContentText(null);
        dlg.getDialogPane().getStylesheets().add(CSS);
        dlg.getDialogPane().getStyleClass().add("styled-dialog");
        dlg.getDialogPane().setPrefWidth(400);
        return dlg.showAndWait();
    }

    // ─── Internal helpers ─────────────────────────────────────────────────────

    private static void show(Alert.AlertType type, String title, String message) {
        buildAlert(type, title, message).showAndWait();
    }

    static Alert buildAlert(Alert.AlertType type, String title, String message) {
        Alert a = new Alert(type);
        a.initStyle(StageStyle.UNDECORATED);
        a.setTitle(title);
        a.setHeaderText(title);
        a.setContentText(message);
        a.getDialogPane().getStylesheets().add(CSS);
        a.getDialogPane().getStyleClass().add("styled-dialog");
        a.getDialogPane().setPrefWidth(460);
        return a;
    }
}
