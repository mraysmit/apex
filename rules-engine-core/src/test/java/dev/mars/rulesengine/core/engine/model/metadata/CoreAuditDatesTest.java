package dev.mars.rulesengine.core.engine.model.metadata;

import dev.mars.rulesengine.core.engine.config.RulesEngineConfiguration;
import dev.mars.rulesengine.core.engine.model.Rule;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

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
 * Test class demonstrating the two critical audit attributes: createdDate and modifiedDate.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Test class demonstrating the two critical audit attributes: createdDate and modifiedDate.
 * These are the most important metadata attributes for enterprise rule management.
 */
public class CoreAuditDatesTest {

    @Test
    public void testCriticalDatesAreAlwaysAvailable() {
        RulesEngineConfiguration config = new RulesEngineConfiguration();
        
        // Create a rule - dates should be automatically set
        Rule rule = config.rule("TEST-001")
            .withName("Test Rule")
            .withCondition("#value > 0")
            .withMessage("Value is positive")
            .build();
        
        // CRITICAL: These dates must NEVER be null
        assertNotNull(rule.getCreatedDate(), "Created date must never be null");
        assertNotNull(rule.getModifiedDate(), "Modified date must never be null");
        
        // For new rules, modified date should equal created date
        assertEquals(rule.getCreatedDate(), rule.getModifiedDate(), 
                    "For new rules, modified date should equal created date");
    }

    @Test
    public void testExplicitDateSetting() {
        RulesEngineConfiguration config = new RulesEngineConfiguration();
        
        // Set explicit dates (e.g., for data migration scenarios)
        Instant specificDate = Instant.parse("2024-01-15T10:30:00Z");
        
        Rule rule = config.rule("MIGRATED-001")
            .withName("Migrated Rule")
            .withCondition("#amount > 1000")
            .withMessage("Amount validation")
            .withCreatedDate(specificDate)
            .withModifiedDate(specificDate)
            .build();
        
        // Verify the explicit dates were set
        assertEquals(specificDate, rule.getCreatedDate());
        assertEquals(specificDate, rule.getModifiedDate());
    }

    @Test
    public void testModificationUpdatesModifiedDate() throws InterruptedException {
        RulesEngineConfiguration config = new RulesEngineConfiguration();
        
        // Create original rule
        Rule originalRule = config.rule("MODIFY-001")
            .withName("Original Rule")
            .withCondition("#status == 'ACTIVE'")
            .withMessage("Status check")
            .build();
        
        Instant originalCreated = originalRule.getCreatedDate();
        Instant originalModified = originalRule.getModifiedDate();
        
        // Wait a moment to ensure time difference
        Thread.sleep(10);
        
        // Modify the rule - this should update modifiedDate
        Rule modifiedRule = originalRule.withStatus(RuleStatus.INACTIVE, "admin@company.com");
        
        // CRITICAL: Creation date must be preserved
        assertEquals(originalCreated, modifiedRule.getCreatedDate(), 
                    "Creation date must be preserved during modifications");
        
        // CRITICAL: Modified date must be updated
        assertTrue(modifiedRule.getModifiedDate().isAfter(originalModified), 
                  "Modified date must be updated when rule is changed");
    }

    @Test
    public void testDirectMetadataAccess() {
        RulesEngineConfiguration config = new RulesEngineConfiguration();
        
        Rule rule = config.rule("METADATA-001")
            .withName("Metadata Test")
            .withCondition("#test == true")
            .withMessage("Test message")
            .build();
        
        // Test direct access through metadata
        RuleMetadata metadata = rule.getMetadata();
        assertNotNull(metadata.getCreatedDate());
        assertNotNull(metadata.getModifiedDate());
        
        // Test convenience methods on rule
        assertEquals(metadata.getCreatedDate(), rule.getCreatedDate());
        assertEquals(metadata.getModifiedDate(), rule.getModifiedDate());
    }

    @Test
    public void testAuditTrailScenario() throws InterruptedException {
        RulesEngineConfiguration config = new RulesEngineConfiguration();
        
        // Create rule
        Rule rule = config.rule("AUDIT-001")
            .withName("Audit Trail Test")
            .withCondition("#value > 100")
            .withMessage("Value exceeds threshold")
            .withCreatedByUser("developer@company.com")
            .build();
        
        Instant created = rule.getCreatedDate();
        
        // First modification
        Thread.sleep(10);
        Rule modified1 = rule.withStatus(RuleStatus.TESTING, "tester@company.com");
        
        // Second modification
        Thread.sleep(10);
        Rule modified2 = modified1.withStatus(RuleStatus.ACTIVE, "approver@company.com");
        
        // Verify audit trail
        assertEquals(created, rule.getCreatedDate());
        assertEquals(created, modified1.getCreatedDate());
        assertEquals(created, modified2.getCreatedDate());
        
        // Each modification should have a later modified date
        assertTrue(modified1.getModifiedDate().isAfter(rule.getModifiedDate()));
        assertTrue(modified2.getModifiedDate().isAfter(modified1.getModifiedDate()));
    }

    @Test
    public void testRuleAgeCalculation() {
        RulesEngineConfiguration config = new RulesEngineConfiguration();
        
        // Create rule with specific creation date
        Instant oneHourAgo = Instant.now().minus(Duration.ofHours(1));
        
        Rule rule = config.rule("AGE-001")
            .withName("Age Test Rule")
            .withCondition("#age >= 18")
            .withMessage("Age validation")
            .withCreatedDate(oneHourAgo)
            .withModifiedDate(oneHourAgo)
            .build();
        
        // Calculate rule age
        Duration ruleAge = Duration.between(rule.getCreatedDate(), Instant.now());
        
        // Should be approximately 1 hour old
        assertTrue(ruleAge.toMinutes() >= 59 && ruleAge.toMinutes() <= 61, 
                  "Rule should be approximately 1 hour old");
    }

    @Test
    public void testToStringShowsCriticalDates() {
        RulesEngineConfiguration config = new RulesEngineConfiguration();
        
        Rule rule = config.rule("TOSTRING-001")
            .withName("ToString Test")
            .withCondition("#test == true")
            .withMessage("Test message")
            .build();
        
        String ruleString = rule.toString();
        
        // Verify that the toString includes the critical dates
        assertTrue(ruleString.contains("createdDate="), 
                  "toString should include createdDate");
        assertTrue(ruleString.contains("modifiedDate="), 
                  "toString should include modifiedDate");
    }

    @Test
    public void testMetadataToStringPrioritizesDates() {
        RuleMetadata metadata = RuleMetadata.builder()
            .createdByUser("test@company.com")
            .businessOwner("Business Team")
            .version("1.0")
            .build();
        
        String metadataString = metadata.toString();
        
        // Verify that dates appear first in toString
        int createdIndex = metadataString.indexOf("createdDate=");
        int modifiedIndex = metadataString.indexOf("modifiedDate=");
        int versionIndex = metadataString.indexOf("version=");
        
        assertTrue(createdIndex >= 0, "toString should include createdDate");
        assertTrue(modifiedIndex >= 0, "toString should include modifiedDate");
        assertTrue(createdIndex < versionIndex, "createdDate should appear before version");
        assertTrue(modifiedIndex < versionIndex, "modifiedDate should appear before version");
    }
}
