import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.0"
    id("java-gradle-plugin")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenLocal()
    google()
    mavenCentral()
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

gradlePlugin {
    plugins {
        create("vegasPlugin") {
            id = "com.fitnow.vegas"
            implementationClass = "com.fitnow.vegas.compiler.VegasPlugin"
        }
    }
}

dependencies {
    implementation(gradleApi())
    implementation("com.fitnow:vegas-core:0.0.1")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("com.squareup:kotlinpoet:2.0.0")
    compileOnly("com.android.tools.build:gradle:8.13.1")
}
