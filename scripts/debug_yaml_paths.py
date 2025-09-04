#!/usr/bin/env python3
"""
Debug YAML Path Matching
========================

Debug script to see what YAML files are referenced vs found.
"""

import re
from pathlib import Path


def main():
    """Debug function."""
    print("🔍 Debug YAML Path Matching")
    print("=" * 50)
    
    # Set up paths
    apex_root = Path(".")
    demo_root = apex_root / "apex-demo"
    demo_src = demo_root / "src" / "main" / "java" / "dev" / "mars" / "apex" / "demo"
    demo_resources = demo_root / "src" / "main" / "resources"
    
    # Find referenced YAML files from Java code
    print("📋 YAML files referenced in Java code:")
    referenced_files = set()
    
    for java_file in demo_src.rglob("*.java"):
        if any(skip in str(java_file) for skip in ["Test.java", "/model/", "/util/TestUtilities.java"]):
            continue
            
        try:
            content = java_file.read_text(encoding='utf-8', errors='ignore')
            class_match = re.search(r'public class (\w+)', content)
            if not class_match:
                continue
                
            class_name = class_match.group(1)
            
            # Find YAML references
            yaml_refs = set()
            for match in re.finditer(r'loadFromClasspath\s*\(\s*["\']([^"\']+\.yaml?)["\']', content):
                yaml_path = match.group(1)
                if not yaml_path.endswith('.yaml'):
                    yaml_path += '.yaml'
                yaml_refs.add(yaml_path)
            
            if yaml_refs:
                print(f"  {class_name}: {sorted(yaml_refs)}")
                referenced_files.update(yaml_refs)
                
        except Exception:
            continue
    
    print(f"\nTotal referenced files: {len(referenced_files)}")
    print("First 10 referenced files:")
    for i, ref_file in enumerate(sorted(referenced_files)[:10]):
        print(f"  {i+1}. '{ref_file}'")
    
    # Find actual YAML files
    print(f"\n📁 YAML files found in resources:")
    found_files = set()
    
    for yaml_file in demo_resources.rglob("*.yaml"):
        relative_path = str(yaml_file.relative_to(demo_resources)).replace('\\', '/')
        found_files.add(relative_path)
    
    print(f"Total found files: {len(found_files)}")
    print("First 10 found files:")
    for i, found_file in enumerate(sorted(found_files)[:10]):
        print(f"  {i+1}. '{found_file}'")
    
    # Compare
    print(f"\n🔍 Comparison:")
    missing = referenced_files - found_files
    extra = found_files - referenced_files
    matching = referenced_files & found_files
    
    print(f"  Matching files: {len(matching)}")
    print(f"  Missing files: {len(missing)}")
    print(f"  Extra files: {len(extra)}")
    
    if missing:
        print(f"\n❌ Missing files (first 10):")
        for i, missing_file in enumerate(sorted(missing)[:10]):
            print(f"  {i+1}. '{missing_file}'")
    
    if matching:
        print(f"\n✅ Matching files (first 10):")
        for i, matching_file in enumerate(sorted(matching)[:10]):
            print(f"  {i+1}. '{matching_file}'")
    
    return 0


if __name__ == "__main__":
    exit(main())
