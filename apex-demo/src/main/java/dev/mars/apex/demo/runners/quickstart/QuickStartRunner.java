package dev.mars.apex.demo.runners.quickstart;

import dev.mars.apex.demo.QuickStartDemo;
import dev.mars.apex.demo.support.util.ResourcePathResolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Scanner;

/**
 * QuickStartRunner provides a 5-10 minute introduction to the APEX Rules Engine.
 * 
 * This runner demonstrates the most essential concepts needed to get started:
 * - Basic validation rules
 * - Simple enrichment patterns
 * - YAML configuration basics
 * - Core API usage
 * 
 * LEARNING OBJECTIVES:
 * After completing this quickstart, users will understand:
 * 1. How to write basic validation rules using SpEL expressions
 * 2. How to configure simple enrichments for data lookup
 * 3. How to load and use YAML configurations
 * 4. Basic patterns for rule execution and result handling
 * 
 * DEMO CONTENT:
 * - Customer validation example (age, email, phone format)
 * - Currency enrichment lookup
 * - Basic error handling and result processing
 * - Introduction to the organized demo structure
 * 
 * TIME ESTIMATE: 5-10 minutes
 * 
 * NEXT STEPS:
 * After completing this quickstart, proceed to:
 * - FundamentalsRunner for deeper concept exploration
 * - PatternsRunner for implementation patterns
 * 
 * @author apex-demo team
 * @version 1.0
 * @since 2025-08-24
 */
public class QuickStartRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(QuickStartRunner.class);
    
    /**
     * Main entry point for QuickStart demos.
     * 
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        logger.info("╔" + "═".repeat(78) + "╗");
        logger.info("║" + " ".repeat(25) + "APEX RULES ENGINE QUICKSTART" + " ".repeat(25) + "║");
        logger.info("╚" + "═".repeat(78) + "╝");
        logger.info("");
        logger.info("Welcome to the APEX Rules Engine! 🚀");
        logger.info("");
        logger.info("This 5-10 minute quickstart will introduce you to:");
        logger.info("• Basic validation rules using SpEL expressions");
        logger.info("• Simple data enrichment patterns");
        logger.info("• YAML configuration fundamentals");
        logger.info("• Core API usage patterns");
        logger.info("");
        
        try {
            // Show the new organized structure
            showOrganizedStructure();
            
            // Run the core QuickStart demo
            runCoreQuickStartDemo();
            
            // Show YAML configuration example
            showYamlConfigurationExample();
            
            // Provide next steps guidance
            showNextSteps();
            
            logger.info("✅ QuickStart completed successfully!");
            logger.info("You're now ready to explore more advanced features.");
            
        } catch (Exception e) {
            logger.error("QuickStart demo failed: {}", e.getMessage(), e);
            throw new RuntimeException("QuickStart demo failed", e);
        }
    }
    
    /**
     * Show the new organized demo structure.
     */
    private static void showOrganizedStructure() {
        logger.info("📁 NEW ORGANIZED DEMO STRUCTURE");
        logger.info("─".repeat(40));
        logger.info("The APEX demos are now organized for better learning:");
        logger.info("");
        logger.info("demos/");
        logger.info("├── quickstart/          ← You are here! (5-10 min)");
        logger.info("├── fundamentals/        ← Core concepts (15-20 min)");
        logger.info("│   ├── rules/           ← Validation & business logic");
        logger.info("│   ├── enrichments/     ← Data transformation");
        logger.info("│   └── datasets/        ← Reference data management");
        logger.info("├── patterns/            ← Implementation patterns (20-30 min)");
        logger.info("│   ├── lookups/         ← Data lookup strategies");
        logger.info("│   ├── calculations/    ← Mathematical operations");
        logger.info("│   └── validations/     ← Validation patterns");
        logger.info("├── industry/            ← Real-world applications (30-45 min)");
        logger.info("│   └── financial-services/  ← Financial industry examples");
        logger.info("└── advanced/            ← Advanced techniques (45+ min)");
        logger.info("    ├── performance/     ← Optimization strategies");
        logger.info("    ├── integration/     ← System integration");
        logger.info("    └── complex-scenarios/  ← Multi-step workflows");
        logger.info("");
        logger.info("This structure provides a clear learning path from basic to advanced!");
        logger.info("");
    }
    
    /**
     * Run the core QuickStart demo.
     */
    private static void runCoreQuickStartDemo() {
        logger.info("🎯 RUNNING CORE QUICKSTART DEMO");
        logger.info("─".repeat(40));
        logger.info("Executing the main QuickStart demonstration...");
        logger.info("");
        
        try {
            // Run the existing QuickStartDemo
            QuickStartDemo.main(new String[]{});
            
        } catch (Exception e) {
            logger.warn("Core QuickStart demo encountered issues: {}", e.getMessage());
            logger.info("This is expected if core dependencies are not available.");
            logger.info("The demo structure and concepts are still valid!");
        }
    }
    
    /**
     * Show YAML configuration example from the quickstart.
     */
    private static void showYamlConfigurationExample() {
        logger.info("📄 YAML CONFIGURATION EXAMPLE");
        logger.info("─".repeat(40));
        logger.info("Here's a sample of the YAML configuration used in quickstart:");
        logger.info("");
        
        try {
            // Load and display a portion of the quickstart YAML
            String yamlPath = "demos/quickstart/quick-start.yaml";
            InputStream yamlStream = QuickStartRunner.class.getClassLoader().getResourceAsStream(yamlPath);
            
            if (yamlStream != null) {
                Scanner scanner = new Scanner(yamlStream);
                int lineCount = 0;
                
                logger.info("File: {}", yamlPath);
                logger.info("─".repeat(60));
                
                while (scanner.hasNextLine() && lineCount < 20) {
                    String line = scanner.nextLine();
                    logger.info("{:2d}: {}", lineCount + 1, line);
                    lineCount++;
                }
                
                if (scanner.hasNextLine()) {
                    logger.info("... (truncated for brevity)");
                }
                
                scanner.close();
                yamlStream.close();
                
            } else {
                logger.info("YAML configuration file not found: {}", yamlPath);
                logger.info("This is expected if resources are not available in the current context.");
            }
            
        } catch (Exception e) {
            logger.info("Could not load YAML example: {}", e.getMessage());
            logger.info("This is expected in some environments.");
        }
        
        logger.info("");
        logger.info("Key YAML concepts demonstrated:");
        logger.info("• metadata: Configuration information and versioning");
        logger.info("• rules: Validation and business logic definitions");
        logger.info("• enrichments: Data transformation and lookup rules");
        logger.info("• rule-chains: Orchestration of multiple rules");
        logger.info("");
    }
    
    /**
     * Show next steps and learning path guidance.
     */
    private static void showNextSteps() {
        logger.info("🎓 NEXT STEPS IN YOUR LEARNING JOURNEY");
        logger.info("─".repeat(40));
        logger.info("Now that you've completed the quickstart, here's your learning path:");
        logger.info("");
        logger.info("IMMEDIATE NEXT STEPS (Choose one):");
        logger.info("1. 📚 FundamentalsRunner - Deep dive into core concepts (15-20 min)");
        logger.info("   • Learn about rules, enrichments, and datasets in detail");
        logger.info("   • Understand the rules engine architecture");
        logger.info("   • Explore different types of validation and enrichment");
        logger.info("");
        logger.info("2. 🔧 PatternsRunner - Learn implementation patterns (20-30 min)");
        logger.info("   • Master lookup strategies and techniques");
        logger.info("   • Understand calculation and validation patterns");
        logger.info("   • See common real-world implementation approaches");
        logger.info("");
        logger.info("ADVANCED LEARNING:");
        logger.info("3. 🏢 IndustryRunner - Real-world applications (30-45 min)");
        logger.info("   • Financial services examples and use cases");
        logger.info("   • Regulatory compliance scenarios");
        logger.info("   • Production-ready configurations");
        logger.info("");
        logger.info("4. 🚀 AdvancedRunner - Advanced techniques (45+ min)");
        logger.info("   • Performance optimization strategies");
        logger.info("   • Complex integration patterns");
        logger.info("   • Multi-step workflow orchestration");
        logger.info("");
        logger.info("HANDS-ON EXPLORATION:");
        logger.info("• Examine YAML files in src/main/resources/demos/");
        logger.info("• Try modifying configurations and re-running demos");
        logger.info("• Create your own rules based on the patterns you've learned");
        logger.info("");
        logger.info("Run individual runners: java -cp ... dev.mars.apex.demo.runners.fundamentals.FundamentalsRunner");
        logger.info("");
    }
}
