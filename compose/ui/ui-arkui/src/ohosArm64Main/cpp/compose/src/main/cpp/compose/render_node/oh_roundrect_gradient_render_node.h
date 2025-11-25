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

#ifndef OH_ROUNDRECT_GRADIENT_RENDER_NODE_H
#define OH_ROUNDRECT_GRADIENT_RENDER_NODE_H

#include "../shader/oh_native_linear_gradient_shader.h"
#include "../filter/oh_compose_native_color_filter.h"
#include "oh_base_render_node.h"

namespace OH {

/**
 * RoundRectGradientRenderNode - 圆角矩形渐变渲染节点
 * 参考 iOS TMMCALayerDrawRoundRect 实现（使用 TMMNativeComposeGradientLayer + cornerRadius）
 *
 * 职责：
 * 1. 在 ContentModifier 中使用 OH_Drawing_CanvasDrawRoundRect 绘制带渐变的圆角矩形
 * 2. 支持 Fill 和 Stroke 两种绘制模式
 * 3. 支持 ColorFilter 颜色滤镜效果
 */
class RoundRectGradientRenderNode : public BaseRenderNode {
public:
    ~RoundRectGradientRenderNode() override;
    RoundRectGradientRenderNode();

    /**
     * 绘制带渐变的圆角矩形
     *
     * @param left 左边界
     * @param top 上边界
     * @param right 右边界
     * @param bottom 下边界
     * @param radiusX X 方向圆角半径
     * @param radiusY Y 方向圆角半径
     * @param strokeWidth 描边宽度（仅 Stroke 模式有效）
     * @param shader 渐变 shader
     * @param style 绘制模式（Fill 或 Stroke）
     * @param colorFilter 颜色滤镜（可选）
     */
    void drawRoundRect(float left, float top, float right, float bottom, float radiusX, float radiusY,
                       float strokeWidth, NativeBasicShader *shader, OH_Native_Draw_PaintingStyle style,
                       OHComposeNativeColorFilter *colorFilter = nullptr);

    OH_DrawingNode_Type getType() override;

private:
    void invalidate() override;
    void initModifier() override;

    // 圆角矩形参数
    float left_ = 0.0f;
    float top_ = 0.0f;
    float right_ = 0.0f;
    float bottom_ = 0.0f;
    float radiusX_ = 0.0f;
    float radiusY_ = 0.0f;
    float strokeWidth_ = 0.0f;
    NativeBasicShader *shader_ = nullptr;
    OHComposeNativeColorFilter *colorFilter_ = nullptr;
    OH_Native_Draw_PaintingStyle paintingStyle_ = OH_Native_Draw_PaintingStyle::Fill;

    // PropertyHandle 用于触发 onDraw
    ArkUI_FloatPropertyHandle invalidateCountProperty_ = nullptr;
    ArkUI_RenderContentModifierHandle modifier_ = nullptr;
};

} // namespace OH

#endif // OH_ROUNDRECT_GRADIENT_RENDER_NODE_H

