package nmcp.internal

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar

fun Project.createAndroidPublication(variantName: String, emptyJavadoc: TaskProvider<Jar>) {
    val android = extensions.getByName("android")
    check(android is LibraryExtension) {
        "Librarian: cannot publish non-library project"
    }
    android.publishing {
        singleVariant(variantName) {
            withSourcesJar()
        }
    }

    extensions.getByType(PublishingExtension::class.java).apply {
        publications.register("default", MavenPublication::class.java) { publication ->
            afterEvaluate {
                publication.from(components.getByName(variantName))
                publication.artifact(emptyJavadoc)
            }
        }
    }
}
