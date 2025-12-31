plugins {
  id("org.jetbrains.kotlin.jvm").version("2.3.0").apply(false)
  id("com.gradleup.nmcp.aggregation").version("1.4.3-SNAPSHOT")
}

group = "com.example"
version = "1.0.0"

dependencies {
  nmcpAggregation(project(":duplicate-name"))
}
