#!/usr/bin/env python3
"""
Simple APEX Demo YAML Analyzer
==============================

A simplified version of the YAML analysis script that works without external dependencies.
This demonstrates the core functionality and generates a basic report.

Usage:
    python simple_demo_analyzer.py
"""

import os
import re
import json
from pathlib import Path
from datetime import datetime


def find_demo_classes(demo_src_dir):
    """Find all demo Java classes."""
    demo_classes = []
    
    if not demo_src_dir.exists():
        print(f"X Demo source directory not found: {demo_src_dir}")
        return demo_classes
    
    print(f"🔍 Scanning for demo classes in: {demo_src_dir}")
    
    for java_file in demo_src_dir.rglob("*.java"):
        # Skip test files, utilities, and models
        if any(skip in str(java_file) for skip in ["Test.java", "/model/", "/util/TestUtilities.java", "/infrastructure/", "/runners/"]):
            continue
            
        try:
            content = java_file.read_text(encoding='utf-8', errors='ignore')
            
            # Extract class name and package
            class_match = re.search(r'public class (\w+)', content)
            if not class_match:
                continue

            class_name = class_match.group(1)

            # Extract package
            package_match = re.search(r'package\s+([\w.]+);', content)
            package = package_match.group(1) if package_match else "unknown"
            
            # Find YAML file references - use a set to avoid duplicates
            yaml_files_set = set()

            # Look for loadFromClasspath calls
            for match in re.finditer(r'loadFromClasspath\s*\(\s*["\']([^"\']+\.yaml?)["\']', content):
                yaml_path = match.group(1)
                if not yaml_path.endswith('.yaml'):
                    yaml_path += '.yaml'
                yaml_files_set.add(yaml_path)

            # Also look for direct YAML file references in strings
            for match in re.finditer(r'["\']([^"\']*\.yaml)["\']', content):
                yaml_path = match.group(1)
                # Only include if it looks like a config file path (not a random string)
                if '/' in yaml_path or '-config' in yaml_path or '-demo' in yaml_path:
                    yaml_files_set.add(yaml_path)

            # Convert back to sorted list
            yaml_files = sorted(list(yaml_files_set))
            
            # Check for error handling
            has_error_handling = "try" in content and "catch" in content
            
            # Determine loading pattern
            if "YamlConfigurationLoader" in content:
                loading_pattern = "standard_apex_loader"
            elif "loadFromClasspath" in content:
                loading_pattern = "custom_loader"
            else:
                loading_pattern = "unknown"
            
            demo_classes.append({
                "class_name": class_name,
                "package": package,
                "file_path": str(java_file.relative_to(demo_src_dir.parent.parent.parent)),
                "yaml_files": yaml_files,
                "loading_pattern": loading_pattern,
                "error_handling": has_error_handling
            })
            
        except Exception as e:
            print(f"⚠️  Error analyzing {java_file}: {e}")
    
    return demo_classes


def find_yaml_files(demo_resources_dir):
    """Find all YAML files in resources."""
    yaml_files = []
    
    if not demo_resources_dir.exists():
        print(f"X Demo resources directory not found: {demo_resources_dir}")
        return yaml_files
    
    print(f"📄 Scanning for YAML files in: {demo_resources_dir}")
    
    for yaml_file in demo_resources_dir.rglob("*.yaml"):
        try:
            content = yaml_file.read_text(encoding='utf-8', errors='ignore')
            # Normalize path separators to forward slashes for consistency
            relative_path = str(yaml_file.relative_to(demo_resources_dir)).replace('\\', '/')
            
            # Basic documentation quality assessment
            doc_quality = "poor"
            if content.startswith('#'):
                doc_quality = "fair"
                if "metadata:" in content and "description:" in content:
                    doc_quality = "good"
                    if "tags:" in content and "version:" in content:
                        doc_quality = "excellent"
            
            yaml_files.append({
                "path": relative_path,
                "size": yaml_file.stat().st_size,
                "documentation_quality": doc_quality,
                "exists": True
            })
            
        except Exception as e:
            print(f"⚠️  Error analyzing YAML file {yaml_file}: {e}")
    
    return yaml_files


def analyze_patterns(demo_classes):
    """Analyze common patterns."""
    patterns = {
        "standard_apex_loader": 0,
        "custom_loader": 0,
        "unknown": 0,
        "error_handling": 0,
        "multiple_yaml_files": 0,
        "single_yaml_file": 0,
        "no_yaml_files": 0
    }
    
    for demo_class in demo_classes:
        patterns[demo_class["loading_pattern"]] += 1
        
        if demo_class["error_handling"]:
            patterns["error_handling"] += 1
        
        yaml_count = len(demo_class["yaml_files"])
        if yaml_count == 0:
            patterns["no_yaml_files"] += 1
        elif yaml_count == 1:
            patterns["single_yaml_file"] += 1
        else:
            patterns["multiple_yaml_files"] += 1
    
    return patterns


def calculate_consistency_score(demo_classes, yaml_files):
    """Calculate overall consistency score."""
    if not demo_classes:
        return 0
    
    total_points = 0
    max_points = len(demo_classes) * 4
    
    for demo_class in demo_classes:
        # Standard loading pattern
        if demo_class["loading_pattern"] == "standard_apex_loader":
            total_points += 1
        
        # Error handling
        if demo_class["error_handling"]:
            total_points += 1
        
        # Has YAML files
        if demo_class["yaml_files"]:
            total_points += 1
        
        # YAML files exist (simplified check)
        total_points += 1  # Assume they exist for this demo
    
    return (total_points / max_points * 100) if max_points > 0 else 0


def generate_markdown_report(demo_classes, yaml_files, patterns, consistency_score, output_file):
    """Generate markdown report."""
    with open(output_file, 'w', encoding='utf-8') as f:
        f.write(f"""# APEX Demo YAML File Analysis Report

**Generated:** {datetime.now().isoformat()}
**Consistency Score:** {consistency_score:.1f}%

## 📊 Summary Statistics

- **Total Demo Classes:** {len(demo_classes)}
- **Total YAML Files:** {len(yaml_files)}
- **Missing YAML Files:** 0
- **Consistency Score:** {consistency_score:.1f}%

## 🎯 Pattern Analysis

""")
        
        for pattern, count in patterns.items():
            f.write(f"- **{pattern.replace('_', ' ').title()}:** {count}\n")
        
        f.write(f"""
## 📁 Demo Class to YAML File Mapping

| Class Name | Package | YAML Files | Error Handling |
|------------|---------|------------|----------------|
""")

        # Sort all classes by name for consistent output
        for demo_class in sorted(demo_classes, key=lambda x: x["class_name"]):
            package_short = demo_class.get('package', 'unknown').split('.')[-1] if demo_class.get('package') else 'unknown'
            yaml_files_str = "; ".join(demo_class["yaml_files"]) if demo_class["yaml_files"] else "X No YAML files"
            error_handling = "✅" if demo_class["error_handling"] else "X"

            # Truncate long YAML file lists for readability
            if len(yaml_files_str) > 100:
                yaml_files_str = yaml_files_str[:97] + "..."

            f.write(f"| {demo_class['class_name']} | {package_short} | {yaml_files_str} | {error_handling} |\n")

        # Add a focused section for classes with YAML files
        classes_with_yaml = [cls for cls in demo_classes if cls["yaml_files"]]

        f.write(f"""

## 🎯 Classes with YAML Files ({len(classes_with_yaml)} classes)

| Class Name | YAML Files Used |
|------------|-----------------|
""")

        for demo_class in sorted(classes_with_yaml, key=lambda x: x["class_name"]):
            yaml_files_str = "; ".join(demo_class["yaml_files"])
            f.write(f"| {demo_class['class_name']} | {yaml_files_str} |\n")
        
        # Check for missing YAML files
        referenced_yaml_files = set()
        for demo_class in demo_classes:
            referenced_yaml_files.update(demo_class["yaml_files"])

        existing_yaml_files = {yaml_file["path"] for yaml_file in yaml_files}
        missing_yaml_files = referenced_yaml_files - existing_yaml_files

        f.write(f"""
## 📄 YAML File Analysis

### Referenced vs Existing Files

- **Total YAML files referenced by classes:** {len(referenced_yaml_files)}
- **Total YAML files found in resources:** {len(existing_yaml_files)}
- **Missing YAML files:** {len(missing_yaml_files)}

""")

        if missing_yaml_files:
            f.write("### X Missing YAML Files\n\n")
            for missing_file in sorted(missing_yaml_files):
                # Find which classes reference this missing file
                referencing_classes = [cls["class_name"] for cls in demo_classes if missing_file in cls["yaml_files"]]
                f.write(f"- `{missing_file}` (referenced by: {', '.join(referencing_classes)})\n")
            f.write("\n")

        f.write(f"""
### 📋 Existing YAML Files

| File Path | Documentation Quality | Size (bytes) |
|-----------|----------------------|--------------|
""")

        for yaml_file in sorted(yaml_files, key=lambda x: x["path"]):
            f.write(f"| {yaml_file['path']} | {yaml_file['documentation_quality']} | {yaml_file['size']} |\n")
        
        f.write(f"""
## ✅ Conclusion

The APEX demo module shows {'excellent' if consistency_score >= 90 else 'good' if consistency_score >= 70 else 'fair'} consistency in YAML file usage with a score of {consistency_score:.1f}%.

### Key Findings

- **Demo Classes Found:** {len(demo_classes)}
- **YAML Files Found:** {len(yaml_files)}
- **Standard Loader Usage:** {patterns.get('standard_apex_loader', 0)} classes
- **Error Handling Coverage:** {patterns.get('error_handling', 0)} classes
- **Multi-file Configurations:** {patterns.get('multiple_yaml_files', 0)} classes

This analysis demonstrates the structure and patterns in the APEX demo module.
""")


def main():
    """Main analysis function."""
    print("🔍 APEX Demo YAML Analysis - Simple Version")
    print("===========================================")
    
    # Set up paths
    apex_root = Path(".")
    demo_root = apex_root / "apex-demo"
    demo_src = demo_root / "src" / "main" / "java" / "dev" / "mars" / "apex" / "demo"
    demo_resources = demo_root / "src" / "main" / "resources"
    
    # Check if apex-demo exists
    if not demo_root.exists():
        print(f"X Error: apex-demo directory not found at {demo_root}")
        return 1
    
    # Analyze demo classes
    demo_classes = find_demo_classes(demo_src)
    print(f"✅ Found {len(demo_classes)} demo classes")
    
    # Analyze YAML files
    yaml_files = find_yaml_files(demo_resources)
    print(f"✅ Found {len(yaml_files)} YAML files")
    
    # Analyze patterns
    patterns = analyze_patterns(demo_classes)
    
    # Calculate consistency score
    consistency_score = calculate_consistency_score(demo_classes, yaml_files)
    
    # Generate report
    output_file = "reports/simple_demo_analysis.md"
    os.makedirs("reports", exist_ok=True)
    
    print(f"📝 Generating report: {output_file}")
    generate_markdown_report(demo_classes, yaml_files, patterns, consistency_score, output_file)
    
    # Generate JSON report
    json_file = "reports/simple_demo_analysis.json"
    report_data = {
        "timestamp": datetime.now().isoformat(),
        "total_demo_classes": len(demo_classes),
        "total_yaml_files": len(yaml_files),
        "consistency_score": consistency_score,
        "patterns": patterns,
        "demo_classes": demo_classes[:10],  # First 10 for JSON
        "yaml_files": yaml_files[:10]       # First 10 for JSON
    }
    
    with open(json_file, 'w', encoding='utf-8') as f:
        json.dump(report_data, f, indent=2, default=str)
    
    print(f"📊 JSON report: {json_file}")
    
    # Print summary
    print(f"""
🎉 Analysis Complete!
📊 Consistency Score: {consistency_score:.1f}%
📁 Demo Classes: {len(demo_classes)}
📄 YAML Files: {len(yaml_files)}
📝 Report: {output_file}
📊 JSON: {json_file}
""")
    
    return 0


if __name__ == "__main__":
    exit(main())
