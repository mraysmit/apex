Graph-100 dataset (work in progress)

Purpose
- Stress-test dependency analysis with a large, interrelated folder
- Cover fan-in/out, deep chains, shared libs, cycles, and missing references

Structure (initial seed)
- 00-scenario-registry.yaml (root)
- 10-scenario-a.yaml, 11-scenario-b.yaml
- 20-groups-a.yaml, 21-groups-b.yaml
- 30-rules-a.yaml, 31-rules-b.yaml

Planned scale
- Target ~100 YAML files across scenarios, groups, rules/enrichments/configs, shared libs

Usage
- Use /yaml-manager/api/dependencies/scan-folder?folderPath=... to list files
- Build a tree with /yaml-manager/api/dependencies/tree?rootFile=... (00-scenario-registry.yaml)
- Validate with POST /yaml-manager/api/dependencies/validate-tree?rootFile=...

