package com.fitnow.vegas.compiler

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Root model for the Vegas JSON manifest.
 * Defines the sources and their associated keys that will be code-generated.
 */
@Serializable
data class VegasManifest(
    val sources: List<SourceDefinition>
)

/**
 * Defines a single data source (e.g., "User", "History").
 */
@Serializable
data class SourceDefinition(
    val name: String,
    val keys: List<KeyDefinition>
)

/**
 * Defines a single key within a source.
 */
@Serializable
data class KeyDefinition(
    val name: String,
    val type: KeyType
)

/**
 * Supported key types for code generation.
 */
@Serializable
enum class KeyType {
    @SerialName("Int")
    INT,
    @SerialName("String")
    STRING,
    @SerialName("Boolean")
    BOOLEAN,
    @SerialName("Long")
    LONG,
    @SerialName("Double")
    DOUBLE
}
