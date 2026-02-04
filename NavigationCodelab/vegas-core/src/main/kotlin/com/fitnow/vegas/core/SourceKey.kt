package com.fitnow.vegas.core

/**
 * A typed key that can resolve a value from a data source.
 * The generic parameters ensure type safety without runtime casts.
 *
 * @param D The specific QueryDataSource implementation
 * @param T The type of value this key resolves to (covariant)
 */
sealed interface SourceKey<D : QueryDataSource, out T> {
    /**
     * Resolves the value from the given data source.
     *
     * @param dataSource The typed data source (not the base interface)
     * @return The resolved value, or null if not available
     */
    fun resolve(dataSource: D ): T?
}

/**
 * Marker interface for SourceKeys that resolve to Int values.
 */
interface IntSourceKey<D : QueryDataSource> : SourceKey<D, Int>

/**
 * Marker interface for SourceKeys that resolve to String values.
 */
interface StringSourceKey<D : QueryDataSource> : SourceKey<D, String>

/**
 * Marker interface for SourceKeys that resolve to Boolean values.
 */
interface BooleanSourceKey<D : QueryDataSource> : SourceKey<D, Boolean>

/**
 * Marker interface for SourceKeys that resolve to Set<String> values.
 */
interface StringSetSourceKey<D : QueryDataSource> : SourceKey<D, Set<String>>
