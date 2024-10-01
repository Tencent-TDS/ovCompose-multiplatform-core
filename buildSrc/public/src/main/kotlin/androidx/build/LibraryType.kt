/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.build

/**
 * LibraryType represents the purpose and type of a library, whether it is a conventional library, a
 * set of samples showing how to use a conventional library, a set of lint rules for using a
 * conventional library, or any other type of published project.
 *
 * LibraryType collects a set of properties together, to make the "why" more clear and to simplify
 * setting these properties for library developers, so that only a single enum inferrable from the
 * purpose of the library needs to be set, rather than a variety of more arcane options.
 *
 * These properties are as follows: LibraryType.publish represents how the library is published to
 * GMaven LibraryType.sourceJars represents whether we publish the source code for the library to
 * GMaven in a way accessible to download, such as by Android Studio LibraryType.generateDocs
 * represents whether we generate documentation from the library to put on developer.android.com
 * LibraryType.checkApi represents whether we enforce API compatibility of the library according to
 * our semantic versioning protocol
 *
 * The possible values of LibraryType are as follows:
 * - [PUBLISHED_LIBRARY]: a conventional library published, sourced, documented, and versioned.
 * - [PUBLISHED_TEST_LIBRARY]: [PUBLISHED_LIBRARY], but allows calling `@VisibleForTesting` API.
 *   Used for libraries that allow developers to test code that uses your library. Often provides
 *   test fakes.
 * - [INTERNAL_TEST_LIBRARY]: unpublished, untracked, undocumented. Used in internal tests. Usually
 *   contains integration tests, but is _not_ an app. Runs device tests.
 * - [INTERNAL_HOST_TEST_LIBRARY]: as [INTERNAL_TEST_LIBRARY], but runs host tests instead. Avoid
 *   mixing host tests and device tests in the same library, for performance / test-result-caching
 *   reasons.
 * - [SAMPLES]: a library containing sample code referenced in your library's documentation with
 *   `@sampled`, published as a documentation-related supplement to a conventional library.
 * - [LINT]: a library of lint rules for using a conventional library. Published through lintPublish
 *   as part of an AAR, not published standalone.
 * - [GRADLE_PLUGIN]: a library that is a gradle plugin.
 * - [ANNOTATION_PROCESSOR]: a library consisting of an annotation processor. Used only while
 *   compiling.
 * - [ANNOTATION_PROCESSOR_UTILS]: contains reference code for understanding an annotation
 *   processor. Publishes source jars, but does not track API.
 * - [OTHER_CODE_PROCESSOR]: a library that algorithmically generates and/or alters code but not
 *   through hooking into custom annotations or the kotlin compiler. For example,
 *   navigation:safe-args-generator or Jetifier.
 * - [IDE_PLUGIN]: a library that should only ever be downloaded by studio. Unfortunately, we don't
 *   yet have a good way to track API for these. b/281843422
 * - [UNSET]: a library that has not yet been migrated to using LibraryType. Should never be used.
 */
sealed class LibraryType(
    val publish: Publish = Publish.NONE,
    val sourceJars: Boolean = false,
    val checkApi: RunApiTasks = RunApiTasks.No("Unknown Library Type"),
    val compilationTarget: CompilationTarget = CompilationTarget.DEVICE,
    val allowCallingVisibleForTestsApis: Boolean = false,
    val targetsKotlinConsumersOnly: Boolean = false
) {
    val name: String
        get() = javaClass.simpleName

    companion object {
        @JvmStatic val ANNOTATION_PROCESSOR = AnnotationProcessor()
        @JvmStatic val ANNOTATION_PROCESSOR_UTILS = AnnotationProcessorUtils()
        @JvmStatic val GRADLE_PLUGIN = GradlePlugin()
        @JvmStatic val IDE_PLUGIN = IdePlugin()
        @JvmStatic val INTERNAL_TEST_LIBRARY = InternalTestLibrary()
        @JvmStatic val INTERNAL_HOST_TEST_LIBRARY = InternalHostTestLibrary()
        @JvmStatic val LINT = Lint()
        @JvmStatic val PUBLISHED_LIBRARY = PublishedLibrary()
        @JvmStatic
        val PUBLISHED_PROTO_LIBRARY =
            PublishedLibrary(
                checkApi =
                    RunApiTasks.No("Metalava doesn't properly parse the proto sources b/180579063")
            )
        @JvmStatic
        val PUBLISHED_LIBRARY_ONLY_USED_BY_KOTLIN_CONSUMERS =
            PublishedLibrary(targetsKotlinConsumersOnly = true)
        @JvmStatic val PUBLISHED_TEST_LIBRARY = PublishedTestLibrary()
        @JvmStatic
        val PUBLISHED_KOTLIN_ONLY_TEST_LIBRARY =
            PublishedTestLibrary(targetsKotlinConsumersOnly = true)
        @JvmStatic val SAMPLES = Samples()
        @JvmStatic val OTHER_CODE_PROCESSOR = OtherCodeProcessor()
        val UNSET = Unset()

        private val allTypes =
            mapOf(
                "PUBLISHED_LIBRARY" to PUBLISHED_LIBRARY,
                "PUBLISHED_PROTO_LIBRARY" to PUBLISHED_PROTO_LIBRARY,
                "PUBLISHED_LIBRARY_ONLY_USED_BY_KOTLIN_CONSUMERS" to
                    PUBLISHED_LIBRARY_ONLY_USED_BY_KOTLIN_CONSUMERS,
                "PUBLISHED_TEST_LIBRARY" to PUBLISHED_TEST_LIBRARY,
                "PUBLISHED_KOTLIN_ONLY_TEST_LIBRARY" to PUBLISHED_KOTLIN_ONLY_TEST_LIBRARY,
                "INTERNAL_TEST_LIBRARY" to INTERNAL_TEST_LIBRARY,
                "INTERNAL_HOST_TEST_LIBRARY" to INTERNAL_HOST_TEST_LIBRARY,
                "SAMPLES" to SAMPLES,
                "LINT" to LINT,
                "GRADLE_PLUGIN" to GRADLE_PLUGIN,
                "ANNOTATION_PROCESSOR" to ANNOTATION_PROCESSOR,
                "ANNOTATION_PROCESSOR_UTILS" to ANNOTATION_PROCESSOR_UTILS,
                "OTHER_CODE_PROCESSOR" to OTHER_CODE_PROCESSOR,
                "IDE_PLUGIN" to IDE_PLUGIN,
                "UNSET" to UNSET,
            )

        fun valueOf(name: String): LibraryType {
            val result = allTypes[name]
            check(result != null) { "LibraryType with name $name not found" }
            return result
        }
    }

    open class PublishedLibrary(
        checkApi: RunApiTasks = RunApiTasks.Yes(),
        allowCallingVisibleForTestsApis: Boolean = false,
        targetsKotlinConsumersOnly: Boolean = false
    ) :
        LibraryType(
            publish = Publish.SNAPSHOT_AND_RELEASE,
            sourceJars = true,
            checkApi = checkApi,
            allowCallingVisibleForTestsApis = allowCallingVisibleForTestsApis,
            targetsKotlinConsumersOnly = targetsKotlinConsumersOnly
        )

    open class InternalLibrary(
        compilationTarget: CompilationTarget = CompilationTarget.DEVICE,
        allowCallingVisibleForTestsApis: Boolean = false
    ) :
        LibraryType(
            checkApi = RunApiTasks.No("Internal Library"),
            compilationTarget = compilationTarget,
            allowCallingVisibleForTestsApis = allowCallingVisibleForTestsApis
        )

    class PublishedTestLibrary(targetsKotlinConsumersOnly: Boolean = false) :
        PublishedLibrary(
            allowCallingVisibleForTestsApis = true,
            targetsKotlinConsumersOnly = targetsKotlinConsumersOnly
        )

    class InternalTestLibrary() : InternalLibrary(allowCallingVisibleForTestsApis = true)

    class InternalHostTestLibrary() : InternalLibrary(CompilationTarget.HOST)

    class Samples :
        LibraryType(
            publish = Publish.SNAPSHOT_AND_RELEASE,
            sourceJars = true,
            checkApi = RunApiTasks.No("Sample Library")
        )

    class Lint :
        LibraryType(
            publish = Publish.NONE,
            sourceJars = false,
            checkApi = RunApiTasks.No("Lint Library"),
            compilationTarget = CompilationTarget.HOST
        )

    class CompilerDaemon :
        LibraryType(
            Publish.SNAPSHOT_AND_RELEASE,
            sourceJars = false,
            RunApiTasks.No("Compiler Daemon (Host-only)"),
            CompilationTarget.HOST
        )

    class CompilerDaemonTest :
        LibraryType(
            Publish.NONE,
            sourceJars = false,
            RunApiTasks.No("Compiler Daemon (Host-only) Test"),
            CompilationTarget.HOST
        )

    class CompilerPlugin :
        LibraryType(
            Publish.SNAPSHOT_AND_RELEASE,
            sourceJars = false,
            RunApiTasks.No("Compiler Plugin (Host-only)"),
            CompilationTarget.HOST
        )

    class GradlePlugin :
        LibraryType(
            Publish.SNAPSHOT_AND_RELEASE,
            sourceJars = false,
            RunApiTasks.No("Gradle Plugin (Host-only)"),
            CompilationTarget.HOST
        )

    class AnnotationProcessor :
        LibraryType(
            publish = Publish.SNAPSHOT_AND_RELEASE,
            sourceJars = false,
            checkApi = RunApiTasks.No("Annotation Processor"),
            compilationTarget = CompilationTarget.HOST
        )

    class AnnotationProcessorUtils :
        LibraryType(
            publish = Publish.SNAPSHOT_AND_RELEASE,
            sourceJars = true,
            checkApi = RunApiTasks.No("Annotation Processor Helper Library"),
            compilationTarget = CompilationTarget.HOST
        )

    class OtherCodeProcessor(publish: Publish = Publish.SNAPSHOT_AND_RELEASE) :
        LibraryType(
            publish = publish,
            sourceJars = false,
            checkApi = RunApiTasks.No("Code Processor (Host-only)"),
            compilationTarget = CompilationTarget.HOST
        )

    class IdePlugin :
        LibraryType(
            publish = Publish.NONE,
            sourceJars = false,
            // TODO: figure out a way to make sure we don't break Studio
            checkApi = RunApiTasks.No("IDE Plugin (consumed only by Android Studio"),
            // This is a bit complicated. IDE plugins usually have an on-device component installed
            // by
            // Android Studio, rather than by a client of the library, but also a host-side
            // component.
            compilationTarget = CompilationTarget.DEVICE
        )

    class Unset : LibraryType()
}

enum class CompilationTarget {
    /** This library is meant to run on the host machine (like an annotation processor). */
    HOST,
    /** This library is meant to run on an Android device. */
    DEVICE
}

/**
 * Publish Enum: Publish.NONE -> Generates no artifacts; does not generate snapshot artifacts or
 * releasable maven artifacts Publish.SNAPSHOT_ONLY -> Only generates snapshot artifacts
 * Publish.SNAPSHOT_AND_RELEASE -> Generates both snapshot artifacts and releasable maven artifact
 * Publish.UNSET -> Do the default, based on LibraryType. If LibraryType.UNSET -> Publish.NONE
 *
 * TODO: should we introduce a Publish.lintPublish?
 * TODO: remove Publish.UNSET once we remove LibraryType.UNSET. It is necessary now in order to be
 *   able to override LibraryType.publish (with Publish.None)
 */
enum class Publish {
    NONE,
    SNAPSHOT_ONLY,
    SNAPSHOT_AND_RELEASE,
    UNSET;

    fun shouldRelease() = this == SNAPSHOT_AND_RELEASE

    fun shouldPublish() = shouldRelease() || this == SNAPSHOT_ONLY
}

sealed class RunApiTasks {
    /** Automatically determine whether API tasks should be run. */
    object Auto : RunApiTasks()

    /** Always run API tasks regardless of other project properties. */
    data class Yes(val reason: String? = null) : RunApiTasks()

    /** Do not run any API tasks. */
    data class No(val reason: String) : RunApiTasks()
}
