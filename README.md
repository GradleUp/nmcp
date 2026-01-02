<header>
  <div align="center">
    <img src="https://raw.githubusercontent.com/GradleUp/nmcp/refs/heads/main/docs/public/logo.svg" height="100" alt="Nmcp Logo">
  </div>
    <div align="center">

[![Slack](https://img.shields.io/static/v1?label=gradle-community&message=gradleup&color=A97BFF&logo=slack&style=flat-square)](https://gradle-community.slack.com/archives/C07GJEMUZDH)

[![Maven Central](https://img.shields.io/maven-central/v/com.gradleup.nmcp/nmcp?style=flat-square)](https://central.sonatype.com/namespace/com.gradleup.nmcp)
[![Maven Snapshots](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Fcentral.sonatype.com%2Frepository%2Fmaven-snapshots%2Fcom%2Fgradleup%2Fnmcp%2Fnmcp%2Fmaven-metadata.xml&style=flat-square&label=snapshots&color=%2315252D&strategy=latestProperty)](https://central.sonatype.com/repository/maven-snapshots/com/gradleup/nmcp/)

  </div> 
</header>


---

Nmcp (New Maven Central Publishing) is a plugin that uses the new [Central Portal publisher API](https://central.sonatype.org/publish/publish-portal-api/) to publish existing publications to Maven Central and Snapshots.

Nmcp is compatible with modern Gradle development practices:
* Compatible with [Configuration Cache](https://docs.gradle.org/current/userguide/configuration_cache_enabling.html).
* Compatible with [Isolated projects](https://docs.gradle.org/current/userguide/isolated_projects.html).
* Compatible with [multi-project builds](https://docs.gradle.org/current/userguide/multi_project_builds.html).
* Uses [classloader isolation](https://mbonnin.net/2025-08-24_isolation_101/) to prevent conflicts with other plugins.
* Uses [Gradle 8.8 lifecycle callbacks ](https://docs.gradle.org/8.8/release-notes.html) for easy configuration.

Nmcp is used in [Apollo Kotlin](https://github.com/apollographql/apollo-kotlin), [Koin](https://github.com/InsertKoinIO/koin), [Kotest](https://github.com/kotest/kotest), [Minestorm](https://github.com/Minestom/Minestom) and [many other projects](https://github.com/search?q=com.gradleup.nmcp&type=code).

## 📚 Documentation

See the project website for documentation:<br/>
[https://gradleup.github.io/nmcp/](https://gradleup.github.io/nmcp/)

The Kdoc API reference can be found at:<br/>
[https://gradleup.github.io/nmcp/kdoc](https://gradleup.github.io/nmcp/kdoc)
