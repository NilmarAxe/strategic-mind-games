package com.mindgames.ui;

import com.mindgames.core.*;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

public class BoardPanel extends JPanel {
    private final GameEngine engine;
    private GameState currentState;
    
    private static final Color BACKGROUND_COLOR = new Color(25, 25, 35);
    private static final Color PLAYER1_COLOR = new Color(70, 130, 220);
    private static final Color PLAYER2_COLOR = new Color(220, 70, 90);
    private static final Color NEUTRAL_COLOR = new Color(150, 150, 150);
    
    public BoardPanel(GameEngine engine) {
        this.engine = engine;
        setPreferredSize(new Dimension(800, 700));
        setBackground(BACKGROUND_COLOR);
    }
    
    public void updateState(GameState state) {
        this.currentState = state;
        repaint();
    }
    
    public void reset() {
        this.currentState = null;
        repaint();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        if (currentState == null) {
            drawWelcomeScreen(g2d);
        } else {
            drawGameBoard(g2d);
        }
    }
    
    private void drawWelcomeScreen(Graphics2D g2d) {
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;
        
        g2d.setColor(new Color(200, 200, 200));
        g2d.setFont(new Font("Arial", Font.BOLD, 48));
        
        String title = "STRATEGIC MIND GAMES";
        FontMetrics fm = g2d.getFontMetrics();
        int titleWidth = fm.stringWidth(title);
        g2d.drawString(title, centerX - titleWidth / 2, centerY - 50);
        
        g2d.setFont(new Font("Arial", Font.PLAIN, 20));
        String subtitle = "Psychological Warfare Board Game";
        int subtitleWidth = g2d.getFontMetrics().stringWidth(subtitle);
        g2d.setColor(new Color(150, 150, 150));
        g2d.drawString(subtitle, centerX - subtitleWidth / 2, centerY + 10);
        
        // Draw decorative elements
        drawDecorativeCircles(g2d, centerX, centerY);
    }
    
    private void drawDecorativeCircles(Graphics2D g2d, int centerX, int centerY) {
        g2d.setStroke(new BasicStroke(2));
        
        for (int i = 0; i < 3; i++) {
            int radius = 100 + (i * 40);
            int alpha = 50 - (i * 15);
            g2d.setColor(new Color(100, 150, 200, alpha));
            g2d.drawOval(centerX - radius, centerY - radius, 
                        radius * 2, radius * 2);
        }
    }
    
    private void drawGameBoard(Graphics2D g2d) {
        int width = getWidth();
        int height = getHeight();
        
        // Draw central dividing line
        g2d.setColor(NEUTRAL_COLOR);
        g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0));
        g2d.drawLine(width / 2, 50, width / 2, height - 50);
        
        // Draw player areas
        drawPlayerArea(g2d, currentState.getPlayer1(), 50, 100, 
                      width / 2 - 100, true);
        drawPlayerArea(g2d, currentState.getPlayer2(), width / 2 + 50, 100,
                      width / 2 - 100, false);
        
        // Draw current round info
        drawRoundInfo(g2d);
        
        // Draw current phase indicator
        drawPhaseIndicator(g2d);
        
        // Draw trust visualization
        drawTrustVisualization(g2d);
        
        // Draw current claim if exists
        if (currentState.getCurrentClaim() != null) {
            drawCurrentClaim(g2d);
        }
    }
    
    private void drawPlayerArea(Graphics2D g2d, Player player, 
                               int x, int y, int width, boolean isPlayer1) {
        if (player == null) return;
        
        Color playerColor = isPlayer1 ? PLAYER1_COLOR : PLAYER2_COLOR;
        
        // Draw player name
        g2d.setColor(playerColor);
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        g2d.drawString(player.getName(), x, y);
        
        // Draw player type
        g2d.setFont(new Font("Arial", Font.PLAIN, 14));
        g2d.setColor(NEUTRAL_COLOR);
        g2d.drawString(player.getType().toString(), x, y + 25);
        
        // Draw trust score with bar
        int trustY = y + 60;
        g2d.setFont(new Font("Arial", Font.BOLD, 18));
        g2d.setColor(Color.WHITE);
        g2d.drawString("Trust: " + player.getTrustScore(), x, trustY);
        
        drawTrustBar(g2d, x, trustY + 10, width, player.getTrustScore(), playerColor);
        
        // Draw statistics
        int statsY = trustY + 80;
        g2d.setFont(new Font("Arial", Font.PLAIN, 14));
        g2d.setColor(new Color(180, 180, 180));
        
        g2d.drawString("Bluff Success: " + 
            String.format("%.1f%%", player.getBluffSuccessRate() * 100), 
            x, statsY);
        g2d.drawString("Challenge Success: " + 
            String.format("%.1f%%", player.getChallengeSuccessRate() * 100),
            x, statsY + 25);
    }
    
    private void drawTrustBar(Graphics2D g2d, int x, int y, 
                             int width, int trustScore, Color color) {
        // Background bar
        g2d.setColor(new Color(50, 50, 50));
        g2d.fillRect(x, y, width, 20);
        
        // Calculate fill percentage (trust ranges from -50 to 100)
        double percentage = (trustScore + 50) / 150.0;
        percentage = Math.max(0, Math.min(1, percentage));
        
        int fillWidth = (int) (width * percentage);
        
        // Gradient fill
        GradientPaint gradient = new GradientPaint(
            x, y, color.darker(),
            x + fillWidth, y, color
        );
        g2d.setPaint(gradient);
        g2d.fillRect(x, y, fillWidth, 20);
        
        // Border
        g2d.setColor(color);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRect(x, y, width, 20);
        
        // Milestone markers
        g2d.setColor(new Color(200, 200, 200, 100));
        int zeroPoint = (int) (width * (50.0 / 150.0)); // 0 trust position
        g2d.drawLine(x + zeroPoint, y, x + zeroPoint, y + 20);
    }
    
    private void drawRoundInfo(Graphics2D g2d) {
        int centerX = getWidth() / 2;
        
        g2d.setFont(new Font("Arial", Font.BOLD, 28));
        g2d.setColor(Color.WHITE);
        
        String roundText = "Round " + currentState.getCurrentRound() + " / 20";
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(roundText);
        
        g2d.drawString(roundText, centerX - textWidth / 2, 40);
    }
    
    private void drawPhaseIndicator(Graphics2D g2d) {
        int centerX = getWidth() / 2;
        int y = getHeight() - 200;
        
        GameEngine.GamePhase phase = currentState.getPhase();
        
        String[] phases = {"CLAIM", "CHALLENGE", "RESOLUTION"};
        int phaseIndex = phase.ordinal();
        
        int totalWidth = 400;
        int phaseWidth = totalWidth / phases.length;
        int startX = centerX - totalWidth / 2;
        
        for (int i = 0; i < phases.length; i++) {
            int x = startX + (i * phaseWidth);
            
            if (i == phaseIndex) {
                // Active phase
                g2d.setColor(new Color(100, 200, 100));
                g2d.fillRoundRect(x, y, phaseWidth - 10, 40, 10, 10);
                g2d.setColor(Color.WHITE);
            } else if (i < phaseIndex) {
                // Completed phase
                g2d.setColor(new Color(70, 70, 70));
                g2d.fillRoundRect(x, y, phaseWidth - 10, 40, 10, 10);
                g2d.setColor(new Color(150, 150, 150));
            } else {
                // Future phase
                g2d.setColor(new Color(40, 40, 40));
                g2d.fillRoundRect(x, y, phaseWidth - 10, 40, 10, 10);
                g2d.setColor(new Color(100, 100, 100));
            }
            
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(phases[i]);
            g2d.drawString(phases[i], 
                x + (phaseWidth - textWidth) / 2 - 5, y + 25);
        }
    }
    
    private void drawTrustVisualization(Graphics2D g2d) {
        int centerX = getWidth() / 2;
        int y = getHeight() - 120;
        
        Player p1 = currentState.getPlayer1();
        Player p2 = currentState.getPlayer2();
        
        if (p1 == null || p2 == null) return;
        
        int barWidth = 300;
        int barHeight = 30;
        
        // Draw comparison bar
        g2d.setColor(new Color(50, 50, 50));
        g2d.fillRect(centerX - barWidth / 2, y, barWidth, barHeight);
        
        // Calculate relative positions
        int total = Math.abs(p1.getTrustScore()) + Math.abs(p2.getTrustScore());
        if (total == 0) total = 1;
        
        int p1Width = (int) ((Math.abs(p1.getTrustScore()) / (double) total) * barWidth);
        
        // Player 1 portion
        g2d.setColor(PLAYER1_COLOR);
        g2d.fillRect(centerX - barWidth / 2, y, p1Width, barHeight);
        
        // Player 2 portion
        g2d.setColor(PLAYER2_COLOR);
        g2d.fillRect(centerX - barWidth / 2 + p1Width, y, barWidth - p1Width, barHeight);
        
        // Border
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRect(centerX - barWidth / 2, y, barWidth, barHeight);
        
        // Labels
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        g2d.setColor(Color.WHITE);
        g2d.drawString(String.valueOf(p1.getTrustScore()), 
            centerX - barWidth / 2 - 30, y + 20);
        g2d.drawString(String.valueOf(p2.getTrustScore()),
            centerX + barWidth / 2 + 10, y + 20);
    }
    
    private void drawCurrentClaim(Graphics2D g2d) {
        Claim claim = currentState.getCurrentClaim();
        Player claimant = currentState.getClaimant();
        
        int centerX = getWidth() / 2;
        int y = getHeight() / 2 - 50;
        
        // Draw claim box
        int boxWidth = 500;
        int boxHeight = 150;
        
        g2d.setColor(new Color(40, 40, 50, 230));
        g2d.fillRoundRect(centerX - boxWidth / 2, y, boxWidth, boxHeight, 20, 20);
        
        g2d.setColor(new Color(100, 150, 200));
        g2d.setStroke(new BasicStroke(3));
        g2d.drawRoundRect(centerX - boxWidth / 2, y, boxWidth, boxHeight, 20, 20);
        
        // Draw claimant name
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 18));
        String header = claimant.getName() + " claims:";
        g2d.drawString(header, centerX - boxWidth / 2 + 20, y + 30);
        
        // Draw claim text (wrapped)
        g2d.setFont(new Font("Arial", Font.PLAIN, 16));
        drawWrappedText(g2d, claim.getDescription(), 
            centerX - boxWidth / 2 + 20, y + 60, boxWidth - 40);
        
        // Draw boldness indicator
        int boldnessY = y + boxHeight - 25;
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        g2d.setColor(new Color(200, 200, 200));
        g2d.drawString("Boldness: ", centerX - boxWidth / 2 + 20, boldnessY);
        
        drawBoldnessBar(g2d, centerX - boxWidth / 2 + 100, boldnessY - 12, 
                       150, claim.getBoldness());
    }
    
    private void drawBoldnessBar(Graphics2D g2d, int x, int y, 
                                int width, double boldness) {
        g2d.setColor(new Color(60, 60, 60));
        g2d.fillRect(x, y, width, 15);
        
        int fillWidth = (int) (width * boldness);
        
        Color boldnessColor;
        if (boldness < 0.3) {
            boldnessColor = new Color(100, 200, 100);
        } else if (boldness < 0.7) {
            boldnessColor = new Color(200, 200, 100);
        } else {
            boldnessColor = new Color(200, 100, 100);
        }
        
        g2d.setColor(boldnessColor);
        g2d.fillRect(x, y, fillWidth, 15);
        
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(1));
        g2d.drawRect(x, y, width, 15);
}

    private void drawWrappedText(Graphics2D g2d, String text, 
                                int x, int y, int maxWidth) {
        FontMetrics fm = g2d.getFontMetrics();
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        int currentY = y;
    
        for (String word : words) {
        String testLine = line + word + " ";
        int testWidth = fm.stringWidth(testLine);
        
        if (testWidth > maxWidth && line.length() > 0) {
            g2d.drawString(line.toString().trim(), x, currentY);
            line = new StringBuilder(word + " ");
            currentY += fm.getHeight();
        } else {
            line.append(word).append(" ");
        }
        }
    
        if (line.length() > 0) {
        g2d.drawString(line.toString().trim(), x, currentY);
        }
   }
}