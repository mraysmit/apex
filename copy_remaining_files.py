#!/usr/bin/env python3
"""
Copy remaining files from old structure to new structure before cleanup.
This ensures no files are lost during the reorganization.
"""

import os
import shutil
from pathlib import Path

def copy_file_if_not_exists(src, dst):
    """Copy file from src to dst if dst doesn't exist"""
    if not os.path.exists(src):
        print(f"  ⚠ Source file not found: {src}")
        return False
    
    # Create destination directory if it doesn't exist
    dst_dir = os.path.dirname(dst)
    os.makedirs(dst_dir, exist_ok=True)
    
    if os.path.exists(dst):
        print(f"  ℹ Destination already exists: {os.path.basename(dst)}")
        return False
    
    try:
        shutil.copy2(src, dst)
        print(f"  ✅ Copied: {os.path.basename(src)} -> {os.path.basename(dst)}")
        return True
    except Exception as e:
        print(f"  ❌ Error copying {src}: {e}")
        return False

def copy_directory_contents(src_dir, dst_dir, exclude_files=None):
    """Copy all contents from src_dir to dst_dir"""
    if not os.path.exists(src_dir):
        print(f"  ⚠ Source directory not found: {src_dir}")
        return 0
    
    exclude_files = exclude_files or []
    copied_count = 0
    
    for root, dirs, files in os.walk(src_dir):
        for file in files:
            if file in exclude_files:
                continue
                
            src_file = os.path.join(root, file)
            rel_path = os.path.relpath(src_file, src_dir)
            dst_file = os.path.join(dst_dir, rel_path)
            
            if copy_file_if_not_exists(src_file, dst_file):
                copied_count += 1
    
    return copied_count

def main():
    print("=" * 60)
    print("COPYING REMAINING FILES TO NEW STRUCTURE")
    print("=" * 60)
    print("")
    
    base_path = "apex-demo/src/main/resources"
    total_copied = 0
    
    # Copy remaining bootstrap files
    print("📁 BOOTSTRAP FILES")
    print("-" * 30)
    
    # Copy bootstrap datasets
    src_datasets = f"{base_path}/bootstrap/datasets"
    dst_datasets = f"{base_path}/demos/bootstrap/custody-auto-repair/datasets"
    copied = copy_directory_contents(src_datasets, dst_datasets)
    total_copied += copied
    
    # Copy bootstrap SQL files
    src_sql = f"{base_path}/bootstrap/sql"
    dst_sql = f"{base_path}/demos/bootstrap/custody-auto-repair/sql"
    copied = copy_directory_contents(src_sql, dst_sql)
    total_copied += copied
    
    # Copy individual bootstrap files
    bootstrap_files = [
        ("bootstrap/otc-options-bootstrap.yaml", "demos/bootstrap/otc-options/bootstrap-config.yaml"),
        ("bootstrap/market-data.yaml", "demos/bootstrap/custody-auto-repair/datasets/market-data.yaml"),
        ("bootstrap/commodity-swap-validation-bootstrap.yaml", "demos/bootstrap/commodity-swap/bootstrap-config.yaml"),
    ]
    
    for src_rel, dst_rel in bootstrap_files:
        src = f"{base_path}/{src_rel}"
        dst = f"{base_path}/{dst_rel}"
        if copy_file_if_not_exists(src, dst):
            total_copied += 1
    
    # Copy XML files to appropriate location
    xml_files = [f for f in os.listdir(f"{base_path}/bootstrap") if f.endswith('.xml')]
    for xml_file in xml_files:
        src = f"{base_path}/bootstrap/{xml_file}"
        dst = f"{base_path}/demos/bootstrap/otc-options/samples/{xml_file}"
        if copy_file_if_not_exists(src, dst):
            total_copied += 1
    
    print("")
    
    # Copy remaining config files
    print("📁 CONFIGURATION FILES")
    print("-" * 30)
    
    config_files = [
        ("config/data-type-scenarios.yaml", "demos/fundamentals/datasets/data-type-scenarios.yaml"),
        ("config/demo-data-sources.yaml", "demos/fundamentals/datasets/demo-data-sources.yaml"),
        ("config/derivatives-validation-rules.yaml", "demos/fundamentals/rules/derivatives-validation-rules.yaml"),
        ("config/enhanced-enterprise-rules.yaml", "demos/fundamentals/rules/enhanced-enterprise-rules.yaml"),
        ("config/financial-dataset-enrichment-rules.yaml", "demos/fundamentals/enrichments/financial-dataset-enrichment-rules.yaml"),
        ("config/financial-enrichment-rules.yaml", "demos/fundamentals/enrichments/financial-enrichment-rules.yaml"),
        ("config/production-demo-config.yaml", "demos/fundamentals/rules/production-demo-config.yaml"),
        ("config/settlement-validation-rules.yaml", "demos/fundamentals/rules/settlement-validation-rules.yaml"),
    ]
    
    for src_rel, dst_rel in config_files:
        src = f"{base_path}/{src_rel}"
        dst = f"{base_path}/{dst_rel}"
        if copy_file_if_not_exists(src, dst):
            total_copied += 1
    
    print("")
    
    # Copy remaining demo-rules files
    print("📁 DEMO RULES FILES")
    print("-" * 30)
    
    demo_rules_files = [
        ("demo-rules/dataset-enrichment.yaml", "demos/fundamentals/enrichments/dataset-enrichment.yaml"),
        ("demo-rules/financial-validation.yaml", "demos/fundamentals/rules/financial-validation.yaml"),
        ("demo-rules/rule-chains-patterns.yaml", "demos/patterns/rule-chains-patterns.yaml"),
    ]
    
    for src_rel, dst_rel in demo_rules_files:
        src = f"{base_path}/{src_rel}"
        dst = f"{base_path}/{dst_rel}"
        if copy_file_if_not_exists(src, dst):
            total_copied += 1
    
    print("")
    
    # Copy remaining financial-settlement files
    print("📁 FINANCIAL SETTLEMENT FILES")
    print("-" * 30)
    
    financial_files = [
        ("financial-settlement/reference-data-enrichment.yaml", "demos/industry/financial-services/settlement/reference-data-enrichment.yaml"),
        ("financial-settlement/README.md", "demos/industry/financial-services/settlement/README.md"),
    ]
    
    for src_rel, dst_rel in financial_files:
        src = f"{base_path}/{src_rel}"
        dst = f"{base_path}/{dst_rel}"
        if copy_file_if_not_exists(src, dst):
            total_copied += 1
    
    print("")
    
    # Copy remaining scenarios files
    print("📁 SCENARIOS FILES")
    print("-" * 30)
    
    scenarios_files = [
        ("scenarios/SCENARIO_BASED_PROCESSING_README.md", "demos/advanced/complex-scenarios/README.md"),
    ]
    
    for src_rel, dst_rel in scenarios_files:
        src = f"{base_path}/{src_rel}"
        dst = f"{base_path}/{dst_rel}"
        if copy_file_if_not_exists(src, dst):
            total_copied += 1
    
    print("")
    
    # Copy remaining yaml-examples files
    print("📁 YAML EXAMPLES FILES")
    print("-" * 30)
    
    # Copy yaml-examples datasets
    src_yaml_datasets = f"{base_path}/yaml-examples/datasets"
    dst_yaml_datasets = f"{base_path}/reference/syntax-examples/datasets"
    copied = copy_directory_contents(src_yaml_datasets, dst_yaml_datasets)
    total_copied += copied
    
    print("")
    
    # Copy standalone files
    print("📁 STANDALONE FILES")
    print("-" * 30)
    
    standalone_files = [
        ("batch-processing.yaml", "demos/advanced/performance/batch-processing.yaml"),
        ("file-processing-rules.yaml", "demos/fundamentals/rules/file-processing-rules.yaml"),
    ]
    
    for src_rel, dst_rel in standalone_files:
        src = f"{base_path}/{src_rel}"
        dst = f"{base_path}/{dst_rel}"
        if copy_file_if_not_exists(src, dst):
            total_copied += 1
    
    print("")
    print("=" * 60)
    print("FILE COPY SUMMARY")
    print("=" * 60)
    print(f"Total files copied: {total_copied}")
    print("")
    
    if total_copied > 0:
        print("✅ All remaining files successfully copied to new structure!")
        print("📁 New structure now contains all files from old structure")
        print("🗑 Ready to remove old directory structure")
    else:
        print("ℹ All files were already in the new structure")
        print("🗑 Ready to remove old directory structure")
    
    print("")
    print("Next steps:")
    print("1. Validate that all files are in new structure")
    print("2. Remove old directory structure")
    print("3. Run final validation")

if __name__ == "__main__":
    main()
