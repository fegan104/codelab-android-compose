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
    // Extract sources and keys from the manifest rules
    private val sourcesAndKeys: Map<String, Set<SourceKeyInfo>> = manifest.extractSourcesAndKeys()

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
        sourcesAndKeys.keys.forEach { sourceName ->
            val pascalName = sourceName.toPascalCase()
            appendLine("/**")
            appendLine(" * QuerySource for $sourceName data.")
            appendLine(" */")
            appendLine("object ${pascalName}Source : QuerySource")
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

        sourcesAndKeys.forEach { (sourceName, keys) ->
            val pascalSourceName = sourceName.toPascalCase()
            keys.groupBy { it.type }.forEach { (type, _) ->
                val sourceKeyType = "${pascalSourceName}Keys.${pascalSourceName}${type.displayName}SourceKey"
                appendLine()
                appendLine("    /**")
                appendLine("     * Fetches a ${type.displayName} value for the given key from $sourceName.")
                appendLine("     */")
                appendLine("    fun fetch${pascalSourceName}${type.displayName}(source: ${pascalSourceName}Source, key: $sourceKeyType): ${type.kotlinType}?")
            }
        }

        appendLine("}")
    }

    /**
     * Generates key container objects with sealed parent classes and concrete key objects.
     */
    private fun StringBuilder.generateKeyContainers() {
        sourcesAndKeys.forEach { (sourceName, keys) ->
            val pascalSourceName = sourceName.toPascalCase()
            appendLine("/**")
            appendLine(" * Container object for $sourceName source keys.")
            appendLine(" */")
            appendLine("object ${pascalSourceName}Keys {")

            // Group keys by type
            val keysByType = keys.groupBy { it.type }

            // Generate sealed parent class for each type
            keysByType.forEach { (type, typeKeys) ->
                appendLine()
                appendLine("    /**")
                appendLine("     * Sealed parent for $sourceName ${type.displayName} keys.")
                appendLine("     */")
                appendLine("    sealed class ${pascalSourceName}${type.displayName}SourceKey : ${type.sourceKeyInterface}<GeneratedVegasDataSource, ${pascalSourceName}Source> {")
                appendLine("        abstract val keyName: String")
                appendLine()
                appendLine("        override fun resolve(dataSource: GeneratedVegasDataSource, source: ${pascalSourceName}Source): ${type.kotlinType}? {")
                appendLine("            return dataSource.fetch${pascalSourceName}${type.displayName}(source, this)")
                appendLine("        }")
                appendLine("    }")

                // Generate concrete key objects
                typeKeys.forEach { keyInfo ->
                    appendLine()
                    appendLine("    /**")
                    appendLine("     * Key for ${keyInfo.name} (${type.displayName}).")
                    appendLine("     */")
                    appendLine("    data object ${keyInfo.name.toPascalCase()} : ${pascalSourceName}${type.displayName}SourceKey() {")
                    appendLine("        override val keyName: String = \"${keyInfo.name}\"")
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
        sourcesAndKeys.forEach { (sourceName, keys) ->
            val pascalSourceName = sourceName.toPascalCase()
            keys.forEach { keyInfo ->
                entries.add("        (\"$sourceName\" to \"${keyInfo.name}\") to ${pascalSourceName}Keys.${keyInfo.name.toPascalCase()}")
            }
        }
        appendLine(entries.joinToString(",\n"))

        appendLine("    )")
        appendLine()
        appendLine("    private val sourceMap: Map<String, QuerySource> = mapOf(")
        val sources = sourcesAndKeys.keys.map { sourceName ->
            "        \"$sourceName\" to ${sourceName.toPascalCase()}Source"
        }
        appendLine(sources.joinToString(",\n"))
        appendLine("    )")
        appendLine()
        appendLine("    override fun findKey(sourceName: String, keyName: String): SourceKey<GeneratedVegasDataSource, *, *>? {")
        appendLine("        return keyMap[sourceName to keyName]")
        appendLine("    }")
        appendLine()
        appendLine("    override fun findSource(sourceName: String): QuerySource? {")
        appendLine("        return sourceMap[sourceName]")
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
        KeyType.STRING_SET -> "Set<String>"
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
        KeyType.STRING_SET -> "StringSet"
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
        KeyType.STRING_SET -> "StringSetSourceKey"
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
