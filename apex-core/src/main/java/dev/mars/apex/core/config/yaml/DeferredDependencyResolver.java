package dev.mars.apex.core.config.yaml;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deferred dependency resolver for sequential YAML processing.
 * 
 * Handles forward references and complex dependencies in YAML configurations
 * where sections reference elements that are defined later in the document.
 * 
 * Key Features:
 * - Forward reference detection and resolution
 * - Circular dependency detection and prevention
 * - Deferred processing queue management
 * - Dependency graph analysis
 * - Error reporting for unresolvable dependencies
 * 
 * Common Dependency Scenarios:
 * 1. Rules referencing enrichments defined later
 * 2. Enrichments referencing data sources defined later
 * 3. Rule groups referencing rules defined later
 * 4. Cross-section references and complex chains
 * 
 * @author APEX Sequential Processing Implementation
 * @since 1.0
 * @version 1.0
 */
public class DeferredDependencyResolver {
    
    private static final Logger logger = LoggerFactory.getLogger(DeferredDependencyResolver.class);
    
    // Patterns for detecting references in YAML content
    private static final Pattern FIELD_REFERENCE_PATTERN = Pattern.compile("#([a-zA-Z_][a-zA-Z0-9_.]*)");
    // Dependency tracking
    private final Map<String, Set<String>> dependencies;
    private final Map<String, Set<String>> reverseDependencies;
    private final Set<String> processedSections;
    private final Queue<DeferredSection> deferredQueue;
    private final Set<String> availableIds;
    
    /**
     * Create dependency resolver.
     */
    public DeferredDependencyResolver() {
        this.dependencies = new HashMap<>();
        this.reverseDependencies = new HashMap<>();
        this.processedSections = new HashSet<>();
        this.deferredQueue = new LinkedList<>();
        this.availableIds = new HashSet<>();
        
        logger.info("DeferredDependencyResolver initialized for forward reference handling");
    }
    
    /**
     * Analyze section for dependencies before processing.
     * 
     * @param sectionName Name of the section
     * @param sectionContent Content of the section
     * @return Dependency analysis result
     */
    public DependencyAnalysis analyzeDependencies(String sectionName, Object sectionContent) {
        logger.debug("Analyzing dependencies for section: " + sectionName);
        
        Set<String> requiredIds = extractRequiredIds(sectionContent);
        Set<String> providedIds = extractProvidedIds(sectionName, sectionContent);
        Set<String> unresolvedDependencies = new HashSet<>();
        
        // Check which required IDs are not yet available
        for (String requiredId : requiredIds) {
            if (!availableIds.contains(requiredId)) {
                unresolvedDependencies.add(requiredId);
            }
        }
        
        // Update dependency tracking
        dependencies.put(sectionName, requiredIds);
        
        // Update reverse dependencies
        for (String requiredId : requiredIds) {
            reverseDependencies.computeIfAbsent(requiredId, k -> new HashSet<>()).add(sectionName);
        }
        
        // Add provided IDs to available set
        availableIds.addAll(providedIds);
        
        DependencyAnalysis analysis = new DependencyAnalysis(
            sectionName, requiredIds, providedIds, unresolvedDependencies
        );
        
        logger.debug("Dependency analysis for " + sectionName + ": " + analysis);
        
        return analysis;
    }
    
    /**
     * Check if section can be processed immediately or needs to be deferred.
     * 
     * @param analysis Dependency analysis result
     * @return true if section can be processed now
     */
    public boolean canProcessImmediately(DependencyAnalysis analysis) {
        return analysis.getUnresolvedDependencies().isEmpty();
    }
    
    /**
     * Defer section processing until dependencies are resolved.
     * 
     * @param sectionName Section name
     * @param sectionContent Section content
     * @param analysis Dependency analysis
     */
    public void deferSection(String sectionName, Object sectionContent, DependencyAnalysis analysis) {
        DeferredSection deferred = new DeferredSection(sectionName, sectionContent, analysis);
        deferredQueue.offer(deferred);
        
        logger.info("Deferred section '" + sectionName + "' due to unresolved dependencies: " + 
                   analysis.getUnresolvedDependencies());
    }
    
    /**
     * Mark section as processed and check if any deferred sections can now be processed.
     * 
     * @param sectionName Processed section name
     * @param providedIds IDs provided by this section
     * @return List of sections that can now be processed
     */
    public List<DeferredSection> markSectionProcessed(String sectionName, Set<String> providedIds) {
        processedSections.add(sectionName);
        availableIds.addAll(providedIds);
        
        logger.debug("Section '" + sectionName + "' processed, provided IDs: " + providedIds);
        
        // Check deferred queue for sections that can now be processed
        List<DeferredSection> readySections = new ArrayList<>();
        Iterator<DeferredSection> iterator = deferredQueue.iterator();
        
        while (iterator.hasNext()) {
            DeferredSection deferred = iterator.next();
            
            // Recheck dependencies
            Set<String> stillUnresolved = new HashSet<>();
            for (String requiredId : deferred.getAnalysis().getUnresolvedDependencies()) {
                if (!availableIds.contains(requiredId)) {
                    stillUnresolved.add(requiredId);
                }
            }
            
            if (stillUnresolved.isEmpty()) {
                // All dependencies resolved
                readySections.add(deferred);
                iterator.remove();
                logger.info("Section '" + deferred.getSectionName() + "' dependencies resolved, ready for processing");
            } else {
                // Update unresolved dependencies
                deferred.getAnalysis().setUnresolvedDependencies(stillUnresolved);
            }
        }
        
        return readySections;
    }
    
    /**
     * Check for circular dependencies in the dependency graph.
     * 
     * @return Circular dependency detection result
     */
    public CircularDependencyResult detectCircularDependencies() {
        logger.debug("Checking for circular dependencies...");
        
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();
        List<String> circularPath = new ArrayList<>();
        
        for (String section : dependencies.keySet()) {
            if (!visited.contains(section)) {
                if (hasCircularDependency(section, visited, recursionStack, circularPath)) {
                    logger.warn("Circular dependency detected: " + circularPath);
                    return new CircularDependencyResult(true, new ArrayList<>(circularPath));
                }
            }
        }
        
        return new CircularDependencyResult(false, Collections.emptyList());
    }
    
    /**
     * Get current status of dependency resolution.
     * 
     * @return Dependency resolution status
     */
    public DependencyResolutionStatus getResolutionStatus() {
        int totalSections = dependencies.size();
        int processedCount = processedSections.size();
        int deferredCount = deferredQueue.size();
        boolean hasUnresolvable = false;
        
        // Check for unresolvable dependencies
        for (DeferredSection deferred : deferredQueue) {
            if (!deferred.getAnalysis().getUnresolvedDependencies().isEmpty()) {
                // Check if any unresolved dependency will never be available
                for (String unresolvedId : deferred.getAnalysis().getUnresolvedDependencies()) {
                    if (!willIdBeProvided(unresolvedId)) {
                        hasUnresolvable = true;
                        break;
                    }
                }
            }
        }
        
        return new DependencyResolutionStatus(
            totalSections, processedCount, deferredCount, hasUnresolvable, 
            new ArrayList<>(deferredQueue)
        );
    }
    
    /**
     * Extract required IDs from section content.
     */
    private Set<String> extractRequiredIds(Object sectionContent) {
        Set<String> requiredIds = new HashSet<>();

        if (sectionContent == null) {
            return requiredIds;
        }

        String contentStr = sectionContent.toString();

        // Extract field references (#enrichment-id.field)
        Matcher fieldMatcher = FIELD_REFERENCE_PATTERN.matcher(contentStr);
        while (fieldMatcher.find()) {
            String fieldRef = fieldMatcher.group(1);
            // Convert field references to potential ID references
            if (fieldRef.contains(".")) {
                String[] parts = fieldRef.split("\\.");
                String potentialId = parts[0];
                // Only add if it looks like an enrichment/rule ID (contains dash or underscore)
                if (potentialId.contains("-") || potentialId.contains("_")) {
                    requiredIds.add(potentialId);
                }
            }
        }

        // Look for data-source references
        if (contentStr.contains("data-source:")) {
            Pattern dataSourcePattern = Pattern.compile("data-source:\\s*\"([^\"]+)\"");
            Matcher matcher = dataSourcePattern.matcher(contentStr);
            while (matcher.find()) {
                requiredIds.add(matcher.group(1));
            }
        }

        // Look for depends_on references
        if (contentStr.contains("depends_on:")) {
            Pattern dependsPattern = Pattern.compile("depends_on:\\s*\"([^\"]+)\"");
            Matcher matcher = dependsPattern.matcher(contentStr);
            while (matcher.find()) {
                requiredIds.add(matcher.group(1));
            }
        }

        return requiredIds;
    }
    
    /**
     * Extract provided IDs from section content.
     */
    private Set<String> extractProvidedIds(String sectionName, Object sectionContent) {
        Set<String> providedIds = new HashSet<>();

        if (sectionContent != null) {
            String contentStr = sectionContent.toString();

            // Look for id: "value" patterns
            Pattern idPattern = Pattern.compile("id:\\s*\"([^\"]+)\"");
            Matcher matcher = idPattern.matcher(contentStr);
            while (matcher.find()) {
                providedIds.add(matcher.group(1));
            }

            // Look for name: "value" patterns (for data sources)
            Pattern namePattern = Pattern.compile("name:\\s*\"([^\"]+)\"");
            Matcher nameMatcher = namePattern.matcher(contentStr);
            while (nameMatcher.find()) {
                providedIds.add(nameMatcher.group(1));
            }
        }

        return providedIds;
    }
    
    /**
     * Check if a section has circular dependencies using DFS.
     */
    private boolean hasCircularDependency(String section, Set<String> visited, 
                                        Set<String> recursionStack, List<String> path) {
        visited.add(section);
        recursionStack.add(section);
        path.add(section);
        
        Set<String> sectionDeps = dependencies.get(section);
        if (sectionDeps != null) {
            for (String dep : sectionDeps) {
                if (!visited.contains(dep)) {
                    if (hasCircularDependency(dep, visited, recursionStack, path)) {
                        return true;
                    }
                } else if (recursionStack.contains(dep)) {
                    // Found circular dependency
                    path.add(dep);
                    return true;
                }
            }
        }
        
        recursionStack.remove(section);
        path.remove(path.size() - 1);
        return false;
    }
    
    /**
     * Check if an ID will be provided by any remaining section.
     */
    private boolean willIdBeProvided(String id) {
        // This is a simplified check
        // In a real implementation, this would analyze remaining sections
        // to determine if the ID will be provided
        return false;
    }
    
    // Inner classes for data structures
    
    /**
     * Dependency analysis result.
     */
    public static class DependencyAnalysis {
        private final String sectionName;
        private final Set<String> requiredIds;
        private final Set<String> providedIds;
        private Set<String> unresolvedDependencies;
        
        public DependencyAnalysis(String sectionName, Set<String> requiredIds, 
                                Set<String> providedIds, Set<String> unresolvedDependencies) {
            this.sectionName = sectionName;
            this.requiredIds = new HashSet<>(requiredIds);
            this.providedIds = new HashSet<>(providedIds);
            this.unresolvedDependencies = new HashSet<>(unresolvedDependencies);
        }
        
        // Getters
        public String getSectionName() { return sectionName; }
        public Set<String> getRequiredIds() { return Collections.unmodifiableSet(requiredIds); }
        public Set<String> getProvidedIds() { return Collections.unmodifiableSet(providedIds); }
        public Set<String> getUnresolvedDependencies() { return unresolvedDependencies; }
        
        public void setUnresolvedDependencies(Set<String> unresolvedDependencies) {
            this.unresolvedDependencies = new HashSet<>(unresolvedDependencies);
        }
        
        @Override
        public String toString() {
            return "DependencyAnalysis{" +
                   "section='" + sectionName + '\'' +
                   ", required=" + requiredIds +
                   ", provided=" + providedIds +
                   ", unresolved=" + unresolvedDependencies +
                   '}';
        }
    }
    
    /**
     * Deferred section waiting for dependency resolution.
     */
    public static class DeferredSection {
        private final String sectionName;
        private final Object sectionContent;
        private final DependencyAnalysis analysis;
        
        public DeferredSection(String sectionName, Object sectionContent, DependencyAnalysis analysis) {
            this.sectionName = sectionName;
            this.sectionContent = sectionContent;
            this.analysis = analysis;
        }
        
        public String getSectionName() { return sectionName; }
        public Object getSectionContent() { return sectionContent; }
        public DependencyAnalysis getAnalysis() { return analysis; }
        
        @Override
        public String toString() {
            return "DeferredSection{" +
                   "section='" + sectionName + '\'' +
                   ", unresolved=" + analysis.getUnresolvedDependencies() +
                   '}';
        }
    }
    
    /**
     * Circular dependency detection result.
     */
    public static class CircularDependencyResult {
        private final boolean hasCircularDependency;
        private final List<String> circularPath;
        
        public CircularDependencyResult(boolean hasCircularDependency, List<String> circularPath) {
            this.hasCircularDependency = hasCircularDependency;
            this.circularPath = circularPath;
        }
        
        public boolean hasCircularDependency() { return hasCircularDependency; }
        public List<String> getCircularPath() { return Collections.unmodifiableList(circularPath); }
        
        @Override
        public String toString() {
            return "CircularDependencyResult{" +
                   "hasCircular=" + hasCircularDependency +
                   ", path=" + circularPath +
                   '}';
        }
    }
    
    /**
     * Dependency resolution status.
     */
    public static class DependencyResolutionStatus {
        private final int totalSections;
        private final int processedSections;
        private final int deferredSections;
        private final boolean hasUnresolvableDependencies;
        private final List<DeferredSection> deferredQueue;
        
        public DependencyResolutionStatus(int totalSections, int processedSections, int deferredSections,
                                        boolean hasUnresolvableDependencies, List<DeferredSection> deferredQueue) {
            this.totalSections = totalSections;
            this.processedSections = processedSections;
            this.deferredSections = deferredSections;
            this.hasUnresolvableDependencies = hasUnresolvableDependencies;
            this.deferredQueue = new ArrayList<>(deferredQueue);
        }
        
        public int getTotalSections() { return totalSections; }
        public int getProcessedSections() { return processedSections; }
        public int getDeferredSections() { return deferredSections; }
        public boolean hasUnresolvableDependencies() { return hasUnresolvableDependencies; }
        public List<DeferredSection> getDeferredQueue() { return Collections.unmodifiableList(deferredQueue); }
        
        public boolean isComplete() {
            return deferredSections == 0 && !hasUnresolvableDependencies;
        }
        
        @Override
        public String toString() {
            return "DependencyResolutionStatus{" +
                   "total=" + totalSections +
                   ", processed=" + processedSections +
                   ", deferred=" + deferredSections +
                   ", unresolvable=" + hasUnresolvableDependencies +
                   '}';
        }
    }
}
