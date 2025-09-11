package dev.mars.apex.demo.lookup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test to verify logging configuration and debug capabilities.
 */
public class LoggingConfigTest {

    private static final Logger logger = LoggerFactory.getLogger(LoggingConfigTest.class);

    @Test
    @DisplayName("Should show current logging configuration and test debug logging")
    void testLoggingConfiguration() {
        logger.info("=== LOGGING CONFIGURATION TEST ===");
        
        // Test different log levels
        logger.error("ERROR level logging - should always appear");
        logger.warn("WARN level logging - should appear");
        logger.info("INFO level logging - should appear");
        logger.debug("DEBUG level logging - may or may not appear depending on configuration");
        logger.trace("TRACE level logging - usually does not appear");
        
        // Check SLF4J implementation
        logger.info("SLF4J Logger class: {}", logger.getClass().getName());
        
        // Test specific APEX loggers
        Logger dbLookupLogger = LoggerFactory.getLogger("dev.mars.apex.core.service.lookup.DatabaseLookupService");
        Logger jdbcUtilsLogger = LoggerFactory.getLogger("dev.mars.apex.core.service.data.external.database.JdbcParameterUtils");
        
        logger.info("DatabaseLookupService logger class: {}", dbLookupLogger.getClass().getName());
        logger.info("JdbcParameterUtils logger class: {}", jdbcUtilsLogger.getClass().getName());
        
        // Test debug on specific loggers
        dbLookupLogger.debug("DEBUG: DatabaseLookupService debug test");
        jdbcUtilsLogger.debug("DEBUG: JdbcParameterUtils debug test");
        
        logger.info("=== END LOGGING CONFIGURATION TEST ===");
    }
}
