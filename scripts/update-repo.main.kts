#!/usr/bin/env kotlin

@file:Repository("https://repo.maven.apache.org/maven2/")
@file:Repository("https://dl.google.com/android/maven2/")
@file:Repository("https://storage.googleapis.com/gradleup/m2")
//@file:Repository("file://~/.m2/repository")
@file:DependsOn("com.gradleup.librarian:librarian-cli:0.2.2-SNAPSHOT-b89fe292839b757bf152e8369a88991052d32d0b")

import com.gradleup.librarian.repo.updateRepo

updateRepo(
    setVersion = {
        file("nmcp/testProjec/duplicate-name/build.gradle.kts") {
            replacePluginVersion("com.gradleup.nmcp.aggregation")
        }
    },
    setVersionInDocs = {
        file("docs/src/content/docs/index.mdx") {
            replacePluginVersion("com.gradleup.nmcp.settings")
            replacePluginVersion("com.gradleup.nmcp.aggregation")
            replacePluginVersion("com.gradleup.nmcp")
        }
        file("docs/src/content/docs/manual-configuration.mdx") {
            replacePluginVersion("com.gradleup.nmcp.settings")
            replacePluginVersion("com.gradleup.nmcp.aggregation")
            replacePluginVersion("com.gradleup.nmcp")
        }
    },
)
