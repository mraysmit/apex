package dev.mars.apex.demo.examples;

import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

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

/**
 * Test class to verify YAML configuration for custody auto-repair rules.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-29
 * @version 1.0
 */
public class CustodyAutoRepairYamlTest {
    
    @Test
    void testYamlConfigurationExists() {
        // Test that the YAML configuration file exists in resources
        InputStream configStream = getClass().getClassLoader()
            .getResourceAsStream("demo-rules/custody-auto-repair-rules.yaml");
        
        assertNotNull(configStream, "YAML configuration file should exist in resources");
        
        try {
            configStream.close();
        } catch (Exception e) {
            // Ignore close errors
        }
    }
    
    @Test
    void testYamlConfigurationStructure() {
        // Test that we can read the YAML file as text
        InputStream configStream = getClass().getClassLoader()
            .getResourceAsStream("demo-rules/custody-auto-repair-rules.yaml");
        
        assertNotNull(configStream);
        
        try {
            String yamlContent = new String(configStream.readAllBytes());
            
            // Verify key sections exist
            assertTrue(yamlContent.contains("metadata:"), "YAML should contain metadata section");
            assertTrue(yamlContent.contains("rule-chains:"), "YAML should contain rule-chains section");
            assertTrue(yamlContent.contains("enrichments:"), "YAML should contain enrichments section");
            
            // Verify specific configurations
            assertTrue(yamlContent.contains("si-auto-repair-chain"), "YAML should contain SI auto-repair chain");
            assertTrue(yamlContent.contains("accumulative-chaining"), "YAML should use accumulative chaining pattern");
            assertTrue(yamlContent.contains("client-si-enrichment"), "YAML should contain client SI enrichment");
            assertTrue(yamlContent.contains("market-si-enrichment"), "YAML should contain market SI enrichment");
            assertTrue(yamlContent.contains("instrument-si-enrichment"), "YAML should contain instrument SI enrichment");
            
            // Verify Asian markets are included
            assertTrue(yamlContent.contains("JAPAN"), "YAML should include Japan market");
            assertTrue(yamlContent.contains("HONG_KONG"), "YAML should include Hong Kong market");
            assertTrue(yamlContent.contains("SINGAPORE"), "YAML should include Singapore market");
            assertTrue(yamlContent.contains("KOREA"), "YAML should include Korea market");
            
            // Verify client configurations
            assertTrue(yamlContent.contains("CLIENT_A"), "YAML should include CLIENT_A configuration");
            assertTrue(yamlContent.contains("CLIENT_B"), "YAML should include CLIENT_B configuration");
            assertTrue(yamlContent.contains("PREMIUM_CLIENT_X"), "YAML should include PREMIUM_CLIENT_X configuration");
            
            // Verify weight configurations
            assertTrue(yamlContent.contains("weight: 0.6"), "YAML should include client-level weight 0.6");
            assertTrue(yamlContent.contains("weight: 0.3"), "YAML should include market-level weight 0.3");
            assertTrue(yamlContent.contains("weight: 0.1"), "YAML should include instrument-level weight 0.1");
            
            System.out.println("YAML Configuration Validation:");
            System.out.println("✓ File exists and is readable");
            System.out.println("✓ Contains required metadata section");
            System.out.println("✓ Contains rule-chains with accumulative chaining");
            System.out.println("✓ Contains enrichments for client/market/instrument SIs");
            System.out.println("✓ Includes all Asian markets (Japan, Hong Kong, Singapore, Korea)");
            System.out.println("✓ Contains weighted decision making configuration");
            System.out.println("✓ Includes sample client configurations");
            
        } catch (Exception e) {
            fail("Failed to read YAML configuration: " + e.getMessage());
        }
    }
    
    @Test
    void testYamlConfigurationBusinessUserFriendly() {
        // Test that the YAML is structured for business user maintenance
        InputStream configStream = getClass().getClassLoader()
            .getResourceAsStream("demo-rules/custody-auto-repair-rules.yaml");
        
        assertNotNull(configStream);
        
        try {
            String yamlContent = new String(configStream.readAllBytes());
            
            // Verify business-friendly features
            assertTrue(yamlContent.contains("# Custody and Safekeeping Auto-Repair Rules Configuration"), 
                      "YAML should have descriptive header comment");
            assertTrue(yamlContent.contains("description:"), "YAML should contain descriptions");
            assertTrue(yamlContent.contains("name:"), "YAML should contain human-readable names");
            assertTrue(yamlContent.contains("author:"), "YAML should contain author information");
            assertTrue(yamlContent.contains("tags:"), "YAML should contain tags for categorization");
            
            // Verify inline datasets (business user maintainable)
            assertTrue(yamlContent.contains("type: \"inline\""), "YAML should use inline datasets");
            assertTrue(yamlContent.contains("key-field:"), "YAML should specify key fields for lookups");
            assertTrue(yamlContent.contains("data:"), "YAML should contain inline data sections");
            
            // Verify field mappings are clear
            assertTrue(yamlContent.contains("field-mappings:"), "YAML should contain field mappings");
            assertTrue(yamlContent.contains("source-field:"), "YAML should specify source fields");
            assertTrue(yamlContent.contains("target-field:"), "YAML should specify target fields");
            
            System.out.println("\nBusiness User Maintenance Features:");
            System.out.println("✓ Descriptive comments and documentation");
            System.out.println("✓ Human-readable names and descriptions");
            System.out.println("✓ Inline datasets for easy maintenance");
            System.out.println("✓ Clear field mapping specifications");
            System.out.println("✓ Structured for non-technical users");
            
        } catch (Exception e) {
            fail("Failed to validate business user features: " + e.getMessage());
        }
    }
    
    @Test
    void testYamlConfigurationCompleteness() {
        // Test that all required components are present
        InputStream configStream = getClass().getClassLoader()
            .getResourceAsStream("demo-rules/custody-auto-repair-rules.yaml");
        
        assertNotNull(configStream);
        
        try {
            String yamlContent = new String(configStream.readAllBytes());
            
            // Count key components
            int clientEntries = countOccurrences(yamlContent, "clientId:");
            int marketEntries = countOccurrences(yamlContent, "market:");
            int instrumentEntries = countOccurrences(yamlContent, "instrumentType:");
            
            assertTrue(clientEntries >= 3, "Should have at least 3 client entries");
            assertTrue(marketEntries >= 4, "Should have at least 4 market entries (Asian markets)");
            assertTrue(instrumentEntries >= 4, "Should have at least 4 instrument type entries");
            
            // Verify enrichment types
            assertTrue(yamlContent.contains("client-si-enrichment"), "Should have client SI enrichment");
            assertTrue(yamlContent.contains("market-si-enrichment"), "Should have market SI enrichment");
            assertTrue(yamlContent.contains("instrument-si-enrichment"), "Should have instrument SI enrichment");
            assertTrue(yamlContent.contains("counterparty-resolution-enrichment"), "Should have counterparty resolution");
            assertTrue(yamlContent.contains("custodial-account-enrichment"), "Should have custodial account enrichment");
            
            System.out.println("\nConfiguration Completeness:");
            System.out.println("✓ Client entries: " + clientEntries);
            System.out.println("✓ Market entries: " + marketEntries);
            System.out.println("✓ Instrument entries: " + instrumentEntries);
            System.out.println("✓ All enrichment types present");
            System.out.println("✓ Complete configuration for demo scenarios");
            
        } catch (Exception e) {
            fail("Failed to validate configuration completeness: " + e.getMessage());
        }
    }
    
    private int countOccurrences(String text, String pattern) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(pattern, index)) != -1) {
            count++;
            index += pattern.length();
        }
        return count;
    }
}
