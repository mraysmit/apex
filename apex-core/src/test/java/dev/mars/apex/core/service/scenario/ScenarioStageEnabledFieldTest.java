package dev.mars.apex.core.service.scenario;

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

import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ScenarioStage enabled field functionality.
 *
 * Tests cover:
 * - Default enabled state (true)
 * - Explicit enabled/disabled states  
 * - Enabled flag in equals/hashCode/toString
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 3.0
 */
@DisplayName("Scenario Stage Enabled Field Tests")
class ScenarioStageEnabledFieldTest {

    private static final Logger logger = LoggerFactory.getLogger(ScenarioStageEnabledFieldTest.class);

    @Test
    @DisplayName("Should default to enabled=true")
    void testDefaultEnabledState() {
        logger.info("=== Testing Stage Default Enabled State ===");
        
        ScenarioStage stage = new ScenarioStage();
        assertTrue(stage.isEnabled(), "New stage should be enabled by default");
    }

    @Test
    @DisplayName("Should allow setting enabled to false")
    void testSetEnabledFalse() {
        logger.info("=== Testing Stage Enabled=false ===");
        
        ScenarioStage stage = new ScenarioStage();
        stage.setEnabled(false);
        assertFalse(stage.isEnabled(), "Stage should be disabled after setEnabled(false)");
    }

    @Test
    @DisplayName("Should allow setting enabled to true explicitly")
    void testSetEnabledTrue() {
        logger.info("=== Testing Stage Enabled=true ===");
        
        ScenarioStage stage = new ScenarioStage();
        stage.setEnabled(false);
        stage.setEnabled(true);
        assertTrue(stage.isEnabled(), "Stage should be enabled after setEnabled(true)");
    }

    @Test
    @DisplayName("Should include enabled flag in equals()")
    void testEnabledInEquals() {
        logger.info("=== Testing Enabled Flag in Equals ===");
        
        ScenarioStage stage1 = new ScenarioStage("test", "config.yaml", 1);
        ScenarioStage stage2 = new ScenarioStage("test", "config.yaml", 1);
        
        assertEquals(stage1, stage2, "Stages with same enabled state should be equal");
        
        stage1.setEnabled(false);
        assertNotEquals(stage1, stage2, "Stages with different enabled state should not be equal");
        
        stage2.setEnabled(false);
        assertEquals(stage1, stage2, "Stages with same disabled state should be equal");
    }

    @Test
    @DisplayName("Should include enabled flag in hashCode()")
    void testEnabledInHashCode() {
        logger.info("=== Testing Enabled Flag in HashCode ===");
        
        ScenarioStage stage1 = new ScenarioStage("test", "config.yaml", 1);
        ScenarioStage stage2 = new ScenarioStage("test", "config.yaml", 1);
        
        assertEquals(stage1.hashCode(), stage2.hashCode(), 
            "Stages with same enabled state should have same hash code");
        
        stage1.setEnabled(false);
        // Hash codes may differ (not required to differ, but typically do)
        
        stage2.setEnabled(false);
        assertEquals(stage1.hashCode(), stage2.hashCode(), 
            "Stages with same disabled state should have same hash code");
    }

    @Test
    @DisplayName("Should include enabled flag in toString()")
    void testEnabledInToString() {
        logger.info("=== Testing Enabled Flag in ToString ===");
        
        ScenarioStage stage = new ScenarioStage("test", "config.yaml", 1);
        
        String str1 = stage.toString();
        assertTrue(str1.contains("enabled=true"), "toString() should show enabled=true by default");
        
        stage.setEnabled(false);
        String str2 = stage.toString();
        assertTrue(str2.contains("enabled=false"), "toString() should show enabled=false when disabled");
    }

    @Test
    @DisplayName("Should work with constructor-created stages")
    void testEnabledWithConstructor() {
        logger.info("=== Testing Enabled with Constructor ===");
        
        ScenarioStage stage = new ScenarioStage("validation", "rules.yaml", 1);
        assertTrue(stage.isEnabled(), "Constructor-created stage should be enabled by default");
        
        stage.setEnabled(false);
        assertFalse(stage.isEnabled(), "Should be able to disable constructor-created stage");
    }

    @Test
    @DisplayName("Should work with all constructor variants")
    void testEnabledWithAllConstructors() {
        logger.info("=== Testing Enabled with All Constructors ===");
        
        ScenarioStage stage1 = new ScenarioStage();
        assertTrue(stage1.isEnabled(), "Default constructor should create enabled stage");
        
        ScenarioStage stage2 = new ScenarioStage("test", "config.yaml", 1);
        assertTrue(stage2.isEnabled(), "3-arg constructor should create enabled stage");
        
        ScenarioStage stage3 = new ScenarioStage("test", "config.yaml", 1, "terminate");
        assertTrue(stage3.isEnabled(), "4-arg constructor should create enabled stage");
    }
}
