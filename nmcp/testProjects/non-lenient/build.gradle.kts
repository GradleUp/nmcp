plugins {
  id("maven-publish")
  id("com.gradleup.nmcp").version("1.6.3-SNAPSHOT")
  id("com.gradleup.nmcp.aggregation").version("1.6.3-SNAPSHOT")
}

val fooTask = tasks.register("foo") {
    outputs.dir(layout.projectDirectory.file("dir1"))
    outputs.dir(layout.projectDirectory.file("dir2"))
}

nmcp {
  /**
   * error: the task contains several outputs and therefore cannot be added as an artifact
   */
  extraFiles(fooTask)
}

dependencies {
  nmcpAggregation(project)
}
