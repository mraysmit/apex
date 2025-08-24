#!/usr/bin/env python3
"""
Validation script for apex-demo resource reorganization.
This script validates that the migration is working correctly by:
1. Checking that new resource files exist
2. Verifying that old and new files have the same content
3. Validating that the ResourcePathResolver mappings are correct
"""

import os
import sys
from pathlib import Path

def check_file_exists(file_path):
    """Check if a file exists and return True/False"""
    return Path(file_path).exists()

def compare_files(old_path, new_path):
    """Compare content of two files and return True if identical"""
    if not check_file_exists(old_path) or not check_file_exists(new_path):
        return False
    
    try:
        with open(old_path, 'r', encoding='utf-8') as f1, open(new_path, 'r', encoding='utf-8') as f2:
            return f1.read() == f2.read()
    except Exception as e:
        print(f"Error comparing files {old_path} and {new_path}: {e}")
        return False

def validate_migration():
    """Main validation function"""
    print("=" * 80)
    print("APEX DEMO RESOURCE MIGRATION VALIDATION")
    print("=" * 80)
    
    # Define the base path
    base_path = "apex-demo/src/main/resources"
    
    # Define migration mappings (same as in ResourcePathResolver)
    migrations = {
        # Bootstrap files
        "bootstrap/custody-auto-repair-bootstrap.yaml": "demos/bootstrap/custody-auto-repair/bootstrap-config.yaml",
        
        # Lookup examples
        "examples/lookups/simple-field-lookup.yaml": "demos/patterns/lookups/simple-field-lookup.yaml",
        "examples/lookups/conditional-expression-lookup.yaml": "demos/patterns/lookups/conditional-expression-lookup.yaml",
        "examples/lookups/nested-field-lookup.yaml": "demos/patterns/lookups/nested-field-lookup.yaml",
        "examples/lookups/compound-key-lookup.yaml": "demos/patterns/lookups/compound-key-lookup.yaml",
        
        # Financial settlement
        "financial-settlement/comprehensive-settlement-enrichment.yaml": "demos/industry/financial-services/settlement/comprehensive-settlement-enrichment.yaml",
        
        # Configuration files
        "config/financial-validation-rules.yaml": "demos/fundamentals/rules/financial-validation-rules.yaml",
        
        # Demo rules
        "demo-rules/custody-auto-repair-rules.yaml": "demos/industry/financial-services/custody/custody-auto-repair-rules.yaml",
        "demo-rules/quick-start.yaml": "demos/quickstart/quick-start.yaml",

        # Demo configs
        "demo-configs/comprehensive-lookup-demo.yaml": "demos/patterns/lookups/comprehensive-lookup-demo.yaml",

        # YAML examples
        "yaml-examples/file-processing-config.yaml": "reference/syntax-examples/file-processing-config.yaml",
    }
    
    print(f"\nValidating {len(migrations)} file migrations...")
    print("-" * 60)
    
    success_count = 0
    total_count = len(migrations)
    
    for old_path, new_path in migrations.items():
        old_full_path = os.path.join(base_path, old_path)
        new_full_path = os.path.join(base_path, new_path)
        
        print(f"\nChecking: {old_path}")
        print(f"     -> {new_path}")
        
        # Check if old file exists
        old_exists = check_file_exists(old_full_path)
        new_exists = check_file_exists(new_full_path)
        
        if not old_exists:
            print(f"  ❌ OLD FILE MISSING: {old_full_path}")
            continue
            
        if not new_exists:
            print(f"  ❌ NEW FILE MISSING: {new_full_path}")
            continue
            
        # Compare file contents
        if compare_files(old_full_path, new_full_path):
            print(f"  ✅ FILES MATCH")
            success_count += 1
        else:
            print(f"  ❌ FILES DIFFER")
    
    print("\n" + "=" * 60)
    print(f"VALIDATION SUMMARY")
    print("=" * 60)
    print(f"Total migrations: {total_count}")
    print(f"Successful: {success_count}")
    print(f"Failed: {total_count - success_count}")
    
    if success_count == total_count:
        print("🎉 ALL MIGRATIONS VALIDATED SUCCESSFULLY!")
        return True
    else:
        print("❌ SOME MIGRATIONS FAILED VALIDATION")
        return False

def validate_new_structure():
    """Validate that the new directory structure exists"""
    print("\n" + "=" * 60)
    print("VALIDATING NEW DIRECTORY STRUCTURE")
    print("=" * 60)
    
    base_path = "apex-demo/src/main/resources"
    expected_dirs = [
        "demos/quickstart",
        "demos/fundamentals/rules",
        "demos/fundamentals/enrichments", 
        "demos/fundamentals/datasets",
        "demos/patterns/lookups",
        "demos/patterns/calculations",
        "demos/patterns/validations",
        "demos/industry/financial-services/settlement",
        "demos/industry/financial-services/trading",
        "demos/industry/financial-services/custody",
        "demos/bootstrap/custody-auto-repair/datasets",
        "demos/bootstrap/custody-auto-repair/sql",
        "demos/bootstrap/commodity-swap/datasets",
        "demos/bootstrap/commodity-swap/schemas",
        "demos/advanced/performance",
        "demos/advanced/integration",
        "demos/advanced/complex-scenarios",
        "reference/syntax-examples",
    ]
    
    success_count = 0
    for dir_path in expected_dirs:
        full_path = os.path.join(base_path, dir_path)
        if os.path.exists(full_path):
            print(f"  ✅ {dir_path}")
            success_count += 1
        else:
            print(f"  ❌ {dir_path}")
    
    print(f"\nDirectory structure: {success_count}/{len(expected_dirs)} directories exist")
    return success_count == len(expected_dirs)

def validate_java_classes():
    """Validate that Java classes have been updated to use ResourcePathResolver"""
    print("\n" + "=" * 60)
    print("VALIDATING JAVA CLASS UPDATES")
    print("=" * 60)
    
    java_files_to_check = [
        "apex-demo/src/main/java/dev/mars/apex/demo/examples/lookups/SimpleFieldLookupDemo.java",
        "apex-demo/src/main/java/dev/mars/apex/demo/examples/lookups/ConditionalExpressionLookupDemo.java",
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
    
    success_count = 0
    for java_file in java_files_to_check:
        if check_file_exists(java_file):
            try:
                with open(java_file, 'r', encoding='utf-8') as f:
                    content = f.read()
                    if "ResourcePathResolver.resolvePath" in content:
                        print(f"  ✅ {os.path.basename(java_file)} - Updated to use ResourcePathResolver")
                        success_count += 1
                    else:
                        print(f"  ❌ {os.path.basename(java_file)} - Not updated to use ResourcePathResolver")
            except Exception as e:
                print(f"  ❌ {os.path.basename(java_file)} - Error reading file: {e}")
        else:
            print(f"  ❌ {os.path.basename(java_file)} - File not found")
    
    print(f"\nJava class updates: {success_count}/{len(java_files_to_check)} classes updated")
    return success_count == len(java_files_to_check)

if __name__ == "__main__":
    print("Starting apex-demo resource migration validation...")
    
    # Run all validations
    migration_ok = validate_migration()
    structure_ok = validate_new_structure()
    java_ok = validate_java_classes()
    
    print("\n" + "=" * 80)
    print("FINAL VALIDATION RESULTS")
    print("=" * 80)
    print(f"File migrations: {'✅ PASS' if migration_ok else '❌ FAIL'}")
    print(f"Directory structure: {'✅ PASS' if structure_ok else '❌ FAIL'}")
    print(f"Java class updates: {'✅ PASS' if java_ok else '❌ FAIL'}")
    
    if migration_ok and structure_ok and java_ok:
        print("\n🎉 PHASE 1 MIGRATION COMPLETED SUCCESSFULLY!")
        print("✅ All resource files have been copied to new locations")
        print("✅ New directory structure is in place")
        print("✅ Java classes have been updated to use ResourcePathResolver")
        print("\nNext steps:")
        print("1. Run tests to ensure functionality is preserved")
        print("2. Update remaining Java classes")
        print("3. Update test files to use new paths")
        print("4. Remove old directory structure")
        sys.exit(0)
    else:
        print("\n❌ PHASE 1 MIGRATION HAS ISSUES")
        print("Please review the failed validations above and fix them before proceeding.")
        sys.exit(1)
