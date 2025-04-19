package utils;

import java.net.*;
import java.io.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import org.json.*;

public class HttpHelper {
    private static final int TIMEOUT = 10000000;

    public static String searchExternalSource(String source, String query) {
        try {
            switch (source.toLowerCase()) {
                case "wikipedia":
                    return "\n" + searchWikipedia(query);
                case "openrouter":
                    return "\n" + queryOpenRouter(query);
                case "duckduckgo":
                    return "\n" + searchDuckDuckGo(query);
                case "googlebooks":
                    return "\n" + searchGoogleBooks(query);
                case "wikidata":
                    return "\n" + searchWikidata(query);
                case "togetherai":
                    return "\n" + queryTogetherAI(query);
                case "langsearch":
                    return "\n" + queryLangSearch(query);
                case "deepinfra":
                    return "\n" + queryDeepInfra(query);
                default:
                    return "Unknown source: " + source;
            }
        } catch (Exception e) {
            return source + " Result:\nError fetching from " + source + ": " + cleanErrorMessage(e.getMessage());
        }
    }

    public static String searchWikidata(String query) throws Exception {
        String formattedQuery = query.trim().replace(" ", "_");
        String url = "https://www.wikidata.org/w/api.php?action=wbgetentities&sites=enwiki&titles="
                + URLEncoder.encode(formattedQuery, "UTF-8") + "&format=json";
        String response = sendGet(url);
        JSONObject json = new JSONObject(response);
        JSONObject entities = json.optJSONObject("entities");
        if (entities == null) {
            throw new Exception("No entities found in Wikidata response");
        }
        for (String key : entities.keySet()) {
            JSONObject entity = entities.getJSONObject(key);
            JSONObject labels = entity.optJSONObject("labels");
            JSONObject descriptions = entity.optJSONObject("descriptions");

            String label = (labels != null && labels.has("en"))
                    ? labels.getJSONObject("en").getString("value")
                    : "";
            String description = (descriptions != null && descriptions.has("en"))
                    ? descriptions.getJSONObject("en").getString("value")
                    : "No description available";

            return label + ": " + description;
        }
        throw new Exception("No Wikidata entity found");
    }

    public static String queryLangSearch(String query) throws IOException {
        String apiUrl = "https://api.langsearch.com/v1/web-search";
        String apiKey = "Bearer sk-fc5b15b69cf74f8a8658a47fe46bca03";  // TODO: replace with your key

        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", apiKey);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        String payload = String.format(
                "{\"query\":\"%s\",\"freshness\":\"oneYear\",\"summary\":true,\"count\":5}",
                query.replace("\"", "\\\"")
        );
        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload.getBytes("utf-8"));
        }

        // 2) Read response
        StringBuilder raw = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))) {
            String line;
            while ((line = br.readLine()) != null) raw.append(line);
        }

        // 3) Parse JSON and extract snippet + link
        JSONObject root = new JSONObject(raw.toString());
        JSONObject data = root.optJSONObject("data");
        if (data == null || !data.has("webPages")) {
            return "No results found.";
        }

        JSONArray pages = data.getJSONObject("webPages").optJSONArray("value");
        if (pages == null || pages.length() == 0) {
            return "No results found.";
        }

        StringBuilder out = new StringBuilder();
        int maxResults = 3; // Show fewer but more complete results
        int maxSnippetLength = 300; // Limit per snippet

        for (int i = 0; i < Math.min(pages.length(), maxResults); i++) {
            JSONObject page = pages.getJSONObject(i);
            String title = page.optString("name", "No title");
            String urlLink = page.optString("url", "No URL");
            String snippet = page.optString("snippet", "")
                    .replaceAll("\\s+", " ")
                    .trim();

            // Handle truncation markers
            boolean isTruncated = snippet.endsWith("...") ||
                    snippet.length() >= maxSnippetLength;

            if (snippet.length() > maxSnippetLength) {
                snippet = snippet.substring(0, maxSnippetLength) + "...";
            }

            out.append("• ").append(title).append("\n")
                    .append("  ").append(urlLink).append("\n")
                    .append("  ").append(snippet);

            if (isTruncated) {
                out.append(" [read more]");
            }
            out.append("\n\n");
        }

        return out.toString().trim();
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

        if (json.has("extract") && !json.isNull("extract")) {
            String extract = json.getString("extract");
            // Add "..." only if the extract appears truncated
            if (extract.length() > 0 && extract.charAt(extract.length()-1) != '.') {
                extract += "...";
            }
            return extract;
        }

        if (json.has("content_urls")) {
            return "Summary not available. Full article: " +
                    json.getJSONObject("content_urls").getString("desktop");
        }

        throw new Exception("Page not found or no summary available");
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

    public static String searchWolframAlpha(String query) throws Exception {
        String apiKey = "YEU4GX-67X6429KHW"; // Replace with your API key
        String encodedQuery = URLEncoder.encode(query, "UTF-8");
        // Added more parameters to get richer results
        String url = "http://api.wolframalpha.com/v2/query?input=" + encodedQuery +
                "&format=plaintext,image&output=JSON&appid=" + apiKey +
                "&includepodid=Result&includepodid=IndefiniteIntegral&includepodid=DefiniteIntegral" +
                "&includepodid=Plot&includepodid=Derivative&includepodid=Limit";

        String response = sendGet(url);
        JSONObject json = new JSONObject(response);
        JSONObject queryResult = json.getJSONObject("queryresult");

        if (!queryResult.getBoolean("success")) {
            return "No results found for the query.";
        }

        StringBuilder result = new StringBuilder();

        if (!queryResult.has("pods") || queryResult.getJSONArray("pods").length() == 0) {
            return "No interpretable results found.";
        }

        JSONArray pods = queryResult.getJSONArray("pods");

        // List of important pods we want to include
        Set<String> importantPods = new HashSet<>(Arrays.asList(
                "Input", "Result", "IndefiniteIntegral", "DefiniteIntegral",
                "Derivative", "Limit", "Plot", "GeometricFigure", "3DPlot",
                "Integral", "SeriesExpansion", "AlternateForm"
        ));

        for (int i = 0; i < pods.length(); i++) {
            JSONObject pod = pods.getJSONObject(i);
            String podId = pod.getString("id");

            // Only process important pods
            if (importantPods.contains(podId)) {
                String podTitle = pod.getString("title");
                result.append("\n=== ").append(podTitle).append(" ===\n");

                JSONArray subpods = pod.getJSONArray("subpods");
                for (int j = 0; j < subpods.length(); j++) {
                    JSONObject subpod = subpods.getJSONObject(j);
                    String subpodTitle = subpod.optString("title", "");
                    String plaintext = subpod.optString("plaintext", "");

                    if (!subpodTitle.isEmpty()) {
                        result.append(subpodTitle).append(": ");
                    }
                    if (!plaintext.isEmpty()) {
                        result.append(plaintext).append("\n");
                    }

                    // Check for images if available
                    if (subpod.has("img")) {
                        JSONObject img = subpod.getJSONObject("img");
                        result.append("[Image available: ").append(img.getString("title")).append("]\n");
                    }
                }
            }
        }

        // Add assumptions and warnings if available
        if (queryResult.has("assumptions")) {
            result.append("\nAssumptions:\n");
            JSONArray assumptions = queryResult.getJSONArray("assumptions");
            for (int i = 0; i < assumptions.length(); i++) {
                JSONObject assumption = assumptions.getJSONObject(i);
                result.append("- ").append(assumption.getString("desc")).append("\n");
            }
        }

        if (queryResult.has("warnings")) {
            JSONObject warnings = queryResult.getJSONObject("warnings");
            if (warnings.has("delimiters")) {
                result.append("\nNote: Interpretation may be ambiguous. Try being more specific.\n");
            }
        }

        return result.toString().trim();
    }

//    private static String queryTogetherAI(String prompt) {
//        try {
//            URL url = new URL("https://api.together.xyz/v1/chat/completions");
//            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//            conn.setRequestMethod("POST");
//            conn.setRequestProperty("Authorization", "Bearer tgp_v1_cQRZMhXE8oa_GDIe6783hJ_rjSenBq4OooZblbILCc8");
//            conn.setRequestProperty("Content-Type", "application/json");
//            conn.setConnectTimeout(TIMEOUT);
//            conn.setReadTimeout(TIMEOUT);
//            conn.setDoOutput(true);
//
//            String payload = String.format("""
//                {
//                  \"model\": \"mistralai/Mixtral-8x7B-Instruct-v0.1\",
//                  \"messages\": [{\"role\": \"user\", \"content\": \"%s\"}],
//                  \"temperature\": 0.7,
//                  \"max_tokens\": 1000
//                }
//                """, prompt.replace("\"", "\\\""));
//
//            try (OutputStream os = conn.getOutputStream()) {
//                os.write(payload.getBytes());
//                os.flush();
//            }
//
//            if (conn.getResponseCode() == 200) {
//                try (BufferedReader in = new BufferedReader(
//                        new InputStreamReader(conn.getInputStream()))) {
//                    JSONObject jsonResponse = new JSONObject(readAll(in));
//                    return jsonResponse.getJSONArray("choices")
//                            .getJSONObject(0)
//                            .getJSONObject("message")
//                            .getString("content");
//                }
//            } else {
//                return "API Error: " + conn.getResponseCode();
//            }
//        } catch (Exception e) {
//            return "Connection Error: " + e.getMessage();
//        }
//    }//

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
    public static String queryTogetherAI(String prompt) {
        try {
            URL url = new URL("https://api.together.xyz/v1/chat/completions");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer tgp_v1_DdPVLqyTQKjDAhxIrWGGh9r2DEHqDY82EK2AeO5aTYY"); // Replace with your key
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(15000); // 15 seconds timeout
            conn.setReadTimeout(15000);
            conn.setDoOutput(true);

            // Improved prompt formatting
            String formattedPrompt = prompt.endsWith("?") ? prompt : prompt + "?";
            String payload = String.format("""
            {
              "model": "mistralai/Mixtral-8x7B-Instruct-v0.1",
              "messages": [{
                "role": "system",
                "content": "You are a helpful AI assistant that provides accurate and concise answers."
              },{
                "role": "user",
                "content": "%s"
              }],
              "temperature": 0.7,
              "max_tokens": 1000,
              "stop": ["</s>"]
            }
            """, formattedPrompt.replace("\"", "\\\""));

            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.getBytes());
                os.flush();
            }

            // Check response status
            if (conn.getResponseCode() == 200) {
                try (BufferedReader in = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()))) {
                    JSONObject jsonResponse = new JSONObject(readAll(in));
                    String content = jsonResponse.getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content");

                    // Clean up the response
                    content = content.replaceAll("^\"|\"$", "") // Remove surrounding quotes
                            .replaceAll("\\n{3,}", "\n\n") // Limit consecutive newlines
                            .trim();

                    return content.isEmpty() ? "No response content from Together.ai" : content;
                }
            } else {
                // Read error stream for more details
                try (BufferedReader err = new BufferedReader(
                        new InputStreamReader(conn.getErrorStream()))) {
                    String error = readAll(err);
                    return "Together.ai API Error (" + conn.getResponseCode() + "): " + error;
                }
            }
        } catch (Exception e) {
            return "Together.ai Connection Error: " + e.getMessage();
        }
    }

    public static String queryDeepInfra(String prompt) throws Exception {
        URL url = new URL("https://api.deepinfra.com/v1/openai/chat/completions");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer fxwH02MoRWd188aXfwZbi6muVDg5mp92");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(TIMEOUT);
        conn.setReadTimeout(TIMEOUT);
        conn.setDoOutput(true);

        String payload = String.format("""
        {
          "model": "mistralai/Mixtral-8x7B-Instruct-v0.1",
          "messages": [{
            "role": "user",
            "content": "%s"
          }],
          "temperature": 0.7,
          "max_tokens": 1000
        }
        """, prompt.replace("\"", "\\\""));

        return executeRequest(conn, payload);
    }
    private static String executeRequest(HttpURLConnection conn, String payload) throws Exception {
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
            throw new IOException("API Error: " + conn.getResponseCode());
        }
    }


}