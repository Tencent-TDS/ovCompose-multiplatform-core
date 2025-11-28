/*
 * Tencent is pleased to support the open source community by making ovCompose available.
 * Copyright (C) 2025 Tencent. All rights reserved.
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

#include "oh_circle_gradient_render_node.h"
#include <native_drawing/drawing_brush.h>
#include <native_drawing/drawing_canvas.h>
#include <native_drawing/drawing_pen.h>
#include <native_drawing/drawing_filter.h>
#include <cfloat>
#include "../shader/oh_native_shader_utils.h"
#include "../xcomponent_log.h"
#include "oh_render_node_color_filter_utils.h"

namespace OH {

CircleGradientRenderNode::~CircleGradientRenderNode() {
    if (invalidateCountProperty_) {
        OH_ArkUI_RenderNodeUtils_DisposeFloatProperty(invalidateCountProperty_);
    }
    if (modifier_) {
        OH_ArkUI_RenderNodeUtils_DisposeContentModifier(modifier_);
    }
}

CircleGradientRenderNode::CircleGradientRenderNode() {
    this->CircleGradientRenderNode::initModifier();
}

OH_DrawingNode_Type CircleGradientRenderNode::getType() {
    return OH_DrawingNode_Type::CircleGradientNode;
}

void CircleGradientRenderNode::drawCircle(float centerX, float centerY, float radius, float strokeWidth,
                                          NativeBasicShader *shader, OH_Native_Draw_PaintingStyle style,
                                          OHComposeNativeColorFilter *colorFilter) {
    // 更新成员变量
    centerX_ = centerX;
    centerY_ = centerY;
    radius_ = radius;
    strokeWidth_ = strokeWidth;
    shader_ = shader;
    paintingStyle_ = style;
    colorFilter_ = colorFilter;

    // 触发重绘
    invalidate();
}

void CircleGradientRenderNode::invalidate() {
    // 读取当前值
    float currentCount = 0.0f;
    OH_ArkUI_RenderNodeUtils_GetFloatPropertyValue(invalidateCountProperty_, &currentCount);

    // 加1，处理溢出（回绕到0）
    float newCount = (currentCount >= FLT_MAX - 1.0f) ? 0.0f : (currentCount + 1.0f);

    // 设置新值，触发 onDraw 回调
    OH_ArkUI_RenderNodeUtils_SetFloatPropertyValue(invalidateCountProperty_, newCount);
}

void CircleGradientRenderNode::initModifier() {
    if (!modifier_) {
        modifier_ = OH_ArkUI_RenderNodeUtils_CreateContentModifier();
        maybeThrow(OH_ArkUI_RenderNodeUtils_AttachContentModifier(nodeHandle_, modifier_));

        // 创建 invalidateCount PropertyHandle
        invalidateCountProperty_ = OH_ArkUI_RenderNodeUtils_CreateFloatProperty(0.0f);
        maybeThrow(OH_ArkUI_RenderNodeUtils_AttachFloatProperty(modifier_, invalidateCountProperty_));

        maybeThrow(OH_ArkUI_RenderNodeUtils_SetContentModifierOnDraw(
            modifier_, this, [](ArkUI_DrawContext *context, void *userData) {
                const auto *data = static_cast<CircleGradientRenderNode *>(userData);
                auto *canvas1 = OH_ArkUI_DrawContext_GetCanvas(context);
                auto *canvas = static_cast<OH_Drawing_Canvas *>(canvas1);

                if (data->shader_ == nullptr) {
                    LOGE("CircleGradientRenderNode::onDraw: shader is null");
                    return;
                }

                // 圆形绘制使用相对坐标
                // RenderNode 的 bounds 已经设置为包含整个圆形的矩形
                // 圆心相对于 RenderNode 左上角的坐标
                const float centerX = data->centerX_;
                const float centerY = data->centerY_;
                const float radius = data->radius_;

                // 创建 shader effect
                // 圆形的绘制尺寸为直径
                const float diameter = radius * 2.0f;
                OH_Drawing_ShaderEffect *shaderEffect =
                    CreateShaderEffectWithScaledSize(data->shader_, diameter, diameter);
                OH_Drawing_Point* point = OH_Drawing_PointCreate(centerX, centerY);

                if (data->paintingStyle_ == OH_Native_Draw_PaintingStyle::Fill) {
                    LOGI("CircleGradientRenderNode::onDraw: draw fill circle with shader");
                    
                    // 填充模式：使用 Brush
                    OH_Drawing_Brush *brush = OH_Drawing_BrushCreate();
                    OH_Drawing_BrushSetShaderEffect(brush, shaderEffect);

                    // 应用 ColorFilter 到 Brush
                    OH_Drawing_Filter *filter = ApplyColorFilterToBrush(brush, data->colorFilter_);

                    OH_Drawing_CanvasAttachBrush(canvas, brush);

                    // 绘制圆形
                    OH_Drawing_CanvasDrawCircle(canvas, point, radius);

                    // 清理资源
                    OH_Drawing_CanvasDetachBrush(canvas);
                    OH_Drawing_BrushDestroy(brush);
                    if (filter != nullptr) {
                        OH_Drawing_FilterDestroy(filter);
                    }
                } else {
                    LOGI("CircleGradientRenderNode::onDraw: draw stroke circle with shader, strokeWidth=%{public}f",
                         data->strokeWidth_);
                    
                    // 描边模式：使用 Pen
                    OH_Drawing_Pen *pen = OH_Drawing_PenCreate();
                    OH_Drawing_PenSetShaderEffect(pen, shaderEffect);
                    OH_Drawing_PenSetWidth(pen, data->strokeWidth_);

                    // 应用 ColorFilter 到 Pen
                    OH_Drawing_Filter *filter = ApplyColorFilterToPen(pen, data->colorFilter_);

                    OH_Drawing_CanvasAttachPen(canvas, pen);

                    // 绘制圆形
                    OH_Drawing_CanvasDrawCircle(canvas, point, radius);

                    // 清理资源
                    OH_Drawing_CanvasDetachPen(canvas);
                    OH_Drawing_PenDestroy(pen);
                    if (filter != nullptr) {
                        OH_Drawing_FilterDestroy(filter);
                    }
                }

                OH_Drawing_ShaderEffectDestroy(shaderEffect);
                OH_Drawing_PointDestroy(point);
                LOGI("CircleGradientRenderNode::onDraw: draw circle with shader finish");
            }));
    }
}

} // namespace OH




