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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * Test utility for wrapping operations that intentionally trigger errors.
 * Provides clear logging markers to distinguish expected test errors from unexpected failures.
 * 
 * <p>This utility helps with:</p>
 * <ul>
 *   <li>Clear identification of intentionally triggered errors in test logs</li>
 *   <li>Consistent formatting of test error context markers</li>
 *   <li>Distinguishing test validation errors from real application errors</li>
 * </ul>
 * 
 * <h2>Usage Examples</h2>
 * 
 * <h3>For operations that throw exceptions:</h3>
 * <pre>{@code
 * TestErrorContext.expectingException("testing invalid YAML configuration", () -> {
 *     yamlLoader.loadFromFile("invalid-config.yaml");
 * });
 * }</pre>
 * 
 * <h3>For operations that should return error results:</h3>
 * <pre>{@code
 * RuleResult result = TestErrorContext.expectingErrorResult(
 *     "testing rule with missing data",
 *     () -> rulesEngine.evaluate(incompleteData)
 * );
 * assertFalse(result.isSuccess());
 * }</pre>
 * 
 * <h3>With JUnit assertThrows:</h3>
 * <pre>{@code
 * TestErrorContext.expectingException("testing null parameter validation", () -> {
 *     assertThrows(NullPointerException.class, () -> service.process(null));
 * });
 * }</pre>
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2026-01-24
 */
public final class TestErrorContext {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TestErrorContext.class);
    
    /** Prefix for expected error messages in test logs */
    public static final String EXPECTED_ERROR_PREFIX = "[TEST-EXPECTED-ERROR]";
    
    /** Prefix for expected warning messages in test logs */
    public static final String EXPECTED_WARNING_PREFIX = "[TEST-EXPECTED-WARNING]";
    
    /** Prefix for validation test messages in test logs */
    public static final String VALIDATION_PREFIX = "[TEST-VALIDATION]";
    
    private TestErrorContext() {
        // Utility class - no instantiation
    }
    
    /**
     * Execute an operation that is expected to throw an exception.
     * Logs clear markers before and after execution to identify expected errors.
     * 
     * @param description Description of the expected error scenario (e.g., "testing invalid YAML syntax")
     * @param operation The operation that is expected to throw
     * @param <E> The expected exception type
     * @return The caught exception for further assertions
     * @throws AssertionError if no exception was thrown
     */
    public static <E extends Throwable> E expectingException(String description, ThrowingRunnable operation) {
        LOGGER.info("{} Triggering intentional error: {}", EXPECTED_ERROR_PREFIX, description);
        
        try {
            operation.run();
            String errorMsg = String.format("Expected error was not thrown: %s", description);
            LOGGER.error("{} TEST FAILURE - {}", EXPECTED_ERROR_PREFIX, errorMsg);
            throw new AssertionError(errorMsg);
        } catch (AssertionError e) {
            // Re-throw AssertionErrors - these indicate test failures
            throw e;
        } catch (Throwable e) {
            LOGGER.info("{} Error correctly caught: {} - {}", EXPECTED_ERROR_PREFIX, 
                       e.getClass().getSimpleName(), e.getMessage());
            @SuppressWarnings("unchecked")
            E result = (E) e;
            return result;
        }
    }
    
    /**
     * Execute an operation that is expected to produce a warning or error result
     * (but not throw an exception).
     * 
     * @param description Description of the expected error scenario
     * @param operation The operation to execute
     * @param <T> The return type
     * @return The result of the operation
     */
    public static <T> T expectingErrorResult(String description, Supplier<T> operation) {
        LOGGER.info("{} Triggering operation expected to produce error result: {}", 
                   EXPECTED_WARNING_PREFIX, description);
        
        T result = operation.get();
        
        LOGGER.info("{} Operation completed - verify result indicates expected error", 
                   EXPECTED_WARNING_PREFIX);
        return result;
    }
    
    /**
     * Execute an operation that is expected to produce a warning.
     * 
     * @param description Description of the expected warning scenario
     * @param operation The operation to execute
     * @param <T> The return type
     * @return The result of the operation
     */
    public static <T> T expectingWarning(String description, Supplier<T> operation) {
        LOGGER.info("{} Triggering operation expected to produce warning: {}", 
                   EXPECTED_WARNING_PREFIX, description);
        
        T result = operation.get();
        
        LOGGER.info("{} Operation completed with expected warning", EXPECTED_WARNING_PREFIX);
        return result;
    }
    
    /**
     * Execute a validation test scenario.
     * 
     * @param description Description of the validation being tested
     * @param operation The validation operation to execute
     */
    public static void validation(String description, Runnable operation) {
        LOGGER.info("{} Starting validation: {}", VALIDATION_PREFIX, description);
        
        try {
            operation.run();
            LOGGER.info("{} Validation completed: {}", VALIDATION_PREFIX, description);
        } catch (Exception e) {
            LOGGER.error("{} Validation failed unexpectedly: {} - {}", 
                        VALIDATION_PREFIX, description, e.getMessage());
            throw e;
        }
    }
    
    /**
     * Execute an operation that is expected to throw an exception,
     * returning a result from a callable instead of just running.
     * 
     * @param description Description of the expected error scenario
     * @param operation The callable operation
     * @param <T> The return type (not used since exception is expected)
     * @return The caught exception
     * @throws AssertionError if no exception was thrown
     */
    public static <T> Throwable expectingExceptionFrom(String description, Callable<T> operation) {
        LOGGER.info("{} Triggering intentional error from callable: {}", EXPECTED_ERROR_PREFIX, description);
        
        try {
            T result = operation.call();
            String errorMsg = String.format("Expected error was not thrown (got result: %s): %s", 
                                           result, description);
            LOGGER.error("{} TEST FAILURE - {}", EXPECTED_ERROR_PREFIX, errorMsg);
            throw new AssertionError(errorMsg);
        } catch (AssertionError ae) {
            throw ae; // Re-throw our own assertion errors
        } catch (Throwable e) {
            LOGGER.info("{} Error correctly caught: {} - {}", EXPECTED_ERROR_PREFIX, 
                       e.getClass().getSimpleName(), e.getMessage());
            return e;
        }
    }
    
    /**
     * Wrap an assertThrows call with proper logging context.
     * Use this when you want to use JUnit's assertThrows but still want logging markers.
     * 
     * @param description Description of the expected error scenario
     * @param expectedType The expected exception type
     * @param operation The operation that should throw
     * @param <E> The exception type
     * @return The caught exception for further assertions
     */
    public static <E extends Throwable> E assertThrowsWithContext(
            String description, 
            Class<E> expectedType, 
            ThrowingRunnable operation) {
        
        LOGGER.info("{} Expecting {} to be thrown: {}", 
                   EXPECTED_ERROR_PREFIX, expectedType.getSimpleName(), description);
        
        try {
            operation.run();
            String errorMsg = String.format("Expected %s was not thrown: %s", 
                                           expectedType.getSimpleName(), description);
            LOGGER.error("{} TEST FAILURE - {}", EXPECTED_ERROR_PREFIX, errorMsg);
            throw new AssertionError(errorMsg);
        } catch (Throwable e) {
            if (expectedType.isInstance(e)) {
                LOGGER.info("{} Correct exception caught: {} - {}", EXPECTED_ERROR_PREFIX, 
                           e.getClass().getSimpleName(), e.getMessage());
                return expectedType.cast(e);
            } else {
                String errorMsg = String.format("Expected %s but got %s: %s", 
                                               expectedType.getSimpleName(), 
                                               e.getClass().getSimpleName(), 
                                               description);
                LOGGER.error("{} TEST FAILURE - {}", EXPECTED_ERROR_PREFIX, errorMsg);
                throw new AssertionError(errorMsg, e);
            }
        }
    }
    
    /**
     * Log a marker indicating an intentional error is about to be triggered.
     * Use this for simple marker logging without wrapping the entire operation.
     * 
     * @param description Description of the expected error
     */
    public static void markExpectedError(String description) {
        LOGGER.info("{} Intentional error expected: {}", EXPECTED_ERROR_PREFIX, description);
    }
    
    /**
     * Execute an operation that is expected to produce error/warning logs from 
     * production code (but not throw an exception to the caller).
     * Wraps the operation with clear start/end markers.
     * 
     * @param description Description of the expected error scenario
     * @param operation The operation to execute
     */
    public static void withExpectedErrors(String description, ThrowingRunnable operation) {
        LOGGER.info("{} START - Executing code that triggers intentional errors: {}", 
                   EXPECTED_ERROR_PREFIX, description);
        
        try {
            operation.run();
            LOGGER.info("{} END - Completed (errors above were expected): {}", 
                       EXPECTED_ERROR_PREFIX, description);
        } catch (Throwable e) {
            LOGGER.info("{} END - Completed with exception (expected): {} - {}", 
                       EXPECTED_ERROR_PREFIX, e.getClass().getSimpleName(), e.getMessage());
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Log a marker indicating an expected error was successfully caught.
     * 
     * @param description Description of the error that was caught
     */
    public static void markErrorCaught(String description) {
        LOGGER.info("{} Error correctly handled: {}", EXPECTED_ERROR_PREFIX, description);
    }
    
    /**
     * Log a marker indicating a validation is starting.
     * 
     * @param description Description of the validation
     */
    public static void markValidationStart(String description) {
        LOGGER.info("{} Starting: {}", VALIDATION_PREFIX, description);
    }
    
    /**
     * Log a marker indicating a validation completed successfully.
     * 
     * @param description Description of the validation
     */
    public static void markValidationComplete(String description) {
        LOGGER.info("{} Completed: {}", VALIDATION_PREFIX, description);
    }
    
    /**
     * Functional interface for operations that may throw any exception.
     */
    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Throwable;
    }
}
