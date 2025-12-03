package com.mindgames.ui;

import com.mindgames.core.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class ControlPanel extends JPanel {
    private final GameWindow gameWindow;
    private final GameEngine engine;
    
    private JButton makeClaimButton;
    private JButton challengeButton;
    private JButton acceptButton;
    private JComboBox<String> claimTypeCombo;
    private JSlider boldnessSlider;
    private JTextArea claimTextArea;
    
    private boolean controlsEnabled = false;
    
    public ControlPanel(GameWindow gameWindow, GameEngine engine) {
        this.gameWindow = gameWindow;
        this.engine = engine;
        
        setPreferredSize(new Dimension(1400, 200));
        setBackground(new Color(35, 35, 45));
        setBorder(BorderFactory.createMatteBorder(2, 0, 0, 0, 
            new Color(80, 80, 90)));
        
        initializeComponents();
        layoutComponents();
        bindEvents();
        enableControls(false);
    }
    
    private void initializeComponents() {
        makeClaimButton = new JButton("Make Claim");
        challengeButton = new JButton("Challenge Claim");
        acceptButton = new JButton("Accept Claim");
        
        styleButton(makeClaimButton, new Color(70, 130, 220));
        styleButton(challengeButton, new Color(220, 70, 90));
        styleButton(acceptButton, new Color(100, 180, 100));
        
        String[] claimTypes = {"Information", "Prediction", "Accusation", "Alliance"};
        claimTypeCombo = new JComboBox<>(claimTypes);
        claimTypeCombo.setFont(new Font("Arial", Font.PLAIN, 14));
        
        boldnessSlider = new JSlider(0, 100, 50);
        boldnessSlider.setMajorTickSpacing(25);
        boldnessSlider.setMinorTickSpacing(5);
        boldnessSlider.setPaintTicks(true);
        boldnessSlider.setPaintLabels(true);
        boldnessSlider.setBackground(new Color(35, 35, 45));
        boldnessSlider.setForeground(Color.WHITE);
        
        claimTextArea = new JTextArea(3, 40);
        claimTextArea.setLineWrap(true);
        claimTextArea.setWrapStyleWord(true);
        claimTextArea.setFont(new Font("Arial", Font.PLAIN, 14));
        claimTextArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(80, 80, 90), 2),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
    }
    
    private void styleButton(JButton button, Color color) {
        button.setFont(new Font("Arial", Font.BOLD, 16));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setPreferredSize(new Dimension(180, 50));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(color.brighter());
                }
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(color);
            }
        });
    }
    
    private void layoutComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Left panel - Claim creation
        JPanel claimPanel = new JPanel();
        claimPanel.setLayout(new BoxLayout(claimPanel, BoxLayout.Y_AXIS));
        claimPanel.setBackground(new Color(35, 35, 45));
        
        JLabel claimLabel = new JLabel("Create Claim:");
        claimLabel.setForeground(Color.WHITE);
        claimLabel.setFont(new Font("Arial", Font.BOLD, 14));
        claimLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JPanel typePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        typePanel.setBackground(new Color(35, 35, 45));
        JLabel typeLabel = new JLabel("Type:");
        typeLabel.setForeground(Color.WHITE);
        typePanel.add(typeLabel);
        typePanel.add(claimTypeCombo);
        typePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JPanel boldnessPanel = new JPanel(new BorderLayout());
        boldnessPanel.setBackground(new Color(35, 35, 45));
        JLabel boldnessLabel = new JLabel("Boldness (Risk/Reward):");
        boldnessLabel.setForeground(Color.WHITE);
        boldnessPanel.add(boldnessLabel, BorderLayout.NORTH);
        boldnessPanel.add(boldnessSlider, BorderLayout.CENTER);
        boldnessPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JScrollPane textScrollPane = new JScrollPane(claimTextArea);
        textScrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        claimPanel.add(claimLabel);
        claimPanel.add(Box.createVerticalStrut(5));
        claimPanel.add(typePanel);
        claimPanel.add(Box.createVerticalStrut(10));
        claimPanel.add(textScrollPane);
        claimPanel.add(Box.createVerticalStrut(10));
        claimPanel.add(boldnessPanel);
        
        // Right panel - Actions
        JPanel actionPanel = new JPanel();
        actionPanel.setLayout(new GridLayout(3, 1, 10, 10));
        actionPanel.setBackground(new Color(35, 35, 45));
        actionPanel.add(makeClaimButton);
        actionPanel.add(challengeButton);
        actionPanel.add(acceptButton);
        
        add(claimPanel, BorderLayout.CENTER);
        add(actionPanel, BorderLayout.EAST);
    }
    
    private void bindEvents() {
        makeClaimButton.addActionListener(e -> handleMakeClaim());
        challengeButton.addActionListener(e -> handleChallenge());
        acceptButton.addActionListener(e -> handleAccept());
    }
    
    private void handleMakeClaim() {
        String claimText = claimTextArea.getText().trim();
        
        if (claimText.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please enter a claim description.",
                "Invalid Claim",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        Claim.ClaimType type = switch (claimTypeCombo.getSelectedIndex()) {
            case 0 -> Claim.ClaimType.INFORMATION;
            case 1 -> Claim.ClaimType.PREDICTION;
            case 2 -> Claim.ClaimType.ACCUSATION;
            case 3 -> Claim.ClaimType.ALLIANCE;
            default -> Claim.ClaimType.INFORMATION;
        };
        
        double boldness = boldnessSlider.getValue() / 100.0;
        
        Claim claim = new Claim(claimText, type, boldness);
        
        GameState state = engine.getCurrentState();
        Player currentPlayer = state.getPlayer1(); // Assuming human is player1
        
        Move move = new Move.Builder()
            .player(currentPlayer)
            .type(Move.MoveType.CLAIM)
            .claim(claim)
            .build();
        
        gameWindow.processPlayerMove(move);
        
        claimTextArea.setText("");
        boldnessSlider.setValue(50);
    }
    
    private void handleChallenge() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to challenge this claim?\n" +
            "You'll gain +15 trust if successful, or lose -15 if wrong.",
            "Confirm Challenge",
            JOptionPane.YES_NO_OPTION);
        
        if (confirm != JOptionPane.YES_OPTION) return;
        
        GameState state = engine.getCurrentState();
        Player currentPlayer = state.getPlayer1();
        
        Move move = new Move.Builder()
            .player(currentPlayer)
            .type(Move.MoveType.CHALLENGE)
            .challenge(true)
            .confidence(0.7)
            .build();
        
        gameWindow.processPlayerMove(move);
    }
    
    private void handleAccept() {
        GameState state = engine.getCurrentState();
        Player currentPlayer = state.getPlayer1();
        
        Move move = new Move.Builder()
            .player(currentPlayer)
            .type(Move.MoveType.ACCEPT)
            .challenge(false)
            .confidence(0.5)
            .build();
        
        gameWindow.processPlayerMove(move);
    }
    
    public void updateForState(GameState state) {
        GameEngine.GamePhase phase = state.getPhase();
        
        switch (phase) {
            case CLAIM:
                makeClaimButton.setEnabled(true);
                challengeButton.setEnabled(false);
                acceptButton.setEnabled(false);
                claimTextArea.setEnabled(true);
                claimTypeCombo.setEnabled(true);
                boldnessSlider.setEnabled(true);
                break;
                
            case CHALLENGE:
                makeClaimButton.setEnabled(false);
                challengeButton.setEnabled(true);
                acceptButton.setEnabled(true);
                claimTextArea.setEnabled(false);
                claimTypeCombo.setEnabled(false);
                boldnessSlider.setEnabled(false);
                break;
                
            case RESOLUTION:
                makeClaimButton.setEnabled(false);
                challengeButton.setEnabled(false);
                acceptButton.setEnabled(false);
                claimTextArea.setEnabled(false);
                claimTypeCombo.setEnabled(false);
                boldnessSlider.setEnabled(false);
                break;
        }
    }
    
    public void enableControls(boolean enabled) {
        this.controlsEnabled = enabled;
        
        if (!enabled) {
            makeClaimButton.setEnabled(false);
            challengeButton.setEnabled(false);
            acceptButton.setEnabled(false);
            claimTextArea.setEnabled(false);
            claimTypeCombo.setEnabled(false);
            boldnessSlider.setEnabled(false);
        }
    }
    
    public void reset() {
        claimTextArea.setText("");
        boldnessSlider.setValue(50);
        claimTypeCombo.setSelectedIndex(0);
        enableControls(false);
    }
}