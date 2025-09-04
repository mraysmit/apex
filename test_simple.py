#!/usr/bin/env python3
import re
from pathlib import Path

print("=== TESTING YAML MAPPING ===")

# Test 1: Check if demo directory exists
demo_root = Path("apex-demo")
print(f"Demo root exists: {demo_root.exists()}")

if demo_root.exists():
    demo_src = demo_root / "src" / "main" / "java" / "dev" / "mars" / "apex" / "demo"
    demo_resources = demo_root / "src" / "main" / "resources"
    
    print(f"Demo src exists: {demo_src.exists()}")
    print(f"Demo resources exists: {demo_resources.exists()}")
    
    # Test 2: Find one specific class
    basic_usage_file = demo_src / "validation" / "BasicUsageExamples.java"
    print(f"BasicUsageExamples.java exists: {basic_usage_file.exists()}")
    
    if basic_usage_file.exists():
        content = basic_usage_file.read_text(encoding='utf-8')
        yaml_matches = re.findall(r'loadFromClasspath\s*\(\s*["\']([^"\']+)["\']', content)
        print(f"YAML files found in BasicUsageExamples: {yaml_matches}")
        
        # Test 3: Check if referenced YAML exists
        for yaml_file in yaml_matches:
            yaml_path = demo_resources / yaml_file
            print(f"  {yaml_file} exists: {yaml_path.exists()}")
    
    # Test 4: Count total YAML files in resources
    yaml_count = len(list(demo_resources.rglob("*.yaml")))
    print(f"Total YAML files in resources: {yaml_count}")
    
    # Test 5: List first few YAML files
    print("First 5 YAML files:")
    for i, yaml_file in enumerate(demo_resources.rglob("*.yaml")):
        if i >= 5:
            break
        rel_path = yaml_file.relative_to(demo_resources)
        print(f"  {rel_path}")

print("=== TEST COMPLETE ===")
