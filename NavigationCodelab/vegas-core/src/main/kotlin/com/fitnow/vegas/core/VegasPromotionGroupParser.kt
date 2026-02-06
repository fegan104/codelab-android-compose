package com.fitnow.vegas.core

import kotlinx.serialization.json.Json

/**
 * Parser for Vegas promotion groups from JSON format.
 * Uses @Serializable classes for automatic parsing of simple fields,
 * while rules are parsed manually due to their dynamic typed nature.
 *
 * @param D The specific VegasQueryDataSource implementation
 * @property registry The registry used to resolve string-based key references
 */
class VegasPromotionGroupParser<D : QueryDataSource>(
    private val registry: VegasSourceKeyRegistry<D>
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val ruleParser = VegasRuleParser(registry)

    /**
     * Parses a JSON string into a PromotionGroup.
     *
     * Expected JSON format:
     * ```json
     * {
     *   "id": "dashboard-ad-group",
     *   "type": "affiliate",
     *   "commonRulesV2": [...],
     *   "promotions": [
     *     {
     *       "id": "promo1",
     *       "actionUrl": "...",
     *       "rulesV2": [...],
     *       "creativeTreatments": [...]
     *     }
     *   ]
     * }
     * ```
     *
     * @param jsonString The JSON string to parse
     * @return A Result with a parsed PromotionGroup if parsing succeeds or a Result.failure is JSON
     * file  is invalid.
     */
    fun parse(jsonString: String): Result<PromotionGroup<D>> = runCatching {
        // Use @Serializable DTO for automatic parsing of structure
        val jsonObject = json.decodeFromString<PromotionGroupJson>(jsonString)

        // Transform json object to domain object, parsing rules manually
        PromotionGroup(
            id = ParsedPromotionGroupId(jsonObject.id),
            type = jsonObject.type,
            commonRules = ruleParser.parseRules(jsonObject.commonRules),
            promotions = jsonObject.promotions.map { it.toDomain() }
        )
    }

    private fun PromotionJson.toDomain(): Promotion<D> = Promotion(
        id = id,
        actionUrl = actionUrl,
        rules = ruleParser.parseRules(rules),
        creativeTreatments = creativeTreatments
    )
}

/**
 * Simple [PromotionGroupId] wrapper for IDs parsed directly from JSON.
 */
private data class ParsedPromotionGroupId(override val raw: String) : PromotionGroupId