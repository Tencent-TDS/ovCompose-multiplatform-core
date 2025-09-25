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
 * @file native_dialog.h
 *
 * @brief Defines a set of custom dialog box APIs of ArkUI on the native side.
 *
 * @library libace_ndk.z.so
 * @syscap SystemCapability.ArkUI.ArkUI.Full
 * @kit ArkUI
 * @since 12
 */

#ifndef ARKUI_NATIVE_DIALOG_H
#define ARKUI_NATIVE_DIALOG_H

#include <stdbool.h>
#include "native_type.h"
#include "native_node.h"

#ifdef __cplusplus
extern "C" {
#endif

/**
* @brief Enumerates the actions for triggering closure of the dialog box.
*
* @since 12
*/
typedef enum {
    /** Touching the system-defined Back button or pressing the Esc key. */
    DIALOG_DISMISS_BACK_PRESS = 0,
    /** Touching the mask. */
    DIALOG_DISMISS_TOUCH_OUTSIDE,
    /** Touching the Close button. */
    DIALOG_DISMISS_CLOSE_BUTTON,
    /** Sliding down. */
    DIALOG_DISMISS_SLIDE_DOWN,
} ArkUI_DismissReason;

/**
* @brief Enumerates the state of dialog.
*
* @syscap SystemCapability.ArkUI.ArkUI.Full
*
* @since 20
*/
typedef enum {
    /**
     * @brief Uninitialized.
     * @syscap SystemCapability.ArkUI.ArkUI.Full
     * @since 20
     */
    DIALOG_UNINITIALIZED = 0,
    /**
     * @brief Initialized.
     * @syscap SystemCapability.ArkUI.ArkUI.Full
     * @since 20
     */
    DIALOG_INITIALIZED,
    /**
     * @brief Appearing.
     * @syscap SystemCapability.ArkUI.ArkUI.Full
     * @since 20
     */
    DIALOG_APPEARING,
    /**
     * @brief Appeared.
     * @syscap SystemCapability.ArkUI.ArkUI.Full
     * @since 20
     */
    DIALOG_APPEARED,
    /**
     * @brief Disappearing.
     * @syscap SystemCapability.ArkUI.ArkUI.Full
     * @since 20
     */
    DIALOG_DISAPPEARING,
    /**
     * @brief Disappeared.
     * @syscap SystemCapability.ArkUI.ArkUI.Full
     * @since 20
     */
    DIALOG_DISAPPEARED,
} ArkUI_DialogState;

/**
* @brief Enumerates the level mode.
*
* @since 15
*/
typedef enum {
    /** overlay mode. */
    ARKUI_LEVEL_MODE_OVERLAY = 0,
    /** embedded mode. */
    ARKUI_LEVEL_MODE_EMBEDDED,
} ArkUI_LevelMode;

/**
* @brief Enumerates the immersive mode.
*
* @since 15
*/
typedef enum {
    /** Mask covering the parent node area. */
    ARKUI_IMMERSIVE_MODE_DEFAULT = 0,
    /** Mask extend safe area includes status bar and navigation bar. */
    ARKUI_IMMERSIVE_MODE_EXTEND,
} ArkUI_ImmersiveMode;

/**
* @brief Invoked when the dialog box is closed.
*
* @since 12
*/
typedef bool (*ArkUI_OnWillDismissEvent)(int32_t reason);

/**
 * @brief Defines a struct for a dialog box dismiss event.
 *
 * @since 12
 */
typedef struct ArkUI_DialogDismissEvent ArkUI_DialogDismissEvent;

/**
 * @brief Defines a struct for the content object of a custom dialog box.
 *
 * @since 19
 */
typedef struct ArkUI_CustomDialogOptions ArkUI_CustomDialogOptions;

/**
 * @brief Provides the custom dialog box APIs for the native side.
 *
 * @version 1
 * @since 12
 */
typedef struct {
    /**
    * @brief Creates a custom dialog box and returns the pointer to the created dialog box.
    *
    * @note This method must be called before the <b>show</b> method.
    * @return Returns the pointer to the created custom dialog box; returns a null pointer if the creation fails.
    */
    ArkUI_NativeDialogHandle (*create)();
    /**
    * @brief Destroys a custom dialog box.
    *
    * @param handle Indicates the pointer to the custom dialog box controller.
    */
    void (*dispose)(ArkUI_NativeDialogHandle handle);
    /**
    * @brief Attaches the content of a custom dialog box.
    *
    * @note This method must be called before the <b>show</b> method.
    * @param handle Indicates the pointer to the custom dialog box controller.
    * @param content Indicates the pointer to the root node of the custom dialog box content.
    * @return Returns the error code.
    *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
    *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
    */
    int32_t (*setContent)(ArkUI_NativeDialogHandle handle, ArkUI_NodeHandle content);
    /**
    * @brief Detaches the content of a custom dialog box.
    *
    * @note This method must be called before the <b>show</b> method.
    * @param handle Indicates the pointer to the custom dialog box controller.
    * @return Returns the error code.
    *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
    *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
    */
    int32_t (*removeContent)(ArkUI_NativeDialogHandle handle);
    /**
    * @brief Sets the alignment mode for a custom dialog box.
    *
    * @note This method must be called before the <b>show</b> method.
    * @param handle Indicates the pointer to the custom dialog box controller.
    * @param alignment Indicates the alignment mode. The parameter type is {@link ArkUI_Alignment}.
    * @param offsetX Indicates the horizontal offset of the custom dialog box. The value is a floating point number.
    * @param offsetY Indicates the vertical offset of the custom dialog box. The value is a floating point number.
    * @return Returns the error code.
    *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
    *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
    */
    int32_t (*setContentAlignment)(ArkUI_NativeDialogHandle handle, int32_t alignment, float offsetX, float offsetY);
    /**
    * @brief Resets the alignment mode of a custom dialog box to its default settings.
    *
    * @note This method must be called before the <b>show</b> method.
    * @param handle Indicates the pointer to the custom dialog box controller.
    * @return Returns the error code.
    *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
    *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
    */
    int32_t (*resetContentAlignment)(ArkUI_NativeDialogHandle handle);
    /**
    * @brief Sets the modal mode for a custom dialog box.
    *
    * @note This method must be called before the <b>show</b> method.
    * @param handle Indicates the pointer to the custom dialog box controller.
    * @param isModal Specifies whether the custom dialog box is a modal, which has a mask applied. The value
    * <b>true</b> means that the custom dialog box is a modal, and <b>false</b> means the opposite.
    * @return Returns the error code.
    *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
    *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
    */
    int32_t (*setModalMode)(ArkUI_NativeDialogHandle handle, bool isModal);
    /**
    * @brief Specifies whether to allow users to touch the mask to dismiss the custom dialog box.
    *
    * @note This method must be called before the <b>show</b> method.
    * @param handle Indicates the pointer to the custom dialog box controller.
    * @param autoCancel Specifies whether to allow users to touch the mask to dismiss the dialog box.
    * The value <b>true</b> means to allow users to do so, and <b>false</b> means the opposite.
    * @return Returns the error code.
    *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
    *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
    */
    int32_t (*setAutoCancel)(ArkUI_NativeDialogHandle handle, bool autoCancel);
    /**
    * @brief Sets the mask for a custom dialog box.
    *
    * @note This method must be called before the <b>show</b> method.
    * @param handle Indicates the pointer to the custom dialog box controller.
    * @param maskColor Indicates the mask color, in 0xARGB format.
    * @param maskRect Indicates the pointer to the mask area. Events outside the mask area are transparently
    * transmitted, and events within the mask area are not. The parameter type is {@link ArkUI_Rect}.
    * @return Returns the error code.
    *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
    *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
    */
    int32_t (*setMask)(ArkUI_NativeDialogHandle handle, uint32_t maskColor, const ArkUI_Rect* maskRect);
    /**
    * @brief Sets the background color for a custom dialog box.
    *
    * @note This method must be called before the <b>show</b> method.
    * @param handle Indicates the pointer to the custom dialog box controller.
    * @param backgroundColor Indicates the background color of the custom dialog box, in 0xARGB format.
    * @return Returns the error code.
    *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
    *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
    */
    int32_t (*setBackgroundColor)(ArkUI_NativeDialogHandle handle, uint32_t backgroundColor);
    /**
    * @brief Sets the background corner radius for a custom dialog box.
    *
    * @note This method must be called before the <b>show</b> method.
    * @param handle Indicates the pointer to the custom dialog box controller.
    * @param topLeft Indicates the radius of the upper left corner of the custom dialog box background.
    * @param topRight Indicates the radius of the upper right corner of the custom dialog box background.
    * @param bottomLeft Indicates the radius of the lower left corner of the custom dialog box background.
    * @param bottomRight Indicates the radius of the lower right corner of the custom dialog box background.
    * @return Returns the error code.
    *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
    *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
    */
    int32_t (*setCornerRadius)(ArkUI_NativeDialogHandle handle, float topLeft, float topRight,
        float bottomLeft, float bottomRight);
    /**
    * @brief Sets the number of grid columns occupied by a custom dialog box.
    *
    * @note This method must be called before the <b>show</b> method.
    * @param handle Indicates the pointer to the custom dialog box controller.
    * @param gridCount Indicates the number of grid columns occupied by the dialog box. The default value is subject to
    * the window size, and the maximum value is the maximum number of columns supported by the system.
    * @return Returns the error code.
    *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
    *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
    */
    int32_t (*setGridColumnCount)(ArkUI_NativeDialogHandle handle, int32_t gridCount);
    /**
    * @brief Specifies whether to use a custom style for the custom dialog box.
    *
    * @note This method must be called before the <b>show</b> method.
    * @param handle Indicates the pointer to the custom dialog box controller.
    * @param enableCustomStyle Specifies whether to use a custom style for the dialog box.
    * <b>true</b>: The dialog box automatically adapts its width to the child components; the rounded corner is 0;
    * the background color is transparent.
    * <b>false</b>: The dialog box automatically adapts its width to the grid system and its height to the child
    * components; the rounded corner is 24 vp.
    * @return Returns the error code.
    *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
    *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
    */
    int32_t (*enableCustomStyle)(ArkUI_NativeDialogHandle handle, bool enableCustomStyle);
    /**
    * @brief Specifies whether to use a custom animation for a custom dialog box.
    *
    * @note This method must be called before the <b>show</b> method.
    * @param handle Indicates the pointer to the custom dialog box controller.
    * @param enableCustomAnimation Specifies whether to use a custom animation. The value <b>true</b> means to use a
    * custom animation, and <b>false</b> means to use the default animation.
    * @return Returns the error code.
    *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
    *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
    */
    int32_t (*enableCustomAnimation)(ArkUI_NativeDialogHandle handle, bool enableCustomAnimation);
    /**
    * @brief Registers a callback for a custom dialog box so that the user can decide whether to close the dialog box
    * after they touch the Back button or press the Esc key.
    *
    * @note This method must be called before the <b>show</b> method.
    * @param handle Indicates the pointer to the custom dialog box controller.
    * @param eventHandler Indicates the callback to register. The parameter type is {@link ArkUI_OnWillDismissEvent}.
    * @return Returns the error code.
    *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
    *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
    */
    int32_t (*registerOnWillDismiss)(ArkUI_NativeDialogHandle handle, ArkUI_OnWillDismissEvent eventHandler);
    /**
    * @brief Shows a custom dialog box.
    *
    * @param handle Indicates the pointer to the custom dialog box controller.
    * @param showInSubWindow Specifies whether to show the dialog box in a sub-window.
    * @return Returns the error code.
    *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
    *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
    */
    int32_t (*show)(ArkUI_NativeDialogHandle handle, bool showInSubWindow);
    /**
    * @brief Closes a custom dialog box. If the dialog box has been closed, this API does not take effect.
    *
    * @param handle Indicates the pointer to the custom dialog box controller.
    * @return Returns the error code.
    *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
    *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
    */
    int32_t (*close)(ArkUI_NativeDialogHandle handle);
    /**
    * @brief Registers a listener for the dismiss event of the custom dialog box.
    *
    * @param handle Indicates the pointer to the custom dialog box controller.
    * @param userData Indicates the pointer to the custom data.
    * @param callback Indicates the callback for the dismiss event of the custom dialog box.
    * @return Returns the result code.
    *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
    *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
    */
    int32_t (*registerOnWillDismissWithUserData)(
        ArkUI_NativeDialogHandle handle, void* userData, void (*callback)(ArkUI_DialogDismissEvent* event));
} ArkUI_NativeDialogAPI_1;

/**
 * @brief Provides the custom dialog box APIs for the native side.
 *
 * @version 2
 * @since 15
 */
typedef struct {
    /**
     * @brief Provides the custom dialog box APIs for the native side. The API scope is {@link ArkUI_NativeDialogAPI_1}
     *
     * @since 15
     */
    ArkUI_NativeDialogAPI_1 nativeDialogAPI1;
    /**
     * @brief Defines the distance between the customDialog and system keyboard.
     *
	 * @note This method must be called before the <b>show</b> method.
     * @param handle Indicates the pointer to the custom dialog box controller.
     * @param distance distance, in vp.
     * @param unit  Indicates the unit, which is an enumerated value of {@link ArkUI_LengthMetricUnit}
     * @return Returns the result code.
     *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
     *         Returns {@link ARKUI_ERROR_CODE_CAPI_INIT_ERROR} if the CAPI init error.
     *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
     * @since 15
     */
    int32_t (*setKeyboardAvoidDistance)(ArkUI_NativeDialogHandle handle, float distance, ArkUI_LengthMetricUnit unit);

    /**
    * @brief Sets the level mode for a custom dialog box.
    *
    * @note This method must be called before the <b>show</b> method.
    * @param handle Indicates the pointer to the custom dialog box controller.
    * @param levelMode Indicates the level mode. The parameter type is {@link ArkUI_LevelMode}.
    * @return Returns the error code.
    *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
    *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
    * @since 15
    */
    int32_t (*setLevelMode)(ArkUI_NativeDialogHandle handle, ArkUI_LevelMode levelMode);

    /**
    * @brief Sets the level uniqueId for a custom dialog box.
    *
    * @note This method must be called before the <b>setLevelMode</b> method.
    * @param handle Indicates the pointer to the custom dialog box controller.
    * @param uniqueId Indicates the uniquedId of any nodes in router or navigation pages.
    * @return Returns the error code.
    *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
    *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
    * @since 15
    */
    int32_t (*setLevelUniqueId)(ArkUI_NativeDialogHandle handle, int32_t uniqueId);

    /**
    * @brief Sets the immersive mode for a custom dialog box.
    *
    * @note This method must be called before the <b>show</b> method.
    * @param handle Indicates the pointer to the custom dialog box controller.
    * @param immersiveMode Indicates the immersive mode. The parameter type is {@link ArkUI_ImmersiveMode}.
    * @return Returns the error code.
    *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
    *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
    * @since 15
    */
    int32_t (*setImmersiveMode)(ArkUI_NativeDialogHandle handle, ArkUI_ImmersiveMode immersiveMode);
} ArkUI_NativeDialogAPI_2;

/**
 * @brief Provides the custom dialog box APIs for the native side.
 *
 * @version 3
 * @since 19
 */
typedef struct {
    /**
     * @brief Provides the custom dialog box APIs for the native side. The API scope is {@link ArkUI_NativeDialogAPI_1}
     *
     * @since 19
     */
    ArkUI_NativeDialogAPI_1 nativeDialogAPI1;
    /**
     * @brief Provides the custom dialog box APIs for the native side. The API scope is {@link ArkUI_NativeDialogAPI_2}
     *
     * @since 19
     */
    ArkUI_NativeDialogAPI_2 nativeDialogAPI2;
    /**
     * @brief Sets the display order for a custom dialog box.
     *
     * @note This method must be called before the <b>show</b> method.
     * @param handle Indicates the pointer to the custom dialog box controller.
     * @param levelOrder Indicates the display order. The valid range is [-100000.0, 100000.0].
     * @return Returns the error code.
     *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
     *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
     * @since 19
     */
    int32_t (*setLevelOrder)(ArkUI_NativeDialogHandle handle, double levelOrder);
    /**
     * @brief Registers a listener callback before the dialog openAnimation starts.
     *
     * @param handle Indicates the pointer to the custom dialog box controller.
     * @param userData Indicates the pointer to the custom data.
     * @param callback Indicates the callback before the dialog openAnimation starts.
     * @return Returns the result code.
     *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
     *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
     * @since 19
     */

    int32_t (*registerOnWillAppear)(
        ArkUI_NativeDialogHandle handle, void* userData, void (*callback)(void* userData));

    /**
     * @brief Registers a listener callback when the dialog appears.
     *
     * @param handle Indicates the pointer to the custom dialog box controller.
     * @param userData Indicates the pointer to the custom data.
     * @param callback Indicates the callback when the dialog appears.
     * @return Returns the result code.
     *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
     *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
     * @since 19
     */
    int32_t (*registerOnDidAppear)(
        ArkUI_NativeDialogHandle handle, void* userData, void (*callback)(void* userData));

    /**
     * @brief Registers a listener callback before the dialog closeAnimation starts.
     *
     * @param handle Indicates the pointer to the custom dialog box controller.
     * @param userData Indicates the pointer to the custom data.
     * @param callback Indicates the callback before the dialog closeAnimation starts.
     * @return Returns the result code.
     *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
     *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
     * @since 19
     */
    int32_t (*registerOnWillDisappear)(
        ArkUI_NativeDialogHandle handle, void* userData, void (*callback)(void* userData));

    /**
     * @brief Registers a listener callback when the dialog disappears.
     *
     * @param handle Indicates the pointer to the custom dialog box controller.
     * @param userData Indicates the pointer to the custom data.
     * @param callback Indicates the callback when the dialog disappears.
     * @return Returns the result code.
     *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
     *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
     * @since 19
     */
    int32_t (*registerOnDidDisappear)(
        ArkUI_NativeDialogHandle handle, void* userData, void (*callback)(void* userData));

    /**
     * @brief Sets the border width of the dialog box.
     *
     * @note This method must be called before the <b>show</b> method.
     * @param handle Pointer to the dialog box controller.
     * @param top Width of the top border.
     * @param right Width of the right border.
     * @param bottom Width of the bottom border.
     * @param left Width of the left border.
     * @param unit Unit of the width. The default value is vp.
     * @return Returns the error code.
     *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
     *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occur..
     * @since 19
     */
    int32_t (*setBorderWidth)(
        ArkUI_NativeDialogHandle handle, float top, float right, float bottom, float left, ArkUI_LengthMetricUnit unit);

    /**
     * @brief Sets the border color of the dialog box.
     *
     * @note This method must be called before the <b>show</b> method.
     * @param handle Pointer to the dialog box controller.
     * @param top Color of the top border.
     * @param right Color of the right border.
     * @param bottom Color of the bottom border.
     * @param left Color of the left border.
     * @return Returns the error code.
     *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
     *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occur..
     * @since 19
     */
    int32_t (*setBorderColor)(
        ArkUI_NativeDialogHandle handle, uint32_t top, uint32_t right, uint32_t bottom, uint32_t left);

    /**
     * @brief Sets the border style of the dialog box.
     *
     * @note This method must be called before the <b>show</b> method.
     * @param handle Pointer to the dialog box controller.
     * @param top Style of the top border.
     * @param right Style of the right border.
     * @param bottom Style of the bottom border.
     * @param left Style of the left border.
     * @return Returns the error code.
     *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
     *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occur..
     * @since 19
     */
    int32_t (*setBorderStyle)(
        ArkUI_NativeDialogHandle handle, int32_t top, int32_t right, int32_t bottom, int32_t left);

    /**
     * @brief Sets the width of the dialog box background.
     *
     * @note This method must be called before the <b>show</b> method.
     * @param handle Pointer to the dialog box controller.
     * @param width Width of the background.
     * @param unit Unit of the width. The default value is vp.
     * @return Returns the error code.
     *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
     *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occur..
     * @since 19
     */
    int32_t (*setWidth)(ArkUI_NativeDialogHandle handle, float width, ArkUI_LengthMetricUnit unit);

    /**
     * @brief Sets the height of the dialog box background.
     *
     * @note This method must be called before the <b>show</b> method.
     * @param handle Pointer to the dialog box controller.
     * @param height Height of the background.
     * @param unit Unit of the height. The default value is vp.
     * @return Returns the error code.
     *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
     *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occur..
     * @since 19
     */
    int32_t (*setHeight)(ArkUI_NativeDialogHandle handle, float height, ArkUI_LengthMetricUnit unit);

    /**
     * @brief Sets the shadow of the dialog box background.
     *
     * @note This method must be called before the <b>show</b> method.
     * @param handle Pointer to the dialog box controller.
     * @param shadow Shadow style of the background, specified by an enumerated value.
     * @return Returns the error code.
     *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
     *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occur..
     * @since 19
     */
    int32_t (*setShadow)(ArkUI_NativeDialogHandle handle, ArkUI_ShadowStyle shadow);

    /**
     * @brief Sets the custom shadow of the dialog box background.
     *
     * @note This method must be called before the <b>show</b> method.
     * @param handle Pointer to the dialog box controller.
     * @param customShadow Custom shadow parameter. The format is the same as that of the <b>NODE_SHADOW</b> property.
     * @return Returns the error code.
     *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
     *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occur..
     * @since 19
     */
    int32_t (*setCustomShadow)(ArkUI_NativeDialogHandle handle, const ArkUI_AttributeItem* customShadow);

    /**
     * @brief Sets the background blur style of the dialog box.
     *
     * @note This method must be called before the <b>show</b> method.
     * @param handle Pointer to the dialog box controller.
     * @param blurStyle Background blur style, specified by an enumerated value.
     * @return Returns the error code.
     *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
     *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occur..
     * @since 19
     */
    int32_t (*setBackgroundBlurStyle)(ArkUI_NativeDialogHandle handle, ArkUI_BlurStyle blurStyle);

    /**
     * @brief Sets the keyboard avoidance mode of the dialog box.
     *
     * @note This method must be called before the <b>show</b> method.
     * @param handle Pointer to the dialog box controller.
     * @param keyboardAvoidMode Keyboard avoidance mode, specified by an enumerated value.
     * @return Returns the error code.
     *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
     *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occur..
     * @since 19
     */
    int32_t (*setKeyboardAvoidMode)(ArkUI_NativeDialogHandle handle, ArkUI_KeyboardAvoidMode keyboardAvoidMode);

    /**
     * @brief Sets whether to enable the hover mode for the dialog box.
     *
     * @note This method must be called before the <b>show</b> method.
     * @param handle Pointer to the dialog box controller.
     * @param enableHoverMode Whether to enable the hover mode. The default value is <b>false</b>.
     * @return Returns the error code.
     *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
     *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occur..
     * @since 19
     */
    int32_t (*enableHoverMode)(ArkUI_NativeDialogHandle handle, bool enableHoverMode);

    /**
     * @brief Set the default display area of the dialog box in hover mode.
     *
     * @note This method must be called before the <b>show</b> method.
     * @param handle Pointer to the dialog box controller.
     * @param hoverModeAreaType Display area in hover mode, specified by an enumerated value.
     * @return Returns the error code.
     *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
     *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occur.
     * @since 19
     */
    int32_t (*setHoverModeArea)(ArkUI_NativeDialogHandle handle, ArkUI_HoverModeAreaType hoverModeAreaType);

    /**
     * @brief Sets whether to get focus when the custom dialog is displayed.
     *
     * @param handle Indicates the pointer to the custom dialog box controller.
     * @param focusable Specifies whether to get focus when the custom dialog is displayed. The default value is true.
     * @return Returns the error code.
     *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
     *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
     * @since 19
     */
    int32_t (*setFocusable)(ArkUI_NativeDialogHandle handle, bool focusable);

    /**
     * @brief Sets the background blur effect for a custom dialog box.
     *
     * @note This method must be called before the <b>show</b> method.
     * @param handle Indicates the pointer to the custom dialog box controller.
     * @param backgroundBlurStyleOptions Background blur effect options.
     *        Format of the {@link ArkUI_AttributeItem} parameter: \n
     *        .value[0].i32: color mode. The value is an enum of {@link ArkUI_ColorMode}. \n
     *        .value[1]?.i32: adaptive color mode. The value is an enum of {@link ArkUI_AdaptiveColor}. \n
     *        .value[2]?.f32: blur degree. The value range is [0.0, 1.0]. \n
     *        .value[3]?.u32: brightness of black in the grayscale blur. The value range is [0, 127]. \n
     *        .value[4]?.u32: degree of darkening the white color in the grayscale blur. The value range is [0, 127]. \n
     *        .value[5]?.i32: blur activation policy. The value is an enum of {@link ArkUI_BlurStyleActivePolicy}. \n
     *        .value[6]?.u32: background color, in 0xARGB format, of the components within the window after the window
     *                        loses focus (in which case, the blur effect on the components within the window is
     *                        removed). \n
     * @return Returns the result code.
     *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
     *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
     * @since 19
     */
    int32_t (*setBackgroundBlurStyleOptions)(
        ArkUI_NativeDialogHandle handle, const ArkUI_AttributeItem* backgroundBlurStyleOptions);

    /**
     * @brief Sets the background effect parameters for a custom dialog box.
     *
     * @note This method must be called before the <b>show</b> method.
     * @param handle Indicates the pointer to the custom dialog box controller.
     * @param backgroundEffect Background effect.
     *        Format of the {@link ArkUI_AttributeItem} parameter: \n
     *        .value[0].f32: blur radius, in vp. \n
     *        .value[1]?.f32: saturation. \n
     *        .value[2]?.f32: brightness. \n
     *        .value[3]?.u32: color, in 0xARGB format. \n
     *        .value[4]?.i32: adaptive color mode. The value is an enum of {@link ArkUI_AdaptiveColor}. \n
     *        .value[5]?.u32: brightness of black in the grayscale blur. The value range is [0, 127]. \n
     *        .value[6]?.u32: degree of darkening the white color in the grayscale blur. The value range is [0, 127]. \n
     *        .value[7]?.i32: blur activation policy. The value is an enum of {@link ArkUI_BlurStyleActivePolicy}. \n
     *        .value[8]?.u32: background color, in 0xARGB format, of the components within the window after the window
     *                        loses focus (in which case, the blur effect on the components within the window is
     *                        removed). \n
     * @return Returns the result code.
     *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
     *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
     * @since 19
     */
    int32_t (*setBackgroundEffect)(ArkUI_NativeDialogHandle handle, const ArkUI_AttributeItem* backgroundEffect);
} ArkUI_NativeDialogAPI_3;

/**
 * @brief Sets whether to block the system behavior of dismissing a dialog box.
 *
 * @param event Indicates the pointer to a dialog box dismiss event object.
 * @param shouldBlockDismiss Indicates whether to block the system behavior of dismissing the dialog box. The value
 *                           <b>true</b> means to block the system behavior, and <b>false</b> means the opposite.
 * @since 12
 */
void OH_ArkUI_DialogDismissEvent_SetShouldBlockDismiss(ArkUI_DialogDismissEvent* event, bool shouldBlockDismiss);

/**
 * @brief Obtains the pointer to user data in a dialog box dismiss event object.
 *
 * @param event Indicates the pointer to a dialog box dismiss event object.
 *
 * @return Returns the pointer to user data.
 * @since 12
 */
void* OH_ArkUI_DialogDismissEvent_GetUserData(ArkUI_DialogDismissEvent* event);

/**
 * @brief Obtains the c from a dialog box dismiss event object.
 *
 * @param event Indicates the pointer to a dialog box dismiss event object.
 *
 * @return Returns the dismissal reason. Returns <b>-1</b> if an exception occurs.
 *         {@link DIALOG_DISMISS_BACK_PRESS}: touching the Back button, swiping left or right on the screen, or
 *                                            pressing the Esc key.
 *         {@link DIALOG_DISMISS_TOUCH_OUTSIDE}: touching the mask.
 *         {@link DIALOG_DISMISS_CLOSE_BUTTON}: touching the Close button.
 *         {@link DIALOG_DISMISS_SLIDE_DOWN}: sliding down.
 * @since 12
 */
int32_t OH_ArkUI_DialogDismissEvent_GetDismissReason(ArkUI_DialogDismissEvent* event);

/**
 * @brief Displays a custom dialog box.
 *
 * @param options Dialog box parameters.
 * @param callback Callback to be invoked when the custom dialog box displays.
 * @return Returns the error code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 19
 */
int32_t OH_ArkUI_CustomDialog_OpenDialog(ArkUI_CustomDialogOptions* options, void (*callback)(int32_t dialogId));

/**
 * @brief Updates a custom dialog box.
 *
 * @param options Dialog box parameters.
 * @param callback Callback to be invoked when the custom dialog box updates.
 * @return Returns the error code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 19
 */
int32_t OH_ArkUI_CustomDialog_UpdateDialog(ArkUI_CustomDialogOptions* options, void (*callback)(int32_t dialogId));

/**
 * @brief Closes a custom dialog box.
 *
 * @param dialogId Dialog id.
 * @return Returns the error code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 19
 */
int32_t OH_ArkUI_CustomDialog_CloseDialog(int32_t dialogId);

/**
 * @brief Creates custom dialog box options.
 *
 * @param content Content of the custom dialog box.
 * @return Returns the pointer to the custom dialog box options.
 * @since 19
 */
ArkUI_CustomDialogOptions* OH_ArkUI_CustomDialog_CreateOptions(ArkUI_NodeHandle content);

/**
 * @brief Destroys the custom dialog box options.
 *
 * @param options The pointer to the custom dialog box options.
 * @since 19
 */
void OH_ArkUI_CustomDialog_DisposeOptions(ArkUI_CustomDialogOptions* options);

/**
 * @brief Sets the level mode for a custom dialog box.
 *
 * @note This method must be called before the <b>OH_ArkUI_CustomDialog_OpenDialog</b> method.
 * @param options Indicates the pointer to the custom dialog options.
 * @param levelMode Indicates the level mode. The parameter type is {@link ArkUI_LevelMode}.
 * @return Returns the error code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 19
 */
int32_t OH_ArkUI_CustomDialog_SetLevelMode(ArkUI_CustomDialogOptions* options, ArkUI_LevelMode levelMode);

/**
 * @brief Sets the level uniqueId for a custom dialog box.
 *
 * @param options Indicates the pointer to the custom dialog options.
 * @param uniqueId Indicates the unique id of any nodes in router or navigation pages.
 * @return Returns the error code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 19
 */
int32_t OH_ArkUI_CustomDialog_SetLevelUniqueId(ArkUI_CustomDialogOptions* options, int32_t uniqueId);

/**
 * @brief Sets the immersive mode for a custom dialog box.
 *
 * @note This method must be called before the <b>OH_ArkUI_CustomDialog_OpenDialog</b> method.
 * @param options Indicates the pointer to the custom dialog options.
 * @param immersiveMode Indicates the immersive mode. The parameter type is {@link ArkUI_ImmersiveMode}.
 * @return Returns the error code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 19
 */
int32_t OH_ArkUI_CustomDialog_SetImmersiveMode(ArkUI_CustomDialogOptions* options, ArkUI_ImmersiveMode immersiveMode);

/**
 * @brief Sets the background color of the dialog box.
 *
 * @param options Dialog box parameters.
 * @param backgroundColor Background color of the dialog box.
 * @return Returns the error code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 19
 */
int32_t OH_ArkUI_CustomDialog_SetBackgroundColor(ArkUI_CustomDialogOptions* options, uint32_t backgroundColor);

/**
 * @brief Sets the corner radius for a custom dialog box.
 *
 * @param options Dialog box parameters.
 * @param topLeft Corner radius of the upper left corner.
 * @param topRight Corner radius of the upper right corner.
 * @param bottomLeft Corner radius of the lower left corner.
 * @param bottomRight Corner radius of the lower right corner.
 * @return Returns the error code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 19
 */
int32_t OH_ArkUI_CustomDialog_SetCornerRadius(
    ArkUI_CustomDialogOptions* options, float topLeft, float topRight, float bottomLeft, float bottomRight);

/**
 * @brief Sets the border width of the dialog box.
 *
 * @param options Dialog box parameters.
 * @param top Width of the top border.
 * @param right Width of the right border.
 * @param bottom Width of the bottom border.
 * @param left Width of the left border.
 * @param unit Unit of the width. The default value is vp.
 * @return Returns the error code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 19
 */
int32_t OH_ArkUI_CustomDialog_SetBorderWidth(
    ArkUI_CustomDialogOptions* options, float top, float right, float bottom, float left, ArkUI_LengthMetricUnit unit);

/**
 * @brief Sets the border color of the dialog box.
 *
 * @param options Dialog box parameters.
 * @param top Color of the top border.
 * @param right Color of the right border.
 * @param bottom Color of the bottom border.
 * @param left Color of the left border.
 * @return Returns the error code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 19
 */
int32_t OH_ArkUI_CustomDialog_SetBorderColor(
    ArkUI_CustomDialogOptions* options, uint32_t top, uint32_t right, uint32_t bottom, uint32_t left);

/**
 * @brief Sets the border style of the dialog box.
 *
 * @param options Dialog box parameters.
 * @param top Style of the top border.
 * @param right Style of the right border.
 * @param bottom Style of the bottom border.
 * @param left Style of the left border.
 * @return Returns the error code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 19
 */
int32_t OH_ArkUI_CustomDialog_SetBorderStyle(
    ArkUI_CustomDialogOptions* options, int32_t top, int32_t right, int32_t bottom, int32_t left);

/**
 * @brief Sets the width of the dialog box background.
 *
 * @param options Dialog box parameters.
 * @param width Width of the background.
 * @param unit Unit of the width. The default value is vp.
 * @return Returns the error code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 19
 */
int32_t OH_ArkUI_CustomDialog_SetWidth(ArkUI_CustomDialogOptions* options, float width, ArkUI_LengthMetricUnit unit);

/**
 * @brief Sets the height of the dialog box background.
 *
 * @param options Dialog box parameters.
 * @param height Height of the background.
 * @param unit Unit of the height. The default value is vp.
 * @return Returns the error code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 19
 */
int32_t OH_ArkUI_CustomDialog_SetHeight(ArkUI_CustomDialogOptions* options, float height, ArkUI_LengthMetricUnit unit);

/**
 * @brief Sets the shadow of the dialog box background.
 *
 * @param options Dialog box parameters.
 * @param shadow Shadow style of the background, specified by an enumerated value.
 * @return Returns the error code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 19
 */
int32_t OH_ArkUI_CustomDialog_SetShadow(ArkUI_CustomDialogOptions* options, ArkUI_ShadowStyle shadow);

/**
 * @brief Sets the custom shadow of the dialog box background.
 *
 * @param options Dialog box parameters.
 * @param customShadow Custom shadow parameter. The format is the same as that of
 * the <b>NODE_CUSTOM_SHADOW</b> property.
 * @return Returns the error code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 19
 */
int32_t OH_ArkUI_CustomDialog_SetCustomShadow(
    ArkUI_CustomDialogOptions* options, const ArkUI_AttributeItem* customShadow);

/**
 * @brief Sets the background blur style of the dialog box.
 *
 * @param options Dialog box parameters.
 * @param blurStyle Background blur style, specified by an enumerated value.
 * @return Returns the error code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 19
 */
int32_t OH_ArkUI_CustomDialog_SetBackgroundBlurStyle(ArkUI_CustomDialogOptions* options, ArkUI_BlurStyle blurStyle);

/**
 * @brief Sets the alignment mode of the dialog box.
 *
 * @param options Dialog box parameters.
 * @param alignment Alignment mode of the dialog box. The parameter type is {@link ArkUI_Alignment}.
 * @param offsetX Indicates the horizontal offset of the custom dialog box. The value is a floating point number.
 * @param offsetY Indicates the vertical offset of the custom dialog box. The value is a floating point number.
 * @return Returns the error code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 19
 */
int32_t OH_ArkUI_CustomDialog_SetAlignment(
    ArkUI_CustomDialogOptions* options, int32_t alignment, float offsetX, float offsetY);

/**
 * @brief Sets the modal mode for a custom dialog box.
 *
 * @param options Dialog box parameters.
 * @param isModal Whether the dialog box is a modal. A modal dialog box has a mask applied,
 * while a non-modal dialog box does not. The value <b>true</b> means that the dialog box is a modal.
 * @return Returns the error code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 19
 */
int32_t OH_ArkUI_CustomDialog_SetModalMode(ArkUI_CustomDialogOptions* options, bool isModal);

/**
 * @brief Specifies whether to allow users to touch the mask to dismiss the custom dialog box.
 *
 * @param options Dialog box parameters.
 * @param autoCancel Specifies whether to allow users to touch the mask to dismiss the dialog box.
 * The value <b>true</b> means to allow users to do so, and <b>false</b> means the opposite.
 * @return Returns the error code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 19
 */
int32_t OH_ArkUI_CustomDialog_SetAutoCancel(ArkUI_CustomDialogOptions* options, bool autoCancel);

/**
 * @brief Sets whether to display the dialog box in a subwindow.
 *
 * @param options Dialog box parameters.
 * @param showInSubwindow Whether to display the dialog box in a subwindow when it is not in the main window.
 * The default value is <b>false</b>, meaning the dialog box is displayed within the application, not in a
 * separate subwindow.
 * @return Returns the error code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 19
 */
int32_t OH_ArkUI_CustomDialog_SetSubwindowMode(ArkUI_CustomDialogOptions* options, bool showInSubwindow);

/**
 * @brief Sets the mask for a custom dialog box.
 *
 * @param options Dialog box parameters.
 * @param maskColor Mask color, in 0xargb format.
 * @param maskRect Pointer to the mask area. Events outside the mask area are transparently transmitted,
 * and events within the mask area are not. The parameter type is {@link ArkUI_Rect}.
 * @return Returns the error code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 19
 */
int32_t OH_ArkUI_CustomDialog_SetMask(
    ArkUI_CustomDialogOptions* options, uint32_t maskColor, const ArkUI_Rect* maskRect);

/**
 * @brief Sets the keyboard avoidance mode of the dialog box.
 *
 * @param options Dialog box parameters.
 * @param keyboardAvoidMode Keyboard avoidance mode, specified by an enumerated value.
 * @return Returns the error code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 19
 */
int32_t OH_ArkUI_CustomDialog_SetKeyboardAvoidMode(
    ArkUI_CustomDialogOptions* options, ArkUI_KeyboardAvoidMode keyboardAvoidMode);

/**
 * @brief Sets whether to enable the hover mode for the dialog box.
 *
 * @param options Dialog box parameters.
 * @param enabled Whether to enable the hover mode. The default value is <b>false</b>.
 * @return Returns the error code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 19
 */
int32_t OH_ArkUI_CustomDialog_SetHoverModeEnabled(ArkUI_CustomDialogOptions* options, bool enabled);

/**
 * @brief Set the default display area of the dialog box in hover mode.
 *
 * @param options Dialog box parameters.
 * @param hoverModeAreaType Display area in hover mode, specified by an enumerated value.
 * @return Returns the error code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 19
 */
int32_t OH_ArkUI_CustomDialog_SetHoverModeArea(
    ArkUI_CustomDialogOptions* options, ArkUI_HoverModeAreaType hoverModeAreaType);

/**
 * @brief Registers a callback for the dismissal event of the custom dialog box.
 *
 * @param options Dialog box parameters.
 * @param userData Pointer to the user-defined data.
 * @param callback Callback for the dismissal event of the custom dialog box.
 * @return Returns the error code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 19
 */
int32_t OH_ArkUI_CustomDialog_RegisterOnWillDismissCallback(
    ArkUI_CustomDialogOptions* options, void* userData, void (*callback)(ArkUI_DialogDismissEvent* event));

/**
 * @brief Registers a callback to be invoked when the custom dialog box is about to appear.
 *
 * @param options Dialog box parameters.
 * @param userData Pointer to the user-defined data.
 * @param callback Callback to be invoked when the dialog box is about to appear.
 * @return Returns the error code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 19
 */
int32_t OH_ArkUI_CustomDialog_RegisterOnWillAppearCallback(
    ArkUI_CustomDialogOptions* options, void* userData, void (*callback)(void* userData));

/**
 * @brief Registers a callback to be invoked when the custom dialog box appears.
 *
 * @param options Dialog box parameters.
 * @param userData Pointer to the user-defined data.
 * @param callback Callback to be invoked when the custom dialog box appears.
 * @return Returns the error code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 19
 */
int32_t OH_ArkUI_CustomDialog_RegisterOnDidAppearCallback(
    ArkUI_CustomDialogOptions* options, void* userData, void (*callback)(void* userData));

/**
 * @brief Registers a callback to be invoked when the custom dialog box is about to disappear.
 *
 * @param options Dialog box parameters.
 * @param userData Pointer to the user-defined data.
 * @param callback Callback to be invoked when the dialog box is about to disappear.
 * @return Returns the error code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 19
 */
int32_t OH_ArkUI_CustomDialog_RegisterOnWillDisappearCallback(
    ArkUI_CustomDialogOptions* options, void* userData, void (*callback)(void* userData));

/**
 * @brief Registers a callback to be invoked when the custom dialog box disappears.
 *
 * @param options Dialog box parameters.
 * @param userData Pointer to the user-defined data.
 * @param callback Callback to be invoked when the custom dialog box disappears.
 * @return Returns the error code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 19
 */
int32_t OH_ArkUI_CustomDialog_RegisterOnDidDisappearCallback(
    ArkUI_CustomDialogOptions* options, void* userData, void (*callback)(void* userData));

/**
 * @brief Get state of dialog.
 *
 * @param handle Indicates the pointer to the custom dialog box controller.
 * @param state Dialog state object.
 * @return Returns the error code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 20
 */
int32_t OH_ArkUI_CustomDialog_GetState(ArkUI_NativeDialogHandle handle, ArkUI_DialogState* state);

/**
 * @brief Sets the background blur effect for a dialog box.
 *
 * @param options Dialog box parameters.
 * @param backgroundBlurStyleOptions Background blur effect options of the dialog box.
 *        Format of the {@link ArkUI_AttributeItem} parameter: \n
 *        .value[0].i32: color mode. The value is an enum of {@link ArkUI_ColorMode}. \n
 *        .value[1]?.i32: adaptive color mode. The value is an enum of {@link ArkUI_AdaptiveColor}. \n
 *        .value[2]?.f32: blur degree. The value range is [0.0, 1.0]. \n
 *        .value[3]?.u32: brightness of black in the grayscale blur. The value range is [0, 127]. \n
 *        .value[4]?.u32: degree of darkening the white color in the grayscale blur. The value range is [0, 127]. \n
 *        .value[5]?.i32: blur activation policy. The value is an enum of {@link ArkUI_BlurStyleActivePolicy}. \n
 *        .value[6]?.u32: background color, in 0xARGB format, of the components within the window after the window loses
 *                        focus (in which case, the blur effect on the components within the window is removed). \n
 * @return Returns the result code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 19
 */
int32_t OH_ArkUI_CustomDialog_SetBackgroundBlurStyleOptions(
    ArkUI_CustomDialogOptions* options, const ArkUI_AttributeItem* backgroundBlurStyleOptions);

/**
 * @brief Sets the background effect parameters for a dialog box.
 *
 * @param options Dialog box parameters.
 * @param backgroundEffect Background effect of the dialog box.
 *        Format of the {@link ArkUI_AttributeItem} parameter: \n
 *        .value[0].f32: blur radius, in vp. \n
 *        .value[1]?.f32: saturation. \n
 *        .value[2]?.f32: brightness. \n
 *        .value[3]?.u32: color, in 0xARGB format. \n
 *        .value[4]?.i32: adaptive color mode. The value is an enum of {@link ArkUI_AdaptiveColor}. \n
 *        .value[5]?.u32: brightness of black in the grayscale blur. The value range is [0, 127]. \n
 *        .value[6]?.u32: degree of darkening the white color in the grayscale blur. The value range is [0, 127]. \n
 *        .value[7]?.i32: blur activation policy. The value is an enum of {@link ArkUI_BlurStyleActivePolicy}. \n
 *        .value[8]?.u32: background color, in 0xARGB format, of the components within the window after the window loses
 *                        focus (in which case, the blur effect on the components within the window is removed). \n
 * @return Returns the result code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 19
 */
int32_t OH_ArkUI_CustomDialog_SetBackgroundEffect(
    ArkUI_CustomDialogOptions* options, const ArkUI_AttributeItem* backgroundEffect);

#ifdef __cplusplus
};
#endif

#endif // ARKUI_NATIVE_DIALOG_H
/** @} */
