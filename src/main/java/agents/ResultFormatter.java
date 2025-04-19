package agents;


import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResultFormatter {
    public static void formatAndDisplayResults(JTextArea resultArea, String results) {
        // Clear existing content
        resultArea.setText(results);
        resultArea.setCaretPosition(0);
    }
}