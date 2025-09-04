#!/usr/bin/env python3
"""
Fixed YAML Mapping Script
=========================

Corrected version that handles path separators properly.
"""

import re
from pathlib import Path


def find_class_yaml_mappings():
    """Find all demo classes and their YAML file mappings."""
    
    # Set up paths
    demo_root = Path("apex-demo")
    demo_src = demo_root / "src" / "main" / "java" / "dev" / "mars" / "apex" / "demo"
    demo_resources = demo_root / "src" / "main" / "resources"
    
    if not demo_root.exists():
        print("❌ Error: apex-demo directory not found")
        return []
    
    print(f"🔍 Scanning demo classes in: {demo_src}")
    print(f"📁 Looking for YAML files in: {demo_resources}")
    
    # Find all existing YAML files (normalize paths to forward slashes)
    existing_yaml_files = set()
    for yaml_file in demo_resources.rglob("*.yaml"):
        # Convert Windows backslashes to forward slashes for consistency
        normalized_path = str(yaml_file.relative_to(demo_resources)).replace('\\', '/')
        existing_yaml_files.add(normalized_path)
    
    print(f"📄 Found {len(existing_yaml_files)} YAML files in resources")
    
    # Find demo classes and their YAML references
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
            
            # Only include classes that have YAML files
            if yaml_files:
                yaml_list = sorted(list(yaml_files))
                
                # Check which files exist
                existing_files = []
                missing_files = []
                
                for yaml_file in yaml_list:
                    if yaml_file in existing_yaml_files:
                        existing_files.append(yaml_file)
                    else:
                        missing_files.append(yaml_file)
                
                mappings.append({
                    'class_name': class_name,
                    'yaml_files': yaml_list,
                    'existing_files': existing_files,
                    'missing_files': missing_files
                })
                
        except Exception as e:
            print(f"⚠️  Error analyzing {java_file}: {e}")
            continue
    
    return mappings, existing_yaml_files


def main():
    """Main function."""
    print("📋 APEX Demo Class to YAML File Mapping (FIXED VERSION)")
    print("=" * 80)
    
    mappings, all_yaml_files = find_class_yaml_mappings()
    
    if not mappings:
        print("❌ No demo classes with YAML files found")
        return 1
    
    # Sort by class name
    mappings.sort(key=lambda x: x['class_name'])
    
    print(f"\n✅ Found {len(mappings)} demo classes with YAML files:")
    print(f"📄 Total YAML files in resources: {len(all_yaml_files)}")
    
    # Calculate statistics
    total_references = sum(len(m['yaml_files']) for m in mappings)
    total_existing = sum(len(m['existing_files']) for m in mappings)
    total_missing = sum(len(m['missing_files']) for m in mappings)
    
    print(f"🔗 Total YAML file references: {total_references}")
    print(f"✅ Existing references: {total_existing}")
    print(f"❌ Missing references: {total_missing}")
    
    print(f"\n{'Class Name':<45} | {'YAML Files':<80} | {'Status'}")
    print("-" * 45 + " | " + "-" * 80 + " | " + "-" * 10)
    
    for mapping in mappings:
        class_name = mapping['class_name']
        yaml_files = mapping['yaml_files']
        existing_count = len(mapping['existing_files'])
        total_count = len(yaml_files)
        
        # Show first YAML file and status
        if yaml_files:
            first_yaml = yaml_files[0]
            status = f"{existing_count}/{total_count}"
            print(f"{class_name:<45} | {first_yaml:<80} | {status}")
            
            # Show additional YAML files
            for yaml_file in yaml_files[1:]:
                exists_marker = "✅" if yaml_file in mapping['existing_files'] else "❌"
                print(f"{'':<45} | {exists_marker} {yaml_file:<77} | ")
        
        print()  # Empty line between classes
    
    # Show missing files if any
    if total_missing > 0:
        print(f"\n❌ Missing YAML Files ({total_missing} total):")
        for mapping in mappings:
            if mapping['missing_files']:
                print(f"  {mapping['class_name']}:")
                for missing_file in mapping['missing_files']:
                    print(f"    - {missing_file}")
    
    # Create CSV output
    csv_file = "fixed_class_yaml_mapping.csv"
    with open(csv_file, 'w', encoding='utf-8') as f:
        f.write("Class Name,YAML Files,Existing Count,Total Count,Status\n")
        for mapping in mappings:
            class_name = mapping['class_name']
            yaml_files_str = "; ".join(mapping['yaml_files'])
            existing_count = len(mapping['existing_files'])
            total_count = len(mapping['yaml_files'])
            status = "Complete" if existing_count == total_count else f"Missing {total_count - existing_count}"
            f.write(f'"{class_name}","{yaml_files_str}",{existing_count},{total_count},"{status}"\n')
    
    print(f"\n📄 Detailed CSV report saved to: {csv_file}")
    
    return 0


if __name__ == "__main__":
    exit(main())
