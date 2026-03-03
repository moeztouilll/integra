package edu.connexion3a8.tools;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads environment variables from .env file at project root.
 * Priority: System env > .env file
 * 
 * Usage: String key = EnvConfig.get("AWS_ACCESS_KEY_ID");
 */
public class EnvConfig {

    private static final Map<String, String> envVars = new HashMap<>();
    private static boolean loaded = false;

    static {
        loadEnvFile();
    }

    private static void loadEnvFile() {
        if (loaded) return;
        loaded = true;

        Path envPath = Paths.get(".env");
        if (!Files.exists(envPath)) {
            // Try parent directories (for when running from IDE)
            envPath = Paths.get(System.getProperty("user.dir"), ".env");
        }

        if (!Files.exists(envPath)) {
            System.out.println("[EnvConfig] No .env file found. Using system environment variables only.");
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(envPath.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // Skip comments and empty lines
                if (line.isEmpty() || line.startsWith("#")) continue;

                int eqIndex = line.indexOf('=');
                if (eqIndex > 0) {
                    String key = line.substring(0, eqIndex).trim();
                    String value = line.substring(eqIndex + 1).trim();
                    envVars.put(key, value);
                }
            }
            System.out.println("[EnvConfig] Loaded " + envVars.size() + " variables from .env");
        } catch (IOException e) {
            System.err.println("[EnvConfig] Error reading .env: " + e.getMessage());
        }
    }

    /**
     * Get a config value. Checks system env first, then .env file.
     */
    public static String get(String key) {
        // System environment takes priority
        String sysVal = System.getenv(key);
        if (sysVal != null && !sysVal.isEmpty()) {
            return sysVal;
        }
        return envVars.getOrDefault(key, "");
    }

    /**
     * Get a config value with a default fallback.
     */
    public static String get(String key, String defaultValue) {
        String val = get(key);
        return (val != null && !val.isEmpty()) ? val : defaultValue;
    }

    /**
     * Check if a key is configured (non-empty and not a placeholder).
     */
    public static boolean isSet(String key) {
        String val = get(key);
        return val != null && !val.isEmpty() 
            && !val.startsWith("YOUR_") 
            && !val.equals("PASTE_HERE");
    }
}
