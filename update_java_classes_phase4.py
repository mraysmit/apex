#!/usr/bin/env python3
"""
Phase 4 Java class update script.
This script updates all Java classes to use new paths directly,
removing ResourcePathResolver dependencies.
"""

import os
import re
from pathlib import Path

def update_java_file(file_path, path_mappings):
    """Update a Java file to use new paths directly"""
    if not os.path.exists(file_path):
        print(f"⚠ File not found: {file_path}")
        return False
    
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        original_content = content
        updated = False
        
        # Remove ResourcePathResolver import
        import_pattern = r'import dev\.mars\.apex\.demo\.support\.util\.ResourcePathResolver;\s*\n'
        if re.search(import_pattern, content):
            content = re.sub(import_pattern, '', content)
            updated = True
            print(f"  📝 Removed ResourcePathResolver import")
        
        # Update ResourcePathResolver.resolvePath() calls
        for old_path, new_path in path_mappings.items():
            # Pattern to match ResourcePathResolver.resolvePath("old-path")
            pattern = rf'ResourcePathResolver\.resolvePath\("{re.escape(old_path)}"\)'
            replacement = f'"{new_path}"'
            
            if re.search(pattern, content):
                content = re.sub(pattern, replacement, content)
                updated = True
                print(f"  🔄 Updated path: {old_path} -> {new_path}")
        
        # Handle cases where ResourcePathResolver is used with variables or concatenation
        # Look for any remaining ResourcePathResolver usage
        remaining_usage = re.search(r'ResourcePathResolver\.[a-zA-Z]+\(', content)
        if remaining_usage:
            print(f"  ⚠ Manual review needed: Found remaining ResourcePathResolver usage")
            print(f"    Context: {remaining_usage.group()}")
        
        if updated:
            with open(file_path, 'w', encoding='utf-8') as f:
                f.write(content)
            print(f"  ✅ Updated successfully")
            return True
        else:
            print(f"  ℹ No changes needed")
            return False
            
    except Exception as e:
        print(f"  ❌ Error updating file: {e}")
        return False

def main():
    print("=" * 60)
    print("PHASE 4: UPDATING JAVA CLASSES TO USE NEW PATHS DIRECTLY")
    print("=" * 60)
    print("")
    
    # Path mappings from old to new
    path_mappings = {
        # Bootstrap files
        "bootstrap/custody-auto-repair-bootstrap.yaml": "demos/bootstrap/custody-auto-repair/bootstrap-config.yaml",
        "bootstrap/otc-options-bootstrap.yaml": "demos/bootstrap/otc-options/bootstrap-config.yaml",
        "bootstrap/datasets/market-data.yaml": "demos/bootstrap/custody-auto-repair/datasets/market-data.yaml",
        
        # Lookup examples
        "examples/lookups/simple-field-lookup.yaml": "demos/patterns/lookups/simple-field-lookup.yaml",
        "examples/lookups/conditional-expression-lookup.yaml": "demos/patterns/lookups/conditional-expression-lookup.yaml",
        "examples/lookups/nested-field-lookup.yaml": "demos/patterns/lookups/nested-field-lookup.yaml",
        "examples/lookups/compound-key-lookup.yaml": "demos/patterns/lookups/compound-key-lookup.yaml",
        "examples/lookups/hierarchical-lookup.yaml": "demos/patterns/lookups/hierarchical-lookup.yaml",
        
        # Financial settlement
        "financial-settlement/comprehensive-settlement-enrichment.yaml": "demos/industry/financial-services/settlement/comprehensive-settlement-enrichment.yaml",
        
        # Configuration files
        "config/financial-validation-rules.yaml": "demos/fundamentals/rules/financial-validation-rules.yaml",
        "config/data-type-scenarios.yaml": "demos/fundamentals/datasets/data-type-scenarios.yaml",
        
        # Demo rules
        "demo-rules/custody-auto-repair-rules.yaml": "demos/industry/financial-services/custody/custody-auto-repair-rules.yaml",
        "demo-rules/quick-start.yaml": "demos/quickstart/quick-start.yaml",
        
        # Demo configs
        "demo-configs/comprehensive-lookup-demo.yaml": "demos/patterns/lookups/comprehensive-lookup-demo.yaml",
        
        # Scenarios
        "scenarios/otc-options-scenario.yaml": "demos/advanced/complex-scenarios/otc-options-scenario.yaml",
        "scenarios/commodity-swaps-scenario.yaml": "demos/advanced/complex-scenarios/commodity-swaps-scenario.yaml",
        "scenarios/settlement-auto-repair-scenario.yaml": "demos/advanced/complex-scenarios/settlement-auto-repair-scenario.yaml",
        
        # YAML examples
        "yaml-examples/file-processing-config.yaml": "reference/syntax-examples/file-processing-config.yaml",
    }
    
    # Java files to update
    java_files = [
        "apex-demo/src/main/java/dev/mars/apex/demo/examples/lookups/NestedFieldLookupDemo.java",
        "apex-demo/src/main/java/dev/mars/apex/demo/examples/lookups/CompoundKeyLookupDemo.java",
        "apex-demo/src/main/java/dev/mars/apex/demo/financial/ComprehensiveFinancialSettlementDemo.java",
        "apex-demo/src/main/java/dev/mars/apex/demo/bootstrap/CustodyAutoRepairBootstrap.java",
        "apex-demo/src/main/java/dev/mars/apex/demo/lookup/ComprehensiveLookupDemo.java",
        "apex-demo/src/main/java/dev/mars/apex/demo/examples/YamlConfiguredFileProcessingDemo.java",
        "apex-demo/src/main/java/dev/mars/apex/demo/examples/CustodyAutoRepairYamlDemo.java",
        "apex-demo/src/main/java/dev/mars/apex/demo/QuickStartDemo.java",
        "apex-demo/src/test/java/dev/mars/apex/demo/YamlFilesCoverageAnalysisTest.java",
        "apex-demo/src/test/java/dev/mars/apex/demo/examples/CustodyAutoRepairYamlTest.java",
    ]
    
    updated_count = 0
    total_count = len(java_files)
    
    for java_file in java_files:
        print(f"📁 Processing: {os.path.basename(java_file)}")
        if update_java_file(java_file, path_mappings):
            updated_count += 1
        print("")
    
    print("=" * 60)
    print("JAVA CLASS UPDATE SUMMARY")
    print("=" * 60)
    print(f"Total files processed: {total_count}")
    print(f"Files updated: {updated_count}")
    print(f"Files unchanged: {total_count - updated_count}")
    print("")
    
    if updated_count > 0:
        print("✅ Java classes successfully updated to use new paths directly!")
        print("🚀 Performance improvement: No more path resolution overhead")
        print("🧹 Cleaner code: Direct path usage, no indirection")
    else:
        print("ℹ All Java classes were already using direct paths")
    
    print("")
    print("Next steps:")
    print("1. Test the updated classes")
    print("2. Remove old directory structure")
    print("3. Run final validation")

if __name__ == "__main__":
    main()
