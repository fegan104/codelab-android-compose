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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlin.collections.all

/**
 * Represents a promotion group containing common rules and a list of promotions.
 * The common rules must all pass before any individual promotion is evaluated.
 *
 * @param C The specific VegasQueryDataSource implementation
 * @property id The unique identifier for this promotion group
 * @property type The type of promotion group (e.g., "affiliate")
 * @property commonRules Rules that must all pass before evaluating individual promotions
 * @property promotions List of promotions to evaluate in priority order (first match wins)
 */
data class PromotionGroup<C : QueryDataSource>(
    val id: String,
    val type: String,
    val commonRules: List<Rule<C, *, *>>,
    val promotions: List<Promotion<C>>
)

/**
 * Represents a single promotion within a promotion group.
 *
 * @param C The specific VegasQueryDataSource implementation
 * @property id The unique identifier for this promotion
 * @property actionUrl Optional action URL for this promotion
 * @property rules Rules specific to this promotion that must all pass
 * @property creativeTreatments Available creative treatments for display
 */
data class Promotion<C : QueryDataSource>(
    val id: String,
    val actionUrl: String?,
    val rules: List<Rule<C, *, *>>,
    val creativeTreatments: List<Creative>
)

/**
 * Data class for creative treatment information.
 * Uses @Serializable for automatic JSON parsing.
 */
@Serializable
data class Creative(
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
 * JSON schema representation for promotions.
 * Used for automatic deserialization before transforming to the typed domain object.
 */
@Serializable
internal data class PromotionJson(
    val id: String,
    val actionUrl: String? = null,
    @SerialName("rulesV2") val rules: JsonArray = JsonArray(emptyList()),
    val creativeTreatments: List<Creative> = emptyList()
)

/**
 * JSON schema representation for promotion groups.
 */
@Serializable
internal data class PromotionGroupJson(
    val id: String,
    val type: String,
    @SerialName("commonRulesV2") val commonRules: JsonArray = JsonArray(emptyList()),
    val promotions: List<PromotionJson> = emptyList()
)

/**
 * Parser for Vegas promotion groups from JSON format.
 * Uses @Serializable classes for automatic parsing of simple fields,
 * while rules are parsed manually due to their dynamic typed nature.
 *
 * @param C The specific VegasQueryDataSource implementation
 * @property registry The registry used to resolve string-based key references
 */
class VegasPromotionGroupParser<C : QueryDataSource>(
    private val registry: VegasSourceKeyRegistry<C>
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
     * @return Parsed PromotionGroup
     * @throws IllegalArgumentException if the JSON format is invalid
     */
    fun parse(jsonString: String): PromotionGroup<C> {
        // Use @Serializable DTO for automatic parsing of structure
        val dto = json.decodeFromString<PromotionGroupJson>(jsonString)

        // Transform DTO to domain object, parsing rules manually
        return PromotionGroup(
            id = dto.id,
            type = dto.type,
            commonRules = ruleParser.parseRulesArray(dto.commonRules),
            promotions = dto.promotions.map { it.toDomain() }
        )
    }

    private fun PromotionJson.toDomain(): Promotion<C> = Promotion(
        id = id,
        actionUrl = actionUrl,
        rules = ruleParser.parseRulesArray(rules),
        creativeTreatments = creativeTreatments
    )
}

/**
 * Evaluates a promotion group against a data source and returns the first matching promotion.
 *
 * Evaluation logic:
 * 1. First, evaluate all commonRulesV2 - if any fail, return null immediately
 * 2. If all common rules pass, evaluate each promotion in order (priority)
 * 3. For each promotion, evaluate all its rules - if any fail, skip to the next promotion
 * 4. Return the first promotion where all rules pass
 * 5. If no promotion matches, return null
 *
 * @param C The specific VegasQueryDataSource implementation
 * @param promoGroup The promotion group to evaluate
 * @param dataSource The data source to evaluate rules against
 * @return The first matching Promotion, or null if no promotion qualifies
 */
fun <C : QueryDataSource> findPromotion(
    promoGroup: PromotionGroup<C>,
    dataSource: C
): Promotion<C>? {
    // Step 1: Evaluate all common rules first
    // If any common rule fails, the entire group fails
    val commonRulesPassed = promoGroup.commonRules.all { rule ->
        rule.evaluate(dataSource)
    }

    if (!commonRulesPassed) {
        return null
    }

    // Step 2: Evaluate promotions in priority order (list order = priority)
    // Return the first promotion where all rules pass
    for (promotion in promoGroup.promotions) {
        val promotionRulesPassed = promotion.rules.all { rule ->
            rule.evaluate(dataSource)
        }

        if (promotionRulesPassed) {
            return promotion
        }
    }

    // No promotion matched
    return null
}