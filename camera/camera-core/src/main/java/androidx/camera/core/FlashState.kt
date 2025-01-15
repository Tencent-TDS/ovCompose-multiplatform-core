/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.core

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.camera.core.ImageCapture.OnImageCapturedCallback

/**
 * The camera flash state values represent the state of the physical flash unit of a camera.
 *
 * The possible values are [FLASH_STATE_UNKNOWN], [FLASH_STATE_FIRED], [FLASH_STATE_UNAVAILABLE],
 * and [FLASH_STATE_READY].
 *
 * In case of any error, how it is notified depends on the API that is used for obtaining the
 * [FlashState]. For example, if the flash state is obtained by invoking
 * [ImageCapture.takePicture(Executor, OnImageCapturedCallback)][ImageCapture.takePicture] and using
 * the returned [ImageProxy] to use [ImageInfo.getFlashState], any related error will be notified
 * via [OnImageCapturedCallback.onError].
 */
public object FlashState {
    /** The camera flash state. */
    @IntDef(FLASH_STATE_UNKNOWN, FLASH_STATE_FIRED, FLASH_STATE_UNAVAILABLE, FLASH_STATE_READY)
    @Retention(AnnotationRetention.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public annotation class FlashState

    /**
     * Unknown flash state.
     *
     * If a device does not report the flash state, this is used. For example, legacy camera devices
     * don't report the flash state in certain conditions.
     *
     * @see FlashState
     */
    public const val FLASH_STATE_UNKNOWN: Int = 0

    /**
     * State indicating the flash was fired.
     *
     * This state will always be reported if an image is captured with [ImageCapture.FLASH_MODE_ON]
     * or [CameraControl.enableTorch] is used. When [ImageCapture.FLASH_MODE_AUTO] is used, this
     * state will be reported if the flash was actually triggered which depends on the lighting
     * condition of the capture scene.
     *
     * @see FlashState
     */
    public const val FLASH_STATE_FIRED: Int = 1

    /**
     * State indicating that flash is unavailable.
     *
     * If a camera device does not include a physical flash unit or the physical flash is not
     * available for usage, this state is used.
     *
     * @see FlashState
     */
    public const val FLASH_STATE_UNAVAILABLE: Int = 2

    /**
     * State indicating that flash is ready and can be fired if required.
     *
     * This is used when the flash is available to be fired but not fired yet.
     *
     * @see FlashState
     */
    public const val FLASH_STATE_READY: Int = 3
}
