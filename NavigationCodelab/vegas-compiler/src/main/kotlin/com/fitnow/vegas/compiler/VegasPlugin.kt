package com.fitnow.vegas.compiler

import java.io.File
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer

/**
 * Vegas Gradle Plugin that generates type-safe Kotlin code from a JSON manifest.
 *
 * This plugin:
 * 1. Creates a `vegas` extension for configuration
 * 2. Registers a `generateVegasApi` task
 * 3. Wires the generated sources into the build
 *
 * Usage:
 * ```
 * plugins {
 *     id 'com.fitnow.vegas'
 * }
 *
 * vegas {
 *     manifestFile = file("src/main/resources/vegas-manifest.json")
 *     packageName = "com.example.generated"
 * }
 * ```
 */
class VegasPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        // Create the extension
        val extension = project.extensions.create(
            "vegas",
            VegasExtension::class.java
        )

        // Set default values
        extension.packageName.convention("com.fitnow.vegas.generated")

        // Define output directory
        val outputDir = project.layout.buildDirectory.dir("generated/vegas/kotlin")

        // Register the generation task
        val generateTask = project.tasks.register(
            "generateVegasApi",
            VegasGenerateTask::class.java
        ) { task ->
            task.description = "Generates type-safe Vegas API from JSON manifest"
            task.group = "vegas"

            task.manifestFile.set(extension.manifestFile)
            task.packageName.set(extension.packageName)
            task.outputDir.set(outputDir)
        }

        // Wire generated sources into the build
        project.afterEvaluate {
            // For Kotlin JVM projects
            project.extensions.findByType(SourceSetContainer::class.java)?.let { sourceSets ->
                sourceSets.findByName("main")?.let { sourceSet ->
                    sourceSet.java.srcDir(outputDir)
                }
            }

            // For Android projects (configured via reflection to avoid AGP dependency)
            addAndroidSourceSet(project, outputDir.get().asFile)

            // Make compileKotlin depend on generation
            project.tasks.findByName("compileKotlin")?.dependsOn(generateTask)

            // For Android projects
            project.tasks.findByName("preBuild")?.dependsOn(generateTask)
        }
    }

    private fun addAndroidSourceSet(project: Project, outputDir: File) {
        val androidExtension = project.extensions.findByName("android") ?: return
        val sourceSets = androidExtension.javaClass.methods
            .firstOrNull { it.name == "getSourceSets" }
            ?.invoke(androidExtension) as? NamedDomainObjectContainer<*>
            ?: return
        val mainSourceSet = sourceSets.findByName("main") ?: return
        val javaDirSet = mainSourceSet.javaClass.methods
            .firstOrNull { it.name == "getJava" }
            ?.invoke(mainSourceSet)
            ?: return
        javaDirSet.javaClass.methods
            .firstOrNull { it.name == "srcDir" }
            ?.invoke(javaDirSet, outputDir)
    }
}
