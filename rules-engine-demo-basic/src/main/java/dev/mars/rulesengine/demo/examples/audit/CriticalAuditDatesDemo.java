package dev.mars.rulesengine.demo.examples.audit;

import dev.mars.rulesengine.core.engine.config.RulesEngine;
import dev.mars.rulesengine.core.engine.config.RulesEngineConfiguration;
import dev.mars.rulesengine.core.engine.model.Rule;
import dev.mars.rulesengine.core.engine.model.metadata.RuleMetadata;
import dev.mars.rulesengine.core.engine.model.metadata.RuleStatus;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Critical Audit Dates Demo
 * 
 * Demonstrates the two most important rule metadata attributes:
 * 1. createdDate - When the rule was first created (NEVER null)
 * 2. modifiedDate - When the rule was last modified (NEVER null)
 * 
 * These dates enable:
 * - Compliance and regulatory reporting
 * - Governance and change management  
 * - Operations and maintenance tracking
 * - Security auditing
 * - Performance monitoring and filtering
 */
public class CriticalAuditDatesDemo {
    
    private final RulesEngineConfiguration configuration;
    private final RulesEngine rulesEngine;
    private final List<Rule> demoRules;
    
    public CriticalAuditDatesDemo() {
        this.configuration = new RulesEngineConfiguration();
        this.rulesEngine = new RulesEngine(configuration);
        this.demoRules = new ArrayList<>();
    }
    
    public static void main(String[] args) {
        System.out.println("=== CRITICAL AUDIT DATES DEMO ===");
        System.out.println("Showcasing createdDate and modifiedDate - the foundation of rule governance\n");
        
        CriticalAuditDatesDemo demo = new CriticalAuditDatesDemo();
        
        // Step 1: Create rules with different temporal patterns
        demo.createRulesWithTemporalPatterns();
        
        // Step 2: Demonstrate compliance reporting capabilities
        demo.demonstrateComplianceReporting();
        
        // Step 3: Show governance and lifecycle management
        demo.demonstrateGovernanceCapabilities();
        
        // Step 4: Operations - maintenance and monitoring
        demo.demonstrateOperationalMonitoring();
        
        // Step 5: Security auditing scenarios
        demo.demonstrateSecurityAuditing();
        
        // Step 6: Performance queries and temporal filtering
        demo.demonstratePerformanceQueries();
        
        System.out.println("\n=== CRITICAL DATES ENABLE ENTERPRISE GOVERNANCE ===");
        System.out.println("✓ Compliance: Automated regulatory reporting");
        System.out.println("✓ Governance: Rule lifecycle tracking and change management");
        System.out.println("✓ Operations: Stale rule identification and maintenance planning");
        System.out.println("✓ Security: Complete audit trails for who changed what when");
        System.out.println("✓ Performance: Efficient temporal queries and filtering");
        
        System.out.println("\n=== DEMO COMPLETED ===");
    }
    
    /**
     * Create rules with different creation and modification patterns to simulate real scenarios
     */
    private void createRulesWithTemporalPatterns() {
        System.out.println("=== STEP 1: CREATING RULES WITH TEMPORAL PATTERNS ===");
        
        Instant now = Instant.now();
        
        // Legacy rule - created 6 months ago, never modified
        Rule legacyRule = createRuleWithDates("LEGACY-001", 
            "Legacy Customer Validation", 
            "#age >= 18 && #income > 30000",
            now.minus(Duration.ofDays(180)),  // 6 months ago
            now.minus(Duration.ofDays(180)),  // Never modified
            "legacy.system@company.com");
        
        // Recent rule - created last week, modified yesterday  
        Rule recentRule = createRuleWithDates("RECENT-001",
            "New KYC Validation",
            "#kycStatus == 'VERIFIED'",
            now.minus(Duration.ofDays(7)),    // 1 week ago
            now.minus(Duration.ofDays(1)),    // Modified yesterday
            "compliance.team@company.com");
        
        // Active rule - created 3 months ago, modified today
        Rule activeRule = createRuleWithDates("ACTIVE-001",
            "Risk Assessment Rule",
            "#riskScore <= 75",
            now.minus(Duration.ofDays(90)),   // 3 months ago
            now.minus(Duration.ofMinutes(30)), // Modified 30 minutes ago
            "risk.team@company.com");
        
        // Stale rule - created 1 year ago, last modified 6 months ago
        Rule staleRule = createRuleWithDates("STALE-001",
            "Old Credit Check",
            "#creditScore >= 600",
            now.minus(Duration.ofDays(365)),  // 1 year ago
            now.minus(Duration.ofDays(180)),  // Last modified 6 months ago
            "system.admin@company.com");
        
        demoRules.addAll(Arrays.asList(legacyRule, recentRule, activeRule, staleRule));
        
        System.out.println("   ✓ Created " + demoRules.size() + " rules with different temporal patterns");
        System.out.println("   ✓ Each rule has GUARANTEED createdDate and modifiedDate (never null)");
        
        // Show the critical dates for each rule
        for (Rule rule : demoRules) {
            System.out.println("   • " + rule.getId() + ":");
            System.out.println("     Created: " + formatInstant(rule.getCreatedDate()));
            System.out.println("     Modified: " + formatInstant(rule.getModifiedDate()));
            System.out.println("     Age: " + Duration.between(rule.getCreatedDate(), now).toDays() + " days");
            System.out.println("     Staleness: " + Duration.between(rule.getModifiedDate(), now).toDays() + " days");
        }
        System.out.println();
    }
    
    /**
     * COMPLIANCE: Demonstrate regulatory reporting enabled by critical dates
     */
    private void demonstrateComplianceReporting() {
        System.out.println("=== STEP 2: COMPLIANCE & REGULATORY REPORTING ===");
        
        Instant thirtyDaysAgo = Instant.now().minus(Duration.ofDays(30));
        
        // Generate compliance report for last 30 days
        List<Rule> recentChanges = demoRules.stream()
            .filter(rule -> rule.getModifiedDate().isAfter(thirtyDaysAgo))
            .sorted(Comparator.comparing(Rule::getModifiedDate).reversed())
            .collect(Collectors.toList());
        
        System.out.println("1. REGULATORY CHANGE REPORT (Last 30 Days):");
        System.out.println("   Total rules in system: " + demoRules.size());
        System.out.println("   Rules modified in period: " + recentChanges.size());
        System.out.println("   Compliance status: " + (recentChanges.size() > 0 ? "CHANGES DETECTED" : "NO CHANGES"));
        
        if (!recentChanges.isEmpty()) {
            System.out.println("\n   DETAILED CHANGE LOG:");
            for (Rule rule : recentChanges) {
                System.out.println("   • " + rule.getId() + " - " + rule.getName());
                System.out.println("     Modified: " + formatInstant(rule.getModifiedDate()));
                System.out.println("     By: " + rule.getMetadata().getLastModifiedByUser());
                System.out.println("     Days since creation: " + 
                    Duration.between(rule.getCreatedDate(), Instant.now()).toDays());
            }
        }
        
        System.out.println("\n2. CRITICAL DATES ENABLE COMPLIANCE:");
        System.out.println("   ✓ Precise change tracking with modifiedDate");
        System.out.println("   ✓ Rule age determination with createdDate");
        System.out.println("   ✓ Automated regulatory timeline compliance");
        System.out.println("   ✓ Complete audit trail for regulatory submissions");
        System.out.println();
    }
    
    /**
     * GOVERNANCE: Track rule lifecycle and change management
     */
    private void demonstrateGovernanceCapabilities() {
        System.out.println("=== STEP 3: GOVERNANCE & CHANGE MANAGEMENT ===");
        
        Instant now = Instant.now();
        
        System.out.println("1. RULE LIFECYCLE ANALYSIS:");
        
        // Categorize rules by age using createdDate
        Map<String, List<Rule>> rulesByAge = demoRules.stream()
            .collect(Collectors.groupingBy(rule -> {
                Duration age = Duration.between(rule.getCreatedDate(), now);
                if (age.toDays() < 30) return "New (< 30 days)";
                else if (age.toDays() < 90) return "Recent (30-90 days)";
                else if (age.toDays() < 365) return "Mature (90-365 days)";
                else return "Legacy (> 1 year)";
            }));
        
        rulesByAge.forEach((category, rules) -> {
            System.out.println("   " + category + ": " + rules.size() + " rules");
        });
        
        System.out.println("\n2. CHANGE MANAGEMENT TRACKING:");
        
        // Track modification patterns using modifiedDate
        demoRules.forEach(rule -> {
            Duration timeSinceModification = Duration.between(rule.getModifiedDate(), now);
            String changePattern = timeSinceModification.toDays() == 0 ? "ACTIVE" :
                                 timeSinceModification.toDays() < 30 ? "RECENT" :
                                 timeSinceModification.toDays() < 90 ? "STABLE" : "STALE";
            
            System.out.println("   " + rule.getId() + " - Pattern: " + changePattern + 
                             " (last modified " + timeSinceModification.toDays() + " days ago)");
        });
        
        System.out.println("\n3. GOVERNANCE INSIGHTS:");
        System.out.println("   ✓ createdDate enables rule lifecycle stage determination");
        System.out.println("   ✓ modifiedDate enables staleness detection for maintenance");
        System.out.println("   ✓ Combined dates support governance health scoring");
        System.out.println();
    }
    
    /**
     * OPERATIONS: Identify maintenance needs and monitoring
     */
    private void demonstrateOperationalMonitoring() {
        System.out.println("=== STEP 4: OPERATIONAL MONITORING ===");
        
        Instant now = Instant.now();
        
        // Identify stale rules (not modified in 90+ days)
        List<Rule> staleRules = demoRules.stream()
            .filter(rule -> rule.getModifiedDate().isBefore(now.minus(Duration.ofDays(90))))
            .collect(Collectors.toList());
        
        System.out.println("1. MAINTENANCE NEEDS ASSESSMENT:");
        System.out.println("   Stale rules (not modified in 90+ days): " + staleRules.size());
        
        for (Rule rule : staleRules) {
            Duration staleness = Duration.between(rule.getModifiedDate(), now);
            System.out.println("   • " + rule.getId() + " - " + rule.getName());
            System.out.println("     Last modified: " + staleness.toDays() + " days ago");
            System.out.println("     Recommendation: REVIEW FOR RELEVANCE");
        }
        
        // Recent activity (modified in last 7 days)
        List<Rule> recentActivity = demoRules.stream()
            .filter(rule -> rule.getModifiedDate().isAfter(now.minus(Duration.ofDays(7))))
            .collect(Collectors.toList());
        
        System.out.println("\n2. RECENT ACTIVITY (Last 7 Days): " + recentActivity.size() + " rules");
        recentActivity.forEach(rule -> {
            System.out.println("   • " + rule.getId() + " - Modified: " + formatInstant(rule.getModifiedDate()));
        });
        
        // Operational health score based on modification activity
        double healthScore = ((double) recentActivity.size() / demoRules.size()) * 100;
        System.out.println("\n3. OPERATIONAL HEALTH SCORE: " + String.format("%.1f%%", healthScore));
        System.out.println("   (Based on recent modification activity - higher is more active)");
        
        System.out.println("\n4. OPERATIONAL BENEFITS:");
        System.out.println("   ✓ modifiedDate identifies stale rules needing review");
        System.out.println("   ✓ createdDate helps prioritize maintenance by rule age");
        System.out.println("   ✓ Combined dates enable operational health monitoring");
        System.out.println();
    }
    
    /**
     * SECURITY: Audit who changed what and when
     */
    private void demonstrateSecurityAuditing() {
        System.out.println("=== STEP 5: SECURITY AUDITING ===");
        
        System.out.println("1. USER ACTIVITY ANALYSIS:");
        
        // Group modifications by user
        Map<String, List<Rule>> modificationsByUser = demoRules.stream()
            .filter(rule -> rule.getMetadata().getLastModifiedByUser() != null)
            .collect(Collectors.groupingBy(rule -> rule.getMetadata().getLastModifiedByUser()));
        
        modificationsByUser.forEach((user, rules) -> {
            System.out.println("   User: " + user);
            System.out.println("     Rules modified: " + rules.size());
            rules.forEach(rule -> {
                System.out.println("       • " + rule.getId() + " on " + formatInstant(rule.getModifiedDate()));
            });
        });
        
        System.out.println("\n2. SECURITY TIMELINE ANALYSIS:");
        
        // Check for recent modifications (potential security events)
        Instant oneDayAgo = Instant.now().minus(Duration.ofDays(1));
        List<Rule> recentModifications = demoRules.stream()
            .filter(rule -> rule.getModifiedDate().isAfter(oneDayAgo))
            .collect(Collectors.toList());
        
        if (recentModifications.isEmpty()) {
            System.out.println("   ✓ No rule modifications in last 24 hours");
        } else {
            System.out.println("   ⚠️  " + recentModifications.size() + " rule(s) modified in last 24 hours:");
            recentModifications.forEach(rule -> {
                System.out.println("     • " + rule.getId() + " by " + 
                    rule.getMetadata().getLastModifiedByUser() + 
                    " at " + formatInstant(rule.getModifiedDate()));
            });
        }
        
        System.out.println("\n3. SECURITY AUDIT CAPABILITIES:");
        System.out.println("   ✓ modifiedDate provides precise timing of all changes");
        System.out.println("   ✓ createdDate establishes rule origin timeline");
        System.out.println("   ✓ Combined with user info enables complete audit trails");
        System.out.println("   ✓ Temporal analysis detects suspicious modification patterns");
        System.out.println();
    }
    
    /**
     * PERFORMANCE: Query and filter rules by temporal characteristics
     */
    private void demonstratePerformanceQueries() {
        System.out.println("=== STEP 6: PERFORMANCE QUERIES & FILTERING ===");
        
        System.out.println("1. TEMPORAL QUERY PERFORMANCE:");
        
        long startTime = System.nanoTime();
        
        // Query 1: Rules created this month using createdDate
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        Instant monthStart = startOfMonth.atStartOfDay(ZoneOffset.UTC).toInstant();
        
        List<Rule> thisMonthRules = demoRules.stream()
            .filter(rule -> rule.getCreatedDate().isAfter(monthStart))
            .collect(Collectors.toList());
        
        long query1Time = System.nanoTime() - startTime;
        System.out.println("   Query 1 - Rules created this month: " + thisMonthRules.size() + 
                          " (" + String.format("%.2f", query1Time / 1_000_000.0) + "ms)");
        
        // Query 2: Rules modified in last hour using modifiedDate
        startTime = System.nanoTime();
        Instant oneHourAgo = Instant.now().minus(Duration.ofHours(1));
        
        List<Rule> lastHourModifications = demoRules.stream()
            .filter(rule -> rule.getModifiedDate().isAfter(oneHourAgo))
            .collect(Collectors.toList());
        
        long query2Time = System.nanoTime() - startTime;
        System.out.println("   Query 2 - Rules modified in last hour: " + lastHourModifications.size() + 
                          " (" + String.format("%.2f", query2Time / 1_000_000.0) + "ms)");
        
        // Query 3: Complex temporal filtering (old rules with recent changes)
        startTime = System.nanoTime();
        
        List<Rule> complexQuery = demoRules.stream()
            .filter(rule -> {
                Duration age = Duration.between(rule.getCreatedDate(), Instant.now());
                Duration staleness = Duration.between(rule.getModifiedDate(), Instant.now());
                return age.toDays() > 30 && staleness.toDays() < 7;  // Old but recently modified
            })
            .collect(Collectors.toList());
        
        long query3Time = System.nanoTime() - startTime;
        System.out.println("   Query 3 - Old rules with recent changes: " + complexQuery.size() + 
                          " (" + String.format("%.2f", query3Time / 1_000_000.0) + "ms)");
        
        System.out.println("\n2. PERFORMANCE INSIGHTS:");
        System.out.println("   ✓ Date-based queries are highly efficient");
        System.out.println("   ✓ createdDate and modifiedDate enable real-time monitoring");
        System.out.println("   ✓ Complex temporal logic supports sophisticated governance");
        System.out.println("   ✓ Temporal filtering scales well with large rule sets");
        
        System.out.println();
    }
    
    // Helper methods
    
    private Rule createRuleWithDates(String id, String name, String condition, 
                                   Instant createdDate, Instant modifiedDate, String createdBy) {
        RuleMetadata metadata = RuleMetadata.builder()
            .createdDate(createdDate)
            .modifiedDate(modifiedDate)
            .createdByUser(createdBy)
            .lastModifiedByUser(createdBy)
            .businessDomain("Enterprise Governance")
            .sourceSystem("AUDIT_DEMO")
            .build();
        
        Rule rule = configuration.rule(id)
            .withName(name)
            .withCondition(condition)
            .withMessage("Rule validation passed")
            .withCreatedDate(createdDate)
            .withModifiedDate(modifiedDate)
            .withCreatedByUser(createdBy)
            .build();
        
        configuration.registerRule(rule);
        return rule;
    }
    
    private String formatInstant(Instant instant) {
        return DateTimeFormatter.ISO_LOCAL_DATE_TIME
            .format(instant.atZone(ZoneOffset.UTC))
            .replace('T', ' ');
    }
}
