package dev.mars.apex.core.script;

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

import java.nio.file.Path;

/**
 * Immutable metadata for a runtime script.
 * Tracks identity, location, versioning, and enabled state.
 *
 * @param id           Script identifier (filename without extension)
 * @param path         Absolute path to the script file
 * @param checksum     SHA-256 hex digest of the script source
 * @param lastModified Epoch millis of last file modification
 * @param enabled      Whether the script is available for execution
 * @param version      Monotonically increasing version counter per script ID
 */
public record ScriptMetadata(
        String id,
        Path path,
        String checksum,
        long lastModified,
        boolean enabled,
        long version
) {
    /**
     * Create a new ScriptMetadata with an updated checksum, lastModified, and incremented version.
     */
    public ScriptMetadata withUpdated(String newChecksum, long newLastModified) {
        return new ScriptMetadata(id, path, newChecksum, newLastModified, enabled, version + 1);
    }
}
