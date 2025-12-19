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
 * @file drawing_path.h
 *
 * @brief Declares functions related to the <b>path</b> object in the drawing module.
 *
 * @kit ArkGraphics2D
 * @library libnative_drawing.so
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @since 8
 * @version 1.0
 */

#ifndef C_INCLUDE_DRAWING_PATH_H
#define C_INCLUDE_DRAWING_PATH_H

#include "drawing_error_code.h"
#include "drawing_types.h"

#ifdef __cplusplus
extern "C" {
#endif

/**
 * @brief Direction for adding closed contours.
 *
 * @since 12
 * @version 1.0
 */
typedef enum {
    /** clockwise direction for adding closed contours */
    PATH_DIRECTION_CW,
    /** counter-clockwise direction for adding closed contours */
    PATH_DIRECTION_CCW,
} OH_Drawing_PathDirection;

/**
 * @brief FillType of path.
 *
 * @since 12
 * @version 1.0
 */
typedef enum {
    /** Specifies that "inside" is computed by a non-zero sum of signed edge crossings */
    PATH_FILL_TYPE_WINDING,
    /** Specifies that "inside" is computed by an odd number of edge crossings */
    PATH_FILL_TYPE_EVEN_ODD,
    /** Same as Winding, but draws outside of the path, rather than inside */
    PATH_FILL_TYPE_INVERSE_WINDING,
    /** Same as EvenOdd, but draws outside of the path, rather than inside */
    PATH_FILL_TYPE_INVERSE_EVEN_ODD,
} OH_Drawing_PathFillType;

/**
 * @brief Add mode of path.
 *
 * @since 12
 * @version 1.0
 */
typedef enum {
    /** Appended to destination unaltered */
    PATH_ADD_MODE_APPEND,
    /** Add line if prior contour is not closed */
    PATH_ADD_MODE_EXTEND,
} OH_Drawing_PathAddMode;

/**
 * @brief Operations when two paths are combined.
 *
 * @since 12
 * @version 1.0
 */
typedef enum {
    /**
     * Difference operation.
     */
    PATH_OP_MODE_DIFFERENCE,
    /**
     * Intersect operation.
     */
    PATH_OP_MODE_INTERSECT,
    /**
     * Union operation.
     */
    PATH_OP_MODE_UNION,
    /**
     * Xor operation.
     */
    PATH_OP_MODE_XOR,
    /**
     * Reverse difference operation.
     */
    PATH_OP_MODE_REVERSE_DIFFERENCE,
} OH_Drawing_PathOpMode;

/**
 * @brief Enumerates the matrix information corresponding to the path measurements.
 *
 * @since 12
 * @version 1.0
 */
typedef enum {
    /**
     * Gets position.
     */
    GET_POSITION_MATRIX,
    /**
     * Gets tangent.
     */
    GET_TANGENT_MATRIX,
    /**
     * Gets both position and tangent.
     */
    GET_POSITION_AND_TANGENT_MATRIX,
} OH_Drawing_PathMeasureMatrixFlags;

/**
 * @brief Creates an <b>OH_Drawing_Path</b> object.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @return Returns the pointer to the <b>OH_Drawing_Path</b> object created.
 * @since 8
 * @version 1.0
 */
OH_Drawing_Path* OH_Drawing_PathCreate(void);

/**
 * @brief Creates an <b>OH_Drawing_Path</b> copy object.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param path Indicates the pointer to an <b>OH_Drawing_Rect</b> object.
 * @return Returns the pointer to the <b>OH_Drawing_Path</b> object created.
 * @since 12
 * @version 1.0
 */
OH_Drawing_Path* OH_Drawing_PathCopy(OH_Drawing_Path* path);

/**
 * @brief Destroys an <b>OH_Drawing_Path</b> object and reclaims the memory occupied by the object.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param path Indicates the pointer to an <b>OH_Drawing_Path</b> object.
 * @since 8
 * @version 1.0
 */
void OH_Drawing_PathDestroy(OH_Drawing_Path* path);

/**
 * @brief Sets <b>OH_Drawing_Path</b> object with the same content of another.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param path Indicates the pointer to an <b>OH_Drawing_Path</b> object.
 * @param other Indicates the pointer to an <b>OH_Drawing_Path</b> object to copy content from.
 * @return Returns the error code.
 *         Returns {@link OH_DRAWING_SUCCESS} if the operation is successful.
 *         Returns {@link OH_DRAWING_ERROR_INVALID_PARAMETER} if path or other is nullptr.
 * @since 20
 * @version 1.0
 */
OH_Drawing_ErrorCode OH_Drawing_PathSetPath(OH_Drawing_Path* path, OH_Drawing_Path* other);

/**
 * @brief Checks if <b>OH_Drawing_Path</b> object is empty.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param path Indicates the pointer to an <b>OH_Drawing_Path</b> object.
 * @param isEmpty Indicates the return value.
 * @return Returns the error code.
 *         Returns {@link OH_DRAWING_SUCCESS} if the operation is successful.
 *         Returns {@link OH_DRAWING_ERROR_INVALID_PARAMETER} if path or isEmpty is nullptr.
 * @since 20
 * @version 1.0
 */
OH_Drawing_ErrorCode OH_Drawing_PathIsEmpty(OH_Drawing_Path* path, bool* isEmpty);

/**
 * @brief Checks if <b>OH_Drawing_Path</b> represents a rectangle.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param path Indicates the pointer to an <b>OH_Drawing_Path</b> object.
 * @param rect Indicates the Pointer to an <b>OH_Drawing_Rect</b> object.
 * @param isRect Indicates the return value.
 * @return Returns the error code.
 *         Returns {@link OH_DRAWING_SUCCESS} if the operation is successful.
 *         Returns {@link OH_DRAWING_ERROR_INVALID_PARAMETER} if path or isRect is nullptr.
 * @since 20
 * @version 1.0
 */
OH_Drawing_ErrorCode OH_Drawing_PathIsRect(OH_Drawing_Path* path, OH_Drawing_Rect* rect, bool* isRect);

/**
 * @brief Sets the start point of a path.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param path Indicates the pointer to an <b>OH_Drawing_Path</b> object.
 * @param x Indicates the x coordinate of the start point.
 * @param y Indicates the y coordinate of the start point.
 * @since 8
 * @version 1.0
 */
void OH_Drawing_PathMoveTo(OH_Drawing_Path* path, float x, float y);

/**
 * @brief Draws a line segment from the last point of a path to the target point.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param path Indicates the pointer to an <b>OH_Drawing_Path</b> object.
 * @param x Indicates the x coordinate of the target point.
 * @param y Indicates the y coordinate of the target point.
 * @since 8
 * @version 1.0
 */
void OH_Drawing_PathLineTo(OH_Drawing_Path* path, float x, float y);

/**
 * @brief Draws an arc to a path. 
 * 
 * This is done by using angle arc mode. In this mode, a rectangle that encloses an ellipse is specified first,
 * and then a start angle and a sweep angle are specified.
 * The arc is a portion of the ellipse defined by the start angle and the sweep angle. 
 * By default, a line segment from the last point of the path to the start point of the arc is also added.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param path Indicates the pointer to an <b>OH_Drawing_Path</b> object.
 * @param x1 Indicates the x coordinate of the upper left corner of the rectangle.
 * @param y1 Indicates the y coordinate of the upper left corner of the rectangle.
 * @param x2 Indicates the x coordinate of the lower right corner of the rectangle.
 * @param y2 Indicates the y coordinate of the lower right corner of the rectangle.
 * @param startDeg Indicates the start angle, in degrees.
 * @param sweepDeg Indicates the angle to sweep, in degrees.
 * @since 8
 * @version 1.0
 */
void OH_Drawing_PathArcTo(OH_Drawing_Path* path,
    float x1, float y1, float x2, float y2, float startDeg, float sweepDeg);

/**
 * @brief Draws a quadratic Bezier curve from the last point of a path to the target point.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param path Indicates the pointer to an <b>OH_Drawing_Path</b> object.
 * @param ctrlX Indicates the x coordinate of the control point.
 * @param ctrlY Indicates the y coordinate of the control point.
 * @param endX Indicates the x coordinate of the target point.
 * @param endY Indicates the y coordinate of the target point.
 * @since 8
 * @version 1.0
 */
void OH_Drawing_PathQuadTo(OH_Drawing_Path* path, float ctrlX, float ctrlY, float endX, float endY);

/**
 * @brief Draws a conic from the last point of a path to the target point.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param path Indicates the pointer to an <b>OH_Drawing_Path</b> object.
 * @param ctrlX Indicates the x coordinate of the control point.
 * @param ctrlY Indicates the y coordinate of the control point.
 * @param endX Indicates the x coordinate of the target point.
 * @param endY Indicates the y coordinate of the target point.
 * @param weight Indicates the weight of added conic.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_PathConicTo(OH_Drawing_Path* path, float ctrlX, float ctrlY, float endX, float endY, float weight);

/**
 * @brief Draws a cubic Bezier curve from the last point of a path to the target point.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param path Indicates the pointer to an <b>OH_Drawing_Path</b> object.
 * @param ctrlX1 Indicates the x coordinate of the first control point.
 * @param ctrlY1 Indicates the y coordinate of the first control point.
 * @param ctrlX2 Indicates the x coordinate of the second control point.
 * @param ctrlY2 Indicates the y coordinate of the second control point.
 * @param endX Indicates the x coordinate of the target point.
 * @param endY Indicates the y coordinate of the target point.
 * @since 8
 * @version 1.0
 */
void OH_Drawing_PathCubicTo(
    OH_Drawing_Path* path, float ctrlX1, float ctrlY1, float ctrlX2, float ctrlY2, float endX, float endY);

/**
 * @brief Sets the relative starting point of a path.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param path Indicates the pointer to an <b>OH_Drawing_Path</b> object.
 * @param x Indicates the x coordinate of the relative starting point.
 * @param y Indicates the y coordinate of the relative starting point.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_PathRMoveTo(OH_Drawing_Path* path, float x, float y);

/**
 * @brief Draws a line segment from the last point of a path to the relative target point.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param path Indicates the pointer to an <b>OH_Drawing_Path</b> object.
 * @param x Indicates the x coordinate of the relative target point.
 * @param y Indicates the y coordinate of the relative target point.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_PathRLineTo(OH_Drawing_Path* path, float x, float y);

/**
 * @brief Draws a quadratic bezier curve from the last point of a path to the relative target point.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param path Indicates the pointer to an <b>OH_Drawing_Path</b> object.
 * @param ctrlX Indicates the x coordinate of the relative control point.
 * @param ctrlY Indicates the y coordinate of the relative control point.
 * @param endX Indicates the x coordinate of the relative target point.
 * @param endY Indicates the y coordinate of the relative target point.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_PathRQuadTo(OH_Drawing_Path* path, float ctrlX, float ctrlY, float endX, float endY);

/**
 * @brief Draws a conic from the last point of a path to the relative target point.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param path Indicates the pointer to an <b>OH_Drawing_Path</b> object.
 * @param ctrlX Indicates the x coordinate of the relative control point.
 * @param ctrlY Indicates the y coordinate of the relative control point.
 * @param endX Indicates the x coordinate of the relative target point.
 * @param endY Indicates the y coordinate of the relative target point.
 * @param weight Indicates the weight of added conic.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_PathRConicTo(OH_Drawing_Path* path, float ctrlX, float ctrlY, float endX, float endY, float weight);

/**
 * @brief Draws a cubic bezier curve from the last point of a path to the relative target point.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param path Indicates the pointer to an <b>OH_Drawing_Path</b> object.
 * @param ctrlX1 Indicates the x coordinate of the first relative control point.
 * @param ctrlY1 Indicates the y coordinate of the first relative control point.
 * @param ctrlX2 Indicates the x coordinate of the second relative control point.
 * @param ctrlY2 Indicates the y coordinate of the second relative control point.
 * @param endX Indicates the x coordinate of the relative target point.
 * @param endY Indicates the y coordinate of the relative target point.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_PathRCubicTo(OH_Drawing_Path* path, float ctrlX1, float ctrlY1, float ctrlX2, float ctrlY2,
    float endX, float endY);

/**
 * @brief Adds a new contour to the path, defined by the rect, and wound in the specified direction.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param path Indicates the pointer to an <b>OH_Drawing_Path</b> object.
 * @param left Indicates the left coordinate of the upper left corner of the rectangle.
 * @param top Indicates the top coordinate of the upper top corner of the rectangle.
 * @param right Indicates the right coordinate of the lower right corner of the rectangle.
 * @param bottom Indicates the bottom coordinate of the lower bottom corner of the rectangle.
 * @param pathDirection Indicates the path direction.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_PathAddRect(OH_Drawing_Path* path, float left, float top, float right, float bottom,
    OH_Drawing_PathDirection pathDirection);

/**
 * @brief Adds a new contour to the path, defined by the rect, and wound in the specified direction.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param path Indicates the pointer to an <b>OH_Drawing_Path</b> object.
 * @param rect Indicates the pointer to an <b>OH_Drawing_Rect</b> object.
 * @param pathDirection Indicates the path direction.
 * @param start Indicates initial corner of rect to add.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_PathAddRectWithInitialCorner(OH_Drawing_Path* path, const OH_Drawing_Rect* rect,
    OH_Drawing_PathDirection pathDirection, uint32_t start);

/**
 * @brief Adds a new contour to the path, defined by the round rect, and wound in the specified direction.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param path Indicates the pointer to an <b>OH_Drawing_Path</b> object.
 * @param roundRect Indicates the pointer to an <b>OH_Drawing_RoundRect</b> object.
 * @param pathDirection Indicates the path direction.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_PathAddRoundRect(OH_Drawing_Path* path,
    const OH_Drawing_RoundRect* roundRect, OH_Drawing_PathDirection pathDirection);

/**
 * @brief Adds a oval to the path, defined by the rect, and wound in the specified direction.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param path Indicates the pointer to an <b>OH_Drawing_Path</b> object.
 * @param rect Indicates the pointer to an <b>OH_Drawing_Rect</b> object.
 * @param start Index of initial point of ellipse.
 * @param pathDirection Indicates the path direction.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_PathAddOvalWithInitialPoint(OH_Drawing_Path* path, const OH_Drawing_Rect* rect,
    uint32_t start, OH_Drawing_PathDirection pathDirection);

/**
 * @brief Adds a oval to the path, defined by the rect, and wound in the specified direction.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param path Indicates the pointer to an <b>OH_Drawing_Path</b> object.
 * @param rect Indicates the pointer to an <b>OH_Drawing_Rect</b> object.
 * @param pathDirection Indicates the path direction.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_PathAddOval(OH_Drawing_Path* path,
    const OH_Drawing_Rect* rect, OH_Drawing_PathDirection pathDirection);

/**
 * @brief Appends arc to path, as the start of new contour.Arc added is part of ellipse bounded by oval,
 * from startAngle through sweepAngle. Both startAngle and sweepAngle are measured in degrees, where zero degrees 
 * is aligned with the positive x-axis, and positive sweeps extends arc clockwise.If sweepAngle <= -360, or
 * sweepAngle >= 360; and startAngle modulo 90 is nearly zero, append oval instead of arc. Otherwise, sweepAngle
 * values are treated modulo 360, and arc may or may not draw depending on numeric rounding.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param path Indicates the pointer to an <b>OH_Drawing_Path</b> object.
 * @param rect Indicates the pointer to an <b>OH_Drawing_Rect</b> object.
 * @param startAngle Indicates the starting angle of arc in degrees.
 * @param sweepAngle Indicates the sweep, in degrees. Positive is clockwise.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_PathAddArc(OH_Drawing_Path* path, const OH_Drawing_Rect* rect, float startAngle, float sweepAngle);

/**
 * @brief Appends src path to path, transformed by matrix. Transformed curves may have different verbs,
 * point, and conic weights.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param path Indicates the pointer to an <b>OH_Drawing_Path</b> object.
 * @param src Indicates the pointer to an <b>OH_Drawing_Path</b> object.
 * @param matrix Indicates the length of the <b>OH_Drawing_Matrix</b> object.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_PathAddPath(OH_Drawing_Path* path, const OH_Drawing_Path* src, const OH_Drawing_Matrix* matrix);

/**
 * @brief Appends src path to path, transformed by matrix and mode. Transformed curves may have different verbs,
 * point, and conic weights.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param path Indicates the pointer to an <b>OH_Drawing_Path</b> object.
 * @param src Indicates the pointer to an <b>OH_Drawing_Path</b> object.
 * @param matrix Indicates the length of the <b>OH_Drawing_Matrix</b> object.
 * @param pathAddMode Indicates the add path's add mode.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_PathAddPathWithMatrixAndMode(OH_Drawing_Path* path, const OH_Drawing_Path* src,
    const OH_Drawing_Matrix* matrix, OH_Drawing_PathAddMode pathAddMode);

/**
 * @brief Appends src path to path, transformed by mode. Transformed curves may have different verbs,
 * point, and conic weights.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param path Indicates the pointer to an <b>OH_Drawing_Path</b> object.
 * @param src Indicates the pointer to an <b>OH_Drawing_Path</b> object, which is Appends src path to path.
 * @param pathAddMode Indicates the add path's add mode.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_PathAddPathWithMode(OH_Drawing_Path* path,
    const OH_Drawing_Path* src, OH_Drawing_PathAddMode pathAddMode);

/**
 * @brief Appends src path to path, transformed by offset and mode. Transformed curves may have different verbs,
 * point, and conic weights.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param path Indicates the pointer to an <b>OH_Drawing_Path</b> object.
 * @param src Indicates the pointer to an <b>OH_Drawing_Path</b> object.
 * @param dx Indicates offset added to src path x-axis coordinates.
 * @param dy Indicates offset added to src path y-axis coordinates.
 * @param pathAddMode Indicates the add path's add mode.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_PathAddPathWithOffsetAndMode(OH_Drawing_Path* path, const OH_Drawing_Path* src, float dx, float dy,
    OH_Drawing_PathAddMode pathAddMode);

/**
 * @brief Adds contour created from point array, adding (count - 1) line segments.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param path Indicates the pointer to an <b>OH_Drawing_Path</b> object.
 * @param points Indicates the point array.
 * @param count Indicates the size of point array.
 * @param isClosed Indicates Whether to add lines that connect the end and start.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_PathAddPolygon(OH_Drawing_Path* path, const OH_Drawing_Point2D* points, uint32_t count, bool isClosed);

/**
 * @brief  Adds a circle to the path, and wound in the specified direction.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param path Indicates the pointer to an <b>OH_Drawing_Path</b> object.
 * @param x Indicates the x coordinate of the center of the circle.
 * @param y Indicates the y coordinate of the center of the circle.
 * @param radius Indicates the radius of the circle.
 * @param pathDirection Indicates the path direction.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_PathAddCircle(OH_Drawing_Path* path,
    float x, float y, float radius, OH_Drawing_PathDirection pathDirection);

/**
 * @brief Parses the svg path from the string.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param path Indicates the pointer to an <b>OH_Drawing_Path</b> object.
 * @param str Indicates the string of the SVG path.
 * @return Returns true if build path is successful, returns false otherwise.
 * @since 12
 * @version 1.0
 */
bool OH_Drawing_PathBuildFromSvgString(OH_Drawing_Path* path, const char* str);

/**
 * @brief Return the status that point (x, y) is contained by path.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param path Indicates the pointer to an <b>OH_Drawing_Path</b> object.
 * @param x Indicates the x-axis value of containment test.
 * @param y Indicates the y-axis value of containment test.
 * @return Returns true if the point (x, y) is contained by path.
 * @since 12
 * @version 1.0
 */
bool OH_Drawing_PathContains(OH_Drawing_Path* path, float x, float y);

/**
 * @brief Transforms verb array, point array, and weight by matrix. transform may change verbs
 * and increase their number. path is replaced by transformed data.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param path Indicates the pointer to an <b>OH_Drawing_Path</b> object.
 * @param matrix Indicates the pointer to an <b>OH_Drawing_Matrix</b> object.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_PathTransform(OH_Drawing_Path* path, const OH_Drawing_Matrix* matrix);

/**
 * @brief Transforms verb array, point array, and weight by matrix.
 * Transform may change verbs and increase their number.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param src Indicates the pointer to an <b>OH_Drawing_Path</b> object.
 * @param matrix Indicates the pointer to an <b>OH_Drawing_Matrix</b> object.
 * @param dst Indicates the pointer to an <b>OH_Drawing_Path</b> object.
 * @param applyPerspectiveClip Indicates whether to apply perspective clip.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_PathTransformWithPerspectiveClip(OH_Drawing_Path* src, const OH_Drawing_Matrix* matrix,
    OH_Drawing_Path* dst, bool applyPerspectiveClip);

/**
 * @brief Sets FillType, the rule used to fill path.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param path Indicates the pointer to an <b>OH_Drawing_Path</b> object.
 * @param pathFillType Indicates the add path's fill type.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_PathSetFillType(OH_Drawing_Path* path, OH_Drawing_PathFillType pathFillType);

/**
 * @brief Gets FillType, the rule used to fill path.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param path Indicates the pointer to an <b>OH_Drawing_Path</b> object.
 * @param pathFillType Indicates the FillType apply to path.
 * @return Returns the error code.
 *         Returns {@link OH_DRAWING_SUCCESS} if the operation is successful.
 *         Returns {@link OH_DRAWING_ERROR_INVALID_PARAMETER} if path or pathFillType is nullptr.
 * @since 20
 * @version 1.0
 */
OH_Drawing_ErrorCode OH_Drawing_PathGetFillType(OH_Drawing_Path* path, OH_Drawing_PathFillType* pathFillType);

/**
 * @brief Gets the length of the current path object.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param path Indicates the pointer to an <b>OH_Drawing_Path</b> object.
 * @param forceClosed Indicates whether free to modify/delete the path after this call.
 * @return Returns the length of the current path object.
 * @since 12
 * @version 1.0
 */
float OH_Drawing_PathGetLength(OH_Drawing_Path* path, bool forceClosed);

/**
 * @brief Gets the smallest bounding box that contains the path.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param path Indicates the pointer to an <b>OH_Drawing_Path</b> object.
 * @param rect Indicates the pointer to an <b>OH_Drawing_Rect</b> object.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_PathGetBounds(OH_Drawing_Path* path, OH_Drawing_Rect* rect);

/**
 * @brief Closes a path. A line segment from the start point to the last point of the path is added.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param path Indicates the pointer to an <b>OH_Drawing_Path</b> object.
 * @since 8
 * @version 1.0
 */
void OH_Drawing_PathClose(OH_Drawing_Path* path);

/**
 * @brief Offset path replaces dst.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param path Indicates the pointer to an <b>OH_Drawing_Path</b> object.
 * @param dst Indicates the pointer to an <b>OH_Drawing_Path</b> object.
 * @param dx Indicates offset added to dst path x-axis coordinates.
 * @param dy Indicates offset added to dst path y-axis coordinates.
 * @since 12
 * @version 1.0
 */
void OH_Drawing_PathOffset(OH_Drawing_Path* path, OH_Drawing_Path* dst, float dx, float dy);

/**
 * @brief Resets path data.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param path Indicates the pointer to an <b>OH_Drawing_Path</b> object.
 * @since 8
 * @version 1.0
 */
void OH_Drawing_PathReset(OH_Drawing_Path* path);

/**
 * @brief Determines whether the path current contour is closed.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param path Indicates the pointer to an <b>OH_Drawing_Path</b> object.
 * @param forceClosed Whether to close the Path.
 * @return Returns <b>true</b> if the path current contour is closed; returns <b>false</b> otherwise.
 * @since 12
 * @version 1.0
 */
bool OH_Drawing_PathIsClosed(OH_Drawing_Path* path, bool forceClosed);

/**
 * @brief Gets the position and tangent of the distance from the starting position of the Path.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param path Indicates the pointer to an <b>OH_Drawing_Path</b> object.
 * @param forceClosed Whether to close the Path.
 * @param distance The distance from the start of the Path.
 * @param position Sets to the position of distance from the starting position of the Path.
 * @param tangent Sets to the tangent of distance from the starting position of the Path.
 * @return Returns <b>true</b> if succeeded; returns <b>false</b> otherwise.
 * @since 12
 * @version 1.0
 */
bool OH_Drawing_PathGetPositionTangent(OH_Drawing_Path* path, bool forceClosed,
    float distance, OH_Drawing_Point2D* position, OH_Drawing_Point2D* tangent);

/**
 * @brief Gets the path between the start and end points.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param path Indicates the pointer to an <b>OH_Drawing_Path</b> object.
 * @param forceClosed Whether to close the path.
 * @param start The distance from the starting point of the segment to the starting point of the path.
 * @param stop The distance from the end point of the segment to the starting point of the path.
 * @param startWithMoveTo Whether the path obtained moveTo to the starting segment.
 * @param dst The path obtained.
 * @param result Indicates the result of getting the path segment.
 *               The value is false if the segment is zero-length or start >= stop, and true otherwise.
 * @return Returns the error code.
 *         Returns {@link OH_DRAWING_SUCCESS} if the operation is successful.
 *         Returns {@link OH_DRAWING_ERROR_INVALID_PARAMETER} if any of path, dst and result is nullptr.
 * @since 18
 * @version 1.0
 */
OH_Drawing_ErrorCode OH_Drawing_PathGetSegment(OH_Drawing_Path* path, bool forceClosed,
    float start, float stop, bool startWithMoveTo, OH_Drawing_Path* dst, bool* result);

/**
 * @brief Combines two paths.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param path Indicates the pointer to an <b>OH_Drawing_Path</b> object.
 * @param other Indicates the pointer to an <b>OH_Drawing_Path</b> object.
 * @param op Indicates the operation to apply to combine.
 * @return Returns <b>true</b> if constructed path is not empty; returns <b>false</b> otherwise.
 * @since 12
 * @version 1.0
 */
bool OH_Drawing_PathOp(OH_Drawing_Path* path, const OH_Drawing_Path* other, OH_Drawing_PathOpMode op);

/**
 * @brief Computes the corresponding matrix at the specified distance.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param path Indicates the pointer to an <b>OH_Drawing_Path</b> object.
 * @param forceClosed Whether to close the Path.
 * @param distance The distance from the start of the Path.
 * @param matrix Indicates the pointer to an <b>OH_Drawing_Matrix</b> object.
 * @param flag Indicates what should be returned in the matrix.
 * @return Returns <b>false</b> if path is nullptr or zero-length;
           returns <b>true</b> if path is not nullptr and not zero-length.
 * @since 12
 * @version 1.0
 */
bool OH_Drawing_PathGetMatrix(OH_Drawing_Path* path, bool forceClosed,
    float distance, OH_Drawing_Matrix* matrix, OH_Drawing_PathMeasureMatrixFlags flag);

/**
 * @brief Approximates the path with a series of line segments.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param path Indicates the pointer to an <b>OH_Drawing_Path</b> object.
 * @param acceptableError Indicates the acceptable error for a line on the path. Should be no less than 0.
 * @param vals Indicates the storage for the computed array containing point components.
 * There are three components for each point:
 * 1.Fraction along the length of the path that the point resides.
 * 2.The x coordinate of the point.
 * 3.The y coordinate of the point.
 * @param count Returns with the size of array.
 * @return Returns the error code.
 *         Returns {@link OH_DRAWING_SUCCESS} if the operation is successful.
 *         Returns {@link OH_DRAWING_ERROR_INVALID_PARAMETER} if path or count is nullptr.
 *         Returns {@link OH_DRAWING_ERROR_PARAMETER_OUT_OF_RANGE} if acceptableError is less than 0.
 * @since 20
 * @version 1.0
 */
OH_Drawing_ErrorCode OH_Drawing_PathApproximate(OH_Drawing_Path* path, float acceptableError, float* vals,
    uint32_t* count);

/**
 * @brief Performs interpolation between the current path and another path based on a given weight, and stores the
 * result in the target path object.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param path Indicates the pointer to an <b>OH_Drawing_Path</b> object.
 * @param other Indicates the pointer to an <b>OH_Drawing_Path</b> object to be interpolated with path.
 * @param weight Indicates the interpolation weight, which must be in the range [0, 1].
 * @param success Indicates the interpolation is success or not.
 * @param interpolatedPath Indicates the pointer to an <b>OH_Drawing_Path</b> object to store the result.
 * @return Returns the error code.
 *         Returns {@link OH_DRAWING_SUCCESS} if the operation is successful.
 *         Returns {@link OH_DRAWING_ERROR_INVALID_PARAMETER} if either path, other, success or interpolatedPath is
 *                 nullptr.
 *         Returns {@link OH_DRAWING_ERROR_PARAMETER_OUT_OF_RANGE} if weight is outside the range [0, 1].
 * @since 20
 * @version 1.0
 */
OH_Drawing_ErrorCode OH_Drawing_PathInterpolate(OH_Drawing_Path* path, OH_Drawing_Path* other,
    float weight, bool* success, OH_Drawing_Path* interpolatedPath);

/**
 * @brief Checks whether the current path is compatible with another path (other) for interpolation, which means
 * they have exactly the same structure, both paths must have the same operations, in the same order.
 * If any of the operations are of type CONIC, then the weights of those conics must also match.
 *
 * @syscap SystemCapability.Graphic.Graphic2D.NativeDrawing
 * @param path Indicates the pointer to an <b>OH_Drawing_Path</b> object.
 * @param other Indicates the pointer to an <b>OH_Drawing_Path</b> object to be interpolated with path.
 * @param result Indicates whether the current path and the other path are compatible for interpolation.
 *               The value is true if the paths are compatible, and false otherwise.
 * @return Returns the error code.
 *         Returns {@link OH_DRAWING_SUCCESS} if the operation is successful.
 *         Returns {@link OH_DRAWING_ERROR_INVALID_PARAMETER} if either path, other or result is nullptr.
 * @since 20
 * @version 1.0
 */
OH_Drawing_ErrorCode OH_Drawing_PathIsInterpolate(OH_Drawing_Path* path, OH_Drawing_Path* other, bool* result);

#ifdef __cplusplus
}
#endif
/** @} */
#endif
