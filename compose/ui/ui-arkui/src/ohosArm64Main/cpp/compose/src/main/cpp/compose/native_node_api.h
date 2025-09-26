#pragma once
#include <arkui/native_interface.h>
#include <arkui/native_node.h>

class NativeNodeApi {
 public:
  static ArkUI_NativeNodeAPI_1* getInstance();

 private:
  NativeNodeApi() {}

  static ArkUI_NativeNodeAPI_1* api;
};
