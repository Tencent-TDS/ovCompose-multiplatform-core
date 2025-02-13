#!/bin/bash

cd "$(dirname "$0")"

ANDROID_COMPILE_SDK=35
ANDROID_BUILD_TOOLS=35.0.0
ANDROID_NDK=27.0.12077973
CMAKE=3.22.1

downloadLinuxSDK() {
    curl -o commandlinetools-linux.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
    unzip -o -d fullsdk-linux commandlinetools-linux.zip
    rm commandlinetools-linux.zip
    echo y | "fullsdk-linux/cmdline-tools/bin/sdkmanager" --sdk_root=fullsdk-linux "platform-tools" "platforms;android-$ANDROID_COMPILE_SDK" "build-tools;$ANDROID_BUILD_TOOLS" "ndk;$ANDROID_NDK" "cmake;$CMAKE"
    echo y | "fullsdk-linux/cmdline-tools/bin/sdkmanager" --sdk_root=fullsdk-linux --licenses
}

downloadMacOsSDK() {
    curl -o commandlinetools-mac.zip https://dl.google.com/android/repository/commandlinetools-mac-11076708_latest.zip
    unzip -o -d fullsdk-darwin commandlinetools-mac.zip
    rm commandlinetools-mac.zip
    echo y | "fullsdk-darwin/cmdline-tools/bin/sdkmanager" --sdk_root=fullsdk-darwin "platform-tools" "platforms;android-$ANDROID_COMPILE_SDK" "build-tools;$ANDROID_BUILD_TOOLS" "ndk;$ANDROID_NDK" "cmake;$CMAKE"
    echo y | "fullsdk-darwin/cmdline-tools/bin/sdkmanager" --sdk_root=fullsdk-darwin --licenses
}

if [[ "$OSTYPE" == "linux-gnu"* ]]; then
    downloadLinuxSDK
elif [[ "$OSTYPE" == "darwin"* ]]; then
    downloadMacOsSDK
elif [[ "$OSTYPE" == "cygwin" ]]; then
    echo "Please download Android SDK manually (https://developer.android.com/studio)"
elif [[ "$OSTYPE" == "msys" ]]; then
    echo "Please download Android SDK manually (https://developer.android.com/studio)"
elif [[ "$OSTYPE" == "win32" ]]; then
    echo "Please download Android SDK manually (https://developer.android.com/studio)"
else
    echo "Unknown OS"
fi
