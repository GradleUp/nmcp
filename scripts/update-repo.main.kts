#!/usr/bin/env kotlin

@file:Repository("https://repo.maven.apache.org/maven2/")
@file:Repository("https://storage.googleapis.com/gradleup/m2")
@file:Repository("https://jitpack.io")
@file:DependsOn("com.gradleup.librarian:librarian-cli:0.0.11-SNAPSHOT-f1a6a7856f6bfcd2c9c28e2cff9d316e5f191142")

import com.gradleup.librarian.cli.updateRepo

updateRepo(args) {
    file("README.md") {
        replacePluginId("com.gradleup.nmcp.aggregation")
        replacePluginId("com.gradleup.nmcp")
    }
}
