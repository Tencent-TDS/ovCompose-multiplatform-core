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

#ifndef ANDROIDX_COMPOSE_UI_ARKUI_UTILS_XCOMPONENTRENDER_H
#define ANDROIDX_COMPOSE_UI_ARKUI_UTILS_XCOMPONENTRENDER_H

#include <EGL/egl.h>
#include <EGL/eglext.h>
#include <ace/xcomponent/native_interface_xcomponent.h>
#include <string>

typedef void (*OH_NativeXComponent_OnFrameCallback)(OH_NativeXComponent *component, uint64_t timestamp,
                                                    uint64_t targetTimestamp);

namespace androidx::compose::ui::arkui::utils {
typedef struct ArkUIViewController ArkUIViewController;
class XComponentRender {
public:
    explicit XComponentRender(OH_NativeXComponent *nativeXComponent);
    ~XComponentRender();

    bool EglInit(void *window);
    bool EglPrepareDraw() const;
    bool EglFinishDraw() const;

    void RegisterFrameCallback();
    void UnregisterFrameCallback();

public:
    std::string const id;
    OH_NativeXComponent *const component;
    ArkUIViewController *controller;

private:
    EGLNativeWindowType eglWindow;
    EGLDisplay eglDisplay = EGL_NO_DISPLAY;
    EGLSurface eglSurface = EGL_NO_SURFACE;
    EGLContext eglContext = EGL_NO_CONTEXT;
    EGLConfig eglConfig = EGL_NO_CONFIG_KHR;
    OH_NativeXComponent_Callback callback;
    OH_NativeXComponent_MouseEvent_Callback mouseCallback;

    void releaseEGL();
};
} // namespace androidx::compose::ui::arkui::utils

#endif