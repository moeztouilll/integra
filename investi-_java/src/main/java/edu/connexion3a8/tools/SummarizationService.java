package edu.connexion3a8.tools;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI-Powered Text Summarization
 * Priority: 1) Groq API (free, fast) → 2) AWS Bedrock → 3) Local TextRank
 *
 * Configuration in .env:
 *   GROQ_API_KEY=gsk_...        (primary - get from console.groq.com)
 *   BEDROCK_API_KEY=ABSK...     (backup - get from AWS Bedrock console)
 */
public class SummarizationService {

    private static final int MIN_TEXT_LENGTH = 100;
    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
        "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with",
        "by", "from", "as", "is", "was", "are", "were", "been", "be", "have", "has", "had",
        "do", "does", "did", "will", "would", "could", "should", "may", "might", "must",
        "it", "its", "this", "that", "these", "those", "i", "you", "he", "she", "we", "they",
        "what", "which", "who", "all", "each", "every", "both", "few", "more", "most", "other",
        "some", "such", "no", "nor", "not", "only", "own", "same", "so", "than", "too", "very"
    ));

    public static String summarize(String text) {
        if (text == null || text.trim().length() < MIN_TEXT_LENGTH) return null;

        // 1) Try Groq (free, fast, reliable)
        if (EnvConfig.isSet("GROQ_API_KEY")) {
            try {
                System.out.println("[Summarization] Trying Groq API...");
                String result = callGroqAPI(text);
                if (result != null && !result.isEmpty()) {
                    System.out.println("[Summarization] ✓ Summary via Groq");
                    return result;
                }
            } catch (Exception e) {
                System.err.println("[Summarization] Groq error: " + e.getMessage());
            }
        }

        // 2) Try AWS Bedrock
        if (EnvConfig.isSet("BEDROCK_API_KEY")) {
            try {
                System.out.println("[Summarization] Trying AWS Bedrock...");
                String result = callBedrockAPI(text);
                if (result != null && !result.isEmpty()) {
                    System.out.println("[Summarization] ✓ Summary via Bedrock");
                    return result;
                }
            } catch (Exception e) {
                System.err.println("[Summarization] Bedrock error: " + e.getMessage());
            }
        }

        // 3) Local fallback
        System.out.println("[Summarization] Using local TextRank");
        return textRankSummarize(text);
    }

    public static boolean shouldSummarize(String text) {
        return text != null && text.trim().length() >= MIN_TEXT_LENGTH;
    }

    public static boolean isConfigured() {
        return true;
    }

    // ========== GROQ API ==========

    private static String callGroqAPI(String text) throws Exception {
        String truncated = text.length() > 6000 ? text.substring(0, 6000) : text;
        String apiKey = EnvConfig.get("GROQ_API_KEY");

        String body = "{\"model\":\"llama-3.1-8b-instant\","
                + "\"messages\":[{\"role\":\"user\",\"content\":\""
                + escapeJson("Summarize this in 2-3 short sentences:\n\n" + truncated)
                + "\"}],\"max_tokens\":200,\"temperature\":0.3}";

        URL url = new URL(GROQ_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(15000);
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        System.out.println("[Summarization] Groq response: " + code);

        if (code == 200) {
            String resp = readStream(conn);
            // Extract content from: "content":"..."
            String marker = "\"content\":\"";
            int start = resp.lastIndexOf(marker);
            if (start == -1) return null;
            start += marker.length();
            StringBuilder result = new StringBuilder();
            for (int i = start; i < resp.length(); i++) {
                char c = resp.charAt(i);
                if (c == '\\' && i + 1 < resp.length()) {
                    char next = resp.charAt(i + 1);
                    if (next == '"') { result.append('"'); i++; }
                    else if (next == 'n') { result.append(' '); i++; }
                    else if (next == '\\') { result.append('\\'); i++; }
                    else { result.append(c); }
                } else if (c == '"') {
                    break;
                } else {
                    result.append(c);
                }
            }
            return result.toString().trim();
        } else {
            System.err.println("[Summarization] Groq error " + code + ": " + readErrorStream(conn));
            return null;
        }
    }

    // ========== AWS BEDROCK API ==========

    private static String callBedrockAPI(String text) throws Exception {
        String truncated = text.length() > 6000 ? text.substring(0, 6000) : text;
        String region = EnvConfig.get("AWS_REGION", "eu-west-3");
        String apiKey = EnvConfig.get("BEDROCK_API_KEY");

        String body = "{\"messages\":[{\"role\":\"user\",\"content\":[{\"text\":\""
                + escapeJson("Summarize this in 2-3 short sentences:\n\n" + truncated)
                + "\"}]}],\"inferenceConfig\":{\"maxTokens\":200,\"temperature\":0.3,\"topP\":0.9}}";

        String[] models = {"eu.amazon.nova-micro-v1:0", "eu.amazon.nova-lite-v1:0", "mistral.mistral-7b-instruct-v0:2"};

        for (String model : models) {
            String endpoint = "https://bedrock-runtime." + region + ".amazonaws.com/model/" + model + "/converse";
            System.out.println("[Summarization] Bedrock trying: " + model);

            URL url = new URL(endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(20000);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            if (code == 200) {
                String resp = readStream(conn);
                String marker = "\"text\":\"";
                int outIdx = resp.indexOf("\"output\"");
                if (outIdx == -1) continue;
                int start = resp.indexOf(marker, outIdx);
                if (start == -1) continue;
                start += marker.length();
                int end = resp.indexOf("\"", start);
                if (end == -1) continue;
                return resp.substring(start, end).replace("\\n", " ").trim();
            } else {
                System.err.println("[Summarization] Bedrock " + model + " → " + code);
                continue;
            }
        }
        return null;
    }

    // ========== HELPERS ==========

    private static String readStream(HttpURLConnection conn) throws Exception {
        BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) sb.append(line);
        r.close();
        return sb.toString();
    }

    private static String readErrorStream(HttpURLConnection conn) {
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(
                    conn.getErrorStream() != null ? conn.getErrorStream() : conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
            r.close();
            return sb.toString();
        } catch (Exception e) { return e.getMessage(); }
    }

    // ========== LOCAL TEXTRANK FALLBACK ==========

    private static String textRankSummarize(String text) {
        List<String> sentences = Arrays.stream(text.split("(?<=[.!?])\\s+"))
                .filter(s -> s.trim().length() > 10).collect(Collectors.toList());
        if (sentences.size() <= 3)
            return text.length() > 300 ? text.substring(0, 297) + "..." : text;

        Map<String, Integer> wordFreq = new HashMap<>();
        for (String word : text.toLowerCase().split("\\W+"))
            if (word.length() > 2 && !STOP_WORDS.contains(word))
                wordFreq.merge(word, 1, Integer::sum);

        Map<Integer, Double> scores = new HashMap<>();
        for (int i = 0; i < sentences.size(); i++) {
            double score = 0; int count = 0;
            for (String word : sentences.get(i).toLowerCase().split("\\W+"))
                if (word.length() > 2 && !STOP_WORDS.contains(word)) {
                    score += wordFreq.getOrDefault(word, 0); count++;
                }
            scores.put(i, count > 0 ? (score / Math.sqrt(count)) * (i == 0 ? 1.5 : 1) : 0);
        }

        List<Integer> topIdx = scores.entrySet().stream()
                .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
                .limit(3).map(Map.Entry::getKey).sorted().collect(Collectors.toList());
        String result = topIdx.stream().map(sentences::get).collect(Collectors.joining(" "));
        return result.length() > 350 ? result.substring(0, 347) + "..." : result;
    }

    private static String escapeJson(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        return sb.toString();
    }
}
