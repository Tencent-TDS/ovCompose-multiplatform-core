/*
 * Copyright (c) 2025 Huawei Device Co., Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"),
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
 * @addtogroup ArkUI_RenderNodeUtils
 * @{
 *
 * @brief Provides RenderNode capabilities of ArkUI on the native side, such as RenderNode creation and destruction,
 * tree node operations, and attribute setting.
 *
 * @since 20
 */

/**
 * @file native_render.h
 *
 * @brief Provides type definitions for <b>NativeRenderNode</b> APIs.
 *
 * @library libace_ndk.z.so
 * @syscap SystemCapability.ArkUI.ArkUI.Full
 * @kit ArkUI
 * @since 20
 */

#ifndef ARKUI_NATIVE_RENDER_H
#define ARKUI_NATIVE_RENDER_H

#ifdef __cplusplus
#include <cstdint>
#else
#include <stdint.h>
#endif

#include "native_type.h"

#ifdef __cplusplus
extern "C" {
#endif

/**
 * @brief Handle to ArkUI native render node object.
 *
 * @since 20
 */
typedef struct ArkUI_RenderNode* ArkUI_RenderNodeHandle;

/**
 * @brief Handle to ArkUI native render content modifier.
 *
 * @since 20
 */
typedef struct ArkUI_RenderContentModifier* ArkUI_RenderContentModifierHandle;

/**
 * @brief Handle to the ArkUI native float render property.
 *
 * @since 20
 */
typedef struct ArkUI_FloatProperty* ArkUI_FloatPropertyHandle;

/**
 * @brief Handle to the ArkUI native vector2 render property.
 *
 * @since 20
 */
typedef struct ArkUI_Vector2Property* ArkUI_Vector2PropertyHandle;

/**
 * @brief Handle to the ArkUI native color render property.
 *
 * @since 20
 */
typedef struct ArkUI_ColorProperty* ArkUI_ColorPropertyHandle;

/**
 * @brief Handle to the ArkUI native animatable float render property.
 *
 * @since 20
 */
typedef struct ArkUI_FloatAnimatableProperty* ArkUI_FloatAnimatablePropertyHandle;

/**
 * @brief Handle to the ArkUI native animatable vector2 render property.
 *
 * @since 20
 */
typedef struct ArkUI_Vector2AnimatableProperty* ArkUI_Vector2AnimatablePropertyHandle;

/**
 * @brief Handle to the ArkUI native animatable color render property.
 *
 * @since 20
 */
typedef struct ArkUI_ColorAnimatableProperty* ArkUI_ColorAnimatablePropertyHandle;

/**
 * @brief Rectangle shape option.
 *
 * @since 20
 */
typedef struct ArkUI_RectShape ArkUI_RectShapeOption;

/**
 * @brief Node border style option.
 *
 * @since 20
 */
typedef struct ArkUI_NodeBorderStyle ArkUI_NodeBorderStyleOption;

/**
 * @brief Node border width option.
 *
 * @since 20
 */
typedef struct ArkUI_NodeBorderWidth ArkUI_NodeBorderWidthOption;

/**
 * @brief Node border color option.
 *
 * @since 20
 */
typedef struct ArkUI_NodeBorderColor ArkUI_NodeBorderColorOption;

/**
 * @brief Node border radius option.
 *
 * @since 20
 */
typedef struct ArkUI_NodeBorderRadius ArkUI_NodeBorderRadiusOption;

/**
 * @brief Circle shape option.
 *
 * @since 20
 */
typedef struct ArkUI_CircleShape ArkUI_CircleShapeOption;

/**
 * @brief Round rect shape option.
 *
 * @since 20
 */
typedef struct ArkUI_RoundRectShape ArkUI_RoundRectShapeOption;

/**
 * @brief Command path option.
 *
 * @since 20
 */
typedef struct ArkUI_CommandPath ArkUI_CommandPathOption;

/**
 * @brief Render node mask option.
 *
 * @since 20
 */
typedef struct ArkUI_RenderNodeMaskOption ArkUI_RenderNodeMaskOption;

/**
 * @brief Render node clip option.
 *
 * @since 20
 */
typedef struct ArkUI_RenderNodeClipOption ArkUI_RenderNodeClipOption;

/**
 * @brief Adds a child render node to a parent node.
 *
 * @param node the target parent node.
 *     - Only customNode type parent nodes are supported.
 *     - Each customNode can mount only one ArkUI_RenderNodeHandle.
 *     - customNode cannot mount other ArkUI_NodeHandle.
 * @param child the child RenderNode to add.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 *         {@link ARKUI_ERROR_CODE_NOT_CUSTOM_NODE} The node is not a customNode.
 *         {@link ARKUI_ERROR_CODE_CHILD_EXISTED} The node already has a child.
 *         {@link ARKUI_ERROR_CODE_RENDER_PARENT_EXISTED} The child already has a parent node.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_AddRenderNode(ArkUI_NodeHandle node, ArkUI_RenderNodeHandle child);

/**
 * @brief Remove the specified child node from its parent node.
 *
 * @param node the parent node.
 * @param child the child RenderNode to remove.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 *         {@link ARKUI_ERROR_CODE_NOT_CUSTOM_NODE} The node is not a customNode.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_RemoveRenderNode(ArkUI_NodeHandle node, ArkUI_RenderNodeHandle child);

/**
 * @brief Clear all child render nodes from the specified parent node.
 *
 * @param node the target parent node.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 *         {@link ARKUI_ERROR_CODE_NOT_CUSTOM_NODE} The node is not a customNode.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_ClearRenderNodeChildren(ArkUI_NodeHandle node);

/**
 * @brief Mark dirty the node handle. Invalidate the specified node, causing its lifecycle and triggering a
 *     rerender of its child render nodes.
 *
 * @param node Target node.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_Invalidate(ArkUI_NodeHandle node);

/**
 * @brief Create a new RenderNode handle.
 *
 * @return ArkUI_RenderNodeHandle pointer to receive the new node handle.
 * @since 20
 */
ArkUI_RenderNodeHandle OH_ArkUI_RenderNodeUtils_CreateNode();

/**
 * @brief Dispose a RenderNode handle.
 *
 * @param node Handle to the render node to dispose.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_DisposeNode(ArkUI_RenderNodeHandle node);

/**
 * @brief Add a child node to the specified RenderNode.
 *
 * @param node the parent RenderNode.
 * @param child the child RenderNode to add.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_AddChild(ArkUI_RenderNodeHandle node, ArkUI_RenderNodeHandle child);

/**
 * @brief Insert a child node after the specified sibling node in RenderNode.
 *
 * @param node the parent RenderNode.
 * @param child the child RenderNode to insert.
 * @param sibling the reference sibling node. If the sibling node does not exist in the children,
 *     the child component will be added to the end.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_InsertChildAfter(
    ArkUI_RenderNodeHandle node, ArkUI_RenderNodeHandle child, ArkUI_RenderNodeHandle sibling);

/**
 * @brief Remove a child node from the specified RenderNode.
 *
 * @param node the parent RenderNode.
 * @param child the child RenderNode to remove.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_RemoveChild(ArkUI_RenderNodeHandle node, ArkUI_RenderNodeHandle child);

/**
 * @brief Clear all children from the specified RenderNode.
 *
 * @param node Handle to the target render node.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_ClearChildren(ArkUI_RenderNodeHandle node);

/**
 * @brief Get a child node at the specified index position.
 *
 * @param node the parent RenderNode.
 * @param index Zero-based index of the child node.
 * @param child ArkUI_RenderNodeHandle pointer to receive the child node.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 *         {@link ARKUI_ERROR_CODE_CHILD_RENDER_NOT_EXIST} The child does not exist.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_GetChild(ArkUI_RenderNodeHandle node, int32_t index, ArkUI_RenderNodeHandle* child);

/**
 * @brief Get the first child node of the specified RenderNode.
 *
 * @param node Handle to the target render node.
 * @param child ArkUI_RenderNodeHandle pointer to receive the first child.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 *         {@link ARKUI_ERROR_CODE_CHILD_RENDER_NOT_EXIST} The child does not exist.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_GetFirstChild(ArkUI_RenderNodeHandle node, ArkUI_RenderNodeHandle* child);

/**
 * @brief Get the next sibling node of the specified node.
 *
 * @param node the reference node.
 * @param sibling ArkUI_RenderNodeHandle pointer to receive the next sibling.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 *         {@link ARKUI_ERROR_CODE_CHILD_RENDER_NOT_EXIST} The child does not exist.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_GetNextSibling(ArkUI_RenderNodeHandle node, ArkUI_RenderNodeHandle* sibling);

/**
 * @brief Get the previous sibling node of the specified node.
 *
 * @param node the reference node.
 * @param sibling ArkUI_RenderNodeHandle pointer to receive the previous sibling.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 *         {@link ARKUI_ERROR_CODE_CHILD_RENDER_NOT_EXIST} The child does not exist.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_GetPreviousSibling(ArkUI_RenderNodeHandle node, ArkUI_RenderNodeHandle* sibling);

/**
 * @brief Retrieve all child render nodes of a parent render node.
 *     This function populates a list of child render nodes for the specified parent node.
 *     The caller is responsible for freeing the allocated child node array.
 *
 * @param node Handle to the target parent render node.
 * @param children Pointer to an array of child render node handles.
 * @param count Pointer to store the number of child nodes retrieved.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_GetChildren(
    ArkUI_RenderNodeHandle node, ArkUI_RenderNodeHandle** children, int32_t* count);

/**
 * @brief Retrieve render child nodes count of a render node.
 *
 * @param node Handle to the target parent render node.
 * @param count Pointer to store the number of child nodes retrieved.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_GetChildrenCount(ArkUI_RenderNodeHandle node, int32_t* count);

/**
 * @brief Set the background color for a RenderNode.
 *
 * @param node Handle to the target render node
 * @param color Packed ARGB color value (32-bit unsigned integer)
 *     Color byte layout:
 *     - Bits 24-31: Alpha channel (0x00 fully transparent, 0xFF fully opaque)
 *     - Bits 16-23: Red channel
 *     - Bits 8-15: Green channel
 *     - Bits 0-7: Blue channel
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_SetBackgroundColor(ArkUI_RenderNodeHandle node, uint32_t color);

/**
 * @brief Get the background color of a RenderNode.
 *
 * @param node Handle to the target render node
 * @param color Integer pointer to store the retrieved RGBA color value.
 *     The retrieved color is a packed 32-bit value with the following byte layout:
 *     - Bits 24-31: Alpha channel (0x00 fully transparent, 0xFF fully opaque)
 *     - Bits 16-23: Red channel
 *     - Bits 8-15: Green channel
 *     - Bits 0-7: Blue channel
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_GetBackgroundColor(ArkUI_RenderNodeHandle node, uint32_t* color);

/**
 * @brief Set whether to clip content to the node's frame.
 *
 * @param node Handle to the target render node.
 * @param clipToFrame Integer (1 = clip to frame, 0 = do not clip).
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 *         {@link ARKUI_ERROR_CODE_PARAM_OUT_OF_RANGE} Parameter out of range.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_SetClipToFrame(ArkUI_RenderNodeHandle node, int32_t clipToFrame);

/**
 * @brief Get the clip-to-frame status of a RenderNode.
 *
 * @param node Handle to the target render node.
 * @param clipToFrame Integer pointer to receive clip status (1 or 0).
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_GetClipToFrame(ArkUI_RenderNodeHandle node, int32_t* clipToFrame);

/**
 * @brief Set whether to clip content to the node's Bounds.
 *
 * @param node Handle to the target render node.
 * @param clipToBounds Clipping flag (1: clip to bounds, 0: do not clip)
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 *         {@link ARKUI_ERROR_CODE_PARAM_OUT_OF_RANGE} Parameter out of range.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_SetClipToBounds(ArkUI_RenderNodeHandle node, int32_t clipToBounds);

/**
 * @brief Get whether to clip content to the node's Bounds.
 *
 * @param node Handle to the target render node.
 * @param clipToBounds Integer pointer (1 = clip to bounds, 0 = do not clip).
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_GetClipToBounds(ArkUI_RenderNodeHandle node, int32_t* clipToBounds);

/**
 * @brief Set the opacity value for a RenderNode.
 *
 * @param node Handle to the target render node.
 * @param opacity Opacity value (0.0-1.0).
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 *         {@link ARKUI_ERROR_CODE_PARAM_OUT_OF_RANGE} Parameter out of range.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_SetOpacity(ArkUI_RenderNodeHandle node, float opacity);

/**
 * @brief Get the opacity value of a RenderNode.
 *
 * @param node Handle to the target render node.
 * @param opacity Pointer to receive opacity value (0.0-1.0).
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_GetOpacity(ArkUI_RenderNodeHandle node, float* opacity);

/**
 * @brief Set the dimensions for a RenderNode.
 *
 * @param node Handle to the target render node.
 * @param width Width value in pixels.
 * @param height Height value in pixels.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 *         {@link ARKUI_ERROR_CODE_PARAM_OUT_OF_RANGE} Parameter out of range.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_SetSize(ArkUI_RenderNodeHandle node, int32_t width, int32_t height);

/**
 * @brief Get the dimensions of a RenderNode.
 *
 * @param node Handle to the target render node.
 * @param width Pointer to receive width value in pixels.
 * @param height Pointer to receive height value in pixels.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_GetSize(ArkUI_RenderNodeHandle node, int32_t* width, int32_t* height);

/**
 * @brief Set the position coordinates for a RenderNode.
 *
 * @param node Handle to the target render node.
 * @param x X-coordinate value in pixels.
 * @param y Y-coordinate value in pixels.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_SetPosition(ArkUI_RenderNodeHandle node, int32_t x, int32_t y);

/**
 * @brief Get the position coordinates of a RenderNode.
 *
 * @param node Handle to the target render node.
 * @param x Pointer to receive X-coordinate value in pixels.
 * @param y Pointer to receive Y-coordinate value in pixels.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_GetPosition(ArkUI_RenderNodeHandle node, int32_t* x, int32_t* y);

/**
 * @brief Set the pivot point for transformations of a RenderNode.
 *
 * @param node Handle to the target render node.
 * @param x The X-coordinate of pivot (normalized 0.0-1.0).
 * @param y The Y-coordinate of pivot (normalized 0.0-1.0).
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_SetPivot(ArkUI_RenderNodeHandle node, float x, float y);

/**
 * @brief Get the pivot point coordinates of a RenderNode.
 *
 * @param node Handle to the target render node.
 * @param x Pointer to receive the X-coordinate of pivot.
 * @param y Pointer to receive the Y-coordinate of pivot.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_GetPivot(ArkUI_RenderNodeHandle node, float* x, float* y);

/**
 * @brief Set the scaling factors for a RenderNode.
 *
 * @param node Handle to the target render node.
 * @param x Horizontal scale factor.
 * @param y Vertical scale factor.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_SetScale(ArkUI_RenderNodeHandle node, float x, float y);

/**
 * @brief Get the scaling factors of a RenderNode.
 *
 * @param node Handle to the target render node.
 * @param x Pointer to receive horizontal scale factor.
 * @param y Pointer to receive vertical scale factor.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_GetScale(ArkUI_RenderNodeHandle node, float* x, float* y);

/**
 * @brief Set the translation offset for a RenderNode.
 *
 * @param node Handle to the target render node.
 * @param x Horizontal translation in pixels.
 * @param y Vertical translation in pixels.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_SetTranslation(ArkUI_RenderNodeHandle node, float x, float y);

/**
 * @brief Get the translation offset of a RenderNode.
 *
 * @param node Handle to the target render node.
 * @param x Pointer to receive horizontal translation.
 * @param y Pointer to receive vertical translation.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_GetTranslation(ArkUI_RenderNodeHandle node, float* x, float* y);

/**
 * @brief Set the rotation angles for a RenderNode.
 *
 * @param node Handle to the target render node.
 * @param x Rotation angle around X-axis in degrees.
 * @param y Rotation angle around Y-axis in degrees.
 * @param z Rotation angle around Z-axis in degrees.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_SetRotation(ArkUI_RenderNodeHandle node, float x, float y, float z);

/**
 * @brief Get the rotation angles of a RenderNode.
 *
 * @param node Handle to the target render node.
 * @param x Pointer to receive X-axis rotation angle in degrees.
 * @param y Pointer to receive Y-axis rotation angle in degrees.
 * @param z Pointer to receive Z-axis rotation angle in degrees.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_GetRotation(ArkUI_RenderNodeHandle node, float* x, float* y, float* z);

/**
 * @brief Set the transformation matrix for a RenderNode.
 *
 * @param node Handle to the target render node.
 * @param matrix Pointer to a 4x4 transformation matrix (16 consecutive values).
 *     The transformation matrix should be provided as 16 consecutive floating-point values in row-major order:
 *       [m00, m01, m02, m03,
 *        m10, m11, m12, m13,
 *        m20, m21, m22, m23,
 *        m30, m31, m32, m33]
 *     Where the matrix represents:
 *       | m00 m01 m02 m03 |
 *       | m10 m11 m12 m13 |
 *       | m20 m21 m22 m23 |
 *       | m30 m31 m32 m33 |
 *     Matrix components:
 *       | m00  | Scaling value of the x-axis. The default value is **1** for the identity matrix.     |
 *       | m01  | The second value, which is affected by the rotation or tilt of the x, y, and z axes.  |
 *       | m02  | The third value, which is affected by the rotation of the x, y, and z axes.  |
 *       | m03  | The fourth value, which is affected by perspective projection.              |
 *       | m10  | The fifth value, which is affected by the rotation or tilt of the x, y, and z axes.  |
 *       | m11  | Scaling value of the y-axis. The default value is **1** for the identity matrix.     |
 *       | m12  | The seventh value, which is affected by the rotation of the x, y, and z axes.  |
 *       | m13  | The eighth value, which is affected by perspective projection.              |
 *       | m20  | The ninth value, which is affected by the rotation of the x, y, and z axes.  |
 *       | m21  | The tenth value, which is affected by the rotation of the x, y, and z axes. |
 *       | m22  | Scaling value of the z-axis. The default value is **1** for the identity matrix.     |
 *       | m23  | The 12th value, which is affected by perspective projection.              |
 *       | m30  | Translation value of the x-axis, in px. The default value is **0** for the identity matrix.|
 *       | m31  | Translation value of the y-axis, in px. The default value is **0** for the identity matrix.|
 *       | m32  | Translation value of the z-axis, in px. The default value is **0** for the identity matrix.|
 *       | m33  | Valid in homogeneous coordinates, presenting the perspective projection effect.   |
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 *         {@link ARKUI_ERROR_CODE_PARAM_OUT_OF_RANGE} Parameter out of range.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_SetTransform(ArkUI_RenderNodeHandle node, float* matrix);

/**
 * @brief Set the shadow color for a RenderNode.
 *
 * @param node Handle to the target render node.
 * @param color Packed ARGB color value (32-bit unsigned integer)
 *     Color byte layout:
 *     - Bits 24-31: Alpha channel (0x00 fully transparent, 0xFF fully opaque)
 *     - Bits 16-23: Red channel
 *     - Bits 8-15: Green channel
 *     - Bits 0-7: Blue channel
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_SetShadowColor(ArkUI_RenderNodeHandle node, uint32_t color);

/**
 * @brief Get the shadow color of a RenderNode.
 *
 * @param node Handle to the target render node.
 * @param color Integer pointer to store the retrieved RGBA color value.
 *     The retrieved color is a packed 32-bit value with the following byte layout:
 *     - Bits 24-31: Alpha channel (0x00 fully transparent, 0xFF fully opaque)
 *     - Bits 16-23: Red channel
 *     - Bits 8-15: Green channel
 *     - Bits 0-7: Blue channel
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_GetShadowColor(ArkUI_RenderNodeHandle node, uint32_t* color);

/**
 * @brief Set the shadow offset for a RenderNode.
 *
 * @param node Handle to the target render node.
 * @param x Horizontal offset value in pixels.
 * @param y Vertical offset value in pixels.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_SetShadowOffset(ArkUI_RenderNodeHandle node, int32_t x, int32_t y);

/**
 * @brief Get the shadow offset of a RenderNode.
 *
 * @param node Handle to the target render node.
 * @param x Pointer to receive horizontal offset value.
 * @param y Pointer to receive vertical offset value.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_GetShadowOffset(ArkUI_RenderNodeHandle node, int32_t* x, int32_t* y);

/**
 * @brief Set the shadow transparency for a RenderNode.
 *
 * @param node Handle to the target render node.
 * @param alpha Shadow alpha value (0.0-1.0).
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 *         {@link ARKUI_ERROR_CODE_PARAM_OUT_OF_RANGE} Parameter out of range.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_SetShadowAlpha(ArkUI_RenderNodeHandle node, float alpha);

/**
 * @brief Get the shadow transparency of a RenderNode.
 *
 * @param node Handle to the target render node.
 * @param alpha Pointer to receive shadow alpha value.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_GetShadowAlpha(ArkUI_RenderNodeHandle node, float* alpha);

/**
 * @brief Set the shadow elevation for a RenderNode.
 *
 * @param node Handle to the target render node.
 * @param elevation Elevation value.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 *         {@link ARKUI_ERROR_CODE_PARAM_OUT_OF_RANGE} Parameter out of range.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_SetShadowElevation(ArkUI_RenderNodeHandle node, float elevation);

/**
 * @brief Get the shadow elevation of a RenderNode.
 *
 * @param node Handle to the target render node.
 * @param elevation Pointer to receive elevation value.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_GetShadowElevation(ArkUI_RenderNodeHandle node, float* elevation);

/**
 * @brief Set the shadow radius of a RenderNode.
 *
 * @param node Handle to the target render node.
 * @param radius Radius value.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 *         {@link ARKUI_ERROR_CODE_PARAM_OUT_OF_RANGE} Parameter out of range.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_SetShadowRadius(ArkUI_RenderNodeHandle node, float radius);

/**
 * @brief Get the shadow radius of a RenderNode.
 *
 * @param node Handle to the target render node.
 * @param radius Pointer to receive radius value.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_GetShadowRadius(ArkUI_RenderNodeHandle node, float* radius);

/**
 * @brief Set the border style for a RenderNode.
 *
 * @param node Handle to the target render node.
 * @param borderStyle Handle to border style option.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_SetBorderStyle(ArkUI_RenderNodeHandle node, ArkUI_NodeBorderStyleOption* borderStyle);

/**
 * @brief Get the border style of a RenderNode.
 *
 * @param node Handle to the target render node.
 * @param borderStyle Pointer to receive border style option.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_GetBorderStyle(ArkUI_RenderNodeHandle node, ArkUI_NodeBorderStyleOption** borderStyle);

/**
 * @brief Set the border widths for a RenderNode.
 *
 * @param node Handle to the target render node.
 * @param borderWidth Handle to border width option.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_SetBorderWidth(ArkUI_RenderNodeHandle node, ArkUI_NodeBorderWidthOption* borderWidth);

/**
 * @brief Get the border widths of a RenderNode.
 *
 * @param node Handle to the target render node.
 * @param borderWidth Pointer to receive border width option.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_GetBorderWidth(ArkUI_RenderNodeHandle node, ArkUI_NodeBorderWidthOption** borderWidth);

/**
 * @brief Set the border colors for a RenderNode.
 *
 * @param node Handle to the target render node.
 * @param borderColor Handle to border color option.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_SetBorderColor(ArkUI_RenderNodeHandle node, ArkUI_NodeBorderColorOption* borderColor);

/**
 * @brief Get the border colors of a RenderNode.
 *
 * @param node Handle to the target render node.
 * @param borderColor Pointer to receive border color option.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_GetBorderColor(ArkUI_RenderNodeHandle node, ArkUI_NodeBorderColorOption** borderColor);

/**
 * @brief Set the border corner radius for a RenderNode.
 *
 * @param node Handle to the target render node.
 * @param borderRadius Handle to border radius option.
 * @r eturn Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_SetBorderRadius(
    ArkUI_RenderNodeHandle node, ArkUI_NodeBorderRadiusOption* borderRadius);

/**
 * @brief Get the border corner radius of a RenderNode.
 *
 * @param node Handle to the target render node.
 * @param borderRadius Pointer to receive border radius option.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_GetBorderRadius(
    ArkUI_RenderNodeHandle node, ArkUI_NodeBorderRadiusOption** borderRadius);

/**
 * @brief Apply a mask to a render node using the mask option.
 *
 * @param node Handle to the target render node.
 * @param mask Handle to the mask option.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_SetMask(ArkUI_RenderNodeHandle node, ArkUI_RenderNodeMaskOption* mask);

/**
 * @brief Apply a clip to a render node using the clip option.
 *
 * @param node Handle to the target render node.
 * @param clip Handle to the clip option.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_SetClip(ArkUI_RenderNodeHandle node, ArkUI_RenderNodeClipOption* clip);

/**
 * @brief Mark whether to preferentially draw the node and its children.
 *
 * @param node Handle to the target render node.
 * @param markNodeGroup Boolean flag indicates whether to preferentially draw the node and its children.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_SetMarkNodeGroup(ArkUI_RenderNodeHandle node, bool markNodeGroup);

/**
 * @brief Set the bounds for a RenderNode.
 *
 * @param node Handle to the target render node.
 * @param x X-coordinate of the bounds's top-left corner (in pixels)
 * @param y Y-coordinate of the bounds's top-left corner (in pixels)
 * @param width Width of the bounds (in pixels).
 * @param height Height of the bounds (in pixels).
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 *         {@link ARKUI_ERROR_CODE_PARAM_OUT_OF_RANGE} Parameter out of range.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_SetBounds(
    ArkUI_RenderNodeHandle node, int32_t x, int32_t y, int32_t width, int32_t height);

/**
 * @brief Get the bounds for a RenderNode.
 *
 * @param node Handle to the target render node.
 * @param x Pointer to receive x-coordinate value of the bounds's top-left corner (in pixels)
 * @param y Pointer to receive y-coordinate value of the bounds's top-left corner (in pixels)
 * @param width Pointer to receive width value in pixels.
 * @param height Pointer to receive height value in pixels.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_GetBounds(
    ArkUI_RenderNodeHandle node, int32_t* x, int32_t* y, int32_t* width, int32_t* height);

/**
 * @brief Set the draw region for a RenderNode.
 *
 * @param node Handle to the target render node.
 * @param x X-coordinate of the bounds's top-left corner (in pixels).
 * @param y Y-coordinate of the bounds's top-left corner (in pixels).
 * @param w Width of the bounds (in pixels).
 * @param h Height of the bounds (in pixels).
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_SetDrawRegion(ArkUI_RenderNodeHandle node, float x, float y, float w, float h);

/**
 * @brief Attach a content modifier for the render node.
 *
 * @param node Handle to the target render node.
 * @param modifier {@link ArkUI_RenderContentModifierHandle} The content modifier handle.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_AttachContentModifier(
    ArkUI_RenderNodeHandle node, ArkUI_RenderContentModifierHandle modifier);

/**
 * @brief Create a content modifier handle.
 *
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
ArkUI_RenderContentModifierHandle OH_ArkUI_RenderNodeUtils_CreateContentModifier();

/**
 * @brief Dispose the content modifier handle.
 *
 * @param modifier {@link ArkUI_RenderContentModifierHandle} Handle to the content modifier.
 * @since 20
 */
void OH_ArkUI_RenderNodeUtils_DisposeContentModifier(ArkUI_RenderContentModifierHandle modifier);

/**
 * @brief Attach a float property to the target content modifier.
 *
 * @param modifier {@link ArkUI_RenderContentModifierHandle} Set float property to the target content modifier.
 * @param property {@link ArkUI_FloatPropertyHandle} Handle to the float property.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_AttachFloatProperty(
    ArkUI_RenderContentModifierHandle modifier, ArkUI_FloatPropertyHandle property);

/**
 * @brief Attach a vector2 property to the target content modifier.
 *
 * @param modifier {@link ArkUI_RenderContentModifierHandle} Set vector2 property to the target content modifier.
 * @param property {@link ArkUI_Vector2PropertyHandle} Handle to the vector2 property.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_AttachVector2Property(
    ArkUI_RenderContentModifierHandle modifier, ArkUI_Vector2PropertyHandle property);

/**
 * @brief Attach a color property to the target content modifier.
 *
 * @param modifier {@link ArkUI_RenderContentModifierHandle} Set color property to the target content modifier.
 * @param property {@link ArkUI_ColorPropertyHandle} Handle to the color property.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_AttachColorProperty(
    ArkUI_RenderContentModifierHandle modifier, ArkUI_ColorPropertyHandle property);

/**
 * @brief Attach a float animatable property to the target content modifier.
 *
 * @param modifier {@link ArkUI_RenderContentModifierHandle} Set float animatable property to the target content
 *     modifier.
 * @param property {@link ArkUI_FloatAnimatablePropertyHandle} Handle to the float animatable property.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_AttachFloatAnimatableProperty(
    ArkUI_RenderContentModifierHandle modifier, ArkUI_FloatAnimatablePropertyHandle property);

/**
 * @brief Attach a vector2 animatable property to the target content modifier.
 *
 * @param modifier {@link ArkUI_RenderContentModifierHandle} Set vector2 animatable property to the target content
 *     modifier.
 * @param property {@link ArkUI_Vector2AnimatablePropertyHandle} Handle to the vector2 animatable property.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_AttachVector2AnimatableProperty(
    ArkUI_RenderContentModifierHandle modifier, ArkUI_Vector2AnimatablePropertyHandle property);

/**
 * @brief Attach a color property to the target content modifier.
 *
 * @param modifier {@link ArkUI_RenderContentModifierHandle} Set color animatable property to the target content
 *     modifier.
 * @param property {@link ArkUI_ColorAnimatablePropertyHandle} Handle to the color animatable property.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_AttachColorAnimatableProperty(
    ArkUI_RenderContentModifierHandle modifier, ArkUI_ColorAnimatablePropertyHandle property);

/**
 * @brief Create a float property handle.
 *
 * @param value The property value.
 * @return Handle to the float property.
 * @since 20
 */
ArkUI_FloatPropertyHandle OH_ArkUI_RenderNodeUtils_CreateFloatProperty(float value);

/**
 * @brief Set value of the float property.
 *
 * @param property {@link ArkUI_FloatPropertyHandle} The float property handle.
 * @param value The property value.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_SetFloatPropertyValue(ArkUI_FloatPropertyHandle property, float value);

/**
 * @brief Get value of the float property.
 *
 * @param property {@link ArkUI_FloatPropertyHandle} The float property handle.
 * @param value The pointer to receive property value.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_GetFloatPropertyValue(ArkUI_FloatPropertyHandle property, float* value);

/**
 * @brief Dispose the float property handle.
 *
 * @param property {@link ArkUI_FloatPropertyHandle} Handle to the float property.
 * @since 20
 */
void OH_ArkUI_RenderNodeUtils_DisposeFloatProperty(ArkUI_FloatPropertyHandle property);

/**
 * @brief Create a vector2 property handle.
 *
 * @param x X-coordinate value of the property.
 * @param y Y-coordinate value of the property.
 * @return Handle to the vector2 property.
 * @since 20
 */
ArkUI_Vector2PropertyHandle OH_ArkUI_RenderNodeUtils_CreateVector2Property(float x, float y);

/**
 * @brief Set value of the vector2 property.
 *
 * @param property {@link ArkUI_Vector2PropertyHandle} The vector2 property handle.
 * @param x X-coordinate value of the property.
 * @param y Y-coordinate value of the property.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_SetVector2PropertyValue(ArkUI_Vector2PropertyHandle property, float x, float y);

/**
 * @brief Get value of the vector2 property.
 *
 * @param property {@link ArkUI_Vector2PropertyHandle} The Vector2 property handle.
 * @param x The pointer to receive x-coordinate value of the property.
 * @param y The pointer to receive y-coordinate value of the property.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_GetVector2PropertyValue(ArkUI_Vector2PropertyHandle property, float* x, float* y);

/**
 * @brief Dispose the vector2 property handle.
 *
 * @param property {@link ArkUI_Vector2PropertyHandle} Handle to the vector2 property.
 * @since 20
 */
void OH_ArkUI_RenderNodeUtils_DisposeVector2Property(ArkUI_Vector2PropertyHandle property);

/**
 * @brief Create a color property handle.
 *
 * @param value The property value.
 * @return Handle to the color property.
 * @since 20
 */
ArkUI_ColorPropertyHandle OH_ArkUI_RenderNodeUtils_CreateColorProperty(uint32_t value);

/**
 * @brief Set value of the color property.
 *
 * @param property {@link ArkUI_ColorPropertyHandle} The color property handle.
 * @param value The property value.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_SetColorPropertyValue(ArkUI_ColorPropertyHandle property, uint32_t value);

/**
 * @brief Get value of the color property.
 *
 * @param property {@link ArkUI_ColorPropertyHandle} The color property handle.
 * @param value The pointer to receive property value.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_GetColorPropertyValue(ArkUI_ColorPropertyHandle property, uint32_t* value);

/**
 * @brief Dispose the color property handle.
 *
 * @param property {@link ArkUI_ColorPropertyHandle} Handle to the color property.
 * @since 20
 */
void OH_ArkUI_RenderNodeUtils_DisposeColorProperty(ArkUI_ColorPropertyHandle property);

/**
 * @brief Create a float animatable property handle.
 *
 * @param value The property value.
 * @return Handle to the float animatable property.
 * @since 20
 */
ArkUI_FloatAnimatablePropertyHandle OH_ArkUI_RenderNodeUtils_CreateFloatAnimatableProperty(float value);

/**
 * @brief Set value of the float animatable property.
 *
 * @param property {@link ArkUI_FloatAnimatablePropertyHandle} The float animatable property handle.
 * @param value The property value.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_SetFloatAnimatablePropertyValue(
    ArkUI_FloatAnimatablePropertyHandle property, float value);

/**
 * @brief Get value of the float animatable property.
 *
 * @param property {@link ArkUI_FloatAnimatablePropertyHandle} The float animatable property handle.
 * @param value The pointer to receive property value.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_GetFloatAnimatablePropertyValue(
    ArkUI_FloatAnimatablePropertyHandle property, float* value);

/**
 * @brief Dispose the float animatable property handle.
 *
 * @param property {@link ArkUI_FloatAnimatablePropertyHandle} Handle to the float animatable property.
 * @since 20
 */
void OH_ArkUI_RenderNodeUtils_DisposeFloatAnimatableProperty(ArkUI_FloatAnimatablePropertyHandle property);

/**
 * @brief Create a vector2 animatable property handle.
 *
 * @param x X-coordinate value of the property.
 * @param y Y-coordinate value of the property.
 * @return Handle to the vector2 animatable property.
 * @since 20
 */
ArkUI_Vector2AnimatablePropertyHandle OH_ArkUI_RenderNodeUtils_CreateVector2AnimatableProperty(float x, float y);

/**
 * @brief Set value of the vector2 animatable property.
 *
 * @param property {@link ArkUI_Vector2AnimatablePropertyHandle} The vector2 animatable property handle.
 * @param x X-coordinate value of the property.
 * @param y Y-coordinate value of the property.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_SetVector2AnimatablePropertyValue(
    ArkUI_Vector2AnimatablePropertyHandle property, float x, float y);

/**
 * @brief Get value of the vector2 animatable property.
 *
 * @param property {@link ArkUI_Vector2AnimatablePropertyHandle} The Vector2 animatable property handle.
 * @param x The pointer to receive x-coordinate value of the property.
 * @param y The pointer to receive y-coordinate value of the property.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_GetVector2AnimatablePropertyValue(
    ArkUI_Vector2AnimatablePropertyHandle property, float* x, float* y);

/**
 * @brief Dispose the vector2 animatable property handle.
 *
 * @param property {@link ArkUI_Vector2AnimatablePropertyHandle} Handle to the vector2 animatable property.
 * @since 20
 */
void OH_ArkUI_RenderNodeUtils_DisposeVector2AnimatableProperty(ArkUI_Vector2AnimatablePropertyHandle property);

/**
 * @brief Create a color animatable property handle.
 *
 * @param value The property value.
 * @return Handle to the color animatable property.
 * @since 20
 */
ArkUI_ColorAnimatablePropertyHandle OH_ArkUI_RenderNodeUtils_CreateColorAnimatableProperty(uint32_t value);

/**
 * @brief Set value of the color animatable property.
 *
 * @param property {@link ArkUI_ColorAnimatablePropertyHandle} The color animatable property handle.
 * @param value The property value.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_SetColorAnimatablePropertyValue(
    ArkUI_ColorAnimatablePropertyHandle property, uint32_t value);

/**
 * @brief Get value of the color animatable property.
 *
 * @param property {@link ArkUI_ColorAnimatablePropertyHandle} The color animatable property handle.
 * @param value The pointer to receive property value.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_GetColorAnimatablePropertyValue(
    ArkUI_ColorAnimatablePropertyHandle property, uint32_t* value);

/**
 * @brief Dispose the color animatable property handle.
 *
 * @param property {@link ArkUI_ColorAnimatablePropertyHandle} Handle to the color animatable property.
 * @since 20
 */
void OH_ArkUI_RenderNodeUtils_DisposeColorAnimatableProperty(ArkUI_ColorAnimatablePropertyHandle property);

/**
 * @brief Set the onDraw function of the content modifier.
 *
 * @param modifier The target content modifier handle.
 * @param userData Custom data to be passed to the callback.
 * @param callback The draw event receiver callback.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
int32_t OH_ArkUI_RenderNodeUtils_SetContentModifierOnDraw(ArkUI_RenderContentModifierHandle modifier, void* userData,
    void (*callback)(ArkUI_DrawContext* context, void* userData));

/**
 * @brief Create a RectShape option.
 *
 * @return A pointer to the RectShape option.
 * @since 20
 */
ArkUI_RectShapeOption* OH_ArkUI_RenderNodeUtils_CreateRectShapeOption();

/**
 * @brief Dispose the RectShape option.
 *
 * @param option Pointer to the RectShape option.
 * @since 20
 */
void OH_ArkUI_RenderNodeUtils_DisposeRectShapeOption(ArkUI_RectShapeOption* option);

/**
 * @brief Set the edge value of RectShape option.
 *
 * @param option Pointer to the RectShape option.
 * @param edgeValue The edge value of the RectShape.
 * @param direction {@Link ArkUI_EdgeDirection} The direction of the edge.
 * @since 20
 */
void OH_ArkUI_RenderNodeUtils_SetRectShapeOptionEdgeValue(
    ArkUI_RectShapeOption* option, float edgeValue, ArkUI_EdgeDirection direction);

/**
 * @brief Create a NodeBorderStyle option.
 *
 * @return A pointer to the NodeBorderStyle option.
 * @since 20
 */
ArkUI_NodeBorderStyleOption* OH_ArkUI_RenderNodeUtils_CreateNodeBorderStyleOption();

/**
 * @brief Dispose the NodeBorderStyle option.
 *
 * @param option Pointer to the NodeBorderStyle option.
 * @since 20
 */
void OH_ArkUI_RenderNodeUtils_DisposeNodeBorderStyleOption(ArkUI_NodeBorderStyleOption* option);

/**
 * @brief Set the edge value of NodeBorderStyle option.
 *
 * @param option Pointer to the NodeBorderStyle option.
 * @param edgeStyle {@Link ArkUI_BorderStyle} The edge border style value of the NodeBorderStyle option.
 * @param direction {@Link ArkUI_EdgeDirection} The direction of the edge.
 * @since 20
 */
void OH_ArkUI_RenderNodeUtils_SetNodeBorderStyleOptionEdgeStyle(
    ArkUI_NodeBorderStyleOption* option, ArkUI_BorderStyle edgeStyle, ArkUI_EdgeDirection direction);

/**
 * @brief Create a NodeBorderWidth option.
 *
 * @return A pointer to the NodeBorderWidth option.
 * @since 20
 */
ArkUI_NodeBorderWidthOption* OH_ArkUI_RenderNodeUtils_CreateNodeBorderWidthOption();

/**
 * @brief Dispose the NodeBorderWidth option.
 *
 * @param option Pointer to the NodeBorderWidth option.
 * @since 20
 */
void OH_ArkUI_RenderNodeUtils_DisposeNodeBorderWidthOption(ArkUI_NodeBorderWidthOption* option);

/**
 * @brief Set the edge value of NodeBorderWidth option.
 *
 * @param option Pointer to the NodeBorderWidth option.
 * @param edgeWidth The edge width value of the NodeBorderWidth option.
 * @param direction {@Link ArkUI_EdgeDirection} The direction of the edge.
 * @since 20
 */
void OH_ArkUI_RenderNodeUtils_SetNodeBorderWidthOptionEdgeWidth(
    ArkUI_NodeBorderWidthOption* option, float edgeWidth, ArkUI_EdgeDirection direction);

/**
 * @brief Create a NodeBorderColor option.
 *
 * @return A pointer to the NodeBorderColor option.
 * @since 20
 */
ArkUI_NodeBorderColorOption* OH_ArkUI_RenderNodeUtils_CreateNodeBorderColorOption();

/**
 * @brief Dispose the NodeBorderColor option.
 *
 * @param option Pointer to the NodeBorderColor option.
 * @since 20
 */
void OH_ArkUI_RenderNodeUtils_DisposeNodeBorderColorOption(ArkUI_NodeBorderColorOption* option);

/**
 * @brief Set the edge value of NodeBorderColor option.
 *
 * @param option Pointer to the NodeBorderColor option.
 * @param edgeColor The edge color value of the NodeBorderColor option.
 * @param direction {@Link ArkUI_EdgeDirection} The direction of the edge.
 * @since 20
 */
void OH_ArkUI_RenderNodeUtils_SetNodeBorderColorOptionEdgeColor(
    ArkUI_NodeBorderColorOption* option, uint32_t edgeColor, ArkUI_EdgeDirection direction);

/**
 * @brief Create a NodeBorderRadius option.
 *
 * @return A pointer to the NodeBorderRadius option.
 * @since 20
 */
ArkUI_NodeBorderRadiusOption* OH_ArkUI_RenderNodeUtils_CreateNodeBorderRadiusOption();

/**
 * @brief Dispose the NodeBorderRadius option.
 *
 * @param option Pointer to the NodeBorderRadius option.
 * @since 20
 */
void OH_ArkUI_RenderNodeUtils_DisposeNodeBorderRadiusOption(ArkUI_NodeBorderRadiusOption* option);

/**
 * @brief Set the corner value of NodeBorderRadius option.
 *
 * @param option Pointer to the NodeBorderRadius option.
 * @param cornerRadius The corner radius value of the NodeBorderRadius option.
 * @param direction {@Link ArkUI_CornerDirection} The direction of the corner.
 * @since 20
 */
void OH_ArkUI_RenderNodeUtils_SetNodeBorderRadiusOptionCornerRadius(
    ArkUI_NodeBorderRadiusOption* option, uint32_t cornerRadius, ArkUI_CornerDirection direction);

/**
 * @brief Create a CircleShape option.
 *
 * @return A pointer to the CircleShape option.
 * @since 20
 */
ArkUI_CircleShapeOption* OH_ArkUI_RenderNodeUtils_CreateCircleShapeOption();

/**
 * @brief Dispose the CircleShape option.
 *
 * @param option Pointer to the CircleShape option.
 * @since 20
 */
void OH_ArkUI_RenderNodeUtils_DisposeCircleShapeOption(ArkUI_CircleShapeOption* option);

/**
 * @brief Set the centerX value of CircleShape option.
 *
 * @param option Pointer to the CircleShape option.
 * @param centerX The centerX value.
 * @since 20
 */
void OH_ArkUI_RenderNodeUtils_SetCircleShapeOptionCenterX(ArkUI_CircleShapeOption* option, float centerX);

/**
 * @brief Set the centerY value of CircleShape option.
 *
 * @param option Pointer to the CircleShape option.
 * @param centerY The centerY value.
 * @since 20
 */
void OH_ArkUI_RenderNodeUtils_SetCircleShapeOptionCenterY(ArkUI_CircleShapeOption* option, float centerY);

/**
 * @brief Set the radius value of CircleShape option.
 *
 * @param option Pointer to the CircleShape option.
 * @param radius The radius value.
 * @since 20
 */
void OH_ArkUI_RenderNodeUtils_SetCircleShapeOptionRadius(ArkUI_CircleShapeOption* option, float radius);

/**
 * @brief Create a RoundRectShape option.
 *
 * @return A pointer to the RoundRectShape option.
 * @since 20
 */
ArkUI_RoundRectShapeOption* OH_ArkUI_RenderNodeUtils_CreateRoundRectShapeOption();

/**
 * @brief Dispose the RoundRectShape option.
 *
 * @param option Pointer to the RoundRectShape option.
 * @since 20
 */
void OH_ArkUI_RenderNodeUtils_DisposeRoundRectShapeOption(ArkUI_RoundRectShapeOption* option);

/**
 * @brief Set the edge value of RoundRectShape option.
 *
 * @param option Pointer to the RoundRectShape option.
 * @param edgeValue The edge value of the RoundRectShape.
 * @param direction {@Link ArkUI_EdgeDirection} The direction of the edge.
 * @since 20
 */
void OH_ArkUI_RenderNodeUtils_SetRoundRectShapeOptionEdgeValue(
    ArkUI_RoundRectShapeOption* option, float edgeValue, ArkUI_EdgeDirection direction);

/**
 * @brief Set the coordinate value of the target corner.
 *
 * @param option Pointer to the RoundRectShape option.
 * @param x X-coordinate of the target corner (in pixels).
 * @param y Y-coordinate of the target corner (in pixels).
 * @param direction {@Link ArkUI_CornerDirection} The direction of the corner.
 * @since 20
 */
void OH_ArkUI_RenderNodeUtils_SetRoundRectShapeOptionCornerXY(
    ArkUI_RoundRectShapeOption* option, float x, float y, ArkUI_CornerDirection direction);

/**
 * @brief Create a CommandPath option.
 *
 * @return A pointer to the CommandPath option.
 * @since 20
 */
ArkUI_CommandPathOption* OH_ArkUI_RenderNodeUtils_CreateCommandPathOption();

/**
 * @brief Dispose the CommandPath option.
 *
 * @param option Pointer to the CommandPath option.
 * @since 20
 */
void OH_ArkUI_RenderNodeUtils_DisposeCommandPathOption(ArkUI_CommandPathOption* option);

/**
 * @brief Set the commands value of CommandPath option.
 *
 * @param option Pointer to the CommandPath option.
 * @param commands The commands value.
 * @since 20
 */
void OH_ArkUI_RenderNodeUtils_SetCommandPathOptionCommands(ArkUI_CommandPathOption* option, char* commands);

/**
 * @brief Create a mask option from a RectShape option.
 *
 * @param shape {@link ArkUI_RectShapeOption} Pointer to the RectShape option.
 * @return A pointer to the RenderNodeMask option.
 * @since 20
 */
ArkUI_RenderNodeMaskOption* OH_ArkUI_RenderNodeUtils_CreateRenderNodeMaskOptionFromRectShape(
    ArkUI_RectShapeOption* shape);

/**
 * @brief Create a mask option from a RoundRectShape option.
 *
 * @param shape {@link ArkUI_RoundRectShapeOption} Pointer to the RoundRectShape option.
 * @return A pointer to the RenderNodeMask option.
 * @since 20
 */
ArkUI_RenderNodeMaskOption* OH_ArkUI_RenderNodeUtils_CreateRenderNodeMaskOptionFromRoundRectShape(
    ArkUI_RoundRectShapeOption* shape);

/**
 * @brief Create a mask option from a CircleShape option.
 *
 * @param shape {@link ArkUI_CircleShapeOption} Pointer to the CircleShape option.
 * @return A pointer to the RenderNodeMask option.
 * @since 20
 */
ArkUI_RenderNodeMaskOption* OH_ArkUI_RenderNodeUtils_CreateRenderNodeMaskOptionFromCircleShape(
    ArkUI_CircleShapeOption* shape);

/**
 * @brief Create a mask option from a OvalShape option.
 *
 * @param shape {@link ArkUI_RectShapeOption} Pointer to the OvalShape option.
 * @return A pointer to the RenderNodeMask option.
 * @since 20
 */
ArkUI_RenderNodeMaskOption* OH_ArkUI_RenderNodeUtils_CreateRenderNodeMaskOptionFromOvalShape(
    ArkUI_RectShapeOption* shape);

/**
 * @brief Create a mask option from a CommandPath option.
 *
 * @param path {@link ArkUI_CommandPathOption} Pointer to the CommandPath option.
 * @return A pointer to the RenderNodeMask option.
 * @since 20
 */
ArkUI_RenderNodeMaskOption* OH_ArkUI_RenderNodeUtils_CreateRenderNodeMaskOptionFromCommandPath(
    ArkUI_CommandPathOption* path);

/**
 * @brief Dispose the RenderNodeMask option.
 *
 * @param option Pointer to the RenderNodeMask option.
 * @since 20
 */
void OH_ArkUI_RenderNodeUtils_DisposeRenderNodeMaskOption(ArkUI_RenderNodeMaskOption* option);

/**
 * @brief Set the fill color of RenderNodeMask option.
 *
 * @param mask Pointer to the RenderNodeMask option.
 * @param fillColor The fill color of the mask.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
void OH_ArkUI_RenderNodeUtils_SetRenderNodeMaskOptionFillColor(ArkUI_RenderNodeMaskOption* mask, uint32_t fillColor);

/**
 * @brief Set the stroke color of RenderNodeMask option.
 *
 * @param mask Pointer to the RenderNodeMask option.
 * @param strokeColor The stroke color of the mask.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
void OH_ArkUI_RenderNodeUtils_SetRenderNodeMaskOptionStrokeColor(
    ArkUI_RenderNodeMaskOption* mask, uint32_t strokeColor);

/**
 * @brief Set the stroke width of RenderNodeMask option.
 *
 * @param mask Pointer to the RenderNodeMask option.
 * @param strokeWidth The stroke width of the mask.
 * @return Error code.
 *         {@link ARKUI_ERROR_CODE_NO_ERROR} Success.
 *         {@link ARKUI_ERROR_CODE_PARAM_INVALID} Function parameter exception.
 *         {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if CAPI init error.
 * @since 20
 */
void OH_ArkUI_RenderNodeUtils_SetRenderNodeMaskOptionStrokeWidth(ArkUI_RenderNodeMaskOption* mask, float strokeWidth);

/**
 * @brief Create a clip option from a RectShape option.
 *
 * @param shape {@link ArkUI_RectShapeOption} Pointer to the RectShape option.
 * @return A pointer to the RenderNodeClip option.
 * @since 20
 */
ArkUI_RenderNodeClipOption* OH_ArkUI_RenderNodeUtils_CreateRenderNodeClipOptionFromRectShape(
    ArkUI_RectShapeOption* shape);

/**
 * @brief Create a clip option from a RoundRectShape option.
 *
 * @param shape {@link ArkUI_RoundRectShapeOption} Pointer to the RoundRectShape option.
 * @return A pointer to the RenderNodeClip option.
 * @since 20
 */
ArkUI_RenderNodeClipOption* OH_ArkUI_RenderNodeUtils_CreateRenderNodeClipOptionFromRoundRectShape(
    ArkUI_RoundRectShapeOption* shape);

/**
 * @brief Create a clip option from a CircleShape option.
 *
 * @param shape {@link ArkUI_CircleShapeOption} Pointer to the CircleShape option.
 * @return A pointer to the RenderNodeClip option.
 * @since 20
 */
ArkUI_RenderNodeClipOption* OH_ArkUI_RenderNodeUtils_CreateRenderNodeClipOptionFromCircleShape(
    ArkUI_CircleShapeOption* shape);

/**
 * @brief Create a clip option from a OvalShape option.
 *
 * @param shape {@link ArkUI_RectShapeOption} Pointer to the OvalShape option.
 * @return A pointer to the RenderNodeClip option.
 * @since 20
 */
ArkUI_RenderNodeClipOption* OH_ArkUI_RenderNodeUtils_CreateRenderNodeClipOptionFromOvalShape(
    ArkUI_RectShapeOption* shape);

/**
 * @brief Create a clip option from a CommandPath option.
 *
 * @param path {@link ArkUI_CommandPathOption} Pointer to the CommandPath option.
 * @return A pointer to the RenderNodeClip option.
 * @since 20
 */
ArkUI_RenderNodeClipOption* OH_ArkUI_RenderNodeUtils_CreateRenderNodeClipOptionFromCommandPath(
    ArkUI_CommandPathOption* path);

/**
 * @brief Dispose the RenderNodeClip option.
 *
 * @param option Pointer to the RenderNodeClip option.
 * @since 20
 */
void OH_ArkUI_RenderNodeUtils_DisposeRenderNodeClipOption(ArkUI_RenderNodeClipOption* option);

#ifdef __cplusplus
};
#endif

#endif // ARKUI_NATIVE_RENDER_H
/** @} */
