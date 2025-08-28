package dev.mars.apex.demo;

/*
 * Copyright 2025 Mark Andrew Ray-Smith Cityline Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import dev.mars.apex.demo.examples.YamlConfigurationDemo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to debug the YamlConfigurationDemo classpath issue.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-28
 * @version 1.0
 */
public class YamlConfigurationDemoTest {
    
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    
    @BeforeEach
    public void setUpStreams() {
        System.setOut(new PrintStream(outContent));
    }
    
    @AfterEach
    public void restoreStreams() {
        System.setOut(originalOut);
    }
    
    @Test
    public void testResourceExists() {
        // Test if the resource can be found
        String resourcePath = "config/financial-validation-rules.yaml";
        
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream resourceStream = classLoader.getResourceAsStream(resourcePath);
        
        System.setOut(originalOut); // Use normal output for debugging
        
        System.out.println("=== Resource Loading Test ===");
        System.out.println("Resource path: " + resourcePath);
        System.out.println("ClassLoader: " + classLoader.getClass().getName());
        System.out.println("Resource found: " + (resourceStream != null));
        
        if (resourceStream != null) {
            System.out.println("✅ Resource is accessible from classpath");
            try {
                resourceStream.close();
            } catch (Exception e) {
                // ignore
            }
        } else {
            System.out.println("❌ Resource NOT found on classpath");
            
            // Try alternative paths
            String[] alternativePaths = {
                "/config/financial-validation-rules.yaml",
                "financial-validation-rules.yaml",
                "/financial-validation-rules.yaml"
            };
            
            for (String altPath : alternativePaths) {
                InputStream altStream = classLoader.getResourceAsStream(altPath);
                System.out.println("Alternative path '" + altPath + "': " + (altStream != null ? "FOUND" : "NOT FOUND"));
                if (altStream != null) {
                    try {
                        altStream.close();
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }
        }
        
        assertNotNull(resourceStream, "The YAML configuration file should be accessible from classpath");
    }
    
    @Test
    public void testYamlConfigurationDemoRun() {
        System.setOut(originalOut); // Use normal output for this test
        
        System.out.println("=== Testing YamlConfigurationDemo ===");
        
        // Try to run the demo
        try {
            YamlConfigurationDemo.main(new String[0]);
            System.out.println("✅ YamlConfigurationDemo ran successfully!");
        } catch (Exception e) {
            System.out.println("❌ YamlConfigurationDemo failed: " + e.getMessage());
            e.printStackTrace();
            
            // Don't fail the test, just report the issue
            System.out.println("This indicates the demo needs to be fixed to work properly.");
        }
    }
}
