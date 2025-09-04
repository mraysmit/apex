# APEX Demo YAML Analysis Scripts

This directory contains Python scripts for analyzing YAML file usage patterns and documentation consistency in the APEX Rules Engine demo module.

## 📋 Scripts Overview

### `analyze_demo_yaml_files.py`
Comprehensive analysis script that examines:
- YAML file usage patterns in demo classes
- Documentation consistency across files
- File organization and structure
- Missing files or inconsistencies
- Loading patterns and error handling
- Generates detailed reports in Markdown and JSON formats

## 🚀 Quick Start

### Prerequisites
- Python 3.7 or higher
- APEX Rules Engine project

### Installation
```bash
# Install dependencies
pip install -r requirements.txt

# Make script executable (Linux/Mac)
chmod +x analyze_demo_yaml_files.py
```

### Basic Usage
```bash
# Run from APEX root directory
python scripts/analyze_demo_yaml_files.py

# Specify custom paths
python scripts/analyze_demo_yaml_files.py --apex-root /path/to/apex --output my_report.md

# Generate both Markdown and JSON reports
python scripts/analyze_demo_yaml_files.py --output report.md --json report.json

# Verbose output
python scripts/analyze_demo_yaml_files.py --verbose
```

## 📊 Report Contents

### Markdown Report Sections
1. **Summary Statistics**
   - Total demo classes and YAML files
   - Missing files count
   - Overall consistency score

2. **Pattern Analysis**
   - Loading pattern distribution
   - Error handling coverage
   - File organization patterns

3. **Demo Classes Analysis**
   - Class-by-class breakdown
   - YAML file associations
   - Loading patterns used
   - Error handling status

4. **YAML Files Analysis**
   - Documentation quality assessment
   - File sizes and metadata
   - Tag analysis

5. **Recommendations**
   - Specific improvement suggestions
   - Missing file identification
   - Consistency improvements

### JSON Report Structure
```json
{
  "timestamp": "2025-01-04T...",
  "total_demo_classes": 40,
  "total_yaml_files": 60,
  "missing_yaml_files": 0,
  "consistency_score": 95.5,
  "demo_classes": [...],
  "yaml_files": [...],
  "patterns": {...},
  "recommendations": [...]
}
```

## 🔍 Analysis Features

### Demo Class Analysis
- **File Discovery**: Automatically finds all demo classes
- **Pattern Recognition**: Identifies YAML loading patterns
- **Error Handling**: Checks for proper exception handling
- **Documentation**: Extracts class-level documentation
- **YAML References**: Finds all YAML file references in code

### YAML File Analysis
- **Metadata Extraction**: Parses YAML metadata sections
- **Documentation Quality**: Assesses documentation completeness
- **File Organization**: Analyzes directory structure
- **Tag Analysis**: Extracts and categorizes tags
- **Size Analysis**: Reports file sizes and complexity

### Consistency Scoring
The script calculates a consistency score based on:
- Standard loading patterns (25%)
- Error handling presence (25%)
- YAML file existence (25%)
- Documentation quality (25%)

## 📁 Output Examples

### Sample Markdown Report
```markdown
# APEX Demo YAML File Analysis Report

**Generated:** 2025-01-04T10:30:00
**Consistency Score:** 95.5%

## 📊 Summary Statistics
- **Total Demo Classes:** 40
- **Total YAML Files:** 60
- **Missing YAML Files:** 0
- **Consistency Score:** 95.5%

## 🎯 Pattern Analysis
- **Standard Apex Loader:** 38
- **Error Handling:** 40
- **Single Yaml File:** 32
- **Multiple Yaml Files:** 8
```

### Sample JSON Output
```json
{
  "timestamp": "2025-01-04T10:30:00",
  "consistency_score": 95.5,
  "demo_classes": [
    {
      "class_name": "YamlDatasetDemo",
      "package": "dev.mars.apex.demo.enrichment",
      "yaml_files": ["enrichment/yaml-dataset-demo-config.yaml"],
      "loading_pattern": "standard_apex_loader",
      "error_handling": true
    }
  ]
}
```

## 🛠️ Customization

### Adding New Analysis Features
The script is designed to be extensible. To add new analysis features:

1. **Extend Data Classes**: Add new fields to `DemoClassInfo` or `YamlFileInfo`
2. **Add Analysis Methods**: Create new methods in `ApexDemoAnalyzer`
3. **Update Scoring**: Modify `_calculate_consistency_score` method
4. **Enhance Reports**: Update `generate_markdown_report` function

### Custom Patterns
To analyze custom patterns, modify the regex patterns in `__init__`:
```python
self.custom_pattern = re.compile(r'your_pattern_here')
```

## 🔧 Troubleshooting

### Common Issues

**Script can't find apex-demo directory:**
```bash
# Ensure you're in the APEX root directory
cd /path/to/apex-rules-engine
python scripts/analyze_demo_yaml_files.py

# Or specify the path explicitly
python scripts/analyze_demo_yaml_files.py --apex-root /path/to/apex-rules-engine
```

**YAML parsing errors:**
- Check for malformed YAML files
- Ensure proper encoding (UTF-8)
- Verify YAML syntax with a validator

**Missing dependencies:**
```bash
pip install -r scripts/requirements.txt
```

### Debug Mode
For detailed debugging information:
```bash
python scripts/analyze_demo_yaml_files.py --verbose
```

## 📈 Interpreting Results

### Consistency Score Ranges
- **90-100%**: Excellent consistency
- **70-89%**: Good consistency with minor issues
- **50-69%**: Fair consistency, needs improvement
- **Below 50%**: Poor consistency, major issues

### Common Recommendations
- **Missing YAML files**: Create corresponding configuration files
- **Non-standard loaders**: Standardize to `YamlConfigurationLoader`
- **Poor documentation**: Add metadata and header comments
- **Missing error handling**: Add try-catch blocks with proper logging

## 🤝 Contributing

To contribute improvements to the analysis script:

1. Fork the repository
2. Create a feature branch
3. Add your enhancements
4. Test with the APEX demo module
5. Submit a pull request

## 📝 License

This script is part of the APEX Rules Engine project and follows the same Apache 2.0 license.
