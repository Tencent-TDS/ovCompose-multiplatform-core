/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("unused")

package androidx.build.jetbrains

import androidx.build.AndroidXExtension
import androidx.build.multiplatformExtension
import androidx.build.resolveProject
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.KotlinClosure1
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.extra
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinSoftwareComponentWithCoordinatesAndPublication
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.konan.target.KonanTarget

open class JetbrainsExtensions(
    val project: Project,
    val multiplatformExtension: KotlinMultiplatformExtension
) {

    // check for example here: https://maven.google.com/web/index.html?q=lifecyc#androidx.lifecycle
    val defaultKonanTargetsPublishedByAndroidx = setOf(
        KonanTarget.LINUX_X64,
        KonanTarget.IOS_X64,
        KonanTarget.IOS_ARM64,
        KonanTarget.IOS_SIMULATOR_ARM64,
        KonanTarget.MACOS_X64,
        KonanTarget.MACOS_ARM64,
    )

    @JvmOverloads
    fun configureKNativeRedirectingDependenciesInKlibManifest(
        konanTargets: Set<KonanTarget> = defaultKonanTargetsPublishedByAndroidx
    ) {
        multiplatformExtension.targets.all {
            if (it is KotlinNativeTarget && it.konanTarget in konanTargets) {
                it.substituteForRedirectedPublishedDependencies()
            }
        }
    }

    /**
     * When https://youtrack.jetbrains.com/issue/KT-61096 is implemented,
     * this workaround won't be needed anymore:
     *
     * K/Native stores the dependencies in klib manifest and tries to resolve them during compilation.
     * Since we use project dependency - implementation(project(...)), the klib manifest will reference
     * our groupId (for example org.jetbrains.compose.collection-internal instead of androidx.collection).
     * Therefore, the dependency can't be resolved since we don't publish libs for some k/native targets.
     *
     * To workaround that, we need to make sure
     * that the project dependency is substituted by a module dependency (from androidx).
     * We do this here. It should be called only for those k/native targets which require
     * redirection to androidx artefacts.
     *
     * For available androidx targets see:
     * https://maven.google.com/web/index.html#androidx.annotation
     * https://maven.google.com/web/index.html#androidx.collection
     * https://maven.google.com/web/index.html#androidx.lifecycle
     */
    fun KotlinNativeTarget.substituteForRedirectedPublishedDependencies() {
        val main = compilations.getByName("main")
        val test = compilations.getByName("test")
        val kNativeManifestRedirectingModulesRaw =
            project.property("artifactRedirecting.modules-for-knative-manifest") as String

        val projectPathToRedirectingVersionMap = kNativeManifestRedirectingModulesRaw
            .split(",").associate {
                val pair = it.split("=")
                pair[0] to project.property(pair[1]) as String
            }
        listOf(main, test).flatMap {
            val configurations = it.configurations
            listOf(
                configurations.compileDependencyConfiguration,
                configurations.runtimeDependencyConfiguration,
                configurations.apiConfiguration,
                configurations.implementationConfiguration,
                configurations.runtimeOnlyConfiguration,
                configurations.compileOnlyConfiguration
            )
        }.forEach { c ->
            c?.resolutionStrategy {
                it.dependencySubstitution {
                    projectPathToRedirectingVersionMap.forEach { path, version ->
                        val artifact = path.replaceFirst(":", "").let {
                            "androidx.$it:$version"
                        }
                        it.substitute(it.project(path)).using(it.module(artifact))
                    }
                }
            }
        }
    }

}
class JetBrainsAndroidXImplPlugin : Plugin<Project> {

    @Suppress("UNREACHABLE_CODE", "UNUSED_VARIABLE")
    override fun apply(project: Project) {
        project.plugins.all { plugin ->
            if (plugin is KotlinMultiplatformPluginWrapper) {
                onKotlinMultiplatformPluginApplied(project)
            }
        }
    }

    private fun onKotlinMultiplatformPluginApplied(project: Project) {
        enableArtifactRedirectingPublishing(project)
        enableBinaryCompatibilityValidator(project)
        val multiplatformExtension =
            project.extensions.getByType(KotlinMultiplatformExtension::class.java)

        val extension = project.extensions.create<JetbrainsExtensions>(
            "jetbrainsExtension",
            project,
            multiplatformExtension
        )

        // it simply returns the requested project, but having a unique function name
        // clarifies its purpose
        project.extra.set("relocatedProject", KotlinClosure1<String, Project>(
            function = {
                // `this` refers to the first parameter of the closure.
                project.resolveProject(this)
            }
        ))

        // Note: Currently we call it unconditionally since Androidx provides the same set of
        // Konan targets for all multiplatform libs they publish.
        // In the future we might need to call it with non-default konan targets set in some modules
        extension.configureKNativeRedirectingDependenciesInKlibManifest()

        multiplatformExtension.removeRelocatedProjectsFromTestDependencies()
        project.configureRelocatingPublication()
    }

    private fun Project.configureRelocatingPublication() {
        val shouldRelocate = project.findProperty("artifactRedirecting.relocated") == "true"

        if (shouldRelocate) {
            extensions.configure(PublishingExtension::class.java) {
                it.publications {
                    // we use artifactRedirecting function here, because relocation
                    // is a more broad/full case of artifact redirection,
                    // so we can get all the necessary data from it
                    val relocatedTo = project.artifactRedirecting()
                    it.create<MavenPublication>("relocation") {
                        pom {
                            groupId = project.group.toString() // our published groupId
                            artifactId = project.name
                            it.distributionManagement {
                                it.relocation {
                                    it.groupId.set(relocatedTo.groupId)
                                    it.artifactId.set(project.name)
                                    it.version.set(relocatedTo.defaultVersion)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun KotlinMultiplatformExtension.removeRelocatedProjectsFromTestDependencies() {
//        val project = this.project
        targets.all { target ->
            if (target is KotlinJsIrTarget) {
                val c = target.compilations.getByName("test")
                val main = target.compilations.getByName("main")
                listOf(c, main).flatMap {
                    val configurations = it.configurations
                    listOf(
                        configurations.compileDependencyConfiguration,
                        configurations.runtimeDependencyConfiguration,
                        configurations.apiConfiguration,
                        configurations.implementationConfiguration,
                        configurations.runtimeOnlyConfiguration,
                        configurations.compileOnlyConfiguration
                    )
                }.forEach { conf ->
                    conf?.resolutionStrategy {
                        it.dependencySubstitution {
                            it.substitute(it.project(":collection:collection")).using(
                                // dumb dependency
//                                it.project(project.path)
                                it.module("androidx.collection:collection:1.5.0-beta02")
                            )
                            it.substitute(it.project(":annotation:annotation")).using(
                                // dumb dependency
//                                it.project(project.path)
                                it.module("androidx.annotation:annotation:1.9.1")
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun Project.experimentalArtifactRedirectingPublication() : Boolean = findProperty("artifactRedirecting.publication") == "true"
private fun Project.artifactRedirectingAndroidxVersion() : String? = findProperty("artifactRedirecting.androidx.compose.version") as String?
private fun Project.artifactRedirectingAndroidxFoundationVersion() : String? = findProperty("artifactRedirecting.androidx.compose.foundation.version") as String?
private fun Project.artifactRedirectingAndroidxMaterial3Version() : String? = findProperty("artifactRedirecting.androidx.compose.material3.version") as String?
private fun Project.artifactRedirectingAndroidxMaterialVersion() : String? = findProperty("artifactRedirecting.androidx.compose.material.version") as String?

private fun enableArtifactRedirectingPublishing(project: Project) {
    if (!project.experimentalArtifactRedirectingPublication()) return

    if (project.experimentalArtifactRedirectingPublication() && (project.artifactRedirectingAndroidxVersion() == null)) {
        error("androidx version should be specified for OEL publications")
    }

    val ext = project.multiplatformExtension ?: error("expected a multiplatform project")

    val redirecting = project.artifactRedirecting()
    val newRootComponent: CustomRootComponent = run {
        val rootComponent = project
            .components
            .withType(KotlinSoftwareComponentWithCoordinatesAndPublication::class.java)
            .getByName("kotlin")

        CustomRootComponent(rootComponent) { configuration ->
            val targetName = redirecting.targetVersions.keys.firstOrNull {
                // we rely on the fact that configuration name starts with target name
                configuration.name.startsWith(it, ignoreCase = true)
            }
            val targetVersion = redirecting.versionForTargetOrDefault(targetName ?: "")
            project.dependencies.create(
                redirecting.groupId, project.name, targetVersion
            )
        }
    }

    val oelTargetNames = (project.findProperty("artifactRedirecting.publication.targetNames") as? String ?: "")
        .split(",").map { it.lowercase() }.toSet()

    ext.targets.all { target ->
        if (target.name.lowercase() in oelTargetNames || target is KotlinAndroidTarget) {
            project.publishAndroidxReference(target as AbstractKotlinTarget, newRootComponent)
        }
    }
}

private fun enableBinaryCompatibilityValidator(project: Project) {
    val androidXExtension = project.extensions.findByType(AndroidXExtension::class.java)
        ?: throw Exception("You have applied AndroidXComposePlugin without AndroidXPlugin")
    project.afterEvaluate {
        if (androidXExtension.shouldPublish()) {
            project.apply(plugin = "org.jetbrains.kotlinx.binary-compatibility-validator")
        }
    }
}