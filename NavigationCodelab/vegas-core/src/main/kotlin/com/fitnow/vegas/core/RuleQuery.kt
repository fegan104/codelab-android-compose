package com.fitnow.vegas.core

/**
 * Represents a query that can resolve a value from a data source.
 *
 * @param D The specific VegasQueryDataSource implementation
 * @param E The type of value being queried
 * @property key The source key used to resolve the value
 * @property defaultValue The default value if resolution returns null
 */
data class RuleQuery<D: QueryDataSource,  E>(
    val key: SourceKey<D, E>,
    val defaultValue: E?
)
