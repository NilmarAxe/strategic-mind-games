package com.mindgames.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Configuration {
    private static final Properties properties = new Properties();
    private static boolean loaded = false;
    
    static {
        loadConfiguration();
    }
    
    private static void loadConfiguration() {
        try (InputStream input = Configuration.class
                .getClassLoader()
                .getResourceAsStream("config.properties")) {
            
            if (input == null) {
                System.err.println("Unable to find config.properties");
                return;
            }
            
            properties.load(input);
            loaded = true;
            System.out.println("Configuration loaded successfully");
            
        } catch (IOException e) {
            System.err.println("Error loading configuration: " + e.getMessage());
        }
    }
    
    public static String get(String key) {
        return properties.getProperty(key);
    }
    
    public static String get(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
    
    public static int getInt(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                System.err.println("Invalid integer value for " + key + ": " + value);
            }
        }
        return defaultValue;
    }
    
    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }
    
    public static boolean isLoaded() {
        return loaded;
    }
    
    // Convenience methods for common properties
    public static String getAIServerUrl() {
        return get("ai.server.url", "http://localhost:5000");
    }
    
    public static int getAIServerTimeout() {
        return getInt("ai.server.timeout", 10000);
    }
    
    public static int getAIRetryAttempts() {
        return getInt("ai.server.retry.attempts", 3);
    }
    
    public static int getAIRetryDelay() {
        return getInt("ai.server.retry.delay", 1000);
    }
    
    public static String getRustLibraryName() {
        return get("rust.library.name", "strategic_mind_optimizer");
    }
    
    public static int getRustDefaultDepth() {
        return getInt("rust.search.default.depth", 4);
    }
    
    public static int getWindowWidth() {
        return getInt("ui.window.width", 1400);
    }
    
    public static int getWindowHeight() {
        return getInt("ui.window.height", 900);
    }
}