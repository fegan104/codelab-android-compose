package com.example.compose.rally

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.compose.rally.data.AppVegasDataSource
import com.example.compose.rally.generated.GeneratedVegasSourceKeyRegistry
import com.example.compose.rally.generated.PromotionGroupId
import com.fitnow.vegas.ui.Vegas

class RallyViewModel(app: Application) : AndroidViewModel(app) {

    private val appDataSource = AppVegasDataSource()
    val vegas = Vegas.newBuilder(appDataSource, GeneratedVegasSourceKeyRegistry)
        .buildFromAssets(app, PromotionGroupId.DashboardPromo.raw)
        .getOrThrow()

    val observePromo = vegas.currentPromotion
}