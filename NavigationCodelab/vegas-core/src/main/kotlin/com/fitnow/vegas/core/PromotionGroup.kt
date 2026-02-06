package com.fitnow.vegas.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

//interface PromotionGroupId {
//    val raw: String
//}

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
 * JSON schema representation for promotion groups.
 */
@Serializable
data class PromotionGroupJson(
    val id: String,
    val type: String,
    @SerialName("commonRulesV2") val commonRules: List<RuleJson> = emptyList(),
    val promotions: List<PromotionJson> = emptyList()
)

/**
 * JSON schema representation for promotions.
 * Used for automatic deserialization before transforming to the typed domain object.
 */
@Serializable
data class PromotionJson(
    val id: String,
    val actionUrl: String? = null,
    @SerialName("rulesV2") val rules: List<RuleJson> = emptyList(),
    val creativeTreatments: List<Creative> = emptyList()
)

/**
 * JSON schema representation for a rule that evaluates a condition.
 */
@Serializable
data class RuleJson(
    val operator: String,
    val lhs: RuleQueryJson,
    val rhs: JsonElement
)

/**
 * JSON schema representation for the left-hand side of a rule (the data source reference).
 */
@Serializable
data class RuleQueryJson(
    val source: String,
    val key: String,
    val where: JsonObject? = null,
    val default: JsonElement? = null
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
