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
 * @param T The type of values being compared
 */
sealed interface RuleOperator<T> {
    /**
     * Evaluates the comparison between left-hand side and right-hand side values.
     *
     * @param lhs The left-hand side value (resolved from the data source)
     * @param rhs The right-hand side value (from the rule definition)
     * @return true if the comparison holds, false otherwise
     */
    fun evaluate(lhs: T, rhs: T): Boolean
}

// ============================================================================
// Int Operators
// ============================================================================

sealed interface IntOperator : RuleOperator<Int>

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
// Long Operators
// ============================================================================

sealed interface LongOperator : RuleOperator<Long>

data object LongEquals : LongOperator {
    override fun evaluate(lhs: Long, rhs: Long): Boolean = lhs == rhs
}

data object LongNotEquals : LongOperator {
    override fun evaluate(lhs: Long, rhs: Long): Boolean = lhs != rhs
}

data object LongGreaterThan : LongOperator {
    override fun evaluate(lhs: Long, rhs: Long): Boolean = lhs > rhs
}

data object LongGreaterThanOrEquals : LongOperator {
    override fun evaluate(lhs: Long, rhs: Long): Boolean = lhs >= rhs
}

data object LongLessThan : LongOperator {
    override fun evaluate(lhs: Long, rhs: Long): Boolean = lhs < rhs
}

data object LongLessThanOrEquals : LongOperator {
    override fun evaluate(lhs: Long, rhs: Long): Boolean = lhs <= rhs
}

// ============================================================================
// Double Operators
// ============================================================================

sealed interface DoubleOperator : RuleOperator<Double>

data object DoubleEquals : DoubleOperator {
    override fun evaluate(lhs: Double, rhs: Double): Boolean = lhs == rhs
}

data object DoubleNotEquals : DoubleOperator {
    override fun evaluate(lhs: Double, rhs: Double): Boolean = lhs != rhs
}

data object DoubleGreaterThan : DoubleOperator {
    override fun evaluate(lhs: Double, rhs: Double): Boolean = lhs > rhs
}

data object DoubleGreaterThanOrEquals : DoubleOperator {
    override fun evaluate(lhs: Double, rhs: Double): Boolean = lhs >= rhs
}

data object DoubleLessThan : DoubleOperator {
    override fun evaluate(lhs: Double, rhs: Double): Boolean = lhs < rhs
}

data object DoubleLessThanOrEquals : DoubleOperator {
    override fun evaluate(lhs: Double, rhs: Double): Boolean = lhs <= rhs
}

// ============================================================================
// String Operators
// ============================================================================

sealed interface StringOperator : RuleOperator<String>

data object StringEquals : StringOperator {
    override fun evaluate(lhs: String, rhs: String): Boolean = lhs == rhs
}

data object StringNotEquals : StringOperator {
    override fun evaluate(lhs: String, rhs: String): Boolean = lhs != rhs
}

data object StringContains : StringOperator {
    override fun evaluate(lhs: String, rhs: String): Boolean = lhs.contains(rhs)
}

data object StringStartsWith : StringOperator {
    override fun evaluate(lhs: String, rhs: String): Boolean = lhs.startsWith(rhs)
}

data object StringEndsWith : StringOperator {
    override fun evaluate(lhs: String, rhs: String): Boolean = lhs.endsWith(rhs)
}

data object StringEqualsIgnoreCase : StringOperator {
    override fun evaluate(lhs: String, rhs: String): Boolean = lhs.equals(rhs, ignoreCase = true)
}

// ============================================================================
// Boolean Operators
// ============================================================================

sealed interface BooleanOperator : RuleOperator<Boolean>

data object BooleanEquals : BooleanOperator {
    override fun evaluate(lhs: Boolean, rhs: Boolean): Boolean = lhs == rhs
}

data object BooleanNotEquals : BooleanOperator {
    override fun evaluate(lhs: Boolean, rhs: Boolean): Boolean = lhs != rhs
}
