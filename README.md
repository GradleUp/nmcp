_New accounts created after Feb. 1st 2024 are configured to use the new publishing by default and can use this plugin. Other accounts can continue publishing to OSSRH the usual way until [June 30th 2025](https://central.sonatype.org/news/20250326_ossrh_sunset/)._

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
    id("com.gradleup.nmcp.aggregation").version("0.1.0")
}

nmcp {
  centralPortal {
    username = TODO()
    password = TODO()
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

# Project isolation compatible version

`publishAllProjectsProbablyBreakingProjectIsolation()` uses the `allprojects {}` block and is incompatible with [Project-isolation](https://gradle.github.io/configuration-cache/). 

You can be 100% compatible by adding the plugin to each module you want to publish:

```kotlin
//root/module/build.gradle.kts
plugins {
    id("com.gradleup.nmcp").version("0.1.1")
}
```

And then list all modules in your root project:

```kotlin
//root/build.gradle.kts
plugins {
    id("com.gradleup.nmcp.aggregation").version("0.1.1")
}

nmcp {
    centralPortal {
        username = TODO()
        password = TODO()
        publishingType = "USER_MANAGED"
    }
}

dependencies {
    nmcpAggregation(project(":module1"))
    nmcpAggregation(project(":module2"))
    nmcpAggregation(project(":module3"))
}
```

Then call `publishAggregationToCentralPortal` to publish the aggregation:

```bash
./gradlew publishAggregationToCentralPortal
# yay everything is uploaded 🎉
# go to https://central.sonatype.com/ to release if you used USER_MANAGED
```

# Single-module

```kotlin
plugins {
    id("maven-publish")
    id("com.gradleup.nmcp").version("0.1.0")
}

// Create your publications

nmcp {
    centralPortal {
        username = TODO("Create a token at https://central.sonatype.com/account") 
        password = TODO("Create a token at https://central.sonatype.com/account")
        // publish manually from the portal
        publishingType = "USER_MANAGED"
        // or if you want to publish automatically
        publishingType = "AUTOMATIC"
    }
}
```

`nmcp` creates a `"publish${publicationName.capitalize()}PublicationToCentralPortal"` task for each Maven publication.

There is also a lifecycle task to deploy all the publication to the central portal:

```bash
./gradlew publishAllPublicationsToCentralPortal
# yay everything is uploaded 🎉
# go to https://central.sonatype.com/ to release if you used USER_MANAGED
```
