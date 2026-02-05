package com.fitnow.vegas.core

/**
 * Marker interface for the app's data provider.
 * Implementations provide access to data that can be queried by Vegas rules.
 */
interface QueryDataSource {

    fun <D : QueryDataSource> appRules(
        group: PromotionGroup<D>,
        promotion: Promotion<D>,
    ): Boolean = true
}