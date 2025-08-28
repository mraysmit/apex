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


import dev.mars.apex.demo.runners.AllDemosRunner;
import org.junit.jupiter.api.Test;

/**
 * Manual test to verify AllDemosRunnerAlt actually works by running it directly.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-28
 * @version 1.0
 */
public class AllDemosRunnerAltManualTest {
    
    @Test
    public void manualTestListCommand() {
        System.out.println("=== MANUAL TEST: AllDemosRunnerAlt --list ===");
        
        // Run the AllDemosRunnerAlt directly
        AllDemosRunner.main(new String[]{"--list"});
        
        System.out.println("=== END MANUAL TEST ===");
    }
    
    @Test
    public void manualTestPackageCommand() {
        System.out.println("=== MANUAL TEST: AllDemosRunnerAlt --package core ===");
        
        // Run the AllDemosRunnerAlt directly
        AllDemosRunner.main(new String[]{"--package", "core"});
        
        System.out.println("=== END MANUAL TEST ===");
    }
    
    @Test
    public void manualTestDiscovery() {
        System.out.println("=== MANUAL TEST: Direct Discovery ===");
        
        AllDemosRunner runner = new AllDemosRunner();
        runner.discoverDemos();
        
        System.out.println("Discovered " + runner.discoveredDemos.size() + " demos:");
        for (AllDemosRunner.DemoInfo demo : runner.discoveredDemos) {
            System.out.println("  - " + demo.getFullName() + " (" + demo.getExecutionType() + ")");
        }
        
        System.out.println("=== END MANUAL TEST ===");
    }
}
