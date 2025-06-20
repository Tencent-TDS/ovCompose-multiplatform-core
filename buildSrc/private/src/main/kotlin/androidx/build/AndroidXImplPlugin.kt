/*
 * Copyright 2018 The Android Open Source Project
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

import androidx.benchmark.gradle.BenchmarkPlugin
import androidx.build.AndroidXImplPlugin.Companion.TASK_TIMEOUT_MINUTES
import androidx.build.Release.DEFAULT_PUBLISH_CONFIG
import androidx.build.SupportConfig.COMPILE_SDK_VERSION
import androidx.build.SupportConfig.DEFAULT_MIN_SDK_VERSION
import androidx.build.SupportConfig.INSTRUMENTATION_RUNNER
import androidx.build.SupportConfig.TARGET_SDK_VERSION
import androidx.build.buildInfo.addCreateLibraryBuildInfoFileTasks
import androidx.build.checkapi.JavaApiTaskConfig
import androidx.build.checkapi.KmpApiTaskConfig
import androidx.build.checkapi.LibraryApiTaskConfig
import androidx.build.checkapi.configureProjectForApiTasks
import androidx.build.dependencies.KOTLIN_VERSION
import androidx.build.docs.AndroidXKmpDocsImplPlugin
import androidx.build.gradle.isRoot
import androidx.build.license.configureExternalDependencyLicenseCheck
import androidx.build.resources.configurePublicResourcesStub
import androidx.build.sbom.validateAllArchiveInputsRecognized
import androidx.build.studio.StudioTask
import androidx.build.testConfiguration.ModuleInfoGenerator
import androidx.build.testConfiguration.TestModule
import androidx.build.testConfiguration.addAppApkToTestConfigGeneration
import androidx.build.testConfiguration.configureTestConfigGeneration
import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.dsl.ManagedVirtualDevice
import com.android.build.api.dsl.TestOptions
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.HasAndroidTest
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.android.build.api.variant.Variant
import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.TestExtension
import com.android.build.gradle.TestPlugin
import com.android.build.gradle.TestedExtension
import java.io.File
import java.time.Duration
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.JavaVersion.VERSION_11
import org.gradle.api.JavaVersion.VERSION_17
import org.gradle.api.JavaVersion.VERSION_1_8
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.component.SoftwareComponentFactory
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.kotlin.dsl.KotlinClosure1
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePluginWrapper
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithSimulatorTests
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile

/**
 * A plugin which enables all of the Gradle customizations for AndroidX.
 * This plugin reacts to other plugins being added and adds required and optional functionality.
 */

class AndroidXImplPlugin @Inject constructor(val componentFactory: SoftwareComponentFactory) :
    Plugin<Project> {
    override fun apply(project: Project) {
        if (project.isRoot)
            throw Exception("Root project should use AndroidXRootImplPlugin instead")
        val extension = project.extensions.create<AndroidXExtension>(EXTENSION_NAME, project)

        val kmpExtension = project.extensions.create<AndroidXMultiplatformExtension>(
            AndroidXMultiplatformExtension.EXTENSION_NAME,
            project
        )

        project.tasks.register(BUILD_ON_SERVER_TASK, DefaultTask::class.java)
        // Perform different actions based on which plugins have been applied to the project.
        // Many of the actions overlap, ex. API tracking.
        project.plugins.all { plugin ->
            when (plugin) {
                is JavaPlugin -> configureWithJavaPlugin(project, extension)
                is LibraryPlugin -> configureWithLibraryPlugin(project, extension)
                is AppPlugin -> configureWithAppPlugin(project, extension)
                is TestPlugin -> configureWithTestPlugin(project, extension)
                is KotlinBasePluginWrapper -> configureWithKotlinPlugin(project, extension, plugin)
            }
        }

//        project.configureKtlint()
        project.configureKotlinStdlibVersion()

        // Configure all Jar-packing tasks for hermetic builds.
        project.tasks.withType(Zip::class.java).configureEach { it.configureForHermeticBuild() }
        project.tasks.withType(Copy::class.java).configureEach { it.configureForHermeticBuild() }

        // copy host side test results to DIST
        project.tasks.withType(AbstractTestTask::class.java) {
                task -> configureTestTask(project, task)
        }
        project.tasks.withType(Test::class.java) {
                task -> configureJvmTestTask(project, task)
        }

        project.configureTaskTimeouts()
        project.configureMavenArtifactUpload(extension, kmpExtension, componentFactory)
        project.configureExternalDependencyLicenseCheck()
//        project.configureProjectStructureValidation(extension)
        // TODO: [1.4 Update] check that it is not needed
        //   This validation is not needed for JetBrains Fork, since they usually set in a force way
//        project.configureProjectVersionValidation(extension)

        // JetBrains Fork only.
        // extension to download latest androidx artifacts instead of depending on project modules
        project.registerAndroidxArtifact(extension)

        project.registerProjectOrArtifact()
        project.addCreateLibraryBuildInfoFileTasks(extension)

        project.configurations.create("samples")
//        project.validateMultiplatformPluginHasNotBeenApplied()

        project.tasks.register("printCoordinates", PrintProjectCoordinatesTask::class.java) {
            it.configureWithAndroidXExtension(extension)
        }
        project.configureConstraintsWithinGroup(extension)
//        project.validateProjectParser(extension)
//        project.validateAllArchiveInputsRecognized()
//        project.afterEvaluate {
//            if (extension.shouldPublish()) {
//                project.validatePublishedMultiplatformHasDefault()
//            }
//        }

        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        project.pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            project.extensions.configure(KotlinMultiplatformExtension::class.java) {
                it.compilerOptions {
                    languageVersion.set(KotlinVersion.KOTLIN_1_9)
                    apiVersion.set(KotlinVersion.KOTLIN_1_9)
                }
            }
        }
    }

    /**
     * Register task that provides androidx library name by given androidx module
     *
     * It works using libraryversions.toml where versions for androidx libraries are located.
     *
     * JetBrains Fork only.
     */
    private fun Project.registerAndroidxArtifact(extension: AndroidXExtension) {
        extra.set(
            ANDROIDX_ARTIFACT_EXT_NAME,
            KotlinClosure1<String, Any>(
                function = {
                    val (group, libraryName) = this.trim(':').split(":")

                    // find androidx library from libraryversions.toml by given module
                    val androidxLibrary = extension.getLibraryGroupFromProjectPath(this)!!
                    val atomicVersion = androidxLibrary.atomicGroupVersion

                    // not all libraries have atomicVersion, for such cases we will use group name
                    // e.g. CORE doesn't have atomicVersion, so we will find just a version for CORE
                    // assuming that it will exist
                    val version = atomicVersion ?: extension.LibraryVersions[group.uppercase()]!!

                    "${androidxLibrary.group}:$libraryName:$version"
                }
            )
        )
    }

    private fun Project.registerProjectOrArtifact() {
        // Add a method for each sub project where they can declare an optional
        // dependency on a project or its latest snapshot artifact.
        if (!ProjectLayoutType.isPlayground(this)) {
            // In AndroidX build, this is always enforced to the project
            extra.set(
                PROJECT_OR_ARTIFACT_EXT_NAME,
                KotlinClosure1<String, Project>(
                    function = {
                        // this refers to the first parameter of the closure.
                        project.resolveProject(this)
                    }
                )
            )
        } else {
            // In Playground builds, they are converted to the latest SNAPSHOT artifact if the
            // project is not included in that playground.
            extra.set(
                PROJECT_OR_ARTIFACT_EXT_NAME,
                KotlinClosure1<String, Any>(
                    function = {
                        AndroidXPlaygroundRootImplPlugin.projectOrArtifact(rootProject, this)
                    }
                )
            )
        }
    }

    /**
     * Disables timestamps and ensures filesystem-independent archive ordering to maximize
     * cross-machine byte-for-byte reproducibility of artifacts.
     */
    private fun Zip.configureForHermeticBuild() {
        isReproducibleFileOrder = true
        isPreserveFileTimestamps = false
    }

    private fun Copy.configureForHermeticBuild() {
        duplicatesStrategy = DuplicatesStrategy.FAIL
    }

    private fun configureJvmTestTask(project: Project, task: Test) {
        // Robolectric 1.7 increased heap size requirements, see b/207169653.
        task.maxHeapSize = "3g"

        // For non-playground setup use robolectric offline
        if (!ProjectLayoutType.isPlayground(project)) {
            task.systemProperty("robolectric.offline", "true")
            val robolectricDependencies =
                File(
                    project.getPrebuiltsRoot(),
                    "androidx/external/org/robolectric/android-all-instrumented"
                )
            task.systemProperty(
                "robolectric.dependency.dir",
                robolectricDependencies.relativeTo(project.projectDir)
            )
        }
    }

    private fun configureTestTask(project: Project, task: AbstractTestTask) {
        val ignoreFailuresProperty = project.providers.gradleProperty(
            TEST_FAILURES_DO_NOT_FAIL_TEST_TASK
        )
        val ignoreFailures = ignoreFailuresProperty.isPresent
        if (ignoreFailures) {
            task.ignoreFailures = true
        }
        task.inputs.property("ignoreFailures", ignoreFailures)

        val xmlReportDestDir = project.getHostTestResultDirectory()
        val testName = "${project.path}:${task.name}"
        project.rootProject.tasks.named("createModuleInfo").configure {
            it as ModuleInfoGenerator
            it.testModules.add(
                TestModule(
                    name = testName,
                    path = listOf(
                        project.projectDir.toRelativeString(project.getSupportRootFolder())
                    )
                )
            )
        }
        val archiveName = "$testName.zip"
        if (project.isDisplayTestOutput()) {
            // Enable tracing to see results in command line
            task.testLogging.apply {
                events = hashSetOf(
                    TestLogEvent.FAILED, TestLogEvent.PASSED,
                    TestLogEvent.SKIPPED, TestLogEvent.STANDARD_OUT
                )
                showExceptions = true
                showCauses = true
                showStackTraces = true
                exceptionFormat = TestExceptionFormat.FULL
            }
        } else {
            task.testLogging.apply {
                showExceptions = false
                // Disable all output, including the names of the failing tests, by specifying
                // that the minimum granularity we're interested in is this very high number
                // (which is higher than the current maximum granularity that Gradle offers (3))
                minGranularity = 1000
            }
            val testTaskName = task.name
            val capitalizedTestTaskName = testTaskName.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
            val xmlReport = task.reports.junitXml
            if (xmlReport.required.get()) {
                val zipXmlTask = project.tasks.register(
                    "zipXmlResultsOf$capitalizedTestTaskName",
                    Zip::class.java
                ) {
                    it.destinationDirectory.set(xmlReportDestDir)
                    it.archiveFileName.set(archiveName)
                    it.from(project.file(xmlReport.outputLocation))
                }
                task.finalizedBy(zipXmlTask)
            }
        }
    }

    private fun configureWithKotlinPlugin(
        project: Project,
        extension: AndroidXExtension,
        plugin: KotlinBasePluginWrapper
    ) {
        project.afterEvaluate {
            project.tasks.withType(KotlinCompile::class.java).configureEach { task ->
                if (extension.type.compilationTarget == CompilationTarget.HOST &&
                    extension.type != LibraryType.ANNOTATION_PROCESSOR_UTILS
                ) {
                    task.compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
                } else {
                    task.compilerOptions.jvmTarget.set(JvmTarget.JVM_11)
                }
                val kotlinCompilerArgs = mutableListOf(
                    "-Xskip-metadata-version-check",
                )
                // TODO (b/259578592): enable -Xjvm-default=all for camera-camera2-pipe projects
                if (!project.name.contains("camera-camera2-pipe")) {
                    kotlinCompilerArgs += "-Xjvm-default=all"
                }
                task.compilerOptions.freeCompilerArgs.addAll(kotlinCompilerArgs)
            }

            val isAndroidProject = project.plugins.hasPlugin(LibraryPlugin::class.java) ||
                project.plugins.hasPlugin(AppPlugin::class.java)
            // Explicit API mode is broken for Android projects
            // https://youtrack.jetbrains.com/issue/KT-37652
            if (extension.shouldEnforceKotlinStrictApiMode() && !isAndroidProject) {
                project.tasks.withType(KotlinCompile::class.java).configureEach { task ->
                    // Workaround for https://youtrack.jetbrains.com/issue/KT-37652
                    if (task.name.endsWith("TestKotlin")) return@configureEach
                    if (task.name.endsWith("TestKotlinJvm")) return@configureEach
                    task.compilerOptions.freeCompilerArgs.addAll(listOf("-Xexplicit-api=strict"))
                }
            }
        }
        // setup a partial docs artifact that can be used to generate offline docs, if requested.
        AndroidXKmpDocsImplPlugin.setupPartialDocsArtifact(project)
        if (plugin is KotlinMultiplatformPluginWrapper) {
            //project.configureKonanDirectory()
            project.extensions.findByType<LibraryExtension>()?.apply {
                configureAndroidLibraryWithMultiplatformPluginOptions()
            }
            project.configureKmpTests()
            project.configureSourceJarForMultiplatform()
            project.configureLintForMultiplatform(extension)
        }
    }

    @Suppress("UnstableApiUsage") // AGP DSL APIs
    private fun configureWithAppPlugin(project: Project, androidXExtension: AndroidXExtension) {
        project.extensions.getByType<AppExtension>().apply {
            configureAndroidBaseOptions(project, androidXExtension)
            configureAndroidApplicationOptions(project, androidXExtension)
        }

        project.extensions.getByType<ApplicationAndroidComponentsExtension>().apply {
            onVariants {
                it.configureTests()
                it.artRewritingWorkaround()
            }
            finalizeDsl {
                project.configureAndroidProjectForLint(
                    it.lint,
                    androidXExtension,
                    isLibrary = false
                )
            }
        }
    }

    private fun configureWithTestPlugin(
        project: Project,
        androidXExtension: AndroidXExtension
    ) {
        project.extensions.getByType<TestExtension>().apply {
            configureAndroidBaseOptions(project, androidXExtension)
            project.addAppApkToTestConfigGeneration(androidXExtension)
        }

        project.configureJavaCompilationWarnings(androidXExtension)

        project.addToProjectMap(androidXExtension)
    }

    private fun HasAndroidTest.configureTests() {
        configureLicensePackaging()
        excludeVersionFilesFromTestApks()
    }

    private fun Variant.artRewritingWorkaround() {
        // b/279234807
        experimentalProperties.put(
            "android.experimental.art-profile-r8-rewriting",
            false
        )
    }

    private fun HasAndroidTest.configureLicensePackaging() {
        androidTest?.packaging?.resources?.apply {
            // Workaround a limitation in AGP that fails to merge these META-INF license files.
            pickFirsts.add("/META-INF/AL2.0")
            // In addition to working around the above issue, we exclude the LGPL2.1 license as we're
            // approved to distribute code via AL2.0 and the only dependencies which pull in LGPL2.1
            // are currently dual-licensed with AL2.0 and LGPL2.1. The affected dependencies are:
            //   - net.java.dev.jna:jna:5.5.0
            excludes.add("/META-INF/LGPL2.1")
        }
    }

    /**
     * Excludes files telling which versions of androidx libraries were used in test apks
     * to avoid invalidating the build cache as often
     */
    private fun HasAndroidTest.excludeVersionFilesFromTestApks() {
        androidTest?.packaging?.resources?.apply {
            excludes.add("/META-INF/androidx*.version")
        }
    }

    fun Project.configureKotlinStdlibVersion() {
        project.configurations.all { configuration ->
            configuration.resolutionStrategy { strategy ->
                strategy.eachDependency { details ->
                    if (details.requested.group == "org.jetbrains.kotlin" &&
                        (details.requested.name == "kotlin-stdlib-jdk7" ||
                            details.requested.name == "kotlin-stdlib-jdk8")) {
                        details.useVersion(KOTLIN_VERSION)
                    }
                }
            }
        }
    }

    @Suppress("UnstableApiUsage", "DEPRECATION") // AGP DSL APIs
    private fun configureWithLibraryPlugin(
        project: Project,
        androidXExtension: AndroidXExtension
    ) {
        val libraryExtension = project.extensions.getByType<LibraryExtension>().apply {
            configureAndroidBaseOptions(project, androidXExtension)
            project.addAppApkToTestConfigGeneration(androidXExtension)
            configureAndroidLibraryOptions(project, androidXExtension)

            // Make sure the main Kotlin source set doesn't contain anything under src/main/kotlin.
            val mainKotlinSrcDir = (sourceSets.findByName("main")?.kotlin
                as com.android.build.gradle.api.AndroidSourceDirectorySet)
                .srcDirs
                .filter { it.name == "kotlin" }
                .getOrNull(0)
            if (mainKotlinSrcDir?.isDirectory == true) {
                throw GradleException(
                    "Invalid project structure! AndroidX does not support \"kotlin\" as a " +
                        "top-level source directory for libraries, use \"java\" instead: " +
                        mainKotlinSrcDir.path
                )
            }
        }

        // Remove the android:targetSdkVersion element from the manifest used for AARs.
        project.extensions.getByType<LibraryAndroidComponentsExtension>().onVariants { variant ->
            project.tasks.register(
                variant.name + "AarManifestTransformer",
                AarManifestTransformerTask::class.java
            ).let { taskProvider ->
                variant.artifacts.use(taskProvider)
                    .wiredWithFiles(
                        AarManifestTransformerTask::aarFile,
                        AarManifestTransformerTask::updatedAarFile
                    )
                    .toTransform(SingleArtifact.AAR)
            }
        }

        project.extensions.getByType<com.android.build.api.dsl.LibraryExtension>().apply {
            publishing {
                singleVariant(DEFAULT_PUBLISH_CONFIG)
            }
        }

        project.extensions.getByType<LibraryAndroidComponentsExtension>().apply {
            beforeVariants(selector().withBuildType("release")) { variant ->
                variant.enableUnitTest = false
            }
            onVariants {
                it.configureTests()
                it.artRewritingWorkaround()
            }
            finalizeDsl {
                project.configureAndroidProjectForLint(it.lint, androidXExtension, isLibrary = true)
            }
        }

        project.configurePublicResourcesStub(libraryExtension)
        project.configureSourceJarForAndroid(libraryExtension)
        project.configureVersionFileWriter(libraryExtension, androidXExtension)
        project.configureJavaCompilationWarnings(androidXExtension)

        project.configureDependencyVerification(androidXExtension) { taskProvider ->
            libraryExtension.defaultPublishVariant { libraryVariant ->
                taskProvider.configure { task ->
                    task.dependsOn(libraryVariant.javaCompileProvider)
                }
            }
        }

        val reportLibraryMetrics = project.configureReportLibraryMetricsTask()
        project.addToBuildOnServer(reportLibraryMetrics)
        libraryExtension.defaultPublishVariant { libraryVariant ->
            reportLibraryMetrics.configure {
                it.jarFiles.from(
                    libraryVariant.packageLibraryProvider.map { zip ->
                        zip.inputs.files
                    }
                )
            }
        }

        project.addToProjectMap(androidXExtension)
    }

    private fun configureWithJavaPlugin(project: Project, extension: AndroidXExtension) {
        project.configureErrorProneForJava()

        // Force Java 1.8 source- and target-compatibility for all Java libraries.
        val javaExtension = project.extensions.getByType<JavaPluginExtension>()
        project.afterEvaluate {
            if (extension.type == LibraryType.COMPILER_PLUGIN) {
                javaExtension.apply {
                    sourceCompatibility = VERSION_11
                    targetCompatibility = VERSION_11
                }
            } else if (extension.type.compilationTarget == CompilationTarget.HOST &&
                extension.type != LibraryType.ANNOTATION_PROCESSOR_UTILS
            ) {
                javaExtension.apply {
                    sourceCompatibility = VERSION_17
                    targetCompatibility = VERSION_17
                }
            } else {
                javaExtension.apply {
                    sourceCompatibility = VERSION_1_8
                    targetCompatibility = VERSION_1_8
                }
            }
            if (!project.plugins.hasPlugin(KotlinBasePluginWrapper::class.java)) {
                project.configureSourceJarForJava()
            }
        }

        project.configureJavaCompilationWarnings(extension)

        project.hideJavadocTask()

        project.configureDependencyVerification(extension) { taskProvider ->
            taskProvider.configure { task ->
                task.dependsOn(project.tasks.named(JavaPlugin.COMPILE_JAVA_TASK_NAME))
            }
        }

        // Standard lint, docs, and Metalava configuration for AndroidX projects.
        if (project.multiplatformExtension == null) {
            project.configureNonAndroidProjectForLint(extension)
        }

        project.afterEvaluate {
            if (extension.shouldRelease()) {
                project.extra.set("publish", true)
            }
        }

        // Workaround for b/120487939 wherein Gradle's default resolution strategy prefers external
        // modules with lower versions over local projects with higher versions.
        project.configurations.all { configuration ->
            configuration.resolutionStrategy.preferProjectModules()
        }

        project.addToProjectMap(extension)
    }

    private fun Project.configureProjectStructureValidation(
        extension: AndroidXExtension
    ) {
        // AndroidXExtension.mavenGroup is not readable until afterEvaluate.
        afterEvaluate {
            val mavenGroup = extension.mavenGroup
            val isProbablyPublished = extension.type == LibraryType.PUBLISHED_LIBRARY ||
                extension.type == LibraryType.UNSET
            if (mavenGroup != null && isProbablyPublished && extension.shouldPublish()) {
                validateProjectStructure(mavenGroup.group)
                validateProjectMavenName(extension.name.get(), mavenGroup.group)
            }
        }
    }

    private fun Project.configureProjectVersionValidation(
        extension: AndroidXExtension
    ) {
        // AndroidXExtension.mavenGroup is not readable until afterEvaluate.
        afterEvaluate {
            extension.validateMavenVersion()
        }
    }

    @Suppress("UnstableApiUsage") // Usage of ManagedVirtualDevice
    private fun TestOptions.configureVirtualDevices() {
        managedDevices.devices.register<ManagedVirtualDevice>("pixel2api29") {
            device = "Pixel 2"
            apiLevel = 29
            systemImageSource = "aosp"
        }
        managedDevices.devices.register<ManagedVirtualDevice>("pixel2api30") {
            device = "Pixel 2"
            apiLevel = 30
            systemImageSource = "aosp"
        }
        managedDevices.devices.register<ManagedVirtualDevice>("pixel2api31") {
            device = "Pixel 2"
            apiLevel = 31
            systemImageSource = "aosp"
        }
    }

    private fun BaseExtension.configureAndroidBaseOptions(
        project: Project,
        androidXExtension: AndroidXExtension
    ) {
        compileOptions.apply {
            sourceCompatibility = VERSION_1_8
            targetCompatibility = VERSION_1_8
        }

        compileSdkVersion(COMPILE_SDK_VERSION)
        buildToolsVersion = SupportConfig.buildToolsVersion(project)
        defaultConfig.targetSdk = TARGET_SDK_VERSION
        ndkVersion = SupportConfig.NDK_VERSION

        defaultConfig.testInstrumentationRunner = INSTRUMENTATION_RUNNER

        testOptions.animationsDisabled = true
        testOptions.unitTests.isReturnDefaultValues = true
        testOptions.unitTests.all { task ->
            // https://github.com/robolectric/robolectric/issues/7456
            task.jvmArgs = listOf(
                "--add-opens=java.base/java.lang=ALL-UNNAMED",
                "--add-opens=java.base/java.util=ALL-UNNAMED",
                "--add-opens=java.base/java.io=ALL-UNNAMED",
            )
            // Robolectric 1.7 increased heap size requirements, see b/207169653.
            task.maxHeapSize = "3g"
        }
        testOptions.configureVirtualDevices()

        // Include resources in Robolectric tests as a workaround for b/184641296 and
        // ensure the build directory exists as a workaround for b/187970292.
        testOptions.unitTests.isIncludeAndroidResources = true
        if (!project.buildDir.exists()) project.buildDir.mkdirs()

        defaultConfig.minSdk = DEFAULT_MIN_SDK_VERSION
        project.afterEvaluate {
            val minSdkVersion = defaultConfig.minSdk!!
            check(minSdkVersion >= DEFAULT_MIN_SDK_VERSION) {
                "minSdkVersion $minSdkVersion lower than the default of $DEFAULT_MIN_SDK_VERSION"
            }
            check(compileSdkVersion == COMPILE_SDK_VERSION ||
                project.isCustomCompileSdkAllowed()
            ) {
                "compileSdkVersion must not be explicitly specified, was \"$compileSdkVersion\""
            }
            project.configurations.all { configuration ->
                configuration.resolutionStrategy.eachDependency { dep ->
                    val target = dep.target
                    val version = target.version
                    // Enforce the ban on declaring dependencies with version ranges.
                    // Note: In playground, this ban is exempted to allow unresolvable prebuilts
                    // to automatically get bumped to snapshot versions via version range
                    // substitution.
                    if (version != null && Version.isDependencyRange(version) &&
                        project.rootProject.rootDir == project.getSupportRootFolder()
                    ) {
                        throw IllegalArgumentException(
                            "Dependency ${dep.target} declares its version as " +
                                "version range ${dep.target.version} however the use of " +
                                "version ranges is not allowed, please update the " +
                                "dependency to list a fixed version."
                        )
                    }
                }
            }

            if (androidXExtension.type.compilationTarget != CompilationTarget.DEVICE) {
                throw IllegalStateException(
                    "${androidXExtension.type.name} libraries cannot apply the android plugin, as" +
                        " they do not target android devices"
                )
            }
        }

        val debugSigningConfig = signingConfigs.getByName("debug")
        // Use a local debug keystore to avoid build server issues.
        debugSigningConfig.storeFile = project.getKeystore()
        buildTypes.all { buildType ->
            // Sign all the builds (including release) with debug key
            buildType.signingConfig = debugSigningConfig
        }

        project.configureErrorProneForAndroid(variants)

        // workaround for b/120487939
        project.configurations.all { configuration ->
            // Gradle seems to crash on androidtest configurations
            // preferring project modules...
            if (!configuration.name.lowercase(Locale.US).contains("androidtest")) {
                configuration.resolutionStrategy.preferProjectModules()
            }
        }

        project.configureTestConfigGeneration(this)
        project.configureFtlRunner()

        // AGP warns if we use project.buildDir (or subdirs) for CMake's generated
        // build files (ninja build files, CMakeCache.txt, etc.). Use a staging directory that
        // lives alongside the project's buildDir.
        externalNativeBuild.cmake.buildStagingDirectory =
            File(project.buildDir, "../nativeBuildStaging")
    }

    private fun LibraryExtension.configureAndroidLibraryOptions(
        project: Project,
        androidXExtension: AndroidXExtension
    ) {
        // Note, this should really match COMPILE_SDK_VERSION, however
        // this API takes an integer and we are unable to set it to a
        // pre-release SDK.
        defaultConfig.aarMetadata.minCompileSdk = TARGET_SDK_VERSION
        project.configurations.all { config ->
            val isTestConfig = config.name.lowercase(Locale.US).contains("test")

            config.dependencyConstraints.configureEach { dependencyConstraint ->
                dependencyConstraint.apply {
                    // Clear strict constraints on test dependencies and listenablefuture:1.0
                    // Don't clear non-strict constraints because they might refer to projects,
                    // and clearing their versions might be unsupported and unnecessary
                    if (versionConstraint.strictVersion != "") {
                        if (isTestConfig ||
                            (group == "com.google.guava" &&
                            name == "listenablefuture" &&
                            version == "1.0")
                        ) {
                            version { versionConstraint ->
                                versionConstraint.strictly("")
                            }
                        }
                    }
                }
            }
        }

        project.afterEvaluate {
            if (androidXExtension.shouldRelease()) {
                project.extra.set("publish", true)
            }
        }
    }

    private fun TestedExtension.configureAndroidLibraryWithMultiplatformPluginOptions() {
        sourceSets.findByName("main")!!.manifest.srcFile("src/androidMain/AndroidManifest.xml")
        sourceSets.findByName("androidTest")!!
            .manifest.srcFile("src/androidAndroidTest/AndroidManifest.xml")
    }

    /**
     * Sets the konan distribution url to the prebuilts directory.
     */
    private fun Project.configureKonanDirectory() {
        if (ProjectLayoutType.isPlayground(this)) {
            return // playground does not use prebuilts
        }
        overrideKotlinNativeDistributionUrlToLocalDirectory()
        overrideKotlinNativeDependenciesUrlToLocalDirectory()
    }

    private fun Project.overrideKotlinNativeDependenciesUrlToLocalDirectory() {
        val konanPrebuiltsFolder = getKonanPrebuiltsFolder()
        // use relative path so it doesn't affect gradle remote cache.
        val relativeRootPath = konanPrebuiltsFolder.relativeTo(rootProject.projectDir).path
        val relativeProjectPath = konanPrebuiltsFolder.relativeTo(projectDir).path
        tasks.withType(KotlinNativeCompile::class.java).configureEach {
            it.compilerOptions.freeCompilerArgs.add(
                "-Xoverride-konan-properties=dependenciesUrl=file:$relativeRootPath"
            )
        }
        tasks.withType(CInteropProcess::class.java).configureEach {
            it.settings.extraOpts += listOf(
                "-Xoverride-konan-properties",
                "dependenciesUrl=file:$relativeProjectPath"
            )
        }
    }

    private fun Project.overrideKotlinNativeDistributionUrlToLocalDirectory() {
        val relativePath = getKonanPrebuiltsFolder()
            .resolve("nativeCompilerPrebuilts")
            .relativeTo(projectDir)
            .path
        val url = "file:$relativePath"
        extensions.extraProperties["kotlin.native.distribution.baseDownloadUrl"] = url
    }

    private fun Project.configureKmpTests() {
        val kmpExtension = checkNotNull(
            project.extensions.findByType<KotlinMultiplatformExtension>()
        ) {
            """
            Project ${project.path} applies kotlin multiplatform plugin but we cannot find the
            KotlinMultiplatformExtension.
            """.trimIndent()
        }
        kmpExtension.testableTargets.all { kotlinTarget ->
            if (kotlinTarget is KotlinNativeTargetWithSimulatorTests) {
                kotlinTarget.binaries.all {
                    // Use std allocator to avoid the following warning:
                    // w: Mimalloc allocator isn't supported on target <target>. Used standard mode.
                    it.freeCompilerArgs += "-Xallocator=std"
                }
            }
        }
    }

    private fun AppExtension.configureAndroidApplicationOptions(
        project: Project,
        androidXExtension: AndroidXExtension
    ) {
        defaultConfig.apply {
            versionCode = 1
            versionName = "1.0"
        }

        project.addAppApkToTestConfigGeneration(androidXExtension)
        project.addAppApkToFtlRunner()
    }

    private fun Project.configureDependencyVerification(
        extension: AndroidXExtension,
        taskConfigurator: (TaskProvider<VerifyDependencyVersionsTask>) -> Unit
    ) {
        afterEvaluate {
            if (extension.type != LibraryType.SAMPLES) {
                val verifyDependencyVersionsTask = project.createVerifyDependencyVersionsTask()
                if (verifyDependencyVersionsTask != null) {
                    taskConfigurator(verifyDependencyVersionsTask)
                }
            }
        }
    }

    // If this project wants other project in the same group to have the same version,
    // this function configures those constraints.
    private fun Project.configureConstraintsWithinGroup(
        extension: AndroidXExtension
    ) {
        if (!project.shouldAddGroupConstraints().get()) {
            return
        }
        project.afterEvaluate {
            if (project.hasKotlinNativeTarget().get()) {
                // KMP plugin cannot handle constraints properly for native targets
                // b/274786186, YT: KT-57531
                // It is expected to be fixed in Kotlin 1.9 after which, we should remove this check
                return@afterEvaluate
            }

            // make sure that the project has a group
            val projectGroup = extension.mavenGroup
            if (projectGroup == null)
                return@afterEvaluate
            // make sure that this group is configured to use a single version
            val requiredVersion = projectGroup.atomicGroupVersion
            if (requiredVersion == null)
                return@afterEvaluate

            // We don't want to emit the same constraint into our .module file more than once,
            // and we don't want to try to apply a constraint to a configuration that doesn't accept them,
            // so we create a configuration to hold the constraints and make each other constraint extend it
            val constraintConfiguration = project.configurations.create("groupConstraints")
            project.configurations.configureEach { configuration ->
                if (configuration != constraintConfiguration)
                    configuration.extendsFrom(constraintConfiguration)
            }

            val otherProjectsInSameGroup = extension.getOtherProjectsInSameGroup()
            val constraints = project.dependencies.constraints
            val allProjectsExist = buildContainsAllStandardProjects()
            for (otherProject in otherProjectsInSameGroup) {
	        val otherGradlePath = otherProject.gradlePath
                if (otherGradlePath == ":compose:ui:ui-android-stubs") {
                    // exemption for library that doesn't truly get published: b/168127161
                    continue
                }
                // We only enable constraints for builds that we intend to be able to publish from.
                //   If a project isn't included in a build we intend to be able to publish from,
                //   the project isn't going to be published.
                // Sometimes this can happen when a project subset is enabled:
                //   The KMP project subset enabled by androidx_multiplatform_mac.sh contains
                //   :benchmark:benchmark-common but not :benchmark:benchmark-benchmark
                //   This is ok because we don't intend to publish that artifact from that build
                val otherProjectShouldExist =
                    allProjectsExist || findProject(otherGradlePath) != null
                if (!otherProjectShouldExist) {
                    continue
                }
                // We only emit constraints referring to projects that will release
                val otherFilepath = File(otherProject.filePath, "build.gradle")
                val parsed = parseBuildFile(otherFilepath)
                if (!parsed.shouldRelease()) {
                    continue
                }
                if (parsed.libraryType == LibraryType.SAMPLES) {
                    // a SAMPLES project knows how to publish, but we don't intend to actually
                    // publish it
                    continue
                }
                // Under certain circumstances, a project is allowed to override its
                // version see ( isGroupVersionOverrideAllowed ), in which case it's
                // not participating in the versioning policy yet and we don't emit
                // version constraints referencing it
                if (parsed.specifiesVersion) {
                    continue
                }
                val dependencyConstraint = project(otherGradlePath)
                constraints.add(
                    constraintConfiguration.name,
                    dependencyConstraint
                )
            }

            // disallow duplicate constraints
            project.configurations.all { config ->
                // find all constraints contributed by this Configuration and its ancestors
                val configurationConstraints: MutableSet<String> = mutableSetOf()
                config.hierarchy.forEach { parentConfig ->
                    parentConfig.dependencyConstraints.configureEach { dependencyConstraint ->
                        dependencyConstraint.apply {
                            if (
                                versionConstraint.requiredVersion != "" &&
                                versionConstraint.requiredVersion != "unspecified"
                            ) {
                                val key =
                                    "${dependencyConstraint.group}:${dependencyConstraint.name}"
                                if (configurationConstraints.contains(key)) {
                                    throw GradleException(
                                        "Constraint on $key was added multiple times in " +
                                        "$config (version = " +
                                        "${versionConstraint.requiredVersion}).\n\n" +
                                        "This is unnecessary and can also trigger " +
                                        "https://github.com/gradle/gradle/issues/24037 in " +
                                        "builds trying to use the resulting artifacts.")
                                }
                                configurationConstraints.add(key)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Tells whether this build contains the usual set of all projects (`./gradlew projects`)
     * Sometimes developers request to include fewer projects because this may run more quickly
     */
    private fun Project.buildContainsAllStandardProjects(): Boolean {
        if (getProjectSubset() != null)
            return false
        if (ProjectLayoutType.isPlayground(this))
            return false
        return true
    }

    companion object {
        const val CREATE_LIBRARY_BUILD_INFO_FILES_TASK = "createLibraryBuildInfoFiles"
        const val GENERATE_TEST_CONFIGURATION_TASK = "GenerateTestConfiguration"
        const val ZIP_TEST_CONFIGS_WITH_APKS_TASK = "zipTestConfigsWithApks"
        const val ZIP_CONSTRAINED_TEST_CONFIGS_WITH_APKS_TASK = "zipConstrainedTestConfigsWithApks"

        const val TASK_GROUP_API = "API"

        const val EXTENSION_NAME = "androidx"

        /**
         * Fail the build if a non-Studio task runs longer than expected
         */
        const val TASK_TIMEOUT_MINUTES = 60L
    }
}

private const val PROJECTS_MAP_KEY = "projects"
private const val ACCESSED_PROJECTS_MAP_KEY = "accessedProjectsMap"

/**
 * Hides a project's Javadoc tasks from the output of `./gradlew tasks` by setting their group to
 * `null`.
 *
 * AndroidX projects do not use the Javadoc task for docs generation, so we don't want them
 * cluttering up the task overview.
 */
private fun Project.hideJavadocTask() {
    tasks.withType(Javadoc::class.java).configureEach {
        if (it.name == "javadoc") {
            it.group = null
        }
    }
}

private fun Project.addToProjectMap(extension: AndroidXExtension) {
    // TODO(alanv): Move this out of afterEvaluate
    afterEvaluate {
        if (extension.shouldRelease()) {
            val group = extension.mavenGroup?.group
            if (group != null) {
                val module = "$group:$name"

                if (project.rootProject.extra.has(ACCESSED_PROJECTS_MAP_KEY)) {
                    throw GradleException(
                        "Attempted to add $project to project map after " +
                            "the contents of the map were accessed"
                    )
                }
                @Suppress("UNCHECKED_CAST")
                val projectModules = project.rootProject.extra.get(PROJECTS_MAP_KEY)
                    as ConcurrentHashMap<String, String>
                projectModules[module] = path
            }
        }
    }
}

val Project.multiplatformExtension
    get() = extensions.findByType(KotlinMultiplatformExtension::class.java)

@Suppress("UNCHECKED_CAST")
fun Project.getProjectsMap(): ConcurrentHashMap<String, String> {
    project.rootProject.extra.set(ACCESSED_PROJECTS_MAP_KEY, true)
    return rootProject.extra.get(PROJECTS_MAP_KEY) as ConcurrentHashMap<String, String>
}

/**
 * Configures all non-Studio tasks in a project (see b/153193718 for background) to time out after
 * [TASK_TIMEOUT_MINUTES].
 */
private fun Project.configureTaskTimeouts() {
    tasks.configureEach { t ->
        // skip adding a timeout for some tasks that both take a long time and
        // that we can count on the user to monitor
        if (t !is StudioTask) {
            t.timeout.set(Duration.ofMinutes(TASK_TIMEOUT_MINUTES))
        }
    }
}

private fun Project.configureJavaCompilationWarnings(androidXExtension: AndroidXExtension) {
    afterEvaluate {
        project.tasks.withType(JavaCompile::class.java).configureEach { task ->
            // If we're running a hypothetical test build confirming that tip-of-tree versions
            // are compatible, then we're not concerned about warnings
            if (!project.usingMaxDepVersions()) {
                task.options.compilerArgs.add("-Xlint:unchecked")
                if (androidXExtension.failOnDeprecationWarnings) {
                    task.options.compilerArgs.add("-Xlint:deprecation")
                }
            }
        }
    }
}

fun Project.hasBenchmarkPlugin(): Boolean {
    return this.plugins.hasPlugin(BenchmarkPlugin::class.java)
}

/**
 * Returns a string that is a valid filename and loosely based on the project name
 * The value returned for each project will be distinct
 */
fun String.asFilenamePrefix(): String {
    return this.substring(1).replace(':', '-')
}

/**
 * Sets the specified [task] as a dependency of the top-level `check` task, ensuring that it runs
 * as part of `./gradlew check`.
 */
fun <T : Task> Project.addToCheckTask(task: TaskProvider<T>) {
    project.tasks.named("check").configure {
        it.dependsOn(task)
    }
}

/**
 * Expected to be called in afterEvaluate when all extensions are available
 */
internal fun Project.hasAndroidTestSourceCode(): Boolean {
    // com.android.test modules keep test code in main sourceset
    extensions.findByType(TestExtension::class.java)?.let { extension ->
        extension.sourceSets.findByName("main")?.let { sourceSet ->
            if (!sourceSet.java.getSourceFiles().isEmpty) return true
        }
        // check kotlin-android main source set
        extensions.findByType(KotlinAndroidProjectExtension::class.java)
            ?.sourceSets?.findByName("main")?.let {
                if (it.kotlin.files.isNotEmpty()) return true
            }
        // Note, don't have to check for kotlin-multiplatform as it is not compatible with
        // com.android.test modules
    }

    // check Java androidTest source set
    extensions.findByType(TestedExtension::class.java)
        ?.sourceSets
        ?.findByName("androidTest")
        ?.let { sourceSet ->
            // using getSourceFiles() instead of sourceFiles due to b/150800094
            if (!sourceSet.java.getSourceFiles().isEmpty) return true
        }

    // check kotlin-android androidTest source set
    extensions.findByType(KotlinAndroidProjectExtension::class.java)
        ?.sourceSets?.findByName("androidTest")?.let {
            if (it.kotlin.files.isNotEmpty()) return true
        }

    // check kotlin-multiplatform androidAndroidTest source set
    multiplatformExtension?.apply {
        sourceSets.findByName("androidAndroidTest")?.let {
            if (it.kotlin.files.isNotEmpty()) return true
        }
    }

    return false
}

fun Project.validateMultiplatformPluginHasNotBeenApplied() {
    if (plugins.hasPlugin(KotlinMultiplatformPluginWrapper::class.java)) {
        throw GradleException(
            "The Kotlin multiplatform plugin should only be applied by the AndroidX plugin."
        )
    }
}

/**
 * Verifies that ProjectParser computes the correct values for this project
 */
fun Project.validateProjectParser(extension: AndroidXExtension) {
    project.afterEvaluate {
        val parsed = project.parse()
        check(extension.type == parsed.libraryType) {
            "ProjectParser incorrectly computed libraryType = ${parsed.libraryType} " +
                "instead of ${extension.type}"
        }
        check(extension.publish == parsed.publish) {
            "ProjectParser incorrectly computed publish = ${parsed.publish} " +
                "instead of ${extension.publish}"
        }
        check(extension.shouldPublish() == parsed.shouldPublish()) {
            "ProjectParser incorrectly computed shouldPublish() = ${parsed.shouldPublish()} " +
                "instead of ${extension.shouldPublish()}"
        }
        check(extension.shouldRelease() == parsed.shouldRelease()) {
            "ProjectParser incorrectly computed shouldRelease() = ${parsed.shouldRelease()} " +
                "instead of ${extension.shouldRelease()}"
        }
        check(extension.projectDirectlySpecifiesMavenVersion == parsed.specifiesVersion) {
            "ProjectParser incorrectly computed specifiesVersion = ${parsed.specifiesVersion}" +
                "instead of ${extension.projectDirectlySpecifiesMavenVersion}"
        }
    }
}

/**
 * Validates the Maven version against Jetpack guidelines.
 */
fun AndroidXExtension.validateMavenVersion() {
    val mavenGroup = mavenGroup
    val mavenVersion = mavenVersion
    val forcedVersion = mavenGroup?.atomicGroupVersion
    if (forcedVersion != null && forcedVersion == mavenVersion) {
        throw GradleException(
            """
            Unnecessary override of same-group library version

            Project version is already set to $forcedVersion by same-version group
            ${mavenGroup.group}.

            To fix this error, remove "mavenVersion = ..." from your build.gradle
            configuration.
            """.trimIndent()
        )
    }
}

/**
 * Removes the line and column attributes from the [baseline].
 */
fun removeLineAndColumnAttributes(baseline: String): String = baseline.replace(
    "\\s*(line|column)=\"\\d+?\"".toRegex(),
    ""
)

const val ANDROIDX_ARTIFACT_EXT_NAME = "androidxArtifact"
const val PROJECT_OR_ARTIFACT_EXT_NAME = "projectOrArtifact"
