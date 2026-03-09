package dev.mars.apex.engine.core;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link FieldMappingProcessor}.
 *
 * <p>Covers code evaluation (constant and SpEL), field mapping application,
 * and error handling with correct APEX error code classification.</p>
 *
 * @author Mark A Ray-Smith
 * @since 2026-02-27
 */
@DisplayName("FieldMappingProcessor Tests")
@ExtendWith(ColoredTestOutputExtension.class)
class FieldMappingProcessorTest {

    private FieldMappingProcessor processor;
    private StandardEvaluationContext context;

    @BeforeAll
    static void classSetUp() {
        MDC.put("testContext", "[EXPECTED] ");
        LoggerFactory.getLogger(FieldMappingProcessorTest.class)
                .info("[INTENTIONAL-FAILURE-TEST-CLASS-START] FieldMappingProcessorTest intentionally triggers ERROR/WARN logs");
    }

    @BeforeEach
    void setUp() {
        processor = new FieldMappingProcessor(SpelParserHolder.INSTANCE);
        context = new StandardEvaluationContext();
        context.setVariable("amount", 1500.0);
        context.setVariable("currency", "EUR");
        context.setVariable("status", "ACTIVE");
    }

    // ========================================================================
    // evaluateCode() Tests
    // ========================================================================

    @Nested
    @DisplayName("evaluateCode()")
    class EvaluateCodeTests {

        @Test
        @DisplayName("Should return constant string as-is")
        void testConstantString() {
            String result = processor.evaluateCode("SUCCESS_CODE", context);
            assertEquals("SUCCESS_CODE", result);
        }

        @Test
        @DisplayName("Should evaluate SpEL expression starting with #")
        void testSpelExpression() {
            String result = processor.evaluateCode("#amount > 1000 ? 'HIGH' : 'LOW'", context);
            assertEquals("HIGH", result);
        }

        @Test
        @DisplayName("Should evaluate SpEL variable reference")
        void testSpelVariableReference() {
            String result = processor.evaluateCode("#currency", context);
            assertEquals("EUR", result);
        }

        @Test
        @DisplayName("Should return null for null expression")
        void testNullExpression() {
            String result = processor.evaluateCode(null, context);
            assertNull(result);
        }

        @Test
        @DisplayName("Should return null for empty expression")
        void testEmptyExpression() {
            String result = processor.evaluateCode("", context);
            assertNull(result);
        }

        @Test
        @DisplayName("Should return null for whitespace-only expression")
        void testWhitespaceExpression() {
            String result = processor.evaluateCode("   ", context);
            assertNull(result);
        }

        @Test
        @DisplayName("Should return null on SpEL parse failure (invalid expression)")
        void testInvalidSpelExpression() {
            // Invalid SpEL — triggers SpelParseException → APEX-RULE-001
            String result = processor.evaluateCode("#amount >>>> 100", context);
            assertNull(result, "Invalid SpEL expression should return null gracefully");
        }

        @Test
        @DisplayName("Should return null on undefined variable reference")
        void testUndefinedVariable() {
            // #nonExistent evaluates to null in SpEL, toString() on null returns null
            String result = processor.evaluateCode("#nonExistent", context);
            assertNull(result, "Undefined variable should return null");
        }

        @Test
        @DisplayName("Should handle SpEL expression returning null")
        void testSpelReturningNull() {
            context.setVariable("nullValue", null);
            String result = processor.evaluateCode("#nullValue", context);
            assertNull(result, "SpEL expression returning null should return null");
        }

        @Test
        @DisplayName("Should handle numeric SpEL result as string")
        void testNumericResult() {
            String result = processor.evaluateCode("#amount", context);
            assertEquals("1500.0", result);
        }
    }

    // ========================================================================
    // applyFieldMappings() Tests
    // ========================================================================

    @Nested
    @DisplayName("applyFieldMappings()")
    class ApplyFieldMappingsTests {

        @Test
        @DisplayName("Should apply single mapping with SpEL expression")
        void testSingleMapping() {
            Map<String, Object> enrichedData = new HashMap<>();
            processor.applyFieldMappings(
                    List.of("transactionCurrency = #currency"),
                    context, enrichedData, null, null);

            assertEquals("EUR", enrichedData.get("transactionCurrency"));
        }

        @Test
        @DisplayName("Should apply multiple mappings")
        void testMultipleMappings() {
            Map<String, Object> enrichedData = new HashMap<>();
            processor.applyFieldMappings(
                    List.of("amt = #amount", "ccy = #currency", "sts = #status"),
                    context, enrichedData, null, null);

            assertEquals(1500.0, enrichedData.get("amt"));
            assertEquals("EUR", enrichedData.get("ccy"));
            assertEquals("ACTIVE", enrichedData.get("sts"));
        }

        @Test
        @DisplayName("Should make success_code available in mapping expressions")
        void testSuccessCodeAvailable() {
            Map<String, Object> enrichedData = new HashMap<>();
            processor.applyFieldMappings(
                    List.of("code = #success_code"),
                    context, enrichedData, "TXN-OK-200", null);

            assertEquals("TXN-OK-200", enrichedData.get("code"));
        }

        @Test
        @DisplayName("Should make error_code available in mapping expressions")
        void testErrorCodeAvailable() {
            Map<String, Object> enrichedData = new HashMap<>();
            processor.applyFieldMappings(
                    List.of("errCode = #error_code"),
                    context, enrichedData, null, "VALIDATION-FAIL-001");

            assertEquals("VALIDATION-FAIL-001", enrichedData.get("errCode"));
        }

        @Test
        @DisplayName("Should handle null mapToField gracefully")
        void testNullMapToField() {
            Map<String, Object> enrichedData = new HashMap<>();
            // Should not throw
            processor.applyFieldMappings(null, context, enrichedData, null, null);
            assertTrue(enrichedData.isEmpty());
        }

        @Test
        @DisplayName("Should handle conditional SpEL in mapping")
        void testConditionalMapping() {
            Map<String, Object> enrichedData = new HashMap<>();
            processor.applyFieldMappings(
                    List.of("tier = #amount > 1000 ? 'PREMIUM' : 'STANDARD'"),
                    context, enrichedData, null, null);

            assertEquals("PREMIUM", enrichedData.get("tier"));
        }

        @Test
        @DisplayName("Should skip invalid mapping format (no equals sign) with WARN")
        void testInvalidMappingFormat() {
            Map<String, Object> enrichedData = new HashMap<>();
            // Missing '=' — should log WARN and skip, not throw
            processor.applyFieldMappings(
                    List.of("invalidMappingNoEquals"),
                    context, enrichedData, null, null);

            assertTrue(enrichedData.isEmpty(),
                    "Invalid mapping format should be skipped without adding data");
        }

        @Test
        @DisplayName("Should continue processing after one mapping fails")
        void testContinueAfterFailure() {
            Map<String, Object> enrichedData = new HashMap<>();
            processor.applyFieldMappings(
                    List.of(
                            "good = #currency",
                            "bad = #nonExistent.someMethod()",   // Will fail — method call on null
                            "alsoGood = #amount"
                    ),
                    context, enrichedData, null, null);

            // The first and third mappings should succeed even if the second fails
            assertEquals("EUR", enrichedData.get("good"));
            assertEquals(1500.0, enrichedData.get("alsoGood"));
        }

        @Test
        @DisplayName("Should apply mapping with constant expression")
        void testConstantExpressionMapping() {
            Map<String, Object> enrichedData = new HashMap<>();
            processor.applyFieldMappings(
                    List.of("label = 'FIXED_VALUE'"),
                    context, enrichedData, null, null);

            assertEquals("FIXED_VALUE", enrichedData.get("label"));
        }
    }
}
