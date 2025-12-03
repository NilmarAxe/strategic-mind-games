package com.mindgames.integration;

import com.mindgames.core.GameState;
import com.mindgames.core.Player;
import com.mindgames.core.GameEngine;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.File;
import java.util.*;

public class RustBridge {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static boolean initialized = false;
    private static boolean nativeLibraryLoaded = false;
    private static String libraryPath = null;
    
    // Native method declarations
    private native static String nativeSearchOptimalMove(String gameStateJson, int maxDepth, int playerId);
    private native static double nativeEvaluateState(String gameStateJson, int playerId);
    private native static int nativeInitialize();
    private native static String nativeGetVersion();
    
    static {
        try {
            loadNativeLibrary();
            nativeLibraryLoaded = true;
        } catch (Exception e) {
            System.err.println("[RustBridge] Failed to load native library: " + e.getMessage());
            System.err.println("[RustBridge] Rust optimization will not be available");
            nativeLibraryLoaded = false;
        }
    }
    
    public static void initialize() {
        if (initialized) {
            System.out.println("[RustBridge] Already initialized");
            return;
        }
        
        System.out.println("[RustBridge] Initializing Rust Optimizer...");
        
        if (!nativeLibraryLoaded) {
            System.err.println("[RustBridge] Native library not loaded, initialization skipped");
            return;
        }
        
        try {
            // Test native library
            int result = nativeInitialize();
            if (result != 0) {
                throw new RuntimeException("Native initialization failed with code: " + result);
            }
            
            // Get version
            String version = nativeGetVersion();
            System.out.println("[RustBridge] Native library version: " + version);
            
            // Test evaluation
            String testJson = createTestStateJson();
            double eval = nativeEvaluateState(testJson, 1);
            System.out.println("[RustBridge] Test evaluation: " + eval);
            
            initialized = true;
            System.out.println("[RustBridge] Initialization successful");
            
        } catch (Exception e) {
            System.err.println("[RustBridge] Initialization failed: " + e.getMessage());
            e.printStackTrace();
            initialized = false;
        }
    }
    
    private static void loadNativeLibrary() {
        String osName = System.getProperty("os.name").toLowerCase();
        String osArch = System.getProperty("os.arch").toLowerCase();
        
        System.out.println("[RustBridge] OS: " + osName + ", Arch: " + osArch);
        
        String libName = determineLibraryName(osName);
        libraryPath = findLibraryPath(libName);
        
        if (libraryPath == null) {
            throw new RuntimeException("Native library not found: " + libName);
        }
        
        System.out.println("[RustBridge] Loading library: " + libraryPath);
        System.load(libraryPath);
        System.out.println("[RustBridge] Native library loaded successfully");
    }
    
    private static String determineLibraryName(String osName) {
        if (osName.contains("win")) {
            return "strategic_mind_optimizer.dll";
        } else if (osName.contains("mac")) {
            return "libstrategic_mind_optimizer.dylib";
        } else {
            return "libstrategic_mind_optimizer.so";
        }
    }
    
    private static String findLibraryPath(String libName) {
        // Search paths in order of preference
        String[] searchPaths = {
            // Relative to java-frontend
            "../rust-optimizer/target/release/" + libName,
            // Relative to project root
            "rust-optimizer/target/release/" + libName,
            // In lib directory
            "./lib/" + libName,
            "./lib/native/" + libName,
            // Current directory
            "./" + libName,
            // System library path
            System.getProperty("java.library.path") + "/" + libName
        };
        
        for (String path : searchPaths) {
            File file = new File(path);
            if (file.exists() && file.isFile()) {
                System.out.println("[RustBridge] Found library at: " + file.getAbsolutePath());
                return file.getAbsolutePath();
            }
        }
        
        // Print searched paths for debugging
        System.err.println("[RustBridge] Library not found. Searched paths:");
        for (String path : searchPaths) {
            System.err.println("  - " + new File(path).getAbsolutePath());
        }
        
        return null;
    }
    
    public static boolean isAvailable() {
        return nativeLibraryLoaded && initialized;
    }
    
    public static SearchResult searchBestMove(GameState state, Player player, int depth) {
        if (!isAvailable()) {
            System.err.println("[RustBridge] Not available, using fallback heuristic");
            return fallbackSearch(state, player, depth);
        }
        
        try {
            String stateJson = convertStateToJson(state);
            int playerId = player.equals(state.getPlayer1()) ? 1 : 2;
            
            String resultJson = nativeSearchOptimalMove(stateJson, depth, playerId);
            
            if (resultJson == null || resultJson.isEmpty()) {
                System.err.println("[RustBridge] Native search returned null, using fallback");
                return fallbackSearch(state, player, depth);
            }
            
            return parseSearchResult(resultJson);
            
        } catch (Exception e) {
            System.err.println("[RustBridge] Search failed: " + e.getMessage());
            return fallbackSearch(state, player, depth);
        }
    }
    
    public static double evaluatePosition(GameState state, Player player) {
        if (!isAvailable()) {
            return evaluatePositionFallback(state, player);
        }
        
        try {
            String stateJson = convertStateToJson(state);
            int playerId = player.equals(state.getPlayer1()) ? 1 : 2;
            
            return nativeEvaluateState(stateJson, playerId);
        } catch (Exception e) {
            System.err.println("[RustBridge] Evaluation failed: " + e.getMessage());
            return evaluatePositionFallback(state, player);
        }
    }
    
    private static String convertStateToJson(GameState state) throws Exception {
        Map<String, Object> stateMap = new HashMap<>();
        
        stateMap.put("round", state.getCurrentRound());
        stateMap.put("phase", convertPhase(state.getPhase()));
        stateMap.put("player1_trust", state.getPlayer1().getTrustScore());
        stateMap.put("player2_trust", state.getPlayer2().getTrustScore());
        stateMap.put("current_claim", null);
        
        // Convert move history
        List<Map<String, Object>> historyList = new ArrayList<>();
        for (GameState.HistoricalMove move : state.getMoveHistory()) {
            Map<String, Object> moveMap = new HashMap<>();
            moveMap.put("round", move.getRound());
            moveMap.put("player", move.getPlayerName());
            moveMap.put("action", move.getAction());
            moveMap.put("trust_change", move.getTrustChange());
            historyList.add(moveMap);
        }
        stateMap.put("move_history", historyList);
        
        return mapper.writeValueAsString(stateMap);
    }
    
    private static String createTestStateJson() throws Exception {
        Map<String, Object> stateMap = new HashMap<>();
        stateMap.put("round", 1);
        stateMap.put("phase", "Claim");
        stateMap.put("player1_trust", 50);
        stateMap.put("player2_trust", 50);
        stateMap.put("current_claim", null);
        stateMap.put("move_history", new ArrayList<>());
        
        return mapper.writeValueAsString(stateMap);
    }
    
    private static String convertPhase(GameEngine.GamePhase phase) {
        return switch (phase) {
            case CLAIM -> "Claim";
            case CHALLENGE -> "Challenge";
            case RESOLUTION -> "Resolution";
        };
    }
    
    private static SearchResult parseSearchResult(String json) throws Exception {
        JsonNode root = mapper.readTree(json);
        
        // Extract best_move if present
        JsonNode bestMoveNode = root.get("best_move");
        MoveInfo moveInfo = null;
        
        if (bestMoveNode != null && !bestMoveNode.isNull()) {
            moveInfo = new MoveInfo(
                bestMoveNode.get("action").asText(),
                bestMoveNode.get("confidence").asDouble()
            );
        }
        
        return new SearchResult(
            root.get("evaluation").asDouble(),
            root.get("nodes_explored").asLong(),
            root.get("depth_reached").asInt(),
            root.get("time_ms").asLong(),
            moveInfo
        );
    }
    
    public static void shutdown() {
        if (initialized) {
            System.out.println("[RustBridge] Shutdown complete");
            initialized = false;
        }
    }
    
    // Result classes
    public static class SearchResult {
        public final double evaluation;
        public final long nodesExplored;
        public final int depthReached;
        public final long timeMs;
        public final MoveInfo bestMove;
        
        public SearchResult(double evaluation, long nodesExplored, 
                          int depthReached, long timeMs, MoveInfo bestMove) {
            this.evaluation = evaluation;
            this.nodesExplored = nodesExplored;
            this.depthReached = depthReached;
            this.timeMs = timeMs;
            this.bestMove = bestMove;
        }
        
        @Override
        public String toString() {
            return String.format(
                "SearchResult[eval=%.2f, nodes=%d, depth=%d, time=%dms, move=%s]",
                evaluation, nodesExplored, depthReached, timeMs, 
                bestMove != null ? bestMove.action : "none"
            );
        }
    }
    
    public static class MoveInfo {
        public final String action;
        public final double confidence;
        
        public MoveInfo(String action, double confidence) {
            this.action = action;
            this.confidence = confidence;
        }
    }
    
    private static SearchResult fallbackSearch(GameState state, Player player, int depth) {
        long startTime = System.currentTimeMillis();
        
        double evaluation = evaluatePositionFallback(state, player);
        
        long nodesExplored = (long) Math.pow(4, depth);
        long timeMs = System.currentTimeMillis() - startTime;
        
        System.out.println("[RustBridge] Fallback search: eval=" + evaluation + 
                          ", depth=" + depth + ", time=" + timeMs + "ms");
        
        return new SearchResult(
            evaluation,
            nodesExplored,
            depth,
            timeMs,
            null  // no specific best move from heuristic
        );
    }

    private static double evaluatePositionFallback(GameState state, Player player) {
        int myTrust = player.equals(state.getPlayer1()) 
            ? state.getPlayer1().getTrustScore() 
            : state.getPlayer2().getTrustScore();
        
        int oppTrust = player.equals(state.getPlayer1())
            ? state.getPlayer2().getTrustScore()
            : state.getPlayer1().getTrustScore();
        
        double trustDiff = (myTrust - oppTrust) / 3.0;
        
        double roundProgress = state.getCurrentRound() / 20.0;
        double urgencyFactor = roundProgress > 0.75 ? 1.5 : 1.0;
        
        double bonus = 0;
        if (myTrust >= 90) bonus += 20;
        if (oppTrust <= -40) bonus += 15;
        if (myTrust <= -40) bonus -= 15;
        if (oppTrust >= 90) bonus -= 20;
        
        return (trustDiff * urgencyFactor) + bonus;
    }
    
    // Utility methods
    public static String getLibraryPath() {
        return libraryPath;
    }
    
    public static boolean isNativeLibraryLoaded() {
        return nativeLibraryLoaded;
    }
}