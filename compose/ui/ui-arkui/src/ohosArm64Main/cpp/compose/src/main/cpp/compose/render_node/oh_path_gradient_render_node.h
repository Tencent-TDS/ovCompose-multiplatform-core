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

#ifndef OH_PATH_GRADIENT_RENDER_NODE_H
#define OH_PATH_GRADIENT_RENDER_NODE_H

#include "../shader/oh_native_linear_gradient_shader.h"
#include "../filter/oh_compose_native_color_filter.h"
#include "oh_base_render_node.h"
#include <native_drawing/drawing_path.h>

namespace OH {

/**
 * PathGradientRenderNode - 路径渐变渲染节点
 * 参考 iOS TMMCALayerDrawPath 实现（使用 TMMNativeComposeGradientLayer + path）
 *
 * 职责：
 * 1. 在 ContentModifier 中使用 OH_Drawing_CanvasDrawPath 绘制带渐变的路径
 * 2. 支持 Fill 和 Stroke 两种绘制模式
 * 3. 支持 ColorFilter 颜色滤镜效果
 * 4. 支持任意复杂的路径形状
 */
class PathGradientRenderNode : public BaseRenderNode {
public:
    ~PathGradientRenderNode() override;
    PathGradientRenderNode();

    /**
     * 绘制带渐变的路径
     *
     * @param path Drawing路径对象（需要 clone）
     * @param strokeWidth 描边宽度（仅 Stroke 模式有效）
     * @param shader 渐变 shader
     * @param style 绘制模式（Fill 或 Stroke）
     * @param colorFilter 颜色滤镜（可选）
     */
    void drawPath(OH_Drawing_Path *path, float strokeWidth, NativeBasicShader *shader,
                  OH_Native_Draw_PaintingStyle style, OHComposeNativeColorFilter *colorFilter = nullptr);

    OH_DrawingNode_Type getType() override;

private:
    void invalidate() override;
    void initModifier() override;

    // 路径参数
    OH_Drawing_Path *path_ = nullptr;  // 持有 path 的副本
    float strokeWidth_ = 0.0f;
    NativeBasicShader *shader_ = nullptr;
    OHComposeNativeColorFilter *colorFilter_ = nullptr;
    OH_Native_Draw_PaintingStyle paintingStyle_ = OH_Native_Draw_PaintingStyle::Fill;

    // PropertyHandle 用于触发 onDraw
    ArkUI_FloatPropertyHandle invalidateCountProperty_ = nullptr;
    ArkUI_RenderContentModifierHandle modifier_ = nullptr;
};

} // namespace OH

#endif // OH_PATH_GRADIENT_RENDER_NODE_H




