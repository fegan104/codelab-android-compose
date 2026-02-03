package com.fitnow.vegas.compiler

import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Gradle task that generates type-safe Kotlin code from a Vegas JSON manifest.
 */
@CacheableTask
abstract class VegasGenerateTask : DefaultTask() {

    /**
     * The input JSON manifest file.
     */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val manifestFile: RegularFileProperty

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
        val manifestContent = manifestFile.get().asFile.readText()
        val manifest = json.decodeFromString<VegasManifest>(manifestContent)

        val generator = VegasCodeGenerator(
            manifest = manifest,
            packageName = packageName.get()
        )

        // Use KotlinPoet's FileSpec to write the generated code
        val fileSpec = generator.generateFileSpec()
        val outputDirectory = outputDir.get().asFile

        // KotlinPoet handles package directory structure automatically
        fileSpec.writeTo(outputDirectory)

        val sourcesAndKeys = manifest.extractSourcesAndKeys()
        val totalKeys = sourcesAndKeys.values.sumOf { it.size }
        val outputFile = outputDirectory
            .resolve(packageName.get().replace(".", "/"))
            .resolve("VegasGeneratedApi.kt")

        logger.lifecycle("Vegas: Generated ${sourcesAndKeys.size} sources with $totalKeys keys")
        logger.lifecycle("Vegas: Output written to ${outputFile.absolutePath}")
    }
}
