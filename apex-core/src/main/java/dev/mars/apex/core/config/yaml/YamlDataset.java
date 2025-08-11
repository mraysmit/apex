package dev.mars.apex.core.config.yaml;

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
