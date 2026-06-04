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

#ifndef KONAN_LIB_COMPOSE_ARK_UI_UTILS_H
#define KONAN_LIB_COMPOSE_ARK_UI_UTILS_H
#ifdef __cplusplus
extern "C" {
#endif
#ifdef __cplusplus
typedef bool            KBoolean;
#else
typedef _Bool           KBoolean;
#endif
typedef long long          KLong;
typedef float              KFloat;
typedef double             KDouble;

extern void androidx_compose_ui_arkui_ArkUIViewController_aboutToAppear(void* controllerRef);
extern void androidx_compose_ui_arkui_ArkUIViewController_aboutToDisappear(void* controllerRef);
extern void androidx_compose_ui_arkui_ArkUIViewController_cancelSyncRefresh(void* controllerRef, int refreshId);
extern void androidx_compose_ui_arkui_ArkUIViewController_dispatchHoverEvent(void* controllerRef);
extern void androidx_compose_ui_arkui_ArkUIViewController_dispatchMouseEvent(void* controllerRef);
extern KBoolean androidx_compose_ui_arkui_ArkUIViewController_dispatchTouchEvent(void* controllerRef, void* nativeTouchEvent, KBoolean ignoreInteropView);
extern const char* androidx_compose_ui_arkui_ArkUIViewController_getId(void* controllerRef);
extern void* androidx_compose_ui_arkui_ArkUIViewController_getXComponentRender(void* controllerRef);
extern void androidx_compose_ui_arkui_ArkUIViewController_keyboardWillHide(void* controllerRef);
extern void androidx_compose_ui_arkui_ArkUIViewController_keyboardWillShow(void* controllerRef, KFloat keyboardHeight);
extern void androidx_compose_ui_arkui_ArkUIViewController_updateDensity(void* controllerRef, KFloat density);
extern KBoolean androidx_compose_ui_arkui_ArkUIViewController_onBackPress(void* controllerRef);
extern void androidx_compose_ui_arkui_ArkUIViewController_onFinalize(void* controllerRef);
extern void androidx_compose_ui_arkui_ArkUIViewController_onFocusEvent(void* controllerRef);
extern void androidx_compose_ui_arkui_ArkUIViewController_onFrame(void* controllerRef, KLong timestamp, KLong targetTimestamp);
extern KBoolean androidx_compose_ui_arkui_ArkUIViewController_onKeyEvent(void* controllerRef, void* event);
extern void androidx_compose_ui_arkui_ArkUIViewController_onAxisEvent(void* controllerRef, void* event);
extern void androidx_compose_ui_arkui_ArkUIViewController_onPinchEvent(void* controllerRef, void* event);
extern void androidx_compose_ui_arkui_ArkUIViewController_onMouseEvent(void* controllerRef, void* event);
extern void androidx_compose_ui_arkui_ArkUIViewController_onIdle(void* controllerRef, KLong timeLeft);
extern void androidx_compose_ui_arkui_ArkUIViewController_draw(void* controllerRef, void* canvas);
extern void* androidx_compose_ui_arkui_ArkUIViewController_getJsNode(void* controllerRef);
extern void androidx_compose_ui_arkui_ArkUIViewController_onPageHide(void* controllerRef);
extern void androidx_compose_ui_arkui_ArkUIViewController_onPageShow(void* controllerRef);
extern void androidx_compose_ui_arkui_ArkUIViewController_onSurfaceChanged(void* controllerRef, int width, int height);
extern void androidx_compose_ui_arkui_ArkUIViewController_onSurfaceCreated(void* controllerRef, void* xcomponentPtr, int width, int height);
extern void androidx_compose_ui_arkui_ArkUIViewController_onSurfaceDestroyed(void* controllerRef);
extern void androidx_compose_ui_arkui_ArkUIViewController_onSurfaceHide(void* controllerRef);
extern void androidx_compose_ui_arkui_ArkUIViewController_onSurfaceShow(void* controllerRef);
extern int androidx_compose_ui_arkui_ArkUIViewController_requestSyncRefresh(void* controllerRef);
extern const char* androidx_compose_ui_arkui_ArkUIViewController_sendMessage(void* controllerRef, const char* type, const char* message);
extern void androidx_compose_ui_arkui_ArkUIViewController_setContext(void* controllerRef, void* context);
extern void androidx_compose_ui_arkui_ArkUIViewController_setEnv(void* controllerRef, void* env);
extern void androidx_compose_ui_arkui_ArkUIViewController_setId(void* controllerRef, const char* id);
extern void androidx_compose_ui_arkui_ArkUIViewController_setMessenger(void* controllerRef, void* messenger);
extern void androidx_compose_ui_arkui_ArkUIViewController_setRootView(void* controllerRef, void* backRootView, void* foreRootView, void* touchableRootView);
extern void androidx_compose_ui_arkui_ArkUIViewController_setNativeInteropContainer(void* controllerRef, void* backContainer, void* foreContainer, void* touchableContainer);
extern void androidx_compose_ui_arkui_ArkUIViewController_initRenderContext(void* controllerRef, KBoolean enableCApi, void* importFrameMgr, void* rootContent);
extern void androidx_compose_ui_arkui_ArkUIViewController_setUIContext(void* controllerRef, void* uiContext);
extern void androidx_compose_ui_arkui_ArkUIViewController_setXComponentRender(void* controllerRef, void* render);
extern void androidx_compose_ui_arkui_init(void* env, void* exports);
extern void androidx_compose_ui_arkui_ArkUIViewController_setExtraParam(void* controllerRef, const char* key, const char* value);
extern void initJsRenderNodeContext(void* env, void* nodeConstructor, void* statusModifyConstructor, KDouble ratio, KBoolean fixed);
extern void jsNodeDraw(void* canvas, void* node);
extern void* mapFrameNodePos(int rootNodeId, int extNodeId, double touchX, double touchY);
#ifdef __cplusplus
}  /* extern "C" */
#endif
#endif  /* KONAN_LIB_COMPOSE_ARK_UI_UTILS_H */
