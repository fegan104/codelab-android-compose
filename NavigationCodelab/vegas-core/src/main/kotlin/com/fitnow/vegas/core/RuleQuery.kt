package com.fitnow.vegas.core

/**
 * Represents a query that can resolve a value from a data source.
 *
 * @param D The specific VegasQueryDataSource implementation
 * @param S The specific QuerySource type
 * @param L The type of value being queried
 * @property source The query source to resolve from
 * @property key The source key used to resolve the value
 * @property defaultValue The default value if resolution returns null
 */
data class RuleQuery<D: QueryDataSource, S : QuerySource, L>(
    val source: S,
    val key: SourceKey<D, L>,
    val defaultValue: L?
)
