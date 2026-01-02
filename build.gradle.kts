import com.gradleup.librarian.gradle.Librarian
import nmcp.NmcpAggregationExtension

buildscript {
    dependencies {
        classpath(libs.nmcp.tasks) // only needed to upgrade the version for testing
    }
}
plugins {
    alias(libs.plugins.kgp).apply(false)
    alias(libs.plugins.ksp).apply(false)
    alias(libs.plugins.gratatouille).apply(false)
    alias(libs.plugins.librarian).apply(false)
    alias(libs.plugins.nmcp).apply(false) // only needed to upgrade the version for testing
    alias(libs.plugins.compat).apply(false)
    alias(libs.plugins.serialization).apply(false)
}

Librarian.root(project)

tasks.register("docsNpmInstall", Exec::class.java) {
    enabled = file("docs").exists()

    commandLine("npm", "ci")
    workingDir("docs")
}

tasks.register("docsNpmBuild", Exec::class.java) {
    dependsOn("docsNpmInstall")

    enabled = file("docs").exists()

    commandLine("npm", "run", "build")
    workingDir("docs")
}

tasks.named("librarianStaticContent").configure {
    dependsOn("docsNpmBuild")

    val from = file("docs/dist")
    doLast {
        from.copyRecursively(outputs.files.single(), overwrite = true)
    }
}

extensions.getByType(NmcpAggregationExtension::class.java).localRepository {
    this.name = "test"
    this.path = "build/m2"
}
