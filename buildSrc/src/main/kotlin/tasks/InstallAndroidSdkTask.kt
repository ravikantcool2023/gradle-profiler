package tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.tooling.GradleConnector
import java.io.File

/**
 * Installs Android SDK. This task requires the ANDROID_SDK_ROOT env variable to be set.
 */
@UntrackedTask(because = "Output directory can change when running other builds")
abstract class InstallAndroidSdkTask : DefaultTask() {

    @get:Input
    abstract val androidSdkVersion: Property<String>

    /**
     * Optional since we use manual error message
     */
    @get:Input
    @get:Optional
    abstract val androidSdkRootEnvVariable: Property<String>

    @get:OutputDirectory
    abstract val androidProjectDir: DirectoryProperty

    @TaskAction
    fun install() {
        if (!androidSdkRootEnvVariable.isPresent) {
            throw GradleException("ANDROID_SDK_ROOT env variable is not set but it should be.")
        }

        val androidSdkVersion = androidSdkVersion.get()
        val projectDir = androidProjectDir.get().asFile

        createAndroidProject(projectDir, androidSdkVersion)
        buildAndroidProject(projectDir)
    }

    private fun buildAndroidProject(projectDir: File) {
        val connector = GradleConnector.newConnector()
            .forProjectDirectory(projectDir)
        connector.connect().use {
            it.newBuild().forTasks("build").run()
        }
    }

    private fun createAndroidProject(projectDir: File, androidSdkVersion: String) {
        File(projectDir, "settings.gradle").writeText(
            """
            pluginManagement {
                repositories {
                    gradlePluginPortal()
                    google()
                    mavenCentral()
                }
            }
            rootProject.name = "android-sdk-project"
            """.trimIndent()
        )

        File(projectDir, "build.gradle").writeText(
            """
            plugins {
                id 'com.android.application' version "$androidSdkVersion"
            }
            repositories {
                google()
                mavenCentral()
            }
            android {
                compileSdk 31
            }
            """.trimIndent()
        )

        File(projectDir, "src/main").mkdirs()
        File(projectDir, "src/main/AndroidManifest.xml").writeText(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                package="com.example.myapplication">
            </manifest>
            """.trimIndent()
        )
    }
}
