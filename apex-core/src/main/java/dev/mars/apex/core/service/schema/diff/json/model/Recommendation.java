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
 * Created: 2026-01-17
 */
package dev.mars.apex.core.service.schema.diff.json.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Migration recommendation with actionable steps.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2026-01-18
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Recommendation {

    @JsonProperty("priority")
    private String priority;

    @JsonProperty("category")
    private String category;

    @JsonProperty("title")
    private String title;

    @JsonProperty("description")
    private String description;

    @JsonProperty("action")
    private MigrationAction action;

    @JsonProperty("automatable")
    private boolean automatable;

    @JsonProperty("validationRequired")
    private boolean validationRequired;

    public Recommendation() {
    }

    public Recommendation(String priority, String message, Object actions) {
        this.priority = priority;
        this.description = message;
        // actions parameter ignored - tests may pass actions, but we use MigrationAction field
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public MigrationAction getAction() {
        return action;
    }

    public void setAction(MigrationAction action) {
        this.action = action;
    }

    public boolean isAutomatable() {
        return automatable;
    }

    public void setAutomatable(boolean automatable) {
        this.automatable = automatable;
    }

    public boolean isValidationRequired() {
        return validationRequired;
    }

    public void setValidationRequired(boolean validationRequired) {
        this.validationRequired = validationRequired;
    }

    public static class MigrationAction {
        @JsonProperty("type")
        private String type;

        @JsonProperty("sql")
        private String sql;

        @JsonProperty("estimatedImpact")
        private String estimatedImpact;

        @JsonProperty("rollbackPlan")
        private String rollbackPlan;

        public MigrationAction() {}

        public MigrationAction(String type, String sql) {
            this.type = type;
            this.sql = sql;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getSql() {
            return sql;
        }

        public void setSql(String sql) {
            this.sql = sql;
        }

        public String getEstimatedImpact() {
            return estimatedImpact;
        }

        public void setEstimatedImpact(String estimatedImpact) {
            this.estimatedImpact = estimatedImpact;
        }

        public String getRollbackPlan() {
            return rollbackPlan;
        }

        public void setRollbackPlan(String rollbackPlan) {
            this.rollbackPlan = rollbackPlan;
        }
    }
}
