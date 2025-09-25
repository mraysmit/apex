#!/usr/bin/env python3
"""
Smart APEX Keyword Analysis

This script intelligently distinguishes between actual APEX syntax keywords
and field names/data values to provide accurate documentation gap analysis.
"""

import re
from pathlib import Path
from typing import Set, List, Dict

def get_true_apex_keywords() -> Set[str]:
    """Define the actual APEX syntax keywords (not field names or data values)."""
    return {
        # Core metadata
        'metadata', 'id', 'name', 'version', 'description', 'type', 'author', 'created-date', 'tags',
        
        # Document types
        'rule-config', 'enrichment', 'dataset', 'scenario', 'external-data-config',
        
        # Rules section
        'rules', 'condition', 'message', 'severity', 'priority',
        
        # Enrichments section
        'enrichments', 'enabled',
        
        # Enrichment types
        'field-enrichment', 'lookup-enrichment', 'calculation-enrichment',
        
        # Field enrichment
        'field-mappings', 'source-field', 'target-field', 'default-value', 'required',
        
        # Calculation enrichment
        'calculations', 'calculation-config', 'expression', 'result-field', 'field',
        
        # Lookup enrichment
        'lookup-config', 'lookup-key', 'lookup-dataset', 'key-field', 'data',
        
        # Data sources
        'data-sources', 'data-source-refs', 'data-source-ref', 'connection', 'base-url', 'endpoints',
        'cache', 'ttlSeconds', 'maxIdleSeconds', 'maxSize', 'keyPrefix',
        
        # Database connections
        'connection-timeout', 'maxConnectionPoolSize', 'minPoolSize', 'maxPoolSize',
        'test-on-borrow', 'test-while-idle', 'validation-query',
        
        # REST API
        'operations', 'operation-ref', 'method', 'path', 'headers', 'parameters',
        
        # External data configs
        'queries', 'query-ref', 'sql', 'namedQueries',
        
        # Pipeline configs
        'pipeline', 'steps', 'step', 'source', 'sink', 'transformation',
        
        # Rule groups
        'rule-groups', 'rule-group-references', 'rule-refs', 'rule-ids',
        
        # Performance and monitoring
        'performance', 'metrics', 'monitoring', 'statistics-enabled', 'metricsEnabled',
        'cache-enabled', 'enable-caching', 'include-performance-metrics',
        
        # Error handling
        'error-handling', 'continue-on-error', 'fail-fast', 'retry', 'retry-attempts',
        'retry-delay', 'max-retries', 'timeout', 'timeoutSeconds',
        
        # Validation
        'validation', 'required-fields', 'optional', 'mandatoryFields',
        
        # Configuration
        'configuration', 'settings', 'properties', 'format', 'encoding',
        'delimiter', 'hasHeaderRow', 'columnMappings'
    }

def extract_keywords_from_yaml_structure(file_path: Path) -> Set[str]:
    """Extract structural keywords from YAML files (not field values)."""
    keywords = set()
    
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # Extract top-level sections
        top_level = re.findall(r'^([a-zA-Z][a-zA-Z0-9_-]*):(?:\s|$)', content, re.MULTILINE)
        keywords.update(top_level)
        
        # Extract enrichment types
        enrichment_types = re.findall(r'type:\s*["\']?([a-zA-Z][a-zA-Z0-9_-]*)["\']?', content)
        keywords.update(enrichment_types)
        
        # Extract specific APEX structural keywords
        apex_patterns = [
            r'(field-mappings|source-field|target-field|default-value)',
            r'(lookup-config|lookup-key|lookup-dataset|key-field)',
            r'(calculation-config|calculations|expression|result-field)',
            r'(data-sources|data-source-refs|data-source-ref)',
            r'(rule-groups|rule-refs|rule-ids)',
            r'(cache|ttlSeconds|maxSize|keyPrefix)',
            r'(connection|base-url|endpoints|operations)',
            r'(queries|query-ref|namedQueries)',
            r'(pipeline|steps|transformation)'
        ]
        
        for pattern in apex_patterns:
            matches = re.findall(pattern, content)
            keywords.update(matches)
            
    except Exception as e:
        print(f"Error processing {file_path}: {e}")
    
    return keywords

def filter_apex_keywords(all_keywords: Set[str]) -> Set[str]:
    """Filter to keep only true APEX syntax keywords."""
    true_apex_keywords = get_true_apex_keywords()
    
    # Additional filtering rules
    filtered_keywords = set()
    
    for keyword in all_keywords:
        # Skip obvious field names and data values
        if any([
            keyword.startswith('get'),  # Database query names
            keyword.startswith('find'), # Database query names
            keyword.endswith('_id'),    # Field names
            keyword.endswith('_name'),  # Field names
            keyword.endswith('_type'),  # Field names
            keyword.endswith('_date'),  # Field names
            keyword.endswith('_status'), # Field names
            keyword.endswith('_details'), # Field names
            keyword.endswith('_profile'), # Field names
            keyword.endswith('_info'),   # Field names
            keyword.endswith('Account'), # Business field names
            keyword.endswith('Bank'),    # Business field names
            keyword.endswith('Rate'),    # Business field names
            keyword.endswith('Amount'),  # Business field names
            keyword.endswith('Currency'), # Business field names
            keyword.endswith('Tier'),    # Business field names
            keyword.endswith('Level'),   # Business field names
            keyword in ['active', 'dormant', 'prospect', 'churned'], # Status values
            keyword in ['equity', 'fixed_income', 'commodity', 'derivative'], # Asset classes
            keyword in ['nasdaq', 'nyse', 'cboe', 'nymex'], # Venue names
            keyword in ['goldman_sachs', 'jp_morgan', 'deutsche_bank', 'barclays', 'ubs'], # Bank names
            keyword.startswith('customer') and '_' in keyword, # Customer field names
            keyword.startswith('trade') and '_' in keyword,    # Trade field names
            keyword.startswith('risk') and '_' in keyword,     # Risk field names
            keyword.startswith('settlement') and '_' in keyword, # Settlement field names
            len(keyword) > 30,  # Very long names are likely field names
            keyword.replace('-', '').replace('_', '').isdigit(), # Numeric values
        ]):
            continue
            
        # Keep if it's a known APEX keyword or looks like APEX syntax
        if (keyword in true_apex_keywords or 
            keyword.endswith('-enrichment') or
            keyword.endswith('-config') or
            keyword.endswith('-ref') or
            keyword.endswith('-refs') or
            keyword.startswith('data-') or
            keyword.startswith('rule-') or
            keyword.startswith('cache-') or
            keyword.startswith('connection-') or
            keyword.startswith('max-') or
            keyword.startswith('min-') or
            keyword.startswith('enable-') or
            keyword.startswith('include-')):
            filtered_keywords.add(keyword)
    
    return filtered_keywords

def main():
    print("🧠 Smart APEX Keyword Analysis")
    print("=" * 50)
    
    # Load all keywords from previous extraction
    try:
        with open("apex_keywords_extracted.txt", 'r') as f:
            content = f.read()
        
        all_keywords = set()
        in_alphabetical_section = False
        for line in content.split('\n'):
            if "Complete Alphabetical List" in line:
                in_alphabetical_section = True
                continue
            elif in_alphabetical_section and line.strip().startswith('- '):
                keyword = line.strip()[2:]
                all_keywords.add(keyword)
                
    except FileNotFoundError:
        print("Error: Run extract_apex_keywords.py first")
        return
    
    print(f"📊 Total keywords extracted: {len(all_keywords)}")
    
    # Filter to true APEX keywords
    apex_keywords = filter_apex_keywords(all_keywords)
    print(f"🎯 True APEX syntax keywords: {len(apex_keywords)}")
    
    # Load documented keywords
    try:
        with open("docs/APEX_YAML_REFERENCE.md", 'r', encoding='utf-8') as f:
            content = f.read()
        
        documented_keywords = set()
        yaml_blocks = re.findall(r'```yaml\n(.*?)\n```', content, re.DOTALL)
        for block in yaml_blocks:
            properties = re.findall(r'^\s*([a-zA-Z][a-zA-Z0-9_-]*):(?:\s|$)', block, re.MULTILINE)
            documented_keywords.update(properties)
        
        # Filter documented keywords too
        documented_apex_keywords = filter_apex_keywords(documented_keywords)
        
    except FileNotFoundError:
        print("Error: APEX_YAML_REFERENCE.md not found")
        return
    
    print(f"📖 Documented APEX keywords: {len(documented_apex_keywords)}")
    
    # Find real gaps
    missing_keywords = apex_keywords - documented_apex_keywords
    documented_but_unused = documented_apex_keywords - apex_keywords
    
    print(f"\n🚨 REAL Missing Keywords: {len(missing_keywords)}")
    print(f"⚠️  Documented but unused: {len(documented_but_unused)}")
    
    # Categorize missing keywords
    critical_missing = []
    important_missing = []
    other_missing = []
    
    for keyword in sorted(missing_keywords):
        if keyword in ['default-value', 'field-enrichment', 'calculation-config', 'result-field']:
            critical_missing.append(keyword)
        elif any([
            keyword.endswith('-enrichment'),
            keyword.endswith('-config'),
            keyword.startswith('field-'),
            keyword.startswith('lookup-'),
            keyword.startswith('calculation-'),
            keyword.startswith('data-'),
            keyword.startswith('cache-')
        ]):
            important_missing.append(keyword)
        else:
            other_missing.append(keyword)
    
    print(f"\n🔥 CRITICAL Missing ({len(critical_missing)}):")
    for keyword in critical_missing:
        print(f"  ❌ {keyword}")
    
    print(f"\n⚠️  IMPORTANT Missing ({len(important_missing)}):")
    for keyword in important_missing:
        print(f"  - {keyword}")
    
    print(f"\n📝 OTHER Missing ({len(other_missing)}):")
    for keyword in other_missing:
        print(f"  - {keyword}")
    
    print(f"\n✅ Well Documented:")
    common_keywords = apex_keywords & documented_apex_keywords
    for keyword in sorted(common_keywords):
        print(f"  ✓ {keyword}")
    
    # Save smart results
    with open("smart_apex_analysis.txt", 'w') as f:
        f.write("Smart APEX Keyword Analysis Results\n")
        f.write("=" * 50 + "\n\n")
        f.write(f"Total keywords found: {len(all_keywords)}\n")
        f.write(f"True APEX keywords: {len(apex_keywords)}\n")
        f.write(f"Documented APEX keywords: {len(documented_apex_keywords)}\n")
        f.write(f"Missing from documentation: {len(missing_keywords)}\n\n")
        
        f.write("CRITICAL Missing Keywords:\n")
        for keyword in critical_missing:
            f.write(f"  - {keyword}\n")
        
        f.write("\nIMPORTANT Missing Keywords:\n")
        for keyword in important_missing:
            f.write(f"  - {keyword}\n")
        
        f.write("\nOTHER Missing Keywords:\n")
        for keyword in other_missing:
            f.write(f"  - {keyword}\n")
        
        f.write("\nWell Documented Keywords:\n")
        for keyword in sorted(common_keywords):
            f.write(f"  - {keyword}\n")
    
    print(f"\n💾 Smart analysis saved to: smart_apex_analysis.txt")

if __name__ == "__main__":
    main()
