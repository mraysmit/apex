package dev.mars.apex.core.util;

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

import dev.mars.apex.core.config.yaml.YamlConfigurationException;
import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TestErrorContext utility.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2026-01-24
 */
@ExtendWith({ColoredTestOutputExtension.class, TestClassLoggingExtension.class})
@DisplayName("TestErrorContext Tests")
class TestErrorContextTest {
    
    private static final Logger logger = LoggerFactory.getLogger(TestErrorContextTest.class);
    
    @Nested
    @DisplayName("expectingException Tests")
    class ExpectingExceptionTests {
        
        @Test
        @DisplayName("Should catch expected exception and return it")
        void shouldCatchExpectedException() {
            logger.info("=== Testing expectingException with RuntimeException ===");
            
            RuntimeException caught = TestErrorContext.expectingException(
                "testing null pointer scenario",
                () -> {
                    throw new RuntimeException("Test error");
                }
            );
            
            assertNotNull(caught);
            assertEquals("Test error", caught.getMessage());
            logger.info("[OK] Exception caught correctly");
        }
        
        @Test
        @DisplayName("Should fail when no exception is thrown")
        void shouldFailWhenNoExceptionThrown() {
            logger.info("=== Testing expectingException when no exception thrown ===");
            
            AssertionError caught = assertThrows(AssertionError.class, () -> {
                TestErrorContext.expectingException(
                    "testing operation that should throw but doesn't",
                    () -> {
                        // Does nothing - should cause AssertionError
                    }
                );
            });
            
            assertTrue(caught.getMessage().contains("Expected error was not thrown"));
            logger.info("[OK] AssertionError thrown correctly when no exception occurred");
        }
        
        @Test
        @DisplayName("Should preserve exception message and type")
        void shouldPreserveExceptionDetails() {
            logger.info("=== Testing exception details preservation ===");
            
            IllegalArgumentException caught = TestErrorContext.expectingException(
                "testing illegal argument",
                () -> {
                    throw new IllegalArgumentException("Invalid value: -1");
                }
            );
            
            assertInstanceOf(IllegalArgumentException.class, caught);
            assertTrue(caught.getMessage().contains("Invalid value"));
            logger.info("[OK] Exception details preserved correctly");
        }
    }
    
    @Nested
    @DisplayName("assertThrowsWithContext Tests")
    class AssertThrowsWithContextTests {
        
        @Test
        @DisplayName("Should catch correct exception type")
        void shouldCatchCorrectExceptionType() {
            logger.info("=== Testing assertThrowsWithContext with correct type ===");
            
            YamlConfigurationException caught = TestErrorContext.assertThrowsWithContext(
                "testing YAML parse error",
                YamlConfigurationException.class,
                () -> {
                    throw new YamlConfigurationException("Invalid YAML");
                }
            );
            
            assertNotNull(caught);
            assertEquals("Invalid YAML", caught.getMessage());
            logger.info("[OK] Correct exception type caught");
        }
        
        @Test
        @DisplayName("Should fail when wrong exception type is thrown")
        void shouldFailOnWrongExceptionType() {
            logger.info("=== Testing assertThrowsWithContext with wrong type ===");
            
            assertThrows(AssertionError.class, () -> {
                TestErrorContext.assertThrowsWithContext(
                    "testing expected NullPointerException but getting IllegalArgument",
                    NullPointerException.class,
                    () -> {
                        throw new IllegalArgumentException("Wrong type");
                    }
                );
            });
            
            logger.info("[OK] AssertionError thrown for wrong exception type");
        }
        
        @Test
        @DisplayName("Should fail when no exception is thrown")
        void shouldFailWhenNoExceptionThrown() {
            logger.info("=== Testing assertThrowsWithContext when no exception ===");
            
            assertThrows(AssertionError.class, () -> {
                TestErrorContext.assertThrowsWithContext(
                    "testing expected exception that never happens",
                    RuntimeException.class,
                    () -> {
                        // No exception
                    }
                );
            });
            
            logger.info("[OK] AssertionError thrown when no exception occurred");
        }
    }
    
    @Nested
    @DisplayName("expectingErrorResult Tests")
    class ExpectingErrorResultTests {
        
        @Test
        @DisplayName("Should return result from operation")
        void shouldReturnResult() {
            logger.info("=== Testing expectingErrorResult ===");
            
            String result = TestErrorContext.expectingErrorResult(
                "testing operation that returns error indicator",
                () -> "ERROR: Something failed"
            );
            
            assertEquals("ERROR: Something failed", result);
            logger.info("[OK] Result returned correctly");
        }
        
        @Test
        @DisplayName("Should work with boolean error indicators")
        void shouldWorkWithBooleanResult() {
            logger.info("=== Testing expectingErrorResult with boolean ===");
            
            Boolean result = TestErrorContext.expectingErrorResult(
                "testing validation that returns false",
                () -> false
            );
            
            assertFalse(result);
            logger.info("[OK] Boolean error result handled correctly");
        }
    }
    
    @Nested
    @DisplayName("expectingWarning Tests")
    class ExpectingWarningTests {
        
        @Test
        @DisplayName("Should return result and log warning context")
        void shouldReturnResultWithWarningContext() {
            logger.info("=== Testing expectingWarning ===");
            
            Integer result = TestErrorContext.expectingWarning(
                "testing operation that produces warning but continues",
                () -> 42
            );
            
            assertEquals(42, result);
            logger.info("[OK] Result returned with warning context");
        }
    }
    
    @Nested
    @DisplayName("validation Tests")
    class ValidationTests {
        
        @Test
        @DisplayName("Should complete validation successfully")
        void shouldCompleteValidationSuccessfully() {
            logger.info("=== Testing successful validation ===");
            
            assertDoesNotThrow(() -> {
                TestErrorContext.validation(
                    "testing that value is not null",
                    () -> {
                        Object value = "not null";
                        assertNotNull(value);
                    }
                );
            });
            
            logger.info("[OK] Validation completed successfully");
        }
        
        @Test
        @DisplayName("Should propagate exceptions from failed validation")
        void shouldPropagateValidationFailure() {
            logger.info("=== Testing failed validation ===");
            
            assertThrows(RuntimeException.class, () -> {
                TestErrorContext.validation(
                    "testing validation that should fail",
                    () -> {
                        throw new RuntimeException("Validation failed");
                    }
                );
            });
            
            logger.info("[OK] Validation failure propagated correctly");
        }
    }
    
    @Nested
    @DisplayName("Marker Methods Tests")
    class MarkerMethodsTests {
        
        @Test
        @DisplayName("Should log expected error marker")
        void shouldLogExpectedErrorMarker() {
            logger.info("=== Testing markExpectedError ===");
            
            // These just log - verify no exceptions
            assertDoesNotThrow(() -> {
                TestErrorContext.markExpectedError("testing invalid configuration");
            });
            
            logger.info("[OK] markExpectedError logged without exception");
        }
        
        @Test
        @DisplayName("Should log error caught marker")
        void shouldLogErrorCaughtMarker() {
            logger.info("=== Testing markErrorCaught ===");
            
            assertDoesNotThrow(() -> {
                TestErrorContext.markErrorCaught("invalid configuration handled");
            });
            
            logger.info("[OK] markErrorCaught logged without exception");
        }
        
        @Test
        @DisplayName("Should log validation markers")
        void shouldLogValidationMarkers() {
            logger.info("=== Testing validation markers ===");
            
            assertDoesNotThrow(() -> {
                TestErrorContext.markValidationStart("field validation");
                // ... do validation ...
                TestErrorContext.markValidationComplete("field validation");
            });
            
            logger.info("[OK] Validation markers logged without exception");
        }
    }
    
    @Nested
    @DisplayName("Prefix Constants Tests")
    class PrefixConstantsTests {
        
        @Test
        @DisplayName("Should have correct prefix values")
        void shouldHaveCorrectPrefixes() {
            logger.info("=== Testing prefix constants ===");
            
            assertEquals("[TEST-EXPECTED-ERROR]", TestErrorContext.EXPECTED_ERROR_PREFIX);
            assertEquals("[TEST-EXPECTED-WARNING]", TestErrorContext.EXPECTED_WARNING_PREFIX);
            assertEquals("[TEST-VALIDATION]", TestErrorContext.VALIDATION_PREFIX);
            
            logger.info("[OK] All prefix constants have correct values");
        }
    }
    
    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {
        
        @Test
        @DisplayName("Should work with real exception scenarios")
        void shouldWorkWithRealExceptionScenarios() {
            logger.info("=== Testing real exception scenario ===");
            
            // Simulate testing a service that validates input
            NullPointerException caught = TestErrorContext.assertThrowsWithContext(
                "testing null input validation",
                NullPointerException.class,
                () -> {
                    String input = null;
                    if (input == null) {
                        throw new NullPointerException("Input cannot be null");
                    }
                }
            );
            
            assertTrue(caught.getMessage().contains("cannot be null"));
            logger.info("[OK] Real exception scenario handled correctly");
        }
        
        @Test
        @DisplayName("Should provide clear logging output")
        void shouldProvideClearLoggingOutput() {
            logger.info("=== Testing logging clarity ===");
            
            // This test is primarily for visual verification of log output
            TestErrorContext.markExpectedError("testing invalid rule condition");
            
            try {
                throw new IllegalStateException("Rule condition syntax error");
            } catch (IllegalStateException e) {
                TestErrorContext.markErrorCaught("rule condition validation");
            }
            
            logger.info("[OK] Check console output for clear [TEST-EXPECTED-*] markers");
        }
    }
}
