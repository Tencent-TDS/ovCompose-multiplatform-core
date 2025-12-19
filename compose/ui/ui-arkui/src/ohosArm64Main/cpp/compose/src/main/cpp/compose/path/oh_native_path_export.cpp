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

#include "oh_native_path_export.h"
#include "../xcomponent_log.h"
#include "native_drawing/drawing_path.h"
#include "native_drawing/drawing_rect.h"
#include "native_drawing/drawing_error_code.h"

EXTERN_C_START

// OH_Drawing_Path creation and building functions
// Simplified function names for better readability

OH_Drawing_Path_Handle OHPath_create(void) {
    LOGI("OHPath_create: start");
    return OH_Drawing_PathCreate();
}

void OHPath_moveTo(OH_Drawing_Path_Handle path, float x, float y) {
    if (path == nullptr) {
        LOGE("OHPath_moveTo: path is nullptr");
        return;
    }
    OH_Drawing_PathMoveTo(path, x, y);
}

void OHPath_lineTo(OH_Drawing_Path_Handle path, float x, float y) {
    if (path == nullptr) {
        LOGE("OHPath_lineTo: path is nullptr");
        return;
    }
    OH_Drawing_PathLineTo(path, x, y);
}

void OHPath_quadTo(OH_Drawing_Path_Handle path, float ctrlX, float ctrlY, float endX, float endY) {
    if (path == nullptr) {
        LOGE("OHPath_quadTo: path is nullptr");
        return;
    }
    OH_Drawing_PathQuadTo(path, ctrlX, ctrlY, endX, endY);
}

void OHPath_cubicTo(OH_Drawing_Path_Handle path, float ctrlX1, float ctrlY1, float ctrlX2, float ctrlY2, float endX, float endY) {
    if (path == nullptr) {
        LOGE("OHPath_cubicTo: path is nullptr");
        return;
    }
    OH_Drawing_PathCubicTo(path, ctrlX1, ctrlY1, ctrlX2, ctrlY2, endX, endY);
}

void OHPath_close(OH_Drawing_Path_Handle path) {
    if (path == nullptr) {
        LOGE("OHPath_close: path is nullptr");
        return;
    }
    OH_Drawing_PathClose(path);
}

void OHPath_destroy(OH_Drawing_Path_Handle path) {
    if (path == nullptr) {
        LOGE("OHPath_destroy: path is nullptr");
        return;
    }
    OH_Drawing_PathDestroy(path);
}

void OHPath_reset(OH_Drawing_Path_Handle path) {
    if (path == nullptr) {
        LOGE("OHPath_reset: path is nullptr");
        return;
    }
    OH_Drawing_PathReset(path);
}

void OHPath_rMoveTo(OH_Drawing_Path_Handle path, float dx, float dy) {
    if (path == nullptr) {
        LOGE("OHPath_rMoveTo: path is nullptr");
        return;
    }
    OH_Drawing_PathRMoveTo(path, dx, dy);
}

void OHPath_rLineTo(OH_Drawing_Path_Handle path, float dx, float dy) {
    if (path == nullptr) {
        LOGE("OHPath_rLineTo: path is nullptr");
        return;
    }
    OH_Drawing_PathRLineTo(path, dx, dy);
}

void OHPath_rQuadTo(OH_Drawing_Path_Handle path, float dx1, float dy1, float dx2, float dy2) {
    if (path == nullptr) {
        LOGE("OHPath_rQuadTo: path is nullptr");
        return;
    }
    OH_Drawing_PathRQuadTo(path, dx1, dy1, dx2, dy2);
}

void OHPath_rCubicTo(OH_Drawing_Path_Handle path, float dx1, float dy1, float dx2, float dy2, float dx3, float dy3) {
    if (path == nullptr) {
        LOGE("OHPath_rCubicTo: path is nullptr");
        return;
    }
    OH_Drawing_PathRCubicTo(path, dx1, dy1, dx2, dy2, dx3, dy3);
}

void OHPath_arcTo(OH_Drawing_Path_Handle path, float left, float top, float right, float bottom, float startAngleDegrees, float sweepAngleDegrees) {
    if (path == nullptr) {
        LOGE("OHPath_arcTo: path is nullptr");
        return;
    }
    OH_Drawing_Rect *rect = OH_Drawing_RectCreate(left, top, right, bottom);
    if (rect != nullptr) {
        OH_Drawing_PathAddArc(path, rect, startAngleDegrees, sweepAngleDegrees);
        OH_Drawing_RectDestroy(rect);
    }
}

void OHPath_addRect(OH_Drawing_Path_Handle path, float left, float top, float right, float bottom) {
    if (path == nullptr) {
        LOGE("OHPath_addRect: path is nullptr");
        return;
    }
    // Use COUNTER_CLOCKWISE direction (similar to iOS)
    OH_Drawing_PathAddRect(path, left, top, right, bottom, PATH_DIRECTION_CCW);
}

void OHPath_addOval(OH_Drawing_Path_Handle path, float left, float top, float right, float bottom) {
    if (path == nullptr) {
        LOGE("OHPath_addOval: path is nullptr");
        return;
    }
    OH_Drawing_Rect *rect = OH_Drawing_RectCreate(left, top, right, bottom);
    if (rect != nullptr) {
        // Use COUNTER_CLOCKWISE direction (similar to iOS)
        OH_Drawing_PathAddOval(path, rect, PATH_DIRECTION_CCW);
        OH_Drawing_RectDestroy(rect);
    }
}

void OHPath_addArc(OH_Drawing_Path_Handle path, float left, float top, float right, float bottom, float startAngleDegrees, float sweepAngleDegrees) {
    if (path == nullptr) {
        LOGE("OHPath_addArc: path is nullptr");
        return;
    }
    OH_Drawing_Rect *rect = OH_Drawing_RectCreate(left, top, right, bottom);
    if (rect != nullptr) {
        OH_Drawing_PathAddArc(path, rect, startAngleDegrees, sweepAngleDegrees);
        OH_Drawing_RectDestroy(rect);
    }
}

void OHPath_addPath(OH_Drawing_Path_Handle path, OH_Drawing_Path_Handle src, float offsetX, float offsetY) {
    if (path == nullptr || src == nullptr) {
        LOGE("OHPath_addPath: path or src is nullptr");
        return;
    }
    // Use PATH_ADD_MODE_APPEND mode and create identity matrix for offset
    // Note: OH_Drawing_PathAddPathWithOffsetAndMode requires offset, but we need to create a matrix
    // For now, use the offset version
    OH_Drawing_PathAddPathWithOffsetAndMode(path, src, offsetX, offsetY, PATH_ADD_MODE_APPEND);
}

void OHPath_translate(OH_Drawing_Path_Handle path, float dx, float dy) {
    if (path == nullptr) {
        LOGE("OHPath_translate: path is nullptr");
        return;
    }
    // OH_Drawing_PathOffset requires dst, so we create a temporary path
    OH_Drawing_Path_Handle dst = OH_Drawing_PathCreate();
    if (dst != nullptr) {
        OH_Drawing_PathOffset(path, dst, dx, dy);
        OH_Drawing_ErrorCode result = OH_Drawing_PathSetPath(path, dst);
        if (result != OH_DRAWING_SUCCESS) {
            LOGE("OHPath_translate: OH_Drawing_PathSetPath failed");
        }
        OH_Drawing_PathDestroy(dst);
    }
}

void OHPath_getBounds(OH_Drawing_Path_Handle path, float *left, float *top, float *right, float *bottom) {
    if (path == nullptr || left == nullptr || top == nullptr || right == nullptr || bottom == nullptr) {
        LOGE("OHPath_getBounds: path or output parameters is nullptr");
        return;
    }
    OH_Drawing_Rect *rect = OH_Drawing_RectCreate(0.0f, 0.0f, 0.0f, 0.0f);
    if (rect != nullptr) {
        OH_Drawing_PathGetBounds(path, rect);
        *left = OH_Drawing_RectGetLeft(rect);
        *top = OH_Drawing_RectGetTop(rect);
        *right = OH_Drawing_RectGetRight(rect);
        *bottom = OH_Drawing_RectGetBottom(rect);
        OH_Drawing_RectDestroy(rect);
    }
}

void OHPath_setFillType(OH_Drawing_Path_Handle path, uint32_t fillType) {
    if (path == nullptr) {
        LOGE("OHPath_setFillType: path is nullptr");
        return;
    }
    OH_Drawing_PathSetFillType(path, static_cast<OH_Drawing_PathFillType>(fillType));
}

uint32_t OHPath_getFillType(OH_Drawing_Path_Handle path) {
    if (path == nullptr) {
        LOGE("OHPath_getFillType: path is nullptr");
        return 0;
    }
    OH_Drawing_PathFillType fillType;
    OH_Drawing_ErrorCode result = OH_Drawing_PathGetFillType(path, &fillType);
    if (result == OH_DRAWING_SUCCESS) {
        return static_cast<uint32_t>(fillType);
    }
    return 0;
}

bool OHPath_isEmpty(OH_Drawing_Path_Handle path) {
    if (path == nullptr) {
        LOGE("OHPath_isEmpty: path is nullptr");
        return true;
    }
    bool isEmpty = false;
    OH_Drawing_ErrorCode result = OH_Drawing_PathIsEmpty(path, &isEmpty);
    if (result == OH_DRAWING_SUCCESS) {
        return isEmpty;
    }
    return true;
}

bool OHPath_isConvex(OH_Drawing_Path_Handle path) {
    if (path == nullptr) {
        LOGE("OHPath_isConvex: path is nullptr");
        return false;
    }
    // Note: OHOS doesn't have a direct isConvex API
    // For now, return false as a placeholder
    // This can be implemented later if needed
    return false;
}

EXTERN_C_END

