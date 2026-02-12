package com.fitnow.vegas.compiler

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
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
class VegasCodeGenerator internal constructor(
    private val sourcesAndKeys: Map<String, Set<SourceKeyInfo>>,
    private val promotionGroupIds: Set<String> = emptySet(),
    private val packageName: String = "com.fitnow.vegas.generated"
) {

    // Core type references
    private val vegasCorePackage = "com.fitnow.vegas.core"
    private val querySourceType = ClassName(vegasCorePackage, "QuerySource")
    private val queryDataSourceType = ClassName(vegasCorePackage, "QueryDataSource")
    private val vegasSourceKeyParserType = ClassName(vegasCorePackage, "SourceKeyParser")
    private val sourceKeyType = ClassName(vegasCorePackage, "SourceKey")
    private val generatedDataSourceType = ClassName(packageName, "GeneratedVegasDataSource")

    /**
     * Generates the complete VegasGeneratedApi.kt content as a FileSpec.
     */
    fun generateFileSpec(): FileSpec {
        return FileSpec.builder(packageName, "VegasGeneratedApi")
            .apply {
                // Add PromotionGroupId sealed interface
                if (promotionGroupIds.isNotEmpty()) {
                    addType(generatePromotionGroupId())
                }

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

                // Add SourceKey parser
                addType(generateSourceKeyParser())
            }
            .build()
    }

    /**
     * Generates the PromotionGroupIds sealed interface that extends the core PromotionGroupId,
     * with a data object for each unique promotion group ID.
     */
    private fun generatePromotionGroupId(): TypeSpec {
        val corePromotionGroupIdType = ClassName(vegasCorePackage, "PromotionGroupId")
        val generatedPromotionGroupIdsType = ClassName(packageName, "PromotionGroupIds")

        return TypeSpec.interfaceBuilder("PromotionGroupIds")
            .addKdoc("Sealed interface representing known promotion group IDs.")
            .addModifiers(KModifier.SEALED)
            .addSuperinterface(corePromotionGroupIdType)
            .apply {
                promotionGroupIds.forEach { id ->
                    val className = id.toPascalCase()
                    addType(
                        TypeSpec.objectBuilder(className)
                            .addKdoc("Promotion group ID for %S.", id)
                            .addModifiers(KModifier.DATA)
                            .addSuperinterface(generatedPromotionGroupIdsType)
                            .addProperty(
                                PropertySpec.builder("raw", String::class)
                                    .addKdoc("The raw promotion group ID as it appears in the JSON manifest.")
                                    .addModifiers(KModifier.OVERRIDE)
                                    .initializer("%S", id)
                                    .build()
                            )
                            .build()
                    )
                }
            }
            .build()
    }

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
            .addSuperinterface(queryDataSourceType)
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
                                .addModifiers(KModifier.ABSTRACT, KModifier.SUSPEND)
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

        return TypeSpec.objectBuilder("${pascalSourceName}Keys")
            .addKdoc("Container object for $sourceName source keys.")
            .apply {
                val keysByType = keys.groupBy { it.type }

                keysByType.forEach { (type, typeKeys) ->
                    val sealedClassName = "${pascalSourceName}${type.displayName}SourceKey"
                    val sourceKeyInterface = ClassName(vegasCorePackage, type.sourceKeyInterface)
                        .parameterizedBy(generatedDataSourceType)

                    // Sealed parent class
                    val sealedClass = TypeSpec.classBuilder(sealedClassName)
                        .addKdoc("Sealed parent for $sourceName ${type.displayName} keys.")
                        .addModifiers(KModifier.SEALED)
                        .addSuperinterface(sourceKeyInterface)
                        .addProperty(
                            PropertySpec.builder("raw", String::class)
                                .addModifiers(KModifier.ABSTRACT)
                                .build()
                        )
                        .addFunction(
                            FunSpec.builder("resolve")
                                .addModifiers(KModifier.OVERRIDE, KModifier.SUSPEND)
                                .addParameter("dataSource", generatedDataSourceType)
                                .returns(type.kotlinTypeName.copy(nullable = true))
                                .addStatement(
                                    "return dataSource.fetch${pascalSourceName}${type.displayName}(this)"
                                )
                                .build()
                        )
                        .apply {
                            // Add concrete key classes as nested classes
                            typeKeys.forEach { keyInfo ->
                                val keyClassName = keyInfo.name.toPascalCase()
                                val superclassName = ClassName(packageName, "${pascalSourceName}Keys", sealedClassName)
                                
                                if (keyInfo.hasWhereClause) {
                                    // Companion object with KEY constant for accessing the raw key name
                                    val companionObject = TypeSpec.companionObjectBuilder()
                                        .addProperty(
                                            PropertySpec.builder("KEY", String::class)
                                                .addKdoc("The raw key name as it appears in the JSON manifest.")
                                                .addModifiers(KModifier.CONST)
                                                .initializer("%S", keyInfo.name)
                                                .build()
                                        )
                                        .build()

                                    // Build constructor parameters and properties dynamically from where clause keys
                                    val constructorBuilder = FunSpec.constructorBuilder()
                                    val whereProperties = keyInfo.wherePropertyNames.map { jsonKey ->
                                        val camelName = jsonKey.toCamelCase()
                                        constructorBuilder.addParameter(camelName, String::class)
                                        PropertySpec.builder(camelName, String::class)
                                            .initializer(camelName)
                                            .build()
                                    }

                                    // Generate a data class with where clause parameters
                                    addType(
                                        TypeSpec.classBuilder(keyClassName)
                                            .addKdoc("Key for ${keyInfo.name} (${type.displayName}) with where clause parameters.")
                                            .addModifiers(KModifier.DATA)
                                            .superclass(superclassName)
                                            .primaryConstructor(constructorBuilder.build())
                                            .apply {
                                                whereProperties.forEach { addProperty(it) }
                                            }
                                            .addProperty(
                                                PropertySpec.builder("raw", String::class)
                                                    .addModifiers(KModifier.OVERRIDE)
                                                    .initializer("KEY")
                                                    .build()
                                            )
                                            .addType(companionObject)
                                            .build()
                                    )
                                } else {
                                    // Generate a simple data object (no where clause)
                                    // For objects, raw is directly accessible (e.g., Target.raw)
                                    addType(
                                        TypeSpec.objectBuilder(keyClassName)
                                            .addKdoc("Key for ${keyInfo.name} (${type.displayName}).")
                                            .addModifiers(KModifier.DATA)
                                            .superclass(superclassName)
                                            .addProperty(
                                                PropertySpec.builder("raw", String::class)
                                                    .addKdoc("The raw key name as it appears in the JSON manifest.")
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
     * Generates the GeneratedVegasSourceKeyParser for string-to-key lookups.
     */
    private fun generateSourceKeyParser(): TypeSpec {
        val sourceKeyParserInterface = vegasSourceKeyParserType.parameterizedBy(generatedDataSourceType)
        val sourceKeyWildcard = sourceKeyType.parameterizedBy(
            generatedDataSourceType,
            STAR
        )
        val pairType = Pair::class.asTypeName()
            .parameterizedBy(String::class.asTypeName(), String::class.asTypeName())
        val keyMapType = Map::class.asTypeName()
            .parameterizedBy(pairType, sourceKeyWildcard)
        val whereParamsType = Map::class.asTypeName()
            .parameterizedBy(String::class.asTypeName(), String::class.asTypeName())

        return TypeSpec.objectBuilder("GeneratedVegasSourceKeyParser")
            .addKdoc("Registry for looking up generated SourceKeys by source and key names.")
            .addSuperinterface(sourceKeyParserInterface)
            .addProperty(
                PropertySpec.builder("keyMap", keyMapType)
                    .addModifiers(KModifier.PRIVATE)
                    .initializer(buildKeyMapInitializer())
                    .build()
            )
            .addFunction(
                FunSpec.builder("findKey")
                    .addModifiers(KModifier.OVERRIDE)
                    .addParameter("sourceName", String::class)
                    .addParameter("raw", String::class)
                    .returns(sourceKeyWildcard.copy(nullable = true))
                    .addStatement("return keyMap[sourceName to raw]")
                    .build()
            )
            .addFunction(
                FunSpec.builder("createKeyWithWhere")
                    .addKdoc("Creates a parameterized SourceKey using where clause parameters.")
                    .addModifiers(KModifier.OVERRIDE)
                    .addParameter("sourceName", String::class)
                    .addParameter("keyName", String::class)
                    .addParameter("whereParams", whereParamsType)
                    .returns(sourceKeyWildcard.copy(nullable = true))
                    .addCode(buildCreateKeyWithWhereBody())
                    .build()
            )
            .build()
    }

    /**
     * Builds the body of createKeyWithWhere that handles parameterized keys.
     */
    private fun buildCreateKeyWithWhereBody(): CodeBlock {
        return CodeBlock.builder()
            .beginControlFlow("return when")
            .apply {
                sourcesAndKeys.forEach { (sourceName, keys) ->
                    val pascalSourceName = sourceName.toPascalCase()
                    keys.filter { it.hasWhereClause }.forEach { keyInfo ->
                        val sealedClassName = "${pascalSourceName}${keyInfo.type.displayName}SourceKey"
                        val keyClassName = keyInfo.name.toPascalCase()
                        // Build constructor args dynamically from the where clause property names
                        val constructorArgs = keyInfo.wherePropertyNames.joinToString(", ") { jsonKey ->
                            val camelName = jsonKey.toCamelCase()
                            "$camelName = whereParams[\"$jsonKey\"] ?: \"\""
                        }
                        addStatement(
                            "sourceName == %S && keyName == %S -> %L",
                            sourceName,
                            keyInfo.name,
                            "${pascalSourceName}Keys.$sealedClassName.$keyClassName($constructorArgs)"
                        )
                    }
                }
            }
            .addStatement("else -> null")
            .endControlFlow()
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

    private fun buildSourceMapInitializer(): CodeBlock {
        val mapOf = MemberName("kotlin.collections", "mapOf")
        return CodeBlock.builder()
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
 * Converts a snake_case or kebab-case string to PascalCase.
 */
private fun String.toPascalCase(): String {
    return split("_", "-")
        .joinToString("") { word ->
            word.replaceFirstChar { it.uppercase() }
        }
}

/**
 * Converts a snake_case or kebab-case string to camelCase.
 * E.g., "survey-name" -> "surveyName", "history_type" -> "historyType"
 */
private fun String.toCamelCase(): String {
    return toPascalCase().replaceFirstChar { it.lowercase() }
}
