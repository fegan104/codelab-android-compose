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
 * Sealed interface for rule operators that evaluate comparisons.
 *
 * @param L The type of the left-hand side value
 * @param R The type of the right-hand side value
 */
sealed interface RuleOperator<L, R> {
    /**
     * Evaluates the comparison between left-hand side and right-hand side values.
     *
     * @param lhs The left-hand side value (resolved from the data source)
     * @param rhs The right-hand side value (from the rule definition)
     * @return true if the comparison holds, false otherwise
     */
    fun evaluate(lhs: L, rhs: R): Boolean
}

// ============================================================================
// Int Operators
// ============================================================================

sealed interface IntOperator : RuleOperator<Int, Int>

data object IntEquals : IntOperator {
    override fun evaluate(lhs: Int, rhs: Int): Boolean = lhs == rhs
}

data object IntNotEquals : IntOperator {
    override fun evaluate(lhs: Int, rhs: Int): Boolean = lhs != rhs
}

data object IntGreaterThan : IntOperator {
    override fun evaluate(lhs: Int, rhs: Int): Boolean = lhs > rhs
}

data object IntGreaterThanOrEquals : IntOperator {
    override fun evaluate(lhs: Int, rhs: Int): Boolean = lhs >= rhs
}

data object IntLessThan : IntOperator {
    override fun evaluate(lhs: Int, rhs: Int): Boolean = lhs < rhs
}

data object IntLessThanOrEquals : IntOperator {
    override fun evaluate(lhs: Int, rhs: Int): Boolean = lhs <= rhs
}

// ============================================================================
// String Operators
// ============================================================================

sealed interface StringOperator<R> : RuleOperator<String, R>

data object StringEquals : StringOperator<String> {
    override fun evaluate(lhs: String, rhs: String): Boolean = lhs == rhs
}

data object StringNotEquals : StringOperator<String> {
    override fun evaluate(lhs: String, rhs: String): Boolean = lhs != rhs
}

data object StringContains : StringOperator<String> {
    override fun evaluate(lhs: String, rhs: String): Boolean = lhs.contains(rhs)
}

data object StringNotContains : StringOperator<String> {
    override fun evaluate(lhs: String, rhs: String): Boolean = !lhs.contains(rhs)
}

// ============================================================================
// Set<String> Operators
// ============================================================================

/**
 * Operators that compare a Set<String> against some right-hand side value.
 */
sealed interface SetStringOperator<R> : RuleOperator<Set<String>, R>

/**
 * Checks if the sets are equivalent (contain the same elements).
 */
data object StringSetEquivalent : SetStringOperator<List<String>> {
    override fun evaluate(lhs: Set<String>, rhs: List<String>): Boolean = lhs == rhs.toSet()
}

/**
 * Checks if the sets are not equivalent.
 */
data object StringSetNotEquivalent : SetStringOperator<List<String>> {
    override fun evaluate(lhs: Set<String>, rhs: List<String>): Boolean = lhs != rhs.toSet()
}

/**
 * Checks if the left-hand set is a subset of the right-hand list.
 */
data object StringSetIsSubset : SetStringOperator<List<String>> {
    override fun evaluate(lhs: Set<String>, rhs: List<String>): Boolean = rhs.toSet().containsAll(lhs)
}

/**
 * Checks if the left-hand set is not a subset of the right-hand list.
 */
data object StringSetNotIsSubset : SetStringOperator<List<String>> {
    override fun evaluate(lhs: Set<String>, rhs: List<String>): Boolean = !rhs.toSet().containsAll(lhs)
}

/**
 * Checks if the left-hand set is a superset of the right-hand list.
 */
data object StringSetIsSuperset : SetStringOperator<List<String>> {
    override fun evaluate(lhs: Set<String>, rhs: List<String>): Boolean = lhs.containsAll(rhs)
}

/**
 * Checks if the left-hand set is not a superset of the right-hand list.
 */
data object StringSetNotIsSuperset : SetStringOperator<List<String>> {
    override fun evaluate(lhs: Set<String>, rhs: List<String>): Boolean = !lhs.containsAll(rhs)
}

/**
 * Checks if any element of the left-hand set matches any element of the right-hand list.
 */
data object StringSetAnyMatch : SetStringOperator<List<String>> {
    override fun evaluate(lhs: Set<String>, rhs: List<String>): Boolean = lhs.any { it in rhs }
}

/**
 * Checks if no element of the left-hand set matches any element of the right-hand list.
 */
data object StringSetNotAnyMatch : SetStringOperator<List<String>> {
    override fun evaluate(lhs: Set<String>, rhs: List<String>): Boolean = lhs.none { it in rhs }
}

// ============================================================================
// Boolean Operators
// ============================================================================

sealed interface BooleanOperator : RuleOperator<Boolean, Boolean>

data object BooleanEquals : BooleanOperator {
    override fun evaluate(lhs: Boolean, rhs: Boolean): Boolean = lhs == rhs
}

data object BooleanNotEquals : BooleanOperator {
    override fun evaluate(lhs: Boolean, rhs: Boolean): Boolean = lhs != rhs
}
