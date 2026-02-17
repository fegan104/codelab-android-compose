package com.example.compose.rally.data.mock

/**
 * Represents a unique source/key entry with its type and default value.
 * Used for displaying and editing values in the control panel.
 */
sealed class MockSourceKeyEntry {
    abstract val source: String
    abstract val key: String
    abstract val whereParams: Map<String, String>?
    
    /**
     * Creates a unique identifier for this entry.
     */
    val uniqueId: String
        get() = buildString {
            append("$source/$key")
            whereParams?.let { params ->
                if (params.isNotEmpty()) {
                    append("?")
                    append(params.entries.sortedBy { it.key }.joinToString("&") { "${it.key}=${it.value}" })
                }
            }
        }
    
    /**
     * Returns a display-friendly label for this entry.
     */
    val displayLabel: String
        get() = buildString {
            append("$source.$key")
            whereParams?.let { params ->
                if (params.isNotEmpty()) {
                    append(" (")
                    append(params.entries.joinToString(", ") { "${it.key}=${it.value}" })
                    append(")")
                }
            }
        }
    
    data class IntEntry(
        override val source: String,
        override val key: String,
        override val whereParams: Map<String, String>? = null,
        val defaultValue: Int = 1
    ) : MockSourceKeyEntry()
    
    data class StringEntry(
        override val source: String,
        override val key: String,
        override val whereParams: Map<String, String>? = null,
        val defaultValue: String = ""
    ) : MockSourceKeyEntry()
    
    data class BooleanEntry(
        override val source: String,
        override val key: String,
        override val whereParams: Map<String, String>? = null,
        val defaultValue: Boolean = false
    ) : MockSourceKeyEntry()
    
    data class StringSetEntry(
        override val source: String,
        override val key: String,
        override val whereParams: Map<String, String>? = null,
        val defaultValue: Set<String> = emptySet()
    ) : MockSourceKeyEntry()
}
