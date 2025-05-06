### 仓库说明

[compose-multiplatform]()：跨端 Compose 编译器插件源码

[compose-multiplatform-core ]()：跨端 Compose 源码

[compose-multiplatform-sample ]()：跨端 Compose 示例项目



### 编译发布

1. 修改 libraryversions.toml 文件中发布的 Compose 版本

   ```toml
   COMPOSE = "1.6.1-dev-18.0.1"
   ```

   

2. 发布跨端 Compose 到本地

   ```bash
   ./gradlew :mpp:publishComposeJbToMavenLocal -Pcompose.platforms=android,ohosArm64,uikit
   ```

   

3. 发布用于鸿蒙项目的 compose.har

   在安装 [DevEco-Studio](https://developer.huawei.com/consumer/cn/deveco-studio/) 的情况下，使用 DevEco-Studio 打开 `compose/ui/ui-arkui/src/ohosArm64Main/cpp/composeApp` 目录下的 composeApp 项目。使用以下命令打包 `compose.har` ，产物位于 `compose/build/outputs/default/compose.har`。

   ```bash
   # 执行 hvigor 打包 compose.har
   /Applications/DevEco-Studio.app/Contents/tools/hvigor/bin/hvigorw --mode module -p product -p module=compose assembleHar
   ```




### 使用说明

> 参考 compose-multiplatform-sample 项目

#### ArkUI 接入 Compose

1. 创建 Compose 跨端项目

   使用 [Android Studio](https://developer.android.com/studio) 创建 Kotlin 跨端项目，并使用腾讯视频开源的支持鸿蒙平台的 Kotlin 版本构建项目。

   `libs.version.toml` 参考如下

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

   `build.gradle.kts` 参考如下

   其中链接 skia 库中的 `libskia.so` 从 `compose-multiplatform-sample` 项目中获取。

   ```kotlin
   plugins {
       // 添加 kotlinMultiplatform jetbrainsCompose 和 composeCompiler 插件
       alias(libs.plugins.kotlinMultiplatform)
       alias(libs.plugins.jetbrainsCompose)
       alias(libs.plugins.composeCompiler)
   }
   
   kotlin {
       ohosArm64 {
           binaries.sharedLib {
               // 指定二进制产物名称
               baseName = "kn"
               // 链接 skia 库
               linkerOpts("-L${projectDir}/libs/", "-lskia")
               // 导出 compose.export
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
                   // 使用 api 方式依赖 compose.multiplatform.export 库，用于导出 C 函数
                   api(libs.compose.multiplatform.export)
               }
           }
       }
   }
   ```

   

2. 编写跨端 Compose 代码

   ```kotlin
   // 写在 commonMain 下，支持跨端复用
   @Composable
   internal fun Hello() {
       Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
           Text("Hello Compose!")
       }
   }
   
   // 写在 ohosArm64Main 下，创建接入鸿蒙 ArkUI 视图体系的 ArkUIViewController
   @CName("createHelloArkUIViewController")
   fun createHelloArkUIViewController(env: napi_env): napi_value =
       ComposeArkUIViewController(env) {
           Hello()
       }
   ```

   

3. 编译跨端 Compose 代码，生成接入鸿蒙平台的二进制产物

   执行跨端模块的 `linkDebugSharedOhosArm64 ` 或 `linkReleaseSharedOhosArm64` 任务，

   在该模块的 `build/bin/ohosArm64` 目录下将会生成 `libkn.so` 和 `libkn_api.h` 两个文件，这两个文件将被用于集成到鸿蒙项目中。

   

4. 创建鸿蒙 harmonyApp 项目

   - 创建项目

     在跨端项目下使用 [DevEco-Studio](https://developer.huawei.com/consumer/cn/deveco-studio/) 创建 harmonyApp 项目，在 Create Project 选择 "Native C++" 创建带有 Native 代码的项目。

   - 添加 Compose 跨端二进制产物

     将前步骤 3 中生成两个文件复制到 harmonyApp 项目下，其中：

     - `libkn.so` 复制到 `entry/libs/arm64-v8a/` 目录下。
     - `libkn_api.h` 复制到 `entry/src/main/cpp/include/` 目录下。

     为了简化这个步骤，我们可以在跨端 Compose 项目中创建一个 Gradle Task 执行这个复制任务。这样只需执行 `publishDebugBinariesToHarmonyApp` 或者 `publishReleaseBinariesToHarmonyApp` 即可编译 Compose 跨端代码并复制产物到鸿蒙项目。

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

   - 添加 `skikobridge.har` 和 `compose.har` 依赖

     - 将 `skikobridge.har` 复制到 `entry/libs/` 目录下，其中：`skikobridge.har` 可以从 `compose-multiplatform-sample/harmonyApp` 项目下获取。
     - 将 `compose.har` 复制到 `entry/libs` 目录下，其中 `compose.har` 是从 `compose-multiplatform-core/ui-arkui` 模块发布出来的，请参考文档中的编译发布板块里的第三部分内容。

   

5. 配置  harmonyApp 项目

   配置依赖：`entry/oh-package.json`

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
       // 添加 compose.har 依赖
       "compose": "file:./libs/compose.har",
       // 添加 skikobridge.har 依赖
       "skikobridge": "file:./libs/skikobridge.har"
     }
   }
   ```

   配置 CMake 编译：`entry/src/main/cpp/CMakeLists.txt`

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
   # 链接 libkn.so
   target_link_libraries(entry PUBLIC ${NATIVERENDER_ROOT_PATH}/../../../libs/arm64-v8a/libkn.so)
   # 链接 skikobridge 中的 skikobridge.so
   target_link_libraries(entry PUBLIC skikobridge::skikobridge)
   # 链接其他必要库
   target_link_libraries(entry PUBLIC ${EGL-lib} ${GLES-lib} ${hilog-lib} ${libace-lib} ${libnapi-lib} ${libuv-lib} libc++_shared.so)
   ```

   

6. 将 Compose 接入 harmonyApp 项目

   Compose ArkUI 初始化和 ArkUIViewController 的构建

   ```c++
   // entry/src/main/cpp/napi_init.cpp
   
   static napi_value CreateHelloArkUIViewController(napi_env env, napi_callback_info info) {
       // 调用 Compose 跨端项目构造 ArkUIViewController 的函数
       auto controller = createHelloArkUIViewController(env);
       return reinterpret_cast<napi_value>(controller);
   }
   
   static napi_value Init(napi_env env, napi_value exports) {
       // 初始化 compose arkui
       androidx_compose_ui_arkui_init(env, exports);
       // 使用 napi 注册一个 ArkTS 层的 createHelloArkUIViewController 函数
       napi_property_descriptor desc[] = {
           {"createHelloArkUIViewController", nullptr, CreateHelloArkUIViewController, nullptr, nullptr, nullptr, napi_default, nullptr}};
       napi_define_properties(env, exports, sizeof(desc) / sizeof(desc[0]), desc);
       return exports;
   }
   ```

   添加 `createHelloArkUIViewController()` 函数声明：

   ```typescript
   // entry/src/main/cpp/types/libentry/index.d.ets
   
   import { ArkUIViewController } from 'compose';
   
   // 声明 createHelloArkUIViewController 函数
   export const createHelloArkUIViewController: () => ArkUIViewController
   ```

   在 ArkUI 页面中接入 Compose

   ```typescript
   import { common } from '@kit.AbilityKit';
   import { ArkUIViewController, Compose } from 'compose';
   import { createHelloArkUIViewController } from 'libentry.so';
   
   @Entry
   @Component
   struct ComposePage {
     private controller: ArkUIViewController = createHelloArkUIViewController()
   
     // onPageShow 仅 @Entry 有效；页面每次显示时触发一次，包括路由过程、应用进入前台等场景
     onPageShow(): void {
       // 通知 controller onPageShow，用于处理生命周期
       this.controller.onPageShow()
     }
   
     // onPageHide 仅 @Entry 有效；页面每次隐藏时触发一次，包括路由过程、应用进入后台等场景
     onPageHide(): void {
       // 通知 controller onPageHide，用于处理生命周期
       this.controller.onPageHide()
     }
   
     // onBackPress 仅 @Entry 有效；
     onBackPress(): boolean | void {
       // 转发返回事件给 controller，由 controller 优先消费
       return this.controller.onBackPress()
     }
   
     build() {
       Stack({ alignContent: Alignment.Center }) {
         Compose({
           controller: this.controller,
           libraryName: "entry",
           onBackPressed: () => {
             // 未被 Compose 消费的返回事件，交由接入层兜底处理
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

   在 ArkUI 导航节点中接入 Compose

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
               // 未被 Compose 消费的返回事件，交由接入层兜底处理
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
       // 通知 controller onPageShow
       .onShown(() => {
         this.controller.onPageShow()
       })
       // 通知 controller onPageHide
       .onHidden(() => {
         this.controller.onPageHide()
       })
       // 转发返回事件给 controller，由 controller 优先消费
       .onBackPressed(() => this.controller.onBackPress())
     }
   }
   ```

   



### 错误排除

1. 在编译 compose-multipleform-core 可能会遇到未安装 `Ninja` 的错误

   问题原因：

   在编译 `ui-arkui` 模块时，会使用鸿蒙编译工具链混合编译 C++ 代码，编译 C++ 代码时，需要使用 `Ninja` 编译工具。

   ```bash
   CMake Error: CMake was unable to find a build program corresponding to "Ninja".  CMAKE_MAKE_PROGRAM is not set.  You probably need to select a different build tool.
   ```

   解决方案：

   安装 `ninja` 编译工具，MacOS 系统安装命令如下：

   ```bash
   brew install ninja
   ```

