package agents;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Consumer;

public class HistoryItem extends JPanel {
    private final String query;
    private final JLabel queryLabel;
    private final JLabel dateLabel;

    public HistoryItem(String query, long timestamp, Consumer<String> onClickAction) {
        this.query = query;

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 220, 220)),
                new EmptyBorder(10, 15, 10, 15)));
        setBackground(Color.WHITE);
        setOpaque(true);

        // Format timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm");
        String dateStr = sdf.format(new Date(timestamp));

        // Create labels with improved styling
        queryLabel = new JLabel("<html><div style='width:250px;'>" + query + "</div></html>");
        queryLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        dateLabel = new JLabel(dateStr);
        dateLabel.setForeground(new Color(120, 120, 120));
        dateLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));

        // Add components with padding
        JPanel contentPanel = new JPanel(new BorderLayout(0, 5));
        contentPanel.setOpaque(false);
        contentPanel.add(queryLabel, BorderLayout.CENTER);
        contentPanel.add(dateLabel, BorderLayout.SOUTH);
        add(contentPanel, BorderLayout.CENTER);

        // Add hover effect
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                onClickAction.accept(query);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                setBackground(new Color(240, 245, 255));
                queryLabel.setForeground(new Color(25, 118, 210));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setBackground(Color.WHITE);
                queryLabel.setForeground(UIManager.getColor("Label.foreground"));
            }
        });
    }

    public String getQuery() {
        return query;
    }
}