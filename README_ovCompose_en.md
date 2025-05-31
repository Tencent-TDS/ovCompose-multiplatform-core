<p align="center">
    <img alt="ovCompose Logo" src="img/ovCompose.svg" />
</p>

English | [简体中文](README_ovCompose_zh.md)


ovCompose (online-video-compose) is a cross-platform development framework launched by the Tencent Video team within Oteam, the leading frontend group at Tencent. It is based on the Compose Multiplatform ecosystem and aims to address the limitations of Jetbrains Compose Multiplatform, specifically its lack of support for the HarmonyOS platform and the constraints on mixed layout rendering on iOS. ovCompose makes it easier for businesses to build fully cross-platform apps.

## Compose Repositories

[compose-multiplatform](https://github.com/Tencent-TDS/KuiklyBase-platform/tree/main/compose-multiplatform): Plugins for compose multiplatform with compose-gradle-plugin, resources, ui-tooling-preview.

[compose-multiplatform-core](https://github.com/Tencent-TDS/ovCompose-multiplatform-core): The source core of multiplatform compose with material, foundation, ui, runtime.

[ovCompose-sample](https://github.com/Tencent-TDS/ovCompose-sample): Sample of multiplatform compose about ui, layout, gesture.



## Compile and Publish

1. Replace the `Compose` value in `libraryversions.toml` with your own version.

   ```toml
   COMPOSE = "1.6.1-dev-xxx"
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

## [Get Started with ovCompose](https://github.com/Tencent-TDS/ovCompose-sample)

> see [ovCompose-sample](https://github.com/Tencent-TDS/ovCompose-sample) for details

## Some Issues

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

## License
ovCompose-multiplatform-core is released under the Apache 2.0 License. For details, see: [License](License_ovCompose-multiplatform-core.txt)
