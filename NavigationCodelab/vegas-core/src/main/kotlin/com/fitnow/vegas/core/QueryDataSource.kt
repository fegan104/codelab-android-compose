package com.fitnow.vegas.core

/**
 * Marker interface for the app's data provider.
 * Implementations provide access to data that can be queried by Vegas rules.
 */
interface QueryDataSource {

    /**
     * Optional method to provide custom rules to determine a promotion's eligibility beyond
     * just its rules.
     *
     * @param group The promotion group being evaluated
     * @param promotion The promotion being evaluated
     */
    fun <D : QueryDataSource> customPromotionRules(
        group: PromotionGroup<D>,
        promotion: Promotion<D>,
    ): Boolean = true

    /**
     * Optional method to provide custom validation for a promotion creative.
     *
     * @param creative The promotion creative being evaluated.
     */
    fun isValid(creative: Creative): Boolean = true
}