package dev.mars.apex.core.test;

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
 * Utility class for marking intentional test errors in log output.
 * 
 * <p>This class provides methods to clearly mark test scenarios that intentionally
 * trigger error conditions, making it easy to distinguish between real errors and
 * expected test errors in log files.
 * 
 * <p>Complies with ERROR_HANDLING_IMPROVEMENT_TASKS.md Task 1 requirements.
 * 
 * <p>Usage:
 * <pre>{@code
 * TestErrorContext.withExpectedErrors("testing invalid configuration", () -> {
 *     // code that triggers error
 * });
 * }</pre>
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2026-01-25
 * @version 1.0
 */
public class TestErrorContext {
    
    private static final Logger logger = LoggerFactory.getLogger(TestErrorContext.class);
    
    /**
     * Prefix for expected error log messages.
     */
    public static final String EXPECTED_ERROR_PREFIX = "[TEST-EXPECTED-ERROR]";
    
    /**
     * Prefix for expected warning log messages.
     */
    public static final String EXPECTED_WARNING_PREFIX = "[TEST-EXPECTED-WARNING]";
    
    /**
     * Prefix for validation test scenarios.
     */
    public static final String VALIDATION_PREFIX = "[TEST-VALIDATION]";
    
    /**
     * Functional interface for code that may throw exceptions.
     */
    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Exception;
    }
    
    /**
     * Execute code that is expected to produce errors.
     * 
     * @param description description of the test scenario
     * @param runnable the code to execute
     * @throws Exception if the code throws an exception
     */
    public static void withExpectedErrors(String description, ThrowingRunnable runnable) throws Exception {
        logger.info("{} Starting: {}", EXPECTED_ERROR_PREFIX, description);
        try {
            runnable.run();
        } finally {
            logger.info("{} Completed: {}", EXPECTED_ERROR_PREFIX, description);
        }
    }
    
    /**
     * Execute code that is expected to produce errors (non-throwing version).
     * 
     * @param description description of the test scenario
     * @param runnable the code to execute
     */
    public static void withExpectedErrors(String description, Runnable runnable) {
        logger.info("{} Starting: {}", EXPECTED_ERROR_PREFIX, description);
        try {
            runnable.run();
        } finally {
            logger.info("{} Completed: {}", EXPECTED_ERROR_PREFIX, description);
        }
    }
    
    /**
     * Execute code that is expected to produce warnings.
     * 
     * @param description description of the test scenario
     * @param runnable the code to execute
     * @throws Exception if the code throws an exception
     */
    public static void withExpectedWarnings(String description, ThrowingRunnable runnable) throws Exception {
        logger.info("{} Starting: {}", EXPECTED_WARNING_PREFIX, description);
        try {
            runnable.run();
        } finally {
            logger.info("{} Completed: {}", EXPECTED_WARNING_PREFIX, description);
        }
    }
    
    /**
     * Execute code that is expected to produce warnings (non-throwing version).
     * 
     * @param description description of the test scenario
     * @param runnable the code to execute
     */
    public static void withExpectedWarnings(String description, Runnable runnable) {
        logger.info("{} Starting: {}", EXPECTED_WARNING_PREFIX, description);
        try {
            runnable.run();
        } finally {
            logger.info("{} Completed: {}", EXPECTED_WARNING_PREFIX, description);
        }
    }
    
    /**
     * Execute validation test code.
     * 
     * @param description description of the validation scenario
     * @param runnable the code to execute
     * @throws Exception if the code throws an exception
     */
    public static void withValidation(String description, ThrowingRunnable runnable) throws Exception {
        logger.info("{} Starting: {}", VALIDATION_PREFIX, description);
        try {
            runnable.run();
        } finally {
            logger.info("{} Completed: {}", VALIDATION_PREFIX, description);
        }
    }
    
    /**
     * Execute validation test code (non-throwing version).
     * 
     * @param description description of the validation scenario
     * @param runnable the code to execute
     */
    public static void withValidation(String description, Runnable runnable) {
        logger.info("{} Starting: {}", VALIDATION_PREFIX, description);
        try {
            runnable.run();
        } finally {
            logger.info("{} Completed: {}", VALIDATION_PREFIX, description);
        }
    }
    
    /**
     * Mark that an expected error is about to occur.
     * 
     * @param description description of the expected error
     */
    public static void markExpectedError(String description) {
        logger.info("{} {}", EXPECTED_ERROR_PREFIX, description);
    }
    
    /**
     * Mark that an expected warning is about to occur.
     * 
     * @param description description of the expected warning
     */
    public static void markExpectedWarning(String description) {
        logger.info("{} {}", EXPECTED_WARNING_PREFIX, description);
    }
    
    /**
     * Mark a validation scenario.
     * 
     * @param description description of the validation
     */
    public static void markValidation(String description) {
        logger.info("{} {}", VALIDATION_PREFIX, description);
    }
}
