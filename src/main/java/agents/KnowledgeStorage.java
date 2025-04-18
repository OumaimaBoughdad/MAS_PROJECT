package agents;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class KnowledgeStorage {
    private static final String STORAGE_FILE = "knowledge_base.json";
    private static Map<String, String> knowledgeMap = new HashMap<>();

    static {
        loadKnowledge();
    }

    public static synchronized void store(String query, String response) {
        // Don't store error responses
        if (response == null || response.contains("Error fetching") ||
                response.contains("No result") || response.contains("HTTP error") ||
                response.contains("API Error")) {
            return;
        }

        try {
            JSONObject entry = new JSONObject();
            entry.put("query", query.toLowerCase()); // Normalize to lowercase
            entry.put("response", response);
            entry.put("timestamp", System.currentTimeMillis());

            JSONArray knowledgeArray;
            if (Files.exists(Paths.get(STORAGE_FILE))) {
                String content = new String(Files.readAllBytes(Paths.get(STORAGE_FILE)));
                knowledgeArray = new JSONArray(content);
            } else {
                knowledgeArray = new JSONArray();
            }

            // Remove any existing entry for this query
            for (int i = 0; i < knowledgeArray.length(); i++) {
                JSONObject existing = knowledgeArray.getJSONObject(i);
                if (existing.getString("query").equalsIgnoreCase(query)) {
                    knowledgeArray.remove(i);
                    break;
                }
            }

            knowledgeArray.put(entry);
            Files.write(Paths.get(STORAGE_FILE), knowledgeArray.toString().getBytes());
            knowledgeMap.put(query.toLowerCase(), response);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static synchronized String retrieve(String query) {
        String cachedResponse = knowledgeMap.get(query.toLowerCase());
        if (cachedResponse != null) {
            return cachedResponse;
        }

        try {
            if (Files.exists(Paths.get(STORAGE_FILE))) {
                String content = new String(Files.readAllBytes(Paths.get(STORAGE_FILE)));
                JSONArray knowledgeArray = new JSONArray(content);

                for (int i = 0; i < knowledgeArray.length(); i++) {
                    JSONObject entry = knowledgeArray.getJSONObject(i);
                    if (entry.getString("query").equalsIgnoreCase(query)) {
                        String response = entry.getString("response");
                        knowledgeMap.put(query.toLowerCase(), response);
                        return response;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void loadKnowledge() {
        try {
            if (Files.exists(Paths.get(STORAGE_FILE))) {
                String content = new String(Files.readAllBytes(Paths.get(STORAGE_FILE)));
                JSONArray knowledgeArray = new JSONArray(content);

                for (int i = 0; i < knowledgeArray.length(); i++) {
                    JSONObject entry = knowledgeArray.getJSONObject(i);
                    knowledgeMap.put(
                            entry.getString("query").toLowerCase(),
                            entry.getString("response")
                    );
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}