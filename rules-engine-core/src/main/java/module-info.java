module dev.mars.rulesengine.core {
    // Existing dependencies (may vary for your project)
    requires java.base;

    requires java.logging;
    requires spring.expression;
    requires spring.context;

    // Logging dependencies
    requires org.slf4j;
    requires ch.qos.logback.classic;
    requires ch.qos.logback.core;

    // YAML processing dependencies
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.dataformat.yaml;

    // Existing exports
    exports dev.mars.rulesengine.core.service.validation;
    exports dev.mars.rulesengine.core.service.common;
    exports dev.mars.rulesengine.core.service.lookup;
    exports dev.mars.rulesengine.core.engine.model;
    exports dev.mars.rulesengine.core.engine.config;

    // New exports for enhanced error handling
    exports dev.mars.rulesengine.core.exception;
    exports dev.mars.rulesengine.core.service.error;
    exports dev.mars.rulesengine.core.api;

    // YAML configuration exports
    exports dev.mars.rulesengine.core.config.yaml;

    // New exports for performance monitoring
    exports dev.mars.rulesengine.core.service.monitoring;

    // Enhanced API exports for simplified configuration
    // Note: dev.mars.rulesengine.core.api is already exported above

    // Export other existing services
    exports dev.mars.rulesengine.core.service.engine;
    exports dev.mars.rulesengine.core.service.data;
    exports dev.mars.rulesengine.core.service.transform;
    exports dev.mars.rulesengine.core.util;

}