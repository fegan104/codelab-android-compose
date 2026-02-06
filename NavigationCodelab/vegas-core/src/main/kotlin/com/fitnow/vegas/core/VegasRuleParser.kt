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
 * @param D The specific VegasQueryDataSource implementation
 * @property registry The registry used to resolve string-based key references
 */
internal class VegasRuleParser<D : QueryDataSource>(
    val registry: VegasSourceKeyRegistry<D>
) {
    /**
     * Parses a JsonArray of rules into a list of Rule objects.
     * Exposed for use by VegasPromotionGroupParser.
     *
     * @param rulesArray The JSON array containing rule definitions
     * @return List of parsed Rules (rules with unknown keys are skipped)
     */
    internal fun parseRulesArray(rulesArray: JsonArray): List<Rule<D, *, *>> {
        return rulesArray.mapNotNull { ruleElement ->
            parseRule(ruleElement.jsonObject)
        }
    }

    private fun parseRule(ruleJson: JsonObject): Rule<D, *, *>? {
        val lhsObject = ruleJson["lhs"]?.jsonObject
            ?: throw IllegalArgumentException("Rule missing 'lhs' field")
        val sourceName = lhsObject["source"]?.jsonPrimitive?.content
            ?: throw IllegalArgumentException("Rule missing 'source' field")
        val keyName = lhsObject["key"]?.jsonPrimitive?.content
            ?: throw IllegalArgumentException("Rule missing 'key' field")
        val operatorName = ruleJson["operator"]?.jsonPrimitive?.content
            ?: throw IllegalArgumentException("Rule missing 'operator' field")
        val valueElement = ruleJson["rhs"]
            ?: throw IllegalArgumentException("Rule missing 'rhs' field")
        val defaultElement = lhsObject["default"] ?: ruleJson["default"]
        val whereObject = lhsObject["where"]?.jsonObject

        // Look up the key from the registry - use createKeyWithWhere if where clause exists
        val sourceKey = if (whereObject != null) {
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
            is IntSourceKey -> createIntRule(
                sourceKey,
                operatorName,
                valueElement,
                defaultElement,
                source
            )

            is StringSourceKey -> createStringRule(
                sourceKey,
                operatorName,
                valueElement,
                defaultElement,
                source
            )

            is BooleanSourceKey -> createBooleanRule(
                sourceKey,
                operatorName,
                valueElement,
                defaultElement,
                source
            )

            is StringSetSourceKey -> createStringSetRule(
                sourceKey,
                operatorName,
                valueElement,
                defaultElement,
                source
            )
        }
    }

    private fun createIntRule(
        key: IntSourceKey<D>,
        operatorName: String,
        valueElement: JsonElement,
        defaultElement: JsonElement?,
        source: QuerySource
    ): Rule<D, QuerySource, Int> {
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
        key: StringSourceKey<D>,
        operatorName: String,
        valueElement: JsonElement,
        defaultElement: JsonElement?,
        source: QuerySource
    ): Rule<D, QuerySource, String> {
        val operator = parseStringOperator(operatorName)
        val value = valueElement.jsonPrimitive.content
        val default = defaultElement?.jsonPrimitive?.content

        return Rule(
            operator = operator,
            rhs = value,
            lhs = RuleQuery(source, key, default)
        )
    }

    private fun createBooleanRule(
        key: BooleanSourceKey<D>,
        operatorName: String,
        valueElement: JsonElement,
        defaultElement: JsonElement?,
        source: QuerySource
    ): Rule<D, QuerySource, Boolean> {
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
        key: StringSetSourceKey<D>,
        operatorName: String,
        valueElement: JsonElement,
        defaultElement: JsonElement?,
        source: QuerySource
    ): Rule<D, QuerySource, Set<String>> {
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
