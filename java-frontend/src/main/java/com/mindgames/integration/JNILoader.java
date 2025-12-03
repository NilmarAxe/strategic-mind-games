package com.mindgames.integration;

import java.io.*;
import java.nio.file.*;

public class JNILoader {
    private static boolean loaded = false;
    
    public static synchronized void loadLibrary(String libraryName) throws IOException {
        if (loaded) {
            System.out.println("[JNILoader] Library already loaded");
            return;
        }
        
        String osName = System.getProperty("os.name").toLowerCase();
        String osArch = System.getProperty("os.arch").toLowerCase();
        
        System.out.println("[JNILoader] Loading library: " + libraryName);
        System.out.println("[JNILoader] OS: " + osName + ", Architecture: " + osArch);
        
        String libraryPath = determineLibraryPath(libraryName, osName, osArch);
        
        try {
            System.load(libraryPath);
            loaded = true;
            System.out.println("[JNILoader] Successfully loaded: " + libraryPath);
        } catch (UnsatisfiedLinkError e) {
            System.err.println("[JNILoader] Failed to load library: " + libraryPath);
            throw new IOException("Failed to load native library: " + libraryPath, e);
        }
    }
    
    private static String determineLibraryPath(String baseName, String os, String arch) {
        String prefix = "";
        String extension = "";
        
        if (os.contains("win")) {
            extension = ".dll";
        } else if (os.contains("mac")) {
            prefix = "lib";
            extension = ".dylib";
        } else {
            prefix = "lib";
            extension = ".so";
        }
        
        String libraryName = prefix + baseName + extension;
        
        // Try multiple possible locations
        String[] searchPaths = {
            "../rust-optimizer/target/release/" + libraryName,
            "rust-optimizer/target/release/" + libraryName,
            "./lib/" + libraryName,
            "./lib/native/" + libraryName,
            "./" + libraryName
        };
        
        for (String path : searchPaths) {
            File file = new File(path);
            if (file.exists()) {
                System.out.println("[JNILoader] Found at: " + file.getAbsolutePath());
                return file.getAbsolutePath();
            }
        }
        
        throw new RuntimeException("Native library not found: " + libraryName + 
                                 "\nSearched paths:\n" + String.join("\n", searchPaths));
    }
    
    public static boolean isLoaded() {
        return loaded;
    }
    
    /**
     * Extract native library from JAR to temp directory and load it.
     * Useful for distribution.
     */
    public static synchronized void loadLibraryFromResources(String libraryName) throws IOException {
        if (loaded) {
            return;
        }
        
        String osName = System.getProperty("os.name").toLowerCase();
        String resourcePath = "/native/" + getLibraryFileName(libraryName, osName);
        
        try (InputStream is = JNILoader.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("Library not found in resources: " + resourcePath);
            }
            
            // Create temp file
            String tempFileName = getLibraryFileName(libraryName, osName);
            Path tempFile = Files.createTempFile("jni_", "_" + tempFileName);
            tempFile.toFile().deleteOnExit();
            
            // Copy to temp
            Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);
            
            // Load
            System.load(tempFile.toAbsolutePath().toString());
            loaded = true;
            
            System.out.println("[JNILoader] Loaded from resources: " + tempFile);
            
        } catch (IOException e) {
            throw new IOException("Failed to load library from resources", e);
        }
    }
    
    private static String getLibraryFileName(String baseName, String os) {
        if (os.contains("win")) {
            return baseName + ".dll";
        } else if (os.contains("mac")) {
            return "lib" + baseName + ".dylib";
        } else {
            return "lib" + baseName + ".so";
        }
    }
}