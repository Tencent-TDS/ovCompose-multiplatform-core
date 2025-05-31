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

#include "xcomponent_render.h"
#include "arkui_view_controller.h"
#include "xcomponent_constant.h"
#include "xcomponent_holder.h"
#include "xcomponent_log.h"
#include "xcomponent_utils.h"
#include <GLES3/gl3.h>

using namespace androidx::compose::ui::arkui::utils::XComponentUtils;

namespace androidx::compose::ui::arkui::utils {

static void OnFrameCallbackCB(OH_NativeXComponent *component, uint64_t timestamp, uint64_t targetTimestamp) {
    auto controller = XComponentHolder::GetInstance()->GetArkUIViewController(component);
    if (controller == nullptr) {
        LOGE("XComponentRender: OnFrameCallbackCB: controller is null");
        return;
    }
    ArkUIViewController_onFrame(controller, timestamp, targetTimestamp);
};

static void OnSurfaceCreatedCB(OH_NativeXComponent *component, void *window) {
    LOGI("XComponentRender: OnSurfaceCreatedCB");
    OH_NativeXComponent_RegisterOnFrameCallback(component, OnFrameCallbackCB);

    if (component == nullptr || window == nullptr) {
        LOGE("XComponentRender: OnSurfaceCreatedCB: component or window is null");
        return;
    }

    auto render = XComponentHolder::GetInstance()->GetXComponentRender(component);
    if (render == nullptr) {
        LOGE("XComponentRender: OnSurfaceCreatedCB: render is null");
        return;
    }
    render->EglInit(window);

    auto controller = render->controller;
    if (controller == nullptr) {
        LOGE("XComponentRender: OnSurfaceCreatedCB: controller is null");
        return;
    }
    uint64_t width;
    uint64_t height;
    int32_t result = OH_NativeXComponent_GetXComponentSize(component, window, &width, &height);

    if (result != OH_NATIVEXCOMPONENT_RESULT_SUCCESS) {
        LOGE("XComponentRender: OnSurfaceCreatedCB: unable to get component size");
        return;
    }
    ArkUIViewController_onSurfaceCreated(controller, component, width, height);
}
static void OnSurfaceChangedCB(OH_NativeXComponent *component, void *window) {
    LOGI("XComponentRender: OnSurfaceChangedCB");
    if (component == nullptr || window == nullptr) {
        LOGE("XComponentRender: OnSurfaceChangedCB: component or window is null");
        return;
    }

    uint64_t width;
    uint64_t height;
    int32_t result = OH_NativeXComponent_GetXComponentSize(component, window, &width, &height);
    if (result != OH_NATIVEXCOMPONENT_RESULT_SUCCESS) {
        LOGE("XComponentRender: OnSurfaceChangedCB: unable to get component size");
        return;
    }

    auto controller = XComponentHolder::GetInstance()->GetArkUIViewController(component);
    if (controller == nullptr) {
        LOGE("XComponentRender: OnSurfaceChangedCB: controller is null");
        return;
    }
    ArkUIViewController_onSurfaceChanged(controller, width, height);
}

static void OnSurfaceShowCB(OH_NativeXComponent *component, void *window) {
    LOGI("XComponentRender: OnSurfaceShowCB");
    auto controller = XComponentHolder::GetInstance()->GetArkUIViewController(component);
    ArkUIViewController_onSurfaceShow(controller);
}

static void OnSurfaceHideCB(OH_NativeXComponent *component, void *window) {
    LOGI("XComponentRender: OnSurfaceHideCB");
    auto controller = XComponentHolder::GetInstance()->GetArkUIViewController(component);
    ArkUIViewController_onSurfaceHide(controller);
}

static void OnSurfaceDestroyedCB(OH_NativeXComponent *component, void *window) {
    LOGI("XComponentRender: OnSurfaceDestroyedCB");
    OH_NativeXComponent_UnregisterOnFrameCallback(component);

    auto controller = XComponentHolder::GetInstance()->GetArkUIViewController(component);
    ArkUIViewController_onSurfaceDestroyed(controller);

    auto render = XComponentHolder::GetInstance()->GetXComponentRender(component);
    // render is nullable, but it has no effect to delete a nullptr.
    delete render;
}

// 目前事件是从 ArkView 层分发的
static void DispatchTouchEventCB(OH_NativeXComponent *component, void *window) {}

static void OnFocusEventCB(OH_NativeXComponent *component, void *window) {
    LOGI("XComponentRender: OnFocusEventCB");
    auto controller = XComponentHolder::GetInstance()->GetArkUIViewController(component);
    ArkUIViewController_onFocusEvent(controller);
}

static void OnKeyEventCB(OH_NativeXComponent *component, void *window) {
    LOGI("XComponentRender: OnKeyEventCB");
    auto controller = XComponentHolder::GetInstance()->GetArkUIViewController(component);
    ArkUIViewController_onKeyEvent(controller);
}

static void DispatchMouseEventCB(OH_NativeXComponent *component, void *window) {
    LOGI("XComponentRender: DispatchMouseEventCB");
    auto controller = XComponentHolder::GetInstance()->GetArkUIViewController(component);
    ArkUIViewController_dispatchMouseEvent(controller);
}
static void DispatchHoverEventCB(OH_NativeXComponent *component, bool isHover) {
    LOGI("XComponentRender: DispatchHoverEventCB");
    auto controller = XComponentHolder::GetInstance()->GetArkUIViewController(component);
    ArkUIViewController_dispatchHoverEvent(controller);
}

XComponentRender::XComponentRender(OH_NativeXComponent *nativeXComponent)
    : id(XComponentUtils::GetXComponentId(nativeXComponent)), component(nativeXComponent),
      callback{.OnSurfaceCreated = OnSurfaceCreatedCB,
               .OnSurfaceChanged = OnSurfaceChangedCB,
               .OnSurfaceDestroyed = OnSurfaceDestroyedCB,
               .DispatchTouchEvent = DispatchTouchEventCB},
      mouseCallback{.DispatchMouseEvent = DispatchMouseEventCB, .DispatchHoverEvent = DispatchHoverEventCB} {

    OH_NativeXComponent_RegisterCallback(this->component, &callback);
    OH_NativeXComponent_RegisterSurfaceShowCallback(this->component, OnSurfaceShowCB);
    OH_NativeXComponent_RegisterSurfaceHideCallback(this->component, OnSurfaceHideCB);
    OH_NativeXComponent_RegisterFocusEventCallback(this->component, OnFocusEventCB);
    OH_NativeXComponent_RegisterKeyEventCallback(this->component, OnKeyEventCB);
    OH_NativeXComponent_RegisterMouseEventCallback(this->component, &mouseCallback);
}

// OH_NativeXComponent is managed by js engine, should be destroyed by js gc.
// ArkUIViewController is managed by js engine, should be destroyed by js gc.
XComponentRender::~XComponentRender() {
    auto controller = this->controller;
    auto controllerRender = ArkUIViewController_getXComponentRender(controller);
    if (controller != nullptr && controllerRender != nullptr && controllerRender == this) {
        // 解除赋值，重置为 nullptr
        ArkUIViewController_setXComponentRender(controller, nullptr);
    }
    XComponentHolder::GetInstance()->RemoveXComponentRender(this->id);
    releaseEGL();
};

void XComponentRender::releaseEGL() {
    if (eglDisplay) {
        if (eglSurface) {
            eglDestroySurface(eglDisplay, eglSurface);
        }
        if (eglContext) {
            eglDestroyContext(eglDisplay, eglContext);
        }

        eglTerminate(eglDisplay);
        eglDisplay = nullptr;
        eglSurface = nullptr;
        eglContext = nullptr;
    }
}

bool XComponentRender::EglInit(void *window) {
    eglWindow = reinterpret_cast<EGLNativeWindowType>(window);
    if (eglWindow == 0) {
        LOGE("XComponentRender: EglInit: eglWindow is null");
        return false;
    }

    eglDisplay = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    if (eglDisplay == EGL_NO_DISPLAY) {
        LOGE("XComponentRender: EglInit: unable to get EGL display");
        return false;
    }

    EGLint majorVersion;
    EGLint minorVersion;
    if (!eglInitialize(eglDisplay, &majorVersion, &minorVersion)) {
        LOGE("XComponentRender: EglInit: unable to initialize EGL display");
        return false;
    }

    const EGLint configSize = 1;
    EGLint numConfig;
    if (!eglChooseConfig(eglDisplay, ATTRIB_LIST, &eglConfig, configSize, &numConfig)) {
        LOGE("XComponentRender: EglInit: unable to choose config");
        return false;
    }

    eglSurface = eglCreateWindowSurface(eglDisplay, eglConfig, eglWindow, NULL);
    if (eglSurface == nullptr) {
        LOGE("XComponentRender: EglInit: unable to create window surface");
        return false;
    }

    eglContext = eglCreateContext(eglDisplay, eglConfig, EGL_NO_CONTEXT, CONTEXT_ATTRIBS);
    if (eglContext == nullptr) {
        LOGE("XComponentRender: EglInit: unable to create context");
        return false;
    }
    return true;
}

bool XComponentRender::EglPrepareDraw() const {
    if (eglDisplay == nullptr || eglSurface == nullptr || eglContext == nullptr) {
        LOGE("XComponentRender: EglPrepareDraw: eglDisplay or eglSurface or eglContext is null");
        return false;
    }
    if (!eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
        LOGE("XComponentRender: EglPrepareDraw: unable to mak current");
        return false;
    }
    return true;
}
bool XComponentRender::EglFinishDraw() const {
    if (eglDisplay == nullptr || eglSurface == nullptr) {
        LOGE("XComponentRender: EglFinishDraw: eglDisplay or eglSurface is null");
        return false;
    }
    eglSwapBuffers(eglDisplay, eglSurface);
    return true;
}

void XComponentRender::RegisterFrameCallback() {
    OH_NativeXComponent_RegisterOnFrameCallback(component, OnFrameCallbackCB);
}

void XComponentRender::UnregisterFrameCallback() { OH_NativeXComponent_UnregisterOnFrameCallback(component); }

} // namespace androidx::compose::ui::arkui::utils