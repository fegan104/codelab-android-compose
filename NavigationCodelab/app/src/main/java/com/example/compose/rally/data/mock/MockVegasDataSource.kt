package com.example.compose.rally.data.mock

import com.fitnow.vegas.core.QueryDataSource

/**
 * Mock implementation of QueryDataSource for Rally app.
 * Provides static mock data for promotion evaluation.
 * Parameters are intentionally unused as this returns static values.
 */
@Suppress("UNUSED_PARAMETER")
class MockVegasDataSource : QueryDataSource {

    /**
     * Returns a static Int value (1) for any int query.
     */
    fun getInt(sourceName: String, keyName: String, whereParams: Map<String, String>? = null): Int {
        return 1
    }

    /**
     * Returns a static empty String for any string query.
     */
    fun getString(sourceName: String, keyName: String, whereParams: Map<String, String>? = null): String {
        return ""
    }

    /**
     * Returns a static false for any boolean query.
     */
    fun getBoolean(sourceName: String, keyName: String, whereParams: Map<String, String>? = null): Boolean {
        return false
    }

    /**
     * Returns an empty Set for any string set query.
     */
    fun getStringSet(sourceName: String, keyName: String, whereParams: Map<String, String>? = null): Set<String> {
        return emptySet()
    }
}
