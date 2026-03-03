package edu.connexion3a8.services.collaboration;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * CurrencyService — fetches live exchange rates from the free ExchangeRate API.
 * API: https://open.er-api.com/v6/latest/USD (no key required, 1500
 * calls/month)
 * Rates are cached for the app session to avoid hitting rate limits.
 */
public class CurrencyService {

    private static final String API_URL = "https://open.er-api.com/v6/latest/USD";

    // Session-level cache: base is USD
    private static Map<String, Double> cachedRates = null;
    private static long lastFetchTime = 0;
    // Cache for 1 hour (3600000 ms)
    private static final long CACHE_DURATION_MS = 3_600_000L;

    private static final OkHttpClient httpClient = new OkHttpClient();

    /**
     * Returns all available rates (base USD).
     * Returns cached value if still fresh.
     */
    public static Map<String, Double> getRates() {
        long now = System.currentTimeMillis();
        if (cachedRates != null && (now - lastFetchTime) < CACHE_DURATION_MS) {
            return cachedRates;
        }
        try {
            Request request = new Request.Builder().url(API_URL).build();
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String body = response.body().string();
                    JSONObject json = new JSONObject(body);
                    JSONObject rates = json.getJSONObject("rates");
                    Map<String, Double> map = new HashMap<>();
                    for (String key : rates.keySet()) {
                        map.put(key, rates.getDouble(key));
                    }
                    cachedRates = map;
                    lastFetchTime = now;
                    System.out.println("[CurrencyService] Rates fetched from API.");
                    return cachedRates;
                }
            }
        } catch (IOException e) {
            System.err.println("[CurrencyService] Network error: " + e.getMessage());
        }
        // Fallback rates if API is unavailable
        if (cachedRates == null) {
            cachedRates = new HashMap<>();
            cachedRates.put("USD", 1.0);
            cachedRates.put("EUR", 0.92);
            cachedRates.put("TND", 3.11);
            cachedRates.put("GBP", 0.79);
            cachedRates.put("MAD", 10.1);
            cachedRates.put("DZD", 134.5);
            System.err.println("[CurrencyService] Using fallback rates.");
        }
        return cachedRates;
    }

    /**
     * Converts an amount from USD to the target currency.
     * All internal values are stored in USD.
     */
    public static double convertFromUSD(double amountUSD, String targetCurrency) {
        Map<String, Double> rates = getRates();
        Double rate = rates.get(targetCurrency.toUpperCase());
        if (rate == null)
            return amountUSD;
        return amountUSD * rate;
    }

    /**
     * Converts an amount from the source currency to USD.
     */
    public static double convertToUSD(double amount, String fromCurrency) {
        Map<String, Double> rates = getRates();
        Double rate = rates.get(fromCurrency.toUpperCase());
        if (rate == null || rate == 0)
            return amount;
        return amount / rate;
    }

    /**
     * Formats amount with currency symbol.
     */
    public static String format(double amount, String currency) {
        return String.format("%.2f %s", amount, currency.toUpperCase());
    }

    /**
     * Returns the list of supported display currencies for UI combo boxes.
     */
    public static String[] getSupportedCurrencies() {
        return new String[] { "USD", "EUR", "TND", "GBP", "MAD", "DZD" };
    }
}
