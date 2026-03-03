package edu.connexion3a8.tools;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class GeocodingService {

    private static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org/search";
    private static final String USER_AGENT = "INVESTI-EventApp/1.0";

    public static class GeoLocation {
        public final double latitude;
        public final double longitude;
        public final String displayName;

        public GeoLocation(double latitude, double longitude, String displayName) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.displayName = displayName;
        }
    }

    public static GeoLocation geocode(String address) {
        try {
            String encodedAddress = URLEncoder.encode(address, StandardCharsets.UTF_8);
            String urlString = NOMINATIM_URL + "?q=" + encodedAddress + "&format=json&limit=1";

            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();

                JSONArray results = new JSONArray(response.toString());
                if (results.length() > 0) {
                    JSONObject location = results.getJSONObject(0);
                    double lat = location.getDouble("lat");
                    double lon = location.getDouble("lon");
                    String displayName = location.getString("display_name");
                    return new GeoLocation(lat, lon, displayName);
                }
            }
        } catch (Exception e) {
            System.err.println("Geocoding error: " + e.getMessage());
        }
        return null;
    }
}
