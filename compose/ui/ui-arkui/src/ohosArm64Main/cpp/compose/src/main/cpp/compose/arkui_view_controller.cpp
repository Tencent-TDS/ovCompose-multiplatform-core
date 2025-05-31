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

#include "arkui_view_controller.h"
#include "libkn_api.h"
#include "xcomponent_log.h"
#include "xcomponent_render.h"

namespace androidx::compose::ui::arkui::utils {
void ArkUIViewController_setId(ArkUIViewController *controller, const std::string &id) {
    if (controller == nullptr) {
        LOGE("ArkUIViewController_setId: controller is null");
        return;
    }
    const char *idStr = id.c_str();
    androidx_compose_ui_arkui_ArkUIViewController_setId(controller, idStr);
}

std::string ArkUIViewController_getId(ArkUIViewController *controller) {
    if (controller == nullptr) {
        LOGE("ArkUIViewController_getId: controller is null");
        return "";
    }
    auto id = androidx_compose_ui_arkui_ArkUIViewController_getId(controller);
    if (id == nullptr) {
        LOGE("ArkUIViewController_getId: id is null");
        return "";
    }
    return std::string(id);
}

void ArkUIViewController_setEnv(ArkUIViewController *controller, napi_env env) {
    if (controller == nullptr || env == nullptr) {
        LOGE("ArkUIViewController_setEnv: controller or env is null");
        return;
    }
    androidx_compose_ui_arkui_ArkUIViewController_setEnv(controller, env);
}

void ArkUIViewController_setContext(ArkUIViewController *controller, napi_value context) {
    if (controller == nullptr || context == nullptr) {
        LOGE("ArkUIViewController_setContext: controller or context is null");
        return;
    }
    androidx_compose_ui_arkui_ArkUIViewController_setContext(controller, context);
}

void ArkUIViewController_setUIContext(ArkUIViewController *controller, napi_value uiContext) {
    if (controller == nullptr || uiContext == nullptr) {
        LOGE("ArkUIViewController_setUIContext: controller or context is null");
        return;
    }
    androidx_compose_ui_arkui_ArkUIViewController_setUIContext(controller, uiContext);
}

void ArkUIViewController_setMessenger(ArkUIViewController *controller, napi_value messenger) {
    if (controller == nullptr || messenger == nullptr) {
        LOGE("ArkUIViewController_setUIContext: controller or context is null");
        return;
    }
    androidx_compose_ui_arkui_ArkUIViewController_setMessenger(controller, messenger);
}

void ArkUIViewController_setXComponentRender(ArkUIViewController *controller, void *render) {
    if (controller == nullptr) {
        LOGE("ArkUIViewController_setXComponentRender: controller is null");
        return;
    }
    androidx_compose_ui_arkui_ArkUIViewController_setXComponentRender(controller, render);
}

void *ArkUIViewController_getXComponentRender(ArkUIViewController *controller) {
    if (controller == nullptr) {
        LOGE("ArkUIViewController_getXComponentRender: controller is null");
        return nullptr;
    }
    return androidx_compose_ui_arkui_ArkUIViewController_getXComponentRender(controller);
}

void ArkUIViewController_setRootView(ArkUIViewController *controller, napi_value backRootView, napi_value foreRootView,
                                     napi_value touchableRootView) {
    if (controller == nullptr) {
        LOGE("ArkUIViewController_setRootView: controller is null");
        return;
    }

    if (backRootView == nullptr) {
        LOGE("ArkUIViewController_setRootView: backRootView is null");
        return;
    }

    if (foreRootView == nullptr) {
        LOGE("ArkUIViewController_setRootView: foreRootView is null");
        return;
    }

    if (touchableRootView == nullptr) {
        LOGE("ArkUIViewController_setRootView: touchableRootView is null");
        return;
    }

    androidx_compose_ui_arkui_ArkUIViewController_setRootView(controller, backRootView, foreRootView,
                                                              touchableRootView);
}

void ArkUIViewController_aboutToAppear(ArkUIViewController *controller) {
    if (controller == nullptr) {
        LOGE("ArkUIViewController_aboutToAppear: controller is null");
        return;
    }
    androidx_compose_ui_arkui_ArkUIViewController_aboutToAppear(controller);
}

void ArkUIViewController_aboutToDisappear(ArkUIViewController *controller) {
    if (controller == nullptr) {
        LOGE("ArkUIViewController_aboutToDisappear: controller is null");
        return;
    }
    androidx_compose_ui_arkui_ArkUIViewController_aboutToDisappear(controller);
}

void ArkUIViewController_onPageShow(ArkUIViewController *controller) {
    if (controller == nullptr) {
        LOGE("ArkUIViewController_onPageShow: controller is null");
        return;
    }
    androidx_compose_ui_arkui_ArkUIViewController_onPageShow(controller);
}
void ArkUIViewController_onPageHide(ArkUIViewController *controller) {
    if (controller == nullptr) {
        LOGE("ArkUIViewController_onPageHide: controller is null");
        return;
    }
    androidx_compose_ui_arkui_ArkUIViewController_onPageHide(controller);
}
bool ArkUIViewController_onBackPress(ArkUIViewController *controller) {
    if (controller == nullptr) {
        LOGE("ArkUIViewController_onBackPress: controller is null");
        return false;
    }
    return androidx_compose_ui_arkui_ArkUIViewController_onBackPress(controller);
}

void ArkUIViewController_onSurfaceCreated(ArkUIViewController *controller, OH_NativeXComponent *component,
                                          uint64_t width, uint64_t height) {
    if (controller == nullptr) {
        LOGE("ArkUIViewController_onSurfaceCreated: controller is null");
        return;
    }
    int intWidth = static_cast<int>(width);
    int intHeight = static_cast<int>(height);
    androidx_compose_ui_arkui_ArkUIViewController_onSurfaceCreated(controller, component, intWidth, intHeight);
}

void ArkUIViewController_onSurfaceChanged(ArkUIViewController *controller, uint64_t width, uint64_t height) {
    if (controller == nullptr) {
        LOGE("ArkUIViewController_onSurfaceChanged: controller is null");
        return;
    }
    int intWidth = static_cast<int>(width);
    int intHeight = static_cast<int>(height);
    androidx_compose_ui_arkui_ArkUIViewController_onSurfaceChanged(controller, intWidth, intHeight);
}

void ArkUIViewController_onSurfaceShow(ArkUIViewController *controller) {
    if (controller == nullptr) {
        LOGE("ArkUIViewController_onSurfaceShow: controller is null");
        return;
    }
    androidx_compose_ui_arkui_ArkUIViewController_onSurfaceShow(controller);
}

void ArkUIViewController_onSurfaceHide(ArkUIViewController *controller) {
    if (controller == nullptr) {
        LOGE("ArkUIViewController_onSurfaceHide: controller is null");
        return;
    }
    androidx_compose_ui_arkui_ArkUIViewController_onSurfaceHide(controller);
}

void ArkUIViewController_onSurfaceDestroyed(ArkUIViewController *controller) {
    if (controller == nullptr) {
        LOGE("ArkUIViewController_onSurfaceDestroyed: controller is null");
        return;
    }
    androidx_compose_ui_arkui_ArkUIViewController_onSurfaceDestroyed(controller);
}

void ArkUIViewController_onFrame(ArkUIViewController *controller, uint64_t timestamp, uint64_t targetTimestamp) {
    if (controller == nullptr) {
        LOGE("ArkUIViewController_onFrame: controller is null");
        return;
    }
    long long longTimestamp = static_cast<long long>(timestamp);
    long long longTargetTimestamp = static_cast<long long>(targetTimestamp);
    androidx_compose_ui_arkui_ArkUIViewController_onFrame(controller, longTimestamp, longTargetTimestamp);
}

void ArkUIViewController_onFocusEvent(ArkUIViewController *controller) {
    if (controller == nullptr) {
        LOGE("ArkUIViewController_onFocusEvent: controller is null");
        return;
    }
    androidx_compose_ui_arkui_ArkUIViewController_onFocusEvent(controller);
}

void ArkUIViewController_onKeyEvent(ArkUIViewController *controller) {
    if (controller == nullptr) {
        LOGE("ArkUIViewController_onKeyEvent: controller is null");
        return;
    }
    androidx_compose_ui_arkui_ArkUIViewController_onKeyEvent(controller);
}

void ArkUIViewController_dispatchTouchEvent(ArkUIViewController *controller, void *nativeTouchEvent,
                                            bool ignoreInteropView) {
    if (controller == nullptr) {
        LOGE("ArkUIViewController_dispatchTouchEvent: controller is null");
        return;
    }
    androidx_compose_ui_arkui_ArkUIViewController_dispatchTouchEvent(controller, nativeTouchEvent, ignoreInteropView);
}

void ArkUIViewController_dispatchMouseEvent(ArkUIViewController *controller) {
    if (controller == nullptr) {
        LOGE("ArkUIViewController_dispatchMouseEvent: controller is null");
        return;
    }
    androidx_compose_ui_arkui_ArkUIViewController_dispatchMouseEvent(controller);
}

void ArkUIViewController_dispatchHoverEvent(ArkUIViewController *controller) {
    if (controller == nullptr) {
        LOGE("ArkUIViewController_dispatchHoverEvent: controller is null");
        return;
    }
    androidx_compose_ui_arkui_ArkUIViewController_dispatchHoverEvent(controller);
}

void ArkUIViewController_keyboardWillShow(ArkUIViewController *controller, double keyboardHeight) {
    if (controller == nullptr) {
        LOGE("ArkUIViewController_keyboardWillShow: controller is null");
        return;
    }
    androidx_compose_ui_arkui_ArkUIViewController_keyboardWillShow(controller, keyboardHeight);
}

void ArkUIViewController_keyboardWillHide(ArkUIViewController *controller) {
    if (controller == nullptr) {
        LOGE("ArkUIViewController_keyboardWillHide: controller is null");
        return;
    }
    androidx_compose_ui_arkui_ArkUIViewController_keyboardWillHide(controller);
}

int ArkUIViewController_requestSyncRefresh(ArkUIViewController *controller) {
    if (controller == nullptr) {
        LOGE("ArkUIViewController_requestSyncRefresh: controller is null");
        return -1;
    }
    return androidx_compose_ui_arkui_ArkUIViewController_requestSyncRefresh(controller);
}

void ArkUIViewController_cancelSyncRefresh(ArkUIViewController *controller, uint64_t refreshId) {
    if (controller == nullptr) {
        LOGE("ArkUIViewController_cancelSyncRefresh: controller is null");
        return;
    }
    androidx_compose_ui_arkui_ArkUIViewController_cancelSyncRefresh(controller, refreshId);
}

const char *ArkUIViewController_sendMessage(ArkUIViewController *controller, const std::string &type,
                                            const std::string &message) {
    return androidx_compose_ui_arkui_ArkUIViewController_sendMessage(controller, type.c_str(), message.c_str());
}

void ArkUIViewController_onFinalize(ArkUIViewController *controller) {
    if (controller == nullptr) {
        LOGE("ArkUIViewController_onFinalize: controller is null");
        return;
    }
    androidx_compose_ui_arkui_ArkUIViewController_onFinalize(controller);
}
} // namespace androidx::compose::ui::arkui::utils
