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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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
data class PromotionGroup<C : VegasQueryDataSource>(
    val id: String,
    val type: String,
    val commonRules: List<Rule<C, *, *, *>>,
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
data class Promotion<C : VegasQueryDataSource>(
    val id: String,
    val actionUrl: String?,
    val rules: List<Rule<C, *, *, *>>,
    val creativeTreatments: List<CreativeTreatmentData>
)

/**
 * Data class for creative treatment information.
 */
data class CreativeTreatmentData(
    val id: String,
    val heroImageUrl: String?,
    val titleText: String?,
    val bodyText: String?,
    val actionText: String?,
    val buttonText: String?,
    val noThanksText: String?,
    val weight: Int
)

/**
 * Parser for Vegas promotion groups from JSON format.
 * Parses the entire promotion group structure including common rules and individual promotions.
 *
 * @param C The specific VegasQueryDataSource implementation
 * @property registry The registry used to resolve string-based key references
 */
class VegasPromotionGroupParser<C : VegasQueryDataSource>(
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
        val rootObject = json.parseToJsonElement(jsonString).jsonObject

        val id = rootObject["id"]?.jsonPrimitive?.content
            ?: throw IllegalArgumentException("PromotionGroup missing 'id' field")
        val type = rootObject["type"]?.jsonPrimitive?.content
            ?: throw IllegalArgumentException("PromotionGroup missing 'type' field")

        // Parse common rules
        val commonRulesArray = rootObject["commonRulesV2"]?.jsonArray ?: JsonArray(emptyList())
        val commonRules = ruleParser.parseRulesArray(commonRulesArray)

        // Parse promotions
        val promotionsArray = rootObject["promotions"]?.jsonArray ?: JsonArray(emptyList())
        val promotions = promotionsArray.map { parsePromotion(it.jsonObject) }

        return PromotionGroup(
            id = id,
            type = type,
            commonRules = commonRules,
            promotions = promotions
        )
    }

    private fun parsePromotion(promoJson: JsonObject): Promotion<C> {
        val id = promoJson["id"]?.jsonPrimitive?.content
            ?: throw IllegalArgumentException("Promotion missing 'id' field")
        val actionUrl = promoJson["actionUrl"]?.jsonPrimitive?.content

        // Parse promotion-specific rules
        val rulesArray = promoJson["rulesV2"]?.jsonArray ?: JsonArray(emptyList())
        val rules = ruleParser.parseRulesArray(rulesArray)

        // Parse creative treatments
        val creativeTreatmentsArray = promoJson["creativeTreatments"]?.jsonArray ?: JsonArray(emptyList())
        val creativeTreatments = creativeTreatmentsArray.map { parseCreativeTreatment(it.jsonObject) }

        return Promotion(
            id = id,
            actionUrl = actionUrl,
            rules = rules,
            creativeTreatments = creativeTreatments
        )
    }

    private fun parseCreativeTreatment(treatmentJson: JsonObject): CreativeTreatmentData {
        return CreativeTreatmentData(
            id = treatmentJson["id"]?.jsonPrimitive?.content
                ?: throw IllegalArgumentException("CreativeTreatment missing 'id' field"),
            heroImageUrl = treatmentJson["heroImageUrl"]?.jsonPrimitive?.content,
            titleText = treatmentJson["titleText"]?.jsonPrimitive?.content,
            bodyText = treatmentJson["bodyText"]?.jsonPrimitive?.content,
            actionText = treatmentJson["actionText"]?.jsonPrimitive?.content,
            buttonText = treatmentJson["buttonText"]?.jsonPrimitive?.content,
            noThanksText = treatmentJson["noThanksText"]?.jsonPrimitive?.content,
            weight = treatmentJson["weight"]?.jsonPrimitive?.intOrNull ?: 1
        )
    }
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
fun <C : VegasQueryDataSource> findPromotion(
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