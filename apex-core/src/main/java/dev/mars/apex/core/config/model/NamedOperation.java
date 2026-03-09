package dev.mars.apex.core.config.model;

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

import dev.mars.apex.core.config.exception.ConfigurationException;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Model class for named operation definitions in array format.
 * 
 * Similar to NamedQuery but used for data sink operations.
 * 
 * <p>Example YAML:
 * <pre>
 * operations:
 *   - id: "OP-001"
 *     name: "insertCustomer"
 *     description: "Insert new customer record"
 *     query: "INSERT INTO customers (name, email) VALUES (:name, :email)"
 *     parameters: ["name", "email"]
 *     tags: ["customer", "write"]
 * </pre>
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2026-01-16
 * @version 1.0
 */
public class NamedOperation {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("query")
    private String query;
    
    @JsonProperty("description")
    private String description;
    
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
    
    public NamedOperation() {
    }
    
    public NamedOperation(String name, String query) {
        this.name = name;
        this.query = query;
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
    
    public String getQuery() {
        return query;
    }
    
    public void setQuery(String query) {
        this.query = query;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
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
    
    public void validate() throws ConfigurationException {
        if (name == null || name.trim().isEmpty()) {
            throw new ConfigurationException("Named operation must have a 'name' field");
        }
        if (query == null || query.trim().isEmpty()) {
            throw new ConfigurationException("Named operation '" + name + "' must have a 'query' field");
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NamedOperation that = (NamedOperation) o;
        return Objects.equals(name, that.name);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
    
    @Override
    public String toString() {
        return "NamedOperation{" +
               "id='" + id + '\'' +
               ", name='" + name + '\'' +
               ", description='" + description + '\'' +
               ", version='" + version + '\'' +
               ", deprecated=" + deprecated +
               '}';
    }
}
