package com.mindgames.ui;

import com.mindgames.core.*;
import com.mindgames.integration.AIBridge;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class GameWindow extends JFrame implements GameEngine.GameObserver {
    private static final int WINDOW_WIDTH = 1400;
    private static final int WINDOW_HEIGHT = 900;
    
    private final GameEngine engine;
    private BoardPanel boardPanel;
    private ControlPanel controlPanel;
    private StatisticsPanel statisticsPanel;
    private LogPanel logPanel;
    private MenuBar menuBar;
    
    private Player humanPlayer;
    private Player aiPlayer;
    
    public GameWindow() {
        this.engine = new GameEngine();
        this.engine.addObserver(this);
        
        initializeWindow();
        initializeComponents();
        layoutComponents();
        bindEvents();
        
        showMainMenu();
    }
    
    private void initializeWindow() {
        setTitle("Strategic Mind Games - Psychological Warfare");
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        
        try {
            setIconImage(createGameIcon());
        } catch (Exception e) {
            // Icon loading failed, continue without it
        }
    }
    
    private Image createGameIcon() {
        // Create a simple icon programmatically
        int size = 32;
        var image = new java.awt.image.BufferedImage(size, size, 
            java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
            RenderingHints.VALUE_ANTIALIAS_ON);
        
        g.setColor(new Color(45, 45, 65));
        g.fillOval(2, 2, size - 4, size - 4);
        g.setColor(new Color(100, 150, 200));
        g.fillOval(8, 8, size - 16, size - 16);
        
        g.dispose();
        return image;
    }
    
    private void initializeComponents() {
        menuBar = new MenuBar(this);
        setJMenuBar(menuBar);
        
        boardPanel = new BoardPanel(engine);
        controlPanel = new ControlPanel(this, engine);
        statisticsPanel = new StatisticsPanel();
        logPanel = new LogPanel();
    }
    
    private void layoutComponents() {
        setLayout(new BorderLayout(10, 10));
        
        // Main game area (center)
        add(boardPanel, BorderLayout.CENTER);
        
        // Control panel (bottom)
        add(controlPanel, BorderLayout.SOUTH);
        
        // Statistics and log (right side)
        JPanel rightPanel = new JPanel(new BorderLayout(5, 5));
        rightPanel.setPreferredSize(new Dimension(350, WINDOW_HEIGHT));
        rightPanel.add(statisticsPanel, BorderLayout.NORTH);
        rightPanel.add(new JScrollPane(logPanel), BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);
    }
    
    private void bindEvents() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleExit();
            }
        });
    }
    
    private void showMainMenu() {
        String[] options = {"Single Player vs AI", "Local Multiplayer", 
                           "Online Multiplayer", "Exit"};
        
        int choice = JOptionPane.showOptionDialog(
            this,
            "Select Game Mode",
            "Strategic Mind Games",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]
        );
        
        switch (choice) {
            case 0:
                startSinglePlayerGame();
                break;
            case 1:
                startLocalMultiplayerGame();
                break;
            case 2:
                showOnlineMultiplayerDialog();
                break;
            case 3:
            default:
                handleExit();
                break;
        }
    }
    
    public void startSinglePlayerGame() {
        String playerName = JOptionPane.showInputDialog(
            this,
            "Enter your name:",
            "Player Setup",
            JOptionPane.QUESTION_MESSAGE
        );
        
        if (playerName == null || playerName.trim().isEmpty()) {
            playerName = "Player";
        }
        
        String[] difficulties = {"Easy", "Medium", "Hard", "Ruthless"};
        int difficulty = JOptionPane.showOptionDialog(
            this,
            "Select AI Difficulty",
            "AI Configuration",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            difficulties,
            difficulties[1]
        );
        
        Player.PlayerType aiType = switch (difficulty) {
            case 0 -> Player.PlayerType.AI_EASY;
            case 2 -> Player.PlayerType.AI_HARD;
            case 3 -> Player.PlayerType.AI_RUTHLESS;
            default -> Player.PlayerType.AI_MEDIUM;
        };
        
        humanPlayer = new Player(playerName, Player.PlayerType.HUMAN);
        aiPlayer = new Player("AI Opponent", aiType);
        
        engine.startNewGame(humanPlayer, aiPlayer);
        
        logPanel.addMessage("Game started: " + humanPlayer.getName() + 
                           " vs " + aiPlayer.getName());
        logPanel.addMessage("AI Difficulty: " + difficulties[Math.max(0, difficulty)]);
        
        controlPanel.enableControls(true);
        updateDisplay();
    }
    
    private void startLocalMultiplayerGame() {
        String player1Name = JOptionPane.showInputDialog(
            this,
            "Enter Player 1 name:",
            "Player Setup",
            JOptionPane.QUESTION_MESSAGE
        );
        
        String player2Name = JOptionPane.showInputDialog(
            this,
            "Enter Player 2 name:",
            "Player Setup",
            JOptionPane.QUESTION_MESSAGE
        );
        
        if (player1Name == null || player1Name.trim().isEmpty()) {
            player1Name = "Player 1";
        }
        if (player2Name == null || player2Name.trim().isEmpty()) {
            player2Name = "Player 2";
        }
        
        humanPlayer = new Player(player1Name, Player.PlayerType.HUMAN);
        Player player2 = new Player(player2Name, Player.PlayerType.HUMAN);
        
        engine.startNewGame(humanPlayer, player2);
        
        logPanel.addMessage("Local multiplayer started");
        logPanel.addMessage(player1Name + " vs " + player2Name);
        
        controlPanel.enableControls(true);
        updateDisplay();
    }
    
    private void showOnlineMultiplayerDialog() {
        JOptionPane.showMessageDialog(
            this,
            "Online multiplayer coming soon!\nFeatures:\n" +
            "- Ranked matchmaking\n" +
            "- Friend challenges\n" +
            "- Tournament mode",
            "Online Multiplayer",
            JOptionPane.INFORMATION_MESSAGE
        );
        showMainMenu();
    }
    
    public void processPlayerMove(Move move) {
        engine.processMove(move).thenAccept(result -> {
            SwingUtilities.invokeLater(() -> {
                handleMoveResult(result);
                
                GameState state = engine.getCurrentState();
                if (!state.isGameOver() && shouldAIMove(state)) {
                    processAIMove();
                }
            });
        }).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> {
                logPanel.addError("Move processing failed: " + ex.getMessage());
            });
            return null;
        });
    }
    
    private void processAIMove() {
        controlPanel.enableControls(false);
        logPanel.addMessage("AI is thinking...");
        
        new SwingWorker<Move, Void>() {
            @Override
            protected Move doInBackground() throws Exception {
                GameState state = engine.getCurrentState();
                return AIBridge.requestMove(state, aiPlayer);
            }
            
            @Override
            protected void done() {
                try {
                    Move aiMove = get();
                    logPanel.addMessage("AI made a move");
                    
                    engine.processMove(aiMove).thenAccept(result -> {
                        SwingUtilities.invokeLater(() -> {
                            handleMoveResult(result);
                            controlPanel.enableControls(true);
                        });
                    });
                } catch (Exception e) {
                    logPanel.addError("AI move failed: " + e.getMessage());
                    controlPanel.enableControls(true);
                }
            }
        }.execute();
    }
    
    private boolean shouldAIMove(GameState state) {
        if (state.isGameOver()) return false;
        
        Player currentPlayer = state.getCurrentPlayer();
        return currentPlayer != null && 
               currentPlayer.getType() != Player.PlayerType.HUMAN;
    }
    
    private void handleMoveResult(RoundResult result) {
        if (result.isSuccess()) {
            logPanel.addMessage(result.getMessage());
            
            if (result.getTrustChange() != 0) {
                String change = result.getTrustChange() > 0 ? "+" : "";
                logPanel.addMessage("Trust change: " + change + result.getTrustChange());
            }
        } else {
            logPanel.addError("Move failed: " + result.getMessage());
        }
        
        updateDisplay();
        
        if (result.isRoundComplete()) {
            logPanel.addMessage("--- Round Complete ---");
            statisticsPanel.updateStatistics(
                engine.getCurrentState().getPlayer1(),
                engine.getCurrentState().getPlayer2()
            );
        }
        
        GameState state = engine.getCurrentState();
        if (state.isGameOver()) {
            handleGameEnd(state);
        }
    }
    
    private void handleGameEnd(GameState state) {
        controlPanel.enableControls(false);
        
        String message = state.getVictoryMessage() + "\n\n" +
                        "Final Scores:\n" +
                        state.getPlayer1().getName() + ": " + 
                        state.getPlayer1().getTrustScore() + "\n" +
                        state.getPlayer2().getName() + ": " + 
                        state.getPlayer2().getTrustScore();
        
        int choice = JOptionPane.showConfirmDialog(
            this,
            message + "\n\nPlay again?",
            "Game Over",
            JOptionPane.YES_NO_OPTION
        );
        
        if (choice == JOptionPane.YES_OPTION) {
            resetGame();
            showMainMenu();
        } else {
            handleExit();
        }
    }
    
    private void resetGame() {
        boardPanel.reset();
        controlPanel.reset();
        statisticsPanel.reset();
        logPanel.clear();
    }
    
    private void updateDisplay() {
        GameState state = engine.getCurrentState();
        boardPanel.updateState(state);
        statisticsPanel.updateStatistics(state.getPlayer1(), state.getPlayer2());
        controlPanel.updateForState(state);
    }
    
    @Override
    public void onGameEvent(GameEngine.GameEvent event, GameState state) {
        SwingUtilities.invokeLater(() -> {
            switch (event) {
                case GAME_STARTED:
                    logPanel.addMessage("=== Game Started ===");
                    break;
                case MOVE_PROCESSED:
                    updateDisplay();
                    break;
                case GAME_ENDED:
                    logPanel.addMessage("=== Game Ended ===");
                    break;
            }
        });
    }
    
    private void handleExit() {
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to exit?",
            "Confirm Exit",
            JOptionPane.YES_NO_OPTION
        );
        
        if (confirm == JOptionPane.YES_OPTION) {
            engine.shutdown();
            dispose();
            System.exit(0);
        }
    }
    
    // Menu bar implementation
    private static class MenuBar extends JMenuBar {
        public MenuBar(GameWindow window) {
            JMenu gameMenu = new JMenu("Game");
            JMenuItem newGame = new JMenuItem("New Game");
            JMenuItem exit = new JMenuItem("Exit");
            
            newGame.addActionListener(e -> {
                window.resetGame();
                window.showMainMenu();
            });
            
            exit.addActionListener(e -> window.handleExit());
            
            gameMenu.add(newGame);
            gameMenu.addSeparator();
            gameMenu.add(exit);
            
            JMenu helpMenu = new JMenu("Help");
            JMenuItem rules = new JMenuItem("Rules");
            JMenuItem about = new JMenuItem("About");
            
            rules.addActionListener(e -> showRules(window));
            about.addActionListener(e -> showAbout(window));
            
            helpMenu.add(rules);
            helpMenu.add(about);
            
            add(gameMenu);
            add(helpMenu);
        }
        
        private void showRules(JFrame parent) {
            String rulesText = """
                STRATEGIC MIND GAMES - RULES
                
                OBJECTIVE:
                Reach 100 trust points or force opponent below -50 points.
                
                GAMEPLAY:
                1. CLAIM PHASE: Make a claim about hidden information
                2. CHALLENGE PHASE: Opponent challenges or accepts
                3. RESOLUTION: Truth revealed, points awarded
                
                SCORING:
                - Successful bluff: +10 to +40 points
                - Failed bluff: -20 to -55 points
                - Successful challenge: +15 points
                - Failed challenge: -15 points
                - Accepted claim: +5 points
                
                STRATEGY:
                Build reputation to make bold claims more believable.
                Challenge suspicious claims to protect your position.
                Balance aggression with caution.
                """;
            
            JTextArea textArea = new JTextArea(rulesText);
            textArea.setEditable(false);
            textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
            
            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(500, 400));
            
            JOptionPane.showMessageDialog(parent, scrollPane, "Game Rules",
                JOptionPane.INFORMATION_MESSAGE);
        }
        
        private void showAbout(JFrame parent) {
            String aboutText = """
                Strategic Mind Games v1.0
                
                A psychological warfare board game inspired by
                strategic deception and mind games.
                
                Technologies:
                - Java (UI & Game Logic)
                - Python (AI Engine)
                - Rust (Performance Optimization)
                
                Features:
                - Single-player vs AI
                - Local multiplayer
                - Multiple AI difficulty levels
                - Statistical analysis
                """;
            
            JOptionPane.showMessageDialog(parent, aboutText, "About",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
}