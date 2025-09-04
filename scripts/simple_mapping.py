#!/usr/bin/env python3
"""
Simple Class to YAML Mapping
============================

Just show the mapping without complex analysis.
"""

import re
from pathlib import Path


def main():
    """Main function."""
    print("📋 APEX Demo Class to YAML File Mapping")
    print("=" * 80)
    
    # Set up paths
    apex_root = Path(".")
    demo_root = apex_root / "apex-demo"
    demo_src = demo_root / "src" / "main" / "java" / "dev" / "mars" / "apex" / "demo"
    demo_resources = demo_root / "src" / "main" / "resources"
    
    if not demo_root.exists():
        print(f"❌ Error: apex-demo directory not found")
        return 1
    
    # Find demo classes and their YAML files
    mappings = []
    
    for java_file in demo_src.rglob("*.java"):
        # Skip test files, utilities, and models
        if any(skip in str(java_file) for skip in ["Test.java", "/model/", "/util/TestUtilities.java", "/infrastructure/", "/runners/"]):
            continue
            
        try:
            content = java_file.read_text(encoding='utf-8', errors='ignore')
            
            # Extract class name
            class_match = re.search(r'public class (\w+)', content)
            if not class_match:
                continue
                
            class_name = class_match.group(1)
            
            # Find YAML file references
            yaml_files = set()
            for match in re.finditer(r'loadFromClasspath\s*\(\s*["\']([^"\']+\.yaml?)["\']', content):
                yaml_path = match.group(1)
                if not yaml_path.endswith('.yaml'):
                    yaml_path += '.yaml'
                yaml_files.add(yaml_path)
            
            # Only include classes that have YAML files
            if yaml_files:
                yaml_list = sorted(list(yaml_files))
                mappings.append((class_name, yaml_list))
                
        except Exception:
            continue
    
    # Sort by class name
    mappings.sort()
    
    print(f"Found {len(mappings)} demo classes with YAML files:\n")
    
    # Print the mapping
    for class_name, yaml_files in mappings:
        print(f"**{class_name}**")
        for yaml_file in yaml_files:
            # Check if file exists
            full_path = demo_resources / yaml_file
            exists = "✅" if full_path.exists() else "❌"
            print(f"  {exists} {yaml_file}")
        print()
    
    # Summary
    total_yaml_refs = sum(len(yaml_files) for _, yaml_files in mappings)
    existing_count = 0
    
    for _, yaml_files in mappings:
        for yaml_file in yaml_files:
            full_path = demo_resources / yaml_file
            if full_path.exists():
                existing_count += 1
    
    print(f"📊 Summary:")
    print(f"  Demo classes with YAML: {len(mappings)}")
    print(f"  Total YAML references: {total_yaml_refs}")
    print(f"  Existing YAML files: {existing_count}")
    print(f"  Missing YAML files: {total_yaml_refs - existing_count}")
    
    return 0


if __name__ == "__main__":
    exit(main())
