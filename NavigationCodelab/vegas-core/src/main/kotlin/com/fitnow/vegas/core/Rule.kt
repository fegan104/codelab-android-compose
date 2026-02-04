package com.fitnow.vegas.core

/**
 * Represents a query that can resolve a value from a data source.
 *
 * @param C The specific VegasQueryDataSource implementation
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

/**
 * Represents a rule that evaluates a condition against data from a source.
 * The generic parameters ensure type safety without runtime casts.
 *
 * @param C The specific VegasQueryDataSource implementation
 * @param S The specific QuerySource type
 * @param E The type of the equation's values
 * @property operator The operator used to compare values
 * @property rhs The right-hand side value (constant from rule definition)
 * @property lhs The left-hand side query (resolved from data source)
 */
data class Rule<D : QueryDataSource, S : QuerySource, E>(
    val operator: RuleOperator<E>,
    val rhs: E,
    val lhs: RuleQuery<D, S, E>
) {
    /**
     * Evaluates this rule against the provided data source.
     *
     * The evaluation flow:
     * 1. Resolve the LHS value using the key from the query
     * 2. If null, use the default value from the query
     * 3. If still null, return false (cannot evaluate)
     * 4. Apply the operator to compare LHS with RHS
     *
     * @param dataSource The typed data source to evaluate against (no casting needed!)
     * @return true if the rule condition is satisfied, false otherwise
     */
    fun evaluate(dataSource: D): Boolean {
        // Resolve the value from the data source - no casting needed!
        val resolvedValue = lhs.key.resolve(dataSource)

        // Use resolved value or fall back to default
        val lhsValue = resolvedValue ?: lhs.defaultValue

        // Cannot evaluate if we have no value
        if (lhsValue == null) {
            return false
        }

        // Apply the operator
        return operator.evaluate(lhsValue, rhs)
    }
}
