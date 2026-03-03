package services;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class NavyApiService {

    private static final String API_URL = "https://api.navy/v1/chat/completions";
    private static final String API_KEY = "sk-navy-CPFLtUUlVBr6LKSuK3LrUDYCVXdsua-OgMkdJ6wPcBY";
    private final HttpClient httpClient;

    public NavyApiService() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
    }

    public CompletableFuture<String> detectCurrencyByIP() {
        return CompletableFuture.supplyAsync(() -> {
            System.out.println("LOG: Detecting currency by IP...");

            // Try ipapi.co
            String currency = tryIpApiCo();
            if (currency == null) {
                // Try freeipapi.com
                currency = tryFreeIpApi();
            }
            if (currency == null) {
                // Try ip-api.com (returns country, we map it)
                currency = tryIpApiCom();
            }

            String result = (currency != null) ? currency : "USD";
            System.out.println("LOG: Final detected currency: " + result);
            return result;
        });
    }

    private String tryIpApiCo() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://ipapi.co/json/"))
                    .GET()
                    .header("User-Agent", "Mozilla/5.0")
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JSONObject json = new JSONObject(response.body());
                return json.optString("currency", null);
            }
        } catch (Exception e) {
            System.err.println("LOG ERR: ipapi.co failed: " + e.getMessage());
        }
        return null;
    }

    private String tryFreeIpApi() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://freeipapi.com/api/json"))
                    .GET()
                    .header("User-Agent", "Mozilla/5.0")
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JSONObject json = new JSONObject(response.body());
                return json.optString("currencyCode", null);
            }
        } catch (Exception e) {
            System.err.println("LOG ERR: freeipapi.com failed: " + e.getMessage());
        }
        return null;
    }

    private String tryIpApiCom() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://ip-api.com/json"))
                    .GET()
                    .header("User-Agent", "Mozilla/5.0")
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JSONObject json = new JSONObject(response.body());
                String countryCode = json.optString("countryCode", "");
                if ("TN".equalsIgnoreCase(countryCode))
                    return "TND";
                if ("FR".equalsIgnoreCase(countryCode) || "DE".equalsIgnoreCase(countryCode)
                        || "IT".equalsIgnoreCase(countryCode))
                    return "EUR";
                // Add more common mappings if needed or use country code as fallback
                return null;
            }
        } catch (Exception e) {
            System.err.println("LOG ERR: ip-api.com failed: " + e.getMessage());
        }
        return null;
    }

    public CompletableFuture<String> generateContent(String prompt) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("model", "gpt-4.1-nano");

                JSONArray messages = new JSONArray();
                JSONObject userMessage = new JSONObject();
                userMessage.put("role", "user");
                userMessage.put("content", prompt);
                messages.put(userMessage);

                payload.put("messages", messages);
                payload.put("max_tokens", 500);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_URL))
                        .header("Authorization", "Bearer " + API_KEY)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    JSONObject jsonResponse = new JSONObject(response.body());
                    JSONArray choices = jsonResponse.getJSONArray("choices");
                    if (choices.length() > 0) {
                        return choices.getJSONObject(0).getJSONObject("message").getString("content").trim();
                    }
                } else {
                    throw new RuntimeException("API Error: " + response.statusCode());
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to generate text content: " + e.getMessage(), e);
            }
            return null;
        });
    }

    public CompletableFuture<String> generateContentFromImage(File imageFile, String prompt) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 1. Read and encode image to base64
                byte[] fileContent = Files.readAllBytes(imageFile.toPath());
                String base64Image = Base64.getEncoder().encodeToString(fileContent);
                String mimeType = getMimeType(imageFile);
                String dataUrl = "data:" + mimeType + ";base64," + base64Image;

                // 2. Build JSON payload
                JSONObject payload = new JSONObject();
                payload.put("model", "gpt-4.1-nano");

                JSONArray messages = new JSONArray();
                JSONObject userMessage = new JSONObject();
                userMessage.put("role", "user");

                JSONArray contentArray = new JSONArray();

                JSONObject textObject = new JSONObject();
                textObject.put("type", "text");
                textObject.put("text", prompt);
                contentArray.put(textObject);

                JSONObject imageObject = new JSONObject();
                imageObject.put("type", "image_url");
                JSONObject imageUrlObject = new JSONObject();
                imageUrlObject.put("url", dataUrl);
                imageObject.put("image_url", imageUrlObject);
                contentArray.put(imageObject);

                userMessage.put("content", contentArray);
                messages.put(userMessage);

                payload.put("messages", messages);
                payload.put("max_tokens", 800);

                // 3. Send HTTP Request
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_URL))
                        .header("Authorization", "Bearer " + API_KEY)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    JSONObject jsonResponse = new JSONObject(response.body());
                    JSONArray choices = jsonResponse.getJSONArray("choices");
                    if (choices.length() > 0) {
                        return choices.getJSONObject(0).getJSONObject("message").getString("content").trim();
                    }
                } else {
                    System.err.println("API Error: " + response.statusCode() + " - " + response.body());
                    throw new RuntimeException("API Error: " + response.statusCode());
                }

            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Failed to generate content: " + e.getMessage(), e);
            }
            return null;
        });
    }

    public CompletableFuture<JSONObject> validateProductContext(File imageFile, String name, String fullDesc,
            double price, String currency) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("model", "gpt-4.1-nano");
                payload.put("response_format", new JSONObject().put("type", "json_object"));

                JSONArray messages = new JSONArray();
                JSONObject userMessage = new JSONObject();
                userMessage.put("role", "user");

                JSONArray contentArray = new JSONArray();

                String promptText = "You are a strict product validation AI. Analyze the product details below. " +
                        "1. Is the price logical for this type of product? " +
                        "2. Does the title match the product/image? " +
                        "3. Does the description fit the title and the image? " +
                        "\n\nTitle: " + name + "\nDescription: " + fullDesc + "\nPrice: " + price + " " + currency +
                        "\n\nReturn a JSON object EXCLUSIVELY with two fields: " +
                        "'valid' (boolean) and 'reason' (string explaining what is wrong in French if 'valid' is false. If true, reason can be empty).";

                JSONObject textObject = new JSONObject();
                textObject.put("type", "text");
                textObject.put("text", promptText);
                contentArray.put(textObject);

                if (imageFile != null && imageFile.exists()) {
                    byte[] fileContent = Files.readAllBytes(imageFile.toPath());
                    String base64Image = Base64.getEncoder().encodeToString(fileContent);
                    String mimeType = getMimeType(imageFile);
                    String dataUrl = "data:" + mimeType + ";base64," + base64Image;

                    JSONObject imageObject = new JSONObject();
                    imageObject.put("type", "image_url");
                    imageObject.put("image_url", new JSONObject().put("url", dataUrl));
                    contentArray.put(imageObject);
                }

                userMessage.put("content", contentArray);
                messages.put(userMessage);

                payload.put("messages", messages);
                payload.put("max_tokens", 800);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_URL))
                        .header("Authorization", "Bearer " + API_KEY)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    JSONObject jsonResponse = new JSONObject(response.body());
                    JSONArray choices = jsonResponse.getJSONArray("choices");
                    if (choices.length() > 0) {
                        String content = choices.getJSONObject(0).getJSONObject("message").getString("content").trim();
                        return new JSONObject(cleanJsonResponse(content)); // Parses the AI's cleaned JSON output
                    }
                } else {
                    System.err.println("API Error: " + response.statusCode() + " - " + response.body());
                    throw new RuntimeException("API Error: " + response.statusCode());
                }

            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Failed to validate context: " + e.getMessage(), e);
            }
            return new JSONObject().put("valid", false).put("reason", "Erreur technique lors de la validation.");
        });
    }

    public CompletableFuture<List<Long>> searchProductsByText(String userRequirement, List<models.Product> catalog) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("model", "gpt-4.1-nano");
                payload.put("response_format", new JSONObject().put("type", "json_object"));

                JSONArray messages = new JSONArray();
                JSONObject userMessage = new JSONObject();
                userMessage.put("role", "user");

                StringBuilder sb = new StringBuilder();
                sb.append(
                        "You are a semantic product search engine. A user is looking for a product with this requirement: '")
                        .append(userRequirement).append("'\n");
                sb.append(
                        "Analyze the provided catalog and return the IDs of the products that best satisfy the user's need.\n");
                sb.append(
                        "Return ONLY a JSON object: {\"matching_ids\": [1, 5, 8]}. If no products match, return an empty array.\n\n");
                sb.append("Catalog:\n[\n");
                for (models.Product p : catalog) {
                    sb.append(String.format(
                            "  {\"id\": %d, \"name\": \"%s\", \"category\": \"%s\", \"description\": \"%s\"},\n",
                            p.getId(), p.getName().replace("\"", "\\\""),
                            p.getCategoryName() != null ? p.getCategoryName() : "Unknown",
                            p.getDescription() != null ? p.getDescription().replace("\"", "\\\"") : ""));
                }
                sb.append("]");

                userMessage.put("content", sb.toString());
                messages.put(userMessage);
                payload.put("messages", messages);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_URL))
                        .header("Authorization", "Bearer " + API_KEY)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    JSONObject jsonResponse = new JSONObject(response.body());
                    String content = jsonResponse.getJSONArray("choices").getJSONObject(0).getJSONObject("message")
                            .getString("content").trim();
                    JSONObject aiResult = new JSONObject(cleanJsonResponse(content));
                    JSONArray array = aiResult.optJSONArray("matching_ids");
                    List<Long> ids = new java.util.ArrayList<>();
                    if (array != null) {
                        for (int i = 0; i < array.length(); i++)
                            ids.add(array.getLong(i));
                    }
                    return ids;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return new java.util.ArrayList<>();
        });
    }

    public CompletableFuture<List<Long>> searchProductsByImage(File imageFile, List<models.Product> catalog) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("model", "gpt-4.1-nano");
                payload.put("response_format", new JSONObject().put("type", "json_object"));

                JSONArray messages = new JSONArray();
                JSONObject userMessage = new JSONObject();
                userMessage.put("role", "user");

                JSONArray contentArray = new JSONArray();

                // Build a textual catalog of available products
                StringBuilder sb = new StringBuilder();
                sb.append(
                        "You are a semantic image search engine. I will provide an image and a JSON list of products available in my store.\n");
                sb.append(
                        "Your task: Identify the product(s) in the list that visually, conceptually, or functionally match the uploaded image.\n");
                sb.append(
                        "Return ONLY a JSON object exactly like this: {\"matching_ids\": [1, 5, 8]} where the array contains the IDs of the products that are a good match for the image. If none match, return an empty array.\n\n");
                sb.append("Available Products List:\n[\n");
                for (models.Product p : catalog) {
                    sb.append(String.format(
                            "  {\"id\": %d, \"name\": \"%s\", \"category\": \"%s\", \"description\": \"%s\"},\n",
                            p.getId(),
                            p.getName().replace("\"", "\\\""),
                            p.getCategoryName() != null ? p.getCategoryName() : "Unknown",
                            p.getDescription() != null ? p.getDescription().replace("\"", "\\\"") : ""));
                }
                sb.append("]");

                JSONObject textObject = new JSONObject();
                textObject.put("type", "text");
                textObject.put("text", sb.toString());
                contentArray.put(textObject);

                if (imageFile != null && imageFile.exists()) {
                    byte[] fileContent = Files.readAllBytes(imageFile.toPath());
                    String base64Image = Base64.getEncoder().encodeToString(fileContent);
                    String mimeType = getMimeType(imageFile);
                    String dataUrl = "data:" + mimeType + ";base64," + base64Image;

                    JSONObject imageObject = new JSONObject();
                    imageObject.put("type", "image_url");
                    imageObject.put("image_url", new JSONObject().put("url", dataUrl));
                    contentArray.put(imageObject);
                }

                userMessage.put("content", contentArray);
                messages.put(userMessage);

                payload.put("messages", messages);
                payload.put("max_tokens", 800);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_URL))
                        .header("Authorization", "Bearer " + API_KEY)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    JSONObject jsonResponse = new JSONObject(response.body());
                    JSONArray choices = jsonResponse.getJSONArray("choices");
                    if (choices.length() > 0) {
                        String content = choices.getJSONObject(0).getJSONObject("message").getString("content").trim();
                        JSONObject aiResult = new JSONObject(cleanJsonResponse(content));
                        JSONArray matchingIdsObj = aiResult.optJSONArray("matching_ids");

                        List<Long> matchingIds = new java.util.ArrayList<>();
                        if (matchingIdsObj != null) {
                            for (int i = 0; i < matchingIdsObj.length(); i++) {
                                matchingIds.add(matchingIdsObj.getLong(i));
                            }
                        }
                        return matchingIds;
                    }
                } else {
                    System.err.println("API Error: " + response.statusCode() + " - " + response.body());
                    throw new RuntimeException("API Error: " + response.statusCode());
                }

            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Failed to perform semantic search: " + e.getMessage(), e);
            }
            return new java.util.ArrayList<>();
        });
    }

    public CompletableFuture<String> compareProducts(models.Product p1, models.Product p2) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String prompt = String.format(
                        "Compare ces deux produits de manière professionnelle en français.\n" +
                                "Produit 1: %s (%s) - %s\n" +
                                "Produit 2: %s (%s) - %s\n" +
                                "Donne les points forts de chacun et une recommandation selon l'usage. " +
                                "IMPORTANT: N'utilise JAMAIS d'étoiles (*) ou de symboles markdown. Utilise des tirets (-) simples pour les listes. "
                                +
                                "Formatte la réponse avec des sauts de ligne clairs.",
                        p1.getName(), p1.getCategory() != null ? p1.getCategory() : "Inconnue", p1.getDescription(),
                        p2.getName(), p2.getCategory() != null ? p2.getCategory() : "Inconnue", p2.getDescription());
                return generateContent(prompt).get();
            } catch (Exception e) {
                e.printStackTrace();
                return "Erreur lors de la comparaison.";
            }
        });
    }

    public CompletableFuture<String> chatAboutProducts(models.Product p1, models.Product p2, String query) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String prompt = String.format(
                        "Tu es un expert en produits. Réponds à la question suivante en te basant sur ces deux produits :\n"
                                +
                                "1. %s: %s\n" +
                                "2. %s: %s\n\n" +
                                "Question: %s\n\n" +
                                "Réponds de manière concise et utile en français. N'utilise JAMAIS de symboles markdown comme les étoiles (**).",
                        p1.getName(), p1.getDescription(),
                        p2.getName(), p2.getDescription(),
                        query);
                return generateContent(prompt).get();
            } catch (Exception e) {
                e.printStackTrace();
                return "Erreur lors de la discussion.";
            }
        });
    }

    private String cleanJsonResponse(String content) {
        if (content == null)
            return "{}";
        String cleaned = content.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        return cleaned.trim();
    }

    private String getMimeType(File file) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".png"))
            return "image/png";
        if (name.endsWith(".gif"))
            return "image/gif";
        if (name.endsWith(".webp"))
            return "image/webp";
        return "image/jpeg";
    }
}
