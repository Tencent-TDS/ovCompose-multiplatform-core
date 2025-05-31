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

#ifndef ANDROIDX_COMPOSE_UI_ARKUI_UTILS_ARKUIVIEWCONTROLLER_H
#define ANDROIDX_COMPOSE_UI_ARKUI_UTILS_ARKUIVIEWCONTROLLER_H

#include "napi/native_api.h"
#include <ace/xcomponent/native_interface_xcomponent.h>
#include <cstdint>
#include <string>

namespace androidx::compose::ui::arkui::utils {
typedef struct ArkUIViewController ArkUIViewController;

void ArkUIViewController_setId(ArkUIViewController *controller, const std::string &id);
std::string ArkUIViewController_getId(ArkUIViewController *controller);
void ArkUIViewController_setEnv(ArkUIViewController *controller, napi_env env);
void ArkUIViewController_setContext(ArkUIViewController *controller, napi_value context);
void ArkUIViewController_setUIContext(ArkUIViewController *controller, napi_value context);
void ArkUIViewController_setMessenger(ArkUIViewController *controller, napi_value messenger);
void ArkUIViewController_setXComponentRender(ArkUIViewController *controller, void *render);
void *ArkUIViewController_getXComponentRender(ArkUIViewController *controller);
void ArkUIViewController_setRootView(ArkUIViewController *controller, napi_value backRootView, napi_value foreRootView,
                                     napi_value touchableRootView);
void ArkUIViewController_aboutToAppear(ArkUIViewController *controller);
void ArkUIViewController_aboutToDisappear(ArkUIViewController *controller);
void ArkUIViewController_onPageShow(ArkUIViewController *controller);
void ArkUIViewController_onPageHide(ArkUIViewController *controller);
bool ArkUIViewController_onBackPress(ArkUIViewController *controller);
void ArkUIViewController_onSurfaceCreated(ArkUIViewController *controller, OH_NativeXComponent *component,
                                          uint64_t width, uint64_t height);
void ArkUIViewController_onSurfaceChanged(ArkUIViewController *controller, uint64_t width, uint64_t height);
void ArkUIViewController_onSurfaceShow(ArkUIViewController *controller);
void ArkUIViewController_onSurfaceHide(ArkUIViewController *controller);
void ArkUIViewController_onSurfaceDestroyed(ArkUIViewController *controller);
void ArkUIViewController_onFrame(ArkUIViewController *controller, uint64_t timestamp, uint64_t targetTimestamp);
void ArkUIViewController_onFocusEvent(ArkUIViewController *controller);
void ArkUIViewController_onKeyEvent(ArkUIViewController *controller);
void ArkUIViewController_dispatchTouchEvent(ArkUIViewController *controller, void *nativeTouchEvent,
                                            bool ignoreInteropView);
void ArkUIViewController_dispatchMouseEvent(ArkUIViewController *controller);
void ArkUIViewController_dispatchHoverEvent(ArkUIViewController *controller);
void ArkUIViewController_keyboardWillShow(ArkUIViewController *controller, double keyboardHeight);
void ArkUIViewController_keyboardWillHide(ArkUIViewController *controller);
int ArkUIViewController_requestSyncRefresh(ArkUIViewController *controller);
void ArkUIViewController_cancelSyncRefresh(ArkUIViewController *controller, uint64_t refreshId);
const char *ArkUIViewController_sendMessage(ArkUIViewController *controller, const std::string &type,
                                            const std::string &message);
void ArkUIViewController_onFinalize(ArkUIViewController *controller);
} // namespace androidx::compose::ui::arkui::utils
#endif