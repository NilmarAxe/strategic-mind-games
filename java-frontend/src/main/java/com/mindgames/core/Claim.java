package com.mindgames.core;

public class Claim {
    private final String description;
    private final ClaimType type;
    private final double boldness; // 0.0 to 1.0
    private final int potentialGain;
    private final int potentialLoss;
    
    public enum ClaimType {
        INFORMATION, PREDICTION, ACCUSATION, ALLIANCE
    }
    
    public Claim(String description, ClaimType type, double boldness) {
        this.description = description;
        this.type = type;
        this.boldness = Math.max(0.0, Math.min(1.0, boldness));
        this.potentialGain = calculatePotentialGain();
        this.potentialLoss = calculatePotentialLoss();
    }
    
    private int calculatePotentialGain() {
        return (int) (10 + (boldness * 30));
    }
    
    private int calculatePotentialLoss() {
        return (int) (15 + (boldness * 35));
    }
    
    public String getDescription() { return description; }
    public ClaimType getType() { return type; }
    public double getBoldness() { return boldness; }
    public int getPotentialGain() { return potentialGain; }
    public int getPotentialLoss() { return potentialLoss; }
}