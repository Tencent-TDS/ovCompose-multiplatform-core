#include <native_drawing/drawing_canvas.h>
#include <native_drawing/drawing_pen.h>
#include <native_drawing/drawing_brush.h>
#include <native_drawing/drawing_types.h>
#include <cfloat>
#include <algorithm>
#include "oh_points_render_node.h"
#include "../shader/oh_native_shader_utils.h"
#include "../xcomponent_log.h"

namespace OH {
PointsRenderNode::~PointsRenderNode() {
    if (invalidateCountProperty_) {
        OH_ArkUI_RenderNodeUtils_DisposeFloatProperty(invalidateCountProperty_);
    }
    if (modifier_) {
        OH_ArkUI_RenderNodeUtils_DisposeContentModifier(modifier_);
    }
    // Note: paint_ is not owned by PointsRenderNode, so we don't release it here
}

PointsRenderNode::PointsRenderNode() {
    this->PointsRenderNode::initModifier();
}

OH_DrawingNode_Type PointsRenderNode::getType() {
    return OH_DrawingNode_Type::PointsNode;
}

void PointsRenderNode::drawPoints(OH_Drawing_PointMode pointMode, const float *points, size_t pointCount,
                                  OHComposeNativePaint *paint) {
    LOGI("PointsRenderNode::drawPoints: pointMode=%{public}d, pointCount=%{public}zu", pointMode, pointCount);

    // 保存参数
    pointMode_ = pointMode;
    points_.assign(points, points + pointCount * 2); // 复制点数据 [x1, y1, x2, y2, ...]
    paint_ = paint;

    // 计算边界框
    if (pointCount == 0) {
        this->setSize(0, 0);
        return;
    }

    float minX = points[0];
    float minY = points[1];
    float maxX = points[0];
    float maxY = points[1];

    for (size_t i = 0; i < pointCount; ++i) {
        const float x = points[i * 2];
        const float y = points[i * 2 + 1];
        minX = std::min(minX, x);
        minY = std::min(minY, y);
        maxX = std::max(maxX, x);
        maxY = std::max(maxY, y);
    }

    // 考虑 strokeWidth（如果是 Stroke 模式）
    const float strokeWidth = paint ? paint->strokeWidth : 0.0f;
    const float halfStroke = strokeWidth / 2.0f;

    // 设置 RenderNode 的位置和尺寸
    const int32_t x = static_cast<int32_t>(minX - halfStroke);
    const int32_t y = static_cast<int32_t>(minY - halfStroke);
    const int32_t width = static_cast<int32_t>(maxX - minX + strokeWidth);
    const int32_t height = static_cast<int32_t>(maxY - minY + strokeWidth);

    this->setPosition(x, y);
    this->setSize(width, height);

    // 触发重绘
    invalidate();
}

void PointsRenderNode::invalidate() {
    if (!invalidateCountProperty_) {
        return;
    }

    // 读取当前值
    float currentCount = 0.0f;
    OH_ArkUI_RenderNodeUtils_GetFloatPropertyValue(invalidateCountProperty_, &currentCount);

    // 加1，处理溢出（回绕到0）
    float newCount = (currentCount >= FLT_MAX - 1.0f) ? 0.0f : (currentCount + 1.0f);

    // 设置新值，触发onDraw回调
    OH_ArkUI_RenderNodeUtils_SetFloatPropertyValue(invalidateCountProperty_, newCount);
}

void PointsRenderNode::initModifier() {
    if (!modifier_) {
        modifier_ = OH_ArkUI_RenderNodeUtils_CreateContentModifier();
        maybeThrow(OH_ArkUI_RenderNodeUtils_AttachContentModifier(nodeHandle_, modifier_));

        // 创建invalidateCount PropertyHandle
        invalidateCountProperty_ = OH_ArkUI_RenderNodeUtils_CreateFloatProperty(0.0f);
        maybeThrow(OH_ArkUI_RenderNodeUtils_AttachFloatProperty(modifier_, invalidateCountProperty_));

        maybeThrow(OH_ArkUI_RenderNodeUtils_SetContentModifierOnDraw(
            modifier_, this, [](ArkUI_DrawContext *context, void *userData) {
                const auto *data = static_cast<PointsRenderNode *>(userData);
                auto *canvas1 = OH_ArkUI_DrawContext_GetCanvas(context);
                auto *canvas = static_cast<OH_Drawing_Canvas *>(canvas1);

                if (data->points_.empty() || data->paint_ == nullptr) {
                    LOGI("PointsRenderNode::onDraw: points empty or paint is null, skipping");
                    return;
                }

                const size_t pointCount = data->points_.size() / 2;
                if (pointCount == 0) {
                    return;
                }

                // 计算最小坐标（用于转换为相对坐标）
                float minX = data->points_[0];
                float minY = data->points_[1];
                for (size_t i = 0; i < pointCount; ++i) {
                    minX = std::min(minX, data->points_[i * 2]);
                    minY = std::min(minY, data->points_[i * 2 + 1]);
                }

                const float strokeWidth = data->paint_->strokeWidth;
                const float halfStroke = strokeWidth / 2.0f;

                // 转换为相对坐标（相对于 RenderNode 的左上角）
                // RenderNode 的 position 已经设置为 (minX - halfStroke, minY - halfStroke)
                // 所以相对坐标需要加上 halfStroke
                std::vector<OH_Drawing_Point2D> relativePoints(pointCount);
                for (size_t i = 0; i < pointCount; ++i) {
                    relativePoints[i].x = data->points_[i * 2] - minX + halfStroke;
                    relativePoints[i].y = data->points_[i * 2 + 1] - minY + halfStroke;
                }

                // 应用 paint 属性
                const OH_Native_Draw_PaintingStyle style = data->paint_->style;
                const uint32_t color = data->paint_->color;
                const OH::NativeBasicShader *shader = data->paint_->shader;

                if (style == OH_Native_Draw_PaintingStyle::Stroke) {
                    // Stroke 模式：使用 Pen
                    OH_Drawing_Pen *pen = OH_Drawing_PenCreate();
                    OH_Drawing_PenSetWidth(pen, strokeWidth);
                    OH_Drawing_PenSetColor(pen, color);

                    OH_Drawing_ShaderEffect *shaderEffect = nullptr;
                    // 应用 shader（如果有）
                    if (shader) {
                        // 计算绘制区域的尺寸（用于 shader 坐标缩放）
                        float maxX = relativePoints[0].x;
                        float maxY = relativePoints[0].y;
                        for (size_t i = 0; i < pointCount; ++i) {
                            maxX = std::max(maxX, relativePoints[i].x);
                            maxY = std::max(maxY, relativePoints[i].y);
                        }
                        const float drawWidth = maxX;
                        const float drawHeight = maxY;
                        shaderEffect = CreateShaderEffectWithScaledSize(
                            const_cast<OH::NativeBasicShader *>(shader), drawWidth, drawHeight);
                        OH_Drawing_PenSetShaderEffect(pen, shaderEffect);
                    }

                    // 应用 strokeCap
                    OH_Drawing_PenLineCapStyle lineCapStyle = OH_Drawing_PenLineCapStyle::LINE_FLAT_CAP;
                    if (data->paint_->strokeCap == OH_Native_Draw_StrokeCap::StrokeCapRound) {
                        lineCapStyle = OH_Drawing_PenLineCapStyle::LINE_ROUND_CAP;
                    } else if (data->paint_->strokeCap == OH_Native_Draw_StrokeCap::StrokeCapSquare) {
                        lineCapStyle = OH_Drawing_PenLineCapStyle::LINE_SQUARE_CAP;
                    }
                    OH_Drawing_PenSetCap(pen, lineCapStyle);

                    // 应用 strokeJoin
                    OH_Drawing_PenLineJoinStyle lineJoinStyle = OH_Drawing_PenLineJoinStyle::LINE_MITER_JOIN;
                    if (data->paint_->strokeJoin == OH_Native_Draw_StrokeJoin::StrokeJoinRound) {
                        lineJoinStyle = OH_Drawing_PenLineJoinStyle::LINE_ROUND_JOIN;
                    } else if (data->paint_->strokeJoin == OH_Native_Draw_StrokeJoin::StrokeJoinBevel) {
                        lineJoinStyle = OH_Drawing_PenLineJoinStyle::LINE_BEVEL_JOIN;
                    }
                    OH_Drawing_PenSetJoin(pen, lineJoinStyle);

                    // 应用 pathEffect（如果有）
                    if (data->paint_->pathEffect) {
                        OH_Drawing_PenSetPathEffect(pen, data->paint_->pathEffect);
                    }

                    OH_Drawing_CanvasAttachPen(canvas, pen);
                    OH_Drawing_CanvasDrawPoints(canvas, data->pointMode_, pointCount, relativePoints.data());
                    OH_Drawing_CanvasDetachPen(canvas);

                    // 释放资源
                    OH_Drawing_PenDestroy(pen);
                    if (shaderEffect) {
                        OH_Drawing_ShaderEffectDestroy(shaderEffect);
                    }
                } else {
                    // Fill 模式：使用 Brush
                    OH_Drawing_Brush *brush = OH_Drawing_BrushCreate();
                    OH_Drawing_BrushSetColor(brush, color);

                    OH_Drawing_ShaderEffect *shaderEffect = nullptr;
                    // 应用 shader（如果有）
                    if (shader) {
                        // 计算绘制区域的尺寸（用于 shader 坐标缩放）
                        float maxX = relativePoints[0].x;
                        float maxY = relativePoints[0].y;
                        for (size_t i = 0; i < pointCount; ++i) {
                            maxX = std::max(maxX, relativePoints[i].x);
                            maxY = std::max(maxY, relativePoints[i].y);
                        }
                        const float drawWidth = maxX;
                        const float drawHeight = maxY;
                        shaderEffect = CreateShaderEffectWithScaledSize(
                            const_cast<OH::NativeBasicShader *>(shader), drawWidth, drawHeight);
                        OH_Drawing_BrushSetShaderEffect(brush, shaderEffect);
                    }

                    OH_Drawing_CanvasAttachBrush(canvas, brush);
                    OH_Drawing_CanvasDrawPoints(canvas, data->pointMode_, pointCount, relativePoints.data());
                    OH_Drawing_CanvasDetachBrush(canvas);

                    // 释放资源
                    OH_Drawing_BrushDestroy(brush);
                    if (shaderEffect) {
                        OH_Drawing_ShaderEffectDestroy(shaderEffect);
                    }
                }
            }));
    }
}
} // namespace OH
