package com.fitnow.vegas.ui

import android.content.Context
import android.util.Log
import com.fitnow.vegas.core.PromotionGroup
import com.fitnow.vegas.core.PromotionGroupId
import com.fitnow.vegas.core.QueryDataSource
import com.fitnow.vegas.core.VegasPromotionGroupParser
import com.fitnow.vegas.core.VegasSourceKeyRegistry
import com.fitnow.vegas.core.findPromotion
import com.fitnow.vegas.core.weightedRandom
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart

class Vegas<D : QueryDataSource> private constructor(
    private val dataSource: D,
    private val promotionGroup: PromotionGroup<D>,
) {

    private val _currentPromotion = MutableStateFlow<VegasResponse<D>?>(null)

    val currentPromotion: Flow<VegasResponse<D>?> = _currentPromotion.onStart {
        val response = findPromotion(promotionGroup, dataSource)?.let { promotion ->
            VegasResponse(
                group = promotionGroup,
                promotion = promotion,
                creative = promotion
                    .creativeTreatments
                    .filter { dataSource.isValid(it) }
                    .weightedRandom(),
            )
        }

        _currentPromotion.value = response
    }.catch { reason ->
        Log.e("Vegas", "Error evaluating promotions", reason)
        emit(null)
    }

    internal fun onDismiss() {
        _currentPromotion.value = null
    }

    class Builder<D : QueryDataSource> internal constructor(
        private val dataSource: D,
        private val keyRegistry: VegasSourceKeyRegistry<D>
    ) {

        fun buildFromAssets(context: Context, promotionGroupId: PromotionGroupId): Result<Vegas<D>> {
            val rawJson = context.assets.open("${promotionGroupId.raw}.json").bufferedReader().use { it.readText() }
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