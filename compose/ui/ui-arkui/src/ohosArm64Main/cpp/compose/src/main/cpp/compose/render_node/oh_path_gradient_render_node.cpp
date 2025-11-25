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

#include "oh_path_gradient_render_node.h"
#include <native_drawing/drawing_brush.h>
#include <native_drawing/drawing_canvas.h>
#include <native_drawing/drawing_pen.h>
#include <native_drawing/drawing_rect.h>
#include <native_drawing/drawing_filter.h>
#include <cfloat>
#include "../shader/oh_native_shader_utils.h"
#include "../xcomponent_log.h"
#include "oh_render_node_color_filter_utils.h"

namespace OH {

PathGradientRenderNode::~PathGradientRenderNode() {
    if (path_) {
        OH_Drawing_PathDestroy(path_);
        path_ = nullptr;
    }
    if (invalidateCountProperty_) {
        OH_ArkUI_RenderNodeUtils_DisposeFloatProperty(invalidateCountProperty_);
    }
    if (modifier_) {
        OH_ArkUI_RenderNodeUtils_DisposeContentModifier(modifier_);
    }
}

PathGradientRenderNode::PathGradientRenderNode() {
    this->PathGradientRenderNode::initModifier();
}

OH_DrawingNode_Type PathGradientRenderNode::getType() {
    return OH_DrawingNode_Type::PathNode;
}

void PathGradientRenderNode::drawPath(OH_Drawing_Path *path, const float strokeWidth, NativeBasicShader *shader,
                                     const OH_Native_Draw_PaintingStyle style,
                                     OHComposeNativeColorFilter *colorFilter) {
    // 释放旧的 path_
    if (path_) {
        OH_Drawing_PathDestroy(path_);
        path_ = nullptr;
    }

    // 复制新的 path（需要持有副本，因为原 path 可能会被释放）
    if (path) {
        path_ = OH_Drawing_PathCopy(path);
    }

    // 更新成员变量
    strokeWidth_ = strokeWidth;
    shader_ = shader;
    paintingStyle_ = style;
    colorFilter_ = colorFilter;

    // 触发重绘
    invalidate();
}

void PathGradientRenderNode::invalidate() {
    // 读取当前值
    float currentCount = 0.0f;
    OH_ArkUI_RenderNodeUtils_GetFloatPropertyValue(invalidateCountProperty_, &currentCount);

    // 加1，处理溢出（回绕到0）
    float newCount = (currentCount >= FLT_MAX - 1.0f) ? 0.0f : (currentCount + 1.0f);

    // 设置新值，触发 onDraw 回调
    OH_ArkUI_RenderNodeUtils_SetFloatPropertyValue(invalidateCountProperty_, newCount);
}

void PathGradientRenderNode::initModifier() {
    if (!modifier_) {
        modifier_ = OH_ArkUI_RenderNodeUtils_CreateContentModifier();
        maybeThrow(OH_ArkUI_RenderNodeUtils_AttachContentModifier(nodeHandle_, modifier_));

        // 创建 invalidateCount PropertyHandle
        invalidateCountProperty_ = OH_ArkUI_RenderNodeUtils_CreateFloatProperty(0.0f);
        maybeThrow(OH_ArkUI_RenderNodeUtils_AttachFloatProperty(modifier_, invalidateCountProperty_));

        maybeThrow(OH_ArkUI_RenderNodeUtils_SetContentModifierOnDraw(
            modifier_, this, [](ArkUI_DrawContext *context, void *userData) {
                const auto *data = static_cast<PathGradientRenderNode *>(userData);
                auto *canvas1 = OH_ArkUI_DrawContext_GetCanvas(context);
                auto *canvas = static_cast<OH_Drawing_Canvas *>(canvas1);

                if (data->shader_ == nullptr) {
                    LOGE("PathGradientRenderNode::onDraw: shader is null");
                    return;
                }

                if (data->path_ == nullptr) {
                    LOGE("PathGradientRenderNode::onDraw: path is null");
                    return;
                }

                // 获取 path 的边界框，用于计算 shader 的缩放
                OH_Drawing_Rect *bounds = OH_Drawing_RectCreate(0, 0, 0, 0);
                OH_Drawing_PathGetBounds(data->path_, bounds);
                
                float left = OH_Drawing_RectGetLeft(bounds);
                float top = OH_Drawing_RectGetTop(bounds);
                float right = OH_Drawing_RectGetRight(bounds);
                float bottom = OH_Drawing_RectGetBottom(bounds);
                OH_Drawing_RectDestroy(bounds);

                const float width = right - left;
                const float height = bottom - top;

                // 创建 shader effect
                OH_Drawing_ShaderEffect *shaderEffect = CreateShaderEffectWithScaledSize(data->shader_, width, height);

                if (data->paintingStyle_ == OH_Native_Draw_PaintingStyle::Fill) {
                    LOGI("PathGradientRenderNode::onDraw: draw fill path with shader");

                    // 填充模式：使用 Brush
                    OH_Drawing_Brush *brush = OH_Drawing_BrushCreate();
                    OH_Drawing_BrushSetShaderEffect(brush, shaderEffect);

                    // 应用 ColorFilter 到 Brush
                    OH_Drawing_Filter *filter = ApplyColorFilterToBrush(brush, data->colorFilter_);

                    OH_Drawing_CanvasAttachBrush(canvas, brush);

                    // 绘制路径
                    OH_Drawing_CanvasDrawPath(canvas, data->path_);

                    // 清理资源
                    OH_Drawing_CanvasDetachBrush(canvas);
                    OH_Drawing_BrushDestroy(brush);
                    if (filter != nullptr) {
                        OH_Drawing_FilterDestroy(filter);
                    }
                } else {
                    LOGI("PathGradientRenderNode::onDraw: draw stroke path with shader, strokeWidth=%{public}f",
                         data->strokeWidth_);

                    // 描边模式：使用 Pen
                    OH_Drawing_Pen *pen = OH_Drawing_PenCreate();
                    OH_Drawing_PenSetShaderEffect(pen, shaderEffect);
                    OH_Drawing_PenSetWidth(pen, data->strokeWidth_);

                    // 应用 ColorFilter 到 Pen
                    OH_Drawing_Filter *filter = ApplyColorFilterToPen(pen, data->colorFilter_);

                    OH_Drawing_CanvasAttachPen(canvas, pen);

                    // 绘制路径
                    OH_Drawing_CanvasDrawPath(canvas, data->path_);

                    // 清理资源
                    OH_Drawing_CanvasDetachPen(canvas);
                    OH_Drawing_PenDestroy(pen);
                    if (filter != nullptr) {
                        OH_Drawing_FilterDestroy(filter);
                    }
                }

                OH_Drawing_ShaderEffectDestroy(shaderEffect);
                LOGI("PathGradientRenderNode::onDraw: draw path with shader finish");
            }));
    }
}

} // namespace OH

