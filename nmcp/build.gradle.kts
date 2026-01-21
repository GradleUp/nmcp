import com.gradleup.librarian.gradle.Librarian
import kotlin.jvm.java
import tapmoc.TapmocExtension

plugins {
    alias(libs.plugins.kgp)
    alias(libs.plugins.ksp)
    id("com.gradleup.gratatouille")
}

Librarian.module(project)

extensions.getByType(TapmocExtension::class.java).apply {
    // Override the default Kotlin version to work with older Gradle
    kotlin("2.1.0")
    //kotlin(kotlinVersionForGradle(libs.versions.gradle.api.get().toString()))
}

gratatouille {
    addDependencies = false
    pluginLocalPublication("com.gradleup.nmcp.settings")
}

dependencies {
    gratatouille(project(":nmcp-tasks"))
    compileOnly(libs.gradle.min)
    compileOnly(libs.agp)
    implementation(libs.gratatouille.runtime)

    testImplementation(libs.kotlin.test)
    testImplementation(gradleTestKit())
}

tasks.withType(Test::class.java).configureEach {
    dependsOn(":nmcpPublishAggregationToTestRepository")
}
