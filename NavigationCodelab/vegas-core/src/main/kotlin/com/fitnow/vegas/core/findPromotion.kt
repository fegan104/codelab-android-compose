package com.fitnow.vegas.core


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
 * @param D The specific VegasQueryDataSource implementation
 * @param promoGroup The promotion group to evaluate
 * @param dataSource The data source to evaluate rules against
 * @return The first matching Promotion, or null if no promotion qualifies
 */
fun <D : QueryDataSource> findPromotion(
    promoGroup: PromotionGroup<D>,
    dataSource: D,
): Promotion<D>? {
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
            dataSource.appRules(promoGroup, promotion) && rule.evaluate(dataSource)
        }

        if (promotionRulesPassed) {
            return promotion
        }
    }

    // No promotion matched
    return null
}