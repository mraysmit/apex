package dev.mars.rulesengine.demo.examples;

import dev.mars.rulesengine.demo.model.SettlementInstruction;
import dev.mars.rulesengine.demo.model.StandingInstruction;
import dev.mars.rulesengine.demo.model.SIRepairResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

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
 * Test class for CustodyAutoRepairDemo to verify the SI auto-repair functionality.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-29
 * @version 1.0
 */
public class CustodyAutoRepairDemoTest {
    
    private CustodyAutoRepairDemo demo;
    
    @BeforeEach
    void setUp() {
        demo = new CustodyAutoRepairDemo();
    }
    
    @Test
    void testSettlementInstructionCreation() {
        // Test that we can create settlement instructions
        SettlementInstruction instruction = new SettlementInstruction(
            "SI001", "CLIENT_A", "JAPAN", "EQUITY", 
            new BigDecimal("1000000"), "JPY", LocalDate.now().plusDays(2)
        );
        
        assertNotNull(instruction);
        assertEquals("SI001", instruction.getInstructionId());
        assertEquals("CLIENT_A", instruction.getClientId());
        assertEquals("JAPAN", instruction.getMarket());
        assertEquals("EQUITY", instruction.getInstrumentType());
        assertEquals(new BigDecimal("1000000"), instruction.getSettlementAmount());
        assertEquals("JPY", instruction.getSettlementCurrency());
        assertFalse(instruction.isRequiresRepair()); // Initially no repair needed
    }
    
    @Test
    void testStandingInstructionCreation() {
        // Test client-specific SI
        StandingInstruction clientSI = new StandingInstruction("SI_CLIENT_A", "CLIENT_A", "Client A Default SI");
        
        assertNotNull(clientSI);
        assertEquals("SI_CLIENT_A", clientSI.getSiId());
        assertEquals("CLIENT_A", clientSI.getClientId());
        assertEquals("CLIENT", clientSI.getScopeType());
        assertEquals(0.6, clientSI.getWeight(), 0.01); // Client-level weight
        assertTrue(clientSI.isEnabled());
        
        // Test market-specific SI
        StandingInstruction marketSI = new StandingInstruction("SI_JAPAN", "JAPAN", "Japan Market SI", true);
        
        assertNotNull(marketSI);
        assertEquals("SI_JAPAN", marketSI.getSiId());
        assertEquals("JAPAN", marketSI.getMarket());
        assertEquals("MARKET", marketSI.getScopeType());
        assertEquals(0.3, marketSI.getWeight(), 0.01); // Market-level weight
    }
    
    @Test
    void testSIRepairResultCreation() {
        // Test repair result creation and manipulation
        SIRepairResult result = new SIRepairResult("SI001");
        
        assertNotNull(result);
        assertEquals("SI001", result.getInstructionId());
        assertFalse(result.isRepairSuccessful()); // Initially not successful
        assertEquals("PENDING", result.getRepairStatus());
        assertEquals(0, result.getFieldsRepaired());
        
        // Test adding field repair
        StandingInstruction si = new StandingInstruction("SI_TEST", "CLIENT_A", "Test SI");
        si.setDefaultCounterpartyId("CP_TEST");
        
        result.addFieldRepair("counterpartyId", "CP_TEST", si);
        
        assertEquals(1, result.getFieldsRepaired());
        assertTrue(result.hasFieldRepair("counterpartyId"));
        assertEquals("CP_TEST", result.getFieldRepairValue("counterpartyId"));
        assertEquals(si, result.getFieldRepairSource("counterpartyId"));
        
        // Test marking as successful
        result.markAsSuccessful("Test repair completed");
        
        assertTrue(result.isRepairSuccessful());
        assertEquals("SUCCESS", result.getRepairStatus());
        assertEquals("Test repair completed", result.getDecisionRationale());
    }
    
    @Test
    void testSettlementInstructionRepairEligibility() {
        // Test high-value transaction (not eligible for auto-repair)
        SettlementInstruction highValueInstruction = new SettlementInstruction(
            "SI_HIGH", "CLIENT_B", "SINGAPORE", "EQUITY",
            new BigDecimal("50000000"), "SGD", LocalDate.now().plusDays(3)
        );
        highValueInstruction.setHighValueTransaction(true);
        
        assertFalse(highValueInstruction.isEligibleForAutoRepair());
        
        // Test client opt-out (not eligible for auto-repair)
        SettlementInstruction optOutInstruction = new SettlementInstruction(
            "SI_OPT_OUT", "CLIENT_OPT_OUT", "KOREA", "EQUITY",
            new BigDecimal("3000000"), "KRW", LocalDate.now().plusDays(2)
        );
        optOutInstruction.setClientOptOut(true);
        
        assertFalse(optOutInstruction.isEligibleForAutoRepair());
        
        // Test normal instruction with missing fields (eligible for auto-repair)
        SettlementInstruction normalInstruction = new SettlementInstruction(
            "SI_NORMAL", "CLIENT_C", "JAPAN", "EQUITY",
            new BigDecimal("1000000"), "JPY", LocalDate.now().plusDays(2)
        );
        normalInstruction.addMissingField("counterpartyId");
        
        assertTrue(normalInstruction.isEligibleForAutoRepair());
    }
    
    @Test
    void testStandingInstructionApplicability() {
        // Create test instruction
        SettlementInstruction instruction = new SettlementInstruction(
            "SI001", "CLIENT_A", "JAPAN", "EQUITY", 
            new BigDecimal("1000000"), "JPY", LocalDate.now().plusDays(2)
        );
        
        // Test client-specific SI applicability
        StandingInstruction clientSI = new StandingInstruction("SI_CLIENT_A", "CLIENT_A", "Client A SI");
        assertTrue(clientSI.isApplicableToInstruction(instruction));
        
        // Test market-specific SI applicability
        StandingInstruction marketSI = new StandingInstruction("SI_JAPAN", "JAPAN", "Japan Market SI", true);
        assertTrue(marketSI.isApplicableToInstruction(instruction));
        
        // Test non-applicable SI
        StandingInstruction wrongClientSI = new StandingInstruction("SI_CLIENT_B", "CLIENT_B", "Client B SI");
        assertFalse(wrongClientSI.isApplicableToInstruction(instruction));
        
        StandingInstruction wrongMarketSI = new StandingInstruction("SI_HONG_KONG", "HONG_KONG", "Hong Kong SI", true);
        assertFalse(wrongMarketSI.isApplicableToInstruction(instruction));
    }
    
    @Test
    void testDemoMainMethodDoesNotThrow() {
        // Test that the main method can be called without throwing exceptions
        // This is a basic smoke test to ensure the demo structure is sound
        assertDoesNotThrow(() -> {
            // We can't easily test the full demo without mocking the rules engine
            // but we can test that the demo class can be instantiated
            CustodyAutoRepairDemo testDemo = new CustodyAutoRepairDemo();
            assertNotNull(testDemo);
        });
    }
    
    @Test
    void testModelClassesIntegration() {
        // Test that all model classes work together properly
        SettlementInstruction instruction = new SettlementInstruction(
            "SI001", "CLIENT_A", "JAPAN", "EQUITY", 
            new BigDecimal("1000000"), "JPY", LocalDate.now().plusDays(2)
        );
        
        // Add missing fields to trigger repair
        instruction.setCounterpartyId(null);
        instruction.addMissingField("counterpartyId");
        
        // Create standing instruction to provide repair
        StandingInstruction si = new StandingInstruction("SI_CLIENT_A", "CLIENT_A", "Client A Default SI");
        si.setDefaultCounterpartyId("CP_CLIENT_A_DEFAULT");
        
        // Create repair result
        SIRepairResult result = new SIRepairResult(instruction.getInstructionId());
        
        // Verify the integration works
        assertTrue(instruction.isRequiresRepair());
        assertTrue(instruction.isEligibleForAutoRepair());
        assertTrue(si.isApplicableToInstruction(instruction));
        assertTrue(si.hasDefaultValue("counterpartyId"));
        assertEquals("CP_CLIENT_A_DEFAULT", si.getDefaultValue("counterpartyId"));
        
        // Apply the repair
        result.addFieldRepair("counterpartyId", si.getDefaultValue("counterpartyId"), si);
        result.markAsSuccessful("Repair applied successfully");
        
        // Verify the result
        assertTrue(result.isRepairSuccessful());
        assertEquals(1, result.getFieldsRepaired());
        assertTrue(result.hasFieldRepair("counterpartyId"));
    }
}
