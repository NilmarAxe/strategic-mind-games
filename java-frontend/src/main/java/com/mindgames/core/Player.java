package com.mindgames.core;

import java.util.*;

public class Player {
    private final String id;
    private final String name;
    private final PlayerType type;
    private int trustScore;
    private int successfulBluffs;
    private int failedBluffs;
    private int successfulChallenges;
    private int failedChallenges;
    private final List<MoveRecord> moveHistory;
    
    public enum PlayerType {
        HUMAN, AI_EASY, AI_MEDIUM, AI_HARD, AI_RUTHLESS
    }
    
    public Player(String name, PlayerType type) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.type = type;
        this.trustScore = 50; // Start neutral
        this.moveHistory = new ArrayList<>();
    }
    
    public String getId() { return id; }
    public String getName() { return name; }
    public PlayerType getType() { return type; }
    
    public int getTrustScore() { return trustScore; }
    
    public void modifyTrust(int change) {
        this.trustScore += change;
    }
    
    public void recordMove(int round, int trustChange) {
        moveHistory.add(new MoveRecord(round, trustChange));
    }
    
    public void recordSuccessfulBluff() {
        successfulBluffs++;
    }
    
    public void recordFailedBluff() {
        failedBluffs++;
    }
    
    public void recordSuccessfulChallenge() {
        successfulChallenges++;
    }
    
    public void recordFailedChallenge() {
        failedChallenges++;
    }
    
    public double getBluffSuccessRate() {
        int total = successfulBluffs + failedBluffs;
        return total == 0 ? 0.0 : (double) successfulBluffs / total;
    }
    
    public double getChallengeSuccessRate() {
        int total = successfulChallenges + failedChallenges;
        return total == 0 ? 0.0 : (double) successfulChallenges / total;
    }
    
    public List<MoveRecord> getMoveHistory() {
        return Collections.unmodifiableList(moveHistory);
    }
    
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("trust_score", trustScore);
        stats.put("successful_bluffs", successfulBluffs);
        stats.put("failed_bluffs", failedBluffs);
        stats.put("bluff_rate", getBluffSuccessRate());
        stats.put("challenge_rate", getChallengeSuccessRate());
        return stats;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Player player = (Player) o;
        return Objects.equals(id, player.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    public static class MoveRecord {
        private final int round;
        private final int trustChange;
        private final long timestamp;
        
        public MoveRecord(int round, int trustChange) {
            this.round = round;
            this.trustChange = trustChange;
            this.timestamp = System.currentTimeMillis();
        }
        
        public int getRound() { return round; }
        public int getTrustChange() { return trustChange; }
        public long getTimestamp() { return timestamp; }
    }
}