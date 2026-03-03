package edu.connexion3a8.tools;

import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses @username mentions in text and creates styled TextFlow nodes.
 * Designed for easy integration with a user management system later.
 */
public class MentionParser {

    private static final Pattern MENTION_PATTERN = Pattern.compile("@(\\w+(?:\\s\\w+)?)");

    /**
     * Extract all @mentions from a text string.
     * @return list of mentioned usernames (without the @ prefix)
     */
    public static List<String> extractMentions(String text) {
        List<String> mentions = new ArrayList<>();
        if (text == null) return mentions;
        Matcher matcher = MENTION_PATTERN.matcher(text);
        while (matcher.find()) {
            mentions.add(matcher.group(1));
        }
        return mentions;
    }

    /**
     * Creates a TextFlow with @mentions highlighted in a distinct color.
     * @param text the raw text containing @mentions
     * @param textColor CSS color for normal text
     * @param fontSize CSS font size (e.g. "15px")
     * @return a TextFlow with styled segments
     */
    public static TextFlow createStyledText(String text, String textColor, String fontSize) {
        TextFlow flow = new TextFlow();
        if (text == null || text.isEmpty()) return flow;

        Matcher matcher = MENTION_PATTERN.matcher(text);
        int lastEnd = 0;

        while (matcher.find()) {
            // Add normal text before the mention
            if (matcher.start() > lastEnd) {
                Text normalText = new Text(text.substring(lastEnd, matcher.start()));
                normalText.setStyle("-fx-fill: " + textColor + "; -fx-font-size: " + fontSize + ";");
                flow.getChildren().add(normalText);
            }

            // Add the mention with highlight
            Text mentionText = new Text(matcher.group());
            mentionText.setStyle("-fx-fill: #456990; -fx-font-size: " + fontSize + "; -fx-font-weight: bold; -fx-cursor: hand;");
            flow.getChildren().add(mentionText);

            lastEnd = matcher.end();
        }

        // Add remaining text after last mention
        if (lastEnd < text.length()) {
            Text remainingText = new Text(text.substring(lastEnd));
            remainingText.setStyle("-fx-fill: " + textColor + "; -fx-font-size: " + fontSize + ";");
            flow.getChildren().add(remainingText);
        }

        return flow;
    }

    /**
     * Check if a text contains any @mentions.
     */
    public static boolean hasMentions(String text) {
        if (text == null) return false;
        return MENTION_PATTERN.matcher(text).find();
    }
}
