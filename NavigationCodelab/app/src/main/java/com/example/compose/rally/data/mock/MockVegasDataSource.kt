package com.example.compose.rally.data.mock

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.fitnow.vegas.core.QueryDataSource

/**
 * Mock implementation of QueryDataSource for Rally app.
 * Provides editable mock data for promotion evaluation via control panel.
 */
@Suppress("unused") // Public API functions kept for programmatic access
class MockVegasDataSource : QueryDataSource {

    // Storage maps for each value type, keyed by unique identifier
    private val intValues: SnapshotStateMap<String, Int> = mutableStateMapOf()
    private val stringValues: SnapshotStateMap<String, String> = mutableStateMapOf()
    private val booleanValues: SnapshotStateMap<String, Boolean> = mutableStateMapOf()
    private val stringSetValues: SnapshotStateMap<String, Set<String>> = mutableStateMapOf()

    // Default values
    companion object {
        const val DEFAULT_INT = 1
        const val DEFAULT_STRING = ""
        const val DEFAULT_BOOLEAN = false
        val DEFAULT_STRING_SET: Set<String> = emptySet()
    }

    /**
     * Creates a unique key for the value storage maps.
     */
    private fun createKey(sourceName: String, keyName: String, whereParams: Map<String, String>?): String {
        return buildString {
            append("$sourceName/$keyName")
            whereParams?.let { params ->
                if (params.isNotEmpty()) {
                    append("?")
                    append(params.entries.sortedBy { it.key }.joinToString("&") { "${it.key}=${it.value}" })
                }
            }
        }
    }

    /**
     * Returns the Int value for the given query, or the default if not set.
     */
    fun getInt(sourceName: String, keyName: String, whereParams: Map<String, String>? = null): Int {
        val key = createKey(sourceName, keyName, whereParams)
        return intValues[key] ?: DEFAULT_INT
    }

    /**
     * Sets the Int value for the given query.
     */
    fun setInt(sourceName: String, keyName: String, whereParams: Map<String, String>? = null, value: Int) {
        val key = createKey(sourceName, keyName, whereParams)
        intValues[key] = value
    }

    /**
     * Returns the String value for the given query, or the default if not set.
     */
    fun getString(sourceName: String, keyName: String, whereParams: Map<String, String>? = null): String {
        val key = createKey(sourceName, keyName, whereParams)
        return stringValues[key] ?: DEFAULT_STRING
    }

    /**
     * Sets the String value for the given query.
     */
    fun setString(sourceName: String, keyName: String, whereParams: Map<String, String>? = null, value: String) {
        val key = createKey(sourceName, keyName, whereParams)
        stringValues[key] = value
    }

    /**
     * Returns the Boolean value for the given query, or the default if not set.
     */
    fun getBoolean(sourceName: String, keyName: String, whereParams: Map<String, String>? = null): Boolean {
        val key = createKey(sourceName, keyName, whereParams)
        return booleanValues[key] ?: DEFAULT_BOOLEAN
    }

    /**
     * Sets the Boolean value for the given query.
     */
    fun setBoolean(sourceName: String, keyName: String, whereParams: Map<String, String>? = null, value: Boolean) {
        val key = createKey(sourceName, keyName, whereParams)
        booleanValues[key] = value
    }

    /**
     * Returns the StringSet value for the given query, or the default if not set.
     */
    fun getStringSet(sourceName: String, keyName: String, whereParams: Map<String, String>? = null): Set<String> {
        val key = createKey(sourceName, keyName, whereParams)
        return stringSetValues[key] ?: DEFAULT_STRING_SET
    }

    /**
     * Sets the StringSet value for the given query.
     */
    fun setStringSet(sourceName: String, keyName: String, whereParams: Map<String, String>? = null, value: Set<String>) {
        val key = createKey(sourceName, keyName, whereParams)
        stringSetValues[key] = value
    }

    /**
     * Gets the current Int value or null if using default.
     */
    fun getIntOrNull(uniqueId: String): Int? = intValues[uniqueId]

    /**
     * Gets the current String value or null if using default.
     */
    fun getStringOrNull(uniqueId: String): String? = stringValues[uniqueId]

    /**
     * Gets the current Boolean value or null if using default.
     */
    fun getBooleanOrNull(uniqueId: String): Boolean? = booleanValues[uniqueId]

    /**
     * Gets the current StringSet value or null if using default.
     */
    fun getStringSetOrNull(uniqueId: String): Set<String>? = stringSetValues[uniqueId]

    /**
     * Sets a value by unique ID (for control panel use).
     */
    fun setValueByUniqueId(uniqueId: String, entry: MockSourceKeyEntry) {
        when (entry) {
            is MockSourceKeyEntry.IntEntry -> intValues[uniqueId] = entry.defaultValue
            is MockSourceKeyEntry.StringEntry -> stringValues[uniqueId] = entry.defaultValue
            is MockSourceKeyEntry.BooleanEntry -> booleanValues[uniqueId] = entry.defaultValue
            is MockSourceKeyEntry.StringSetEntry -> stringSetValues[uniqueId] = entry.defaultValue
        }
    }

    /**
     * Sets an Int value by unique ID.
     */
    fun setIntByUniqueId(uniqueId: String, value: Int) {
        intValues[uniqueId] = value
    }

    /**
     * Sets a String value by unique ID.
     */
    fun setStringByUniqueId(uniqueId: String, value: String) {
        stringValues[uniqueId] = value
    }

    /**
     * Sets a Boolean value by unique ID.
     */
    fun setBooleanByUniqueId(uniqueId: String, value: Boolean) {
        booleanValues[uniqueId] = value
    }

    /**
     * Sets a StringSet value by unique ID.
     */
    fun setStringSetByUniqueId(uniqueId: String, value: Set<String>) {
        stringSetValues[uniqueId] = value
    }

    /**
     * Resets all values to defaults.
     */
    fun resetAll() {
        intValues.clear()
        stringValues.clear()
        booleanValues.clear()
        stringSetValues.clear()
    }
}
