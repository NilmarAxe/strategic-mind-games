package com.mindgames.core;

import java.util.*;
import java.util.concurrent.*;

public class GameEngine {
    private GameState currentState;
    private final Queue<Move> moveQueue;
    private final ExecutorService executor;
    private final List<GameObserver> observers;
    
    private static final int MAX_ROUNDS = 20;
    private static final int VICTORY_THRESHOLD = 100;
    private static final int DEFEAT_THRESHOLD = -50;
    
    public GameEngine() {
        this.moveQueue = new ConcurrentLinkedQueue<>();
        this.executor = Executors.newFixedThreadPool(4);
        this.observers = new CopyOnWriteArrayList<>();
        this.currentState = new GameState();
    }
    
    public void startNewGame(Player player1, Player player2) {
        currentState = new GameState();
        currentState.setPlayer1(player1);
        currentState.setPlayer2(player2);
        currentState.setCurrentRound(1);
        currentState.setPhase(GamePhase.CLAIM);
        currentState.setCurrentPlayer(player1); // CORREÇÃO: Definir jogador inicial
        
        notifyObservers(GameEvent.GAME_STARTED);
    }
    
    public CompletableFuture<RoundResult> processMove(Move move) {
        return CompletableFuture.supplyAsync(() -> {
            validateMove(move);
            moveQueue.offer(move);
            
            return executeRound(move);
        }, executor);
    }
    
    private RoundResult executeRound(Move move) {
        RoundResult result = new RoundResult();
        
        switch (currentState.getPhase()) {
            case CLAIM:
                result = processClaim(move);
                currentState.setPhase(GamePhase.CHALLENGE);
                break;
                
            case CHALLENGE:
                result = processChallenge(move);
                currentState.setPhase(GamePhase.RESOLUTION);
                break;
                
            case RESOLUTION:
                result = resolveRound();
                advanceToNextRound();
                break;
        }
        
        checkVictoryConditions();
        notifyObservers(GameEvent.MOVE_PROCESSED);
        
        return result;
    }
    
    private RoundResult processClaim(Move move) {
        RoundResult result = new RoundResult();
        
        currentState.setCurrentClaim(move.getClaim());
        currentState.setClaimant(move.getPlayer());
        
        result.setSuccess(true);
        result.setMessage("Claim registered: " + move.getClaim().getDescription());
        result.setTrustChange(0);
        
        return result;
    }
    
    private RoundResult processChallenge(Move move) {
        RoundResult result = new RoundResult();
        
        if (move.isChallenge()) {
            boolean claimWasTrue = evaluateClaimTruth();
            
            if (claimWasTrue) {
                result.setTrustChange(-15);
                result.setMessage("Challenge failed! Claim was true.");
                updatePlayerTrust(move.getPlayer(), -15);
                updatePlayerTrust(currentState.getClaimant(), 10);
                currentState.getClaimant().recordSuccessfulBluff(); // ADICIONAR
            } else {
                result.setTrustChange(15);
                result.setMessage("Challenge succeeded! Claim was false.");
                updatePlayerTrust(move.getPlayer(), 15);
                updatePlayerTrust(currentState.getClaimant(), -20);
                move.getPlayer().recordSuccessfulChallenge(); // ADICIONAR
                currentState.getClaimant().recordFailedBluff(); // ADICIONAR
            }
        } else {
            result.setTrustChange(5);
            result.setMessage("Claim accepted.");
            updatePlayerTrust(currentState.getClaimant(), 5);
        }
        
        result.setSuccess(true);
        return result;
    }
    
    private RoundResult resolveRound() {
        RoundResult result = new RoundResult();
        result.setRoundComplete(true);
        
        // Calculate round bonuses
        Player player1 = currentState.getPlayer1();
        Player player2 = currentState.getPlayer2();
        
        int p1Trust = player1.getTrustScore();
        int p2Trust = player2.getTrustScore();
        
        result.setPlayer1Score(p1Trust);
        result.setPlayer2Score(p2Trust);
        result.setMessage("Round " + currentState.getCurrentRound() + " complete.");
        
        return result;
    }
    
    private void advanceToNextRound() {
        int nextRound = currentState.getCurrentRound() + 1;
        
        if (nextRound > MAX_ROUNDS) {
            endGame("Maximum rounds reached");
            return;
        }
        
        currentState.setCurrentRound(nextRound);
        currentState.setPhase(GamePhase.CLAIM);
        currentState.setCurrentClaim(null);
        
        // CORREÇÃO: Alternar jogador ativo
        Player current = currentState.getCurrentPlayer();
        if (current != null) {
            Player next = current.equals(currentState.getPlayer1()) 
                ? currentState.getPlayer2() 
                : currentState.getPlayer1();
            currentState.setCurrentPlayer(next);
        } else {
            // Fallback se currentPlayer for null
            currentState.setCurrentPlayer(currentState.getPlayer1());
        }
    }
    
    private boolean evaluateClaimTruth() {
        Claim claim = currentState.getCurrentClaim();
        
        // This uses probability and hidden information
        // Simplified: 60% of claims are true, 40% are bluffs
        double truthProbability = calculateTruthProbability(claim);
        return Math.random() < truthProbability;
    }
    
    private double calculateTruthProbability(Claim claim) {
        Player claimant = currentState.getClaimant();
        double baseProbability = 0.6;
        
        // Adjust based on player's reputation
        int trustScore = claimant.getTrustScore();
        double reputationModifier = (trustScore / 100.0) * 0.2;
        
        // Adjust based on claim boldness
        double boldnessModifier = claim.getBoldness() * -0.15;
        
        return Math.max(0.1, Math.min(0.9, 
            baseProbability + reputationModifier + boldnessModifier));
    }
    
    private void updatePlayerTrust(Player player, int change) {
        player.modifyTrust(change);
        player.recordMove(currentState.getCurrentRound(), change);
    }
    
    private void checkVictoryConditions() {
        Player p1 = currentState.getPlayer1();
        Player p2 = currentState.getPlayer2();
        
        if (p1.getTrustScore() >= VICTORY_THRESHOLD) {
            endGame(p1.getName() + " achieved victory through trust!");
        } else if (p2.getTrustScore() >= VICTORY_THRESHOLD) {
            endGame(p2.getName() + " achieved victory through trust!");
        } else if (p1.getTrustScore() <= DEFEAT_THRESHOLD) {
            endGame(p2.getName() + " wins! " + p1.getName() + " lost all credibility.");
        } else if (p2.getTrustScore() <= DEFEAT_THRESHOLD) {
            endGame(p1.getName() + " wins! " + p2.getName() + " lost all credibility.");
        }
    }
    
    private void endGame(String reason) {
        currentState.setGameOver(true);
        currentState.setVictoryMessage(reason);
        notifyObservers(GameEvent.GAME_ENDED);
    }
    
    private void validateMove(Move move) {
        if (move == null) {
            throw new IllegalArgumentException("Move cannot be null");
        }
        
        if (!move.getPlayer().equals(currentState.getCurrentPlayer())) {
            throw new IllegalStateException("Not this player's turn");
        }
    }
    
    public void addObserver(GameObserver observer) {
        observers.add(observer);
    }
    
    private void notifyObservers(GameEvent event) {
        observers.forEach(obs -> obs.onGameEvent(event, currentState));
    }
    
    public GameState getCurrentState() {
        return currentState.clone();
    }
    
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }
    
    public enum GamePhase {
        CLAIM, CHALLENGE, RESOLUTION
    }
    
    public enum GameEvent {
        GAME_STARTED, MOVE_PROCESSED, ROUND_COMPLETE, GAME_ENDED
    }
    
    public interface GameObserver {
        void onGameEvent(GameEvent event, GameState state);
    }
}