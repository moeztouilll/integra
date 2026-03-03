package edu.connexion3a8.tools;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Bad Words Filter - Detects and blocks inappropriate language
 * Add your bad words to the BLOCKED_WORDS array
 */
public class BadWordsFilter {

    // ========== CONFIGURE YOUR BAD WORDS HERE ==========
    private static final String[] BLOCKED_WORDS = {
       "fuck","shit","merde","pute","cunt"    };
    // ===================================================

    private static final Set<String> badWordsSet = new HashSet<>(Arrays.asList(BLOCKED_WORDS));

    /**
     * Check if text contains any bad words
     * @param text The text to check
     * @return true if bad word found, false if clean
     */
    public static boolean containsBadWords(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        String lowerText = text.toLowerCase();
        
        for (String badWord : badWordsSet) {
            // Use word boundary to match whole words only
            // This prevents false positives like "class" matching "ass"
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(badWord) + "\\b", 
                    Pattern.CASE_INSENSITIVE);
            if (pattern.matcher(lowerText).find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the first bad word found in text (for error message)
     * @param text The text to check
     * @return The bad word found, or null if clean
     */
    public static String getFirstBadWord(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        
        String lowerText = text.toLowerCase();
        
        for (String badWord : badWordsSet) {
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(badWord) + "\\b", 
                    Pattern.CASE_INSENSITIVE);
            if (pattern.matcher(lowerText).find()) {
                return badWord;
            }
        }
        return null;
    }

    /**
     * Censor bad words in text (replace with ****)
     * @param text The text to censor
     * @return Text with bad words replaced by asterisks
     */
    public static String censorText(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        String result = text;
        for (String badWord : badWordsSet) {
            String replacement = "*".repeat(badWord.length());
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(badWord) + "\\b", 
                    Pattern.CASE_INSENSITIVE);
            result = pattern.matcher(result).replaceAll(replacement);
        }
        return result;
    }

    /**
     * Check if the filter is configured (has bad words)
     */
    public static boolean isConfigured() {
        return !badWordsSet.isEmpty();
    }

    /**
     * Get count of blocked words configured
     */
    public static int getBlockedWordsCount() {
        return badWordsSet.size();
    }
}
