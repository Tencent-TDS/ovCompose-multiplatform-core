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

#ifndef OH_CIRCLE_GRADIENT_RENDER_NODE_H
#define OH_CIRCLE_GRADIENT_RENDER_NODE_H

#include "../shader/oh_native_linear_gradient_shader.h"
#include "../filter/oh_compose_native_color_filter.h"
#include "oh_base_render_node.h"

namespace OH {

/**
 * CircleGradientRenderNode - 圆形渐变渲染节点
 * 参考 iOS TMMCALayerDrawCircle 实现（使用 TMMNativeComposeGradientLayer + 圆形 mask）
 *
 * 职责：
 * 1. 在 ContentModifier 中使用 OH_Drawing_CanvasDrawCircle 绘制带渐变的圆形
 * 2. 支持 Fill 和 Stroke 两种绘制模式
 * 3. 支持 ColorFilter 颜色滤镜效果
 */
class CircleGradientRenderNode : public BaseRenderNode {
public:
    ~CircleGradientRenderNode() override;
    CircleGradientRenderNode();

    /**
     * 绘制带渐变的圆形
     *
     * @param centerX 圆心 X 坐标
     * @param centerY 圆心 Y 坐标
     * @param radius 半径
     * @param strokeWidth 描边宽度（仅 Stroke 模式有效）
     * @param shader 渐变 shader
     * @param style 绘制模式（Fill 或 Stroke）
     * @param colorFilter 颜色滤镜（可选）
     */
    void drawCircle(float centerX, float centerY, float radius, float strokeWidth, NativeBasicShader *shader,
                    OH_Native_Draw_PaintingStyle style, OHComposeNativeColorFilter *colorFilter = nullptr);

    OH_DrawingNode_Type getType() override;

private:
    void invalidate() override;
    void initModifier() override;

    // 圆形参数
    float centerX_ = 0.0f;
    float centerY_ = 0.0f;
    float radius_ = 0.0f;
    float strokeWidth_ = 0.0f;
    NativeBasicShader *shader_ = nullptr;
    OHComposeNativeColorFilter *colorFilter_ = nullptr;
    OH_Native_Draw_PaintingStyle paintingStyle_ = OH_Native_Draw_PaintingStyle::Fill;

    // PropertyHandle 用于触发 onDraw
    ArkUI_FloatPropertyHandle invalidateCountProperty_ = nullptr;
    ArkUI_RenderContentModifierHandle modifier_ = nullptr;
};

} // namespace OH

#endif // OH_CIRCLE_GRADIENT_RENDER_NODE_H




