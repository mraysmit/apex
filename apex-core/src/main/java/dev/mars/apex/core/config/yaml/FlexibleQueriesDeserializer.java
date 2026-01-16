package dev.mars.apex.core.config.yaml;

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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonMappingException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Custom Jackson deserializer that handles both map and array formats for queries.
 * 
 * Supports two formats:
 * 1. Map format (traditional): { "queryName": "SELECT ..." }
 * 2. Array format (new): [ { "name": "queryName", "query": "SELECT ...", ... } ]
 * 
 * The deserializer automatically detects the format and converts both to Map<String, String>
 * for consistent runtime usage.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.2.0
 * @version 1.0
 */
public class FlexibleQueriesDeserializer extends JsonDeserializer<Map<String, String>> {
    
    @Override
    public Map<String, String> deserialize(JsonParser parser, DeserializationContext ctx) 
            throws IOException {
        
        JsonNode node = parser.getCodec().readTree(parser);
        
        if (node.isObject()) {
            // Traditional map format: { "key": "value" }
            return deserializeMapFormat(node);
        } else if (node.isArray()) {
            // New array format: [ { "name": "key", "query": "value" } ]
            return deserializeArrayFormat(node, ctx, parser);
        } else {
            throw new JsonMappingException(parser,
                "Field 'queries' must be either a map object or an array of query objects. " +
                "Found: " + node.getNodeType());
        }
    }
    
    /**
     * Deserialize traditional map format.
     */
    private Map<String, String> deserializeMapFormat(JsonNode node) {
        Map<String, String> result = new HashMap<>();
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String key = field.getKey();
            JsonNode valueNode = field.getValue();
            
            if (valueNode.isTextual()) {
                result.put(key, valueNode.asText());
            } else {
                // Handle multi-line strings and other types
                result.put(key, valueNode.toString());
            }
        }
        
        return result;
    }
    
    /**
     * Deserialize new array format with NamedQuery objects.
     */
    private Map<String, String> deserializeArrayFormat(JsonNode node, 
                                                       DeserializationContext ctx,
                                                       JsonParser parser) throws IOException {
        Map<String, String> result = new HashMap<>();
        
        for (JsonNode item : node) {
            if (!item.isObject()) {
                throw new JsonMappingException(parser,
                    "Array format for 'queries' must contain objects with 'name' and 'query' fields. " +
                    "Found array element of type: " + item.getNodeType());
            }
            
            // Extract required fields
            JsonNode nameNode = item.get("name");
            JsonNode queryNode = item.get("query");
            
            if (nameNode == null || !nameNode.isTextual()) {
                throw new JsonMappingException(parser,
                    "Each query object must have a 'name' field (string). " +
                    "Found: " + (nameNode == null ? "missing" : nameNode.getNodeType()));
            }
            
            if (queryNode == null) {
                throw new JsonMappingException(parser,
                    "Query object with name '" + nameNode.asText() + "' must have a 'query' field");
            }
            
            String name = nameNode.asText();
            String query = queryNode.isTextual() ? queryNode.asText() : queryNode.toString();
            
            // Check for duplicates
            if (result.containsKey(name)) {
                throw new JsonMappingException(parser,
                    "Duplicate query name found: '" + name + "'. Query names must be unique.");
            }
            
            result.put(name, query);
        }
        
        return result;
    }
}
