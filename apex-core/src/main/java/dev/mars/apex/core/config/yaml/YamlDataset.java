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


import java.util.List;
import java.util.Map;

/**
 * Configuration class for YAML dataset files.
 * 
 * Represents the structure of a YAML dataset file as described in the Data Management Guide.
 * A dataset file contains metadata and data sections.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
public class YamlDataset {
    
    private Map<String, Object> metadata;
    private List<Map<String, Object>> data;
    
    /**
     * Gets the metadata section of the dataset.
     * 
     * @return metadata map containing name, version, description, type, etc.
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    /**
     * Sets the metadata section of the dataset.
     * 
     * @param metadata metadata map
     */
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    /**
     * Gets the data section of the dataset.
     * 
     * @return list of data records
     */
    public List<Map<String, Object>> getData() {
        return data;
    }
    
    /**
     * Sets the data section of the dataset.
     * 
     * @param data list of data records
     */
    public void setData(List<Map<String, Object>> data) {
        this.data = data;
    }
    
    /**
     * Gets the dataset name from metadata.
     * 
     * @return dataset name or null if not set
     */
    public String getName() {
        return metadata != null ? (String) metadata.get("name") : null;
    }
    
    /**
     * Gets the dataset version from metadata.
     * 
     * @return dataset version or null if not set
     */
    public String getVersion() {
        return metadata != null ? (String) metadata.get("version") : null;
    }
    
    /**
     * Gets the dataset type from metadata.
     * 
     * @return dataset type or null if not set
     */
    public String getType() {
        return metadata != null ? (String) metadata.get("type") : null;
    }
    
    /**
     * Gets the dataset description from metadata.
     * 
     * @return dataset description or null if not set
     */
    public String getDescription() {
        return metadata != null ? (String) metadata.get("description") : null;
    }
}
