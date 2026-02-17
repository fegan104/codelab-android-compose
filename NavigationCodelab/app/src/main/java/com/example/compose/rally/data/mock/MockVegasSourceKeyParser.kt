package com.example.compose.rally.data.mock

import com.fitnow.vegas.core.BooleanSourceKey
import com.fitnow.vegas.core.IntSourceKey
import com.fitnow.vegas.core.PromotionGroupJson
import com.fitnow.vegas.core.RuleJson
import com.fitnow.vegas.core.SourceKey
import com.fitnow.vegas.core.SourceKeyParser
import com.fitnow.vegas.core.StringSetSourceKey
import com.fitnow.vegas.core.StringSourceKey
import kotlinx.serialization.json.Json

/**
 * Parser for resolving SourceKeys from JSON source/key names.
 * Parses the raw JSON at runtime and determines key types from operator values.
 */
class MockVegasSourceKeyParser(rawJson: String) : SourceKeyParser<MockVegasDataSource> {

    private val json = Json { ignoreUnknownKeys = true }
    private val promotionGroup: PromotionGroupJson? = runCatching {
        json.decodeFromString<PromotionGroupJson>(rawJson)
    }.getOrNull()

    /**
     * Collects all rules from commonRules and all promotion rules.
     */
    private val allRules: List<RuleJson>
        get() = buildList {
            promotionGroup?.commonRules?.let { addAll(it) }
            promotionGroup?.promotions?.forEach { promotion ->
                addAll(promotion.rules)
            }
        }

    /**
     * Finds a rule matching the given source and key names.
     */
    private fun findRule(sourceName: String, keyName: String): RuleJson? {
        return allRules.find { rule ->
            rule.lhs.source == sourceName && rule.lhs.key == keyName
        }
    }

    /**
     * Determines the key type based on the operator name.
     */
    private fun operatorToKeyType(operator: String): KeyType {
        return when {
            operator.startsWith("int") -> KeyType.INT
            operator.startsWith("string") && operator.contains("Set", ignoreCase = true) -> KeyType.STRING_SET
            operator.startsWith("string") -> KeyType.STRING
            operator == "bool" -> KeyType.BOOLEAN
            else -> KeyType.STRING // Default fallback
        }
    }

    override fun findKey(sourceName: String, raw: String): SourceKey<MockVegasDataSource, *>? {
        val rule = findRule(sourceName, raw) ?: return null
        return when (operatorToKeyType(rule.operator)) {
            KeyType.INT -> MockIntSourceKey(sourceName, raw)
            KeyType.STRING -> MockStringSourceKey(sourceName, raw)
            KeyType.BOOLEAN -> MockBooleanSourceKey(sourceName, raw)
            KeyType.STRING_SET -> MockStringSetSourceKey(sourceName, raw)
        }
    }

    override fun createKeyWithWhere(
        sourceName: String,
        keyName: String,
        whereParams: Map<String, String>
    ): SourceKey<MockVegasDataSource, *>? {
        // For where clauses, we look for a rule that matches both the source/key
        // and has a where clause (the specific where values don't matter for type detection)
        val rule = allRules.find { rule ->
            rule.lhs.source == sourceName && 
            rule.lhs.key == keyName && 
            rule.lhs.where != null
        } ?: return null
        
        return when (operatorToKeyType(rule.operator)) {
            KeyType.INT -> MockIntSourceKey(sourceName, keyName, whereParams)
            KeyType.STRING -> MockStringSourceKey(sourceName, keyName, whereParams)
            KeyType.BOOLEAN -> MockBooleanSourceKey(sourceName, keyName, whereParams)
            KeyType.STRING_SET -> MockStringSetSourceKey(sourceName, keyName, whereParams)
        }
    }

    private enum class KeyType {
        INT, STRING, BOOLEAN, STRING_SET
    }
}

/**
 * Mock IntSourceKey that returns static value from MockVegasDataSource.
 */
@Suppress("RedundantNullableReturnType")
class MockIntSourceKey(
    val sourceName: String,
    val keyName: String,
    val whereParams: Map<String, String>? = null
) : IntSourceKey<MockVegasDataSource> {
    override suspend fun resolve(dataSource: MockVegasDataSource): Int? {
        return dataSource.getInt(sourceName, keyName, whereParams)
    }
}

/**
 * Mock StringSourceKey that returns static value from MockVegasDataSource.
 */
@Suppress("RedundantNullableReturnType")
class MockStringSourceKey(
    val sourceName: String,
    val keyName: String,
    val whereParams: Map<String, String>? = null
) : StringSourceKey<MockVegasDataSource> {
    override suspend fun resolve(dataSource: MockVegasDataSource): String? {
        return dataSource.getString(sourceName, keyName, whereParams)
    }
}

/**
 * Mock BooleanSourceKey that returns static value from MockVegasDataSource.
 */
@Suppress("RedundantNullableReturnType")
class MockBooleanSourceKey(
    val sourceName: String,
    val keyName: String,
    val whereParams: Map<String, String>? = null
) : BooleanSourceKey<MockVegasDataSource> {
    override suspend fun resolve(dataSource: MockVegasDataSource): Boolean? {
        return dataSource.getBoolean(sourceName, keyName, whereParams)
    }
}

/**
 * Mock StringSetSourceKey that returns static value from MockVegasDataSource.
 */
@Suppress("RedundantNullableReturnType")
class MockStringSetSourceKey(
    val sourceName: String,
    val keyName: String,
    val whereParams: Map<String, String>? = null
) : StringSetSourceKey<MockVegasDataSource> {
    override suspend fun resolve(dataSource: MockVegasDataSource): Set<String>? {
        return dataSource.getStringSet(sourceName, keyName, whereParams)
    }
}
