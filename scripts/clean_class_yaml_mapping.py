#!/usr/bin/env python3
"""
Clean Class to YAML Mapping
===========================

A focused script to extract clean class-to-YAML mappings without duplicates.
"""

import re
from pathlib import Path


def extract_yaml_files_from_class(java_file_path):
    """Extract YAML files referenced in a Java class file."""
    try:
        content = java_file_path.read_text(encoding='utf-8', errors='ignore')
        
        # Extract class name
        class_match = re.search(r'public class (\w+)', content)
        if not class_match:
            return None, []
            
        class_name = class_match.group(1)
        
        # Find YAML file references using multiple patterns
        yaml_files = set()
        
        # Pattern 1: loadFromClasspath calls
        for match in re.finditer(r'loadFromClasspath\s*\(\s*["\']([^"\']+\.yaml?)["\']', content):
            yaml_path = match.group(1)
            if not yaml_path.endswith('.yaml'):
                yaml_path += '.yaml'
            yaml_files.add(yaml_path)
        
        # Pattern 2: Direct YAML file strings (more selective)
        for match in re.finditer(r'["\']([a-zA-Z0-9/_-]+\.yaml)["\']', content):
            yaml_path = match.group(1)
            # Only include if it looks like a config file path
            if any(keyword in yaml_path for keyword in ['config', 'demo', 'validation', 'enrichment', 'evaluation']):
                yaml_files.add(yaml_path)
        
        return class_name, sorted(list(yaml_files))
        
    except Exception as e:
        return None, []


def main():
    """Main function."""
    print("📋 APEX Demo Class to YAML File Mapping (Clean Version)")
    print("=" * 70)
    
    # Set up paths
    apex_root = Path(".")
    demo_root = apex_root / "apex-demo"
    demo_src = demo_root / "src" / "main" / "java" / "dev" / "mars" / "apex" / "demo"
    
    if not demo_root.exists():
        print(f"❌ Error: apex-demo directory not found at {demo_root}")
        return 1
    
    # Find all demo classes
    demo_mappings = []
    
    for java_file in demo_src.rglob("*.java"):
        # Skip test files, utilities, and models
        if any(skip in str(java_file) for skip in ["Test.java", "/model/", "/util/TestUtilities.java", "/infrastructure/", "/runners/"]):
            continue
            
        class_name, yaml_files = extract_yaml_files_from_class(java_file)
        
        if class_name and yaml_files:
            demo_mappings.append((class_name, yaml_files))
    
    if not demo_mappings:
        print("❌ No demo classes with YAML files found")
        return 1
    
    # Sort by class name
    demo_mappings.sort(key=lambda x: x[0])
    
    print(f"Found {len(demo_mappings)} demo classes with YAML files:\n")
    
    # Print in a clean table format
    print(f"{'Class Name':<45} | {'YAML Files'}")
    print("-" * 45 + " | " + "-" * 80)
    
    for class_name, yaml_files in demo_mappings:
        if len(yaml_files) == 1:
            # Single YAML file
            print(f"{class_name:<45} | {yaml_files[0]}")
        else:
            # Multiple YAML files
            print(f"{class_name:<45} | {yaml_files[0]}")
            for yaml_file in yaml_files[1:]:
                print(f"{'':<45} | {yaml_file}")
        print()  # Empty line between entries
    
    print(f"Total: {len(demo_mappings)} demo classes with YAML configurations")
    
    # Also create a simple CSV output
    csv_file = "class_yaml_mapping.csv"
    with open(csv_file, 'w', encoding='utf-8') as f:
        f.write("Class Name,YAML Files\n")
        for class_name, yaml_files in demo_mappings:
            yaml_files_str = "; ".join(yaml_files)
            f.write(f'"{class_name}","{yaml_files_str}"\n')
    
    print(f"\n📄 CSV output saved to: {csv_file}")
    
    return 0


if __name__ == "__main__":
    exit(main())
