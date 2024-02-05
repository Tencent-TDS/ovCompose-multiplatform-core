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
    private val isOelPublication: Boolean by lazy {
        composeProperties.isOelPublication
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

    // android publication is always an OEL (published by androidx team, not jb),
    // therefore add it unconditionally to OEL set
    private val defaultOelTargetNames = setOf("android")

    fun publishMultiplatform(component: ComposeComponent) {
        val oelTargetNames = project.rootProject.findProject(component.path)!!
            .findProperty("oel.publication.targetNames").let {
                (it as? String)?.split(",") ?: emptyList()
            }.toSet() + defaultOelTargetNames

        val supportedTargetNames = component.supportedPlatforms.map { it.toKotlinTargetName() }
        val useOelPublication = supportedTargetNames.intersect(oelTargetNames).isNotEmpty()

        // To make OEL publishing work properly with kotlin >= 1.9.0,
        // we use decorated `KotlinMultiplatform` publication named - 'KotlinMultiplatformDecorated'.
        // see AndroidXComposeMultiplatformExtensionImpl.publishAndroidxReference for details.
        if (useOelPublication) {
            val kotlinCommonPublicationName = "${ComposePlatforms.KotlinMultiplatform.name}Decorated"
            dependsOnComposeTask("${component.path}:publish${kotlinCommonPublicationName}PublicationTo$repository")
        } else {
            dependsOnComposeTask("${component.path}:publish${ComposePlatforms.KotlinMultiplatform.name}PublicationTo$repository")
        }

        for (platform in targetPlatforms) {
            if (platform !in component.supportedPlatforms) continue
            if (platform.toKotlinTargetName() in oelTargetNames) continue

            dependsOnComposeTask("${component.path}:publish${platform.name}PublicationTo$repository")
        }
    }
}