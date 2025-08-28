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
 * Represents a dependency relationship between two YAML files.
 * 
 * This class captures the relationship where one YAML file (source) references
 * another YAML file (target), including the type of reference and the location
 * within the YAML structure where the reference occurs.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
public class YamlDependency {
    
    private final String sourceFile;
    private final String targetFile;
    private final String referenceType;
    private String yamlPath;
    
    public YamlDependency(String sourceFile, String targetFile, String referenceType) {
        this.sourceFile = sourceFile;
        this.targetFile = targetFile;
        this.referenceType = referenceType;
    }
    
    public YamlDependency(String sourceFile, String targetFile, String referenceType, String yamlPath) {
        this(sourceFile, targetFile, referenceType);
        this.yamlPath = yamlPath;
    }
    
    // Getters
    public String getSourceFile() {
        return sourceFile;
    }
    
    public String getTargetFile() {
        return targetFile;
    }
    
    public String getReferenceType() {
        return referenceType;
    }
    
    public String getYamlPath() {
        return yamlPath;
    }
    
    public void setYamlPath(String yamlPath) {
        this.yamlPath = yamlPath;
    }
    
    /**
     * Gets a human-readable description of this dependency.
     */
    public String getDescription() {
        StringBuilder desc = new StringBuilder();
        desc.append(sourceFile).append(" → ").append(targetFile);
        
        if (referenceType != null) {
            desc.append(" (").append(referenceType).append(")");
        }
        
        if (yamlPath != null) {
            desc.append(" at ").append(yamlPath);
        }
        
        return desc.toString();
    }
    
    /**
     * Checks if this dependency represents a direct reference.
     */
    public boolean isDirect() {
        return "yaml-reference".equals(referenceType) || 
               "rule-configurations".equals(referenceType) ||
               "rule-chains".equals(referenceType) ||
               "enrichment-refs".equals(referenceType);
    }
    
    /**
     * Checks if this dependency represents an include/import.
     */
    public boolean isInclude() {
        return "include".equals(referenceType) || "import".equals(referenceType);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        YamlDependency that = (YamlDependency) o;
        
        if (!sourceFile.equals(that.sourceFile)) return false;
        if (!targetFile.equals(that.targetFile)) return false;
        return referenceType.equals(that.referenceType);
    }
    
    @Override
    public int hashCode() {
        int result = sourceFile.hashCode();
        result = 31 * result + targetFile.hashCode();
        result = 31 * result + referenceType.hashCode();
        return result;
    }
    
    @Override
    public String toString() {
        return "YamlDependency{" +
                "sourceFile='" + sourceFile + '\'' +
                ", targetFile='" + targetFile + '\'' +
                ", referenceType='" + referenceType + '\'' +
                ", yamlPath='" + yamlPath + '\'' +
                '}';
    }
}
