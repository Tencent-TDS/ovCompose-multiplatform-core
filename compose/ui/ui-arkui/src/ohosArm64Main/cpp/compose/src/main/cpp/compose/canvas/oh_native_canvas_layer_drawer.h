#ifndef OH_NATIVE_CANVAS_LAYER_DRAWER_H
#define OH_NATIVE_CANVAS_LAYER_DRAWER_H

#include <native_drawing/drawing_canvas.h>
#include <native_drawing/drawing_path.h>
#include "../paragraph/oh_native_paragraph.h"
#include "../picture_recorder/oh_native_render_node_save_state.h"
#include "../render_node/oh_base_render_node.h"
#include "../shader/oh_native_basic_shader.h"
#include "../paint/oh_compose_native_paint.h"

namespace OH {
void OHRenderNodeDrawRect(float left, float top, float right, float bottom, NativeBasicShader *shader,
                          const RenderNodeSaveState *saveState, BaseRenderNode *renderNodeForDrawing,
                          const OH::OHComposeNativePaint *paint);

void OHRenderNodeDrawRoundRect(float left, float top, float right, float bottom, float radiusX, float radiusY,
                               const NativeBasicShader *shader, const RenderNodeSaveState *saveState,
                               BaseRenderNode *renderNodeForDrawing,
                               const OH::OHComposeNativePaint *paint);

void OHRenderNodeDrawLine(float x1, float y1, float x2, float y2, NativeBasicShader *shader,
                          const RenderNodeSaveState *saveState, BaseRenderNode *renderNodeForDrawing,
                          const OH::OHComposeNativePaint *paint);

void OHRenderNodeDrawCircle(float centerX, float centerY, float radius, const NativeBasicShader *shader,
                            const RenderNodeSaveState *saveState, BaseRenderNode *renderNodeForDrawing,
                            const OH::OHComposeNativePaint *paint);

void OHRenderNodeDrawOval(float left, float top, float right, float bottom, const NativeBasicShader *shader,
                          const RenderNodeSaveState *saveState, BaseRenderNode *renderNodeForDrawing,
                          const OH::OHComposeNativePaint *paint);

void OHRenderNodeDrawArc(float left, float top, float right, float bottom, float startAngle, float sweepAngle,
                         bool useCenter, const NativeBasicShader *shader, const RenderNodeSaveState *saveState,
                         BaseRenderNode *renderNodeForDrawing,
                         const OH::OHComposeNativePaint *paint);

void OHRenderNodeDrawPath(OH_Drawing_Path_Handle path, const NativeBasicShader *shader, const RenderNodeSaveState *saveState,
                          BaseRenderNode *renderNodeForDrawing,
                          const OH::OHComposeNativePaint *paint);

void OHRenderNodeDrawImageRect(OH_PixelmapNative *pixelMap, int32_t srcX, int32_t srcY, int32_t srcWidth,
                               int32_t srcHeight, int32_t dstX, int32_t dstY, int32_t dstWidth, int32_t dstHeight,
                               const RenderNodeSaveState *saveState, BaseRenderNode *renderNodeForDrawing,
                               const OH::OHComposeNativePaint *paint);

void OHRenderNodeDrawPoints(OH_Drawing_PointMode pointMode, const float *points, size_t pointCount,
                            const RenderNodeSaveState *saveState, BaseRenderNode *renderNodeForDrawing,
                            OH::OHComposeNativePaint *paint);

void OHRenderNodeDrawClipRect(float left, float top, float right, float bottom, const RenderNodeSaveState *saveState,
                              BaseRenderNode *renderNodeForDrawing, OH_Native_Draw_ClipOp clipOp);

void OHRenderNodeDrawClipPath(OH_Drawing_Path_Handle path, OH_Native_Draw_ClipOp clipOp,
                              const RenderNodeSaveState *saveState, BaseRenderNode *renderNodeForDrawing);

void OHRenderNodeDrawClipRoundRect(float left, float top, float right, float bottom, float radiusX, float radiusY,
                                   const RenderNodeSaveState *saveState, BaseRenderNode *renderNodeForDrawing);

void OHRenderNodeDrawText(const RenderNodeSaveState *saveState, Paragraph *paragraphNode);

void OHRenderNodeDrawTextPixelMap(OH_PixelmapNative *pixelMap, int32_t cacheKey, int32_t width, int32_t height,
                                  const RenderNodeSaveState *saveState, BaseRenderNode *renderNodeForDrawing);

void OHRenderNodeDrawTextPixelMapWithPtr(OH_PixelmapNative *pixelMap, int32_t width, int32_t height,
                                         const RenderNodeSaveState *saveState, BaseRenderNode *renderNodeForDrawing);

OH_PixelmapNative *OHNativeComposeHasTextImageCache(int32_t cacheKey);

OH_PixelmapNative *OHNativeComposePixelMapFromImageBitmap(OH_PixelmapNative *pixelMapNative, int32_t cacheKey);

void OHRenderNodeDrawThrow(int32_t status);

OH_Drawing_ShaderEffect *CreateShaderEffect(NativeBasicShader *shader);
} // namespace OH
#endif