/*
 * Copyright (c) 2023-2024 Huawei Device Co., Ltd.
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
 * @addtogroup Drawing
 * @{
 *
 * @brief Provides functions such as 2D graphics rendering, text drawing, and image display.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 *
 * @since 12
 * @version 1.0
 */

/**
 * @file drawing_sampling_options.h
 *
 * @brief Declares functions related to the <b>sampling options</b> object in the drawing module.
 *
 * @kit ArkGraphics2D
 * @library libnative_drawing.so
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @since 12
 * @version 1.0
 */

#ifndef C_INCLUDE_DRAWING_SAMPLING_OPTIONS_H
#define C_INCLUDE_DRAWING_SAMPLING_OPTIONS_H

#include "drawing_types.h"

#ifdef __cplusplus
extern "C" {
#endif

/**
 * @brief Enumerates storage filter mode.
 *
 * @since 12
 * @version 1.0
 */
typedef enum {
    /** single sample point (nearest neighbor) */
    FILTER_MODE_NEAREST,
    /** interporate between 2x2 sample points (bilinear interpolation) */
    FILTER_MODE_LINEAR,
} OH_Drawing_FilterMode;

/**
 * @brief Enumerates storage formats mipmap mode.
 *
 * @since 12
 * @version 1.0
 */
typedef enum {
    /** ignore mipmap levels, sample from the "base" */
    MIPMAP_MODE_NONE,
    /** sample from the nearest level */
    MIPMAP_MODE_NEAREST,
    /** interpolate between the two nearest levels */
    MIPMAP_MODE_LINEAR,
} OH_Drawing_MipmapMode;

/**
 * @brief Creates an <b>OH_Drawing_SamplingOptions</b> object.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param filterMode sampling filter mode.
 * @param mipmapMode sampling mipmap mode..
 * @return Returns the pointer to the <b>OH_Drawing_SamplingOptions</b> object created.
 * @since 12
 * @version 1.0
 */
OH_Drawing_SamplingOptions* OH_Drawing_SamplingOptionsCreate(OH_Drawing_FilterMode filterMode,
    OH_Drawing_MipmapMode mipmapMode);

/**
 * @brief Creates an <b>OH_Drawing_SamplingOptions</b> copy object.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param samplingOptions Indicates the pointer to an <b>OH_Drawing_SamplingOptions</b> object.
 * @return Returns the pointer to the <b>OH_Drawing_SamplingOptions</b> object created.
 * @since 20
 * @version 1.0
 */
OH_Drawing_SamplingOptions* OH_Drawing_SamplingOptionsCopy(OH_Drawing_SamplingOptions* samplingOptions);

/**
 * @brief Destroys an <b>OH_Drawing_SamplingOptions</b> object and reclaims the memory occupied by the object.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param samplingOptions Indicates the pointer to an <b>OH_Drawing_SamplingOptions</b> object.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_SamplingOptionsDestroy(OH_Drawing_SamplingOptions* samplingOptions);

#ifdef __cplusplus
}
#endif
/** @} */
#endif
