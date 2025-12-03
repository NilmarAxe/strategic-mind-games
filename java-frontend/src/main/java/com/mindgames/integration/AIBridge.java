package com.mindgames.integration;

import com.mindgames.core.*;
import com.mindgames.config.Configuration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public class AIBridge {
    private static final String PYTHON_API_URL = Configuration.getAIServerUrl();
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final ExecutorService executor = Executors.newFixedThreadPool(2);
    
    private static final int MAX_RETRY_ATTEMPTS = Configuration.getAIRetryAttempts();
    private static final int RETRY_DELAY_MS = Configuration.getAIRetryDelay();
    private static final int SERVER_STARTUP_TIMEOUT_SECONDS = 30;
    private static final int HEALTH_CHECK_TIMEOUT_MS = 2000;
    private static final int REQUEST_TIMEOUT_MS = Configuration.getAIServerTimeout();
    
    private static boolean initialized = false;
    private static Process pythonProcess;
    private static volatile boolean serverHealthy = false;
    
    // Health check daemon
    private static ScheduledExecutorService healthChecker;
    
    public static void initialize() throws Exception {
        if (initialized) {
            System.out.println("[AIBridge] Already initialized");
            return;
        }
        
        System.out.println("[AIBridge] Initializing Python AI Engine...");
        
        try {
            // Start Python API server
            startPythonServer();
            
            // Wait for server to be ready
            waitForServer(SERVER_STARTUP_TIMEOUT_SECONDS);
            
            // Start health check daemon
            startHealthCheckDaemon();
            
            initialized = true;
            serverHealthy = true;
            System.out.println("[AIBridge] AI Engine initialized successfully");
            
        } catch (Exception e) {
            System.err.println("[AIBridge] Initialization failed: " + e.getMessage());
            cleanup();
            throw e;
        }
    }
    
    private static void startPythonServer() throws IOException {
        // Try to find Python executable
        String[] pythonCommands = {"python3", "python", "py"};
        String pythonCmd = null;
        
        for (String cmd : pythonCommands) {
            if (isPythonAvailable(cmd)) {
                pythonCmd = cmd;
                break;
            }
        }
        
        if (pythonCmd == null) {
            throw new IOException("Python executable not found. Please install Python 3.9+");
        }
        
        System.out.println("[AIBridge] Using Python command: " + pythonCmd);
        
        ProcessBuilder pb = new ProcessBuilder(
            pythonCmd, "-m", "src.api_server"
        );
        
        // Set working directory to python-ai folder
        File pythonDir = new File("../python-ai");
        if (!pythonDir.exists()) {
            // Try alternative paths
            pythonDir = new File("python-ai");
            if (!pythonDir.exists()) {
                throw new IOException("Python AI directory not found: " + pythonDir.getAbsolutePath());
            }
        }
        
        pb.directory(pythonDir);
        pb.redirectErrorStream(true);
        
        System.out.println("[AIBridge] Starting Python server from: " + pythonDir.getAbsolutePath());
        
        pythonProcess = pb.start();
        
        // Start thread to consume output
        executor.submit(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(pythonProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[Python] " + line);
                }
            } catch (IOException e) {
                if (initialized) {
                    System.err.println("[AIBridge] Error reading Python output: " + e.getMessage());
                }
            }
        });
        
        // Monitor process
        executor.submit(() -> {
            try {
                int exitCode = pythonProcess.waitFor();
                if (initialized) {
                    System.err.println("[AIBridge] Python process exited with code: " + exitCode);
                    serverHealthy = false;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
    
    private static boolean isPythonAvailable(String command) {
        try {
            Process process = new ProcessBuilder(command, "--version").start();
            process.waitFor(2, TimeUnit.SECONDS);
            return process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    private static void waitForServer(int timeoutSeconds) throws Exception {
        int attempts = 0;
        int maxAttempts = timeoutSeconds * 2;
        
        System.out.println("[AIBridge] Waiting for server to start...");
        
        while (attempts < maxAttempts) {
            try {
                if (checkHealth()) {
                    System.out.println("[AIBridge] Server is ready!");
                    return;
                }
            } catch (Exception e) {
                // Server not ready yet
            }
            
            Thread.sleep(500);
            attempts++;
            
            if (attempts % 10 == 0) {
                System.out.println("[AIBridge] Still waiting... (" + (attempts / 2) + "s)");
            }
            
            // Check if process died
            if (pythonProcess != null && !pythonProcess.isAlive()) {
                throw new Exception("Python process died during startup");
            }
        }
        
        throw new Exception("Python server failed to start within " + timeoutSeconds + " seconds");
    }
    
    private static void startHealthCheckDaemon() {
        healthChecker = Executors.newScheduledThreadPool(1);
        
        healthChecker.scheduleAtFixedRate(() -> {
            try {
                serverHealthy = checkHealth();
                if (!serverHealthy) {
                    System.err.println("[AIBridge] Health check failed - server appears down");
                }
            } catch (Exception e) {
                serverHealthy = false;
            }
        }, 10, 10, TimeUnit.SECONDS);
    }
    
    public static boolean checkHealth() {
        try {
            URL url = new URL(PYTHON_API_URL + "/health");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(HEALTH_CHECK_TIMEOUT_MS);
            conn.setReadTimeout(HEALTH_CHECK_TIMEOUT_MS);
            
            int responseCode = conn.getResponseCode();
            conn.disconnect();
            
            return responseCode == 200;
        } catch (Exception e) {
            return false;
        }
    }
    
    public static boolean isServerHealthy() {
        return serverHealthy;
    }
    
    // Modified requestMove with fallback behavior (REPLACED as requested)
    public static Move requestMove(GameState state, Player player) throws Exception {
        if (!initialized) {
            throw new IllegalStateException("AI Bridge not initialized. Call initialize() first.");
        }
        
        if (!serverHealthy) {
            System.err.println("[AIBridge] Server unhealthy, using fallback AI");
            return createFallbackMove(state, player);
        }
        
        try {
            return requestMoveWithRetry(state, player);
        } catch (Exception e) {
            System.err.println("[AIBridge] All retries failed, using fallback: " + e.getMessage());
            return createFallbackMove(state, player);
        }
    }
    
    private static Move requestMoveWithRetry(GameState state, Player player) throws Exception {
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                return executeRequest(state, player);
                
            } catch (Exception e) {
                lastException = e;
                System.err.println("[AIBridge] Attempt " + attempt + "/" + MAX_RETRY_ATTEMPTS + 
                                 " failed: " + e.getMessage());
                
                if (attempt < MAX_RETRY_ATTEMPTS) {
                    System.out.println("[AIBridge] Retrying in " + RETRY_DELAY_MS + "ms...");
                    Thread.sleep(RETRY_DELAY_MS);
                    
                    // Check if server is still alive
                    if (!checkHealth()) {
                        throw new Exception("Server is not responding to health checks");
                    }
                }
            }
        }
        
        throw new Exception("Failed after " + MAX_RETRY_ATTEMPTS + " attempts", lastException);
    }
    
    private static Move executeRequest(GameState state, Player player) throws Exception {
        // Convert game state to JSON
        Map<String, Object> stateMap = buildStateMap(state);
        
        // Add player information
        Map<String, Object> request = new HashMap<>();
        request.put("game_state", stateMap);
        request.put("player_type", player.getType().toString());
        
        String requestJson = mapper.writeValueAsString(request);
        
        // Make HTTP request to Python API
        String responseJson = makePostRequest("/ai/decide", requestJson);
        
        // Parse response
        JsonNode response = mapper.readTree(responseJson);
        
        // Check for errors in response
        if (response.has("error")) {
            throw new Exception("AI Engine error: " + response.get("error").asText());
        }
        
        return parseAIDecision(response, player);
    }
    
    private static Map<String, Object> buildStateMap(GameState state) {
        Map<String, Object> stateMap = new HashMap<>();
        
        stateMap.put("round", state.getCurrentRound());
        stateMap.put("phase", state.getPhase().toString());
        stateMap.put("player1_trust", state.getPlayer1().getTrustScore());
        stateMap.put("player2_trust", state.getPlayer2().getTrustScore());
        
        if (state.getCurrentClaim() != null) {
            Map<String, Object> claimMap = new HashMap<>();
            Claim claim = state.getCurrentClaim();
            claimMap.put("description", claim.getDescription());
            claimMap.put("type", claim.getType().toString());
            claimMap.put("boldness", claim.getBoldness());
            stateMap.put("current_claim", claimMap);
        } else {
            stateMap.put("current_claim", null);
        }
        
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
        
        return stateMap;
    }
    
    private static Move parseAIDecision(JsonNode response, Player player) {
        String action = response.get("action").asText();
        double confidence = response.get("confidence").asDouble();
        
        Move.Builder builder = new Move.Builder()
            .player(player)
            .confidence(confidence);
        
        switch (action.toUpperCase()) {
            case "CLAIM":
                JsonNode claimData = response.get("claim_data");
                if (claimData == null) {
                    throw new IllegalArgumentException("Claim action requires claim_data");
                }
                
                Claim claim = new Claim(
                    claimData.get("description").asText(),
                    parseClaimType(claimData.get("type").asText()),
                    claimData.get("boldness").asDouble()
                );
                builder.type(Move.MoveType.CLAIM).claim(claim);
                break;
                
            case "CHALLENGE":
                builder.type(Move.MoveType.CHALLENGE).challenge(true);
                break;
                
            case "ACCEPT":
                builder.type(Move.MoveType.ACCEPT).challenge(false);
                break;
                
            default:
                throw new IllegalArgumentException("Unknown action: " + action);
        }
        
        return builder.build();
    }
    
    private static Claim.ClaimType parseClaimType(String type) {
        return switch (type.toUpperCase()) {
            case "INFORMATION" -> Claim.ClaimType.INFORMATION;
            case "PREDICTION" -> Claim.ClaimType.PREDICTION;
            case "ACCUSATION" -> Claim.ClaimType.ACCUSATION;
            case "ALLIANCE" -> Claim.ClaimType.ALLIANCE;
            default -> Claim.ClaimType.INFORMATION;
        };
    }
    
    private static String makePostRequest(String endpoint, String jsonBody) throws IOException {
        URL url = new URL(PYTHON_API_URL + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(REQUEST_TIMEOUT_MS);
        
        // Write request body
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
            os.flush();
        }
        
        // Read response
        int responseCode = conn.getResponseCode();
        
        InputStream is;
        if (responseCode >= 200 && responseCode < 300) {
            is = conn.getInputStream();
        } else {
            is = conn.getErrorStream();
            if (is == null) {
                throw new IOException("HTTP error code: " + responseCode);
            }
        }
        
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line.trim());
            }
        }
        
        conn.disconnect();
        
        if (responseCode >= 400) {
            throw new IOException("HTTP " + responseCode + ": " + response.toString());
        }
        
        return response.toString();
    }
    
    public static CompletableFuture<Move> requestMoveAsync(GameState state, Player player) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return requestMove(state, player);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, executor);
    }
    
    public static void setDifficulty(String difficulty) throws Exception {
        if (!initialized) {
            throw new IllegalStateException("AI Bridge not initialized");
        }
        
        Map<String, String> request = new HashMap<>();
        request.put("difficulty", difficulty.toLowerCase());
        
        String requestJson = mapper.writeValueAsString(request);
        makePostRequest("/ai/set_difficulty", requestJson);
        
        System.out.println("[AIBridge] AI difficulty set to: " + difficulty);
    }

    private static Move createFallbackMove(GameState state, Player player) {
        System.out.println("[AIBridge] Using local fallback AI");
        
        Move.Builder builder = new Move.Builder()
            .player(player)
            .confidence(0.6);
        
        if (state.getPhase() == GameEngine.GamePhase.CLAIM) {
            return createFallbackClaim(state, player, builder);
            
        } else if (state.getPhase() == GameEngine.GamePhase.CHALLENGE) {
            return createFallbackChallengeDecision(state, player, builder);
        }
        
        // Default: accept
        return builder.type(Move.MoveType.ACCEPT).challenge(false).build();
    }
    
    private static Move createFallbackClaim(GameState state, Player player, Move.Builder builder) {
        int myTrust = player.equals(state.getPlayer1()) 
            ? state.getPlayer1().getTrustScore() 
            : state.getPlayer2().getTrustScore();
        
        int oppTrust = player.equals(state.getPlayer1())
            ? state.getPlayer2().getTrustScore()
            : state.getPlayer1().getTrustScore();
        
        double roundProgress = state.getCurrentRound() / 20.0;
        
        double boldness;
        String claimDescription;
        Claim.ClaimType claimType;
        
        if (myTrust < oppTrust - 20) {
            boldness = 0.6 + (roundProgress * 0.2);
            claimDescription = "I have critical information that will shift the balance";
            claimType = Claim.ClaimType.ACCUSATION;
        } else if (myTrust > oppTrust + 20) {
            boldness = 0.3;
            claimDescription = "My analysis confirms a favorable position";
            claimType = Claim.ClaimType.INFORMATION;
        } else {
            boldness = 0.45;
            claimDescription = "I predict developments that favor strategic positioning";
            claimType = Claim.ClaimType.PREDICTION;
        }
        
        Claim claim = new Claim(claimDescription, claimType, boldness);
        
        return builder
            .type(Move.MoveType.CLAIM)
            .claim(claim)
            .confidence(0.5 + (1.0 - boldness) * 0.3)
            .build();
    }
    
    private static Move createFallbackChallengeDecision(GameState state, Player player, 
                                                       Move.Builder builder) {
        Claim currentClaim = state.getCurrentClaim();
        
        if (currentClaim == null) {
            return builder.type(Move.MoveType.ACCEPT).challenge(false).build();
        }
        
        double boldness = currentClaim.getBoldness();
        
        // Trust differential
        int myTrust = player.equals(state.getPlayer1()) 
            ? state.getPlayer1().getTrustScore() 
            : state.getPlayer2().getTrustScore();
        
        int oppTrust = player.equals(state.getPlayer1())
            ? state.getPlayer2().getTrustScore()
            : state.getPlayer1().getTrustScore();
        
        double suspicionScore = boldness * 0.6;
        
        if (oppTrust < myTrust - 20) {
            suspicionScore += 0.2;
        }
        
        if (myTrust < oppTrust - 20) {
            suspicionScore += 0.15;
        }
        
        boolean shouldChallenge = suspicionScore > 0.55;
        
        if (shouldChallenge) {
            return builder
                .type(Move.MoveType.CHALLENGE)
                .challenge(true)
                .confidence(suspicionScore)
                .build();
        } else {
            return builder
                .type(Move.MoveType.ACCEPT)
                .challenge(false)
                .confidence(1.0 - suspicionScore)
                .build();
        }
    }
    
    private static void cleanup() {
        if (healthChecker != null) {
            healthChecker.shutdown();
        }
        
        if (pythonProcess != null && pythonProcess.isAlive()) {
            pythonProcess.destroy();
            try {
                if (!pythonProcess.waitFor(5, TimeUnit.SECONDS)) {
                    pythonProcess.destroyForcibly();
                }
            } catch (InterruptedException e) {
                pythonProcess.destroyForcibly();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    public static void shutdown() {
        System.out.println("[AIBridge] Shutting down...");
        
        cleanup();
        
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        initialized = false;
        serverHealthy = false;
        
        System.out.println("[AIBridge] Shutdown complete");
    }
    
    // Utility methods for monitoring
    public static boolean isInitialized() {
        return initialized;
    }
    
    public static ProcessInfo getProcessInfo() {
        return new ProcessInfo(
            initialized,
            serverHealthy,
            pythonProcess != null && pythonProcess.isAlive()
        );
    }
    
    public static class ProcessInfo {
        public final boolean initialized;
        public final boolean serverHealthy;
        public final boolean processAlive;
        
        public ProcessInfo(boolean initialized, boolean serverHealthy, boolean processAlive) {
            this.initialized = initialized;
            this.serverHealthy = serverHealthy;
            this.processAlive = processAlive;
        }
        
        @Override
        public String toString() {
            return String.format("ProcessInfo[initialized=%s, healthy=%s, alive=%s]",
                initialized, serverHealthy, processAlive);
        }
    }
}