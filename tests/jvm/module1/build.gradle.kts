import nmcp.NmcpExtension

afterEvaluate {
    extensions.getByType(NmcpExtension::class.java).apply {
        publishAllPublicationsToCentralPortal {
            username = System.getenv("MAVEN_CENTRAL_USERNAME")
            password = System.getenv("MAVEN_CENTRAL_PASSWORD")
        }
    }
}
