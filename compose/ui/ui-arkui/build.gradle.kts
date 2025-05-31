/*
 * Tencent is pleased to support the open source community by making ovCompose available.
 * Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
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

import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.konan.target.HostManager

plugins {
    id("AndroidXPlugin")
    id("kotlin-multiplatform")
}

kotlin {
    ohosArm64 {
        val main by compilations.getting {
            cmakes.create("compose_arkui_utils") {
                sourceDir = "src/ohosArm64Main/cpp/compose/src/main/cpp"
            }
            cinterops.create("compose_arkui_utils") {
                defFile("src/nativeInterop/cinterop/compose_arkui_utils.def")
                includeDirs("src/ohosArm64Main/cpp/compose/src/main/cpp/compose")
            }

            cinterops.create("arkui") {
                val cinterop = File(project.projectDir, "src/ohosArm64Main/cinterop")
                defFile(File(cinterop, "arkui.def"))
                includeDirs(File(cinterop, "include"))
                includeDirs(File(cinterop, "include/arkui"))
            }
        }
    }
}

apply(from = "androidx.gradle")

// Models CMake integration after Kotlin's CInterop API to enable C/C++ project compilation.
// Planned for future migration into Kotlin Gradle Plugin, providing HarmonyOS projects with
// similar native interoperability capabilities as CInterop.
val KotlinNativeCompilation.cmakes: CMakeSettingsHolder
    get() = CMakeSettingsHolder(this)

class CMakeSettings(val name: String) {
    var sourceDir: String = ""
}

class CMakeSettingsHolder(private val compilation: KotlinNativeCompilation) {

    fun create(name: String, configure: CMakeSettings.() -> Unit): CMakeSettings {
        val settings = CMakeSettings(name).apply(configure)
        check(settings.sourceDir.isNotEmpty()) { "sourceDir is empty." }
        compilation.cmake(settings.name, settings.sourceDir)
        return settings
    }

    private fun KotlinNativeCompilation.cmake(name: String, sourceDirString: String) {
        val sourceDir = file(sourceDirString)
        val buildDir = "${project.buildDir}/cpp/${target.name}/$name"
        val binaryFile = File(buildDir, "lib${name}.a")

        val harmonyNativeDir = File(getLocalSdkPath(), "native")
        val cmakePath = File("$harmonyNativeDir/build-tools/cmake/bin/cmake")
        val cmakeToolChainPath = File("$harmonyNativeDir/build/cmake/ohos.toolchain.cmake")
        val taskCmakeConfig = "${compileTaskProvider.name}lib${name}CMakeConfig"
        val taskCmakeBuild = "${compileTaskProvider.name}lib${name}CMakeBuild"
        tasks.register<Exec>(taskCmakeConfig) {
            group = "build"
            description = "CMake Config"
            commandLine(
                cmakePath.absolutePath,
                "-S", sourceDir,
                "-B", buildDir,
                "-GNinja",
                "-DCMAKE_TOOLCHAIN_FILE=${cmakeToolChainPath.absolutePath}",
                "-DOHOS_ARCH=arm64-v8a",
                "-DOHOS_PLATFORM=OHOS"
            )
        }

        tasks.register<Exec>(taskCmakeBuild) {
            group = "build"
            description = "CMake Build"
            dependsOn(taskCmakeConfig)
            commandLine(
                cmakePath.absolutePath,
                "--build", buildDir
            )
        }

        tasks.getByName(compileTaskProvider.name) {
            dependsOn(taskCmakeBuild)
            inputs
                .file(binaryFile)
                .withPropertyName("$name-${target.name}-static-lib")
        }

        target.binaries.all {
            freeCompilerArgs += listOf("-include-binary", binaryFile.absolutePath)
        }
        target.compilations.all {
            kotlinOptions {
                freeCompilerArgs += listOf("-include-binary", binaryFile.absolutePath)
            }
        }
    }


    private fun getLocalSdkPath(): String {
        if (HostManager.host.family.isAppleFamily) {
            val sdkPath =
                getSystemValue("OHOS_SDK_HOME") ?: File(
                    getSystemValue("DEVECO_STUDIO_HOME") ?: "/Applications/DevEco-Studio.app",
                    "Contents/sdk/default/openharmony"
                ).path
            checkOhosSdkPath(sdkPath)
            checkOhosSdkVersion(sdkPath)
            return sdkPath
        } else {
            throw IllegalStateException("Unsupported host: ${HostManager.host}")
        }
    }

    private fun checkOhosSdkPath(sdkPath: String) {
        check(File(sdkPath).exists()) {
            "OHOS SDK is not found. It is required to build platform libs for OHOS.\n" +
                "Set 'OHOS_SDK_HOME=/path/to/openharmony' or 'DEVECO_STUDIO_HOME=/path/to/DevEco-Studio' in the gradle.properties " +
                "or install DevEco Studio in the default location '/Applications/DevEco-Studio.app'. "
        }
    }

    private fun checkOhosSdkVersion(sdkPath: String) {
        if (rootProject.findProperty("ignoreOhosSdkVersionCheck") != "true") {
            val minimalVersion =
                rootProject.findProperty("minimalOhosSdkVersion")?.toString()?.toIntOrNull()
                    ?: DEFAULT_OHOS_SDK_VERSION
            val sdkPkg = File(sdkPath, "native/oh-uni-package.json").readText()
            val currentVersion =
                Regex(""""apiVersion": "(\d+)"""").find(sdkPkg)?.groupValues?.getOrNull(1)
                    ?.toIntOrNull() ?: Int.MIN_VALUE
            check(currentVersion >= minimalVersion) {
                "Unsupported OHOS SDK version $currentVersion(bundled in $sdkPath), minimal supported version is $minimalVersion."
            }
        }
    }

    private fun getSystemValue(key: String): String? {
        return (System.getProperty(key) ?: System.getenv(key))?.takeIf { it.isNotBlank() }
    }

    companion object {
        private const val DEFAULT_OHOS_SDK_VERSION = 15
    }
}