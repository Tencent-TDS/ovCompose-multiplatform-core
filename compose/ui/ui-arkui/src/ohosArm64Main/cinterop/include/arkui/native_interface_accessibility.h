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
 * @addtogroup ArkUI_Accessibility
 * @{
 *
 * @brief Describes the native capabilities supported by ArkUI Accessibility, such as querying accessibility nodes and
 * reporting accessibility events.
 *
 * @since 13
 */

/**
 * @file native_interface_accessibility.h
 *
 * @brief Declares the APIs used to access the native Accessibility.
 *
 * @library libace_ndk.z.so
 * @syscap SystemCapability.ArkUI.ArkUI.Full
 * @kit ArkUI
 * @since 13
 */
#ifndef _NATIVE_INTERFACE_ACCESSIBILITY_H
#define _NATIVE_INTERFACE_ACCESSIBILITY_H

#ifdef __cplusplus
#include <cstdint>
#else
#include <stdint.h>
#endif

#ifdef __cplusplus
extern "C" {
#endif

/**
 * @brief Defines a struct for accessibility element information.
 *
 * @since 13
 */
typedef struct ArkUI_AccessibilityElementInfo ArkUI_AccessibilityElementInfo;

/**
 * @brief Defines a struct for accessibility event information.
 *
 * @since 13
 */
typedef struct ArkUI_AccessibilityEventInfo ArkUI_AccessibilityEventInfo;

/**
 * @brief Defines a struct for the local provider of accessibility.
 *
 * @since 13
 */
typedef struct ArkUI_AccessibilityProvider ArkUI_AccessibilityProvider;

/**
 * @brief Defines a struct for accessibility action arguments.
 *
 * @since 13
 */
typedef struct ArkUI_AccessibilityActionArguments ArkUI_AccessibilityActionArguments;

/**
 * @brief Defines an enum for accessibility action types.
 *
 * @since 13
 */
typedef enum {
    /** Invalid action. */
    ARKUI_ACCESSIBILITY_NATIVE_ACTION_TYPE_INVALID = 0,
    /** Response to a click. */
    ARKUI_ACCESSIBILITY_NATIVE_ACTION_TYPE_CLICK = 0x00000010,
    /** Response to a long click. */
    ARKUI_ACCESSIBILITY_NATIVE_ACTION_TYPE_LONG_CLICK = 0x00000020,
    /** Accessibility focus acquisition. */
    ARKUI_ACCESSIBILITY_NATIVE_ACTION_TYPE_GAIN_ACCESSIBILITY_FOCUS = 0x00000040,
    /** Accessibility focus clearance. */
    ARKUI_ACCESSIBILITY_NATIVE_ACTION_TYPE_CLEAR_ACCESSIBILITY_FOCUS = 0x00000080,
    /** Forward scroll action. */
    ARKUI_ACCESSIBILITY_NATIVE_ACTION_TYPE_SCROLL_FORWARD = 0x00000100,
    /** Backward scroll action. */
    ARKUI_ACCESSIBILITY_NATIVE_ACTION_TYPE_SCROLL_BACKWARD = 0x00000200,
    /** Copy action for text content. */
    ARKUI_ACCESSIBILITY_NATIVE_ACTION_TYPE_COPY = 0x00000400,
    /** Paste action for text content. */
    ARKUI_ACCESSIBILITY_NATIVE_ACTION_TYPE_PASTE = 0x00000800,
    /** Cut action for text content. */
    ARKUI_ACCESSIBILITY_NATIVE_ACTION_TYPE_CUT = 0x00001000,
    /** Text selection action, requiring the setting of <b>selectTextBegin</b>, <b>TextEnd</b>, and <b>TextInForward</b>
     *  parameters to select a text segment in the text box. */
    ARKUI_ACCESSIBILITY_NATIVE_ACTION_TYPE_SELECT_TEXT = 0x00002000,
    /** Text content setting action. */
    ARKUI_ACCESSIBILITY_NATIVE_ACTION_TYPE_SET_TEXT = 0x00004000,
    /** Cursor position setting action. */
    ARKUI_ACCESSIBILITY_NATIVE_ACTION_TYPE_SET_CURSOR_POSITION = 0x00100000,
    /** Support action for find next item in focus move operation
     *  @since 15
     */
    ARKUI_ACCESSIBILITY_NATIVE_ACTION_TYPE_NEXT_HTML_ITEM = 0x02000000,
    /** Support action for find previous item in focus move operation
     *  @since 15
     */
    ARKUI_ACCESSIBILITY_NATIVE_ACTION_TYPE_PREVIOUS_HTML_ITEM = 0x04000000,
} ArkUI_Accessibility_ActionType;

/**
 * @brief Defines an enum for accessibility event types.
 *
 * @since 13
 */
typedef enum {
    /** Invalid event. */
    ARKUI_ACCESSIBILITY_NATIVE_EVENT_TYPE_INVALID = 0,
    /** Click event, sent after the UI component responds. */
    ARKUI_ACCESSIBILITY_NATIVE_EVENT_TYPE_CLICKED = 0x00000001,
    /** Long click event, sent after the UI component responds. */
    ARKUI_ACCESSIBILITY_NATIVE_EVENT_TYPE_LONG_CLICKED = 0x00000002,
    /** Selection event, sent after the UI component responds. */
    ARKUI_ACCESSIBILITY_NATIVE_EVENT_TYPE_SELECTED = 0x00000004,
    /** Text update event, sent when text is updated. */
    ARKUI_ACCESSIBILITY_NATIVE_EVENT_TYPE_TEXT_UPDATE = 0x00000010,
    /** Page state update event, sent when the page transitions, switches, resizes, or moves. */
    ARKUI_ACCESSIBILITY_NATIVE_EVENT_TYPE_PAGE_STATE_UPDATE = 0x00000020,
    /** Page content update event, sent when the page content changes. */
    ARKUI_ACCESSIBILITY_NATIVE_EVENT_TYPE_PAGE_CONTENT_UPDATE = 0x00000800,
    /** Scrolled event, sent when a scrollable component experiences a scroll event. */
    ARKUI_ACCESSIBILITY_NATIVE_EVENT_TYPE_SCROLLED = 0x000001000,
    /** Accessibility focus event, sent after the UI component responds. */
    ARKUI_ACCESSIBILITY_NATIVE_EVENT_TYPE_ACCESSIBILITY_FOCUSED = 0x00008000,
    /** Accessibility focus cleared event, sent after the UI component responds. */
    ARKUI_ACCESSIBILITY_NATIVE_EVENT_TYPE_ACCESSIBILITY_FOCUS_CLEARED = 0x00010000,
    /** FOcus request for a specific node. */
    ARKUI_ACCESSIBILITY_NATIVE_EVENT_TYPE_REQUEST_ACCESSIBILITY_FOCUS = 0x02000000,
    /** Page open event reported by the UI component. */
    ARKUI_ACCESSIBILITY_NATIVE_EVENT_TYPE_PAGE_OPEN = 0x20000000,
    /** Page close event reported by the UI component. */
    ARKUI_ACCESSIBILITY_NATIVE_EVENT_TYPE_PAGE_CLOSE = 0x08000000,
    /** Announcement event, indicating a request to proactively announce specified content. */
    ARKUI_ACCESSIBILITY_NATIVE_EVENT_TYPE_ANNOUNCE_FOR_ACCESSIBILITY = 0x10000000,
    /** Focus update event, used for focus update scenarios. */
    ARKUI_ACCESSIBILITY_NATIVE_EVENT_TYPE_FOCUS_NODE_UPDATE = 0x10000001,
} ArkUI_AccessibilityEventType;

/**
 * @brief Defines a struct for the accessible action.
 *
 * @since 13
 */
typedef struct {
    /** Action type. */
    ArkUI_Accessibility_ActionType actionType;
    /** Action description. */
    const char* description;
} ArkUI_AccessibleAction;

/**
 * @brief Defines a struct for the accessible rectangle.
 *
 * @since 13
 */
typedef struct {
    /** X coordinate of the upper left corner. */
    int32_t leftTopX;
    /** Y coordinate of the upper left corner. */
    int32_t leftTopY;
    /** X coordinate of the lower right corner. */
    int32_t rightBottomX;
    /** Y coordinate of the lower right corner. */
    int32_t rightBottomY;
} ArkUI_AccessibleRect;

/**
 * @brief Define a struct for the accessible range information.
 *
 * @since 13
 */
typedef struct {
    /** Minimum value. */
    double min;
    /** Maximum value. */
    double max;
    /** Current value. */
    double current;
} ArkUI_AccessibleRangeInfo;

/**
 * @brief Defines a struct for the accessible grid information.
 *
 * @since 13
 */
typedef struct {
    /** Number of rows. */
    int32_t rowCount;
    /** Number of columns. */
    int32_t columnCount;
    /** Selection mode. The value <b>0</b> indicates that only one row can be selected. */
    int32_t selectionMode;
} ArkUI_AccessibleGridInfo;

/**
 * @brief Defines a struct for the accessible grid item information.
 *
 * @since 13
 */
typedef struct {
    /** Whether it is a header. */
    bool heading;
    /** Whether it is selected. */
    bool selected;
    /** Column index. */
    int32_t columnIndex;
    /** Row index. */
    int32_t rowIndex;
    /** Column span. */
    int32_t columnSpan;
    /** Row span. */
    int32_t rowSpan;
} ArkUI_AccessibleGridItemInfo;

/**
 * @brief Enumerates the accessibility error codes.
 *
 * @since 13
 */
typedef enum {
    /**
     * @error Success.
     */
    ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL = 0,
    /**
     * @error Failure.
     */
    ARKUI_ACCESSIBILITY_NATIVE_RESULT_FAILED = -1,
    /**
     * @error Invalid parameter.
     */
    ARKUI_ACCESSIBILITY_NATIVE_RESULT_BAD_PARAMETER = -2,
    /**
     * @error Out of memory.
     */
    ARKUI_ACCESSIBILITY_NATIVE_RESULT_OUT_OF_MEMORY = -3,
} ArkUI_AcessbilityErrorCode;

/**
 * @brief Defines an enum for the accessibility search modes.
 *
 * @since 13
 */
typedef enum {
    /** Search for current nodes. */
    ARKUI_ACCESSIBILITY_NATIVE_SEARCH_MODE_PREFETCH_CURRENT = 0,
    /** Search for parent nodes. */
    ARKUI_ACCESSIBILITY_NATIVE_SEARCH_MODE_PREFETCH_PREDECESSORS = 1 << 0,
    /** Search for sibling nodes. */
    ARKUI_ACCESSIBILITY_NATIVE_SEARCH_MODE_PREFETCH_SIBLINGS = 1 << 1,
    /** Search for child nodes at the next level. */
    ARKUI_ACCESSIBILITY_NATIVE_SEARCH_MODE_PREFETCH_CHILDREN = 1 << 2,
    /** Search for all child nodes. */
    ARKUI_ACCESSIBILITY_NATIVE_SEARCH_MODE_PREFETCH_RECURSIVE_CHILDREN = 1 << 3,
} ArkUI_AccessibilitySearchMode;

/**
 * @brief Defines an enum for the accessibility focus types.
 *
 * @since 13
 */
typedef enum {
    /** Invalid type. */
    ARKUI_ACCESSIBILITY_NATIVE_FOCUS_TYPE_INVALID = -1,
    /** Input focus type. */
    ARKUI_ACCESSIBILITY_NATIVE_FOCUS_TYPE_INPUT = 1 << 0,
    /** Accessibility focus type. */
    ARKUI_ACCESSIBILITY_NATIVE_FOCUS_TYPE_ACCESSIBILITY = 1 << 1,
} ArkUI_AccessibilityFocusType;

/**
 * @brief Enumerates the directions for moving the accessibility focus.
 *
 * @since 13
 */
typedef enum {
    /** Invalid direction. */
    ARKUI_ACCESSIBILITY_NATIVE_DIRECTION_INVALID = 0,
    /** Up. */
    ARKUI_ACCESSIBILITY_NATIVE_DIRECTION_UP = 0x00000001,
    /** Down. */
    ARKUI_ACCESSIBILITY_NATIVE_DIRECTION_DOWN = 0x00000002,
    /** Left. */
    ARKUI_ACCESSIBILITY_NATIVE_DIRECTION_LEFT = 0x00000004,
    /** Right. */
    ARKUI_ACCESSIBILITY_NATIVE_DIRECTION_RIGHT = 0x00000008,
    /** Forward. */
    ARKUI_ACCESSIBILITY_NATIVE_DIRECTION_FORWARD = 0x00000010,
    /** Backward. */
    ARKUI_ACCESSIBILITY_NATIVE_DIRECTION_BACKWARD = 0x00000020,
} ArkUI_AccessibilityFocusMoveDirection;

/**
 * @brief Defines a struct for the accessibility element information list.
 *
 * @since 13
 */
typedef struct ArkUI_AccessibilityElementInfoList ArkUI_AccessibilityElementInfoList;

/**
 * @brief Registers callbacks for the accessibility provider.
 *
 * @since 13
 */
typedef struct ArkUI_AccessibilityProviderCallbacks {
    /**
    * @brief Called to obtain element information based on a specified node.
    *
    * @param elementId Indicates the element ID.
    * @param mode Indicates accessibility search mode.
    * @param requestId Indicates the request ID.
    * @param elementList Indicates accessibility elementInfo list.
    * @return Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL} if the operation is successful.
    *         Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_BAD_PARAMETER} if a parameter is incorrect.
    */
    int32_t (*findAccessibilityNodeInfosById)(int64_t elementId, ArkUI_AccessibilitySearchMode mode,
        int32_t requestId, ArkUI_AccessibilityElementInfoList* elementList);
    /**
    * @brief Called to obtain element information based on a specified node and text content.
    *
    * @param elementId Indicates the element ID.
    * @param text Indicates accessibility text.
    * @param requestId Indicates the request ID.
    * @param elementList Indicates accessibility elementInfo list.
    * @return Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL} if the operation is successful.
    *         Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_BAD_PARAMETER} if a parameter is incorrect.
    */
    int32_t (*findAccessibilityNodeInfosByText)(int64_t elementId, const char* text, int32_t requestId,
        ArkUI_AccessibilityElementInfoList* elementList);
    /**
    * @brief Called to obtain focused element information based on a specified node.
    *
    * @param elementId Indicates the element ID.
    * @param focusType Indicates focus type.
    * @param requestId Indicates the request ID.
    * @param elementInfo Indicates accessibility elementInfo.
    * @return Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL} if the operation is successful.
    *         Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_BAD_PARAMETER} if a parameter is incorrect.
    */
    int32_t (*findFocusedAccessibilityNode)(int64_t elementId, ArkUI_AccessibilityFocusType focusType,
        int32_t requestId, ArkUI_AccessibilityElementInfo* elementInfo);
    /**
    * @brief Called to find the next focusable node based on the reference node.
    *
    * @param elementId Indicates the element ID.
    * @param direction Indicates direction.
    * @param requestId Indicates the request ID.
    * @param elementInfo Indicates accessibility elementInfo.
    * @return Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL} if the operation is successful.
    *         Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_BAD_PARAMETER} if a parameter is incorrect.
    */
    int32_t (*findNextFocusAccessibilityNode)(
        int64_t elementId, ArkUI_AccessibilityFocusMoveDirection direction,
        int32_t requestId, ArkUI_AccessibilityElementInfo* elementInfo);
    /**
    * @brief Called to execute a specified action on a specified node.
    *
    * @param elementId Indicates the element ID.
    * @param action Indicates action.
    * @param actionArguments Indicates action arguments.
    * @param requestId Indicates the request ID.
    * @return Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL} if the operation is successful.
    *         Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_BAD_PARAMETER} if a parameter is incorrect.
    */
    int32_t (*executeAccessibilityAction)(int64_t elementId, ArkUI_Accessibility_ActionType action,
        ArkUI_AccessibilityActionArguments *actionArguments, int32_t requestId);
    /**
    * @brief Called to clear the focus state of the current focused node.
    *
    * @return Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL} if the operation is successful.
    *         Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_FAILED} if the operation is failed.
    */
    int32_t (*clearFocusedFocusAccessibilityNode)();
    /**
    * @brief Called to query the current cursor position of the specified node.
    *
    * @param elementId Indicates the element ID.
    * @param requestId Indicates the request ID.
    * @param index Indicates index.
    * @return Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL} if the operation is successful.
    *         Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_BAD_PARAMETER} if a parameter is incorrect.
    */
    int32_t (*getAccessibilityNodeCursorPosition)(int64_t elementId, int32_t requestId, int32_t* index);
} ArkUI_AccessibilityProviderCallbacks;

/**
 * @brief Registers a callback for this <b>ArkUI_AccessibilityProvider</b> instance.
 *
 * @param provider Indicates the pointer to the <b>ArkUI_AccessibilityProvider</b> instance.
 * @param callbacks Indicates the pointer to the <b>GetAccessibilityNodeCursorPosition</b> callback.
 * @return Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL} if the operation is successful.
 *         Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_BAD_PARAMETER} if a parameter is incorrect.
 * @since 13
 */
int32_t OH_ArkUI_AccessibilityProviderRegisterCallback(
    ArkUI_AccessibilityProvider* provider, ArkUI_AccessibilityProviderCallbacks* callbacks);

/**
 * @brief Registers callbacks with instance for the accessibility provider.
 * @since 15
 */
typedef struct ArkUI_AccessibilityProviderCallbacksWithInstance {
    /**
    * @brief Called to obtain element information based on a specified node.
    * @param instanceId Indicates ID of third-party framework instance.
    * @param elementId The unique id of the component ID.
    * @param mode Indicates accessibility search mode.
    * @param requestId Matched the request and response. transfer it by callback only.
    * @param elementList The all obtained accessibility elements list information.
    * @return Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL} if the operation is successful.
    *         Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_BAD_PARAMETER} if a parameter is incorrect.
    */
    int32_t (*findAccessibilityNodeInfosById)(const char* instanceId, int64_t elementId,
        ArkUI_AccessibilitySearchMode mode, int32_t requestId, ArkUI_AccessibilityElementInfoList* elementList);
    /**
    * @brief Called to obtain element information based on a specified node and text content.
    * @param instanceId Indicates ID of third-party framework instance.
    * @param elementId The unique id of the component ID.
    * @param text Filter for the child components to matched with the text.
    * @param requestId Matched the request and response. transfer it by callback only.
    * @param elementList The all obtained accessibility elements list information.
    * @return Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL} if the operation is successful.
    *         Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_BAD_PARAMETER} if a parameter is incorrect.
    */
    int32_t (*findAccessibilityNodeInfosByText)(const char* instanceId, int64_t elementId, const char* text,
        int32_t requestId, ArkUI_AccessibilityElementInfoList* elementList);
    /**
    * @brief Called to obtain focused element information based on a specified node.
    * @param instanceId Indicates ID of third-party framework instance.
    * @param elementId The unique id of the component ID.
    * @param focusType Indicates focus type.
    * @param requestId Matched the request and response. transfer it by callback only.
    * @param elementInfo The all obtained accessibility elements list information.
    * @return Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL} if the operation is successful.
    *         Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_BAD_PARAMETER} if a parameter is incorrect.
    */
    int32_t (*findFocusedAccessibilityNode)(const char* instanceId, int64_t elementId,
        ArkUI_AccessibilityFocusType focusType, int32_t requestId, ArkUI_AccessibilityElementInfo* elementInfo);
    /**
    * @brief Called to find the next focusable node based on the reference node.
    * @param instanceId Indicates ID of third-party framework instance.
    * @param elementId The unique id of the component ID.
    * @param direction Indicates direction.
    * @param requestId Matched the request and response. transfer it by callback only.
    * @param elementInfo The all obtained accessibility elements list information.
    * @return Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL} if the operation is successful.
    *         Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_BAD_PARAMETER} if a parameter is incorrect.
    */
    int32_t (*findNextFocusAccessibilityNode)(
        const char* instanceId, int64_t elementId, ArkUI_AccessibilityFocusMoveDirection direction,
        int32_t requestId, ArkUI_AccessibilityElementInfo* elementInfo);
    /**
    * @brief Called to execute a specified action on a specified node.
    * @param instanceId Indicates ID of third-party framework instance.
    * @param elementId The unique id of the component ID.
    * @param action Indicates action.
    * @param actionArguments Indicates action arguments.
    * @param requestId Matched the request and response. transfer it by callback only.
    * @return Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL} if the operation is successful.
    *         Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_BAD_PARAMETER} if a parameter is incorrect.
    */
    int32_t (*executeAccessibilityAction)(const char* instanceId, int64_t elementId,
        ArkUI_Accessibility_ActionType action, ArkUI_AccessibilityActionArguments *actionArguments, int32_t requestId);
    /**
    * @brief Called to clear the focus state of the current focused node.
    * @param instanceId Indicates ID of third-party framework instance.
    * @return Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL} if the operation is successful.
    *         Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_FAILED} if the operation is failed.
    */
    int32_t (*clearFocusedFocusAccessibilityNode)(const char* instanceId);
    /**
    * @brief Called to query the current cursor position of the specified node.
    * @param instanceId Indicates ID of third-party framework instance.
    * @param elementId The unique id of the component ID.
    * @param requestId Matched the request and response. transfer it by callback only.
    * @param index Indicates index.
    * @return Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL} if the operation is successful.
    *         Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_BAD_PARAMETER} if a parameter is incorrect.
    */
    int32_t (*getAccessibilityNodeCursorPosition)(const char* instanceId, int64_t elementId,
        int32_t requestId, int32_t* index);
} ArkUI_AccessibilityProviderCallbacksWithInstance;

/**
 * @brief Registers a callback with instance for this <b>ArkUI_AccessibilityProvider</b> instance.
 * @param instanceId Indicates ID of third-party framework instance.
 * @param provider Indicates the pointer to the <b>ArkUI_AccessibilityProvider</b> instance.
 * @param callbacks Indicates the pointer to the <b>ArkUI_AccessibilityProviderCallbacksWithInstance</b> callback.
 * @return Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL} if the operation is successful.
 *         Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_BAD_PARAMETER} if a parameter is incorrect.
 * @since 15
 */
int32_t OH_ArkUI_AccessibilityProviderRegisterCallbackWithInstance(const char* instanceId,
    ArkUI_AccessibilityProvider* provider, ArkUI_AccessibilityProviderCallbacksWithInstance* callbacks);

/**
 * @brief Sends accessibility event information.
 *
 * @param provider Indicates the pointer to the <b>ArkUI_AccessibilityProvider</b> instance.
 * @param eventInfo Indicates the pointer to the accessibility event information.
 * @param callback Indicates the pointer to the callback that is called after the event is sent.
 * @since 13
 */
void OH_ArkUI_SendAccessibilityAsyncEvent(
    ArkUI_AccessibilityProvider* provider, ArkUI_AccessibilityEventInfo* eventInfo,
    void (*callback)(int32_t errorCode));

/**
 * @brief Adds and obtains the pointer to an <b>ArkUI_AccessibilityElementInfo</b> object.
 *
 * @param list Indicates the pointer to an <b>ArkUI_AccessibilityElementInfoList</b> object.
 * @return Returns the pointer to the <b>ArkUI_AccessibilityElementInfo</b> object.
 * @since 13
 */
ArkUI_AccessibilityElementInfo* OH_ArkUI_AddAndGetAccessibilityElementInfo(
    ArkUI_AccessibilityElementInfoList* list);

/**
* @brief Sets the element ID for an <b>ArkUI_AccessibilityElementInfo</b> object.
*
* @param elementInfo Indicates the pointer to an <b>ArkUI_AccessibilityElementInfo</b> object.
* @param elementId Indicates the element ID.
* @return Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL} if the operation is successful.
*         Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_BAD_PARAMETER} if a parameter is incorrect.
* @since 13
*/
int32_t OH_ArkUI_AccessibilityElementInfoSetElementId(
    ArkUI_AccessibilityElementInfo* elementInfo, int32_t elementId);

/**
* @brief Sets the parent ID for an <b>ArkUI_AccessibilityElementInfo</b> object.
*
* @param elementInfo Indicates the pointer to an <b>ArkUI_AccessibilityElementInfo</b> object.
* @param parentId Indicates the parent ID.
* @return Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL} if the operation is successful.
*         Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_BAD_PARAMETER} if a parameter is incorrect.
* @since 13
*/
int32_t OH_ArkUI_AccessibilityElementInfoSetParentId(
    ArkUI_AccessibilityElementInfo* elementInfo, int32_t parentId);

/**
* @brief Sets the component type for an <b>ArkUI_AccessibilityElementInfo</b> object.
*
* @param elementInfo Indicates the pointer to an <b>ArkUI_AccessibilityElementInfo</b> object.
* @param componentType Indicates the component type.
* @return Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL} if the operation is successful.
*         Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_BAD_PARAMETER} if a parameter is incorrect.
* @since 13
*/
int32_t OH_ArkUI_AccessibilityElementInfoSetComponentType(
    ArkUI_AccessibilityElementInfo* elementInfo, const char* componentType);

/**
* @brief Sets the component content for an <b>ArkUI_AccessibilityElementInfo</b> object.
*
* @param elementInfo Indicates the pointer to an <b>ArkUI_AccessibilityElementInfo</b> object.
* @param contents Indicates the component content.
* @return Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL} if the operation is successful.
*         Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_BAD_PARAMETER} if a parameter is incorrect.
* @since 13
*/
int32_t OH_ArkUI_AccessibilityElementInfoSetContents(
    ArkUI_AccessibilityElementInfo* elementInfo, const char* contents);

/**
* @brief Sets the hint text for an <b>ArkUI_AccessibilityElementInfo</b> object.
*
* @param elementInfo Indicates the pointer to an <b>ArkUI_AccessibilityElementInfo</b> object.
* @param hintText Indicates the hint text.
* @return Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL} if the operation is successful.
*         Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_BAD_PARAMETER} if a parameter is incorrect.
* @since 13
*/
int32_t OH_ArkUI_AccessibilityElementInfoSetHintText(
    ArkUI_AccessibilityElementInfo* elementInfo, const char* hintText);

/**
* @brief Sets the accessibility text for an <b>ArkUI_AccessibilityElementInfo</b> object.
*
* @param elementInfo Indicates the pointer to an <b>ArkUI_AccessibilityElementInfo</b> object.
* @param accessibilityText Indicates the accessibility text.
* @return Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL} if the operation is successful.
*         Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_BAD_PARAMETER} if a parameter is incorrect.
* @since 13
*/
int32_t OH_ArkUI_AccessibilityElementInfoSetAccessibilityText(
    ArkUI_AccessibilityElementInfo* elementInfo, const char* accessibilityText);

/**
* @brief Sets the accessibility description for an <b>ArkUI_AccessibilityElementInfo</b> object.
*
* @param elementInfo Indicates the pointer to an <b>ArkUI_AccessibilityElementInfo</b> object.
* @param accessibilityDescription Indicates the accessibility description.
* @return Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL} if the operation is successful.
*         Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_BAD_PARAMETER} if a parameter is incorrect.
* @since 13
*/
int32_t OH_ArkUI_AccessibilityElementInfoSetAccessibilityDescription(
    ArkUI_AccessibilityElementInfo* elementInfo, const char* accessibilityDescription);

/**
* @brief Set the number of child nodes and child node IDs for an <b>ArkUI_AccessibilityElementInfo</b> object.
*
* @param elementInfo Indicates the pointer to an <b>ArkUI_AccessibilityElementInfo</b> object.
* @param childCount Indicates the number of child nodes.
* @param childNodeIds Indicates an array of child node IDs.
* @return Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL} if the operation is successful.
*         Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_BAD_PARAMETER} if a parameter is incorrect.
* @since 13
*/
int32_t OH_ArkUI_AccessibilityElementInfoSetChildNodeIds(
    ArkUI_AccessibilityElementInfo* elementInfo, int32_t childCount, int64_t* childNodeIds);

/**
* @brief Sets the operation actions for an <b>ArkUI_AccessibilityElementInfo</b> object.
*
* @param elementInfo Indicates the pointer to an <b>ArkUI_AccessibilityElementInfo</b> object.
* @param operationCount Indicates the operation count.
* @param operationActions Indicates the operation actions.
* @return Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL} if the operation is successful.
*         Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_BAD_PARAMETER} if a parameter is incorrect.
* @since 13
*/
int32_t OH_ArkUI_AccessibilityElementInfoSetOperationActions(ArkUI_AccessibilityElementInfo* elementInfo,
    int32_t operationCount, ArkUI_AccessibleAction* operationActions);

/**
* @brief Sets the screen area for an <b>ArkUI_AccessibilityElementInfo</b> object.
*
* @param elementInfo Indicates the pointer to an <b>ArkUI_AccessibilityElementInfo</b> object.
* @param screenRect Indicates the screen area.
* @return Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL} if the operation is successful.
*         Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_BAD_PARAMETER} if a parameter is incorrect.
* @since 13
*/
int32_t OH_ArkUI_AccessibilityElementInfoSetScreenRect(
    ArkUI_AccessibilityElementInfo* elementInfo, ArkUI_AccessibleRect* screenRect);

/**
* @brief Sets whether the element is checkable for an <b>ArkUI_AccessibilityElementInfo</b> object.
*
* @param elementInfo Indicates the pointer to an <b>ArkUI_AccessibilityElementInfo</b> object.
* @param checkable Indicates whether the element is checkable.
* @return Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL} if the operation is successful.
*         Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_BAD_PARAMETER} if a parameter is incorrect.
* @since 13
*/
int32_t OH_ArkUI_AccessibilityElementInfoSetCheckable(
    ArkUI_AccessibilityElementInfo* elementInfo, bool checkable);

/**
* @brief Sets whether the element is checked for an <b>ArkUI_AccessibilityElementInfo</b> object.
*
* @param elementInfo Indicates the pointer to an <b>ArkUI_AccessibilityElementInfo</b> object.
* @param checked Indicates whether the element is checked.
* @return Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL} if the operation is successful.
*         Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_BAD_PARAMETER} if a parameter is incorrect.
* @since 13
*/
int32_t OH_ArkUI_AccessibilityElementInfoSetChecked(
    ArkUI_AccessibilityElementInfo* elementInfo, bool checked);

/**
* @brief Sets whether the element is focusable for an <b>ArkUI_AccessibilityElementInfo</b> object.
* @param elementInfo Indicates the pointer to an <b>ArkUI_AccessibilityElementInfo</b> object.
* @param focusable Indicates whether the element is focusable.
* @return Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL} if the operation is successful.
*         Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_BAD_PARAMETER} if a parameter is incorrect.
* @since 13
*/
int32_t OH_ArkUI_AccessibilityElementInfoSetFocusable(
    ArkUI_AccessibilityElementInfo* elementInfo, bool focusable);

/**
* @brief Sets whether the element is focused for an <b>ArkUI_AccessibilityElementInfo</b> object.
*
* @param elementInfo Indicates the pointer to an <b>ArkUI_AccessibilityElementInfo</b> object.
* @param isFocused Indicates whether the element is focused.
* @return Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL} if the operation is successful.
*         Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_BAD_PARAMETER} if a parameter is incorrect.
* @since 13
*/
int32_t OH_ArkUI_AccessibilityElementInfoSetFocused(
    ArkUI_AccessibilityElementInfo* elementInfo, bool isFocused);

/**
* @brief Sets whether the element is visible for an <b>ArkUI_AccessibilityElementInfo</b> object.
*
* @param elementInfo Indicates the pointer to an <b>ArkUI_AccessibilityElementInfo</b> object.
* @param isVisible Indicates whether the element is visible.
* @return Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL} if the operation is successful.
*         Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_BAD_PARAMETER} if a parameter is incorrect.
* @since 13
*/
int32_t OH_ArkUI_AccessibilityElementInfoSetVisible(
    ArkUI_AccessibilityElementInfo* elementInfo, bool isVisible);

/**
* @brief Sets the accessibility focus state for an <b>ArkUI_AccessibilityElementInfo</b> object.
*
* @param elementInfo Indicates the pointer to an <b>ArkUI_AccessibilityElementInfo</b> object.
* @param accessibilityFocused Indicates whether the element has accessibility focus.
* @return Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL} if the operation is successful.
*         Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_BAD_PARAMETER} if a parameter is incorrect.
* @since 13
*/
int32_t OH_ArkUI_AccessibilityElementInfoSetAccessibilityFocused(
    ArkUI_AccessibilityElementInfo* elementInfo, bool accessibilityFocused);

/**
* @brief Sets whether the element is selected for an <b>ArkUI_AccessibilityElementInfo</b> object.
*
* @param elementInfo Indicates the pointer to an <b>ArkUI_AccessibilityElementInfo</b> object.
* @param selected Indicates whether the element is selected.
* @return Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL} if the operation is successful.
*         Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_BAD_PARAMETER} if a parameter is incorrect.
* @since 13
*/
int32_t OH_ArkUI_AccessibilityElementInfoSetSelected(
    ArkUI_AccessibilityElementInfo* elementInfo, bool selected);

/**
* @brief Sets whether the element is clickable for an <b>ArkUI_AccessibilityElementInfo</b> object.
*
* @param elementInfo Indicates the pointer to an <b>ArkUI_AccessibilityElementInfo</b> object.
* @param clickable Indicates whether the element is clickable.
* @return Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL} if the operation is successful.
*         Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_BAD_PARAMETER} if a parameter is incorrect.
* @since 13
*/
int32_t OH_ArkUI_AccessibilityElementInfoSetClickable(
    ArkUI_AccessibilityElementInfo* elementInfo, bool clickable);

/**
* @brief Sets whether the element is long clickable for an <b>ArkUI_AccessibilityElementInfo</b> object.
*
* @param elementInfo Indicates the pointer to an <b>ArkUI_AccessibilityElementInfo</b> object.
* @param longClickable Indicates whether the element is long clickable.
* @return Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL} if the operation is successful.
*         Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_BAD_PARAMETER} if a parameter is incorrect.
* @since 13
*/
int32_t OH_ArkUI_AccessibilityElementInfoSetLongClickable(
    ArkUI_AccessibilityElementInfo* elementInfo, bool longClickable);

/**
* @brief Sets whether the element is enabled for an <b>ArkUI_AccessibilityElementInfo</b> object.
*
* @param elementInfo Indicates the pointer to an <b>ArkUI_AccessibilityElementInfo</b> object.
* @param isEnabled Indicates whether the element is enabled.
* @return Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL} if the operation is successful.
*         Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_BAD_PARAMETER} if a parameter is incorrect.
* @since 13
*/
int32_t OH_ArkUI_AccessibilityElementInfoSetEnabled(
    ArkUI_AccessibilityElementInfo* elementInfo, bool isEnabled);

/**
* @brief Sets whether the element is a password for an <b>ArkUI_AccessibilityElementInfo</b> object.
*
* @param elementInfo Indicates the pointer to an <b>ArkUI_AccessibilityElementInfo</b> object.
* @param isPassword Indicates whether the element is a password.
* @return Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL} if the operation is successful.
*         Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_BAD_PARAMETER} if a parameter is incorrect.
* @since 13
*/
int32_t OH_ArkUI_AccessibilityElementInfoSetIsPassword(
    ArkUI_AccessibilityElementInfo* elementInfo, bool isPassword);

/**
* @brief Sets whether the element is scrollable for an <b>ArkUI_AccessibilityElementInfo</b> object.
*
* @param elementInfo Indicates the pointer to an <b>ArkUI_AccessibilityElementInfo</b> object.
* @param scrollable Indicates whether the element is scrollable.
* @return Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL} if the operation is successful.
*         Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_BAD_PARAMETER} if a parameter is incorrect.
* @since 13
*/
int32_t OH_ArkUI_AccessibilityElementInfoSetScrollable(
    ArkUI_AccessibilityElementInfo* elementInfo, bool scrollable);

/**
* @brief Sets whether the element is editable for an <b>ArkUI_AccessibilityElementInfo</b> object.
*
* @param elementInfo Indicates the pointer to an <b>ArkUI_AccessibilityElementInfo</b> object.
* @param editable Indicates whether the element is editable.
* @return Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL} if the operation is successful.
*         Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_BAD_PARAMETER} if a parameter is incorrect.
* @since 13
*/
int32_t OH_ArkUI_AccessibilityElementInfoSetEditable(
    ArkUI_AccessibilityElementInfo* elementInfo, bool editable);

/**
* @brief Sets whether the element is a hint for an <b>ArkUI_AccessibilityElementInfo</b> object.
*
* @param elementInfo Indicates the pointer to an <b>ArkUI_AccessibilityElementInfo</b> object.
* @param isHint Indicates whether the element is a hint.
* @return Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL} if the operation is successful.
*         Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_BAD_PARAMETER} if a parameter is incorrect.
* @since 13
*/
int32_t OH_ArkUI_AccessibilityElementInfoSetIsHint(
    ArkUI_AccessibilityElementInfo* elementInfo, bool isHint);

/**
* @brief Sets the range information for an <b>ArkUI_AccessibilityElementInfo</b> object.
*
* @param elementInfo Indicates the pointer to an <b>ArkUI_AccessibilityElementInfo</b> object.
* @param rangeInfo Indicates the range information.
* @return Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL} if the operation is successful.
*         Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_BAD_PARAMETER} if a parameter is incorrect.
* @since 13
*/
int32_t OH_ArkUI_AccessibilityElementInfoSetRangeInfo(
    ArkUI_AccessibilityElementInfo* elementInfo, ArkUI_AccessibleRangeInfo* rangeInfo);

/**
* @brief Sets the grid information for an <b>ArkUI_AccessibilityElementInfo</b> object.
*
* @param elementInfo Indicates the pointer to an <b>ArkUI_AccessibilityElementInfo</b> object.
* @param gridInfo Indicates the grid information.
* @return Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL} if the operation is successful.
*         Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_BAD_PARAMETER} if a parameter is incorrect.
* @since 13
*/
int32_t OH_ArkUI_AccessibilityElementInfoSetGridInfo(
    ArkUI_AccessibilityElementInfo* elementInfo, ArkUI_AccessibleGridInfo* gridInfo);

/**
* @brief Sets the grid item for an <b>ArkUI_AccessibilityElementInfo</b> object.
*
* @param elementInfo Indicates the pointer to an <b>ArkUI_AccessibilityElementInfo</b> object.
* @param gridItem Indicates the grid item.
* @return Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL} if the operation is successful.
*         Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_BAD_PARAMETER} if a parameter is incorrect.
* @since 13
*/
int32_t OH_ArkUI_AccessibilityElementInfoSetGridItemInfo(
    ArkUI_AccessibilityElementInfo* elementInfo, ArkUI_AccessibleGridItemInfo* gridItem);

/**
* @brief Sets the starting index of the selected text for an <b>ArkUI_AccessibilityElementInfo</b> object.
*
* @param elementInfo Indicates the pointer to an <b>ArkUI_AccessibilityElementInfo</b> object.
* @param selectedTextStart Indicates the starting index of the selected text
* @return Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL} if the operation is successful.
*         Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_BAD_PARAMETER} if a parameter is incorrect.
* @since 13
*/
int32_t OH_ArkUI_AccessibilityElementInfoSetSelectedTextStart(
    ArkUI_AccessibilityElementInfo* elementInfo, int32_t selectedTextStart);

/**
* @brief Sets the end index of the selected text for an <b>ArkUI_AccessibilityElementInfo</b> object.
*
* @param elementInfo Indicates the pointer to an <b>ArkUI_AccessibilityElementInfo</b> object.
* @param selectedTextEnd Indicates the end index of the selected text
* @return Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL} if the operation is successful.
*         Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_BAD_PARAMETER} if a parameter is incorrect.
* @since 13
*/
int32_t OH_ArkUI_AccessibilityElementInfoSetSelectedTextEnd(
    ArkUI_AccessibilityElementInfo* elementInfo, int32_t selectedTextEnd);

/**
* @brief Sets the index of the currently selected item for an <b>ArkUI_AccessibilityElementInfo</b> object.
*
* @param elementInfo Indicates the pointer to an <b>ArkUI_AccessibilityElementInfo</b> object.
* @param currentItemIndex Indicates the index of the currently selected item.
* @return Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL} if the operation is successful.
*         Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_BAD_PARAMETER} if a parameter is incorrect.
* @since 13
*/
int32_t OH_ArkUI_AccessibilityElementInfoSetCurrentItemIndex(
    ArkUI_AccessibilityElementInfo* elementInfo, int32_t currentItemIndex);

/**
* @brief Sets the index of the first item for an <b>ArkUI_AccessibilityElementInfo</b> object.
*
* @param elementInfo Indicates the pointer to an <b>ArkUI_AccessibilityElementInfo</b> object.
* @param startItemIndex Indicates the index of the first item.
* @return Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL} if the operation is successful.
*         Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_BAD_PARAMETER} if a parameter is incorrect.
* @since 13
*/
int32_t OH_ArkUI_AccessibilityElementInfoSetStartItemIndex(
    ArkUI_AccessibilityElementInfo* elementInfo, int32_t startItemIndex);

/**
* @brief Sets the index of the last item for an <b>ArkUI_AccessibilityElementInfo</b> object.
*
* @param elementInfo Indicates the pointer to an <b>ArkUI_AccessibilityElementInfo</b> object.
* @param endItemIndex Indicates the index of the last item.
* @return Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL} if the operation is successful.
*         Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_BAD_PARAMETER} if a parameter is incorrect.
* @since 13
*/
int32_t OH_ArkUI_AccessibilityElementInfoSetEndItemIndex(
    ArkUI_AccessibilityElementInfo* elementInfo, int32_t endItemIndex);

/**
* @brief Sets the number of items for an <b>ArkUI_AccessibilityElementInfo</b> object.
*
* @param elementInfo Indicates the pointer to an <b>ArkUI_AccessibilityElementInfo</b> object.
* @param itemCount Indicates the number of items.
* @return Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL} if the operation is successful.
*         Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_BAD_PARAMETER} if a parameter is incorrect.
* @since 13
*/
int32_t OH_ArkUI_AccessibilityElementInfoSetItemCount(
    ArkUI_AccessibilityElementInfo* elementInfo, int32_t itemCount);

/**
* @brief Sets the offset for an <b>ArkUI_AccessibilityElementInfo</b> object.
*
* @param elementInfo Indicates the pointer to an <b>ArkUI_AccessibilityElementInfo</b> object.
* @param offset Indicates the scroll pixel offset relative to the top of the element.
* @return Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL} if the operation is successful.
*         Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_BAD_PARAMETER} if a parameter is incorrect.
* @since 13
*/
int32_t OH_ArkUI_AccessibilityElementInfoSetAccessibilityOffset(
    ArkUI_AccessibilityElementInfo* elementInfo, int32_t offset);

/**
* @brief Sets the accessibility group for an <b>ArkUI_AccessibilityElementInfo</b> object.
*
* @param elementInfo Indicates the pointer to an <b>ArkUI_AccessibilityElementInfo</b> object.
* @param accessibilityGroup Indicates the accessibility group.
* @return Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL} if the operation is successful.
*         Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_BAD_PARAMETER} if a parameter is incorrect.
* @since 13
*/
int32_t OH_ArkUI_AccessibilityElementInfoSetAccessibilityGroup(
    ArkUI_AccessibilityElementInfo* elementInfo, bool accessibilityGroup);

/**
* @brief Sets the accessibility level for an <b>ArkUI_AccessibilityElementInfo</b> object.
*
* @param elementInfo Indicates the pointer to an <b>ArkUI_AccessibilityElementInfo</b> object.
* @param accessibilityLevel Indicates the accessibility level.
* @return Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL} if the operation is successful.
*         Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_BAD_PARAMETER} if a parameter is incorrect.
* @since 13
*/
int32_t OH_ArkUI_AccessibilityElementInfoSetAccessibilityLevel(
    ArkUI_AccessibilityElementInfo* elementInfo, const char* accessibilityLevel);

/**
* @brief Sets the z-index for an <b>ArkUI_AccessibilityElementInfo</b> object.
*
* @param elementInfo Indicates the pointer to an <b>ArkUI_AccessibilityElementInfo</b> object.
* @param zIndex Indicates the z-index value.
* @return Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL} if the operation is successful.
*         Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_BAD_PARAMETER} if a parameter is incorrect.
* @since 13
*/
int32_t OH_ArkUI_AccessibilityElementInfoSetZIndex(
    ArkUI_AccessibilityElementInfo* elementInfo, int32_t zIndex);

/**
* @brief Sets the opacity for an <b>ArkUI_AccessibilityElementInfo</b> object.
*
* @param elementInfo Indicates the pointer to an <b>ArkUI_AccessibilityElementInfo</b> object.
* @param opacity Indicates the opacity.
* @return Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL} if the operation is successful.
*         Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_BAD_PARAMETER} if a parameter is incorrect.
* @since 13
*/
int32_t OH_ArkUI_AccessibilityElementInfoSetAccessibilityOpacity(
    ArkUI_AccessibilityElementInfo* elementInfo, float opacity);

/**
* @brief Sets the background color for an <b>ArkUI_AccessibilityElementInfo</b> object.
*
* @param elementInfo Indicates the pointer to an <b>ArkUI_AccessibilityElementInfo</b> object.
* @param backgroundColor Indicates the background color.
* @return Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL} if the operation is successful.
*         Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_BAD_PARAMETER} if a parameter is incorrect.
* @since 13
*/
int32_t OH_ArkUI_AccessibilityElementInfoSetBackgroundColor(
    ArkUI_AccessibilityElementInfo* elementInfo, const char* backgroundColor);

/**
* @brief Sets the background image for an <b>ArkUI_AccessibilityElementInfo</b> object.
*
* @param elementInfo Indicates the pointer to an <b>ArkUI_AccessibilityElementInfo</b> object.
* @param backgroundImage Indicates the backgroundImage.
* @return Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL} if the operation is successful.
*         Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_BAD_PARAMETER} if a parameter is incorrect.
* @since 13
*/
int32_t OH_ArkUI_AccessibilityElementInfoSetBackgroundImage(
    ArkUI_AccessibilityElementInfo* elementInfo, const char* backgroundImage);

/**
* @brief Sets the blur effect for an <b>ArkUI_AccessibilityElementInfo</b> object.
*
* @param elementInfo Indicates the pointer to an <b>ArkUI_AccessibilityElementInfo</b> object.
* @param blur Indicates the blur effect.
* @return Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL} if the operation is successful.
*         Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_BAD_PARAMETER} if a parameter is incorrect.
* @since 13
*/
int32_t OH_ArkUI_AccessibilityElementInfoSetBlur(
    ArkUI_AccessibilityElementInfo* elementInfo, const char* blur);

/**
* @brief Sets the hit test behavior for an <b>ArkUI_AccessibilityElementInfo</b> object.
*
* @param elementInfo Indicates the pointer to an <b>ArkUI_AccessibilityElementInfo</b> object.
* @param hitTestBehavior Indicates the hit test behavior.
* @return Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL} if the operation is successful.
*         Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_BAD_PARAMETER} if a parameter is incorrect.
* @since 13
*/
int32_t OH_ArkUI_AccessibilityElementInfoSetHitTestBehavior(
    ArkUI_AccessibilityElementInfo* elementInfo, const char* hitTestBehavior);

/**
 * @brief Creates an <b>ArkUI_AccessibilityElementInfo</b> object.
 *
 * @return Returns the <b>ArkUI_AccessibilityElementInfo</b> object, or NULL if it fails to create.
 *         The possible reason for failure is that the memory error occurred during object creation.
 * @since 13
 * @version 1.0
 */
ArkUI_AccessibilityElementInfo* OH_ArkUI_CreateAccessibilityElementInfo(void);

/**
 * @brief Destroys an <b>ArkUI_AccessibilityElementInfo</b> object.
 *
 * @param elementInfo Indicates the pointer to the <b>ArkUI_AccessibilityElementInfo</b> object to destroy.
 * @since 13
 * @version 1.0
 */
void OH_ArkUI_DestoryAccessibilityElementInfo(ArkUI_AccessibilityElementInfo* elementInfo);

/**
 * @brief Creates an <b>ArkUI_AccessibilityEventInfo</b> object.
 *
 * @return Returns the <b>ArkUI_AccessibilityEventInfo</b> object, or NULL if it fails to create.
 *         The possible reason for failure is that the memory error occurred during object creation.
 * @since 13
 */
ArkUI_AccessibilityEventInfo* OH_ArkUI_CreateAccessibilityEventInfo(void);

/**
 * @brief Destroys an <b>ArkUI_AccessibilityEventInfo</b> object.
 *
 * @param eventInfo Indicates the pointer to the <b>ArkUI_AccessibilityEventInfo</b> object to destroy.
 * @since 13
 */
void OH_ArkUI_DestoryAccessibilityEventInfo(ArkUI_AccessibilityEventInfo* eventInfo);

/**
* @brief Sets the event type for an <b>ArkUI_AccessibilityEventInfo</b> object.
*
* @param eventInfo Indicates the pointer to an <b>ArkUI_AccessibilityEventInfo</b> object.
* @param eventType Indicates the event type.
* @return Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL} if the operation is successful.
*         Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_BAD_PARAMETER} if a parameter is incorrect.
* @since 13
*/
int32_t OH_ArkUI_AccessibilityEventSetEventType(
    ArkUI_AccessibilityEventInfo* eventInfo,  ArkUI_AccessibilityEventType eventType);

/**
* @brief Sets the text announced for accessibility for an <b>ArkUI_AccessibilityEventInfo</b> object.
*
* @param eventInfo Indicates the pointer to an <b>ArkUI_AccessibilityEventInfo</b> object.
* @param textAnnouncedForAccessibility Indicates the text announced for accessibility.
* @return Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL} if the operation is successful.
*         Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_BAD_PARAMETER} if a parameter is incorrect.
* @since 13
*/
int32_t OH_ArkUI_AccessibilityEventSetTextAnnouncedForAccessibility(
    ArkUI_AccessibilityEventInfo* eventInfo,  const char* textAnnouncedForAccessibility);

/**
* @brief Sets the request focus ID for an <b>ArkUI_AccessibilityEventInfo</b> object.
*
* @param eventInfo Indicates the pointer to an <b>ArkUI_AccessibilityEventInfo</b> object.
* @param requestFocusId Indicates the request focus ID.
* @return Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL} if the operation is successful.
*         Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_BAD_PARAMETER} if a parameter is incorrect.
* @since 13
*/
int32_t OH_ArkUI_AccessibilityEventSetRequestFocusId(
    ArkUI_AccessibilityEventInfo* eventInfo,  int32_t requestFocusId);

/**
* @brief Sets the element information for an <b>ArkUI_AccessibilityEventInfo</b> object.
*
* @param eventInfo Indicates the pointer to an <b>ArkUI_AccessibilityEventInfo</b> object.
* @param elementInfo Indicates the pointer to an <b>ArkUI_AccessibilityElementInfo</b> object.
* @return Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL} if the operation is successful.
*         Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_BAD_PARAMETER} if a parameter is incorrect.
* @since 13
*/
int32_t OH_ArkUI_AccessibilityEventSetElementInfo(
    ArkUI_AccessibilityEventInfo* eventInfo,  ArkUI_AccessibilityElementInfo* elementInfo);

/**
* @brief Obtains the value of a key from an <b>ArkUI_AccessibilityActionArguments</b> object.
*
* @param arguments Indicates the pointer to an <b>ArkUI_AccessibilityActionArguments</b> object.
* @param key Indicates the key.
* @param value Indicates the value.
* @return Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL} if the operation is successful.
*         Returns {@link ARKUI_ACCESSIBILITY_NATIVE_RESULT_BAD_PARAMETER} if a parameter is incorrect.
* @since 13
*/
int32_t OH_ArkUI_FindAccessibilityActionArgumentByKey(
    ArkUI_AccessibilityActionArguments* arguments, const char* key, char** value);
#ifdef __cplusplus
};
#endif
#endif // _NATIVE_INTERFACE_ACCESSIBILITY_H
/** @} */