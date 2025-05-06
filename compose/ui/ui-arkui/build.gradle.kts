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
        val userHomeDir = System.getProperty("user.home")
        val osArch = System.getProperty("os.arch").lowercase()
        val suffix: String = when (osArch) {
            "aarch64", "arm64" -> "aarch64"
            "x86_64", "x64", "amd64" -> "x64"
            else -> "aarch64"
        }
        val harmonyNativeDir = "$userHomeDir/.konan/dependencies/native-15-ohos-macos-${suffix}"
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
}