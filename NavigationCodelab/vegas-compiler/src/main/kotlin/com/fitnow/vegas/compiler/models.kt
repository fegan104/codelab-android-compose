package com.fitnow.vegas.compiler

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Root model for the Vegas JSON manifest.
 * Represents a promotion group configuration.
 */
@Serializable
internal data class PromotionGroupJson(
    val id: String,
    val type: String,
    @SerialName("commonRulesV2")
    val commonRules: List<Rule> = emptyList(),
    val promotions: List<PromotionJson> = emptyList()
)

/**
 * Defines a single promotion within the manifest.
 */
@Serializable
internal data class PromotionJson(
    val id: String,
    val actionUrl: String? = null,
    @SerialName("rulesV2")
    val rules: List<Rule> = emptyList(),
    val creativeTreatments: List<CreativeTreatment> = emptyList()
)

/**
 * Defines a rule that evaluates a condition.
 */
@Serializable
internal data class Rule(
    val operator: String,
    val lhs: LhsDefinition,
    val rhs: JsonElement
)

/**
 * Defines the left-hand side of a rule (the data source reference).
 */
@Serializable
internal data class LhsDefinition(
    val source: String,
    val key: String,
    val where: JsonObject? = null,
    val default: JsonElement? = null
)

/**
 * Defines a creative treatment for a promotion.
 */
@Serializable
internal data class CreativeTreatment(
    val id: String,
    val heroImageUrl: String? = null,
    val titleText: String? = null,
    val bodyText: String? = null,
    val actionText: String? = null,
    val buttonText: String? = null,
    val noThanksText: String? = null,
    val weight: Int = 1
)

/**
 * Supported key types for code generation.
 */
@Serializable
internal enum class KeyType {
    @SerialName("Int")
    INT,
    @SerialName("String")
    STRING,
    @SerialName("Boolean")
    BOOLEAN,
    @SerialName("Long")
    LONG,
    @SerialName("Double")
    DOUBLE,
    @SerialName("StringSet")
    STRING_SET
}

/**
 * Extracts all unique sources and their keys from the manifest.
 * Used for code generation.
 */
internal fun PromotionGroupJson.extractSourcesAndKeys(): Map<String, Set<SourceKeyInfo>> {
    val result = mutableMapOf<String, MutableSet<SourceKeyInfo>>()
    
    fun processRule(rule: Rule) {
        val source = rule.lhs.source
        val key = rule.lhs.key
        val keyType = inferKeyType(rule.operator)
        val wherePropertyNames = rule.lhs.where?.keys?.toList() ?: emptyList()
        
        result.getOrPut(source) { mutableSetOf() }
            .add(SourceKeyInfo(key, keyType, rule.lhs.where != null, wherePropertyNames))
    }
    
    commonRules.forEach { processRule(it) }
    promotions.forEach { promo ->
        promo.rules.forEach { processRule(it) }
    }
    
    return result
}

/**
 * Information about a source key extracted from rules.
 *
 * @param wherePropertyNames The JSON key names from the where clause (e.g., ["survey-name", "step-name"]).
 *   These are used to generate data class properties with camelCase names.
 */
internal data class SourceKeyInfo(
    val name: String,
    val type: KeyType,
    val hasWhereClause: Boolean,
    val wherePropertyNames: List<String> = emptyList()
)

/**
 * Infers the key type from the operator and rhs value.
 */
private fun inferKeyType(operator: String): KeyType {
    return when {
        operator == "bool" -> KeyType.BOOLEAN
        operator.startsWith("stringSet") -> KeyType.STRING_SET
        operator.startsWith("int") -> KeyType.INT
        operator.startsWith("long") -> KeyType.LONG
        operator.startsWith("double") -> KeyType.DOUBLE
        operator.startsWith("string") -> KeyType.STRING
        else -> KeyType.STRING // Default fallback
    }
}
