## Split the plugin in 2 separate plugins:

- `com.gradleup.nmcp` creates a `zip${publicationName.capitalized()}Publication` and `publish${publicationName.capitalized()}PublicationToCentralPortal` task for each publication
  - `publish${publicationName.capitalized()}PublicationToCentralPortal` can be used to publish an individual publication to the central portal. If using this, you need to configure the `centralPortal {}` block.
  - The output of `zip${publicationName.capitalized()}Publication` is registered as an outgoing artifact so that the aggregation plugin can collect the files from all projects.
- `com.gradleup.nmcp.aggregation` can aggregate all zips from all projects and upload them in a single deployment to the central portal.

## Other changes:

- The default `publicationType` is now `"AUTOMATIC"`, make sure to set it to `"USER_MANAGED"` if you want to manually confirm releases.
- `NmcpSpec.endpoint` is replaced by `NmcpSpec.baseUrl`.
- `NmcpSpec.publicationType` is renamed `NmcpSpec.publishingType`.