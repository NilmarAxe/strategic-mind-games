package com.mindgames.ui;

import com.mindgames.core.Player;

import javax.swing.*;
import java.awt.*;

public class StatisticsPanel extends JPanel {
    private JLabel player1NameLabel;
    private JLabel player1TrustLabel;
    private JLabel player1BluffLabel;
    private JLabel player1ChallengeLabel;
    
    private JLabel player2NameLabel;
    private JLabel player2TrustLabel;
    private JLabel player2BluffLabel;
    private JLabel player2ChallengeLabel;
    
    public StatisticsPanel() {
        setPreferredSize(new Dimension(350, 300));
        setBackground(new Color(30, 30, 40));
        setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(80, 80, 90), 2),
            "Player Statistics",
            0,
            0,
            new Font("Arial", Font.BOLD, 16),
            Color.WHITE
        ));
        
        initializeComponents();
        layoutComponents();
    }
    
    private void initializeComponents() {
        player1NameLabel = createLabel("Player 1", 18, true);
        player1TrustLabel = createLabel("Trust: --", 14, false);
        player1BluffLabel = createLabel("Bluff Success: --%", 12, false);
        player1ChallengeLabel = createLabel("Challenge Success: --%", 12, false);
        
        player2NameLabel = createLabel("Player 2", 18, true);
        player2TrustLabel = createLabel("Trust: --", 14, false);
        player2BluffLabel = createLabel("Bluff Success: --%", 12, false);
        player2ChallengeLabel = createLabel("Challenge Success: --%", 12, false);
    }
    
    private JLabel createLabel(String text, int fontSize, boolean bold) {
        JLabel label = new JLabel(text);
        label.setForeground(Color.WHITE);
        label.setFont(new Font("Arial", 
            bold ? Font.BOLD : Font.PLAIN, fontSize));
        return label;
    }
    
    private void layoutComponents() {
        setLayout(new GridLayout(2, 1, 10, 10));
        setBorder(BorderFactory.createCompoundBorder(
            getBorder(),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        
        JPanel player1Panel = createPlayerPanel(
            player1NameLabel, player1TrustLabel, 
            player1BluffLabel, player1ChallengeLabel,
            new Color(70, 130, 220)
        );
        
        JPanel player2Panel = createPlayerPanel(
            player2NameLabel, player2TrustLabel,
            player2BluffLabel, player2ChallengeLabel,
            new Color(220, 70, 90)
        );
        
        add(player1Panel);
        add(player2Panel);
    }
    
    private JPanel createPlayerPanel(JLabel nameLabel, JLabel trustLabel,
                                    JLabel bluffLabel, JLabel challengeLabel,
                                    Color accentColor) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(40, 40, 50));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(accentColor, 2),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        trustLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        bluffLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        challengeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        panel.add(nameLabel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(trustLabel);
        panel.add(Box.createVerticalStrut(5));
        panel.add(bluffLabel);
        panel.add(Box.createVerticalStrut(5));
        panel.add(challengeLabel);
        
        return panel;
    }
    
    public void updateStatistics(Player player1, Player player2) {
        if (player1 != null) {
            player1NameLabel.setText(player1.getName());
            player1TrustLabel.setText("Trust: " + player1.getTrustScore());
            player1BluffLabel.setText(String.format("Bluff Success: %.1f%%",
                player1.getBluffSuccessRate() * 100));
            player1ChallengeLabel.setText(String.format("Challenge Success: %.1f%%",
                player1.getChallengeSuccessRate() * 100));
        }
        
        if (player2 != null) {
            player2NameLabel.setText(player2.getName());
            player2TrustLabel.setText("Trust: " + player2.getTrustScore());
            player2BluffLabel.setText(String.format("Bluff Success: %.1f%%",
                player2.getBluffSuccessRate() * 100));
            player2ChallengeLabel.setText(String.format("Challenge Success: %.1f%%",
                player2.getChallengeSuccessRate() * 100));
        }
    }
    
    public void reset() {
        player1NameLabel.setText("Player 1");
        player1TrustLabel.setText("Trust: --");
        player1BluffLabel.setText("Bluff Success: --%");
        player1ChallengeLabel.setText("Challenge Success: --%");
        
        player2NameLabel.setText("Player 2");
        player2TrustLabel.setText("Trust: --");
        player2BluffLabel.setText("Bluff Success: --%");
        player2ChallengeLabel.setText("Challenge Success: --%");
    }
}