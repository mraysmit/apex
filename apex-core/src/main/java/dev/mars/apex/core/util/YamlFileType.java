package dev.mars.apex.core.util;

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
 * Enumeration of YAML file types in the APEX system.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-28
 * @version 1.0
 */
public enum YamlFileType {
    SCENARIO("Scenario Configuration"),
    RULE_CONFIG("Rule Configuration"),
    RULE_CHAIN("Rule Chain"),
    ENRICHMENT("Enrichment Configuration"),
    DATASET("Dataset Configuration"),
    BOOTSTRAP("Bootstrap Configuration"),
    UNKNOWN("Unknown Type");
    
    private final String description;
    
    YamlFileType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    @Override
    public String toString() {
        return description;
    }
}
