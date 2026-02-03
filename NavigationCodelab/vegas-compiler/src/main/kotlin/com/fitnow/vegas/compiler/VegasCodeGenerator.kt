package com.fitnow.vegas.compiler

/**
 * Generates type-safe Kotlin code from a Vegas manifest.
 * Produces:
 * - QuerySource implementations for each source
 * - GeneratedVegasDataSource interface with fetch methods
 * - Sealed key classes with type-safe resolve implementations
 * - GeneratedVegasSourceKeyRegistry for string-to-key lookups
 */
class VegasCodeGenerator(
    private val manifest: VegasManifest,
    private val packageName: String = "com.fitnow.vegas.generated"
) {

    /**
     * Generates the complete VegasGeneratedApi.kt content.
     */
    fun generate(): String = buildString {
        appendLine(COPYRIGHT_HEADER)
        appendLine()
        appendLine("package $packageName")
        appendLine()
        appendLine("import com.fitnow.vegas.core.*")
        appendLine()

        generateQuerySources()
        appendLine()
        generateDataSourceInterface()
        appendLine()
        generateKeyContainers()
        appendLine()
        generateRegistry()
    }

    /**
     * Generates QuerySource object implementations for each source.
     */
    private fun StringBuilder.generateQuerySources() {
        manifest.sources.forEach { source ->
            appendLine("/**")
            appendLine(" * QuerySource for ${source.name} data.")
            appendLine(" */")
            appendLine("object ${source.name}Source : QuerySource")
        }
    }

    /**
     * Generates the GeneratedVegasDataSource interface with typed fetch methods.
     */
    private fun StringBuilder.generateDataSourceInterface() {
        appendLine("/**")
        appendLine(" * Generated data source interface with typed fetch methods.")
        appendLine(" * Implement this interface to provide data for Vegas rules.")
        appendLine(" */")
        appendLine("interface GeneratedVegasDataSource : VegasQueryDataSource {")

        manifest.sources.forEach { source ->
            source.keys.groupBy { it.type }.forEach { (type, keys) ->
                val sourceKeyType = "${source.name}Keys.${source.name}${type.displayName}SourceKey"
                appendLine()
                appendLine("    /**")
                appendLine("     * Fetches a ${type.displayName} value for the given key from ${source.name}.")
                appendLine("     */")
                appendLine("    fun fetch${source.name}${type.displayName}(source: ${source.name}Source, key: $sourceKeyType): ${type.kotlinType}?")
            }
        }

        appendLine("}")
    }

    /**
     * Generates key container objects with sealed parent classes and concrete key objects.
     */
    private fun StringBuilder.generateKeyContainers() {
        manifest.sources.forEach { source ->
            appendLine("/**")
            appendLine(" * Container object for ${source.name} source keys.")
            appendLine(" */")
            appendLine("object ${source.name}Keys {")

            // Group keys by type
            val keysByType = source.keys.groupBy { it.type }

            // Generate sealed parent class for each type
            keysByType.forEach { (type, keys) ->
                appendLine()
                appendLine("    /**")
                appendLine("     * Sealed parent for ${source.name} ${type.displayName} keys.")
                appendLine("     */")
                appendLine("    sealed class ${source.name}${type.displayName}SourceKey : ${type.sourceKeyInterface}<GeneratedVegasDataSource, ${source.name}Source> {")
                appendLine("        abstract val keyName: String")
                appendLine()
                appendLine("        override fun resolve(dataSource: GeneratedVegasDataSource, source: ${source.name}Source): ${type.kotlinType}? {")
                appendLine("            return dataSource.fetch${source.name}${type.displayName}(source, this)")
                appendLine("        }")
                appendLine("    }")

                // Generate concrete key objects
                keys.forEach { key ->
                    appendLine()
                    appendLine("    /**")
                    appendLine("     * Key for ${key.name} (${type.displayName}).")
                    appendLine("     */")
                    appendLine("    data object ${key.name.toPascalCase()} : ${source.name}${type.displayName}SourceKey() {")
                    appendLine("        override val keyName: String = \"${key.name}\"")
                    appendLine("    }")
                }
            }

            appendLine("}")
        }
    }

    /**
     * Generates the GeneratedVegasSourceKeyRegistry for string-to-key lookups.
     */
    private fun StringBuilder.generateRegistry() {
        appendLine("/**")
        appendLine(" * Registry for looking up generated SourceKeys by source and key names.")
        appendLine(" */")
        appendLine("object GeneratedVegasSourceKeyRegistry : VegasSourceKeyRegistry<GeneratedVegasDataSource> {")
        appendLine()
        appendLine("    private val keyMap: Map<Pair<String, String>, SourceKey<GeneratedVegasDataSource, *, *>> = mapOf(")

        val entries = mutableListOf<String>()
        manifest.sources.forEach { source ->
            source.keys.forEach { key ->
                entries.add("        (\"${source.name}\" to \"${key.name}\") to ${source.name}Keys.${key.name.toPascalCase()}")
            }
        }
        appendLine(entries.joinToString(",\n"))

        appendLine("    )")
        appendLine()
        appendLine("    override fun findKey(sourceName: String, keyName: String): SourceKey<GeneratedVegasDataSource, *, *>? {")
        appendLine("        return keyMap[sourceName to keyName]")
        appendLine("    }")
        appendLine("}")
    }

    companion object {
        private const val COPYRIGHT_HEADER = """/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// AUTO-GENERATED FILE. DO NOT MODIFY.
// Generated by the Vegas Gradle Plugin."""
    }
}

/**
 * Extension property for getting the Kotlin type name.
 */
private val KeyType.kotlinType: String
    get() = when (this) {
        KeyType.INT -> "Int"
        KeyType.STRING -> "String"
        KeyType.BOOLEAN -> "Boolean"
        KeyType.LONG -> "Long"
        KeyType.DOUBLE -> "Double"
    }

/**
 * Extension property for getting the display name.
 */
private val KeyType.displayName: String
    get() = when (this) {
        KeyType.INT -> "Int"
        KeyType.STRING -> "String"
        KeyType.BOOLEAN -> "Boolean"
        KeyType.LONG -> "Long"
        KeyType.DOUBLE -> "Double"
    }

/**
 * Extension property for getting the SourceKey interface name.
 */
private val KeyType.sourceKeyInterface: String
    get() = when (this) {
        KeyType.INT -> "IntSourceKey"
        KeyType.STRING -> "StringSourceKey"
        KeyType.BOOLEAN -> "BooleanSourceKey"
        KeyType.LONG -> "LongSourceKey"
        KeyType.DOUBLE -> "DoubleSourceKey"
    }

/**
 * Converts a snake_case or camelCase string to PascalCase.
 */
private fun String.toPascalCase(): String {
    return split("_", "-")
        .joinToString("") { word ->
            word.replaceFirstChar { it.uppercase() }
        }
}
