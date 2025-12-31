plugins {
  id("org.jetbrains.kotlin.jvm")
  id("maven-publish")
  id("com.gradleup.nmcp")
  id("signing")
}

group = rootProject.group

publishing {
  publications.create("default", MavenPublication::class.java) {
    from(components.getByName("java"))
  }
}
