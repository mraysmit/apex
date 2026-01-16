package dev.mars.apex.core.config.yaml;

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
 * Model class for named query definitions in array format.
 * 
 * Supports rich metadata for queries including description, parameters,
 * versioning, and categorization via tags.
 * 
 * <p>Example YAML:
 * <pre>
 * queries:
 *   - id: "Q-001"
 *     name: "getCustomerById"
 *     description: "Retrieve customer profile by ID"
 *     query: "SELECT * FROM customers WHERE id = :id"
 *     parameters: ["id"]
 *     tags: ["customer", "read"]
 *     version: "1.0"
 * </pre>
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.2.0
 * @version 1.0
 */
public class NamedQuery {
    
    /**
     * Unique identifier for the query (optional, for documentation).
     */
    @JsonProperty("id")
    private String id;
    
    /**
     * Query name - used as the map key for lookups (REQUIRED).
     */
    @JsonProperty("name")
    private String name;
    
    /**
     * The actual SQL or query string (REQUIRED).
     */
    @JsonProperty("query")
    private String query;
    
    /**
     * Human-readable description of what the query does.
     */
    @JsonProperty("description")
    private String description;
    
    /**
     * List of parameter names used in the query.
     */
    @JsonProperty("parameters")
    private List<String> parameters = new ArrayList<>();
    
    /**
     * Tags for categorization and discovery.
     */
    @JsonProperty("tags")
    private List<String> tags = new ArrayList<>();
    
    /**
     * Version identifier for the query.
     */
    @JsonProperty("version")
    private String version;
    
    /**
     * Whether this query is deprecated.
     */
    @JsonProperty("deprecated")
    private boolean deprecated = false;
    
    /**
     * Deprecation message if deprecated is true.
     */
    @JsonProperty("deprecation-message")
    private String deprecationMessage;
    
    /**
     * Author or team responsible for the query.
     */
    @JsonProperty("author")
    private String author;
    
    /**
     * Last modification date/timestamp.
     */
    @JsonProperty("last-modified")
    private String lastModified;
    
    /**
     * Compliance tags (e.g., GDPR, PCI-DSS).
     */
    @JsonProperty("compliance")
    private List<String> compliance = new ArrayList<>();
    
    /**
     * Default constructor.
     */
    public NamedQuery() {
    }
    
    /**
     * Constructor with required fields.
     * 
     * @param name Query name (used as map key)
     * @param query The SQL/query string
     */
    public NamedQuery(String name, String query) {
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
    
    public String getAuthor() {
        return author;
    }
    
    public void setAuthor(String author) {
        this.author = author;
    }
    
    public String getLastModified() {
        return lastModified;
    }
    
    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }
    
    public List<String> getCompliance() {
        return compliance;
    }
    
    public void setCompliance(List<String> compliance) {
        this.compliance = compliance != null ? compliance : new ArrayList<>();
    }
    
    /**
     * Validates that required fields are present.
     * 
     * @throws YamlConfigurationException if validation fails
     */
    public void validate() throws YamlConfigurationException {
        if (name == null || name.trim().isEmpty()) {
            throw new YamlConfigurationException("Named query must have a 'name' field");
        }
        if (query == null || query.trim().isEmpty()) {
            throw new YamlConfigurationException("Named query '" + name + "' must have a 'query' field");
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NamedQuery that = (NamedQuery) o;
        return Objects.equals(name, that.name);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
    
    @Override
    public String toString() {
        return "NamedQuery{" +
               "id='" + id + '\'' +
               ", name='" + name + '\'' +
               ", description='" + description + '\'' +
               ", version='" + version + '\'' +
               ", deprecated=" + deprecated +
               '}';
    }
}
