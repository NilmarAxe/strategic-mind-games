package com.mindgames.core;

import java.io.Serializable;
import java.util.*;

public class GameState implements Cloneable, Serializable {
    private static final long serialVersionUID = 1L;
    
    private Player player1;
    private Player player2;
    private int currentRound;
    private GameEngine.GamePhase phase;
    private Claim currentClaim;
    private Player claimant;
    private Player currentPlayer;
    private boolean gameOver;
    private String victoryMessage;
    private List<HistoricalMove> moveHistory;
    
    public GameState() {
        this.currentRound = 0;
        this.phase = GameEngine.GamePhase.CLAIM;
        this.gameOver = false;
        this.moveHistory = new ArrayList<>();
    }
    
    // Getters and setters
    public Player getPlayer1() { return player1; }
    public void setPlayer1(Player p) { this.player1 = p; }
    
    public Player getPlayer2() { return player2; }
    public void setPlayer2(Player p) { this.player2 = p; }
    
    public int getCurrentRound() { return currentRound; }
    public void setCurrentRound(int round) { this.currentRound = round; }
    
    public GameEngine.GamePhase getPhase() { return phase; }
    public void setPhase(GameEngine.GamePhase phase) { this.phase = phase; }
    
    public Claim getCurrentClaim() { return currentClaim; }
    public void setCurrentClaim(Claim claim) { this.currentClaim = claim; }
    
    public Player getClaimant() { return claimant; }
    public void setClaimant(Player claimant) { 
        this.claimant = claimant;
    }
    
    public Player getCurrentPlayer() { return currentPlayer; }
    public void setCurrentPlayer(Player player) { this.currentPlayer = player; }
    
    public boolean isGameOver() { return gameOver; }
    public void setGameOver(boolean gameOver) { this.gameOver = gameOver; }
    
    public String getVictoryMessage() { return victoryMessage; }
    public void setVictoryMessage(String msg) { this.victoryMessage = msg; }
    
    public List<HistoricalMove> getMoveHistory() { 
        return Collections.unmodifiableList(moveHistory); 
    }
    
    public void addHistoricalMove(HistoricalMove move) {
        moveHistory.add(move);
    }
    
    @Override
    public GameState clone() {
        try {
            GameState cloned = (GameState) super.clone();
            cloned.moveHistory = new ArrayList<>(this.moveHistory);
            return cloned;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError("Clone not supported", e);
        }
    }
    
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("round", currentRound);
        map.put("phase", phase.name());
        map.put("player1_trust", player1 != null ? player1.getTrustScore() : 0);
        map.put("player2_trust", player2 != null ? player2.getTrustScore() : 0);
        map.put("game_over", gameOver);
        return map;
    }
    
    public static class HistoricalMove {
        private final int round;
        private final String playerName;
        private final String action;
        private final int trustChange;
        private final long timestamp;
        
        public HistoricalMove(int round, String playerName, String action, int trustChange) {
            this.round = round;
            this.playerName = playerName;
            this.action = action;
            this.trustChange = trustChange;
            this.timestamp = System.currentTimeMillis();
        }
        
        public int getRound() { return round; }
        public String getPlayerName() { return playerName; }
        public String getAction() { return action; }
        public int getTrustChange() { return trustChange; }
        public long getTimestamp() { return timestamp; }
    }
}