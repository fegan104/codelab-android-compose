/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.fitnow.vegas.core

/**
 * Registry for looking up [SourceKey]s by their source and key names.
 * Used during JSON parsing to resolve string-based key references to typed SourceKey instances.
 *
 * @param D The specific [QueryDataSource] implementation this registry is bound to.
 */
interface SourceKeyParser<D : QueryDataSource> {
    /**
     * Finds a SourceKey by its source name and key name.
     *
     * @param sourceName The name of the QuerySource
     * @param raw The name of the key within that source
     * @return The matching SourceKey, or null if not found
     */
    fun findKey(sourceName: String, raw: String): SourceKey<D, *>?

    /**
     * Creates a parameterized SourceKey with where clause parameters.
     * Override this method in generated registries to support keys with where clauses.
     *
     * @param sourceName The name of the QuerySource
     * @param keyName The name of the key within that source
     * @param whereParams Map of where clause parameter names to values (e.g., "historyType" to "group")
     * @return The parameterized SourceKey, or null if not supported
     */
    fun createKeyWithWhere(
        sourceName: String,
        keyName: String,
        whereParams: Map<String, String>
    ): SourceKey<D, *>? = null
}
