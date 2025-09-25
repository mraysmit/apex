#!/usr/bin/env python3
"""
Definitive APEX Keywords Analysis

This script creates the definitive list of APEX keywords based on actual
@JsonProperty annotations found in APEX core classes.
"""

def get_definitive_apex_keywords():
    """
    Definitive list of APEX keywords based on @JsonProperty annotations
    found in APEX core classes.
    """
    return {
        # Top-level configuration sections
        'metadata',
        'data-sources', 
        'data-source-refs',
        'rule-refs',
        'data-sinks',
        'categories',
        'rules',
        'rule-groups', 
        'enrichments',
        'transformations',
        
        # Metadata properties (ConfigurationMetadata class)
        'id',
        'name', 
        'version',
        'description',
        'type',
        'author',
        'created',
        'last-modified',
        'tags',
        
        # Rule properties (YamlRule class)
        'category',
        'categories',
        'condition',
        'message',
        'priority',
        'severity',
        'enabled',
        'validation',
        'created-by',
        'business-domain',
        'business-owner',
        'source-system',
        'effective-date',
        'expiration-date',
        'custom-properties',
        
        # Rule validation config
        'required-fields',
        'field-types',
        'custom-validators',
        
        # Enrichment properties (YamlEnrichment class)
        'target-type',
        'field-mappings',
        'conditional-mappings',
        'target-field',
        'mapping-rules',
        'execution-settings',
        'lookup-config',
        'calculation-config',
        
        # Field mapping properties (FieldMapping class)
        'source-field',
        'transformation',
        'default-value',
        'required',
        
        # Transformation properties (YamlTransformation class)
        'transformation-rules',
        
        # Data source properties (YamlDataSource class)
        'source-type',
        'implementation',
        'connection',
        'cache',
        'health-check',
        'authentication',
        'queries',
        'operations',
        'endpoints',
        'topics',
        'key-patterns',
        'file-format',
        'circuit-breaker',
        'response-mapping',
        'parameter-names',
        
        # Connection properties (from YamlDataSource)
        'bootstrap-servers',
        'security-protocol',
        'sasl-mechanism',
        'base-path',
        'file-pattern',
        'polling-interval',
        'encoding',
        'connection-pool',
    }

def get_documented_keywords():
    """Load documented keywords from APEX_YAML_REFERENCE.md"""
    import re
    
    try:
        with open("docs/APEX_YAML_REFERENCE.md", 'r', encoding='utf-8') as f:
            content = f.read()
        
        documented_keywords = set()
        
        # Extract from YAML code blocks
        yaml_blocks = re.findall(r'```yaml\n(.*?)\n```', content, re.DOTALL)
        for block in yaml_blocks:
            properties = re.findall(r'^\s*([a-zA-Z][a-zA-Z0-9_-]*):(?:\s|$)', block, re.MULTILINE)
            documented_keywords.update(properties)
        
        # Extract from table headers
        table_keywords = re.findall(r'\|\s*`([a-zA-Z][a-zA-Z0-9_-]*)`\s*\|', content)
        documented_keywords.update(table_keywords)
        
        return documented_keywords
        
    except FileNotFoundError:
        print("Error: docs/APEX_YAML_REFERENCE.md not found")
        return set()

def main():
    print("🎯 Definitive APEX Keywords Analysis")
    print("=" * 50)
    
    # Get definitive APEX keywords from core classes
    apex_keywords = get_definitive_apex_keywords()
    print(f"📊 Definitive APEX keywords (from @JsonProperty): {len(apex_keywords)}")
    
    # Get documented keywords
    documented_keywords = get_documented_keywords()
    print(f"📖 Documented keywords: {len(documented_keywords)}")
    
    # Find gaps
    missing_keywords = apex_keywords - documented_keywords
    documented_but_not_core = documented_keywords - apex_keywords
    well_documented = apex_keywords & documented_keywords
    
    print(f"\n🚨 Missing from documentation: {len(missing_keywords)}")
    print(f"⚠️  Documented but not in core: {len(documented_but_not_core)}")
    print(f"✅ Well documented: {len(well_documented)}")
    
    # Categorize missing keywords
    critical_missing = []
    important_missing = []
    other_missing = []
    
    for keyword in sorted(missing_keywords):
        if keyword in ['default-value', 'calculation-config', 'field-mappings', 'source-field', 'target-field']:
            critical_missing.append(keyword)
        elif keyword in ['conditional-mappings', 'mapping-rules', 'execution-settings', 'transformation-rules',
                        'required-fields', 'field-types', 'custom-validators', 'parameter-names']:
            important_missing.append(keyword)
        else:
            other_missing.append(keyword)
    
    print(f"\n🔥 CRITICAL Missing Keywords ({len(critical_missing)}):")
    for keyword in critical_missing:
        print(f"  ❌ {keyword}")
    
    print(f"\n⚠️  IMPORTANT Missing Keywords ({len(important_missing)}):")
    for keyword in important_missing:
        print(f"  - {keyword}")
    
    print(f"\n📝 OTHER Missing Keywords ({len(other_missing)}):")
    for keyword in other_missing:
        print(f"  - {keyword}")
    
    print(f"\n✅ Well Documented Keywords ({len(well_documented)}):")
    for keyword in sorted(well_documented):
        print(f"  ✓ {keyword}")
    
    print(f"\n⚠️  Documented but Not in Core ({len(documented_but_not_core)}):")
    for keyword in sorted(documented_but_not_core):
        print(f"  ? {keyword}")
    
    # Save results
    with open("definitive_apex_keywords.txt", 'w', encoding='utf-8') as f:
        f.write("Definitive APEX Keywords Analysis\n")
        f.write("=" * 50 + "\n\n")
        f.write(f"APEX Core Keywords: {len(apex_keywords)}\n")
        f.write(f"Documented Keywords: {len(documented_keywords)}\n")
        f.write(f"Missing from Documentation: {len(missing_keywords)}\n")
        f.write(f"Well Documented: {len(well_documented)}\n\n")
        
        f.write("DEFINITIVE APEX KEYWORDS (from @JsonProperty annotations):\n")
        f.write("-" * 60 + "\n")
        for keyword in sorted(apex_keywords):
            status = "OK" if keyword in documented_keywords else "MISSING"
            f.write(f"  {status} {keyword}\n")
        
        f.write("\nCRITICAL Missing Keywords:\n")
        f.write("-" * 30 + "\n")
        for keyword in critical_missing:
            f.write(f"  - {keyword}\n")
        
        f.write("\nIMPORTANT Missing Keywords:\n")
        f.write("-" * 30 + "\n")
        for keyword in important_missing:
            f.write(f"  - {keyword}\n")
        
        f.write("\nOTHER Missing Keywords:\n")
        f.write("-" * 30 + "\n")
        for keyword in other_missing:
            f.write(f"  - {keyword}\n")
        
        f.write("\nDocumented but Not in Core:\n")
        f.write("-" * 30 + "\n")
        for keyword in sorted(documented_but_not_core):
            f.write(f"  - {keyword}\n")
    
    print(f"\n💾 Results saved to: definitive_apex_keywords.txt")
    
    # Summary
    coverage_percentage = (len(well_documented) / len(apex_keywords)) * 100
    print(f"\n📊 DOCUMENTATION COVERAGE: {coverage_percentage:.1f}%")
    
    if len(critical_missing) > 0:
        print(f"🚨 URGENT: {len(critical_missing)} critical keywords need immediate documentation")
    
    if coverage_percentage < 80:
        print("⚠️  Documentation coverage is below 80% - significant gaps exist")
    elif coverage_percentage < 90:
        print("📝 Documentation coverage is good but could be improved")
    else:
        print("✅ Documentation coverage is excellent!")

if __name__ == "__main__":
    main()
