#ifndef OH_NATIVE_CANVAS_LAYER_DRAWER_H
#define OH_NATIVE_CANVAS_LAYER_DRAWER_H

#include <native_drawing/drawing_path.h>
#include <native_drawing/drawing_shader_effect.h>
#include "../paragraph/oh_native_paragraph.h"
#include "../picture_recorder/oh_native_render_node_save_state.h"
#include "../render_node/oh_base_render_node.h"
#include "../shader/oh_native_basic_shader.h"
#include "oh_compose_native_paint.h"

namespace OH {
void OHRenderNodeDrawRect(float left, float top, float right, float bottom, NativeBasicShader *shader,
                          const RenderNodeSaveState *saveState, BaseRenderNode *renderNodeForDrawing,
                          const androidx::compose::ui::arkui::utils::OHComposeNativePaint *paint);

void OHRenderNodeDrawRoundRect(float left, float top, float right, float bottom, float radiusX, float radiusY,
                               const NativeBasicShader *shader, const RenderNodeSaveState *saveState,
                               BaseRenderNode *renderNodeForDrawing,
                               const androidx::compose::ui::arkui::utils::OHComposeNativePaint *paint);

void OHRenderNodeDrawLine(float x1, float y1, float x2, float y2, NativeBasicShader *shader,
                          const RenderNodeSaveState *saveState, BaseRenderNode *renderNodeForDrawing,
                          const androidx::compose::ui::arkui::utils::OHComposeNativePaint *paint);

void OHRenderNodeDrawCircle(float centerX, float centerY, float radius, const NativeBasicShader *shader,
                            const RenderNodeSaveState *saveState, BaseRenderNode *renderNodeForDrawing,
                            const androidx::compose::ui::arkui::utils::OHComposeNativePaint *paint);

void OHRenderNodeDrawOval(float left, float top, float right, float bottom, const NativeBasicShader *shader,
                          const RenderNodeSaveState *saveState, BaseRenderNode *renderNodeForDrawing,
                          const androidx::compose::ui::arkui::utils::OHComposeNativePaint *paint);

void OHRenderNodeDrawArc(float left, float top, float right, float bottom, float startAngle, float sweepAngle,
                         bool useCenter, const NativeBasicShader *shader, const RenderNodeSaveState *saveState,
                         BaseRenderNode *renderNodeForDrawing,
                         const androidx::compose::ui::arkui::utils::OHComposeNativePaint *paint);

void OHRenderNodeDrawPath(OH_Drawing_Path *path, const NativeBasicShader *shader, const RenderNodeSaveState *saveState,
                          BaseRenderNode *renderNodeForDrawing,
                          const androidx::compose::ui::arkui::utils::OHComposeNativePaint *paint);

void OHRenderNodeDrawImageRect(OH_PixelmapNative *pixelMap, int32_t srcX, int32_t srcY, int32_t srcWidth,
                               int32_t srcHeight, int32_t dstX, int32_t dstY, int32_t dstWidth, int32_t dstHeight,
                               const RenderNodeSaveState *saveState, BaseRenderNode *renderNodeForDrawing,
                               const androidx::compose::ui::arkui::utils::OHComposeNativePaint *paint);

void OHRenderNodeDrawClipRect(float left, float top, float right, float bottom, const RenderNodeSaveState *saveState,
                              BaseRenderNode *renderNodeForDrawing);

void OHRenderNodeDrawText(const RenderNodeSaveState *saveState, Paragraph *paragraphNode);

void OHRenderNodeDrawThrow(int32_t status);

OH_Drawing_ShaderEffect *CreateShaderEffect(NativeBasicShader *shader);
} // namespace OH
#endif