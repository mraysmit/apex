# APEX YAML File Browser - UI Wireframe & Design Specification

## Overview
A standalone web application for browsing, searching, and analyzing APEX YAML configuration files in a directory structure.

---

## 1. Main Layout Wireframe

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  APEX YAML File Browser                                    [Settings] [Help] │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                               │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  📁 Browse Directory: [C:\apex-configs\yaml-files    ] [Browse...]  │   │
│  │  ☑ Include Subfolders    🔍 Search: [____________]  [Search]        │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                               │
├───────────────────────────┬───────────────────────────────────────────────────┤
│  FILE TREE (30%)          │  FILE DETAILS (70%)                              │
├───────────────────────────┼───────────────────────────────────────────────────┤
│                           │                                                   │
│  📁 apex-configs/         │  ┌─────────────────────────────────────────────┐ │
│    📁 scenarios/          │  │ File: 01-trade-validation.yaml              │ │
│      📄 01-trade-valid... │  │ Path: /scenarios/01-trade-validation.yaml   │ │
│      📄 02-compliance...  │  │ Size: 2.4 KB  Modified: 2025-11-09 10:30   │ │
│    📁 rules/              │  └─────────────────────────────────────────────┘ │
│      📄 base-rules.yaml   │                                                   │
│      📄 validation.yaml   │  [Metadata] [Content] [Dependencies] [Validate]  │
│    📁 enrichments/        │                                                   │
│      📄 lookup.yaml       │  ┌─────────────────────────────────────────────┐ │
│    📄 config.yaml         │  │ METADATA                                    │ │
│                           │  │ ─────────────────────────────────────────── │ │
│  ─────────────────────    │  │ ID: trade-validation-rules                  │ │
│  📊 Statistics:           │  │ Name: Trade Validation Rules                │ │
│    Total Files: 47        │  │ Type: rules                                 │ │
│    Scenarios: 12          │  │ Version: 1.0.0                              │ │
│    Rules: 23              │  │ Description: Validates trade data...        │ │
│    Enrichments: 8         │  │                                             │ │
│    Other: 4               │  │ Categories:                                 │ │
│                           │  │   • trading-validation                      │ │
│  [Refresh] [Export List]  │  │   • compliance                              │ │
│                           │  │                                             │ │
│                           │  │ Rules: 5  |  Rule Groups: 2                 │ │
│                           │  │ Enrichments: 0  |  Transformations: 0       │ │
│                           │  └─────────────────────────────────────────────┘ │
│                           │                                                   │
└───────────────────────────┴───────────────────────────────────────────────────┘
│  Status: 47 files loaded from C:\apex-configs\yaml-files (0.3s)             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Component Breakdown

### 2.1 Header Bar
```
┌─────────────────────────────────────────────────────────────────────────────┐
│  🎯 APEX YAML File Browser                         [⚙️ Settings] [❓ Help]  │
└─────────────────────────────────────────────────────────────────────────────┘
```
- **Logo/Title**: Branding and application name
- **Settings Button**: Configuration options (theme, default paths, etc.)
- **Help Button**: User guide and documentation

### 2.2 Control Panel
```
┌─────────────────────────────────────────────────────────────────────────────┐
│  📁 Browse Directory: [C:\apex-configs\yaml-files          ] [Browse...]    │
│  ☑ Include Subfolders    🔍 Search: [file name or content] [Search]        │
│  Filter: [All ▼] [Scenarios] [Rules] [Enrichments] [Other]                 │
└─────────────────────────────────────────────────────────────────────────────┘
```
**Features:**
- Directory path input with browse button
- Checkbox to include/exclude subfolders
- Search box for file names or content
- Quick filter buttons by file type

### 2.3 File Tree Panel (Left - 30% width)
```
┌───────────────────────────────┐
│  FILE TREE                    │
├───────────────────────────────┤
│  📁 apex-configs/             │
│    📁 scenarios/ (12)         │
│      📄 01-trade-valid...     │
│      📄 02-compliance...      │
│      📄 03-enrichment...      │
│    📁 rules/ (23)             │
│      📄 base-rules.yaml       │
│      📄 validation.yaml       │
│    📁 enrichments/ (8)        │
│      📄 lookup.yaml           │
│    📄 config.yaml             │
│                               │
│  ─────────────────────────    │
│  📊 STATISTICS                │
│    Total Files: 47            │
│    ✓ Valid: 45                │
│    ⚠ Warnings: 2              │
│    ✗ Errors: 0                │
│                               │
│    By Type:                   │
│    • Scenarios: 12            │
│    • Rules: 23                │
│    • Enrichments: 8           │
│    • Other: 4                 │
│                               │
│  [🔄 Refresh] [📥 Export]     │
└───────────────────────────────┘
```
**Features:**
- Hierarchical folder/file tree
- File count badges on folders
- Icons by file type (scenario, rule, enrichment)
- Real-time statistics panel
- Refresh and export buttons

### 2.4 File Details Panel (Right - 70% width)

#### Tab 1: Metadata
```
┌─────────────────────────────────────────────────────────────────────────────┐
│  File: 01-trade-validation.yaml                                             │
│  Path: /scenarios/01-trade-validation.yaml                                  │
│  Size: 2.4 KB  |  Modified: 2025-11-09 10:30:45  |  ✓ Valid                │
└─────────────────────────────────────────────────────────────────────────────┘

  [Metadata] [Content] [Dependencies] [Validate] [Actions ▼]

┌─────────────────────────────────────────────────────────────────────────────┐
│  METADATA INFORMATION                                                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                               │
│  Basic Information                                                            │
│  ─────────────────────────────────────────────────────────────────────────  │
│  ID:              trade-validation-rules                                     │
│  Name:            Trade Validation Rules                                     │
│  Type:            rules                                                      │
│  Version:         1.0.0                                                      │
│  Description:     Validates trade data before processing                     │
│                                                                               │
│  Categories                                                                   │
│  ─────────────────────────────────────────────────────────────────────────  │
│  🏷️ trading-validation    🏷️ compliance    🏷️ risk-management              │
│                                                                               │
│  Content Summary                                                              │
│  ─────────────────────────────────────────────────────────────────────────  │
│  Rules:              5        Rule Groups:        2                          │
│  Enrichments:        0        Enrichment Groups:  0                          │
│  Transformations:    0        Data Sources:       1                          │
│                                                                               │
│  Governance                                                                   │
│  ─────────────────────────────────────────────────────────────────────────  │
│  Business Domain:    Trading Operations                                      │
│  Business Owner:     John Smith (john.smith@company.com)                     │
│  Created By:         Jane Doe                                                │
│  Effective Date:     2025-01-01                                              │
│  Expiration Date:    2025-12-31                                              │
│                                                                               │
└─────────────────────────────────────────────────────────────────────────────┘
```

#### Tab 2: Content (YAML Viewer)
```
┌─────────────────────────────────────────────────────────────────────────────┐
│  [Raw YAML] [Formatted] [Tree View]                    [Copy] [Download]    │
├─────────────────────────────────────────────────────────────────────────────┤
│  1  metadata:                                                                │
│  2    id: "trade-validation-rules"                                           │
│  3    name: "Trade Validation Rules"                                         │
│  4    type: "rules"                                                          │
│  5    version: "1.0.0"                                                       │
│  6                                                                            │
│  7  categories:                                                              │
│  8    - name: "trading-validation"                                           │
│  9      business-domain: "Trading Operations"                                │
│ 10                                                                            │
│ 11  rules:                                                                   │
│ 12    - id: "validate-trade-amount"                                          │
│ 13      name: "Validate Trade Amount"                                        │
│ 14      category: "trading-validation"                                       │
│ 15      condition: "#amount > 0"                                        │
│ 16      ...                                                                  │
│                                                                               │
└─────────────────────────────────────────────────────────────────────────────┘
```

#### Tab 3: Dependencies
```
┌─────────────────────────────────────────────────────────────────────────────┐
│  DEPENDENCY GRAPH                                    [Expand All] [Collapse] │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                               │
│  Dependencies (Files this file references)                                   │
│  ─────────────────────────────────────────────────────────────────────────  │
│  └── 📄 base-validation-rules.yaml                                           │
│  └── 📄 trade-config.yaml                                                    │
│  └── 📄 database-lookups.yaml                                                │
│                                                                               │
│  Dependents (Files that reference this file)                                 │
│  ─────────────────────────────────────────────────────────────────────────  │
│  └── 📄 trade-processing-scenario.yaml                                       │
│  └── 📄 compliance-scenario.yaml                                             │
│                                                                               │
│  Dependency Statistics                                                        │
│  ─────────────────────────────────────────────────────────────────────────  │
│  Direct Dependencies:    3                                                   │
│  Total Dependencies:     7 (including transitive)                            │
│  Dependents:            2                                                    │
│  Max Depth:             3                                                    │
│  Circular Dependencies: ✓ None                                               │
│                                                                               │
│  [View Full Tree] [Export Graph]                                             │
│                                                                               │
└─────────────────────────────────────────────────────────────────────────────┘
```

#### Tab 4: Validate
```
┌─────────────────────────────────────────────────────────────────────────────┐
│  VALIDATION RESULTS                                          [Re-validate]   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                               │
│  ✓ Validation Status: PASSED                                                 │
│  Last Validated: 2025-11-09 10:35:22                                         │
│                                                                               │
│  Syntax Check                                                                 │
│  ─────────────────────────────────────────────────────────────────────────  │
│  ✓ Valid YAML syntax                                                         │
│  ✓ No parsing errors                                                         │
│                                                                               │
│  Schema Validation                                                            │
│  ─────────────────────────────────────────────────────────────────────────  │
│  ✓ Required fields present (metadata, rules)                                 │
│  ✓ Valid field types                                                         │
│  ✓ Valid APEX configuration type                                             │
│                                                                               │
│  Content Validation                                                           │
│  ─────────────────────────────────────────────────────────────────────────  │
│  ✓ All rule IDs are unique                                                   │
│  ✓ All category references are valid                                         │
│  ⚠ Warning: Rule 'validate-trade-amount' has no description                  │
│                                                                               │
│  Dependency Validation                                                        │
│  ─────────────────────────────────────────────────────────────────────────  │
│  ✓ All referenced files exist                                                │
│  ✓ No circular dependencies detected                                         │
│                                                                               │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Additional UI Screens

### 3.1 Search Results View
```
┌─────────────────────────────────────────────────────────────────────────────┐
│  Search Results for: "trade validation"                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                               │
│  Found 12 matches in 8 files                                                 │
│                                                                               │
│  📄 01-trade-validation.yaml (3 matches)                                     │
│     Line 2:  name: "Trade Validation Rules"                                  │
│     Line 12: name: "Validate Trade Amount"                                   │
│     Line 18: description: "Validates trade data..."                          │
│                                                                               │
│  📄 02-compliance-rules.yaml (2 matches)                                     │
│     Line 5:  description: "Trade validation for compliance"                  │
│     Line 23: ref: "trade-validation-rules"                                   │
│                                                                               │
│  [Show More Results...]                                                      │
│                                                                               │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 3.2 Settings Dialog
```
┌─────────────────────────────────────────────────────────────────────────────┐
│  ⚙️ Settings                                                    [Save] [Cancel]│
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                               │
│  General                                                                      │
│  ─────────────────────────────────────────────────────────────────────────  │
│  Default Directory:  [C:\apex-configs\yaml-files    ] [Browse...]            │
│  ☑ Remember last directory                                                   │
│  ☑ Auto-refresh on file changes                                              │
│  ☑ Include subfolders by default                                             │
│                                                                               │
│  Display                                                                      │
│  ─────────────────────────────────────────────────────────────────────────  │
│  Theme:              [Light ▼] [Dark] [Auto]                                 │
│  Font Size:          [Medium ▼]                                              │
│  Tree Panel Width:   [30% ▼]                                                 │
│                                                                               │
│  Validation                                                                   │
│  ─────────────────────────────────────────────────────────────────────────  │
│  ☑ Auto-validate on file load                                                │
│  ☑ Show warnings                                                             │
│  ☑ Check for circular dependencies                                           │
│                                                                               │
│  API Connection                                                               │
│  ─────────────────────────────────────────────────────────────────────────  │
│  APEX Manager URL:   [http://localhost:8082] [Test Connection]               │
│  ☑ Enable API integration                                                    │
│                                                                               │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Key Features Summary

### Core Functionality
- ✅ **Directory Browsing**: Browse local file system for YAML files
- ✅ **Recursive Scanning**: Include/exclude subfolders
- ✅ **File Tree View**: Hierarchical display with folder structure
- ✅ **File Details**: Comprehensive metadata and content display
- ✅ **Search**: Search by file name or content
- ✅ **Filter**: Quick filter by file type (scenarios, rules, enrichments)

### Advanced Features
- ✅ **Dependency Analysis**: Show file dependencies and dependents
- ✅ **Validation**: Real-time YAML syntax and schema validation
- ✅ **Statistics**: File counts, type distribution, validation status
- ✅ **Export**: Export file lists and dependency graphs
- ✅ **API Integration**: Connect to APEX YAML Manager REST API

### User Experience
- ✅ **Responsive Layout**: Resizable panels
- ✅ **Dark/Light Theme**: User preference
- ✅ **Keyboard Shortcuts**: Quick navigation
- ✅ **Context Menus**: Right-click actions
- ✅ **Drag & Drop**: Drop folders to browse

---

## 5. Technology Stack Recommendation

### Frontend
- **HTML5**: Semantic structure
- **CSS3**: Modern styling with Flexbox/Grid
- **JavaScript (ES6+)**: Core functionality
- **Optional Libraries**:
  - **CodeMirror** or **Monaco Editor**: YAML syntax highlighting
  - **D3.js** or **Vis.js**: Dependency graph visualization
  - **Bootstrap** or **Tailwind CSS**: UI components

### Backend Integration
- **REST API**: Connect to existing APEX YAML Manager (port 8082)
- **File System API**: For local file browsing (if running as Electron app)
- **WebSocket**: For real-time file change notifications

### Deployment Options
1. **Standalone HTML/JS**: Single-page application
2. **Electron App**: Desktop application with file system access
3. **Web Server**: Hosted on APEX YAML Manager service

---

## 6. Next Steps

Would you like me to:
1. **Create the HTML/CSS/JS implementation** of this wireframe?
2. **Modify the wireframe** based on your feedback?
3. **Create an interactive prototype** with sample data?
4. **Focus on a specific component** first (e.g., file tree, metadata viewer)?

Let me know your preference and I'll proceed with the implementation! 🚀

