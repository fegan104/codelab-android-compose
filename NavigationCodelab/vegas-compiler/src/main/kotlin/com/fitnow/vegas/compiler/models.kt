package com.fitnow.vegas.compiler

import com.fitnow.vegas.core.PromotionGroupJson
import com.fitnow.vegas.core.RuleJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    
    fun processRule(rule: RuleJson) {
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
