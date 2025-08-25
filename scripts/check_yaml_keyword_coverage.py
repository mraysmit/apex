#!/usr/bin/env python3
"""
APEX YAML Keyword Coverage Analysis

This script analyzes the APEX_YAML_REFERENCE.md document to extract all YAML keywords,
properties, and configuration elements, then checks if they are covered in at least
one self-contained demo or example in the apex-demo module.
"""

import os
import re
import yaml
from pathlib import Path
from typing import Set, Dict, List, Tuple
from collections import defaultdict

# Keywords to extract from the reference document
YAML_KEYWORDS = {
    # Top-level sections
    'metadata', 'rules', 'enrichments', 'calculations', 'dataSources', 'configuration',
    'data', 'scenario', 'bootstrap', 'scenarios', 'rule-chains', 'endpoints', 'queries',
    
    # Metadata fields
    'name', 'version', 'description', 'type', 'author', 'created-by', 'created-date',
    'domain', 'tags', 'business-domain', 'owner', 'source',
    
    # Processing configuration
    'processing', 'logging', 'performance', 'cache-enabled', 'cache-ttl', 'parallel',
    'timeout', 'retry-count', 'level', 'include-context',
    
    # Rule properties
    'id', 'condition', 'message', 'severity', 'priority', 'error-code', 'retry-on-failure',
    
    # Enrichment properties
    'lookup-config', 'lookup-key', 'lookup-dataset', 'field-mappings', 'source-field',
    'target-field', 'key-field', 'cache-ttl', 'required',
    
    # Dataset properties
    'type', 'inline', 'external', 'endpoint', 'cache-ttl', 'timeout',
    
    # Data source properties
    'connection', 'host', 'port', 'database', 'username', 'password', 'baseUrl',
    'authentication', 'keyHeader', 'keyValue', 'enabled', 'sourceType',
    
    # Calculation properties
    'field', 'expression',
    
    # Monitoring and health
    'monitoring', 'healthCheckLogging', 'defaultConnectionTimeout',
}

# SpEL expressions and patterns to look for
SPEL_PATTERNS = {
    '#data', '#root', '#this', 'T(', '.matches(', '.contains(', '.startsWith(',
    '.endsWith(', '.toUpperCase(', '.toLowerCase(', '.trim(', '.substring(',
    '.length(', '.size(', '.isAfter(', '.isBefore(', '.plusDays(', '.minusYears(',
    '.format(', 'LocalDate.now()', 'Math.max(', 'Math.min(', 'UUID.randomUUID()',
}

def extract_keywords_from_reference() -> Set[str]:
    """Extract all YAML keywords from the APEX_YAML_REFERENCE.md document."""
    reference_path = Path("docs/APEX_YAML_REFERENCE.md")
    if not reference_path.exists():
        print(f"Warning: {reference_path} not found")
        return YAML_KEYWORDS
    
    keywords = set(YAML_KEYWORDS)
    
    with open(reference_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Extract YAML property names from code blocks
    yaml_blocks = re.findall(r'```yaml\n(.*?)\n```', content, re.DOTALL)
    for block in yaml_blocks:
        # Find property names (lines that end with :)
        properties = re.findall(r'^\s*([a-zA-Z][a-zA-Z0-9_-]*):(?:\s|$)', block, re.MULTILINE)
        keywords.update(properties)
    
    # Extract field names from field-mappings examples
    field_mappings = re.findall(r'(source-field|target-field):\s*"([^"]+)"', content)
    for _, field_name in field_mappings:
        # Extract the property name (last part after dots)
        if '.' in field_name:
            keywords.add(field_name.split('.')[-1])
        else:
            keywords.add(field_name)
    
    return keywords

def find_yaml_files(directory: str) -> List[Path]:
    """Find all YAML files in the given directory."""
    yaml_files = []
    for root, dirs, files in os.walk(directory):
        # Skip target directories
        if 'target' in root:
            continue
        for file in files:
            if file.endswith(('.yaml', '.yml')):
                yaml_files.append(Path(root) / file)
    return yaml_files

def analyze_yaml_file(file_path: Path) -> Tuple[Set[str], Set[str]]:
    """Analyze a YAML file and return found keywords and SpEL patterns."""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # Parse YAML to extract structure
        yaml_data = yaml.safe_load(content)
        
        found_keywords = set()
        found_spel = set()
        
        # Extract keywords from YAML structure
        def extract_from_dict(data, path=""):
            if isinstance(data, dict):
                for key, value in data.items():
                    found_keywords.add(key)
                    extract_from_dict(value, f"{path}.{key}" if path else key)
            elif isinstance(data, list):
                for item in data:
                    extract_from_dict(item, path)
        
        if yaml_data:
            extract_from_dict(yaml_data)
        
        # Extract SpEL patterns from content
        for pattern in SPEL_PATTERNS:
            if pattern in content:
                found_spel.add(pattern)
        
        return found_keywords, found_spel
        
    except Exception as e:
        print(f"Error analyzing {file_path}: {e}")
        return set(), set()

def main():
    print("APEX YAML Keyword Coverage Analysis")
    print("=" * 50)
    
    # Extract keywords from reference document
    reference_keywords = extract_keywords_from_reference()
    print(f"Found {len(reference_keywords)} keywords in APEX_YAML_REFERENCE.md")
    
    # Find all YAML files in apex-demo
    apex_demo_path = "apex-demo"
    if not os.path.exists(apex_demo_path):
        print(f"Error: {apex_demo_path} directory not found")
        return
    
    yaml_files = find_yaml_files(apex_demo_path)
    print(f"Found {len(yaml_files)} YAML files in apex-demo")
    
    # Analyze coverage
    covered_keywords = set()
    covered_spel = set()
    file_coverage = {}
    
    for yaml_file in yaml_files:
        keywords, spel = analyze_yaml_file(yaml_file)
        covered_keywords.update(keywords)
        covered_spel.update(spel)
        file_coverage[yaml_file] = (keywords, spel)
    
    # Calculate coverage statistics
    covered_reference_keywords = reference_keywords & covered_keywords
    missing_keywords = reference_keywords - covered_keywords
    coverage_percentage = (len(covered_reference_keywords) / len(reference_keywords)) * 100
    
    print(f"\nCOVERAGE ANALYSIS")
    print("=" * 50)
    print(f"Total reference keywords: {len(reference_keywords)}")
    print(f"Covered keywords: {len(covered_reference_keywords)}")
    print(f"Missing keywords: {len(missing_keywords)}")
    print(f"Coverage percentage: {coverage_percentage:.1f}%")
    
    if missing_keywords:
        print(f"\nMISSING KEYWORDS ({len(missing_keywords)}):")
        print("=" * 30)
        for keyword in sorted(missing_keywords):
            print(f"  - {keyword}")
    
    print(f"\nSPEL PATTERNS COVERED ({len(covered_spel)}):")
    print("=" * 30)
    for pattern in sorted(covered_spel):
        print(f"  ✓ {pattern}")
    
    missing_spel = SPEL_PATTERNS - covered_spel
    if missing_spel:
        print(f"\nMISSING SPEL PATTERNS ({len(missing_spel)}):")
        print("=" * 30)
        for pattern in sorted(missing_spel):
            print(f"  - {pattern}")

    # Categorize missing keywords by type
    print(f"\nMISSING KEYWORDS BY CATEGORY:")
    print("=" * 40)

    external_data_keywords = {'dataSources', 'baseUrl', 'host', 'port', 'database', 'endpoints',
                             'queries', 'getUserById', 'getActiveUsers', 'getCurrentRate',
                             'getHistoricalRate', 'keyHeader', 'keyValue', 'sourceType',
                             'defaultConnectionTimeout', 'healthCheckLogging'}

    cache_keywords = {'cache', 'cache-ttl', 'ttlSeconds', 'maxSize'}

    processing_keywords = {'parallel', 'timeout', 'retry-count', 'retry-on-failure',
                          'include-context', 'error-code'}

    dataset_keywords = {'inline', 'external'}

    business_keywords = {'bootstrap', 'scenarios', 'counterpartyLEI', 'creditRating',
                        'settlementMethod'}

    missing_external = missing_keywords & external_data_keywords
    missing_cache = missing_keywords & cache_keywords
    missing_processing = missing_keywords & processing_keywords
    missing_dataset = missing_keywords & dataset_keywords
    missing_business = missing_keywords & business_keywords
    missing_other = missing_keywords - external_data_keywords - cache_keywords - processing_keywords - dataset_keywords - business_keywords

    if missing_external:
        print(f"\nExternal Data Configuration ({len(missing_external)}):")
        for kw in sorted(missing_external):
            print(f"  - {kw}")

    if missing_cache:
        print(f"\nCaching Configuration ({len(missing_cache)}):")
        for kw in sorted(missing_cache):
            print(f"  - {kw}")

    if missing_processing:
        print(f"\nProcessing Configuration ({len(missing_processing)}):")
        for kw in sorted(missing_processing):
            print(f"  - {kw}")

    if missing_dataset:
        print(f"\nDataset Configuration ({len(missing_dataset)}):")
        for kw in sorted(missing_dataset):
            print(f"  - {kw}")

    if missing_business:
        print(f"\nBusiness Logic ({len(missing_business)}):")
        for kw in sorted(missing_business):
            print(f"  - {kw}")

    if missing_other:
        print(f"\nOther ({len(missing_other)}):")
        for kw in sorted(missing_other):
            print(f"  - {kw}")

    print(f"\nRECOMMENDATIONS:")
    print("=" * 20)
    print("1. Create external-data-config examples demonstrating database and REST API connections")
    print("2. Add caching configuration examples with TTL and size limits")
    print("3. Create processing configuration examples with parallel execution and retry logic")
    print("4. Add inline vs external dataset comparison examples")
    print("5. Create comprehensive SpEL pattern examples covering all missing patterns")
    print("6. Add bootstrap and scenario registry examples")

if __name__ == "__main__":
    main()
