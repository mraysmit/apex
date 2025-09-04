package dev.mars.apex.core.engine.pipeline;

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
 * Exception thrown by the Data Pipeline Engine.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 * @version 1.0
 */
public class DataPipelineException extends Exception {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Constructor with message.
     */
    public DataPipelineException(String message) {
        super(message);
    }
    
    /**
     * Constructor with message and cause.
     */
    public DataPipelineException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Constructor with cause.
     */
    public DataPipelineException(Throwable cause) {
        super(cause);
    }
}
