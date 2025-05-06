/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Internal

@CacheableTask
abstract class AbstractComposePublishingTask : DefaultTask() {
    @get:Internal
    lateinit var repository: String

    private val composeProperties by lazy {
        ComposeProperties(project)
    }
    private val isArtifactRedirectingPublication: Boolean by lazy {
        composeProperties.isArtifactRedirecting
    }
    private val targetPlatforms: Set<ComposePlatforms> by lazy {
        composeProperties.targetPlatforms
    }

    abstract fun dependsOnComposeTask(task: String)

    fun publish(project: String, publications: Collection<String>) {
        for (publication in publications) {
            dependsOnComposeTask("$project:publish${publication}PublicationTo$repository")
        }
    }

    fun publish(project: String, publications: Collection<String>, onlyWithPlatforms: Set<ComposePlatforms>) {
        if (onlyWithPlatforms.any { it in targetPlatforms }) {
            publish(project, publications)
        }
    }

    // android is always published in ArtifactRedirecting mode (published by androidx team, not jb),
    // therefore add it unconditionally ArtifactRedirecting set
    private val defaultArtifactRedirectingTargetNames = setOf("android")

    fun publishMultiplatform(component: ComposeComponent) {
        val artifactRedirectingTargetNames = project.rootProject.findProject(component.path)!!
            .findProperty("artifactRedirecting.publication.targetNames").let {
                (it as? String)?.split(",") ?: emptyList()
            }.toSet() + defaultArtifactRedirectingTargetNames

        val useArtifactRedirectingPublication = component.supportedPlatforms.any {
            it.matchesAnyIgnoringCase(artifactRedirectingTargetNames)
        }

        // To make ArtifactRedirecting publishing work properly with kotlin >= 1.9.0,
        // we use decorated `KotlinMultiplatform` publication named - 'KotlinMultiplatformDecorated'.
        // see AndroidXComposeMultiplatformExtensionImpl.publishAndroidxReference for details.
        // 重定向指的是对于 Android 等平台，重定向依赖到 Google 的 Jetpack Compose
        println("GavinTag: 配置 Publish 依赖 Task: project=$project component=${component.path} component.support.platform=${component.supportedPlatforms} redirecting.platform=$artifactRedirectingTargetNames useRedirecting=$useArtifactRedirectingPublication")
        if (useArtifactRedirectingPublication) {
            val kotlinCommonPublicationName = "${ComposePlatforms.KotlinMultiplatform.name}Decorated"
            dependsOnComposeTask("${component.path}:publish${kotlinCommonPublicationName}PublicationTo$repository")
        } else {
            dependsOnComposeTask("${component.path}:publish${ComposePlatforms.KotlinMultiplatform.name}PublicationTo$repository")
        }

        for (platform in targetPlatforms) {
            if (platform !in component.supportedPlatforms) continue
            if (platform.matchesAnyIgnoringCase(artifactRedirectingTargetNames)) continue

            dependsOnComposeTask("${component.path}:publish${platform.name}PublicationTo$repository")
        }
    }
}