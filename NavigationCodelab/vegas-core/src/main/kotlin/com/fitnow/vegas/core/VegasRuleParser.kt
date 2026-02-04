package com.fitnow.vegas.core

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Parser for Vegas rules from JSON format.
 * Uses the registry to look up SourceKeys and construct typed Rule instances.
 *
 * @param C The specific VegasQueryDataSource implementation
 * @property registry The registry used to resolve string-based key references
 */
class VegasRuleParser<C : QueryDataSource>(
    val registry: VegasSourceKeyRegistry<C>
) {
    /**
     * Parses a JsonArray of rules into a list of Rule objects.
     * Exposed for use by VegasPromotionGroupParser.
     *
     * @param rulesArray The JSON array containing rule definitions
     * @return List of parsed Rules (rules with unknown keys are skipped)
     */
    internal fun parseRulesArray(rulesArray: JsonArray): List<Rule<C, *, *>> {
        return rulesArray.mapNotNull { ruleElement ->
            parseRule(ruleElement.jsonObject)
        }
    }

    private fun parseRule(ruleJson: JsonObject): Rule<C, *, *>? {
        val lhsObject = ruleJson["lhs"]?.jsonObject
            ?: throw IllegalArgumentException("Rule missing 'lhs' field")
        val sourceName = lhsObject["source"]?.jsonPrimitive?.content
            ?: throw IllegalArgumentException("Rule missing 'source' field")
        val keyName = lhsObject["key"]?.jsonPrimitive?.content
            ?: throw IllegalArgumentException("Rule missing 'key' field")
        val operatorName = ruleJson["operator"]?.jsonPrimitive?.content
            ?: throw IllegalArgumentException("Rule missing 'operator' field")
        val valueElement = ruleJson["rhs"] ?: throw IllegalArgumentException("Rule missing 'rhs' field")
        val defaultElement = lhsObject["default"] ?: ruleJson["default"]
        val whereObject = lhsObject["where"]?.jsonObject

        // Look up the key from the registry - use createKeyWithWhere if where clause exists
        val sourceKey: SourceKey<C, *> = if (whereObject != null) {
            val whereParams = whereObject.entries.associate { (k, v) ->
                k to v.jsonPrimitive.content
            }
            registry.createKeyWithWhere(sourceName, keyName, whereParams)
        } else {
            registry.findKey(sourceName, keyName)
        } ?: return null // Key not found, skip this rule

        val source = resolveSource(sourceName)

        // Determine the type and create the appropriate rule
        return when (sourceKey) {
            is IntSourceKey<*> -> createIntRule(
                sourceKey as IntSourceKey<C>,
                operatorName,
                valueElement,
                defaultElement,
                source
            )

            is StringSourceKey<*> -> createStringRule(
                sourceKey as StringSourceKey<C>,
                operatorName,
                valueElement,
                defaultElement,
                source
            )

            is BooleanSourceKey<*> -> createBooleanRule(
                sourceKey as BooleanSourceKey<C>,
                operatorName,
                valueElement,
                defaultElement,
                source
            )

            is StringSetSourceKey<*> -> createStringSetRule(
                sourceKey as StringSetSourceKey<C>,
                operatorName,
                valueElement,
                defaultElement,
                source
            )
        }
    }

    private fun createIntRule(
        key: IntSourceKey<C>,
        operatorName: String,
        valueElement: JsonElement,
        defaultElement: JsonElement?,
        source: QuerySource
    ): Rule<C, QuerySource, Int> {
        val operator = parseIntOperator(operatorName)
        val value = valueElement.jsonPrimitive.intOrNull
            ?: throw IllegalArgumentException("Expected integer value for int rule")
        val default = defaultElement?.jsonPrimitive?.intOrNull

        return Rule(
            operator = operator,
            rhs = value,
            lhs = RuleQuery(source, key, default)
        )
    }

    private fun createStringRule(
        key: StringSourceKey<C>,
        operatorName: String,
        valueElement: JsonElement,
        defaultElement: JsonElement?,
        source: QuerySource
    ): Rule<C, QuerySource, String> {
        val operator = parseStringOperator(operatorName)
        val value = valueElement.jsonPrimitive.content
        val default = defaultElement?.jsonPrimitive?.content

        @Suppress("UNCHECKED_CAST")
        val typedOperator = operator
        return Rule(
            operator = typedOperator,
            rhs = value,
            lhs = RuleQuery(source, key, default)
        )
    }

    private fun createBooleanRule(
        key: BooleanSourceKey<C>,
        operatorName: String,
        valueElement: JsonElement,
        defaultElement: JsonElement?,
        source: QuerySource
    ): Rule<C, QuerySource, Boolean> {
        val operator = parseBooleanOperator(operatorName)
        val value = valueElement.jsonPrimitive.booleanOrNull
            ?: throw IllegalArgumentException("Expected boolean value for boolean rule")
        val default = defaultElement?.jsonPrimitive?.booleanOrNull

        return Rule(
            operator = operator,
            rhs = value,
            lhs = RuleQuery(source, key, default)
        )
    }

    private fun createStringSetRule(
        key: StringSetSourceKey<C>,
        operatorName: String,
        valueElement: JsonElement,
        defaultElement: JsonElement?,
        source: QuerySource
    ): Rule<C, QuerySource, Set<String>> {
        val operator = parseStringSetOperator(operatorName)
        val values = valueElement.jsonArray.map { it.jsonPrimitive.content }.toSet()
        val default = defaultElement?.jsonArray?.map { it.jsonPrimitive.content }?.toSet()

        return Rule(
            operator = operator,
            rhs = values,
            lhs = RuleQuery(source, key, default)
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

    private fun resolveSource(sourceName: String): QuerySource {
        return registry.findSource(sourceName)
            ?: throw IllegalArgumentException("Unknown source: $sourceName")
    }
}
