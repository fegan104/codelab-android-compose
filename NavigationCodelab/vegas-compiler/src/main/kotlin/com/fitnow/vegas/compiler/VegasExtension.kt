package com.fitnow.vegas.compiler

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property

/**
 * Extension for configuring the Vegas plugin.
 *
 * Usage in build.gradle:
 * ```
 * vegas {
 *     manifestFile = file("src/main/resources/vegas-manifest.json")
 *     packageName = "com.example.app.generated"
 * }
 * ```
 */
interface VegasExtension {
    /**
     * The JSON manifest file that defines sources and keys.
     */
    val manifestFile: RegularFileProperty

    /**
     * The package name for generated code.
     * Defaults to "com.fitnow.vegas.generated".
     */
    val packageName: Property<String>
}
