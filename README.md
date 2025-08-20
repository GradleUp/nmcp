_New accounts created after March 12th 2024 are configured to use the new publishing by default and can use this plugin._

_If your account was created before March 12th 2024, you'll need to either [migrate to the central portal publisher API](https://central.sonatype.org/faq/what-is-different-between-central-portal-and-legacy-ossrh/#process-to-migrate) or [update your url to the Portal OSSRH staging API url](https://central.sonatype.org/publish/publish-portal-ossrh-staging-api/)._

_The date for [OSSRH sunsetting](https://central.sonatype.org/news/20250326_ossrh_sunset/) was June, 30th 2025. The service is now [EOL](https://central.sonatype.org/pages/ossrh-eol/)._

---

# Nmcp: New Maven Central Publishing

A plugin that uses the new [Central Portal publisher API](https://central.sonatype.org/publish/publish-portal-api/) to publish existing publications to Maven Central.

> [!WARNING]
> Nmcp does **not** create publications or apply the `maven-publish` plugin. This must be done using other means. Nmcp uses existing publications, stages them locally and uploads a zip to the Central Portal publisher API.
>
> For a higher level API use [vanniktech/gradle-maven-publish-plugin](https://github.com/vanniktech/gradle-maven-publish-plugin/).

# QuickStart

Configure `nmcp` in your root project using the quick way:

```kotlin
// root/build.gradle[.kts]
plugins {
  id("com.gradleup.nmcp.aggregation").version("1.1.0")
}

nmcpAggregation {
  centralPortal {
    username = TODO("Create a token username at https://central.sonatype.com/account")
    password = TODO("Create a token password at https://central.sonatype.com/account")
    // publish manually from the portal
    publishingType = "USER_MANAGED"
    // or if you want to publish automatically
    publishingType = "AUTOMATIC"
  }

  // Publish all projects that apply the 'maven-publish' plugin
  publishAllProjectsProbablyBreakingProjectIsolation()
}
```

Call `publishAggregationToCentralPortal` to publish the aggregation:

```bash
./gradlew publishAggregationToCentralPortal
# yay everything is uploaded 🎉
# go to https://central.sonatype.com/ to release if you used USER_MANAGED
```

Call `publishAggregationToCentralPortalSnapshots` to publish to the snapshots:

```bash
./gradlew publishAggregationToCentralPortalSnapshots
# yay everything is uploaded to "https://central.sonatype.com/repository/maven-snapshots/" 🎉
```

# Project isolation compatible version

`publishAllProjectsProbablyBreakingProjectIsolation()` uses the `allprojects {}` block and is incompatible with [Project-isolation](https://gradle.github.io/configuration-cache/). 

You can be 100% compatible by adding the plugin to each module you want to publish:

```kotlin
//root/module/build.gradle.kts
plugins {
  id("com.gradleup.nmcp").version("1.1.0")
}
```

And then list all modules in your root project:

```kotlin
//root/build.gradle.kts
plugins {
  id("com.gradleup.nmcp.aggregation").version("1.1.0")
}

nmcpAggregation {
  centralPortal {
    username = TODO("Create a token username at https://central.sonatype.com/account")
    password = TODO("Create a token password at https://central.sonatype.com/account")
    publishingType = "USER_MANAGED"
  }
}

dependencies {
  // Add all dependencies here 
  nmcpAggregation(project(":module1"))
  nmcpAggregation(project(":module2"))
  nmcpAggregation(project(":module3"))
}
```

Call `publishAggregationToCentralPortal` to publish the aggregation:

```bash
./gradlew publishAggregationToCentralPortal
# yay everything is uploaded 🎉
# go to https://central.sonatype.com/ to release if you used USER_MANAGED
```

Call `publishAggregationToCentralPortalSnapshots` to publish to the snapshots:

```bash
./gradlew publishAggregationToCentralPortalSnapshots
# yay everything is uploaded to "https://central.sonatype.com/repository/maven-snapshots/" 🎉
```

# All options

```kotlin
// root/build.gradle[.kts]
nmcpAggregation {
  centralPortal {
    // publish manually from the portal
    publishingType = "USER_MANAGED"

    // Increase the validation timeout to 30 minutes
    validationTimeout = java.time.Duration.of(30, ChronoUnit.MINUTES)
    // Disable waiting for validation
    validationTimeout = java.time.Duration.ZERO

    // Publish automatically once validation is successful
    publishingType = "AUTOMATIC"

    // Increase the publishing timeout to 30 minutes
    publishingTimeout = java.time.Duration.of(30, ChronoUnit.MINUTES)
    // Disable waiting for publishing
    publishingTimeout = java.time.Duration.ZERO

    // Customize the publication name 
    publicationName = "My Awesome Library version $version"

    // send publications one after the other instead of in parallel (might be slower) 
    uploadSnapshotsParallelism.set(1)
  }
}
```

# Inspect the deployment content

The `nmcpZipAggregation` task is an intermediate task of `publishAggregationToCentralPortal` that generates the ZIP file that is then sent to Maven Central.  
You can use this task to inspect the entire content before publishing your project.

```bash
./gradlew nmcpZipAggregation
# go to build/nmcp/zip/aggregation.zip
```

# Requirements

Nmcp requires Java 17+, Gradle 8.8+ for the settings plugin and Gradle 8.2+ otherwise.

# KDoc

The API reference is available at https://gradleup.com/nmcp/kdoc/nmcp/index.html
