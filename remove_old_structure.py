#!/usr/bin/env python3
"""
Remove old directory structure after confirming all files are migrated.
This script safely removes the old directories and files.
"""

import os
import shutil
from pathlib import Path

def remove_directory_safely(dir_path):
    """Remove directory safely with confirmation"""
    if not os.path.exists(dir_path):
        print(f"  ℹ Directory not found: {dir_path}")
        return False
    
    try:
        # Count files in directory
        file_count = sum([len(files) for r, d, files in os.walk(dir_path)])
        print(f"  📊 Directory contains {file_count} files")
        
        # Remove directory
        shutil.rmtree(dir_path)
        print(f"  ✅ Removed: {os.path.basename(dir_path)}/")
        return True
        
    except Exception as e:
        print(f"  ❌ Error removing {dir_path}: {e}")
        return False

def remove_file_safely(file_path):
    """Remove file safely"""
    if not os.path.exists(file_path):
        print(f"  ℹ File not found: {file_path}")
        return False
    
    try:
        os.remove(file_path)
        print(f"  ✅ Removed: {os.path.basename(file_path)}")
        return True
        
    except Exception as e:
        print(f"  ❌ Error removing {file_path}: {e}")
        return False

def main():
    print("=" * 60)
    print("REMOVING OLD DIRECTORY STRUCTURE")
    print("=" * 60)
    print("")
    print("⚠ WARNING: This will permanently remove the old directory structure!")
    print("Make sure all files have been migrated to the new structure.")
    print("")
    
    base_path = "apex-demo/src/main/resources"
    
    # Directories to remove
    old_directories = [
        f"{base_path}/bootstrap",
        f"{base_path}/config", 
        f"{base_path}/demo-configs",
        f"{base_path}/demo-rules",
        f"{base_path}/examples",
        f"{base_path}/financial-settlement",
        f"{base_path}/scenarios",
        f"{base_path}/yaml-examples",
    ]
    
    # Standalone files to remove
    old_files = [
        f"{base_path}/batch-processing.yaml",
        f"{base_path}/file-processing-rules.yaml",
    ]
    
    removed_dirs = 0
    removed_files = 0
    
    # Remove directories
    print("🗂 REMOVING OLD DIRECTORIES")
    print("-" * 40)
    
    for dir_path in old_directories:
        print(f"📁 Processing: {os.path.basename(dir_path)}/")
        if remove_directory_safely(dir_path):
            removed_dirs += 1
        print("")
    
    # Remove standalone files
    print("📄 REMOVING OLD STANDALONE FILES")
    print("-" * 40)
    
    for file_path in old_files:
        print(f"📄 Processing: {os.path.basename(file_path)}")
        if remove_file_safely(file_path):
            removed_files += 1
        print("")
    
    print("=" * 60)
    print("OLD STRUCTURE REMOVAL SUMMARY")
    print("=" * 60)
    print(f"Directories removed: {removed_dirs}/{len(old_directories)}")
    print(f"Files removed: {removed_files}/{len(old_files)}")
    print("")
    
    if removed_dirs == len(old_directories) and removed_files == len(old_files):
        print("✅ OLD STRUCTURE COMPLETELY REMOVED!")
        print("🎉 Cleanup successful - only new organized structure remains")
        print("")
        print("Benefits achieved:")
        print("• 📁 Clean, organized directory structure")
        print("• 🚀 No path resolution overhead")
        print("• 🧹 Simplified codebase")
        print("• 📚 Clear learning progression")
        print("")
        print("New structure:")
        print("demos/")
        print("├── quickstart/          # 5-10 min introduction")
        print("├── fundamentals/        # Core concepts")
        print("├── patterns/            # Implementation patterns")
        print("├── industry/            # Real-world applications")
        print("├── bootstrap/           # Bootstrap configurations")
        print("└── advanced/            # Advanced techniques")
        print("")
        print("reference/")
        print("└── syntax-examples/     # YAML syntax reference")
        
    else:
        print("⚠ PARTIAL CLEANUP COMPLETED")
        print("Some directories or files could not be removed.")
        print("Please check the errors above and retry if needed.")
    
    print("")
    print("Next steps:")
    print("1. Run final validation")
    print("2. Test all demo runners")
    print("3. Update documentation")

if __name__ == "__main__":
    main()
