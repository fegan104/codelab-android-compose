package com.fitnow.vegas.ui

import com.fitnow.vegas.core.Creative
import com.fitnow.vegas.core.Promotion
import com.fitnow.vegas.core.PromotionGroup
import com.fitnow.vegas.core.QueryDataSource

data class VegasResponse<D : QueryDataSource>(
    val group: PromotionGroup<D>,
    val promotion: Promotion<D>,
    val creative: Creative
)