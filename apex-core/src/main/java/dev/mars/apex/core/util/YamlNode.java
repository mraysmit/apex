package dev.mars.apex.core.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single YAML file node in the dependency graph.
 * 
 * Each node contains information about a YAML file including its path,
 * type, existence status, validity, and its relationships to other files.
 * 
 * @author APEX Rules Engine Team
 * @since 1.0.0
 */
public class YamlNode {
    
    private final String filePath;
    private YamlFileType fileType;
    private boolean exists;
    private boolean yamlValid;
    private List<String> referencedFiles;
    private List<String> referencedBy;
    
    public YamlNode(String filePath) {
        this.filePath = filePath;
        this.fileType = YamlFileType.UNKNOWN;
        this.exists = false;
        this.yamlValid = false;
        this.referencedFiles = new ArrayList<>();
        this.referencedBy = new ArrayList<>();
    }
    
    public YamlNode(String filePath, YamlFileType fileType) {
        this(filePath);
        this.fileType = fileType;
    }
    
    // Getters and Setters
    public String getFilePath() {
        return filePath;
    }
    
    public YamlFileType getFileType() {
        return fileType;
    }
    
    public void setFileType(YamlFileType fileType) {
        this.fileType = fileType;
    }
    
    public boolean exists() {
        return exists;
    }
    
    public void setExists(boolean exists) {
        this.exists = exists;
    }
    
    public boolean isYamlValid() {
        return yamlValid;
    }
    
    public void setYamlValid(boolean yamlValid) {
        this.yamlValid = yamlValid;
    }
    
    public List<String> getReferencedFiles() {
        return referencedFiles;
    }
    
    public void setReferencedFiles(List<String> referencedFiles) {
        this.referencedFiles = referencedFiles != null ? referencedFiles : new ArrayList<>();
    }
    
    public List<String> getReferencedBy() {
        return referencedBy;
    }
    
    public void setReferencedBy(List<String> referencedBy) {
        this.referencedBy = referencedBy != null ? referencedBy : new ArrayList<>();
    }
    
    public void addReferencedBy(String filePath) {
        if (!this.referencedBy.contains(filePath)) {
            this.referencedBy.add(filePath);
        }
    }
    
    public void addReferencedFile(String filePath) {
        if (!this.referencedFiles.contains(filePath)) {
            this.referencedFiles.add(filePath);
        }
    }
    
    /**
     * Checks if this node represents a valid, existing YAML file.
     */
    public boolean isValid() {
        return exists && yamlValid;
    }
    
    /**
     * Checks if this node has any dependencies.
     */
    public boolean hasDependencies() {
        return referencedFiles != null && !referencedFiles.isEmpty();
    }
    
    /**
     * Checks if this node is referenced by other files.
     */
    public boolean isReferenced() {
        return referencedBy != null && !referencedBy.isEmpty();
    }
    
    /**
     * Gets the number of files this node references.
     */
    public int getDependencyCount() {
        return referencedFiles != null ? referencedFiles.size() : 0;
    }
    
    /**
     * Gets the number of files that reference this node.
     */
    public int getReferenceCount() {
        return referencedBy != null ? referencedBy.size() : 0;
    }
    
    /**
     * Gets a status indicator for this node.
     */
    public String getStatusIndicator() {
        if (!exists) {
            return "✗ (missing)";
        } else if (!yamlValid) {
            return "⚠ (invalid YAML)";
        } else {
            return "✓";
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        YamlNode yamlNode = (YamlNode) o;
        return filePath.equals(yamlNode.filePath);
    }
    
    @Override
    public int hashCode() {
        return filePath.hashCode();
    }
    
    @Override
    public String toString() {
        return "YamlNode{" +
                "filePath='" + filePath + '\'' +
                ", fileType=" + fileType +
                ", exists=" + exists +
                ", yamlValid=" + yamlValid +
                ", dependencies=" + getDependencyCount() +
                ", references=" + getReferenceCount() +
                '}';
    }
}


