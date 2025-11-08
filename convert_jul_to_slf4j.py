#!/usr/bin/env python3
"""Convert java.util.logging to SLF4J in Java files."""

import re
from pathlib import Path

# List of files to convert
FILES_TO_CONVERT = [
    "apex-core/src/main/java/dev/mars/apex/core/config/yaml/DeferredDependencyResolver.java",
    "apex-core/src/main/java/dev/mars/apex/core/config/yaml/OrderedYamlConfiguration.java",
    "apex-core/src/main/java/dev/mars/apex/core/config/yaml/OrderedYamlParser.java",
    "apex-core/src/main/java/dev/mars/apex/core/config/yaml/ProcessingContext.java",
    "apex-core/src/main/java/dev/mars/apex/core/config/yaml/RulesEngineService.java",
    "apex-core/src/main/java/dev/mars/apex/core/config/yaml/SequentialYamlProcessor.java",
    "apex-core/src/main/java/dev/mars/apex/core/config/yaml/YamlConfigurationLoader.java",
    "apex-core/src/main/java/dev/mars/apex/core/config/yaml/YamlConfigurationMerger.java",
    "apex-core/src/main/java/dev/mars/apex/core/config/yaml/YamlRuleFactory.java",
    "apex-core/src/main/java/dev/mars/apex/core/config/yaml/YamlRulesEngineService.java",
    "apex-core/src/main/java/dev/mars/apex/core/engine/config/RulesEngineConfiguration.java",
    "apex-core/src/main/java/dev/mars/apex/core/engine/executor/PatternExecutor.java",
    "apex-core/src/main/java/dev/mars/apex/core/engine/executor/RuleChainExecutor.java",
    "apex-core/src/main/java/dev/mars/apex/core/service/data/DataServiceManager.java",
    "apex-core/src/main/java/dev/mars/apex/core/service/data/external/database/JdbcTemplateFactory.java",
    "apex-core/src/main/java/dev/mars/apex/core/service/data/external/database/SqlErrorClassifier.java",
    "apex-core/src/main/java/dev/mars/apex/core/service/engine/RuleEngineService.java",
    "apex-core/src/main/java/dev/mars/apex/core/service/engine/TemplateProcessorService.java",
    "apex-core/src/main/java/dev/mars/apex/core/service/enrichment/EnrichmentGroupFactory.java",
    "apex-core/src/main/java/dev/mars/apex/core/service/enrichment/YamlEnrichmentProcessor.java",
    "apex-core/src/main/java/dev/mars/apex/core/service/error/ErrorContextService.java",
    "apex-core/src/main/java/dev/mars/apex/core/service/error/ErrorRecoveryService.java",
    "apex-core/src/main/java/dev/mars/apex/core/service/expression/ExpressionEvaluationService.java",
    "apex-core/src/main/java/dev/mars/apex/core/service/lookup/DatasetLookupService.java",
    "apex-core/src/main/java/dev/mars/apex/core/service/lookup/DatasetLookupServiceFactory.java",
    "apex-core/src/main/java/dev/mars/apex/core/service/lookup/DatasetSignature.java",
    "apex-core/src/main/java/dev/mars/apex/core/service/monitoring/RulePerformanceMonitor.java",
    "apex-core/src/main/java/dev/mars/apex/core/service/scenario/ScenarioConfiguration.java",
    "apex-core/src/main/java/dev/mars/apex/core/service/transform/GenericTransformer.java",
    "apex-core/src/main/java/dev/mars/apex/core/service/transform/GenericTransformerService.java",
    "apex-core/src/main/java/dev/mars/apex/core/service/validation/ValidationService.java",
    "apex-core/src/main/java/dev/mars/apex/core/util/RuleParameterExtractor.java",
    "apex-core/src/main/java/dev/mars/apex/core/util/YamlProcessingSequenceAnalyzer.java",
]

def convert_file(file_path):
    """Convert a single Java file from java.util.logging to SLF4J."""
    path = Path(file_path)
    if not path.exists():
        print(f"  ✗ File not found: {file_path}")
        return False
    
    print(f"Converting: {file_path}")
    
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original_content = content
    
    # 1. Remove java.util.logging imports
    content = re.sub(r'import java\.util\.logging\.Level;\n', '', content)
    content = re.sub(r'import java\.util\.logging\.Logger;\n', '', content)
    
    # 2. Add SLF4J imports if not already present
    if 'import org.slf4j.Logger;' not in content:
        # Find the position after the last import
        import_pattern = r'(import [^;]+;\n)'
        imports = list(re.finditer(import_pattern, content))
        if imports:
            last_import = imports[-1]
            insert_pos = last_import.end()
            content = content[:insert_pos] + 'import org.slf4j.Logger;\nimport org.slf4j.LoggerFactory;\n' + content[insert_pos:]
    
    # 3. Replace logger declarations
    content = re.sub(
        r'private static final Logger LOGGER = Logger\.getLogger\(([^)]+)\.class\.getName\(\)\);',
        r'private static final Logger logger = LoggerFactory.getLogger(\1.class);',
        content
    )
    content = re.sub(
        r'private final Logger LOGGER = Logger\.getLogger\(([^)]+)\.class\.getName\(\)\);',
        r'private final Logger logger = LoggerFactory.getLogger(\1.class);',
        content
    )
    
    # 4. Replace LOGGER method calls
    content = re.sub(r'\bLOGGER\.fine\(', 'logger.debug(', content)
    content = re.sub(r'\bLOGGER\.finest\(', 'logger.trace(', content)
    content = re.sub(r'\bLOGGER\.info\(', 'logger.info(', content)
    content = re.sub(r'\bLOGGER\.warning\(', 'logger.warn(', content)
    content = re.sub(r'\bLOGGER\.severe\(', 'logger.error(', content)
    
    # 5. Replace LOGGER.log(Level.X, ...) patterns
    content = re.sub(r'\bLOGGER\.log\(Level\.SEVERE,\s*', 'logger.error(', content)
    content = re.sub(r'\bLOGGER\.log\(Level\.WARNING,\s*', 'logger.warn(', content)
    content = re.sub(r'\bLOGGER\.log\(Level\.INFO,\s*', 'logger.info(', content)
    content = re.sub(r'\bLOGGER\.log\(Level\.FINE,\s*', 'logger.debug(', content)
    content = re.sub(r'\bLOGGER\.log\(Level\.FINEST,\s*', 'logger.trace(', content)
    
    # Only write if content changed
    if content != original_content:
        with open(path, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"  ✓ Converted")
        return True
    else:
        print(f"  - No changes needed")
        return False

def main():
    """Main function to convert all files."""
    print(f"Converting {len(FILES_TO_CONVERT)} files from java.util.logging to SLF4J\n")
    
    converted_count = 0
    for file_path in FILES_TO_CONVERT:
        if convert_file(file_path):
            converted_count += 1
    
    print(f"\nConversion complete! Converted {converted_count} out of {len(FILES_TO_CONVERT)} files")

if __name__ == '__main__':
    main()

