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

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * Parser for Vegas rules from JSON format.
 * Uses the registry to look up SourceKeys and construct typed Rule instances.
 *
 * @param C The specific VegasQueryDataSource implementation
 * @property registry The registry used to resolve string-based key references
 */
class VegasRuleParser<C : VegasQueryDataSource>(
    val registry: VegasSourceKeyRegistry<C>
) {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Parses a JSON string into a list of Rules.
     *
     * Expected JSON formats:
     * ```json
     * {
     *   "commonRulesV2": [
     *     {
     *       "operator": "intGreaterThanOrEqualTo",
     *       "lhs": {
     *         "source": "user",
     *         "key": "day",
     *         "default": 0
     *       },
     *       "rhs": 8705
     *     }
     *   ]
     * }
     * ```
     *
     * Or a direct rules array:
     * ```json
     * [
     *   {
     *     "operator": "equals",
     *     "lhs": {
     *       "source": "user",
     *       "key": "day",
     *       "default": 0
     *     },
     *     "rhs": 42
     *   }
     * ]
     * ```
     *
     * @param jsonString The JSON string to parse
     * @return List of parsed Rules
     * @throws IllegalArgumentException if the JSON format is invalid
     */
    fun parse(jsonString: String): List<Rule<C, *, *, *>> {
        val element = json.parseToJsonElement(jsonString)
        val rulesArray = when (element) {
            is JsonArray -> element
            is JsonObject -> findRulesArray(element)
            else -> throw IllegalArgumentException("Expected JSON array or object with rules array")
        }

        return rulesArray.mapNotNull { ruleElement ->
            parseRule(ruleElement.jsonObject)
        }
    }

    @Suppress("UNCHECKED_CAST", "RedundantElseInWhen")
    private fun parseRule(ruleJson: JsonObject): Rule<C, *, *, *>? {
        val lhsObject = ruleJson["lhs"]?.jsonObject
        val sourceNameRaw = lhsObject?.get("source")?.jsonPrimitive?.content
            ?: ruleJson["source"]?.jsonPrimitive?.content
            ?: throw IllegalArgumentException("Rule missing 'source' field")
        val keyNameRaw = lhsObject?.get("key")?.jsonPrimitive?.content
            ?: ruleJson["key"]?.jsonPrimitive?.content
            ?: throw IllegalArgumentException("Rule missing 'key' field")
        val operatorName = ruleJson["operator"]?.jsonPrimitive?.content
            ?: throw IllegalArgumentException("Rule missing 'operator' field")
        val valueElement = ruleJson["rhs"]
            ?: ruleJson["value"]
            ?: throw IllegalArgumentException("Rule missing 'rhs' field")
        val defaultElement = lhsObject?.get("default") ?: ruleJson["default"]
        val sourceName = normalizeSourceName(sourceNameRaw)
        val keyName = normalizeKeyName(keyNameRaw)

        // Look up the key from the registry
        val sourceKey = registry.findKey(sourceName, keyName)
            ?: return null // Key not found, skip this rule

        val source = resolveSource(sourceName)

        // Determine the type and create the appropriate rule
        return when (sourceKey) {
            is IntSourceKey<*, *> -> createIntRule(
                sourceKey as SourceKey<C, QuerySource, Int>,
                operatorName,
                valueElement,
                defaultElement,
                source
            )
            is LongSourceKey<*, *> -> createLongRule(
                sourceKey as SourceKey<C, QuerySource, Long>,
                operatorName,
                valueElement,
                defaultElement,
                source
            )
            is DoubleSourceKey<*, *> -> createDoubleRule(
                sourceKey as SourceKey<C, QuerySource, Double>,
                operatorName,
                valueElement,
                defaultElement,
                source
            )
            is StringSourceKey<*, *> -> createStringRule(
                sourceKey as SourceKey<C, QuerySource, String>,
                operatorName,
                valueElement,
                defaultElement,
                source
            )
            is BooleanSourceKey<*, *> -> createBooleanRule(
                sourceKey as SourceKey<C, QuerySource, Boolean>,
                operatorName,
                valueElement,
                defaultElement,
                source
            )
            is StringSetSourceKey<*, *> -> createStringSetRule(
                sourceKey as SourceKey<C, QuerySource, Set<String>>,
                operatorName,
                valueElement,
                defaultElement,
                source
            )
        }
    }

    private fun createIntRule(
        key: SourceKey<C, QuerySource, Int>,
        operatorName: String,
        valueElement: JsonElement,
        defaultElement: JsonElement?,
        source: QuerySource
    ): Rule<C, QuerySource, Int, Int> {
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

    private fun createLongRule(
        key: SourceKey<C, QuerySource, Long>,
        operatorName: String,
        valueElement: JsonElement,
        defaultElement: JsonElement?,
        source: QuerySource
    ): Rule<C, QuerySource, Long, Long> {
        val operator = parseLongOperator(operatorName)
        val value = valueElement.jsonPrimitive.longOrNull
            ?: throw IllegalArgumentException("Expected long value for long rule")
        val default = defaultElement?.jsonPrimitive?.longOrNull

        return Rule(
            operator = operator,
            rhs = value,
            lhs = RuleQuery(source, key, default)
        )
    }

    private fun createDoubleRule(
        key: SourceKey<C, QuerySource, Double>,
        operatorName: String,
        valueElement: JsonElement,
        defaultElement: JsonElement?,
        source: QuerySource
    ): Rule<C, QuerySource, Double, Double> {
        val operator = parseDoubleOperator(operatorName)
        val value = valueElement.jsonPrimitive.doubleOrNull
            ?: throw IllegalArgumentException("Expected double value for double rule")
        val default = defaultElement?.jsonPrimitive?.doubleOrNull

        return Rule(
            operator = operator,
            rhs = value,
            lhs = RuleQuery(source, key, default)
        )
    }

    private fun createStringRule(
        key: SourceKey<C, QuerySource, String>,
        operatorName: String,
        valueElement: JsonElement,
        defaultElement: JsonElement?,
        source: QuerySource
    ): Rule<C, QuerySource, *, *> {
        val operator = parseStringOperator(operatorName)
        val default = defaultElement?.jsonPrimitive?.content

        return if (operator == StringSetAnyMatch) {
            val values = valueElement.jsonArray.map { it.jsonPrimitive.content }
            Rule(
                operator = StringSetAnyMatch,
                rhs = values,
                lhs = RuleQuery(source, key, default)
            )
        } else {
            val value = valueElement.jsonPrimitive.content
            @Suppress("UNCHECKED_CAST")
            val typedOperator = operator as StringOperator<String>
            Rule(
                operator = typedOperator,
                rhs = value,
                lhs = RuleQuery(source, key, default)
            )
        }
    }

    private fun createBooleanRule(
        key: SourceKey<C, QuerySource, Boolean>,
        operatorName: String,
        valueElement: JsonElement,
        defaultElement: JsonElement?,
        source: QuerySource
    ): Rule<C, QuerySource, Boolean, Boolean> {
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
        key: SourceKey<C, QuerySource, Set<String>>,
        operatorName: String,
        valueElement: JsonElement,
        defaultElement: JsonElement?,
        source: QuerySource
    ): Rule<C, QuerySource, Set<String>, List<String>> {
        // StringSet rules use SetStringAnyMatch operator
        val values = valueElement.jsonArray.map { it.jsonPrimitive.content }
        val default = defaultElement?.jsonArray?.map { it.jsonPrimitive.content }?.toSet()

        return Rule(
            operator = SetStringAnyMatch,
            rhs = values,
            lhs = RuleQuery(source, key, default)
        )
    }

    private fun parseIntOperator(name: String): IntOperator {
        val normalized = name.lowercase().removePrefix("int")
        return when (normalized) {
            "equals", "eq", "==" -> IntEquals
            "notequals", "neq", "!=" -> IntNotEquals
            "greaterthan", "gt", ">" -> IntGreaterThan
            "greaterthanorequalto", "greaterthanorequals", "gte", ">=" -> IntGreaterThanOrEquals
            "lessthan", "lt", "<" -> IntLessThan
            "lessthanorequalto", "lessthanorequals", "lte", "<=" -> IntLessThanOrEquals
            else -> throw IllegalArgumentException("Unknown int operator: $name")
        }
    }

    private fun parseLongOperator(name: String): LongOperator {
        val normalized = name.lowercase().removePrefix("long")
        return when (normalized) {
            "equals", "eq", "==" -> LongEquals
            "notequals", "neq", "!=" -> LongNotEquals
            "greaterthan", "gt", ">" -> LongGreaterThan
            "greaterthanorequalto", "greaterthanorequals", "gte", ">=" -> LongGreaterThanOrEquals
            "lessthan", "lt", "<" -> LongLessThan
            "lessthanorequalto", "lessthanorequals", "lte", "<=" -> LongLessThanOrEquals
            else -> throw IllegalArgumentException("Unknown long operator: $name")
        }
    }

    private fun parseDoubleOperator(name: String): DoubleOperator {
        val normalized = name.lowercase().removePrefix("double")
        return when (normalized) {
            "equals", "eq", "==" -> DoubleEquals
            "notequals", "neq", "!=" -> DoubleNotEquals
            "greaterthan", "gt", ">" -> DoubleGreaterThan
            "greaterthanorequalto", "greaterthanorequals", "gte", ">=" -> DoubleGreaterThanOrEquals
            "lessthan", "lt", "<" -> DoubleLessThan
            "lessthanorequalto", "lessthanorequals", "lte", "<=" -> DoubleLessThanOrEquals
            else -> throw IllegalArgumentException("Unknown double operator: $name")
        }
    }

    private fun parseStringOperator(name: String): StringOperator<*> = when (name.lowercase()) {
        "equals", "eq", "==" -> StringEquals
        "notequals", "neq", "!=" -> StringNotEquals
        "contains" -> StringContains
        "startswith" -> StringStartsWith
        "endswith" -> StringEndsWith
        "equalsignorecase" -> StringEqualsIgnoreCase
        "stringsetanymatch" -> StringSetAnyMatch
        else -> throw IllegalArgumentException("Unknown string operator: $name")
    }

    private fun parseBooleanOperator(name: String): BooleanOperator = when (name.lowercase()) {
        "equals", "eq", "==", "bool", "boolean" -> BooleanEquals
        "notequals", "neq", "!=", "not" -> BooleanNotEquals
        else -> throw IllegalArgumentException("Unknown boolean operator: $name")
    }

    private fun findRulesArray(root: JsonObject): JsonArray {
        val rules = root["commonRulesV2"] ?: root["rulesV2"] ?: root["rules"]
        return rules?.jsonArray
            ?: throw IllegalArgumentException("Expected rules array in JSON object")
    }

    private fun normalizeSourceName(sourceName: String): String {
        return when (sourceName) {
            "user" -> "User"
            "history" -> "History"
            "promotionHistory" -> "History"
            else -> sourceName.replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase() else char.toString()
            }
        }
    }

    private fun normalizeKeyName(keyName: String): String {
        if (keyName.contains("_")) {
            return keyName
        }
        return keyName.replace(Regex("([a-z0-9])([A-Z])"), "$1_$2").lowercase()
    }

    private fun resolveSource(sourceName: String): QuerySource {
        return registry.findSource(sourceName)
            ?: throw IllegalArgumentException("Unknown source: $sourceName")
    }
}
