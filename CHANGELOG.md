
# Version 1.4.0

Nmcp can now be 100% configured from `settings.gradle[.kts]`:

```kotlin
// settings.gradle.kts
plugins {
  id("com.gradleup.nmcp.setting").version("1.4.0")
}

nmcpAggregation {
  centralPortal {
    username = TODO()
    password = TODO()
    // ...
  }
}
```

This version also contains a few quality of life improvements courtesy of @SimonMarquis and fixes an issue that could sometimes cause maven-metadata.xml to be out of sync. 

## What's Changed
* Clamp remaining time to seconds https://github.com/GradleUp/nmcp/pull/213
* maven-metadata.xml: make sure the base version is always added to the list of versions  https://github.com/GradleUp/nmcp/pull/214, https://github.com/GradleUp/nmcp/pull/222
* Set an explicit configuration name to load nmcp-tasks nmcp https://github.com/GradleUp/nmcp/pull/215
* Register tasks earlier so that they are generated as typesafe tasks accessors  https://github.com/GradleUp/nmcp/pull/219
* Expand the zip files when passing it to `allFiles` nmcp https://github.com/GradleUp/nmcp/pull/218
* Publish to nmcp repo tasks should never be up-to-date nmcp https://github.com/GradleUp/nmcp/pull/221
* Allow to configure the whole build from the settings script nmcp https://github.com/GradleUp/nmcp/pull/223


# Version 1.3.0

Enable [classloader isolation](https://github.com/GradleUp/gratatouille?tab=readme-ov-file#classloader-isolation-optional) to fix issues such as #210.

# Version 1.2.1

A few fixes mostly around the settings plugin + fixed the publishing timeout.

## All Changes
* Settings plugin: only apply to subprojects if the maven-publish plugin is applied nmcp in https://github.com/GradleUp/nmcp/pull/196
* Settings plugin: allow the root project to apply both the regular and aggregation plugins nmcp in https://github.com/GradleUp/nmcp/pull/198
* Use allprojects {} instead of subprojects {} nmcp in https://github.com/GradleUp/nmcp/pull/199
* Fix publishing timeout nmcp in https://github.com/GradleUp/nmcp/pull/201
* Add publishFileByFile(File, File) helper function nmcp in https://github.com/GradleUp/nmcp/pull/207

# Version 1.2.0

Bunch of fixes + you can now call `nmcpPublishAggregationToMavenLocal` to verify your publishing process and/or test locally.

## All Changes
* Add plugin marker for the settings plugin nmcp in https://github.com/GradleUp/nmcp/pull/184
* Update to gratatouille 0.1.1 and use @GPlugin for the settings plugin nmcp in https://github.com/GradleUp/nmcp/pull/185
* Use the snapshot version of the gratatouille plugin nmcp in https://github.com/GradleUp/nmcp/pull/187
* Add nmcpPublishAggregationToMavenLocal nmcp in https://github.com/GradleUp/nmcp/pull/192
* Always populate snapshotVersions manually nmcp in https://github.com/GradleUp/nmcp/pull/193
* Bump bootstrapped version nmcp in https://github.com/GradleUp/nmcp/pull/194

# Version 1.1.0

This version uploads SNAPSHOTs publications in parallel by default (inside a single publication, files are still uploaded serially, which means this is only useful for multi-publications uploads). 

On the Apollo Kotlin repo, this made the snapshot CI workflow down to ~30min from ~1h previously. 

If you notice any issue, you can roll back to the previous behaviour with `uploadSnapshotsParallelism`:

```kotlin
nmcpAggregation {
  centralPortal {
     uploadSnapshotsParallelism.set(1)
  }
}
```

You may also experiment with different values of parallelism (current default is 8). If you do, let us know your findings!

## All changes
* Improve comment and error message  https://github.com/GradleUp/nmcp/pull/176
* Add option to upload snapshots in parallel nmcp in https://github.com/GradleUp/nmcp/pull/180
* Update compat-patrouille https://github.com/GradleUp/nmcp/pull/179
* Enable parallelism by default nmcp in https://github.com/GradleUp/nmcp/pull/181


# Version 1.0.3

Compatibility with isolated projects alongside a few UX improvements.

## All changes
* Add description to the tasks so that they are visible in `./gradlew --tasks` nmcp in https://github.com/GradleUp/nmcp/pull/160
* Compatibility with isolated projects nmcp in https://github.com/GradleUp/nmcp/pull/164
* Better error message on missing credentials nmcp in https://github.com/GradleUp/nmcp/pull/167
* Hide nmcpClient nmcp in https://github.com/GradleUp/nmcp/pull/168
* Check that `publishingType` has a valid value nmcp in https://github.com/GradleUp/nmcp/pull/169
* Improve the error message on publishing errors nmcp in https://github.com/GradleUp/nmcp/pull/149
* Add NmcpExtension.extraFiles() nmcp in https://github.com/GradleUp/nmcp/pull/170
* Bump compat-patrouille nmcp in https://github.com/GradleUp/nmcp/pull/172

# Version 1.0.2

* Add simple check to prevent some GHA workflows to run on forks by @SimonMarquis in https://github.com/GradleUp/nmcp/pull/152
* Replace `DisplayNameTest` with a more robust `FindDeploymentNameTest` by @SimonMarquis in https://github.com/GradleUp/nmcp/pull/151
* Make tasks visible in `./gradlew tasks` nmcp in https://github.com/GradleUp/nmcp/pull/156
* Fix OkHttp Response leak nmcp in https://github.com/GradleUp/nmcp/pull/158

# Version 1.0.1

* Tweak symbols visibility nmcp in https://github.com/GradleUp/nmcp/pull/138
* Hide more symbols nmcp in https://github.com/GradleUp/nmcp/pull/140
* Fix publishingTimeout KDoc nmcp in https://github.com/GradleUp/nmcp/pull/146
* Fix parsing artifact metadata with a modelVersion nmcp in https://github.com/GradleUp/nmcp/pull/147
* Document all options in `README.md` by @SimonMarquis in https://github.com/GradleUp/nmcp/pull/145

# Version 1.0.0

* Try to guess better deployment names nmcp in https://github.com/GradleUp/nmcp/pull/130
* Fix typo in log message in `nmcpPublishWithPublisherApi.kt` by @SimonMarquis in https://github.com/GradleUp/nmcp/pull/131
* Add GitHub action to publish Kdocs nmcp in https://github.com/GradleUp/nmcp/pull/135

# Version 1.0.0-rc.1

* Publish all projects (including root), and not only subprojects nmcp in https://github.com/GradleUp/nmcp/pull/124
* Add intermediate zip task back nmcp in https://github.com/GradleUp/nmcp/pull/126
* Update librarian and nmcp bootstrap versions (fixes the aggregation plugin marker) nmcp in https://github.com/GradleUp/nmcp/pull/128

# Version 1.0.0-rc.0

* Make uploading snapshots less verbose nmcp in https://github.com/GradleUp/nmcp/pull/120
* Add logs for zipping the files nmcp in https://github.com/GradleUp/nmcp/pull/121
* Fix an OkHttp response leak nmcp in https://github.com/GradleUp/nmcp/pull/122

# Version 0.2.1

* Update changelog and readme nmcp in https://github.com/GradleUp/nmcp/pull/92
* Remove single module from the README, we want to focus on aggregation use cases nmcp in https://github.com/GradleUp/nmcp/pull/105
* Improve logging nmcp in https://github.com/GradleUp/nmcp/pull/106
* Use "Authentication: Bearer" instead of "Authentication: UserToken" nmcp in https://github.com/GradleUp/nmcp/pull/107
* update Gratatouille nmcp in https://github.com/GradleUp/nmcp/pull/108
* Use a lenient configuration nmcp in https://github.com/GradleUp/nmcp/pull/109
* Add transport API nmcp in https://github.com/GradleUp/nmcp/pull/110
* Drop support for publishing a single publication nmcp in https://github.com/GradleUp/nmcp/pull/111
* Validate username and password early nmcp in https://github.com/GradleUp/nmcp/pull/104
* [infra] Release automatically nmcp in https://github.com/GradleUp/nmcp/pull/112
* Add NmcpAggregationExtension.allFiles nmcp in https://github.com/GradleUp/nmcp/pull/114
* hide some symbols nmcp in https://github.com/GradleUp/nmcp/pull/113

# Version 0.2.0

* Configure compatibility flags nmcp in https://github.com/GradleUp/nmcp/pull/89
* Change the default publishingType to AUTOMATIC nmcp in https://github.com/GradleUp/nmcp/pull/90
* Add `publishingTimeout` and restore log messages nmcp in https://github.com/GradleUp/nmcp/pull/91
* Compatibility with Kotlin 1.9 nmcp in https://github.com/GradleUp/nmcp/pull/94
* Hide internal tasks nmcp in https://github.com/GradleUp/nmcp/pull/95
* PUBLISHING is also a valid status nmcp in https://github.com/GradleUp/nmcp/pull/97
* Unify snapshots code nmcp in https://github.com/GradleUp/nmcp/pull/96
* Add `com.gradleup.nmcp.settings` nmcp in https://github.com/GradleUp/nmcp/pull/98
* Remove intermediate zip task nmcp in https://github.com/GradleUp/nmcp/pull/99
* Simplify finding a name for the deployment nmcp in https://github.com/GradleUp/nmcp/pull/100
* Add publishAggregationToCentralPortalSnapshots as a "shortcut" lifecyle task nmcp in https://github.com/GradleUp/nmcp/pull/101
* Only update maven-metadata.xml once all the files have been uploaded nmcp in https://github.com/GradleUp/nmcp/pull/102

# Version 0.1.5

## Fix for publishing snapshots.

Publishing proper SNAPSHOTs requires computing the timestamps and build number locally, which wasn't done in `0.1.4` (the contents of the zip where just plainly uploaded). `0.1.5` falls back to `maven-publish` to provide a workaround until the proper logic is implemented.

As a consequence, it's not possible as of today to publish the snapshots from the aggregation project because it typically does not apply `maven-central`. If you are using an aggregation, you should configure your Central Portal credentials in each subproject and call `publishAllPublicationsToCentralSnapshots`

```
./gradlew publishAllPublicationsToCentralSnapshots
```

This limitation will be lifted in a future version.

# Version 0.1.4

* fix: improve deployment status toString() by @vlsi in https://github.com/GradleUp/nmcp/pull/60
* Fix deployment name nmcp in https://github.com/GradleUp/nmcp/pull/62
* Add `publishAggregationToCentralSnapshots` and `publishFooPublicationToCentralSnapshots` nmcp in https://github.com/GradleUp/nmcp/pull/63
* Remove empty aggregation check nmcp in https://github.com/GradleUp/nmcp/pull/64

# Version 0.1.3

* Rename internal publicationType to publishingType by @SimonMarquis in https://github.com/GradleUp/nmcp/pull/49
* Update code snippets with better guidance in README.md by @SimonMarquis in https://github.com/GradleUp/nmcp/pull/50
* Upgrade Gradle to stable version 8.14.0 by @SimonMarquis in https://github.com/GradleUp/nmcp/pull/51
* feat: igonre SocketTimeoutException when waiting for the deployment to publish by @vlsi in https://github.com/GradleUp/nmcp/pull/56
* feat: log deployment status while waiting for it to complete, use exponential delays by @vlsi in https://github.com/GradleUp/nmcp/pull/57
* feat: add deploymentId to the error messages by @vlsi in https://github.com/GradleUp/nmcp/pull/58

# Version 0.1.2

No behaviour change.

This version publishes the plugin marker for `com.gradleup.nmcp.aggregation` (#48)

# Version 0.1.1

Technical release to remove a dependency on the GradleUp snapshots repository. 

This release is also the first release of `Nmcp` made using `Nmcp`.

# Version 0.1.0

## Split the plugin in two separate plugins:

- `com.gradleup.nmcp` creates a `zip${publicationName.capitalized()}Publication` and `publish${publicationName.capitalized()}PublicationToCentralPortal` task for each publication
  - `publish${publicationName.capitalized()}PublicationToCentralPortal` can be used to publish an individual publication to the central portal. If using this, you need to configure the `centralPortal {}` block.
  - The output of `zip${publicationName.capitalized()}Publication` is registered as an outgoing artifact so that the aggregation plugin can collect the files from all projects.
- `com.gradleup.nmcp.aggregation` can aggregate all zips from several projects and upload them in a single deployment to the central portal.

See the README for more instructions

## Other changes:

- The default `publicationType` is now `"AUTOMATIC"`, make sure to set it to `"USER_MANAGED"` if you want to manually confirm releases.
- `NmcpSpec.endpoint` is replaced by `NmcpSpec.baseUrl`.
- `NmcpSpec.publicationType` is renamed `NmcpSpec.publishingType`.
