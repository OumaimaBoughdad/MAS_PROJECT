package utils;

import java.net.*;
import java.io.*;
import java.util.Scanner;
import org.json.*;

public class HttpHelper {
    private static final int TIMEOUT = 10000;

    public static String searchExternalSource(String source, String query) {
        try {
            switch (source.toLowerCase()) {
                case "wikipedia":
                    return "Wikipedia Result:\n" + searchWikipedia(query);
                case "openrouter":
                    return "AI Response:\n" + queryOpenRouter(query);
                case "duckduckgo":
                    return "DuckDuckGo Result:\n" + searchDuckDuckGo(query);
                case "googlebooks":
                    return "Book Result:\n" + searchGoogleBooks(query);
                default:
                    return "Unknown source: " + source;
            }
        } catch (Exception e) {
            return source + " Result:\nError fetching from " + source + ": " + cleanErrorMessage(e.getMessage());
        }
    }

    private static String cleanErrorMessage(String error) {
        if (error == null) return "Unknown error";
        // Remove any regex patterns from error messages
        return error.replaceAll("\\^\\[\\\\\\?\\w+\\\\\\?\\]\\?\\\\s\\*\\(", "")
                .replaceAll("\\|.*", "")
                .trim();
    }

    private static String queryOpenRouter(String prompt) {
        try {
            URL url = new URL("https://openrouter.ai/api/v1/chat/completions");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer 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");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("HTTP-Referer", "YOUR_WEBSITE_URL");
            conn.setRequestProperty("X-Title", "YOUR_APP_NAME");
            conn.setConnectTimeout(TIMEOUT);
            conn.setReadTimeout(TIMEOUT);
            conn.setDoOutput(true);

            String payload = String.format("""
                {
                  "model": "mistralai/mistral-7b-instruct",
                  "messages": [{"role": "user", "content": "%s"}],
                  "temperature": 0.7,
                  "max_tokens": 1000
                }
                """, prompt.replace("\"", "\\\""));

            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.getBytes());
                os.flush();
            }

            if (conn.getResponseCode() == 200) {
                try (BufferedReader in = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()))) {
                    JSONObject jsonResponse = new JSONObject(readAll(in));
                    return jsonResponse.getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content");
                }
            } else {
                return "API Error: " + conn.getResponseCode();
            }
        } catch (Exception e) {
            return "Connection Error: " + e.getMessage();
        }
    }

    public static String searchWikipedia(String query) throws Exception {
        String formattedQuery = query.trim().replace(" ", "_");
        String url = "https://en.wikipedia.org/api/rest_v1/page/summary/" +
                URLEncoder.encode(formattedQuery, "UTF-8");
        String response = sendGet(url);
        JSONObject json = new JSONObject(response);

        if (json.has("title") && !json.isNull("extract")) {
            return json.getString("extract");
        }
        throw new Exception("Page not found");
    }

    public static String searchDuckDuckGo(String query) throws Exception {
        String url = "https://api.duckduckgo.com/?q=" +
                URLEncoder.encode(query, "UTF-8") + "&format=json&no_html=1&skip_disambig=1";
        String response = sendGet(url);
        JSONObject json = new JSONObject(response);

        String result = json.optString("AbstractText");
        if (result == null || result.isEmpty()) {
            JSONArray relatedTopics = json.optJSONArray("RelatedTopics");
            if (relatedTopics != null && relatedTopics.length() > 0) {
                result = relatedTopics.getJSONObject(0)
                        .optString("Text", "No result found.");
            }
        }

        if (result == null || result.isEmpty()) {
            throw new Exception("No results found");
        }
        return result;
    }

    public static String searchGoogleBooks(String query) throws Exception {
        String url = "https://www.googleapis.com/books/v1/volumes?q=" +
                URLEncoder.encode(query, "UTF-8") + "&maxResults=1";
        String response = sendGet(url);
        JSONObject json = new JSONObject(response);
        JSONArray items = json.optJSONArray("items");

        if (items == null || items.length() == 0) {
            throw new Exception("No books found");
        }

        JSONObject volumeInfo = items.getJSONObject(0).getJSONObject("volumeInfo");
        StringBuilder result = new StringBuilder();
        result.append("Title: ").append(volumeInfo.optString("title", "Unknown"));

        if (volumeInfo.has("authors")) {
            result.append("\nAuthors: ")
                    .append(String.join(", ", volumeInfo.getJSONArray("authors").toList().stream()
                            .map(Object::toString)
                            .toArray(String[]::new)));
        }

        result.append("\nPublished: ")
                .append(volumeInfo.optString("publishedDate", "Unknown"))
                .append("\nDescription: ")
                .append(volumeInfo.optString("description", "Not available"));

        return result.toString();
    }

    private static String sendGet(String url) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        conn.setConnectTimeout(TIMEOUT);
        conn.setReadTimeout(TIMEOUT);

        int status = conn.getResponseCode();
        if (status >= 200 && status < 300) {
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()))) {
                return readAll(in);
            }
        } else {
            throw new IOException("HTTP error code: " + status);
        }
    }

    private static String readAll(BufferedReader reader) throws IOException {
        StringBuilder content = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            content.append(line);
        }
        return content.toString();
    }
}