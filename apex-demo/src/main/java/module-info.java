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
 * @author Mark Andrew Ray-Smith Cityline Ltd
 */


module dev.mars.apex.demo {
    // Core dependencies
    requires java.base;
    requires java.logging;
    requires java.sql;
    requires java.net.http; // For HttpClient used in tests
    requires jdk.httpserver;  // For REST API testing with JDK HTTP server

    // APEX dependencies
    requires transitive dev.mars.apex.core;
    requires spring.expression;
    requires spring.context;
    requires spring.beans;

    // Database dependencies
    requires org.postgresql.jdbc;
    requires com.h2database;
    requires java.sql.rowset;

    // Jackson dependencies for JSON and YAML processing
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.dataformat.yaml;

    // Logging dependencies
    requires org.slf4j;
    requires org.slf4j.simple;
    requires jul.to.slf4j;
    requires jdk.compiler;

    // Enable automatic JDBC driver loading
    uses java.sql.Driver;

    // Export packages for Spring Expression Language access and general use

    // Export main demo packages for external access
    exports dev.mars.apex.demo.model;
    exports dev.mars.apex.demo.infrastructure;

    // Open packages for JUnit testing and reflection access
    // Open packages to allow JUnit and other testing frameworks reflective access to lifecycle methods
    // Using ALL-UNNAMED to allow access from any unnamed module (including test frameworks)
    // Only opening packages that actually exist in main source
    opens dev.mars.apex.demo.model;
    opens dev.mars.apex.demo.infrastructure;
    opens dev.mars.apex.demo;
}
