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


module dev.mars.apex.core {
    // Existing dependencies (may vary for your project)
    requires java.base;

    requires java.logging;
    requires java.sql;
    requires java.net.http;
    requires transitive spring.expression;
    requires spring.context;

    // Logging dependencies
    requires transitive org.slf4j;


    // YAML and XML processing dependencies
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.dataformat.yaml;
    requires com.fasterxml.jackson.dataformat.xml;

    // Enable automatic JDBC driver loading
    uses java.sql.Driver;

    // Existing exports
    exports dev.mars.apex.core.service.validation;
    exports dev.mars.apex.core.service.common;
    exports dev.mars.apex.core.service.lookup;
    exports dev.mars.apex.core.engine.model;
    exports dev.mars.apex.core.engine.model.metadata;
    exports dev.mars.apex.core.engine.config;

    // New exports for enhanced error handling
    exports dev.mars.apex.core.exception;
    exports dev.mars.apex.core.service.error;
    exports dev.mars.apex.core.api;

    // YAML configuration exports
    exports dev.mars.apex.core.config.yaml;
    exports dev.mars.apex.core.service.yaml;

    // New exports for performance monitoring
    exports dev.mars.apex.core.service.monitoring;

    // Enhanced API exports for simplified configuration
    // Note: dev.mars.apex.core.api is already exported above

    // Export other existing services
    exports dev.mars.apex.core.service.engine;
    exports dev.mars.apex.core.service.data;
    exports dev.mars.apex.core.service.transform;
    exports dev.mars.apex.core.service.enrichment;
    exports dev.mars.apex.core.service.scenario;
    exports dev.mars.apex.core.util;

    // External data source exports
    exports dev.mars.apex.core.service.data.external;
    exports dev.mars.apex.core.config.datasource;
    exports dev.mars.apex.core.service.data.external.registry;
    exports dev.mars.apex.core.service.data.external.factory;
    exports dev.mars.apex.core.service.data.external.manager;
    exports dev.mars.apex.core.service.data.external.config;
    exports dev.mars.apex.core.service.data.external.cache;
    exports dev.mars.apex.core.service.data.external.file;

    // Unified caching exports
    exports dev.mars.apex.core.cache;

    // Data sink exports
    exports dev.mars.apex.core.config.datasink;
    exports dev.mars.apex.core.service.data.external.database;

    // Pipeline engine exports
    exports dev.mars.apex.core.engine.pipeline;

    // Pipeline configuration exports
    exports dev.mars.apex.core.config.pipeline;

    // Phase 1.2 exports - Input Data Classification System
    exports dev.mars.apex.core.engine;
    exports dev.mars.apex.core.service.classification;

}
