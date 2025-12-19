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
 * @file drawing_pen.h
 *
 * @brief Declares functions related to the <b>pen</b> object in the drawing module.
 *
 * @kit ArkGraphics2D
 * @library libnative_drawing.so
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @since 8
 * @version 1.0
 */

#ifndef C_INCLUDE_DRAWING_PEN_H
#define C_INCLUDE_DRAWING_PEN_H

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
 * @brief Creates an <b>OH_Drawing_Pen</b> object.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @return Returns the pointer to the <b>OH_Drawing_Pen</b> object created.
 * @since 8
 * @version 1.0
 */
OH_Drawing_Pen* OH_Drawing_PenCreate(void);

/**
 * @brief Creates an <b>OH_Drawing_Pen</b> copy object.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param pen Indicates the pointer to an <b>OH_Drawing_Pen</b> object.
 * @return Returns the pointer to the <b>OH_Drawing_Pen</b> object created.
 *         If nullptr is returned, the creation fails.
 *         The possible cause of the failure is that the available memory is empty or a nullptr is passed.
 * @since 12
 * @version 1.0
 */
OH_Drawing_Pen* OH_Drawing_PenCopy(OH_Drawing_Pen* pen);

/**
 * @brief Destroys an <b>OH_Drawing_Pen</b> object and reclaims the memory occupied by the object.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param pen Indicates the pointer to an <b>OH_Drawing_Pen</b> object.
 * @since 8
 * @version 1.0
 */
void OH_Drawing_PenDestroy(OH_Drawing_Pen* pen);

/**
 * @brief Checks whether anti-aliasing is enabled for a pen. If anti-aliasing is enabled,
 * edges will be drawn with partial transparency.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param pen Indicates the pointer to an <b>OH_Drawing_Pen</b> object.
 * @return Returns <b>true</b> if anti-aliasing is enabled; returns <b>false</b> otherwise.
 * @since 8
 * @version 1.0
 */
bool OH_Drawing_PenIsAntiAlias(const OH_Drawing_Pen* pen);

/**
 * @brief Enables or disables anti-aliasing for a pen. If anti-aliasing is enabled,
 * edges will be drawn with partial transparency.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param pen Indicates the pointer to an <b>OH_Drawing_Pen</b> object.
 * @param antiAlias Specifies whether to enable anti-aliasing. The value <b>true</b> means
 *             to enable anti-aliasing, and <b>false</b> means the opposite.
 * @since 8
 * @version 1.0
 */
void OH_Drawing_PenSetAntiAlias(OH_Drawing_Pen* pen, bool antiAlias);

/**
 * @brief Obtains the color of a pen. The color is used by the pen to outline a shape.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param pen Indicates the pointer to an <b>OH_Drawing_Pen</b> object.
 * @return Returns a 32-bit (ARGB) variable that describes the color.
 * @since 8
 * @version 1.0
 */
uint32_t OH_Drawing_PenGetColor(const OH_Drawing_Pen* pen);

/**
 * @brief Sets the color for a pen. The color is used by the pen to outline a shape.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param pen Indicates the pointer to an <b>OH_Drawing_Pen</b> object.
 * @param color Indicates the color to set, which is a 32-bit (ARGB) variable.
 * @since 8
 * @version 1.0
 */
void OH_Drawing_PenSetColor(OH_Drawing_Pen* pen, uint32_t color);

/**
 * @brief Obtains the alpha of a pen. The alpha is used by the pen to outline a shape.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param pen Indicates the pointer to an <b>OH_Drawing_Pen</b> object.
 * @return Returns a 8-bit variable that describes the alpha.
 * @since 11
 * @version 1.0
 */
uint8_t OH_Drawing_PenGetAlpha(const OH_Drawing_Pen* pen);

/**
 * @brief Sets the alpha for a pen. The alpha is used by the pen to outline a shape.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param pen Indicates the pointer to an <b>OH_Drawing_Pen</b> object.
 * @param alpha Indicates the alpha to set, which is a 8-bit variable.
 * @since 11
 * @version 1.0
 */
void OH_Drawing_PenSetAlpha(OH_Drawing_Pen* pen, uint8_t alpha);

/**
 * @brief Sets the color for a pen. The color will be used by the pen to fill in a shape.
 * The color is an ARGB structure described by floating point numbers and interpreted as being in the colorSpaceManager.
 * If colorSpaceManager is nullptr, then color is assumed to be in the sRGB color space.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param pen Indicates the pointer to an <b>OH_Drawing_Pen</b> object.
 * @param a Indicates the alpha component of color, represented as a floating point number between 0 and 1.
 * @param r Indicates the red component of color, represented as a floating point number between 0 and 1.
 * @param g Indicates the green component of color, represented as a floating point number between 0 and 1.
 * @param b Indicates the blue component of color, represented as a floating point number between 0 and 1.
 * @param colorSpaceManager Indicates the pointer to an <b>OH_NativeColorSpaceManager</b> object.
 * @return Returns the error code.
 *         Returns {@link OH_DRAWING_SUCCESS} if the operation is successful.
 *         Returns {@link OH_DRAWING_ERROR_INVALID_PARAMETER} if pen is nullptr.
 * @since 20
 * @version 1.0
 */
OH_Drawing_ErrorCode OH_Drawing_PenSetColor4f(OH_Drawing_Pen* pen, float a, float r, float g, float b,
    OH_NativeColorSpaceManager* colorSpaceManager);

/**
 * @brief Obtains the alpha component of a pen.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param pen Indicates the pointer to an <b>OH_Drawing_Pen</b> object.
 * @param a Indicates the alpha component of color.
 * @return Returns the error code.
 *         Returns {@link OH_DRAWING_SUCCESS} if the operation is successful.
 *         Returns {@link OH_DRAWING_ERROR_INVALID_PARAMETER} if pen or a is nullptr.
 * @since 20
 * @version 1.0
 */
OH_Drawing_ErrorCode OH_Drawing_PenGetAlphaFloat(OH_Drawing_Pen* pen, float* a);

/**
 * @brief Obtains the red component of a pen.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param pen Indicates the pointer to an <b>OH_Drawing_Pen</b> object.
 * @param r Indicates the red component of color.
 * @return Returns the error code.
 *         Returns {@link OH_DRAWING_SUCCESS} if the operation is successful.
 *         Returns {@link OH_DRAWING_ERROR_INVALID_PARAMETER} if pen or r is nullptr.
 * @since 20
 * @version 1.0
 */
OH_Drawing_ErrorCode OH_Drawing_PenGetRedFloat(OH_Drawing_Pen* pen, float* r);

/**
 * @brief Obtains the green component of a pen.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param pen Indicates the pointer to an <b>OH_Drawing_Pen</b> object.
 * @param g Indicates the green component of color.
 * @return Returns the error code.
 *         Returns {@link OH_DRAWING_SUCCESS} if the operation is successful.
 *         Returns {@link OH_DRAWING_ERROR_INVALID_PARAMETER} if pen or g is nullptr.
 * @since 20
 * @version 1.0
 */
OH_Drawing_ErrorCode OH_Drawing_PenGetGreenFloat(OH_Drawing_Pen* pen, float* g);

/**
 * @brief Obtains the blue component of a pen.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param pen Indicates the pointer to an <b>OH_Drawing_Pen</b> object.
 * @param b Indicates the blue component of color.
 * @return Returns the error code.
 *         Returns {@link OH_DRAWING_SUCCESS} if the operation is successful.
 *         Returns {@link OH_DRAWING_ERROR_INVALID_PARAMETER} if pen or b is nullptr.
 * @since 20
 * @version 1.0
 */
OH_Drawing_ErrorCode OH_Drawing_PenGetBlueFloat(OH_Drawing_Pen* pen, float* b);

/**
 * @brief Obtains the thickness of a pen. This thickness determines the width of the outline of a shape.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param pen Indicates the pointer to an <b>OH_Drawing_Pen</b> object.
 * @return Returns the thickness.
 * @since 8
 * @version 1.0
 */
float OH_Drawing_PenGetWidth(const OH_Drawing_Pen* pen);

/**
 * @brief Sets the thickness for a pen. This thickness determines the width of the outline of a shape.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param pen Indicates the pointer to an <b>OH_Drawing_Pen</b> object.
 * @param width Indicates the thickness to set, which is a variable.
 * @since 8
 * @version 1.0
 */
void OH_Drawing_PenSetWidth(OH_Drawing_Pen* pen, float width);

/**
 * @brief Obtains the stroke miter limit of a polyline drawn by a pen.
 *
 * When the corner type is bevel, a beveled corner is displayed if the miter limit is exceeded,
 * and a mitered corner is displayed if the miter limit is not exceeded.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param pen Indicates the pointer to an <b>OH_Drawing_Pen</b> object.
 * @return Returns the miter limit.
 * @since 8
 * @version 1.0
 */
float OH_Drawing_PenGetMiterLimit(const OH_Drawing_Pen* pen);

/**
 * @brief Sets the stroke miter limit for a polyline drawn by a pen.
 *
 * When the corner type is bevel, a beveled corner is displayed if the miter limit is exceeded,
 * and a mitered corner is displayed if the miter limit is not exceeded.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param pen Indicates the pointer to an <b>OH_Drawing_Pen</b> object.
 * @param miter Indicates a variable that describes the miter limit.
 * @since 8
 * @version 1.0
 */
void OH_Drawing_PenSetMiterLimit(OH_Drawing_Pen* pen, float miter);

/**
 * @brief Enumerates line cap styles of a pen. The line cap style defines
 * the style of both ends of a line segment drawn by the pen.
 *
 * @since 8
 * @version 1.0
 */
typedef enum {
    /**
     * There is no cap style. Both ends of the line segment are cut off square.
     */
    LINE_FLAT_CAP,
    /**
     * Square cap style. Both ends have a square, the height of which
     * is half of the width of the line segment, with the same width.
     */
    LINE_SQUARE_CAP,
    /**
     * Round cap style. Both ends have a semicircle centered, the diameter of which
     * is the same as the width of the line segment.
     */
    LINE_ROUND_CAP
} OH_Drawing_PenLineCapStyle;

/**
 * @brief Obtains the line cap style of a pen.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param pen Indicates the pointer to an <b>OH_Drawing_Pen</b> object.
 * @return Returns the line cap style.
 * @since 8
 * @version 1.0
 */
OH_Drawing_PenLineCapStyle OH_Drawing_PenGetCap(const OH_Drawing_Pen* pen);

/**
 * @brief Sets the line cap style for a pen.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param pen Indicates the pointer to an <b>OH_Drawing_Pen</b> object.
 * @param capStyle Indicates a variable that describes the line cap style.
 * @since 8
 * @version 1.0
 */
void OH_Drawing_PenSetCap(OH_Drawing_Pen* pen, OH_Drawing_PenLineCapStyle capStyle);

/**
 * @brief Enumerates pen line join styles. The line join style defines
 * the shape of the joints of a polyline segment drawn by the pen.
 *
 * @since 8
 * @version 1.0
 */
typedef enum {
    /**
     * Mitered corner. If the angle of a polyline is small, its miter length may be inappropriate.
     * In this case, you need to use the miter limit to limit the miter length.
     */
    LINE_MITER_JOIN,
    /** Round corner. */
    LINE_ROUND_JOIN,
    /** Beveled corner. */
    LINE_BEVEL_JOIN
} OH_Drawing_PenLineJoinStyle;

/**
 * @brief Obtains the line join style of a pen.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param pen Indicates the pointer to an <b>OH_Drawing_Pen</b> object.
 * @return Returns the line join style.
 * @since 8
 * @version 1.0
 */
OH_Drawing_PenLineJoinStyle OH_Drawing_PenGetJoin(const OH_Drawing_Pen* pen);

/**
 * @brief Sets the line join style for a pen.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param pen Indicates the pointer to an <b>OH_Drawing_Pen</b> object.
 * @param joinStyle Indicates a variable that describes the line join style.
 * @since 8
 * @version 1.0
 */
void OH_Drawing_PenSetJoin(OH_Drawing_Pen* pen, OH_Drawing_PenLineJoinStyle joinStyle);

/**
 * @brief Sets the shaderEffect for a pen.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param pen Indicates the pointer to an <b>OH_Drawing_Pen</b> object.
 * @param shaderEffect Indicates the pointer to an <b>OH_Drawing_ShaderEffect</b> object.
 * @since 11
 * @version 1.0
 */
void OH_Drawing_PenSetShaderEffect(OH_Drawing_Pen* pen, OH_Drawing_ShaderEffect* shaderEffect);

/**
 * @brief Sets the shadowLayer for a pen.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param pen Indicates the pointer to an <b>OH_Drawing_Pen</b> object.
 * @param shadowLayer Indicates the pointer to an <b>OH_Drawing_ShadowLayer</b> object.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_PenSetShadowLayer(OH_Drawing_Pen* pen, OH_Drawing_ShadowLayer* shadowLayer);

/**
 * @brief Sets the pathEffect for a pen.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param pen Indicates the pointer to an <b>OH_Drawing_Pen</b> object.
 * @param pathEffect Indicates the pointer to an <b>OH_Drawing_PathEffect</b> object.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_PenSetPathEffect(OH_Drawing_Pen* pen, OH_Drawing_PathEffect* pathEffect);

/**
 * @brief Sets the filter for a pen.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param pen Indicates the pointer to an <b>OH_Drawing_Pen</b> object.
 * @param filter Indicates the pointer to an <b>OH_Drawing_Filter</b> object.
 * @since 11
 * @version 1.0
 */
void OH_Drawing_PenSetFilter(OH_Drawing_Pen* pen, OH_Drawing_Filter* filter);

/**
 * @brief Gets the filter from a pen.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param pen Indicates the pointer to an <b>OH_Drawing_Pen</b> object.
 * @param filter Indicates the pointer to an <b>OH_Drawing_Filter</b> object.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_PenGetFilter(OH_Drawing_Pen* pen, OH_Drawing_Filter* filter);

/**
 * @brief Sets a blender that implements the specified blendmode enum for a pen.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param pen Indicates the pointer to an <b>OH_Drawing_Pen</b> object.
 * @param blendMode Indicates the blend mode.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_PenSetBlendMode(OH_Drawing_Pen* pen, OH_Drawing_BlendMode blendMode);

/**
 * @brief Gets the filled equivalent of the src path.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param pen Indicates the pointer to an <b>OH_Drawing_Pen</b> object.
 * @param src Indicates the Path read to create a filled version.
 * @param dst Indicates the resulting Path.
 * @param rect Indicates the pointer to an <b>OH_Drawing_Rect</b> object that limits the PathEffect area if
                          Pen has PathEffect.
 * @param matrix Indicates the pointer to an <b>OH_Drawing_Matrix</b> object that tranfomation applied to
                          PathEffect if Pen has PathEffect.
 * @return Returns true if get successes; false if get fails.
 * @since 12
 * @version 1.0
 */
bool OH_Drawing_PenGetFillPath(OH_Drawing_Pen* pen, const OH_Drawing_Path* src, OH_Drawing_Path* dst,
    const OH_Drawing_Rect* rect, const OH_Drawing_Matrix* matrix);

/**
 * @brief Resets all pen contents to their initial values.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param pen Indicates the pointer to an <b>OH_Drawing_Pen</b> object.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_PenReset(OH_Drawing_Pen* pen);

#ifdef __cplusplus
}
#endif
/** @} */
#endif
