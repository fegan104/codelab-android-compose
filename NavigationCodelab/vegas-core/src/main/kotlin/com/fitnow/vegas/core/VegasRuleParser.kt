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
import kotlinx.serialization.json.JsonPrimitive
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
     * Expected JSON format:
     * ```json
     * {
     *   "rules": [
     *     {
     *       "source": "sourceName",
     *       "key": "keyName",
     *       "operator": "equals",
     *       "value": 42,
     *       "default": 0
     *     }
     *   ]
     * }
     * ```
     *
     * Or as a simple array:
     * ```json
     * [
     *   {
     *     "source": "sourceName",
     *     "key": "keyName",
     *     "operator": "equals",
     *     "value": 42
     *   }
     * ]
     * ```
     *
     * @param jsonString The JSON string to parse
     * @return List of parsed Rules
     * @throws IllegalArgumentException if the JSON format is invalid
     */
    fun parse(jsonString: String): List<Rule<C, *, *>> {
        val element = json.parseToJsonElement(jsonString)
        val rulesArray = when (element) {
            is JsonArray -> element
            is JsonObject -> element["rules"]?.jsonArray
                ?: throw IllegalArgumentException("Expected 'rules' array in JSON object")
            else -> throw IllegalArgumentException("Expected JSON array or object with 'rules' array")
        }

        return rulesArray.mapNotNull { ruleElement ->
            parseRule(ruleElement.jsonObject)
        }
    }

    @Suppress("UNCHECKED_CAST", "RedundantElseInWhen")
    private fun parseRule(ruleJson: JsonObject): Rule<C, *, *>? {
        val sourceName = ruleJson["source"]?.jsonPrimitive?.content
            ?: throw IllegalArgumentException("Rule missing 'source' field")
        val keyName = ruleJson["key"]?.jsonPrimitive?.content
            ?: throw IllegalArgumentException("Rule missing 'key' field")
        val operatorName = ruleJson["operator"]?.jsonPrimitive?.content
            ?: throw IllegalArgumentException("Rule missing 'operator' field")
        val valueElement = ruleJson["value"]
            ?: throw IllegalArgumentException("Rule missing 'value' field")
        val defaultElement = ruleJson["default"]

        // Look up the key from the registry
        val sourceKey = registry.findKey(sourceName, keyName)
            ?: return null // Key not found, skip this rule

        // Determine the type and create the appropriate rule
        return when (sourceKey) {
            is IntSourceKey<*, *> -> createIntRule(
                sourceKey as SourceKey<C, QuerySource, Int>,
                operatorName,
                valueElement,
                defaultElement,
                sourceName
            )
            is LongSourceKey<*, *> -> createLongRule(
                sourceKey as SourceKey<C, QuerySource, Long>,
                operatorName,
                valueElement,
                defaultElement,
                sourceName
            )
            is DoubleSourceKey<*, *> -> createDoubleRule(
                sourceKey as SourceKey<C, QuerySource, Double>,
                operatorName,
                valueElement,
                defaultElement,
                sourceName
            )
            is StringSourceKey<*, *> -> createStringRule(
                sourceKey as SourceKey<C, QuerySource, String>,
                operatorName,
                valueElement,
                defaultElement,
                sourceName
            )
            is BooleanSourceKey<*, *> -> createBooleanRule(
                sourceKey as SourceKey<C, QuerySource, Boolean>,
                operatorName,
                valueElement,
                defaultElement,
                sourceName
            )
            else -> null // Unknown key type
        }
    }

    private fun createIntRule(
        key: SourceKey<C, QuerySource, Int>,
        operatorName: String,
        valueElement: JsonElement,
        defaultElement: JsonElement?,
        sourceName: String
    ): Rule<C, QuerySource, Int> {
        val operator = parseIntOperator(operatorName)
        val value = valueElement.jsonPrimitive.intOrNull
            ?: throw IllegalArgumentException("Expected integer value for int rule")
        val default = defaultElement?.jsonPrimitive?.intOrNull
        val source = createPlaceholderSource(sourceName)

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
        sourceName: String
    ): Rule<C, QuerySource, Long> {
        val operator = parseLongOperator(operatorName)
        val value = valueElement.jsonPrimitive.longOrNull
            ?: throw IllegalArgumentException("Expected long value for long rule")
        val default = defaultElement?.jsonPrimitive?.longOrNull
        val source = createPlaceholderSource(sourceName)

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
        sourceName: String
    ): Rule<C, QuerySource, Double> {
        val operator = parseDoubleOperator(operatorName)
        val value = valueElement.jsonPrimitive.doubleOrNull
            ?: throw IllegalArgumentException("Expected double value for double rule")
        val default = defaultElement?.jsonPrimitive?.doubleOrNull
        val source = createPlaceholderSource(sourceName)

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
        sourceName: String
    ): Rule<C, QuerySource, String> {
        val operator = parseStringOperator(operatorName)
        val value = valueElement.jsonPrimitive.content
        val default = defaultElement?.jsonPrimitive?.content
        val source = createPlaceholderSource(sourceName)

        return Rule(
            operator = operator,
            rhs = value,
            lhs = RuleQuery(source, key, default)
        )
    }

    private fun createBooleanRule(
        key: SourceKey<C, QuerySource, Boolean>,
        operatorName: String,
        valueElement: JsonElement,
        defaultElement: JsonElement?,
        sourceName: String
    ): Rule<C, QuerySource, Boolean> {
        val operator = parseBooleanOperator(operatorName)
        val value = valueElement.jsonPrimitive.booleanOrNull
            ?: throw IllegalArgumentException("Expected boolean value for boolean rule")
        val default = defaultElement?.jsonPrimitive?.booleanOrNull
        val source = createPlaceholderSource(sourceName)

        return Rule(
            operator = operator,
            rhs = value,
            lhs = RuleQuery(source, key, default)
        )
    }

    private fun parseIntOperator(name: String): IntOperator = when (name.lowercase()) {
        "equals", "eq", "==" -> IntEquals
        "notequals", "neq", "!=" -> IntNotEquals
        "greaterthan", "gt", ">" -> IntGreaterThan
        "greaterthanorequals", "gte", ">=" -> IntGreaterThanOrEquals
        "lessthan", "lt", "<" -> IntLessThan
        "lessthanorequals", "lte", "<=" -> IntLessThanOrEquals
        else -> throw IllegalArgumentException("Unknown int operator: $name")
    }

    private fun parseLongOperator(name: String): LongOperator = when (name.lowercase()) {
        "equals", "eq", "==" -> LongEquals
        "notequals", "neq", "!=" -> LongNotEquals
        "greaterthan", "gt", ">" -> LongGreaterThan
        "greaterthanorequals", "gte", ">=" -> LongGreaterThanOrEquals
        "lessthan", "lt", "<" -> LongLessThan
        "lessthanorequals", "lte", "<=" -> LongLessThanOrEquals
        else -> throw IllegalArgumentException("Unknown long operator: $name")
    }

    private fun parseDoubleOperator(name: String): DoubleOperator = when (name.lowercase()) {
        "equals", "eq", "==" -> DoubleEquals
        "notequals", "neq", "!=" -> DoubleNotEquals
        "greaterthan", "gt", ">" -> DoubleGreaterThan
        "greaterthanorequals", "gte", ">=" -> DoubleGreaterThanOrEquals
        "lessthan", "lt", "<" -> DoubleLessThan
        "lessthanorequals", "lte", "<=" -> DoubleLessThanOrEquals
        else -> throw IllegalArgumentException("Unknown double operator: $name")
    }

    private fun parseStringOperator(name: String): StringOperator = when (name.lowercase()) {
        "equals", "eq", "==" -> StringEquals
        "notequals", "neq", "!=" -> StringNotEquals
        "contains" -> StringContains
        "startswith" -> StringStartsWith
        "endswith" -> StringEndsWith
        "equalsignorecase" -> StringEqualsIgnoreCase
        else -> throw IllegalArgumentException("Unknown string operator: $name")
    }

    private fun parseBooleanOperator(name: String): BooleanOperator = when (name.lowercase()) {
        "equals", "eq", "==" -> BooleanEquals
        "notequals", "neq", "!=" -> BooleanNotEquals
        else -> throw IllegalArgumentException("Unknown boolean operator: $name")
    }

    /**
     * Creates a placeholder QuerySource for the given source name.
     * The actual source resolution happens through the SourceKey.
     */
    private fun createPlaceholderSource(sourceName: String): QuerySource {
        return object : QuerySource {
            override fun toString(): String = "QuerySource($sourceName)"
        }
    }
}
