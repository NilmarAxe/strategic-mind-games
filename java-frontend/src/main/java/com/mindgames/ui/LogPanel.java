package com.mindgames.ui;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogPanel extends JTextPane {
    private final StyledDocument document;
    private final SimpleDateFormat timeFormat;
    
    private static final int MAX_MESSAGES = 1000;
    private int messageCount = 0;
    
    public LogPanel() {
        this.document = getStyledDocument();
        this.timeFormat = new SimpleDateFormat("HH:mm:ss");
        
        setEditable(false);
        setBackground(new Color(25, 25, 35));
        setForeground(Color.WHITE);
        setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        initializeStyles();
        
        addMessage("System initialized. Welcome to Strategic Mind Games.");
    }
    
    private void initializeStyles() {
        Style defaultStyle = document.addStyle("default", null);
        StyleConstants.setForeground(defaultStyle, Color.WHITE);
        StyleConstants.setFontFamily(defaultStyle, "Monospaced");
        StyleConstants.setFontSize(defaultStyle, 12);
        
        Style timestampStyle = document.addStyle("timestamp", defaultStyle);
        StyleConstants.setForeground(timestampStyle, new Color(120, 120, 140));
        
        Style infoStyle = document.addStyle("info", defaultStyle);
        StyleConstants.setForeground(infoStyle, new Color(100, 200, 255));
        
        Style successStyle = document.addStyle("success", defaultStyle);
        StyleConstants.setForeground(successStyle, new Color(100, 255, 100));
        
        Style warningStyle = document.addStyle("warning", defaultStyle);
        StyleConstants.setForeground(warningStyle, new Color(255, 200, 100));
        
        Style errorStyle = document.addStyle("error", defaultStyle);
        StyleConstants.setForeground(errorStyle, new Color(255, 100, 100));
        StyleConstants.setBold(errorStyle, true);
    }
    
    public void addMessage(String message) {
        addMessage(message, "info");
    }
    
    public void addSuccess(String message) {
        addMessage(message, "success");
    }
    
    public void addWarning(String message) {
        addMessage(message, "warning");
    }
    
    public void addError(String message) {
        addMessage(message, "error");
    }
    
    private void addMessage(String message, String styleType) {
        SwingUtilities.invokeLater(() -> {
            try {
                String timestamp = timeFormat.format(new Date());
                
                document.insertString(document.getLength(), 
                    "[" + timestamp + "] ", 
                    document.getStyle("timestamp"));
                
                document.insertString(document.getLength(), 
                    message + "\n", 
                    document.getStyle(styleType));
                
                messageCount++;
                
                // Limit message history
                if (messageCount > MAX_MESSAGES) {
                    Element root = document.getDefaultRootElement();
                    Element first = root.getElement(0);
                    document.remove(0, first.getEndOffset());
                    messageCount--;
                }
                
                // Auto-scroll to bottom
                setCaretPosition(document.getLength());
                
            } catch (BadLocationException e) {
                // Silently fail on UI update error
            }
        });
    }
    
    public void clear() {
        SwingUtilities.invokeLater(() -> {
            try {
                document.remove(0, document.getLength());
                messageCount = 0;
                addMessage("Log cleared.");
            } catch (BadLocationException e) {
                // Silently fail
            }
        });
    }
}