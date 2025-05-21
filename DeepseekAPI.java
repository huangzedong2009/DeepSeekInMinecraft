package com.deepseek.minecraft;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class DeepseekAPI {
    private static FileConfiguration config;
    private static int eventIdCounter = 1000;

    public static void setup(JavaPlugin plugin) {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        eventIdCounter = config.getInt("events.default_event_id", 1000);
    }

    public static synchronized void reloadConfig(JavaPlugin plugin) {
        config = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "config.yml"));
    }

    public static synchronized int generateEventId() {
        return ++eventIdCounter;
    }

    public static String sendToDeepseek(String message, int eventId) {
        String endpoint = config.getString("api.endpoint");
        String apiKey = config.getString("api.key");
        int timeout = config.getInt("api.timeout", 5000);
        String model = config.getString("api.model", "deepseek-chat");

        try {
            URL url = new URL(endpoint);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setConnectTimeout(timeout);
            connection.setDoOutput(true);

            String jsonBody = String.format(
                "{\"model\":\"%s\",\"messages\":[{\"role\":\"user\",\"content\":\"%s\"}],\"event_id\":%d}",
                model,
                sanitizeMessage(message),
                eventId
            );

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                return parseResponse(connection.getInputStream(), eventId);
            } else {
                return parseError(connection.getErrorStream(), responseCode, eventId);
            }
        } catch (Exception e) {
            return formatError(e, eventId);
        }
    }

    private static String sanitizeMessage(String message) {
        return message.replace("\"", "\\\"")
                     .replace("\\", "\\\\")
                     .replace("\n", "\\n");
    }

    private static String parseResponse(InputStream input, int eventId) throws IOException {
        try (BufferedReader br = new BufferedReader(
            new InputStreamReader(input, StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }

            String responseStr = response.toString();
            int contentStart = responseStr.indexOf("\"content\":\"") + 11;
            if (contentStart > 10) {
                int contentEnd = responseStr.indexOf("\"", contentStart);
                return String.format("[EventID:%d] %s", 
                    eventId,
                    responseStr.substring(contentStart, contentEnd)
                        .replace("\\n", "\n")
                        .replace("\\\"", "\"")
                );
            }
            return String.format("[EventID:%d] 无效响应格式", eventId);
        }
    }

    private static String parseError(InputStream input, int code, int eventId) throws IOException {
        if (input == null) return String.format("[EventID:%d] 错误 %d (无详细信息)", eventId, code);
        
        try (BufferedReader br = new BufferedReader(
            new InputStreamReader(input, StandardCharsets.UTF_8))) {
            return String.format("[EventID:%d] 错误 %d: %s", 
                eventId, 
                code, 
                br.readLine()
            );
        }
    }

    private static String formatError(Exception e, int eventId) {
        return String.format("[EventID:%d] 异常: %s", 
            eventId, 
            e.getClass().getSimpleName() + ": " + e.getMessage()
        );
    }
}