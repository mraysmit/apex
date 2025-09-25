#!/usr/bin/env python3
"""
APEX Documentation Gap Analysis

This script compares the keywords extracted from apex-demo YAML files
with the keywords documented in APEX_YAML_REFERENCE.md to identify
documentation gaps and missing coverage.
"""

import re
from pathlib import Path
from typing import Set, List, Dict

def load_extracted_keywords() -> Set[str]:
    """Load keywords extracted from apex-demo YAML files."""
    keywords = set()
    
    try:
        with open("apex_keywords_extracted.txt", 'r') as f:
            content = f.read()
        
        # Extract keywords from the complete alphabetical list
        in_alphabetical_section = False
        for line in content.split('\n'):
            if "Complete Alphabetical List" in line:
                in_alphabetical_section = True
                continue
            elif in_alphabetical_section and line.strip().startswith('- '):
                keyword = line.strip()[2:]  # Remove '- ' prefix
                keywords.add(keyword)
    
    except FileNotFoundError:
        print("Error: apex_keywords_extracted.txt not found. Run extract_apex_keywords.py first.")
        return set()
    
    return keywords

def extract_documented_keywords() -> Set[str]:
    """Extract keywords from APEX_YAML_REFERENCE.md."""
    keywords = set()
    
    try:
        with open("docs/APEX_YAML_REFERENCE.md", 'r', encoding='utf-8') as f:
            content = f.read()
        
        # Extract YAML property names from code blocks
        yaml_blocks = re.findall(r'```yaml\n(.*?)\n```', content, re.DOTALL)
        for block in yaml_blocks:
            # Find property names (lines that end with :)
            properties = re.findall(r'^\s*([a-zA-Z][a-zA-Z0-9_-]*):(?:\s|$)', block, re.MULTILINE)
            keywords.update(properties)
        
        # Extract keywords from table headers and descriptions
        table_keywords = re.findall(r'\|\s*`([a-zA-Z][a-zA-Z0-9_-]*)`\s*\|', content)
        keywords.update(table_keywords)
        
        # Extract field names from field-mappings examples
        field_mappings = re.findall(r'(source-field|target-field):\s*"([^"]+)"', content)
        for _, field_name in field_mappings:
            # Extract the property name (last part after dots)
            if '.' in field_name:
                keywords.add(field_name.split('.')[-1])
            else:
                keywords.add(field_name)
    
    except FileNotFoundError:
        print("Error: docs/APEX_YAML_REFERENCE.md not found.")
        return set()
    
    return keywords

def categorize_missing_keywords(missing_keywords: Set[str]) -> Dict[str, List[str]]:
    """Categorize missing keywords by their likely purpose."""
    categories = {
        'Core APEX Keywords': [],
        'Field Enrichment': [],
        'Data Sources': [],
        'Configuration': [],
        'Business Logic': [],
        'Performance/Caching': [],
        'Database/Queries': [],
        'REST API': [],
        'Financial Domain': [],
        'Other': []
    }
    
    # Define patterns for categorization
    patterns = {
        'Core APEX Keywords': [
            'default-value', 'field-enrichment', 'calculation-enrichment', 'lookup-enrichment',
            'field-mappings', 'source-field', 'target-field', 'result-field', 'calculation-config'
        ],
        'Field Enrichment': [
            'enrichment', 'mapping', 'transformation', 'field', 'target', 'source'
        ],
        'Data Sources': [
            'data-source', 'database', 'connection', 'query', 'endpoint', 'rest-api'
        ],
        'Configuration': [
            'config', 'setting', 'parameter', 'timeout', 'retry', 'enabled', 'disabled'
        ],
        'Performance/Caching': [
            'cache', 'ttl', 'performance', 'metrics', 'monitoring', 'statistics'
        ],
        'Database/Queries': [
            'sql', 'query', 'table', 'column', 'database', 'connection', 'pool'
        ],
        'REST API': [
            'rest', 'api', 'http', 'endpoint', 'url', 'response', 'request'
        ],
        'Financial Domain': [
            'trade', 'currency', 'market', 'price', 'risk', 'settlement', 'counterparty',
            'instrument', 'portfolio', 'customer', 'account'
        ]
    }
    
    for keyword in sorted(missing_keywords):
        categorized = False
        for category, pattern_list in patterns.items():
            if keyword in pattern_list or any(pattern in keyword.lower() for pattern in pattern_list):
                categories[category].append(keyword)
                categorized = True
                break
        
        if not categorized:
            categories['Other'].append(keyword)
    
    return categories

def identify_critical_missing_keywords(missing_keywords: Set[str]) -> List[str]:
    """Identify the most critical missing keywords that should be documented."""
    critical_keywords = [
        'default-value',           # Used in field-enrichment but not documented
        'field-enrichment',        # Enrichment type not properly documented
        'calculation-config',      # Used in calculation enrichments
        'result-field',           # Used in calculation enrichments
        'field-mappings',         # Core concept but limited documentation
        'data-source-ref',        # External data source references
        'query-ref',              # Named query references
        'operation-ref',          # REST API operation references
        'cache',                  # Caching configuration
        'ttlSeconds',             # Cache TTL
        'maxSize',                # Cache size limits
        'keyPrefix',              # Cache key prefixes
        'enabled',                # Enable/disable flags
        'required',               # Required field flags
        'parameters',             # Query parameters
        'connection',             # Database connections
        'base-url',               # REST API base URLs
        'endpoints',              # REST API endpoints
    ]
    
    return [kw for kw in critical_keywords if kw in missing_keywords]

def main():
    print("🔍 APEX Documentation Gap Analysis")
    print("=" * 50)
    
    # Load extracted keywords from apex-demo
    extracted_keywords = load_extracted_keywords()
    print(f"📊 Keywords found in apex-demo: {len(extracted_keywords)}")
    
    # Load documented keywords from APEX_YAML_REFERENCE.md
    documented_keywords = extract_documented_keywords()
    print(f"📖 Keywords documented in APEX_YAML_REFERENCE.md: {len(documented_keywords)}")
    
    # Find gaps
    missing_keywords = extracted_keywords - documented_keywords
    documented_but_unused = documented_keywords - extracted_keywords
    
    print(f"\n🚨 Missing from documentation: {len(missing_keywords)} keywords")
    print(f"⚠️  Documented but not used: {len(documented_but_unused)} keywords")
    
    # Identify critical missing keywords
    critical_missing = identify_critical_missing_keywords(missing_keywords)
    
    print(f"\n🔥 CRITICAL Missing Keywords ({len(critical_missing)}):")
    print("=" * 50)
    for keyword in critical_missing:
        print(f"  ❌ {keyword}")
    
    # Categorize all missing keywords
    categorized_missing = categorize_missing_keywords(missing_keywords)
    
    print(f"\n📋 All Missing Keywords by Category:")
    print("=" * 50)
    
    for category, keywords in categorized_missing.items():
        if keywords:
            print(f"\n🔸 {category} ({len(keywords)} keywords):")
            for keyword in keywords[:10]:  # Show first 10 to avoid overwhelming output
                print(f"  - {keyword}")
            if len(keywords) > 10:
                print(f"  ... and {len(keywords) - 10} more")
    
    # Generate recommendations
    print(f"\n💡 RECOMMENDATIONS:")
    print("=" * 50)
    print("1. 🎯 HIGH PRIORITY: Document field-enrichment type with:")
    print("   - Property table (id, type, condition, field-mappings)")
    print("   - default-value keyword explanation")
    print("   - Multiple examples showing different scenarios")
    print()
    print("2. 🔧 MEDIUM PRIORITY: Enhance existing documentation:")
    print("   - Add calculation-config and result-field to calculation-enrichment")
    print("   - Document caching properties (ttlSeconds, maxSize, keyPrefix)")
    print("   - Add more field-mappings examples with required/default-value")
    print()
    print("3. 📚 LOW PRIORITY: Document domain-specific keywords:")
    print("   - Financial domain keywords (trade, currency, risk, etc.)")
    print("   - Performance monitoring keywords")
    print("   - Advanced configuration options")
    
    # Save detailed results
    output_file = "apex_documentation_gaps.txt"
    with open(output_file, 'w') as f:
        f.write("APEX Documentation Gap Analysis Results\n")
        f.write("=" * 50 + "\n\n")
        f.write(f"Keywords in apex-demo: {len(extracted_keywords)}\n")
        f.write(f"Keywords documented: {len(documented_keywords)}\n")
        f.write(f"Missing from documentation: {len(missing_keywords)}\n")
        f.write(f"Documented but unused: {len(documented_but_unused)}\n\n")
        
        f.write("CRITICAL Missing Keywords:\n")
        f.write("-" * 30 + "\n")
        for keyword in critical_missing:
            f.write(f"  - {keyword}\n")
        
        f.write("\nAll Missing Keywords by Category:\n")
        f.write("-" * 30 + "\n")
        for category, keywords in categorized_missing.items():
            if keywords:
                f.write(f"\n{category} ({len(keywords)}):\n")
                for keyword in keywords:
                    f.write(f"  - {keyword}\n")
        
        f.write("\nDocumented but Not Used:\n")
        f.write("-" * 30 + "\n")
        for keyword in sorted(documented_but_unused):
            f.write(f"  - {keyword}\n")
    
    print(f"\n💾 Detailed results saved to: {output_file}")

if __name__ == "__main__":
    main()
