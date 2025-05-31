/*
 * Copyright (c) 2024 Huawei Device Co., Ltd.
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
 * @addtogroup ArkUI_NativeModule
 * @{
 *
 * @brief Provides UI capabilities of ArkUI on the native side, such as UI component creation and destruction,
 * tree node operations, attribute setting, and event listening.
 *
 * @since 12
 */

/**
 * @file native_node_napi.h
 *
 * @brief Declares APIs for converting <b>FrameNode</b> objects on the ArkTS side to <b>ArkUI_NodeHandle</b> objects on
 * the native side.
 *
 * @library libace_ndk.z.so
 * @syscap SystemCapability.ArkUI.ArkUI.Full
 * @kit ArkUI
 * @since 12
 */

#ifndef ARKUI_NATIVE_NODE_NAPI_H
#define ARKUI_NATIVE_NODE_NAPI_H

#include "drawable_descriptor.h"
#include "napi/native_api.h"
#include "native_type.h"

#ifdef __cplusplus
extern "C" {
#endif
/**
 * @brief Obtains a <b>FrameNode</b> object on the ArkTS side and maps it to an <b>ArkUI_NodeHandle</b> object on the
 * native side.
 *
 * @param env Indicates the NAPI environment pointer.
 * @param frameNode Indicates the <b>FrameNode</b> object created on the ArkTS side.
 * @param handle Indicates the pointer to the <b>ArkUI_NodeHandle</b> object.
 * @return Returns the error code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 12
 */
int32_t OH_ArkUI_GetNodeHandleFromNapiValue(napi_env env, napi_value frameNode, ArkUI_NodeHandle* handle);

/**
 * @brief Obtains a <b>UIContext</b> object on the ArkTS side and maps it to an <b>ArkUI_ContextHandle</b> object on the
 * native side.
 *
 * @param env ndicates the NAPI environment pointer.
 * @param value Indicates the <b>UIContext</b> object created on the ArkTS side.
 * @param context Indicates the pointer to the <b>ArkUI_ContextHandle</b> object.
 * @return Returns the error code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 12
 */
int32_t OH_ArkUI_GetContextFromNapiValue(napi_env env, napi_value value, ArkUI_ContextHandle* context);


/**
  * @brief Obtains a <b>NodeContent</b> object on the ArkTS side and maps it to an <b>ArkUI_NodeContentHandle</b>
  * object on the native side.
  *
  * @param env ndicates the NAPI environment pointer.
  * @param value Indicates the <b>NodeContent</b> object created on the ArkTS side.
  * @param content Indicates the pointer to the <b>ArkUI_NodeContentHandle</b> object.
  * @return Returns the error code.
  *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
  *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
  * @since 12
  */
int32_t OH_ArkUI_GetNodeContentFromNapiValue(napi_env env, napi_value value, ArkUI_NodeContentHandle* content);

/**
 * @brief Obtains a <b>DrawableDescriptor</b> object on the ArkTS side and maps it to an
 * <b>ArkUI_DrawableDescriptro</b> object on the native side.
 *
 * @param env Indicates the NAPI environment pointer.
 * @param value Indicates the <b>DrawableDescriptor</b> object created on the ArkTS side.
 * @param drawableDescriptor Indicates the pointer to the <b>ArkUI_DrawableDescriptro</b> object.
 * @return Returns the error code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 12
*/
int32_t OH_ArkUI_GetDrawableDescriptorFromNapiValue(
    napi_env env, napi_value value, ArkUI_DrawableDescriptor** drawableDescriptor);

/**
 * @brief Obtains a <b>Resource</b> object on the ArkTS side and maps it to an
 * <b>ArkUI_DrawableDescriptro</b> object on the native side.
 *
 * @param env Indicates the NAPI environment pointer.
 * @param value Indicates the <b>Resource</b> object created on the ArkTS side.
 * @param drawableDescriptor Indicates the pointer to the <b>ArkUI_DrawableDescriptro</b> object.
 * @return Returns the error code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 12
*/
int32_t OH_ArkUI_GetDrawableDescriptorFromResourceNapiValue(
    napi_env env, napi_value value, ArkUI_DrawableDescriptor** drawableDescriptor);

/**
 * @brief Obtain the ID of the Navigation component where the node is located.
 *
 * @param node The node.
 * @param buffer The buffer to which NavigationID writes to the memory,
 *               memory space needs to be allocated by the developer.
 * @param bufferSize The buffer size
 * @param writeLength Indicates the string length actually written to the buffer
 *                    when returning {@link ARKUI_ERROR_CODE_NO_ERROR}.
 *                    Indicates the minimum buffer size that can accommodate the target
 *                    when {@link ARKUI_ERROR_CODE_BUFFER_SIZE_ERROR} is returned.
 * @return The error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} if the node, buffer or writeLength is null.
 *         {@link ARKUI_ERROR_CODE_GET_INFO_FAILED} if query information failed,
 *         this may be because the node is not in Navigation.
 *         {@link ARKUI_ERROR_CODE_BUFFER_SIZE_ERROR} If the buffer size is less than the minimum buffer size.
 * @since 12
 */
ArkUI_ErrorCode  OH_ArkUI_GetNavigationId(
    ArkUI_NodeHandle node, char* buffer, int32_t bufferSize, int32_t* writeLength);

/**
 * @brief Obtain the name of the NavDestination component where the node is located.
*
 * @param node The node.
 * @param buffer The buffer to which NavDestination name writes to the memory,
 *               memory space needs to be allocated by the developer.
 * @param bufferSize The buffer size
 * @param writeLength Indicates the string length actually written to the buffer
 *                    when returning {@link ARKUI_ERROR_CODE_NO_ERROR}.
 *                    Indicates the minimum buffer size that can accommodate the target
 *                    when {@link ARKUI_ERROR_CODE_BUFFER_SIZE_ERROR} is returned.
 * @return The error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} if the node, buffer or writeLength is null.
 *         {@link ARKUI_ERROR_CODE_GET_INFO_FAILED} if query information failed,
 *         this may be because the node is not in Navigation.
 *         {@link ARKUI_ERROR_CODE_BUFFER_SIZE_ERROR} If the buffer size is less than the minimum buffer size.
 * @since 12
 */
ArkUI_ErrorCode OH_ArkUI_GetNavDestinationName(
    ArkUI_NodeHandle node, char* buffer, int32_t bufferSize, int32_t* writeLength);

/**
 * @brief Based on the given index value, obtain the length of the Navigation stack where the node is located.
 *
 * @param node The node.
 * @param length The length of the stack. After the operation succeeds, the result is written back to this parameter.
 * @return The error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} if the node or length is null.
 *         {@link ARKUI_ERROR_CODE_GET_INFO_FAILED} if query information failed,
 *         this may be because the node is not in Navigation.
 * @since 12
 */
ArkUI_ErrorCode OH_ArkUI_GetNavStackLength(ArkUI_NodeHandle node, int32_t* length);

/**
 * @brief Based on the given index value, obtain the page name of the corresponding position
 *        in the navigation stack where the node is located.
 *        Index values are counted from 0, with 0 being the bottom of the stack.
 *
 * @param node The node.
 * @param index The index of the NavDestination in the stack is queried.
 * @param buffer The buffer to which NavDestination index writes to the memory,
 *               memory space needs to be allocated by the developer.
 * @param bufferSize The buffer size
 * @param writeLength Indicates the string length actually written to the buffer
 *                    when returning {@link ARKUI_ERROR_CODE_NO_ERROR}.
 *                    Indicates the minimum buffer size that can accommodate the target
 *                    when {@link ARKUI_ERROR_CODE_BUFFER_SIZE_ERROR} is returned.
 * @return The error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} if the node, buffer or writeLength is null.
 *         {@link ARKUI_ERROR_CODE_NODE_INDEX_INVALID} if index is an invalid value.
 *         {@link ARKUI_ERROR_CODE_GET_INFO_FAILED} if query information failed,
 *         this may be because the node is not in Navigation.
 *         {@link ARKUI_ERROR_CODE_BUFFER_SIZE_ERROR} If the buffer size is less than the minimum buffer size.
 * @since 12
 */
ArkUI_ErrorCode OH_ArkUI_GetNavDestinationNameByIndex(
    ArkUI_NodeHandle node, int32_t index, char* buffer, int32_t bufferSize, int32_t* writeLength);

/**
 * @brief Obtain the ID of the NavDestination component where the node is located.
 *
 * @param node The node.
 * @param buffer The buffer to which NavDestination ID writes to the memory,
 *               memory space needs to be allocated by the developer.
 * @param bufferSize The buffer size
 * @param writeLength Indicates the string length actually written to the buffer
 *                    when returning {@link ARKUI_ERROR_CODE_NO_ERROR}.
 *                    Indicates the minimum buffer size that can accommodate the target
 *                    when {@link ARKUI_ERROR_CODE_BUFFER_SIZE_ERROR} is returned.
 * @return The error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} if the node, buffer or writeLength is null.
 *         {@link ARKUI_ERROR_CODE_GET_INFO_FAILED} if query information failed,
 *         this may be because the node is not in Navigation.
 *         {@link ARKUI_ERROR_CODE_BUFFER_SIZE_ERROR} If the buffer size is less than the minimum buffer size.
 * @since 12
 */
ArkUI_ErrorCode OH_ArkUI_GetNavDestinationId(
    ArkUI_NodeHandle node, char* buffer, int32_t bufferSize, int32_t* writeLength);

/**
 * @brief Obtain the state of the NavDestination component where the node is located.
 *
 * @param node The node.
 * @param state The state value of NavDestination is written back into this parameter.
 * @return The error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} if the node or state is null.
 *         {@link ARKUI_ERROR_CODE_GET_INFO_FAILED} if query information failed,
 *         this may be because the node is not in Navigation.
 * @since 12
 */
ArkUI_ErrorCode OH_ArkUI_GetNavDestinationState(ArkUI_NodeHandle node, ArkUI_NavDestinationState* state);

/**
 * @brief Obtain the index of the NavDestination component on the Navigation stack where the node is located.
 *
 * @param node The node.
 * @param index Index value, counted from 0.
 * @return The error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} if the node or index is null.
 *         {@link ARKUI_ERROR_CODE_GET_INFO_FAILED} if query information failed,
 *         this may be because the node is not in Navigation.
 * @since 12
 */
ArkUI_ErrorCode OH_ArkUI_GetNavDestinationIndex(ArkUI_NodeHandle node, int32_t* index);

/**
 * @brief Obtain the parameters of the NavDestination component where the node is located.
 *
 * @param node The node.
 * @return The parameters.
 *         If a null pointer is returned, it may be because the node is empty or the parameters does not exist.
 * @since 12
 */
napi_value OH_ArkUI_GetNavDestinationParam(ArkUI_NodeHandle node);

/**
 * @brief Obtain the index of the page where the node resides in the Router page stack.
 *
 * @param node The node.
 * @param index Index value, counted from 1.
 * @return The error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} if the node or index is null.
 *         {@link ARKUI_ERROR_CODE_GET_INFO_FAILED} if query information failed,
 *         this may be because the node is not in RouterPage.
 * @since 12
 */
ArkUI_ErrorCode OH_ArkUI_GetRouterPageIndex(ArkUI_NodeHandle node, int32_t* index);

/**
 * @brief Obtain the name of the page where the node is located.
 *
 * @param node The node.
 * @param buffer The buffer to which page name writes to the memory,
 *               memory space needs to be allocated by the developer.
 * @param bufferSize The buffer size
 * @param writeLength Indicates the string length actually written to the buffer
 *                    when returning {@link ARKUI_ERROR_CODE_NO_ERROR}.
 *                    Indicates the minimum buffer size that can accommodate the target
 *                    when {@link ARKUI_ERROR_CODE_BUFFER_SIZE_ERROR} is returned.
 * @return The error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} if the node, buffer or writeLength is null.
 *         {@link ARKUI_ERROR_CODE_GET_INFO_FAILED} if query information failed,
 *         this may be because the node is not in RouterPage.
 *         {@link ARKUI_ERROR_CODE_BUFFER_SIZE_ERROR} If the buffer size is less than the minimum buffer size.
 * @since 12
 */
ArkUI_ErrorCode OH_ArkUI_GetRouterPageName(
    ArkUI_NodeHandle node, char* buffer, int32_t bufferSize, int32_t* writeLength);

/**
 * @brief Obtain the path of the page where the node is located.
 *
 * @param node The node.
 * @param buffer The buffer to which page path writes to the memory,
 *               memory space needs to be allocated by the developer.
 * @param bufferSize The buffer size
 * @param writeLength Indicates the string length actually written to the buffer
 *                    when returning {@link ARKUI_ERROR_CODE_NO_ERROR}.
 *                    Indicates the minimum buffer size that can accommodate the target
 *                    when {@link ARKUI_ERROR_CODE_BUFFER_SIZE_ERROR} is returned.
 * @return The error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} if the node, buffer or writeLength is null.
 *         {@link ARKUI_ERROR_CODE_GET_INFO_FAILED} if query information failed,
 *         this may be because the node is not in RouterPage.
 *         {@link ARKUI_ERROR_CODE_BUFFER_SIZE_ERROR} If the buffer size is less than the minimum buffer size.
 * @since 12
 */
ArkUI_ErrorCode OH_ArkUI_GetRouterPagePath(
    ArkUI_NodeHandle node, char* buffer, int32_t bufferSize, int32_t* writeLength);
/**
 * @brief Obtain the state of the page where the node is located.
 *
 * @param node The node.
 * @param state The state value of the page is written back to this parameter.
 * @return The error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} if the node or state is null.
 *         {@link ARKUI_ERROR_CODE_GET_INFO_FAILED} if query information failed,
 *         this may be because the node is not in RouterPage.
 * @since 12
 */
ArkUI_ErrorCode OH_ArkUI_GetRouterPageState(ArkUI_NodeHandle node, ArkUI_RouterPageState* state);

/**
 * @brief Obtain the ID of the page where the node is located.
 *
 * @param node The node.
 * @param buffer The buffer to which page ID writes to the memory,
 *               memory space needs to be allocated by the developer.
 * @param bufferSize The buffer size
 * @param writeLength Indicates the string length actually written to the buffer
 *                    when returning {@link ARKUI_ERROR_CODE_NO_ERROR}.
 *                    Indicates the minimum buffer size that can accommodate the target
 *                    when {@link ARKUI_ERROR_CODE_BUFFER_SIZE_ERROR} is returned.
 * @return The error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} if the node, buffer or writeLength is null.
 *         {@link ARKUI_ERROR_CODE_GET_INFO_FAILED} if query information failed,
 *         this may be because the node is not in RouterPage.
 *         {@link ARKUI_ERROR_CODE_BUFFER_SIZE_ERROR} If the buffer size is less than the minimum buffer size.
 * @since 12
 */
ArkUI_ErrorCode OH_ArkUI_GetRouterPageId(
    ArkUI_NodeHandle node, char* buffer, int32_t bufferSize, int32_t* writeLength);

#ifdef __cplusplus
};
#endif

#endif // ARKUI_NATIVE_NODE_NAPI_H
/** @} */
