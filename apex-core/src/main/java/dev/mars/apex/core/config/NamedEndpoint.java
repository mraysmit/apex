package dev.mars.apex.core.config;

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

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Model class for named endpoint definitions in array format.
 * 
 * Used for REST API endpoint configurations with metadata.
 * 
 * <p>Example YAML:
 * <pre>
 * endpoints:
 *   - id: "EP-001"
 *     name: "getCustomer"
 *     endpoint: "/api/customers/{id}"
 *     description: "Retrieve customer by ID"
 *     method: "GET"
 *     parameters: ["id"]
 *     tags: ["customer", "rest-api"]
 * </pre>
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2026-01-16
 * @version 1.0
 */
public class NamedEndpoint {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("endpoint")
    private String endpoint;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("method")
    private String method;
    
    @JsonProperty("parameters")
    private List<String> parameters = new ArrayList<>();
    
    @JsonProperty("tags")
    private List<String> tags = new ArrayList<>();
    
    @JsonProperty("version")
    private String version;
    
    @JsonProperty("deprecated")
    private boolean deprecated = false;
    
    @JsonProperty("deprecation-message")
    private String deprecationMessage;
    
    public NamedEndpoint() {
    }
    
    public NamedEndpoint(String name, String endpoint) {
        this.name = name;
        this.endpoint = endpoint;
    }
    
    // Getters and Setters
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getEndpoint() {
        return endpoint;
    }
    
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getMethod() {
        return method;
    }
    
    public void setMethod(String method) {
        this.method = method;
    }
    
    public List<String> getParameters() {
        return parameters;
    }
    
    public void setParameters(List<String> parameters) {
        this.parameters = parameters != null ? parameters : new ArrayList<>();
    }
    
    public List<String> getTags() {
        return tags;
    }
    
    public void setTags(List<String> tags) {
        this.tags = tags != null ? tags : new ArrayList<>();
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    public boolean isDeprecated() {
        return deprecated;
    }
    
    public void setDeprecated(boolean deprecated) {
        this.deprecated = deprecated;
    }
    
    public String getDeprecationMessage() {
        return deprecationMessage;
    }
    
    public void setDeprecationMessage(String deprecationMessage) {
        this.deprecationMessage = deprecationMessage;
    }
    
    public void validate() throws YamlConfigurationException {
        if (name == null || name.trim().isEmpty()) {
            throw new YamlConfigurationException("Named endpoint must have a 'name' field");
        }
        if (endpoint == null || endpoint.trim().isEmpty()) {
            throw new YamlConfigurationException("Named endpoint '" + name + "' must have an 'endpoint' field");
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NamedEndpoint that = (NamedEndpoint) o;
        return Objects.equals(name, that.name);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
    
    @Override
    public String toString() {
        return "NamedEndpoint{" +
               "id='" + id + '\'' +
               ", name='" + name + '\'' +
               ", endpoint='" + endpoint + '\'' +
               ", method='" + method + '\'' +
               ", description='" + description + '\'' +
               ", version='" + version + '\'' +
               ", deprecated=" + deprecated +
               '}';
    }
}
