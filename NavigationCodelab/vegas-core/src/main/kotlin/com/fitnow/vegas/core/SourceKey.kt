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
 * A typed key that can resolve a value from a data source.
 * The generic parameters ensure type safety without runtime casts.
 *
 * @param C The specific VegasQueryDataSource implementation
 * @param S The specific QuerySource type this key operates on
 * @param T The type of value this key resolves to (covariant)
 */
sealed interface SourceKey<C : VegasQueryDataSource, S : QuerySource, out T> {
    /**
     * Resolves the value from the given data source and query source.
     *
     * @param dataSource The typed data source (not the base interface)
     * @param source The query source to resolve from
     * @return The resolved value, or null if not available
     */
    fun resolve(dataSource: C, source: S): T?
}

/**
 * Marker interface for SourceKeys that resolve to Int values.
 */
interface IntSourceKey<C : VegasQueryDataSource, S : QuerySource> : SourceKey<C, S, Int>

/**
 * Marker interface for SourceKeys that resolve to String values.
 */
interface StringSourceKey<C : VegasQueryDataSource, S : QuerySource> : SourceKey<C, S, String>

/**
 * Marker interface for SourceKeys that resolve to Boolean values.
 */
interface BooleanSourceKey<C : VegasQueryDataSource, S : QuerySource> : SourceKey<C, S, Boolean>

/**
 * Marker interface for SourceKeys that resolve to Long values.
 */
interface LongSourceKey<C : VegasQueryDataSource, S : QuerySource> : SourceKey<C, S, Long>

/**
 * Marker interface for SourceKeys that resolve to Double values.
 */
interface DoubleSourceKey<C : VegasQueryDataSource, S : QuerySource> : SourceKey<C, S, Double>

/**
 * Marker interface for SourceKeys that resolve to Set<String> values.
 */
interface StringSetSourceKey<C : VegasQueryDataSource, S : QuerySource> : SourceKey<C, S, Set<String>>
