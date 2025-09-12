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

package dev.mars.apex.demo;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

/**
 * JUnit 5 extension that provides colored console output for test execution.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 */
public class ColoredTestOutputExtension implements BeforeEachCallback, AfterEachCallback, TestWatcher {
    
    // ANSI color codes
    private static final String GREEN = "\u001B[32m";
    private static final String RED = "\u001B[31m";
    private static final String BLUE = "\u001B[34m";
    private static final String YELLOW = "\u001B[33m";
    private static final String CYAN = "\u001B[36m";
    private static final String RESET = "\u001B[0m";
    private static final String BOLD = "\u001B[1m";
    
    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        String testName = context.getDisplayName();
        String className = context.getTestClass().map(Class::getSimpleName).orElse("Unknown");
        
        System.out.println(BLUE + BOLD + "▶ STARTING: " + RESET + CYAN + className + "." + testName + RESET);
    }
    
    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        // This will be called after each test, but TestWatcher methods provide more specific info
    }
    
    @Override
    public void testSuccessful(ExtensionContext context) {
        String testName = context.getDisplayName();
        String className = context.getTestClass().map(Class::getSimpleName).orElse("Unknown");
        
        System.out.println(GREEN + BOLD + "✓ PASSED: " + RESET + GREEN + className + "." + testName + RESET);
    }
    
    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        String testName = context.getDisplayName();
        String className = context.getTestClass().map(Class::getSimpleName).orElse("Unknown");
        
        System.out.println(RED + BOLD + "✗ FAILED: " + RESET + RED + className + "." + testName + RESET);
        System.out.println(RED + "  Reason: " + cause.getMessage() + RESET);
    }
    
    @Override
    public void testAborted(ExtensionContext context, Throwable cause) {
        String testName = context.getDisplayName();
        String className = context.getTestClass().map(Class::getSimpleName).orElse("Unknown");
        
        System.out.println(YELLOW + BOLD + "⚠ ABORTED: " + RESET + YELLOW + className + "." + testName + RESET);
        System.out.println(YELLOW + "  Reason: " + cause.getMessage() + RESET);
    }
    
    @Override
    public void testDisabled(ExtensionContext context, java.util.Optional<String> reason) {
        String testName = context.getDisplayName();
        String className = context.getTestClass().map(Class::getSimpleName).orElse("Unknown");
        
        System.out.println(YELLOW + "⏸ DISABLED: " + className + "." + testName + RESET);
        reason.ifPresent(r -> System.out.println(YELLOW + "  Reason: " + r + RESET));
    }
    
    /**
     * Utility method for colored logging within tests
     */
    public static void logInfo(String message) {
        System.out.println(BLUE + "ℹ " + message + RESET);
    }
    
    public static void logSuccess(String message) {
        System.out.println(GREEN + "✓ " + message + RESET);
    }
    
    public static void logError(String message) {
        System.out.println(RED + "✗ " + message + RESET);
    }
    
    public static void logWarning(String message) {
        System.out.println(YELLOW + "⚠ " + message + RESET);
    }
}
