#!/usr/bin/env kotlin

@file:Repository("https://repo.maven.apache.org/maven2/")
@file:Repository("https://storage.googleapis.com/gradleup/m2")
@file:Repository("https://jitpack.io")
@file:DependsOn("com.gradleup.librarian:librarian-cli:0.0.11-SNAPSHOT-e8ab4653f68118d36889519f430f1fcd118745e3")

import com.gradleup.librarian.cli.updateRepo

updateRepo(args) {
  file("README.md") {
    val file = File("README.md")
    val newContent = file.readText()
        .replace(Regex("\\.version\\(\"[^\"]*\"\\)"), ".version(\"$version\")")
    file.writeText(newContent)
  }
}
