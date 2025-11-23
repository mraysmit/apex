# APEX Blocks Prototype Guide

**Version:** 1.0
**Date:** 2025-11-23
**Status:** Prototype

## Overview

The **APEX Blocks Prototype** (`apex_blocks_prototype.html`) is a visual rule editor designed to simplify the creation of APEX YAML configurations. By leveraging **Google Blockly**, it provides a drag-and-drop interface that allows users to construct business logic visually without needing to memorize the strict YAML syntax.

This tool automatically generates valid APEX YAML in real-time as you build your logic, making it an excellent tool for:
*   **Rapid Prototyping**: Quickly sketching out rule logic.
*   **Learning APEX**: Understanding the structure of APEX configurations visually.
*   **Non-Technical Users**: Enabling business analysts to define rules without writing code.

## Getting Started

### Accessing the Tool
The prototype is a standalone HTML file located in the `apex-yaml-manager` module:
`apex-yaml-manager/src/main/resources/static/apex_blocks_prototype.html`

To use it, simply open this file in any modern web browser. No server or build process is required for the basic prototype functionality.

### Interface Layout
1.  **Header**: Contains the title and the **Download YAML** button.
2.  **Workspace (Left)**: The main canvas where you drag and drop blocks to build your configuration.
3.  **YAML Output (Right)**: A real-time preview of the generated YAML code.
4.  **Toolbox (Popup)**: A categorized menu of available blocks (Configuration, Rules, Enrichments, Logic, Actions).

## Functionality & Usage

### 1. Creating a Configuration
Every configuration must start with a **Configuration** block. This block serves as the root of your document and corresponds to the document structure defined in the [APEX YAML Reference](../../docs/APEX_YAML_REFERENCE.md#3-document-structure--metadata).

*   **Drag** the `Configuration` block from the *Configuration* category to the workspace.
*   **Set** the Configuration ID (e.g., `my-trade-rules`).
*   This block provides slots for **Rules**, **Rule Groups**, **Enrichments**, and **Enrichment Groups**.

### 2. Defining Rules
Rules are the core validation units.

*   **Category**: *Rules*
*   **Block**: `Rule`
*   **Usage**: Drag a `Rule` block into the `Rules` slot of your Configuration block.
*   **Fields**:
    *   **ID**: Unique identifier for the rule.
    *   **Name**: Human-readable name.
    *   **Condition**: A boolean logic block (from the *Logic* category) defining when the rule triggers.
    *   **Actions**: (Currently supports logging).
*   **Reference**: Maps to the [Rules Section](../../docs/APEX_YAML_REFERENCE.md#5-rules-section) of the YAML reference.

### 3. Grouping Rules
Rule Groups allow you to combine multiple rules with logical operators (AND/OR).

*   **Category**: *Rules*
*   **Block**: `Rule Group`
*   **Usage**: Drag into the `Rule Groups` slot.
*   **Fields**:
    *   **Operator**: AND / OR.
    *   **Rules**: Accepts `Rule Reference` blocks.
*   **Reference**: Maps to the [Rule Groups Section](../../docs/APEX_YAML_REFERENCE.md#6-rule-groups-section).

### 4. Adding Enrichments
Enrichments modify or enhance data. The prototype supports several types:

*   **Category**: *Enrichments*
*   **Blocks**:
    *   **Calculation**: Performs mathematical or string operations.
    *   **Field Enrichment**: Maps or transforms fields.
    *   **Lookup**: Retrieves data from external datasets (e.g., Counterparty Data).
*   **Usage**: Drag these blocks into the `Enrichments` slot of the Configuration block.

### 5. Building Logic & Expressions
The *Logic* category provides blocks to construct SpEL (Spring Expression Language) expressions visually.

*   **Comparison**: `==`, `!=`, `>`, `<`, etc.
*   **Logic**: `AND`, `OR`.
*   **Field Reference**: Access data fields (e.g., `#amount`, `#currency`).
*   **Values**: Text, Numbers, Booleans.

### 6. Exporting
Once your configuration is complete:
1.  Review the generated YAML in the right-hand panel.
2.  Click the **Download YAML** button in the header.
3.  Save the file as `apex-rules.yaml`.

## Block-to-YAML Mapping

The following table illustrates how visual blocks map to the APEX YAML specification:

| Visual Block | YAML Section | Description |
| :--- | :--- | :--- |
| **Configuration** | `metadata`, `rules`, `enrichments` | The root document structure. |
| **Rule** | `rules` list item | Defines a single validation rule. |
| **Rule Group** | `rule-groups` list item | Groups rules with logic. |
| **Lookup Enrichment** | `enrichments` (type: `lookup-enrichment`) | Configures data lookups. |
| **Calculation** | `enrichments` (type: `calculation-enrichment`) | Performs SpEL calculations. |
| **Field Ref (`#field`)** | SpEL Expression (`#field`) | Direct field access syntax. |

## Technical Details

*   **Library**: Built on [Google Blockly](https://developers.google.com/blockly).
*   **YAML Generation**: Uses `js-yaml` for client-side YAML serialization.
*   **Customization**: The block definitions and code generators are located in the `<script>` section of the HTML file.

## Future Improvements
*   Support for `error-handling` configuration in Rule Groups.
*   Integration with the APEX REST API for direct deployment.
*   Support for `Rule Chains` and `Pipelines`.
