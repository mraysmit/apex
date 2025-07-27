package dev.mars.rulesengine.rest.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Test-aware logging utility that modifies log messages when running in test environment.
 * This prevents automated log monitoring systems from incorrectly flagging intentional test errors.
 */
@Component
public class TestAwareLogger {
    
    private static final String TEST_PREFIX = "[TEST-EXPECTED-ERROR] ";
    private static final String TEST_WARN_PREFIX = "[TEST-EXPECTED-WARNING] ";
    
    private final Environment environment;
    
    public TestAwareLogger(Environment environment) {
        this.environment = environment;
    }
    
    /**
     * Check if we're running in a test environment.
     */
    private boolean isTestEnvironment() {
        // Check for test profile
        String[] activeProfiles = environment.getActiveProfiles();
        for (String profile : activeProfiles) {
            if ("test".equals(profile)) {
                return true;
            }
        }

        // Check for test environment property
        String testEnv = environment.getProperty("test.environment");
        if ("true".equals(testEnv)) {
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
    public void error(Logger logger, String message, Object... args) {
        if (isTestEnvironment()) {
            String prefixedMessage = TEST_PREFIX + message;
            logger.error(prefixedMessage, args);
        } else {
            logger.error(message, args);
        }
    }
    
    /**
     * Log an error message with throwable, prefixing with test indicator if in test environment.
     * In test environment, only logs the message without stack trace to avoid log pollution.
     */
    public void error(Logger logger, String message, Throwable throwable, Object... args) {
        if (isTestEnvironment()) {
            // In test environment, log only the message without stack trace to keep logs clean
            String prefixedMessage = TEST_PREFIX + message;
            if (args.length > 0) {
                logger.error(prefixedMessage, args[0]);
            } else {
                logger.error(prefixedMessage);
            }
        } else {
            // In production, log with full stack trace for debugging
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
    public void warn(Logger logger, String message, Object... args) {
        if (isTestEnvironment()) {
            String prefixedMessage = TEST_WARN_PREFIX + message;
            logger.warn(prefixedMessage, args);
        } else {
            logger.warn(message, args);
        }
    }
    
    /**
     * Log a warning message with throwable, prefixing with test indicator if in test environment.
     * In test environment, only logs the message without stack trace to avoid log pollution.
     */
    public void warn(Logger logger, String message, Throwable throwable, Object... args) {
        if (isTestEnvironment()) {
            // In test environment, log only the message without stack trace to keep logs clean
            String prefixedMessage = TEST_WARN_PREFIX + message;
            if (args.length > 0) {
                logger.warn(prefixedMessage, args[0]);
            } else {
                logger.warn(prefixedMessage);
            }
        } else {
            // In production, log with full stack trace for debugging
            if (args.length > 0) {
                logger.warn(message, args[0], throwable);
            } else {
                logger.warn(message, throwable);
            }
        }
    }
}
