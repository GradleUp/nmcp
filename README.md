# Nmcp: New Maven Central Publishing (or New Maven Central Portal too!)

A plugin that uses the new [Central Portal publisher API](https://central.sonatype.org/publish/publish-portal-api/) to publish to Maven Central.

New accounts created after Feb. 1st 2024 are configured to use the new publishing by default and can use this plugin. Other accounts can continue publishing to OSSRH the usual way.

Nmcp does not create publications or apply the `maven-publish` plugin. This must be done using other means. Nmcp uses existing publications, stages them locally and uploads a zip to the Central Portal publisher API. 

### QuickStart:

```kotlin
plugins {
    id("com.gradleup.nmcp").version("0.0.4")
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

### Multi-module:

If you have a lot of publications, use the "quick" way by configuring your root project:

```kotlin
// root/build.gradle[.kts]
plugins {
    id("com.gradleup.nmcp").version("0.0.4")
}

nmcp {
  publishAllProjectsProbablyBreakingProjectIsolation {
    username = TODO()
    password = TODO()
    publicationType = "USER_MANAGED"
  }
}
```

### Multi-module (project isolation):

`publishAllProjectsProbablyBreakingProjectIsolation` uses the `subproject {}` block and might be incompatible with [Project-isolation](https://gradle.github.io/configuration-cache/). To be compatible, you can add the plugin to each module you want to publish:

```kotlin
//root/moduleN/build.gradle.kts
plugins {
    id("com.gradleup.nmcp").version("0.0.4")
}

nmcp {
  publishAllPublications {}
}
```

And then list all modules in your root project:

```kotlin
//root/build.gradle.kts
plugins {
    id("com.gradleup.nmcp").version("0.0.4")
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
