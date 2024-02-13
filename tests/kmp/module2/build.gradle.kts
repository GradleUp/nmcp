plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("maven-publish")
    id("com.gradleup.nmcp")
}

nmcp {
    publishAllPublications {}
}

group = "sample.kmp"
version = "0.0.1"

kotlin {
    jvm()
    linuxArm64()
    js {
        nodejs()
    }
}