#!/usr/bin/env kotlin

@file:Repository("https://repo.maven.apache.org/maven2/")
@file:Repository("https://dl.google.com/android/maven2/")
@file:Repository("https://storage.googleapis.com/gradleup/m2")
@file:Repository("https://jitpack.io")
//@file:Repository("file://~/.m2/repository")
@file:DependsOn("com.gradleup.librarian:librarian-cli:0.2.2-SNAPSHOT-f89d61cb6d4bd03bb9f48d1c8220262c0d02094c")

import com.gradleup.librarian.cli.updateRepo

updateRepo(
    args,
    setVersion = {
        file("nmcp/testProjec/duplicate-name/build.gradle.kts") {
            replacePluginVersion("com.gradleup.nmcp.aggregation")
        }
    },
    setDocsVersion = {
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
