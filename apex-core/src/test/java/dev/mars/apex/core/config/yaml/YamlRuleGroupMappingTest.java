package dev.mars.apex.core.config.yaml;
import dev.mars.apex.core.config.model.*;
import dev.mars.apex.core.config.loader.*;
import dev.mars.apex.core.config.exception.*;
import dev.mars.apex.core.config.service.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.DisplayName;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class YamlRuleGroupMappingTest {

    @Test
    @DisplayName("YamlRuleGroup supports 'rule-groups' alias")
    void testRuleGroupsAlias() throws Exception {
        String yaml = """
            id: rg-alias-test
            rule-groups: [RG1, RG2]
            """;

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        YamlRuleGroup group = mapper.readValue(yaml, YamlRuleGroup.class);
        
        assertNotNull(group.getRuleGroupReferences());
        assertEquals(2, group.getRuleGroupReferences().size());
        assertTrue(group.getRuleGroupReferences().containsAll(List.of("RG1", "RG2")));
    }

    @Test
    @DisplayName("YamlRuleGroup prefers primary 'rule-group-references' over alias")
    void testPrimaryFieldPrecedence() throws Exception {
        String yaml = """
            id: rg-precedence-test
            rule-group-references: [PRIMARY]
            rule-groups: [ALIAS]
            """;

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        YamlRuleGroup group = mapper.readValue(yaml, YamlRuleGroup.class);
        
        assertNotNull(group.getRuleGroupReferences());
        assertEquals(1, group.getRuleGroupReferences().size());
        assertEquals("PRIMARY", group.getRuleGroupReferences().get(0));
    }
}
