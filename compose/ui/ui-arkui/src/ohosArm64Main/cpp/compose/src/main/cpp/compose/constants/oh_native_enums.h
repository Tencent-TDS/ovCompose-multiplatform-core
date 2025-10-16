#ifndef ANDROIDX_COMPOSE_UI_ARKUI_UTILS_OHNATIVEENUM_H
#define ANDROIDX_COMPOSE_UI_ARKUI_UTILS_OHNATIVEENUM_H

#include "stdbool.h"
#include "stddef.h"
#include "stdint.h"

#ifdef __cplusplus
extern "C" {
#endif
typedef enum {
    Fill,
    Stroke
} OH_Native_Draw_PaintingStyle;

typedef enum {
    Empty,
    EvenOdd
} OH_Native_Draw_PathFillType;

typedef enum {
    StrokeCapButt,
    StrokeCapRound,
    StrokeCapSquare
} OH_Native_Draw_StrokeCap;

typedef enum {
    StrokeJoinMitter,
    StrokeJoinRound,
    StrokeJoinBevel
} OH_Native_Draw_StrokeJoin;

typedef enum {
    None,
    Low,
    Medium,
    High
} OH_Native_Draw_FilterQuality;

typedef enum {
    Difference,
    Intersect
} OH_Native_Draw_ClipOp;

typedef enum {
    DrawingTypeNone,
    DrawingTypeRect,
    DrawingTypeShaderRect,
    DrawingTypeLine,
    DrawingTypeShaderLine,
    DrawingTypeOval,
    DrawingTypeShaderOval,
    DrawingTypeCircle,
    DrawingTypeShaderCircle,
    DrawingTypeArc,
    DrawingTypeShaderArc,
    DrawingTypePath,
    DrawingTypeShaderPath,
    DrawingTypeImage,
    DrawingTypeShaderImage,
    DrawingTypeImageRect,
    DrawingTypeImageData,
    DrawingTypePoints,
    DrawingTypeShaderPoints,
    DrawingTypeRowPoints,
    DrawingTypeShaderRowPoints,
    DrawingTypeRowVertices,
    DrawingTypeShaderRowVertices,
    DrawingTypeDrawLayer,
    DrawingTypeSave,
    DrawingTypeRestore,
    DrawingTypeClip,
    DrawingTypePop
} OH_Native_Drawing_Type;

// Enum to specify the type of RenderNode save state
typedef enum {
    SafeGuard, // Safe guard
    Save,      // Pure save operation
    Clip       // Clip operation
} OH_RenderNode_SaveState_MakeType;

typedef enum {
    PathOpDifference,
    PathOpIntersect,
    PathOpUnion,
    PathOpXor,
    PathOpReverseDifference
} OH_Native_Draw_Path_Operation;

typedef enum {
    Points,
    Lines,
    Polygon
} OH_Native_Draw_PointMode;

typedef enum {
    BlendModeClear,
    BlendModeSrc,
    BlendModeDst,
    BlendModeSrcOver,
    BlendModeDstOver,
    BlendModeSrcIn,
    BlendModeDstIn,
    BlendModeSrcOut,
    BlendModeDstOut,
    BlendModeDstAtop,
    BlendModeSrcAtop,
    BlendModeXor,
    BlendModePlus,
    BlendModeModulate,
    BlendModeScreen,
    BlendModeOverlay,
    BlendModeDarken,
    BlendModeLighten,
    BlendModeColorDodge,
    BlendModeColorBurn,
    BlendModeHardlight,
    BlendModeSoftlight,
    BlendModeDifference,
    BlendModeExclusion,
    BlendModeMultiply,
    BlendModeHue,
    BlendModeSaturation,
    BlendModeColor,
    BlendModeLuminosity,
    BlendModeUnknown
} OH_Native_Draw_BlendMode;

#ifdef __cplusplus
}
#endif
/** @} */
#endif