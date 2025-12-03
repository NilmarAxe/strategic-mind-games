package com.mindgames;

import com.mindgames.ui.GameWindow;
import com.mindgames.integration.AIBridge;
import com.mindgames.integration.RustBridge;
import com.mindgames.config.Configuration;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.JOptionPane;

public class Main {
    private static final String VERSION = "1.0.0";
    private static final boolean DEBUG_MODE = true;
    
    public static void main(String[] args) {
        configureEnvironment();
        
        // Try to initialize bridges, but don't fail if they're not available
        initializeBridges();
        
        launchInterface();
    }
    
    private static void configureEnvironment() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            logError("Failed to set system look and feel", e);
        }
        
        System.setProperty("java.awt.headless", "false");
        
        if (DEBUG_MODE) {
            System.out.println("Strategic Mind Games v" + VERSION);
            System.out.println("Initializing components...");
        }
        
        // Load configuration
        if (Configuration.isLoaded()) {
            System.out.println("Configuration loaded successfully");
        } else {
            System.err.println("Warning: Configuration not loaded, using defaults");
        }
    }
    
    private static void initializeBridges() {
        // Initialize AI Bridge
        try {
            AIBridge.initialize();
            System.out.println("AI Bridge: Connected");
        } catch (Exception e) {
            System.err.println("AI Bridge initialization failed: " + e.getMessage());
            System.err.println("Game will run with fallback AI");
            if (DEBUG_MODE) {
                e.printStackTrace();
            }
        }
        
        // Initialize Rust Bridge
        try {
            RustBridge.initialize();
            if (RustBridge.isAvailable()) {
                System.out.println("Rust Optimizer: Loaded");
            } else {
                System.out.println("Rust Optimizer: Not available (using Java fallback)");
            }
        } catch (Exception e) {
            System.err.println("Rust bridge initialization failed: " + e.getMessage());
            System.err.println("Game will run without Rust optimization");
            if (DEBUG_MODE) {
                e.printStackTrace();
            }
        }
        
        // Show status summary
        showInitializationSummary();
    }
    
    private static void showInitializationSummary() {
        System.out.println("\n=== Initialization Summary ===");
        System.out.println("AI Server:      " + (AIBridge.isServerHealthy() ? "✓ Online" : "✗ Offline (using fallback)"));
        System.out.println("Rust Optimizer: " + (RustBridge.isAvailable() ? "✓ Available" : "✗ Not available"));
        System.out.println("Game Mode:      " + (AIBridge.isServerHealthy() && RustBridge.isAvailable() 
            ? "Full AI" : "Basic"));
        System.out.println("==============================\n");
    }
    
    private static void launchInterface() {
        SwingUtilities.invokeLater(() -> {
            try {
                GameWindow window = new GameWindow();
                window.setVisible(true);
                
                // Add shutdown hook
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    System.out.println("Shutting down application...");
                    shutdownBridges();
                }));
                
                // Show warnings if components not available
                showWarningsIfNeeded();
                
            } catch (Exception e) {
                System.err.println("Fatal error launching game window");
                e.printStackTrace();
                
                JOptionPane.showMessageDialog(
                    null,
                    "Failed to launch game: " + e.getMessage(),
                    "Fatal Error",
                    JOptionPane.ERROR_MESSAGE
                );
                
                System.exit(1);
            }
        });
    }
    
    private static void showWarningsIfNeeded() {
        SwingUtilities.invokeLater(() -> {
            StringBuilder warnings = new StringBuilder();
            
            if (!AIBridge.isServerHealthy()) {
                warnings.append("• Python AI Server is offline\n");
                warnings.append("  Using fallback AI (limited intelligence)\n\n");
            }
            
            if (!RustBridge.isAvailable()) {
                warnings.append("• Rust Optimizer not available\n");
                warnings.append("  Game tree search will be slower\n\n");
            }
            
            if (warnings.length() > 0) {
                warnings.insert(0, "Game running in limited mode:\n\n");
                warnings.append("\nThe game will still work, but with reduced AI capabilities.");
                
                JOptionPane.showMessageDialog(
                    null,
                    warnings.toString(),
                    "Limited Mode",
                    JOptionPane.WARNING_MESSAGE
                );
            }
        });
    }
    
    private static void shutdownBridges() {
        try {
            AIBridge.shutdown();
        } catch (Exception e) {
            System.err.println("Error shutting down AI Bridge: " + e.getMessage());
        }
        
        try {
            RustBridge.shutdown();
        } catch (Exception e) {
            System.err.println("Error shutting down Rust Bridge: " + e.getMessage());
        }
    }
    
    private static void logError(String message, Exception e) {
        System.err.println("[ERROR] " + message);
        if (DEBUG_MODE && e != null) {
            e.printStackTrace();
        }
    }
}