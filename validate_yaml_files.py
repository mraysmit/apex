#!/usr/bin/env python3
"""
APEX YAML File Validation Script

This script validates all YAML files in the APEX project to ensure they conform
to the APEX system's YAML format requirements.

Requirements:
- All YAML files must have metadata section with: name, version, description, type
- Valid types: scenario, scenario-registry, bootstrap, rule-config, dataset, enrichment, rule-chain, external-data-config
- Type-specific required fields:
  - scenario: business-domain, owner
  - scenario-registry: created-by
  - bootstrap: business-domain, created-by
  - rule-config: author
  - dataset: source
  - enrichment: author
  - rule-chain: author
  - external-data-config: author
"""

import os
import yaml
import sys
from pathlib import Path
from typing import Dict, List, Set, Any, Optional

# APEX YAML format requirements
REQUIRED_METADATA_FIELDS = {"name", "version", "description", "type"}
VALID_FILE_TYPES = {
    "scenario", "scenario-registry", "bootstrap", "rule-config",
    "dataset", "enrichment", "rule-chain", "external-data-config"
}
TYPE_SPECIFIC_REQUIRED_FIELDS = {
    "scenario": {"business-domain", "owner"},
    "scenario-registry": {"created-by"},
    "bootstrap": {"business-domain", "created-by"},
    "rule-config": {"author"},
    "dataset": {"source"},
    "enrichment": {"author"},
    "rule-chain": {"author"},
    "external-data-config": {"author"}
}

# Files to exclude from validation (Spring Boot configs, etc.)
EXCLUDED_FILES = {
    "application.yml", "application-test.yml", "application-dev.yml", 
    "application-prod.yml", "logback.xml", "logback-spring.xml"
}

# Directories to exclude (build artifacts)
EXCLUDED_DIRS = {"target", "build", ".git", ".idea", "node_modules"}

class ValidationResult:
    def __init__(self, file_path: str):
        self.file_path = file_path
        self.errors: List[str] = []
        self.warnings: List[str] = []
        self.is_valid = True
    
    def add_error(self, message: str):
        self.errors.append(message)
        self.is_valid = False
    
    def add_warning(self, message: str):
        self.warnings.append(message)

def find_yaml_files(root_dir: str) -> List[str]:
    """Find all YAML files in the project, excluding build artifacts and Spring configs."""
    yaml_files = []
    root_path = Path(root_dir)
    
    for file_path in root_path.rglob("*.yaml"):
        # Skip excluded directories
        if any(excluded in file_path.parts for excluded in EXCLUDED_DIRS):
            continue
        # Skip excluded files
        if file_path.name in EXCLUDED_FILES:
            continue
        yaml_files.append(str(file_path))
    
    for file_path in root_path.rglob("*.yml"):
        # Skip excluded directories
        if any(excluded in file_path.parts for excluded in EXCLUDED_DIRS):
            continue
        # Skip excluded files
        if file_path.name in EXCLUDED_FILES:
            continue
        yaml_files.append(str(file_path))
    
    return sorted(yaml_files)

def load_yaml_file(file_path: str) -> Optional[Dict[str, Any]]:
    """Load and parse a YAML file."""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            return yaml.safe_load(f)
    except yaml.YAMLError as e:
        return None
    except Exception as e:
        return None

def validate_yaml_file(file_path: str) -> ValidationResult:
    """Validate a single YAML file against APEX requirements."""
    result = ValidationResult(file_path)
    
    # Load YAML content
    content = load_yaml_file(file_path)
    if content is None:
        result.add_error("Failed to parse YAML file - invalid syntax")
        return result
    

    
    # Check for metadata section
    metadata = content.get("metadata")
    if not metadata:
        result.add_error("Missing required 'metadata' section")
        return result
    
    # Check required metadata fields
    for field in REQUIRED_METADATA_FIELDS:
        if field not in metadata:
            result.add_error(f"Missing required metadata field: {field}")
    
    # Check valid file type
    file_type = metadata.get("type")
    if file_type and file_type not in VALID_FILE_TYPES:
        result.add_error(f"Invalid file type: {file_type}. Valid types: {sorted(VALID_FILE_TYPES)}")
    
    # Check type-specific required fields
    if file_type in TYPE_SPECIFIC_REQUIRED_FIELDS:
        required_fields = TYPE_SPECIFIC_REQUIRED_FIELDS[file_type]
        for field in required_fields:
            if field not in metadata:
                result.add_error(f"Missing required field for type '{file_type}': {field}")
    
    return result

def main():
    """Main validation function."""
    print("APEX YAML File Validation")
    print("=" * 50)
    
    # Find all YAML files
    yaml_files = find_yaml_files(".")
    print(f"Found {len(yaml_files)} YAML files to validate")
    print()
    
    # Validate each file
    all_results = []
    valid_count = 0
    error_count = 0
    warning_count = 0
    
    for file_path in yaml_files:
        result = validate_yaml_file(file_path)
        all_results.append(result)
        
        if result.is_valid:
            valid_count += 1
        else:
            error_count += 1
        
        warning_count += len(result.warnings)
    
    # Print results
    print("VALIDATION RESULTS")
    print("=" * 50)
    
    for result in all_results:
        if result.errors or result.warnings:
            print(f"\n📁 {result.file_path}")
            
            for error in result.errors:
                print(f"  ❌ ERROR: {error}")
            
            for warning in result.warnings:
                print(f"  ⚠️  WARNING: {warning}")
    
    # Print summary
    print(f"\nSUMMARY")
    print("=" * 50)
    print(f"Total files: {len(yaml_files)}")
    print(f"Valid files: {valid_count}")
    print(f"Files with errors: {error_count}")
    print(f"Total warnings: {warning_count}")
    
    if error_count > 0:
        print(f"\n❌ {error_count} files have validation errors")
        sys.exit(1)
    else:
        print(f"\n✅ All files passed validation!")

if __name__ == "__main__":
    main()
