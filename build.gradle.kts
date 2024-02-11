plugins {
  id("org.jetbrains.kotlin.multiplatform").version("1.9.20")
}

kotlin {
  jvm()
  
  sourceSets {
    getByName("jvmTest") {
      dependencies {
        implementation(kotlin("test"))
      }
    }
  }
}