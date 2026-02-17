package com.example.compose.rally

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.example.compose.rally.data.AppVegasDataSource
import com.example.compose.rally.generated.GeneratedVegasSourceKeyParser
import com.example.compose.rally.generated.GeneratedVegasDataSource
import com.fitnow.vegas.ui.VegasPromoter

class RallyViewModel(app: Application) : AndroidViewModel(app) {

    var jsonContent by mutableStateOf("")

    fun buildPromoterFromJson(): VegasPromoter<GeneratedVegasDataSource>? {
        if (jsonContent.isBlank()) return null
        return VegasPromoter.newBuilder(AppVegasDataSource(), GeneratedVegasSourceKeyParser)
            .buildFromJson(jsonContent)
            .getOrNull()
    }
}