package dev.mars.rulesengine.demo;

import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to debug resource loading issues.
 */
public class ResourceLoadingTest {
    
    @Test
    public void testResourceLoadingFromDifferentClassLoaders() {
        String resourcePath = "config/financial-validation-rules.yaml";
        
        System.out.println("=== Resource Loading Debug ===");
        
        // Test 1: Current class classloader
        ClassLoader currentClassLoader = this.getClass().getClassLoader();
        InputStream stream1 = currentClassLoader.getResourceAsStream(resourcePath);
        System.out.println("1. Current class classloader: " + (stream1 != null ? "FOUND" : "NOT FOUND"));
        if (stream1 != null) {
            try { stream1.close(); } catch (Exception e) {}
        }
        
        // Test 2: Thread context classloader
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        InputStream stream2 = contextClassLoader.getResourceAsStream(resourcePath);
        System.out.println("2. Thread context classloader: " + (stream2 != null ? "FOUND" : "NOT FOUND"));
        if (stream2 != null) {
            try { stream2.close(); } catch (Exception e) {}
        }
        
        // Test 3: System classloader
        ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
        InputStream stream3 = systemClassLoader.getResourceAsStream(resourcePath);
        System.out.println("3. System classloader: " + (stream3 != null ? "FOUND" : "NOT FOUND"));
        if (stream3 != null) {
            try { stream3.close(); } catch (Exception e) {}
        }
        
        // Test 4: Try different paths
        String[] alternativePaths = {
            "config/financial-validation-rules.yaml",
            "/config/financial-validation-rules.yaml",
            "financial-validation-rules.yaml",
            "/financial-validation-rules.yaml"
        };
        
        System.out.println("\nTesting alternative paths:");
        for (String path : alternativePaths) {
            InputStream stream = currentClassLoader.getResourceAsStream(path);
            System.out.println("  " + path + ": " + (stream != null ? "FOUND" : "NOT FOUND"));
            if (stream != null) {
                try { stream.close(); } catch (Exception e) {}
            }
        }
        
        // Test 5: Check what resources are available
        System.out.println("\nClassloader info:");
        System.out.println("Current classloader: " + currentClassLoader.getClass().getName());
        System.out.println("Context classloader: " + contextClassLoader.getClass().getName());
        System.out.println("System classloader: " + systemClassLoader.getClass().getName());
        
        // At least one should work
        assertTrue(stream1 != null || stream2 != null || stream3 != null, 
                  "Resource should be accessible from at least one classloader");
    }
}
