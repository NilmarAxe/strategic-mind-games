package com.mindgames.core;

public class RoundResult {
    private boolean success;
    private String message;
    private int trustChange;
    private boolean roundComplete;
    private int player1Score;
    private int player2Score;
    
    public RoundResult() {
        this.success = false;
        this.roundComplete = false;
    }
    
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public int getTrustChange() { return trustChange; }
    public void setTrustChange(int change) { this.trustChange = change; }
    
    public boolean isRoundComplete() { return roundComplete; }
    public void setRoundComplete(boolean complete) { this.roundComplete = complete; }
    
    public int getPlayer1Score() { return player1Score; }
    public void setPlayer1Score(int score) { this.player1Score = score; }
    
    public int getPlayer2Score() { return player2Score; }
    public void setPlayer2Score(int score) { this.player2Score = score; }
}