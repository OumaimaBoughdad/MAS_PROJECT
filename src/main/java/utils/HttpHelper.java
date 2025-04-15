package utils;

import java.net.*;
import java.io.*;
import java.util.Scanner;

import org.json.*;

public class HttpHelper {
    public static String searchExternalSource(String source, String query) {
        try {
            switch (source.toLowerCase()) {
                case "wikipedia":
                    return searchWikipedia(query);
                case "openrouter":
                    return queryOpenRouter(query);  // ✅ Add this line
                case "duckduckgo":
                    return searchDuckDuckGo(query);
                case "googlebooks":
                    return searchGoogleBooks(query);
                default:
                    return "Unknown source: " + source;
            }
        } catch (Exception e) {
            return "Error fetching from " + source + ": " + e.getMessage();
        }
    }



    private static String queryOpenRouter(String prompt) {
        try {
            URL url = new URL("https://openrouter.ai/api/v1/chat/completions");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer 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");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String payload = """
        {
          "model": "mistralai/mistral-7b-instruct",
          "messages": [{"role": "user", "content": "%s"}]
        }
        """.formatted(prompt);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.getBytes());
                os.flush();
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder result = new StringBuilder();
            String line;

            while ((line = in.readLine()) != null) {
                result.append(line);
            }

            in.close();
            return extractOpenAIResponse(result.toString()); // reuse your existing method
        } catch (Exception e) {
            e.printStackTrace();
            return "Error calling OpenRouter.";
        }
    }


    // new function
    private static String queryWolframAlpha(String query) {
        String apiKey = "YEU4GX-67X6429KHW";  // ✅ Your working WolframAlpha AppID
        try {
            String encodedQuery = URLEncoder.encode(query, "UTF-8");
            String endpoint = "http://api.wolframalpha.com/v2/query?input=" +
                    encodedQuery + "&format=plaintext&output=JSON&appid=" + apiKey;

            URL url = new URL(endpoint);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            InputStream is = connection.getInputStream();
            Scanner scanner = new Scanner(is).useDelimiter("\\A");
            String response = scanner.hasNext() ? scanner.next() : "";

            JSONObject jsonResponse = new JSONObject(response);

            if (jsonResponse.getJSONObject("queryresult").getBoolean("success")) {
                return jsonResponse
                        .getJSONObject("queryresult")
                        .getJSONArray("pods")
                        .getJSONObject(0)
                        .getJSONArray("subpods")
                        .getJSONObject(0)
                        .getString("plaintext");
            } else {
                return "No results found for the query.";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Error fetching data from WolframAlpha: " + e.getMessage();
        }
    }





    private static String extractOpenAIResponse(String json) {
        int index = json.indexOf("\"content\":\"");
        if (index == -1) return "No valid response.";
        String partial = json.substring(index + 11);
        int end = partial.indexOf("\"");
        return partial.substring(0, end).replace("\\n", "\n");
    }

    public static String searchWikipedia(String query) throws Exception {
        String formattedQuery = query.trim().replace(" ", "_");
        String url = "https://en.wikipedia.org/api/rest_v1/page/summary/" +
                URLEncoder.encode(formattedQuery, "UTF-8");
        String response = sendGet(url);
        JSONObject json = new JSONObject(response);
        return json.optString("extract", "No summary found.");
    }






    public static String searchDuckDuckGo(String query) throws Exception {
        String url = "https://api.duckduckgo.com/?q=" +
                URLEncoder.encode(query, "UTF-8") + "&format=json";
        String response = sendGet(url);
        JSONObject json = new JSONObject(response);
        String result = json.optString("AbstractText");
        if (result == null || result.isEmpty()) {
            JSONArray relatedTopics = json.optJSONArray("RelatedTopics");
            if (relatedTopics != null && relatedTopics.length() > 0) {
                JSONObject topic = relatedTopics.getJSONObject(0);
                result = topic.optString("Text", "No result found.");
            }
        }
        return result != null && !result.isEmpty() ? result : "No result found.";
    }

    public static String searchGoogleBooks(String query) throws Exception {
        String url = "https://www.googleapis.com/books/v1/volumes?q=" +
                URLEncoder.encode(query, "UTF-8");
        String response = sendGet(url);
        JSONObject json = new JSONObject(response);
        JSONArray items = json.optJSONArray("items");
        if (items != null && items.length() > 0) {
            JSONObject firstItem = items.getJSONObject(0);
            JSONObject volumeInfo = firstItem.getJSONObject("volumeInfo");
            String title = volumeInfo.optString("title", "No title");
            String authors = volumeInfo.has("authors") ?
                    volumeInfo.getJSONArray("authors").join(", ") : "No authors";
            String description = volumeInfo.optString("description", "No description");
            return "Title: " + title + "\nAuthors: " + authors +
                    "\nDescription: " + description;
        } else {
            return "No book info found.";
        }
    }

    public static String sendGet(String url) throws Exception {
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");

        int status = con.getResponseCode();
        BufferedReader in;
        if (status >= 200 && status < 300) {
            in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        } else {
            throw new IOException("HTTP error code: " + status);
        }

        StringBuilder content = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            content.append(line);
        }
        in.close();
        con.disconnect();

        return content.toString();
    }
}