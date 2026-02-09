/*
 * Copyright 2026 Mark Andrew Ray-Smith Cityline Ltd
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
 *
 * Created: 2026-01-13
 */
package dev.mars.apex.core.service.schema.diff;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration options for schema comparison.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2026-01-13
 */
public class ComparisonOptions {

    private boolean caseInsensitiveNames = true;
    private boolean inferredTypeTolerance = true;
    private boolean allowAddedColumns = true;
    private boolean allowRemovedColumns = false;
    private Map<String, String> typeMappings = new HashMap<>();

    public boolean isCaseInsensitiveNames() {
        return caseInsensitiveNames;
    }

    public void setCaseInsensitiveNames(boolean caseInsensitiveNames) {
        this.caseInsensitiveNames = caseInsensitiveNames;
    }

    public boolean isInferredTypeTolerance() {
        return inferredTypeTolerance;
    }

    public void setInferredTypeTolerance(boolean inferredTypeTolerance) {
        this.inferredTypeTolerance = inferredTypeTolerance;
    }

    public boolean isAllowAddedColumns() {
        return allowAddedColumns;
    }

    public void setAllowAddedColumns(boolean allowAddedColumns) {
        this.allowAddedColumns = allowAddedColumns;
    }

    public boolean isAllowRemovedColumns() {
        return allowRemovedColumns;
    }

    public void setAllowRemovedColumns(boolean allowRemovedColumns) {
        this.allowRemovedColumns = allowRemovedColumns;
    }

    public Map<String, String> getTypeMappings() {
        return typeMappings;
    }

    public void setTypeMappings(Map<String, String> typeMappings) {
        this.typeMappings = typeMappings;
    }

    public void addTypeMapping(String sourceType, String targetType) {
        typeMappings.put(sourceType, targetType);
    }

    /**
     * Create default comparison options.
     */
    public static ComparisonOptions defaults() {
        ComparisonOptions options = new ComparisonOptions();
        // Add common type mappings
        options.addTypeMapping("NVARCHAR", "VARCHAR");
        options.addTypeMapping("VARCHAR2", "VARCHAR");
        options.addTypeMapping("NUMBER", "NUMERIC");
        return options;
    }
}
