package dev.mars.apex.demo.runners;

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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AllDemosRunnerAlt provides a comprehensive demonstration of the APEX Rules Engine
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-28
 * @version 1.0
 */
/**
 * AllDemosRunnerAlt provides a comprehensive demonstration of the APEX Rules Engine
 * by running all demo categories in the new organized structure.
 * 
 * This runner showcases the complete learning path:
 * 1. QuickStart - Getting started with basic concepts
 * 2. Fundamentals - Core rules engine concepts
 * 3. Patterns - Common implementation patterns
 * 4. Industry - Real-world industry applications
 * 5. Advanced - Advanced features and techniques
 * 
 * DEMO ORGANIZATION:
 * The demos are organized in a logical learning progression that takes users
 * from basic concepts to advanced real-world applications:
 * 
 * QUICKSTART (5-10 minutes)
 * - Basic validation rules
 * - Simple enrichment examples
 * - Getting started guide
 * 
 * FUNDAMENTALS (15-20 minutes)
 * - Rules: Validation, business logic, compliance
 * - Enrichments: Lookup, calculation, transformation
 * - Datasets: Inline, external, compound keys
 * 
 * PATTERNS (20-30 minutes)
 * - Lookups: Simple, conditional, nested, compound
 * - Calculations: Mathematical, string, date operations
 * - Validations: Format, business rules, cross-field
 * 
 * INDUSTRY (30-45 minutes)
 * - Financial Services: Settlement, trading, custody
 * - Real-world scenarios and use cases
 * - Regulatory compliance examples
 * 
 * ADVANCED (45+ minutes)
 * - Performance optimization techniques
 * - Integration patterns and best practices
 * - Complex multi-step scenarios
 * 
 * USAGE:
 * Run this class to execute all demos in sequence, or run individual
 * category runners for focused learning:
 * 
 * - QuickStartRunner: 5-minute introduction
 * - FundamentalsRunner: Core concepts deep dive
 * - PatternsRunner: Implementation patterns
 * - IndustryRunner: Real-world applications
 * - AdvancedRunner: Advanced techniques
 * 
 * @author apex-demo team
 * @version 1.0
 * @since 2025-08-24
 */
public class AllDemosRunnerAlt {
    
    private static final Logger logger = LoggerFactory.getLogger(AllDemosRunnerAlt.class);
    
    /**
     * Main entry point for running all APEX Rules Engine demos.
     * 
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        logger.info("=".repeat(80));
        logger.info("APEX RULES ENGINE - COMPREHENSIVE DEMO SUITE");
        logger.info("=".repeat(80));
        logger.info("");
        logger.info("Welcome to the APEX Rules Engine comprehensive demo suite!");
        logger.info("This demo will take you through a complete learning journey:");
        logger.info("");
        logger.info("1. QUICKSTART - Basic concepts and getting started (5-10 min)");
        logger.info("2. FUNDAMENTALS - Core rules engine concepts (15-20 min)");
        logger.info("3. PATTERNS - Common implementation patterns (20-30 min)");
        logger.info("4. INDUSTRY - Real-world industry applications (30-45 min)");
        logger.info("5. ADVANCED - Advanced features and techniques (45+ min)");
        logger.info("");
        logger.info("Total estimated time: 2-3 hours for complete walkthrough");
        logger.info("You can also run individual category runners for focused learning.");
        logger.info("");
        
        try {
            // Track overall execution time
            long startTime = System.currentTimeMillis();
            
            // Run each demo category in sequence
            runQuickStartDemos();
            runFundamentalsDemos();
            runPatternsDemos();
            runIndustryDemos();
            runAdvancedDemos();
            
            // Calculate and display total execution time
            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;
            
            logger.info("");
            logger.info("=".repeat(80));
            logger.info("DEMO SUITE COMPLETED SUCCESSFULLY!");
            logger.info("=".repeat(80));
            logger.info("Total execution time: {} ms ({} seconds)", totalTime, totalTime / 1000.0);
            logger.info("");
            logger.info("🎉 Congratulations! You've completed the full APEX Rules Engine demo suite.");
            logger.info("");
            logger.info("NEXT STEPS:");
            logger.info("- Explore the organized YAML configurations in src/main/resources/demos/");
            logger.info("- Try modifying the configurations to see how rules behave");
            logger.info("- Create your own rules based on the patterns you've learned");
            logger.info("- Check out the reference materials in src/main/resources/reference/");
            logger.info("");
            logger.info("For more information, visit: https://apex-rules-engine.dev");
            
        } catch (Exception e) {
            logger.error("Error running demo suite: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
    
    /**
     * Run QuickStart demos - basic concepts and getting started.
     */
    private static void runQuickStartDemos() {
        logger.info("-".repeat(60));
        logger.info("RUNNING QUICKSTART DEMOS");
        logger.info("-".repeat(60));
        
        try {
            QuickStartRunner.main(new String[]{});
        } catch (Exception e) {
            logger.warn("QuickStart demos encountered issues: {}", e.getMessage());
            logger.info("Continuing with remaining demos...");
        }
    }
    
    /**
     * Run Fundamentals demos - core rules engine concepts.
     */
    private static void runFundamentalsDemos() {
        logger.info("-".repeat(60));
        logger.info("RUNNING FUNDAMENTALS DEMOS");
        logger.info("-".repeat(60));
        
        try {
            FundamentalsRunner.main(new String[]{});
        } catch (Exception e) {
            logger.warn("Fundamentals demos encountered issues: {}", e.getMessage());
            logger.info("Continuing with remaining demos...");
        }
    }
    
    /**
     * Run Patterns demos - common implementation patterns.
     */
    private static void runPatternsDemos() {
        logger.info("-".repeat(60));
        logger.info("RUNNING PATTERNS DEMOS");
        logger.info("-".repeat(60));
        
        try {
            PatternsRunner.main(new String[]{});
        } catch (Exception e) {
            logger.warn("Patterns demos encountered issues: {}", e.getMessage());
            logger.info("Continuing with remaining demos...");
        }
    }
    
    /**
     * Run Industry demos - real-world industry applications.
     */
    private static void runIndustryDemos() {
        logger.info("-".repeat(60));
        logger.info("RUNNING INDUSTRY DEMOS");
        logger.info("-".repeat(60));
        
        try {
            IndustryRunner.main(new String[]{});
        } catch (Exception e) {
            logger.warn("Industry demos encountered issues: {}", e.getMessage());
            logger.info("Continuing with remaining demos...");
        }
    }
    
    /**
     * Run Advanced demos - advanced features and techniques.
     */
    private static void runAdvancedDemos() {
        logger.info("-".repeat(60));
        logger.info("RUNNING ADVANCED DEMOS");
        logger.info("-".repeat(60));
        
        try {
            AdvancedRunner.main(new String[]{});
        } catch (Exception e) {
            logger.warn("Advanced demos encountered issues: {}", e.getMessage());
            logger.info("Demo suite completed with some warnings.");
        }
    }
}
