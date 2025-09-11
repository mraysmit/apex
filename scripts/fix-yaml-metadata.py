#!/usr/bin/env python3
"""
APEX YAML Auto-Fixer
Automatically fixes common YAML validation issues in APEX configuration files.
"""

import os
import re
import sys
import yaml
from pathlib import Path
from typing import Dict, List, Optional

class ApexYamlFixer:
    """Fixes common YAML validation issues for APEX configurations."""
    
    VALID_DOCUMENT_TYPES = {
        'rule-config', 'enrichment', 'dataset', 'scenario',
        'scenario-registry', 'bootstrap', 'rule-chain', 
        'external-data-config', 'pipeline-config'
    }
    
    TYPE_REQUIRED_FIELDS = {
        'rule-config': ['author'],
        'enrichment': ['author'],
        'dataset': ['source'],
        'scenario': ['business-domain', 'owner'],
        'scenario-registry': ['created-by'],
        'bootstrap': ['business-domain', 'created-by'],
        'rule-chain': ['author'],
        'external-data-config': ['author'],
        'pipeline-config': ['author']
    }
    
    def __init__(self, dry_run: bool = False):
        self.dry_run = dry_run
        self.fixes_applied = []
        
    def fix_yaml_file(self, file_path: Path) -> bool:
        """Fix a single YAML file. Returns True if fixes were applied."""
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            original_content = content
            content = self._fix_metadata_section(content, file_path)
            
            if content != original_content:
                if not self.dry_run:
                    # Backup original file
                    backup_path = file_path.with_suffix(file_path.suffix + '.backup')
                    with open(backup_path, 'w', encoding='utf-8') as f:
                        f.write(original_content)
                    
                    # Write fixed content
                    with open(file_path, 'w', encoding='utf-8') as f:
                        f.write(content)
                    
                    print(f"✅ Fixed: {file_path}")
                    print(f"   Backup: {backup_path}")
                else:
                    print(f"🔍 Would fix: {file_path}")
                
                return True
            else:
                print(f"✅ Already valid: {file_path}")
                return False
                
        except Exception as e:
            print(f"❌ Error processing {file_path}: {e}")
            return False
    
    def _fix_metadata_section(self, content: str, file_path: Path) -> str:
        """Fix metadata section issues."""
        # Check if metadata section exists
        if not re.search(r'metadata\s*:', content):
            print(f"⚠️  No metadata section found in {file_path}")
            return content
        
        # Parse YAML to work with structure
        try:
            data = yaml.safe_load(content)
            if not isinstance(data, dict) or 'metadata' not in data:
                return content
                
            metadata = data['metadata']
            if not isinstance(metadata, dict):
                return content
            
            # Fix missing required fields
            fixes = []
            
            # Add missing 'id' field
            if 'id' not in metadata:
                # Generate ID from filename
                file_id = self._generate_id_from_filename(file_path)
                metadata['id'] = file_id
                fixes.append(f"Added 'id': {file_id}")
            
            # Add missing 'type' field
            if 'type' not in metadata:
                # Infer type from content or filename
                doc_type = self._infer_document_type(data, file_path)
                if doc_type:
                    metadata['type'] = doc_type
                    fixes.append(f"Added 'type': {doc_type}")
            
            # Add type-specific required fields
            if 'type' in metadata and metadata['type'] in self.TYPE_REQUIRED_FIELDS:
                for required_field in self.TYPE_REQUIRED_FIELDS[metadata['type']]:
                    if required_field not in metadata:
                        default_value = self._get_default_value(required_field)
                        metadata['author'] = default_value
                        fixes.append(f"Added '{required_field}': {default_value}")
            
            if fixes:
                self.fixes_applied.extend(fixes)
                # Convert back to YAML while preserving comments
                return self._preserve_yaml_structure(content, data)
            
            return content
            
        except yaml.YAMLError as e:
            print(f"⚠️  YAML parsing error in {file_path}: {e}")
            return content
    
    def _generate_id_from_filename(self, file_path: Path) -> str:
        """Generate a reasonable ID from filename."""
        # Remove extension and convert to kebab-case
        name = file_path.stem
        name = re.sub(r'[_\s]+', '-', name)
        name = re.sub(r'[^a-zA-Z0-9\-]', '', name)
        return name.lower()
    
    def _infer_document_type(self, data: Dict, file_path: Path) -> Optional[str]:
        """Infer document type from content and filename."""
        # Check content sections
        if 'rules' in data or 'enrichments' in data:
            return 'rule-config'
        elif 'dataSources' in data or 'data-sources' in data:
            return 'external-data-config'
        elif 'pipeline' in data:
            return 'pipeline-config'
        elif 'data' in data:
            return 'dataset'
        elif 'scenario' in data:
            return 'scenario'
        
        # Check filename patterns
        filename = file_path.name.lower()
        if 'pipeline' in filename:
            return 'pipeline-config'
        elif 'rule' in filename:
            return 'rule-config'
        elif 'enrichment' in filename:
            return 'enrichment'
        elif 'data' in filename:
            return 'external-data-config'
        
        # Default fallback
        return 'rule-config'
    
    def _get_default_value(self, field_name: str) -> str:
        """Get default value for a field."""
        defaults = {
            'author': 'apex.team@company.com',
            'business-domain': 'General',
            'owner': 'APEX Team',
            'created-by': 'APEX System',
            'source': 'Generated'
        }
        return defaults.get(field_name, 'APEX System')
    
    def _preserve_yaml_structure(self, original_content: str, data: Dict) -> str:
        """Preserve YAML structure and comments while updating data."""
        # This is a simplified approach - in production, you'd want a more
        # sophisticated YAML preserving library like ruamel.yaml
        
        # For now, we'll do a simple replacement in the metadata section
        lines = original_content.split('\n')
        result_lines = []
        in_metadata = False
        metadata_indent = 0
        
        for line in lines:
            if re.match(r'^metadata\s*:', line):
                in_metadata = True
                metadata_indent = len(line) - len(line.lstrip())
                result_lines.append(line)
                
                # Add missing fields after metadata line
                metadata = data['metadata']
                current_fields = set()
                
                # Scan existing fields
                for next_line in lines[lines.index(line)+1:]:
                    if next_line.strip() == '' or next_line.startswith('#'):
                        continue
                    if not next_line.startswith(' ' * (metadata_indent + 2)):
                        break
                    field_match = re.match(r'\s*(\w+)\s*:', next_line)
                    if field_match:
                        current_fields.add(field_match.group(1))
                
                # Add missing required fields
                required_fields = ['id', 'name', 'version', 'description', 'type']
                for field in required_fields:
                    if field not in current_fields and field in metadata:
                        indent = ' ' * (metadata_indent + 2)
                        value = metadata[field]
                        if isinstance(value, str):
                            result_lines.append(f'{indent}{field}: "{value}"')
                        else:
                            result_lines.append(f'{indent}{field}: {value}')
                
            elif in_metadata and line.strip() and not line.startswith(' ' * (metadata_indent + 1)):
                in_metadata = False
                result_lines.append(line)
            else:
                result_lines.append(line)
        
        return '\n'.join(result_lines)

def main():
    """Main function to run the YAML fixer."""
    import argparse
    
    parser = argparse.ArgumentParser(description='Fix APEX YAML validation issues')
    parser.add_argument('paths', nargs='*', help='YAML files or directories to fix')
    parser.add_argument('--dry-run', action='store_true', help='Show what would be fixed without making changes')
    parser.add_argument('--recursive', '-r', action='store_true', help='Process directories recursively')
    
    args = parser.parse_args()
    
    if not args.paths:
        args.paths = ['.']
    
    fixer = ApexYamlFixer(dry_run=args.dry_run)
    
    yaml_files = []
    for path_str in args.paths:
        path = Path(path_str)
        if path.is_file() and path.suffix.lower() in ['.yaml', '.yml']:
            yaml_files.append(path)
        elif path.is_dir():
            if args.recursive:
                yaml_files.extend(path.rglob('*.yaml'))
                yaml_files.extend(path.rglob('*.yml'))
            else:
                yaml_files.extend(path.glob('*.yaml'))
                yaml_files.extend(path.glob('*.yml'))
    
    # Filter out target directories
    yaml_files = [f for f in yaml_files if 'target' not in str(f)]
    
    print(f"🔍 Found {len(yaml_files)} YAML files to process")
    
    fixed_count = 0
    for yaml_file in yaml_files:
        if fixer.fix_yaml_file(yaml_file):
            fixed_count += 1
    
    print(f"\n📊 Summary:")
    print(f"   Files processed: {len(yaml_files)}")
    print(f"   Files fixed: {fixed_count}")
    
    if args.dry_run:
        print(f"\n💡 Run without --dry-run to apply fixes")

if __name__ == '__main__':
    main()
