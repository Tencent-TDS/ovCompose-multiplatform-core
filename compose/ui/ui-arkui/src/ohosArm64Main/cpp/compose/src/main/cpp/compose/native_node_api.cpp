#include "native_node_api.h"

ArkUI_NativeNodeAPI_1* NativeNodeApi::getInstance() {
  static ArkUI_NativeNodeAPI_1* INSTANCE = nullptr;
  if (INSTANCE == nullptr) {
    OH_ArkUI_GetModuleInterface(
        ARKUI_NATIVE_NODE, ArkUI_NativeNodeAPI_1, INSTANCE);
  }
  return INSTANCE;
}