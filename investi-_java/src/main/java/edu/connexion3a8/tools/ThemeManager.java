package edu.connexion3a8.tools;

import javafx.scene.Scene;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages dark/light theme switching across the application.
 * Holds the current theme state and provides color values for inline styles.
 */
public class ThemeManager {

    public enum Theme { DARK, LIGHT }

    private static Theme currentTheme = Theme.DARK;
    private static final List<Scene> managedScenes = new ArrayList<>();

    // --- Dark theme colors ---
    private static final String DARK_BG = "#0f1114";
    private static final String DARK_CARD = "#1a1d23";
    private static final String DARK_BORDER = "#2a2d32";
    private static final String DARK_TEXT = "#e7e9ea";
    private static final String DARK_TEXT_SEC = "#71767b";
    private static final String DARK_TEXT_MUTED = "#8899a6";
    private static final String DARK_HEADER_BG = "rgba(15,17,20,0.85)";
    private static final String DARK_OVERLAY = "rgba(0,0,0,0.9)";
    private static final String DARK_SUMMARY_BG = "#1a1a2e";

    // --- Light theme colors ---
    private static final String LIGHT_BG = "#f7f9fb";
    private static final String LIGHT_CARD = "#ffffff";
    private static final String LIGHT_BORDER = "#d0d7de";
    private static final String LIGHT_TEXT = "#2c3e50";
    private static final String LIGHT_TEXT_SEC = "#5a6a7a";
    private static final String LIGHT_TEXT_MUTED = "#8899a6";
    private static final String LIGHT_HEADER_BG = "rgba(255,255,255,0.92)";
    private static final String LIGHT_OVERLAY = "rgba(255,255,255,0.95)";
    private static final String LIGHT_SUMMARY_BG = "#f0f2f5";

    private static final String LIGHT_CSS = "styles-light.css";

    public static Theme getCurrentTheme() { return currentTheme; }
    public static boolean isDark() { return currentTheme == Theme.DARK; }

    public static void toggle() {
        currentTheme = (currentTheme == Theme.DARK) ? Theme.LIGHT : Theme.DARK;
        applyToAllScenes();
    }

    /** Register a scene so theme changes apply to it. */
    public static void registerScene(Scene scene) {
        if (!managedScenes.contains(scene)) {
            managedScenes.add(scene);
        }
        applyToScene(scene);
    }

    /** Unregister a scene (call on dialog close). */
    public static void unregisterScene(Scene scene) {
        managedScenes.remove(scene);
    }

    private static void applyToAllScenes() {
        for (Scene scene : new ArrayList<>(managedScenes)) {
            applyToScene(scene);
        }
    }

    private static void applyToScene(Scene scene) {
        String lightCssUrl = ThemeManager.class.getResource("/" + LIGHT_CSS) != null
                ? ThemeManager.class.getResource("/" + LIGHT_CSS).toExternalForm()
                : null;
        if (lightCssUrl == null) return;

        if (currentTheme == Theme.LIGHT) {
            if (!scene.getStylesheets().contains(lightCssUrl)) {
                scene.getStylesheets().add(lightCssUrl);
            }
        } else {
            scene.getStylesheets().remove(lightCssUrl);
        }
    }

    // --- Color getters for inline styles ---
    public static String bg()        { return isDark() ? DARK_BG : LIGHT_BG; }
    public static String card()      { return isDark() ? DARK_CARD : LIGHT_CARD; }
    public static String border()    { return isDark() ? DARK_BORDER : LIGHT_BORDER; }
    public static String text()      { return isDark() ? DARK_TEXT : LIGHT_TEXT; }
    public static String textSec()   { return isDark() ? DARK_TEXT_SEC : LIGHT_TEXT_SEC; }
    public static String textMuted() { return isDark() ? DARK_TEXT_MUTED : LIGHT_TEXT_MUTED; }
    public static String headerBg()  { return isDark() ? DARK_HEADER_BG : LIGHT_HEADER_BG; }
    public static String overlay()   { return isDark() ? DARK_OVERLAY : LIGHT_OVERLAY; }
    public static String summaryBg() { return isDark() ? DARK_SUMMARY_BG : LIGHT_SUMMARY_BG; }
    public static String userRowBg() { return isDark() ? DARK_CARD : "#f0f2f5"; }
    public static String inputBg()   { return isDark() ? DARK_CARD : "#f0f2f5"; }
}
