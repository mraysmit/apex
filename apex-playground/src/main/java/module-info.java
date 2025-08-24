module dev.mars.apex.playground {
    // Core dependencies
    requires java.base;
    requires java.logging;
    requires java.sql;

    // APEX dependencies
    requires transitive dev.mars.apex.core;
    requires transitive dev.mars.apex.demo;
    requires spring.core;
    requires spring.expression;
    requires spring.context;
    requires spring.beans;

    // Spring Boot dependencies
    requires spring.boot;
    requires spring.boot.autoconfigure;
    requires spring.web;
    requires spring.webmvc;
    requires spring.boot.actuator;
    requires spring.boot.actuator.autoconfigure;

    // Jackson dependencies for JSON processing
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.dataformat.yaml;
    requires com.fasterxml.jackson.dataformat.xml;

    // Logging dependencies
    requires org.slf4j;
    requires org.slf4j.simple;

    // OpenAPI/Swagger dependencies
    requires io.swagger.v3.oas.annotations;

    // Micrometer for metrics
    requires micrometer.core;

    // Export packages for Spring and other frameworks
    exports dev.mars.apex.playground;
    exports dev.mars.apex.playground.controller;
    exports dev.mars.apex.playground.service;
    exports dev.mars.apex.playground.config;
    exports dev.mars.apex.playground.model;

    // Open packages for Spring reflection
    opens dev.mars.apex.playground to spring.core, spring.beans, spring.context;
    opens dev.mars.apex.playground.controller to spring.core, spring.beans, spring.context;
    opens dev.mars.apex.playground.service to spring.core, spring.beans, spring.context;
    opens dev.mars.apex.playground.config to spring.core, spring.beans, spring.context;
    opens dev.mars.apex.playground.model to spring.core, spring.beans, spring.context;
}
