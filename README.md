# Nmcp: New Maven Central Publishing (or New Maven Central Portal)

A plugin that uses the new [Central Portal publisher API](https://central.sonatype.org/publish/publish-portal-api/) to publish to Maven Central.

New accounts created after Feb. 1st 2024 are configured to use the new publishing by defaults and can use this plugin. Other accounts can continue publishing to OSSRH.

Nmcp does not create publications or apply the `maven-publish`. This must be done using other means. Nmcp uses existing publications, stages them locally and upload a zip to the Central Portal publisher API. 

### QuickStart:

```kotlin
plugins {
    id("com.gradleup.nmcp").version("0.0.1")
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