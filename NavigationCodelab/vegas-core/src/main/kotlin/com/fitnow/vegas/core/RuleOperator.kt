package com.fitnow.vegas.core

/**
 * Sealed interface for rule operators that evaluate comparisons.
 *
 * @param E The type of the equation.
 */
sealed interface RuleOperator<E> {
    /**
     * Evaluates the comparison between left-hand side and right-hand side values.
     *
     * @param lhs The left-hand side value (resolved from the data source)
     * @param rhs The right-hand side value (from the rule definition)
     * @return true if the comparison holds, false otherwise
     */
    fun evaluate(lhs: E, rhs: E): Boolean
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

data object StringNotContains : StringOperator {
    override fun evaluate(lhs: String, rhs: String): Boolean = !lhs.contains(rhs)
}

// ============================================================================
// Set<String> Operators
// ============================================================================

/**
 * Operators that compare a Set<String> against some right-hand side value.
 */
sealed interface SetStringOperator : RuleOperator<Set<String>>

/**
 * Checks if the sets are equivalent (contain the same elements).
 */
data object StringSetEquivalent : SetStringOperator {
    override fun evaluate(lhs: Set<String>, rhs: Set<String>): Boolean = lhs == rhs.toSet()
}

/**
 * Checks if the sets are not equivalent.
 */
data object StringSetNotEquivalent : SetStringOperator {
    override fun evaluate(lhs: Set<String>, rhs: Set<String>): Boolean = lhs != rhs.toSet()
}

/**
 * Checks if the left-hand set is a subset of the right-hand list.
 */
data object StringSetIsSubset : SetStringOperator {
    override fun evaluate(lhs: Set<String>, rhs: Set<String>): Boolean = rhs.toSet().containsAll(lhs)
}

/**
 * Checks if the left-hand set is not a subset of the right-hand list.
 */
data object StringSetNotIsSubset : SetStringOperator {
    override fun evaluate(lhs: Set<String>, rhs: Set<String>): Boolean = !rhs.toSet().containsAll(lhs)
}

/**
 * Checks if the left-hand set is a superset of the right-hand list.
 */
data object StringSetIsSuperset : SetStringOperator {
    override fun evaluate(lhs: Set<String>, rhs: Set<String>): Boolean = lhs.containsAll(rhs)
}

/**
 * Checks if the left-hand set is not a superset of the right-hand list.
 */
data object StringSetNotIsSuperset : SetStringOperator {
    override fun evaluate(lhs: Set<String>, rhs: Set<String>): Boolean = !lhs.containsAll(rhs)
}

/**
 * Checks if any element of the left-hand set matches any element of the right-hand list.
 */
data object StringSetAnyMatch : SetStringOperator {
    override fun evaluate(lhs: Set<String>, rhs: Set<String>): Boolean = lhs.any { it in rhs }
}

/**
 * Checks if no element of the left-hand set matches any element of the right-hand list.
 */
data object StringSetNotAnyMatch : SetStringOperator {
    override fun evaluate(lhs: Set<String>, rhs: Set<String>): Boolean = lhs.none { it in rhs }
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
