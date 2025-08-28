package dev.mars.apex.playground.service;

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


import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Debug test to understand why enrichment lookup is failing
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-28
 * @version 1.0
 */
class EnrichmentDebugTest {

    @Test
    @DisplayName("Debug expression evaluation for enrichment lookup")
    void testExpressionEvaluation() {
        // Create the same data structure that would be passed to enrichment
        Map<String, Object> data = new HashMap<>();
        data.put("currency", "USD");
        data.put("amount", 1000.0);
        
        System.out.println("=== EXPRESSION EVALUATION DEBUG ===");
        System.out.println("Data structure: " + data);
        System.out.println("Data class: " + data.getClass().getName());
        System.out.println("Currency value: " + data.get("currency"));
        System.out.println("Currency class: " + data.get("currency").getClass().getName());
        
        // Test expression evaluator
        ExpressionEvaluatorService evaluator = new ExpressionEvaluatorService();

        // Create evaluation context like the enrichment processor does
        StandardEvaluationContext context = new StandardEvaluationContext(data);

        // Add map entries as variables (like YamlEnrichmentProcessor does)
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            context.setVariable(entry.getKey(), entry.getValue());
        }

        try {
            // Test different expression formats
            String[] expressions = {
                "#currency",
                "currency",
                "#data.currency",
                "data.currency",
                "${currency}",
                "#{currency}"
            };

            for (String expr : expressions) {
                try {
                    Object result = evaluator.evaluate(expr, context, Object.class);
                    System.out.println("Expression '" + expr + "' -> " + result + " (type: " + (result != null ? result.getClass().getSimpleName() : "null") + ")");
                } catch (Exception e) {
                    System.out.println("Expression '" + expr + "' -> ERROR: " + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            System.out.println("Failed to create expression evaluator: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("=== END DEBUG ===");
    }
}
