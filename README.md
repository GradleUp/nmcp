_New accounts created after March 12th 2024 are configured to use the new publishing by default and can use this plugin._

_If your account was created after March 12th 2024, you'll need to either [migrate to the central portal publisher API](https://central.sonatype.org/faq/what-is-different-between-central-portal-and-legacy-ossrh/#process-to-migrate) or [update your url to the Portal OSSRH staging API url](https://central.sonatype.org/publish/publish-portal-ossrh-staging-api/)._

_The current date for [OSSRH sunsetting](https://central.sonatype.org/news/20250326_ossrh_sunset/) is June, 30th 2025_

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
  id("com.gradleup.nmcp.aggregation").version("1.0.0")
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
  id("com.gradleup.nmcp").version("1.0.0")
}
```

And then list all modules in your root project:

```kotlin
//root/build.gradle.kts
plugins {
  id("com.gradleup.nmcp.aggregation").version("1.0.0")
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

# Advanced usage

## Skip the validation timeout

By default, `publishAggregationToCentralPortal` waits for Maven Central to validate the publication.  
This might not be desirable on CI jobs where reaching the timeout leads to the job failure.  
You can disable this behavior by setting the timeout(s) to `Duration.ZERO`, and the publication task will then immediately finish after uploading the artifacts.

```kotlin
// root/build.gradle[.kts]
nmcpAggregation {
  centralPortal {
    // Main timeout to wait for the deployment validation 
    validationTimeout = java.time.Duration.ZERO
    // Additional timeout to wait specifically for the publication with the AUTOMATIC `publishingType`
    publishingTimeout = java.time.Duration.ZERO
  }
}
```

## Customize the publication name

You can provide a custom name for your publication, which will be displayed in Maven Central's deployment dashboard.  
By default, it generates a name from the deployment contents. If the deployment contains several publications, it will show the common parts (typically `groupId` and `version`).

```kotlin
// root/build.gradle[.kts]
nmcpAggregation {
  centralPortal {
    publicationName = "My Awesome Library"
  }
}
```

## Inspect the deployment content

The `nmcpZipAggregation` task is an intermediate task of `publishAggregationToCentralPortal` that generates the ZIP file that is then sent to Maven Central.  
You can use this task to inspect the entire content before publishing your project.

```bash
./gradlew nmcpZipAggregation
# go to build/nmcp/zip/aggregation.zip
```

# KDoc

The API reference is available at https://gradleup.com/nmcp/kdoc/nmcp/index.html
