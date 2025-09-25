#!/usr/bin/env python3
"""
APEX YAML Keyword Extraction Script

This script scans all YAML files in the apex-demo module to extract
a comprehensive list of valid APEX keywords, properties, and syntax elements.
"""

import os
import re
import yaml
from pathlib import Path
from typing import Set, Dict, List
from collections import defaultdict

def find_yaml_files(root_dir: str) -> List[Path]:
    """Find all YAML files in the apex-demo module."""
    yaml_files = []
    root_path = Path(root_dir)
    
    # Skip target directories and other build artifacts
    excluded_dirs = {'target', 'node_modules', '.git', '.idea'}
    
    for file_path in root_path.rglob("*.yaml"):
        if not any(excluded in file_path.parts for excluded in excluded_dirs):
            yaml_files.append(file_path)
    
    for file_path in root_path.rglob("*.yml"):
        if not any(excluded in file_path.parts for excluded in excluded_dirs):
            yaml_files.append(file_path)
    
    return sorted(yaml_files)

def extract_keywords_from_yaml(file_path: Path) -> Set[str]:
    """Extract all YAML property names from a file."""
    keywords = set()
    
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # Parse YAML to extract structure
        try:
            yaml_data = yaml.safe_load(content)
            if yaml_data:
                extract_keys_recursive(yaml_data, keywords)
        except yaml.YAMLError:
            # If YAML parsing fails, extract keywords using regex
            extract_keywords_regex(content, keywords)
            
    except Exception as e:
        print(f"Error processing {file_path}: {e}")
    
    return keywords

def extract_keys_recursive(data, keywords: Set[str], path=""):
    """Recursively extract all keys from YAML data structure."""
    if isinstance(data, dict):
        for key, value in data.items():
            keywords.add(key)
            extract_keys_recursive(value, keywords, f"{path}.{key}" if path else key)
    elif isinstance(data, list):
        for item in data:
            extract_keys_recursive(item, keywords, path)

def extract_keywords_regex(content: str, keywords: Set[str]):
    """Extract keywords using regex when YAML parsing fails."""
    # Find property names (lines that end with :)
    properties = re.findall(r'^\s*([a-zA-Z][a-zA-Z0-9_-]*):(?:\s|$)', content, re.MULTILINE)
    keywords.update(properties)

def categorize_keywords(keywords: Set[str]) -> Dict[str, List[str]]:
    """Categorize keywords by their likely purpose."""
    categories = {
        'metadata': [],
        'enrichment': [],
        'rules': [],
        'data_sources': [],
        'field_mappings': [],
        'calculations': [],
        'lookups': [],
        'conditions': [],
        'configuration': [],
        'other': []
    }
    
    # Define keyword patterns for categorization
    patterns = {
        'metadata': ['id', 'name', 'version', 'description', 'type', 'author', 'created-date', 'tags'],
        'enrichment': ['enrichments', 'field-enrichment', 'lookup-enrichment', 'calculation-enrichment'],
        'rules': ['rules', 'condition', 'message', 'severity', 'priority'],
        'data_sources': ['data-sources', 'data-source-refs', 'connection', 'base-url', 'endpoints'],
        'field_mappings': ['field-mappings', 'source-field', 'target-field', 'default-value', 'required'],
        'calculations': ['calculations', 'calculation-config', 'expression', 'result-field'],
        'lookups': ['lookup-config', 'lookup-key', 'lookup-dataset', 'key-field', 'data'],
        'conditions': ['enabled', 'condition'],
        'configuration': ['cache', 'ttlSeconds', 'maxSize', 'keyPrefix', 'parameters']
    }
    
    for keyword in sorted(keywords):
        categorized = False
        for category, pattern_list in patterns.items():
            if keyword in pattern_list or any(pattern in keyword.lower() for pattern in pattern_list):
                categories[category].append(keyword)
                categorized = True
                break
        
        if not categorized:
            categories['other'].append(keyword)
    
    return categories

def main():
    print("🔍 APEX YAML Keyword Extraction")
    print("=" * 50)
    
    # Find all YAML files in apex-demo
    apex_demo_path = "apex-demo"
    if not os.path.exists(apex_demo_path):
        print(f"Error: {apex_demo_path} directory not found")
        return
    
    yaml_files = find_yaml_files(apex_demo_path)
    print(f"📊 Found {len(yaml_files)} YAML files in apex-demo")
    
    # Extract all keywords
    all_keywords = set()
    file_keywords = {}
    
    for yaml_file in yaml_files:
        keywords = extract_keywords_from_yaml(yaml_file)
        all_keywords.update(keywords)
        file_keywords[yaml_file] = keywords
        print(f"  ✅ {yaml_file.name}: {len(keywords)} keywords")
    
    print(f"\n🎯 Total unique keywords found: {len(all_keywords)}")
    
    # Categorize keywords
    categories = categorize_keywords(all_keywords)
    
    print("\n📋 APEX Keywords by Category:")
    print("=" * 50)
    
    for category, keywords in categories.items():
        if keywords:
            print(f"\n🔸 {category.upper().replace('_', ' ')} ({len(keywords)} keywords):")
            for keyword in sorted(keywords):
                print(f"  - {keyword}")
    
    # Generate comprehensive keyword list
    print(f"\n📝 Complete Alphabetical List ({len(all_keywords)} keywords):")
    print("=" * 50)
    for keyword in sorted(all_keywords):
        print(f"  - {keyword}")
    
    # Save results to file
    output_file = "apex_keywords_extracted.txt"
    with open(output_file, 'w') as f:
        f.write("APEX YAML Keywords Extracted from apex-demo Module\n")
        f.write("=" * 50 + "\n\n")
        f.write(f"Total files analyzed: {len(yaml_files)}\n")
        f.write(f"Total unique keywords: {len(all_keywords)}\n\n")
        
        f.write("Keywords by Category:\n")
        f.write("-" * 30 + "\n")
        for category, keywords in categories.items():
            if keywords:
                f.write(f"\n{category.upper().replace('_', ' ')} ({len(keywords)}):\n")
                for keyword in sorted(keywords):
                    f.write(f"  - {keyword}\n")
        
        f.write(f"\nComplete Alphabetical List ({len(all_keywords)}):\n")
        f.write("-" * 30 + "\n")
        for keyword in sorted(all_keywords):
            f.write(f"  - {keyword}\n")
    
    print(f"\n💾 Results saved to: {output_file}")

if __name__ == "__main__":
    main()
