package dev.mars.apex.core.service.data;

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
 * Interface for data sources that can provide various types of data.
 *
 * This interface is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Interface for data sources that can provide various types of data.
 * This interface abstracts away the concrete implementation of data sources,
 * allowing for different data sources to be used interchangeably.
 */
public interface DataSource {
    /**
     * Get the name of this data source.
     * 
     * @return The name of the data source
     */
    String getName();

    /**
     * Get the type of data this source provides.
     * 
     * @return The type of data (e.g., "products", "customers", "trades")
     */
    String getDataType();

    /**
     * Check if this data source can provide the specified data type.
     * 
     * @param dataType The type of data to check for
     * @return True if this data source can provide the specified data type, false otherwise
     */
    boolean supportsDataType(String dataType);

    /**
     * Get data from this source.
     * 
     * @param <T> The type of data to return
     * @param dataType The type of data to get
     * @param parameters Optional parameters to filter or customize the data
     * @return The requested data, or null if the data type is not supported
     */
    <T> T getData(String dataType, Object... parameters);
}
