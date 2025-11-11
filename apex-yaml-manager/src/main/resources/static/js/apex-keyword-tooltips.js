/**
 * APEX Keyword Tooltips
 * 
 * Provides hover tooltips for APEX YAML keywords showing their descriptions
 * from the APEX_YAML_REFERENCE.md documentation.
 */

// APEX Keyword Dictionary - extracted from APEX_YAML_REFERENCE.md
const APEX_KEYWORDS = {
    // Metadata Keywords
    'metadata': {
        category: 'Document',
        description: 'Document metadata section containing identification and configuration',
        required: true
    },
    'id': {
        category: 'Metadata',
        description: 'Unique identifier for the configuration',
        required: true
    },
    'name': {
        category: 'Metadata',
        description: 'Human-readable name for the configuration',
        required: false
    },
    'type': {
        category: 'Metadata',
        description: 'Document type (rule-config, enrichment, dataset, scenario, external-data-config)',
        required: true
    },
    'version': {
        category: 'Metadata',
        description: 'Version identifier for the configuration',
        required: false
    },
    'author': {
        category: 'Metadata',
        description: 'Author of the configuration (required for rule-config, enrichment)',
        required: 'conditional'
    },
    'description': {
        category: 'Metadata',
        description: 'Human-readable description of the configuration',
        required: false
    },
    'created': {
        category: 'Metadata',
        description: 'Creation timestamp (ISO 8601 format)',
        required: false
    },
    'last-modified': {
        category: 'Metadata',
        description: 'Last modification timestamp (ISO 8601 format)',
        required: false
    },
    'tags': {
        category: 'Metadata',
        description: 'Classification tags for the configuration',
        required: false
    },
    
    // Rule Keywords
    'rules': {
        category: 'Document',
        description: 'Rule definitions section',
        required: false
    },
    'condition': {
        category: 'Rule/Enrichment',
        description: 'SpEL expression defining when rule/enrichment applies',
        required: false
    },
    'message': {
        category: 'Rule',
        description: 'Message displayed when rule is triggered',
        required: false
    },
    'severity': {
        category: 'Rule',
        description: 'Severity level (ERROR, WARNING, INFO)',
        required: false
    },
    'priority': {
        category: 'Rule/Enrichment',
        description: 'Execution priority (lower numbers = higher priority)',
        required: false
    },
    'enabled': {
        category: 'Rule/Enrichment',
        description: 'Whether the rule/enrichment is active',
        required: false
    },
    'category': {
        category: 'Rule',
        description: 'Single category for rule classification',
        required: false
    },
    'categories': {
        category: 'Document',
        description: 'Category definitions for the configuration',
        required: false
    },
    'business-domain': {
        category: 'Rule',
        description: 'Business domain classification',
        required: false
    },
    'business-owner': {
        category: 'Rule',
        description: 'Business owner responsible for the rule',
        required: false
    },
    'created-by': {
        category: 'Rule',
        description: 'Creator identifier',
        required: false
    },
    'effective-date': {
        category: 'Rule',
        description: 'Date when rule becomes effective (ISO 8601)',
        required: false
    },
    'expiration-date': {
        category: 'Rule',
        description: 'Date when rule expires (ISO 8601)',
        required: false
    },
    'custom-properties': {
        category: 'Rule',
        description: 'Custom extensible properties for rules',
        required: false
    },
    'validation': {
        category: 'Rule',
        description: 'Validation configuration for rules',
        required: false
    },
    
    // Rule Group Keywords
    'rule-groups': {
        category: 'Document',
        description: 'Rule group definitions',
        required: false
    },
    'operator': {
        category: 'RuleGroup',
        description: 'Logical operator for rule group (AND/OR)',
        required: false
    },
    'rule-ids': {
        category: 'RuleGroup',
        description: 'List of rule IDs in the group',
        required: false
    },
    'rule-references': {
        category: 'RuleGroup',
        description: 'Detailed rule references with metadata',
        required: false
    },
    'rule-id': {
        category: 'RuleReference',
        description: 'ID of rule being referenced',
        required: true
    },
    'override-priority': {
        category: 'RuleReference',
        description: 'Override priority for rule within group',
        required: false
    },
    'sequence': {
        category: 'RuleReference',
        description: 'Execution sequence for rule within group',
        required: false
    },
    'stop-on-first-failure': {
        category: 'RuleGroup',
        description: 'Stop group execution on first rule failure',
        required: false
    },
    'parallel-execution': {
        category: 'RuleGroup',
        description: 'Enable parallel execution of rules in group',
        required: false
    },
    'debug-mode': {
        category: 'RuleGroup',
        description: 'Enable debug mode for rule group execution',
        required: false
    },
    'rule-group-references': {
        category: 'RuleGroup',
        description: 'References to other rule groups',
        required: false
    },
    
    // Enrichment Keywords
    'enrichments': {
        category: 'Document',
        description: 'Data enrichment configurations',
        required: false
    },
    'enrichment-groups': {
        category: 'Document',
        description: 'Enrichment group definitions',
        required: false
    },
    'lookup-config': {
        category: 'Enrichment',
        description: 'Configuration for lookup-enrichment type',
        required: false
    },
    'calculation-config': {
        category: 'Enrichment',
        description: 'Configuration for calculation-enrichment type',
        required: false
    },
    'field-mappings': {
        category: 'Enrichment',
        description: 'Field mapping configurations for enrichments',
        required: false
    },
    'source-field': {
        category: 'FieldMapping',
        description: 'Source field name in field mappings',
        required: true
    },
    'target-field': {
        category: 'FieldMapping',
        description: 'Target field name in field mappings',
        required: true
    },
    'expression': {
        category: 'FieldMapping',
        description: 'SpEL expression for field transformation',
        required: false
    },
    'default-value': {
        category: 'FieldMapping',
        description: 'Fallback value when source field is missing/null',
        required: false
    },
    'required': {
        category: 'FieldMapping',
        description: 'Whether field mapping is mandatory',
        required: false
    },
    'conditional-mappings': {
        category: 'Enrichment',
        description: 'Conditional field mapping configurations',
        required: false
    },
    'mapping-rules': {
        category: 'Enrichment',
        description: 'Complex mapping rule definitions',
        required: false
    },
    'target-type': {
        category: 'Enrichment',
        description: 'Target object type for enrichment',
        required: false
    },
    'execution-settings': {
        category: 'Enrichment',
        description: 'Execution behavior configuration for enrichments',
        required: false
    },
    
    // Data Source Keywords
    'data-source-refs': {
        category: 'Document',
        description: 'References to external data source configurations',
        required: false
    },
    'data-sources': {
        category: 'Document',
        description: 'Inline data source definitions',
        required: false
    },
    'source-type': {
        category: 'DataSource',
        description: 'Type of data source (database, rest-api, file, kafka, etc.)',
        required: true
    },
    'connection': {
        category: 'DataSource',
        description: 'Database/external system connection configuration',
        required: true
    },
    'queries': {
        category: 'DataSource',
        description: 'Named query definitions for database sources',
        required: false
    },
    'endpoints': {
        category: 'DataSource',
        description: 'REST API endpoint definitions',
        required: false
    },
    'operations': {
        category: 'DataSource',
        description: 'Operation definitions for REST APIs',
        required: false
    },
    'authentication': {
        category: 'DataSource',
        description: 'Authentication configuration for external data sources',
        required: false
    },
    'cache': {
        category: 'DataSource',
        description: 'Caching configuration for data sources',
        required: false
    },
    'connection-pool': {
        category: 'DataSource',
        description: 'Connection pool settings for database sources',
        required: false
    },
    'circuit-breaker': {
        category: 'DataSource',
        description: 'Circuit breaker configuration for resilience',
        required: false
    },
    'health-check': {
        category: 'DataSource',
        description: 'Health check configuration for data sources',
        required: false
    },
    
    // Scenario Keywords
    'scenario-id': {
        category: 'Scenario',
        description: 'Unique identifier for the scenario',
        required: true
    },
    'data-types': {
        category: 'Scenario',
        description: 'Data types this scenario applies to',
        required: false
    },
    'processing-stages': {
        category: 'Scenario',
        description: 'Stage-based processing configuration',
        required: false
    },
    'stage-name': {
        category: 'Stage',
        description: 'Unique identifier for processing stage',
        required: true
    },
    'execution-order': {
        category: 'Stage',
        description: 'Numeric execution order for stage',
        required: true
    },
    'config-file': {
        category: 'Stage',
        description: 'Path to rule configuration file for stage',
        required: true
    },
    'depends-on': {
        category: 'Stage',
        description: 'List of stage dependencies',
        required: false
    },
    'failure-policy': {
        category: 'Stage',
        description: 'Stage failure handling policy',
        required: false
    },
    'stage-metadata': {
        category: 'Stage',
        description: 'Additional metadata for stage',
        required: false
    },
    
    // External References
    'rule-refs': {
        category: 'Document',
        description: 'References to external rule configurations',
        required: false
    }
};

/**
 * Apply APEX keyword tooltips to a code element
 * @param {HTMLElement} codeElement - The <code> element containing highlighted YAML
 */
function applyApexKeywordTooltips(codeElement) {
    if (!codeElement) return;
    
    // Get all token elements created by Prism
    const tokens = codeElement.querySelectorAll('.token.key, .token.property');
    
    tokens.forEach(token => {
        const keyword = token.textContent.trim().replace(/:$/, ''); // Remove trailing colon
        
        if (APEX_KEYWORDS[keyword]) {
            const info = APEX_KEYWORDS[keyword];
            
            // Wrap the token in a span with tooltip data
            const wrapper = document.createElement('span');
            wrapper.className = 'apex-keyword-tooltip';
            wrapper.setAttribute('data-keyword', keyword);
            wrapper.setAttribute('data-description', info.description);
            wrapper.setAttribute('data-category', info.category);
            wrapper.setAttribute('data-required', info.required);
            
            // Replace the token with the wrapper
            token.parentNode.insertBefore(wrapper, token);
            wrapper.appendChild(token);
        }
    });
}

