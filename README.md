# Nmcp: New Maven Central Publishing (or New Maven Central Portal too!)

A plugin that uses the new [Central Portal publisher API](https://central.sonatype.org/publish/publish-portal-api/) to publish to Maven Central.

New accounts created after Feb. 1st 2024 are configured to use the new publishing by default and can use this plugin. Other accounts can continue publishing to OSSRH the usual way.

> [!WARNING]
> Nmcp does **not** create publications or apply the `maven-publish` plugin. This must be done using other means. Nmcp uses existing publications, stages them locally and uploads a zip to the Central Portal publisher API.
> To configure the publications, you can use [vanniktech/gradle-maven-publish-plugin](https://github.com/vanniktech/gradle-maven-publish-plugin/) and call nmcp `publishAllPublicationsToCentralPortal` instead of vanniktech `publishAndReleaseToMavenCentral`.
>
> See [StylianosGakis/molecule-aacvm](https://github.com/StylianosGakis/molecule-aacvm/blob/main/gradle.properties) for an example.

> [!NOTE]
> This project was created as a short term solution and a good learning opportunity. I'm hoping more streamlined solutions will appear in the long run, either
 in [gradle-maven-publish-plugin](https://github.com/vanniktech/gradle-maven-publish-plugin/issues/722) or as [first party](https://github.com/gradle/gradle/issues/28120).

# QuickStart:

Configure `nmcp` in your root project using the quick way:

```kotlin
// root/build.gradle[.kts]
plugins {
    id("com.gradleup.nmcp").version("0.0.8")
}

nmcp {
  publishAllProjectsProbablyBreakingProjectIsolation {
    username = TODO()
    password = TODO()
    // publish manually from the portal
    publicationType = "USER_MANAGED"
    // or if you want to publish automatically
    publicationType = "AUTOMATIC"
  }
}
```

Then call `publishAllPublicationsToCentralPortal` to publish all your publications:

```
./gradlew publishAllPublicationsToCentralPortal
# yay everything is uploaded 🎉
# go to https://central.sonatype.com/ to release if you used USER_MANAGED
```

# Project isolation compatible version:

`publishAllProjectsProbablyBreakingProjectIsolation` uses the `allprojects {}` block and might be incompatible with [Project-isolation](https://gradle.github.io/configuration-cache/). 

You can be 100% compatible by adding the plugin to each module you want to publish:

```kotlin
//root/moduleN/build.gradle.kts
plugins {
    id("com.gradleup.nmcp").version("0.0.8")
}

nmcp {
  publishAllPublications {}
}
```

And then list all modules in your root project:

```kotlin
//root/build.gradle.kts
plugins {
    id("com.gradleup.nmcp").version("0.0.8")
}

nmcp {
    publishAggregation {
        project(":module1")
        project(":module2")
        project(":module3")

        username = TODO()
        password = TODO()
        publicationType = "USER_MANAGED"
    }
}
```

# Single-module:

```kotlin
plugins {
    id("com.gradleup.nmcp").version("0.0.8")
}

// Create your publications

nmcp {
    // nameOfYourPublication must point to an existing publication
    publish(nameOfYourPublication) {
        username = TODO("Create a token at https://central.sonatype.com/account") 
        password = TODO("Create a token at https://central.sonatype.com/account")
        // publish manually from the portal
        publicationType = "USER_MANAGED"
        // or if you want to publish automatically
        publicationType = "AUTOMATIC"
    }
}
```
