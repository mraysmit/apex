package dev.mars.apex.core.test.extension;

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

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JUnit 5 extension that logs test class lifecycle events.
 * 
 * <p>This extension automatically logs when a test class starts and ends execution,
 * providing clear boundaries in test output logs. This is particularly useful for:
 * <ul>
 *   <li>Identifying test class boundaries in large log files</li>
 *   <li>Debugging test execution order and timing</li>
 *   <li>Correlating log output with specific test classes</li>
 * </ul>
 * 
 * <p>Usage:
 * <pre>{@code
 * @ExtendWith({ColoredTestOutputExtension.class, TestClassLoggingExtension.class})
 * public class MyTest {
 *     // ... your tests
 * }
 * }</pre>
 * 
 * <p>Or create a composite annotation:
 * <pre>{@code
 * @Target(ElementType.TYPE)
 * @Retention(RetentionPolicy.RUNTIME)
 * @ExtendWith({ColoredTestOutputExtension.class, TestClassLoggingExtension.class})
 * public @interface ApexTest {
 * }
 * }</pre>
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2026-01-25
 * @version 1.0
 */
public class TestClassLoggingExtension implements BeforeAllCallback, AfterAllCallback {
    
    /**
     * Marker prefix for test class start events.
     */
    private static final String TEST_CLASS_START_MARKER = "[TEST-CLASS-START]";
    
    /**
     * Marker prefix for test class end events.
     */
    private static final String TEST_CLASS_END_MARKER = "[TEST-CLASS-END]";
    
    /**
     * Separator line for visual clarity in logs.
     */
    private static final String SEPARATOR = "================================================================================";
    
    /**
     * Called before all tests in a test class.
     * Logs the start of test class execution.
     *
     * @param context the extension context
     */
    @Override
    public void beforeAll(ExtensionContext context) {
        Class<?> testClass = context.getRequiredTestClass();
        Logger logger = LoggerFactory.getLogger(testClass);
        
        logger.info(SEPARATOR);
        logger.info("{} {}", TEST_CLASS_START_MARKER, testClass.getSimpleName());
        logger.info("{} Full class name: {}", TEST_CLASS_START_MARKER, testClass.getName());
        logger.info(SEPARATOR);
    }
    
    /**
     * Called after all tests in a test class.
     * Logs the end of test class execution.
     *
     * @param context the extension context
     */
    @Override
    public void afterAll(ExtensionContext context) {
        Class<?> testClass = context.getRequiredTestClass();
        Logger logger = LoggerFactory.getLogger(testClass);
        
        logger.info(SEPARATOR);
        logger.info("{} {}", TEST_CLASS_END_MARKER, testClass.getSimpleName());
        logger.info("{} All tests completed in: {}", TEST_CLASS_END_MARKER, testClass.getName());
        logger.info(SEPARATOR);
    }
}
