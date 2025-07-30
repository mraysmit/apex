package dev.mars.apex.core.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

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

/**
 * Test class for verifying logging improvements including:
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Test class for verifying logging improvements including:
 * - Structured logging with MDC
 * - Performance-optimized logging
 * - Context management
 * - Proper log formatting
 */
public class LoggingImprovementsTest {

    private ListAppender<ILoggingEvent> listAppender;
    private Logger logger;
    private RulesEngineLogger rulesEngineLogger;

    @BeforeEach
    void setUp() {
        // Create the RulesEngineLogger first
        rulesEngineLogger = new RulesEngineLogger(LoggingImprovementsTest.class);

        // Set up test logger with list appender to capture log events
        // Get the actual logger that RulesEngineLogger is using
        logger = (Logger) rulesEngineLogger.getLogger();
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        // Set level to ALL to capture all log events including DEBUG and INFO
        logger.setLevel(Level.ALL);

        // Clear any existing MDC
        MDC.clear();
        LoggingContext.clearContext();
    }

    @AfterEach
    void tearDown() {
        // Clean up
        logger.detachAppender(listAppender);
        MDC.clear();
        LoggingContext.clearContext();
    }

    @Test
    void testLoggingContextManagement() {
        // Test correlation ID generation
        String correlationId = LoggingContext.initializeContext();
        assertNotNull(correlationId);
        assertEquals(correlationId, LoggingContext.getCorrelationId());
        assertEquals(correlationId, MDC.get(LoggingContext.CORRELATION_ID));

        // Test rule context
        LoggingContext.setRuleName("test-rule");
        LoggingContext.setRulePhase("evaluation");
        LoggingContext.setEvaluationTime(15.5);

        assertEquals("test-rule", LoggingContext.getRuleName());
        assertEquals("test-rule", MDC.get(LoggingContext.RULE_NAME));
        assertEquals("evaluation", MDC.get(LoggingContext.RULE_PHASE));
        assertEquals("15.50", MDC.get(LoggingContext.EVALUATION_TIME));

        // Test context clearing
        LoggingContext.clearRuleContext();
        assertNull(MDC.get(LoggingContext.RULE_NAME));
        assertNull(MDC.get(LoggingContext.RULE_PHASE));
        assertNull(MDC.get(LoggingContext.EVALUATION_TIME));
        
        // Correlation ID should still be present
        assertEquals(correlationId, MDC.get(LoggingContext.CORRELATION_ID));
    }

    @Test
    void testRulesEngineLoggerRuleEvaluation() {
        // Test rule evaluation logging
        rulesEngineLogger.ruleEvaluationStart("customer-validation");
        
        List<ILoggingEvent> logEvents = listAppender.list;
        assertFalse(logEvents.isEmpty());

        ILoggingEvent lastEvent = logEvents.get(logEvents.size() - 1);
        assertEquals(Level.INFO, lastEvent.getLevel());
        assertTrue(lastEvent.getFormattedMessage().contains("Starting evaluation of rule: customer-validation"));
        assertNotNull(lastEvent.getMarker(), "Marker should not be null");
        assertEquals("RULE_EVALUATION", lastEvent.getMarker().getName());
    }

    @Test
    void testRulesEngineLoggerPerformance() {
        // Test performance logging
        rulesEngineLogger.performance("slow-rule", 125.5, 2048L);
        
        List<ILoggingEvent> logEvents = listAppender.list;
        assertFalse(logEvents.isEmpty());
        
        ILoggingEvent lastEvent = logEvents.get(logEvents.size() - 1);
        assertEquals(Level.DEBUG, lastEvent.getLevel());
        assertTrue(lastEvent.getFormattedMessage().contains("slow-rule"));
        assertTrue(lastEvent.getFormattedMessage().contains("125.50ms"));
        assertTrue(lastEvent.getFormattedMessage().contains("2KB memory"));
        assertEquals("PERFORMANCE", lastEvent.getMarker().getName());
    }

    @Test
    void testRulesEngineLoggerSlowRuleWarning() {
        // Test slow rule warning
        rulesEngineLogger.slowRule("very-slow-rule", 250.0, 100.0);
        
        List<ILoggingEvent> logEvents = listAppender.list;
        assertFalse(logEvents.isEmpty());
        
        ILoggingEvent lastEvent = logEvents.get(logEvents.size() - 1);
        assertEquals(Level.WARN, lastEvent.getLevel());
        assertTrue(lastEvent.getFormattedMessage().contains("Slow rule detected"));
        assertTrue(lastEvent.getFormattedMessage().contains("very-slow-rule"));
        assertTrue(lastEvent.getFormattedMessage().contains("250.00ms"));
        assertTrue(lastEvent.getFormattedMessage().contains("threshold: 100.00ms"));
        assertEquals("PERFORMANCE", lastEvent.getMarker().getName());
    }

    @Test
    void testRulesEngineLoggerErrorRecovery() {
        // Test error recovery logging
        rulesEngineLogger.errorRecoveryAttempt("failing-rule", "RETRY_WITH_SAFE_EXPRESSION");
        rulesEngineLogger.errorRecoverySuccess("failing-rule", "RETRY_WITH_SAFE_EXPRESSION");
        
        List<ILoggingEvent> logEvents = listAppender.list;
        assertTrue(logEvents.size() >= 2);
        
        // Check attempt log
        ILoggingEvent attemptEvent = logEvents.get(logEvents.size() - 2);
        assertEquals(Level.INFO, attemptEvent.getLevel());
        assertTrue(attemptEvent.getFormattedMessage().contains("Attempting error recovery"));
        assertTrue(attemptEvent.getFormattedMessage().contains("failing-rule"));
        assertEquals("ERROR_RECOVERY", attemptEvent.getMarker().getName());

        // Check success log
        ILoggingEvent successEvent = logEvents.get(logEvents.size() - 1);
        assertEquals(Level.INFO, successEvent.getLevel());
        assertTrue(successEvent.getFormattedMessage().contains("Error recovery successful"));
        assertTrue(successEvent.getFormattedMessage().contains("failing-rule"));
        assertEquals("ERROR_RECOVERY", successEvent.getMarker().getName());
    }

    @Test
    void testContextualLogging() {
        // Test logging with context
        LoggingContext.withRuleContext("test-rule", "parsing", () -> {
            rulesEngineLogger.debug("Processing rule in parsing phase");
            
            // Verify context is set
            assertEquals("test-rule", MDC.get(LoggingContext.RULE_NAME));
            assertEquals("parsing", MDC.get(LoggingContext.RULE_PHASE));
        });
        
        // Verify context is cleared after execution
        assertNull(MDC.get(LoggingContext.RULE_NAME));
        assertNull(MDC.get(LoggingContext.RULE_PHASE));
    }

    @Test
    void testCorrelationIdPropagation() {
        String correlationId = "test-correlation-123";
        
        LoggingContext.withCorrelationId(correlationId, () -> {
            rulesEngineLogger.info("Test message with correlation ID");
            
            // Verify correlation ID is set
            assertEquals(correlationId, MDC.get(LoggingContext.CORRELATION_ID));
            
            List<ILoggingEvent> logEvents = listAppender.list;
            assertFalse(logEvents.isEmpty());
            
            ILoggingEvent lastEvent = logEvents.get(logEvents.size() - 1);
            assertEquals("Test message with correlation ID", lastEvent.getMessage());
        });
    }

    @Test
    void testLazyLogging() {
        // Test that expensive operations are not executed when logging is disabled
        logger.setLevel(Level.WARN); // Disable DEBUG and INFO
        
        boolean[] expensiveOperationCalled = {false};
        
        rulesEngineLogger.debug(() -> {
            expensiveOperationCalled[0] = true;
            return "Expensive debug message";
        });
        
        // Expensive operation should not have been called
        assertFalse(expensiveOperationCalled[0]);
        
        // Enable DEBUG and try again
        logger.setLevel(Level.DEBUG);
        
        rulesEngineLogger.debug(() -> {
            expensiveOperationCalled[0] = true;
            return "Expensive debug message";
        });
        
        // Now it should have been called
        assertTrue(expensiveOperationCalled[0]);
    }

    @Test
    void testAuditLogging() {
        // Set up audit logger with list appender
        Logger auditLogger = (Logger) LoggerFactory.getLogger("dev.mars.apex.audit");
        ListAppender<ILoggingEvent> auditAppender = new ListAppender<>();
        auditAppender.start();
        auditLogger.addAppender(auditAppender);
        auditLogger.setLevel(Level.ALL);

        try {
            // Test audit logging
            LoggingContext.auditLog("RULE_EXECUTION", "customer-rule", "Rule executed successfully");

            // Check that audit log was captured
            List<ILoggingEvent> auditEvents = auditAppender.list;
            assertFalse(auditEvents.isEmpty());

            ILoggingEvent auditEvent = auditEvents.get(auditEvents.size() - 1);
            assertEquals(Level.INFO, auditEvent.getLevel());
            assertTrue(auditEvent.getFormattedMessage().contains("Rule executed successfully"));

            // Check MDC values in the audit event
            Map<String, String> mdc = auditEvent.getMDCPropertyMap();
            assertEquals("RULE_EXECUTION", mdc.get(LoggingContext.OPERATION));
            assertEquals("customer-rule", mdc.get(LoggingContext.RULE_NAME));
        } finally {
            auditLogger.detachAppender(auditAppender);
        }
    }

    @Test
    void testConfigurationLogging() {
        // Test configuration logging
        rulesEngineLogger.configuration("RulesEngine", "Initialized with custom configuration");
        
        List<ILoggingEvent> logEvents = listAppender.list;
        assertFalse(logEvents.isEmpty());
        
        ILoggingEvent lastEvent = logEvents.get(logEvents.size() - 1);
        assertEquals(Level.INFO, lastEvent.getLevel());
        assertTrue(lastEvent.getFormattedMessage().contains("Configuration [RulesEngine]"));
        assertTrue(lastEvent.getFormattedMessage().contains("Initialized with custom configuration"));
        assertEquals("CONFIGURATION", lastEvent.getMarker().getName());
    }

    @Test
    void testLogLevelChecks() {
        // Test log level checking methods
        logger.setLevel(Level.INFO);
        
        assertFalse(rulesEngineLogger.isDebugEnabled());
        assertTrue(rulesEngineLogger.isInfoEnabled());
        assertTrue(rulesEngineLogger.isWarnEnabled());
        assertTrue(rulesEngineLogger.isErrorEnabled());
        
        logger.setLevel(Level.ERROR);
        
        assertFalse(rulesEngineLogger.isDebugEnabled());
        assertFalse(rulesEngineLogger.isInfoEnabled());
        assertFalse(rulesEngineLogger.isWarnEnabled());
        assertTrue(rulesEngineLogger.isErrorEnabled());
    }
}
