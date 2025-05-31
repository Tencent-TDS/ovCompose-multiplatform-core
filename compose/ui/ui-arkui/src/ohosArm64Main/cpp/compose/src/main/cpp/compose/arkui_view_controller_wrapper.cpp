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

#include "arkui_view_controller_wrapper.h"
#include "arkui_view_controller.h"
#include "napi/native_api.h"
#include "xcomponent_holder.h"
#include "xcomponent_render.h"
#include "xcomponent_utils.h"

namespace androidx::compose::ui::arkui::utils::ArkUIViewControllerWrapper {
static void FinalizeArkViewController(napi_env env, void *data, void *hint) {
    auto controller = reinterpret_cast<ArkUIViewController *>(data);
    auto id = ArkUIViewController_getId(controller);
    auto render = reinterpret_cast<XComponentRender *>(ArkUIViewController_getXComponentRender(controller));
    if (render != nullptr && render->controller == controller) {
        // 解除赋值，重置为 nullptr
        render->controller = nullptr;
    }
    XComponentHolder::GetInstance()->RemoveArkUIViewController(id);
    ArkUIViewController_onFinalize(controller);
}

static napi_value Init(napi_env env, napi_callback_info info) {
    size_t argc = 4;
    napi_value args[4] = {nullptr, nullptr, nullptr, nullptr};
    napi_value thisArg = nullptr;
    napi_get_cb_info(env, info, &argc, args, &thisArg, nullptr);

    char idString[OH_XCOMPONENT_ID_BUFFER_LEN_MAX] = {0};
    size_t idSize = 0;
    napi_get_value_string_utf8(env, args[0], idString, OH_XCOMPONENT_ID_BUFFER_LEN_MAX, &idSize);

    std::string id(idString);

    void *controller = nullptr;
    napi_unwrap(env, thisArg, &controller);
    ArkUIViewController *typedController = reinterpret_cast<ArkUIViewController *>(controller);
    ArkUIViewController_setId(typedController, id);
    ArkUIViewController_setEnv(typedController, env);
    ArkUIViewController_setRootView(typedController, args[1], args[2], args[3]);

    XComponentHolder::GetInstance()->InitArkViewController(env, id, typedController);
    return nullptr;
}

static napi_value InitContext(napi_env env, napi_callback_info info) {
    size_t argc = 1;
    napi_value args[1] = {nullptr};
    napi_value thisArg = nullptr;
    napi_get_cb_info(env, info, &argc, args, &thisArg, nullptr);

    void *controller = nullptr;
    napi_unwrap(env, thisArg, &controller);
    ArkUIViewController *typedController = reinterpret_cast<ArkUIViewController *>(controller);
    ArkUIViewController_setContext(typedController, args[0]);
    return nullptr;
}

static napi_value InitUIContext(napi_env env, napi_callback_info info) {
    size_t argc = 1;
    napi_value args[1] = {nullptr};
    napi_value thisArg = nullptr;
    napi_get_cb_info(env, info, &argc, args, &thisArg, nullptr);

    void *controller = nullptr;
    napi_unwrap(env, thisArg, &controller);
    ArkUIViewController *typedController = reinterpret_cast<ArkUIViewController *>(controller);
    ArkUIViewController_setUIContext(typedController, args[0]);
    return nullptr;
}

static napi_value InitMessenger(napi_env env, napi_callback_info info) {
    size_t argc = 1;
    napi_value args[1] = {nullptr};
    napi_value thisArg = nullptr;
    napi_get_cb_info(env, info, &argc, args, &thisArg, nullptr);

    void *controller = nullptr;
    napi_unwrap(env, thisArg, &controller);
    ArkUIViewController *typedController = reinterpret_cast<ArkUIViewController *>(controller);
    ArkUIViewController_setMessenger(typedController, args[0]);
    return nullptr;
}

static napi_value AboutToAppear(napi_env env, napi_callback_info info) {
    size_t argc = 0;
    napi_value thisArg = nullptr;
    napi_get_cb_info(env, info, &argc, nullptr, &thisArg, nullptr);

    void *controller = nullptr;
    napi_unwrap(env, thisArg, &controller);
    ArkUIViewController_aboutToAppear(reinterpret_cast<ArkUIViewController *>(controller));
    return nullptr;
}

static napi_value AboutToDisappear(napi_env env, napi_callback_info info) {
    size_t argc = 0;
    napi_value thisArg = nullptr;
    napi_get_cb_info(env, info, &argc, nullptr, &thisArg, nullptr);

    void *controller = nullptr;
    napi_unwrap(env, thisArg, &controller);
    ArkUIViewController_aboutToDisappear(reinterpret_cast<ArkUIViewController *>(controller));
    return nullptr;
}

static napi_value OnPageShow(napi_env env, napi_callback_info info) {
    size_t argc = 0;
    napi_value thisArg = nullptr;
    napi_get_cb_info(env, info, &argc, nullptr, &thisArg, nullptr);

    void *controller = nullptr;
    napi_unwrap(env, thisArg, &controller);
    ArkUIViewController_onPageShow(reinterpret_cast<ArkUIViewController *>(controller));
    return nullptr;
}

static napi_value OnPageHide(napi_env env, napi_callback_info info) {
    size_t argc = 0;
    napi_value thisArg = nullptr;
    napi_get_cb_info(env, info, &argc, nullptr, &thisArg, nullptr);

    void *controller = nullptr;
    napi_unwrap(env, thisArg, &controller);
    ArkUIViewController_onPageHide(reinterpret_cast<ArkUIViewController *>(controller));
    return nullptr;
}

static napi_value OnBackPress(napi_env env, napi_callback_info info) {
    size_t argc = 0;
    napi_value thisArg = nullptr;
    napi_get_cb_info(env, info, &argc, nullptr, &thisArg, nullptr);

    void *controller = nullptr;
    napi_unwrap(env, thisArg, &controller);
    bool consumed = ArkUIViewController_onBackPress(reinterpret_cast<ArkUIViewController *>(controller));

    napi_value result = nullptr;
    napi_get_boolean(env, consumed, &result);
    return result;
}

static napi_value DispatchTouchEvent(napi_env env, napi_callback_info info) {
    size_t argc = 2;
    napi_value args[2] = {nullptr, nullptr};
    napi_value thisArg = nullptr;
    napi_get_cb_info(env, info, &argc, args, &thisArg, nullptr);

    napi_value touchEvent = args[0];
    bool ignoreInteropView;
    napi_get_value_bool(env, args[1], &ignoreInteropView);

    void *controller = nullptr;
    napi_unwrap(env, thisArg, &controller);

    ArkUIViewController *typedController = reinterpret_cast<ArkUIViewController *>(controller);
    ArkUIViewController_dispatchTouchEvent(typedController, touchEvent, ignoreInteropView);
    return nullptr;
}

static napi_value KeyboardWillShow(napi_env env, napi_callback_info info) {
    size_t argc = 1;
    napi_value args[1] = {nullptr};
    napi_value thisArg = nullptr;
    napi_get_cb_info(env, info, &argc, args, &thisArg, nullptr);

    double keyboardHeight;
    napi_get_value_double(env, args[0], &keyboardHeight);

    void *controller = nullptr;
    napi_unwrap(env, thisArg, &controller);

    ArkUIViewController *typedController = reinterpret_cast<ArkUIViewController *>(controller);
    ArkUIViewController_keyboardWillShow(typedController, keyboardHeight);

    return nullptr;
}

static napi_value KeyboardWillHide(napi_env env, napi_callback_info info) {
    size_t argc = 0;
    napi_value thisArg = nullptr;
    napi_get_cb_info(env, info, &argc, nullptr, &thisArg, nullptr);

    void *controller = nullptr;
    napi_unwrap(env, thisArg, &controller);

    ArkUIViewController *typedController = reinterpret_cast<ArkUIViewController *>(controller);
    ArkUIViewController_keyboardWillHide(typedController);

    return nullptr;
}

static napi_value RequestSyncRefresh(napi_env env, napi_callback_info info) {
    size_t argc = 0;
    napi_value thisArg = nullptr;
    napi_get_cb_info(env, info, &argc, nullptr, &thisArg, nullptr);

    void *controller = nullptr;
    napi_unwrap(env, thisArg, &controller);

    ArkUIViewController *typedController = reinterpret_cast<ArkUIViewController *>(controller);
    int consumed = ArkUIViewController_requestSyncRefresh(typedController);
    napi_value result = nullptr;
    napi_create_int32(env, consumed, &result);
    return result;
}

static napi_value CancelSyncRefresh(napi_env env, napi_callback_info info) {
    size_t argc = 1;
    napi_value args[1] = {nullptr};
    napi_value thisArg = nullptr;
    napi_get_cb_info(env, info, &argc, args, &thisArg, nullptr);

    double refreshId;
    napi_get_value_double(env, args[0], &refreshId);

    void *controller = nullptr;
    napi_unwrap(env, thisArg, &controller);

    ArkUIViewController *typedController = reinterpret_cast<ArkUIViewController *>(controller);
    ArkUIViewController_cancelSyncRefresh(typedController, refreshId);

    return nullptr;
}

static napi_value SendMessage(napi_env env, napi_callback_info info) {
    size_t argc = 2;
    napi_value args[2] = {nullptr, nullptr};
    napi_value thisArg = nullptr;
    napi_get_cb_info(env, info, &argc, args, &thisArg, nullptr);

    char typeString[OH_XCOMPONENT_ID_BUFFER_LEN_MAX] = {0};
    size_t typeSize = 0;
    napi_get_value_string_utf8(env, args[0], typeString, OH_XCOMPONENT_ID_BUFFER_LEN_MAX, &typeSize);
    std::string type(typeString);

    char messageString[OH_XCOMPONENT_ID_BUFFER_LEN_MAX] = {0};
    size_t messageSize = 0;
    napi_get_value_string_utf8(env, args[1], messageString, OH_XCOMPONENT_ID_BUFFER_LEN_MAX, &messageSize);
    std::string message(messageString);

    void *controller = nullptr;
    napi_unwrap(env, thisArg, &controller);
    ArkUIViewController *typedController = reinterpret_cast<ArkUIViewController *>(controller);

    auto response = ArkUIViewController_sendMessage(typedController, type, message);

    napi_value result = nullptr;

    if (response == nullptr) {
        napi_get_null(env, &result);
    } else {
        napi_create_string_utf8(env, response, NAPI_AUTO_LENGTH, &result);
    }
    return result;
}


static void bindFunction(napi_env env, napi_value object, const char *functionName, napi_callback functionCallback) {
    napi_value functionValue = nullptr;
    napi_create_function(env, functionName, NAPI_AUTO_LENGTH, functionCallback, nullptr, &functionValue);
    napi_set_named_property(env, object, functionName, functionValue);
}

napi_value Wrapped(napi_env env, void *nativeController) {
    napi_value result = nullptr;
    napi_create_object(env, &result);
    napi_wrap(env, result, nativeController, FinalizeArkViewController, nullptr, nullptr);

    bindFunction(env, result, "init", Init);
    bindFunction(env, result, "initContext", InitContext);
    bindFunction(env, result, "initUIContext", InitUIContext);
    bindFunction(env, result, "initMessenger", InitMessenger);
    bindFunction(env, result, "aboutToAppear", AboutToAppear);
    bindFunction(env, result, "aboutToDisappear", AboutToDisappear);
    bindFunction(env, result, "onPageShow", OnPageShow);
    bindFunction(env, result, "onPageHide", OnPageHide);
    bindFunction(env, result, "onBackPress", OnBackPress);
    bindFunction(env, result, "dispatchTouchEvent", DispatchTouchEvent);
    bindFunction(env, result, "onKeyboardShow", KeyboardWillShow);
    bindFunction(env, result, "onKeyboardHide", KeyboardWillHide);
    bindFunction(env, result, "requestSyncRefresh", RequestSyncRefresh);
    bindFunction(env, result, "cancelSyncRefresh", CancelSyncRefresh);
    bindFunction(env, result, "sendMessage", SendMessage);
    return result;
}
} // namespace androidx::compose::ui::arkui::utils::ArkUIViewControllerWrapper