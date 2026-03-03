package edu.connexion3a8.services.gamification;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Service for translating text using MyMemory Translation API
 * MyMemory is completely FREE with no API key required
 * Free tier: 10,000 words per day
 * API: https://mymemory.translated.net/doc/spec.php
 */
public class LibreTranslateService {
    
    // MyMemory Translation API endpoint (completely free, no API key needed)
    private static final String API_URL = "https://api.mymemory.translated.net/get";
    
    private final OkHttpClient client;
    private final Gson gson;
    
    public LibreTranslateService() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
    }
    
    /**
     * Translates text from source language to target language
     * 
     * @param text Text to translate
     * @param sourceLang Source language code (e.g., "en", "fr", "ar") or "auto" for auto-detect
     * @param targetLang Target language code (e.g., "en", "fr", "ar")
     * @return Translated text
     * @throws IOException if translation fails
     */
    public String translate(String text, String sourceLang, String targetLang) throws IOException {
        if (text == null || text.trim().isEmpty()) {
            throw new IOException("Text to translate cannot be empty");
        }
        
        // Clean the text
        String cleanedText = text.trim();
        
        // MyMemory doesn't support "auto" - we need to detect or use a default
        // For simplicity, we'll try common languages if auto is specified
        if (sourceLang.equals("auto")) {
            // Try to detect language by attempting translation from common sources
            // Most PDFs are in English, French, or Arabic in your context
            sourceLang = detectLanguageSimple(cleanedText, targetLang);
        }
        
        // Check if source and target are the same
        if (sourceLang.equals(targetLang)) {
            return cleanedText;
        }
        
        // MyMemory has a limit of ~500 chars per request for best results
        // For longer texts, split and translate in chunks
        if (cleanedText.length() > 450) {
            return translateLongText(cleanedText, sourceLang, targetLang);
        }
        
        // Build URL with query parameters
        String langPair = sourceLang + "|" + targetLang;
        String encodedText = URLEncoder.encode(cleanedText, StandardCharsets.UTF_8);
        String url = API_URL + "?q=" + encodedText + "&langpair=" + langPair;
        
        System.out.println("Translation request: " + langPair + ", textLength=" + cleanedText.length());
        
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        
        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();
            
            if (!response.isSuccessful()) {
                System.err.println("Translation API error " + response.code() + ": " + responseBody);
                throw new IOException("Translation failed with code " + response.code());
            }
            
            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
            
            // Check response status
            if (jsonResponse.has("responseStatus") && jsonResponse.get("responseStatus").getAsInt() != 200) {
                String errorMsg = "Translation failed";
                if (jsonResponse.has("responseDetails")) {
                    errorMsg = jsonResponse.get("responseDetails").getAsString();
                }
                throw new IOException(errorMsg);
            }
            
            // Extract translated text
            if (jsonResponse.has("responseData")) {
                JsonObject responseData = jsonResponse.getAsJsonObject("responseData");
                if (responseData.has("translatedText")) {
                    return responseData.get("translatedText").getAsString();
                }
            }
            
            throw new IOException("Unexpected response format: " + responseBody);
        }
    }
    
    /**
     * Simple language detection by trying common source languages
     * Returns the most likely source language
     */
    private String detectLanguageSimple(String text, String targetLang) {
        // Sample first 100 characters for detection
        String sample = text.length() > 100 ? text.substring(0, 100) : text;
        
        // Check for common language patterns
        // Arabic: contains Arabic characters
        if (sample.matches(".*[\\u0600-\\u06FF].*")) {
            return "ar";
        }
        
        // Chinese: contains Chinese characters
        if (sample.matches(".*[\\u4E00-\\u9FFF].*")) {
            return "zh";
        }
        
        // Japanese: contains Japanese characters
        if (sample.matches(".*[\\u3040-\\u309F\\u30A0-\\u30FF].*")) {
            return "ja";
        }
        
        // Korean: contains Korean characters
        if (sample.matches(".*[\\uAC00-\\uD7AF].*")) {
            return "ko";
        }
        
        // Russian/Cyrillic: contains Cyrillic characters
        if (sample.matches(".*[\\u0400-\\u04FF].*")) {
            return "ru";
        }
        
        // Greek: contains Greek characters
        if (sample.matches(".*[\\u0370-\\u03FF].*")) {
            return "el";
        }
        
        // Hebrew: contains Hebrew characters
        if (sample.matches(".*[\\u0590-\\u05FF].*")) {
            return "he";
        }
        
        // For Latin-based languages, check common words
        String lowerSample = sample.toLowerCase();
        
        // French indicators
        if (lowerSample.matches(".*(le|la|les|de|du|des|un|une|est|sont|avec|pour|dans).*")) {
            return "fr";
        }
        
        // Spanish indicators
        if (lowerSample.matches(".*(el|la|los|las|de|del|un|una|es|son|con|para|en).*")) {
            return "es";
        }
        
        // German indicators
        if (lowerSample.matches(".*(der|die|das|den|dem|des|ein|eine|ist|sind|mit|für|in).*")) {
            return "de";
        }
        
        // Italian indicators
        if (lowerSample.matches(".*(il|lo|la|i|gli|le|di|del|un|una|è|sono|con|per|in).*")) {
            return "it";
        }
        
        // Portuguese indicators
        if (lowerSample.matches(".*(o|a|os|as|de|do|da|um|uma|é|são|com|para|em).*")) {
            return "pt";
        }
        
        // Default to English if can't detect
        return "en";
    }
    
    /**
     * Translates long text by splitting into chunks
     */
    private String translateLongText(String text, String sourceLang, String targetLang) throws IOException {
        StringBuilder result = new StringBuilder();
        
        // Split by paragraphs (double newline)
        String[] paragraphs = text.split("\n\n");
        
        for (int i = 0; i < paragraphs.length; i++) {
            String paragraph = paragraphs[i];
            
            if (paragraph.trim().isEmpty()) {
                result.append("\n\n");
                continue;
            }
            
            // If paragraph is still too long, split by sentences
            if (paragraph.length() > 450) {
                String[] sentences = paragraph.split("(?<=[.!?])\\s+");
                StringBuilder chunk = new StringBuilder();
                
                for (String sentence : sentences) {
                    if (chunk.length() + sentence.length() > 400) {
                        // Translate current chunk
                        if (chunk.length() > 0) {
                            String translated = translate(chunk.toString(), sourceLang, targetLang);
                            result.append(translated).append(" ");
                            chunk = new StringBuilder();
                        }
                    }
                    chunk.append(sentence).append(" ");
                }
                
                // Translate remaining chunk
                if (chunk.length() > 0) {
                    String translated = translate(chunk.toString(), sourceLang, targetLang);
                    result.append(translated);
                }
            } else {
                // Translate paragraph
                String translated = translate(paragraph, sourceLang, targetLang);
                result.append(translated);
            }
            
            if (i < paragraphs.length - 1) {
                result.append("\n\n");
            }
            
            // Small delay to avoid rate limiting
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        return result.toString();
    }
    
    /**
     * Detects the language of the given text
     * Note: MyMemory doesn't have a dedicated detect endpoint, so we use auto detection in translation
     */
    public String detectLanguage(String text) throws IOException {
        // MyMemory handles auto-detection automatically when using "auto" as source
        return "auto";
    }
    
    /**
     * Language information class
     */
    public static class Language {
        public final String code;
        public final String name;
        
        public Language(String code, String name) {
            this.code = code;
            this.name = name;
        }
        
        @Override
        public String toString() {
            return name;
        }
    }
    
    /**
     * Get list of supported languages
     * MyMemory supports many language pairs
     */
    public static Language[] getSupportedLanguages() {
        return new Language[] {
            new Language("en", "English"),
            new Language("fr", "French"),
            new Language("es", "Spanish"),
            new Language("de", "German"),
            new Language("it", "Italian"),
            new Language("pt", "Portuguese"),
            new Language("ru", "Russian"),
            new Language("ar", "Arabic"),
            new Language("zh", "Chinese (Simplified)"),
            new Language("ja", "Japanese"),
            new Language("ko", "Korean"),
            new Language("hi", "Hindi"),
            new Language("tr", "Turkish"),
            new Language("nl", "Dutch"),
            new Language("pl", "Polish"),
            new Language("uk", "Ukrainian"),
            new Language("sv", "Swedish"),
            new Language("cs", "Czech"),
            new Language("da", "Danish"),
            new Language("fi", "Finnish"),
            new Language("el", "Greek"),
            new Language("hu", "Hungarian"),
            new Language("id", "Indonesian"),
            new Language("no", "Norwegian"),
            new Language("ro", "Romanian"),
            new Language("sk", "Slovak"),
            new Language("th", "Thai"),
            new Language("vi", "Vietnamese"),
            new Language("he", "Hebrew"),
            new Language("bn", "Bengali"),
            new Language("fa", "Persian"),
            new Language("ur", "Urdu")
        };
    }
}
