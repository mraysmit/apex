package dev.mars.apex.core.exception;

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
 * Exception thrown when there are issues with rule configuration.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
public class RuleConfigurationException extends RuleEngineException {
    private static final long serialVersionUID = 1L;
    
    private final String configurationElement;
    private final String expectedFormat;
    
    public RuleConfigurationException(String configurationElement, String message) {
        super("RULE_CONFIGURATION_ERROR", message, "Configuration element: " + configurationElement);
        this.configurationElement = configurationElement;
        this.expectedFormat = null;
    }
    
    public RuleConfigurationException(String configurationElement, String message, String expectedFormat) {
        super("RULE_CONFIGURATION_ERROR", message, "Configuration element: " + configurationElement);
        this.configurationElement = configurationElement;
        this.expectedFormat = expectedFormat;
    }
    
    public RuleConfigurationException(String configurationElement, String message, Throwable cause) {
        super("RULE_CONFIGURATION_ERROR", message, "Configuration element: " + configurationElement, cause);
        this.configurationElement = configurationElement;
        this.expectedFormat = null;
    }
    
    public String getConfigurationElement() {
        return configurationElement;
    }
    
    public String getExpectedFormat() {
        return expectedFormat;
    }
    
    @Override
    public String getDetailedMessage() {
        StringBuilder sb = new StringBuilder(super.getDetailedMessage());
        if (expectedFormat != null && !expectedFormat.trim().isEmpty()) {
            sb.append("\nExpected format: ").append(expectedFormat);
        }
        return sb.toString();
    }
}
