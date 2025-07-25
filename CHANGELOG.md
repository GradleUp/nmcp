
# Version 1.0.1

* Tweak symbols visibility by @martinbonnin in https://github.com/GradleUp/nmcp/pull/138
* Hide more symbols by @martinbonnin in https://github.com/GradleUp/nmcp/pull/140
* Fix publishingTimeout KDoc by @martinbonnin in https://github.com/GradleUp/nmcp/pull/146
* Fix parsing artifact metadata with a modelVersion by @martinbonnin in https://github.com/GradleUp/nmcp/pull/147
* Document all options in `README.md` by @SimonMarquis in https://github.com/GradleUp/nmcp/pull/145

# Version 1.0.0

* Try to guess better deployment names by @martinbonnin in https://github.com/GradleUp/nmcp/pull/130
* Fix typo in log message in `nmcpPublishWithPublisherApi.kt` by @SimonMarquis in https://github.com/GradleUp/nmcp/pull/131
* Add GitHub action to publish Kdocs by @martinbonnin in https://github.com/GradleUp/nmcp/pull/135

# Version 1.0.0-rc.1

* Publish all projects (including root), and not only subprojects by @martinbonnin in https://github.com/GradleUp/nmcp/pull/124
* Add intermediate zip task back by @martinbonnin in https://github.com/GradleUp/nmcp/pull/126
* Update librarian and nmcp bootstrap versions (fixes the aggregation plugin marker) by @martinbonnin in https://github.com/GradleUp/nmcp/pull/128

# Version 1.0.0-rc.0

* Make uploading snapshots less verbose by @martinbonnin in https://github.com/GradleUp/nmcp/pull/120
* Add logs for zipping the files by @martinbonnin in https://github.com/GradleUp/nmcp/pull/121
* Fix an OkHttp response leak by @martinbonnin in https://github.com/GradleUp/nmcp/pull/122

# Version 0.2.1

* Update changelog and readme by @martinbonnin in https://github.com/GradleUp/nmcp/pull/92
* Remove single module from the README, we want to focus on aggregation use cases by @martinbonnin in https://github.com/GradleUp/nmcp/pull/105
* Improve logging by @martinbonnin in https://github.com/GradleUp/nmcp/pull/106
* Use "Authentication: Bearer" instead of "Authentication: UserToken" by @martinbonnin in https://github.com/GradleUp/nmcp/pull/107
* update Gratatouille by @martinbonnin in https://github.com/GradleUp/nmcp/pull/108
* Use a lenient configuration by @martinbonnin in https://github.com/GradleUp/nmcp/pull/109
* Add transport API by @martinbonnin in https://github.com/GradleUp/nmcp/pull/110
* Drop support for publishing a single publication by @martinbonnin in https://github.com/GradleUp/nmcp/pull/111
* Validate username and password early by @martinbonnin in https://github.com/GradleUp/nmcp/pull/104
* [infra] Release automatically by @martinbonnin in https://github.com/GradleUp/nmcp/pull/112
* Add NmcpAggregationExtension.allFiles by @martinbonnin in https://github.com/GradleUp/nmcp/pull/114
* hide some symbols by @martinbonnin in https://github.com/GradleUp/nmcp/pull/113

# Version 0.2.0

* Configure compatibility flags by @martinbonnin in https://github.com/GradleUp/nmcp/pull/89
* Change the default publishingType to AUTOMATIC by @martinbonnin in https://github.com/GradleUp/nmcp/pull/90
* Add `publishingTimeout` and restore log messages by @martinbonnin in https://github.com/GradleUp/nmcp/pull/91
* Compatibility with Kotlin 1.9 by @martinbonnin in https://github.com/GradleUp/nmcp/pull/94
* Hide internal tasks by @martinbonnin in https://github.com/GradleUp/nmcp/pull/95
* PUBLISHING is also a valid status by @martinbonnin in https://github.com/GradleUp/nmcp/pull/97
* Unify snapshots code by @martinbonnin in https://github.com/GradleUp/nmcp/pull/96
* Add `com.gradleup.nmcp.settings` by @martinbonnin in https://github.com/GradleUp/nmcp/pull/98
* Remove intermediate zip task by @martinbonnin in https://github.com/GradleUp/nmcp/pull/99
* Simplify finding a name for the deployment by @martinbonnin in https://github.com/GradleUp/nmcp/pull/100
* Add publishAggregationToCentralPortalSnapshots as a "shortcut" lifecyle task by @martinbonnin in https://github.com/GradleUp/nmcp/pull/101
* Only update maven-metadata.xml once all the files have been uploaded by @martinbonnin in https://github.com/GradleUp/nmcp/pull/102

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
* Fix deployment name by @martinbonnin in https://github.com/GradleUp/nmcp/pull/62
* Add `publishAggregationToCentralSnapshots` and `publishFooPublicationToCentralSnapshots` by @martinbonnin in https://github.com/GradleUp/nmcp/pull/63
* Remove empty aggregation check by @martinbonnin in https://github.com/GradleUp/nmcp/pull/64

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
