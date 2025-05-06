import java.util.zip.ZipInputStream
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartReader
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer

plugins {
    id("base")
}
buildscript {
    repositories {
        mavenCentral()
    }

    dependencies {
        classpath("build-logic:build-logic")
        classpath("com.gradleup.nmcp:nmcp")
        classpath(libs.mockwebserver)
        classpath(libs.okhttp)
    }
}

apply(plugin = "com.gradleup.nmcp.aggregation")

val mockServer = MockWebServer()
mockServer.enqueue(MockResponse())
mockServer.enqueue(MockResponse().setBody("{\"deploymentState\": \"PUBLISHED\"}"))

dependencies {
    add("nmcpAggregation", project(":module1"))
    add("nmcpAggregation", project(":module2"))
}

extensions.getByType(PublishingExtension::class.java).apply {
    (this as ExtensionAware).extensions.getByType(nmcp.CentralPortalOptions::class.java).apply {
        if (System.getenv("MAVEN_CENTRAL_USERNAME") == null) {
            username = "placeholder"
            password = "placeholedr"
            baseUrl.set(mockServer.url("/").toString())
        } else {
            username = System.getenv("MAVEN_CENTRAL_USERNAME")
            password = System.getenv("MAVEN_CENTRAL_PASSWORD")
        }
        publishingType = "USER_MANAGED"
    }
}


tasks.register("checkZip") {
    dependsOn("publishAggregationToCentralPortal")
    doLast {
        val request = mockServer.takeRequest()

        val boundary = request.headers.get("content-type")!!.toMediaType().parameter("boundary")!!
        val paths = MultipartReader(request.body, boundary).use {
            ZipInputStream(it.nextPart()!!.body.inputStream()).use {
                buildList {
                    while (true) {
                        val entry = it.nextEntry
                        if (entry == null) {
                            break
                        }

                        add(entry.name)
                    }
                }
            }
        }

        check(
            paths.sorted().equals(
                listOf(
                    "sample/",
                    "sample/kmp/",
                    "sample/kmp/module1-js/",
                    "sample/kmp/module1-js/0.0.1/",
                    "sample/kmp/module1-js/0.0.1/module1-js-0.0.1-sources.jar",
                    "sample/kmp/module1-js/0.0.1/module1-js-0.0.1-sources.jar.md5",
                    "sample/kmp/module1-js/0.0.1/module1-js-0.0.1-sources.jar.sha1",
                    "sample/kmp/module1-js/0.0.1/module1-js-0.0.1-sources.jar.sha256",
                    "sample/kmp/module1-js/0.0.1/module1-js-0.0.1-sources.jar.sha512",
                    "sample/kmp/module1-js/0.0.1/module1-js-0.0.1.klib",
                    "sample/kmp/module1-js/0.0.1/module1-js-0.0.1.klib.md5",
                    "sample/kmp/module1-js/0.0.1/module1-js-0.0.1.klib.sha1",
                    "sample/kmp/module1-js/0.0.1/module1-js-0.0.1.klib.sha256",
                    "sample/kmp/module1-js/0.0.1/module1-js-0.0.1.klib.sha512",
                    "sample/kmp/module1-js/0.0.1/module1-js-0.0.1.module",
                    "sample/kmp/module1-js/0.0.1/module1-js-0.0.1.module.md5",
                    "sample/kmp/module1-js/0.0.1/module1-js-0.0.1.module.sha1",
                    "sample/kmp/module1-js/0.0.1/module1-js-0.0.1.module.sha256",
                    "sample/kmp/module1-js/0.0.1/module1-js-0.0.1.module.sha512",
                    "sample/kmp/module1-js/0.0.1/module1-js-0.0.1.pom",
                    "sample/kmp/module1-js/0.0.1/module1-js-0.0.1.pom.md5",
                    "sample/kmp/module1-js/0.0.1/module1-js-0.0.1.pom.sha1",
                    "sample/kmp/module1-js/0.0.1/module1-js-0.0.1.pom.sha256",
                    "sample/kmp/module1-js/0.0.1/module1-js-0.0.1.pom.sha512",
                    "sample/kmp/module1-jvm/",
                    "sample/kmp/module1-jvm/0.0.1/",
                    "sample/kmp/module1-jvm/0.0.1/module1-jvm-0.0.1-sources.jar",
                    "sample/kmp/module1-jvm/0.0.1/module1-jvm-0.0.1-sources.jar.md5",
                    "sample/kmp/module1-jvm/0.0.1/module1-jvm-0.0.1-sources.jar.sha1",
                    "sample/kmp/module1-jvm/0.0.1/module1-jvm-0.0.1-sources.jar.sha256",
                    "sample/kmp/module1-jvm/0.0.1/module1-jvm-0.0.1-sources.jar.sha512",
                    "sample/kmp/module1-jvm/0.0.1/module1-jvm-0.0.1.jar",
                    "sample/kmp/module1-jvm/0.0.1/module1-jvm-0.0.1.jar.md5",
                    "sample/kmp/module1-jvm/0.0.1/module1-jvm-0.0.1.jar.sha1",
                    "sample/kmp/module1-jvm/0.0.1/module1-jvm-0.0.1.jar.sha256",
                    "sample/kmp/module1-jvm/0.0.1/module1-jvm-0.0.1.jar.sha512",
                    "sample/kmp/module1-jvm/0.0.1/module1-jvm-0.0.1.module",
                    "sample/kmp/module1-jvm/0.0.1/module1-jvm-0.0.1.module.md5",
                    "sample/kmp/module1-jvm/0.0.1/module1-jvm-0.0.1.module.sha1",
                    "sample/kmp/module1-jvm/0.0.1/module1-jvm-0.0.1.module.sha256",
                    "sample/kmp/module1-jvm/0.0.1/module1-jvm-0.0.1.module.sha512",
                    "sample/kmp/module1-jvm/0.0.1/module1-jvm-0.0.1.pom",
                    "sample/kmp/module1-jvm/0.0.1/module1-jvm-0.0.1.pom.md5",
                    "sample/kmp/module1-jvm/0.0.1/module1-jvm-0.0.1.pom.sha1",
                    "sample/kmp/module1-jvm/0.0.1/module1-jvm-0.0.1.pom.sha256",
                    "sample/kmp/module1-jvm/0.0.1/module1-jvm-0.0.1.pom.sha512",
                    "sample/kmp/module1-linuxarm64/",
                    "sample/kmp/module1-linuxarm64/0.0.1/",
                    "sample/kmp/module1-linuxarm64/0.0.1/module1-linuxarm64-0.0.1-sources.jar",
                    "sample/kmp/module1-linuxarm64/0.0.1/module1-linuxarm64-0.0.1-sources.jar.md5",
                    "sample/kmp/module1-linuxarm64/0.0.1/module1-linuxarm64-0.0.1-sources.jar.sha1",
                    "sample/kmp/module1-linuxarm64/0.0.1/module1-linuxarm64-0.0.1-sources.jar.sha256",
                    "sample/kmp/module1-linuxarm64/0.0.1/module1-linuxarm64-0.0.1-sources.jar.sha512",
                    "sample/kmp/module1-linuxarm64/0.0.1/module1-linuxarm64-0.0.1.klib",
                    "sample/kmp/module1-linuxarm64/0.0.1/module1-linuxarm64-0.0.1.klib.md5",
                    "sample/kmp/module1-linuxarm64/0.0.1/module1-linuxarm64-0.0.1.klib.sha1",
                    "sample/kmp/module1-linuxarm64/0.0.1/module1-linuxarm64-0.0.1.klib.sha256",
                    "sample/kmp/module1-linuxarm64/0.0.1/module1-linuxarm64-0.0.1.klib.sha512",
                    "sample/kmp/module1-linuxarm64/0.0.1/module1-linuxarm64-0.0.1.module",
                    "sample/kmp/module1-linuxarm64/0.0.1/module1-linuxarm64-0.0.1.module.md5",
                    "sample/kmp/module1-linuxarm64/0.0.1/module1-linuxarm64-0.0.1.module.sha1",
                    "sample/kmp/module1-linuxarm64/0.0.1/module1-linuxarm64-0.0.1.module.sha256",
                    "sample/kmp/module1-linuxarm64/0.0.1/module1-linuxarm64-0.0.1.module.sha512",
                    "sample/kmp/module1-linuxarm64/0.0.1/module1-linuxarm64-0.0.1.pom",
                    "sample/kmp/module1-linuxarm64/0.0.1/module1-linuxarm64-0.0.1.pom.md5",
                    "sample/kmp/module1-linuxarm64/0.0.1/module1-linuxarm64-0.0.1.pom.sha1",
                    "sample/kmp/module1-linuxarm64/0.0.1/module1-linuxarm64-0.0.1.pom.sha256",
                    "sample/kmp/module1-linuxarm64/0.0.1/module1-linuxarm64-0.0.1.pom.sha512",
                    "sample/kmp/module1/",
                    "sample/kmp/module1/0.0.1/",
                    "sample/kmp/module1/0.0.1/module1-0.0.1-kotlin-tooling-metadata.json",
                    "sample/kmp/module1/0.0.1/module1-0.0.1-kotlin-tooling-metadata.json.md5",
                    "sample/kmp/module1/0.0.1/module1-0.0.1-kotlin-tooling-metadata.json.sha1",
                    "sample/kmp/module1/0.0.1/module1-0.0.1-kotlin-tooling-metadata.json.sha256",
                    "sample/kmp/module1/0.0.1/module1-0.0.1-kotlin-tooling-metadata.json.sha512",
                    "sample/kmp/module1/0.0.1/module1-0.0.1-sources.jar",
                    "sample/kmp/module1/0.0.1/module1-0.0.1-sources.jar.md5",
                    "sample/kmp/module1/0.0.1/module1-0.0.1-sources.jar.sha1",
                    "sample/kmp/module1/0.0.1/module1-0.0.1-sources.jar.sha256",
                    "sample/kmp/module1/0.0.1/module1-0.0.1-sources.jar.sha512",
                    "sample/kmp/module1/0.0.1/module1-0.0.1.jar",
                    "sample/kmp/module1/0.0.1/module1-0.0.1.jar.md5",
                    "sample/kmp/module1/0.0.1/module1-0.0.1.jar.sha1",
                    "sample/kmp/module1/0.0.1/module1-0.0.1.jar.sha256",
                    "sample/kmp/module1/0.0.1/module1-0.0.1.jar.sha512",
                    "sample/kmp/module1/0.0.1/module1-0.0.1.module",
                    "sample/kmp/module1/0.0.1/module1-0.0.1.module.md5",
                    "sample/kmp/module1/0.0.1/module1-0.0.1.module.sha1",
                    "sample/kmp/module1/0.0.1/module1-0.0.1.module.sha256",
                    "sample/kmp/module1/0.0.1/module1-0.0.1.module.sha512",
                    "sample/kmp/module1/0.0.1/module1-0.0.1.pom",
                    "sample/kmp/module1/0.0.1/module1-0.0.1.pom.md5",
                    "sample/kmp/module1/0.0.1/module1-0.0.1.pom.sha1",
                    "sample/kmp/module1/0.0.1/module1-0.0.1.pom.sha256",
                    "sample/kmp/module1/0.0.1/module1-0.0.1.pom.sha512",
                    "sample/kmp/module2-js/",
                    "sample/kmp/module2-js/0.0.1/",
                    "sample/kmp/module2-js/0.0.1/module2-js-0.0.1-sources.jar",
                    "sample/kmp/module2-js/0.0.1/module2-js-0.0.1-sources.jar.md5",
                    "sample/kmp/module2-js/0.0.1/module2-js-0.0.1-sources.jar.sha1",
                    "sample/kmp/module2-js/0.0.1/module2-js-0.0.1-sources.jar.sha256",
                    "sample/kmp/module2-js/0.0.1/module2-js-0.0.1-sources.jar.sha512",
                    "sample/kmp/module2-js/0.0.1/module2-js-0.0.1.klib",
                    "sample/kmp/module2-js/0.0.1/module2-js-0.0.1.klib.md5",
                    "sample/kmp/module2-js/0.0.1/module2-js-0.0.1.klib.sha1",
                    "sample/kmp/module2-js/0.0.1/module2-js-0.0.1.klib.sha256",
                    "sample/kmp/module2-js/0.0.1/module2-js-0.0.1.klib.sha512",
                    "sample/kmp/module2-js/0.0.1/module2-js-0.0.1.module",
                    "sample/kmp/module2-js/0.0.1/module2-js-0.0.1.module.md5",
                    "sample/kmp/module2-js/0.0.1/module2-js-0.0.1.module.sha1",
                    "sample/kmp/module2-js/0.0.1/module2-js-0.0.1.module.sha256",
                    "sample/kmp/module2-js/0.0.1/module2-js-0.0.1.module.sha512",
                    "sample/kmp/module2-js/0.0.1/module2-js-0.0.1.pom",
                    "sample/kmp/module2-js/0.0.1/module2-js-0.0.1.pom.md5",
                    "sample/kmp/module2-js/0.0.1/module2-js-0.0.1.pom.sha1",
                    "sample/kmp/module2-js/0.0.1/module2-js-0.0.1.pom.sha256",
                    "sample/kmp/module2-js/0.0.1/module2-js-0.0.1.pom.sha512",
                    "sample/kmp/module2-jvm/",
                    "sample/kmp/module2-jvm/0.0.1/",
                    "sample/kmp/module2-jvm/0.0.1/module2-jvm-0.0.1-sources.jar",
                    "sample/kmp/module2-jvm/0.0.1/module2-jvm-0.0.1-sources.jar.md5",
                    "sample/kmp/module2-jvm/0.0.1/module2-jvm-0.0.1-sources.jar.sha1",
                    "sample/kmp/module2-jvm/0.0.1/module2-jvm-0.0.1-sources.jar.sha256",
                    "sample/kmp/module2-jvm/0.0.1/module2-jvm-0.0.1-sources.jar.sha512",
                    "sample/kmp/module2-jvm/0.0.1/module2-jvm-0.0.1.jar",
                    "sample/kmp/module2-jvm/0.0.1/module2-jvm-0.0.1.jar.md5",
                    "sample/kmp/module2-jvm/0.0.1/module2-jvm-0.0.1.jar.sha1",
                    "sample/kmp/module2-jvm/0.0.1/module2-jvm-0.0.1.jar.sha256",
                    "sample/kmp/module2-jvm/0.0.1/module2-jvm-0.0.1.jar.sha512",
                    "sample/kmp/module2-jvm/0.0.1/module2-jvm-0.0.1.module",
                    "sample/kmp/module2-jvm/0.0.1/module2-jvm-0.0.1.module.md5",
                    "sample/kmp/module2-jvm/0.0.1/module2-jvm-0.0.1.module.sha1",
                    "sample/kmp/module2-jvm/0.0.1/module2-jvm-0.0.1.module.sha256",
                    "sample/kmp/module2-jvm/0.0.1/module2-jvm-0.0.1.module.sha512",
                    "sample/kmp/module2-jvm/0.0.1/module2-jvm-0.0.1.pom",
                    "sample/kmp/module2-jvm/0.0.1/module2-jvm-0.0.1.pom.md5",
                    "sample/kmp/module2-jvm/0.0.1/module2-jvm-0.0.1.pom.sha1",
                    "sample/kmp/module2-jvm/0.0.1/module2-jvm-0.0.1.pom.sha256",
                    "sample/kmp/module2-jvm/0.0.1/module2-jvm-0.0.1.pom.sha512",
                    "sample/kmp/module2-linuxarm64/",
                    "sample/kmp/module2-linuxarm64/0.0.1/",
                    "sample/kmp/module2-linuxarm64/0.0.1/module2-linuxarm64-0.0.1-sources.jar",
                    "sample/kmp/module2-linuxarm64/0.0.1/module2-linuxarm64-0.0.1-sources.jar.md5",
                    "sample/kmp/module2-linuxarm64/0.0.1/module2-linuxarm64-0.0.1-sources.jar.sha1",
                    "sample/kmp/module2-linuxarm64/0.0.1/module2-linuxarm64-0.0.1-sources.jar.sha256",
                    "sample/kmp/module2-linuxarm64/0.0.1/module2-linuxarm64-0.0.1-sources.jar.sha512",
                    "sample/kmp/module2-linuxarm64/0.0.1/module2-linuxarm64-0.0.1.klib",
                    "sample/kmp/module2-linuxarm64/0.0.1/module2-linuxarm64-0.0.1.klib.md5",
                    "sample/kmp/module2-linuxarm64/0.0.1/module2-linuxarm64-0.0.1.klib.sha1",
                    "sample/kmp/module2-linuxarm64/0.0.1/module2-linuxarm64-0.0.1.klib.sha256",
                    "sample/kmp/module2-linuxarm64/0.0.1/module2-linuxarm64-0.0.1.klib.sha512",
                    "sample/kmp/module2-linuxarm64/0.0.1/module2-linuxarm64-0.0.1.module",
                    "sample/kmp/module2-linuxarm64/0.0.1/module2-linuxarm64-0.0.1.module.md5",
                    "sample/kmp/module2-linuxarm64/0.0.1/module2-linuxarm64-0.0.1.module.sha1",
                    "sample/kmp/module2-linuxarm64/0.0.1/module2-linuxarm64-0.0.1.module.sha256",
                    "sample/kmp/module2-linuxarm64/0.0.1/module2-linuxarm64-0.0.1.module.sha512",
                    "sample/kmp/module2-linuxarm64/0.0.1/module2-linuxarm64-0.0.1.pom",
                    "sample/kmp/module2-linuxarm64/0.0.1/module2-linuxarm64-0.0.1.pom.md5",
                    "sample/kmp/module2-linuxarm64/0.0.1/module2-linuxarm64-0.0.1.pom.sha1",
                    "sample/kmp/module2-linuxarm64/0.0.1/module2-linuxarm64-0.0.1.pom.sha256",
                    "sample/kmp/module2-linuxarm64/0.0.1/module2-linuxarm64-0.0.1.pom.sha512",
                    "sample/kmp/module2/",
                    "sample/kmp/module2/0.0.1/",
                    "sample/kmp/module2/0.0.1/module2-0.0.1-kotlin-tooling-metadata.json",
                    "sample/kmp/module2/0.0.1/module2-0.0.1-kotlin-tooling-metadata.json.md5",
                    "sample/kmp/module2/0.0.1/module2-0.0.1-kotlin-tooling-metadata.json.sha1",
                    "sample/kmp/module2/0.0.1/module2-0.0.1-kotlin-tooling-metadata.json.sha256",
                    "sample/kmp/module2/0.0.1/module2-0.0.1-kotlin-tooling-metadata.json.sha512",
                    "sample/kmp/module2/0.0.1/module2-0.0.1-sources.jar",
                    "sample/kmp/module2/0.0.1/module2-0.0.1-sources.jar.md5",
                    "sample/kmp/module2/0.0.1/module2-0.0.1-sources.jar.sha1",
                    "sample/kmp/module2/0.0.1/module2-0.0.1-sources.jar.sha256",
                    "sample/kmp/module2/0.0.1/module2-0.0.1-sources.jar.sha512",
                    "sample/kmp/module2/0.0.1/module2-0.0.1.jar",
                    "sample/kmp/module2/0.0.1/module2-0.0.1.jar.md5",
                    "sample/kmp/module2/0.0.1/module2-0.0.1.jar.sha1",
                    "sample/kmp/module2/0.0.1/module2-0.0.1.jar.sha256",
                    "sample/kmp/module2/0.0.1/module2-0.0.1.jar.sha512",
                    "sample/kmp/module2/0.0.1/module2-0.0.1.module",
                    "sample/kmp/module2/0.0.1/module2-0.0.1.module.md5",
                    "sample/kmp/module2/0.0.1/module2-0.0.1.module.sha1",
                    "sample/kmp/module2/0.0.1/module2-0.0.1.module.sha256",
                    "sample/kmp/module2/0.0.1/module2-0.0.1.module.sha512",
                    "sample/kmp/module2/0.0.1/module2-0.0.1.pom",
                    "sample/kmp/module2/0.0.1/module2-0.0.1.pom.md5",
                    "sample/kmp/module2/0.0.1/module2-0.0.1.pom.sha1",
                    "sample/kmp/module2/0.0.1/module2-0.0.1.pom.sha256",
                    "sample/kmp/module2/0.0.1/module2-0.0.1.pom.sha512",
                )
            )
        )
    }
}

// The build task is added later from the JS KMP plugin in module1
tasks.configureEach {
    if (name == "build") {
        dependsOn("checkZip")
    }
}
