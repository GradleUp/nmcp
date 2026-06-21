import nmcp.NmcpAggregationExtension

plugins {
    id("java")
    id("com.gradleup.nmcp")
    id("maven-publish")
    id("signing")
    id("com.gradleup.nmcp.aggregation")
}

group = "net.mbonnin.tnmcp.checksums"
version = "0.0.0-SNAPSHOT"

publishing {
    publications {
        create("default", MavenPublication::class.java) {
            from(components.getByName("java"))
        }
    }
}

signing {
    sign(publishing.publications)
    // This is a test-only key, it's OK to have it in source control
    useInMemoryPgpKeys(
        """
            -----BEGIN PGP PRIVATE KEY BLOCK-----

            lFgEajfv+hYJKwYBBAHaRw8BAQdAAtZjfx7pt7TkHKxX6UC9ORR73co/I6+S6t7u
            /rXV9wgAAQD634Lubr21SycUMR9XlMopjyAiraFsJhmnlJSEjzUCgA5BtBRUZXN0
            IDx0ZXN0QHRlc3QuY29tPoi1BBMWCgBdFiEEr/qnDkskFs9qzOQsipLIGWOW/iQF
            Amo37/obFIAAAAAABAAObWFudTIsMi41KzEuMTIsMCwzAhsDBQkFo5qABQsJCAcC
            AiICBhUKCQgLAgQWAgMBAh4HAheAAAoJEIqSyBljlv4kg20A/iDNNgJl3m4cCQoO
            mTvvX8QEMnWZLHLpxI69cKZVh36HAQD6eBcof42zc0+6h/xJgkOwFg6F70KIVZv+
            TaqMV1TYBJxdBGo37/oSCisGAQQBl1UBBQEBB0BJYHa33fClfG+p8vhZ4LuBj5Cq
            XArQPF/W1ZfAbmTCMQMBCAcAAP9Wk1ssXCVSyeLflcKW+aS8RXgFYTrm8KKwK+A7
            2GeuwBKKiJoEGBYKAEIWIQSv+qcOSyQWz2rM5CyKksgZY5b+JAUCajfv+hsUgAAA
            AAAEAA5tYW51MiwyLjUrMS4xMiwwLDMCGwwFCQWjmoAACgkQipLIGWOW/iRhAQEA
            up+FQ86sGGp8oKYCCOS0l5NkgszgJJZYVKCghlSG2G4BAK4hqML04TGh53iXYjFt
            KP12LB2cwKyM3On8/1LmBEQC
            =PPy4
            -----END PGP PRIVATE KEY BLOCK-----
        """.trimIndent(),
        ""
    )
}

val testDirectory = "build/m2"

nmcpAggregation {
    localRepository {
        name = "test"
        path = testDirectory
    }
}

dependencies {
    nmcpAggregation(project)
}

tasks.named("check") {
    dependsOn("nmcpPublishAggregationToTestRepository")
    doLast {
        check(project.file(testDirectory).walk().any {
          it.name.endsWith(".asc.sha512")
        })
        check(project.file(testDirectory).walk().any {
            it.name.endsWith(".asc.sha256")
        })
    }
}