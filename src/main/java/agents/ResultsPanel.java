package agents;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResultsPanel extends JPanel {
    private JTextPane resultPane;
    private StyledDocument doc;
    private Style defaultStyle;
    private Style sourceStyle;
    private Style queryStyle;
    private Style headerStyle;
    private Style highlightStyle;

    public ResultsPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        setBackground(new Color(245, 245, 245));

        setupTextPane();

        JScrollPane scrollPane = new JScrollPane(resultPane);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setViewportBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(scrollPane, BorderLayout.CENTER);
    }

    private void setupTextPane() {
        resultPane = new JTextPane();
        resultPane.setEditable(false);
        resultPane.setBackground(Color.WHITE);
        resultPane.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        resultPane.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        // Create styles
        doc = resultPane.getStyledDocument();
        defaultStyle = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);
        StyleConstants.setFontFamily(defaultStyle, "Segoe UI");
        StyleConstants.setFontSize(defaultStyle, 14);

        headerStyle = resultPane.addStyle("Header", defaultStyle);
        StyleConstants.setBold(headerStyle, true);
        StyleConstants.setForeground(headerStyle, new Color(33, 33, 33));
        StyleConstants.setFontSize(headerStyle, 18);
        StyleConstants.setSpaceBelow(headerStyle, 10f);

        sourceStyle = resultPane.addStyle("Source", defaultStyle);
        StyleConstants.setBold(sourceStyle, true);
        StyleConstants.setForeground(sourceStyle, new Color(25, 118, 210));
        StyleConstants.setFontSize(sourceStyle, 14);
        StyleConstants.setSpaceBelow(sourceStyle, 5f);

        queryStyle = resultPane.addStyle("Query", defaultStyle);
        StyleConstants.setBold(queryStyle, true);
        StyleConstants.setForeground(queryStyle, new Color(33, 33, 33));
        StyleConstants.setFontSize(queryStyle, 14);

        highlightStyle = resultPane.addStyle("Highlight", defaultStyle);
        StyleConstants.setBackground(highlightStyle, new Color(255, 255, 200));
    }

    public void displayResults(String results) {
        try {
            doc.remove(0, doc.getLength());
            doc.insertString(doc.getLength(), "Search Results\n", headerStyle);

            Pattern sourcePattern = Pattern.compile("\\[(.*?) Result\\]\\n");
            Matcher matcher = sourcePattern.matcher(results);

            int lastEnd = 0;
            while (matcher.find()) {
                if (matcher.start() > lastEnd) {
                    doc.insertString(doc.getLength(),
                            results.substring(lastEnd, matcher.start()),
                            defaultStyle);
                }

                String sourceLabel = matcher.group();
                doc.insertString(doc.getLength(), sourceLabel, sourceStyle);
                lastEnd = matcher.end();
            }

            if (lastEnd < results.length()) {
                doc.insertString(doc.getLength(),
                        results.substring(lastEnd),
                        defaultStyle);
            }

            resultPane.setCaretPosition(0);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    public void clear() {
        try {
            doc.remove(0, doc.getLength());
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
}