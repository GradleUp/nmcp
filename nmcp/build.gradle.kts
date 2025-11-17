import com.gradleup.librarian.gradle.Librarian

plugins {
    alias(libs.plugins.kgp)
    alias(libs.plugins.ksp)
    id("com.gradleup.gratatouille.wiring")
}

Librarian.module(project)

gratatouille {
    codeGeneration {
        addDependencies.set(false)
    }
    pluginLocalPublication("com.gradleup.nmcp.settings")
}

dependencies {
    gratatouille(project(":nmcp-tasks"))
    compileOnly(libs.gradle.min)
    implementation(libs.gratatouille.wiring.runtime)
    testImplementation(libs.kotlin.test)
}
