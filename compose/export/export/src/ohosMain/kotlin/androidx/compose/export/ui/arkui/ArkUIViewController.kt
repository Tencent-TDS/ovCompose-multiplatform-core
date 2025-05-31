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

@file:Suppress("FunctionName")

package androidx.compose.export.ui.arkui

import androidx.compose.export.annotation.InternalExportApi
import androidx.compose.ui.arkui._ArkUIViewController_aboutToAppear
import androidx.compose.ui.arkui._ArkUIViewController_aboutToDisappear
import androidx.compose.ui.arkui._ArkUIViewController_cancelSyncRefresh
import androidx.compose.ui.arkui._ArkUIViewController_dispatchHoverEvent
import androidx.compose.ui.arkui._ArkUIViewController_dispatchMouseEvent
import androidx.compose.ui.arkui._ArkUIViewController_dispatchTouchEvent
import androidx.compose.ui.arkui._ArkUIViewController_getId
import androidx.compose.ui.arkui._ArkUIViewController_getXComponentRender
import androidx.compose.ui.arkui._ArkUIViewController_keyboardWillHide
import androidx.compose.ui.arkui._ArkUIViewController_keyboardWillShow
import androidx.compose.ui.arkui._ArkUIViewController_onBackPress
import androidx.compose.ui.arkui._ArkUIViewController_onFinalize
import androidx.compose.ui.arkui._ArkUIViewController_onFocusEvent
import androidx.compose.ui.arkui._ArkUIViewController_onFrame
import androidx.compose.ui.arkui._ArkUIViewController_onKeyEvent
import androidx.compose.ui.arkui._ArkUIViewController_onPageHide
import androidx.compose.ui.arkui._ArkUIViewController_onPageShow
import androidx.compose.ui.arkui._ArkUIViewController_onSurfaceChanged
import androidx.compose.ui.arkui._ArkUIViewController_onSurfaceCreated
import androidx.compose.ui.arkui._ArkUIViewController_onSurfaceDestroyed
import androidx.compose.ui.arkui._ArkUIViewController_onSurfaceHide
import androidx.compose.ui.arkui._ArkUIViewController_onSurfaceShow
import androidx.compose.ui.arkui._ArkUIViewController_requestSyncRefresh
import androidx.compose.ui.arkui._ArkUIViewController_sendMessage
import androidx.compose.ui.arkui._ArkUIViewController_setContext
import androidx.compose.ui.arkui._ArkUIViewController_setEnv
import androidx.compose.ui.arkui._ArkUIViewController_setId
import androidx.compose.ui.arkui._ArkUIViewController_setMessenger
import androidx.compose.ui.arkui._ArkUIViewController_setRootView
import androidx.compose.ui.arkui._ArkUIViewController_setUIContext
import androidx.compose.ui.arkui._ArkUIViewController_setXComponentRender
import kotlinx.cinterop.COpaquePointer
import platform.ohos.napi_env
import platform.ohos.napi_value

@InternalExportApi
@CName("androidx_compose_ui_arkui_ArkUIViewController_setId")
fun _Export_ArkUIViewController_setId(controllerRef: COpaquePointer, id: String) =
    _ArkUIViewController_setId(controllerRef, id)

@InternalExportApi
@CName("androidx_compose_ui_arkui_ArkUIViewController_getId")
fun _Export_ArkUIViewController_getId(controllerRef: COpaquePointer): String? =
    _ArkUIViewController_getId(controllerRef)

@InternalExportApi
@CName("androidx_compose_ui_arkui_ArkUIViewController_setEnv")
fun _Export_ArkUIViewController_setEnv(controllerRef: COpaquePointer, env: napi_env) =
    _ArkUIViewController_setEnv(controllerRef, env)

@InternalExportApi
@CName("androidx_compose_ui_arkui_ArkUIViewController_setContext")
fun _Export_ArkUIViewController_setContext(controllerRef: COpaquePointer, context: napi_value) =
    _ArkUIViewController_setContext(controllerRef, context)

@InternalExportApi
@CName("androidx_compose_ui_arkui_ArkUIViewController_setUIContext")
fun _Export_ArkUIViewController_setUIContext(controllerRef: COpaquePointer, uiContext: napi_value) =
    _ArkUIViewController_setUIContext(controllerRef, uiContext)

@InternalExportApi
@CName("androidx_compose_ui_arkui_ArkUIViewController_setMessenger")
fun _Export_ArkUIViewController_setMessenger(controllerRef: COpaquePointer, messenger: napi_value) =
    _ArkUIViewController_setMessenger(controllerRef, messenger)

@InternalExportApi
@CName("androidx_compose_ui_arkui_ArkUIViewController_sendMessage")
fun _Export_ArkUIViewController_sendMessage(controllerRef: COpaquePointer, type: String, message: String): String? =
    _ArkUIViewController_sendMessage(controllerRef, type, message)

@InternalExportApi
@CName("androidx_compose_ui_arkui_ArkUIViewController_setXComponentRender")
fun _Export_ArkUIViewController_setXComponentRender(controllerRef: COpaquePointer, render: COpaquePointer) =
    _ArkUIViewController_setXComponentRender(controllerRef, render)

@InternalExportApi
@CName("androidx_compose_ui_arkui_ArkUIViewController_getXComponentRender")
fun _Export_ArkUIViewController_getXComponentRender(controllerRef: COpaquePointer): COpaquePointer? =
    _ArkUIViewController_getXComponentRender(controllerRef)

@InternalExportApi
@CName("androidx_compose_ui_arkui_ArkUIViewController_setRootView")
fun _Export_ArkUIViewController_setRootView(
    controllerRef: COpaquePointer,
    backRootView: napi_value,
    foreRootView: napi_value,
    touchableRootView: napi_value
) = _ArkUIViewController_setRootView(controllerRef, backRootView, foreRootView, touchableRootView)

@InternalExportApi
@CName("androidx_compose_ui_arkui_ArkUIViewController_aboutToAppear")
fun _Export_ArkUIViewController_aboutToAppear(controllerRef: COpaquePointer) =
    _ArkUIViewController_aboutToAppear(controllerRef)

@InternalExportApi
@CName("androidx_compose_ui_arkui_ArkUIViewController_aboutToDisappear")
fun _Export_ArkUIViewController_aboutToDisappear(controllerRef: COpaquePointer) =
    _ArkUIViewController_aboutToDisappear(controllerRef)

@InternalExportApi
@CName("androidx_compose_ui_arkui_ArkUIViewController_onPageShow")
fun _Export_ArkUIViewController_onPageShow(controllerRef: COpaquePointer) =
    _ArkUIViewController_onPageShow(controllerRef)

@InternalExportApi
@CName("androidx_compose_ui_arkui_ArkUIViewController_onPageHide")
fun _Export_ArkUIViewController_onPageHide(controllerRef: COpaquePointer) =
    _ArkUIViewController_onPageHide(controllerRef)

@InternalExportApi
@CName("androidx_compose_ui_arkui_ArkUIViewController_onBackPress")
fun _Export_ArkUIViewController_onBackPress(controllerRef: COpaquePointer): Boolean =
    _ArkUIViewController_onBackPress(controllerRef)

@InternalExportApi
@CName("androidx_compose_ui_arkui_ArkUIViewController_onSurfaceCreated")
fun _Export_ArkUIViewController_onSurfaceCreated(
    controllerRef: COpaquePointer,
    xcomponentPtr: COpaquePointer,
    width: Int,
    height: Int
) = _ArkUIViewController_onSurfaceCreated(controllerRef, xcomponentPtr, width, height)

@InternalExportApi
@CName("androidx_compose_ui_arkui_ArkUIViewController_onSurfaceChanged")
fun _Export_ArkUIViewController_onSurfaceChanged(controllerRef: COpaquePointer, width: Int, height: Int) =
    _ArkUIViewController_onSurfaceChanged(controllerRef, width, height)

@InternalExportApi
@CName("androidx_compose_ui_arkui_ArkUIViewController_onSurfaceShow")
fun _Export_ArkUIViewController_onSurfaceShow(controllerRef: COpaquePointer) =
    _ArkUIViewController_onSurfaceShow(controllerRef)

@InternalExportApi
@CName("androidx_compose_ui_arkui_ArkUIViewController_onSurfaceHide")
fun _Export_ArkUIViewController_onSurfaceHide(controllerRef: COpaquePointer) =
    _ArkUIViewController_onSurfaceHide(controllerRef)

@InternalExportApi
@CName("androidx_compose_ui_arkui_ArkUIViewController_onSurfaceDestroyed")
fun _Export_ArkUIViewController_onSurfaceDestroyed(controllerRef: COpaquePointer) =
    _ArkUIViewController_onSurfaceDestroyed(controllerRef)

@InternalExportApi
@CName("androidx_compose_ui_arkui_ArkUIViewController_onFrame")
fun _Export_ArkUIViewController_onFrame(controllerRef: COpaquePointer, timestamp: Long, targetTimestamp: Long) =
    _ArkUIViewController_onFrame(controllerRef, timestamp, targetTimestamp)

@InternalExportApi
@CName("androidx_compose_ui_arkui_ArkUIViewController_onFocusEvent")
fun _Export_ArkUIViewController_onFocusEvent(controllerRef: COpaquePointer) =
    _ArkUIViewController_onFocusEvent(controllerRef)

@InternalExportApi
@CName("androidx_compose_ui_arkui_ArkUIViewController_onKeyEvent")
fun _Export_ArkUIViewController_onKeyEvent(controllerRef: COpaquePointer) =
    _ArkUIViewController_onKeyEvent(controllerRef)

@InternalExportApi
@CName("androidx_compose_ui_arkui_ArkUIViewController_dispatchTouchEvent")
fun _Export_ArkUIViewController_dispatchTouchEvent(
    controllerRef: COpaquePointer,
    nativeTouchEvent: napi_value,
    ignoreInteropView: Boolean
): Boolean = _ArkUIViewController_dispatchTouchEvent(controllerRef, nativeTouchEvent, ignoreInteropView)

@InternalExportApi
@CName("androidx_compose_ui_arkui_ArkUIViewController_dispatchMouseEvent")
fun _Export_ArkUIViewController_dispatchMouseEvent(controllerRef: COpaquePointer) =
    _ArkUIViewController_dispatchMouseEvent(controllerRef)

@InternalExportApi
@CName("androidx_compose_ui_arkui_ArkUIViewController_dispatchHoverEvent")
fun _Export_ArkUIViewController_dispatchHoverEvent(controllerRef: COpaquePointer) =
    _ArkUIViewController_dispatchHoverEvent(controllerRef)

@InternalExportApi
@CName("androidx_compose_ui_arkui_ArkUIViewController_keyboardWillShow")
fun _Export_ArkUIViewController_keyboardWillShow(controllerRef: COpaquePointer, keyboardHeight: Float) =
    _ArkUIViewController_keyboardWillShow(controllerRef, keyboardHeight)

@InternalExportApi
@CName("androidx_compose_ui_arkui_ArkUIViewController_keyboardWillHide")
fun _Export_ArkUIViewController_keyboardWillHide(controllerRef: COpaquePointer) =
    _ArkUIViewController_keyboardWillHide(controllerRef)

@InternalExportApi
@CName("androidx_compose_ui_arkui_ArkUIViewController_requestSyncRefresh")
fun _Export_ArkUIViewController_requestSyncRefresh(controllerRef: COpaquePointer): Int =
    _ArkUIViewController_requestSyncRefresh(controllerRef)

@InternalExportApi
@CName("androidx_compose_ui_arkui_ArkUIViewController_cancelSyncRefresh")
fun _Export_ArkUIViewController_cancelSyncRefresh(controllerRef: COpaquePointer, refreshId: Int) =
    _ArkUIViewController_cancelSyncRefresh(controllerRef, refreshId)

@InternalExportApi
@CName("androidx_compose_ui_arkui_ArkUIViewController_onFinalize")
fun _Export_ArkUIViewController_onFinalize(controllerRef: COpaquePointer) =
    _ArkUIViewController_onFinalize(controllerRef)