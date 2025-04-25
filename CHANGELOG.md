
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
