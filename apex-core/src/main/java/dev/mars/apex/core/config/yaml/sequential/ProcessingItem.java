package dev.mars.apex.core.config.yaml.sequential;

/*
 * Copyright 2025 Mark Andrew Ray-Smith Cityline Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Represents a single processing item in document order.
 * 
 * This class captures the order of individual items (enrichments, rules, groups, etc.)
 * as they appear in the YAML document. It enables item-level sequential processing
 * where items from different sections can be interleaved in document order.
 * 
 * <p>Example usage:
 * <pre>
 * ProcessingItem item1 = new ProcessingItem("enrichments", "enrich-counterparty");
 * ProcessingItem item2 = new ProcessingItem("rules", "validate-credit-limit");
 * ProcessingItem item3 = new ProcessingItem("enrichment-groups", "risk-calculations");
 * 
 * // Items are processed in the order they appear in the YAML document
 * List&lt;ProcessingItem&gt; itemOrder = Arrays.asList(item1, item2, item3);
 * </pre>
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-11-06
 */
public class ProcessingItem {

    private final String sectionType;  // e.g., "enrichments", "rules", "enrichment-groups"
    private final String itemId;       // e.g., "enrich-1", "rule-1", "group-1"
    private final String itemType;     // e.g., "calculation-enrichment", "lookup-enrichment", "simple-rule"
    private final String itemName;     // e.g., "Group A", "Validate Credit Limit"

    /**
     * Creates a new processing item with full metadata.
     *
     * @param sectionType The type of section this item belongs to (e.g., "enrichments", "rules")
     * @param itemId The unique identifier of the item within its section
     * @param itemType The type of the item (e.g., "calculation-enrichment", "lookup-enrichment")
     * @param itemName The human-readable name of the item (can be null)
     * @throws IllegalArgumentException if sectionType or itemId is null or empty
     */
    public ProcessingItem(String sectionType, String itemId, String itemType, String itemName) {
        if (sectionType == null || sectionType.trim().isEmpty()) {
            throw new IllegalArgumentException("Section type cannot be null or empty");
        }
        if (itemId == null || itemId.trim().isEmpty()) {
            throw new IllegalArgumentException("Item ID cannot be null or empty");
        }

        this.sectionType = sectionType;
        this.itemId = itemId;
        this.itemType = itemType;
        this.itemName = itemName;
    }

    /**
     * Creates a new processing item without metadata (for backward compatibility).
     *
     * @param sectionType The type of section this item belongs to (e.g., "enrichments", "rules")
     * @param itemId The unique identifier of the item within its section
     * @throws IllegalArgumentException if sectionType or itemId is null or empty
     */
    public ProcessingItem(String sectionType, String itemId) {
        this(sectionType, itemId, null, null);
    }
    
    /**
     * Gets the section type of this item.
     * 
     * @return The section type (e.g., "enrichments", "rules", "enrichment-groups")
     */
    public String getSectionType() {
        return sectionType;
    }
    
    /**
     * Gets the unique identifier of this item.
     *
     * @return The item ID (e.g., "enrich-1", "rule-1")
     */
    public String getItemId() {
        return itemId;
    }

    /**
     * Gets the type of this item (e.g., "calculation-enrichment", "lookup-enrichment").
     *
     * @return The item type, or null if not available
     */
    public String getItemType() {
        return itemType;
    }

    /**
     * Gets the human-readable name of this item.
     *
     * @return The item name, or null if not available
     */
    public String getItemName() {
        return itemName;
    }

    /**
     * Checks if this item is an enrichment.
     *
     * @return true if this is an enrichment item
     */
    public boolean isEnrichment() {
        return "enrichments".equals(sectionType);
    }
    
    /**
     * Checks if this item is a rule.
     * 
     * @return true if this is a rule item
     */
    public boolean isRule() {
        return "rules".equals(sectionType);
    }
    
    /**
     * Checks if this item is an enrichment group.
     * 
     * @return true if this is an enrichment group item
     */
    public boolean isEnrichmentGroup() {
        return "enrichment-groups".equals(sectionType);
    }
    
    /**
     * Checks if this item is a rule group.
     * 
     * @return true if this is a rule group item
     */
    public boolean isRuleGroup() {
        return "rule-groups".equals(sectionType);
    }
    
    /**
     * Checks if this item is a transformation.
     * 
     * @return true if this is a transformation item
     */
    public boolean isTransformation() {
        return "transformations".equals(sectionType);
    }
    
    /**
     * Checks if this item is a rule chain.
     * 
     * @return true if this is a rule chain item
     */
    public boolean isRuleChain() {
        return "rule-chains".equals(sectionType);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        ProcessingItem that = (ProcessingItem) o;
        
        if (!sectionType.equals(that.sectionType)) return false;
        return itemId.equals(that.itemId);
    }
    
    @Override
    public int hashCode() {
        int result = sectionType.hashCode();
        result = 31 * result + itemId.hashCode();
        return result;
    }
    
    @Override
    public String toString() {
        return sectionType + ":" + itemId;
    }
    
    /**
     * Gets a human-readable description of this processing item.
     * 
     * @return A description string
     */
    public String getDescription() {
        return "ProcessingItem{" +
                "sectionType='" + sectionType + '\'' +
                ", itemId='" + itemId + '\'' +
                '}';
    }
}

