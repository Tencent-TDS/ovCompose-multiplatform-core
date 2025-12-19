/*
 * Tencent is pleased to support the open source community by making ovCompose
 * available. Copyright (C) 2025 Tencent. All rights reserved.
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

#ifndef ANDROIDX_COMPOSE_UI_ARKUI_UTILS_OHNATIVEPATH_EXPORT_H
#define ANDROIDX_COMPOSE_UI_ARKUI_UTILS_OHNATIVEPATH_EXPORT_H

#include <native_drawing/drawing_path.h>
#include <native_drawing/drawing_types.h>
#include <stdbool.h>
#include <stdint.h>

#include "../xcomponent_common.h"

EXTERN_C_START

// OH_Drawing_Path creation and building functions
// Simplified function names for better readability

// Forward declaration
typedef struct OH_Drawing_Path OH_Drawing_Path;
typedef OH_Drawing_Path *OH_Drawing_Path_Handle;

/**
 * Creates a new OH_Drawing_Path instance.
 * @return A handle to the created path, or nullptr on failure.
 */
OH_Drawing_Path_Handle OHPath_create(void);

/**
 * Moves the current point to the specified coordinates.
 * @param path The path handle
 * @param x The x coordinate
 * @param y The y coordinate
 */
void OHPath_moveTo(OH_Drawing_Path_Handle path, float x, float y);

/**
 * Draws a line from the current point to the specified coordinates.
 * @param path The path handle
 * @param x The x coordinate
 * @param y The y coordinate
 */
void OHPath_lineTo(OH_Drawing_Path_Handle path, float x, float y);

/**
 * Adds a quadratic Bezier curve to the path.
 * @param path The path handle
 * @param ctrlX Control point x coordinate
 * @param ctrlY Control point y coordinate
 * @param endX End point x coordinate
 * @param endY End point y coordinate
 */
void OHPath_quadTo(OH_Drawing_Path_Handle path, float ctrlX, float ctrlY, float endX, float endY);

/**
 * Adds a cubic Bezier curve to the path.
 * @param path The path handle
 * @param ctrlX1 First control point x coordinate
 * @param ctrlY1 First control point y coordinate
 * @param ctrlX2 Second control point x coordinate
 * @param ctrlY2 Second control point y coordinate
 * @param endX End point x coordinate
 * @param endY End point y coordinate
 */
void OHPath_cubicTo(OH_Drawing_Path_Handle path, float ctrlX1, float ctrlY1, float ctrlX2, float ctrlY2, float endX, float endY);

/**
 * Closes the current subpath.
 * @param path The path handle
 */
void OHPath_close(OH_Drawing_Path_Handle path);

/**
 * Destroys the path and frees its resources.
 * @param path The path handle to destroy
 */
void OHPath_destroy(OH_Drawing_Path_Handle path);

/**
 * Resets the path, clearing all subpaths.
 * @param path The path handle
 */
void OHPath_reset(OH_Drawing_Path_Handle path);

/**
 * Moves the current point relative to the current position.
 * @param path The path handle
 * @param dx Relative x offset
 * @param dy Relative y offset
 */
void OHPath_rMoveTo(OH_Drawing_Path_Handle path, float dx, float dy);

/**
 * Draws a line relative to the current point.
 * @param path The path handle
 * @param dx Relative x offset
 * @param dy Relative y offset
 */
void OHPath_rLineTo(OH_Drawing_Path_Handle path, float dx, float dy);

/**
 * Adds a quadratic Bezier curve relative to the current point.
 * @param path The path handle
 * @param dx1 Relative control point x offset
 * @param dy1 Relative control point y offset
 * @param dx2 Relative end point x offset
 * @param dy2 Relative end point y offset
 */
void OHPath_rQuadTo(OH_Drawing_Path_Handle path, float dx1, float dy1, float dx2, float dy2);

/**
 * Adds a cubic Bezier curve relative to the current point.
 * @param path The path handle
 * @param dx1 Relative first control point x offset
 * @param dy1 Relative first control point y offset
 * @param dx2 Relative second control point x offset
 * @param dy2 Relative second control point y offset
 * @param dx3 Relative end point x offset
 * @param dy3 Relative end point y offset
 */
void OHPath_rCubicTo(OH_Drawing_Path_Handle path, float dx1, float dy1, float dx2, float dy2, float dx3, float dy3);

/**
 * Adds an arc to the path.
 * @param path The path handle
 * @param left Left coordinate of the bounding rectangle
 * @param top Top coordinate of the bounding rectangle
 * @param right Right coordinate of the bounding rectangle
 * @param bottom Bottom coordinate of the bounding rectangle
 * @param startAngleDegrees Starting angle in degrees
 * @param sweepAngleDegrees Sweep angle in degrees
 */
void OHPath_arcTo(OH_Drawing_Path_Handle path, float left, float top, float right, float bottom, float startAngleDegrees, float sweepAngleDegrees);

/**
 * Adds a rectangle to the path.
 * @param path The path handle
 * @param left Left coordinate
 * @param top Top coordinate
 * @param right Right coordinate
 * @param bottom Bottom coordinate
 */
void OHPath_addRect(OH_Drawing_Path_Handle path, float left, float top, float right, float bottom);

/**
 * Adds an oval to the path.
 * @param path The path handle
 * @param left Left coordinate of the bounding rectangle
 * @param top Top coordinate of the bounding rectangle
 * @param right Right coordinate of the bounding rectangle
 * @param bottom Bottom coordinate of the bounding rectangle
 */
void OHPath_addOval(OH_Drawing_Path_Handle path, float left, float top, float right, float bottom);

/**
 * Adds an arc to the path.
 * @param path The path handle
 * @param left Left coordinate of the bounding rectangle
 * @param top Top coordinate of the bounding rectangle
 * @param right Right coordinate of the bounding rectangle
 * @param bottom Bottom coordinate of the bounding rectangle
 * @param startAngleDegrees Starting angle in degrees
 * @param sweepAngleDegrees Sweep angle in degrees
 */
void OHPath_addArc(OH_Drawing_Path_Handle path, float left, float top, float right, float bottom, float startAngleDegrees, float sweepAngleDegrees);

/**
 * Adds another path to this path with an offset.
 * @param path The destination path handle
 * @param src The source path handle
 * @param offsetX X offset to apply
 * @param offsetY Y offset to apply
 */
void OHPath_addPath(OH_Drawing_Path_Handle path, OH_Drawing_Path_Handle src, float offsetX, float offsetY);

/**
 * Translates all points in the path by the specified offset.
 * @param path The path handle
 * @param dx X translation offset
 * @param dy Y translation offset
 */
void OHPath_translate(OH_Drawing_Path_Handle path, float dx, float dy);

/**
 * Gets the bounding rectangle of the path.
 * @param path The path handle
 * @param left Output parameter for left coordinate
 * @param top Output parameter for top coordinate
 * @param right Output parameter for right coordinate
 * @param bottom Output parameter for bottom coordinate
 */
void OHPath_getBounds(OH_Drawing_Path_Handle path, float *left, float *top, float *right, float *bottom);

/**
 * Sets the fill type of the path.
 * @param path The path handle
 * @param fillType The fill type (0 = Winding, 1 = EvenOdd)
 */
void OHPath_setFillType(OH_Drawing_Path_Handle path, uint32_t fillType);

/**
 * Gets the fill type of the path.
 * @param path The path handle
 * @return The fill type (0 = Winding, 1 = EvenOdd)
 */
uint32_t OHPath_getFillType(OH_Drawing_Path_Handle path);

/**
 * Checks if the path is empty.
 * @param path The path handle
 * @return true if the path is empty, false otherwise
 */
bool OHPath_isEmpty(OH_Drawing_Path_Handle path);

/**
 * Checks if the path is convex.
 * @param path The path handle
 * @return true if the path is convex, false otherwise (or if not supported)
 */
bool OHPath_isConvex(OH_Drawing_Path_Handle path);

EXTERN_C_END

#endif // ANDROIDX_COMPOSE_UI_ARKUI_UTILS_OHNATIVEPATH_EXPORT_H

