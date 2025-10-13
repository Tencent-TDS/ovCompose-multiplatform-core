#ifndef ANDROIDX_COMPOSE_UI_ARKUI_UTILS_OHNATIVEENUM_H
#define ANDROIDX_COMPOSE_UI_ARKUI_UTILS_OHNATIVEENUM_H

#include "stdbool.h"
#include "stddef.h"
#include "stdint.h"

#ifdef __cplusplus
extern "C" {
#endif
typedef enum {
    OH_NATIVE_PAINTING_STYLE_FILL,
    OH_NATIVE_PAINTING_STYLE_STROKE
} OH_Native_Draw_PaintingStyle;

typedef enum {
    OH_NATIVE_FILL_TYPE_NONE_ZERO,
    OH_NATIVE_FILL_TYPE_EVEN_ODD
} OH_Native_Draw_PathFillType;

typedef enum  {
    OH_NATIVE_STROKE_CAP_BUTT,
    OH_NATIVE_STROKE_CAP_ROUND,
    OH_NATIVE_STROKE_CAP_SQUARE
} OH_Native_Draw_StrokeCap;

typedef enum {
    OH_NATIVE_STROKE_JOIN_MITER,
    OH_NATIVE_STROKE_JOIN_ROUND,
    OH_NATIVE_STROKE_JOIN_BEVEL
} OH_Native_Draw_StrokeJoin ;

typedef enum {
    OH_NATIVE_FILTER_QUALITY_NONE,
    OH_NATIVE_FILTER_QUALITY_LOW,
    OH_NATIVE_FILTER_QUALITY_MEDIUM,
    OH_NATIVE_FILTER_QUALITY_HIGH
} OH_Native_Draw_FilterQuality;

typedef enum {
    OH_NATIVE_CLIPOP_DIFFERENCE,
    OH_NATIVE_CLIPOP_INTERSECT
} OH_Native_Draw_ClipOp;
#ifdef __cplusplus
}
#endif
/** @} */
#endif