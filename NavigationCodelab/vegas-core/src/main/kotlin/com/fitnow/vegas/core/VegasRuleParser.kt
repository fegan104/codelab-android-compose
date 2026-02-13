package com.fitnow.vegas.core

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

/**
 * Parser for Vegas rules from JSON format.
 * Uses the registry to look up SourceKeys and construct typed Rule instances.
 *
 * @param D The specific VegasQueryDataSource implementation
 * @property registry The registry used to resolve string-based key references
 */
internal class VegasRuleParser<D : QueryDataSource>(
    val registry: SourceKeyParser<D>
) {
    /**
     * Parses a list of RuleJson objects into a list of typed Rule objects.
     * Exposed for use by VegasPromotionGroupParser.
     *
     * @param rules The list of RuleJson objects to parse
     * @return List of parsed Rules (rules with unknown keys are skipped)
     */
    internal fun parseRules(rules: List<RuleJson>): List<Rule<D, *>> {
        return rules.mapNotNull { ruleJson ->
            parseRule(ruleJson)
        }
    }

    private fun parseRule(ruleJson: RuleJson): Rule<D, *>? {
        val lhs = ruleJson.lhs
        val sourceName = lhs.source
        val keyName = lhs.key
        val operatorName = ruleJson.operator
        val valueElement = ruleJson.rhs
        val defaultElement = lhs.default
        val whereObject = lhs.where

        // Look up the key from the registry - use createKeyWithWhere if where clause exists
        val sourceKey = if (whereObject != null) {
            val whereParams = whereObject.entries.associate { (k, v) ->
                k to v.jsonPrimitive.content
            }
            registry.createKeyWithWhere(sourceName, keyName, whereParams)
        } else {
            registry.findKey(sourceName, keyName)
        } ?: return null // Key not found, skip this rule

        // Determine the type and create the appropriate rule
        return when (sourceKey) {
            is IntSourceKey -> {
                createRule(
                    key = sourceKey,
                    operatorName = operatorName,
                    valueElement = valueElement,
                    defaultElement = defaultElement,
                    parseOp = ::parseIntOperator,
                    extractValue = { it.jsonPrimitive.int },
                    extractDefault = { it.jsonPrimitive.intOrNull },
                )
            }

            is StringSourceKey -> {
                createRule(
                    key = sourceKey,
                    operatorName = operatorName,
                    valueElement = valueElement,
                    defaultElement = defaultElement,
                    parseOp = ::parseStringOperator,
                    extractValue = { it.jsonPrimitive.content },
                    extractDefault = { it.jsonPrimitive.content },
                )
            }

            is BooleanSourceKey -> {
                createRule(
                    key = sourceKey,
                    operatorName = operatorName,
                    valueElement = valueElement,
                    defaultElement = defaultElement,
                    parseOp = ::parseBooleanOperator,
                    extractValue = { it.jsonPrimitive.boolean },
                    extractDefault = { it.jsonPrimitive.booleanOrNull },
                )
            }

            is StringSetSourceKey -> {
                createRule(
                    key = sourceKey,
                    operatorName = operatorName,
                    valueElement = valueElement,
                    defaultElement = defaultElement,
                    parseOp = ::parseStringSetOperator,
                    extractValue = { it.jsonArray.map { e -> e.jsonPrimitive.content }.toSet() },
                    extractDefault = { it.jsonArray.map { e -> e.jsonPrimitive.content }.toSet() }
                )
            }
        }
    }

    private fun <T, O> createRule(
        key: SourceKey<D, T>,
        operatorName: String,
        valueElement: JsonElement,
        defaultElement: JsonElement?,
        parseOp: (String) -> O,
        extractValue: (JsonElement) -> T,
        extractDefault: (JsonElement) -> T?
    ): Rule<D, T> where O : RuleOperator<T> {
        return Rule(
            operator = parseOp(operatorName),
            rhs = extractValue(valueElement),
            lhs = RuleQuery(key, defaultElement?.let { extractDefault(it) })
        )
    }

    private fun parseIntOperator(name: String): IntOperator = when (name) {
        "intEquals" -> IntEquals
        "intNotEquals" -> IntNotEquals
        "intLessThan" -> IntLessThan
        "intLessThanOrEqualTo" -> IntLessThanOrEquals
        "intGreaterThan" -> IntGreaterThan
        "intGreaterThanOrEqualTo" -> IntGreaterThanOrEquals
        else -> throw IllegalArgumentException("Unknown int operator: $name")
    }

    private fun parseStringOperator(name: String): StringOperator = when (name) {
        "stringEquals" -> StringEquals
        "stringNotEquals" -> StringNotEquals
        "stringContains" -> StringContains
        "stringNotContains" -> StringNotContains
        else -> throw IllegalArgumentException("Unknown string operator: $name")
    }

    private fun parseBooleanOperator(name: String): BooleanOperator = when (name) {
        "bool" -> BooleanEquals
        else -> throw IllegalArgumentException("Unknown boolean operator: $name")
    }

    private fun parseStringSetOperator(name: String): SetStringOperator = when (name) {
        "stringSetEquivalent" -> StringSetEquivalent
        "stringSetNotEquivalent" -> StringSetNotEquivalent
        "stringSetIsSubset" -> StringSetIsSubset
        "stringSetNotIsSubset" -> StringSetNotIsSubset
        "stringSetIsSuperset" -> StringSetIsSuperset
        "stringSetNotIsSuperset" -> StringSetNotIsSuperset
        "stringSetAnyMatch" -> StringSetAnyMatch
        "stringSetNotAnyMatch" -> StringSetNotAnyMatch
        else -> throw IllegalArgumentException("Unknown string set operator: $name")
    }
}
