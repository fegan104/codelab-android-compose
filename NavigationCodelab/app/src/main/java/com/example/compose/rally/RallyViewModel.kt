package com.example.compose.rally

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.example.compose.rally.data.mock.MockVegasDataSource
import com.example.compose.rally.data.mock.MockVegasSourceKeyParser
import com.fitnow.vegas.ui.VegasPromoter

class RallyViewModel(app: Application) : AndroidViewModel(app) {

    var jsonContent by mutableStateOf("")

    fun buildPromoterFromJson(): VegasPromoter<MockVegasDataSource>? {
        if (jsonContent.isBlank()) return null
        return VegasPromoter.newBuilder(MockVegasDataSource(), MockVegasSourceKeyParser(jsonContent))
            .buildFromJson(jsonContent)
            .getOrNull()
    }
}