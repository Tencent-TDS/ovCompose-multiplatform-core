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
 * @brief Provides drag and drop APIs of ArkUI on the native side.
 *
 * @since 12
 */

/**
 * @file drag_and_drop.h
 *
 * @brief Defines the native drag and drop APIs.
 *
 * @library libace_ndk.z.so
 * @kit ArkUI
 * @syscap SystemCapability.ArkUI.ArkUI.Full
 * @since 12
 */

#ifndef ARKUI_NATIVE_DRAG_AND_DROP_H
#define ARKUI_NATIVE_DRAG_AND_DROP_H

#include <stdint.h>

#include "native_type.h"
#include "database/udmf/udmf.h"
#include "multimedia/image_framework/image/pixelmap_native.h"

#ifdef __cplusplus
extern "C" {
#endif

/**
 * @brief Defines an enum for drag results, which are set by the data receiver and transferred by the system to the
 *        drag source so that the drag source is aware of the data processing result of the receiver.
 *
 * @since 12
 */
typedef enum {
    /** The drag and drop operation succeeded. */
    ARKUI_DRAG_RESULT_SUCCESSFUL = 0,
    /** The drag and drop operation failed. */
    ARKUI_DRAG_RESULT_FAILED,
    /** The drag and drop operation was canceled. */
    ARKUI_DRAG_RESULT_CANCELED,
} ArkUI_DragResult;

/**
 * @brief Defines an enum for data processing modes used when data is dropped, which affects the display of the badge.
 *
 * @since 12
 */
typedef enum {
    /** Copy. */
    ARKUI_DROP_OPERATION_COPY = 0,
    /** Cut. */
    ARKUI_DROP_OPERATION_MOVE,
} ArkUI_DropOperation;

/**
 * @brief Defines an enum for interaction states prior to a drop and drop operation.
 *
 * @since 12
 */
typedef enum {
    /** Unknown. */
    ARKUI_PRE_DRAG_STATUS_UNKNOWN = -1,
    /** A drag gesture is being detected. */
    ARKUI_PRE_DRAG_STATUS_ACTION_DETECTING,
    /** The component is ready to be dragged. */
    ARKUI_PRE_DRAG_STATUS_READY_TO_TRIGGER_DRAG,
    /** A lift animation is started. */
    ARKUI_PRE_DRAG_STATUS_PREVIEW_LIFT_STARTED,
    /** A lift animation is finished. */
    ARKUI_PRE_DRAG_STATUS_PREVIEW_LIFT_FINISHED,
    /** A drop animation is started. */
    ARKUI_PRE_DRAG_STATUS_PREVIEW_LANDING_STARTED,
    /** A drop animation is finished. */
    ARKUI_PRE_DRAG_STATUS_PREVIEW_LANDING_FINISHED,
    /** A drop animation is terminated. */
    ARKUI_PRE_DRAG_STATUS_CANCELED_BEFORE_DRAG,
} ArkUI_PreDragStatus;

/**
 * @brief Defines an enum for drag preview scale modes.
 *
 * @since 12
 */
typedef enum {
    /**
     * The system automatically changes the position of the dragged point based on the scenario and
     * scales the drag preview based on set rules.
     */
    ARKUI_DRAG_PREVIEW_SCALE_AUTO = 0,
    /** The system does not scale the drag preview. */
    ARKUI_DRAG_PREVIEW_SCALE_DISABLED,
} ArkUI_DragPreviewScaleMode;

/**
 * @brief Defines an enum for drag states.
 *
 * @since 12
 */
typedef enum {
    /** Unknown. */
    ARKUI_DRAG_STATUS_UNKNOWN = -1,
    /** Started. */
    ARKUI_DRAG_STATUS_STARTED,
    /** Ended. */
    ARKUI_DRAG_STATUS_ENDED,
} ArkUI_DragStatus;

/**
 * @brief Defines a struct for a component event.
 *
 * @since 12
 */
typedef struct ArkUI_NodeEvent ArkUI_NodeEvent;

/**
 * @brief Defines a struct for a UI context object.
 *
 * @since 12
 */
typedef struct ArkUI_Context ArkUI_Context;

/**
 * @brief Defines a struct for a UI context object pointer.
 *
 * @since 12
 */
typedef struct ArkUI_Context* ArkUI_ContextHandle;

/**
 * @brief Defines a struct for a drag event.
 *
 * @since 12
 */
typedef struct ArkUI_DragEvent ArkUI_DragEvent;

/**
 * @brief Defines a struct for custom drag preview options.
 *
 * @since 12
 */
typedef struct ArkUI_DragPreviewOption ArkUI_DragPreviewOption;

/**
 * @brief Defines a struct for a drag action.
 *
 * @since 12
 */
typedef struct ArkUI_DragAction ArkUI_DragAction;

/**
 * @brief Defines a struct for drag and drop information returned through a drag status listener.
 *
 * @since 12
 */
typedef struct ArkUI_DragAndDropInfo ArkUI_DragAndDropInfo;

/**
 * @brief Obtains a <b>ArkUI_DragEvent</b> object from the specified <b>ArkUI_NodeEvent</b> object.
 *
 * @param nodeEvent Indicates the pointer to an <b>ArkUI_NodeEvent</b> object.
 * @return Returns the pointer to an <b>ArkUI_DragEvent</b> object.
 *         Returns <b>null</b> if the parameter passed in is invalid or is not a drag-related event.
 * @since 12
 */
ArkUI_DragEvent* OH_ArkUI_NodeEvent_GetDragEvent(ArkUI_NodeEvent* nodeEvent);

/**
 * @brief Obtains the interaction state prior to a drop and drop operation.
 *
 * @param nodeEvent Indicates the pointer to an <b>ArkUI_NodeEvent</b> object.
 * @return Returns the interaction state prior to the drop and drop operation.
 * @since 12
 */
ArkUI_PreDragStatus OH_ArkUI_NodeEvent_GetPreDragStatus(ArkUI_NodeEvent* nodeEvent);

/**
 * @brief Sets whether to disable the default drop animation.
 * The default drop animation is enabled by default and can be disabled to apply a custom drop animation.
 *
 * @param event Indicates the pointer to an <b>ArkUI_DragEvent</b> object.
 * @param disable Indicates whether to disable the default drop animation.
 * The value <b>true</b> means to disable the default drop animation, and <b>false</b> means the opposite.
 * @return Returns the result code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 12
 */
int32_t OH_ArkUI_DragEvent_DisableDefaultDropAnimation(ArkUI_DragEvent* event, bool disable);

/**
 * @brief Sets the data processing mode.
 *
 * @param event Indicates the pointer to an <b>ArkUI_DragEvent</b> object.
 * @param dropOperation Indicates the data processing mode.
 * @return Returns the result code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 12
 */
int32_t OH_ArkUI_DragEvent_SetSuggestedDropOperation(ArkUI_DragEvent* event, ArkUI_DropOperation dropOperation);

/**
 * @brief Sets the result for a drag event.
 *
 * @param event Indicates the pointer to an <b>ArkUI_DragEvent</b> object.
 * @param result Indicates the drag result.
 * @return Returns the result code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 12
 */
int32_t OH_ArkUI_DragEvent_SetDragResult(ArkUI_DragEvent* event, ArkUI_DragResult result);

/**
 * @brief Set drag data for a drag event.
 *
 * @param event Indicates the pointer to an <b>ArkUI_DragEvent</b> object.
 * @param data Indicates the drag data.
 * @return Returns the result code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 12
 */
int32_t OH_ArkUI_DragEvent_SetData(ArkUI_DragEvent* event, OH_UdmfData* data);

/**
 * @brief Obtains the default drag data from a drag event.
 *
 * @param event Indicates the pointer to an <b>ArkUI_DragEvent</b> object.
 * @param data Indicates the pointer to an <b>OH_UdmfData</b> object. The application needs to create a pointer
 *             for receiving data by using the {@link OH_UdmfData_Create} method.
 * @return Returns the result code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 12
 */
int32_t OH_ArkUI_DragEvent_GetUdmfData(ArkUI_DragEvent* event, OH_UdmfData *data);

/**
 * @brief Obtains the number of drag data types from a drag event.
 *
 * @param event Indicates the pointer to an <b>ArkUI_DragEvent</b> object.
 * @param count Indicates the number of drag data types returned.
 * @return Returns the result code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 12
 */
int32_t OH_ArkUI_DragEvent_GetDataTypeCount(ArkUI_DragEvent* event, int32_t* count);

/**
 * @brief Obtains the list of drag data types from a drag event.
 *
 * @param event Indicates the pointer to an <b>ArkUI_DragEvent</b> object.
 * @param eventTypeArray Indicates the list of the drag data types. You need to create a string array first.
 * @param length Indicates the total length of the list array. It must be greater than or equal to the number obtained
 *        by using {@link OH_ArkUI_DragEvent_GetDataTypeCount}.
 * @param maxStrLen Indicates the max string length of drag data types.
 * @return Returns the result code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 *         Returns {@link ARKUI_ERROR_CODE_BUFFER_SIZE_ERROR} if the giving buffer is not enough for string copy.
 * @since 12
 */
int32_t OH_ArkUI_DragEvent_GetDataTypes(
    ArkUI_DragEvent *event, char *eventTypeArray[], int32_t length, int32_t maxStrLen);

/**
 * @brief Obtains the drag result from a drag event.
 *
 * @param event Indicates the pointer to an <b>ArkUI_DragEvent</b> object.
 * @param result Indicates the drag result returned.
 * @return Returns the result code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 12
 */
int32_t OH_ArkUI_DragEvent_GetDragResult(ArkUI_DragEvent* event, ArkUI_DragResult* result);

/**
 * @brief Obtains the drop operation from a drag event.
 *
 * @param event Indicates the pointer to an <b>ArkUI_DragEvent</b> object.
 * @param operation Indicates the drop operation which the data receiver set.
 * @return Returns the result code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 *                 Possible causes: 1. The given parameters are null or the given event is not a valid DragEvent.
 * @since 12
 */
int32_t OH_ArkUI_DragEvent_GetDropOperation(ArkUI_DragEvent* event, ArkUI_DropOperation* operation);

/**
 * @brief Obtains the X coordinate of the touch point for a drag preview from a drag event.
 *
 * @param event Indicates the pointer to an <b>ArkUI_DragEvent</b> object.
 * @return Returns the X coordinate of the touch point, in px.
 *         Returns the default value <b>0</b> if the input parameter is invalid.
 * @since 12
 */
float OH_ArkUI_DragEvent_GetPreviewTouchPointX(ArkUI_DragEvent* event);

/**
 * @brief Obtains the Y coordinate of the touch point for a drag preview from a drag event.
 *
 * @param event Indicates the pointer to an <b>ArkUI_DragEvent</b> object.
 * @return Returns the Y coordinate of the touch point, in px.
 *         Returns the default value <b>0</b> if the input parameter is invalid.
 * @since 12
 */
float OH_ArkUI_DragEvent_GetPreviewTouchPointY(ArkUI_DragEvent* event);

/**
 * @brief Obtains the width of a drag preview from a drag event.
 *
 * @param event Indicates the pointer to an <b>ArkUI_DragEvent</b> object.
 * @return Returns the width of the drag preview, in px.
 *         Returns the default value <b>0</b> if the input parameter is invalid.
 * @since 12
 */
float OH_ArkUI_DragEvent_GetPreviewRectWidth(ArkUI_DragEvent* event);

/**
 * @brief Obtains the height of a drag preview from a drag event.
 *
 * @param event Indicates the pointer to an <b>ArkUI_DragEvent</b> object.
 * @return Returns the height of the drag preview, in px.
 *         Returns the default value <b>0</b> if the input parameter is invalid.
 * @since 12
 */
float OH_ArkUI_DragEvent_GetPreviewRectHeight(ArkUI_DragEvent* event);

/**
 * @brief Obtains the X coordinate of the touch point relative to the window from a drag event.
 *
 * @param event Indicates the pointer to an <b>ArkUI_DragEvent</b> object.
 * @return Returns the X coordinate of the touch point relative to the window, in px.
 *         Returns the default value <b>0</b> if the input parameter is invalid.
 * @since 12
 */
float OH_ArkUI_DragEvent_GetTouchPointXToWindow(ArkUI_DragEvent* event);

/**
 * @brief Obtains the Y coordinate of the touch point relative to the window from a drag event.
 *
 * @param event Indicates the pointer to an <b>ArkUI_DragEvent</b> object.
 * @return Returns the Y coordinate of the touch point relative to the window, in px.
 *         Returns the default value <b>0</b> if the input parameter is invalid.
 * @since 12
 */
float OH_ArkUI_DragEvent_GetTouchPointYToWindow(ArkUI_DragEvent* event);

/**
 * @brief Obtains the X coordinate of the touch point relative to the current display from a drag event.
 *
 * @param event Indicates the pointer to an <b>ArkUI_DragEvent</b> object.
 * @return Returns the X coordinate of the touch point relative to the current display, in px.
 *         Returns the default value <b>0</b> if the input parameter is invalid.
 * @since 12
 */
float OH_ArkUI_DragEvent_GetTouchPointXToDisplay(ArkUI_DragEvent* event);

/**
 * @brief Obtains the Y coordinate of the touch point relative to the current display from a drag event.
 *
 * @param event Indicates the pointer to an <b>ArkUI_DragEvent</b> object.
 * @return Returns the Y coordinate of the touch point relative to the current display, in px.
 *         Returns the default value <b>0</b> if the input parameter is invalid.
 * @since 12
 */
float OH_ArkUI_DragEvent_GetTouchPointYToDisplay(ArkUI_DragEvent* event);

/**
 * @brief Obtains the dragging velocity along the x-axis.
 *
 * @param event Indicates the pointer to an <b>ArkUI_DragEvent</b> object.
 * @return Returns the dragging velocity along the x-axis, in px.
 *         Returns the default value <b>0</b> if the input parameter is invalid.
 * @since 12
 */
float OH_ArkUI_DragEvent_GetVelocityX(ArkUI_DragEvent* event);

/**
 * @brief Obtains the dragging velocity along the y-axis.
 *
 * @param event Indicates the pointer to an <b>ArkUI_DragEvent</b> object.
 * @return Returns the dragging velocity along the y-axis, in px.
 *         Returns the default value <b>0</b> if the input parameter is invalid.
 * @since 12
 */
float OH_ArkUI_DragEvent_GetVelocityY(ArkUI_DragEvent* event);

/**
 * @brief Obtains the dragging velocity along the main axis.
 *
 * @param event Indicates the pointer to an <b>ArkUI_DragEvent</b> object.
 * @return Returns the dragging velocity along the main axis, in px.
 *         Returns the default value <b>0</b> if the input parameter is invalid.
 * @since 12
 */
float OH_ArkUI_DragEvent_GetVelocity(ArkUI_DragEvent* event);

/**
 * @brief Obtains the pressed status of modifier keys from a drag event.
 *
 * @param event Indicates the pointer to an <b>ArkUI_DragEvent</b> object.
 * @param keys Indicates the returned combination of modifier keys that are currently pressed.
 *             The application can determine the pressed modifier keys through bitwise operations.
 * @return Returns the result code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 12
 */
int32_t OH_ArkUI_DragEvent_GetModifierKeyStates(ArkUI_DragEvent* event, uint64_t* keys);

/**
 * @brief Sets whether to enable strict reporting on drag events.
 *        This feature is disabled by default, and you are advised to enable it.
 *        If this feature is disabled, the parent component is not notified when an item in it is dragged over its child
 *        component. If this feature is enabled, the component is notified of the dragged item's leaving, and the chil
 *        component to which the dragged item is dropped is notified of the item's entering. This configuration is
 *        related to a specific UI instance. You can pass in a specific component node on the current UI instance
 *        for association.
 *
 * @param node Indicates the pointer to a component node.
 * @param enabled Indicates whether to enable strict reporting on drag events.
 * @return Returns the result code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 12
 */
int32_t OH_ArkUI_SetDragEventStrictReportWithNode(ArkUI_NodeHandle node, bool enabled);

/**
 * @brief Sets whether to enable strict reporting on drag events.
 *        This feature is disabled by default, and you are advised to enable it.
 *        If this feature is disabled, the parent component is not notified when an item in it is dragged over its child
 *        component. If this feature is enabled, the component is notified of the dragged item's leaving, and the child
 *        component to which the dragged item is dropped is notified of the item's entering. This configuration is
 *        related to a specific UI instance. You can pass in a specific UI instance for association.
 *
 * @param uiContext Indicates the pointer to a UI instance.
 * @param enabled Indicates whether to enable strict reporting on drag events.
 * @return Returns the result code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 12
 */
int32_t OH_ArkUI_SetDragEventStrictReportWithContext(ArkUI_ContextHandle uiContext, bool enabled);

/**
 * @brief Sets the types of data that can be dropped to the specified component. This API resets the settings configured
 *        through {@link OH_ArkUI_DisallowNodeAnyDropDataTypes} and {@link OH_ArkUI_AllowNodeAllDropDataTypes}.
 *
 * @param node Indicates the pointer to a component node.
 * @param typesArray Indicates the array of types of data that can be dropped.
 * @param count Indicates length of an array.
 * @return Returns the result code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 12
 */
int32_t OH_ArkUI_SetNodeAllowedDropDataTypes(ArkUI_NodeHandle node, const char* typesArray[], int32_t count);

/**
 * @brief Configures the specified component to disallow any data types. This API resets the settings configured through
 *        {@link OH_ArkUI_SetNodeAllowedDropDataTypes}.
 *
 * @param node Indicates the pointer to a component node.
 * @return Returns the result code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 12
 */
int32_t OH_ArkUI_DisallowNodeAnyDropDataTypes(ArkUI_NodeHandle node);

/**
 * @brief Configures the specified component to allow any data types. This API resets the settings configured through
 *        {@link OH_ArkUI_SetNodeAllowedDropDataTypes}.
 *
 * @param node Indicates the pointer to a component node.
 * @return Returns the result code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 12
 */
int32_t OH_ArkUI_AllowNodeAllDropDataTypes(ArkUI_NodeHandle node);

/**
 * @brief Sets whether the specified component is draggable.
 *
 * @param node Indicates the pointer to a component node.
 * @param bool Indicates whether the component is draggable.
 * @return Returns the result code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 12
 */
int32_t OH_ArkUI_SetNodeDraggable(ArkUI_NodeHandle node, bool enabled);

/**
 * @brief Sets a custom drag preview for the specified component.
 *
 * @param node Indicates the pointer to a component node.
 * @param preview Indicates the custom drag preview, which is a pixel map.
 * @return Returns the result code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 12
 */
int32_t OH_ArkUI_SetNodeDragPreview(ArkUI_NodeHandle node, OH_PixelmapNative* preview);

/**
 * @brief Creates an <b>ArkUI_DragPreviewOption</b> object.
 *
 * @return Returns the created <b>ArkUI_DragPreviewOption</b> object.
 * @since 12
 */
ArkUI_DragPreviewOption* OH_ArkUI_CreateDragPreviewOption(void);

/**
 * @brief Disposes of a <b>ArkUI_DragPreviewOption</b> object.
 *
 * @param option Indicates the pointer to an <b>ArkUI_DragPreviewOption</b> object.
 * @since 12
 */
void OH_ArkUI_DragPreviewOption_Dispose(ArkUI_DragPreviewOption* option);

/**
 * @brief Sets the scale mode for an <b>ArkUI_DragPreviewOption</b> object.
 *
 * @param option Indicates the pointer to an <b>ArkUI_DragPreviewOption</b> object.
 * @param scaleMode Indicates the scale mode.
 * @return Returns the result code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 12
 */
int32_t OH_ArkUI_DragPreviewOption_SetScaleMode(ArkUI_DragPreviewOption* option, ArkUI_DragPreviewScaleMode scaleMode);

/**
 * @brief Sets whether to enable the shadow effect for an <b>ArkUI_DragPreviewOption</b> object.
 *        The shadow effect is enabled by default.
 *
 * @param option Indicates the pointer to an <b>ArkUI_DragPreviewOption</b> object.
 * @param enabled Indicates whether to enable the shadow effect.
 * @return Returns the result code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 12
 */
int32_t OH_ArkUI_DragPreviewOption_SetDefaultShadowEnabled(ArkUI_DragPreviewOption* option, bool enabled);

/**
 * @brief Sets whether to enable the rounded corner effect for an <b>ArkUI_DragPreviewOption</b> object.
 *        The rounded corner effect is enabled by default.
 *
 * @param option Indicates the pointer to an <b>ArkUI_DragPreviewOption</b> object.
 * @param enabled Indicates whether to enable the rounded corner effect.
 * @return Returns the result code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 12
 */
int32_t OH_ArkUI_DragPreviewOption_SetDefaultRadiusEnabled(ArkUI_DragPreviewOption* option, bool enabled);

/**
 * @brief Sets whether to enable the badge for an <b>ArkUI_DragPreviewOption</b> object.
 *        If this feature is enabled, a badge that contains the number of dragged items is displayed.
 *
 * @param option Indicates the pointer to an <b>ArkUI_DragPreviewOption</b> object.
 * @param enabled Indicates whether to enable badge.
 * @return Returns the result code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 12
 */
int32_t OH_ArkUI_DragPreviewOption_SetNumberBadgeEnabled(ArkUI_DragPreviewOption* option, bool enabled);

/**
 * @brief Sets the count on the badge.
 *        The settings will overwrite the value in the <b>SetDragPreviewNumberBadgeEnabled</b> API.
 *
 * @param option Indicates the pointer to an <b>ArkUI_DragPreviewOption</b> object.
 * @param forcedNumber Indicates the count on the badge.
 * @return Returns the result code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 12
 */
int32_t OH_ArkUI_DragPreviewOption_SetBadgeNumber(ArkUI_DragPreviewOption* option, uint32_t forcedNumber);

/**
 * @brief Sets whether to enable the default animation on a click or touch, it's not used in drag action.
 *
 * @param option Indicates the pointer to an <b>ArkUI_DragPreviewOption</b> object.
 * @param enabled Indicates whether to enable the default animation on a click or touch.
 * @return Returns the result code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 12
 */
int32_t OH_ArkUI_DragPreviewOption_SetDefaultAnimationBeforeLiftingEnabled(
    ArkUI_DragPreviewOption* option, bool enabled);
/**
 * @brief Sets an <b>ArkUI_DragPreviewOption</b> object for the specified component.
 *
 * @param node Indicates the pointer to a component node.
 * @param option Indicates the pointer to an <b>ArkUI_DragPreviewOption</b> object.
 * @return Returns the result code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 12
 */
int32_t OH_ArkUI_SetNodeDragPreviewOption(ArkUI_NodeHandle node, ArkUI_DragPreviewOption* option);

/**
 * @brief Creates a drag action object for a UI instance based on the specified component node of the current
 *        UI instance.
 *
 * @param node Indicates the pointer to a component node.
 * @return Returns the pointer to the created drag action object; returns null if the operation fails.
 * @since 12
 */
ArkUI_DragAction* OH_ArkUI_CreateDragActionWithNode(ArkUI_NodeHandle node);

/**
 * @brief Creates a drag action object for the specified UI instance.
 *
 * @param uiContext Indicates the pointer to a UI instance.
 * @return Returns the pointer to the created drag action object; returns null if the operation fails.
 * @since 12
 */
ArkUI_DragAction* OH_ArkUI_CreateDragActionWithContext(ArkUI_ContextHandle uiContext);

/**
 * @brief Disposes of a drag action object.
 *
 * @param dragAction Indicates the pointer to the target drag action object.
 * @since 12
 */
void OH_ArkUI_DragAction_Dispose(ArkUI_DragAction* dragAction);

/**
 * @brief Sets the pointer ID. If only one finger is operating on the screen, the pointer ID is 0.
 *        In general cases, you can set the pointer ID to 0.
 *
 * @param dragAction Indicates the pointer to the target drag action object.
 * @param pointer Indicates the pointer ID. The value ranges from 0 to 9.
 * @return Returns the result code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 12
 */
int32_t OH_ArkUI_DragAction_SetPointerId(ArkUI_DragAction* dragAction, int32_t pointer);

/**
 * @brief Sets the drag previews for a drag action.
 *
 * @param dragAction Indicates the pointer to the target drag action object.
 * @param pixelmapArray Indicates the array of the drag previews to set, which must be pixel maps.
 * @param size Indicates the size of the drag preview array.
 * @return Returns the result code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 12
 */
int32_t OH_ArkUI_DragAction_SetPixelMaps(
    ArkUI_DragAction* dragAction, OH_PixelmapNative* pixelmapArray[], int32_t size);

/**
 * @brief Sets the touch point relative to the upper left corner of the first drag preview (pixel map).
 *
 * @param dragAction Indicates the pointer to the target drag action object.
 * @param x Indicates the X coordinate of the touch point.
 * @return Returns the result code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 12
 */
int32_t OH_ArkUI_DragAction_SetTouchPointX(ArkUI_DragAction* dragAction, float x);

/**
 * @brief Sets the touch point relative to the upper left corner of the first drag preview (pixel map).
 *
 * @param dragAction Indicates the pointer to the target drag action object.
 * @param y Indicates the Y coordinate of the touch point.
 * @return Returns the result code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 12
 */
int32_t OH_ArkUI_DragAction_SetTouchPointY(ArkUI_DragAction* dragAction, float y);

/**
 * @brief Sets the drag data.
 *
 * @param dragAction Indicates the pointer to the target drag action object.
 * @param data Indicates the drag data.
 * @return Returns the result code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 12
 */
int32_t OH_ArkUI_DragAction_SetData(ArkUI_DragAction* dragAction, OH_UdmfData* data);

/**
 * @brief Sets an <b>ArkUI_DragPreviewOption</b> object for the specified drag action object.
 *
 * @param dragAction Indicates the pointer to the target drag action object.
 * @param option Indicates the pointer to an <b>ArkUI_DragPreviewOption</b> object.
 * @return Returns the result code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 12
 */
int32_t OH_ArkUI_DragAction_SetDragPreviewOption(ArkUI_DragAction* dragAction, ArkUI_DragPreviewOption* option);

/**
 * @brief Registers a drag status listener.
 *        This listener can be used to check whether the data is successfully  received and processed.
 *
 * @param dragAction Indicates the pointer to the target drag action object.
 * @param userData Indicates the custom user data.
 * @param listener
 * Indicates the listener to register. When the callback is invoked, the system returns a pointer to the drag status
 * object. The pointer is destroyed after the callback is complete and the application should not hold it anymore.
 * @return Returns the result code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 12
 */
int32_t OH_ArkUI_DragAction_RegisterStatusListener(ArkUI_DragAction* dragAction, void* userData,
    void(*listener)(ArkUI_DragAndDropInfo* dragAndDropInfo, void* userData));

/**
 * @brief Unregisters a drag status listener.
 *
 * @param dragAction Indicates the pointer to the target drag action object.
 * @since 12
 */
void OH_ArkUI_DragAction_UnregisterStatusListener(ArkUI_DragAction* dragAction);

/**
 * @brief Obtains the drag status of a drag action.
 *
 * @param dragAndDropInfo Indicates the drag and drop information returned by the drag status listener.
 * @return Returns an <b>ArkUI_DragStatus</b> object; returns <b>ArkUI_DRAG_STATUS_UNKNOWN</b> if an error occurs.
 * @since 12
 */
ArkUI_DragStatus OH_ArkUI_DragAndDropInfo_GetDragStatus(ArkUI_DragAndDropInfo* dragAndDropInfo);

/**
 * @brief Obtains a drag event based on the specified drag and drop information.
 *        The drag event can then be used to obtain the drag result and the drag behavior, please note
 *        other info is not included in such a drag event.
 *
 * @param dragAndDropInfo Indicates the drag and drop information returned by the drag status listener.
 * @return Returns an <b>ArkUI_DragEvent</b> object; returns null if an error occurs.
 * @since 12
 */
ArkUI_DragEvent* OH_ArkUI_DragAndDropInfo_GetDragEvent(ArkUI_DragAndDropInfo* dragAndDropInfo);

/**
 * @brief Initiates a drag action through the specified drag action object.
 *
 * @param dragAction Indicates a drag action object.
 * @return Returns the result code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 12
 */
int32_t OH_ArkUI_StartDrag(ArkUI_DragAction* dragAction);

#ifdef __cplusplus
};
#endif

#endif // ARKUI_NATIVE_DRAG_AND_DROP_H
/** @} */