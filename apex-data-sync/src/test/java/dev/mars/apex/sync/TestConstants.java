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
 * Created: 2026-01-14
 */

package dev.mars.apex.sync;

/**
 * Global constants for TestContainers and Integration Tests.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @deprecated Use {@link TestContainerImages} instead for centralized image version management.
 * This class is maintained for backward compatibility but will be removed in future versions.
 */
@Deprecated
public class TestConstants {
    /**
     * @deprecated Use {@link TestContainerImages#MSSQL_SERVER} instead
     */
    @Deprecated
    public static final String MSSQL_IMAGE = TestContainerImages.MSSQL_SERVER;
    
    /**
     * @deprecated Use {@link TestContainerImages#POSTGRES} instead
     */
    @Deprecated
    public static final String POSTGRES_IMAGE = TestContainerImages.POSTGRES;
}
