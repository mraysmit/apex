#!/usr/bin/env python3
"""
APEX Demo YAML File Analysis Script
==================================

This script analyzes the apex-demo module to extract comprehensive information about:
- YAML file usage patterns in demo classes
- Documentation consistency
- File organization and structure
- Missing files or inconsistencies
- Loading patterns and error handling

Usage:
    python analyze_demo_yaml_files.py [--output report.md] [--json report.json]

Author: APEX Development Team
Version: 1.0.0
"""

import os
import re
import json
import yaml
import argparse
from pathlib import Path
from typing import Dict, List, Set, Optional, Tuple
from dataclasses import dataclass, asdict
from datetime import datetime


@dataclass
class YamlFileInfo:
    """Information about a YAML file."""
    path: str
    exists: bool
    size: int
    metadata: Dict
    documentation_quality: str
    tags: List[str]


@dataclass
class DemoClassInfo:
    """Information about a demo class."""
    class_name: str
    package: str
    file_path: str
    yaml_files: List[str]
    loading_pattern: str
    error_handling: bool
    documentation: str


@dataclass
class AnalysisReport:
    """Complete analysis report."""
    timestamp: str
    total_demo_classes: int
    total_yaml_files: int
    missing_yaml_files: int
    consistency_score: float
    demo_classes: List[DemoClassInfo]
    yaml_files: List[YamlFileInfo]
    patterns: Dict[str, int]
    recommendations: List[str]


class ApexDemoAnalyzer:
    """Analyzer for APEX demo YAML file usage."""
    
    def __init__(self, apex_root: str):
        self.apex_root = Path(apex_root)
        self.demo_root = self.apex_root / "apex-demo"
        self.demo_src = self.demo_root / "src" / "main" / "java" / "dev" / "mars" / "apex" / "demo"
        self.demo_resources = self.demo_root / "src" / "main" / "resources"
        
        # Patterns for analysis
        self.yaml_load_pattern = re.compile(r'yamlLoader\.loadFromClasspath\s*\(\s*["\']([^"\']+)["\']')
        self.config_load_pattern = re.compile(r'loadFromClasspath\s*\(\s*["\']([^"\']+)["\']')
        self.class_name_pattern = re.compile(r'public class (\w+)')
        self.package_pattern = re.compile(r'package\s+([\w.]+);')
        
    def analyze(self) -> AnalysisReport:
        """Perform complete analysis of demo YAML usage."""
        print("🔍 Starting APEX Demo YAML Analysis...")
        
        # Find all demo classes
        demo_classes = self._find_demo_classes()
        print(f"📁 Found {len(demo_classes)} demo classes")
        
        # Analyze YAML files
        yaml_files = self._analyze_yaml_files()
        print(f"📄 Found {len(yaml_files)} YAML files")
        
        # Analyze patterns
        patterns = self._analyze_patterns(demo_classes)
        
        # Calculate metrics
        missing_files = sum(1 for cls in demo_classes for yaml_path in cls.yaml_files 
                          if not self._yaml_file_exists(yaml_path))
        
        consistency_score = self._calculate_consistency_score(demo_classes, yaml_files)
        
        # Generate recommendations
        recommendations = self._generate_recommendations(demo_classes, yaml_files)
        
        return AnalysisReport(
            timestamp=datetime.now().isoformat(),
            total_demo_classes=len(demo_classes),
            total_yaml_files=len(yaml_files),
            missing_yaml_files=missing_files,
            consistency_score=consistency_score,
            demo_classes=demo_classes,
            yaml_files=yaml_files,
            patterns=patterns,
            recommendations=recommendations
        )
    
    def _find_demo_classes(self) -> List[DemoClassInfo]:
        """Find and analyze all demo classes."""
        demo_classes = []
        
        for java_file in self.demo_src.rglob("*.java"):
            if self._is_demo_class(java_file):
                class_info = self._analyze_demo_class(java_file)
                if class_info:
                    demo_classes.append(class_info)
        
        return sorted(demo_classes, key=lambda x: x.class_name)
    
    def _is_demo_class(self, java_file: Path) -> bool:
        """Check if a Java file is a demo class."""
        # Skip test files, utilities, and models
        exclude_patterns = [
            "Test.java", "test/", "/model/", "/util/TestUtilities.java",
            "/infrastructure/", "/runners/"
        ]
        
        file_str = str(java_file)
        return not any(pattern in file_str for pattern in exclude_patterns)
    
    def _analyze_demo_class(self, java_file: Path) -> Optional[DemoClassInfo]:
        """Analyze a single demo class file."""
        try:
            content = java_file.read_text(encoding='utf-8')
            
            # Extract class name and package
            class_match = self.class_name_pattern.search(content)
            package_match = self.package_pattern.search(content)
            
            if not class_match:
                return None
            
            class_name = class_match.group(1)
            package = package_match.group(1) if package_match else "unknown"
            
            # Find YAML file references
            yaml_files = self._extract_yaml_references(content)
            
            # Analyze loading pattern
            loading_pattern = self._analyze_loading_pattern(content)
            
            # Check error handling
            error_handling = "try" in content and "catch" in content and "RuntimeException" in content
            
            # Extract documentation
            documentation = self._extract_class_documentation(content)
            
            return DemoClassInfo(
                class_name=class_name,
                package=package,
                file_path=str(java_file.relative_to(self.apex_root)),
                yaml_files=yaml_files,
                loading_pattern=loading_pattern,
                error_handling=error_handling,
                documentation=documentation
            )
            
        except Exception as e:
            print(f"⚠️  Error analyzing {java_file}: {e}")
            return None
    
    def _extract_yaml_references(self, content: str) -> List[str]:
        """Extract YAML file references from Java code."""
        yaml_files = []
        
        # Find loadFromClasspath calls
        for match in self.yaml_load_pattern.finditer(content):
            yaml_path = match.group(1)
            if not yaml_path.endswith('.yaml'):
                yaml_path += '.yaml'
            yaml_files.append(yaml_path)
        
        # Find other config loading patterns
        for match in self.config_load_pattern.finditer(content):
            yaml_path = match.group(1)
            if not yaml_path.endswith('.yaml'):
                yaml_path += '.yaml'
            if yaml_path not in yaml_files:
                yaml_files.append(yaml_path)
        
        return yaml_files
    
    def _analyze_loading_pattern(self, content: str) -> str:
        """Analyze the YAML loading pattern used."""
        if "loadFromClasspath" in content:
            if "YamlConfigurationLoader" in content:
                return "standard_apex_loader"
            else:
                return "custom_loader"
        elif "loadConfiguration" in content:
            return "configuration_method"
        else:
            return "unknown"
    
    def _extract_class_documentation(self, content: str) -> str:
        """Extract class-level documentation."""
        # Look for class JavaDoc
        javadoc_pattern = re.compile(r'/\*\*\s*(.*?)\s*\*/', re.DOTALL)
        matches = javadoc_pattern.findall(content)
        
        if matches:
            # Get the last JavaDoc (usually the class documentation)
            doc = matches[-1].strip()
            # Clean up the documentation
            doc = re.sub(r'\s*\*\s*', ' ', doc)
            return doc[:200] + "..." if len(doc) > 200 else doc
        
        return "No documentation found"
    
    def _analyze_yaml_files(self) -> List[YamlFileInfo]:
        """Analyze all YAML files in the resources directory."""
        yaml_files = []
        
        for yaml_file in self.demo_resources.rglob("*.yaml"):
            yaml_info = self._analyze_yaml_file(yaml_file)
            if yaml_info:
                yaml_files.append(yaml_info)
        
        return sorted(yaml_files, key=lambda x: x.path)
    
    def _analyze_yaml_file(self, yaml_file: Path) -> Optional[YamlFileInfo]:
        """Analyze a single YAML file."""
        try:
            content = yaml_file.read_text(encoding='utf-8')
            relative_path = str(yaml_file.relative_to(self.demo_resources))
            
            # Parse YAML metadata
            try:
                yaml_data = yaml.safe_load(content)
                metadata = yaml_data.get('metadata', {}) if yaml_data else {}
            except yaml.YAMLError:
                metadata = {}
            
            # Analyze documentation quality
            doc_quality = self._assess_documentation_quality(content, metadata)
            
            # Extract tags
            tags = metadata.get('tags', []) if isinstance(metadata.get('tags'), list) else []
            
            return YamlFileInfo(
                path=relative_path,
                exists=True,
                size=yaml_file.stat().st_size,
                metadata=metadata,
                documentation_quality=doc_quality,
                tags=tags
            )
            
        except Exception as e:
            print(f"⚠️  Error analyzing YAML file {yaml_file}: {e}")
            return None
    
    def _assess_documentation_quality(self, content: str, metadata: Dict) -> str:
        """Assess the quality of YAML file documentation."""
        score = 0
        
        # Check for header comments
        if content.startswith('#'):
            score += 1
        
        # Check for metadata section
        if metadata:
            score += 1
            
            # Check for required metadata fields
            required_fields = ['name', 'description', 'version', 'type']
            for field in required_fields:
                if field in metadata:
                    score += 1
        
        # Check for tags
        if metadata.get('tags'):
            score += 1
        
        # Determine quality level
        if score >= 6:
            return "excellent"
        elif score >= 4:
            return "good"
        elif score >= 2:
            return "fair"
        else:
            return "poor"
    
    def _yaml_file_exists(self, yaml_path: str) -> bool:
        """Check if a YAML file exists."""
        full_path = self.demo_resources / yaml_path
        return full_path.exists()
    
    def _analyze_patterns(self, demo_classes: List[DemoClassInfo]) -> Dict[str, int]:
        """Analyze common patterns in demo classes."""
        patterns = {
            "standard_apex_loader": 0,
            "custom_loader": 0,
            "configuration_method": 0,
            "error_handling": 0,
            "multiple_yaml_files": 0,
            "single_yaml_file": 0,
            "no_yaml_files": 0
        }
        
        for demo_class in demo_classes:
            patterns[demo_class.loading_pattern] += 1
            
            if demo_class.error_handling:
                patterns["error_handling"] += 1
            
            yaml_count = len(demo_class.yaml_files)
            if yaml_count == 0:
                patterns["no_yaml_files"] += 1
            elif yaml_count == 1:
                patterns["single_yaml_file"] += 1
            else:
                patterns["multiple_yaml_files"] += 1
        
        return patterns
    
    def _calculate_consistency_score(self, demo_classes: List[DemoClassInfo], 
                                   yaml_files: List[YamlFileInfo]) -> float:
        """Calculate overall consistency score."""
        total_points = 0
        max_points = 0
        
        # Check demo class consistency
        for demo_class in demo_classes:
            max_points += 4
            
            # Standard loading pattern
            if demo_class.loading_pattern == "standard_apex_loader":
                total_points += 1
            
            # Error handling
            if demo_class.error_handling:
                total_points += 1
            
            # Has YAML files
            if demo_class.yaml_files:
                total_points += 1
            
            # All YAML files exist
            if all(self._yaml_file_exists(yaml_path) for yaml_path in demo_class.yaml_files):
                total_points += 1
        
        # Check YAML file consistency
        for yaml_file in yaml_files:
            max_points += 2
            
            # Good documentation
            if yaml_file.documentation_quality in ["excellent", "good"]:
                total_points += 1
            
            # Has metadata
            if yaml_file.metadata:
                total_points += 1
        
        return (total_points / max_points * 100) if max_points > 0 else 0
    
    def _generate_recommendations(self, demo_classes: List[DemoClassInfo], 
                                yaml_files: List[YamlFileInfo]) -> List[str]:
        """Generate recommendations for improvement."""
        recommendations = []
        
        # Check for missing YAML files
        missing_files = []
        for demo_class in demo_classes:
            for yaml_path in demo_class.yaml_files:
                if not self._yaml_file_exists(yaml_path):
                    missing_files.append(f"{demo_class.class_name} -> {yaml_path}")
        
        if missing_files:
            recommendations.append(f"Create missing YAML files: {', '.join(missing_files)}")
        
        # Check for inconsistent loading patterns
        non_standard_loaders = [cls.class_name for cls in demo_classes 
                              if cls.loading_pattern != "standard_apex_loader"]
        if non_standard_loaders:
            recommendations.append(f"Standardize loading patterns in: {', '.join(non_standard_loaders)}")
        
        # Check for poor documentation
        poorly_documented = [yf.path for yf in yaml_files 
                           if yf.documentation_quality == "poor"]
        if poorly_documented:
            recommendations.append(f"Improve documentation in: {', '.join(poorly_documented)}")
        
        # Check for missing error handling
        no_error_handling = [cls.class_name for cls in demo_classes 
                           if not cls.error_handling and cls.yaml_files]
        if no_error_handling:
            recommendations.append(f"Add error handling to: {', '.join(no_error_handling)}")
        
        return recommendations


def generate_markdown_report(report: AnalysisReport, output_file: str):
    """Generate a markdown report."""
    with open(output_file, 'w', encoding='utf-8') as f:
        f.write(f"""# APEX Demo YAML File Analysis Report

**Generated:** {report.timestamp}
**Consistency Score:** {report.consistency_score:.1f}%

## 📊 Summary Statistics

- **Total Demo Classes:** {report.total_demo_classes}
- **Total YAML Files:** {report.total_yaml_files}
- **Missing YAML Files:** {report.missing_yaml_files}
- **Consistency Score:** {report.consistency_score:.1f}%

## 🎯 Pattern Analysis

""")
        
        for pattern, count in report.patterns.items():
            f.write(f"- **{pattern.replace('_', ' ').title()}:** {count}\n")
        
        f.write(f"""
## 📁 Demo Classes Analysis

| Class Name | Package | YAML Files | Loading Pattern | Error Handling |
|------------|---------|------------|-----------------|----------------|
""")
        
        for demo_class in report.demo_classes:
            yaml_count = len(demo_class.yaml_files)
            error_handling = "✅" if demo_class.error_handling else "❌"
            f.write(f"| {demo_class.class_name} | {demo_class.package.split('.')[-1]} | {yaml_count} | {demo_class.loading_pattern} | {error_handling} |\n")
        
        f.write(f"""
## 📄 YAML Files Analysis

| File Path | Documentation Quality | Size (bytes) | Tags |
|-----------|----------------------|--------------|------|
""")
        
        for yaml_file in report.yaml_files:
            tags = ", ".join(yaml_file.tags[:3]) if yaml_file.tags else "None"
            f.write(f"| {yaml_file.path} | {yaml_file.documentation_quality} | {yaml_file.size} | {tags} |\n")
        
        if report.recommendations:
            f.write(f"""
## 🔧 Recommendations

""")
            for i, rec in enumerate(report.recommendations, 1):
                f.write(f"{i}. {rec}\n")
        
        f.write(f"""
## ✅ Conclusion

The APEX demo module shows {'excellent' if report.consistency_score >= 90 else 'good' if report.consistency_score >= 70 else 'fair'} consistency in YAML file usage with a score of {report.consistency_score:.1f}%.
""")


def main():
    """Main entry point."""
    parser = argparse.ArgumentParser(description="Analyze APEX demo YAML file usage")
    parser.add_argument("--apex-root", default=".", help="Path to APEX root directory")
    parser.add_argument("--output", default="demo_yaml_analysis.md", help="Output markdown file")
    parser.add_argument("--json", help="Output JSON file")
    parser.add_argument("--verbose", "-v", action="store_true", help="Verbose output")
    
    args = parser.parse_args()
    
    # Verify apex-demo directory exists
    apex_root = Path(args.apex_root)
    demo_dir = apex_root / "apex-demo"
    
    if not demo_dir.exists():
        print(f"❌ Error: apex-demo directory not found at {demo_dir}")
        print("Please run this script from the APEX root directory or specify --apex-root")
        return 1
    
    # Run analysis
    analyzer = ApexDemoAnalyzer(args.apex_root)
    report = analyzer.analyze()
    
    # Generate outputs
    print(f"📝 Generating markdown report: {args.output}")
    generate_markdown_report(report, args.output)
    
    if args.json:
        print(f"📝 Generating JSON report: {args.json}")
        with open(args.json, 'w', encoding='utf-8') as f:
            json.dump(asdict(report), f, indent=2, default=str)
    
    # Print summary
    print(f"""
🎉 Analysis Complete!
📊 Consistency Score: {report.consistency_score:.1f}%
📁 Demo Classes: {report.total_demo_classes}
📄 YAML Files: {report.total_yaml_files}
❌ Missing Files: {report.missing_yaml_files}
""")
    
    return 0


if __name__ == "__main__":
    exit(main())
