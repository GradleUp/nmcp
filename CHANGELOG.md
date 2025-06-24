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
* 
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
