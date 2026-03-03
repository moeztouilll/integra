package edu.connexion3a8.tools;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Translation Service using MyMemory API (free, no API key required)
 * Supports: English, French, Arabic
 */
public class TranslationService {

    private static final String API_URL = "https://api.mymemory.translated.net/get";

    /**
     * Supported languages
     */
    public enum Language {
        ENGLISH("en", "English"),
        FRENCH("fr", "Français"),
        ARABIC("ar", "العربية");

        private final String code;
        private final String displayName;

        Language(String code, String displayName) {
            this.code = code;
            this.displayName = displayName;
        }

        public String getCode() {
            return code;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Translate text to target language
     * @param text Text to translate
     * @param targetLang Target language
     * @return Translated text or original if translation fails
     */
    public static String translate(String text, Language targetLang) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }

        // Detect source language (simple heuristic)
        Language sourceLang = detectLanguage(text);
        
        // Don't translate if already in target language
        if (sourceLang == targetLang) {
            return text;
        }

        return translateWithAPI(text, sourceLang.getCode(), targetLang.getCode());
    }

    /**
     * Translate text from source to target language
     */
    public static String translate(String text, Language sourceLang, Language targetLang) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }
        
        if (sourceLang == targetLang) {
            return text;
        }

        return translateWithAPI(text, sourceLang.getCode(), targetLang.getCode());
    }

    /**
     * Simple language detection based on character patterns
     */
    public static Language detectLanguage(String text) {
        if (text == null || text.isEmpty()) {
            return Language.ENGLISH;
        }

        // Check for Arabic characters
        if (text.matches(".*[\\u0600-\\u06FF]+.*")) {
            return Language.ARABIC;
        }

        // Check for French-specific characters and common words
        String lower = text.toLowerCase();
        if (lower.matches(".*[éèêëàâäùûüôöîïç]+.*") ||
            lower.contains(" le ") || lower.contains(" la ") || 
            lower.contains(" les ") || lower.contains(" des ") ||
            lower.contains(" est ") || lower.contains(" sont ") ||
            lower.contains(" pour ") || lower.contains(" avec ")) {
            return Language.FRENCH;
        }

        // Default to English
        return Language.ENGLISH;
    }

    /**
     * Call MyMemory API for translation
     */
    private static String translateWithAPI(String text, String sourceLang, String targetLang) {
        try {
            // Build URL with parameters
            String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8.toString());
            String langPair = sourceLang + "|" + targetLang;
            String urlStr = API_URL + "?q=" + encodedText + "&langpair=" + URLEncoder.encode(langPair, StandardCharsets.UTF_8.toString());

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                // Parse JSON response (simple parsing without external library)
                String jsonResponse = response.toString();
                String translatedText = extractTranslatedText(jsonResponse);
                
                if (translatedText != null && !translatedText.isEmpty()) {
                    return translatedText;
                }
            }
        } catch (Exception e) {
            System.err.println("Translation error: " + e.getMessage());
        }

        // Return original text if translation fails
        return text;
    }

    /**
     * Extract translated text from JSON response
     * Response format: {"responseData":{"translatedText":"..."},...}
     */
    private static String extractTranslatedText(String json) {
        try {
            // Find "translatedText":"..."
            String marker = "\"translatedText\":\"";
            int start = json.indexOf(marker);
            if (start == -1) return null;
            
            start += marker.length();
            int end = json.indexOf("\"", start);
            if (end == -1) return null;

            String translated = json.substring(start, end);
            
            // Decode unicode escapes
            translated = decodeUnicode(translated);
            
            return translated;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Decode unicode escape sequences like \u00e9 to actual characters
     */
    private static String decodeUnicode(String text) {
        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            if (i + 5 < text.length() && text.charAt(i) == '\\' && text.charAt(i + 1) == 'u') {
                try {
                    int code = Integer.parseInt(text.substring(i + 2, i + 6), 16);
                    result.append((char) code);
                    i += 6;
                } catch (NumberFormatException e) {
                    result.append(text.charAt(i));
                    i++;
                }
            } else {
                result.append(text.charAt(i));
                i++;
            }
        }
        return result.toString();
    }

    /**
     * Check if translation service is available (has internet)
     */
    public static boolean isAvailable() {
        try {
            URL url = new URL("https://api.mymemory.translated.net");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            conn.setRequestMethod("HEAD");
            return conn.getResponseCode() == HttpURLConnection.HTTP_OK;
        } catch (Exception e) {
            return false;
        }
    }
}
