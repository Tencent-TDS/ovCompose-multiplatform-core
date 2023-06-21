import androidx.build.AndroidXComposePlugin
import java.util.Properties

plugins {
    id("AndroidXPlugin")
    id("AndroidXComposePlugin")
    id("kotlin-multiplatform")
    id("org.jetbrains.gradle.apple.applePlugin") version "222.4550-0.21-dev-0"
}

AndroidXComposePlugin.applyAndConfigureKotlinPlugin(project)

repositories {
    mavenLocal()
}

kotlin {
    val isArm64Host = System.getProperty("os.arch") == "aarch64"
    val additionalCompilerArgs = listOf(
        "-linker-option", "-framework", "-linker-option", "Metal",
        "-linker-option", "-framework", "-linker-option", "CoreText",
        "-linker-option", "-framework", "-linker-option", "CoreGraphics"
    )
    iosArm64 {
        binaries {
            framework {
                baseName = "shared"
                freeCompilerArgs += additionalCompilerArgs
            }
        }
    }
    if (isArm64Host) {
        iosSimulatorArm64 {
            binaries {
                framework {
                    baseName = "shared"
                    freeCompilerArgs += additionalCompilerArgs
                }
            }
        }
    } else {
        iosX64 {
            binaries {
                framework {
                    baseName = "shared"
                    freeCompilerArgs += additionalCompilerArgs
                }
            }
        }
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":compose:foundation:foundation"))
                implementation(project(":compose:foundation:foundation-layout"))
                implementation(project(":compose:material:material"))
                implementation(project(":compose:mpp"))
                implementation(project(":compose:mpp:demo"))
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
        val uikitMain by creating { dependsOn(darwinMain) }
        val iosArm64Main by getting { dependsOn(uikitMain) }
        if (isArm64Host) {
            val iosSimulatorArm64Main by getting { dependsOn(uikitMain) }
        } else {
            val iosX64Main by getting { dependsOn(uikitMain) }
        }
    }
}

apple {
    iosApp {
        productName = "composeuikit"

        sceneDelegateClass = "SceneDelegate"
        launchStoryboard = "LaunchScreen"

        val runOnDevice = findProperty("xcode.arch") == "arm64"
        val projectProperties = Properties()
        val projectPropertiesFile = rootProject.file("project.properties")
        if (projectPropertiesFile.exists()) {
            projectProperties.load(projectPropertiesFile.reader())
        } else {
            projectPropertiesFile.createNewFile()
        }
        val teamId = projectProperties.getProperty("TEAM_ID")
        if (runOnDevice && teamId == null) {
            error("Add TEAM_ID=... to file ${projectPropertiesFile.absolutePath}")
        }
        if (teamId != null) {
            buildSettings.DEVELOPMENT_TEAM(teamId)
        }
        buildSettings.DEPLOYMENT_TARGET("15.0")

        // TODO: add 'CADisableMinimumFrameDurationOnPhone' set to 'YES'

        dependencies {
            // Here we can add additional dependencies to Swift sourceSet
        }
    }
}

// TODO: Workaround, see https://youtrack.jetbrains.com/issue/KT-55751
val myAttribute = Attribute.of("myOwnAttribute", String::class.java)
// replace releaseFrameworkIosFat by the name of the first configuration that conflicts
configurations.named("releaseFrameworkIosFat").configure {
    attributes {
        // put a unique attribute
        attribute(myAttribute, "release-all")
    }
}

// replace debugFrameworkIosFat by the name of the second configuration that conflicts
configurations.named("debugFrameworkIosFat").configure {
    attributes {
        attribute(myAttribute, "debug-all")
    }
}
