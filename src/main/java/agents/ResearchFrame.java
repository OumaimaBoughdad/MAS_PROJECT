package agents;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ResearchFrame extends JFrame {
    private UserAgent agent;
    private JTextArea resultArea;
    private JTextField queryField;
    private JPanel historyPanel;
    private JScrollPane historyScrollPane;

    public ResearchFrame(UserAgent agent) {
        super("Multi-Agent Research System");
        this.agent = agent;
        setupUI();
        loadHistoryFromJson();
    }

    private void setupUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLayout(new BorderLayout());
        setBackground(new Color(245, 245, 245));

        // Create split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(300);
        splitPane.setBorder(BorderFactory.createEmptyBorder());

        // Left side - History panel with refresh button
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        historyPanel = new JPanel();
        historyPanel.setLayout(new BoxLayout(historyPanel, BoxLayout.Y_AXIS));
        historyPanel.setBackground(Color.WHITE);

        historyScrollPane = new JScrollPane(historyPanel);
        historyScrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)),
                "Research History",
                TitledBorder.LEADING,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12),
                new Color(70, 70, 70)));

        // Refresh button panel
        JPanel refreshPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        refreshPanel.setBackground(Color.WHITE);
        JButton refreshButton = new JButton("Refresh History");
        styleButton(refreshButton);
        refreshButton.addActionListener(e -> loadHistoryFromJson());
        refreshPanel.add(refreshButton);

        leftPanel.add(refreshPanel, BorderLayout.NORTH);
        leftPanel.add(historyScrollPane, BorderLayout.CENTER);

        // Right side - Results and input
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        // Query input panel
        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.setBorder(new EmptyBorder(10, 10, 15, 10));

        queryField = new JTextField();
        queryField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        queryField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                new EmptyBorder(8, 10, 8, 10)));

        JButton searchButton = new JButton("Search");
        styleButton(searchButton);

        inputPanel.add(new JLabel("Enter your search query:"), BorderLayout.NORTH);
        inputPanel.add(queryField, BorderLayout.CENTER);
        inputPanel.add(searchButton, BorderLayout.EAST);

        // Results area
        resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        resultArea.setBorder(new EmptyBorder(10, 15, 10, 15));

        JScrollPane resultScrollPane = new JScrollPane(resultArea);
        resultScrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)),
                "Results",
                TitledBorder.LEADING,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12),
                new Color(70, 70, 70)));

        rightPanel.add(inputPanel, BorderLayout.NORTH);
        rightPanel.add(resultScrollPane, BorderLayout.CENTER);

        splitPane.setLeftComponent(leftPanel);
        splitPane.setRightComponent(rightPanel);
        add(splitPane, BorderLayout.CENTER);

        // Action listener for search
        ActionListener searchAction = e -> {
            String query = queryField.getText().trim();
            if (!query.isEmpty()) {
                resultArea.setText("Searching for: " + query + "...");
                agent.sendQuery(query);
            }
        };
        searchButton.addActionListener(searchAction);
        queryField.addActionListener(searchAction);

        setLocationRelativeTo(null);
    }

    private void styleButton(JButton button) {
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setBackground(new Color(25, 118, 210));
        button.setForeground(Color.WHITE);
        button.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    public void displayResults(String results) {
        resultArea.setText(results);
        resultArea.setCaretPosition(0);
    }

    private void loadHistoryFromJson() {
        try {
            if (Files.exists(Paths.get("knowledge_base.json"))) {
                String content = new String(Files.readAllBytes(Paths.get("knowledge_base.json")));
                JSONArray knowledgeArray = new JSONArray(content);

                historyPanel.removeAll();
                for (int i = knowledgeArray.length() - 1; i >= 0; i--) {
                    JSONObject entry = knowledgeArray.getJSONObject(i);
                    String query = entry.getString("query");
                    long timestamp = entry.getLong("timestamp");
                    addHistoryItem(query, timestamp);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addHistoryItem(String query, long timestamp) {
        HistoryItem item = new HistoryItem(query, timestamp, q -> {
            queryField.setText(q);
            agent.sendQuery(q);
        });
        historyPanel.add(item);
    }
}