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
class VegasRuleParser<C : VegasQueryDataSource>(
    val registry: VegasSourceKeyRegistry<C>
) {
    internal val json = Json { ignoreUnknownKeys = true }

    /**
     * Parses a JsonArray of rules into a list of Rule objects.
     * Exposed for use by VegasPromotionGroupParser.
     *
     * @param rulesArray The JSON array containing rule definitions
     * @return List of parsed Rules (rules with unknown keys are skipped)
     */
    internal fun parseRulesArray(rulesArray: JsonArray): List<Rule<C, *, *, *>> {
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
        val whereObject = lhsObject?.get("where")?.jsonObject
        val sourceName = normalizeSourceName(sourceNameRaw)
        val keyName = normalizeKeyName(keyNameRaw)

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
            is IntSourceKey<*, *> -> createIntRule(
                sourceKey as SourceKey<C, QuerySource, Int>,
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

    private fun createStringRule(
        key: SourceKey<C, QuerySource, String>,
        operatorName: String,
        valueElement: JsonElement,
        defaultElement: JsonElement?,
        source: QuerySource
    ): Rule<C, QuerySource, String, String> {
        val operator = parseStringOperator(operatorName)
        val value = valueElement.jsonPrimitive.content
        val default = defaultElement?.jsonPrimitive?.content

        @Suppress("UNCHECKED_CAST")
        val typedOperator = operator as StringOperator<String>
        return Rule(
            operator = typedOperator,
            rhs = value,
            lhs = RuleQuery(source, key, default)
        )
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
    ): Rule<C, QuerySource, Set<String>, Set<String>> {
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

    private fun parseStringOperator(name: String): StringOperator<*> = when (name) {
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

    private fun parseStringSetOperator(name: String): SetStringOperator<Set<String>> = when (name) {
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

    private fun findRulesArray(root: JsonObject): JsonArray {
        val rules = root["commonRulesV2"] ?: root["rulesV2"] ?: root["rules"]
        return rules?.jsonArray
            ?: throw IllegalArgumentException("Expected rules array in JSON object")
    }

    private fun normalizeSourceName(sourceName: String): String {
        // Pass through as-is - registry should use the raw JSON source names
        return sourceName
    }

    private fun normalizeKeyName(keyName: String): String {
        // Pass through as-is - registry should use the raw JSON key names
        return keyName
    }

    private fun resolveSource(sourceName: String): QuerySource {
        return registry.findSource(sourceName)
            ?: throw IllegalArgumentException("Unknown source: $sourceName")
    }
}
