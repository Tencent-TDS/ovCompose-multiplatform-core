#ifndef ANDROIDX_COMPOSE_UI_ARKUI_UTILS_OHNATIVEENUM_H
#define ANDROIDX_COMPOSE_UI_ARKUI_UTILS_OHNATIVEENUM_H

#ifdef __cplusplus
extern "C" {
#endif
typedef enum { Fill,
               Stroke } OH_Native_Draw_PaintingStyle;

typedef enum { Empty,
               EvenOdd } OH_Native_Draw_PathFillType;

typedef enum { StrokeCapButt,
               StrokeCapRound,
               StrokeCapSquare } OH_Native_Draw_StrokeCap;

typedef enum { StrokeJoinMitter,
               StrokeJoinRound,
               StrokeJoinBevel } OH_Native_Draw_StrokeJoin;

typedef enum { None,
               Low,
               Medium,
               High } OH_Native_Draw_FilterQuality;

typedef enum { Difference,
               Intersect } OH_Native_Draw_ClipOp;

typedef enum {
    DrawingTypeNone,
    DrawingTypeRect,
    DrawingTypeShaderRect,
    DrawingTypeShaderRoundRect,
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
    DrawingTypeDrawTextLayer,
    DrawingTypeDrawLayer,
    DrawingTypeSave,
    DrawingTypeRestore,
    DrawingTypeClip,
    DrawingTypeClipPath,
    DrawingTypePop,
    DrawingTypeSaveLayer
} OH_Native_Drawing_Type;

typedef enum {
    BaseNode,
    LineNode,
    ParagraphNode,
    LineGradientNode,
    RectGradientNode,
    RoundRectGradientNode,
    CircleGradientNode, // 圆形渐变节点（带shader的圆形绘制）
    OvalNode,
    ArcNode,
    PathNode,
    ImageDisplayNode, // 图片显示节点（使用ContentModifier直接绘制，支持裁剪和缩放）
    PointsNode,       // 点集绘制节点
    ClipNode,         // 执行裁剪操作节点
    AsyncTaskNode     // 异步任务节点（用于异步文本绘制）
    // TODO: 每实现一个BaseRenderNode的派生类，就需要在此处添加对应的枚举值
} OH_DrawingNode_Type;

// Enum to specify the type of RenderNode save state
typedef enum {
    SafeGuard, // Safe guard
    Save,      // Pure save operation
    Clip,      // Clip operation
    SaveLayer  // SaveLayer operation
} OH_RenderNode_SaveState_MakeType;

typedef enum {
    NoneTypeShader,
    LinearGradientShader,
    RadialGradientShader,
    SweepGradientShader,
    ImageShader
    // TODO: 每实现一个BasicShader的派生类，就需要在此处添加对应的枚举值
} OH_Native_Shader_Type;

typedef enum {
    PathOpDifference,
    PathOpIntersect,
    PathOpUnion,
    PathOpXor,
    PathOpReverseDifference
} OH_Native_Draw_Path_Operation;

typedef enum { Points,
               Lines,
               Polygon } OH_Native_Draw_PointMode;

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