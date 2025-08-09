package dev.mars.apex.core.util;

/**
 * Statistics about a dependency graph.
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
