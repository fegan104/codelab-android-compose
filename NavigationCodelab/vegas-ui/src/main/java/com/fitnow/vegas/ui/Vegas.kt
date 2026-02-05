package com.fitnow.vegas.ui

import android.content.Context
import com.fitnow.vegas.core.Promotion
import com.fitnow.vegas.core.PromotionGroup
import com.fitnow.vegas.core.QueryDataSource
import com.fitnow.vegas.core.VegasPromotionGroupParser
import com.fitnow.vegas.core.VegasSourceKeyRegistry
import com.fitnow.vegas.core.findPromotion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onStart

class Vegas<D : QueryDataSource> private constructor(
    private val dataSource: D,
    private val promotionGroup: PromotionGroup<D>,
) {

    private val _currentPromotion = MutableStateFlow<Promotion<D>?>(null)

    val currentPromotion: Flow<Promotion<D>?> = _currentPromotion.onStart {
        _currentPromotion.value = findPromotion(promotionGroup, dataSource)
    }

    internal fun onDismiss() {
        _currentPromotion.value = null
    }

    class Builder<D : QueryDataSource> internal constructor(
        private val dataSource: D,
        private val keyRegistry: VegasSourceKeyRegistry<D>
    ) {

        //TODO use PromoGroupId not file name
        fun buildFromAssets(context: Context, fileName: String): Result<Vegas<D>> {
            val rawJson = context.assets.open(fileName).bufferedReader().use { it.readText() }
            return build(rawJson)
        }

        fun buildFromJson(rawJson: String): Result<Vegas<D>> {
            return build(rawJson)
        }

        private fun build(rawJson: String): Result<Vegas<D>> {
            val parser = VegasPromotionGroupParser(keyRegistry)
            return parser.parse(rawJson).map { promotionGroup ->
                Vegas(dataSource, promotionGroup)
            }
        }
    }

    companion object {
        fun <D : QueryDataSource> newBuilder(
            dataSource: D,
            keyRegistry: VegasSourceKeyRegistry<D>,
        ): Builder<D> {
            return Builder(dataSource, keyRegistry)
        }
    }
}