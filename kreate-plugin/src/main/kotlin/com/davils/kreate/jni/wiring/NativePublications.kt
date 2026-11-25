/*
 * Copyright 2026 Davils
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.davils.kreate.jni.wiring

import com.davils.kreate.KreateExtension
import com.davils.kreate.gradle.GradlePluginId
import com.davils.kreate.gradle.configurePom
import com.davils.kreate.gradle.declaredProjectName
import com.davils.kreate.host.platformTaskSuffix
import com.davils.kreate.jni.task.VerifyNativePlatforms
import com.davils.kreate.publish.PublishExtension
import com.davils.kreate.task.KreateTaskGroup
import org.gradle.api.Project
import org.gradle.api.publish.PublicationContainer
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType

private const val PUBLISHING_EXTENSION_NAME: String = "publishing"

private const val NATIVE_PUBLICATION_PREFIX: String = "kreateNative"

private const val NATIVE_PUBLISH_TASK_PREFIX: String = "publishKreateNative"

private const val SOURCES_JAR_TASK_NAME: String = "kreateJniNativeSourcesJar"

private const val JAVADOC_JAR_TASK_NAME: String = "kreateJniNativeJavadocJar"

private const val SOURCES_CLASSIFIER: String = "sources"

private const val JAVADOC_CLASSIFIER: String = "javadoc"

private const val DOCUMENTATION_JAR_DIRECTORY: String = "libs/native-docs"

internal fun Project.registerNativePublications(
    extension: KreateExtension,
    platforms: List<String>,
    jars: List<TaskProvider<Jar>>,
    verify: TaskProvider<VerifyNativePlatforms>
) {
    val publishConfig = extension.project.publish
    if (!publishConfig.enabled.get()) return

    if (!plugins.hasPlugin(GradlePluginId.MAVEN_PUBLISH)) {
        logger.warn(
            "Kreate's per-platform native publishing is enabled for project '$path', but the " +
                "'maven-publish' plugin is not applied. The platform JARs are built but nothing " +
                "publishes them."
        )
        return
    }

    val artifactBase = declaredProjectName(extension)
    val projectDescription = extension.project.description.orNull
    val documentationJars = registerDocumentationJars(publishConfig, artifactBase)

    extensions.configure<PublishingExtension>(PUBLISHING_EXTENSION_NAME) {
        requireMainPublicationLater(publications, verify)

        platforms.forEachIndexed { index, platformId ->
            val nativeArtifact = NativePlatformArtifact(platformId, artifactBase, projectDescription, jars[index])
            registerNativePublication(publications, publishConfig, nativeArtifact, documentationJars)
        }
    }

    uploadPlatformArtifactsFirst()
}

private fun Project.requireMainPublicationLater(
    publications: PublicationContainer,
    verify: TaskProvider<VerifyNativePlatforms>
) {
    val mainPublications = provider { mainPublicationNamesOf(publications) }
    verify.configure {
        requiresMainPublication.set(true)
        this.mainPublications.set(mainPublications)
    }
}

private fun mainPublicationNamesOf(publications: PublicationContainer): List<String> {
    val mavenPublications = publications.withType(MavenPublication::class.java)
    val names = mavenPublications.names
    return names.filterNot { name -> name.startsWith(NATIVE_PUBLICATION_PREFIX) }
}

private fun Project.uploadPlatformArtifactsFirst() {
    val publishTasks = tasks.withType<PublishToMavenRepository>()
    val platformUploads = publishTasks.matching { task -> task.name.startsWith(NATIVE_PUBLISH_TASK_PREFIX) }
    publishTasks.configureEach {
        val isPlatformUpload = name.startsWith(NATIVE_PUBLISH_TASK_PREFIX)
        if (isPlatformUpload) return@configureEach
        mustRunAfter(platformUploads)
    }
}

private fun Project.registerNativePublication(
    publications: PublicationContainer,
    publishConfig: PublishExtension,
    nativeArtifact: NativePlatformArtifact,
    documentationJars: List<TaskProvider<Jar>>
) {
    val platformId = nativeArtifact.platformId
    val publicationName = NATIVE_PUBLICATION_PREFIX + platformTaskSuffix(platformId)
    val pomName = "${nativeArtifact.artifactBase} ($platformId)"

    publications.register(publicationName, MavenPublication::class.java) {
        this.groupId = project.group.toString()
        this.artifactId = "${nativeArtifact.artifactBase}-$platformId"
        this.version = project.version.toString()

        artifact(nativeArtifact.jar)
        documentationJars.forEach { documentationJar -> artifact(documentationJar) }

        pom {
            configurePom(publishConfig, pomName, nativeDescription(nativeArtifact))
        }
    }
}

private fun nativeDescription(nativeArtifact: NativePlatformArtifact): String {
    val platformId = nativeArtifact.platformId
    val projectDescription = nativeArtifact.projectDescription ?: return "Native library for $platformId."
    return "$projectDescription — native library for $platformId."
}

private fun Project.registerDocumentationJars(
    publishConfig: PublishExtension,
    artifactBase: String
): List<TaskProvider<Jar>> {
    val isMavenCentralTarget = publishConfig.repositories.mavenCentral.enabled.get()
    if (!isMavenCentralTarget) return emptyList()

    val sources = registerEmptyDocumentationJar(SOURCES_JAR_TASK_NAME, SOURCES_CLASSIFIER, artifactBase)
    val javadoc = registerEmptyDocumentationJar(JAVADOC_JAR_TASK_NAME, JAVADOC_CLASSIFIER, artifactBase)
    return listOf(sources, javadoc)
}

private fun Project.registerEmptyDocumentationJar(
    taskName: String,
    classifier: String,
    artifactBase: String
): TaskProvider<Jar> = tasks.register<Jar>(taskName) {
    group = KreateTaskGroup.JNI.label
    description = "An empty $classifier JAR, required by Maven Central for native artifacts."
    archiveBaseName.set(artifactBase)
    archiveClassifier.set(classifier)
    destinationDirectory.set(layout.buildDirectory.dir(DOCUMENTATION_JAR_DIRECTORY))
}
