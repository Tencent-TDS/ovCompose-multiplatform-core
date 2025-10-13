/*
 * Copyright (c) 2021-2025 Huawei Device Co., Ltd.
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
 * @since 8
 * @version 1.0
 */

/**
 * @file drawing_brush.h
 *
 * @brief Declares functions related to the <b>brush</b> object in the drawing module.
 *
 * @kit ArkGraphics2D
 * @library libnative_drawing.so
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @since 8
 * @version 1.0
 */

#ifndef C_INCLUDE_DRAWING_BRUSH_H
#define C_INCLUDE_DRAWING_BRUSH_H

#include "drawing_error_code.h"
#include "drawing_types.h"

#ifdef __cplusplus
extern "C" {
#endif

/**
 * @brief Defines a colorspace manager. Introduces the color space information defined by ColorManager.
 * @since 20
 */
typedef struct OH_NativeColorSpaceManager OH_NativeColorSpaceManager;

/**
 * @brief Creates an <b>OH_Drawing_Brush</b> object.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @return Returns the pointer to the <b>OH_Drawing_Brush</b> object created.
 * @since 8
 * @version 1.0
 */
OH_Drawing_Brush* OH_Drawing_BrushCreate(void);

/**
 * @brief Creates an <b>OH_Drawing_Brush</b> copy object.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param brush Indicates the pointer to an <b>OH_Drawing_Brush</b> object.
 * @return Returns the pointer to the <b>OH_Drawing_Brush</b> object created.
 *         If nullptr is returned, the creation fails.
 *         The possible cause of the failure is that the available memory is empty or a nullptr is passed.
 * @since 12
 * @version 1.0
 */
OH_Drawing_Brush* OH_Drawing_BrushCopy(OH_Drawing_Brush* brush);

/**
 * @brief Destroys an <b>OH_Drawing_Brush</b> object and reclaims the memory occupied by the object.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param brush Indicates the pointer to an <b>OH_Drawing_Brush</b> object.
 * @since 8
 * @version 1.0
 */
void OH_Drawing_BrushDestroy(OH_Drawing_Brush* brush);

/**
 * @brief Checks whether anti-aliasing is enabled for a brush. If anti-aliasing is enabled,
 * edges will be drawn with partial transparency.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param brush Indicates the pointer to an <b>OH_Drawing_Brush</b> object.
 * @return Returns <b>true</b> if anti-aliasing is enabled; returns <b>false</b> otherwise.
 * @since 8
 * @version 1.0
 */
bool OH_Drawing_BrushIsAntiAlias(const OH_Drawing_Brush* brush);

/**
 * @brief Enables or disables anti-aliasing for a brush. If anti-aliasing is enabled,
 * edges will be drawn with partial transparency.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param brush Indicates the pointer to an <b>OH_Drawing_Brush</b> object.
 * @param antiAlias Specifies whether to enable anti-aliasing. The value <b>true</b> means
 *             to enable anti-aliasing, and <b>false</b> means the opposite.
 * @since 8
 * @version 1.0
 */
void OH_Drawing_BrushSetAntiAlias(OH_Drawing_Brush* brush, bool antiAlias);

/**
 * @brief Obtains the color of a brush. The color is used by the brush to fill in a shape.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param brush Indicates the pointer to an <b>OH_Drawing_Brush</b> object.
 * @return Returns a 32-bit (ARGB) variable that describes the color.
 * @since 8
 * @version 1.0
 */
uint32_t OH_Drawing_BrushGetColor(const OH_Drawing_Brush* brush);

/**
 * @brief Sets the color for a brush. The color will be used by the brush to fill in a shape.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param brush Indicates the pointer to an <b>OH_Drawing_Brush</b> object.
 * @param color Indicates the color to set, which is a 32-bit (ARGB) variable.
 * @since 8
 * @version 1.0
 */
void OH_Drawing_BrushSetColor(OH_Drawing_Brush* brush, uint32_t color);

/**
 * @brief Obtains the alpha of a brush. The alpha is used by the brush to fill in a shape.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param brush Indicates the pointer to an <b>OH_Drawing_Brush</b> object.
 * @return Returns a 8-bit variable that describes the alpha.
 * @since 11
 * @version 1.0
 */
uint8_t OH_Drawing_BrushGetAlpha(const OH_Drawing_Brush* brush);

/**
 * @brief Sets the alpha for a brush. The alpha will be used by the brush to fill in a shape.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param brush Indicates the pointer to an <b>OH_Drawing_Brush</b> object.
 * @param alpha Indicates the alpha to set, which is a 8-bit variable.
 * @since 11
 * @version 1.0
 */
void OH_Drawing_BrushSetAlpha(OH_Drawing_Brush* brush, uint8_t alpha);

/**
 * @brief Sets the color for a brush. The color will be used by the brush to fill in a shape.
 * The color is an ARGB structure described by floating point numbers and interpreted as being in the colorSpaceManager.
 * If colorSpaceManager is nullptr, then color is assumed to be in the sRGB color space.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param brush Indicates the pointer to an <b>OH_Drawing_Brush</b> object.
 * @param a Indicates the alpha component of color, represented as a floating point number between 0 and 1.
 * @param r Indicates the red component of color, represented as a floating point number between 0 and 1.
 * @param g Indicates the green component of color, represented as a floating point number between 0 and 1.
 * @param b Indicates the blue component of color, represented as a floating point number between 0 and 1.
 * @param colorSpaceManager Indicates the pointer to an <b>OH_NativeColorSpaceManager</b> object.
 * @return Returns the error code.
 *         Returns {@link OH_DRAWING_SUCCESS} if the operation is successful.
 *         Returns {@link OH_DRAWING_ERROR_INVALID_PARAMETER} if brush is nullptr.
 * @since 20
 * @version 1.0
 */
OH_Drawing_ErrorCode OH_Drawing_BrushSetColor4f(OH_Drawing_Brush* brush, float a, float r, float g, float b,
    OH_NativeColorSpaceManager* colorSpaceManager);

/**
 * @brief Obtains the alpha component of a brush.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param brush Indicates the pointer to an <b>OH_Drawing_Brush</b> object.
 * @param a Indicates the alpha component of color.
 * @return Returns the error code.
 *         Returns {@link OH_DRAWING_SUCCESS} if the operation is successful.
 *         Returns {@link OH_DRAWING_ERROR_INVALID_PARAMETER} if brush or a is nullptr.
 * @since 20
 * @version 1.0
 */
OH_Drawing_ErrorCode OH_Drawing_BrushGetAlphaFloat(const OH_Drawing_Brush* brush, float* a);

/**
 * @brief Obtains the red component of a brush.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param brush Indicates the pointer to an <b>OH_Drawing_Brush</b> object.
 * @param r Indicates the red component of color.
 * @return Returns the error code.
 *         Returns {@link OH_DRAWING_SUCCESS} if the operation is successful.
 *         Returns {@link OH_DRAWING_ERROR_INVALID_PARAMETER} if brush or r is nullptr.
 * @since 20
 * @version 1.0
 */
OH_Drawing_ErrorCode OH_Drawing_BrushGetRedFloat(const OH_Drawing_Brush* brush, float* r);

/**
 * @brief Obtains the green component of a brush.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param brush Indicates the pointer to an <b>OH_Drawing_Brush</b> object.
 * @param g Indicates the green component of color.
 * @return Returns the error code.
 *         Returns {@link OH_DRAWING_SUCCESS} if the operation is successful.
 *         Returns {@link OH_DRAWING_ERROR_INVALID_PARAMETER} if brush or g is nullptr.
 * @since 20
 * @version 1.0
 */
OH_Drawing_ErrorCode OH_Drawing_BrushGetGreenFloat(const OH_Drawing_Brush* brush, float* g);

/**
 * @brief Obtains the blue component of a brush.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param brush Indicates the pointer to an <b>OH_Drawing_Brush</b> object.
 * @param b Indicates the blue component of color.
 * @return Returns the error code.
 *         Returns {@link OH_DRAWING_SUCCESS} if the operation is successful.
 *         Returns {@link OH_DRAWING_ERROR_INVALID_PARAMETER} if brush or b is nullptr.
 * @since 20
 * @version 1.0
 */
OH_Drawing_ErrorCode OH_Drawing_BrushGetBlueFloat(const OH_Drawing_Brush* brush, float* b);

/**
 * @brief Sets the shaderEffect for a brush.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param brush Indicates the pointer to an <b>OH_Drawing_Brush</b> object.
 * @param shaderEffect Indicates the pointer to an <b>OH_Drawing_ShaderEffect</b> object.
 * @since 11
 * @version 1.0
 */
void OH_Drawing_BrushSetShaderEffect(OH_Drawing_Brush* brush, OH_Drawing_ShaderEffect* shaderEffect);

/**
 * @brief Sets the shadowLayer for a brush.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param brush Indicates the pointer to an <b>OH_Drawing_Brush</b> object.
 * @param shadowLayer Indicates the pointer to an <b>OH_Drawing_ShadowLayer</b> object.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_BrushSetShadowLayer(OH_Drawing_Brush* brush, OH_Drawing_ShadowLayer* shadowLayer);

/**
 * @brief Sets the filter for a brush.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param brush Indicates the pointer to an <b>OH_Drawing_Brush</b> object.
 * @param filter Indicates the pointer to an <b>OH_Drawing_Filter</b> object.
 * @since 11
 * @version 1.0
 */
void OH_Drawing_BrushSetFilter(OH_Drawing_Brush* brush, OH_Drawing_Filter* filter);

/**
 * @brief Gets the filter from a brush.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param brush Indicates the pointer to an <b>OH_Drawing_Brush</b> object.
 * @param filter Indicates the pointer to an <b>OH_Drawing_Filter</b> object.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_BrushGetFilter(OH_Drawing_Brush* brush, OH_Drawing_Filter* filter);

/**
 * @brief Sets a blender that implements the specified blendmode enum for a brush.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param brush Indicates the pointer to an <b>OH_Drawing_Brush</b> object.
 * @param blendMode Indicates the blend mode.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_BrushSetBlendMode(OH_Drawing_Brush* brush, OH_Drawing_BlendMode blendMode);

/**
 * @brief Resets all brush contents to their initial values.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param brush Indicates the pointer to an <b>OH_Drawing_Brush</b> object.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_BrushReset(OH_Drawing_Brush* brush);

#ifdef __cplusplus
}
#endif
/** @} */
#endif
