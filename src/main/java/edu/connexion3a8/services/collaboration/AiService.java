package edu.connexion3a8.services.collaboration;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import edu.connexion3a8.entities.collaboration.Project;

import java.io.IOException;

public class AiService {

    private static final String API_KEY = "AIzaSyDgXsgJ7sIq_hmXvOqiA9eQ6PJx97Yp66g";
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key="
            + API_KEY;
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build();

    /**
     * Helper method to call the Gemini API.
     */
    private static String callGeminiAPI(String promptText) throws IOException {
        JSONObject payload = new JSONObject();
        JSONArray contents = new JSONArray();
        JSONObject contentObj = new JSONObject();
        JSONArray parts = new JSONArray();
        JSONObject partObj = new JSONObject();

        partObj.put("text", promptText);
        parts.put(partObj);
        contentObj.put("parts", parts);
        contents.put(contentObj);
        payload.put("contents", contents);
        payload.put("generationConfig", new JSONObject().put("temperature", 0.7));

        RequestBody body = RequestBody.create(payload.toString(), MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(GEMINI_API_URL)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            String responseBody = response.body().string();
            JSONObject jsonResponse = new JSONObject(responseBody);
            JSONArray candidates = jsonResponse.getJSONArray("candidates");
            if (candidates.length() > 0) {
                JSONObject firstCandidate = candidates.getJSONObject(0);
                JSONObject content = firstCandidate.getJSONObject("content");
                JSONArray partsArray = content.getJSONArray("parts");
                if (partsArray.length() > 0) {
                    return stripMarkdown(partsArray.getJSONObject(0).getString("text"));
                }
            }
        }
        return "AI Response could not be parsed.";
    }

    /**
     * Strips common markdown characters (*, #, `) from the text to ensure raw text
     * output for JavaFX labels/alerts.
     */
    private static String stripMarkdown(String text) {
        if (text == null)
            return "";
        return text.replaceAll("(?m)^#.*$", "") // remove headers
                .replaceAll("\\*\\*", "") // remove bold
                .replaceAll("```[a-z]*", "") // remove code block start
                .replaceAll("```", "") // remove code block end
                .replaceAll("`", "") // remove inline code
                .trim();
    }

    /**
     * Generates a detailed explanation of the project for an investor.
     * Idea 1
     */
    public static String generateProjectExplanation(Project project) {
        String prompt = "Act as an AI investment analyst analyzing a project:\n"
                + "Title: " + project.getTitle() + "\n"
                + "Category: " + project.getCategory() + "\n"
                + "Requested Amount: $" + project.getAmountRequested() + "\n"
                + "Equity Offered: " + project.getEquityOffered() + "%\n"
                + "Description: " + project.getDescription() + "\n\n"
                + "Provide a detailed, objective, and professional explanation of this project. "
                + "Break it down into:\n"
                + "1. Executive Summary\n"
                + "2. Market Potential\n"
                + "3. Risk & Reward Analysis\n"
                + "4. Final Verdict\n"
                + "Do not use markdown like **bold**, use plain text or basic formatting as this goes to an email/PDF.";

        try {
            return callGeminiAPI(prompt);
        } catch (Exception e) {
            e.printStackTrace();
            return "Could not generate AI explanation due to an error: " + e.getMessage();
        }
    }

    /**
     * Generates an evaluation for an investor indicating whether the project is
     * worth investing in.
     * Idea 2/6
     */
    public static String evaluateProjectForInvestor(Project project) {
        String prompt = "Act as an AI investment advisor. Evaluate if this project is a good investment:\n"
                + "Title: " + project.getTitle() + "\n"
                + "Category: " + project.getCategory() + "\n"
                + "Requested Amount: $" + project.getAmountRequested() + "\n"
                + "Equity Offered: " + project.getEquityOffered() + "%\n"
                + "Description: " + project.getDescription() + "\n\n"
                + "Provide a short, punchy analysis (2-3 sentences) on whether an investor should invest in this, and why.";

        try {
            return callGeminiAPI(prompt);
        } catch (Exception e) {
            e.printStackTrace();
            return "AI Analysis unavailable.";
        }
    }

    /**
     * Generates an evaluation for an entrepreneur regarding their own
     * project/offers.
     * Idea 3
     */
    public static String evaluateLogicForEntrepreneur(double amountRequested, double equityOffered,
            String description) {
        String prompt = "Act as a startup mentor. An entrepreneur is planning to pitch a project asking for $"
                + amountRequested + " in exchange for " + equityOffered + "% equity.\n"
                + "Project idea: " + description + "\n\n"
                + "Provide a concise (2-3 sentences) logical evaluation of whether this valuation and offer is realistic and good for them, and why or why not.";

        try {
            return callGeminiAPI(prompt);
        } catch (Exception e) {
            e.printStackTrace();
            return "AI Mentorship unavailable.";
        }
    }

    /**
     * Recommends new investments based on an investor's past portfolio
     * and a list of currently open projects.
     */
    public static String getPortfolioRecommendations(String pastInvestmentsContext, String openProjectsContext) {
        String prompt = "Act as an expert AI Portfolio Advisor for an investor.\n\n"
                + "Here is the context of what the investor has previously invested in:\n"
                + pastInvestmentsContext + "\n\n"
                + "Here are the currently OPEN projects available for investment:\n"
                + openProjectsContext + "\n\n"
                + "Based on their past investment behavior, suggest 1 or 2 open projects they might be interested in. "
                + "Explain briefly why these match their past interests, or suggest diversifying if their past portfolio is too narrow. "
                + "Keep your entire response to exactly 1 short paragraph (3-4 sentences max).";

        try {
            return callGeminiAPI(prompt);
        } catch (Exception e) {
            e.printStackTrace();
            return "Could not generate AI Portfolio Advice at this time.";
        }
    }
}
