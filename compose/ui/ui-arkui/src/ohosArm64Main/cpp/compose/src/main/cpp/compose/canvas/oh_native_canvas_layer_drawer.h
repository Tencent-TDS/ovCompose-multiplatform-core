#ifndef OH_NATIVE_CANVAS_LAYER_DRAWER_H
#define OH_NATIVE_CANVAS_LAYER_DRAWER_H

#include <arkui/native_render.h>
#include <native_drawing/drawing_shader_effect.h>
#include "../picture_recorder/oh_native_render_node_save_state.h"
#include "../render_node/oh_base_render_node.h"
#include "../shader/oh_native_basic_shader.h"
#include "oh_compose_native_paint.h"

namespace OH {
void OHRenderNodeDrawRect(float left, float top, float right, float bottom,
                          NativeBasicShader *shader,
                          const RenderNodeSaveState *saveState,
                          BaseRenderNode *renderNodeForDrawing,
                          androidx::compose::ui::arkui::utils::OHComposeNativePaint *paint);

void OHRenderNodeDrawRoundRect(float left, float top, float right, float bottom,
                               float roundX, float roundY,
                                NativeBasicShader* shader,
                                const RenderNodeSaveState *saveState,
                                BaseRenderNode* renderNodeForDrawing,
                                androidx::compose::ui::arkui::utils::OHComposeNativePaint* paint);

void OHRenderNodeDrawLine(float x1, float y1, float x2, float y2,
                          NativeBasicShader *shader,
                          const RenderNodeSaveState *saveState,
                          BaseRenderNode *renderNodeForDrawing,
                          androidx::compose::ui::arkui::utils::OHComposeNativePaint* paint);

void OHRenderNodeDrawClipRect(float left, float top, float right, float bottom,
                              const RenderNodeSaveState *saveState,
                              BaseRenderNode *renderNodeForDrawing);

void OHRenderNodeDrawThrow(int32_t status);

OH_Drawing_ShaderEffect *CreateShaderEffect(NativeBasicShader *shader);
} // namespace OH
#endif