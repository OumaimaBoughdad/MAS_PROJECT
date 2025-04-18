package agents;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public class HistoryLoader {
    private static final String STORAGE_FILE = "knowledge_base.json";

    public static void loadHistory(JPanel historyPanel, Consumer<String> clickAction) {
        try {
            if (Files.exists(Paths.get(STORAGE_FILE))) {
                String content = new String(Files.readAllBytes(Paths.get(STORAGE_FILE)));
                JSONArray knowledgeArray = new JSONArray(content);

                List<HistoryItem> historyItems = new ArrayList<>();

                for (int i = 0; i < knowledgeArray.length(); i++) {
                    JSONObject entry = knowledgeArray.getJSONObject(i);
                    String query = entry.getString("query");
                    long timestamp = entry.getLong("timestamp");
                    historyItems.add(new HistoryItem(query, timestamp, clickAction));
                }

                // Sort by timestamp (newest first)
                Collections.sort(historyItems, Comparator.comparingLong(item ->
                        -Long.parseLong(item.getName().split("_")[1])));

                historyPanel.removeAll();
                for (HistoryItem item : historyItems) {
                    historyPanel.add(item);
                }

                historyPanel.revalidate();
                historyPanel.repaint();
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "Error loading history: " + e.getMessage(),
                    "History Load Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void addHistoryItem(JPanel historyPanel, String query, long timestamp, Consumer<String> clickAction) {
        HistoryItem item = new HistoryItem(query, timestamp, clickAction);
        historyPanel.add(item, 0);
        historyPanel.revalidate();
        historyPanel.repaint();
    }
}