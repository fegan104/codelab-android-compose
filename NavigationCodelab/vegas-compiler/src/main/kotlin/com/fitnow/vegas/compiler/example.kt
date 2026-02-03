package com.fitnow.vegas.compiler

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.util.Locale

abstract class CodeGenTask : DefaultTask() {

    @InputFile
    val manifestFile: RegularFileProperty = project.objects.fileProperty()

    @OutputDirectory
    val outputDir: DirectoryProperty = project.objects.directoryProperty()

    // --- Type Helpers ---
    private val packageName = "com.fitnow.vegas"
    private val querySourceClassName = ClassName(packageName, "QuerySource")
    private val dataContextClassName = ClassName(packageName, "DataContext")
    private val keyInterfaceClassName = ClassName(packageName, "Key")

    // Maps JSON types to Kotlin classes
    enum class DataType(val kotlinType: TypeName, val keySuffix: String, val fetchPrefix: String) {
        INT(com.squareup.kotlinpoet.INT, "IntKey", "Int"),
        STRING(com.squareup.kotlinpoet.STRING, "StringKey", "String"),
        BOOL(BOOLEAN, "BoolKey", "Bool"),
        STRING_SET(SET.parameterizedBy(com.squareup.kotlinpoet.STRING), "StringSetKey", "StringSet")
    }

    @TaskAction
    fun generate() {
        println("--- Starting Vegas API Generation ---")
        val jsonFile = manifestFile.get().asFile

        // 1. Parse JSON (Using Groovy's JsonSlurper for zero-dep parsing in Gradle)
        val json = JsonSlurper().parse(jsonFile) as Map<String, Any>

        // 2. Analyze the Manifest to extract Metadata
        val sourceMetadata = extractSourceMetadata(json)
        val keyMetadata = extractKeyMetadata(json)

        // 3. Begin File Generation
        val fileSpec = FileSpec.builder(packageName, "VegasApi")

        // 4. Generate QuerySource (Sealed Interface)
        generateQuerySources(fileSpec, sourceMetadata)

        // 5. Generate Key Interfaces (Sealed Interface + Intermediates)
        generateKeyInterfaces(fileSpec)

        // 6. Generate DataContext Interface
        generateDataContext(fileSpec, keyMetadata)

        // 7. Generate Concrete Keys (The Manifest Objects)
        generateConcreteKeys(fileSpec, keyMetadata)

        // 8. Write to Disk
        fileSpec.build().writeTo(outputDir.get().asFile)
        println("--- Generation Complete: ${outputDir.get().asFile.path} ---")
    }

    // ========================================================================
    // Analysis Logic
    // ========================================================================

    data class SourceDefinition(val name: String, val whereKeys: Set<String>)
    data class KeyDefinition(val source: String, val name: String, val type: DataType)

    private fun extractSourceMetadata(json: Map<String, Any>): Map<String, SourceDefinition> {
        val sources = mutableMapOf<String, MutableSet<String>>()

        // Recursive function to find all "lhs" objects
        fun findRules(obj: Any?) {
            if (obj is Map<*, *>) {
                if (obj.containsKey("lhs")) {
                    val lhs = obj["lhs"] as Map<String, Any>
                    val sourceName = lhs["source"] as String
                    val where = lhs["where"] as? Map<String, Any>

                    val existing = sources.getOrPut(sourceName) { mutableSetOf() }
                    where?.keys?.forEach { existing.add(it) }
                }
                obj.values.forEach { findRules(it) }
            } else if (obj is List<*>) {
                obj.forEach { findRules(it) }
            }
        }

        findRules(json)

        return sources.mapValues { (name, keys) ->
            SourceDefinition(name, keys)
        }
    }

    private fun extractKeyMetadata(json: Map<String, Any>): List<KeyDefinition> {
        val keys = mutableListOf<KeyDefinition>()

        fun findRules(obj: Any?) {
            if (obj is Map<*, *>) {
                if (obj.containsKey("lhs") && obj.containsKey("operator")) {
                    val lhs = obj["lhs"] as Map<String, Any>
                    val operator = obj["operator"] as String
                    val source = lhs["source"] as String
                    val keyName = lhs["key"] as String

                    val type = when {
                        operator.startsWith("int") -> DataType.INT
                        operator.startsWith("bool") -> DataType.BOOL
                        operator.startsWith("stringSet") -> DataType.STRING_SET
                        else -> DataType.STRING
                    }
                    keys.add(KeyDefinition(source, keyName, type))
                }
                obj.values.forEach { findRules(it) }
            } else if (obj is List<*>) {
                obj.forEach { findRules(it) }
            }
        }
        findRules(json)
        return keys.distinct()
    }

    // ========================================================================
    // Code Generation Logic
    // ========================================================================

    private fun generateQuerySources(fileSpec: FileSpec.Builder, sources: Map<String, SourceDefinition>) {
        // 1. Create the Builder for the parent Sealed Interface
        val sourceInterfaceBuilder = TypeSpec.interfaceBuilder("QuerySource")
            .addModifiers(KModifier.SEALED)

        sources.values.forEach { sourceDef ->
            val className = sourceDef.name.capitalize()

            // 2. Create the builder for the inner type (User, PromotionHistory)
            val sourceSpecBuilder = if (sourceDef.whereKeys.isEmpty()) {
                // Case A: Singleton Object (e.g. object User)
                TypeSpec.objectBuilder(className)
            } else {
                // Case B: Data Class (e.g. data class PromotionHistory)
                val constructor = FunSpec.constructorBuilder()
                sourceDef.whereKeys.forEach { key ->
                    constructor.addParameter(key, STRING)
                }

                TypeSpec.classBuilder(className)
                    .addModifiers(KModifier.DATA)
                    .primaryConstructor(constructor.build())
                    .apply {
                        // Add properties for the constructor params
                        sourceDef.whereKeys.forEach { key ->
                            addProperty(PropertySpec.builder(key, STRING)
                                .initializer(key)
                                .build())
                        }
                    }
            }

            // 3. Make it implement the parent interface
            sourceSpecBuilder.addSuperinterface(querySourceClassName)

            // 4. Add the inner type to the PARENT builder (Nesting!)
            sourceInterfaceBuilder.addType(sourceSpecBuilder.build())
        }

        // 5. Finally, add the fully constructed parent interface to the file
        fileSpec.addType(sourceInterfaceBuilder.build())
    }

    private fun generateKeyInterfaces(fileSpec: FileSpec.Builder) {
        val sType = TypeVariableName("S", querySourceClassName)
        val tType = TypeVariableName("T", KModifier.OUT)

        val resolveFun = FunSpec.builder("resolve")
            .addModifiers(KModifier.ABSTRACT)
            .addParameter("context", dataContextClassName)
            .addParameter("source", sType)
            .returns(tType.copy(nullable = true)) // Nullable for Default support!
            .build()

        val keyInterface = TypeSpec.interfaceBuilder("Key")
            .addModifiers(KModifier.SEALED)
            .addTypeVariable(sType)
            .addTypeVariable(tType)
            .addFunction(resolveFun)

        // Add Intermediate Interfaces (IntKey, StringKey, etc.)
        DataType.entries.forEach { dataType ->
            val subInterface = TypeSpec.interfaceBuilder(dataType.keySuffix)
                .addTypeVariable(sType)
                .addSuperinterface(keyInterfaceClassName.parameterizedBy(sType, dataType.kotlinType))
                .build()
            keyInterface.addType(subInterface)
        }

        fileSpec.addType(keyInterface.build())
    }

    private fun generateDataContext(
        fileSpec: FileSpec.Builder,
        keys: List<KeyDefinition>
    ) {
        val interfaceBuilder = TypeSpec.interfaceBuilder("DataContext")

        val usedCombinations = keys.map { it.source to it.type }.distinct()

        usedCombinations.forEach { (sourceName, dataType) ->
            val sourceClassName = sourceName.capitalize()

            // 1. Reference the Source (QuerySource.User)
            val sourceClass = querySourceClassName.nestedClass(sourceClassName)

            // 2. Reference the Specific Sealed Key Class (UserKeys.UserIntKey)
            // OLD (Wrong): val keyParamType = keyInterfaceClassName.nestedClass(dataType.keySuffix).parameterizedBy(sourceClass)

            val keysContainer = ClassName(packageName, "${sourceClassName}Keys")
            val keyParamType = keysContainer.nestedClass("$sourceClassName${dataType.keySuffix}")

            val funName = "fetch$sourceClassName${dataType.fetchPrefix}"

            val func = FunSpec.builder(funName)
                .addModifiers(KModifier.ABSTRACT)
                .addParameter("source", sourceClass)
                .addParameter("key", keyParamType) // Now uses specific type!
                .returns(dataType.kotlinType.copy(nullable = true))
                .build()

            interfaceBuilder.addFunction(func)
        }

        fileSpec.addType(interfaceBuilder.build())
    }

    private fun generateConcreteKeys(
        fileSpec: FileSpec.Builder,
        keys: List<KeyDefinition>
    ) {
        val keysBySource = keys.groupBy { it.source }

        keysBySource.forEach { (sourceName, sourceKeys) ->
            val sourceClassName = sourceName.capitalize()

            // FIX: Reference QuerySource.User, not just User
            // Old: val sourceTypeName = ClassName(packageName, sourceClassName)
            val sourceTypeName = querySourceClassName.nestedClass(sourceClassName)

            // The rest remains the same...
            val containerBuilder = TypeSpec.objectBuilder("${sourceClassName}Keys")

            val keysByType = sourceKeys.groupBy { it.type }

            keysByType.forEach { (dataType, specificKeys) ->
                val sealedParentName = "$sourceClassName${dataType.keySuffix}"

                val parentSpec = TypeSpec.classBuilder(sealedParentName)
                    .addModifiers(KModifier.SEALED)
                    .addSuperinterface(
                        keyInterfaceClassName.nestedClass(dataType.keySuffix)
                            .parameterizedBy(sourceTypeName)
                    )

                val resolveSpec = FunSpec.builder("resolve")
                    .addModifiers(KModifier.OVERRIDE)
                    .addParameter("context", dataContextClassName)
                    .addParameter("source", sourceTypeName)
                    .returns(dataType.kotlinType.copy(nullable = true))
                    .addStatement("return context.fetch$sourceClassName${dataType.fetchPrefix}(source, this)")
                    .build()

                parentSpec.addFunction(resolveSpec)

                specificKeys.forEach { keyDef ->
                    val keyObjectName = keyDef.name.capitalize()
                    val objectSpec = TypeSpec.objectBuilder(keyObjectName)
                        .superclass(ClassName(packageName, "${sourceClassName}Keys", sealedParentName))
                        .build()

                    containerBuilder.addType(objectSpec)
                }

                containerBuilder.addType(parentSpec.build())
            }

            fileSpec.addType(containerBuilder.build())
        }
    }

    // String extension helper
    private fun String.capitalize() = replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}