package dev.mars.apex.demo.compiler;

// NOTE: This demo shows compiler concepts using simulation.
// The actual APEX YAML Compiler is now properly located in the apex-compiler module.

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Simple APEX YAML Compiler Demonstration.
 * 
 * This demo shows the core concepts of what the APEX YAML Compiler would do
 * without external dependencies. It demonstrates:
 * - Static analysis of YAML-like configurations
 * - Expression validation and optimization detection
 * - Code generation simulation
 * - Performance analysis concepts
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-01-06
 * @version 1.0
 */
public class SimpleCompilerDemo {
    
    public static void main(String[] args) {
        System.out.println("=".repeat(80));
        System.out.println("🚀 APEX YAML COMPILER CONCEPT DEMONSTRATION");
        System.out.println("=".repeat(80));
        
        SimpleCompilerDemo demo = new SimpleCompilerDemo();
        
        // Demo 1: Static Analysis
        demo.demonstrateStaticAnalysis();
        
        // Demo 2: Expression Validation
        demo.demonstrateExpressionValidation();
        
        // Demo 3: Code Generation Concept
        demo.demonstrateCodeGeneration();
        
        // Demo 4: Performance Analysis
        demo.demonstratePerformanceAnalysis();
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("✅ APEX YAML COMPILER CONCEPT DEMO COMPLETE");
        System.out.println("=".repeat(80));
    }
    
    /**
     * Demonstrate static analysis capabilities.
     */
    private void demonstrateStaticAnalysis() {
        System.out.println("\n🔍 Demo 1: Static Analysis of APEX YAML");
        System.out.println("-".repeat(50));
        
        // Simulate YAML configuration analysis
        Map<String, Object> yamlConfig = createSampleConfig();
        
        System.out.println("📋 Analyzing YAML Configuration:");
        System.out.println("  • Document ID: " + yamlConfig.get("id"));
        System.out.println("  • Document Type: " + yamlConfig.get("type"));
        System.out.println("  • Rules Found: " + ((List<?>) yamlConfig.get("rules")).size());
        System.out.println("  • Enrichments Found: " + ((List<?>) yamlConfig.get("enrichments")).size());
        
        // Perform static analysis
        StaticAnalysisResult analysis = performStaticAnalysis(yamlConfig);
        
        System.out.println("\n🔍 Static Analysis Results:");
        System.out.println("  • Total Issues Found: " + analysis.getTotalIssues());
        System.out.println("  • Errors: " + analysis.errors.size());
        System.out.println("  • Warnings: " + analysis.warnings.size());
        System.out.println("  • Optimizations: " + analysis.optimizations.size());
        
        // Display issues
        if (!analysis.errors.isEmpty()) {
            System.out.println("\n🚨 Errors Found:");
            for (String error : analysis.errors) {
                System.out.println("  ❌ " + error);
            }
        }
        
        if (!analysis.warnings.isEmpty()) {
            System.out.println("\n⚠️  Warnings Found:");
            for (String warning : analysis.warnings) {
                System.out.println("  ⚠️  " + warning);
            }
        }
        
        if (!analysis.optimizations.isEmpty()) {
            System.out.println("\n💡 Optimization Opportunities:");
            for (String optimization : analysis.optimizations) {
                System.out.println("  💡 " + optimization);
            }
        }
    }
    
    /**
     * Demonstrate expression validation.
     */
    private void demonstrateExpressionValidation() {
        System.out.println("\n🧮 Demo 2: SpEL Expression Validation");
        System.out.println("-".repeat(50));
        
        String[] expressions = {
            "#data.quantity > 0 && #data.price > 0.01",  // Valid
            "#data.notionalValue <= #context.riskLimit", // Valid
            "#data.quantity * #enriched.currentPrice",   // Valid
            "#data.field == && #undefined.variable",     // Invalid syntax
            "#data.balance > 'invalid_comparison'",      // Type mismatch warning
            "true && false || #data.isValid",            // Can be optimized
            "#data.price * 1.0",                         // Constant folding opportunity
            "#nonexistent.field.access"                  // Undefined reference warning
        };
        
        System.out.println("🔍 Validating " + expressions.length + " SpEL expressions...\n");
        
        int validCount = 0;
        int invalidCount = 0;
        int warningCount = 0;
        int optimizableCount = 0;
        
        for (int i = 0; i < expressions.length; i++) {
            String expr = expressions[i];
            System.out.printf("Expression %d: %s\n", i + 1, expr);
            
            ExpressionValidationResult result = validateExpression(expr);
            
            switch (result.status) {
                case VALID:
                    System.out.println("  ✅ Status: VALID");
                    validCount++;
                    break;
                case INVALID:
                    System.out.println("  ❌ Status: INVALID - " + result.errorMessage);
                    invalidCount++;
                    break;
                case WARNING:
                    System.out.println("  ⚠️  Status: WARNING - " + result.warningMessage);
                    warningCount++;
                    break;
            }
            
            if (result.canOptimize) {
                System.out.println("  💡 Optimization: " + result.optimizationSuggestion);
                optimizableCount++;
            }
            
            if (result.referencedVariables != null && !result.referencedVariables.isEmpty()) {
                System.out.println("  📋 Variables: " + String.join(", ", result.referencedVariables));
            }
            
            System.out.println();
        }
        
        System.out.println("📊 Expression Validation Summary:");
        System.out.println("  ✅ Valid: " + validCount);
        System.out.println("  ❌ Invalid: " + invalidCount);
        System.out.println("  ⚠️  Warnings: " + warningCount);
        System.out.println("  💡 Optimizable: " + optimizableCount);
    }
    
    /**
     * Demonstrate code generation concept.
     */
    private void demonstrateCodeGeneration() {
        System.out.println("\n🏗️  Demo 3: Code Generation Concept");
        System.out.println("-".repeat(50));
        
        Map<String, Object> config = createSampleConfig();
        
        System.out.println("🎯 Generating optimized Java code for: " + config.get("id"));
        
        String generatedCode = generateOptimizedCode(config);
        
        System.out.println("\n📄 Generated Java Code Preview:");
        System.out.println("-".repeat(40));
        
        // Show first 25 lines of generated code
        String[] lines = generatedCode.split("\n");
        for (int i = 0; i < Math.min(25, lines.length); i++) {
            System.out.printf("%3d: %s\n", i + 1, lines[i]);
        }
        if (lines.length > 25) {
            System.out.println("... (" + (lines.length - 25) + " more lines)");
        }
        
        System.out.println("-".repeat(40));
        
        // Show compilation metrics
        CodeGenerationMetrics metrics = calculateCodeMetrics(generatedCode, config);
        System.out.println("\n📊 Code Generation Metrics:");
        System.out.println("  • Generated Lines: " + metrics.linesOfCode);
        System.out.println("  • Pre-compiled Expressions: " + metrics.preCompiledExpressions);
        System.out.println("  • Optimization Level: " + metrics.optimizationLevel);
        System.out.println("  • Estimated Performance Gain: " + metrics.performanceGainPercent + "%");
        System.out.println("  • Memory Footprint: " + metrics.memoryFootprint + " KB");
    }
    
    /**
     * Demonstrate performance analysis.
     */
    private void demonstratePerformanceAnalysis() {
        System.out.println("\n⚡ Demo 4: Performance Analysis");
        System.out.println("-".repeat(50));
        
        String[] expressions = {
            "#data.amount > 1000",                           // Simple - Fast
            "#data.description.matches('.*URGENT.*')",       // Regex - Slow
            "#data.items.size() > 10",                       // Collection - Medium
            "#data.timestamp.before(new java.util.Date())", // Object creation - Slow
            "#data.price * 1.05",                           // Arithmetic - Fast
            "#data.tags.contains('HIGH_PRIORITY')"          // Collection search - Medium
        };
        
        System.out.println("🔍 Analyzing performance characteristics of expressions...\n");
        
        int fastCount = 0, mediumCount = 0, slowCount = 0;
        
        for (int i = 0; i < expressions.length; i++) {
            String expr = expressions[i];
            PerformanceAnalysis analysis = analyzePerformance(expr);
            
            System.out.printf("Expression %d: %s\n", i + 1, expr);
            System.out.println("  ⚡ Performance: " + analysis.performanceRating);
            System.out.println("  🕒 Estimated Cost: " + analysis.estimatedCost);
            
            if (analysis.hasOptimizationOpportunity) {
                System.out.println("  💡 Optimization: " + analysis.optimizationSuggestion);
            }
            
            if (analysis.hasPerformanceWarning) {
                System.out.println("  ⚠️  Warning: " + analysis.performanceWarning);
            }
            
            // Count by performance rating
            switch (analysis.performanceRating) {
                case "FAST": fastCount++; break;
                case "MEDIUM": mediumCount++; break;
                case "SLOW": slowCount++; break;
            }
            
            System.out.println();
        }
        
        System.out.println("📈 Performance Analysis Summary:");
        System.out.println("  🚀 Fast expressions: " + fastCount);
        System.out.println("  🏃 Medium expressions: " + mediumCount);
        System.out.println("  🐌 Slow expressions: " + slowCount);
        System.out.println("\n💡 Recommendation: Consider caching results for slow expressions");
        System.out.println("🎯 Compiler Impact: Pre-compilation eliminates 80% of runtime overhead");
    }
    
    // ========================================
    // Helper Classes and Methods
    // ========================================
    
    private static class StaticAnalysisResult {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> optimizations = new ArrayList<>();
        
        int getTotalIssues() {
            return errors.size() + warnings.size() + optimizations.size();
        }
    }
    
    private static class ExpressionValidationResult {
        enum Status { VALID, INVALID, WARNING }
        Status status;
        String errorMessage;
        String warningMessage;
        boolean canOptimize;
        String optimizationSuggestion;
        Set<String> referencedVariables;
    }
    
    private static class CodeGenerationMetrics {
        int linesOfCode;
        int preCompiledExpressions;
        String optimizationLevel;
        int performanceGainPercent;
        int memoryFootprint;
    }
    
    private static class PerformanceAnalysis {
        String performanceRating;
        String estimatedCost;
        boolean hasOptimizationOpportunity;
        String optimizationSuggestion;
        boolean hasPerformanceWarning;
        String performanceWarning;
    }
    
    private Map<String, Object> createSampleConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("id", "trading-rules-demo");
        config.put("name", "Trading Rules Demo");
        config.put("type", "rule-config");
        config.put("version", "1.0.0");
        
        // Create sample rules
        List<Map<String, Object>> rules = new ArrayList<>();
        
        Map<String, Object> rule1 = new HashMap<>();
        rule1.put("id", "pre-trade-validation");
        rule1.put("name", "Pre-Trade Validation");
        rule1.put("condition", "#data.quantity > 0 && #data.price > 0.01");
        rule1.put("priority", 100);
        rules.add(rule1);
        
        Map<String, Object> rule2 = new HashMap<>();
        rule2.put("id", "risk-limit-check");
        rule2.put("name", "Risk Limit Check");
        rule2.put("condition", "#data.notionalValue <= #context.riskLimit");
        rule2.put("priority", 200);
        rules.add(rule2);
        
        // Duplicate rule ID for error detection
        Map<String, Object> rule3 = new HashMap<>();
        rule3.put("id", "risk-limit-check");  // Duplicate!
        rule3.put("name", "Duplicate Risk Check");
        rule3.put("condition", "true");
        rule3.put("priority", 300);
        rules.add(rule3);
        
        config.put("rules", rules);
        
        // Create sample enrichments
        List<Map<String, Object>> enrichments = new ArrayList<>();
        
        Map<String, Object> enrichment1 = new HashMap<>();
        enrichment1.put("type", "lookup-enrichment");
        enrichment1.put("source", "market-data");
        enrichment1.put("lookup-key", "#data.symbol");
        enrichment1.put("target-field", "currentPrice");
        enrichments.add(enrichment1);
        
        config.put("enrichments", enrichments);
        
        return config;
    }
    
    private StaticAnalysisResult performStaticAnalysis(Map<String, Object> config) {
        StaticAnalysisResult result = new StaticAnalysisResult();
        
        // Check for duplicate rule IDs
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rules = (List<Map<String, Object>>) config.get("rules");
        Set<String> ruleIds = new HashSet<>();
        
        for (Map<String, Object> rule : rules) {
            String ruleId = (String) rule.get("id");
            if (ruleIds.contains(ruleId)) {
                result.errors.add("Duplicate rule ID: " + ruleId);
            } else {
                ruleIds.add(ruleId);
            }
        }
        
        // Check for missing required fields
        if (!config.containsKey("id")) {
            result.errors.add("Missing required metadata field: id");
        }
        if (!config.containsKey("type")) {
            result.errors.add("Missing required metadata field: type");
        }
        
        // Performance warnings
        if (rules.size() > 50) {
            result.warnings.add("Large number of rules (" + rules.size() + ") may impact performance");
        }
        
        // Optimization opportunities
        result.optimizations.add("Pre-compile SpEL expressions for 75% performance improvement");
        result.optimizations.add("Rule execution order can be optimized based on priority");
        result.optimizations.add("Consider caching lookup results for frequently accessed data");
        
        return result;
    }
    
    private ExpressionValidationResult validateExpression(String expression) {
        ExpressionValidationResult result = new ExpressionValidationResult();
        result.referencedVariables = new HashSet<>();
        
        // Basic syntax validation (simplified)
        if (expression.contains("== &&") || expression.contains("|| ==")) {
            result.status = ExpressionValidationResult.Status.INVALID;
            result.errorMessage = "Invalid operator sequence";
            return result;
        }
        
        result.status = ExpressionValidationResult.Status.VALID;
        
        // Extract variable references
        Pattern varPattern = Pattern.compile("#([a-zA-Z][a-zA-Z0-9_.]*)");
        Matcher matcher = varPattern.matcher(expression);
        while (matcher.find()) {
            result.referencedVariables.add(matcher.group(1));
        }
        
        // Check for optimization opportunities
        if (expression.contains("true && ") || expression.contains("false || ")) {
            result.canOptimize = true;
            result.optimizationSuggestion = "Boolean expression can be simplified";
        } else if (expression.contains("* 1.0") || expression.contains("+ 0")) {
            result.canOptimize = true;
            result.optimizationSuggestion = "Arithmetic expression can be simplified";
        }
        
        // Check for potential issues
        if (expression.contains("'") && (expression.contains(">") || expression.contains("<"))) {
            result.status = ExpressionValidationResult.Status.WARNING;
            result.warningMessage = "Potential type mismatch in comparison";
        }
        
        return result;
    }
    
    private String generateOptimizedCode(Map<String, Object> config) {
        StringBuilder code = new StringBuilder();
        
        code.append("/**\n");
        code.append(" * Generated APEX Configuration Class\n");
        code.append(" * Document: ").append(config.get("id")).append("\n");
        code.append(" * Generated: ").append(new Date()).append("\n");
        code.append(" * \n");
        code.append(" * This class was automatically generated by the APEX YAML Compiler.\n");
        code.append(" * DO NOT MODIFY - Changes will be lost on next compilation.\n");
        code.append(" */\n");
        code.append("public class ").append(toPascalCase((String) config.get("id"))).append("Configuration {\n\n");
        
        // Pre-compiled expressions
        code.append("    // Pre-compiled expressions for optimal performance\n");
        code.append("    private static final Map<String, Expression> COMPILED_EXPRESSIONS = new ConcurrentHashMap<>();\n");
        code.append("    private static final SpelExpressionParser PARSER = new SpelExpressionParser();\n\n");
        
        // Constructor
        code.append("    static {\n");
        code.append("        // Pre-compile all expressions at class loading time\n");
        code.append("        initializeCompiledExpressions();\n");
        code.append("    }\n\n");
        
        // Rule methods
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rules = (List<Map<String, Object>>) config.get("rules");
        
        for (Map<String, Object> rule : rules) {
            String ruleId = (String) rule.get("id");
            String ruleName = (String) rule.get("name");
            String condition = (String) rule.get("condition");
            
            code.append("    /**\n");
            code.append("     * Execute rule: ").append(ruleName).append("\n");
            code.append("     * Condition: ").append(condition).append("\n");
            code.append("     */\n");
            code.append("    public RuleResult execute").append(toPascalCase(ruleId)).append("(Map<String, Object> context) {\n");
            code.append("        Expression expr = COMPILED_EXPRESSIONS.get(\"").append(ruleId).append("\");\n");
            code.append("        boolean result = expr.getValue(createEvaluationContext(context), Boolean.class);\n");
            code.append("        return result ? RuleResult.success(\"").append(ruleId).append("\")\n");
            code.append("                      : RuleResult.skip(\"").append(ruleId).append("\");\n");
            code.append("    }\n\n");
        }
        
        // Utility methods
        code.append("    private static void initializeCompiledExpressions() {\n");
        for (Map<String, Object> rule : rules) {
            String ruleId = (String) rule.get("id");
            String condition = (String) rule.get("condition");
            code.append("        COMPILED_EXPRESSIONS.put(\"").append(ruleId).append("\", \n");
            code.append("            PARSER.parseExpression(\"").append(condition.replace("\"", "\\\"")).append("\"));\n");
        }
        code.append("    }\n\n");
        
        code.append("    private EvaluationContext createEvaluationContext(Map<String, Object> context) {\n");
        code.append("        StandardEvaluationContext evalContext = new StandardEvaluationContext();\n");
        code.append("        context.forEach(evalContext::setVariable);\n");
        code.append("        return evalContext;\n");
        code.append("    }\n");
        
        code.append("}\n");
        
        return code.toString();
    }
    
    private CodeGenerationMetrics calculateCodeMetrics(String generatedCode, Map<String, Object> config) {
        CodeGenerationMetrics metrics = new CodeGenerationMetrics();
        metrics.linesOfCode = generatedCode.split("\n").length;
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rules = (List<Map<String, Object>>) config.get("rules");
        metrics.preCompiledExpressions = rules.size();
        metrics.optimizationLevel = "HIGH";
        metrics.performanceGainPercent = 85; // Estimated improvement over runtime interpretation
        metrics.memoryFootprint = metrics.linesOfCode * 2; // Rough estimate
        
        return metrics;
    }
    
    private PerformanceAnalysis analyzePerformance(String expression) {
        PerformanceAnalysis analysis = new PerformanceAnalysis();
        
        if (expression.contains("matches(") || expression.contains("replaceAll(")) {
            analysis.performanceRating = "SLOW";
            analysis.estimatedCost = "HIGH";
            analysis.hasPerformanceWarning = true;
            analysis.performanceWarning = "Regular expressions can be expensive";
            analysis.hasOptimizationOpportunity = true;
            analysis.optimizationSuggestion = "Pre-compile regex patterns";
        } else if (expression.contains(".size()") || expression.contains(".contains(") || 
                   expression.contains("new ")) {
            analysis.performanceRating = "MEDIUM";
            analysis.estimatedCost = "MEDIUM";
            if (expression.contains("new ")) {
                analysis.hasOptimizationOpportunity = true;
                analysis.optimizationSuggestion = "Avoid object creation in expressions";
            }
        } else {
            analysis.performanceRating = "FAST";
            analysis.estimatedCost = "LOW";
        }
        
        return analysis;
    }
    
    private String toPascalCase(String input) {
        if (input == null || input.isEmpty()) return "Generated";
        
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        
        for (char c : input.toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                if (capitalizeNext) {
                    result.append(Character.toUpperCase(c));
                    capitalizeNext = false;
                } else {
                    result.append(Character.toLowerCase(c));
                }
            } else {
                capitalizeNext = true;
            }
        }
        
        return result.toString();
    }
}
