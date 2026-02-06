package com.fitnow.vegas.compiler

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property

/**
 * Extension for configuring the Vegas plugin.
 *
 * Usage in build.gradle:
 * ```
 * vegas {
 *     promotionsDirectory = file("src/main/assets/promotions")
 *     packageName = "com.example.app.generated"
 * }
 * ```
 */
interface VegasExtension {
    /**
     * The directory containing JSON promotion manifests.
     * All .json files in this directory will be parsed.
     */
    val promotionsDirectory: DirectoryProperty

    /**
     * The package name for generated code.
     * Defaults to "com.fitnow.vegas.generated".
     */
    val packageName: Property<String>
}
