import com.gradleup.librarian.gradle.Librarian

plugins {
    alias(libs.plugins.kgp)
    alias(libs.plugins.ksp)
    id("com.gradleup.gratatouille.tasks")
    alias(libs.plugins.serialization)
}

Librarian.module(project)

gratatouille {
    codeGeneration {
        addDependencies.set(false)
        classLoaderIsolation {
            configurationName.set("nmcpTasks")
        }
    }
}

dependencies {
    implementation(libs.json)
    implementation(libs.okio)
    implementation(libs.coroutines)
    api(libs.gratatouille.tasks.runtime) {
        because("publishFileByFile requires GInputFiles")
    }
    api(libs.okhttp)
    implementation(libs.xmlutil)

    testImplementation(libs.kotlin.test)
}
