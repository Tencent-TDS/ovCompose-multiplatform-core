import androidx.build.AndroidXComposePlugin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    id("AndroidXPlugin")
    id("AndroidXComposePlugin")
    id("kotlin-multiplatform")
    id("org.jetbrains.gradle.apple.applePlugin") version "222.3345.143-0.16"
}

val osName = System.getProperty("os.name")
val hostOs = when {
    osName == "Mac OS X" -> "macos"
    osName.startsWith("Win") -> "windows"
    osName.startsWith("Linux") -> "linux"
    else -> error("Unsupported OS: $osName")
}

val osArch = System.getProperty("os.arch")
var hostArch = when (osArch) {
    "x86_64", "amd64" -> "x64"
    "aarch64" -> "arm64"
    else -> error("Unsupported arch: $osArch")
}

val host = "${hostOs}-${hostArch}"
enum class Target(val simulator: Boolean, val key: String) {
    WATCHOS_X86(true, "watchos"), WATCHOS_ARM64(false, "watchos"),
    IOS_X64(true, "iosX64"), IOS_ARM64(false, "iosArm64"), IOS_SIMULATOR_ARM64(true, "iosSimulatorArm64")
}

AndroidXComposePlugin.applyAndConfigureKotlinPlugin(project)

dependencies {

}

repositories {
    mavenLocal()
}

kotlin {
    if (hostOs == "macos") {
        ios() {
            configureToLaunchFromAppCode()
            configureToLaunchFromXcode()
        }
        iosSimulatorArm64() {
            configureToLaunchFromAppCode()
            configureToLaunchFromXcode()
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":compose:foundation:foundation"))
                implementation(project(":compose:foundation:foundation-layout"))
                implementation(project(":compose:material:material"))
                implementation(project(":compose:mpp"))
                implementation(project(":compose:runtime:runtime"))
                implementation(project(":compose:ui:ui"))
                implementation(project(":compose:ui:ui-graphics"))
                implementation(project(":compose:ui:ui-text"))
                implementation(libs.kotlinCoroutinesCore)
            }
        }
        val skikoMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.skikoCommon)
            }
        }
        val nativeMain by creating { dependsOn(skikoMain) }
        val darwinMain by creating { dependsOn(nativeMain) }

        if (hostOs == "macos") {
            val iosMain by getting {
                dependsOn(darwinMain)
            }
            val iosSimulatorArm64Main by getting {
                dependsOn(iosMain)
            }
        }
    }
}

apple {
    iosApp {
        productName = "composeuikit"

        sceneDelegateClass = "SceneDelegate"
        launchStoryboard = "LaunchScreen"

        dependencies {
            implementation(project(":compose:mpp:demo-uikit"))
        }
    }
}

if (hostOs == "macos") {
    // Create Xcode integration tasks.
    val sdkName: String? = System.getenv("SDK_NAME")

    val target = sdkName.orEmpty().let {
        when {
            it.startsWith("iphoneos") -> Target.IOS_ARM64
            it.startsWith("watchos") -> Target.WATCHOS_ARM64
            it.startsWith("watchsimulator") -> Target.WATCHOS_X86
            else -> when (host) {
                "macos-x64" -> Target.IOS_X64
                "macos-arm64" -> Target.IOS_SIMULATOR_ARM64
                else -> throw GradleException("Host OS is not supported")
            }
        }
    }

    val targetBuildDir: String? = System.getenv("TARGET_BUILD_DIR")
    val executablePath: String? = System.getenv("EXECUTABLE_PATH")
    val buildType = System.getenv("CONFIGURATION")?.let {
        org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType.valueOf(it.toUpperCase())
    } ?: org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType.DEBUG

    val currentTarget = kotlin.targets[target.key] as org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
    val kotlinBinary = currentTarget.binaries.getExecutable(buildType)
    val xcodeIntegrationGroup = "Xcode integration"

    val packForXCode = if (sdkName == null || targetBuildDir == null || executablePath == null) {
        // The build is launched not by Xcode ->
        // We cannot create a copy task and just show a meaningful error message.
        tasks.create("packForXCode").doLast {
            throw IllegalStateException("Please run the task from Xcode")
        }
    } else {
        // Otherwise copy the executable into the Xcode output directory.
        tasks.create("packForXCode", Copy::class.java) {
            dependsOn(kotlinBinary.linkTask)

            destinationDir = file(targetBuildDir)

            val dsymSource = kotlinBinary.outputFile.absolutePath + ".dSYM"
            val dsymDestination = File(executablePath).parentFile.name + ".dSYM"
            val oldExecName = kotlinBinary.outputFile.name
            val newExecName = File(executablePath).name

            from(dsymSource) {
                into(dsymDestination)
                rename(oldExecName, newExecName)
            }

            from(kotlinBinary.outputFile) {
                rename { executablePath }
            }
        }
    }
}

fun KotlinNativeTarget.configureToLaunchFromAppCode() {
    binaries {
        framework {
            baseName = "shared"
            freeCompilerArgs += listOf(
                "-linker-option", "-framework", "-linker-option", "Metal",
                "-linker-option", "-framework", "-linker-option", "CoreText",
                "-linker-option", "-framework", "-linker-option", "CoreGraphics"
            )
        }
    }
}

fun KotlinNativeTarget.configureToLaunchFromXcode() {
    binaries {
        executable {
            entryPoint = "androidx.compose.mpp.demo.main"
            freeCompilerArgs += listOf(
                "-linker-option", "-framework", "-linker-option", "Metal",
                "-linker-option", "-framework", "-linker-option", "CoreText",
                "-linker-option", "-framework", "-linker-option", "CoreGraphics"
            )
        }
    }
}
