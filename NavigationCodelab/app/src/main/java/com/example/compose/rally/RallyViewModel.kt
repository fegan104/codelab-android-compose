package com.example.compose.rally

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.example.compose.rally.data.mock.MockSourceKeyEntry
import com.example.compose.rally.data.mock.MockVegasDataSource
import com.example.compose.rally.data.mock.MockVegasSourceKeyParser
import com.fitnow.vegas.ui.VegasPromoter

class RallyViewModel(app: Application) : AndroidViewModel(app) {

    var jsonContent by mutableStateOf("")

    /**
     * The data source used for mock Vegas queries.
     * This instance is shared so control panel edits affect promoter evaluation.
     */
    val dataSource = MockVegasDataSource()

    /**
     * The current parser, rebuilt when JSON changes.
     */
    private var currentParser: MockVegasSourceKeyParser? = null

    /**
     * The list of source key entries extracted from the current JSON.
     * Empty if no valid JSON has been parsed.
     */
    var sourceKeyEntries by mutableStateOf<List<MockSourceKeyEntry>>(emptyList())
        private set

    /**
     * Updates the JSON content and rebuilds the parser.
     */
    fun updateJsonContent(newJson: String) {
        jsonContent = newJson
        rebuildParser()
    }

    /**
     * Rebuilds the parser from current JSON and extracts entries.
     */
    private fun rebuildParser() {
        if (jsonContent.isBlank()) {
            currentParser = null
            sourceKeyEntries = emptyList()
            return
        }
        
        val parser = MockVegasSourceKeyParser(jsonContent)
        currentParser = parser
        sourceKeyEntries = parser.getAllEntries()
    }

    /**
     * Builds a VegasPromoter from the current JSON content.
     * Uses the shared dataSource so control panel values are respected.
     */
    fun buildPromoterFromJson(): VegasPromoter<MockVegasDataSource>? {
        if (jsonContent.isBlank()) return null
        
        // Ensure parser is up to date
        if (currentParser == null) {
            rebuildParser()
        }
        
        val parser = currentParser ?: return null
        
        return VegasPromoter.newBuilder(dataSource, parser)
            .buildFromJson(jsonContent)
            .getOrNull()
    }

    /**
     * Resets all data source values to defaults.
     */
    fun resetAllValues() {
        dataSource.resetAll()
    }
}
