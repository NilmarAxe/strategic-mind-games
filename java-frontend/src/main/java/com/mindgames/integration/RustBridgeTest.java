package com.mindgames.integration;

import com.mindgames.core.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class RustBridgeTest {
    
    @BeforeAll
    public static void setup() {
        RustBridge.initialize();
    }
    
    @Test
    public void testIsAvailable() {
        // May be false if native library not built
        System.out.println("RustBridge available: " + RustBridge.isAvailable());
    }
    
    @Test
    public void testEvaluatePosition() {
        if (!RustBridge.isAvailable()) {
            System.out.println("Skipping test - Rust bridge not available");
            return;
        }
        
        GameState state = createTestState();
        Player player = state.getPlayer1();
        
        double evaluation = RustBridge.evaluatePosition(state, player);
        
        System.out.println("Evaluation: " + evaluation);
        assertTrue(evaluation >= -100 && evaluation <= 100, 
                  "Evaluation should be between -100 and 100");
    }
    
    @Test
    public void testSearchBestMove() {
        if (!RustBridge.isAvailable()) {
            System.out.println("Skipping test - Rust bridge not available");
            return;
        }
        
        GameState state = createTestState();
        Player player = state.getPlayer1();
        
        RustBridge.SearchResult result = RustBridge.searchBestMove(state, player, 3);
        
        assertNotNull(result, "Search result should not be null");
        assertTrue(result.nodesExplored > 0, "Should have explored some nodes");
        assertTrue(result.depthReached > 0, "Should have reached some depth");
        
        System.out.println("Search result: " + result);
    }
    
    @Test
    public void testEvaluateAdvantage() {
        if (!RustBridge.isAvailable()) {
            System.out.println("Skipping test - Rust bridge not available");
            return;
        }
        
        // Test state where player1 is ahead
        GameState advantageState = createTestState();
        advantageState.getPlayer1().modifyTrust(30); // 80 trust
        advantageState.getPlayer2().modifyTrust(-20); // 30 trust
        
        double eval = RustBridge.evaluatePosition(advantageState, advantageState.getPlayer1());
        
        System.out.println("Advantage evaluation: " + eval);
        assertTrue(eval > 0, "Player with advantage should have positive evaluation");
    }
    
    @Test
    public void testLibraryPath() {
        String path = RustBridge.getLibraryPath();
        System.out.println("Library path: " + path);
        
        if (RustBridge.isNativeLibraryLoaded()) {
            assertNotNull(path, "Library path should not be null when loaded");
        }
    }
    
    private GameState createTestState() {
        GameState state = new GameState();
        state.setPlayer1(new Player("Player1", Player.PlayerType.HUMAN));
        state.setPlayer2(new Player("Player2", Player.PlayerType.HUMAN));
        state.setCurrentRound(1);
        state.setPhase(GameEngine.GamePhase.CLAIM);
        return state;
    }
}