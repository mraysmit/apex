#!/usr/bin/env python3
"""
Simple Class to YAML Mapping Display
====================================

Shows a clean grid of demo classes and their YAML files.
"""

import os
import re
from pathlib import Path


def find_demo_classes_with_yaml(demo_src_dir):
    """Find demo classes and their YAML files."""
    demo_classes = []
    
    if not demo_src_dir.exists():
        print(f"X Demo source directory not found: {demo_src_dir}")
        return demo_classes
    
    for java_file in demo_src_dir.rglob("*.java"):
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
            yaml_files = []
            for match in re.finditer(r'loadFromClasspath\s*\(\s*["\']([^"\']+)["\']', content):
                yaml_path = match.group(1)
                if not yaml_path.endswith('.yaml'):
                    yaml_path += '.yaml'
                yaml_files.append(yaml_path)
            
            # Only include classes that have YAML files
            if yaml_files:
                demo_classes.append({
                    "class_name": class_name,
                    "yaml_files": yaml_files
                })
            
        except Exception as e:
            continue
    
    return demo_classes


def main():
    """Main function."""
    print("📋 APEX Demo Class to YAML File Mapping")
    print("=" * 80)
    
    # Set up paths
    apex_root = Path(".")
    demo_root = apex_root / "apex-demo"
    demo_src = demo_root / "src" / "main" / "java" / "dev" / "mars" / "apex" / "demo"
    
    # Check if apex-demo exists
    if not demo_root.exists():
        print(f"X Error: apex-demo directory not found at {demo_root}")
        return 1
    
    # Find demo classes with YAML files
    demo_classes = find_demo_classes_with_yaml(demo_src)
    
    if not demo_classes:
        print("X No demo classes with YAML files found")
        return 1
    
    print(f"Found {len(demo_classes)} demo classes with YAML files:\n")
    
    # Print header
    print(f"{'Class Name':<40} | {'YAML Files'}")
    print("-" * 40 + " | " + "-" * 50)
    
    # Print each class and its YAML files
    for demo_class in sorted(demo_classes, key=lambda x: x["class_name"]):
        class_name = demo_class["class_name"]
        yaml_files = demo_class["yaml_files"]
        
        # Print first YAML file on same line as class name
        if yaml_files:
            print(f"{class_name:<40} | {yaml_files[0]}")
            
            # Print additional YAML files indented
            for yaml_file in yaml_files[1:]:
                print(f"{'':<40} | {yaml_file}")
        
        print()  # Empty line between classes
    
    print(f"\nTotal: {len(demo_classes)} demo classes with YAML configurations")
    return 0


if __name__ == "__main__":
    exit(main())
