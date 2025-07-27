package dev.mars.rulesengine.core.util;

import org.slf4j.Logger;

/**
 * Test-aware logging utility that modifies log messages when running in test environment.
 * This prevents automated log monitoring systems from incorrectly flagging intentional test errors.
 */
public class TestAwareLogger {
    
    private static final String TEST_PREFIX = "[TEST-EXPECTED-ERROR] ";
    private static final String TEST_WARN_PREFIX = "[TEST-EXPECTED-WARNING] ";
    
    /**
     * Check if we're running in a test environment.
     */
    private static boolean isTestEnvironment() {
        // Check for test profile in system properties
        String profiles = System.getProperty("spring.profiles.active");
        if (profiles != null && profiles.contains("test")) {
            return true;
        }
        
        // Also check for test class in stack trace
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName();
            if (className.contains("Test") || className.contains("test")) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Log an error message, prefixing with test indicator if in test environment.
     */
    public static void error(Logger logger, String message, Object... args) {
        if (isTestEnvironment()) {
            String prefixedMessage = TEST_PREFIX + message;
            logger.error(prefixedMessage, args);
        } else {
            logger.error(message, args);
        }
    }
    
    /**
     * Log an error message with throwable, prefixing with test indicator if in test environment.
     */
    public static void error(Logger logger, String message, Throwable throwable, Object... args) {
        if (isTestEnvironment()) {
            String prefixedMessage = TEST_PREFIX + message;
            if (args.length > 0) {
                logger.error(prefixedMessage, args[0], throwable);
            } else {
                logger.error(prefixedMessage, throwable);
            }
        } else {
            if (args.length > 0) {
                logger.error(message, args[0], throwable);
            } else {
                logger.error(message, throwable);
            }
        }
    }
    
    /**
     * Log a warning message, prefixing with test indicator if in test environment.
     */
    public static void warn(Logger logger, String message, Object... args) {
        if (isTestEnvironment()) {
            String prefixedMessage = TEST_WARN_PREFIX + message;
            logger.warn(prefixedMessage, args);
        } else {
            logger.warn(message, args);
        }
    }
    
    /**
     * Log a warning message with throwable, prefixing with test indicator if in test environment.
     */
    public static void warn(Logger logger, String message, Throwable throwable, Object... args) {
        if (isTestEnvironment()) {
            String prefixedMessage = TEST_WARN_PREFIX + message;
            if (args.length > 0) {
                logger.warn(prefixedMessage, args[0], throwable);
            } else {
                logger.warn(prefixedMessage, throwable);
            }
        } else {
            if (args.length > 0) {
                logger.warn(message, args[0], throwable);
            } else {
                logger.warn(message, throwable);
            }
        }
    }
}
