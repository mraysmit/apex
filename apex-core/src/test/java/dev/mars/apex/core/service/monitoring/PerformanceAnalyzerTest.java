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
package dev.mars.apex.core.service.monitoring;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test coverage for PerformanceAnalyzer - utility for analyzing rule performance metrics.
 * 
 * Tests include:
 * - PerformanceInsight creation and properties
 * - analyzePerformance() with various snapshot scenarios
 * - generateRecommendations() based on insights
 * - generatePerformanceReport() formatting
 */
@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("PerformanceAnalyzer Tests")
class PerformanceAnalyzerTest {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceAnalyzerTest.class);

    private Map<String, PerformanceSnapshot> snapshots;

    @BeforeEach
    void setUp() {
        snapshots = new HashMap<>();
    }

    @Nested
    @DisplayName("PerformanceInsight Tests")
    class PerformanceInsightTests {

        @Test
        @DisplayName("Should create performance insight with all fields")
        void shouldCreatePerformanceInsightWithAllFields() {
            PerformanceAnalyzer.PerformanceInsight insight = 
                new PerformanceAnalyzer.PerformanceInsight("SLOW_RULE", "test-rule", 
                    "Rule is slow", "HIGH");
            
            assertEquals("SLOW_RULE", insight.getType());
            assertEquals("test-rule", insight.getRuleName());
            assertEquals("Rule is slow", insight.getMessage());
            assertEquals("HIGH", insight.getSeverity());
            
            logger.info("[OK] Performance insight created with all fields");
        }

        @Test
        @DisplayName("Should support fluent details API")
        void shouldSupportFluentDetailsApi() {
            PerformanceAnalyzer.PerformanceInsight insight = 
                new PerformanceAnalyzer.PerformanceInsight("HIGH_MEMORY", "test-rule", 
                    "High memory usage", "MEDIUM")
                .withDetail("averageMemory", 1024L)
                .withDetail("systemAverage", 512L);
            
            Map<String, Object> details = insight.getDetails();
            assertEquals(1024L, details.get("averageMemory"));
            assertEquals(512L, details.get("systemAverage"));
            
            logger.info("[OK] Fluent details API works");
        }

        @Test
        @DisplayName("Should return copy of details map")
        void shouldReturnCopyOfDetailsMap() {
            PerformanceAnalyzer.PerformanceInsight insight = 
                new PerformanceAnalyzer.PerformanceInsight("TEST", "rule", "message", "INFO")
                .withDetail("key", "value");
            
            Map<String, Object> details1 = insight.getDetails();
            Map<String, Object> details2 = insight.getDetails();
            
            assertNotSame(details1, details2);
            assertEquals(details1, details2);
            
            logger.info("[OK] Details map is a copy");
        }

        @Test
        @DisplayName("Should format toString correctly")
        void shouldFormatToStringCorrectly() {
            PerformanceAnalyzer.PerformanceInsight insight = 
                new PerformanceAnalyzer.PerformanceInsight("SLOW_RULE", "validation-rule", 
                    "Rule is 2x slower than average", "HIGH");
            
            String str = insight.toString();
            
            assertTrue(str.contains("HIGH"));
            assertTrue(str.contains("SLOW_RULE"));
            assertTrue(str.contains("validation-rule"));
            assertTrue(str.contains("2x slower"));
            
            logger.info("[OK] toString formats correctly: {}", str);
        }
    }

    @Nested
    @DisplayName("Analyze Performance Tests")
    class AnalyzePerformanceTests {

        @Test
        @DisplayName("Should return empty list for empty snapshots")
        void shouldReturnEmptyListForEmptySnapshots() {
            List<PerformanceAnalyzer.PerformanceInsight> insights = 
                PerformanceAnalyzer.analyzePerformance(snapshots);
            
            assertTrue(insights.isEmpty());
            
            logger.info("[OK] Empty list returned for empty snapshots");
        }

        @Test
        @DisplayName("Should detect slow rules")
        void shouldDetectSlowRules() {
            // Create a slow rule (must be 2x+ the average to be flagged)
            // Average = (10 + 100) / 2 = 55ms, threshold = 55 * 2 = 110ms
            // slow-rule at 100ms is NOT slow (below 110ms)
            // Need: (10 + X) / 2 * 2 < X, so X > 10 * 3 = 30, but bigger margin better
            // Let's use fast=10ms, slow=200ms -> avg=105, threshold=210 - not detected
            // Need 3 fast rules: (10+10+10+200)/4 = 57.5, threshold=115, 200>115 = detected
            PerformanceSnapshot fast1 = createSnapshot("fast-rule-1", 10.0, 1000, 1, 100, 0);
            PerformanceSnapshot fast2 = createSnapshot("fast-rule-2", 10.0, 1000, 1, 100, 0);
            PerformanceSnapshot fast3 = createSnapshot("fast-rule-3", 10.0, 1000, 1, 100, 0);
            PerformanceSnapshot slowRule = createSnapshot("slow-rule", 200.0, 1000, 1, 100, 0);
            
            snapshots.put("fast-rule-1", fast1);
            snapshots.put("fast-rule-2", fast2);
            snapshots.put("fast-rule-3", fast3);
            snapshots.put("slow-rule", slowRule);
            
            List<PerformanceAnalyzer.PerformanceInsight> insights = 
                PerformanceAnalyzer.analyzePerformance(snapshots);
            
            boolean hasSlowRuleInsight = insights.stream()
                .anyMatch(i -> "SLOW_RULE".equals(i.getType()) && "slow-rule".equals(i.getRuleName()));
            
            assertTrue(hasSlowRuleInsight);
            
            logger.info("[OK] Slow rules detected");
        }

        @Test
        @DisplayName("Should detect high memory usage")
        void shouldDetectHighMemoryUsage() {
            // Similar logic - need memory-hog to be 2x+ the average
            PerformanceSnapshot normal1 = createSnapshot("normal-rule-1", 10.0, 1000, 1, 100, 0);
            PerformanceSnapshot normal2 = createSnapshot("normal-rule-2", 10.0, 1000, 1, 100, 0);
            PerformanceSnapshot normal3 = createSnapshot("normal-rule-3", 10.0, 1000, 1, 100, 0);
            PerformanceSnapshot memoryHog = createSnapshot("memory-hog", 10.0, 10000, 1, 100, 0);
            
            snapshots.put("normal-rule-1", normal1);
            snapshots.put("normal-rule-2", normal2);
            snapshots.put("normal-rule-3", normal3);
            snapshots.put("memory-hog", memoryHog);
            
            List<PerformanceAnalyzer.PerformanceInsight> insights = 
                PerformanceAnalyzer.analyzePerformance(snapshots);
            
            boolean hasHighMemoryInsight = insights.stream()
                .anyMatch(i -> "HIGH_MEMORY".equals(i.getType()) && "memory-hog".equals(i.getRuleName()));
            
            assertTrue(hasHighMemoryInsight);
            
            logger.info("[OK] High memory usage detected");
        }

        @Test
        @DisplayName("Should detect high complexity")
        void shouldDetectHighComplexity() {
            PerformanceSnapshot simpleRule = createSnapshot("simple-rule", 10.0, 1000, 5, 100, 0);
            PerformanceSnapshot complexRule = createSnapshot("complex-rule", 10.0, 1000, 20, 100, 0);
            
            snapshots.put("simple-rule", simpleRule);
            snapshots.put("complex-rule", complexRule);
            
            List<PerformanceAnalyzer.PerformanceInsight> insights = 
                PerformanceAnalyzer.analyzePerformance(snapshots);
            
            boolean hasHighComplexityInsight = insights.stream()
                .anyMatch(i -> "HIGH_COMPLEXITY".equals(i.getType()) && "complex-rule".equals(i.getRuleName()));
            
            assertTrue(hasHighComplexityInsight);
            
            logger.info("[OK] High complexity detected");
        }

        @Test
        @DisplayName("Should detect low success rate")
        void shouldDetectLowSuccessRate() {
            PerformanceSnapshot reliableRule = createSnapshot("reliable-rule", 10.0, 1000, 5, 100, 0);
            PerformanceSnapshot unreliableRule = createSnapshot("unreliable-rule", 10.0, 1000, 5, 80, 20);
            
            snapshots.put("reliable-rule", reliableRule);
            snapshots.put("unreliable-rule", unreliableRule);
            
            List<PerformanceAnalyzer.PerformanceInsight> insights = 
                PerformanceAnalyzer.analyzePerformance(snapshots);
            
            boolean hasLowSuccessRateInsight = insights.stream()
                .anyMatch(i -> "LOW_SUCCESS_RATE".equals(i.getType()) && "unreliable-rule".equals(i.getRuleName()));
            
            assertTrue(hasLowSuccessRateInsight);
            
            logger.info("[OK] Low success rate detected");
        }

        @Test
        @DisplayName("Should identify slowest rule")
        void shouldIdentifySlowestRule() {
            PerformanceSnapshot rule1 = createSnapshot("rule-1", 10.0, 1000, 5, 100, 0);
            PerformanceSnapshot rule2 = createSnapshot("rule-2", 20.0, 1000, 5, 100, 0);
            PerformanceSnapshot rule3 = createSnapshot("rule-3", 30.0, 1000, 5, 100, 0);
            
            snapshots.put("rule-1", rule1);
            snapshots.put("rule-2", rule2);
            snapshots.put("rule-3", rule3);
            
            List<PerformanceAnalyzer.PerformanceInsight> insights = 
                PerformanceAnalyzer.analyzePerformance(snapshots);
            
            boolean hasSlowestRuleInsight = insights.stream()
                .anyMatch(i -> "SLOWEST_RULE".equals(i.getType()) && "rule-3".equals(i.getRuleName()));
            
            assertTrue(hasSlowestRuleInsight);
            
            logger.info("[OK] Slowest rule identified");
        }

        @Test
        @DisplayName("Should identify rule with most failures")
        void shouldIdentifyRuleWithMostFailures() {
            PerformanceSnapshot rule1 = createSnapshot("rule-1", 10.0, 1000, 5, 95, 5);
            PerformanceSnapshot rule2 = createSnapshot("rule-2", 10.0, 1000, 5, 80, 20);
            
            snapshots.put("rule-1", rule1);
            snapshots.put("rule-2", rule2);
            
            List<PerformanceAnalyzer.PerformanceInsight> insights = 
                PerformanceAnalyzer.analyzePerformance(snapshots);
            
            boolean hasMostFailuresInsight = insights.stream()
                .anyMatch(i -> "MOST_FAILURES".equals(i.getType()));
            
            assertTrue(hasMostFailuresInsight);
            
            logger.info("[OK] Rule with most failures identified");
        }

        @Test
        @DisplayName("Should include system summary insight")
        void shouldIncludeSystemSummaryInsight() {
            PerformanceSnapshot rule1 = createSnapshot("rule-1", 10.0, 1000, 5, 100, 0);
            snapshots.put("rule-1", rule1);
            
            List<PerformanceAnalyzer.PerformanceInsight> insights = 
                PerformanceAnalyzer.analyzePerformance(snapshots);
            
            boolean hasSystemSummary = insights.stream()
                .anyMatch(i -> "SYSTEM_SUMMARY".equals(i.getType()));
            
            assertTrue(hasSystemSummary);
            
            logger.info("[OK] System summary insight included");
        }
    }

    @Nested
    @DisplayName("Generate Recommendations Tests")
    class GenerateRecommendationsTests {

        @Test
        @DisplayName("Should generate recommendations for slow rules")
        void shouldGenerateRecommendationsForSlowRules() {
            List<PerformanceAnalyzer.PerformanceInsight> insights = Arrays.asList(
                new PerformanceAnalyzer.PerformanceInsight("SLOW_RULE", "test-rule", 
                    "Rule is slow", "HIGH")
            );
            
            List<String> recommendations = PerformanceAnalyzer.generateRecommendations(insights);
            
            assertFalse(recommendations.isEmpty());
            assertTrue(recommendations.stream().anyMatch(r -> r.toLowerCase().contains("slow") || 
                                                              r.toLowerCase().contains("optimiz")));
            
            logger.info("[OK] Recommendations generated for slow rules");
        }

        @Test
        @DisplayName("Should generate recommendations for high memory")
        void shouldGenerateRecommendationsForHighMemory() {
            List<PerformanceAnalyzer.PerformanceInsight> insights = Arrays.asList(
                new PerformanceAnalyzer.PerformanceInsight("HIGH_MEMORY", "test-rule", 
                    "High memory", "MEDIUM")
            );
            
            List<String> recommendations = PerformanceAnalyzer.generateRecommendations(insights);
            
            assertFalse(recommendations.isEmpty());
            assertTrue(recommendations.stream().anyMatch(r -> r.toLowerCase().contains("memory")));
            
            logger.info("[OK] Recommendations generated for high memory");
        }

        @Test
        @DisplayName("Should generate recommendations for low success rate")
        void shouldGenerateRecommendationsForLowSuccessRate() {
            List<PerformanceAnalyzer.PerformanceInsight> insights = Arrays.asList(
                new PerformanceAnalyzer.PerformanceInsight("LOW_SUCCESS_RATE", "test-rule", 
                    "Low success", "HIGH")
            );
            
            List<String> recommendations = PerformanceAnalyzer.generateRecommendations(insights);
            
            assertFalse(recommendations.isEmpty());
            assertTrue(recommendations.stream().anyMatch(r -> r.toLowerCase().contains("reliability") || 
                                                              r.toLowerCase().contains("error")));
            
            logger.info("[OK] Recommendations generated for low success rate");
        }

        @Test
        @DisplayName("Should generate recommendations for high complexity")
        void shouldGenerateRecommendationsForHighComplexity() {
            List<PerformanceAnalyzer.PerformanceInsight> insights = Arrays.asList(
                new PerformanceAnalyzer.PerformanceInsight("HIGH_COMPLEXITY", "test-rule", 
                    "High complexity", "MEDIUM")
            );
            
            List<String> recommendations = PerformanceAnalyzer.generateRecommendations(insights);
            
            assertFalse(recommendations.isEmpty());
            assertTrue(recommendations.stream().anyMatch(r -> r.toLowerCase().contains("complexity")));
            
            logger.info("[OK] Recommendations generated for high complexity");
        }

        @Test
        @DisplayName("Should generate default message for no issues")
        void shouldGenerateDefaultMessageForNoIssues() {
            List<PerformanceAnalyzer.PerformanceInsight> insights = Collections.emptyList();
            
            List<String> recommendations = PerformanceAnalyzer.generateRecommendations(insights);
            
            assertFalse(recommendations.isEmpty());
            assertTrue(recommendations.stream().anyMatch(r -> r.toLowerCase().contains("no specific")));
            
            logger.info("[OK] Default message generated for no issues");
        }
    }

    @Nested
    @DisplayName("Generate Performance Report Tests")
    class GeneratePerformanceReportTests {

        @Test
        @DisplayName("Should generate report for empty snapshots")
        void shouldGenerateReportForEmptySnapshots() {
            String report = PerformanceAnalyzer.generatePerformanceReport(snapshots);
            
            assertNotNull(report);
            assertTrue(report.contains("RULE ENGINE PERFORMANCE REPORT"));
            assertTrue(report.contains("No performance data available"));
            
            logger.info("[OK] Report generated for empty snapshots");
        }

        @Test
        @DisplayName("Should generate complete report with data")
        void shouldGenerateCompleteReportWithData() {
            PerformanceSnapshot rule1 = createSnapshot("fast-rule", 10.0, 1000, 5, 100, 0);
            PerformanceSnapshot rule2 = createSnapshot("medium-rule", 20.0, 1000, 5, 95, 5);
            PerformanceSnapshot rule3 = createSnapshot("slow-rule", 30.0, 1000, 5, 90, 10);
            
            snapshots.put("fast-rule", rule1);
            snapshots.put("medium-rule", rule2);
            snapshots.put("slow-rule", rule3);
            
            String report = PerformanceAnalyzer.generatePerformanceReport(snapshots);
            
            assertNotNull(report);
            assertTrue(report.contains("RULE ENGINE PERFORMANCE REPORT"));
            assertTrue(report.contains("SYSTEM SUMMARY"));
            assertTrue(report.contains("Total Rules Monitored: 3"));
            assertTrue(report.contains("SLOWEST RULES"));
            assertTrue(report.contains("FASTEST RULES"));
            
            logger.info("[OK] Complete report generated with data:\n{}", report);
        }

        @Test
        @DisplayName("Should include insights in report")
        void shouldIncludeInsightsInReport() {
            PerformanceSnapshot reliableRule = createSnapshot("reliable-rule", 10.0, 1000, 5, 100, 0);
            PerformanceSnapshot unreliableRule = createSnapshot("unreliable-rule", 10.0, 1000, 5, 70, 30);
            
            snapshots.put("reliable-rule", reliableRule);
            snapshots.put("unreliable-rule", unreliableRule);
            
            String report = PerformanceAnalyzer.generatePerformanceReport(snapshots);
            
            assertTrue(report.contains("PERFORMANCE INSIGHTS") || report.contains("RECOMMENDATIONS"));
            
            logger.info("[OK] Insights included in report");
        }

        @Test
        @DisplayName("Should include recommendations in report")
        void shouldIncludeRecommendationsInReport() {
            PerformanceSnapshot fastRule = createSnapshot("fast-rule", 10.0, 1000, 5, 100, 0);
            PerformanceSnapshot slowRule = createSnapshot("slow-rule", 100.0, 1000, 5, 100, 0);
            
            snapshots.put("fast-rule", fastRule);
            snapshots.put("slow-rule", slowRule);
            
            String report = PerformanceAnalyzer.generatePerformanceReport(snapshots);
            
            assertTrue(report.contains("RECOMMENDATIONS"));
            
            logger.info("[OK] Recommendations included in report");
        }
    }

    // Helper method to create PerformanceSnapshot using Builder pattern
    private PerformanceSnapshot createSnapshot(String ruleName, double avgTimeMs, 
            long avgMemory, int avgComplexity, long successful, long failed) {
        
        // Create initial metrics using Builder
        RulePerformanceMetrics.Builder builder = new RulePerformanceMetrics.Builder(ruleName)
            .startTime(Instant.now())
            .endTime(Instant.now().plusMillis((long) avgTimeMs))
            .evaluationTime(Duration.ofMillis((long) avgTimeMs))
            .memoryUsed(avgMemory)
            .expressionComplexity(avgComplexity);
        
        if (failed > 0 && successful == 0) {
            builder.evaluationException(new RuntimeException("Test failure"));
        }
        
        RulePerformanceMetrics metrics = builder.build();
        PerformanceSnapshot snapshot = new PerformanceSnapshot(ruleName, metrics);
        
        // Add more evaluations to simulate accumulated stats
        long totalEvaluations = successful + failed;
        for (int i = 1; i < totalEvaluations; i++) {
            RulePerformanceMetrics.Builder additionalBuilder = new RulePerformanceMetrics.Builder(ruleName)
                .startTime(Instant.now())
                .endTime(Instant.now().plusMillis((long) avgTimeMs))
                .evaluationTime(Duration.ofMillis((long) avgTimeMs))
                .memoryUsed(avgMemory)
                .expressionComplexity(avgComplexity);
            
            // Add failures for the last 'failed' number of iterations
            if (i >= successful) {
                additionalBuilder.evaluationException(new RuntimeException("Test failure"));
            }
            
            snapshot = snapshot.update(additionalBuilder.build());
        }
        
        return snapshot;
    }
}
