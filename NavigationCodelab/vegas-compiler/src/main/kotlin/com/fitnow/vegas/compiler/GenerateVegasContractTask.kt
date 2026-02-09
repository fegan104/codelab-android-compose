package com.fitnow.vegas.compiler

import com.fitnow.vegas.core.PromotionGroupJson
import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Gradle task that generates type-safe Kotlin code from a promotion group JSON.
 */
@CacheableTask
abstract class GenerateVegasContractTask : DefaultTask() {

    /**
     * The input directory containing JSON manifest files.
     */
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val promotionsDirectory: DirectoryProperty

    /**
     * The package name for generated code.
     */
    @get:Input
    abstract val packageName: Property<String>

    /**
     * The output directory for generated source files.
     */
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @TaskAction
    fun generate() {
        val directory = promotionsDirectory.get().asFile
        val jsonFiles = directory.listFiles { file -> file.extension == "json" } ?: emptyArray()

        val aggregatedSourcesAndKeys = mutableMapOf<String, MutableSet<SourceKeyInfo>>()
        val promotionGroupIds = mutableSetOf<String>()

        jsonFiles.forEach { file ->
            val content = file.readText()
            val promoGroup = json.decodeFromString<PromotionGroupJson>(content)
            promotionGroupIds.add(promoGroup.id)
            val fileSources = promoGroup.extractSourcesAndKeys()

            fileSources.forEach { (source, keys) ->
                aggregatedSourcesAndKeys
                    .getOrPut(source) { mutableSetOf() }
                    .addAll(keys)
            }
        }

        val generator = VegasCodeGenerator(
            sourcesAndKeys = aggregatedSourcesAndKeys,
            promotionGroupIds = promotionGroupIds,
            packageName = packageName.get()
        )

        // Use KotlinPoet's FileSpec to write the generated code
        val fileSpec = generator.generateFileSpec()
        val outputDirectory = outputDir.get().asFile
        fileSpec.writeTo(outputDirectory)

        val totalKeys = aggregatedSourcesAndKeys.values.sumOf { it.size }
        val outputFile = outputDirectory
            .resolve(packageName.get().replace(".", "/"))
            .resolve("VegasGeneratedApi.kt")

        logger.lifecycle("Vegas: Parsed ${jsonFiles.size} manifest files")
        logger.lifecycle("Vegas: Generated ${aggregatedSourcesAndKeys.size} sources with $totalKeys keys")
        logger.lifecycle("Vegas: Output written to ${outputFile.absolutePath}")
    }
}
