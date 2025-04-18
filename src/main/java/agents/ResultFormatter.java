package agents;


import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResultFormatter {

    /**
     * Formats and displays search results in the result text area with
     * highlighted source information
     */
    public static void formatAndDisplayResults(JTextArea resultArea, String results) {
        // Clear existing content
        resultArea.setText(results);
        resultArea.setCaretPosition(0);

        // If using a more advanced text component like JTextPane,
        // you could add formatting like this:
        /*
        JTextPane textPane = new JTextPane();
        StyledDocument doc = textPane.getStyledDocument();

        // Create styles
        Style defaultStyle = textPane.getStyle(StyleContext.DEFAULT_STYLE);

        Style sourceStyle = textPane.addStyle("Source", defaultStyle);
        StyleConstants.setBold(sourceStyle, true);
        StyleConstants.setForeground(sourceStyle, new Color(0, 102, 204));

        // Find and format source indicators like [Wikipedia Result]
        Pattern sourcePattern = Pattern.compile("\\[(.*?)\\]");
        Matcher matcher = sourcePattern.matcher(results);

        int lastEnd = 0;

        try {
            while (matcher.find()) {
                // Add text before this match with default style
                doc.insertString(doc.getLength(),
                                results.substring(lastEnd, m.latcher.start()),
                                defaultStyle);

                // Add the source with source style
                doc.insertString(doc.getLength(),
                                matcher.group(),
                                sourceStyle);

                lastEnd = matcher.end();
            }

            // Add any remaining text
            if (lastEnd < results.length()) {
                doc.insertString(doc.getLength(),
                                results.substring(lastEnd),
                                defaultStyle);
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        */
    }
}