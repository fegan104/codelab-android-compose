package com.example.compose.rally

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.compose.rally.data.AppVegasDataSource
import com.example.compose.rally.generated.GeneratedVegasSourceKeyParser
import com.example.compose.rally.generated.PromotionGroupIds
import com.fitnow.vegas.ui.VegasPromoter

class RallyViewModel(app: Application) : AndroidViewModel(app) {

    private val appDataSource = AppVegasDataSource()
    val dashboardPromoter = VegasPromoter.newBuilder(appDataSource, GeneratedVegasSourceKeyParser)
        .buildFromAssets(app, PromotionGroupIds.DashboardPromo)
        .getOrThrow()

}