#!/usr/bin/env python3
"""
Fix apex-demo classes to use the correct apex-core API.
This script updates the demo classes to use the actual available API methods.
"""

import os
import re

def fix_comprehensive_financial_settlement_demo():
    """Fix the ComprehensiveFinancialSettlementDemo to use correct API"""
    file_path = "apex-demo/src/main/java/dev/mars/apex/demo/financial/ComprehensiveFinancialSettlementDemo.java"
    
    if not os.path.exists(file_path):
        print(f"File not found: {file_path}")
        return False
    
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # Remove Spring Boot annotations and imports
        content = re.sub(r'import org\.springframework\.stereotype\.Component;\s*\n', '', content)
        content = re.sub(r'@Component\s*\n', '', content)
        
        # Fix the constructor and fields to use a simpler approach
        old_constructor = '''    private final RulesEngine rulesEngine;
    private final RulesEngineConfiguration configuration;

    public ComprehensiveFinancialSettlementDemo(RulesEngine rulesEngine, RulesEngineConfiguration configuration) {
        this.rulesEngine = rulesEngine;
        this.configuration = configuration;
    }'''
        
        new_constructor = '''    private final YamlRulesEngineService yamlService;

    public ComprehensiveFinancialSettlementDemo() {
        this.yamlService = new YamlRulesEngineService();
    }'''
        
        content = content.replace(old_constructor, new_constructor)
        
        # Fix the processTradeConfirmation method
        old_process_method = '''            // Process with rules engine
            ProcessingResult result = rulesEngine.processWithConfiguration(context, configPath);

            // Display validation results
            displayValidationResults(result.getValidationResults());'''
        
        new_process_method = '''            // Create rules engine from YAML configuration
            RulesEngine rulesEngine = yamlService.createRulesEngineFromClasspath(configPath);
            
            // Execute rules
            RuleResult result = rulesEngine.executeRulesForCategory("settlement", context);

            // Display results
            displayRuleResults(result);'''
        
        content = content.replace(old_process_method, new_process_method)
        
        # Fix the displayValidationResults method
        old_display_method = '''    private void displayValidationResults(Map<String, ValidationResult> validationResults) {
        LOGGER.info("\\n" + "=".repeat(60));
        LOGGER.info("VALIDATION RESULTS");
        LOGGER.info("=".repeat(60));

        validationResults.forEach((ruleId, result) -> {
            String status = result.isValid() ? "✓ PASSED" : "✗ FAILED";
            LOGGER.info("{}: {} - {}", status, ruleId, result.getMessage());
            
            if (!result.isValid() && result.getErrorDetails() != null) {
                LOGGER.warn("  Error Details: {}", result.getErrorDetails());
            }
        });
    }'''
        
        new_display_method = '''    private void displayRuleResults(RuleResult result) {
        LOGGER.info("\\n" + "=".repeat(60));
        LOGGER.info("RULE EXECUTION RESULTS");
        LOGGER.info("=".repeat(60));

        if (result.isMatched()) {
            LOGGER.info("✓ PASSED: Rules executed successfully");
            LOGGER.info("Matched Rule: {}", result.getMatchedRule() != null ? result.getMatchedRule().getId() : "Unknown");
        } else {
            LOGGER.info("✗ FAILED: No rules matched or execution failed");
        }
        
        if (result.getException() != null) {
            LOGGER.warn("Exception during execution: {}", result.getException().getMessage());
        }
    }'''
        
        content = content.replace(old_display_method, new_display_method)
        
        # Fix other method signatures that use wrong types
        content = re.sub(r'RuleExecutionResult', 'RuleResult', content)
        content = re.sub(r'ProcessingResult', 'RuleResult', content)
        
        # Add main method if it doesn't exist
        if 'public static void main(' not in content:
            main_method = '''
    /**
     * Main method for running the comprehensive financial settlement demo.
     * 
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        try {
            ComprehensiveFinancialSettlementDemo demo = new ComprehensiveFinancialSettlementDemo();
            demo.runComprehensiveDemo();
        } catch (Exception e) {
            LOGGER.error("Error running comprehensive financial settlement demo: {}", e.getMessage(), e);
        }
    }
'''
            # Insert before the last closing brace
            content = content.rstrip()
            if content.endswith('}'):
                content = content[:-1] + main_method + '\n}'
        
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)
        
        print(f"✅ Fixed ComprehensiveFinancialSettlementDemo API usage")
        return True
        
    except Exception as e:
        print(f"❌ Error fixing ComprehensiveFinancialSettlementDemo: {e}")
        return False

def fix_spring_boot_runner():
    """Fix or remove the Spring Boot runner that has missing dependencies"""
    file_path = "apex-demo/src/main/java/dev/mars/apex/demo/financial/FinancialSettlementDemoRunner.java"
    
    if not os.path.exists(file_path):
        print(f"File not found: {file_path}")
        return True  # Not an error if file doesn't exist
    
    try:
        # For now, let's just rename it to .bak to exclude it from compilation
        backup_path = file_path + ".bak"
        os.rename(file_path, backup_path)
        print(f"✅ Moved Spring Boot runner to backup: {backup_path}")
        return True
        
    except Exception as e:
        print(f"❌ Error handling Spring Boot runner: {e}")
        return False

def add_main_method_to_quickstart():
    """Add main method to QuickStartDemo if missing"""
    file_path = "apex-demo/src/main/java/dev/mars/apex/demo/QuickStartDemo.java"
    
    if not os.path.exists(file_path):
        print(f"File not found: {file_path}")
        return False
    
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # Check if main method already exists
        if 'public static void main(' in content:
            print("✅ QuickStartDemo already has main method")
            return True
        
        # Add main method before the last closing brace
        main_method = '''
    /**
     * Main method for running the QuickStart demo.
     * 
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        try {
            QuickStartDemo demo = new QuickStartDemo();
            demo.runQuickStartDemo();
        } catch (Exception e) {
            System.err.println("Error running QuickStart demo: " + e.getMessage());
            e.printStackTrace();
        }
    }
'''
        
        content = content.rstrip()
        if content.endswith('}'):
            content = content[:-1] + main_method + '\n}'
        
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)
        
        print(f"✅ Added main method to QuickStartDemo")
        return True
        
    except Exception as e:
        print(f"❌ Error adding main method to QuickStartDemo: {e}")
        return False

def main():
    print("=" * 60)
    print("FIXING APEX-DEMO API USAGE")
    print("=" * 60)
    print("")
    
    success_count = 0
    total_fixes = 3
    
    # Fix the main problematic classes
    if fix_comprehensive_financial_settlement_demo():
        success_count += 1
    
    if fix_spring_boot_runner():
        success_count += 1
    
    if add_main_method_to_quickstart():
        success_count += 1
    
    print("")
    print("=" * 60)
    print("API FIX SUMMARY")
    print("=" * 60)
    print(f"Fixes applied: {success_count}/{total_fixes}")
    
    if success_count == total_fixes:
        print("✅ All API fixes applied successfully!")
        print("")
        print("Changes made:")
        print("• Fixed ComprehensiveFinancialSettlementDemo to use correct apex-core API")
        print("• Moved Spring Boot runner to backup (missing dependencies)")
        print("• Added main method to QuickStartDemo")
        print("")
        print("Next steps:")
        print("1. Try compiling apex-demo again")
        print("2. Fix any remaining compilation issues")
        print("3. Test the demo classes")
    else:
        print("⚠ Some fixes failed - please review the errors above")

if __name__ == "__main__":
    main()
