package com.fitnow.vegas.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray

/**
 * Represents a promotion group containing common rules and a list of promotions.
 * The common rules must all pass before any individual promotion is evaluated.
 *
 * @param D The specific VegasQueryDataSource implementation
 * @property id The unique identifier for this promotion group
 * @property type The type of promotion group (e.g., "affiliate")
 * @property commonRules Rules that must all pass before evaluating individual promotions
 * @property promotions List of promotions to evaluate in priority order (first match wins)
 */
data class PromotionGroup<D : QueryDataSource>(
    val id: String,
    val type: String,
    val commonRules: List<Rule<D, *, *>>,
    val promotions: List<Promotion<D>>
)

/**
 * Represents a single promotion within a promotion group.
 *
 * @param D The specific VegasQueryDataSource implementation
 * @property id The unique identifier for this promotion
 * @property actionUrl Optional action URL for this promotion
 * @property rules Rules specific to this promotion that must all pass
 * @property creativeTreatments Available creative treatments for display
 */
data class Promotion<D : QueryDataSource>(
    val id: String,
    val actionUrl: String?,
    val rules: List<Rule<D, *, *>>,
    val creativeTreatments: List<Creative>
)

/**
 * Selects a random Creative from the list based on the weight of each item.
 *
 * If the list is empty, throws [NoSuchElementException].
 *
 * @return A randomly selected [Creative], with probability proportional to its weight.
 */
fun List<Creative>.weightedRandom(): Creative {
    if (isEmpty()) throw NoSuchElementException("List is empty.")

    val totalWeight = sumOf { it.weight }
    if (totalWeight <= 0) return random()

    val cutoff = (0..totalWeight).random()
    var sum = 0
    for (creative in this) {
        sum += creative.weight
        if (sum > cutoff) return creative
    }

    return first()
}


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
            id = jsonObject.id,
            type = jsonObject.type,
            commonRules = ruleParser.parseRulesArray(jsonObject.commonRules),
            promotions = jsonObject.promotions.map { it.toDomain() }
        )
    }

    private fun PromotionJson.toDomain(): Promotion<D> = Promotion(
        id = id,
        actionUrl = actionUrl,
        rules = ruleParser.parseRulesArray(rules),
        creativeTreatments = creativeTreatments
    )
}