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
 * Statistics about a dependency graph.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-28
 * @version 1.0
 */
public class DependencyStatistics {
    private final int totalFiles;
    private final int maxDepth;
    private final int missingFiles;
    private final int invalidFiles;
    private final int circularDependencies;
    private final int totalDependencies;
    
    public DependencyStatistics(int totalFiles, int maxDepth, int missingFiles, 
                               int invalidFiles, int circularDependencies, int totalDependencies) {
        this.totalFiles = totalFiles;
        this.maxDepth = maxDepth;
        this.missingFiles = missingFiles;
        this.invalidFiles = invalidFiles;
        this.circularDependencies = circularDependencies;
        this.totalDependencies = totalDependencies;
    }
    
    // Getters
    public int getTotalFiles() { return totalFiles; }
    public int getMaxDepth() { return maxDepth; }
    public int getMissingFiles() { return missingFiles; }
    public int getInvalidFiles() { return invalidFiles; }
    public int getCircularDependencies() { return circularDependencies; }
    public int getTotalDependencies() { return totalDependencies; }
    
    public boolean isHealthy() {
        return missingFiles == 0 && invalidFiles == 0 && circularDependencies == 0;
    }
    
    @Override
    public String toString() {
        return "DependencyStatistics{" +
                "totalFiles=" + totalFiles +
                ", maxDepth=" + maxDepth +
                ", missingFiles=" + missingFiles +
                ", invalidFiles=" + invalidFiles +
                ", circularDependencies=" + circularDependencies +
                ", totalDependencies=" + totalDependencies +
                ", healthy=" + isHealthy() +
                '}';
    }
}
