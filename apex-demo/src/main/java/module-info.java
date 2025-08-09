module dev.mars.apex.demo {
    // Core dependencies
    requires java.base;
    requires java.logging;
    requires java.sql;

    // APEX dependencies
    requires transitive dev.mars.apex.core;
    requires spring.expression;
    requires spring.context;
    requires spring.beans;

    // Database dependencies
    requires org.postgresql.jdbc;
    requires java.sql.rowset;

    // Jackson dependencies for JSON processing
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;

    // Logging dependencies
    requires org.slf4j;
    requires org.slf4j.simple;
    requires jul.to.slf4j;
    requires jdk.compiler;

    // Export packages for Spring Expression Language access and general use
    exports dev.mars.apex.demo.model;

    // Export main demo packages for external access
    exports dev.mars.apex.demo;
    exports dev.mars.apex.demo.rulesets;
    exports dev.mars.apex.demo.data to spring.expression;
    exports dev.mars.apex.demo.examples;
    exports dev.mars.apex.demo.advanced;
    exports dev.mars.apex.demo.bootstrap;
    exports dev.mars.apex.demo.bootstrap.model to spring.expression;
}
