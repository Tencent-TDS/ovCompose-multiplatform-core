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
 * @brief Provides animation callbacks of ArkUI on the native side.
 *
 * @since 12
 */

/**
 * @file native_animate.h
 *
 * @brief Defines a set of animation APIs of ArkUI on the native side.
 *
 * @library libace_ndk.z.so
 * @syscap SystemCapability.ArkUI.ArkUI.Full
 * @kit ArkUI
 * @since 12
 */

#ifndef ARKUI_NATIVE_ANIMATE_H
#define ARKUI_NATIVE_ANIMATE_H

#include <cstdint>

#include "native_type.h"

#ifdef __cplusplus
extern "C" {
#endif

/**
* @brief Defines the expected frame rate range of the animation.
*
* @since 12
*/
typedef struct {
    /** Expected minimum frame rate. */
    uint32_t min;
    /** Expected maximum frame rate. */
    uint32_t max;
    /** Expected optimal frame rate. */
    uint32_t expected;
} ArkUI_ExpectedFrameRateRange;

/**
* @brief Defines the callback type for when the animation playback is complete.
*
* @since 12
*/
typedef struct {
    /** Type of the <b>onFinish</b> callback. */
    ArkUI_FinishCallbackType type;
    /** Callback invoked when the animation playback is complete. */
    void (*callback)(void* userData);
    /** Custom type. */
    void* userData;
} ArkUI_AnimateCompleteCallback;

/**
* @brief Defines the animation configuration.
*
* @since 12
*/
typedef struct ArkUI_AnimateOption ArkUI_AnimateOption;

/**
* @brief Defines an interpolation curve.
*
* @since 12
*/
typedef struct ArkUI_Curve ArkUI_Curve;

/**
 * @brief Defines the pointer to an interpolation curve.
 *
 * @since 12
 */
typedef struct ArkUI_Curve* ArkUI_CurveHandle;

/**
 * @brief Defines the keyframe animation parameter object.
 *
 * @since 12
 */
typedef struct ArkUI_KeyframeAnimateOption ArkUI_KeyframeAnimateOption;

/**
 * @brief Defines the animator parameter object.
 *
 * @since 12
 */
typedef struct ArkUI_AnimatorOption ArkUI_AnimatorOption;

/**
 * @brief Defines the pointer to an animator object.
 *
 * @since 12
 */
typedef struct ArkUI_Animator* ArkUI_AnimatorHandle;

/**
* @brief Defines the animator callback event object.
*
* @since 12
*/
typedef struct ArkUI_AnimatorEvent ArkUI_AnimatorEvent;

/**
* @brief Defines the callback object when the animator receives a frame.
*
* @since 12
*/
typedef struct ArkUI_AnimatorOnFrameEvent ArkUI_AnimatorOnFrameEvent;

/**
 * @brief Defines the transition effect.
 *
 * @since 12
 */
typedef struct ArkUI_TransitionEffect ArkUI_TransitionEffect;

/**
 * @brief Implements the native animation APIs provided by ArkUI.
 *
 * @version 1
 * @since 12
 */
typedef struct {
    /**
    * @brief Defines an explicit animation.
    *
    * @note Make sure the component attributes to be set in the event closure have been set before.
    *
    * @param context Indicates a <b>UIContext</b> instance.
    * @param option Indicates the pointer to an animation configuration.
    * @param update Indicates the animation closure. The system automatically inserts a transition animation for the
    * state change caused by the closure.
    * @param complete Indicates the callback to be invoked when the animation playback is complete.
    * @return Returns the error code.
    *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
    *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
    */
    int32_t (*animateTo)(ArkUI_ContextHandle context, ArkUI_AnimateOption* option, ArkUI_ContextCallback* update,
        ArkUI_AnimateCompleteCallback* complete);

    /**
    * @brief Sets the keyframe animation.
    *
    *
    * @param context Indicates a <b>UIContext</b> instance.
    * @param option Indicates the keyframe animation parameters.
    * @return Returns the error code.
    *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
    *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
    */
    int32_t (*keyframeAnimateTo)(ArkUI_ContextHandle context, ArkUI_KeyframeAnimateOption* option);

    /**
    * @brief Creates an animator object.
    *
    * @param context Indicates a <b>UIContext</b> instance.
    * @param option Indicates the animator parameters.
    * @return Returns the pointer to the animator object; returns <b>NULL</b> if a function parameter error occurs.
    */
    ArkUI_AnimatorHandle (*createAnimator)(ArkUI_ContextHandle context, ArkUI_AnimatorOption* option);

    /**
    * @brief Disposes of an animator object.
    *
    * @param animator Indicates the target animator object.
    */
    void (*disposeAnimator)(ArkUI_AnimatorHandle animatorHandle);
} ArkUI_NativeAnimateAPI_1;

/**
* @brief Creates an animation configuration.
*
* @return Returns the pointer to the created animation configuration.
* @since 12
*/
ArkUI_AnimateOption* OH_ArkUI_AnimateOption_Create();

/**
* @brief Disposes of an animation configuration.
*
* @since 12
*/
void OH_ArkUI_AnimateOption_Dispose(ArkUI_AnimateOption* option);

/**
* @brief Obtains the animation duration, in milliseconds.
*
* @param option Indicates the pointer to an animation configuration.
* @return Returns the duration.
* @since 12
*/
uint32_t OH_ArkUI_AnimateOption_GetDuration(ArkUI_AnimateOption* option);

/**
* @brief Obtains the animation playback speed.
*
* @param option Indicates the pointer to an animation configuration.
* @return Returns the animation playback speed.
* @since 12
*/
float OH_ArkUI_AnimateOption_GetTempo(ArkUI_AnimateOption* option);

/**
* @brief Obtains the animation curve.
*
* @param option Indicates the pointer to an animation configuration.
* @return Returns the animated curve.If Null is returned, it means option is an invalid value.
* @since 12
*/
ArkUI_AnimationCurve OH_ArkUI_AnimateOption_GetCurve(ArkUI_AnimateOption* option);

/**
* @brief Obtains the animation delay, in milliseconds.
*
* @param option Indicates the pointer to an animation configuration.
* @return Returns the animation delay.
* @since 12
*/
int32_t OH_ArkUI_AnimateOption_GetDelay(ArkUI_AnimateOption* option);

/**
* @brief Obtains the number of times that an animation is played.
*
* @param option Indicates the pointer to an animation configuration.
* @return Returns the number of times that the animation is played.
* @since 12
*/
int32_t OH_ArkUI_AnimateOption_GetIterations(ArkUI_AnimateOption* option);

/**
* @brief Obtains the animation playback mode.
*
* @param option Indicates the pointer to an animation configuration.
* @return Returns the animation playback mode.
* @since 12
*/
ArkUI_AnimationPlayMode OH_ArkUI_AnimateOption_GetPlayMode(ArkUI_AnimateOption* option);

/**
* @brief Obtains the expected frame rate range of an animation.
*
* @param option Indicates the pointer to an animation configuration.
* @return Returns the expected frame rate range.
* @since 12
*/
ArkUI_ExpectedFrameRateRange* OH_ArkUI_AnimateOption_GetExpectedFrameRateRange(ArkUI_AnimateOption* option);

/**
* @brief Sets the animation duration.
*
* @param option Indicates the pointer to an animation configuration.
* @param value Indicates the duration, in milliseconds.
* @since 12
*/
void OH_ArkUI_AnimateOption_SetDuration(ArkUI_AnimateOption* option, int32_t value);

/**
* @brief Sets the animation playback speed.
*
* @param option Indicates the pointer to an animation configuration.
* @param value Indicates the animation playback speed.
* @since 12
*/
void OH_ArkUI_AnimateOption_SetTempo(ArkUI_AnimateOption* option, float value);

/**
* @brief Sets the animation curve.
*
* @param option Indicates the pointer to an animation configuration.
* @param value Indicates the animated curve. Default value：ARKUI_CURVE_LINEAR.
* @since 12
*/
void OH_ArkUI_AnimateOption_SetCurve(ArkUI_AnimateOption* option, ArkUI_AnimationCurve value);

/**
* @brief Sets the animation delay.
*
* @param option Indicates the pointer to an animation configuration.
* @param value Indicates the animation delay.
* @since 12
*/
void OH_ArkUI_AnimateOption_SetDelay(ArkUI_AnimateOption* option, int32_t value);

/**
* @brief Sets the number of times that an animation is played.
*
* @param option Indicates the pointer to an animation configuration.
* @param value Indicates the number of times that the animation is played.
* @since 12
*/
void OH_ArkUI_AnimateOption_SetIterations(ArkUI_AnimateOption* option, int32_t value);

/**
* @brief Sets the animation playback mode.
*
* @param option Indicates the pointer to an animation configuration.
* @param value Indicates the animation playback mode.
* @since 12
*/
void OH_ArkUI_AnimateOption_SetPlayMode(ArkUI_AnimateOption* option, ArkUI_AnimationPlayMode value);

/**
* @brief Sets the expected frame rate range of an animation.
*
* @param option Indicates the pointer to an animation configuration.
* @param value Indicates the expected frame rate range.
* @since 12
*/
void OH_ArkUI_AnimateOption_SetExpectedFrameRateRange(ArkUI_AnimateOption* option, ArkUI_ExpectedFrameRateRange* value);

/**
* @brief Sets the animation curve for the animation of an animator.
*
* @note This method is better than the value set by OH_ArkUI_AnimateOption_SetCurve.
* @param option Indicates the animator parameters.
* @param value Indicates the animation curve settings.
* @since 12
*/
void OH_ArkUI_AnimateOption_SetICurve(ArkUI_AnimateOption* option, ArkUI_CurveHandle value);

/**
* @brief Obtains the animation curve of the animation of an animator.
*
* @param option Indicates the animator parameters.
* @return Returns the animation curve of the specified animation.
* If Null is returned, it means option is an invalid value.
* @since 12
*/
ArkUI_CurveHandle OH_ArkUI_AnimateOption_GetICurve(ArkUI_AnimateOption* option);

/**
 * @brief Obtains the keyframe animation parameters.
 *
 * @param size Indicates the number of keyframe animation states.
 * @return Returns the keyframe animation parameter object; returns <b>NULL</b> if the value of <b>size</b> is less than
 * 0.
 * @since 12
 */
ArkUI_KeyframeAnimateOption* OH_ArkUI_KeyframeAnimateOption_Create(int32_t size);

/**
 * @brief Disposes of the keyframe animation parameter object.
 *
 * @param option Indicates the keyframe animation parameter object.
 * @since 12
 */
void OH_ArkUI_KeyframeAnimateOption_Dispose(ArkUI_KeyframeAnimateOption* option);

/**
 * @brief Sets the overall delay of a keyframe animation, in milliseconds. By default, the keyframe animation is played
 * without delay.
 *
 * @param option Indicates the keyframe animation parameters.
 * @param value Indicates the delay, in milliseconds.
 * @return Returns the error code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 12
 */
int32_t OH_ArkUI_KeyframeAnimateOption_SetDelay(ArkUI_KeyframeAnimateOption* option, int32_t value);

/**
 * @brief Sets the number of times that the keyframe animation is played. By default, the animation is played once.
 * The value <b>-1</b> indicates that the animation is played for an unlimited number of times. The value <b>0</b>
 * indicates that there is no animation.
 *
 * @param option Indicates the keyframe animation parameters.
 * @param value Indicates the number of times that the animation is played.
 * @return Returns the error code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 12
 */
int32_t OH_ArkUI_KeyframeAnimateOption_SetIterations(ArkUI_KeyframeAnimateOption* option, int32_t value);

/**
 * @brief Sets the callback invoked when the keyframe animation playback is complete. This API is called after the
 * keyframe animation has played for the specified number of times.
 *
 * @param option Indicates the keyframe animation parameters.
 * @param userData Indicates the pointer to a custom object.
 * @param onFinish Indicates the callback.
 * @return Returns the error code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 12
 */
int32_t OH_ArkUI_KeyframeAnimateOption_RegisterOnFinishCallback(
    ArkUI_KeyframeAnimateOption* option, void* userData, void (*onFinish)(void* userData));

/**
 * @brief Sets the duration of a keyframe animation, in milliseconds.
 *
 * @param option Indicates the keyframe animation parameters.
  * @param value Indicates the duration to set, in milliseconds.
 * @param index Indicates a state index.
 * @return Returns the error code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 12
 */
int32_t OH_ArkUI_KeyframeAnimateOption_SetDuration(ArkUI_KeyframeAnimateOption* option, int32_t value, int32_t index);

/**
 * @brief Sets the animation curve for a specific keyframe in a keyframe animation.
 *
 * @note Because the <b>springMotion</b>, <b>responsiveSpringMotion</b>, and <b>interpolatingSpring</b> curves do not
 * have effective duration settings, they are not supported.
 * @param option Indicates the keyframe animation parameters.
 * @param value Indicates the animation curve to set. Default value：EASE_IN_OUT.
 * @param index Indicates a state index.
 * @return Returns the error code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 12
 */
int32_t OH_ArkUI_KeyframeAnimateOption_SetCurve(
    ArkUI_KeyframeAnimateOption* option, ArkUI_CurveHandle value, int32_t index);

/**
 * @brief Sets the closure function of the state at the time of the keyframe, that is, the state to be reached at the
 * time of the keyframe.
 *
 * @param option Indicates the keyframe animation parameters.
 * @param event Indicates a closure function.
 * @param userData Indicates the pointer to a custom object.
 * @param index Indicates a state index.
 * @return Returns the error code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 12
 */
int32_t OH_ArkUI_KeyframeAnimateOption_RegisterOnEventCallback(
    ArkUI_KeyframeAnimateOption* option, void* userData, void (*event)(void* userData), int32_t index);

/**
 * @brief Obtains the overall delay of a keyframe animation
 *
 * @param option Indicates the keyframe animation parameters.
 * @return Returns the overall delay.
 * @since 12
 */
int32_t OH_ArkUI_KeyframeAnimateOption_GetDelay(ArkUI_KeyframeAnimateOption* option);

/**
 * @brief Obtains the number of times that a keyframe animation is played.
 *
 * @param option Indicates the keyframe animation parameters.
 * @return Returns the number of times that the animation is played.
 * @since 12
 */
int32_t OH_ArkUI_KeyframeAnimateOption_GetIterations(ArkUI_KeyframeAnimateOption* option);

/**
 * @brief Obtains the duration of a specific state in a keyframe animation.
 *
 * @param option Indicates the keyframe animation parameters.
 * @param index Indicates a state index.
 * @return Returns the duration. The unit is millisecond.
 * @since 12
 */
int32_t OH_ArkUI_KeyframeAnimateOption_GetDuration(ArkUI_KeyframeAnimateOption* option, int32_t index);

/**
 * @brief Obtains the animation curve of a specific state in a keyframe animation.
 *
 * @param option Indicates the keyframe animation parameters.
 * @param index Indicates a state index.
 * @return Returns the animated curve.
 *         Returns <b>NULL</b> if a parameter error occurs.
 * @since 12
 */
ArkUI_CurveHandle OH_ArkUI_KeyframeAnimateOption_GetCurve(ArkUI_KeyframeAnimateOption* option, int32_t index);

/**
 * @brief Creates an animator parameter object.
 *
 * @note When <b>keyframeSize</b> is greater than 0, the animation interpolation start point is 0, and the animation
 * interpolation end point is 1; no setting is allowed.
 * @param keyframeSize Indicates the number of keyframes.
 * @return Returns the pointer to the animator parameter object.
 * returns <b>NULL</b> if the value of <b>size</b> is less than 0.
 * @since 12
 */
ArkUI_AnimatorOption* OH_ArkUI_AnimatorOption_Create(int32_t keyframeSize);

/**
 * @brief Disposes of an animator parameter object.
 *
 * @since 12
 */
void OH_ArkUI_AnimatorOption_Dispose(ArkUI_AnimatorOption* option);

/**
 * @brief Sets the duration for thea nimation of an animator, in milliseconds.
 *
 * @param option Indicates the target animator parameter object.
 * @param value Indicates the playback duration, in milliseconds.
 * @return Returns the error code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 12
 */
int32_t OH_ArkUI_AnimatorOption_SetDuration(ArkUI_AnimatorOption* option, int32_t value);

/**
 * @brief Sets the delay for playing the animation of an animator, in milliseconds.
 *
 * @param option Indicates an animator parameter object.
 * @param value Indicates the delay to set, in milliseconds.
 * @return Returns the error code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 12
 */
int32_t OH_ArkUI_AnimatorOption_SetDelay(ArkUI_AnimatorOption* option, int32_t value);

/**
 * @brief Sets the number of times that the animation of an animator is played. The value <b>0</b> means not to play the
 * animation, and <b>-1</b> means to play the animation for an unlimited number of times.
 *
 * @note If this parameter is set to a negative value other than <b>-1</b>, the value is invalid. In this case, the
 * animation is played once.
 * @param option Indicates an animator parameter object.
 * @param value Indicates the number of times that the animation is played.
 * @return Returns the error code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 12
 */
int32_t OH_ArkUI_AnimatorOption_SetIterations(ArkUI_AnimatorOption* option, int32_t value);

/**
 * @brief Sets whether the animation of an animator is restored to the initial state after being executed.
 *
 * @param option Indicates an animator parameter object.
 * @param value Indicates whether to restore the animation to the initial state after the animation is executed.
 * @return Returns the error code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 12
 */
int32_t OH_ArkUI_AnimatorOption_SetFill(ArkUI_AnimatorOption* option, ArkUI_AnimationFillMode value);

/**
 * @brief Sets the playback direction for the animation of an animator.
 *
 * @param option Indicates an animator parameter object.
 * @param value Indicates the animation playback direction.
 * @return Returns the error code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 12
 */
int32_t OH_ArkUI_AnimatorOption_SetDirection(ArkUI_AnimatorOption* option, ArkUI_AnimationDirection value);

/**
 * @brief Sets the interpolation curve for the animation of an animator.
 *
 * @note <b>springCurve</b>, <b>springMotion</b>, <b>responsiveSpringMotion</b>, <b>interpolatingSpring</b>,
 * and <b>customCurve</b> curves are not supported.
 *
 * @param option Indicates an animator parameter object.
 * @param value Indicates the target interpolation curve. Default value：ARKUI_CURVE_LINEAR.
 * @return Returns the error code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 12
 */
int32_t OH_ArkUI_AnimatorOption_SetCurve(ArkUI_AnimatorOption* option, ArkUI_CurveHandle value);

/**
 * @brief Sets the interpolation start point for the animation of an animator.
 * @note This API does not take effect when the animation is a keyframe animation.
 *
 * @param option Indicates an animator parameter object.
 * @param value Indicates the interpolation start point to set.
 * @return Returns the error code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 12
 */
int32_t OH_ArkUI_AnimatorOption_SetBegin(ArkUI_AnimatorOption* option, float value);

/**
 * @brief Sets the interpolation end point for the animation of an animator.
 * @note This API does not take effect when the animation is a keyframe animation.
 *
 * @param option Indicates an animator parameter object.
 * @param value Indicates the interpolation end point to set.
 * @return Returns the error code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 12
 */
int32_t OH_ArkUI_AnimatorOption_SetEnd(ArkUI_AnimatorOption* option, float value);

/**
 * @brief Sets the expected frame rate range for the animation of an animator.
 *
 * @param option Indicates an animator parameter object.
 * @param value Indicates the expected frame rate range to set.
 * @return Returns the error code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 12
 */
int32_t OH_ArkUI_AnimatorOption_SetExpectedFrameRateRange(
    ArkUI_AnimatorOption* option, ArkUI_ExpectedFrameRateRange* value);

/**
 * @brief Sets the keyframe parameters for the animation of an animator.
 *
 * @param option Indicates an animator parameter object.
 * @param time Indicates the keyframe time. Value range: [0,1].
 * @param value Indicates the keyframe value.
 * @param index Indicates the keyframe index.
 * @return Returns the error code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 12
 */
int32_t OH_ArkUI_AnimatorOption_SetKeyframe(
    ArkUI_AnimatorOption* option, float time, float value, int32_t index);

/**
 * @brief Sets the keyframe curve type for the animation of an animator.
 *
 * @note <b>springCurve</b>, <b>springMotion</b>, <b>responsiveSpringMotion</b>, <b>interpolatingSpring</b>,
 * and <b>customCurve</b> curves are not supported.
 *
 * @param option Indicates an animator parameter object.
 * @param value Indicates the target interpolation curve.
 * @param index Indicates the keyframe index.
 * @return Returns the error code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 12
 */
int32_t OH_ArkUI_AnimatorOption_SetKeyframeCurve(ArkUI_AnimatorOption* option, ArkUI_CurveHandle value, int32_t index);
/**
 * @brief Obtains the duration for playing an animation.
 *
 * @param option Indicates the animator parameters.
 * @return Returns the duration for playing the animation, in milliseconds.
 * @since 12
 */
int32_t OH_ArkUI_AnimatorOption_GetDuration(ArkUI_AnimatorOption* option);

/**
 * @brief Obtains the delay for playing the animation of an animator.
 *
 * @param option Indicates the animator parameters.
 * @return Returns the delay for playing the animation, in milliseconds.
 * @since 12
 */
int32_t OH_ArkUI_AnimatorOption_GetDelay(ArkUI_AnimatorOption* option);

/**
 * @brief Obtains the number of times that an animation is played.
 *
 * @param option Animator animation parameter.
 * @return Returns the number of times that the animation is played.
 * @since 12
 */
int32_t OH_ArkUI_AnimatorOption_GetIterations(ArkUI_AnimatorOption* option);

/**
 * @brief Obtains whether the animator animation is restored to the initial state after being executed.
 *
 * @param option Indicates the animator parameters.
 * @return Returns whether the animator animation is restored to the initial state after being executed.
 * @since 12
 */
ArkUI_AnimationFillMode OH_ArkUI_AnimatorOption_GetFill(ArkUI_AnimatorOption* option);

/**
 * @brief Obtains the playback direction of an animation.
 *
 * @param option Indicates the animator parameters.
 * @return Returns the animation playback direction.
 * @since 12
 */
ArkUI_AnimationDirection OH_ArkUI_AnimatorOption_GetDirection(ArkUI_AnimatorOption* option);

/**
 * @brief Obtains the interpolation curve of the animation of an animator.
 *
 * @param option Indicates the animator parameters.
 * @return Returns the interpolation curve of the animation.
 *         Returns <b>NULL</b> if a parameter error occurs.
 * @since 12
 */
ArkUI_CurveHandle OH_ArkUI_AnimatorOption_GetCurve(ArkUI_AnimatorOption* option);

/**
 * @brief Obtains the interpolation start point of an animation.
 *
 * @param option Indicates the animator parameters.
 * @return Returns the interpolation start point of the animation.
 * @since 12
 */
float OH_ArkUI_AnimatorOption_GetBegin(ArkUI_AnimatorOption* option);

/**
 * @brief Obtains the interpolation end point of an animation.
 *
 * @param option Indicates the animator parameters.
 * @return Returns the interpolation end point of the animation.
 * @since 12
 */
float OH_ArkUI_AnimatorOption_GetEnd(ArkUI_AnimatorOption* option);

/**
 * @brief Obtains the expected frame rate range of an animation.
 *
 * @param option Indicates the animator parameters.
 * @return Returns the pointer to the expected frame rate range object.
 *         Returns <b>NULL</b> if a parameter error occurs.
 * @since 12
 */
ArkUI_ExpectedFrameRateRange* OH_ArkUI_AnimatorOption_GetExpectedFrameRateRange(ArkUI_AnimatorOption* option);

/**
 * @brief Obtains the keyframe time of an animation.
 *
 * @param option Indicates an animator parameter object.
 * @param index Indicates the keyframe index.
 * @return Returns the keyframe time.
 * @since 12
 */
float OH_ArkUI_AnimatorOption_GetKeyframeTime(ArkUI_AnimatorOption* option, int32_t index);

/**
 * @brief Obtains the keyframe value of an animation.
 *
 * @param option Indicates an animator parameter object.
 * @param index Indicates the keyframe index.
 * @return Returns the keyframe value.
 * @since 12
 */
float OH_ArkUI_AnimatorOption_GetKeyframeValue(ArkUI_AnimatorOption* option, int32_t index);

/**
 * @brief Obtains the interpolation curve for a keyframe in the animation of an animator.
 *
 * @param option Indicates an animator parameter object.
 * @param index Indicates the keyframe index.
 * @return Returns the interpolation curve.
 *         Returns <b>NULL</b> if a parameter error occurs.
 * @since 12
 */
ArkUI_CurveHandle OH_ArkUI_AnimatorOption_GetKeyframeCurve(ArkUI_AnimatorOption* option, int32_t index);

/**
 * @brief Obtains the custom object in an animation event object.
 *
 * @param event Indicates an animation event object.
 * @return Returns the custom object.
 *         Returns <b>NULL</b> if a parameter error occurs.
 * @since 12
 */
void* OH_ArkUI_AnimatorEvent_GetUserData(ArkUI_AnimatorEvent* event);

/**
 * @brief Obtains the custom object in an animation event object.
 *
 * @param event Indicates an animation event object.
 * @return Returns the custom object.
 * @since 12
 */
void* OH_ArkUI_AnimatorOnFrameEvent_GetUserData(ArkUI_AnimatorOnFrameEvent* event);

/**
 * @brief Obtains the current progress in an animation event object.
 *
 * @param event Indicates an animation event object.
 * @return Returns the animation progress.
 * @since 12
 */
float OH_ArkUI_AnimatorOnFrameEvent_GetValue(ArkUI_AnimatorOnFrameEvent* event);

/**
 * @brief Sets the callback invoked when the animator receives a frame.
 *
 * @param option Indicates an animator parameter object.
 * @param userData Indicates the custom parameter.
 * @param callback Indicates the callback to set.
 * @return Returns the error code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 12
 */
int32_t OH_ArkUI_AnimatorOption_RegisterOnFrameCallback(
    ArkUI_AnimatorOption* option, void* userData, void (*callback)(ArkUI_AnimatorOnFrameEvent* event));

/**
 * @brief Sets the callback invoked when the animation playback is complete.
 *
 * @param option Indicates an animator parameter object.
 * @param userData Indicates the custom parameter.
 * @param callback Indicates the callback to set.
 * @return Returns the error code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 12
 */
int32_t OH_ArkUI_AnimatorOption_RegisterOnFinishCallback(
    ArkUI_AnimatorOption* option, void* userData, void (*callback)(ArkUI_AnimatorEvent* event));

/**
 * @brief Sets the callback invoked when the animation playback is canceled.
 *
 * @param option Indicates an animator parameter object.
 * @param userData Indicates the custom parameter.
 * @param callback Indicates the callback to set.
 * @return Returns the error code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 12
 */
int32_t OH_ArkUI_AnimatorOption_RegisterOnCancelCallback(
    ArkUI_AnimatorOption* option, void* userData, void (*callback)(ArkUI_AnimatorEvent* event));

/**
 * @brief Sets the callback invoked when the animation playback is repeated.
 *
 * @param option Indicates an animator parameter object.
 * @param userData Indicates the custom parameter.
 * @param callback Indicates the callback to set.
 * @return Returns the error code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 12
 */
int32_t OH_ArkUI_AnimatorOption_RegisterOnRepeatCallback(
    ArkUI_AnimatorOption* option, void* userData, void (*callback)(ArkUI_AnimatorEvent* event));

/**
 * @brief Resets the animation of an animator.
 *
 * @param animatorHandle Indicates an animator object.
 * @param option Indicates the animator parameters.
 * @return Returns the error code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 12
 */
int32_t OH_ArkUI_Animator_ResetAnimatorOption(
    ArkUI_AnimatorHandle animatorHandle, ArkUI_AnimatorOption* option);

/**
 * @brief Starts the animation of an animator.
 *
 * @param animatorHandle Indicates an animator object.
 * @return Returns the error code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 12
 */
int32_t OH_ArkUI_Animator_Play(ArkUI_AnimatorHandle animatorHandle);

/**
 * @brief Ends the animation of an animator.
 *
 * @param animatorHandle Indicates an animator object.
 * @return Returns the error code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 12
 */
int32_t OH_ArkUI_Animator_Finish(ArkUI_AnimatorHandle animatorHandle);

/**
 * @brief Pauses the animation of an animator.
 *
 * @param animatorHandle Indicates an animator object.
 * @return Returns the error code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 12
 */
int32_t OH_ArkUI_Animator_Pause(ArkUI_AnimatorHandle animatorHandle);

/**
 * @brief Cancels the animation of an animator.
 *
 * @param animatorHandle Indicates an animator object.
 * @return Returns the error code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 12
 */
int32_t OH_ArkUI_Animator_Cancel(ArkUI_AnimatorHandle animatorHandle);

/**
 * @brief Plays the animation of an animator in reverse order.
 *
 * @param animatorHandle Indicates an animator object.
 * @return Returns the error code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 12
 */
int32_t OH_ArkUI_Animator_Reverse(ArkUI_AnimatorHandle animatorHandle);

/**
 * @brief Implements initialization for the interpolation curve, which is used to create an interpolation curve based on
 * the input parameter.
 *
 * @param curve Indicates the curve type.
 * @return Returns the pointer to the interpolation object of the curve.
 *         Returns <b>NULL</b> if a parameter error occurs.
 */
ArkUI_CurveHandle OH_ArkUI_Curve_CreateCurveByType(ArkUI_AnimationCurve curve);

/**
 * @brief Creates a step curve.
 *
 * @param count Indicates the number of steps. The value must be a positive integer. Value range: [1, +∞).
 * @param end Indicates whether jumping occurs when the interpolation ends.
 * <b>true</b>: Jumping occurs when the interpolation ends. <b>false</b>: Jumping occurs when the interpolation starts.
 * @return Returns the pointer to the interpolation object of the curve.
 *         Returns <b>NULL</b> if a parameter error occurs.
 */
ArkUI_CurveHandle OH_ArkUI_Curve_CreateStepsCurve(int32_t count, bool end);

/**
 * @brief Creates a cubic Bezier curve.
 *
 *
 * @param x1 Indicates the X coordinate of the first point on the Bezier curve. Value range: [0, 1].
 * A value less than 0 is handed as <b>0</b>. A value greater than 1 is handed as <b>1</b>.
 * @param y1 Indicates the Y coordinate of the first point on the Bezier curve.
 * @param x2 Indicates the X coordinate of the second point on the Bezier curve. Value range: [0, 1].
 * A value less than 0 is handed as <b>0</b>. A value greater than 1 is handed as <b>1</b>.
 * @param y2 Indicates the Y coordinate of the second point on the Bezier curve.
 * @return Returns the pointer to the interpolation object of the curve.
 *         Returns <b>NULL</b> if a parameter error occurs.
 */
ArkUI_CurveHandle OH_ArkUI_Curve_CreateCubicBezierCurve(float x1, float y1, float x2, float y2);

/**
 * @brief Creates a spring curve. The curve shape is subject to the spring parameters, and the animation duration is
 * subject to the <b>duration</b> parameter in <b>animation</b> and <b>animateTo</b>.
 *
  * @param velocity Indicates the initial velocity of the spring. It is applied by external factors to the spring
  * animation, designed to help ensure the smooth transition from the previous motion state. The velocity is the
  * normalized velocity, and its value is equal to the actual velocity at the beginning of the animation divided by the
  *  animation attribute change value.
 * @param mass Indicates the mass, which influences the inertia in the spring system. The greater the mass, the greater
 * the amplitude of the oscillation, and the slower the speed of restoring to the equilibrium position.
 * @param stiffness Indicates the stiffness. It is the degree to which an object deforms by resisting the force applied.
 * In an elastic system, the greater the stiffness, the stronger the ability to resist deformation, and the faster the
 * speed of restoring to the equilibrium position.
 * @param damping Indicates the damping. It is used to describe the oscillation and attenuation of the system after
 * being disturbed. The larger the damping, the smaller the number of oscillations of elastic motion, and the smaller
 * the oscillation amplitude.
 * @return Returns the pointer to the interpolation object of the curve.
 *         Returns <b>NULL</b> if a parameter error occurs.
 */
ArkUI_CurveHandle OH_ArkUI_Curve_CreateSpringCurve(float velocity, float mass, float stiffness, float damping);

/**
 * @brief Creates a spring animation curve. If multiple spring animations are applied to the same attribute of an
 *  object, each animation replaces their predecessor and inherits the velocity.
 * @note The animation duration is subject to the curve parameters, rather than the <b>duration</b> parameter in
 * <b>animation</b> or <b>animateTo</b>.
 *
 * @param response Indicates the duration of one complete oscillation.
 * @param dampingFraction Indicates the damping coefficient.
 * > 0 and < 1: underdamped. In this case, the spring overshoots the equilibrium position.
 * <b>1</b>: critically damped.
 * > 1: overdamped. In this case, the spring approaches equilibrium gradually.
 * @param overlapDuration Indicates the duration for animations to overlap. When animations overlap, the <b>response</b>
 * values of these animations will
 * transit smoothly over this duration if they are different.
 * @return Returns the pointer to the interpolation object of the curve.
 *         Returns <b>NULL</b> if a parameter error occurs.
 */
ArkUI_CurveHandle OH_ArkUI_Curve_CreateSpringMotion(float response, float dampingFraction, float overlapDuration);

/**
 * @brief Creates a responsive spring animation curve. It is a special case of <b>springMotion</b>, with the only
 * difference in the default values. It can be used together with <b>springMotion</b>.
 * @note The animation duration is subject to the curve parameters, rather than the <b>duration</b> parameter in
 * <b>animation</b> or <b>animateTo</b>.
 *
 * @param response Indicates the duration of one complete oscillation.
 * @param dampingFraction Indicates the damping coefficient.
 * > 0 and < 1: underdamped. In this case, the spring overshoots the equilibrium position.
 * <b>1</b>: critically damped.
 * > 1: overdamped. In this case, the spring approaches equilibrium gradually.
 * @param overlapDuration Indicates the duration for animations to overlap. When animations overlap, the
 * <b>response</b> values of these animations will
 * transit smoothly over this duration if they are different.
 * @return Returns the pointer to the interpolation object of the curve.
 *         Returns <b>NULL</b> if a parameter error occurs.
 */
ArkUI_CurveHandle OH_ArkUI_Curve_CreateResponsiveSpringMotion(
    float response, float dampingFraction, float overlapDuration);

/**
 * @brief Creates an interpolating spring curve animated from 0 to 1. The actual animation value is calculated based on
 * the curve.
 * @note The animation duration is subject to the curve parameters, rather than the <b>duration</b> parameter in
 * <b>animation</b> or <b>animateTo</b>.
 *
 *
  * @param velocity Indicates the initial velocity of the spring. It is applied by external factors to the spring
  * animation, esigned to help ensure the smooth transition from the previous motion state. The velocity is the
  * normalized velocity, and its value is equal to the actual velocity
 * at the beginning of the animation divided by the animation attribute change value.
 * @param mass Indicates the mass, which influences the inertia in the spring system.
 * The greater the mass, the greater the amplitude of the oscillation, and the slower the speed of restoring to the
 * equilibrium position.
 * @param stiffness Indicates the stiffness. It is the degree to which an object deforms by resisting the force applied.
 * In an elastic system, the greater the stiffness, the stronger the ability to resist deformation, and the faster the
 * speed of restoring to the equilibrium position.
 * @param damping Indicates the damping. It is used to describe the oscillation and attenuation of the system after
 * being disturbed. The larger the damping, the smaller the number of oscillations of elastic motion, and the smaller
 * the oscillation amplitude.
 * @return Returns the pointer to the interpolation object of the curve.
 *         Returns <b>NULL</b> if a parameter error occurs.
 */
ArkUI_CurveHandle OH_ArkUI_Curve_CreateInterpolatingSpring(float velocity, float mass, float stiffness, float damping);

/**
 * @brief Creates a custom curve.
 *
 * @param userData Indicates the custom data.
 * @param interpolate Indicates the custom interpolation callback. <b>fraction</b> indicates the input x value for
 * interpolation when the animation starts; value range: [0,1].
 * The return value is the y value of the curve; value range: [0,1].
 * If <b>fraction</b> is <b>0</b>, the return value <b>0</b> corresponds to the animation start point; any other return
 * value means that the animation jumps at the start point.
 * If <b>fraction</b> is <b>1</b>, the return value <b>1</b> corresponds to the animation end point; any other return
 * value means that the end value of the animation is not the value of the state variable,
 * which will result in an effect of transition from that end value to the value of the state variable.
 * @return Returns the pointer to the interpolation object of the curve.
 *         Returns <b>NULL</b> if a parameter error occurs.
 */
ArkUI_CurveHandle OH_ArkUI_Curve_CreateCustomCurve(
    void* userData, float (*interpolate)(float fraction, void* userdata));

/**
 * @brief Disposes of a custom curve.
 *
 * @param curve Indicates the pointer to the interpolation object of the curve.
 */
void OH_ArkUI_Curve_DisposeCurve(ArkUI_CurveHandle curveHandle);

/**
 * @brief Creates an opacity object for component transition.
 *
 * @note If the value specified is less than 0, the value <b>0</b> is used. If the value specified is greater than 1,
 * the value <b>1</b> is used.
 * @param opacity Indicates the opacity. Value range: [0, 1].
 * @return Returns the created opacity object for component transition.
 * @since 12
 */
ArkUI_TransitionEffect* OH_ArkUI_CreateOpacityTransitionEffect(float opacity);

/**
 * @brief Creates a translation object for component transition.
 *
 * @param translate Indicates the translation settings for component transition.
 * @return Returns the translation object created for component transition.
 *         Returns <b>NULL</b> if a parameter error occurs.
 * @since 12
 */
ArkUI_TransitionEffect* OH_ArkUI_CreateTranslationTransitionEffect(ArkUI_TranslationOptions* translate);

/**
 * @brief Creates a scaling object for component transition.
 *
 * @param scale Indicates the scaling settings for component transition.
 * @return Returns the scaling object created for component transition.
 *         Returns <b>NULL</b> if a parameter error occurs.
 * @since 12
 */
ArkUI_TransitionEffect* OH_ArkUI_CreateScaleTransitionEffect(ArkUI_ScaleOptions* scale);

/**
 * @brief Creates a rotation object for component transition.
 *
 * @param rotate Indicates the rotation settings for component transition.
 * @return Returns the rotation object created for component transition.
 *         Returns <b>NULL</b> if a parameter error occurs.
 * @since 12
 */
ArkUI_TransitionEffect* OH_ArkUI_CreateRotationTransitionEffect(ArkUI_RotationOptions* rotate);

/**
 * @brief Creates a movement object for component transition.
 *
 * @param edge Indicates the movement type.
 * @return Returns the movement object created for component transition.
 *         Returns <b>NULL</b> if a parameter error occurs.
 * @since 12
 */
ArkUI_TransitionEffect* OH_ArkUI_CreateMovementTransitionEffect(ArkUI_TransitionEdge edge);

/**
 * @brief Creates an asymmetric transition effect.
 *
 * @note If the <b>asymmetric</b> function is not used for <b>TransitionEffect</b>, the transition effect takes effect
 * for both appearance and disappearance of the component.
 * @param appear Indicates the transition effect for appearance.
 * @param disappear Indicates the transition effect for disappearance.
 * @return Returns the asymmetric transition effect.
 *         Returns <b>NULL</b> if a parameter error occurs.
 * @since 12
 */
ArkUI_TransitionEffect* OH_ArkUI_CreateAsymmetricTransitionEffect(
    ArkUI_TransitionEffect* appear, ArkUI_TransitionEffect* disappear);

/**
 * @brief Disposes of a transition effect.
 *
 * @param effect Indicates the transition effect to dispose of.
 * @since 12
 */
void OH_ArkUI_TransitionEffect_Dispose(ArkUI_TransitionEffect* effect);

/**
 * @brief Sets a combination of transition effects.
 *
 * @param firstEffect Indicates the transition effect options.
 * @param secondEffect Indicates the combination of transition effects.
 * @return Returns the error code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 12
 */
int32_t OH_ArkUI_TransitionEffect_Combine(
    ArkUI_TransitionEffect* firstEffect, ArkUI_TransitionEffect* secondEffect);

/**
 * @brief Sets transition effect animation settings.
 *
 * @note If <b>combine</b> is used for combining transition effects, the animation settings of a transition effect are
 * applicable to the one following it.
 * @param effect Indicates the transition effect options.
 * @param animation Indicates the animation settings.
 * @return Returns the error code.
 *         Returns {@link ARKUI_ERROR_CODE_NO_ERROR} if the operation is successful.
 *         Returns {@link ARKUI_ERROR_CODE_PARAM_INVALID} if a parameter error occurs.
 * @since 12
 */
int32_t OH_ArkUI_TransitionEffect_SetAnimation(
    ArkUI_TransitionEffect* effect, ArkUI_AnimateOption* animation);
#ifdef __cplusplus
};
#endif

#endif // ARKUI_NATIVE_ANIMATE_H