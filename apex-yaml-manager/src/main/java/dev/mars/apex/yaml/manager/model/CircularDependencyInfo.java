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

import java.util.*;

/**
 * Represents information about a circular dependency cycle.
 *
 * Contains:
 * - The cycle path (ordered list of files forming the cycle)
 * - Cycle length (number of files in cycle)
 * - All files involved in the cycle
 * - Severity level (CRITICAL, HIGH, MEDIUM, LOW)
 * - Suggested resolution strategies
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-10-18
 * @version 1.0
 */
public class CircularDependencyInfo {

    public enum Severity {
        CRITICAL,  // Self-referencing or 2-file cycle
        HIGH,      // 3-4 file cycle
        MEDIUM,    // 5-10 file cycle
        LOW        // 11+ file cycle
    }

    private final List<String> cyclePath;
    private final int cycleLength;
    private final Set<String> filesInCycle;
    private final Severity severity;
    private final long detectedAt;
    private String resolutionStrategy;

    /**
     * Create circular dependency info from a cycle path.
     */
    public CircularDependencyInfo(List<String> cyclePath) {
        this.cyclePath = new ArrayList<>(cyclePath);
        this.cycleLength = cyclePath.size();
        this.filesInCycle = new HashSet<>(cyclePath);
        this.severity = calculateSeverity(cycleLength);
        this.detectedAt = System.currentTimeMillis();
    }

    /**
     * Calculate severity based on cycle length.
     */
    private Severity calculateSeverity(int length) {
        if (length <= 2) {
            return Severity.CRITICAL;
        } else if (length <= 4) {
            return Severity.HIGH;
        } else if (length <= 10) {
            return Severity.MEDIUM;
        } else {
            return Severity.LOW;
        }
    }

    /**
     * Get the cycle path as a formatted string.
     * Example: "rules.yaml -> enrichments.yaml -> rules.yaml"
     */
    public String getCyclePathAsString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cyclePath.size(); i++) {
            sb.append(cyclePath.get(i));
            if (i < cyclePath.size() - 1) {
                sb.append(" -> ");
            }
        }
        // Complete the cycle
        if (!cyclePath.isEmpty()) {
            sb.append(" -> ").append(cyclePath.get(0));
        }
        return sb.toString();
    }

    /**
     * Check if a file is part of this cycle.
     */
    public boolean containsFile(String filePath) {
        return filesInCycle.contains(filePath);
    }

    /**
     * Get the position of a file in the cycle path.
     */
    public int getFilePosition(String filePath) {
        return cyclePath.indexOf(filePath);
    }

    /**
     * Get the next file in the cycle after the given file.
     */
    public String getNextFileInCycle(String filePath) {
        int index = cyclePath.indexOf(filePath);
        if (index < 0) {
            return null;
        }
        return cyclePath.get((index + 1) % cyclePath.size());
    }

    /**
     * Get the previous file in the cycle before the given file.
     */
    public String getPreviousFileInCycle(String filePath) {
        int index = cyclePath.indexOf(filePath);
        if (index < 0) {
            return null;
        }
        return cyclePath.get((index - 1 + cyclePath.size()) % cyclePath.size());
    }

    /**
     * Suggest a resolution strategy for breaking this cycle.
     */
    public String suggestResolution() {
        if (resolutionStrategy != null) {
            return resolutionStrategy;
        }

        if (cycleLength == 1) {
            resolutionStrategy = String.format(
                "Self-referencing cycle detected in '%s'. Remove the self-reference.",
                cyclePath.get(0)
            );
        } else if (cycleLength == 2) {
            resolutionStrategy = String.format(
                "Bidirectional cycle between '%s' and '%s'. " +
                "Consider extracting common dependencies into a separate file.",
                cyclePath.get(0), cyclePath.get(1)
            );
        } else {
            String breakPoint = cyclePath.get(0);
            String nextFile = cyclePath.get(1);
            resolutionStrategy = String.format(
                "Cycle of %d files detected: %s. " +
                "Consider breaking the cycle by removing the dependency from '%s' to '%s'.",
                cycleLength, getCyclePathAsString(), breakPoint, nextFile
            );
        }

        return resolutionStrategy;
    }

    // Getters
    public List<String> getCyclePath() {
        return new ArrayList<>(cyclePath);
    }

    public int getCycleLength() {
        return cycleLength;
    }

    public Set<String> getFilesInCycle() {
        return new HashSet<>(filesInCycle);
    }

    public Severity getSeverity() {
        return severity;
    }

    public long getDetectedAt() {
        return detectedAt;
    }

    public void setResolutionStrategy(String strategy) {
        this.resolutionStrategy = strategy;
    }

    @Override
    public String toString() {
        return String.format(
            "CircularDependencyInfo{path=%s, severity=%s, length=%d}",
            getCyclePathAsString(), severity, cycleLength
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CircularDependencyInfo that = (CircularDependencyInfo) o;
        return cycleLength == that.cycleLength &&
               filesInCycle.equals(that.filesInCycle);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cycleLength, filesInCycle);
    }
}

