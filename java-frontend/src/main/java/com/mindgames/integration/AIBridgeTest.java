package com.mindgames.integration;

import com.mindgames.core.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class AIBridgeTest {
    
    @BeforeAll
    public static void setup() throws Exception {
        AIBridge.initialize();
    }
    
    @AfterAll
    public static void teardown() {
        AIBridge.shutdown();
    }
    
    @Test
    public void testHealthCheck() {
        assertTrue(AIBridge.isServerHealthy(), "Server should be healthy after initialization");
    }
    
    @Test
    public void testRequestMove() throws Exception {
        GameState state = createTestState();
        Player player = new Player("AI", Player.PlayerType.AI_MEDIUM);
        
        Move move = AIBridge.requestMove(state, player);
        
        assertNotNull(move, "Move should not be null");
        assertNotNull(move.getType(), "Move type should not be null");
    }
    
    @Test
    public void testProcessInfo() {
        AIBridge.ProcessInfo info = AIBridge.getProcessInfo();
        
        assertTrue(info.initialized, "Should be initialized");
        assertTrue(info.serverHealthy, "Server should be healthy");
        assertTrue(info.processAlive, "Process should be alive");
    }
    
    private GameState createTestState() {
        GameState state = new GameState();
        state.setPlayer1(new Player("Player1", Player.PlayerType.HUMAN));
        state.setPlayer2(new Player("AI", Player.PlayerType.AI_MEDIUM));
        state.setCurrentRound(1);
        state.setPhase(GameEngine.GamePhase.CLAIM);
        return state;
    }
}