package dev.mars.apex.yaml.manager.model;

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

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Summary of YAML file contents.
 * Provides high-level statistics about YAML configuration files.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ContentSummary {
    private String filePath;
    private String fileType; // rules, enrichments, scenario, config, etc.
    private String id;
    private String name;
    private String description;
    private String version;
    private String author;
    private String businessDomain;
    private String owner;
    private String createdDate;
    private String lastModifiedDate;
    private List<String> tags;
    private List<String> categories; // Category names defined in this file
    private List<String> dependencies; // Files this config depends on
    private int ruleCount;
    private int ruleGroupCount;
    private int enrichmentCount;
    private int categoryCount;
    private int configFileCount;
    private int referenceCount;
    private Map<String, Integer> contentCounts;
    private String rawContent; // The actual YAML file content

    public ContentSummary() {
        this.contentCounts = new HashMap<>();
        this.tags = new ArrayList<>();
        this.categories = new ArrayList<>();
        this.dependencies = new ArrayList<>();
    }

    public ContentSummary(String filePath) {
        this();
        this.filePath = filePath;
    }

    // Getters and Setters
    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    public String getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(String lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    public String getBusinessDomain() {
        return businessDomain;
    }

    public void setBusinessDomain(String businessDomain) {
        this.businessDomain = businessDomain;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<String> dependencies) {
        this.dependencies = dependencies;
    }

    public int getRuleCount() {
        return ruleCount;
    }

    public void setRuleCount(int ruleCount) {
        this.ruleCount = ruleCount;
    }

    public int getRuleGroupCount() {
        return ruleGroupCount;
    }

    public void setRuleGroupCount(int ruleGroupCount) {
        this.ruleGroupCount = ruleGroupCount;
    }

    public int getEnrichmentCount() {
        return enrichmentCount;
    }

    public void setEnrichmentCount(int enrichmentCount) {
        this.enrichmentCount = enrichmentCount;
    }

    public int getCategoryCount() {
        return categoryCount;
    }

    public void setCategoryCount(int categoryCount) {
        this.categoryCount = categoryCount;
    }

    public int getConfigFileCount() {
        return configFileCount;
    }

    public void setConfigFileCount(int configFileCount) {
        this.configFileCount = configFileCount;
    }

    public int getReferenceCount() {
        return referenceCount;
    }

    public void setReferenceCount(int referenceCount) {
        this.referenceCount = referenceCount;
    }

    public Map<String, Integer> getContentCounts() {
        return contentCounts;
    }

    public void setContentCounts(Map<String, Integer> contentCounts) {
        this.contentCounts = contentCounts;
    }

    public void addContentCount(String key, int count) {
        this.contentCounts.put(key, count);
    }

    public String getRawContent() {
        return rawContent;
    }

    public void setRawContent(String rawContent) {
        this.rawContent = rawContent;
    }

    @Override
    public String toString() {
        return "ContentSummary{" +
                "filePath='" + filePath + '\'' +
                ", fileType='" + fileType + '\'' +
                ", id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", ruleCount=" + ruleCount +
                ", ruleGroupCount=" + ruleGroupCount +
                ", enrichmentCount=" + enrichmentCount +
                ", configFileCount=" + configFileCount +
                ", referenceCount=" + referenceCount +
                '}';
    }
}

