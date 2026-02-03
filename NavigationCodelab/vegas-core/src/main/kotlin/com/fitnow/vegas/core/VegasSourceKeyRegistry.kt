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
 * Registry for looking up SourceKeys by their source and key names.
 * Used during JSON parsing to resolve string-based key references to typed SourceKey instances.
 *
 * @param C The specific VegasQueryDataSource implementation this registry is bound to.
 */
interface VegasSourceKeyRegistry<C : VegasQueryDataSource> {
    /**
     * Finds a SourceKey by its source name and key name.
     *
     * @param sourceName The name of the QuerySource
     * @param keyName The name of the key within that source
     * @return The matching SourceKey, or null if not found
     */
    fun findKey(sourceName: String, keyName: String): SourceKey<C, *, *>?

    /**
     * Finds the QuerySource instance for the given source name.
     *
     * @param sourceName The name of the QuerySource
     * @return The matching QuerySource, or null if not found
     */
    fun findSource(sourceName: String): QuerySource? = null
}
