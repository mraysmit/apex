#!/usr/bin/env python3
"""
APEX YAML File Validation Script

This script validates all YAML files in the APEX project to ensure they conform
to the APEX system's YAML format requirements based on APEX_YAML_REFERENCE.md.

Requirements:
- All YAML files must have metadata section with: name, version, description, type
- Valid types: scenario, scenario-registry, bootstrap, rule-config, dataset, enrichment, rule-chain, external-data-config
- Type-specific required fields and sections per APEX_YAML_REFERENCE.md
- Version format validation (semantic versioning)
- Top-level section validation per document type
"""

import yaml
import sys
import re
from pathlib import Path
from typing import Dict, List, Any, Optional

# APEX YAML format requirements (based on APEX_YAML_REFERENCE.md)
REQUIRED_METADATA_FIELDS = {"id", "name", "version", "description", "type"}
VALID_FILE_TYPES = {
    "scenario", "scenario-registry", "bootstrap", "rule-config",
    "dataset", "enrichment", "rule-chain", "external-data-config"
}

# Type-specific required fields (from APEX_YAML_REFERENCE.md lines 161-170)
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

# Top-level sections required per document type (from APEX_YAML_REFERENCE.md)
TYPE_REQUIRED_SECTIONS = {
    "rule-config": {"rules", "enrichments"},  # At least one required
    "enrichment": {"enrichments"},
    "dataset": {"data"},
    "scenario": {"scenario", "data-types", "rule-configurations"},  # At least one required
    "scenario-registry": {"scenarios"},
    "bootstrap": {"bootstrap", "data-sources"},  # At least one required
    "rule-chain": {"rule-chains"},
    "external-data-config": {"dataSources", "configuration"}  # At least one required
}

# Optional metadata fields for comprehensive validation
OPTIONAL_METADATA_FIELDS = {
    "author", "created-by", "created-date", "domain", "business-domain",
    "tags", "source", "owner"
}

# Semantic versioning pattern
SEMANTIC_VERSION_PATTERN = re.compile(r'^\d+\.\d+(\.\d+)?(-[a-zA-Z0-9\-\.]+)?(\+[a-zA-Z0-9\-\.]+)?$')

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
        self.document_type: Optional[str] = None

    def add_error(self, message: str):
        self.errors.append(message)
        self.is_valid = False

    def add_warning(self, message: str):
        self.warnings.append(message)

    def set_document_type(self, doc_type: str):
        self.document_type = doc_type

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
    except yaml.YAMLError:
        return None
    except Exception:
        return None

def validate_version_format(version: str, result: ValidationResult):
    """Validate semantic versioning format."""
    if not SEMANTIC_VERSION_PATTERN.match(version):
        result.add_warning(f"Version should follow semantic versioning format (e.g., 1.0.0): {version}")

def validate_metadata_fields(metadata: Dict[str, Any], result: ValidationResult):
    """Validate metadata fields comprehensively."""
    # Check required metadata fields
    for field in REQUIRED_METADATA_FIELDS:
        if field not in metadata:
            result.add_error(f"Missing required metadata field: {field}")
        else:
            value = metadata.get(field)
            if not isinstance(value, str) or not value.strip():
                result.add_error(f"Metadata field '{field}' must be a non-empty string")

    # Validate version format
    version = metadata.get("version")
    if version:
        validate_version_format(version, result)

    # Validate file type
    file_type = metadata.get("type")
    if file_type and file_type not in VALID_FILE_TYPES:
        result.add_error(f"Invalid file type: {file_type}. Valid types: {sorted(VALID_FILE_TYPES)}")

    return file_type

def validate_type_specific_fields(metadata: Dict[str, Any], file_type: str, result: ValidationResult):
    """Validate type-specific required fields."""
    if file_type in TYPE_SPECIFIC_REQUIRED_FIELDS:
        required_fields = TYPE_SPECIFIC_REQUIRED_FIELDS[file_type]
        for field in required_fields:
            if field not in metadata:
                result.add_error(f"Missing required field for type '{file_type}': {field}")
            else:
                value = metadata.get(field)
                if not isinstance(value, str) or not value.strip():
                    result.add_error(f"Type-specific field '{field}' must be a non-empty string")

def validate_top_level_sections(content: Dict[str, Any], file_type: str, result: ValidationResult):
    """Validate required top-level sections per document type."""
    if file_type not in TYPE_REQUIRED_SECTIONS:
        return

    required_sections = TYPE_REQUIRED_SECTIONS[file_type]

    # Check if at least one required section exists
    has_required_section = False
    existing_sections = []

    for section in required_sections:
        if section in content and content[section] is not None:
            has_required_section = True
            existing_sections.append(section)

    if not has_required_section:
        result.add_error(f"Document type '{file_type}' must contain at least one of these sections: {sorted(required_sections)}")
    else:
        # Validate section content is not empty
        for section in existing_sections:
            section_content = content[section]
            if isinstance(section_content, (list, dict)) and len(section_content) == 0:
                result.add_warning(f"Section '{section}' is empty - consider adding content or removing the section")

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

    # Validate metadata fields
    file_type = validate_metadata_fields(metadata, result)

    if file_type:
        result.set_document_type(file_type)

        # Validate type-specific required fields
        validate_type_specific_fields(metadata, file_type, result)

        # Validate top-level sections
        validate_top_level_sections(content, file_type, result)

    return result

def print_document_type_summary(all_results: List[ValidationResult]):
    """Print document type distribution summary."""
    type_counts = {}
    for result in all_results:
        if result.document_type:
            type_counts[result.document_type] = type_counts.get(result.document_type, 0) + 1

    if type_counts:
        print("\nDOCUMENT TYPE DISTRIBUTION")
        print("=" * 50)
        for doc_type, count in sorted(type_counts.items()):
            type_desc = {
                "bootstrap": "Demo and initialization configurations",
                "dataset": "Reference data and lookup tables",
                "enrichment": "Data enrichment configurations",
                "external-data-config": "External data source configurations",
                "rule-config": "Business rules and validation logic",
                "scenario": "End-to-end processing scenarios",
                "scenario-registry": "Scenario collection management",
                "rule-chain": "Sequential rule execution definitions"
            }.get(doc_type, "Unknown document type")
            print(f"{doc_type}: {count} files - {type_desc}")

def main():
    """Main validation function."""
    print("APEX YAML File Validation")
    print("=" * 50)
    print("Enhanced validation based on APEX_YAML_REFERENCE.md")
    print()

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
            if result.document_type:
                print(f"   📋 Type: {result.document_type}")

            for error in result.errors:
                print(f"  ❌ ERROR: {error}")

            for warning in result.warnings:
                print(f"  ⚠️  WARNING: {warning}")

    # Print document type distribution
    print_document_type_summary(all_results)

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
        if warning_count > 0:
            print(f"   ⚠️  {warning_count} warnings found (non-blocking)")

if __name__ == "__main__":
    main()
