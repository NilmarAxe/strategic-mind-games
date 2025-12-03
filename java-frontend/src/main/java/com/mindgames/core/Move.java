package com.mindgames.core;

public class Move {
    private final Player player;
    private final MoveType type;
    private final Claim claim;
    private final boolean isChallenge;
    private final double confidence;
    
    public enum MoveType {
        CLAIM, CHALLENGE, ACCEPT
    }
    
    private Move(Builder builder) {
        this.player = builder.player;
        this.type = builder.type;
        this.claim = builder.claim;
        this.isChallenge = builder.isChallenge;
        this.confidence = builder.confidence;
    }
    
    public Player getPlayer() { return player; }
    public MoveType getType() { return type; }
    public Claim getClaim() { return claim; }
    public boolean isChallenge() { return isChallenge; }
    public double getConfidence() { return confidence; }
    
    public static class Builder {
        private Player player;
        private MoveType type;
        private Claim claim;
        private boolean isChallenge;
        private double confidence = 0.5;
        
        public Builder player(Player player) {
            this.player = player;
            return this;
        }
        
        public Builder type(MoveType type) {
            this.type = type;
            return this;
        }
        
        public Builder claim(Claim claim) {
            this.claim = claim;
            return this;
        }
        
        public Builder challenge(boolean isChallenge) {
            this.isChallenge = isChallenge;
            return this;
        }
        
        public Builder confidence(double confidence) {
            this.confidence = confidence;
            return this;
        }
        
        public Move build() {
            return new Move(this);
        }
    }
}