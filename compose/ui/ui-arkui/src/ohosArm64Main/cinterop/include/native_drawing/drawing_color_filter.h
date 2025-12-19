/*
 * Copyright (c) 2023 Huawei Device Co., Ltd.
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
 * @since 11
 * @version 1.0
 */

/**
 * @file drawing_color_filter.h
 *
 * @brief Declares functions related to the <b>colorFilter</b> object in the drawing module.
 *
 * @kit ArkGraphics2D
 * @library libnative_drawing.so
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @since 11
 * @version 1.0
 */

#ifndef C_INCLUDE_DRAWING_COLOR_FILTER_H
#define C_INCLUDE_DRAWING_COLOR_FILTER_H

#include "drawing_types.h"

#ifdef __cplusplus
extern "C" {
#endif

/**
 * @brief Creates an <b>OH_Drawing_ColorFilter</b> with a blend mode.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param color Indicates the color, which is a 32-bit (ARGB) variable.
 * @param blendMode Indicates the blend mode.
 * @return Returns the pointer to the <b>OH_Drawing_ColorFilter</b> object created.
 * @since 11
 * @version 1.0
 */
OH_Drawing_ColorFilter* OH_Drawing_ColorFilterCreateBlendMode(uint32_t color, OH_Drawing_BlendMode blendMode);

/**
 * @brief Creates an <b>OH_Drawing_ColorFilter</b> applies the outerColorFilter and then applies innerColorFilter.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param outerColorFilter Indicates the pointer to an <b>OH_Drawing_ColorFilter</b> object.
 * @param innerColorFilter Indicates the pointer to an <b>OH_Drawing_ColorFilter</b> object.
 * @return Returns the pointer to the <b>OH_Drawing_ColorFilter</b> object created.
 * @since 11
 * @version 1.0
 */
OH_Drawing_ColorFilter* OH_Drawing_ColorFilterCreateCompose(OH_Drawing_ColorFilter* outerColorFilter,
    OH_Drawing_ColorFilter* innerColorFilter);

/**
 * @brief Creates an <b>OH_Drawing_ColorFilter</b> with a 5x4 color matrix.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param matrix Indicates the matrix, which is represented as a float array of length 20.
 * @return Returns the pointer to the <b>OH_Drawing_ColorFilter</b> object created.
 * @since 11
 * @version 1.0
 */
OH_Drawing_ColorFilter* OH_Drawing_ColorFilterCreateMatrix(const float matrix[20]);

/**
 * @brief Creates an <b>OH_Drawing_ColorFilter</b> applies the gamma curve of SRGB to the RGB color channel.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @return Returns the pointer to the <b>OH_Drawing_ColorFilter</b> object created.
 * @since 11
 * @version 1.0
 */
OH_Drawing_ColorFilter* OH_Drawing_ColorFilterCreateLinearToSrgbGamma(void);

/**
 * @brief Creates an <b>OH_Drawing_ColorFilter</b> applies the RGB color channel to the gamma curve of SRGB.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @return Returns the pointer to the <b>OH_Drawing_ColorFilter</b> object created.
 * @since 11
 * @version 1.0
 */
OH_Drawing_ColorFilter* OH_Drawing_ColorFilterCreateSrgbGammaToLinear(void);

/**
 * @brief Creates an <b>OH_Drawing_ColorFilter</b> multiplies the luma of its input into the alpha channel,
 * and sets the red, green, and blue channels to zero.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @return Returns the pointer to the <b>OH_Drawing_ColorFilter</b> object created.
 * @since 11
 * @version 1.0
 */
OH_Drawing_ColorFilter* OH_Drawing_ColorFilterCreateLuma(void);

/**
 * @brief Creates an <b>OH_Drawing_ColorFilter</b> with the given mutColor used to multiply source color and addColor
 * used to add to source color. The Alpha channel will not be affected.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param mulColor Indicates the color, which is a 32-bit (ARGB) variable.
 * @param addColor Indicates the color, which is a 32-bit (ARGB) variable.
 * @return Returns the pointer to the <b>OH_Drawing_ColorFilter</b> object created.
 * @since 20
 * @version 1.0
 */
OH_Drawing_ColorFilter* OH_Drawing_ColorFilterCreateLighting(uint32_t mulColor, uint32_t addColor);

/**
 * @brief Destroys an <b>OH_Drawing_ColorFilter</b> object and reclaims the memory occupied by the object.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param colorFilter Indicates the pointer to an <b>OH_Drawing_ColorFilter</b> object.
 * @since 11
 * @version 1.0
 */
void OH_Drawing_ColorFilterDestroy(OH_Drawing_ColorFilter* colorFilter);

#ifdef __cplusplus
}
#endif
/** @} */
#endif
