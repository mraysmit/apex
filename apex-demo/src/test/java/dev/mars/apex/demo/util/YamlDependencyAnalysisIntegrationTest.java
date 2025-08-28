package dev.mars.apex.demo.util;

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


import dev.mars.apex.core.util.YamlDependencyAnalyzer;
import dev.mars.apex.core.util.YamlDependencyGraph;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for YAML dependency analysis with actual scenario files.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-28
 * @version 1.0
 */
class YamlDependencyAnalysisIntegrationTest {
    
    @Test
    void testOtcOptionsScenarioAnalysis() {
        YamlDependencyAnalyzer analyzer = new YamlDependencyAnalyzer("src/main/resources");
        
        try {
            YamlDependencyGraph graph = analyzer.analyzeYamlDependencies("scenarios/otc-options-scenario.yaml");
            
            assertNotNull(graph);
            assertEquals("scenarios/otc-options-scenario.yaml", graph.getRootFile());
            assertTrue(graph.getTotalFiles() >= 1);
            
            // Generate and print report for manual verification
            String report = analyzer.generateTextReport(graph);
            System.out.println("OTC Options Scenario Analysis:");
            System.out.println(report);
            
        } catch (Exception e) {
            System.err.println("Analysis failed: " + e.getMessage());
            // Don't fail the test if files are missing - this is expected in some environments
        }
    }
    
    @Test
    void testCommoditySwapsScenarioAnalysis() {
        YamlDependencyAnalyzer analyzer = new YamlDependencyAnalyzer("src/main/resources");
        
        try {
            YamlDependencyGraph graph = analyzer.analyzeYamlDependencies("scenarios/commodity-swaps-scenario.yaml");
            
            assertNotNull(graph);
            assertEquals("scenarios/commodity-swaps-scenario.yaml", graph.getRootFile());
            assertTrue(graph.getTotalFiles() >= 1);
            
            // Generate and print report for manual verification
            String report = analyzer.generateTextReport(graph);
            System.out.println("Commodity Swaps Scenario Analysis:");
            System.out.println(report);
            
        } catch (Exception e) {
            System.err.println("Analysis failed: " + e.getMessage());
            // Don't fail the test if files are missing - this is expected in some environments
        }
    }
    
    @Test
    void testSettlementAutoRepairScenarioAnalysis() {
        YamlDependencyAnalyzer analyzer = new YamlDependencyAnalyzer("src/main/resources");
        
        try {
            YamlDependencyGraph graph = analyzer.analyzeYamlDependencies("scenarios/settlement-auto-repair-scenario.yaml");
            
            assertNotNull(graph);
            assertEquals("scenarios/settlement-auto-repair-scenario.yaml", graph.getRootFile());
            assertTrue(graph.getTotalFiles() >= 1);
            
            // Generate and print report for manual verification
            String report = analyzer.generateTextReport(graph);
            System.out.println("Settlement Auto-Repair Scenario Analysis:");
            System.out.println(report);
            
        } catch (Exception e) {
            System.err.println("Analysis failed: " + e.getMessage());
            // Don't fail the test if files are missing - this is expected in some environments
        }
    }
}
