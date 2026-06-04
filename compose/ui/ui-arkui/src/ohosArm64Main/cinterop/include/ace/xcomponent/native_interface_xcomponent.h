/*
 * Copyright (c) 2021-2023 Huawei Device Co., Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @addtogroup OH_NativeXComponent Native XComponent
 * @{
 *
 * @brief Describes the surface and touch event held by the ArkUI XComponent, which can be used for the EGL/OpenGL ES\n
 *        and media data input and displayed on the ArkUI XComponent.
 *
 * @since 8
 * @version 1.0
 */

/**
 * @file native_interface_xcomponent.h
 *
 * @brief Declares APIs for accessing a Native XComponent.
 *
 * @kit ArkUI
 * @since 8
 * @version 1.0
 */

#ifndef _NATIVE_INTERFACE_XCOMPONENT_H_
#define _NATIVE_INTERFACE_XCOMPONENT_H_

#include <stdbool.h>
#include <stdint.h>

#include "arkui/native_interface_accessibility.h"

#ifdef __cplusplus
extern "C" {
#endif

/**
 * @brief Provides an encapsulated <b>OH_NativeXComponent</b> instance.
 *
 * @since 8
 * @version 1.0
 */
typedef struct OH_NativeXComponent OH_NativeXComponent;

/**
 * @brief Obtains the pointer to the <b> ArkUI_AccessibilityProvider</b>
 * instance of this <b>OH_NativeXComponent</b> instance.
 *
 * @param component Indicates the pointer to the <b>OH_NativeXComponent</b> instance.
 * @param handle Indicates the pointer to the <b>ArkUI_AccessibilityProvider</b> instance.
 * @return Returns {@link OH_NATIVEXCOMPONENT_RESULT_SUCCESS} if the operation is successful.
 *         Returns {@link OH_NATIVEXCOMPONENT_RESULT_BAD_PARAMETER} if a parameter error occurs.
 * @since 13
 */
int32_t OH_NativeXComponent_GetNativeAccessibilityProvider(
        OH_NativeXComponent *component, ArkUI_AccessibilityProvider **handle);

#ifdef __cplusplus
};
#endif
#endif // _NATIVE_INTERFACE_XCOMPONENT_H_