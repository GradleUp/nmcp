import com.gradleup.librarian.gradle.Librarian

plugins {
    alias(libs.plugins.kgp)
    alias(libs.plugins.ksp)
    id("com.gradleup.gratatouille")
}

Librarian.module(project)

gratatouille {
    addDependencies = false
    pluginLocalPublication("com.gradleup.nmcp.settings")
}

dependencies {
    gratatouille(project(":nmcp-tasks"))
    compileOnly(libs.gradle.min)
    implementation(libs.gratatouille.runtime)

    testImplementation(libs.kotlin.test)
    testImplementation(gradleTestKit())
}

tasks.withType(Test::class.java).configureEach {
    dependsOn(":nmcpPublishAggregationToTestRepository")
}
