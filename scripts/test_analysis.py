#!/usr/bin/env python3
"""
Test script for APEX Demo YAML Analysis
=======================================

This script demonstrates how the analysis would work and provides
sample output for testing purposes.

Usage:
    python test_analysis.py
"""

import json
from datetime import datetime
from pathlib import Path


def create_sample_report():
    """Create a sample analysis report to demonstrate the output format."""
    
    sample_report = {
        "timestamp": datetime.now().isoformat(),
        "total_demo_classes": 42,
        "total_yaml_files": 65,
        "missing_yaml_files": 0,
        "consistency_score": 95.2,
        
        "demo_classes": [
            {
                "class_name": "YamlDatasetDemo",
                "package": "dev.mars.apex.demo.enrichment",
                "file_path": "apex-demo/src/main/java/dev/mars/apex/demo/enrichment/YamlDatasetDemo.java",
                "yaml_files": ["enrichment/yaml-dataset-demo-config.yaml"],
                "loading_pattern": "standard_apex_loader",
                "error_handling": True,
                "documentation": "YAML Dataset Demo Configuration Real APEX YAML configuration for YamlDatasetDemo.java NO HARDCODED SIMULATION - Uses authentic APEX services"
            },
            {
                "class_name": "SimplifiedAPIDemo",
                "package": "dev.mars.apex.demo.evaluation",
                "file_path": "apex-demo/src/main/java/dev/mars/apex/demo/evaluation/SimplifiedAPIDemo.java",
                "yaml_files": ["evaluation/simplified-api-demo-config.yaml"],
                "loading_pattern": "standard_apex_loader",
                "error_handling": True,
                "documentation": "Simplified API Demo Configuration Real APEX YAML configuration for SimplifiedAPIDemo.java"
            },
            {
                "class_name": "DynamicMethodExecutionDemo",
                "package": "dev.mars.apex.demo.evaluation",
                "file_path": "apex-demo/src/main/java/dev/mars/apex/demo/evaluation/DynamicMethodExecutionDemo.java",
                "yaml_files": [
                    "evaluation/dynamic-method-execution-demo.yaml",
                    "evaluation/dynamic-execution/settlement-processing-config.yaml",
                    "evaluation/dynamic-execution/risk-management-config.yaml",
                    "evaluation/dynamic-execution/dynamic-test-data.yaml"
                ],
                "loading_pattern": "standard_apex_loader",
                "error_handling": True,
                "documentation": "Dynamic Method Execution Demo with real APEX services"
            },
            {
                "class_name": "H2CustomParametersDemo",
                "package": "dev.mars.apex.demo.lookup",
                "file_path": "apex-demo/src/main/java/dev/mars/apex/demo/lookup/H2CustomParametersDemo.java",
                "yaml_files": ["lookup/h2-custom-parameters-demo.yaml"],
                "loading_pattern": "standard_apex_loader",
                "error_handling": True,
                "documentation": "Demonstrates the enhanced H2 parameter support in APEX"
            }
        ],
        
        "yaml_files": [
            {
                "path": "enrichment/yaml-dataset-demo-config.yaml",
                "exists": True,
                "size": 8542,
                "metadata": {
                    "id": "YAML Dataset Demo Configuration",
                    "name": "YAML Dataset Demo Configuration",
                    "version": "2.0.0",
                    "description": "Real APEX YAML dataset demonstration with inline datasets, field mappings, and conditional processing",
                    "type": "rule-config",
                    "author": "apex.demo.team@company.com",
                    "tags": ["apex-demo", "enrichment", "yaml-dataset"]
                },
                "documentation_quality": "excellent",
                "tags": ["apex-demo", "enrichment", "yaml-dataset"]
            },
            {
                "path": "evaluation/simplified-api-demo-config.yaml",
                "exists": True,
                "size": 12750,
                "metadata": {
                    "id": "Simplified API Demo Configuration",
                    "name": "Simplified API Demo Configuration",
                    "version": "2.0.0",
                    "description": "Real APEX simplified API demonstration with ultra-simple processing",
                    "type": "rule-config",
                    "author": "apex.demo.team@company.com",
                    "tags": ["apex-demo", "evaluation", "simplified-api"]
                },
                "documentation_quality": "excellent",
                "tags": ["apex-demo", "evaluation", "simplified-api"]
            },
            {
                "path": "lookup/h2-custom-parameters-demo.yaml",
                "exists": True,
                "size": 4256,
                "metadata": {
                    "name": "H2 Custom Parameters Demo",
                    "version": "1.0.0",
                    "description": "Demonstrates H2 database with custom parameters support",
                    "type": "external-data-config"
                },
                "documentation_quality": "excellent",
                "tags": ["h2", "custom-parameters", "demo"]
            }
        ],
        
        "patterns": {
            "standard_apex_loader": 40,
            "custom_loader": 1,
            "configuration_method": 1,
            "error_handling": 41,
            "multiple_yaml_files": 8,
            "single_yaml_file": 34,
            "no_yaml_files": 0
        },
        
        "recommendations": [
            "Standardize loading patterns in: CustomLoaderDemo",
            "Add error handling to: LegacyDemo"
        ]
    }
    
    return sample_report


def generate_sample_markdown_report(report, output_file):
    """Generate a sample markdown report."""
    with open(output_file, 'w', encoding='utf-8') as f:
        f.write(f"""# APEX Demo YAML File Analysis Report

**Generated:** {report['timestamp']}
**Consistency Score:** {report['consistency_score']:.1f}%

## 📊 Summary Statistics

- **Total Demo Classes:** {report['total_demo_classes']}
- **Total YAML Files:** {report['total_yaml_files']}
- **Missing YAML Files:** {report['missing_yaml_files']}
- **Consistency Score:** {report['consistency_score']:.1f}%

## 🎯 Pattern Analysis

""")
        
        for pattern, count in report['patterns'].items():
            f.write(f"- **{pattern.replace('_', ' ').title()}:** {count}\n")
        
        f.write(f"""
## 📁 Demo Classes Analysis

| Class Name | Package | YAML Files | Loading Pattern | Error Handling |
|------------|---------|------------|-----------------|----------------|
""")
        
        for demo_class in report['demo_classes']:
            yaml_count = len(demo_class['yaml_files'])
            error_handling = "✅" if demo_class['error_handling'] else "❌"
            package_short = demo_class['package'].split('.')[-1]
            f.write(f"| {demo_class['class_name']} | {package_short} | {yaml_count} | {demo_class['loading_pattern']} | {error_handling} |\n")
        
        f.write(f"""
## 📄 YAML Files Analysis

| File Path | Documentation Quality | Size (bytes) | Tags |
|-----------|----------------------|--------------|------|
""")
        
        for yaml_file in report['yaml_files']:
            tags = ", ".join(yaml_file['tags'][:3]) if yaml_file['tags'] else "None"
            f.write(f"| {yaml_file['path']} | {yaml_file['documentation_quality']} | {yaml_file['size']} | {tags} |\n")
        
        if report['recommendations']:
            f.write(f"""
## 🔧 Recommendations

""")
            for i, rec in enumerate(report['recommendations'], 1):
                f.write(f"{i}. {rec}\n")
        
        f.write(f"""
## ✅ Conclusion

The APEX demo module shows {'excellent' if report['consistency_score'] >= 90 else 'good' if report['consistency_score'] >= 70 else 'fair'} consistency in YAML file usage with a score of {report['consistency_score']:.1f}%.

### Key Findings

- **Perfect Coverage**: All demo classes have corresponding YAML files
- **Consistent Patterns**: 95% of demos use the standard APEX loader pattern
- **Excellent Documentation**: All YAML files have comprehensive metadata
- **Robust Error Handling**: All demos implement proper exception handling
- **Organized Structure**: Files are logically organized by functional category

### Highlights

- **Enhanced H2 Support**: New custom parameter support demonstrated
- **Multi-file Configurations**: Complex demos support hierarchical configs
- **External References**: Advanced data-source reference patterns
- **Real APEX Integration**: No hardcoded simulations, all use real services

This analysis confirms that the APEX demo module exemplifies best practices for enterprise configuration management.
""")


def main():
    """Main test function."""
    print("🧪 APEX Demo YAML Analysis - Test Mode")
    print("=====================================")
    
    # Create sample report
    print("📊 Generating sample analysis report...")
    report = create_sample_report()
    
    # Create output directory
    output_dir = Path("reports")
    output_dir.mkdir(exist_ok=True)
    
    # Generate sample outputs
    markdown_file = output_dir / "sample_demo_yaml_analysis.md"
    json_file = output_dir / "sample_demo_yaml_analysis.json"
    
    print(f"📝 Writing sample markdown report: {markdown_file}")
    generate_sample_markdown_report(report, markdown_file)
    
    print(f"📊 Writing sample JSON report: {json_file}")
    with open(json_file, 'w', encoding='utf-8') as f:
        json.dump(report, f, indent=2, default=str)
    
    print("\n✅ Sample reports generated successfully!")
    print(f"📁 Reports directory: {output_dir.absolute()}")
    print(f"📝 Markdown: {markdown_file}")
    print(f"📊 JSON: {json_file}")
    
    print("\n💡 To run the actual analysis:")
    print("1. Install Python 3.7+ and dependencies: pip install -r requirements.txt")
    print("2. Run: python analyze_demo_yaml_files.py")
    print("3. Or use the convenience script: ./run_analysis.sh (Linux/Mac) or run_analysis.bat (Windows)")
    
    return 0


if __name__ == "__main__":
    exit(main())
