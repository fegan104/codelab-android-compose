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
 * Represents a query that can resolve a value from a data source.
 *
 * @param C The specific VegasQueryDataSource implementation
 * @param S The specific QuerySource type
 * @param T The type of value being queried
 * @property source The query source to resolve from
 * @property key The source key used to resolve the value
 * @property defaultValue The default value if resolution returns null
 */
data class RuleQuery<C : VegasQueryDataSource, S : QuerySource, T>(
    val source: S,
    val key: SourceKey<C, S, T>,
    val defaultValue: T?
)

/**
 * Represents a rule that evaluates a condition against data from a source.
 * The generic parameters ensure type safety without runtime casts.
 *
 * @param C The specific VegasQueryDataSource implementation
 * @param S The specific QuerySource type
 * @param T The type of value being compared
 * @property operator The operator used to compare values
 * @property rhs The right-hand side value (constant from rule definition)
 * @property lhs The left-hand side query (resolved from data source)
 */
data class Rule<C : VegasQueryDataSource, S : QuerySource, T>(
    val operator: RuleOperator<T>,
    val rhs: T,
    val lhs: RuleQuery<C, S, T>
) {
    /**
     * Evaluates this rule against the provided data source.
     *
     * The evaluation flow:
     * 1. Resolve the LHS value using the key and source from the query
     * 2. If null, use the default value from the query
     * 3. If still null, return false (cannot evaluate)
     * 4. Apply the operator to compare LHS with RHS
     *
     * @param dataSource The typed data source to evaluate against (no casting needed!)
     * @return true if the rule condition is satisfied, false otherwise
     */
    fun evaluate(dataSource: C): Boolean {
        // Resolve the value from the data source - no casting needed!
        val resolvedValue = lhs.key.resolve(dataSource, lhs.source)

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
