<p align="center">
    <img alt="ovCompose Logo" src="img/ovCompose.svg" />
</p>

ovCompose (online-video-compose) is a cross-platform development framework launched by the Tencent Video team within Oteam, the leading frontend group at Tencent. It is based on the Compose Multiplatform ecosystem and aims to address the limitations of Jetbrains Compose Multiplatform, specifically its lack of support for the HarmonyOS platform and the constraints on mixed layout rendering on iOS. ovCompose makes it easier for businesses to build fully cross-platform apps.

### Compose Repositories

[compose-multiplatform](https://github.com/Tencent-TDS/KuiklyBase-platform/tree/main/compose-multiplatform): Plugins for compose multiplatform with compose-gradle-plugin, resources, ui-tooling-preview.

[compose-multiplatform-core](.): The source core of multiplatform compose with material, foundation, ui, runtime.

[ovCompose-sample](https://github.com/Tencent-TDS/ovCompose-sample): Sample of multiplatform compose about ui, layout, gesture.



### Compile and Publish

1. Replace the `Compose` value in `libraryversions.toml` with your own version.

   ```toml
   COMPOSE = "1.6.1-dev-18.0.1"
   ```

   

2. Publish artifacts with targets to local maven.

   ```bash
   ./gradlew :mpp:publishComposeJbToMavenLocal -Pcompose.platforms=android,ohosArm64,uikit
   ```

   

3. Publish `compose.har` for harmony

   open [composeApp](compose/ui/ui-arkui/src/ohosArm64Main/cpp/composeApp) with [DevEco-Studio](https://developer.huawei.com/consumer/cn/deveco-studio/), 
   run the following command to output the `compose.har`, located `compose/build/outputs/default/compose.har`.

   ```bash
   # build compose.har with hvigorw tool
   /Applications/DevEco-Studio.app/Contents/tools/hvigor/bin/hvigorw --mode module -p product -p module=compose assembleHar
   ```




### Get Started

> see `ovCompose-sample` project for source codes

#### Import Compose in ArkUI

1. Create compose multiplatform project

   Create kotlin multiplatform project with [Android Studio](https://developer.android.com/studio), and build project via the specific kotlin with ohos target, backed by `Tencent OnlineVideo`. 

   `libs.version.toml` is as follows

   ```toml
   [versions]
   agp = "8.0.2"
   compose-plugin = "1.6.1-20.0.9"
   kotlin = "2.0.21-mini-007"
   kotlinx-coroutines = "1.9.23"
   
   [libraries]
   # Compose multiplatform
   compose-multiplatform-export = { module = "org.jetbrains.compose.export:export", version.ref = "compose-plugin" }
   
   kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "kotlinx-coroutines" }
   
   [plugins]
   androidApplication = { id = "com.android.application", version.ref = "agp" }
   androidLibrary = { id = "com.android.library", version.ref = "agp" }
   jetbrainsCompose = { id = "org.jetbrains.compose", version.ref = "compose-plugin" }
   kotlinAndroid = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
   kotlinMultiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
   cocoapods = { id = "org.jetbrains.kotlin.native.cocoapods", version.ref = "kotlin" }
   composeCompiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
   ```

   `build.gradle.kts` is as follows

   ```kotlin
   plugins {
       // apply kotlinMultiplatform jetbrainsCompose and composeCompiler plugins
       alias(libs.plugins.kotlinMultiplatform)
       alias(libs.plugins.jetbrainsCompose)
       alias(libs.plugins.composeCompiler)
   }
   
   kotlin {
       ohosArm64 {
           binaries.sharedLib {
               // specify the shared lib name
               baseName = "kn"
               // link skia lib
               linkerOpts("-L${projectDir}/libs/", "-lskia")
               // export `compose.export`
               export(libs.compose.multiplatform.export)
           }
       }
   
       sourceSets {
           commonMain.dependencies {
               implementation(compose.runtime)
               implementation(compose.foundation)
               implementation(compose.material3)
               implementation(compose.material)
               implementation(compose.ui)
           }
   
           val ohosArm64Main by getting {
               dependencies {
                   // api compose.multiplatform.export lib for export C API.
                   api(libs.compose.multiplatform.export)
               }
           }
       }
   }
   ```

   

2. Declare Compose Multiplatform Code

   ```kotlin
   // declare in commonMain for all targets.
   @Composable
   internal fun Hello() {
       Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
           Text("Hello Compose!")
       }
   }
   
   // declare in ohosArm64Main, returning ArkUIViewController imported into ArkUI.
   @CName("createHelloArkUIViewController")
   fun createHelloArkUIViewController(env: napi_env): napi_value =
       ComposeArkUIViewController(env) {
           Hello()
       }
   ```

   

3. Build kotlin multiplatform project to output binary product imported into harmony platform.

   Run `linkDebugSharedOhosArm64 ` or `linkReleaseSharedOhosArm64` task in multiplatform module.
   `libkn.so` and `libkn_api.h` will be output to `build/bin/ohosArm64` in own module.

   

4. Creating harmonyApp project

   - Creating

     create harmonyApp project with [DevEco-Studio](https://developer.huawei.com/consumer/cn/deveco-studio/) in sub，select "Native C++" for getting native configurations in step `Create Project`

   - Import multiplatform harmony binary product
     - copy `libkn.so` to `entry/libs/arm64-v8a/`
     - copy `libkn_api.h` to `entry/src/main/cpp/include/`

     To simplify all of this, we create Gradle Tasks in multiplatform project, just run `publishDebugBinariesToHarmonyApp` or `publishReleaseBinariesToHarmonyApp` to build and output to harmony project。

     ```kotlin
     kotlin { /* */ }
     arrayOf("debug", "release").forEach { type ->
         tasks.register<Copy>("publish${type.capitalizeUS()}BinariesToHarmonyApp") {
             group = "harmony"
             dependsOn("link${type.capitalizeUS()}SharedOhosArm64")
             into(rootProject.file("harmonyApp"))
             from("build/bin/ohosArm64/${type}Shared/libkn_api.h") {
                 into("entry/src/main/cpp/include/")
             }
             from(project.file("build/bin/ohosArm64/${type}Shared/libkn.so")) {
                 into("/entry/libs/arm64-v8a/")
             }
         }
     }
     ```

   - Import `skikobridge.har` and `compose.har` dependencies

     - Copy `skikobridge.har` to `entry/libs/`, the `skikobridge.har` can get from `ovCompose-sample/harmonyApp`。
     - Copy `compose.har` to `entry/libs`, the `compose.har` can build from `compose-multiplatform-core/ui-arkui`。

   

5. Configure  harmonyApp project

   configure dependencies in `entry/oh-package.json`

   ```json
   {
     "name": "entry",
     "version": "1.0.0",
     "description": "Please describe the basic information.",
     "main": "",
     "author": "",
     "license": "",
     "dependencies": {
       "libentry.so": "file:./src/main/cpp/types/libentry",
       // import compose.har dependency
       "compose": "file:./libs/compose.har",
       // import skikobridge.har dependency
       "skikobridge": "file:./libs/skikobridge.har"
     }
   }
   ```

   Configure CMake compile in `entry/src/main/cpp/CMakeLists.txt`

   ```makefile
   # the minimum version of CMake.
   cmake_minimum_required(VERSION 3.5.0)
   project(harmonyApp)
   
   set(NATIVERENDER_ROOT_PATH ${CMAKE_CURRENT_SOURCE_DIR})
   
   if(DEFINED PACKAGE_FIND_FILE)
       include(${PACKAGE_FIND_FILE})
   endif()
   
   add_definitions(-std=c++17)
   
   include_directories(${NATIVERENDER_ROOT_PATH}
                       ${NATIVERENDER_ROOT_PATH}/include)
   
   # 获取 skikobridge package
   find_package(skikobridge)
   
   add_library(entry SHARED napi_init.cpp)
   target_link_libraries(entry PUBLIC libace_napi.z.so)
   # link libkn.so
   target_link_libraries(entry PUBLIC ${NATIVERENDER_ROOT_PATH}/../../../libs/arm64-v8a/libkn.so)
   # link skikobridge.so in skikobridge
   target_link_libraries(entry PUBLIC skikobridge::skikobridge)
   # link others
   target_link_libraries(entry PUBLIC ${EGL-lib} ${GLES-lib} ${hilog-lib} ${libace-lib} ${libnapi-lib} ${libuv-lib} libc++_shared.so)
   ```

   

6. Import Compose to hramonyApp

   Init Compose ArkUI and ArkUIViewController

   ```c++
   // entry/src/main/cpp/napi_init.cpp
   
   static napi_value CreateHelloArkUIViewController(napi_env env, napi_callback_info info) {
       // call ArkUIViewController declared in Compose
       auto controller = createHelloArkUIViewController(env);
       return reinterpret_cast<napi_value>(controller);
   }
   
   static napi_value Init(napi_env env, napi_value exports) {
       // init compose arkui
       androidx_compose_ui_arkui_init(env, exports);
       // register a createHelloArkUIViewController for ArkTS with napi
       napi_property_descriptor desc[] = {
           {"createHelloArkUIViewController", nullptr, CreateHelloArkUIViewController, nullptr, nullptr, nullptr, napi_default, nullptr}};
       napi_define_properties(env, exports, sizeof(desc) / sizeof(desc[0]), desc);
       return exports;
   }
   ```

   Define `createHelloArkUIViewController()` in ArkTS：

   ```typescript
   // entry/src/main/cpp/types/libentry/index.d.ets
   
   import { ArkUIViewController } from 'compose';
   
   // declare createHelloArkUIViewController
   export const createHelloArkUIViewController: () => ArkUIViewController
   ```

   Import Compose in ArkUI

   ```typescript
   import { common } from '@kit.AbilityKit';
   import { ArkUIViewController, Compose } from 'compose';
   import { createHelloArkUIViewController } from 'libentry.so';
   
   @Entry
   @Component
   struct ComposePage {
     private controller: ArkUIViewController = createHelloArkUIViewController()
   
     // onPageShow only in component with @Entry, invoked on page show
     onPageShow(): void {
       // notify controller onPageShow to handle lifecycle
       this.controller.onPageShow()
     }
   
     // onPageHide only in component with @Entry, invoked on page hide
     onPageHide(): void {
       // notify controller onPageHide to handle lifecycle
       this.controller.onPageHide()
     }
   
     // onBackPress only in component with @Entry
     onBackPress(): boolean | void {
       // propagate back event to controller
       return this.controller.onBackPress()
     }
   
     build() {
       Stack({ alignContent: Alignment.Center }) {
         Compose({
           controller: this.controller,
           libraryName: "entry",
           onBackPressed: () => {
             // handle the event not consumed by compose
             (getContext() as common.UIAbilityContext).windowStage.loadContent("pages/Index")
             return true
           }
         }).width('100%').height('100%')
       }
       .width('100%')
       .height('100%')
     }
   }
   ```

   Import Compose in ArkUI Navigation

   ```typescript
   import { ArkUIViewController, Compose } from 'compose';
   import { createHelloArkUIViewController } from 'libentry.so';
   
   @Component
   export struct ComposeDestination {
     private controller: ArkUIViewController = createHelloArkUIViewController()
     private navContext: NavDestinationContext | null = null;
   
     build() {
       NavDestination() {
         Stack({ alignContent: Alignment.Center }) {
           Compose({
             controller: this.controller,
             libraryName: "entry",
             onBackPressed: () => {
               // handle the event not consumed by compose
               this.navContext?.pathStack.pop()
               return true
             }
           })
         }
         .width('100%')
         .height('100%')
       }
       .onReady((navContext) => {
         this.navContext = navContext
       })
       // notify controller onPageShow
       .onShown(() => {
         this.controller.onPageShow()
       })
       // notify controller onPageHide
       .onHidden(() => {
         this.controller.onPageHide()
       })
       // propagate back event to controller
       .onBackPressed(() => this.controller.onBackPress())
     }
   }
   ```

   



### Some Issues

1. Error: `Ninja` not installed, occurring on compose-multiplatform-core build

   root source:

   we compile some C++ code in `ui-arkui` with harmony command tools based on `Ninja`

   ```bash
   CMake Error: CMake was unable to find a build program corresponding to "Ninja".  CMAKE_MAKE_PROGRAM is not set.  You probably need to select a different build tool.
   ```

   Fix:

   install `ninja` tool, use `brew` as follows for MacOS

   ```bash
   brew install ninja
   ```

### License
ovCompose-multiplatform-core is released under the Apache 2.0 License. For details, see: [License](License_ovCompose-multiplatform-core.txt)
