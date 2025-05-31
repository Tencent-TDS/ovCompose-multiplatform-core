<p align="center">
    <img alt="ovCompose Logo" src="img/ovCompose.svg" />
</p>

[English](README_ovCompose_en.md) | 简体中文


ovCompose（online-video-compose）是腾讯大前端领域Oteam中，腾讯视频团队基于 Compose Multiplatform 生态推出的跨平台开发框架，旨在弥补Jetbrains Compose Multiplatform不支持鸿蒙平台的遗憾与解决iOS平台混排受限的问题，便于业务构建全跨端App。

## 仓库说明

[compose-multiplatform](https://github.com/Tencent-TDS/KuiklyBase-platform/tree/main/compose-multiplatform)：跨端 Compose 编译器插件源码

[compose-multiplatform-core ](https://github.com/Tencent-TDS/ovCompose-multiplatform-core)：跨端 Compose 源码

[ovCompose-sample ](https://github.com/Tencent-TDS/ovCompose-sample)：跨端 Compose 示例项目



## 编译发布

1. 修改 libraryversions.toml 文件中发布的 Compose 版本

   ```toml
   COMPOSE = "1.6.1-dev-xxx"
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
   
## [开始使用ovCompose](https://github.com/Tencent-TDS/ovCompose-sample)

> 详情请参见 [ovCompose-sample](https://github.com/Tencent-TDS/ovCompose-sample)


## 错误排除

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

## License
ovCompose-multiplatform-core 基于 Apache 2.0 协议发布，详见：[License](License_ovCompose-multiplatform-core.txt)