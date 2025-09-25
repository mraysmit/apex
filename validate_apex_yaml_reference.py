#!/usr/bin/env python3
"""
Comprehensive validation of APEX_YAML_REFERENCE.md against actual APEX core implementation.
Identifies all invalid YAML syntax examples in the documentation.
"""

import re
import yaml
from typing import Set, List, Dict, Tuple

def get_definitive_apex_keywords() -> Set[str]:
    """Definitive list of APEX keywords based on @JsonProperty annotations in APEX core."""
    return {
        # Top-level configuration sections
        'metadata', 'data-sources', 'data-source-refs', 'rule-refs', 'data-sinks',
        'categories', 'rules', 'rule-groups', 'enrichments', 'transformations',
        
        # Metadata properties (ConfigurationMetadata class)
        'id', 'name', 'version', 'description', 'type', 'author', 'created', 'last-modified', 'tags',
        
        # Rule properties (YamlRule class)
        'category', 'condition', 'message', 'priority', 'severity', 'enabled', 'validation',
        'created-by', 'business-domain', 'business-owner', 'source-system', 
        'effective-date', 'expiration-date', 'custom-properties',
        
        # Rule validation config
        'required-fields', 'field-types', 'custom-validators',
        
        # Enrichment properties (YamlEnrichment class)
        'target-type', 'field-mappings', 'conditional-mappings', 'target-field',
        'mapping-rules', 'execution-settings', 'lookup-config', 'calculation-config',
        
        # Field mapping properties
        'source-field', 'transformation', 'default-value', 'required',
        
        # Transformation properties
        'transformation-rules',
        
        # Data source properties (YamlDataSource class)
        'source-type', 'implementation', 'connection', 'cache', 'health-check',
        'authentication', 'queries', 'operations', 'endpoints', 'topics',
        'key-patterns', 'file-format', 'circuit-breaker', 'response-mapping',
        'parameter-names',
        
        # Connection-level properties (processed in convertToConnectionConfig)
        'bootstrap-servers', 'security-protocol', 'sasl-mechanism', 'base-path',
        'file-pattern', 'polling-interval', 'encoding', 'connection-pool'
    }

def get_invalid_keywords() -> Set[str]:
    """Keywords that are NOT supported by APEX core but might appear in examples."""
    return {
        # Pipeline/ETL keywords (not APEX core)
        'pipeline', 'steps', 'execution', 'mode', 'error-handling', 'max-retries',
        'retry-delay-ms', 'depends-on', 'sink', 'operation', 'schema', 'auto-create',
        'table-name', 'init-script', 'column-mappings', 'has-header-row', 'delimiter',
        
        # Processing configuration (not supported)
        'processing', 'parallel', 'timeout', 'retry-count',
        
        # Logging configuration (not supported)
        'logging', 'level', 'include-context',
        
        # Performance configuration (not supported)
        'performance', 'cache-enabled', 'cache-ttl', 'max-throughput', 'avg-latency',
        'memory-usage',
        
        # Database/connection properties (not APEX keywords)
        'database', 'username', 'password', 'url', 'driver', 'host', 'port',
        'keyHeader', 'keyValue', 'keyPrefix',
        
        # File system properties (not APEX keywords)
        'path', 'filename', 'directory',
        
        # Demo/example field names (not APEX syntax)
        'customerId', 'customerName', 'email', 'amount', 'currency', 'tradeId',
        'counterparty', 'symbol', 'quantity', 'price'
    }

def extract_yaml_blocks(content: str) -> List[Tuple[str, int]]:
    """Extract all YAML code blocks with their line numbers."""
    yaml_blocks = []
    lines = content.split('\n')
    
    in_yaml_block = False
    yaml_content = []
    start_line = 0
    
    for i, line in enumerate(lines):
        if line.strip() == '```yaml':
            in_yaml_block = True
            yaml_content = []
            start_line = i + 1
        elif line.strip() == '```' and in_yaml_block:
            in_yaml_block = False
            yaml_blocks.append(('\n'.join(yaml_content), start_line))
        elif in_yaml_block:
            yaml_content.append(line)
    
    return yaml_blocks

def validate_yaml_block(yaml_content: str, line_number: int, valid_keywords: Set[str],
                       invalid_keywords: Set[str]) -> List[str]:
    """Validate a single YAML block and return list of issues."""
    issues = []

    try:
        # Parse YAML to get structure
        data = yaml.safe_load(yaml_content)
        if data:
            found_keywords = extract_keywords_from_data_with_context(data)

            # Check for invalid keywords
            for keyword, context in found_keywords:
                if not isinstance(keyword, str):
                    continue  # Skip non-string keys

                # Skip field names in specific contexts
                if is_field_name_context(keyword, context):
                    continue

                if keyword in invalid_keywords:
                    issues.append(f"Line ~{line_number}: INVALID keyword '{keyword}' - not supported by APEX core")
                elif keyword not in valid_keywords and not is_field_name(keyword):
                    issues.append(f"Line ~{line_number}: UNKNOWN keyword '{keyword}' - not in APEX core @JsonProperty annotations")

    except yaml.YAMLError as e:
        issues.append(f"Line ~{line_number}: YAML parsing error: {e}")

    return issues

def extract_keywords_from_data_with_context(data, context="", keywords=None) -> Set[Tuple[str, str]]:
    """Recursively extract all keywords from YAML data structure with context."""
    if keywords is None:
        keywords = set()

    if isinstance(data, dict):
        for key, value in data.items():
            new_context = f"{context}.{key}" if context else key
            keywords.add((key, context))
            extract_keywords_from_data_with_context(value, new_context, keywords)
    elif isinstance(data, list):
        for item in data:
            extract_keywords_from_data_with_context(item, context, keywords)

    return keywords

def is_field_name_context(keyword: str, context: str) -> bool:
    """Check if a keyword is a field name based on its context."""
    # Field names in these contexts should be ignored
    field_contexts = [
        'field-types',
        'required-fields',
        'custom-properties',
        'connection',
        'queries',
        'endpoints',
        'topics',
        'key-patterns'
    ]

    return any(field_context in context for field_context in field_contexts)

def extract_keywords_from_data(data, keywords=None) -> Set[str]:
    """Recursively extract all keywords from YAML data structure."""
    if keywords is None:
        keywords = set()
    
    if isinstance(data, dict):
        for key, value in data.items():
            keywords.add(key)
            extract_keywords_from_data(value, keywords)
    elif isinstance(data, list):
        for item in data:
            extract_keywords_from_data(item, keywords)
    
    return keywords

def is_field_name(keyword) -> bool:
    """Check if a keyword is likely a field name rather than APEX syntax."""
    if not isinstance(keyword, str):
        return True  # Non-string keys are likely data values, not APEX syntax

    # Common field name patterns
    field_patterns = [
        r'^[a-z][A-Za-z]*$',  # camelCase field names
        r'^[a-z]+_[a-z]+',    # snake_case field names
        r'.*Id$',             # ends with Id
        r'.*Name$',           # ends with Name
        r'.*Date$',           # ends with Date
        r'.*Time$',           # ends with Time
    ]

    # Specific field names that are commonly used in examples
    common_field_names = {
        'customerId', 'customerName', 'email', 'amount', 'currency',
        'tradeId', 'counterparty', 'symbol', 'quantity', 'price',
        'firstName', 'lastName', 'tradeDate', 'region', 'customerTier'
    }

    return keyword in common_field_names or any(re.match(pattern, keyword) for pattern in field_patterns)

def main():
    print("🔍 APEX YAML Reference Validation")
    print("=" * 50)
    
    # Load the document
    try:
        with open("docs/APEX_YAML_REFERENCE.md", 'r', encoding='utf-8') as f:
            content = f.read()
    except FileNotFoundError:
        print("❌ Error: docs/APEX_YAML_REFERENCE.md not found")
        return
    
    # Get keyword sets
    valid_keywords = get_definitive_apex_keywords()
    invalid_keywords = get_invalid_keywords()
    
    print(f"✅ Valid APEX keywords: {len(valid_keywords)}")
    print(f"❌ Known invalid keywords: {len(invalid_keywords)}")
    
    # Extract and validate YAML blocks
    yaml_blocks = extract_yaml_blocks(content)
    print(f"📄 Found {len(yaml_blocks)} YAML code blocks")
    
    all_issues = []
    
    for i, (yaml_content, line_number) in enumerate(yaml_blocks):
        issues = validate_yaml_block(yaml_content, line_number, valid_keywords, invalid_keywords)
        all_issues.extend(issues)
    
    # Report results
    print(f"\n🚨 VALIDATION RESULTS:")
    print(f"Total issues found: {len(all_issues)}")
    
    if all_issues:
        print("\n❌ ISSUES FOUND:")
        for issue in sorted(set(all_issues)):  # Remove duplicates and sort
            print(f"  • {issue}")
    else:
        print("\n✅ No issues found - all YAML examples use valid APEX syntax!")

if __name__ == "__main__":
    main()
