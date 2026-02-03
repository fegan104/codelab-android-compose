package com.fitnow.vegas.compiler

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName

/**
 * Generates type-safe Kotlin code from a Vegas manifest using KotlinPoet.
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
    private val sourcesAndKeys: Map<String, Set<SourceKeyInfo>> = manifest.extractSourcesAndKeys()

    // Core type references
    private val vegasCorePackage = "com.fitnow.vegas.core"
    private val querySourceType = ClassName(vegasCorePackage, "QuerySource")
    private val vegasQueryDataSourceType = ClassName(vegasCorePackage, "VegasQueryDataSource")
    private val vegasSourceKeyRegistryType = ClassName(vegasCorePackage, "VegasSourceKeyRegistry")
    private val sourceKeyType = ClassName(vegasCorePackage, "SourceKey")
    private val generatedDataSourceType = ClassName(packageName, "GeneratedVegasDataSource")

    /**
     * Generates the complete VegasGeneratedApi.kt content as a FileSpec.
     */
    fun generateFileSpec(): FileSpec {
        return FileSpec.builder(packageName, "VegasGeneratedApi")
            .addFileComment(COPYRIGHT_HEADER)
            .apply {
                // Add QuerySource objects
                sourcesAndKeys.keys.forEach { sourceName ->
                    addType(generateQuerySource(sourceName))
                }

                // Add GeneratedVegasDataSource interface
                addType(generateDataSourceInterface())

                // Add key container objects
                sourcesAndKeys.forEach { (sourceName, keys) ->
                    addType(generateKeyContainer(sourceName, keys))
                }

                // Add registry
                addType(generateRegistry())
            }
            .build()
    }

    /**
     * Generates the code as a String (for backwards compatibility).
     */
    fun generate(): String = generateFileSpec().toString()

    /**
     * Generates a QuerySource object for a source.
     */
    private fun generateQuerySource(sourceName: String): TypeSpec {
        val pascalName = sourceName.toPascalCase()
        return TypeSpec.objectBuilder("${pascalName}Source")
            .addKdoc("QuerySource for $sourceName data.")
            .addSuperinterface(querySourceType)
            .build()
    }

    /**
     * Generates the GeneratedVegasDataSource interface with typed fetch methods.
     */
    private fun generateDataSourceInterface(): TypeSpec {
        return TypeSpec.interfaceBuilder("GeneratedVegasDataSource")
            .addKdoc(
                """
                Generated data source interface with typed fetch methods.
                Implement this interface to provide data for Vegas rules.
                """.trimIndent()
            )
            .addSuperinterface(vegasQueryDataSourceType)
            .apply {
                sourcesAndKeys.forEach { (sourceName, keys) ->
                    val pascalSourceName = sourceName.toPascalCase()
                    val sourceClassName = ClassName(packageName, "${pascalSourceName}Source")

                    keys.groupBy { it.type }.forEach { (type, _) ->
                        val sourceKeyClassName = ClassName(
                            packageName,
                            "${pascalSourceName}Keys",
                            "${pascalSourceName}${type.displayName}SourceKey"
                        )

                        addFunction(
                            FunSpec.builder("fetch${pascalSourceName}${type.displayName}")
                                .addKdoc("Fetches a ${type.displayName} value for the given key from $sourceName.")
                                .addModifiers(KModifier.ABSTRACT)
                                .addParameter("source", sourceClassName)
                                .addParameter("key", sourceKeyClassName)
                                .returns(type.kotlinTypeName.copy(nullable = true))
                                .build()
                        )
                    }
                }
            }
            .build()
    }

    /**
     * Generates a key container object with sealed parent classes and concrete key objects.
     */
    private fun generateKeyContainer(sourceName: String, keys: Set<SourceKeyInfo>): TypeSpec {
        val pascalSourceName = sourceName.toPascalCase()
        val sourceClassName = ClassName(packageName, "${pascalSourceName}Source")

        return TypeSpec.objectBuilder("${pascalSourceName}Keys")
            .addKdoc("Container object for $sourceName source keys.")
            .apply {
                val keysByType = keys.groupBy { it.type }

                keysByType.forEach { (type, typeKeys) ->
                    val sealedClassName = "${pascalSourceName}${type.displayName}SourceKey"
                    val sourceKeyInterface = ClassName(vegasCorePackage, type.sourceKeyInterface)
                        .parameterizedBy(generatedDataSourceType, sourceClassName)

                    // Sealed parent class
                    val sealedClass = TypeSpec.classBuilder(sealedClassName)
                        .addKdoc("Sealed parent for $sourceName ${type.displayName} keys.")
                        .addModifiers(KModifier.SEALED)
                        .addSuperinterface(sourceKeyInterface)
                        .addProperty(
                            PropertySpec.builder("keyName", String::class)
                                .addModifiers(KModifier.ABSTRACT)
                                .build()
                        )
                        .addFunction(
                            FunSpec.builder("resolve")
                                .addModifiers(KModifier.OVERRIDE)
                                .addParameter("dataSource", generatedDataSourceType)
                                .addParameter("source", sourceClassName)
                                .returns(type.kotlinTypeName.copy(nullable = true))
                                .addStatement(
                                    "return dataSource.fetch${pascalSourceName}${type.displayName}(source, this)"
                                )
                                .build()
                        )
                        .apply {
                            // Add concrete key classes as nested classes
                            typeKeys.forEach { keyInfo ->
                                val keyClassName = keyInfo.name.toPascalCase()
                                val superclassName = ClassName(packageName, "${pascalSourceName}Keys", sealedClassName)
                                
                                if (keyInfo.hasWhereClause) {
                                    // Generate a data class with where clause parameters
                                    addType(
                                        TypeSpec.classBuilder(keyClassName)
                                            .addKdoc("Key for ${keyInfo.name} (${type.displayName}) with where clause parameters.")
                                            .addModifiers(KModifier.DATA)
                                            .superclass(superclassName)
                                            .primaryConstructor(
                                                FunSpec.constructorBuilder()
                                                    .addParameter("historyType", String::class)
                                                    .addParameter("id", String::class)
                                                    .build()
                                            )
                                            .addProperty(
                                                PropertySpec.builder("historyType", String::class)
                                                    .initializer("historyType")
                                                    .build()
                                            )
                                            .addProperty(
                                                PropertySpec.builder("id", String::class)
                                                    .initializer("id")
                                                    .build()
                                            )
                                            .addProperty(
                                                PropertySpec.builder("keyName", String::class)
                                                    .addModifiers(KModifier.OVERRIDE)
                                                    .initializer("%S", keyInfo.name)
                                                    .build()
                                            )
                                            .build()
                                    )
                                } else {
                                    // Generate a simple object (no where clause)
                                    addType(
                                        TypeSpec.objectBuilder(keyClassName)
                                            .addKdoc("Key for ${keyInfo.name} (${type.displayName}).")
                                            .addModifiers(KModifier.DATA)
                                            .superclass(superclassName)
                                            .addProperty(
                                                PropertySpec.builder("keyName", String::class)
                                                    .addModifiers(KModifier.OVERRIDE)
                                                    .initializer("%S", keyInfo.name)
                                                    .build()
                                            )
                                            .build()
                                    )
                                }
                            }
                        }
                        .build()

                    addType(sealedClass)
                }
            }
            .build()
    }

    /**
     * Generates the GeneratedVegasSourceKeyRegistry for string-to-key lookups.
     */
    private fun generateRegistry(): TypeSpec {
        val registryInterface = vegasSourceKeyRegistryType.parameterizedBy(generatedDataSourceType)
        val sourceKeyWildcard = sourceKeyType.parameterizedBy(
            generatedDataSourceType,
            STAR,
            STAR
        )
        val pairType = Pair::class.asTypeName()
            .parameterizedBy(String::class.asTypeName(), String::class.asTypeName())
        val keyMapType = Map::class.asTypeName()
            .parameterizedBy(pairType, sourceKeyWildcard)
        val sourceMapType = Map::class.asTypeName()
            .parameterizedBy(String::class.asTypeName(), querySourceType)

        return TypeSpec.objectBuilder("GeneratedVegasSourceKeyRegistry")
            .addKdoc("Registry for looking up generated SourceKeys by source and key names.")
            .addSuperinterface(registryInterface)
            .addProperty(
                PropertySpec.builder("keyMap", keyMapType)
                    .addModifiers(KModifier.PRIVATE)
                    .initializer(buildKeyMapInitializer())
                    .build()
            )
            .addProperty(
                PropertySpec.builder("sourceMap", sourceMapType)
                    .addModifiers(KModifier.PRIVATE)
                    .initializer(buildSourceMapInitializer())
                    .build()
            )
            .addFunction(
                FunSpec.builder("findKey")
                    .addModifiers(KModifier.OVERRIDE)
                    .addParameter("sourceName", String::class)
                    .addParameter("keyName", String::class)
                    .returns(sourceKeyWildcard.copy(nullable = true))
                    .addStatement("return keyMap[sourceName to keyName]")
                    .build()
            )
            .addFunction(
                FunSpec.builder("findSource")
                    .addModifiers(KModifier.OVERRIDE)
                    .addParameter("sourceName", String::class)
                    .returns(querySourceType.copy(nullable = true))
                    .addStatement("return sourceMap[sourceName]")
                    .build()
            )
            .build()
    }

    private fun buildKeyMapInitializer(): CodeBlock {
        val mapOf = MemberName("kotlin.collections", "mapOf")
        return CodeBlock.builder()
            .add("%M(\n", mapOf)
            .indent()
            .apply {
                val entries = mutableListOf<String>()
                sourcesAndKeys.forEach { (sourceName, keys) ->
                    val pascalSourceName = sourceName.toPascalCase()
                    keys.forEach { keyInfo ->
                        // Skip keys with where clauses - they're data classes requiring constructor args
                        if (!keyInfo.hasWhereClause) {
                            // Key objects are nested inside the sealed class, e.g., UserKeys.UserStringSetSourceKey.Target
                            val sealedClassName = "${pascalSourceName}${keyInfo.type.displayName}SourceKey"
                            entries.add("(\"$sourceName\" to \"${keyInfo.name}\") to ${pascalSourceName}Keys.$sealedClassName.${keyInfo.name.toPascalCase()}")
                        }
                    }
                }
                entries.forEachIndexed { index, entry ->
                    if (index < entries.size - 1) {
                        addStatement("$entry,")
                    } else {
                        addStatement(entry)
                    }
                }
            }
            .unindent()
            .add(")")
            .build()
    }

    private fun buildSourceMapInitializer(): com.squareup.kotlinpoet.CodeBlock {
        val mapOf = MemberName("kotlin.collections", "mapOf")
        return com.squareup.kotlinpoet.CodeBlock.builder()
            .add("%M(\n", mapOf)
            .indent()
            .apply {
                val sources = sourcesAndKeys.keys.toList()
                sources.forEachIndexed { index, sourceName ->
                    val entry = "\"$sourceName\" to ${sourceName.toPascalCase()}Source"
                    if (index < sources.size - 1) {
                        addStatement("$entry,")
                    } else {
                        addStatement(entry)
                    }
                }
            }
            .unindent()
            .add(")")
            .build()
    }

    companion object {
        private val STAR = com.squareup.kotlinpoet.STAR

        private const val COPYRIGHT_HEADER = """
Copyright 2026 The Android Open Source Project

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

AUTO-GENERATED FILE. DO NOT MODIFY.
Generated by the Vegas Gradle Plugin."""
    }
}

/**
 * Extension property for getting the Kotlin TypeName.
 */
private val KeyType.kotlinTypeName: com.squareup.kotlinpoet.TypeName
    get() = when (this) {
        KeyType.INT -> Int::class.asTypeName()
        KeyType.STRING -> String::class.asTypeName()
        KeyType.BOOLEAN -> Boolean::class.asTypeName()
        KeyType.LONG -> Long::class.asTypeName()
        KeyType.DOUBLE -> Double::class.asTypeName()
        KeyType.STRING_SET -> Set::class.asTypeName().parameterizedBy(String::class.asTypeName())
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
